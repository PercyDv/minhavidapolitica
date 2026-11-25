package br.com.minhavidapolitica.api.service;

import br.com.minhavidapolitica.api.domain.Proposta;
import br.com.minhavidapolitica.api.domain.FonteDados;
import br.com.minhavidapolitica.api.domain.Politico;
import br.com.minhavidapolitica.api.dto.ItemAdminResponse;
import br.com.minhavidapolitica.api.dto.ItemRequest;
import br.com.minhavidapolitica.api.exception.RecursoNaoEncontradoException;
import br.com.minhavidapolitica.api.repository.FonteDadosRepository;
import br.com.minhavidapolitica.api.repository.PoliticoRepository;
import br.com.minhavidapolitica.api.repository.PropostaRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Controle de dados: Proposta de um político (módulo ADS). */
@Service
@RequiredArgsConstructor
@Transactional
public class PropostaService {

    private final PropostaRepository repository;
    private final PoliticoRepository politicoRepository;
    private final FonteDadosRepository fonteRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<ItemAdminResponse> listar(Long politicoId) {
        obterPolitico(politicoId);
        return repository.listarDoPolitico(politicoId).stream().map(ItemAdminResponse::de).toList();
    }

    public ItemAdminResponse criar(Long politicoId, ItemRequest req) {
        Proposta item = new Proposta();
        item.setPolitico(obterPolitico(politicoId));
        aplicar(item, req);
        return ItemAdminResponse.de(repository.save(item));
    }

    public ItemAdminResponse atualizar(Long id, ItemRequest req) {
        Proposta item = obter(id);
        aplicar(item, req);
        return ItemAdminResponse.de(repository.save(item));
    }

    public void excluir(Long id) {
        repository.delete(obter(id));
    }

    private Proposta obter(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Registro não encontrado."));
    }

    private Politico obterPolitico(Long id) {
        return politicoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Político não encontrado."));
    }

    private void aplicar(Proposta item, ItemRequest req) {
        FonteDados fonte = fonteRepository.findById(req.fonteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fonte não encontrada."));
        item.setTitulo(req.titulo().trim());
        item.setDescricao(PoliticoService.limpar(req.descricao()));
        item.setDataReferencia(req.data());
        item.setFonte(fonte);
        item.setLinkFonte(PoliticoService.limpar(req.linkFonte()));
        item.setAtualizadoEm(LocalDateTime.now(clock));
    }
}
