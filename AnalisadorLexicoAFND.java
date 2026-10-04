import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 * ANALISADOR LÉXICO UTILIZANDO AFND
 *
 * Arquitetura: um AFND independente por classe léxica (sem transições-ε).
 * O scanner percorre o texto, recorta o maior lexema possível (maior
 * casamento) e usa o AFND correspondente para validá-lo.
 *
 * Classes léxicas:
 *
 * 1) Palavra Reservada
 * 2) Identificador
 * 3) Número Inteiro
 * 4) Número Real
 * 5) Operador Aritmético   (+ - * /  e  "mod")
 * 6) Operador Relacional   (= > >= < <= <>)
 * 7) Operador Lógico       ("and" "or" "not")
 * 8) Símbolo Especial      ( = ( ) , ; : )
 * 9) Atribuição            ( := )
 * 10) Fim                  ( . )
 */
public class AnalisadorLexicoAFND {

    // ============================================================
    // REPRESENTAÇÃO DE UMA TRANSIÇÃO
    // ============================================================

    static class Transicao {

        String origem;
        String simbolo;
        String destino;

        Transicao(String origem, String simbolo, String destino) {
            this.origem = origem;
            this.simbolo = simbolo;
            this.destino = destino;
        }
    }

    // ============================================================
    // REPRESENTAÇÃO DO AFND
    // ============================================================

    static class AFND {

        String estadoInicial;

        Set<String> estadosFinais;

        List<Transicao> transicoes;

        AFND(String estadoInicial) {
            this.estadoInicial = estadoInicial;
            this.estadosFinais = new HashSet<>();
            this.transicoes = new ArrayList<>();
        }

        // Adiciona um estado final
        void adicionarEstadoFinal(String estado) {
            estadosFinais.add(estado);
        }

        // Adiciona uma transição
        void adicionarTransicao(
                String origem,
                String simbolo,
                String destino) {

            transicoes.add(
                new Transicao(origem, simbolo, destino)
            );
        }

        // Retorna os possíveis estados alcançados
        // por uma determinada transição.
        //
        // "LETRA"  -> qualquer letra
        // "DIGITO" -> qualquer dígito
        // outro    -> o próprio caractere (comparação literal)
        Set<String> mover(
                Set<String> estadosAtuais,
                char simboloAtual) {

            Set<String> novosEstados = new HashSet<>();

            for (String estado : estadosAtuais) {

                for (Transicao transicao : transicoes) {

                    if (!transicao.origem.equals(estado)) {
                        continue;
                    }

                    boolean casa =
                        (transicao.simbolo.equals("LETRA")
                            && Character.isLetter(simboloAtual))
                        ||
                        (transicao.simbolo.equals("DIGITO")
                            && Character.isDigit(simboloAtual))
                        ||
                        transicao.simbolo.equals(
                            String.valueOf(simboloAtual)
                        );

                    if (casa) {
                        novosEstados.add(transicao.destino);
                    }
                }
            }

            return novosEstados;
        }

        // Executa o AFND sobre uma palavra
        boolean reconhecer(String palavra) {

            Set<String> estadosAtuais = new HashSet<>();

            estadosAtuais.add(estadoInicial);

            for (int i = 0; i < palavra.length(); i++) {

                char simbolo = palavra.charAt(i);

                estadosAtuais =
                    mover(estadosAtuais, simbolo);

                // Não existe transição possível
                if (estadosAtuais.isEmpty()) {
                    return false;
                }
            }

            // Verifica se algum estado atual é final
            for (String estado : estadosAtuais) {

                if (estadosFinais.contains(estado)) {
                    return true;
                }
            }

            return false;
        }

        // Imprime a tabela de transições deste AFND
        void imprimir(String nome) {

            System.out.println(
                "AFND " + nome + "  (inicial " + estadoInicial + ")"
            );

            for (Transicao transicao : transicoes) {

                System.out.println(
                    "  d(" + transicao.origem
                    + ", " + transicao.simbolo
                    + ") = " + transicao.destino
                );
            }

            System.out.print("  Finais:");

            for (String finalState : estadosFinais) {
                System.out.print(" " + finalState);
            }

            System.out.println();
        }
    }

