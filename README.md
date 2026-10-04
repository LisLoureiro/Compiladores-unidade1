# Analisador Sintático SLR(1) — Linguagem Sigma

Trabalho Prático da II Unidade da disciplina de Compiladores (UESB/DCET).
Implementa, em Java, o **analisador léxico** (AFND, reaproveitado do 1º trabalho)
e o **analisador sintático SLR(1)** para a linguagem **Sigma**.

A documentação completa (formato de artigo) está em [`DOCUMENTACAO.md`](DOCUMENTACAO.md).

## Arquivos

| Arquivo | Descrição |
|---|---|
| `TipoToken.java` | Classes léxicas da linguagem Sigma. |
| `Token.java` | Representação de um token. |
| `AnalisadorLexico.java` | Analisador léxico (simulação de AFND + maior casamento). |
| `Gramatica.java` | Gramática, FIRST/FOLLOW, itens LR(0) e tabela SLR(1). |
| `AnalisadorSintatico.java` | Parser SLR(1) e interface de linha de comando. |
| `exemplo.txt` | Programa de exemplo. |
| `testes/` | 08 arquivos de teste (válidos e com erros). |

## Como executar

### Sem Docker (mais simples)

Os arquivos `.class` já vêm compilados e são compatíveis com **Java 8**:

    executar.bat exemplo.txt            (Windows)
    ./executar.sh exemplo.txt           (Linux/macOS)

Para analisar um dos testes:

    executar.bat testes\01_valido_basico.txt
    ./executar.sh testes/01_valido_basico.txt

Para rodar todos os testes:

    executar-testes.bat                  (Windows)
    ./executar-testes.sh                 (Linux/macOS)

Caso queira recompilar (requer JDK):

    compilar.bat                         (Windows)
    ./compilar.sh                        (Linux/macOS)

### Com Docker

    docker build -t analisador-sigma .
    docker run --rm analisador-sigma                          # programa de exemplo
    docker run --rm analisador-sigma exemplo.txt
    docker run --rm analisador-sigma testes/01_valido_basico.txt --tokens

    # montando a pasta atual para analisar arquivos próprios
    docker run --rm -v "$PWD":/dados analisador-sigma /dados/exemplo.txt

No PowerShell use `${PWD}` e no CMD use `%cd%` no lugar de `"$PWD"`.

## Opções de linha de comando

    java AnalisadorSintatico [arquivo] [opções]

| Opção | Efeito |
|---|---|
| `--tokens` | Imprime a tabela de tokens reconhecidos. |
| `--gramatica` | Imprime produções, conjuntos FIRST e FOLLOW. |
| `--tabela` | Imprime a tabela SLR(1) e os conflitos resolvidos. |
| `--passos` | Imprime o passo a passo da análise sintática. |
| `--automato` | Imprime a tabela de transições do AFND do léxico. |
| `-h`, `--help` | Mostra a ajuda. |

Sem informar arquivo, o programa analisa um exemplo embutido.

O programa retorna **código de saída 0** para programas válidos e **1** para
programas com erro léxico ou sintático, o que facilita o uso em scripts.

## Observação sobre acentos

No console do Windows, rode `chcp 65001` (ou use os scripts `.bat`, que já o
fazem) para exibir corretamente os caracteres acentuados.
