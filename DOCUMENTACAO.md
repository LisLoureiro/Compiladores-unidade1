# Analisador Léxico da linguagem Mini_Pascal — Documentação

Trabalho Prático da I Unidade — disciplina de **Compiladores** (UESB/DCET).

**Integrantes:** _(preencher)_
**Data:** _(preencher)_

---

## 1. Introdução

Este trabalho implementa a **primeira etapa de um compilador**: a **análise
léxica**. O analisador recebe o texto-fonte de um programa escrito na linguagem
**Mini_Pascal** e o divide em uma sequência de **tokens**, isto é, pares
`<cadeia, token>` (o lexema reconhecido e a classe léxica a que ele pertence).

O reconhecimento é feito por um **Autômato Finito Determinístico (AFD)**,
construído manualmente e representado por uma **tabela de transições**. Optou-se
pelo AFD (e não por um AFND) porque, na análise léxica, o determinismo torna o
reconhecimento direto, eficiente (`O(n)` no tamanho da entrada) e de
implementação simples com a técnica de **maior casamento**.

## 2. Descrição da linguagem Mini_Pascal

### 2.1 Classes léxicas

| # | Classe | Exemplos |
|---|---|---|
| 1 | Palavra reservada | `program`, `var`, `integer`, `real`, `char`, `string`, `begin`, `end`, `if`, `then`, `else`, `while`, `do`, `for`, `to`, `repeat`, `until`, `const` |
| 2 | Identificador | `x`, `total`, `media_das_medias`, `Nota1` |
| 3 | Número inteiro | `0`, `13`, `2024` |
| 4 | Número real | `1.33`, `0.5`, `24.40e-04`, `2.5E+3` |
| 5 | Caractere | `'a'`, `'Z'`, `' '` |
| 6 | Cadeia | `"texto"`, `""` |
| 7 | Operador aritmético | `+` `-` `*` `/` `mod` |
| 8 | Operador relacional | `=` `>` `>=` `<` `<=` `<>` |
| 9 | Operador lógico | `and` `or` `not` |
| 10 | Símbolo especial | `(` `)` `,` `;` `:` |
| 11 | Atribuição | `:=` |
| 12 | Fim | `.` |

### 2.2 Expressões regulares das classes

- **Identificador / palavra reservada:** `(letra | _) (letra | dígito | _)*`
- **Número inteiro:** `dígito+`
- **Número real:** `dígito+ . dígito+ ( (e|E) (+|-)? dígito+ )?`
- **Caractere:** `' qualquer-caractere-exceto-quebra-de-linha '`
- **Cadeia:** `" (qualquer-caractere-exceto-quebra-de-linha e aspas)* "`
- **Operadores e símbolos:** como na tabela acima.

Após o reconhecimento de um identificador, uma **tabela de palavras reservadas**
(estrutura de dados própria) decide se a cadeia é palavra reservada, operador
escrito como palavra (`and`/`or`/`not`/`mod`) ou identificador comum.

### 2.3 Regra do maior casamento

Quando existem cadeias ambíguas, vence sempre a **mais longa** possível:

- `:` × `:=` — vence o `:=` quando seguido de `=`;
- `<` × `<=` × `<>` — vence a cadeia mais longa;
- `>` × `>=` — idem;
- `read` × `readx` — `readx` é um único identificador (maior casamento na classe
  identificador), e `read` isolado é identificador também;
- `3.14` × `3.14.15` — o número real consome o maior prefixo válido: `3.14`,
  depois `.` (Fim) e `15` (Inteiro).

## 3. Arquitetura e funcionamento

O programa está em um único arquivo, `AnalisadorLexicoAFD.java`, organizado em:

- **`AFD`** — guarda a tabela `delta[estado][símbolo]` e a classe de cada estado
  final. É construído por `construirAFD()`, que cria os 25 estados e as
  transições de todas as classes léxicas.
- **`TabelaPalavras`** — estrutura de dados das palavras reservadas, com as
  operações `inserir`, `contem`, `tamanho` e o construtor `de(...)`. A consulta
  ignora maiúsculas/minúsculas (`Locale.ROOT`).
- **`Token`** — o par `<cadeia, token>`: lexema, classe, linha e coluna; ou a
  mensagem de erro léxico.
- **`Lexer`** — o analisador. A função **`proximoToken()`** devolve, a cada
  chamada, um token (ou `null` no fim). Ela descarta espaços e comentários,
  executa o AFD e aplica a tabela de palavras reservadas.

### 3.1 Simulação do AFD

```
estado          <- q0
fimAceito       <- -1            (posição do último aceite)
classeAceita    <- null
para cada caractere c da entrada:
    se delta[estado][c] não existe: pare
    estado <- delta[estado][c]
    se estado é final:
        classeAceita <- classe(estado)
        fimAceito    <- posição após c
o token é entrada[inicio..fimAceito], da classe classeAceita
```

Se nenhum prefixo foi aceito, o caractere é reportado como **erro léxico**.

## 4. Instalação e uso

Os arquivos `.class` acompanham o trabalho (Java 8), então **não é preciso
recompilar** para executar:

    executar.bat exemplo.txt            (Windows)
    ./executar.sh exemplo.txt           (Linux/macOS)

