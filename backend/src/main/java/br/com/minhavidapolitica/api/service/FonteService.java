package br.com.minhavidapolitica.api.service;

import br.com.minhavidapolitica.api.domain.FonteDados;
import br.com.minhavidapolitica.api.dto.AprovacaoRequest;
import br.com.minhavidapolitica.api.dto.FonteRequest;
import br.com.minhavidapolitica.api.dto.FonteResponse;
import br.com.minhavidapolitica.api.exception.RecursoNaoEncontradoException;
import br.com.minhavidapolitica.api.exception.RegraNegocioException;
import br.com.minhavidapolitica.api.repository.AcaoConcretizadaRepository;
import br.com.minhavidapolitica.api.repository.FonteDadosRepository;
import br.com.minhavidapolitica.api.repository.LogAtualizacaoRepository;
import br.com.minhavidapolitica.api.repository.PoliticoRepository;
import br.com.minhavidapolitica.api.repository.PropostaRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestão das fontes e do registro de aprovação do cliente (módulo BES; RN05, RF13).
 * Enquanto a fonte não é aprovada, tudo que veio dela fica retido e não aparece ao cidadão.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FonteService {

    private final FonteDadosRepository repository;
    private final PoliticoRepository politicoRepository;
    private final PropostaRepository propostaRepository;
    private final AcaoConcretizadaRepository acaoRepository;
    private final LogAtualizacaoRepository logRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<FonteResponse> listar() {
        return repository.findAllByOrderByNomeAsc().stream().map(FonteResponse::de).toList();
    }

    public FonteResponse criar(FonteRequest req) {
        FonteDados fonte = new FonteDados();
        aplicar(fonte, req);
        return FonteResponse.de(repository.save(fonte));
    }

    public FonteResponse atualizar(Long id, FonteRequest req) {
        FonteDados fonte = obter(id);
        aplicar(fonte, req);
        return FonteResponse.de(repository.save(fonte));
    }

    /** Registra a aprovação formal do cliente. Exige periodicidade definida (critério do RF13). */
    public FonteResponse aprovar(Long id, AprovacaoRequest req) {
        FonteDados fonte = obter(id);
        if (fonte.getPeriodicidade() == null || fonte.getPeriodicidade().isBlank()) {
            throw new RegraNegocioException(
                    "Defina a periodicidade de atualização da fonte antes de registrar a aprovação (RF13).");
        }
        fonte.setAprovada(true);
        fonte.setAprovadaPor(req.aprovadaPor().trim());
        fonte.setDataAprovacao(req.dataAprovacao() != null ? req.dataAprovacao() : LocalDate.now(clock));
        if (req.observacao() != null && !req.observacao().isBlank()) {
            fonte.setObservacao(req.observacao().trim());
        }
        return FonteResponse.de(repository.save(fonte));
    }

    /** Retira a aprovação: os dados da fonte voltam a ficar retidos. */
    public FonteResponse revogar(Long id) {
        FonteDados fonte = obter(id);
        fonte.setAprovada(false);
        fonte.setAprovadaPor(null);
        fonte.setDataAprovacao(null);
        return FonteResponse.de(repository.save(fonte));
    }

    public void excluir(Long id) {
        FonteDados fonte = obter(id);
        boolean emUso = politicoRepository.countByFonteId(id) > 0
                || propostaRepository.countByFonteId(id) > 0
                || acaoRepository.countByFonteId(id) > 0
                || logRepository.existsByFonteId(id);
        if (emUso) {
            throw RegraNegocioException.conflito(
                    "A fonte tem registros vinculados e não pode ser excluída. Desative-a para reter os dados.");
        }
        repository.delete(fonte);
    }

    private FonteDados obter(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fonte não encontrada."));
    }

    private void aplicar(FonteDados fonte, FonteRequest req) {
        String url = req.url().trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new RegraNegocioException("A URL deve começar com http:// ou https://");
        }
        fonte.setNome(req.nome().trim());
        fonte.setUrl(url);
        fonte.setTipo(req.tipo());
        fonte.setPeriodicidade(PoliticoService.limpar(req.periodicidade()));
        fonte.setObservacao(PoliticoService.limpar(req.observacao()));
        fonte.setAtiva(req.ativa() == null || req.ativa());
    }
}
