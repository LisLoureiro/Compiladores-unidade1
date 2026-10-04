import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.IntPredicate;

/**
 * Analisador Léxico da linguagem Sigma.
 *
 * Implementação baseada em um Autômato Finito NÃO Determinístico (AFND),
 * reaproveitada do 1º trabalho e ajustada à gramática da II Unidade:
 *
 *   - Novas palavras reservadas: program, var, integer, boolean, begin, end,
 *     if, then, else, while, do, read, readln, write, writeln.
 *   - Novo token STRING (literal entre aspas duplas).
 *   - Removidos os tokens que não existem em Sigma (mod, and, or, not, real, #).
 *   - "=" passa a ser exclusivamente OPERADOR_RELACIONAL.
 *
 * O AFND tem um estado inicial (q0) com transições-ε para o sub-autômato de
 * cada classe léxica. A simulação percorre o texto mantendo o CONJUNTO de
 * estados ativos (fecho-ε + movimento) e aplica a regra do MAIOR CASAMENTO:
 * o token reconhecido é o maior prefixo aceito pelo AFND. Empates são
 * desfeitos pela ordem de prioridade declarada em {@link TipoToken}.
 */
public final class AnalisadorLexico {

    /** Palavras reservadas da linguagem Sigma. */
    public static final String[] PALAVRAS_RESERVADAS = {
        "program", "var", "integer", "boolean", "begin", "end",
        "if", "then", "else", "while", "do",
        "read", "readln", "write", "writeln"
    };

    // ------------------------------------------------------------------
    // AFND
    // ------------------------------------------------------------------
    static final class AFND {
        static final class Aresta {
            final IntPredicate rotulo;   // null = transição-ε
            final String descricao;
            final int destino;

            Aresta(IntPredicate rotulo, String descricao, int destino) {
                this.rotulo = rotulo;
                this.descricao = descricao;
                this.destino = destino;
            }
        }

        private final List<List<Aresta>> transicoes = new ArrayList<List<Aresta>>();
        private final TreeMap<Integer, TipoToken> finais = new TreeMap<Integer, TipoToken>();
        final int inicial;

        AFND() {
            inicial = novoEstado();
        }

        int novoEstado() {
            transicoes.add(new ArrayList<Aresta>());
            return transicoes.size() - 1;
        }

        void aresta(int origem, int destino, IntPredicate rotulo, String descricao) {
            transicoes.get(origem).add(new Aresta(rotulo, descricao, destino));
        }

        void epsilon(int origem, int destino) {
            transicoes.get(origem).add(new Aresta(null, "ε", destino));
        }

        void marcarFinal(int estado, TipoToken tipo) {
            finais.put(estado, tipo);
        }

        /** Fecho-ε de um conjunto de estados. */
        Set<Integer> fecho(Set<Integer> estados) {
            Set<Integer> resultado = new TreeSet<Integer>(estados);
            Deque<Integer> pilha = new ArrayDeque<Integer>(estados);
            while (!pilha.isEmpty()) {
                int e = pilha.pop();
                for (Aresta a : transicoes.get(e)) {
                    if (a.rotulo == null && resultado.add(a.destino)) {
                        pilha.push(a.destino);
                    }
                }
            }
            return resultado;
        }

        /** Estados alcançáveis lendo o caractere c (sem fecho-ε). */
        Set<Integer> mover(Set<Integer> estados, int c) {
            Set<Integer> resultado = new TreeSet<Integer>();
            for (int e : estados) {
                for (Aresta a : transicoes.get(e)) {
                    if (a.rotulo != null && a.rotulo.test(c)) {
                        resultado.add(a.destino);
                    }
                }
            }
            return resultado;
        }

        /** Classes léxicas aceitas pelo conjunto de estados. */
        List<TipoToken> aceitos(Set<Integer> estados) {
            // A ordem de iteração de TipoToken respeita a prioridade declarada.
            List<TipoToken> tipos = new ArrayList<TipoToken>();
            for (TipoToken t : TipoToken.values()) {
                for (int e : estados) {
                    if (finais.get(e) == t) {
                        tipos.add(t);
                        break;
                    }
                }
            }
            return tipos;
        }

