import java.util.ArrayList;
import java.util.List;

/**
 * Converte os tokens do analisador léxico ({@link Token}/{@link TipoToken})
 * para a forma neutra {@link TokenSintatico} usada pelo parser.
 *
 * Este é o ÚNICO ponto do projeto que conhece as duas representações. O
 * analisador sintático recebe apenas {@link TokenSintatico}, de modo que
 * pode ser usado com qualquer fonte de tokens.
 */
public final class AdaptadorLexico {

    private AdaptadorLexico() {
    }

    /** Converte os tokens válidos do léxico para a entrada do parser. */
    public static List<TokenSintatico> converter(List<Token> tokens) {
        List<TokenSintatico> saida = new ArrayList<TokenSintatico>();
        for (Token t : tokens) {
            if (t.isErro()) continue;
            saida.add(new TokenSintatico(
                    terminalDe(t), t.lexema, t.classeNome(), t.linha, t.coluna));
        }
        return saida;
    }

    /** Mapeia a classe léxica para o terminal correspondente na gramática. */
    private static String terminalDe(Token t) {
        switch (t.tipo) {
            case PALAVRA_RESERVADA:  return t.lexema.toLowerCase();
            case IDENTIFICADOR:      return "id";
            case NUMERO:             return "num";
            case STRING:             return "str";
            case ATRIBUICAO:         return ":=";
            case OPERADOR_RELACIONAL:
            case OPERADOR_ARITMETICO:
            case SIMBOLO_ESPECIAL:
            case FIM:                return t.lexema;
            default:                 return t.lexema;
        }
    }
}