O programa mostra a tabela de tokens no console e grava os pares em `saida.txt`
(pode-se indicar outro arquivo de saída como segundo argumento).

Para rodar todos os testes:

    executar-testes.bat                 (Windows)
    ./executar-testes.sh                (Linux/macOS)

Para ver a tabela de transições do AFD:

    executar.bat --afd

Para recompilar (requer JDK):

    compilar.bat  /  ./compilar.sh

O programa retorna 0 se não houver erro léxico e 1 caso haja.

## 5. O que foi feito

- AFD único com **tabela de transições** explícita e regra do maior casamento.
- As **12 classes léxicas** do enunciado, incluindo **Caractere** e **Cadeia**.
- **Tabela de palavras reservadas** como estrutura de dados dedicada.
- **Case-insensitive** (palavras reservadas reconhecidas em qualquer caixa).
- Descarte de **comentários `/* ... */`** e de caracteres não significativos.
- Saída **`<cadeia, token>`** no console **e** em arquivo `.txt` (um par por
  linha), **sem códigos numéricos**.
- **07 arquivos de teste** (5 válidos, 2 com erro) e scripts de execução.
- Documentação e código comentado.

## 6. O que não foi feito / limitações

- **Sem tabela de símbolos e sem análise sintática**: o trabalho é apenas a
  análise léxica.
- **Cadeia sem aspas internas nem escapes**: `"` termina a cadeia; não há
  representação para uma aspa dentro de uma cadeia.
- **Caractere é exatamente um caractere** entre apóstrofos; não há escapes
  (`'\n'`, `'\''`).
- **Comentário de bloco não aninha** (`/* /* */` fecha no primeiro `*/`).
- Um **número real exige ponto decimal** (`1.0e5` é real; `1e5` seria `1` e o
  identificador `e5`).
- Recuperação de erro: ao encontrar um caractere inválido, reporta-se o erro e o
  analisador continua a partir do caractere seguinte.

## 7. O que funciona / o que não funciona

**Funciona:** reconhecimento de todas as classes; maior casamento; tabela de
palavras reservadas; case-insensitivity; comentários; números com expoente;
caracteres e cadeias; detecção e contagem de erros léxicos; geração do arquivo
de saída.

**Não funciona (por não fazer parte do léxico):** validação semântica ou
sintática. Por exemplo, `3.14.15` **não** é erro léxico — o léxico produz
`3.14`, `.` e `15`; um eventual erro aí seria sintático.

## 8. Comentários sobre a linguagem

- A linguagem é **case-insensitive**; todas as comparações de palavras
  reservadas são feitas em caixa alta.
- `read`, `write` e `writeln` **não** constam da tabela de palavras reservadas do
  enunciado e, portanto, são classificados como **Identificador**.
- Os operadores `and`, `or`, `not` (lógicos) e `mod` (aritmético) são
  reconhecidos inicialmente como identificadores e reclassificados após consulta
  à tabela.
- **Fidelidade ao enunciado:** a lista de palavras reservadas foi transcrita
  exatamente como no enunciado, o que inclui as grafias `DOWTO`, `NIT` e `PROC`.
  Caso o enunciado tenha pretendido `DOWNTO`, `NOT` e `PROCEDURE`, basta editar o
  vetor `PALAVRAS_RESERVADAS` (o comportamento do analisador não muda em nada
  mais).

## 9. Descrição dos testes

| Arquivo | Tipo | Cobre |
|---|---|---|
| `01_valido_completo.txt` | válido | Todas as classes; `if/else`, `while`, literais, operadores. |
| `02_valido_case_insensitive.txt` | válido | Palavras reservadas em maiúsculas; `read`/`write` como identificadores. |
| `03_valido_maior_casamento.txt` | válido | Ambiguidades `read`/`readx`, `while`/`while2`, `<=`/`<`/`<>`, `>=`/`>`. |
| `04_valido_literais_comentarios.txt` | válido | Caractere, cadeia vazia, `' '` e comentários multilinha. |
| `05_erro_caractere_invalido.txt` | erro | `#`, `$`, `@`, `%` fora da linguagem. |
| `06_erro_literais_mal_formados.txt` | erro | Cadeia/comentário sem fechamento e caractere mal formado. |
| `07_valido_expoentes.txt` | válido | Reais com expoente `e`/`E` e sinal. |

Resultado esperado: os testes `valido` produzem **saída com 0 erros**; os testes
`erro` produzem **código de saída 1** e mensagens indicando linha, coluna e o
motivo.

## 10. Conclusão

O analisador atende aos requisitos do trabalho: usa um **AFD** com tabela de
transições, produz **pares `<cadeia, token>`** em arquivo e console, mantém a
**tabela de palavras reservadas** como estrutura de dados e vem acompanhado de
**testes** e **documentação**. As principais limitações (sem escapes em literais,
sem aninhamento de comentários, real exigindo ponto decimal) estão documentadas.

## Referências

- AHO, A. V.; LAM, M. S.; SETHI, R.; ULLMAN, J. D. *Compiladores: princípios,
  técnicas e ferramentas*. 2. ed. Pearson, 2008.
- MENEZES, P. B. *Linguagens Formais e Autômatos*. 6. ed. Bookman, 2011.
- Enunciado do Trabalho Prático da I Unidade — Compiladores (UESB/DCET).
