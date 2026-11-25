package br.com.minhavidapolitica.api.dto;

import br.com.minhavidapolitica.api.domain.Politico;
import java.time.LocalDate;

/** Perfil resumido (RF07). Somente fatos: sem nota, ranking ou adjetivo (RF09, RN04). */
public record PoliticoPublico(
        Long id,
        String nome,
        String cargo,
        String partido,
        String uf,
        String fotoUrl,
        String resumo,
        LocalDate inicioMandato,
        LocalDate fimMandato,
        FonteInfo fonte) {

    public static PoliticoPublico de(Politico p) {
        return new PoliticoPublico(
                p.getId(),
                p.getNome(),
                p.getCargo().getNome(),
                p.getPartido(),
                p.getUf() == null ? null : p.getUf().getSigla(),
                p.getFotoUrl(),
                p.getResumo(),
                p.getInicioMandato(),
                p.getFimMandato(),
                FonteInfo.de(p.getFonte(), p.getLinkFonte(), p.getAtualizadoEm()));
    }
}
