import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/*
 * ============================================================
 *  ANALISADOR LÉXICO DA LINGUAGEM Mini_Pascal — AFD
 * ============================================================
 *
 *  Trabalho Prático da I Unidade — Compiladores (UESB/DCET).
 *
 *  O reconhecimento é feito por UM Autômato Finito DETERMINÍSTICO (AFD).
 *  O AFD é descrito por uma TABELA DE TRANSIÇÕES: delta[estado][símbolo]
 *  = próximo estado.  A simulação lê o texto, mantém o estado corrente e,
 *  ao mesmo tempo, memoriza o ÚLTIMO estado de aceitação encontrado.  Se
 *  em algum ponto não houver transição, o analisador "retrocede" até esse
 *  último estado de aceitação (regra do MAIOR CASAMENTO / maximal munch).
 *
 *  Classes léxicas reconhecidas (rótulos textuais, sem códigos numéricos):
 *    1) Palavra reservada       7) Operador relacional
 *    2) Identificador           8) Operador lógico
 *    3) Número inteiro          9) Símbolo especial
 *    4) Número real            10) Atribuição
 *    5) Caractere              11) Fim
 *    6) Cadeia                 12) (Erro léxico)
 *
 *  Caracteres não significativos (espaços, TAB, ENTER) e COMENTÁRIOS DE
 *  BLOCO "/* ... *\/" são descartados e NÃO aparecem na saída.
 *
 *  Uso:
 *    javac -encoding UTF-8 AnalisadorLexicoAFD.java
 *    java AnalisadorLexicoAFD entrada.txt [saida.txt]
 *    java AnalisadorLexicoAFD --afd                 (imprime a tabela do AFD)
 *    java AnalisadorLexicoAFD --cadeia "x := 1+2;"  (analisa um texto direto)
 *    java AnalisadorLexicoAFD -                     (lê da entrada padrão)
 *
 *  Sem informar entrada, o programa analisa um exemplo embutido.
 *  Código de saída: 0 (sem erros) ou 1 (houve erro léxico).
 */
public class AnalisadorLexicoAFD {

    // ============================================================
    // RÓTULOS DAS CLASSES LÉXICAS (tokens)
    // ============================================================

    static final String PALAVRA_RESERVADA  = "Palavra reservada";
    static final String IDENTIFICADOR      = "Identificador";
    static final String NUMERO_INTEIRO     = "Número inteiro";
    static final String NUMERO_REAL        = "Número real";
    static final String CARACTERE          = "Caractere";
    static final String CADEIA             = "Cadeia";
    static final String OPERADOR_ARITMETICO = "Operador aritmético";
    static final String OPERADOR_RELACIONAL = "Operador relacional";
    static final String OPERADOR_LOGICO     = "Operador lógico";
    static final String SIMBOLO_ESPECIAL    = "Símbolo especial";
    static final String ATRIBUICAO          = "Atribuição";
    static final String FIM                 = "Fim";
    static final String ERRO                = "Erro léxico";

    // ============================================================
    // ALFABETO DE ENTRADA DO AFD
    // ============================================================
    //
    // Além dos caracteres abaixo, o AFD possui classes de símbolo para
    // letra, dígito, a letra "e"/"E" (usada no expoente), quebra de linha
    // e um símbolo genérico "outro".

    static final String SIMBOLOS = "+-*/(),;:.=<>'\"_";

    static final int SIM_E         = SIMBOLOS.length();
    static final int SIM_LETRA     = SIM_E + 1;
    static final int SIM_DIGITO    = SIM_LETRA + 1;
    static final int SIM_NOVALINHA = SIM_DIGITO + 1;
    static final int SIM_OUTRO     = SIM_NOVALINHA + 1;
    static final int NUM_SIMBOLOS  = SIM_OUTRO + 1;

    /** Traduz um caractere da entrada para o símbolo do AFD. */
    static int simbolo(char c) {

        if (c == '\n') {
            return SIM_NOVALINHA;
        }

        // "e"/"E" têm símbolo próprio por causa do expoente dos reais.
        if (c == 'e' || c == 'E') {
            return SIM_E;
        }

        int p = SIMBOLOS.indexOf(c);

        if (p >= 0) {
            return p;
        }

        if (Character.isLetter(c)) {
            return SIM_LETRA;
        }

        if (c >= '0' && c <= '9') {
            return SIM_DIGITO;
        }

        return SIM_OUTRO;
    }

