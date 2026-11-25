package br.com.minhavidapolitica.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import br.com.minhavidapolitica.api.domain.Cargo;
import br.com.minhavidapolitica.api.domain.Esfera;
import br.com.minhavidapolitica.api.domain.FonteDados;
import br.com.minhavidapolitica.api.domain.Politico;
import br.com.minhavidapolitica.api.domain.TipoFonte;
import br.com.minhavidapolitica.api.domain.UnidadeFederativa;
import br.com.minhavidapolitica.api.dto.PoliticoPublico;
import br.com.minhavidapolitica.api.dto.Resultado;
import br.com.minhavidapolitica.api.dto.UfPublica;
import br.com.minhavidapolitica.api.exception.RecursoNaoEncontradoException;
import br.com.minhavidapolitica.api.repository.AcaoConcretizadaRepository;
import br.com.minhavidapolitica.api.repository.PoliticoRepository;
import br.com.minhavidapolitica.api.repository.PropostaRepository;
import br.com.minhavidapolitica.api.repository.UnidadeFederativaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
class PublicoServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 25);

    @Mock
    private PoliticoRepository politicoRepository;
    @Mock
    private PropostaRepository propostaRepository;
    @Mock
    private AcaoConcretizadaRepository acaoRepository;
    @Mock
    private UnidadeFederativaRepository ufRepository;

    private PublicoService service;

    @BeforeEach
    void preparar() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-25T12:00:00Z"), ZoneId.of("America/Bahia"));
        service = new PublicoService(politicoRepository, propostaRepository, acaoRepository, ufRepository, clock);
    }

    @Test
    void presidenteSemDadoEmFonteValidadaRetornaMensagemNeutra() {
        when(politicoRepository.listarNacionaisVisiveis("PRESIDENTE", HOJE)).thenReturn(List.of());

        Resultado<PoliticoPublico> resultado = service.presidente();

        assertThat(resultado.disponivel()).isFalse();
        assertThat(resultado.mensagem()).isEqualTo(Resultado.MENSAGEM_INDISPONIVEL);
        assertThat(resultado.dados()).isNull();
    }

    @Test
    void presidenteComDadoVemAcompanhadoDaFonte() {
        Politico presidente = politico(1L, "Fulano Exemplo", "PRESIDENTE", "Presidente da República", Esfera.NACIONAL, null);
        when(politicoRepository.listarNacionaisVisiveis("PRESIDENTE", HOJE)).thenReturn(List.of(presidente));

        Resultado<PoliticoPublico> resultado = service.presidente();

        assertThat(resultado.disponivel()).isTrue();
        assertThat(resultado.dados().nome()).isEqualTo("Fulano Exemplo");
        assertThat(resultado.dados().uf()).isNull();
        assertThat(resultado.dados().fonte().nome()).isEqualTo("Fonte de Teste");
        assertThat(resultado.dados().fonte().link()).isEqualTo("https://fonte.exemplo.gov.br");
    }

    @Test
    void paginaDaUfSemGovernadorValidadoMostraIndisponivel() {
        when(ufRepository.findBySiglaIgnoreCase("BA")).thenReturn(Optional.of(uf("BA", "Bahia")));
        when(politicoRepository.listarVisiveisPorUf("BA", HOJE)).thenReturn(List.of());

        UfPublica pagina = service.uf("BA");

        assertThat(pagina.sigla()).isEqualTo("BA");
        assertThat(pagina.governador().disponivel()).isFalse();
    }

    @Test
    void ufInexistenteResultaEmNaoEncontrado() {
        when(ufRepository.findBySiglaIgnoreCase("XX")).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> service.uf("XX"));
    }

    @Test
    void representantesNaoIncluemOGovernador() {
        UnidadeFederativa ba = uf("BA", "Bahia");
        Politico governador = politico(10L, "Governadora Exemplo", "GOVERNADOR", "Governador", Esfera.ESTADUAL, ba);
        Politico senador = politico(11L, "Senador Exemplo", "SENADOR", "Senador", Esfera.ESTADUAL, ba);
        when(ufRepository.findBySiglaIgnoreCase("BA")).thenReturn(Optional.of(ba));
        when(politicoRepository.listarVisiveisPorUf("BA", HOJE)).thenReturn(List.of(governador, senador));

        Resultado<List<PoliticoPublico>> resultado = service.representantes("BA");

        assertThat(resultado.disponivel()).isTrue();
        assertThat(resultado.dados()).extracting(PoliticoPublico::cargo).containsExactly("Senador");
    }

    @Test
    void politicoSemFonteValidadaEhTratadoComoInexistente() {
        when(politicoRepository.buscarVisivelPorId(99L, HOJE)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> service.perfil(99L));
    }

    private static UnidadeFederativa uf(String sigla, String nome) {
        return UnidadeFederativa.builder().id(1L).sigla(sigla).nome(nome).regiao("Nordeste").build();
    }

    private static Politico politico(Long id, String nome, String codigoCargo, String nomeCargo, Esfera esfera,
                                     UnidadeFederativa uf) {
        Cargo cargo = Cargo.builder().id(1L).codigo(codigoCargo).nome(nomeCargo).esfera(esfera).build();
        FonteDados fonte = FonteDados.builder().id(1L).nome("Fonte de Teste").url("https://fonte.exemplo.gov.br")
                .tipo(TipoFonte.SITE_OFICIAL).aprovada(true).ativa(true).build();
        return Politico.builder().id(id).nome(nome).cargo(cargo).uf(uf).fonte(fonte)
                .atualizadoEm(LocalDateTime.of(2026, 11, 20, 10, 0)).build();
    }
}
