package br.com.minhavidapolitica.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Carga de candidaturas de uma eleição. Cada item é validado individualmente pelo serviço: um registro
 * inválido é rejeitado e contado sem derrubar a carga.
 */
public record ImportacaoCandidaturasRequest(
        @NotNull(message = "Selecione a fonte da importação") Long fonteId,
        @NotNull(message = "Informe o ano da eleição") @Min(value = 2000, message = "Ano da eleição inválido") Integer ano,
        @NotEmpty(message = "A lista de candidaturas não pode ser vazia")
        @Size(max = 2000, message = "Cada importação aceita até 2000 candidaturas") List<CandidaturaImportada> candidaturas) {

    public record CandidaturaImportada(
            String sqCandidato, String nome, String nomeUrna, String numero, String partido,
            String cargo, String uf, String situacao, String linkFonte,
            String fotoUrl, String planoUrl) {
    }
}
