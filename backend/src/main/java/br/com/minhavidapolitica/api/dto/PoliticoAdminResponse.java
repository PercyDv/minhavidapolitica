package br.com.minhavidapolitica.api.dto;

import br.com.minhavidapolitica.api.domain.Politico;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PoliticoAdminResponse(
        Long id, String nome, String partido, String fotoUrl, String resumo,
        Long cargoId, String cargoNome, Long ufId, String ufSigla,
        LocalDate inicioMandato, LocalDate fimMandato, boolean emExercicio,
        Long fonteId, String fonteNome, boolean fonteAprovada, String linkFonte,
        LocalDateTime atualizadoEm) {

    public static PoliticoAdminResponse de(Politico p) {
        return new PoliticoAdminResponse(
                p.getId(), p.getNome(), p.getPartido(), p.getFotoUrl(), p.getResumo(),
                p.getCargo().getId(), p.getCargo().getNome(),
                p.getUf() == null ? null : p.getUf().getId(),
                p.getUf() == null ? null : p.getUf().getSigla(),
                p.getInicioMandato(), p.getFimMandato(), p.isEmExercicio(),
                p.getFonte().getId(), p.getFonte().getNome(), p.getFonte().liberaExibicao(),
                p.getLinkFonte(), p.getAtualizadoEm());
    }
}
