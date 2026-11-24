# Analisador Léxico (AFND) e Sintático (SLR(1)) para a Linguagem Sigma
## Documentação da versão sem Docker

**Universidade Estadual do Sudoeste da Bahia — UESB**
**Departamento de Ciências Exatas — DCET**
**Disciplina:** Compiladores — **Professor:** José Carlos Martins Oliveira
**Curso:** Ciência da Computação — **Ano:** 2026 — **Período:** II
**Trabalho Prático da II Unidade**

**Integrantes da dupla:** Lis Loureiro e Laiana

---

## Resumo

Este documento descreve o pacote **sem Docker** do analisador da linguagem
Sigma. O pacote reúne três componentes independentes — o **analisador léxico
(AFND)**, o **analisador sintático SLR(1)** e a **interface web local** — além
do conjunto de testes e dos scripts de execução. Para executar o analisador
basta um **JRE 8 ou superior**: os arquivos `.class` já vêm compilados. A
interface é opcional e usa apenas a biblioteca padrão do **Python 3**.

O texto está organizado por componente: a seção 2 explica o léxico, a seção 3
explica o parser e a seção 4 explica a interface. A seção 5 descreve a
instalação e o uso, e a seção 6 apresenta os testes.

**Palavras-chave:** Compiladores. Análise Léxica. AFND. Análise Sintática.
SLR(1). Linguagem Sigma.

---

## 1. Visão geral

O processamento é dividido em duas fases executáveis separadamente:

1. **Fase 1 — análise léxica:** converte o texto-fonte em uma lista de tokens.
2. **Fase 2 — análise sintática:** verifica se a sequência de tokens pertence à
   gramática, usando uma tabela SLR(1).

A ligação entre as fases é feita por um adaptador. O parser **não conhece o
analisador léxico**: ele consome apenas `TokenSintatico`, uma representação
neutra que carrega o terminal da gramática, o lexema e a posição no texto. A
conversão dos tokens do léxico para essa forma neutra fica isolada em
`AdaptadorLexico`.

```
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
```

A interface web não reimplementa nada: ela executa essa mesma aplicação Java e
exibe os resultados, deixando o usuário escolher qual fase rodar.

---

## 2. Analisador léxico

Arquivos: `AnalisadorLexico.java`, `TipoToken.java`, `Token.java`.

### 2.1 Papel

O analisador léxico lê o texto caractere a caractere e devolve uma lista de
`Token`, cada um com:

* a **classe léxica** (`TipoToken`);
* o **lexema** reconhecido;
* a **linha** e a **coluna** iniciais;
* uma eventual **mensagem de erro léxico**.

Espaços, tabulações e quebras de linha são ignorados (as quebras atualizam a
linha). A entrada pública é `AnalisadorLexico.analisar(String texto)`.

### 2.2 Classes léxicas reconhecidas

| Classe | Definição regular / observação | Exemplos |
|---|---|---|
| Palavra reservada | `program, var, integer, boolean, begin, end, if, then, else, while, do, read, readln, write, writeln` (sem diferenciar maiúsculas de minúsculas) | `program`, `while` |
| Identificador | `letra (letra | dígito)*` | `x`, `soma2`, `variável` |
| Número inteiro | `dígito+` | `0`, `13` |
| Número real | `dígito+ '.' dígito+ (('e'|'E')('+'|'-')? dígito+)?` | `1.33`, `24.40e-04` |
| Operador aritmético | `+ - * /` e a palavra `mod` | `a + b`, `x mod 2` |
| Operador relacional | `= >= > < <= <>` | `a <= b` |
| Operador lógico | `and`, `or`, `not` | `p and q` |
| Símbolo especial | `( ) , ; :` | `read(x, y)` |
| Atribuição | `:=` | `x := 1` |
| String | `"` *(qualquer caractere exceto aspa e quebra de linha)* `"` | `"Soma = "` |
| Fim | `.` | `end.` |

As classes cobrem a especificação da I Unidade. A gramática da II Unidade usa
apenas parte delas: **números reais, `mod` e operadores lógicos são
reconhecidos pelo léxico, mas não aparecem nas produções** — se surgirem no
programa, o parser os rejeita na fase 2. Essa separação é proposital: o léxico
é um componente autônomo.

### 2.3 O AFND