    // ============================================================
    // AFND PARA IDENTIFICADORES
    // ============================================================

    static AFND criarAFNDIdentificador() {

        AFND afnd = new AFND("q0");

        /*
         * q0 --LETRA--> q1
         * q1 --LETRA--> q1
         * q1 --DIGITO-> q1
         */

        afnd.adicionarTransicao(
            "q0", "LETRA", "q1"
        );

        afnd.adicionarTransicao(
            "q1", "LETRA", "q1"
        );

        afnd.adicionarTransicao(
            "q1", "DIGITO", "q1"
        );

        // q1 é estado final
        afnd.adicionarEstadoFinal("q1");

        return afnd;
    }

    // ============================================================
    // AFND PARA NÚMERO INTEIRO
    // ============================================================

    static AFND criarAFNDInteiro() {

        AFND afnd = new AFND("q0");

        /*
         * q0 --DIGITO--> q1
         * q1 --DIGITO--> q1
         */

        afnd.adicionarTransicao(
            "q0", "DIGITO", "q1"
        );

        afnd.adicionarTransicao(
            "q1", "DIGITO", "q1"
        );

        afnd.adicionarEstadoFinal("q1");

        return afnd;
    }

    // ============================================================
    // AFND PARA NÚMERO REAL
    // ============================================================

    static AFND criarAFNDReal() {

        AFND afnd = new AFND("q0");

        /*
         * Parte inteira:
         *
         * q0 --DIGITO--> q1
         * q1 --DIGITO--> q1
         *
         * Parte decimal:
         *
         * q1 --"."-----> q2
         * q2 --DIGITO--> q3
         * q3 --DIGITO--> q3
         *
         * Expoente:
         *
         * q3 --e/E-----> q4
         * q4 --+/- ----> q5
         * q4 --DIGITO-> q6
         * q5 --DIGITO-> q6
         * q6 --DIGITO-> q6
         */

        afnd.adicionarTransicao(
            "q0", "DIGITO", "q1"
        );

        afnd.adicionarTransicao(
            "q1", "DIGITO", "q1"
        );

        afnd.adicionarTransicao(
            "q1", ".", "q2"
        );

        afnd.adicionarTransicao(
            "q2", "DIGITO", "q3"
        );

        afnd.adicionarTransicao(
            "q3", "DIGITO", "q3"
        );

        // Expoente com e
        afnd.adicionarTransicao(
            "q3", "e", "q4"
        );

        // Expoente com E
        afnd.adicionarTransicao(
            "q3", "E", "q4"
        );

        // Sinal positivo ou negativo
        afnd.adicionarTransicao(
            "q4", "+", "q5"
        );

        afnd.adicionarTransicao(
            "q4", "-", "q5"
        );

        // Expoente sem sinal
        afnd.adicionarTransicao(
            "q4", "DIGITO", "q6"
        );

        afnd.adicionarTransicao(
            "q5", "DIGITO", "q6"
        );

        afnd.adicionarTransicao(
            "q6", "DIGITO", "q6"
        );

        // Número real sem expoente
        afnd.adicionarEstadoFinal("q3");

        // Número real com expoente
        afnd.adicionarEstadoFinal("q6");

        return afnd;
    }

    // ============================================================
    // AFND PARA ATRIBUIÇÃO
    // ============================================================

    static AFND criarAFNDAtribuicao() {

        AFND afnd = new AFND("q0");

        /*
         * q0 --":"--> q1
         * q1 --"="--> q2   (final)
         */

        afnd.adicionarTransicao(
            "q0", ":", "q1"
        );

        afnd.adicionarTransicao(
            "q1", "=", "q2"
        );

        afnd.adicionarEstadoFinal("q2");

        return afnd;
    }

    // ============================================================
    // AFND PARA OPERADORES RELACIONAIS
    // ============================================================

