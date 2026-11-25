package br.com.minhavidapolitica.api.dto;

import br.com.minhavidapolitica.api.domain.AcaoConcretizada;
import br.com.minhavidapolitica.api.domain.Proposta;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ItemAdminResponse(
        Long id, Long politicoId, String titulo, String descricao, LocalDate data,
        Long fonteId, String fonteNome, boolean fonteAprovada, String linkFonte,
        LocalDateTime atualizadoEm) {

    public static ItemAdminResponse de(Proposta p) {
        return new ItemAdminResponse(p.getId(), p.getPolitico().getId(), p.getTitulo(), p.getDescricao(),
                p.getDataReferencia(), p.getFonte().getId(), p.getFonte().getNome(),
                p.getFonte().liberaExibicao(), p.getLinkFonte(), p.getAtualizadoEm());
    }

    public static ItemAdminResponse de(AcaoConcretizada a) {
        return new ItemAdminResponse(a.getId(), a.getPolitico().getId(), a.getTitulo(), a.getDescricao(),
                a.getDataAcao(), a.getFonte().getId(), a.getFonte().getNome(),
                a.getFonte().liberaExibicao(), a.getLinkFonte(), a.getAtualizadoEm());
    }
}
