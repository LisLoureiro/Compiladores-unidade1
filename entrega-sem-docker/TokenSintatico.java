/**
 * Token na forma consumida pelo analisador sintático.
 *
 * Esta classe é deliberadamente INDEPENDENTE do analisador léxico: carrega o
 * símbolo terminal da gramática (ex.: {@code id}, {@code num}, {@code begin},
 * {@code <=}) e os dados usados nas mensagens de erro. Qualquer fonte de
 * tokens — o léxico da linguagem, um arquivo de tokens ou outro front-end —
 * pode produzir instâncias desta classe.
 */
public final class TokenSintatico {

    /** Símbolo terminal da gramática (lado direito das produções). */
    public final String simbolo;

    /** Texto original reconhecido (usado nas mensagens de erro). */
    public final String lexema;

    /** Nome legível da classe léxica (apenas informativo). */
    public final String classe;

    /** Linha (1-based) onde o lexema começa. */
    public final int linha;

    /** Coluna (1-based) onde o lexema começa. */
    public final int coluna;

    public TokenSintatico(String simbolo, String lexema, String classe,
                          int linha, int coluna) {
        this.simbolo = simbolo;
        this.lexema = lexema;
        this.classe = classe;
        this.linha = linha;
        this.coluna = coluna;
    }
}
