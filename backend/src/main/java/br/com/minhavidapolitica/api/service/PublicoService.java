package br.com.minhavidapolitica.api.service;

import br.com.minhavidapolitica.api.domain.Politico;
import br.com.minhavidapolitica.api.domain.UnidadeFederativa;
import br.com.minhavidapolitica.api.dto.ItemPublico;
import br.com.minhavidapolitica.api.dto.PoliticoPublico;
import br.com.minhavidapolitica.api.dto.Resultado;
import br.com.minhavidapolitica.api.dto.UfPublica;
import br.com.minhavidapolitica.api.dto.UfResumo;
import br.com.minhavidapolitica.api.exception.RecursoNaoEncontradoException;
import br.com.minhavidapolitica.api.repository.AcaoConcretizadaRepository;
import br.com.minhavidapolitica.api.repository.PoliticoRepository;
import br.com.minhavidapolitica.api.repository.PropostaRepository;
import br.com.minhavidapolitica.api.repository.UnidadeFederativaRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consultas do cidadão (RF01 a RF08, RF12). Todas as regras de visibilidade ficam nas consultas:
 * apenas mandato atual (RN01) e apenas fontes aprovadas pelo cliente (RN05).
 * Nenhum método ordena por mérito (RF09, RN04): a ordem é cargo e nome, ou data.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicoService {

    static final String CARGO_PRESIDENTE = "PRESIDENTE";
    static final String CARGO_GOVERNADOR = "GOVERNADOR";

    private final PoliticoRepository politicoRepository;
    private final PropostaRepository propostaRepository;
    private final AcaoConcretizadaRepository acaoRepository;
    private final UnidadeFederativaRepository ufRepository;
    private final Clock clock;

    /** RF01: presidente em exercício. */
    public Resultado<PoliticoPublico> presidente() {
        List<Politico> lista = politicoRepository.listarNacionaisVisiveis(CARGO_PRESIDENTE, hoje());
        return lista.isEmpty() ? Resultado.indisponivel() : Resultado.com(PoliticoPublico.de(lista.get(0)));
    }

    /** RF02: as 27 UFs do seletor. */
    public List<UfResumo> listarUfs() {
        return ufRepository.findAllByOrderBySiglaAsc().stream().map(UfResumo::de).toList();
    }

    /** RF03: página estadual com o governador em exercício daquela UF e de nenhuma outra. */
    public UfPublica uf(String sigla) {
        UnidadeFederativa uf = buscarUf(sigla);
        Resultado<PoliticoPublico> governador = politicoRepository.listarVisiveisPorUf(uf.getSigla(), hoje()).stream()
                .filter(p -> CARGO_GOVERNADOR.equals(p.getCargo().getCodigo()))
                .findFirst()
                .map(p -> Resultado.com(PoliticoPublico.de(p)))
                .orElseGet(Resultado::indisponivel);
        return new UfPublica(uf.getSigla(), uf.getNome(), uf.getRegiao(), governador);
    }

    /** RF04: demais representantes da UF (todos os cargos exceto o de governador). */
    public Resultado<List<PoliticoPublico>> representantes(String sigla) {
        UnidadeFederativa uf = buscarUf(sigla);
        List<PoliticoPublico> lista = politicoRepository.listarVisiveisPorUf(uf.getSigla(), hoje()).stream()
                .filter(p -> !CARGO_GOVERNADOR.equals(p.getCargo().getCodigo()))
                .map(PoliticoPublico::de)
                .toList();
        return lista.isEmpty() ? Resultado.indisponivel() : Resultado.com(lista);
    }

    /** RF07: perfil resumido. Político sem fonte validada é tratado como inexistente. */
    public PoliticoPublico perfil(Long id) {
        return PoliticoPublico.de(buscarVisivel(id));
    }

    /** RF05: propostas do mandato atual. */
    public Resultado<List<ItemPublico>> propostas(Long politicoId) {
        buscarVisivel(politicoId);
        List<ItemPublico> itens = propostaRepository.listarVisiveis(politicoId).stream().map(ItemPublico::de).toList();
        return itens.isEmpty() ? Resultado.indisponivel() : Resultado.com(itens);
    }

    /** RF06: ações concretizadas, sempre com fonte e data. */
    public Resultado<List<ItemPublico>> acoes(Long politicoId) {
        buscarVisivel(politicoId);
        List<ItemPublico> itens = acaoRepository.listarVisiveis(politicoId).stream().map(ItemPublico::de).toList();
        return itens.isEmpty() ? Resultado.indisponivel() : Resultado.com(itens);
    }

    private Politico buscarVisivel(Long id) {
        return politicoRepository.buscarVisivelPorId(id, hoje())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Político não encontrado nas fontes validadas"));
    }

    private UnidadeFederativa buscarUf(String sigla) {
        return ufRepository.findBySiglaIgnoreCase(sigla)
                .orElseThrow(() -> new RecursoNaoEncontradoException("UF não encontrada: " + sigla));
    }

    private LocalDate hoje() {
        return LocalDate.now(clock);
    }
}
