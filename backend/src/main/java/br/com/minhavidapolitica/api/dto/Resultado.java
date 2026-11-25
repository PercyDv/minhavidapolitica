package br.com.minhavidapolitica.api.dto;

/**
 * Envelope das consultas públicas. Quando não há dado em fonte validada, "disponivel" é false e
 * "mensagem" traz o texto neutro do RF12, em vez de campo vazio ou dado não verificado.
 */
public record Resultado<T>(boolean disponivel, String mensagem, T dados) {

    public static final String MENSAGEM_INDISPONIVEL = "Informação não disponível nas fontes oficiais consultadas";

    public static <T> Resultado<T> com(T dados) {
        return new Resultado<>(true, null, dados);
    }

    public static <T> Resultado<T> indisponivel() {
        return new Resultado<>(false, MENSAGEM_INDISPONIVEL, null);
    }
}
