/**
 * Classes léxicas da linguagem do Trabalho Prático da I Unidade.
 *
 * A ORDEM de declaração define a PRIORIDADE usada para desempatar quando um
 * mesmo lexema é aceito por mais de uma classe (o analisador aplica a regra do
 * maior casamento e, em caso de empate, vence a classe declarada primeiro).
 * Casos de empate previstos nesta linguagem:
 *
 *   - palavra reservada (ex.: "read") x identificador  -> palavra reservada;
 *   - operadores escritos como palavras ("and", "or", "not", "mod") x
 *     identificador                                    -> operador;
 *   - "=" aceito por Operador Relacional e por Símbolo Especial -> relacional
 *     (o relacional vem antes do símbolo especial).
 */
public enum TipoToken {

    /** 1) Palavra Reservada. */
    PALAVRA_RESERVADA("Palavra Reservada"),

    /** 5) Operador Aritmético (símbolos "+ - * /" e a palavra "mod"). */
    OPERADOR_ARITMETICO("Operador Aritmético"),

    /** 7) Operador Lógico ("and", "or", "not"). */
    OPERADOR_LOGICO("Operador Lógico"),

    /** 6) Operador Relacional ("=", ">=", ">", "<", "<=", "<>"). */
    OPERADOR_RELACIONAL("Operador Relacional"),

    /** 8) Símbolo Especial ("=", "(", ")", ",", ";", ":"). */
    SIMBOLO_ESPECIAL("Símbolo Especial"),

    /** 9) Atribuição (":="). */
    ATRIBUICAO("Atribuição"),

    /** 4) Número Real (ex.: 1.33, 24.40e-04). */
    NUMERO_REAL("Número Real"),

    /** 3) Número Inteiro (ex.: 1, 13). */
    NUMERO_INTEIRO("Número Inteiro"),

    /** 10) Fim ("."). */
    FIM("Fim"),

    /** 2) Identificador (ex.: x, variavel, i, var2). */
    IDENTIFICADOR("Identificador");

    /** Nome legível da classe, usado nas mensagens e na tabela de tokens. */
    public final String nome;

    TipoToken(String nome) {
        this.nome = nome;
    }
}