O reconhecedor é um **Autômato Finito Não Determinístico (AFND)** construído em
tempo de execução por `construirAFND()`. O estado inicial possui transições-ε
para o sub-autômato de cada classe:

* palavras reservadas e operadores escritos como palavra (`mod`, `and`, `or`,
  `not`), reconhecidos sem diferenciar caixa;
* identificador e números (inteiro e real, incluindo expoente opcional);
* string entre aspas duplas, em uma única linha;
* operadores aritméticos, relacionais, símbolos especiais, atribuição e o ponto
  de fim.

Cada estado final é marcado com a classe que ele reconhece. Como mais de uma
classe pode aceitar o mesmo prefixo (por exemplo, `program` também é um
identificador válido), o desempate segue a **ordem de declaração de
`TipoToken`**: palavra reservada antes de identificador, entre outros.

### 2.4 Simulação e maior casamento

A simulação mantém o **conjunto de estados ativos**:

1. calcula-se o **fecho-ε** do estado inicial;
2. para cada caractere da entrada, aplica-se `mover` e, em seguida, o fecho-ε
   sobre o resultado;
3. guarda-se o maior prefixo aceito (**maximal munch**) e a classe de maior
   prioridade entre as que aceitam aquele prefixo.

Quando nenhum prefixo é aceito, gera-se um token de erro e o analisador consome
um único caractere, continuando a varredura. Uma aspa sem fechamento produz o
erro "cadeia (string) não terminada".

### 2.5 Interface com o restante do projeto

O léxico não conhece gramática nem parser. Ele apenas produz `List<Token>`. A
opção `--fase lexico` (e a Fase 1 da interface) expõe essa etapa isoladamente;
a opção `--automato` imprime as transições do AFND.

---

## 3. Analisador sintático

Arquivos: `AnalisadorSintatico.java`, `Gramatica.java`, `TokenSintatico.java`,
`AdaptadorLexico.java`.

### 3.1 Gramática

A gramática é a GLC do enunciado, com 17 não-terminais (`S, D, V, T, I, L, C,
A, R, W, F, G, M, N, P, E, B`). Para detectar a aceitação, acrescenta-se a
produção aumentada `S' -> S`.

```
S' -> S
S  -> program id ; D begin L end .  |  program id ; begin L end .
D  -> var V
V  -> I : T ; V  |  I : T ;
T  -> integer  |  boolean
I  -> id , I  |  id
L  -> C ; L  |  C ;
C  -> A | R | W | M | N | P
A  -> id := E
R  -> read ( I )  |  readln  |  readln ( I )
W  -> write ( F )  |  writeln  |  writeln ( F )
F  -> G , F  |  G
G  -> str  |  E
M  -> begin L end
N  -> if B then C  |  if B then C else C
P  -> while B do C
E  -> E + E | E - E | E * E | E / E | - E | ( E ) | id | num
B  -> E < E | E <= E | E > E | E >= E | E = E | E <> E | id
```

Essas produções são exatamente as do enunciado; a única inclusão é a produção
aumentada `S'`.

### 3.2 FIRST e FOLLOW

A classe `Gramatica` calcula os conjuntos FIRST e FOLLOW por ponto fixo,
iterando as produções até não haver mudanças. Como não há produções vazias, o
tratamento de ε é mínimo. O FOLLOW de `S'` começa com o marcador de fim de
entrada `$`.

### 3.3 Coleção canônica de itens LR(0)

Partindo do fecho do item `S' -> . S`, aplicam-se as operações de **fecho** e
**transição (goto)** até esgotar os estados. Para a gramática de Sigma resultam
**101 estados**.

### 3.4 Tabela SLR(1) e algoritmo

* item `[A -> α . a β]`, com `a` terminal → ação **empilhar**;
* item `[A -> α .]` → ação **reduzir** para cada terminal em FOLLOW(A);
* item `[S' -> S .]` → **aceitar** em `$`.

A análise usa a pilha de estados clássica:

```
pilha = [0]; i = 0
repita:
    estado = topo(pilha); símbolo = entrada[i]
    ação = ACTION[estado, símbolo]
    se ação == empilhar s: empilha s; i++
    senão se ação == reduzir A -> α:
        desempilha |α| estados
        empilha GOTO[topo, A]
    senão se ação == aceitar: programa válido
    senão: erro com linha, coluna e terminais esperados
```

### 3.5 Conflitos e desambiguação

