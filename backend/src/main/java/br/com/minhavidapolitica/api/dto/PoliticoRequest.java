package br.com.minhavidapolitica.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PoliticoRequest(
        @NotBlank(message = "O nome é obrigatório") @Size(max = 150, message = "O nome aceita até 150 caracteres") String nome,
        @Size(max = 30, message = "O partido aceita até 30 caracteres") String partido,
        @Size(max = 500, message = "A URL da foto aceita até 500 caracteres") String fotoUrl,
        @Size(max = 1000, message = "O resumo aceita até 1000 caracteres") String resumo,
        @NotNull(message = "O cargo é obrigatório") Long cargoId,
        Long ufId,
        LocalDate inicioMandato,
        LocalDate fimMandato,
        Boolean emExercicio,
        @NotNull(message = "A fonte é obrigatória") Long fonteId,
        @Size(max = 500, message = "O link da fonte aceita até 500 caracteres") String linkFonte) {
}
