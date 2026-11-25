package br.com.minhavidapolitica.api.dto;

/** Situação de dados exibíveis por UF. situacao: COMPLETO, PARCIAL ou SEM_DADOS. */
public record CoberturaUf(String sigla, String nome, String governador, int representantes,
                          int comPropostas, int comAcoes, String situacao) {
}