    static AFND criarAFNDRelacional() {

        AFND afnd = new AFND("q0");

        /*
         * Operadores:
         *
         * =
         * >
         * >=
         * <
         * <=
         * <>
         */

        afnd.adicionarTransicao(
            "q0", "=", "q1"
        );

        afnd.adicionarTransicao(
            "q0", ">", "q2"
        );

        afnd.adicionarTransicao(
            "q0", "<", "q3"
        );

        afnd.adicionarTransicao(
            "q2", "=", "q4"
        );

        afnd.adicionarTransicao(
            "q3", "=", "q5"
        );

        afnd.adicionarTransicao(
            "q3", ">", "q6"
        );

        // Estados finais
        afnd.adicionarEstadoFinal("q1"); // =
        afnd.adicionarEstadoFinal("q2"); // >
        afnd.adicionarEstadoFinal("q3"); // <
        afnd.adicionarEstadoFinal("q4"); // >=
        afnd.adicionarEstadoFinal("q5"); // <=
        afnd.adicionarEstadoFinal("q6"); // <>

        return afnd;
    }

    // ============================================================
    // AFND PARA OPERADORES ARITMÉTICOS
    // ============================================================

    static AFND criarAFNDAritmetico() {

        AFND afnd = new AFND("q0");

        /*
         * q0 -- aritmetico --> q1  (final)
         * Aceita: +  -  *  /
         */

        String[] operadores = {
            "+", "-", "*", "/"
        };

        for (String operador : operadores) {

            afnd.adicionarTransicao(
                "q0", operador, "q1"
            );
        }

        afnd.adicionarEstadoFinal("q1");

        return afnd;
    }

    // ============================================================
    // AFND PARA SÍMBOLOS ESPECIAIS
    // ============================================================

    static AFND criarAFNDSimboloEspecial() {

        AFND afnd = new AFND("q0");

        String[] simbolos = {
            "=", "(", ")", ",", ";", ":"
        };

        for (String simbolo : simbolos) {

            afnd.adicionarTransicao(
                "q0", simbolo, "q1"
            );
        }

        afnd.adicionarEstadoFinal("q1");

        return afnd;
    }

    // ============================================================
    // AFND PARA FIM
    // ============================================================

    static AFND criarAFNDFim() {

        AFND afnd = new AFND("q0");

        afnd.adicionarTransicao(
            "q0", ".", "q1"
        );

        afnd.adicionarEstadoFinal("q1");

        return afnd;
    }

    // ============================================================
    // PALAVRAS / OPERADORES ESCRITOS COMO PALAVRAS
    // ============================================================

    static Set<String> palavrasReservadas =
        new HashSet<>(Arrays.asList(
            "program",
            "var",
            "integer",
            "real",
            "begin",
            "end",
            "if",
            "then",
            "else",
            "while",
            "do",
            "read",
            "write"
        ));

    static Set<String> operadoresLogicos =
        new HashSet<>(Arrays.asList(
            "and",
            "or",
            "not"
        ));

    static Set<String> operadoresAritmeticosPalavra =
        new HashSet<>(Arrays.asList(
            "mod"
        ));

    // ============================================================
    // CONTADORES
    // ============================================================

    static int totalTokens = 0;
    static int errosLexicos = 0;

    // ============================================================
    // ANALISADOR LÉXICO
    // ============================================================

    // Operadores de dois caracteres, na ordem em que são testados.
    static final String[] OPERADORES_DOIS_CARACTERES = {
        ":=", ">=", "<=", "<>"
    };

