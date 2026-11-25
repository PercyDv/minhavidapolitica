package br.com.minhavidapolitica.api.dto;

/** Dados do usuário autenticado, usados pelo painel para montar o menu. */
public record SessaoResponse(String username, String nome, String perfil) {
}
