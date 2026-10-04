import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.IntPredicate;

/**
 * Analisador Léxico da linguagem do Trabalho Prático da I Unidade,
 * implementado por um ÚNICO Autômato Finito NÃO Determinístico (AFND).
 *
 * O autômato possui um estado inicial (q0) com transições-ε para o
 * sub-autômato de CADA classe léxica. A simulação percorre o texto mantendo o
 * CONJUNTO de estados ativos (fecho-ε + movimento) e aplica a regra do MAIOR
 * CASAMENTO (maximal munch): o token reconhecido é o maior prefixo aceito pelo
 * AFND. Empates são desfeitos pela ordem de prioridade declarada em
 * {@link TipoToken}.
 *
 * Classes léxicas contempladas:
 *   1) Palavra Reservada       6) Operador Relacional
 *   2) Identificador           7) Operador Lógico
 *   3) Número Inteiro          8) Símbolo Especial
 *   4) Número Real             9) Atribuição
 *   5) Operador Aritmético    10) Fim
 *
 * Uso:
 *   javac -encoding UTF-8 *.java
 *   java AnalisadorLexicoAFND programa.txt     (analisa um arquivo)
 *   java AnalisadorLexicoAFND                  (analisa o programa de exemplo)
 *   java AnalisadorLexicoAFND --afnd           (imprime a tabela de transições do AFND)
 */
public final class AnalisadorLexicoAFND {

    // ------------------------------------------------------------------
    // Palavras da linguagem
    // ------------------------------------------------------------------
    /** Palavras reservadas (classe 1). Ajuste conforme a linguagem do trabalho. */
    public static final String[] PALAVRAS_RESERVADAS = {
        "program", "var", "integer", "real", "begin", "end",
        "if", "then", "else", "while", "do", "read", "write"
    };

    /** Operadores lógicos escritos como palavras (classe 7). */
    public static final String[] PALAVRAS_LOGICAS = { "and", "or", "not" };

    /** Operadores aritméticos escritos como palavras (classe 5). */
    public static final String[] PALAVRAS_ARITMETICAS = { "mod" };

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

