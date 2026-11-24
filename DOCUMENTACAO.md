# Implementação de um Analisador Sintático SLR(1) para a Linguagem Sigma

**Universidade Estadual do Sudoeste da Bahia – UESB**
**Departamento de Ciências Exatas – DCET**
**Disciplina:** Compiladores — **Professores:** José Carlos Martins Oliveira
**Curso:** Ciência da Computação — **Ano:** 2026 — **Período:** II
**Trabalho Prático da II Unidade**

**Integrantes da dupla:** Lis Loureiro e Laiana

---

## Resumo

Este trabalho descreve a especificação e a implementação de um analisador
sintático baseado na técnica **SLR(1)** para a linguagem **Sigma**, apresentada
no enunciado da II Unidade. O analisador léxico foi reaproveitado e corrigido a
partir do trabalho da I Unidade, tendo sido ajustado o conjunto de palavras
reservadas e de operadores e acrescentado o token de cadeia de caracteres
(*string*). A tabela de análise SLR(1) é construída programaticamente a partir
das produções da gramática (conjuntos FIRST/FOLLOW e coleção canônica de itens
LR(0)). Como a gramática do enunciado é ambígua, os conflitos da tabela são
resolvidos por precedência e associatividade, no mesmo estilo da ferramenta
Yacc, e cada conflito é documentado. Ao final, são apresentados dez arquivos de
teste, comentários sobre a linguagem e uma avaliação crítica do trabalho.

**Palavras-chave:** Compiladores. Análise Sintática. SLR(1). Linguagem Sigma.

---

## 1. Introdução

A análise sintática é a etapa da compilação responsável por verificar se a
sequência de tokens produzida pelo analisador léxico pertence à linguagem
definida pela gramática. Este trabalho implementa um **analisador sintático
SLR(1)** (*Simple LR with 1 token of lookahead*) para a linguagem **Sigma**.

O analisador léxico do primeiro trabalho foi reaproveitado, conforme
recomendação do enunciado, com as correções necessárias ao novo conjunto de
tokens. A gramática foi mantida **fiel ao enunciado**; as adaptações
necessárias para que ela seja processável por uma tabela SLR(1) deram-se apenas
no *tratamento dos conflitos* (seção 6), e não na reescrita das produções.

Todo o código está em **Java** e é compatível com **Java 8**. O projeto pode ser
executado diretamente (os arquivos `.class` já vêm compilados) ou via Docker,
sem a necessidade de um ambiente de desenvolvimento completo.

## 2. A Linguagem Sigma

### 2.1 Conjunto de tokens (símbolos terminais e definições regulares)

| Classe léxica | Definição regular / observação | Exemplos |
|---|---|---|
| `program`, `var`, `integer`, `boolean`, `begin`, `end`, `if`, `then`, `else`, `while`, `do`, `read`, `readln`, `write`, `writeln` | Palavras reservadas | `program`, `while` |
| `id` | `letra (letra | dígito)*` | `x`, `soma2` |
| `num` | `dígito+` (número inteiro) | `0`, `42` |
| `str` | `"` *qualquer caractere exceto aspa e quebra de linha* `"` | `"Soma = "` |
| `:=` | atribuição | `x := 1` |
| `+ - * /` | operadores aritméticos | `a + b` |
| `= < <= > >= <>` | operadores relacionais | `a <= b` |
| `( ) , ; :` | símbolos especiais | `read(x, y)` |
| `.` | fim de programa | `end.` |

O analisador léxico aplica a regra do **maior casamento** (*maximal munch*),
que resolve naturalmente os casos `read`/`readln`, `write`/`writeln`,
`:`/`:=` e `<`/`<=`/`<>`. As palavras reservadas são reconhecidas sem
diferenciar maiúsculas de minúsculas.

### 2.2 Gramática em EBNF

Conforme o enunciado, a gramática em EBNF possui 14 não-terminais:

```
<programa>       ::= program id ; [<declarações>] begin <lista comandos> end.
<declarações>    ::= var id {,id} : <tipo>; {id {,id} : <tipo>;}
<tipo>           ::= integer | boolean
<lista comandos> ::= <comando> ; {<comando> ;}
<comando>        ::= <atribuição> | <leitura> | <escrita> | <composto>
                   | <condicional> | <repetição>
<atribuição>     ::= id := <expr>
<leitura>        ::= read (id {,id}) | readln [(id {,id})]
<escrita>        ::= write (<stringvar> {,<stringvar>})
                   | writeln [(<stringvar> {,<stringvar>})]
<composto>       ::= begin <lista comandos> end
<condicional>    ::= if <exprboolean> then <comando> [else <comando>]
<repetição>      ::= while <exprboolean> do <comando>
<expr>           ::= <expr> + <expr> | <expr> - <expr> | <expr> * <expr>
                   | <expr> / <expr> | - <expr> | (<expr>) | id | num
<exprboolean>    ::= <expr> < <expr>  | <expr> <= <expr> | <expr> > <expr>
                   | <expr> >= <expr> | <expr> = <expr>  | <expr> <> <expr> | id
<stringvar>      ::= str | <expr>
```

### 2.3 Gramática em notação formal (GLC)

A gramática G = ⟨N, T, P, S⟩ em notação formal possui 17 não-terminais. As
produções são exatamente as do enunciado:

```
S  -> program id ; D begin L end .   |   program id ; begin L end .
D  -> var V
V  -> I : T ; V   |   I : T ;
T  -> integer   |   boolean
I  -> id   |   id , I
L  -> C ;   |   C ; L
C  -> A   |   R   |   W   |   M   |   N   |   P
A  -> id := E
R  -> read ( I )   |   readln   |   readln ( I )
W  -> write ( F )   |   writeln   |   writeln ( F )
F  -> G   |   G , F
G  -> str   |   E
M  -> begin L end
N  -> if B then C   |   if B then C else C
P  -> while B do C
E  -> E + E | E - E | E * E | E / E | - E | ( E ) | id | num
B  -> E < E | E <= E | E > E | E >= E | E = E | E <> E | id
```

A correspondência com os não-terminais do EBNF é a mesma do enunciado
(S = `<programa>`, D = `<declarações>`, V = lista de variáveis, T = `<tipo>`,
I = lista de ids, L = `<lista comandos>`, C = `<comando>`, A = `<atribuição>`,
R = `<leitura>`, W = `<escrita>`, F = lista de `<stringvar>`, G = `<stringvar>`,
M = `<composto>`, N = `<condicional>`, P = `<repetição>`, E = `<expr>`,
B = `<exprboolean>`). Na implementação, `S` foi aumentada com uma produção
extra `S' -> S`, usada apenas para detectar a aceitação.

## 3. Arquitetura da implementação

O programa foi dividido em sete classes, todas em Java 8:

| Arquivo | Responsabilidade |
|---|---|
| `TipoToken.java` | Enumeração das classes léxicas. A ordem também define a prioridade de desempate do léxico. |
| `Token.java` | Estrutura de um token do léxico (tipo, lexema, linha, coluna, eventual mensagem de erro). |
| `AnalisadorLexico.java` | AFND e sua simulação (maior casamento). Produz a lista de tokens. |
| `TokenSintatico.java` | Token neutro consumido pelo parser (símbolo terminal, lexema, posição). Não conhece o léxico. |
| `AdaptadorLexico.java` | Única ponte entre os tokens do léxico e os terminais da gramática. |
| `Gramatica.java` | Produções, FIRST/FOLLOW, itens LR(0), tabela SLR(1) e resolução de conflitos. |
| `AnalisadorSintatico.java` | Algoritmo de análise dirigido por tabela e interface de linha de comando. |

### 3.1 Analisador léxico

