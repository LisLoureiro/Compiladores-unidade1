import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Analisador Sintático SLR(1) da linguagem Sigma.
 *
 * O parser é INDEPENDENTE DO ANALISADOR LÉXICO: a análise consome apenas uma
 * lista de {@link TokenSintatico}, que carrega o símbolo terminal da gramática
 * e a posição no texto. A conversão dos tokens do léxico fica em
 * {@link AdaptadorLexico}; o núcleo do parser não conhece {@link TipoToken},
 * o AFND nem a classe {@link Token}.
 *
 * A tabela ACTION/GOTO é construída dinamicamente por {@link Gramatica} a
 * partir das produções, e a análise usa o algoritmo clássico de pilha
 * (estados) + fluxo de tokens:
 *
 *   - EMPILHAR estado  : avança na entrada empilhando o novo estado;
 *   - REDUZIR A -> α   : desempilha |α| estados e empilha GOTO[topo, A];
 *   - ACEITAR          : entrada reconhecida;
 *   - erro             : mensagem com linha/coluna e terminais esperados.
 *
 * Uso:
 *   java AnalisadorSintatico [arquivo] [opções]
 *
 * Opções:
 *   --fase lexico|sintatico|ambas  executa só a fase indicada (padrão: ambas)
 *   --tokens      imprime a tabela de tokens reconhecidos
 *   --gramatica   imprime produções, FIRST e FOLLOW
 *   --tabela      imprime a tabela SLR(1) e os conflitos resolvidos
 *   --passos      imprime o passo a passo da análise
 *   --automato    imprime a tabela de transições do AFND do léxico
 *   -h, --help    mostra esta ajuda
 *
 * A fase léxica pode ser usada sozinha (--fase lexico) para gerar a sequência
 * de tokens; a fase sintática (--fase sintatico) monta essa sequência
 * internamente e alimenta o parser, que trabalha apenas sobre tokens.
 */
public final class AnalisadorSintatico {

    /** Programa de exemplo usado quando nenhum arquivo é informado. */
    private static final String EXEMPLO =
        "program exemplo;\n" +
        "var x, y, soma : integer;\n" +
        "    ok : boolean;\n" +
        "begin\n" +
        "  x := 10;\n" +
        "  y := 3;\n" +
        "  soma := x + y * 2 - (x - 1) / 2;\n" +
        "  read(x, y);\n" +
        "  write(\"Soma = \", soma);\n" +
        "  if soma > 0 then\n" +
        "    writeln(\"positivo\")\n" +
        "  else\n" +
        "    writeln(\"nao positivo\");\n" +
        "  while x < 100 do\n" +
        "    x := x + 1;\n" +
        "end.\n";

