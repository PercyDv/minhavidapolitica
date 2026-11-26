package br.com.minhavidapolitica.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.minhavidapolitica.api.domain.Candidatura;
import br.com.minhavidapolitica.api.domain.Cargo;
import br.com.minhavidapolitica.api.domain.Esfera;
import br.com.minhavidapolitica.api.domain.FonteDados;
import br.com.minhavidapolitica.api.domain.LogAtualizacao;
import br.com.minhavidapolitica.api.domain.SituacaoCandidatura;
import br.com.minhavidapolitica.api.domain.TipoFonte;
import br.com.minhavidapolitica.api.domain.UnidadeFederativa;
import br.com.minhavidapolitica.api.dto.CandidaturaPublica;
import br.com.minhavidapolitica.api.dto.ImportacaoCandidaturasRequest;
import br.com.minhavidapolitica.api.dto.ImportacaoCandidaturasRequest.CandidaturaImportada;
import br.com.minhavidapolitica.api.dto.LogResponse;
import br.com.minhavidapolitica.api.dto.Resultado;
import br.com.minhavidapolitica.api.exception.RecursoNaoEncontradoException;
import br.com.minhavidapolitica.api.exception.RegraNegocioException;
import br.com.minhavidapolitica.api.repository.CandidaturaRepository;
import br.com.minhavidapolitica.api.repository.CargoRepository;
import br.com.minhavidapolitica.api.repository.FonteDadosRepository;
import br.com.minhavidapolitica.api.repository.LogAtualizacaoRepository;
import br.com.minhavidapolitica.api.repository.UnidadeFederativaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CandidaturaServiceTest {

    @Mock
    private CandidaturaRepository candidaturaRepository;
    @Mock
    private CargoRepository cargoRepository;
    @Mock
    private UnidadeFederativaRepository ufRepository;
    @Mock
    private FonteDadosRepository fonteRepository;
    @Mock
    private LogAtualizacaoRepository logRepository;

    private CandidaturaService service;

    @BeforeEach
    void preparar() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-26T12:00:00Z"), ZoneId.of("America/Bahia"));
        service = new CandidaturaService(candidaturaRepository, cargoRepository, ufRepository, fonteRepository,
                logRepository, clock);
    }

    @Test
    void semCandidaturaEmFonteAprovadaRetornaMensagemNeutra() {
        when(candidaturaRepository.listarNacionais(2026, "PRESIDENTE")).thenReturn(List.of());

        Resultado<List<CandidaturaPublica>> resultado = service.presidente(2026);

        assertThat(resultado.disponivel()).isFalse();
        assertThat(resultado.mensagem()).isEqualTo(Resultado.MENSAGEM_INDISPONIVEL);
    }

    @Test
    void candidaturaPublicaTrazFonteESituacaoSemVotos() {
        Candidatura c = Candidatura.builder().id(1L).anoEleicao(2026).sqCandidato("100").numero("13")
                .nome("Candidata Exemplo").nomeUrna("Exemplo").partido("PEX")
                .cargo(cargo("PRESIDENTE", Esfera.NACIONAL)).situacao(SituacaoCandidatura.ELEITO)
                .fonte(fonte(true, true)).atualizadoEm(LocalDateTime.of(2026, 11, 20, 10, 0)).build();
        when(candidaturaRepository.listarNacionais(2026, "PRESIDENTE")).thenReturn(List.of(c));

        Resultado<List<CandidaturaPublica>> resultado = service.presidente(2026);

        assertThat(resultado.disponivel()).isTrue();
        CandidaturaPublica publica = resultado.dados().get(0);
        assertThat(publica.situacao()).isEqualTo("ELEITO");
        assertThat(publica.uf()).isNull();
        assertThat(publica.fonte().nome()).isEqualTo("Fonte de Teste");
    }

    @Test
    void governadorDeUfInexistenteResultaEmNaoEncontrado() {
        when(ufRepository.findBySiglaIgnoreCase("XX")).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> service.governador(2026, "XX"));
    }

    @Test
    void fonteInativaNaoPermiteImportar() {
        when(fonteRepository.findById(1L)).thenReturn(Optional.of(fonte(false, false)));

        ImportacaoCandidaturasRequest req = new ImportacaoCandidaturasRequest(1L, 2026,
                List.of(item("100", "PRESIDENTE", null)));

        assertThrows(RegraNegocioException.class, () -> service.importar(req, "admin"));
    }

    @Test
    void cargoForaDoEscopoEhRejeitadoSemDerrubarACarga() {
        when(fonteRepository.findById(1L)).thenReturn(Optional.of(fonte(true, true)));
        salvarLogDevolvendoOMesmo();

        ImportacaoCandidaturasRequest req = new ImportacaoCandidaturasRequest(1L, 2026,
                List.of(item("100", "DEPUTADO_FEDERAL", "BA")));
        LogResponse log = service.importar(req, "admin");

        assertThat(log.status()).isEqualTo("ERRO");
        assertThat(log.rejeitados()).isEqualTo(1);
        assertThat(log.mensagem()).contains("somente PRESIDENTE e GOVERNADOR");
    }

    @Test
    void presidenteComUfEhRejeitado() {
        when(fonteRepository.findById(1L)).thenReturn(Optional.of(fonte(true, true)));
        when(cargoRepository.findByCodigoIgnoreCase("PRESIDENTE"))
                .thenReturn(Optional.of(cargo("PRESIDENTE", Esfera.NACIONAL)));
        salvarLogDevolvendoOMesmo();

        ImportacaoCandidaturasRequest req = new ImportacaoCandidaturasRequest(1L, 2026,
                List.of(item("100", "PRESIDENTE", "BA")));
        LogResponse log = service.importar(req, "admin");

        assertThat(log.rejeitados()).isEqualTo(1);
        assertThat(log.mensagem()).contains("cargo nacional não deve ter UF");
    }

    @Test
    void candidaturaNovaEhCriadaEFonteNaoAprovadaGeraAvisoDeRetencao() {
        when(fonteRepository.findById(1L)).thenReturn(Optional.of(fonte(false, true)));
        when(cargoRepository.findByCodigoIgnoreCase("PRESIDENTE"))
                .thenReturn(Optional.of(cargo("PRESIDENTE", Esfera.NACIONAL)));
        when(candidaturaRepository.findFirstByAnoEleicaoAndSqCandidato(2026, "100")).thenReturn(Optional.empty());
        when(candidaturaRepository.save(any(Candidatura.class))).thenAnswer(inv -> inv.getArgument(0));
        salvarLogDevolvendoOMesmo();

        ImportacaoCandidaturasRequest req = new ImportacaoCandidaturasRequest(1L, 2026,
                List.of(item("100", "PRESIDENTE", null)));
        LogResponse log = service.importar(req, "admin");

        assertThat(log.status()).isEqualTo("SUCESSO");
        assertThat(log.criados()).isEqualTo(1);
        assertThat(log.mensagem()).contains("não está aprovada");
    }

    @Test
    void reimportarAtualizaASituacaoEmVezDeDuplicar() {
        Candidatura existente = Candidatura.builder().id(7L).anoEleicao(2026).sqCandidato("200").numero("22")
                .nome("Candidato Exemplo").cargo(cargo("GOVERNADOR", Esfera.ESTADUAL))
                .uf(UnidadeFederativa.builder().id(5L).sigla("BA").nome("Bahia").regiao("Nordeste").build())
                .situacao(SituacaoCandidatura.SEGUNDO_TURNO).fonte(fonte(true, true)).build();
        when(fonteRepository.findById(1L)).thenReturn(Optional.of(fonte(true, true)));
        when(cargoRepository.findByCodigoIgnoreCase("GOVERNADOR"))
                .thenReturn(Optional.of(cargo("GOVERNADOR", Esfera.ESTADUAL)));
        when(ufRepository.findBySiglaIgnoreCase("BA")).thenReturn(Optional.of(existente.getUf()));
        when(candidaturaRepository.findFirstByAnoEleicaoAndSqCandidato(2026, "200")).thenReturn(Optional.of(existente));
        when(candidaturaRepository.save(any(Candidatura.class))).thenAnswer(inv -> inv.getArgument(0));
        salvarLogDevolvendoOMesmo();

        CandidaturaImportada item = new CandidaturaImportada("200", "Candidato Exemplo", null, "22", "PEX",
                "GOVERNADOR", "BA", "NAO_ELEITO", null);
        LogResponse log = service.importar(new ImportacaoCandidaturasRequest(1L, 2026, List.of(item)), "admin");

        assertThat(log.atualizados()).isEqualTo(1);
        assertThat(log.criados()).isZero();
        assertThat(existente.getSituacao()).isEqualTo(SituacaoCandidatura.NAO_ELEITO);
    }

    private void salvarLogDevolvendoOMesmo() {
        when(logRepository.save(any(LogAtualizacao.class))).thenAnswer(inv -> {
            LogAtualizacao l = inv.getArgument(0);
            l.setId(1L);
            return l;
        });
    }

    private static Cargo cargo(String codigo, Esfera esfera) {
        return Cargo.builder().id(1L).codigo(codigo).nome(codigo).esfera(esfera).ativo(true).build();
    }

    private static FonteDados fonte(boolean aprovada, boolean ativa) {
        return FonteDados.builder().id(1L).nome("Fonte de Teste").url("https://fonte.exemplo.gov.br")
                .tipo(TipoFonte.ARQUIVO).aprovada(aprovada).ativa(ativa).build();
    }

    private static CandidaturaImportada item(String sq, String cargo, String uf) {
        return new CandidaturaImportada(sq, "Candidata Exemplo", "Exemplo", "13", "PEX", cargo, uf, "ELEITO", null);
    }
}
