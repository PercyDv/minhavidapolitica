package br.com.minhavidapolitica.api.service;

import br.com.minhavidapolitica.api.domain.AcaoConcretizada;
import br.com.minhavidapolitica.api.domain.Cargo;
import br.com.minhavidapolitica.api.domain.Esfera;
import br.com.minhavidapolitica.api.domain.FonteDados;
import br.com.minhavidapolitica.api.domain.LogAtualizacao;
import br.com.minhavidapolitica.api.domain.Politico;
import br.com.minhavidapolitica.api.domain.Proposta;
import br.com.minhavidapolitica.api.domain.StatusImportacao;
import br.com.minhavidapolitica.api.domain.UnidadeFederativa;
import br.com.minhavidapolitica.api.dto.ImportacaoRequest;
import br.com.minhavidapolitica.api.dto.ImportacaoRequest.ImportacaoItem;
import br.com.minhavidapolitica.api.dto.ImportacaoRequest.ImportacaoPolitico;
import br.com.minhavidapolitica.api.dto.LogResponse;
import br.com.minhavidapolitica.api.exception.RecursoNaoEncontradoException;
import br.com.minhavidapolitica.api.exception.RegraNegocioException;
import br.com.minhavidapolitica.api.repository.AcaoConcretizadaRepository;
import br.com.minhavidapolitica.api.repository.CargoRepository;
import br.com.minhavidapolitica.api.repository.FonteDadosRepository;
import br.com.minhavidapolitica.api.repository.LogAtualizacaoRepository;
import br.com.minhavidapolitica.api.repository.PoliticoRepository;
import br.com.minhavidapolitica.api.repository.PropostaRepository;
import br.com.minhavidapolitica.api.repository.UnidadeFederativaRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Importação de dados de uma fonte (módulo BES; RF13).
 *
 * <p>Cada político é validado antes de ser gravado; um registro inválido é rejeitado e contabilizado
 * sem derrubar a carga. A chave de atualização é nome + cargo + UF (políticos) e título (propostas
 * e ações): reimportar atualiza em vez de duplicar. Os dados ficam vinculados à fonte escolhida e
 * só aparecem ao cidadão quando ela estiver aprovada (RN05).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImportacaoService {

    private static final int LIMITE_MENSAGEM = 1000;
    private static final int LIMITE_MOTIVOS = 8;

    private final FonteDadosRepository fonteRepository;
    private final CargoRepository cargoRepository;
    private final UnidadeFederativaRepository ufRepository;
    private final PoliticoRepository politicoRepository;
    private final PropostaRepository propostaRepository;
    private final AcaoConcretizadaRepository acaoRepository;
    private final LogAtualizacaoRepository logRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<LogResponse> historico() {
        return logRepository.findTop50ByOrderByIniciadoEmDesc().stream().map(LogResponse::de).toList();
    }

    @Transactional
    public LogResponse importar(ImportacaoRequest req, String executadoPor) {
        FonteDados fonte = fonteRepository.findById(req.fonteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fonte não encontrada."));
        if (!fonte.isAtiva()) {
            throw new RegraNegocioException("A fonte está inativa. Ative-a antes de importar.");
        }

        LocalDateTime inicio = LocalDateTime.now(clock);
        Contadores contadores = new Contadores();

        List<ImportacaoPolitico> politicos = req.politicos();
        for (int i = 0; i < politicos.size(); i++) {
            try {
                processarPolitico(politicos.get(i), fonte, contadores, inicio);
            } catch (RegraNegocioException ex) {
                contadores.rejeitar("Político " + (i + 1) + ": " + ex.getMessage());
            }
        }

        LogAtualizacao registro = LogAtualizacao.builder()
                .fonte(fonte)
                .iniciadoEm(inicio)
                .finalizadoEm(LocalDateTime.now(clock))
                .status(contadores.status())
                .registrosCriados(contadores.criados)
                .registrosAtualizados(contadores.atualizados)
                .registrosRejeitados(contadores.rejeitados)
                .mensagem(montarMensagem(fonte, contadores))
                .executadoPor(executadoPor)
                .build();
        LogAtualizacao salvo = logRepository.save(registro);
        log.info("Importação #{} da fonte '{}' por {}: {} criados, {} atualizados, {} rejeitados",
                salvo.getId(), fonte.getNome(), executadoPor, contadores.criados, contadores.atualizados,
                contadores.rejeitados);
        return LogResponse.de(salvo);
    }

    private void processarPolitico(ImportacaoPolitico item, FonteDados fonte, Contadores c, LocalDateTime agora) {
        // 1) Validação completa antes de gravar qualquer coisa
        String nome = obrigatorio(item.nome(), 150, "nome");
        String codigoCargo = obrigatorio(item.cargo(), 30, "cargo");
        Cargo cargo = cargoRepository.findByCodigoIgnoreCase(codigoCargo)
                .filter(Cargo::isAtivo)
                .orElseThrow(() -> new RegraNegocioException(
                        "cargo \"" + codigoCargo + "\" não existe ou está inativo"));
        UnidadeFederativa uf = resolverUf(cargo, item.uf());
        String partido = opcional(item.partido(), 30, "partido");
        String fotoUrl = opcional(item.fotoUrl(), 500, "fotoUrl");
        String resumo = opcional(item.resumo(), 1000, "resumo");
        String linkFonte = opcional(item.linkFonte(), 500, "linkFonte");
        if (item.inicioMandato() != null && item.fimMandato() != null
                && item.fimMandato().isBefore(item.inicioMandato())) {
            throw new RegraNegocioException("fim do mandato anterior ao início");
        }

        // 2) Cria ou atualiza o político
        Optional<Politico> existente = uf == null
                ? politicoRepository.findFirstByNomeIgnoreCaseAndCargoIdAndUfIsNull(nome, cargo.getId())
                : politicoRepository.findFirstByNomeIgnoreCaseAndCargoIdAndUfId(nome, cargo.getId(), uf.getId());
        boolean novo = existente.isEmpty();
        Politico politico = existente.orElseGet(Politico::new);
        politico.setNome(nome);
        politico.setPartido(partido);
        politico.setFotoUrl(fotoUrl);
        politico.setResumo(resumo);
        politico.setCargo(cargo);
        politico.setUf(uf);
        politico.setInicioMandato(item.inicioMandato());
        politico.setFimMandato(item.fimMandato());
        politico.setEmExercicio(true);
        politico.setFonte(fonte);
        politico.setLinkFonte(linkFonte);
        politico.setAtualizadoEm(agora);
        politico = politicoRepository.save(politico);
        c.contar(novo);

        // 3) Propostas e ações: cada item é validado individualmente
        for (ImportacaoItem proposta : lista(item.propostas())) {
            try {
                importarProposta(politico, proposta, fonte, c, agora);
            } catch (RegraNegocioException ex) {
                c.rejeitar("Proposta de " + nome + ": " + ex.getMessage());
            }
        }
        for (ImportacaoItem acao : lista(item.acoes())) {
            try {
                importarAcao(politico, acao, fonte, c, agora);
            } catch (RegraNegocioException ex) {
                c.rejeitar("Ação de " + nome + ": " + ex.getMessage());
            }
        }
    }

    private void importarProposta(Politico politico, ImportacaoItem it, FonteDados fonte, Contadores c,
                                  LocalDateTime agora) {
        String titulo = obrigatorio(it.titulo(), 200, "título");
        String descricao = opcional(it.descricao(), 2000, "descrição");
        String link = opcional(it.linkFonte(), 500, "linkFonte");
        Optional<Proposta> existente = propostaRepository
                .findFirstByPoliticoIdAndTituloIgnoreCase(politico.getId(), titulo);
        Proposta proposta = existente.orElseGet(Proposta::new);
        proposta.setPolitico(politico);
        proposta.setTitulo(titulo);
        proposta.setDescricao(descricao);
        proposta.setDataReferencia(it.data());
        proposta.setFonte(fonte);
        proposta.setLinkFonte(link);
        proposta.setAtualizadoEm(agora);
        propostaRepository.save(proposta);
        c.contar(existente.isEmpty());
    }

    private void importarAcao(Politico politico, ImportacaoItem it, FonteDados fonte, Contadores c,
                              LocalDateTime agora) {
        String titulo = obrigatorio(it.titulo(), 200, "título");
        String descricao = opcional(it.descricao(), 2000, "descrição");
        String link = opcional(it.linkFonte(), 500, "linkFonte");
        if (it.data() == null) {
            throw new RegraNegocioException("\"" + titulo + "\" sem data (RF06 exige fonte e data)");
        }
        Optional<AcaoConcretizada> existente = acaoRepository
                .findFirstByPoliticoIdAndTituloIgnoreCase(politico.getId(), titulo);
        AcaoConcretizada acao = existente.orElseGet(AcaoConcretizada::new);
        acao.setPolitico(politico);
        acao.setTitulo(titulo);
        acao.setDescricao(descricao);
        acao.setDataAcao(it.data());
        acao.setFonte(fonte);
        acao.setLinkFonte(link);
        acao.setAtualizadoEm(agora);
        acaoRepository.save(acao);
        c.contar(existente.isEmpty());
    }

    private UnidadeFederativa resolverUf(Cargo cargo, String sigla) {
        String texto = sigla == null ? "" : sigla.trim();
        if (cargo.getEsfera() == Esfera.NACIONAL) {
            if (!texto.isEmpty()) {
                throw new RegraNegocioException("cargo nacional não deve ter UF");
            }
            return null;
        }
        if (texto.isEmpty()) {
            throw new RegraNegocioException("cargo estadual exige UF");
        }
        return ufRepository.findBySiglaIgnoreCase(texto)
                .orElseThrow(() -> new RegraNegocioException("UF \"" + texto + "\" não existe"));
    }

    private String montarMensagem(FonteDados fonte, Contadores c) {
        StringBuilder sb = new StringBuilder();
        sb.append(c.criados).append(" criados, ").append(c.atualizados).append(" atualizados, ")
                .append(c.rejeitados).append(" rejeitados.");
        if (!fonte.liberaExibicao()) {
            sb.append(" A fonte ainda não está aprovada pelo cliente: os dados ficam retidos e não aparecem ao cidadão (RN05).");
        }
        if (!c.motivos.isEmpty()) {
            sb.append(" Motivos: ").append(String.join("; ", c.motivos));
            if (c.rejeitados > c.motivos.size()) {
                sb.append("; entre outros.");
            }
        }
        String texto = sb.toString();
        return texto.length() <= LIMITE_MENSAGEM ? texto : texto.substring(0, LIMITE_MENSAGEM - 3) + "...";
    }

    private static <T> List<T> lista(List<T> itens) {
        return itens == null ? List.of() : itens;
    }

    private static String obrigatorio(String valor, int max, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new RegraNegocioException("campo \"" + campo + "\" é obrigatório");
        }
        String texto = valor.trim();
        if (texto.length() > max) {
            throw new RegraNegocioException("campo \"" + campo + "\" excede " + max + " caracteres");
        }
        return texto;
    }

    private static String opcional(String valor, int max, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String texto = valor.trim();
        if (texto.length() > max) {
            throw new RegraNegocioException("campo \"" + campo + "\" excede " + max + " caracteres");
        }
        return texto;
    }

    /** Acumulador da execução. */
    private static class Contadores {
        int criados;
        int atualizados;
        int rejeitados;
        final List<String> motivos = new ArrayList<>();

        void contar(boolean novo) {
            if (novo) {
                criados++;
            } else {
                atualizados++;
            }
        }

        void rejeitar(String motivo) {
            rejeitados++;
            if (motivos.size() < LIMITE_MOTIVOS) {
                motivos.add(motivo);
            }
        }

        StatusImportacao status() {
            if (rejeitados == 0) {
                return StatusImportacao.SUCESSO;
            }
            return (criados + atualizados) > 0 ? StatusImportacao.PARCIAL : StatusImportacao.ERRO;
        }
    }
}