    /** Nome legível de um símbolo do AFD (usado na impressão da tabela). */
    static String nomeSimbolo(int s) {

        if (s == SIM_E)         return "e|E";
        if (s == SIM_LETRA)     return "letra";
        if (s == SIM_DIGITO)    return "dígito";
        if (s == SIM_NOVALINHA) return "\\n";
        if (s == SIM_OUTRO)     return "outro";

        return "'" + SIMBOLOS.charAt(s) + "'";
    }

    // ============================================================
    // O AUTÔMATO FINITO DETERMINÍSTICO
    // ============================================================

    static final class AFD {

        /** tabela[estado][simbolo] = próximo estado (-1 = não existe). */
        final List<int[]> tabela = new ArrayList<int[]>();

        /** classe[estado] = classe léxica aceita (null = não é final). */
        final List<String> classe = new ArrayList<String>();

        /** Cria um novo estado (não final) e devolve seu índice. */
        int novoEstado() {

            int[] linha = new int[NUM_SIMBOLOS];

            for (int i = 0; i < NUM_SIMBOLOS; i++) {
                linha[i] = -1;
            }

            tabela.add(linha);
            classe.add(null);

            return tabela.size() - 1;
        }

        /** Acrescenta a transição delta[origem][simbolo] = destino. */
        void transicao(int origem, int simbolo, int destino) {
            tabela.get(origem)[simbolo] = destino;
        }

        /** Marca o estado como final, aceitando a classe indicada. */
        void marcarFinal(int estado, String classeL) {
            classe.set(estado, classeL);
        }

        /** Liga TODOS os símbolos, exceto os informados, ao destino dado. */
        void todasExceto(int origem, int destino, int... exceto) {

            for (int s = 0; s < NUM_SIMBOLOS; s++) {

                boolean ignorar = false;

                for (int e : exceto) {
                    if (e == s) {
                        ignorar = true;
                        break;
                    }
                }

                if (!ignorar) {
                    transicao(origem, s, destino);
                }
            }
        }
    }

    // ============================================================
    // CONSTRUÇÃO DO AFD (contempla todos os itens léxicos)
    // ============================================================