        /** Classes aceitas pelo conjunto de estados, em ordem de prioridade. */
        List<TipoToken> aceitos(Set<Integer> estados) {
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

        void imprimir() {
            System.out.println("AFND ÚNICO: estado inicial = q" + inicial
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
    // Construção do AFND único (com ε-transições a partir de q0)
    // ------------------------------------------------------------------
    static IntPredicate igual(final char c) {
        return new IntPredicate() {
            public boolean test(int x) { return x == c; }
        };
    }

    /** Ramo do AFND, ligado a q0 por transição-ε, que reconhece a cadeia dada. */
    static void caminho(AFND m, String cadeia, TipoToken tipo) {
        int atual = m.novoEstado();
        m.epsilon(m.inicial, atual);
        for (int k = 0; k < cadeia.length(); k++) {
            char c = cadeia.charAt(k);
            int prox = m.novoEstado();
            m.aresta(atual, prox, igual(c), "'" + c + "'");
            atual = prox;
        }
        m.marcarFinal(atual, tipo);
    }

    /** Constrói o AFND único que contempla TODAS as classes léxicas. */
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
        IntPredicate expoente = new IntPredicate() {
            public boolean test(int c) { return c == 'e' || c == 'E'; }
        };
        IntPredicate sinal = new IntPredicate() {
            public boolean test(int c) { return c == '+' || c == '-'; }
        };

        // 1) Palavras reservadas.
        for (String p : PALAVRAS_RESERVADAS) caminho(m, p, TipoToken.PALAVRA_RESERVADA);

        // 5) e 7) Operadores escritos como palavras.
        for (String w : PALAVRAS_ARITMETICAS) caminho(m, w, TipoToken.OPERADOR_ARITMETICO);
        for (String w : PALAVRAS_LOGICAS) caminho(m, w, TipoToken.OPERADOR_LOGICO);

        // 2) Identificador: letra (letra | dígito)*
        int i0 = m.novoEstado(), i1 = m.novoEstado();
        m.epsilon(m.inicial, i0);
        m.aresta(i0, i1, letra, "letra");
        m.aresta(i1, i1, letraOuDigito, "letra|dígito");
        m.marcarFinal(i1, TipoToken.IDENTIFICADOR);

        // 3) e 4) Números: inteiro dígito+  e  real dígito+ '.' dígito+ ((e|E)(+|-)?dígito+)?
        int n0 = m.novoEstado(), n1 = m.novoEstado(), n2 = m.novoEstado(), n3 = m.novoEstado();
        int n4 = m.novoEstado(), n5 = m.novoEstado(), n6 = m.novoEstado();
        m.epsilon(m.inicial, n0);
        m.aresta(n0, n1, digito, "dígito");
        m.aresta(n1, n1, digito, "dígito");
        m.marcarFinal(n1, TipoToken.NUMERO_INTEIRO);
        m.aresta(n1, n2, igual('.'), "'.'");
        m.aresta(n2, n3, digito, "dígito");
        m.aresta(n3, n3, digito, "dígito");
        m.marcarFinal(n3, TipoToken.NUMERO_REAL);
        m.aresta(n3, n4, expoente, "'e'|'E'");
        m.aresta(n4, n5, sinal, "'+'|'-'");
        m.aresta(n4, n6, digito, "dígito");
        m.aresta(n5, n6, digito, "dígito");
        m.aresta(n6, n6, digito, "dígito");
        m.marcarFinal(n6, TipoToken.NUMERO_REAL);

        // 5) Operadores aritméticos de símbolo: + - * /
        for (String op : new String[] {"+", "-", "*", "/"}) {
            caminho(m, op, TipoToken.OPERADOR_ARITMETICO);
        }

        // 6) Operadores relacionais: = >= > < <= <>
        for (String op : new String[] {"=", ">=", ">", "<", "<=", "<>"}) {
            caminho(m, op, TipoToken.OPERADOR_RELACIONAL);
        }

        // 8) Símbolos especiais: = ( ) , ; :
        for (String s : new String[] {"=", "(", ")", ",", ";", ":"}) {
            caminho(m, s, TipoToken.SIMBOLO_ESPECIAL);
        }

        // 9) Atribuição: :=
        caminho(m, ":=", TipoToken.ATRIBUICAO);

        // 10) Fim: .
        caminho(m, ".", TipoToken.FIM);

        return m;
    }

    // ------------------------------------------------------------------
    // Simulação do AFND (maior casamento)
    // ------------------------------------------------------------------
    /** Converte o texto em uma lista de tokens (espaços em branco são ignorados). */
    public static List<Token> analisar(AFND afnd, String texto) {
        List<Token> tokens = new ArrayList<Token>();
        int i = 0, linha = 1, coluna = 1, n = texto.length();

        while (i < n) {
            char c = texto.charAt(i);
            if (c == '\n') { linha++; coluna = 1; i++; continue; }
            if (Character.isWhitespace(c)) { coluna++; i++; continue; }

            Set<Integer> atual = afnd.fecho(Collections.singleton(afnd.inicial));
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
                tokens.add(Token.erro(String.valueOf(c), linha, coluna,
                        "caractere não reconhecido"));
                i++;
                coluna++;
            } else {
                String lexema = texto.substring(i, melhorFim);
                TipoToken tipo = melhorTipos.get(0); // maior prioridade
                tokens.add(Token.valido(tipo, lexema, linha, coluna));
                coluna += lexema.length();
                i = melhorFim;
            }
        }
        return tokens;
    }

    public static List<Token> analisar(String texto) {
        return analisar(construirAFND(), texto);
    }

    // ------------------------------------------------------------------
    // Saída e main
    // ------------------------------------------------------------------
    static void imprimirTokens(List<Token> tokens) {
        System.out.printf("%-6s %-6s %-20s %s%n", "LINHA", "COL", "CLASSE", "LEXEMA");
        System.out.println(repetir('-', 60));
        int erros = 0;
        for (Token t : tokens) {
            if (t.isErro()) {
                erros++;
                System.out.printf("%-6d %-6d %-20s %s%n", t.linha, t.coluna,
                        "ERRO LÉXICO", "'" + t.lexema + "' (" + t.erro + ")");
            } else {
                System.out.printf("%-6d %-6d %-20s %s%n", t.linha, t.coluna,
                        t.classeNome(), t.lexema);
            }
        }
        System.out.println(repetir('-', 60));
        System.out.println("Total de tokens: " + (tokens.size() - erros)
                + " | Erros léxicos: " + erros);
    }

    static String repetir(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }

    static final String EXEMPLO =
        "program exemplo;\n" +
        "var x: integer;\n" +
        "    y: real;\n" +
        "begin\n" +
        "  x := 13 mod 4;\n" +
        "  y := 24.40e-04 * 1.33 / (x + 1);\n" +
        "  if x >= 1 and not (y < 2.5) then\n" +
        "    x := x + 1;\n" +
        "  read(x);\n" +
        "  write(y);\n" +
        "end.\n";

    public static void main(String[] args) throws IOException {
        AFND afnd = construirAFND();

        if (args.length > 0 && args[0].equals("--afnd")) {
            afnd.imprimir();
            return;
        }

        String texto;
        if (args.length > 0) {
            try {
                texto = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
            } catch (IOException e) {
                System.err.println("Erro ao ler o arquivo '" + args[0] + "': " + e.getMessage());
                System.exit(2);
                return;
            }
        } else {
            texto = EXEMPLO;
            System.out.println("Nenhum arquivo informado. Analisando o programa de exemplo:\n");
            System.out.println(texto);
        }

        List<Token> tokens = analisar(afnd, texto);
        imprimirTokens(tokens);

        for (Token t : tokens) {
            if (t.isErro()) {
                System.exit(1);
                return;
            }
        }
    }

    private AnalisadorLexicoAFND() {
    }
}
