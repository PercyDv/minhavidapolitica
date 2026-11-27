package br.com.minhavidapolitica.api.service;

import br.com.minhavidapolitica.api.domain.Candidatura;
import br.com.minhavidapolitica.api.domain.Cargo;
import br.com.minhavidapolitica.api.domain.Esfera;
import br.com.minhavidapolitica.api.domain.FonteDados;
import br.com.minhavidapolitica.api.domain.LogAtualizacao;
import br.com.minhavidapolitica.api.domain.SituacaoCandidatura;
import br.com.minhavidapolitica.api.domain.StatusImportacao;
import br.com.minhavidapolitica.api.domain.UnidadeFederativa;
import br.com.minhavidapolitica.api.dto.CandidaturaAdminResponse;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Candidaturas de uma eleição a presidente e governador (extensão fora do escopo validado, RN01).
 *
 * <p>Regras: só aparecem ao cidadão candidaturas de fonte aprovada e ativa (RN05); a ordem é sempre
 * alfabética, sem votos nem ranking (RN04); sem dado, vale a mensagem neutra do RF12.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CandidaturaService {

    static final String CARGO_PRESIDENTE = "PRESIDENTE";
    static final String CARGO_GOVERNADOR = "GOVERNADOR";
    private static final Set<String> CARGOS_PERMITIDOS = Set.of(CARGO_PRESIDENTE, CARGO_GOVERNADOR);
    private static final int LIMITE_MENSAGEM = 1000;
    private static final int LIMITE_MOTIVOS = 8;

    private final CandidaturaRepository candidaturaRepository;
    private final CargoRepository cargoRepository;
    private final UnidadeFederativaRepository ufRepository;
    private final FonteDadosRepository fonteRepository;
    private final LogAtualizacaoRepository logRepository;
    private final Clock clock;

    /** Candidatos a presidente na eleição do ano informado. */
    public Resultado<List<CandidaturaPublica>> presidente(int ano) {
        List<CandidaturaPublica> lista = candidaturaRepository.listarNacionais(ano, CARGO_PRESIDENTE).stream()
                .map(CandidaturaPublica::de).toList();
        return lista.isEmpty() ? Resultado.indisponivel() : Resultado.com(lista);
    }

    /** Candidatos a governador da UF na eleição do ano informado. */
    public Resultado<List<CandidaturaPublica>> governador(int ano, String sigla) {
        UnidadeFederativa uf = ufRepository.findBySiglaIgnoreCase(sigla)
                .orElseThrow(() -> new RecursoNaoEncontradoException("UF não encontrada: " + sigla));
        List<CandidaturaPublica> lista = candidaturaRepository.listarPorUf(ano, CARGO_GOVERNADOR, uf.getSigla()).stream()
                .map(CandidaturaPublica::de).toList();
        return lista.isEmpty() ? Resultado.indisponivel() : Resultado.com(lista);
    }

    public List<CandidaturaAdminResponse> listarAdmin(int ano) {
        return candidaturaRepository.listarAdmin(ano).stream().map(CandidaturaAdminResponse::de).toList();
    }

    @Transactional
    public LogResponse importar(ImportacaoCandidaturasRequest req, String executadoPor) {
        FonteDados fonte = fonteRepository.findById(req.fonteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fonte não encontrada."));
        if (!fonte.isAtiva()) {
            throw new RegraNegocioException("A fonte está inativa. Ative-a antes de importar.");
        }

        LocalDateTime inicio = LocalDateTime.now(clock);
        int ano = req.ano();
        Resumo resumo = new Resumo();
        List<CandidaturaImportada> itens = req.candidaturas();
        for (int i = 0; i < itens.size(); i++) {
            try {
                resumo.contar(processar(itens.get(i), fonte, ano, inicio));
            } catch (RegraNegocioException ex) {
                resumo.rejeitar("Candidatura " + (i + 1) + ": " + ex.getMessage());
            }
        }

        LogAtualizacao registro = LogAtualizacao.builder()
                .fonte(fonte)
                .iniciadoEm(inicio)
                .finalizadoEm(LocalDateTime.now(clock))
                .status(resumo.status())
                .registrosCriados(resumo.criados)
                .registrosAtualizados(resumo.atualizados)
                .registrosRejeitados(resumo.rejeitados)
                .mensagem(montarMensagem(fonte, ano, resumo))
                .executadoPor(executadoPor)
                .build();
        LogAtualizacao salvo = logRepository.save(registro);
        log.info("Importação de candidaturas #{} ({}), fonte '{}', por {}: {} criadas, {} atualizadas, {} rejeitadas",
                salvo.getId(), ano, fonte.getNome(), executadoPor, resumo.criados, resumo.atualizados, resumo.rejeitados);
        return LogResponse.de(salvo);
    }

    /** Valida e grava uma candidatura. Retorna true se foi criada, false se atualizada. */
    private boolean processar(CandidaturaImportada item, FonteDados fonte, int ano, LocalDateTime agora) {
        String sq = obrigatorio(item.sqCandidato(), 30, "sqCandidato");
        String nome = obrigatorio(item.nome(), 150, "nome");
        String numero = obrigatorio(item.numero(), 10, "numero");
        String codigoCargo = obrigatorio(item.cargo(), 30, "cargo").toUpperCase(Locale.ROOT);
        if (!CARGOS_PERMITIDOS.contains(codigoCargo)) {
            throw new RegraNegocioException("cargo \"" + codigoCargo + "\" não é aceito (somente PRESIDENTE e GOVERNADOR)");
        }
        Cargo cargo = cargoRepository.findByCodigoIgnoreCase(codigoCargo)
                .filter(Cargo::isAtivo)
                .orElseThrow(() -> new RegraNegocioException("cargo \"" + codigoCargo + "\" não existe ou está inativo"));
        UnidadeFederativa uf = resolverUf(cargo, item.uf());
        SituacaoCandidatura situacao = situacao(item.situacao());
        String nomeUrna = opcional(item.nomeUrna(), 150, "nomeUrna");
        String partido = opcional(item.partido(), 30, "partido");
        String link = opcional(item.linkFonte(), 500, "linkFonte");
        String foto = caminhoOuUrl(item.fotoUrl(), "fotoUrl");
        String plano = caminhoOuUrl(item.planoUrl(), "planoUrl");

        Optional<Candidatura> existente = candidaturaRepository.findFirstByAnoEleicaoAndSqCandidato(ano, sq);
        Candidatura candidatura = existente.orElseGet(Candidatura::new);
        candidatura.setAnoEleicao(ano);
        candidatura.setSqCandidato(sq);
        candidatura.setNumero(numero);
        candidatura.setNome(nome);
        candidatura.setNomeUrna(nomeUrna);
        candidatura.setPartido(partido);
        candidatura.setCargo(cargo);
        candidatura.setUf(uf);
        candidatura.setSituacao(situacao);
        candidatura.setFonte(fonte);
        candidatura.setLinkFonte(link);
        candidatura.setFotoUrl(foto);
        candidatura.setPlanoUrl(plano);
        candidatura.setAtualizadoEm(agora);
        candidaturaRepository.save(candidatura);
        return existente.isEmpty();
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

    private static SituacaoCandidatura situacao(String valor) {
        if (valor == null || valor.isBlank()) {
            return SituacaoCandidatura.SEM_RESULTADO;
        }
        try {
            return SituacaoCandidatura.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new RegraNegocioException("situacao \"" + valor.trim()
                    + "\" inválida (use ELEITO, NAO_ELEITO, SEGUNDO_TURNO ou SEM_RESULTADO)");
        }
    }

    private String montarMensagem(FonteDados fonte, int ano, Resumo r) {
        StringBuilder sb = new StringBuilder("Eleição " + ano + ": ");
        sb.append(r.criados).append(" criadas, ").append(r.atualizados).append(" atualizadas, ")
                .append(r.rejeitados).append(" rejeitadas.");
        if (!fonte.liberaExibicao()) {
            sb.append(" A fonte ainda não está aprovada pelo cliente: as candidaturas ficam retidas e não aparecem ao cidadão (RN05).");
        }
        if (!r.motivos.isEmpty()) {
            sb.append(" Motivos: ").append(String.join("; ", r.motivos));
            if (r.rejeitados > r.motivos.size()) {
                sb.append("; entre outros.");
            }
        }
        String texto = sb.toString();
        return texto.length() <= LIMITE_MENSAGEM ? texto : texto.substring(0, LIMITE_MENSAGEM - 3) + "...";
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

    /**
     * Foto e plano: aceita URL http(s) ou um caminho em assets/ (arquivos do front). Qualquer outro
     * esquema (javascript:, data:, file:) é recusado, porque o valor vira link e imagem na página pública.
     */
    private static String caminhoOuUrl(String valor, String campo) {
        String texto = opcional(valor, 500, campo);
        if (texto == null) {
            return null;
        }
        boolean url = texto.startsWith("http://") || texto.startsWith("https://");
        boolean relativo = texto.startsWith("assets/") && !texto.contains("..");
        boolean semEspaco = texto.chars().noneMatch(Character::isWhitespace);
        if (!(url || relativo) || !semEspaco) {
            throw new RegraNegocioException("campo \"" + campo + "\" deve ser uma URL http(s) ou um caminho em assets/");
        }
        return texto;
    }

    /** Acumulador da execução. */
    private static class Resumo {
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
