package br.com.minhavidapolitica.api.dto;

import br.com.minhavidapolitica.api.domain.Cargo;
import br.com.minhavidapolitica.api.domain.FonteDados;
import java.util.List;

/** Listas de apoio aos formulários do painel (selects de UF, cargo e fonte). */
public record CatalogoResponse(List<UfResumo> ufs, List<CargoResumo> cargos, List<FonteResumo> fontes) {

    public record CargoResumo(Long id, String codigo, String nome, String esfera) {
        public static CargoResumo de(Cargo c) {
            return new CargoResumo(c.getId(), c.getCodigo(), c.getNome(), c.getEsfera().name());
        }
    }

    public record FonteResumo(Long id, String nome, boolean aprovada, boolean ativa) {
        public static FonteResumo de(FonteDados f) {
            return new FonteResumo(f.getId(), f.getNome(), f.isAprovada(), f.isAtiva());
        }
    }
}