    public static void main(String[] args) throws IOException {
        List<String> opcoes = new ArrayList<String>();
        String arquivo = null;
        String fase = "ambas";

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.equals("--fase")) {
                if (i + 1 >= args.length) {
                    System.err.println("Uso: --fase lexico|sintatico|ambas");
                    System.exit(2);
                    return;
                }
                fase = args[++i].toLowerCase();
                if (!fase.equals("lexico") && !fase.equals("sintatico")
                        && !fase.equals("ambas")) {
                    System.err.println("Fase inválida: '" + fase
                            + "'. Use lexico, sintatico ou ambas.");
                    System.exit(2);
                    return;
                }
            } else if (a.startsWith("-")) {
                opcoes.add(a);
            } else if (arquivo == null) {
                arquivo = a;
            }
        }

        if (opcoes.contains("-h") || opcoes.contains("--help")) {
            imprimirAjuda();
            return;
        }

        AnalisadorLexico.AFND afnd = AnalisadorLexico.construirAFND();
        if (opcoes.contains("--automato")) {
            afnd.imprimir();
        }

        String texto;
        if (arquivo != null) {
            try {
                texto = new String(Files.readAllBytes(Paths.get(arquivo)), StandardCharsets.UTF_8);
            } catch (IOException e) {
                System.err.println("Erro ao ler o arquivo '" + arquivo + "': " + e.getMessage());
                System.exit(2);
                return;
            }
        } else {
            texto = EXEMPLO;
            System.out.println("Nenhum arquivo informado. Analisando o programa de exemplo embutido.\n");
        }

        // ------------------------------------------------------------------
        // Fase 1: análise léxica.
        // ------------------------------------------------------------------
        List<Token> tokens = AnalisadorLexico.analisar(afnd, texto);
        if (fase.equals("lexico") || opcoes.contains("--tokens")) {
            imprimirTokens(tokens);
        }

        List<String> errosLexicos = new ArrayList<String>();
        for (Token t : tokens) {
            if (t.isErro()) {
                errosLexicos.add("  linha " + t.linha + ", coluna " + t.coluna
                        + ": " + t.erro + " ('" + t.lexema + "')");
            }
        }

        if (fase.equals("lexico")) {
            if (!errosLexicos.isEmpty()) {
                System.out.println("Foram encontrados " + errosLexicos.size()
                        + " erro(s) léxico(s):");
                for (String e : errosLexicos) System.out.println(e);
                System.exit(1);
            }
            System.out.println("Análise léxica concluída: " + tokens.size()
                    + " token(s) reconhecido(s).");
            return;
        }

        if (!errosLexicos.isEmpty()) {
            System.out.println("Foram encontrados " + errosLexicos.size()
                    + " erro(s) léxico(s):");
            for (String e : errosLexicos) System.out.println(e);
            System.out.println("\nAnálise sintática interrompida.");
            System.exit(1);
            return;
        }

        // ------------------------------------------------------------------
        // Fase 2: análise sintática (independente do léxico).
        // ------------------------------------------------------------------
        List<TokenSintatico> entrada = AdaptadorLexico.converter(tokens);

        Gramatica gramatica = new Gramatica();
        if (opcoes.contains("--gramatica")) {
            gramatica.imprimirGramatica();
        }
        if (opcoes.contains("--tabela")) {
            gramatica.imprimirTabela();
        }

        analisar(gramatica, entrada, opcoes.contains("--passos"));
    }

    // ------------------------------------------------------------------
    // Análise sintática (núcleo independente do léxico)
    // ------------------------------------------------------------------
    private static void analisar(Gramatica g, List<TokenSintatico> tokens, boolean passos) {
        // Sequência de terminais de entrada + marcador de fim "$".
        List<String> entrada = new ArrayList<String>();
        for (TokenSintatico t : tokens) entrada.add(t.simbolo);
        entrada.add(Gramatica.FIM_ENTRADA);

        Deque<Integer> pilha = new ArrayDeque<Integer>();
        pilha.push(0);
        int i = 0;
        int reducoes = 0;

        while (true) {
            int estado = pilha.peek();
            String simbolo = entrada.get(i);
            Gramatica.Acao a = g.acao(estado, simbolo);

            if (a == null || a.tipo == Gramatica.TipoAcao.ERRO) {
                if (passos) System.out.println("ERRO no estado " + estado + ", lookahead '" + simbolo + "'");
                reportarErro(g, estado, tokens, entrada, i);
                System.exit(1);
                return;
            }

            if (passos) {
                System.out.println("Estado " + estado + ", lookahead '" + simbolo + "' -> " + descrever(g, a));
            }

            if (a.tipo == Gramatica.TipoAcao.EMPILHAR) {
                pilha.push(a.valor);
                i++;
            } else if (a.tipo == Gramatica.TipoAcao.REDUZIR) {
                Gramatica.Prod p = g.producao(a.valor);
                for (int k = 0; k < p.rhs.length; k++) pilha.pop();
                Integer destino = g.irPara(pilha.peek(), p.lhs);
                pilha.push(destino);
                reducoes++;
            } else { // ACEITAR
                System.out.println("\nPrograma sintaticamente VÁLIDO.");
                System.out.println("Tokens analisados: " + tokens.size()
                        + " | Reduções: " + reducoes
                        + " | Estados da tabela: " + g.numeroEstados());
                System.exit(0);
                return;
            }
        }
    }

    private static String descrever(Gramatica g, Gramatica.Acao a) {
        switch (a.tipo) {
            case EMPILHAR: return "empilhar " + a.valor;
            case REDUZIR: {
                Gramatica.Prod p = g.producao(a.valor);
                return "reduzir " + a.valor + " (" + p.lhs + " -> " + p.rhsTexto() + ")";
            }
            case ACEITAR: return "aceitar";
            default: return "erro";
        }
    }

    private static void reportarErro(Gramatica g, int estado, List<TokenSintatico> tokens,
                                     List<String> entrada, int i) {
        String simbolo = entrada.get(i);
        if (i < tokens.size()) {
            TokenSintatico t = tokens.get(i);
            System.out.println("\nERRO SINTÁTICO na linha " + t.linha + ", coluna " + t.coluna
                    + ": token inesperado '" + t.lexema + "' (" + t.classe + ").");
        } else {
            System.out.println("\nERRO SINTÁTICO: fim inesperado do arquivo.");
        }
        List<String> esperados = new ArrayList<String>();
        for (String t : g.terminais()) {
            Gramatica.Acao a = g.acao(estado, t);
            if (a != null && a.tipo != Gramatica.TipoAcao.ERRO) esperados.add("'" + t + "'");
        }
        if (!esperados.isEmpty()) {
            System.out.println("Terminais esperados neste ponto: " + join(esperados));
        }
        System.out.println("\nPrograma sintaticamente INVÁLIDO.");
    }

    // ------------------------------------------------------------------
    // Saída auxiliar
    // ------------------------------------------------------------------
    private static void imprimirTokens(List<Token> tokens) {
        System.out.println("===== TOKENS =====");
        System.out.println(String.format("%-6s %-6s %-20s %s", "LINHA", "COL", "CLASSE", "LEXEMA"));
        System.out.println(repetir('-', 60));
        for (Token t : tokens) {
            if (t.isErro()) {
                System.out.println(String.format("%-6d %-6d %-20s %s", t.linha, t.coluna,
                        "ERRO LÉXICO", "'" + t.lexema + "' (" + t.erro + ")"));
            } else {
                System.out.println(String.format("%-6d %-6d %-20s %s", t.linha, t.coluna,
                        t.classeNome(), t.lexema));
            }
        }
        System.out.println(repetir('-', 60));
        System.out.println();
    }

    private static void imprimirAjuda() {
        System.out.println("Analisador Sintático SLR(1) da linguagem Sigma");
        System.out.println();
        System.out.println("Uso: java AnalisadorSintatico [arquivo] [opções]");
        System.out.println();
        System.out.println("Opções:");
        System.out.println("  --fase F      executa apenas a fase F: lexico, sintatico ou ambas");
        System.out.println("  --tokens      imprime a tabela de tokens reconhecidos");
        System.out.println("  --gramatica   imprime produções, FIRST e FOLLOW");
        System.out.println("  --tabela      imprime a tabela SLR(1) e os conflitos resolvidos");
        System.out.println("  --passos      imprime o passo a passo da análise");
        System.out.println("  --automato    imprime a tabela de transições do AFND do léxico");
        System.out.println("  -h, --help    mostra esta ajuda");
        System.out.println();
        System.out.println("Sem arquivo, analisa um programa de exemplo embutido.");
        System.out.println("O parser SLR(1) é independente do léxico: consome apenas tokens");
        System.out.println("(TokenSintatico). O adaptador liga as duas fases na linha de comando.");
    }

    private static String repetir(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }

    private static String join(List<String> itens) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < itens.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(itens.get(i));
        }
        return sb.toString();
    }

    private AnalisadorSintatico() {
    }
}
