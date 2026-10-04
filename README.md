# Analisador Léxico por AFND — Trabalho Prático da I Unidade

Trabalho Prático da I Unidade da disciplina de **Compiladores** (UESB/DCET).
Implementa, em Java, um **analisador léxico** para a linguagem definida no
enunciado, construído com **um AFND por classe léxica** (sem transições-ε).

## Como o léxico funciona

Há um AFND independente para cada classe (`criarAFNDIdentificador`,
`criarAFNDReal`, `criarAFNDRelacional`, ...). Cada AFND é simulado pelo método
`AFND.reconhecer`, que mantém o **conjunto de estados ativos** e verifica se
algum estado final foi atingido.

O scanner percorre o texto e recorta o **maior lexema possível** (*maximal
munch*), usando o AFND correspondente para validá-lo. Os pares ambíguos são
resolvidos pelo maior casamento: `:` × `:=` e `<` × `<=` × `<>` (vence sempre a
cadeia mais longa). A prioridade entre palavra reservada, operador-palavra e
identificador é decidida pela ordem das verificações. O `=` é aceito pelo
operador relacional e pelo símbolo especial; vence o **relacional**, testado
primeiro.

### Classes léxicas

| # | Classe | Exemplos |
|---|---|---|
| 1 | Palavra Reservada | `program`, `var`, `if`, `while`, `read` |
| 2 | Identificador | `x`, `variavel`, `i`, `var2` |
| 3 | Número Inteiro | `1`, `13` |
| 4 | Número Real | `1.33`, `24.40e-04` |
| 5 | Operador Aritmético | `+` `-` `*` `/` `mod` |
| 6 | Operador Relacional | `=` `>` `>=` `<` `<=` `<>` |
| 7 | Operador Lógico | `and` `or` `not` |
| 8 | Símbolo Especial | `=` `(` `)` `,` `;` `:` |
| 9 | Atribuição | `:=` |
| 10 | Fim | `.` |

Observação: comentários (`/* ... */`) e strings **não** fazem parte do escopo.

## Arquivos

| Arquivo | Descrição |
|---|---|
| `AnalisadorLexicoAFND.java` | Os AFNDs (um por classe), o scanner e a CLI. |
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

Imprimir a tabela de transições de cada AFND:

    executar.bat --afnd

Sem informar arquivo, o programa analisa o exemplo embutido no código.

Para recompilar (requer JDK):

    compilar.bat                         (Windows)
    ./compilar.sh                        (Linux/macOS)

## Opções de linha de comando

    java AnalisadorLexicoAFND [arquivo] [--afnd]

| Opção | Efeito |
|---|---|
| `--afnd` | Imprime a tabela de transições e os estados finais de cada AFND. |
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
| `06_valido_mod_logicos.txt` | válido | `mod`, `=`, `<>`, `and`, `or`, `not` com `>=`, `<=`, `:=`. |

> `3.14.15` **não** é erro léxico: pela regra do maior casamento o léxico produz
> `3.14` (Número Real), `.` (Fim) e `15` (Número Inteiro). Trata-se de um erro
> **sintático**, não léxico.

## Observação sobre acentos

No console do Windows, rode `chcp 65001` (ou use os scripts `.bat`, que já o
fazem) para exibir corretamente os caracteres acentuados.
