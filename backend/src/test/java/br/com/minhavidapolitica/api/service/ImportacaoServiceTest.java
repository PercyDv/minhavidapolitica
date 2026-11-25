package br.com.minhavidapolitica.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.minhavidapolitica.api.domain.Cargo;
import br.com.minhavidapolitica.api.domain.Esfera;
import br.com.minhavidapolitica.api.domain.FonteDados;
import br.com.minhavidapolitica.api.domain.LogAtualizacao;
import br.com.minhavidapolitica.api.domain.Politico;
import br.com.minhavidapolitica.api.domain.TipoFonte;
import br.com.minhavidapolitica.api.dto.ImportacaoRequest;
import br.com.minhavidapolitica.api.dto.ImportacaoRequest.ImportacaoPolitico;
import br.com.minhavidapolitica.api.dto.LogResponse;
import br.com.minhavidapolitica.api.exception.RegraNegocioException;
import br.com.minhavidapolitica.api.repository.AcaoConcretizadaRepository;
import br.com.minhavidapolitica.api.repository.CargoRepository;
import br.com.minhavidapolitica.api.repository.FonteDadosRepository;
import br.com.minhavidapolitica.api.repository.LogAtualizacaoRepository;
import br.com.minhavidapolitica.api.repository.PoliticoRepository;
import br.com.minhavidapolitica.api.repository.PropostaRepository;
import br.com.minhavidapolitica.api.repository.UnidadeFederativaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImportacaoServiceTest {

    @Mock
    private FonteDadosRepository fonteRepository;
    @Mock
    private CargoRepository cargoRepository;
    @Mock
    private UnidadeFederativaRepository ufRepository;
    @Mock
    private PoliticoRepository politicoRepository;
    @Mock
    private PropostaRepository propostaRepository;
    @Mock
    private AcaoConcretizadaRepository acaoRepository;
    @Mock
    private LogAtualizacaoRepository logRepository;

    private ImportacaoService service;

    @BeforeEach
    void preparar() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-25T12:00:00Z"), ZoneId.of("America/Bahia"));
        service = new ImportacaoService(fonteRepository, cargoRepository, ufRepository, politicoRepository,
                propostaRepository, acaoRepository, logRepository, clock);
    }

    @Test
    void fonteInativaNaoPermiteImportar() {
        FonteDados fonte = fonte(false, false);
        when(fonteRepository.findById(1L)).thenReturn(Optional.of(fonte));

        ImportacaoRequest req = new ImportacaoRequest(1L, List.of(politico("Fulano Exemplo", "PRESIDENTE", null)));

        assertThrows(RegraNegocioException.class, () -> service.importar(req, "admin"));
    }

    @Test
    void cargoInexistenteRejeitaORegistroSemDerrubarACarga() {
        when(fonteRepository.findById(1L)).thenReturn(Optional.of(fonte(true, true)));
        when(cargoRepository.findByCodigoIgnoreCase("CARGO_INVENTADO")).thenReturn(Optional.empty());
        salvarLogDevolvendoOMesmo();

        ImportacaoRequest req = new ImportacaoRequest(1L,
                List.of(politico("Fulano Exemplo", "CARGO_INVENTADO", null)));
        LogResponse log = service.importar(req, "admin");

        assertThat(log.status()).isEqualTo("ERRO");
        assertThat(log.rejeitados()).isEqualTo(1);
        assertThat(log.criados()).isZero();
        assertThat(log.mensagem()).contains("CARGO_INVENTADO");
    }

    @Test
    void politicoNovoEhCriadoEFonteNaoAprovadaGeraAvisoDeRetencao() {
        Cargo presidente = Cargo.builder().id(1L).codigo("PRESIDENTE").nome("Presidente da República")
                .esfera(Esfera.NACIONAL).ativo(true).build();
        when(fonteRepository.findById(1L)).thenReturn(Optional.of(fonte(false, true)));
        when(cargoRepository.findByCodigoIgnoreCase("PRESIDENTE")).thenReturn(Optional.of(presidente));
        when(politicoRepository.findFirstByNomeIgnoreCaseAndCargoIdAndUfIsNull("Fulano Exemplo", 1L))
                .thenReturn(Optional.empty());
        when(politicoRepository.save(any(Politico.class))).thenAnswer(inv -> inv.getArgument(0));
        salvarLogDevolvendoOMesmo();

        ImportacaoRequest req = new ImportacaoRequest(1L, List.of(politico("Fulano Exemplo", "PRESIDENTE", null)));
        LogResponse log = service.importar(req, "admin");

        assertThat(log.status()).isEqualTo("SUCESSO");
        assertThat(log.criados()).isEqualTo(1);
        assertThat(log.rejeitados()).isZero();
        assertThat(log.mensagem()).contains("não está aprovada");
    }

    @Test
    void cargoNacionalComUfEhRejeitado() {
        Cargo presidente = Cargo.builder().id(1L).codigo("PRESIDENTE").nome("Presidente da República")
                .esfera(Esfera.NACIONAL).ativo(true).build();
        when(fonteRepository.findById(1L)).thenReturn(Optional.of(fonte(true, true)));
        when(cargoRepository.findByCodigoIgnoreCase("PRESIDENTE")).thenReturn(Optional.of(presidente));
        salvarLogDevolvendoOMesmo();

        ImportacaoRequest req = new ImportacaoRequest(1L, List.of(politico("Fulano Exemplo", "PRESIDENTE", "BA")));
        LogResponse log = service.importar(req, "admin");

        assertThat(log.rejeitados()).isEqualTo(1);
        assertThat(log.mensagem()).contains("cargo nacional não deve ter UF");
    }

    private void salvarLogDevolvendoOMesmo() {
        when(logRepository.save(any(LogAtualizacao.class))).thenAnswer(inv -> {
            LogAtualizacao l = inv.getArgument(0);
            l.setId(1L);
            return l;
        });
    }

    private static FonteDados fonte(boolean aprovada, boolean ativa) {
        return FonteDados.builder().id(1L).nome("Fonte de Teste").url("https://fonte.exemplo.gov.br")
                .tipo(TipoFonte.API).aprovada(aprovada).ativa(ativa).build();
    }

    private static ImportacaoPolitico politico(String nome, String cargo, String uf) {
        return new ImportacaoPolitico(nome, null, null, null, cargo, uf, null, null, null, null, null);
    }
}