        /** Imprime a tabela de transições do AFND (opção --automato). */
        void imprimir() {
            System.out.println("AFND: estado inicial = q" + inicial
                    + "  (" + transicoes.size() + " estados)");
            System.out.println("Estados finais:");
            for (java.util.Map.Entry<Integer, TipoToken> e : finais.entrySet()) {
                System.out.println("  q" + e.getKey() + " -> " + e.getValue().nome);
            }
            System.out.println("Transições:");
            for (int e = 0; e < transicoes.size(); e++) {
                for (Aresta a : transicoes.get(e)) {
                    System.out.println("  d(q" + e + ", " + a.descricao + ") = q" + a.destino);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Construção do AFND
    // ------------------------------------------------------------------
    static IntPredicate igual(final char c, final boolean ignorarCaixa) {
        if (ignorarCaixa) {
            return new IntPredicate() {
                public boolean test(int x) {
                    return Character.toLowerCase(x) == Character.toLowerCase(c);
                }
            };
        }
        return new IntPredicate() {
            public boolean test(int x) {
                return x == c;
            }
        };
    }

    /** Ramo do AFND que reconhece exatamente a cadeia dada. */
    static void caminho(AFND m, String cadeia, TipoToken tipo, boolean ignorarCaixa) {
        int atual = m.novoEstado();
        m.epsilon(m.inicial, atual);
        for (int k = 0; k < cadeia.length(); k++) {
            char c = cadeia.charAt(k);
            int prox = m.novoEstado();
            m.aresta(atual, prox, igual(c, ignorarCaixa), "'" + c + "'");
            atual = prox;
        }
        m.marcarFinal(atual, tipo);
    }

    /** Constrói o AFND com todas as classes léxicas de Sigma. */
    public static AFND construirAFND() {
        AFND m = new AFND();
        IntPredicate letra = new IntPredicate() {
            public boolean test(int c) { return Character.isLetter(c); }
        };
        IntPredicate digito = new IntPredicate() {
            public boolean test(int c) { return c >= '0' && c <= '9'; }
        };
        IntPredicate letraOuDigito = new IntPredicate() {
            public boolean test(int c) {
                return Character.isLetter(c) || (c >= '0' && c <= '9');
            }
        };

        // 1) Palavras reservadas (sem diferenciar maiúsculas/minúsculas).
        for (String p : PALAVRAS_RESERVADAS) {
            caminho(m, p, TipoToken.PALAVRA_RESERVADA, true);
        }

        // 2) Identificador: letra (letra | dígito)*
        int i0 = m.novoEstado(), i1 = m.novoEstado();
        m.epsilon(m.inicial, i0);
        m.aresta(i0, i1, letra, "letra");
        m.aresta(i1, i1, letraOuDigito, "letra|dígito");
        m.marcarFinal(i1, TipoToken.IDENTIFICADOR);

        // 3) Número inteiro: dígito+
        int n0 = m.novoEstado(), n1 = m.novoEstado();
        m.epsilon(m.inicial, n0);
        m.aresta(n0, n1, digito, "dígito");
        m.aresta(n1, n1, digito, "dígito");
        m.marcarFinal(n1, TipoToken.NUMERO);

        // 4) String: '"' (qualquer caractere exceto '"' e nova linha)* '"'
        int s0 = m.novoEstado(), s1 = m.novoEstado(), s2 = m.novoEstado();
        m.epsilon(m.inicial, s0);
        m.aresta(s0, s1, igual('"', false), "aspa");
        m.aresta(s1, s1, new IntPredicate() {
            public boolean test(int c) { return c != '"' && c != '\n' && c != '\r'; }
        }, "caractere≠aspa");
        m.aresta(s1, s2, igual('"', false), "aspa");
        m.marcarFinal(s2, TipoToken.STRING);

        // 5) Operadores aritméticos: + - * /
        for (String op : new String[] {"+", "-", "*", "/"}) {
            caminho(m, op, TipoToken.OPERADOR_ARITMETICO, false);
        }

        // 6) Operadores relacionais: = >= > < <= <>
        for (String op : new String[] {"=", ">=", ">", "<", "<=", "<>"}) {
            caminho(m, op, TipoToken.OPERADOR_RELACIONAL, false);
        }

        // 7) Símbolos especiais: ( ) , ; :
        for (String s : new String[] {"(", ")", ",", ";", ":"}) {
            caminho(m, s, TipoToken.SIMBOLO_ESPECIAL, false);
        }

        // 8) Atribuição: :=
        caminho(m, ":=", TipoToken.ATRIBUICAO, false);

        // 9) Fim de programa: .
        caminho(m, ".", TipoToken.FIM, false);

        return m;
    }

    // ------------------------------------------------------------------
    // Simulação do AFND (maior casamento)
    // ------------------------------------------------------------------
    /**
     * Converte o texto de entrada em uma lista de tokens.
     * Espaços em branco são ignorados; caracteres não reconhecidos geram
     * tokens de erro léxico (com {@link Token#erro} preenchido).
     */
    public static List<Token> analisar(AFND afnd, String texto) {
        List<Token> tokens = new ArrayList<Token>();
        int i = 0, linha = 1, coluna = 1, n = texto.length();

        while (i < n) {
            char c = texto.charAt(i);
            if (c == '\n') { linha++; coluna = 1; i++; continue; }
            if (Character.isWhitespace(c)) { coluna++; i++; continue; }

            Set<Integer> atual = afnd.fecho(java.util.Collections.singleton(afnd.inicial));
            int j = i, melhorFim = -1;
            List<TipoToken> melhorTipos = null;

            while (true) {
                List<TipoToken> aceitos = afnd.aceitos(atual);
                if (!aceitos.isEmpty()) {
                    melhorFim = j;
                    melhorTipos = aceitos;
                }
                if (j >= n) break;
                Set<Integer> prox = afnd.mover(atual, texto.charAt(j));
                if (prox.isEmpty()) break;
                atual = afnd.fecho(prox);
                j++;
            }

            if (melhorFim < 0) {
                // Nenhum prefixo aceito: erro léxico, consome 1 caractere.
                String msg;
                if (c == '"') {
                    msg = "cadeia (string) não terminada";
                } else {
                    msg = "caractere não reconhecido";
                }
                tokens.add(Token.erro(String.valueOf(c), linha, coluna, msg));
                i++;
                coluna++;
            } else {
                String lexema = texto.substring(i, melhorFim);
                // melhorTipos já está ordenado por prioridade; o 1º vence.
                TipoToken tipo = melhorTipos.get(0);
                tokens.add(Token.valido(tipo, lexema, linha, coluna));
                coluna += lexema.length();
                i = melhorFim;
            }
        }
        return tokens;
    }

    /** Conveniência: constrói o AFND e analisa o texto. */
    public static List<Token> analisar(String texto) {
        return analisar(construirAFND(), texto);
    }

    private AnalisadorLexico() {
    }
}
