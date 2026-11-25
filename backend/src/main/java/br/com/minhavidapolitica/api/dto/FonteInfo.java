package br.com.minhavidapolitica.api.dto;

import br.com.minhavidapolitica.api.domain.FonteDados;
import java.time.LocalDateTime;

/** Bloco de transparência exigido pelo RF08: nome da fonte, link para o original e data de atualização. */
public record FonteInfo(String nome, String link, LocalDateTime atualizadoEm) {

    public static FonteInfo de(FonteDados fonte, String linkEspecifico, LocalDateTime atualizadoEm) {
        String link = (linkEspecifico == null || linkEspecifico.isBlank()) ? fonte.getUrl() : linkEspecifico;
        return new FonteInfo(fonte.getNome(), link, atualizadoEm);
    }
}