    static void analisar(String codigo) {

        AFND afndIdentificador = criarAFNDIdentificador();
        AFND afndInteiro = criarAFNDInteiro();
        AFND afndReal = criarAFNDReal();
        AFND afndAtribuicao = criarAFNDAtribuicao();
        AFND afndRelacional = criarAFNDRelacional();
        AFND afndAritmetico = criarAFNDAritmetico();
        AFND afndSimboloEspecial = criarAFNDSimboloEspecial();
        AFND afndFim = criarAFNDFim();

        totalTokens = 0;
        errosLexicos = 0;

        System.out.printf(
            "%-6s %-6s %-20s %s%n",
            "LINHA", "COL", "CLASSE", "LEXEMA"
        );
        System.out.println(
            "------------------------------------------------------------"
        );

        int i = 0;
        int linha = 1;
        int coluna = 1;
        int n = codigo.length();

        while (i < n) {

            char c = codigo.charAt(i);

            // Ignorar quebras de linha
            if (c == '\n') {
                linha++;
                coluna = 1;
                i++;
                continue;
            }

            // Ignorar demais espaços
            if (Character.isWhitespace(c)) {
                coluna++;
                i++;
                continue;
            }

            // ====================================================
            // PALAVRAS / IDENTIFICADORES
            // ====================================================

            if (Character.isLetter(c)) {

                int inicio = i;

                while (
                    i < n
                    &&
                    (
                        Character.isLetter(codigo.charAt(i))
                        ||
                        Character.isDigit(codigo.charAt(i))
                    )
                ) {
                    i++;
                }

                String lexema = codigo.substring(inicio, i);

                if (palavrasReservadas.contains(lexema)) {

                    imprimir(lexema, "PALAVRA_RESERVADA", linha, coluna);

                } else if (operadoresLogicos.contains(lexema)) {

                    imprimir(lexema, "OPERADOR_LOGICO", linha, coluna);

                } else if (
                    operadoresAritmeticosPalavra.contains(lexema)
                ) {

                    imprimir(lexema, "OPERADOR_ARITMETICO", linha, coluna);

                } else if (afndIdentificador.reconhecer(lexema)) {

                    imprimir(lexema, "IDENTIFICADOR", linha, coluna);

                } else {

                    imprimir(lexema, "ERRO_LEXICO", linha, coluna);
                }

                coluna += lexema.length();
                continue;
            }

            // ====================================================
            // NÚMEROS
            // ====================================================
            //
            // Maior casamento para o número:
            //   dígitos ( "." dígitos ( (e|E) (+|-)? dígitos )? )?
            //
            // Importante: o "+" e o "-" só entram no lexema quando
            // formam o expoente. Assim "1+2" gera três tokens
            // (1, +, 2) e "10-3" gera (10, -, 3).

            if (Character.isDigit(c)) {

                int inicio = i;

                // Parte inteira
                while (i < n && Character.isDigit(codigo.charAt(i))) {
                    i++;
                }

                // Parte decimal: só se houver um dígito depois do ponto
                if (
                    i + 1 < n
                    && codigo.charAt(i) == '.'
                    && Character.isDigit(codigo.charAt(i + 1))
                ) {

                    i++; // consome o '.'

                    while (i < n && Character.isDigit(codigo.charAt(i))) {
                        i++;
                    }

                    // Expoente opcional: (e|E) (+|-)? dígito+
                    if (
                        i < n
                        && (codigo.charAt(i) == 'e'
                            || codigo.charAt(i) == 'E')
                    ) {

                        int j = i + 1;

                        if (
                            j < n
                            && (codigo.charAt(j) == '+'
                                || codigo.charAt(j) == '-')
                        ) {
                            j++;
                        }

                        // Só consome o expoente se vier ao menos um dígito
                        if (j < n && Character.isDigit(codigo.charAt(j))) {

                            i = j;

                            while (i < n
                                    && Character.isDigit(codigo.charAt(i))) {
                                i++;
                            }
                        }
                    }
                }

                String lexema = codigo.substring(inicio, i);

                if (afndReal.reconhecer(lexema)) {

                    imprimir(lexema, "NUMERO_REAL", linha, coluna);

                } else if (afndInteiro.reconhecer(lexema)) {

                    imprimir(lexema, "NUMERO_INTEIRO", linha, coluna);

                } else {

                    imprimir(lexema, "ERRO_LEXICO", linha, coluna);
                }

                coluna += lexema.length();
                continue;
            }

            // ====================================================
            // OPERADORES DE DOIS CARACTERES (: = > < ! & |)
            // ====================================================

            boolean consumiuDois = false;

            if (i + 1 < n) {

                String dois = codigo.substring(i, i + 2);

                for (String operador : OPERADORES_DOIS_CARACTERES) {

                    if (!operador.equals(dois)) {
                        continue;
                    }

                    if (afndAtribuicao.reconhecer(operador)) {

                        imprimir(operador, "ATRIBUICAO", linha, coluna);

                    } else if (afndRelacional.reconhecer(operador)) {

                        imprimir(operador, "OPERADOR_RELACIONAL", linha, coluna);

                    } else {

                        imprimir(operador, "ERRO_LEXICO", linha, coluna);
                    }

                    i += 2;
                    coluna += 2;
                    consumiuDois = true;
                    break;
                }
            }

            if (consumiuDois) {
                continue;
            }

            // ====================================================
            // OPERADORES DE UM CARACTER
            // ====================================================

            String operador = String.valueOf(c);

            if (afndRelacional.reconhecer(operador)) {

                imprimir(operador, "OPERADOR_RELACIONAL", linha, coluna);

            } else if (afndAtribuicao.reconhecer(operador)) {

                imprimir(operador, "ATRIBUICAO", linha, coluna);

            } else if (afndAritmetico.reconhecer(operador)) {

                imprimir(operador, "OPERADOR_ARITMETICO", linha, coluna);

            } else if (afndSimboloEspecial.reconhecer(operador)) {

                imprimir(operador, "SIMBOLO_ESPECIAL", linha, coluna);

            } else if (afndFim.reconhecer(operador)) {

                imprimir(operador, "FIM", linha, coluna);

            } else {

                imprimir(operador, "ERRO_LEXICO", linha, coluna);
            }

            i++;
            coluna++;
        }

        System.out.println(
            "------------------------------------------------------------"
        );

        System.out.println(
            "Total de tokens: " + totalTokens
            + " | Erros léxicos: " + errosLexicos
        );
    }

