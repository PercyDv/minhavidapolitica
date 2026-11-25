package br.com.minhavidapolitica.api.dto;

import br.com.minhavidapolitica.api.domain.FonteDados;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record FonteResponse(
        Long id, String nome, String url, String tipo, String periodicidade,
        boolean aprovada, LocalDate dataAprovacao, String aprovadaPor, String observacao,
        boolean ativa, LocalDateTime criadoEm) {

    public static FonteResponse de(FonteDados f) {
        return new FonteResponse(f.getId(), f.getNome(), f.getUrl(), f.getTipo().name(), f.getPeriodicidade(),
                f.isAprovada(), f.getDataAprovacao(), f.getAprovadaPor(), f.getObservacao(),
                f.isAtiva(), f.getCriadoEm());
    }
}