    static AFD construirAFD() {

        AFD a = new AFD();

        int q0 = a.novoEstado();

        // ----- Identificador / palavra reservada -----
        // (letra | '_') (letra | dígito | '_' | e|E)*
        int qIdent = a.novoEstado();
        a.transicao(q0, SIM_LETRA, qIdent);
        a.transicao(q0, SIM_E, qIdent);
        a.transicao(q0, simbolo('_'), qIdent);
        a.transicao(qIdent, SIM_LETRA, qIdent);
        a.transicao(qIdent, SIM_E, qIdent);
        a.transicao(qIdent, SIM_DIGITO, qIdent);
        a.transicao(qIdent, simbolo('_'), qIdent);
        a.marcarFinal(qIdent, IDENTIFICADOR);   // reservada/logico/aritm. depois

        // ----- Número inteiro: dígito+ -----
        int qInt = a.novoEstado();
        a.transicao(q0, SIM_DIGITO, qInt);
        a.transicao(qInt, SIM_DIGITO, qInt);
        a.marcarFinal(qInt, NUMERO_INTEIRO);

        // ----- Número real: dígito+ '.' dígito+ ( (e|E)(+|-)? dígito+ )? -----
        int qPonto = a.novoEstado();     // viu o ponto, espera dígito
        int qFrac = a.novoEstado();      // parte fracionária
        a.transicao(qInt, simbolo('.'), qPonto);
        a.transicao(qPonto, SIM_DIGITO, qFrac);
        a.transicao(qFrac, SIM_DIGITO, qFrac);
        a.marcarFinal(qFrac, NUMERO_REAL);

        int qExp = a.novoEstado();       // viu 'e'/'E'
        int qSinal = a.novoEstado();     // viu '+'/'-' do expoente
        int qExpDig = a.novoEstado();    // dígitos do expoente
        a.transicao(qFrac, SIM_E, qExp);
        a.transicao(qExp, simbolo('+'), qSinal);
        a.transicao(qExp, simbolo('-'), qSinal);
        a.transicao(qExp, SIM_DIGITO, qExpDig);
        a.transicao(qSinal, SIM_DIGITO, qExpDig);
        a.transicao(qExpDig, SIM_DIGITO, qExpDig);
        a.marcarFinal(qExpDig, NUMERO_REAL);

        // ----- Caractere: ' c ' -----
        int qCharAbre = a.novoEstado();
        int qCharCorpo = a.novoEstado();
        int qCharFim = a.novoEstado();
        a.transicao(q0, simbolo('\''), qCharAbre);
        a.todasExceto(qCharAbre, qCharCorpo, simbolo('\''), SIM_NOVALINHA);
        a.transicao(qCharCorpo, simbolo('\''), qCharFim);
        a.marcarFinal(qCharFim, CARACTERE);

        // ----- Cadeia: " ... " -----
        int qStrAbre = a.novoEstado();
        int qStr = a.novoEstado();
        int qStrFim = a.novoEstado();
        a.transicao(q0, simbolo('"'), qStrAbre);
        a.transicao(qStrAbre, simbolo('"'), qStrFim);       // cadeia vazia ""
        a.todasExceto(qStrAbre, qStr, simbolo('"'), SIM_NOVALINHA);
        a.transicao(qStr, simbolo('"'), qStrFim);
        a.todasExceto(qStr, qStr, simbolo('"'), SIM_NOVALINHA);
        a.marcarFinal(qStrFim, CADEIA);

        // ----- Operadores aritméticos: + - * / -----
        int qArit = a.novoEstado();
        a.transicao(q0, simbolo('+'), qArit);
        a.transicao(q0, simbolo('-'), qArit);
        a.transicao(q0, simbolo('*'), qArit);
        a.transicao(q0, simbolo('/'), qArit);
        a.marcarFinal(qArit, OPERADOR_ARITMETICO);

        // ----- Símbolos especiais: ( ) , ; -----
        int qEsp = a.novoEstado();
        a.transicao(q0, simbolo('('), qEsp);
        a.transicao(q0, simbolo(')'), qEsp);
        a.transicao(q0, simbolo(','), qEsp);
        a.transicao(q0, simbolo(';'), qEsp);
        a.marcarFinal(qEsp, SIMBOLO_ESPECIAL);

        // ----- Fim: . -----
        int qFim = a.novoEstado();
        a.transicao(q0, simbolo('.'), qFim);
        a.marcarFinal(qFim, FIM);

        // ----- '=' (relacional) -----
        int qIgual = a.novoEstado();
        a.transicao(q0, simbolo('='), qIgual);
        a.marcarFinal(qIgual, OPERADOR_RELACIONAL);

        // ----- ':' (especial) e ':=' (atribuição) -----
        int qDoisPontos = a.novoEstado();
        int qAtrib = a.novoEstado();
        a.transicao(q0, simbolo(':'), qDoisPontos);
        a.transicao(qDoisPontos, simbolo('='), qAtrib);
        a.marcarFinal(qDoisPontos, SIMBOLO_ESPECIAL);
        a.marcarFinal(qAtrib, ATRIBUICAO);

        // ----- '<', '<=' e '<>' -----
        int qMenor = a.novoEstado();
        int qMenorIgual = a.novoEstado();
        int qMenorMaior = a.novoEstado();
        a.transicao(q0, simbolo('<'), qMenor);
        a.transicao(qMenor, simbolo('='), qMenorIgual);
        a.transicao(qMenor, simbolo('>'), qMenorMaior);
        a.marcarFinal(qMenor, OPERADOR_RELACIONAL);
        a.marcarFinal(qMenorIgual, OPERADOR_RELACIONAL);
        a.marcarFinal(qMenorMaior, OPERADOR_RELACIONAL);

        // ----- '>' e '>=' -----
        int qMaior = a.novoEstado();
        int qMaiorIgual = a.novoEstado();
        a.transicao(q0, simbolo('>'), qMaior);
        a.transicao(qMaior, simbolo('='), qMaiorIgual);
        a.marcarFinal(qMaior, OPERADOR_RELACIONAL);
        a.marcarFinal(qMaiorIgual, OPERADOR_RELACIONAL);

        return a;
    }

    static final AFD AFD = construirAFD();

    // ============================================================
    // TABELA DE PALAVRAS RESERVADAS
    // ============================================================
    //
    // A busca é feita em tempo constante (HashMap).  A comparação ignora
    // maiúsculas/minúsculas, pois Mini_Pascal é uma linguagem
    // "case-insensitive" (o exemplo do enunciado usa "program" minúsculo
    // e "Program" maiúsculo com o mesmo significado).

    static final class TabelaPalavras {

        private final java.util.Map<String, String> palavras =
            new java.util.HashMap<String, String>();

        void inserir(String palavra) {
            palavras.put(palavra.toUpperCase(Locale.ROOT), palavra);
        }

