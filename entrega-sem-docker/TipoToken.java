/**
 * Classes léxicas (tipos de token) da linguagem Sigma.
 *
 * A ORDEM de declaração define a PRIORIDADE usada pelo analisador léxico
 * quando um mesmo lexema é aceito por mais de uma classe (regra do maior
 * casamento + desempate por prioridade). Por exemplo, "program" é aceito
 * tanto como palavra reservada quanto como identificador; como
 * PALAVRA_RESERVADA vem antes de IDENTIFICADOR, o token é classificado
 * como palavra reservada.
 */
public enum TipoToken {

    /** Palavras reservadas: program, var, integer, boolean, begin, end, ... */
    PALAVRA_RESERVADA("Palavra Reservada"),

    /** Operador de atribuição ":=". */
    ATRIBUICAO("Atribuição"),

    /** Operadores relacionais: = &lt; &lt;= &gt; &gt;= &lt;&gt; */
    OPERADOR_RELACIONAL("Operador Relacional"),

    /** Operadores lógicos escritos como palavra: and, or, not. */
    OPERADOR_LOGICO("Operador Lógico"),

    /** Operadores aritméticos: + - * / e a palavra-chave mod. */
    OPERADOR_ARITMETICO("Operador Aritmético"),

    /** Símbolos especiais: ( ) , ; : */
    SIMBOLO_ESPECIAL("Símbolo Especial"),

    /** Literal de cadeia (string) entre aspas duplas. */
    STRING("String"),

    /** Número inteiro (sequência de dígitos). */
    NUMERO("Número"),

    /** Número real (ex.: 1.33, 24.40e-04). */
    NUMERO_REAL("Número Real"),

    /** Identificador. */
    IDENTIFICADOR("Identificador"),

    /** Marcador de fim de programa ".". */
    FIM("Fim");

    /** Nome legível da classe, usado nas mensagens e na tabela de tokens. */
    public final String nome;

    TipoToken(String nome) {
        this.nome = nome;
    }
}
