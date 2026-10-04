# Analisador Léxico por AFND — Trabalho Prático da I Unidade

Trabalho Prático da I Unidade da disciplina de **Compiladores** (UESB/DCET).
Implementa, em Java, um **analisador léxico** para a linguagem definida no
enunciado, construído como um **único Autômato Finito Não Determinístico (AFND)
com transições-ε**.

## Como o léxico funciona

O AFND tem um estado inicial `q0` com transições-ε para o sub-autômato de
**cada** uma das 10 classes léxicas. A simulação percorre o texto mantendo o
**conjunto de estados ativos** (fecho-ε + movimento) e aplica a regra do
**maior casamento** (*maximal munch*): o token reconhecido é o maior prefixo
aceito pelo AFND. Empates são desfeitos pela ordem de prioridade declarada em
[`TipoToken.java`](TipoToken.java).

### Classes léxicas (ordem = prioridade de desempate)

| # | Classe | Exemplos |
|---|---|---|
| 1 | Palavra Reservada | `program`, `var`, `if`, `while`, `read` |
| 5 | Operador Aritmético | `+` `-` `*` `/` `%` `mod` |
| 7 | Operador Lógico | `and` `or` `not` `&&` `||` |
| 6 | Operador Relacional | `>` `>=` `<` `<=` `<>` `==` `!=` |
| 8 | Símbolo Especial | `(` `)` `,` `;` `:` |
| 9 | Atribuição | `:=` `=` |
| 4 | Número Real | `1.33`, `24.40e-04` |
| 3 | Número Inteiro | `1`, `13` |
| 10 | Fim | `.` |
| 2 | Identificador | `x`, `variavel`, `i`, `var2` |

Empates previstos: palavra reservada × identificador (vence a palavra
reservada); operadores-palavra `and`/`or`/`not`/`mod` × identificador (vence o
operador). Os pares `=` × `==`, `:` × `:=`, `<` × `<=` × `<>`, `&` × `&&`,
`|` × `||` são resolvidos pelo **maior casamento** (vence a cadeia mais longa).

Observações desta linguagem:

- `=` é **atribuição**; `==` é **igualdade relacional** e `!=` é **diferença**
  (estilo C), convivendo com `<>` da linguagem original.
- `&&` e `||` são operadores lógicos; um `&` ou `|` isolado **não** é
  reconhecido (erro léxico), pois só existem as formas compostas.
- Comentários (`/* ... */`) e strings **não** fazem parte do escopo.

## Arquivos

| Arquivo | Descrição |
|---|---|
| `TipoToken.java` | Enum das classes léxicas, na ordem de prioridade. |
| `Token.java` | Representação de um token (tipo, lexema, linha, coluna). |
| `AnalisadorLexicoAFND.java` | O AFND e a simulação (maior casamento) + CLI. |
| `exemplo.txt` | Programa de exemplo usando todas as classes. |
| `testes/` | 06 arquivos de teste (válidos e com erros léxicos). |

## Como executar

Os arquivos `.class` já vêm compilados para **Java 8** e rodam direto:

    executar.bat exemplo.txt            (Windows)
    ./executar.sh exemplo.txt           (Linux/macOS)

Analisar um teste:

    executar.bat testes\02_valido_sem_espacos.txt
    ./executar.sh testes/02_valido_sem_espacos.txt

Rodar todos os testes:

    executar-testes.bat                  (Windows)
    ./executar-testes.sh                 (Linux/macOS)

Imprimir a tabela de transições do AFND:

    executar.bat --afnd

Sem informar arquivo, o programa analisa o exemplo embutido no código.

Para recompilar (requer JDK):

    compilar.bat                         (Windows)
    ./compilar.sh                        (Linux/macOS)

## Opções de linha de comando

    java AnalisadorLexicoAFND [arquivo] [--afnd]

| Opção | Efeito |
|---|---|
| `--afnd` | Imprime a tabela de transições e os estados finais do AFND. |
| *(arquivo)* | Analisa o arquivo informado. |

O programa retorna **código de saída 0** quando não há erro léxico e **1** quando
há pelo menos um, o que facilita o uso em scripts.

## Testes

| Arquivo | Tipo | O que verifica |
|---|---|---|
| `01_valido_completo.txt` | válido | Todas as classes: palavras, números, operadores, atribuição, `mod`. |
| `02_valido_sem_espacos.txt` | válido | Regressão do `+-`: `x:=1+2;y:=10-3;` (o `+`/`-` são aritméticos, não sinal). |
| `03_valido_maior_casamento.txt` | válido | `read`×`readx`, `while`×`while2`, `<=`×`<`, `<>`×`<`. |
| `04_erro_caractere_invalido.txt` | erro | Caractere `#` não pertence à linguagem. |
| `05_erro_caracteres_invalidos.txt` | erro | Vários caracteres inválidos (`@`, `$`) — reporta cada erro e conta corretamente. |
| `06_valido_operadores_novos.txt` | válido | Novos símbolos: `%`, `==`, `!=`, `&&`, `||`, `=` (atribuição), convivendo com `<>`, `and`, `<=`. |

> `3.14.15` **não** é erro léxico: pela regra do maior casamento o léxico produz
> `3.14` (Número Real), `.` (Fim) e `15` (Número Inteiro). Trata-se de um erro
> **sintático**, não léxico.

## Observação sobre acentos

No console do Windows, rode `chcp 65001` (ou use os scripts `.bat`, que já o
fazem) para exibir corretamente os caracteres acentuados.