Mantém-se a abordagem do trabalho anterior: um **Autômato Finito Não
Determinístico (AFND)** com um estado inicial que possui transições-ε para o
sub-autômato de cada classe léxica. A cada posição do texto simula-se o
conjunto de estados ativos (fecho-ε + movimento) e registra-se o maior prefixo
aceito. O AFND contempla **todas as classes léxicas** da especificação da I
Unidade: palavra reservada, identificador, número inteiro, número real
(`dígito+ '.' dígito+ (('e'|'E')('+'|'-')? dígito+)?`), operador aritmético
(`+ - * / mod`), relacional (`= >= > < <= <>`), lógico (`and`, `or`, `not`),
símbolo especial (`( ) , ; :`), atribuição (`:=`) e fim (`.`). Em relação ao
projeto original, acrescentaram-se as palavras reservadas de Sigma, o token
`str`, os números reais e os operadores `mod/and/or/not`; o operador `=` passou
a ser relacional e o marcador de comentário `#` do Mini_Pascal foi removido,
pois não pertence à linguagem Sigma.

A gramática da II Unidade **não referencia** números reais, operadores lógicos
nem o operador `mod`: o léxico os reconhece (cumprindo a especificação da I
Unidade), mas o parser só consome os terminais que aparecem nas produções. Essa
diferença é proposital — o léxico é um componente autônomo, reaproveitado pela
análise sintática.

### 3.2 Construção da tabela SLR(1)

A tabela é gerada automaticamente pela classe `Gramatica`, na seguinte ordem:

1. **FIRST e FOLLOW** de todos os não-terminais (ponto fixo).
2. **Coleção canônica de itens LR(0)**, obtida por fecho (*closure*) e
   transição (*goto*), partindo do fecho do item `S' -> . S`. Para a gramática
   de Sigma resultam **101 estados**.
3. **Tabela ACTION/GOTO**: cada item `[A -> α . a β]` (com `a` terminal) gera
   uma ação *empilhar*; cada item `[A -> α .]` gera ações *reduzir* para cada
   terminal em FOLLOW(A); o item `[S' -> S .]` gera *aceitar* em `$`.
4. **Resolução de conflitos** (seção 6).

### 3.3 Análise dirigida por tabela

O parser usa a pilha de estados clássica. Para cada passo consulta
`ACTION[topo, a]`:

* **empilhar s** → empilha o estado `s` e avança na entrada;
* **reduzir `A -> α`** → desempilha `|α|` estados e empilha `GOTO[novo topo, A]`;
* **aceitar** → o programa é válido;
* **erro** → emite mensagem com linha, coluna, lexema ofensor e a lista de
  terminais esperados naquela situação.

### 3.4 Independência entre as fases

A análise é dividida em duas fases executáveis separadamente:

1. **Fase 1 — léxica:** transforma o texto-fonte na lista de tokens (`Token`).
2. **Fase 2 — sintática:** consome apenas `TokenSintatico`.

O núcleo do parser (`analisar`) **não conhece o analisador léxico**: ele recebe
uma `List<TokenSintatico>`, na qual cada elemento já traz o símbolo terminal da
gramática, o lexema e a posição. A conversão de `Token` (léxico) para
`TokenSintatico` (sintático) está isolada em `AdaptadorLexico`, a única classe
que referencia os dois mundos. Assim, trocar o léxico não exige alterar o
parser, e o parser poderia ser alimentado por qualquer outra fonte de tokens.

O `main` de `AnalisadorSintatico` funciona como raiz de composição: na fase
`sintatico` ele monta a sequência de tokens internamente (léxico + adaptador) e
a entrega ao parser; com `--fase lexico` executa apenas a primeira fase.

## 4. Instalação e utilização

### 4.1 Requisitos

* **Para executar:** apenas um **JRE 8 ou superior** (os arquivos `.class` já
  vêm compilados com bytecode Java 8); **ou** Docker.
* **Para recompilar (opcional):** um **JDK** (comando `javac`).

### 4.2 Execução sem Docker

Windows:

```
executar.bat exemplo.txt
executar.bat testes\01_valido_basico.txt
executar-testes.bat
```

