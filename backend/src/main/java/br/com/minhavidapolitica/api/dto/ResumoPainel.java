package br.com.minhavidapolitica.api.dto;

import java.util.List;

/** Números da tela inicial do painel. */
public record ResumoPainel(long politicos, long propostas, long acoes,
                           long fontesAprovadas, long fontesPendentes,
                           int ufsComGovernador, int totalUfs,
                           List<LogResponse> ultimasImportacoes) {
}