A gramática do enunciado é **ambígua** (expressões sem precedência e
"dangling else"). A tabela resultante tem **23 conflitos: 21 empilhar/reduzir e
2 reduzir/reduzir**, todos registrados pela opção `--tabela`.

* **Empilhar/reduzir:** resolvidos por precedência e associatividade, no estilo
  Yacc. Níveis: relacionais (1), `+ -` (2), `* /` (3) e menos unário (4). Entre
  operadores de mesma precedência, a associatividade à esquerda leva a reduzir;
  o `else` (dangling else) é empilhado, associando-se ao `if` mais interno.
* **Reduzir/reduzir:** ocorre com `E -> id` versus `B -> id` nos lookaheads
  `then` e `do`. A heurística escolhe a redução cujo não-terminal tem o menor
  FOLLOW contendo o lookahead — na prática, `B -> id` nesses dois casos.

A seção 6.4 da documentação acadêmica registra que uma tabela SLR(1) para uma
gramática ambígua não é livre de conflitos e que a resolução adotada é a mesma
de geradores como o Yacc.

### 3.6 Mensagens de erro

Em caso de erro, o parser imprime:

* a linha e a coluna do token ofensor, o lexema e a classe léxica;
* a lista de terminais esperados naquele estado;
* o veredito "Programa sintaticamente INVÁLIDO.".

Erros léxicos interrompem a fase 2 com a mensagem "Análise sintática
interrompida.".

### 3.7 Independência em relação ao léxico

O núcleo `analisar(Gramatica, List<TokenSintatico>, boolean)` não referencia
`TipoToken`, `Token` nem o AFND. O `main` funciona como raiz de composição:
quando a fase sintática é pedida, ele executa o léxico, converte os tokens com
`AdaptadorLexico` e entrega a lista neutra ao parser. Com `--fase lexico`,
somente a primeira etapa roda.

---

## 4. Interface web

Arquivos: `interface.html`, `servidor.py`, `abrir-interface.bat/.sh`.

### 4.1 Arquitetura

A interface é uma página local servida por um pequeno servidor HTTP em Python,
que usa **apenas a biblioteca padrão**:

* `servidor.py` serve `interface.html` e expõe uma API JSON;
* cada análise executa o analisador Java já compilado como um subprocesso;
* a página exibe tokens, passos do parser, veredito e a saída bruta.

Nada é enviado para a internet: o servidor escuta em `127.0.0.1`.

### 4.2 Endpoints

| Método e rota | Função |
|---|---|
| `GET /` | Serve a página `interface.html`. |
| `GET /api/testes` | Lista `exemplo.txt` e os arquivos de `testes/`. |
| `POST /api/analisar` | Analisa o código enviado no corpo JSON. |
| `POST /api/testes/executar` | Roda a suíte inteira e devolve os resultados. |

O corpo de `/api/analisar` é:

```json
{
  "codigo": "program p; begin writeln(\"ola\"); end.",
  "fase": "lexico" | "sintatico" | "ambas",
  "opcoes": ["--tabela", "--gramatica", "--automato"]
}
```

A resposta inclui `fase`, `tokens`, `passos`, `sintatico` (nulo na fase
léxica), `saida`, `stderr`, `codigoSaida` e `tempoMs`.

### 4.3 As fases como fases de jogo

A página trata as etapas como fases:

* **Fase 1 — análise léxica:** envia `fase = "lexico"`; o servidor executa
  `--fase lexico --tokens` e devolve apenas tokens e erros léxicos.
* **Fase 2 — análise sintática:** envia `fase = "sintatico"`; o servidor
  executa `--fase sintatico --tokens --passos` e devolve os passos SLR(1) e o
  veredito.

O botão "ANALISAR" executa somente a fase selecionada, e o HUD indica a fase
corrente. Cada aba tem uma introdução própria.

Antes das fases, a página exibe uma tela de título ("Bem-vindo à Zecas Bros")
com o sprite do professor Zeca e um cenário de plataforma (colinas, cano,
castelo, blocos e moedas girando); um clique leva ao mapa do mundo, inspirado
no mapa de *Super Mario World*: ilhas com relevo, mar animado, caminho
pontilhado, ponte de madeira, castelo, tubo, Goomba e uma estrela no centro.
As duas fases aparecem como nós circulares numerados (1-1 e 2-1) com placas de
madeira; ao entrar em uma fase, abre-se a introdução correspondente. O botão
**MAPA** do HUD reabre o mapa.

