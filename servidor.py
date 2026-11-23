#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Servidor local da interface de teste do analisador Sigma.

Serve a página interface.html e executa o analisador Java já compilado
(AnalisadorSintatico.class) a cada análise pedida pelo navegador. Usa
apenas a biblioteca padrão do Python 3.

Uso:
    python servidor.py                 # abre http://localhost:8000
    python servidor.py --porta 9000
    python servidor.py --no-browser
"""

import argparse
import json
import os
import re
import subprocess
import sys
import tempfile
import threading
import time
import webbrowser
from concurrent.futures import ThreadPoolExecutor
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

BASE = os.path.dirname(os.path.abspath(__file__))
ARQUIVO_HTML = os.path.join(BASE, "interface.html")
DIR_TESTES = os.path.join(BASE, "testes")
ARQUIVO_EXEMPLO = os.path.join(BASE, "exemplo.txt")

OPCOES_PERMITIDAS = ("--tabela", "--gramatica", "--automato")
LIMITE_CORPO = 1_000_000  # 1 MB

RE_TOKEN = re.compile(r"^(\d+)\s+(\d+)\s+(.+?)\s{2,}(.+)$")
RE_TOKEN_ERRO = re.compile(r"^'(.*)' \((.*)\)$")
RE_ERRO_LEXICO = re.compile(r"^\s*linha (\d+), coluna (\d+): (.*) \('(.*)'\)\s*$")
RE_PASSO = re.compile(r"^Estado (\d+), lookahead '(.*)' -> (.*)$")
RE_STATS = re.compile(
    r"Tokens analisados: (\d+) \| Reduções: (\d+) \| Estados da tabela: (\d+)"
)
RE_ERRO_SINTATICO = re.compile(
    r"^ERRO SINTÁTICO na linha (\d+), coluna (\d+): token inesperado '(.*)' \((.*)\)\.$"
)


# ----------------------------------------------------------------------
# Execução do analisador Java
# ----------------------------------------------------------------------
def executar_java(caminho, opcoes_extra=()):
    """Roda o analisador sobre um arquivo e devolve texto/status/tempo."""
    comando = [
        "java",
        "-Dfile.encoding=UTF-8",
        "AnalisadorSintatico",
        caminho,
        "--tokens",
        "--passos",
    ]
    for opcao in opcoes_extra:
        if opcao in OPCOES_PERMITIDAS and opcao not in comando:
            comando.append(opcao)

    inicio = time.perf_counter()
    try:
        processo = subprocess.run(
            comando,
            cwd=BASE,
            capture_output=True,
            timeout=60,
        )
    except FileNotFoundError:
        raise RuntimeError(
            "Java não encontrado no PATH. Instale o JRE/JDK 8 ou superior."
        )
    except subprocess.TimeoutExpired:
        raise RuntimeError("A análise excedeu 60 segundos.")
    duracao_ms = int((time.perf_counter() - inicio) * 1000)

    saida = processo.stdout.decode("utf-8", "replace")
    erro = processo.stderr.decode("utf-8", "replace").strip()
    return {
        "saida": saida,
        "stderr": erro,
        "codigoSaida": processo.returncode,
        "tempoMs": duracao_ms,
    }


# ----------------------------------------------------------------------
# Interpretação da saída em texto
# ----------------------------------------------------------------------
def parsear_tokens_e_passos(saida):
    tokens = []
    passos = []
    na_tabela = False
    separadores = 0

    for linha in saida.splitlines():
        if linha.startswith("===== TOKENS"):
            na_tabela = True
            separadores = 0
            continue

        if na_tabela:
            if linha.startswith("---"):
                separadores += 1
                if separadores >= 2:
                    na_tabela = False
                continue

            m = RE_TOKEN.match(linha)
            if not m:
                continue

            linha_n, coluna, classe, resto = m.groups()
            erro = None
            if classe.strip() == "ERRO LÉXICO":
                me = RE_TOKEN_ERRO.match(resto.strip())
                if me:
                    lexema, erro = me.group(1), me.group(2)
                else:
                    lexema = resto.strip()
            else:
                lexema = resto.strip()

            tokens.append(
                {
                    "linha": int(linha_n),
                    "coluna": int(coluna),
                    "classe": classe.strip(),
                    "lexema": lexema,
                    "erro": erro,
                }
            )
            continue

        mp = RE_PASSO.match(linha)
        if mp:
            acao = mp.group(3)
            if acao == "aceitar":
                tipo = "aceitar"
            elif acao.startswith("empilhar"):
                tipo = "empilhar"
            elif acao.startswith("reduzir"):
                tipo = "reduzir"
            else:
                tipo = "outro"
            passos.append(
                {
                    "estado": int(mp.group(1)),
                    "lookahead": mp.group(2),
                    "acao": acao,
                    "tipo": tipo,
                }
            )

    return tokens, passos


def resumo_sintatico(saida):
    if "Programa sintaticamente VÁLIDO." in saida:
        dados = {"status": "valido"}
        m = RE_STATS.search(saida)
        if m:
            dados["tokensAnalisados"] = int(m.group(1))
            dados["reducoes"] = int(m.group(2))
            dados["estados"] = int(m.group(3))
        return dados

    if "Programa sintaticamente INVÁLIDO." in saida:
        dados = {"status": "erro_sintatico", "mensagem": None, "esperados": []}
        for linha in saida.splitlines():
            s = linha.strip()
            m = RE_ERRO_SINTATICO.match(s)
            if m:
                dados.update(
                    {
                        "linha": int(m.group(1)),
                        "coluna": int(m.group(2)),
                        "lexema": m.group(3),
                        "classe": m.group(4),
                        "mensagem": s,
                    }
                )
            elif s.startswith("ERRO SINTÁTICO: fim inesperado"):
                dados.update({"fimInesperado": True, "mensagem": s})
            elif s.startswith("Terminais esperados neste ponto:"):
                itens = s.split(":", 1)[1]
                dados["esperados"] = re.findall(r"'([^']*)'", itens)
        return dados

    if "Análise sintática interrompida." in saida:
        erros = []
        for linha in saida.splitlines():
            m = RE_ERRO_LEXICO.match(linha)
            if m:
                erros.append(
                    {
                        "linha": int(m.group(1)),
                        "coluna": int(m.group(2)),
                        "mensagem": m.group(3),
                        "lexema": m.group(4),
                    }
                )
        return {"status": "erro_lexico", "erros": erros}

    return {"status": "desconhecido"}


def ler_texto(caminho):
    with open(caminho, "r", encoding="utf-8", errors="replace") as f:
        return f.read()


def listar_testes():
    itens = []
    if os.path.exists(ARQUIVO_EXEMPLO):
        itens.append(
            {
                "arquivo": "exemplo.txt",
                "grupo": "exemplo",
                "conteudo": ler_texto(ARQUIVO_EXEMPLO),
            }
        )
    if os.path.isdir(DIR_TESTES):
        for nome in sorted(os.listdir(DIR_TESTES)):
            if nome.lower().endswith(".txt"):
                itens.append(
                    {
                        "arquivo": nome,
                        "grupo": "teste",
                        "conteudo": ler_texto(os.path.join(DIR_TESTES, nome)),
                    }
                )
    return itens


def esperado_de(nome):
    n = nome.lower()
    if "valido" in n:
        return "valido"
    if "erro_lexico" in n:
        return "lexico"
    if "erro_sintatico" in n or "erro_fim" in n:
        return "sintatico"
    return "desconhecido"


def obtido_de(sintetico):
    return {
        "valido": "valido",
        "erro_lexico": "lexico",
        "erro_sintatico": "sintatico",
    }.get(sintetico.get("status"), "desconhecido")


def detalhe_de(sintetico, tokens):
    status = sintetico.get("status")
    if status == "valido":
        return "{} tokens · {} reduções".format(
            sintetico.get("tokensAnalisados", len(tokens)),
            sintetico.get("reducoes", 0),
        )
    if status == "erro_lexico":
        erros = sintetico.get("erros") or []
        if erros:
            e = erros[0]
            return "linha {}, col {}: {}".format(e["linha"], e["coluna"], e["mensagem"])
        return "erro léxico"
    if status == "erro_sintatico":
        msg = sintetico.get("mensagem") or "erro sintático"
        return msg[:160]
    return "resultado não reconhecido"


def executar_suite():
    arquivos = []
    if os.path.isdir(DIR_TESTES):
        arquivos = [
            os.path.join(DIR_TESTES, nome)
            for nome in sorted(os.listdir(DIR_TESTES))
            if nome.lower().endswith(".txt")
        ]

    def rodar_um(caminho):
        nome = os.path.basename(caminho)
        resultado = executar_java(caminho)
        tokens, _ = parsear_tokens_e_passos(resultado["saida"])
        sintetico = resumo_sintatico(resultado["saida"])
        esperado = esperado_de(nome)
        obtido = obtido_de(sintetico)
        return {
            "arquivo": nome,
            "esperado": esperado,
            "obtido": obtido,
            "passou": esperado == obtido,
            "tempoMs": resultado["tempoMs"],
            "detalhe": detalhe_de(sintetico, tokens),
        }

    if not arquivos:
        return []
    with ThreadPoolExecutor(max_workers=min(4, len(arquivos))) as executor:
        return list(executor.map(rodar_um, arquivos))


# ----------------------------------------------------------------------
# HTTP
# ----------------------------------------------------------------------
class Manipulador(BaseHTTPRequestHandler):
    server_version = "AnalisadorSigma/1.0"

    def log_message(self, formato, *args):
        sys.stderr.write("[%s] %s\n" % (self.log_date_time_string(), formato % args))

    def _json(self, objeto, codigo=200):
        dados = json.dumps(objeto, ensure_ascii=False).encode("utf-8")
        self.send_response(codigo)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(dados)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(dados)

    def _responder_html(self):
        try:
            with open(ARQUIVO_HTML, "rb") as f:
                dados = f.read()
        except OSError:
            self.send_error(500, "interface.html não encontrada")
            return
        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(dados)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(dados)

    def do_GET(self):
        if self.path in ("/", "/index.html", "/interface.html"):
            self._responder_html()
        elif self.path == "/api/testes":
            self._json({"testes": listar_testes()})
        elif self.path == "/favicon.ico":
            self.send_response(204)
            self.end_headers()
        else:
            self.send_error(404, "Rota não encontrada")

    def do_POST(self):
        if self.path == "/api/analisar":
            self._post_analisar()
        elif self.path == "/api/testes/executar":
            try:
                self._json({"resultados": executar_suite()})
            except RuntimeError as e:
                self._json({"erro": str(e)}, 500)
        else:
            self.send_error(404, "Rota não encontrada")

    def _post_analisar(self):
        try:
            tamanho = int(self.headers.get("Content-Length") or 0)
            if tamanho > LIMITE_CORPO:
                self._json({"erro": "Entrada muito grande (máximo de 1 MB)."}, 413)
                return
            corpo = json.loads(self.rfile.read(tamanho).decode("utf-8")) if tamanho else {}
            codigo = corpo.get("codigo")
            if not isinstance(codigo, str):
                raise ValueError("Campo 'codigo' ausente ou inválido.")
            opcoes = [
                o for o in corpo.get("opcoes", []) if o in OPCOES_PERMITIDAS
            ]

            temporario = tempfile.NamedTemporaryFile(
                "w", suffix=".txt", delete=False, encoding="utf-8", newline=""
            )
            try:
                temporario.write(codigo)
                temporario.close()
                resultado = executar_java(temporario.name, opcoes)
            finally:
                try:
                    os.unlink(temporario.name)
                except OSError:
                    pass

            tokens, passos = parsear_tokens_e_passos(resultado["saida"])
            self._json(
                {
                    "tokens": tokens,
                    "passos": passos,
                    "sintatico": resumo_sintatico(resultado["saida"]),
                    "saida": resultado["saida"],
                    "stderr": resultado["stderr"],
                    "codigoSaida": resultado["codigoSaida"],
                    "tempoMs": resultado["tempoMs"],
                }
            )
        except ValueError as e:
            self._json({"erro": str(e)}, 400)
        except RuntimeError as e:
            self._json({"erro": str(e)}, 500)
        except Exception as e:  # noqa: BLE001 - falha inesperada vira resposta JSON
            self._json({"erro": "Erro interno: {}".format(e)}, 500)


def main():
    if hasattr(sys.stdout, "reconfigure"):
        try:
            sys.stdout.reconfigure(encoding="utf-8")
            sys.stderr.reconfigure(encoding="utf-8")
        except Exception:
            pass

    parser = argparse.ArgumentParser(description="Interface de teste do analisador Sigma")
    parser.add_argument("--porta", type=int, default=8000, help="porta HTTP (padrão: 8000)")
    parser.add_argument(
        "--no-browser", action="store_true", help="não abrir o navegador automaticamente"
    )
    args = parser.parse_args()

    try:
        servidor = ThreadingHTTPServer(("127.0.0.1", args.porta), Manipulador)
    except OSError as e:
        print("Nao foi possivel iniciar na porta {}: {}".format(args.porta, e))
        print("Tente outra porta, por exemplo: python servidor.py --porta 8001")
        sys.exit(1)

    url = "http://localhost:{}".format(args.porta)
    print("Interface disponivel em {}".format(url))
    print("Pressione Ctrl+C para encerrar.")

    if not args.no_browser:
        threading.Timer(0.6, lambda: webbrowser.open(url)).start()

    try:
        servidor.serve_forever()
    except KeyboardInterrupt:
        print("\nEncerrado.")
    finally:
        servidor.server_close()


if __name__ == "__main__":
    main()