Linux/macOS:

```
./executar.sh exemplo.txt
./executar.sh testes/01_valido_basico.txt
./executar-testes.sh
```

Recompilação (requer JDK): `compilar.bat` (Windows) ou `./compilar.sh`.

### 4.3 Execução com Docker

```
docker build -t analisador-sigma .
docker run --rm analisador-sigma
docker run --rm analisador-sigma exemplo.txt
docker run --rm analisador-sigma testes/01_valido_basico.txt --tokens
```

### 4.4 Linha de comando

```
java AnalisadorSintatico [arquivo] [opções]
```

| Opção | Efeito |
|---|---|
| `--fase lexico\|sintatico\|ambas` | Executa apenas a fase indicada (padrão: `ambas`). |
| `--tokens` | Imprime a tabela de tokens reconhecidos. |
| `--gramatica` | Imprime produções, FIRST e FOLLOW. |
| `--tabela` | Imprime a tabela SLR(1) e os conflitos resolvidos. |
| `--passos` | Imprime o passo a passo da análise. |
| `--automato` | Imprime a tabela de transições do AFND do léxico. |
| `-h`, `--help` | Mostra a ajuda. |

O programa retorna **0** para programas válidos e **1** para programas com erro
léxico ou sintático. No console do Windows, use `chcp 65001` (os scripts `.bat`
já o fazem) para exibir os acentos corretamente.

Exemplo de saída:

```
Programa sintaticamente VÁLIDO.
Tokens analisados: 83 | Reduções: 71 | Estados da tabela: 101
```

### 4.5 Interface web por fases

Além da linha de comando, há uma interface web local (`interface.html`,
servida por `servidor.py`, que usa apenas a biblioteca padrão do Python 3 e
executa o analisador Java):

```
python servidor.py            # abre http://localhost:8000
python servidor.py --porta 9000 --no-browser
```

A interface trata as duas fases como fases de um jogo (tema Super Mario, com a
identidade "Zecas Bros"): a abertura mostra a tela de título com o professor
Zeca em um cenário de plataforma; um clique leva ao mapa do mundo no estilo
*Super Mario World* (ilhas, mar, ponte, castelo e fases como nós numerados), e
cada fase é uma etapa do analisador, com uma introdução própria. A tela de
análise é ambientada como a sala de aula do Zeca — quadro verde, piso de
madeira, estante e mesa do professor. O botão **ANALISAR** executa somente a
fase selecionada, e a barra de carregamento mostra o Zeca caminhando enquanto o
analisador roda.

* **Fase 1 — análise léxica:** envia `"fase": "lexico"` e recebe apenas a
  sequência de tokens e os erros léxicos.
* **Fase 2 — análise sintática:** envia `"fase": "sintatico"` e recebe os
  passos do SLR(1) e o veredito.

O servidor expõe `POST /api/analisar` (campos `codigo`, `opcoes`, `fase`),
`GET /api/testes` e `POST /api/testes/executar` (roda a suíte no servidor).

## 5. O que foi feito e o que não foi feito

### 5.1 O que foi feito

* Analisador léxico afim à linguagem Sigma (AFND + maior casamento), com
  detecção de erros léxicos (caractere inválido e cadeia não terminada).
* Geração automática dos conjuntos FIRST/FOLLOW e da coleção canônica de itens
  LR(0).
* Construção e uso da tabela **SLR(1)** (101 estados) e do algoritmo de análise
  por pilha.
* Resolução dos conflitos da tabela por precedência/associatividade (estilo
  Yacc), com relatório de todos os conflitos (opção `--tabela`).
* Mensagens de erro sintático com linha, coluna, lexema ofensor e terminais
  esperados.
* Separação em **duas fases independentes** (`--fase lexico|sintatico|ambas`):
  o parser consome apenas `TokenSintatico`, e a conversão do léxico fica
  isolada em `AdaptadorLexico`.
* Interface web local por fases (`interface.html` + `servidor.py`), com uma
  aba/fase para a análise léxica e outra para a sintática.
