package br.com.minhavidapolitica.api.dto;

import br.com.minhavidapolitica.api.domain.UnidadeFederativa;

/** Item do seletor de estado (RF02). */
public record UfResumo(Long id, String sigla, String nome, String regiao) {

    public static UfResumo de(UnidadeFederativa uf) {
        return new UfResumo(uf.getId(), uf.getSigla(), uf.getNome(), uf.getRegiao());
    }
}
