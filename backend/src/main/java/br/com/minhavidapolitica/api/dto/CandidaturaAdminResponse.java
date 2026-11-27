package br.com.minhavidapolitica.api.dto;

import br.com.minhavidapolitica.api.domain.Candidatura;
import java.time.LocalDateTime;

public record CandidaturaAdminResponse(
        Long id, int anoEleicao, String sqCandidato, String numero, String nome, String nomeUrna,
        String partido, String cargo, String uf, String situacao,
        Long fonteId, String fonteNome, boolean fonteAprovada, LocalDateTime atualizadoEm,
        String fotoUrl, String planoUrl) {

    public static CandidaturaAdminResponse de(Candidatura c) {
        return new CandidaturaAdminResponse(
                c.getId(), c.getAnoEleicao(), c.getSqCandidato(), c.getNumero(), c.getNome(), c.getNomeUrna(),
                c.getPartido(), c.getCargo().getCodigo(), c.getUf() == null ? null : c.getUf().getSigla(),
                c.getSituacao().name(), c.getFonte().getId(), c.getFonte().getNome(),
                c.getFonte().liberaExibicao(), c.getAtualizadoEm(),
                c.getFotoUrl(), c.getPlanoUrl());
    }
}
