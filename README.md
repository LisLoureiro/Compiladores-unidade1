# Analisador Léxico da linguagem Mini_Pascal — AFD

Trabalho Prático da I Unidade da disciplina de **Compiladores** (UESB/DCET).

Implementa, em Java, um **analisador léxico** para a linguagem **Mini_Pascal**
definida no enunciado. O reconhecimento é feito por **um Autômato Finito
Determinístico (AFD)** descrito por uma **tabela de transições**
`delta[estado][símbolo] = próximo estado`.

## Como o léxico funciona

O AFD é construído uma única vez (`construirAFD()`), com 25 estados. A simulação
(`Lexer.proximoToken()`) lê o texto a partir do estado inicial `q0` e, a cada
caractere, consulta a tabela. Ao mesmo tempo em que avança, o analisador
**memoriza o último estado de aceitação** encontrado. Quando não existe transição
para o próximo caractere, ele **retrocede** até esse último aceite — é a regra do
**maior casamento** (*maximal munch*).

Como todo o reconhecimento é feito por um único autômato determinístico, cada
caractere da entrada corresponde a **no máximo uma** transição
(`delta[q][c]` é um estado só), o que caracteriza o determinismo.

### O par `<cadeia, token>`

A unidade devolvida é o par **`<cadeia, token>`** (o enunciado chama de *lexema*
e *token*). A função `Lexer.proximoToken()` devolve um objeto `Token` com o
lexema e a classe léxica, além da linha e da coluna para diagnóstico.

### Classes léxicas

| Classe léxica | Exemplos |
|---|---|
| Palavra reservada | `program`, `var`, `if`, `while`, `begin`, `end`, `for`, `repeat`, `until` |
| Identificador | `x`, `media_das_medias`, `Nota1`, `read`, `write`, `writeln` |
| Número inteiro | `1`, `13`, `2024` |
| Número real | `1.33`, `24.40e-04`, `2.5E+3` |
| Caractere | `'a'`, `'Z'`, `' '` |
| Cadeia | `"linguagem Mini_Pascal"`, `""` |
| Operador aritmético | `+` `-` `*` `/` `mod` |
| Operador relacional | `=` `>` `>=` `<` `<=` `<>` |
| Operador lógico | `and` `or` `not` |
| Símbolo especial | `(` `)` `,` `;` `:` |
| Atribuição | `:=` |
| Fim | `.` |

Observações:

- **`read`, `write` e `writeln` são identificadores**, não palavras reservadas —
  constam assim na tabela de palavras reservadas do enunciado.
- A linguagem é **case-insensitive**: `PROGRAM`, `Program` e `program` são a
  mesma palavra reservada.
- **Comentários** `/* ... */` e caracteres não significativos (espaço, TAB, ENTER)
  são descartados e **não** aparecem na saída.
- **Identificadores aceitam `_`** (o exemplo do enunciado usa nomes como
  `media_das_medias`).

## Arquivos

| Arquivo | Descrição |
|---|---|
| `AnalisadorLexicoAFD.java` | O AFD (tabela), a tabela de palavras reservadas, o analisador e a CLI. |
| `DOCUMENTACAO.md` | Documentação do trabalho (descrição, decisões, testes). |
| `exemplo.txt` | Programa Mini_Pascal de exemplo usando todas as classes. |
| `testes/` | 07 arquivos de teste (válidos e com erros léxicos). |

## Como executar

Os `.class` já vêm compilados para **Java 8** e rodam direto no JRE:

    executar.bat exemplo.txt            (Windows)
    ./executar.sh exemplo.txt           (Linux/macOS)

O programa imprime a tabela de tokens no console **e** grava os pares
`<cadeia, token>` (um por linha) em `saida.txt`:

    java AnalisadorLexicoAFD exemplo.txt saida.txt

Rodar todos os testes (grava um `.txt` por teste na pasta `saida/`):

    executar-testes.bat                  (Windows)
    ./executar-testes.sh                 (Linux/macOS)

Imprimir a tabela de transições do AFD:

    executar.bat --afd

Sem informar arquivo, o programa analisa o exemplo embutido no código.

Para recompilar (requer JDK):

    compilar.bat                         (Windows)
    ./compilar.sh                        (Linux/macOS)

## Opções de linha de comando

    java AnalisadorLexicoAFD [entrada.txt | -] [saida.txt] [--afd]
    java AnalisadorLexicoAFD --cadeia "texto a analisar" [saida.txt]

| Opção | Efeito |
|---|---|
| `--afd` | Imprime os estados finais e a tabela de transições do AFD. |
| `--cadeia "..."` | Analisa o texto passado direto na linha de comando. |
| `-` ou `--stdin` | Lê o programa da entrada padrão (digite e finalize com Ctrl+Z, ENTER no Windows, ou Ctrl+D no Linux/macOS). |
| `entrada.txt` | Programa a analisar (padrão: exemplo embutido). |
| `saida.txt` | Arquivo de saída dos pares (padrão: `saida.txt`). |

Exemplos:

    java AnalisadorLexicoAFD --cadeia "if a <= b then x := 1;"
    java AnalisadorLexicoAFD --cadeia "x := 24.40e-04;"
    echo "x := 1+2;" | java AnalisadorLexicoAFD -

O programa retorna **código de saída 0** quando não há erro léxico e **1** quando
há pelo menos um, o que facilita o uso em scripts.

## Testes

| Arquivo | Tipo | O que verifica |
|---|---|---|
| `01_valido_completo.txt` | válido | Todas as classes: declarações, literais, operadores, `if/else`, `while`. |
| `02_valido_case_insensitive.txt` | válido | Palavras reservadas em maiúsculas/minúsculas; `read`/`write` como identificadores. |
| `03_valido_maior_casamento.txt` | válido | `read`×`readx`, `while`×`while2`, `<=`×`<`×`<>`, `>=`×`>`. |
| `04_valido_literais_comentarios.txt` | válido | Caractere, cadeia (inclusive vazia), `' '` e comentários de bloco multilinha. |
| `05_erro_caractere_invalido.txt` | erro | `#`, `$`, `@`, `%` não pertencem à linguagem. |
| `06_erro_literais_mal_formados.txt` | erro | Cadeia sem fechamento, caractere mal formado e comentário não fechado. |
| `07_valido_expoentes.txt` | válido | Números reais com expoente `e`/`E` e sinal `+`/`-`. |

> `3.14.15` **não** é erro léxico: pela regra do maior casamento o léxico produz
> `3.14` (Número real), `.` (Fim) e `15` (Número inteiro).

## Observação sobre acentos

No console do Windows, rode `chcp 65001` (ou use os scripts `.bat`, que já o
fazem) para exibir corretamente os caracteres acentuados. Os arquivos são
gravados em **UTF-8**.
