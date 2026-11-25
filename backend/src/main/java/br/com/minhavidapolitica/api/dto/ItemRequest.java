package br.com.minhavidapolitica.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Corpo comum de proposta (data = data de referência) e ação concretizada (data = data da ação). */
public record ItemRequest(
        @NotBlank(message = "O título é obrigatório") @Size(max = 200, message = "O título aceita até 200 caracteres") String titulo,
        @Size(max = 2000, message = "A descrição aceita até 2000 caracteres") String descricao,
        LocalDate data,
        @NotNull(message = "A fonte é obrigatória") Long fonteId,
        @Size(max = 500, message = "O link da fonte aceita até 500 caracteres") String linkFonte) {
}