        boolean contem(String lexema) {
            return palavras.containsKey(lexema.toUpperCase(Locale.ROOT));
        }

        int tamanho() {
            return palavras.size();
        }

        static TabelaPalavras de(String... palavras) {

            TabelaPalavras t = new TabelaPalavras();

            for (String p : palavras) {
                t.inserir(p);
            }

            return t;
        }
    }

    /**
     * Lista de palavras reservadas da linguagem, EXATAMENTE como no
     * enunciado.  Note que "read", "write" e "writeln" NÃO fazem parte
     * desta lista (no exemplo do enunciado elas saem como Identificador).
     */
    static final String[] PALAVRAS_RESERVADAS = {
        "ABSOLUTE", "ARRAY", "BEGIN", "CASE", "CHAR", "CONST", "DIV", "DO",
        "DOWTO", "ELSE", "END", "EXTERNAL", "FILE", "FOR", "FORWARD", "FUNC",
        "FUNCTION", "GOTO", "IF", "IMPLEMENTATION", "INTEGER", "INTERFACE",
        "INTERRUPT", "LABEL", "MAIN", "NIL", "NIT", "OF", "PACKED", "PROC",
        "PROGRAM", "REAL", "RECORD", "REPEAT", "SET", "SHL", "SHR", "STRING",
        "THEN", "TO", "TYPE", "UNIT", "UNTIL", "USES", "VAR", "WHILE", "WITH",
        "XOR"
    };

    /** Operadores lógicos escritos como palavra. */
    static final String[] PALAVRAS_LOGICAS = { "AND", "OR", "NOT" };

    /** Operadores aritméticos escritos como palavra. */
    static final String[] PALAVRAS_ARITMETICAS = { "MOD" };

    static final TabelaPalavras TABELA_RESERVADAS =
        TabelaPalavras.de(PALAVRAS_RESERVADAS);

    static final TabelaPalavras TABELA_LOGICAS =
        TabelaPalavras.de(PALAVRAS_LOGICAS);

    static final TabelaPalavras TABELA_ARITMETICAS =
        TabelaPalavras.de(PALAVRAS_ARITMETICAS);

    // ============================================================
    // TOKEN: o par (cadeia, classe léxica) devolvido pelo analisador
    // ============================================================

    static final class Token {

        final String lexema;
        final String classe;
        final int linha;
        final int coluna;

        /** Mensagem de erro léxico, ou null quando o token é válido. */
        final String mensagemErro;

        private Token(String lexema, String classe, int linha, int coluna,
                String mensagemErro) {
            this.lexema = lexema;
            this.classe = classe;
            this.linha = linha;
            this.coluna = coluna;
            this.mensagemErro = mensagemErro;
        }

        static Token valido(String lexema, String classe, int l, int c) {
            return new Token(lexema, classe, l, c, null);
        }

        static Token erro(String lexema, int l, int c, String mensagem) {
            return new Token(lexema, ERRO, l, c, mensagem);
        }

        boolean isErro() {
            return mensagemErro != null;
        }

        /** Par no formato exigido: <cadeia> <token>. */
        String par() {
            return String.format("%-20s %s", lexema, classe);
        }
    }

    // ============================================================
    // ANALISADOR LÉXICO (função que devolve, a cada chamada, um token)
    // ============================================================

    static final class Lexer {

        private final String fonte;
        private final int n;
        private int pos = 0;
        private int linha = 1;
        private int coluna = 1;

        Lexer(String fonte) {
            this.fonte = fonte;
            this.n = fonte.length();
        }

