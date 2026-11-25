package br.com.minhavidapolitica.api.service;

import br.com.minhavidapolitica.api.domain.Cargo;
import br.com.minhavidapolitica.api.domain.Esfera;
import br.com.minhavidapolitica.api.domain.FonteDados;
import br.com.minhavidapolitica.api.domain.Politico;
import br.com.minhavidapolitica.api.domain.UnidadeFederativa;
import br.com.minhavidapolitica.api.dto.PoliticoAdminResponse;
import br.com.minhavidapolitica.api.dto.PoliticoRequest;
import br.com.minhavidapolitica.api.exception.RecursoNaoEncontradoException;
import br.com.minhavidapolitica.api.exception.RegraNegocioException;
import br.com.minhavidapolitica.api.repository.CargoRepository;
import br.com.minhavidapolitica.api.repository.FonteDadosRepository;
import br.com.minhavidapolitica.api.repository.PoliticoRepository;
import br.com.minhavidapolitica.api.repository.UnidadeFederativaRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Controle de dados dos políticos (módulo ADS). */
@Service
@RequiredArgsConstructor
@Transactional
public class PoliticoService {

    private final PoliticoRepository politicoRepository;
    private final CargoRepository cargoRepository;
    private final UnidadeFederativaRepository ufRepository;
    private final FonteDadosRepository fonteRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<PoliticoAdminResponse> listar(String nome, Long ufId, Long cargoId) {
        String filtroNome = nome == null ? "" : nome.trim();
        return politicoRepository.buscarAdmin(filtroNome, ufId == null ? 0L : ufId, cargoId == null ? 0L : cargoId)
                .stream().map(PoliticoAdminResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public PoliticoAdminResponse buscar(Long id) {
        return PoliticoAdminResponse.de(obter(id));
    }

    public PoliticoAdminResponse criar(PoliticoRequest req) {
        Politico politico = new Politico();
        aplicar(politico, req);
        return PoliticoAdminResponse.de(politicoRepository.save(politico));
    }

    public PoliticoAdminResponse atualizar(Long id, PoliticoRequest req) {
        Politico politico = obter(id);
        aplicar(politico, req);
        return PoliticoAdminResponse.de(politicoRepository.save(politico));
    }

    /** As propostas e ações do político são removidas junto (ON DELETE CASCADE no banco). */
    public void excluir(Long id) {
        politicoRepository.delete(obter(id));
    }

    private Politico obter(Long id) {
        return politicoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Político não encontrado."));
    }

    private void aplicar(Politico politico, PoliticoRequest req) {
        Cargo cargo = cargoRepository.findById(req.cargoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cargo não encontrado."));
        if (!cargo.isAtivo()) {
            throw new RegraNegocioException("O cargo \"" + cargo.getNome() + "\" está inativo.");
        }
        UnidadeFederativa uf = resolverUf(cargo, req.ufId());
        FonteDados fonte = fonteRepository.findById(req.fonteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fonte não encontrada."));

        if (req.inicioMandato() != null && req.fimMandato() != null && req.fimMandato().isBefore(req.inicioMandato())) {
            throw new RegraNegocioException("O fim do mandato não pode ser anterior ao início.");
        }

        String nome = req.nome().trim();
        Optional<Politico> duplicado = uf == null
                ? politicoRepository.findFirstByNomeIgnoreCaseAndCargoIdAndUfIsNull(nome, cargo.getId())
                : politicoRepository.findFirstByNomeIgnoreCaseAndCargoIdAndUfId(nome, cargo.getId(), uf.getId());
        if (duplicado.isPresent() && !duplicado.get().getId().equals(politico.getId())) {
            throw RegraNegocioException.conflito("Já existe \"" + nome + "\" cadastrado para este cargo e esta UF.");
        }

        politico.setNome(nome);
        politico.setPartido(limpar(req.partido()));
        politico.setFotoUrl(limpar(req.fotoUrl()));
        politico.setResumo(limpar(req.resumo()));
        politico.setCargo(cargo);
        politico.setUf(uf);
        politico.setInicioMandato(req.inicioMandato());
        politico.setFimMandato(req.fimMandato());
        politico.setEmExercicio(req.emExercicio() == null || req.emExercicio());
        politico.setFonte(fonte);
        politico.setLinkFonte(limpar(req.linkFonte()));
        politico.setAtualizadoEm(LocalDateTime.now(clock));
    }

    /** Cargo nacional não tem UF; cargo estadual exige UF (RN02). */
    private UnidadeFederativa resolverUf(Cargo cargo, Long ufId) {
        if (cargo.getEsfera() == Esfera.NACIONAL) {
            if (ufId != null) {
                throw new RegraNegocioException("Cargo nacional não deve ter UF.");
            }
            return null;
        }
        if (ufId == null) {
            throw new RegraNegocioException("Informe a UF do cargo estadual.");
        }
        return ufRepository.findById(ufId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("UF não encontrada."));
    }

    static String limpar(String valor) {
        if (valor == null) {
            return null;
        }
        String texto = valor.trim();
        return texto.isEmpty() ? null : texto;
    }
}
