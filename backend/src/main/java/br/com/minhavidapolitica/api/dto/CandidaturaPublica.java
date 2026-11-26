package br.com.minhavidapolitica.api.dto;

import br.com.minhavidapolitica.api.domain.Candidatura;

/** Candidatura exibida ao cidadão. Sem votos, sem nota, sem ranking (RN04, RF09). */
public record CandidaturaPublica(
        String nome,
        String nomeUrna,
        String numero,
        String partido,
        String cargo,
        String uf,
        String situacao,
        FonteInfo fonte) {

    public static CandidaturaPublica de(Candidatura c) {
        return new CandidaturaPublica(
                c.getNome(),
                c.getNomeUrna(),
                c.getNumero(),
                c.getPartido(),
                c.getCargo().getNome(),
                c.getUf() == null ? null : c.getUf().getSigla(),
                c.getSituacao().name(),
                FonteInfo.de(c.getFonte(), c.getLinkFonte(), c.getAtualizadoEm()));
    }
}
