package br.com.minhavidapolitica.api.dto;

/** Quantidade de registros vinculados a uma fonte, usada para mostrar o que está retido por falta de aprovação. */
public record FontePendencia(Long fonteId, String fonteNome, boolean aprovada, boolean ativa,
                             long politicos, long propostas, long acoes) {

    public long total() {
        return politicos + propostas + acoes;
    }

    public boolean retido() {
        return !(aprovada && ativa) && total() > 0;
    }
}