A tela de análise é ambientada como a sala de aula do professor Zeca: quadro
verde com rabiscos de giz (Σ, SLR(1), FIRST/FOLLOW), piso de madeira, estante
de livros e mesa do professor com pilha de livros, maçã e porta-lápis.

Enquanto uma análise ou a suíte está rodando, uma barra de carregamento exibe o
Zeca caminhando sobre a trilha; ao terminar, a barra enche e some. O sprite é
embutido como data URI em `interface.html`, de modo que a página continua
autocontida (sem arquivos de imagem externos).

### 4.4 Robustez

* limite de corpo de 1 MB;
* opções aceitas restritas a `--tabela`, `--gramatica` e `--automato`;
* tempo máximo de execução de 60 segundos;
* validação do campo `fase` (valores inválidos retornam HTTP 400).

---

## 5. Instalação e uso sem Docker

### 5.1 Requisitos

* **Java (JRE) 8 ou superior** para executar (os `.class` já estão incluídos);
* **Python 3.7 ou superior** apenas para a interface;
* **JDK 8 ou superior** apenas para recompilar (opcional).

Não é necessário Docker nem qualquer biblioteca externa.

### 5.2 Console

```
executar.bat exemplo.txt                     (Windows)
./executar.sh exemplo.txt                    (Linux/macOS)

executar.bat exemplo.txt --fase lexico
executar.bat exemplo.txt --fase sintatico --passos
executar.bat teste.txt --tabela
```

Sem arquivo informado, o programa analisa um exemplo embutido. O código de
saída é 0 para programas válidos e 1 para programas com erro.

### 5.3 Testes

```
executar-testes.bat        (Windows)
./executar-testes.sh       (Linux/macOS)
```

Os scripts comparam o veredito obtido com o esperado (deduzido do nome do
arquivo) e imprimem um resumo.

### 5.4 Interface

```
abrir-interface.bat               (Windows)
./abrir-interface.sh              (Linux/macOS)

python servidor.py                # equivalente, sem abrir o navegador
python servidor.py --porta 9000 --no-browser
```

A página fica em `http://localhost:8000`.

### 5.5 Recompilação

```
compilar.bat        (Windows)
./compilar.sh       (Linux/macOS)
```

Quando o JDK suporta `--release`, os scripts compilam com `--release 8`, o que
mantém o bytecode executável em JRE 8 mesmo usando um JDK mais novo.

### 5.6 Acentuação no Windows

Use os scripts `.bat` (que executam `chcp 65001`) ou rode `chcp 65001` antes de
chamar `java` diretamente.

---

## 6. Conjunto de testes

Pasta `testes/` (o campo "Resultado" é o esperado):

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
| `09_valido_operadores_relacionais.txt` | Condições com os seis operadores relacionais. | Válido |
| `10_erro_sintatico_mod.txt` | `mod` reconhecido pelo léxico, mas fora da GLC. | Erro sintático |

Resultado da execução: **10/10 testes OK**.

---

## 7. Limitações

### 7.1 Da linguagem

* não há como atribuir uma expressão booleana (`A -> id := E` usa apenas `E`);
* uma condição é uma comparação entre expressões ou um `id` isolado; não há
  `and`/`or`/`not` nas produções;
* comparações encadeadas (`a < b < c`) não são válidas;
* números reais são reconhecidos pelo léxico, mas a gramática só aceita
  inteiros (`num = dígito+`);
* não há comentários nem sublina na identificação (identificador é
  `letra (letra | dígito)*`).

### 7.2 Da implementação

* não há análise semântica (tipos, declaração/uso de variáveis);
* não há recuperação de erros: a análise para no primeiro erro;
* a gramática é ambígua, e a tabela SLR(1) depende das regras de precedência e
  da heurística de desempate reduzir/reduzir descritas na seção 3.5.

---

## Referências

1. AHO, A. V.; LAM, M. S.; SETHI, R.; ULLMAN, J. D. **Compiladores: Princípios,
   Técnicas e Ferramentas**. 2. ed. São Paulo: Pearson, 2007.
2. OLIVEIRA, J. C. M. **Notas de aula da disciplina de Compiladores**. UESB,
   2024.
3. UESB/DCET. **Trabalho Prático da II Unidade — Especificação da linguagem
   Sigma**, 2026.
