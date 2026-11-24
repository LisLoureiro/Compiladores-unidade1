import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Gramática da linguagem Sigma e geração da tabela de análise SLR(1).
 *
 * As produções são fiéis à GLC do enunciado da II Unidade. Como essa
 * gramática é AMBÍGUA (expressões sem precedência e o clássico "dangling
 * else"), a tabela SLR(1) apresenta conflitos, que aqui são resolvidos por
 * precedência/associatividade no estilo Yacc:
 *
 *   1) Conflitos empilhar/reduzir: comparam-se as precedências do terminal
 *      (lookahead) e da produção (precedência do terminal mais à direita,
 *      com o menos unário usando a precedência especial UMINUS). Em caso de
 *      empate, decide-se pela associatividade; sem precedência, empilha-se.
 *
 *   2) Conflitos reduzir/reduzir: escolhe-se a produção cujo não-terminal tem
 *      o menor conjunto FOLLOW que contém o lookahead (heurística de
 *      especificidade); empate desfeito pelo menor índice de produção.
 *
 * Todos os conflitos e as respectivas decisões ficam registrados em
 * {@link #conflitos()} para documentação e para a opção --tabela.
 */
public final class Gramatica {

    // ------------------------------------------------------------------
    // Tipos auxiliares
    // ------------------------------------------------------------------

    /** Associatividade de um operador. */
    public enum Assoc { ESQ, DIR, NAOASSOC }

    /** Precedência: nível (maior vence) e associatividade. */
    public static final class Prec {
        final int nivel;
        final Assoc assoc;
        Prec(int nivel, Assoc assoc) { this.nivel = nivel; this.assoc = assoc; }
    }

    /** Uma produção A -> X1 X2 ... Xn (o elemento [0] é o lado esquerdo). */
    public static final class Prod {
        public final String lhs;
        public final String[] rhs;
        Prod(String lhs, String[] rhs) { this.lhs = lhs; this.rhs = rhs; }
        public String rhsTexto() {
            if (rhs.length == 0) return "ε";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < rhs.length; i++) {
                if (i > 0) sb.append(' ');
                sb.append(rhs[i]);
            }
            return sb.toString();
        }
    }

    /** Tipo de ação da tabela SLR(1). */
    public enum TipoAcao { EMPILHAR, REDUZIR, ACEITAR, ERRO }

    /** Uma ação: empilhar estado, reduzir produção, aceitar ou erro. */
    public static final class Acao {
        public final TipoAcao tipo;
        public final int valor;
        Acao(TipoAcao tipo, int valor) { this.tipo = tipo; this.valor = valor; }
    }

    /** Item LR(0): produção com um marcador "." em certa posição. */
    static final class Item implements Comparable<Item> {
        final int prod;
        final int ponto;
        Item(int prod, int ponto) { this.prod = prod; this.ponto = ponto; }
        public int compareTo(Item o) {
            if (prod != o.prod) return prod - o.prod;
            return ponto - o.ponto;
        }
        public boolean equals(Object o) {
            if (!(o instanceof Item)) return false;
            Item it = (Item) o;
            return prod == it.prod && ponto == it.ponto;
        }
        public int hashCode() { return prod * 1000 + ponto; }
    }

    /** Marcador de fim de entrada usado pela análise sintática. */
    public static final String FIM_ENTRADA = "$";
    /** Marcador de produção nula (vazio) nos conjuntos FIRST. */
    private static final String VAZIO = "ε";

    /**
     * Produções da gramática. O índice 0 é a produção aumentada S' -> S.
     * As demais são exatamente as do enunciado (a GLC da II Unidade).
     */
    private static final String[][] PRODUCOES = {
        {"S'", "S"},                                            // 0 (aumentada)
        {"S", "program", "id", ";", "D", "begin", "L", "end", "."},  // 1
        {"S", "program", "id", ";", "begin", "L", "end", "."},       // 2
        {"D", "var", "V"},                                      // 3
        {"V", "I", ":", "T", ";", "V"},                         // 4
        {"V", "I", ":", "T", ";"},                              // 5
        {"T", "integer"},                                       // 6
        {"T", "boolean"},                                       // 7
        {"I", "id", ",", "I"},                                  // 8
        {"I", "id"},                                            // 9
        {"L", "C", ";", "L"},                                   // 10
        {"L", "C", ";"},                                        // 11
        {"C", "A"},                                             // 12
        {"C", "R"},                                             // 13
        {"C", "W"},                                             // 14
        {"C", "M"},                                             // 15
        {"C", "N"},                                             // 16
        {"C", "P"},                                             // 17
        {"A", "id", ":=", "E"},                                 // 18
        {"R", "read", "(", "I", ")"},                           // 19
        {"R", "readln"},                                        // 20
        {"R", "readln", "(", "I", ")"},                         // 21
        {"W", "write", "(", "F", ")"},                          // 22
        {"W", "writeln"},                                       // 23
        {"W", "writeln", "(", "F", ")"},                         // 24
        {"F", "G", ",", "F"},                                   // 25
        {"F", "G"},                                             // 26
        {"G", "str"},                                           // 27
        {"G", "E"},                                             // 28
        {"M", "begin", "L", "end"},                             // 29
        {"N", "if", "B", "then", "C"},                          // 30
        {"N", "if", "B", "then", "C", "else", "C"},             // 31
        {"P", "while", "B", "do", "C"},                         // 32
        {"E", "E", "+", "E"},                                   // 33
        {"E", "E", "-", "E"},                                   // 34
        {"E", "E", "*", "E"},                                   // 35
        {"E", "E", "/", "E"},                                   // 36
        {"E", "-", "E"},                                        // 37 (menos unário)
        {"E", "(", "E", ")"},                                   // 38
        {"E", "id"},                                            // 39
        {"E", "num"},                                           // 40
        {"B", "E", "<", "E"},                                   // 41
        {"B", "E", "<=", "E"},                                  // 42
        {"B", "E", ">", "E"},                                   // 43
        {"B", "E", ">=", "E"},                                  // 44
        {"B", "E", "=", "E"},                                   // 45
        {"B", "E", "<>", "E"},                                  // 46
        {"B", "id"}                                             // 47
    };

    private final Prod[] prods = new Prod[PRODUCOES.length];
    private final Set<String> naoTerminais = new LinkedHashSet<String>();
    private final List<String> terminais = new ArrayList<String>();
    private final Map<String, Prec> precedencias = new HashMap<String, Prec>();

    private final Map<String, Set<String>> first = new LinkedHashMap<String, Set<String>>();
    private final Map<String, Set<String>> follow = new LinkedHashMap<String, Set<String>>();

    private final List<TreeSet<Item>> estados = new ArrayList<TreeSet<Item>>();
    private final Map<String, Integer> transicao = new HashMap<String, Integer>();
    private final Map<String, Acao> acao = new HashMap<String, Acao>();
    private final List<String> conflitos = new ArrayList<String>();
    private int nConflitosSR = 0;
    private int nConflitosRR = 0;

    public Gramatica() {
        for (int i = 0; i < PRODUCOES.length; i++) {
            String[] p = PRODUCOES[i];
            String[] rhs = new String[p.length - 1];
            System.arraycopy(p, 1, rhs, 0, rhs.length);
            prods[i] = new Prod(p[0], rhs);
            naoTerminais.add(p[0]);
        }
        // Conjunto de terminais = símbolos que não são não-terminais + "$".
        TreeSet<String> ter = new TreeSet<String>();
        for (Prod p : prods) {
            for (String s : p.rhs) {
                if (!naoTerminais.contains(s)) ter.add(s);
            }
        }
        terminais.addAll(ter);
        terminais.add(FIM_ENTRADA);

        // Precedências (menor -> maior). As relacionais têm o menor nível;
        // o menos unário tem o maior (via pseudo-terminal UMINUS).
        precedencias.put("<", new Prec(1, Assoc.NAOASSOC));
        precedencias.put("<=", new Prec(1, Assoc.NAOASSOC));
        precedencias.put(">", new Prec(1, Assoc.NAOASSOC));
        precedencias.put(">=", new Prec(1, Assoc.NAOASSOC));
        precedencias.put("=", new Prec(1, Assoc.NAOASSOC));
        precedencias.put("<>", new Prec(1, Assoc.NAOASSOC));
        precedencias.put("+", new Prec(2, Assoc.ESQ));
        precedencias.put("-", new Prec(2, Assoc.ESQ));
        precedencias.put("*", new Prec(3, Assoc.ESQ));
        precedencias.put("/", new Prec(3, Assoc.ESQ));
        precedencias.put("UMINUS", new Prec(4, Assoc.DIR));

        calcularFirst();
        calcularFollow();
        construirTabela();
    }

    // ------------------------------------------------------------------
    // FIRST / FOLLOW
    // ------------------------------------------------------------------
    private Set<String> primeiroSimbolo(String x) {
        if (naoTerminais.contains(x)) return first.get(x);
        Set<String> s = new LinkedHashSet<String>();
        s.add(x);
        return s;
    }

    /** FIRST de uma sequência, a partir do índice "de". Pode conter "ε". */
    private Set<String> primeiroSequencia(String[] seq, int de) {
        Set<String> res = new LinkedHashSet<String>();
        boolean todosVazios = true;
        for (int i = de; i < seq.length; i++) {
            Set<String> f = primeiroSimbolo(seq[i]);
            for (String s : f) {
                if (!VAZIO.equals(s)) res.add(s);
            }
            if (!f.contains(VAZIO)) { todosVazios = false; break; }
        }
        if (todosVazios) res.add(VAZIO);
        return res;
    }

    private void calcularFirst() {
        for (String nt : naoTerminais) first.put(nt, new LinkedHashSet<String>());
        boolean mudou = true;
        while (mudou) {
            mudou = false;
            for (Prod p : prods) {
                Set<String> f = primeiroSequencia(p.rhs, 0);
                Set<String> destino = first.get(p.lhs);
                for (String s : f) {
                    if (destino.add(s)) mudou = true;
                }
            }
        }
    }

    private void calcularFollow() {
        for (String nt : naoTerminais) follow.put(nt, new LinkedHashSet<String>());
        follow.get("S'").add(FIM_ENTRADA);
        boolean mudou = true;
        while (mudou) {
            mudou = false;
            for (Prod p : prods) {
                for (int i = 0; i < p.rhs.length; i++) {
                    String xi = p.rhs[i];
                    if (!naoTerminais.contains(xi)) continue;
                    Set<String> f = primeiroSequencia(p.rhs, i + 1);
                    Set<String> destino = follow.get(xi);
                    for (String s : f) {
                        if (!VAZIO.equals(s) && destino.add(s)) mudou = true;
                    }
                    if (f.contains(VAZIO)) {
                        for (String s : follow.get(p.lhs)) {
                            if (destino.add(s)) mudou = true;
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Coleção canônica de itens LR(0)
    // ------------------------------------------------------------------
    private TreeSet<Item> fecho(Set<Item> itens) {
        TreeSet<Item> res = new TreeSet<Item>(itens);
        Deque<Item> pilha = new ArrayDeque<Item>(itens);
        while (!pilha.isEmpty()) {
            Item it = pilha.pop();
            Prod p = prods[it.prod];
            if (it.ponto < p.rhs.length) {
                String x = p.rhs[it.ponto];
                if (naoTerminais.contains(x)) {
                    for (int k = 0; k < prods.length; k++) {
                        if (prods[k].lhs.equals(x)) {
                            Item novo = new Item(k, 0);
                            if (res.add(novo)) pilha.push(novo);
                        }
                    }
                }
            }
        }
        return res;
    }

    private TreeSet<Item> mover(Set<Item> itens, String x) {
        TreeSet<Item> base = new TreeSet<Item>();
        for (Item it : itens) {
            Prod p = prods[it.prod];
            if (it.ponto < p.rhs.length && p.rhs[it.ponto].equals(x)) {
                base.add(new Item(it.prod, it.ponto + 1));
            }
        }
        if (base.isEmpty()) return base;
        return fecho(base);
    }

    private static String chaveEstado(TreeSet<Item> itens) {
        StringBuilder sb = new StringBuilder();
        for (Item it : itens) sb.append(it.prod).append('.').append(it.ponto).append(';');
        return sb.toString();
    }

    private static String celula(int estado, String simbolo) {
        return estado + "|" + simbolo;
    }

    private void construirEstados() {
        TreeSet<Item> inicial = fecho(Collections.singleton(new Item(0, 0)));
        estados.add(inicial);
        Map<String, Integer> indice = new HashMap<String, Integer>();
        indice.put(chaveEstado(inicial), 0);

        ArrayDeque<Integer> fila = new ArrayDeque<Integer>();
        fila.add(0);
        while (!fila.isEmpty()) {
            int idx = fila.poll();
            TreeSet<Item> itens = estados.get(idx);

            // Símbolos imediatamente após o "." (ordem determinística).
            TreeSet<String> simbolos = new TreeSet<String>();
            for (Item it : itens) {
                Prod p = prods[it.prod];
                if (it.ponto < p.rhs.length) simbolos.add(p.rhs[it.ponto]);
            }
            for (String x : simbolos) {
                TreeSet<Item> j = mover(itens, x);
                if (j.isEmpty()) continue;
                String k = chaveEstado(j);
                Integer destino = indice.get(k);
                if (destino == null) {
                    destino = estados.size();
                    estados.add(j);
                    indice.put(k, destino);
                    fila.add(destino);
                }
                transicao.put(celula(idx, x), destino);
            }
        }
    }

    // ------------------------------------------------------------------
    // Precedência de produções
    // ------------------------------------------------------------------
    private Prec precProducao(int prod) {
        Prod p = prods[prod];
        // Menos unário: E -> - E recebe a precedência UMINUS.
        if (p.lhs.equals("E") && p.rhs.length == 2
                && p.rhs[0].equals("-") && p.rhs[1].equals("E")) {
            return precedencias.get("UMINUS");
        }
        for (int i = p.rhs.length - 1; i >= 0; i--) {
            Prec q = precedencias.get(p.rhs[i]);
            if (q != null) return q;
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Construção da tabela SLR(1)
    // ------------------------------------------------------------------
    private void construirTabela() {
        construirEstados();

        // Coleta todas as ações candidatas por célula (estado, terminal).
        Map<String, List<Acao>> candidatas = new LinkedHashMap<String, List<Acao>>();
        for (int e = 0; e < estados.size(); e++) {
            for (Item it : estados.get(e)) {
                Prod p = prods[it.prod];
                if (it.ponto < p.rhs.length) {
                    String x = p.rhs[it.ponto];
                    if (!naoTerminais.contains(x)) {
                        Integer destino = transicao.get(celula(e, x));
                        adicionar(candidatas, celula(e, x),
                                new Acao(TipoAcao.EMPILHAR, destino));
                    }
                } else {
                    if (p.lhs.equals("S'")) {
                        adicionar(candidatas, celula(e, FIM_ENTRADA),
                                new Acao(TipoAcao.ACEITAR, 0));
                    } else {
                        for (String t : follow.get(p.lhs)) {
                            adicionar(candidatas, celula(e, t),
                                    new Acao(TipoAcao.REDUZIR, it.prod));
                        }
                    }
                }
            }
        }

        // Resolve cada célula (possíveis conflitos) e monta a tabela final.
        for (Map.Entry<String, List<Acao>> entrada : candidatas.entrySet()) {
            String k = entrada.getKey();
            int barra = k.indexOf('|');
            int estado = Integer.parseInt(k.substring(0, barra));
            String terminal = k.substring(barra + 1);
            Acao decisao = resolver(estado, terminal, entrada.getValue());
            if (decisao != null) acao.put(k, decisao);
        }
    }

    private static void adicionar(Map<String, List<Acao>> mapa, String k, Acao a) {
        List<Acao> l = mapa.get(k);
        if (l == null) { l = new ArrayList<Acao>(); mapa.put(k, l); }
        l.add(a);
    }

    /** Entre várias reduções, escolhe a de FOLLOW mais específico. */
    private int melhorRR(List<Integer> regras, String terminal) {
        Integer melhor = null;
        int melhorTam = Integer.MAX_VALUE;
        for (int r : regras) {
            // Todas as regras candidatas têm "terminal" em FOLLOW(lhs).
            int tam = follow.get(prods[r].lhs).size();
            if (melhor == null || tam < melhorTam || (tam == melhorTam && r < melhor)) {
                melhor = r;
                melhorTam = tam;
            }
        }
        return melhor;
    }

    private Acao resolver(int estado, String terminal, List<Acao> cands) {
        Acao shift = null;
        List<Integer> reduzir = new ArrayList<Integer>();
        boolean aceitar = false;
        boolean erroForcado = false;
        for (Acao a : cands) {
            if (a.tipo == TipoAcao.EMPILHAR) shift = a;
            else if (a.tipo == TipoAcao.REDUZIR) reduzir.add(a.valor);
            else if (a.tipo == TipoAcao.ACEITAR) aceitar = true;
            else erroForcado = true;
        }
        if (aceitar) return new Acao(TipoAcao.ACEITAR, 0);

        Integer red = null;
        if (reduzir.size() == 1) {
            red = reduzir.get(0);
        } else if (reduzir.size() > 1) {
            red = melhorRR(reduzir, terminal);
            nConflitosRR++;
            conflitos.add("Estado " + estado + ", lookahead '" + terminal
                    + "': conflito reduzir/reduzir entre "
                    + listaProducoes(reduzir) + " -> escolhida produção "
                    + red + " (" + prods[red].lhs + " -> " + prods[red].rhsTexto()
                    + ") por especificidade de FOLLOW");
        }

        if (shift != null && red != null) {
            Prec prp = precProducao(red);
            Prec ptp = precedencias.get(terminal);
            Acao escolhida;
            String motivo;
            if (prp != null && ptp != null) {
                if (prp.nivel > ptp.nivel) {
                    escolhida = new Acao(TipoAcao.REDUZIR, red);
                    motivo = "reduzir (produção de maior precedência)";
                } else if (ptp.nivel > prp.nivel) {
                    escolhida = new Acao(TipoAcao.EMPILHAR, shift.valor);
                    motivo = "empilhar (terminal de maior precedência)";
                } else if (prp.assoc == Assoc.ESQ) {
                    escolhida = new Acao(TipoAcao.REDUZIR, red);
                    motivo = "reduzir (associatividade à esquerda)";
                } else if (prp.assoc == Assoc.DIR) {
                    escolhida = new Acao(TipoAcao.EMPILHAR, shift.valor);
                    motivo = "empilhar (associatividade à direita)";
                } else {
                    escolhida = new Acao(TipoAcao.ERRO, 0);
                    motivo = "erro (operador não associativo)";
                }
            } else {
                escolhida = new Acao(TipoAcao.EMPILHAR, shift.valor);
                motivo = "empilhar (padrão)";
            }
            nConflitosSR++;
            conflitos.add("Estado " + estado + ", lookahead '" + terminal
                    + "': conflito empilhar/reduzir (empilhar x reduzir "
                    + red + " " + prods[red].lhs + " -> " + prods[red].rhsTexto()
                    + ") -> " + motivo);
            return escolhida;
        }

        if (shift != null) return shift;
        if (red != null) return new Acao(TipoAcao.REDUZIR, red);
        if (erroForcado) return new Acao(TipoAcao.ERRO, 0);
        return null;
    }

    private String listaProducoes(List<Integer> regras) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < regras.size(); i++) {
            if (i > 0) sb.append(", ");
            int r = regras.get(i);
            sb.append(prods[r].lhs).append(" -> ").append(prods[r].rhsTexto());
        }
        return sb.append(")").toString();
    }

    // ------------------------------------------------------------------
    // API de consulta
    // ------------------------------------------------------------------
    public Acao acao(int estado, String terminal) {
        return acao.get(celula(estado, terminal));
    }

    public Integer irPara(int estado, String naoTerminal) {
        return transicao.get(celula(estado, naoTerminal));
    }

    public List<String> terminais() { return terminais; }

    public Prod producao(int i) { return prods[i]; }

    public int numeroProducoes() { return prods.length; }

    public int numeroEstados() { return estados.size(); }

    public List<String> conflitos() { return conflitos; }

    public int conflitosShiftReduce() { return nConflitosSR; }

    public int conflitosReduceReduce() { return nConflitosRR; }

    // ------------------------------------------------------------------
    // Impressão
    // ------------------------------------------------------------------
    private static String repetir(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }

    public void imprimirGramatica() {
        System.out.println("===== GRAMÁTICA (GLC) =====");
        System.out.println("Produções (0 = aumentada):");
        for (int i = 0; i < prods.length; i++) {
            System.out.println(String.format("  (%2d) %s -> %s",
                    i, prods[i].lhs, prods[i].rhsTexto()));
        }
        System.out.println();
        System.out.println("Não-terminais: " + join(naoTerminais));
        System.out.println("Terminais: " + join(terminais));
        System.out.println();
        System.out.println("Conjuntos FIRST:");
        for (String nt : naoTerminais) {
            System.out.println(String.format("  FIRST(%-3s) = { %s }", nt, join(first.get(nt))));
        }
        System.out.println();
        System.out.println("Conjuntos FOLLOW:");
        for (String nt : naoTerminais) {
            System.out.println(String.format("  FOLLOW(%-3s) = { %s }", nt, join(follow.get(nt))));
        }
        System.out.println();
    }

    public void imprimirTabela() {
        System.out.println("===== TABELA SLR(1) =====");
        System.out.println("Número de estados: " + estados.size());
        System.out.println("Conflitos encontrados: " + (nConflitosSR + nConflitosRR)
                + " (empilhar/reduzir: " + nConflitosSR
                + ", reduzir/reduzir: " + nConflitosRR + ")");
        if (!conflitos.isEmpty()) {
            System.out.println("Detalhe dos conflitos e decisões:");
            for (String c : conflitos) System.out.println("  - " + c);
        }
        System.out.println();
        System.out.println("Ações e transições por estado:");
        for (int e = 0; e < estados.size(); e++) {
            System.out.println("Estado " + e + ":");
            System.out.println("  Itens:");
            for (Item it : estados.get(e)) {
                Prod p = prods[it.prod];
                StringBuilder sb = new StringBuilder("    " + p.lhs + " ->");
                for (int i = 0; i <= p.rhs.length; i++) {
                    if (i == it.ponto) sb.append(" .");
                    if (i < p.rhs.length) sb.append(' ').append(p.rhs[i]);
                }
                System.out.println(sb.toString());
            }
            System.out.println("  Ações:");
            for (String t : terminais) {
                Acao a = acao.get(celula(e, t));
                if (a == null) continue;
                String desc;
                if (a.tipo == TipoAcao.EMPILHAR) desc = "empilhar " + a.valor;
                else if (a.tipo == TipoAcao.REDUZIR) desc = "reduzir " + a.valor
                        + " (" + prods[a.valor].lhs + " -> " + prods[a.valor].rhsTexto() + ")";
                else if (a.tipo == TipoAcao.ACEITAR) desc = "aceitar";
                else desc = "erro";
                System.out.println("    em '" + t + "': " + desc);
            }
            System.out.println("  Goto:");
            for (String nt : naoTerminais) {
                Integer g = transicao.get(celula(e, nt));
                if (g != null) System.out.println("    " + nt + " -> " + g);
            }
        }
        System.out.println(repetir('=', 40));
    }

    private static String join(java.util.Collection<String> colecao) {
        StringBuilder sb = new StringBuilder();
        boolean primeiro = true;
        for (String s : colecao) {
            if (!primeiro) sb.append(", ");
            sb.append(s);
            primeiro = false;
        }
        return sb.toString();
    }
}