* Dez arquivos de teste (válidos e com erros), scripts de execução e imagem
  Docker.
* Arquivos `.class` pré-compilados (bytecode Java 8), executáveis sem
  recompilação.

### 5.2 O que não foi feito

* **Análise semântica / tabela de símbolos:** não há verificação de tipos nem
  controle de declaração/uso de identificadores. O trabalho é estritamente
  léxico-sintático.
* **Geração de código** ou interpretação do programa.
* **Recuperação de erros:** a análise para no primeiro erro sintático.
* **Comentários na linguagem-fonte:** a linguagem Sigma definida não possui
  comentários (ver seção 7).

### 5.3 O que está funcionando e o que não está funcionando

**Está funcionando:** análise léxica completa de Sigma; construção da tabela
SLR(1) e análise de todos os construtores da linguagem (declarações, comandos
compostos, atribuição, leitura/escrita, condicional com `else`, repetição e
expressões com precedência e parênteses); detecção de erros léxicos e
sintáticos com linha, coluna, lexema e terminais esperados; as opções
`--fase lexico|sintatico|ambas`, `--tokens`, `--gramatica`, `--tabela`,
`--passos` e `--automato`; a interface web por fases; execução via JRE 8
(arquivos `.class` prontos) e via Docker; os dez arquivos de teste foram
validados.

**Não está funcionando / não implementado:** recuperação de erros (a análise
para no primeiro erro); verificação semântica (tipos, declaração/uso de
variáveis); tratamento de comentários (a linguagem não os define); e as
construções vedadas pela própria gramática (atribuição de expressão booleana,
operadores lógicos, comparações encadeadas), descritas na seção 7.

## 6. Conflitos da gramática SLR(1) e suas resoluções

A gramática do enunciado é **ambígua** e, portanto, sua tabela SLR(1) apresenta
conflitos. Optou-se por **manter as produções fiéis ao enunciado** e resolver os
conflitos com regras de precedência/associatividade (como faz a ferramenta
Yacc). A ferramenta relata **23 conflitos** (21 *empilhar/reduzir* e 2
*reduzir/reduzir*), descritos a seguir.

### 6.1 Precedência declarada

| Nível (menor → maior) | Operadores | Associatividade |
|---|---|---|
| 1 | `<` `<=` `>` `>=` `=` `<>` | não associativos |
| 2 | `+` `-` (binários) | à esquerda |
| 3 | `*` `/` | à esquerda |
| 4 | `-` (unário, pseudo-terminal `UMINUS`) | à direita |

A precedência de uma produção é dada pelo terminal mais à direita de seu lado
direito; a produção do menos unário `E -> - E` recebe explicitamente a
precedência `UMINUS`.

### 6.2 Conflitos empilhar/reduzir (21)

* **Expressões (20 conflitos):** decorrem de `E -> E + E | E - E | E * E |
  E / E` e do menos unário. Exemplo: no estado que contém `E -> E + E .` com
  lookahead `*`, empilhar (continuar a multiplicação) vs. reduzir (fechar a
  soma). A resolução segue a precedência: como `*` tem precedência maior que
  `+`, **empilha-se** (ou seja, `a + b * c` agrupa `b * c` antes). Entre
  operadores de mesma precedência, a associatividade à esquerda leva a
  **reduzir** (ou seja, `a + b + c` agrupa da esquerda para a direita) e o
  menos unário, por ter a maior precedência, sempre **reduz** antes de uma
  operação binária (`- a + b` = `(-a) + b`).
* **"Dangling else" (1 conflito):** no estado com `N -> if B then C .` e
  `N -> if B then C . else C`, o lookahead `else` gera conflito entre reduzir
  (`N -> if B then C`) e empilhar. A resolução **empilha**, fazendo o `else`
  associar-se ao `if` mais interno — a convenção usual. Assim,
  `if a then if b then c else d` é interpretado como
  `if a then (if b then c else d)`.

