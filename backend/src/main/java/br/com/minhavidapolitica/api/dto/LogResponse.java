package br.com.minhavidapolitica.api.dto;

import br.com.minhavidapolitica.api.domain.LogAtualizacao;
import java.time.LocalDateTime;

public record LogResponse(
        Long id, Long fonteId, String fonteNome, LocalDateTime iniciadoEm, LocalDateTime finalizadoEm,
        String status, int criados, int atualizados, int rejeitados, String mensagem, String executadoPor) {

    public static LogResponse de(LogAtualizacao l) {
        return new LogResponse(l.getId(), l.getFonte().getId(), l.getFonte().getNome(), l.getIniciadoEm(),
                l.getFinalizadoEm(), l.getStatus().name(), l.getRegistrosCriados(), l.getRegistrosAtualizados(),
                l.getRegistrosRejeitados(), l.getMensagem(), l.getExecutadoPor());
    }
}
