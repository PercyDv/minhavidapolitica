package br.com.minhavidapolitica.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.minhavidapolitica.api.domain.FonteDados;
import br.com.minhavidapolitica.api.domain.TipoFonte;
import br.com.minhavidapolitica.api.dto.AprovacaoRequest;
import br.com.minhavidapolitica.api.dto.FonteResponse;
import br.com.minhavidapolitica.api.exception.RegraNegocioException;
import br.com.minhavidapolitica.api.repository.AcaoConcretizadaRepository;
import br.com.minhavidapolitica.api.repository.CandidaturaRepository;
import br.com.minhavidapolitica.api.repository.FonteDadosRepository;
import br.com.minhavidapolitica.api.repository.LogAtualizacaoRepository;
import br.com.minhavidapolitica.api.repository.PoliticoRepository;
import br.com.minhavidapolitica.api.repository.PropostaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FonteServiceTest {

    @Mock
    private FonteDadosRepository repository;
    @Mock
    private PoliticoRepository politicoRepository;
    @Mock
    private PropostaRepository propostaRepository;
    @Mock
    private AcaoConcretizadaRepository acaoRepository;
    @Mock
    private LogAtualizacaoRepository logRepository;
    @Mock
    private CandidaturaRepository candidaturaRepository;

    private FonteService service;

    @BeforeEach
    void preparar() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-25T12:00:00Z"), ZoneId.of("America/Bahia"));
        service = new FonteService(repository, politicoRepository, propostaRepository, acaoRepository,
                logRepository, candidaturaRepository, clock);
    }

    @Test
    void aprovacaoExigePeriodicidadeDefinida() {
        when(repository.findById(1L)).thenReturn(Optional.of(fonte(null)));

        AprovacaoRequest req = new AprovacaoRequest("Samuel (cliente)", null, null);

        assertThrows(RegraNegocioException.class, () -> service.aprovar(1L, req));
    }

    @Test
    void aprovacaoRegistraQuemEQuandoComDataPadraoHoje() {
        when(repository.findById(1L)).thenReturn(Optional.of(fonte("Mensal")));
        when(repository.save(any(FonteDados.class))).thenAnswer(inv -> inv.getArgument(0));

        FonteResponse resposta = service.aprovar(1L, new AprovacaoRequest("Samuel (cliente)", null, null));

        assertThat(resposta.aprovada()).isTrue();
        assertThat(resposta.aprovadaPor()).isEqualTo("Samuel (cliente)");
        assertThat(resposta.dataAprovacao()).isEqualTo(LocalDate.of(2026, 11, 25));
    }

    @Test
    void revogarDevolveAFonteAoEstadoRetido() {
        FonteDados aprovada = fonte("Mensal");
        aprovada.setAprovada(true);
        aprovada.setAprovadaPor("Samuel (cliente)");
        when(repository.findById(1L)).thenReturn(Optional.of(aprovada));
        when(repository.save(any(FonteDados.class))).thenAnswer(inv -> inv.getArgument(0));

        FonteResponse resposta = service.revogar(1L);

        assertThat(resposta.aprovada()).isFalse();
        assertThat(resposta.aprovadaPor()).isNull();
    }

    @Test
    void fonteComRegistrosVinculadosNaoPodeSerExcluida() {
        when(repository.findById(1L)).thenReturn(Optional.of(fonte("Mensal")));
        when(politicoRepository.countByFonteId(1L)).thenReturn(3L);

        assertThrows(RegraNegocioException.class, () -> service.excluir(1L));
    }

    private static FonteDados fonte(String periodicidade) {
        return FonteDados.builder().id(1L).nome("Fonte de Teste").url("https://fonte.exemplo.gov.br")
                .tipo(TipoFonte.API).periodicidade(periodicidade).build();
    }
}
