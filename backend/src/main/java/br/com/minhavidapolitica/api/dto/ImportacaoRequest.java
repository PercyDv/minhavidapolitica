package br.com.minhavidapolitica.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * Carga de dados de uma fonte (RF13). Os itens internos são validados um a um pelo serviço,
 * para que um registro inválido seja rejeitado e contabilizado sem derrubar a carga inteira.
 */
public record ImportacaoRequest(
        @NotNull(message = "Selecione a fonte da importação") Long fonteId,
        @NotEmpty(message = "A lista de políticos não pode ser vazia")
        @Size(max = 500, message = "Cada importação aceita até 500 políticos") List<ImportacaoPolitico> politicos) {

    public record ImportacaoPolitico(
            String nome, String partido, String fotoUrl, String resumo,
            String cargo, String uf,
            LocalDate inicioMandato, LocalDate fimMandato,
            String linkFonte,
            List<ImportacaoItem> propostas,
            List<ImportacaoItem> acoes) {
    }

    public record ImportacaoItem(String titulo, String descricao, LocalDate data, String linkFonte) {
    }
}
