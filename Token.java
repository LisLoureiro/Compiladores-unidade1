/**
 * Um token produzido pelo analisador léxico.
 *
 * Quando um lexema não é reconhecido, o token tem {@link #tipo} igual a
 * {@code null} e {@link #erro} preenchido com a mensagem adequada.
 */
public final class Token {

    /** Classe léxica. {@code null} indica erro léxico. */
    public final TipoToken tipo;

    /** Texto reconhecido (lexema). */
    public final String lexema;

    /** Linha (1-based) onde o lexema começa. */
    public final int linha;

    /** Coluna (1-based) onde o lexema começa. */
    public final int coluna;

    /** Mensagem de erro léxico, ou {@code null} se o token é válido. */
    public final String erro;

    private Token(TipoToken tipo, String lexema, int linha, int coluna, String erro) {
        this.tipo = tipo;
        this.lexema = lexema;
        this.linha = linha;
        this.coluna = coluna;
        this.erro = erro;
    }

    /** Cria um token válido. */
    public static Token valido(TipoToken tipo, String lexema, int linha, int coluna) {
        return new Token(tipo, lexema, linha, coluna, null);
    }

    /** Cria um token de erro léxico. */
    public static Token erro(String lexema, int linha, int coluna, String mensagem) {
        return new Token(null, lexema, linha, coluna, mensagem);
    }

    /** Indica se o token representa um erro léxico. */
    public boolean isErro() {
        return tipo == null;
    }

    /** Nome da classe léxica (ou "ERRO" se for erro). */
    public String classeNome() {
        return tipo == null ? "ERRO" : tipo.nome;
    }

    @Override
    public String toString() {
        return tipo == null ? "ERRO('" + lexema + "')" : tipo.name() + "('" + lexema + "')";
    }
}
