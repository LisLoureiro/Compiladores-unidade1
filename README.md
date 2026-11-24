# Analisador Sintático SLR(1) — Linguagem Sigma

Trabalho Prático da II Unidade da disciplina de Compiladores (UESB/DCET).
Implementa, em Java, o **analisador léxico** (AFND, reaproveitado do 1º trabalho)
e o **analisador sintático SLR(1)** para a linguagem **Sigma**.

A documentação completa (formato de artigo) está em [`DOCUMENTACAO.md`](DOCUMENTACAO.md).

## Arquivos

| Arquivo | Descrição |
|---|---|
| `TipoToken.java` | Classes léxicas da linguagem Sigma. |
| `Token.java` | Representação de um token do léxico. |
| `AnalisadorLexico.java` | Analisador léxico (AFND + maior casamento). |
| `TokenSintatico.java` | Token neutro consumido pelo parser — independente do léxico. |
| `AdaptadorLexico.java` | Única ponte entre os tokens do léxico e o parser. |
| `Gramatica.java` | Gramática, FIRST/FOLLOW, itens LR(0) e tabela SLR(1). |
| `AnalisadorSintatico.java` | Parser SLR(1), fases e interface de linha de comando. |
| `interface.html` / `servidor.py` | Interface web local com seleção de fase. |
| `exemplo.txt` | Programa de exemplo. |
| `testes/` | 10 arquivos de teste (válidos e com erros). |

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

Os scripts deduzem o veredito esperado do nome do arquivo (`valido`,
`erro_lexico`, `erro_sintatico`), comparam com a saída do analisador e exibem um
resumo (`10/10 testes OK`). Retornam código de saída 1 se algum teste falhar.

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
| `--fase lexico\|sintatico\|ambas` | Executa apenas a fase indicada (padrão: ambas). |
| `--tokens` | Imprime a tabela de tokens reconhecidos. |
| `--gramatica` | Imprime produções, conjuntos FIRST e FOLLOW. |
| `--tabela` | Imprime a tabela SLR(1) e os conflitos resolvidos. |
| `--passos` | Imprime o passo a passo da análise sintática. |
| `--automato` | Imprime a tabela de transições do AFND do léxico. |
| `-h`, `--help` | Mostra a ajuda. |

Sem informar arquivo, o programa analisa um exemplo embutido.

O programa retorna **código de saída 0** para programas válidos e **1** para
programas com erro léxico ou sintático, o que facilita o uso em scripts.

## Fases e interface web

O pipeline é dividido em duas fases que podem ser executadas separadamente:

| Fase | Comando | Saída |
|---|---|---|
| Fase 1 — análise léxica | `--fase lexico` | sequência de tokens e erros léxicos |
| Fase 2 — análise sintática | `--fase sintatico` | passos SLR(1) e veredito |

O parser SLR(1) **não conhece o analisador léxico**: ele consome apenas
`TokenSintatico`, e a conversão fica isolada em `AdaptadorLexico.java`.

A interface web abre em uma tela de título ("Bem-vindo à Zecas Bros") com o
professor Zeca e um cenário de plataforma; um clique leva ao mapa do mundo no
estilo *Super Mario World* (ilhas, ponte, castelo e fases como nós numerados),
onde cada fase é um analisador. A análise acontece na sala de aula do Zeca
(quadro verde, piso de madeira, estante e mesa). O botão analisa apenas a fase
selecionada, e a barra de carregamento mostra o Zeca caminhando durante a
execução.

    python servidor.py

Abra `http://localhost:8000`, entre na fase pelo mapa e use **ANALISAR FASE 1**
ou **ANALISAR FASE 2**. O botão **MAPA** no HUD volta ao mapa.

## Observação sobre acentos

No console do Windows, rode `chcp 65001` (ou use os scripts `.bat`, que já o
fazem) para exibir corretamente os caracteres acentuados.

## Trabalho da Unidade 1

O léxico oficial da I Unidade é o **AFND da raiz** (`AnalisadorLexico.java`),
que cobre todas as classes léxicas da especificação e é o léxico usado pelo
parser da II Unidade — atende às Atividades 1 e 2 da I Unidade.

A pasta [`unidade1/`](unidade1/) é **material legado** de uma versão anterior da
disciplina (AFD para a linguagem Mini_Pascal) e é mantida apenas como histórico;
ela **não faz parte da entrega atual** e não deve ser confundida com o léxico
oficial.