        /**
         * Devolve o próximo token (par cadeia + classe) ou null no fim da
         * entrada.  Caracteres não significativos e comentários são
         * descartados aqui.
         */
        Token proximoToken() {

            // ---- descarta espaços, ENTER e comentários de bloco ----
            while (pos < n) {

                char c = fonte.charAt(pos);

                if (c == '\n') {
                    linha++;
                    coluna = 1;
                    pos++;
                    continue;
                }

                if (Character.isWhitespace(c)) {
                    pos++;
                    coluna++;
                    continue;
                }

                // comentário de bloco /* ... */
                if (c == '/' && pos + 1 < n && fonte.charAt(pos + 1) == '*') {

                    int l = linha;
                    int col = coluna;

                    if (!pularComentario()) {
                        return Token.erro(
                            "/*", l, col,
                            "comentário de bloco não fechado"
                        );
                    }

                    continue;
                }

                break;
            }

            if (pos >= n) {
                return null;      // fim da entrada
            }

            // ---- executa o AFD a partir do estado inicial ----
            int inicio = pos;
            int linhaToken = linha;
            int colunaToken = coluna;

            int estado = 0;
            String classeAceita = null;
            int fimAceito = -1;

            int j = pos;

            while (j < n) {

                int s = simbolo(fonte.charAt(j));
                int proximo = (s < 0) ? -1 : AFD.tabela.get(estado)[s];

                if (proximo < 0) {
                    break;            // não há transição: para
                }

                estado = proximo;
                j++;

                String cl = AFD.classe.get(estado);

                if (cl != null) {     // estado final: memoriza o aceite
                    classeAceita = cl;
                    fimAceito = j;
                }
            }

            // ---- nenhum prefixo foi aceito => erro léxico ----
            if (classeAceita == null) {

                char c0 = fonte.charAt(inicio);
                String mensagem;

                if (c0 == '"') {
                    mensagem = "cadeia de caracteres não terminada";
                } else if (c0 == '\'') {
                    mensagem = "constante de caractere mal formada";
                } else {
                    mensagem = "símbolo desconhecido";
                }

                pos++;
                coluna++;

                return Token.erro(String.valueOf(c0), linhaToken, colunaToken,
                        mensagem);
            }

            // ---- produziu um token válido ----
            String lexema = fonte.substring(inicio, fimAceito);

            coluna += (fimAceito - inicio);
            pos = fimAceito;

            // Um identificador pode ser, na verdade, palavra reservada ou
            // operador escrito como palavra (and/or/not/mod).
            if (IDENTIFICADOR.equals(classeAceita)) {

                if (TABELA_RESERVADAS.contem(lexema)) {
                    classeAceita = PALAVRA_RESERVADA;
                } else if (TABELA_LOGICAS.contem(lexema)) {
                    classeAceita = OPERADOR_LOGICO;
                } else if (TABELA_ARITMETICAS.contem(lexema)) {
                    classeAceita = OPERADOR_ARITMETICO;
                }
            }

            return Token.valido(lexema, classeAceita, linhaToken, colunaToken);
        }

        /**
         * Consome "/* ... *\/".  Devolve true se o comentário foi fechado e
         * false se a entrada terminou antes do fechamento.
         */
        private boolean pularComentario() {

            pos += 2;          // consome "/*"
            coluna += 2;

            while (pos < n) {

                char c = fonte.charAt(pos);

                if (c == '*' && pos + 1 < n && fonte.charAt(pos + 1) == '/') {
                    pos += 2;
                    coluna += 2;
                    return true;
                }

                if (c == '\n') {
                    linha++;
                    coluna = 1;
                } else {
                    coluna++;
                }

                pos++;
            }

            return false;
        }
    }

    /** Analisa toda a entrada e devolve a lista de tokens. */
    static List<Token> analisar(String fonte) {

        List<Token> tokens = new ArrayList<Token>();
        Lexer lexer = new Lexer(fonte);

        Token t;

        while ((t = lexer.proximoToken()) != null) {
            tokens.add(t);
        }

        return tokens;
    }

    // ============================================================
    // IMPRESSÃO DA TABELA DO AFD (opção --afd)
    // ============================================================

    static void imprimirAFD() {

        System.out.println(
            "AFD — estado inicial q0 (" + AFD.tabela.size() + " estados)"
        );
        System.out.println();

        System.out.println("Estados finais (de aceitação):");

        for (int e = 0; e < AFD.classe.size(); e++) {
            if (AFD.classe.get(e) != null) {
                System.out.println("  q" + e + " -> " + AFD.classe.get(e));
            }
        }

        System.out.println();
        System.out.println("Transições delta(estado, símbolo) = estado:");

        for (int e = 0; e < AFD.tabela.size(); e++) {

            int[] linha = AFD.tabela.get(e);

            for (int s = 0; s < NUM_SIMBOLOS; s++) {

                if (linha[s] >= 0) {
                    System.out.println(
                        "  d(q" + e + ", " + nomeSimbolo(s)
                        + ") = q" + linha[s]
                    );
                }
            }
        }
    }

    // ============================================================
    // PROGRAMA DE EXEMPLO (usado quando nenhum arquivo é informado)
    // ============================================================