### 6.3 Conflitos reduzir/reduzir (2)

Ocorrem no estado que, após ler um `id` em um contexto booleano (após `if` ou
`while`), contém simultaneamente os itens `E -> id .` e `B -> id .`. Os dois
itens têm lookahead em `{then, do}` (`FOLLOW(B) = {then, do}`), gerando conflito
em `then` e em `do`.

A resolução adotada é uma **heurística de especificidade**: entre as reduções
candidatas, vence aquela cujo não-terminal tem o **menor conjunto FOLLOW que
contém o lookahead**; empates são desfeitos pelo menor índice de produção.
Como `FOLLOW(B) = {then, do}` é menor que `FOLLOW(E)`, escolhe-se `B -> id`
nesses dois lookaheads. Na prática:

* `if flag then ...` e `while flag do ...` → o `id` é reduzido como **condição
  booleana** (`B -> id`), pois o lookahead (`then`/`do`) só é válido para `B`;
* `if x < 3 then ...` → no lookahead `<`, apenas `E -> id` se aplica, e o `id`
  é reduzido como **expressão** para, em seguida, completar `B -> E < E`.

Essa escolha preserva o uso de variáveis booleanas como condição e não impede
as comparações.

### 6.4 Observação sobre o método

Estritamente, uma gramática ambígua **não é SLR(1)** e nenhuma tabela SLR(1)
para ela seria livre de conflitos. Este trabalho constrói a tabela pelo método
SLR(1) (itens LR(0) + FOLLOW) e resolve os conflitos segundo regras explícitas,
documentadas e reproduzíveis — abordagem equivalente à adotada por geradores de
analisadores como o Yacc. A opção `--tabela` imprime todos os conflitos e a
decisão tomada em cada um, tornando a resolução auditável.

## 7. Comentários sobre a linguagem e suas limitações

Ao longo da implementação, percebeu-se que a linguagem Sigma, **como
especificada**, possui algumas limitações e pontos ambíguos. Nada foi alterado
nas produções; as observações abaixo são consequências diretas da gramática do
enunciado e estão documentadas para transparência.

* **Não há como atribuir uma expressão booleana.** A produção é `A -> id := E`,
  e `E` cobre apenas expressões aritméticas; comparações só existem em `B`.
  Logo, `ok := (x < 10)` **não** é sintaticamente válido.
* **`B -> E rel E | id`:** uma condição pode ser um `id` isolado ou uma
  comparação entre duas expressões aritméticas. Uma expressão booleana que não
  seja um `id` sozinho (por exemplo `if a + b then`) **não** é válida. Também
  não há operadores lógicos (`and`, `or`, `not`) nas produções: o léxico os
  reconhece (classe "Operador Lógico"), mas a gramática não os utiliza.
* **Comparações encadeadas** (`a < b < c`) não são válidas, pois o resultado de
  `B` não é, ele próprio, operando de outra comparação.
* **Estruturas de controle terminam com `;`.** Cada comando é seguido de `;`
  (`L -> C ;`), inclusive o último antes de `end`.
* **Números:** a gramática aceita apenas inteiros (`num = dígito+`). O léxico
  reconhece números reais (`1.33`, `24.40e-04`) como a classe "Número Real",
  mas, como a gramática não possui produção que os aceite, um literal real é
  rejeitado na análise sintática. O tipo `integer` convive com `boolean` sem
  verificação semântica.
* **Identificadores** seguem `letra (letra | dígito)*`: **não** é permitido
  sublinhado (`_`), conforme o léxico do 1º trabalho.
* **Cadeias de caracteres** (`str`) são definidas neste trabalho como texto
  entre aspas duplas, em uma única linha e sem sequências de escape.
* **`readln`/`writeln`** podem ser usados com ou sem parênteses, conforme o
  EBNF; `read`/`write` exigem parênteses.
* **Não há comentários** na linguagem.
* **Sem análise semântica:** declaração e uso de variáveis e compatibilidade de
  tipos não são verificados.

