package br.com.minhavidapolitica.api.dto;

import br.com.minhavidapolitica.api.domain.AcaoConcretizada;
import br.com.minhavidapolitica.api.domain.Proposta;
import java.time.LocalDate;

/** Proposta (RF05) ou ação concretizada (RF06) exibida ao cidadão, sempre com fonte e data. */
public record ItemPublico(Long id, String titulo, String descricao, LocalDate data, FonteInfo fonte) {

    public static ItemPublico de(Proposta p) {
        return new ItemPublico(p.getId(), p.getTitulo(), p.getDescricao(), p.getDataReferencia(),
                FonteInfo.de(p.getFonte(), p.getLinkFonte(), p.getAtualizadoEm()));
    }

    public static ItemPublico de(AcaoConcretizada a) {
        return new ItemPublico(a.getId(), a.getTitulo(), a.getDescricao(), a.getDataAcao(),
                FonteInfo.de(a.getFonte(), a.getLinkFonte(), a.getAtualizadoEm()));
    }
}
