# Analisador Sigma — versão sem Docker

Pacote autônomo do Trabalho Prático da II Unidade (Compiladores — UESB/DCET)
com:

- **analisador léxico** em AFND;
- **analisador sintático SLR(1)**;
- **interface web local** com as duas fases;
- conjunto de testes e scripts de execução.

Diferente da versão com Docker, este pacote **não precisa de Docker**: basta ter
**Java instalado** para executar o analisador. A interface web é opcional e
requer Python 3.

A documentação em formato de artigo, com um capítulo para cada componente, está
em [`DOCUMENTACAO.md`](DOCUMENTACAO.md).

## Requisitos

| Recurso | Para quê | Versão |
|---|---|---|
| **Java (JRE)** | executar o analisador (arquivos `.class` já compilados) | 8 ou superior |
| **Python 3** | interface web local (opcional) | 3.7 ou superior |
| **JDK** | apenas para recompilar o código (opcional) | 8 ou superior |

Não é necessário instalar bibliotecas: a interface usa somente a biblioteca
padrão do Python.

## Conteúdo do pacote

| Arquivo | Descrição |
|---|---|
| `AnalisadorLexico.java` / `.class` | Analisador léxico (AFND + maior casamento). |
| `TipoToken.java`, `Token.java` | Classes léxicas e estrutura de token. |
| `AnalisadorSintatico.java` / `.class` | Parser SLR(1) e linha de comando. |
| `Gramatica.java` / `.class` | Gramática, FIRST/FOLLOW, itens LR(0) e tabela SLR(1). |
| `TokenSintatico.java`, `AdaptadorLexico.java` | Desacoplamento entre léxico e parser. |
| `interface.html` + `servidor.py` | Interface web local por fases. |
| `exemplo.txt` | Programa de exemplo. |
| `testes/` | 10 arquivos de teste (válidos e com erros). |
| `executar.bat` / `executar.sh` | Executa o analisador. |
| `executar-testes.bat` / `.sh` | Roda a suíte e confere os vereditos. |
| `compilar.bat` / `compilar.sh` | Recompila (requer JDK). |
| `abrir-interface.bat` / `.sh` | Sobe a interface web e abre o navegador. |

## Executar o analisador (console)

Windows:

    executar.bat exemplo.txt
    executar.bat testes\01_valido_basico.txt --tokens
    executar.bat exemplo.txt --fase lexico
    executar.bat exemplo.txt --fase sintatico --passos

Linux/macOS:

    ./executar.sh exemplo.txt
    ./executar.sh testes/01_valido_basico.txt --tokens
    ./executar.sh exemplo.txt --fase lexico
    ./executar.sh exemplo.txt --fase sintatico --passos

Sem arquivo, o programa analisa um exemplo embutido.

### Opções de linha de comando

| Opção | Efeito |
|---|---|
| `--fase lexico\|sintatico\|ambas` | Executa apenas a fase indicada (padrão: ambas). |
| `--tokens` | Imprime a tabela de tokens reconhecidos. |
| `--gramatica` | Imprime produções, FIRST e FOLLOW. |
| `--tabela` | Imprime a tabela SLR(1) e os conflitos resolvidos. |
| `--passos` | Imprime o passo a passo da análise sintática. |
| `--automato` | Imprime a tabela de transições do AFND do léxico. |
| `-h`, `--help` | Mostra a ajuda. |

O programa retorna **0** para programas válidos e **1** para programas com erro
léxico ou sintático.

## Rodar todos os testes

    executar-testes.bat        (Windows)
    ./executar-testes.sh       (Linux/macOS)

Os scripts deduzem o veredito esperado do nome do arquivo (`valido`,
`erro_lexico`, `erro_sintatico`), comparam com a saída do analisador e exibem um
resumo (`10/10 testes OK`). Retornam código 1 se algum teste falhar.

## Abrir a interface web

Windows:

    abrir-interface.bat

Linux/macOS:

    ./abrir-interface.sh

Ou manualmente:

    python servidor.py
    python servidor.py --porta 9000 --no-browser

O navegador abre em `http://localhost:8000`. A abertura mostra a tela de título
**Bem-vindo à Zecas Bros**, com o professor Zeca em um cenário de plataforma
(colinas, cano, castelo, blocos `?` e moedas); ao clicar (ou pressionar Enter),
aparece o **mapa do mundo** no estilo *Super Mario World*, com ilhas, mar,
ponte, castelo e as fases como nós numerados: a **Fase 1-1** analisa apenas o
léxico (tokens e erros) e a **Fase 2-1** analisa a sintaxe (passos SLR(1) e
veredito). A tela de análise é a sala de aula do Zeca — quadro verde, piso de
madeira, estante e mesa do professor. O botão da interface executa somente a
fase selecionada, e a barra de carregamento mostra o Zeca caminhando enquanto o
analisador roda. O botão **MAPA** no HUD volta ao mapa; as abas continuam
disponíveis para alternar de fase.

## Recompilar (opcional)

Requer JDK instalado:

    compilar.bat        (Windows)
    ./compilar.sh       (Linux/macOS)

## Acentuação no console do Windows

Rode `chcp 65001` em um `cmd` antes de usar `java` diretamente, ou use os
scripts `.bat`, que já configuram o console para UTF-8.

## Como os componentes se conectam

    texto-fonte
        │
        ▼
    AnalisadorLexico (AFND)  ──►  List<Token>
        │                              │
        │                      AdaptadorLexico
        │                              ▼
        │                     List<TokenSintatico>
        │                              │
        └─────────────►  AnalisadorSintatico (SLR(1))

O parser não conhece o léxico: ele consome apenas `TokenSintatico`, e a
conversão fica isolada em `AdaptadorLexico`. A interface web apenas executa
essa mesma aplicação Java e exibe os resultados por fase.
