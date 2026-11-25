package br.com.minhavidapolitica.api.dto;

/** Página estadual: dados da UF e o governador em exercício (RF03). */
public record UfPublica(String sigla, String nome, String regiao, Resultado<PoliticoPublico> governador) {
}
