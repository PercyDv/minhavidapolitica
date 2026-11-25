package br.com.minhavidapolitica.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Registro formal da aprovação do cliente (RN05, RF13). A data padrão é hoje. */
public record AprovacaoRequest(
        @NotBlank(message = "Informe quem registrou a aprovação") @Size(max = 120, message = "O nome aceita até 120 caracteres") String aprovadaPor,
        LocalDate dataAprovacao,
        @Size(max = 500, message = "A observação aceita até 500 caracteres") String observacao) {
}