    // ============================================================
    // IMPRESSÃO
    // ============================================================

    static void imprimir(
            String lexema,
            String classe,
            int linha,
            int coluna) {

        if (classe.equals("ERRO_LEXICO")) {

            errosLexicos++;

            System.out.printf(
                "%-6d %-6d %-20s %s%n",
                linha,
                coluna,
                "ERRO LÉXICO",
                "'" + lexema + "' (caractere não reconhecido)"
            );

        } else {

            totalTokens++;

            System.out.printf(
                "%-6d %-6d %-20s %s%n",
                linha,
                coluna,
                classe,
                lexema
            );
        }
    }

    // ============================================================
    // PROGRAMA DE EXEMPLO
    // ============================================================

    static final String EXEMPLO =
        "program exemplo;\n" +
        "var x: integer;\n" +
        "    y: real;\n" +
        "begin\n" +
        "  x := 13 mod 4;\n" +
        "  y := 24.40e-04 * 1.33 / (x + 1);\n" +
        "  if x >= 1 and not (y < 2.5) then\n" +
        "    x := x + 1;\n" +
        "  if x = 0 or y <> 2.5 then\n" +
        "    x := x - 1;\n" +
        "  read(x);\n" +
        "  write(y);\n" +
        "end.\n";

    // ============================================================
    // MAIN
    // ============================================================

    public static void main(String[] args) throws IOException {

        // --afnd: imprime a tabela de transições de cada AFND
        if (args.length > 0 && args[0].equals("--afnd")) {

            criarAFNDIdentificador().imprimir("Identificador");
            criarAFNDInteiro().imprimir("Inteiro");
            criarAFNDReal().imprimir("Real");
            criarAFNDAtribuicao().imprimir("Atribuicao");
            criarAFNDRelacional().imprimir("Relacional");
            criarAFNDAritmetico().imprimir("Aritmetico");
            criarAFNDSimboloEspecial().imprimir("SimboloEspecial");
            criarAFNDFim().imprimir("Fim");

            return;
        }

        String codigo;

        if (args.length > 0) {

            try {
                codigo = new String(
                    Files.readAllBytes(Paths.get(args[0])),
                    StandardCharsets.UTF_8
                );
            } catch (IOException e) {
                System.err.println(
                    "Erro ao ler o arquivo '"
                    + args[0] + "': " + e.getMessage()
                );
                System.exit(2);
                return;
            }

        } else {

            codigo = EXEMPLO;

            System.out.println(
                "Nenhum arquivo informado. "
                + "Analisando o programa de exemplo:\n"
            );
            System.out.println(codigo);
        }

        System.out.println(
            "========== ANALISADOR LÉXICO AFND ==========\n"
        );
        System.out.println("Código analisado:\n");
        System.out.println(codigo);
        System.out.println(
            "\n========== TOKENS ==========\n"
        );

        analisar(codigo);

        if (errosLexicos > 0) {
            System.exit(1);
        }
    }
}
