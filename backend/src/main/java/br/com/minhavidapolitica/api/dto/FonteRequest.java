package br.com.minhavidapolitica.api.dto;

import br.com.minhavidapolitica.api.domain.TipoFonte;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FonteRequest(
        @NotBlank(message = "O nome é obrigatório") @Size(max = 150, message = "O nome aceita até 150 caracteres") String nome,
        @NotBlank(message = "A URL é obrigatória") @Size(max = 500, message = "A URL aceita até 500 caracteres") String url,
        @NotNull(message = "O tipo é obrigatório") TipoFonte tipo,
        @Size(max = 60, message = "A periodicidade aceita até 60 caracteres") String periodicidade,
        @Size(max = 500, message = "A observação aceita até 500 caracteres") String observacao,
        Boolean ativa) {
}