    static final String EXEMPLO =
        "program teste;\n" +
        "var x,y: integer;\n" +
        "\n" +
        "const pi := 3.1416;\n" +
        "/* inicio do programa */\n" +
        "begin\n" +
        "read(x);\n" +
        "if (x > y) then\n" +
        "y := x ;\n" +
        "else\n" +
        "y := -x;\n" +
        "writeln(x);\n" +
        "write(y);\n" +
        "end.\n";

    // ============================================================
    // MAIN
    // ============================================================

    static String repetir(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    /** Lê toda a entrada de um fluxo (usado para a entrada padrão). */
    static byte[] lerTudo(InputStream in) throws IOException {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] bloco = new byte[4096];
        int lidos;

        while ((lidos = in.read(bloco)) > 0) {
            bos.write(bloco, 0, lidos);
        }

        return bos.toByteArray();
    }

    public static void main(String[] args) throws IOException {

        String arquivoEntrada = null;
        String arquivoSaida = "saida.txt";
        String cadeiaDireta = null;
        boolean lerStdin = false;
        List<String> posicionais = new ArrayList<String>();

        for (int i = 0; i < args.length; i++) {

            String a = args[i];

            if (a.equals("--afd")) {
                imprimirAFD();
                return;
            }

            if (a.equals("--cadeia")) {

                if (i + 1 >= args.length) {
                    System.err.println("Uso: --cadeia \"texto a analisar\"");
                    System.exit(2);
                    return;
                }

                cadeiaDireta = args[++i];

            } else {
                posicionais.add(a);
            }
        }

        if (cadeiaDireta != null) {

            // com --cadeia, o 1º argumento posicional é o arquivo de saída
            if (!posicionais.isEmpty()) {
                arquivoSaida = posicionais.get(0);
            }

        } else if (!posicionais.isEmpty()) {

            String p = posicionais.get(0);

            if (p.equals("-") || p.equals("--stdin")) {
                lerStdin = true;
            } else {
                arquivoEntrada = p;
            }

            if (posicionais.size() >= 2) {
                arquivoSaida = posicionais.get(1);
            }
        }

        String fonte;

        if (cadeiaDireta != null) {

            fonte = cadeiaDireta;

        } else if (lerStdin) {

            System.err.println(
                "Lendo da entrada padrao. Finalize com Ctrl+Z e ENTER "
                + "(Windows) ou Ctrl+D (Linux/macOS)."
            );

            fonte = new String(lerTudo(System.in), StandardCharsets.UTF_8);

        } else if (arquivoEntrada != null) {

            try {
                fonte = new String(
                    Files.readAllBytes(Paths.get(arquivoEntrada)),
                    StandardCharsets.UTF_8
                );
            } catch (IOException e) {
                System.err.println(
                    "Erro ao ler o arquivo '"
                    + arquivoEntrada + "': " + e.getMessage()
                );
                System.exit(2);
                return;
            }

        } else {

            fonte = EXEMPLO;

            System.out.println(
                "Nenhum arquivo informado. Analisando o programa de exemplo.\n"
            );
        }

        // remove eventual BOM no início do arquivo
        if (fonte.startsWith("﻿")) {
            fonte = fonte.substring(1);
        }

        List<Token> tokens = analisar(fonte);

        // ---------- listagem no console ----------
        System.out.println(
            "========== ANALISADOR LÉXICO (AFD) ==========\n"
        );
        System.out.printf("%-6s %-6s %-20s %s%n",
                "LINHA", "COL", "CLASSE", "LEXEMA");
        System.out.println(repetir('-', 70));

        StringBuilder saida = new StringBuilder();
        int erros = 0;

        for (Token t : tokens) {

            if (t.isErro()) {

                erros++;

                System.out.printf("%-6d %-6d %-20s %s%n",
                        t.linha, t.coluna, ERRO,
                        "'" + t.lexema + "' (" + t.mensagemErro + ")");

            } else {

                System.out.printf("%-6d %-6d %-20s %s%n",
                        t.linha, t.coluna, t.classe, t.lexema);
            }

            saida.append(t.par()).append(System.lineSeparator());
        }

        System.out.println(repetir('-', 70));
        System.out.println(
            "Total de tokens: " + (tokens.size() - erros)
            + " | Erros léxicos: " + erros
        );

        // ---------- arquivo de saída ----------
        Files.write(
            Paths.get(arquivoSaida),
            saida.toString().getBytes(StandardCharsets.UTF_8)
        );

        System.out.println("Saída gravada em: " + arquivoSaida);

        if (erros > 0) {
            System.exit(1);
        }
    }
}