## 8. Avaliação da dupla

A dupla considera que o trabalho atingiu os objetivos propostos. A principal
dificuldade foi perceber que a gramática fornecida **não é livre de conflitos
SLR(1)** — por causa das expressões sem precedência e do “dangling else” — e
definir uma estratégia de resolução que preservasse as produções originais e,
ao mesmo tempo, reconhecesse corretamente os programas esperados (expressões,
condições com variáveis booleanas e `else` associado ao `if` mais interno).

Do lado positivo, destacam-se: o reaproveitamento do analisador léxico do 1º
trabalho, a geração automática da tabela SLR(1) (o que facilitou a conferência
dos 101 estados e dos conflitos) e as mensagens de erro com linha, coluna e
terminais esperados. Como limitação, o analisador não faz recuperação de erros
(interrompe no primeiro) e não realiza análise semântica.

Em retrospecto, a dupla avalia que o maior aprendizado foi implementar a tabela
SLR(1) de fato — calculando FIRST/FOLLOW, a coleção canônica de itens LR(0) e as
ações — em vez de recorrer a uma ferramenta pronta. Isso tornou visível por que
a gramática do enunciado, sendo ambígua, exige decisões explícitas de
desambiguação e como essas decisões afetam o comportamento do analisador. O
trabalho também mostrou o valor de separar o léxico do parser: a interface
`TokenSintatico`/`AdaptadorLexico` permitiu evoluir o léxico (números reais,
`mod`, operadores lógicos) sem tocar no núcleo sintático. Como pontos a
melhorar, a dupla aponta a ausência de recuperação de erros e a dependência de
uma heurística no desempate do conflito reduzir/reduzir; uma alternativa seria
reescrever a expressão em níveis de precedência, tornando a gramática não
ambígua.

## 9. Conjunto de testes

Pasta `testes/` (o campo “Resultado” refere-se ao esperado):

| Arquivo | Objetivo | Resultado |
|---|---|---|
| `01_valido_basico.txt` | Atribuições e expressões com precedência, parênteses e menos unário. | Válido |
| `02_valido_condicional_repeticao.txt` | `if`/`else` aninhados (dangling else), `while`, condição com `id`. | Válido |
| `03_valido_declaracoes_io.txt` | Várias declarações; `read`/`readln`; `write`/`writeln` com `str` e expressões. | Válido |
| `04_valido_blocos.txt` | Comandos compostos (`begin ... end`) aninhados. | Válido |
| `05_erro_lexico.txt` | Caractere inválido (`@`) e cadeia não terminada. | Erro léxico |
| `06_erro_sintatico.txt` | Falta de `;` entre comandos. | Erro sintático |
| `07_erro_fim_inesperado.txt` | Ausência de `end.` (fim de arquivo inesperado). | Erro sintático |
| `08_valido_sem_declaracoes.txt` | Programa sem bloco de declarações. | Válido |
| `09_valido_operadores_relacionais.txt` | Condições com os seis operadores relacionais (`= <> >= <= > <`). | Válido |
| `10_erro_sintatico_mod.txt` | Uso de `mod` (reconhecido pelo léxico, mas fora da GLC) — rejeição sintática. | Erro sintático |

Para executar todos: `executar-testes.bat` (Windows) ou `./executar-testes.sh`.
Os scripts deduzem o veredito esperado do nome do arquivo (`valido`,
`erro_lexico`, `erro_sintatico`), comparam com a saída do analisador e exibem um
resumo final.

## Referências

1. AHO, A. V.; LAM, M. S.; SETHI, R.; ULLMAN, J. D. **Compiladores: Princípios,
   Técnicas e Ferramentas**. 2. ed. São Paulo: Pearson, 2007.
2. OLIVEIRA, J. C. M. **Notas de aula da disciplina de Compiladores**. UESB,
   2024.
3. UESB/DCET. **Trabalho Prático da II Unidade — Especificação da linguagem
   Sigma**, 2026.
