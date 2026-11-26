package br.com.minhavidapolitica.api.service;

import br.com.minhavidapolitica.api.domain.Politico;
import br.com.minhavidapolitica.api.domain.UnidadeFederativa;
import br.com.minhavidapolitica.api.dto.CoberturaUf;
import br.com.minhavidapolitica.api.dto.FontePendencia;
import br.com.minhavidapolitica.api.dto.LogResponse;
import br.com.minhavidapolitica.api.dto.ResumoPainel;
import br.com.minhavidapolitica.api.repository.AcaoConcretizadaRepository;
import br.com.minhavidapolitica.api.repository.CandidaturaRepository;
import br.com.minhavidapolitica.api.repository.FonteDadosRepository;
import br.com.minhavidapolitica.api.repository.LogAtualizacaoRepository;
import br.com.minhavidapolitica.api.repository.PoliticoRepository;
import br.com.minhavidapolitica.api.repository.PropostaRepository;
import br.com.minhavidapolitica.api.repository.UnidadeFederativaRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Geração de relatórios do painel (módulo ADS). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RelatorioService {

    public static final String COMPLETO = "COMPLETO";
    public static final String PARCIAL = "PARCIAL";
    public static final String SEM_DADOS = "SEM_DADOS";

    private static final String CARGO_GOVERNADOR = "GOVERNADOR";

    private final UnidadeFederativaRepository ufRepository;
    private final PoliticoRepository politicoRepository;
    private final PropostaRepository propostaRepository;
    private final AcaoConcretizadaRepository acaoRepository;
    private final FonteDadosRepository fonteRepository;
    private final LogAtualizacaoRepository logRepository;
    private final CandidaturaRepository candidaturaRepository;
    private final Clock clock;

    /**
     * Cobertura por UF considerando só o que o cidadão realmente vê (mandato atual + fonte aprovada).
     * COMPLETO = governador visível com ao menos uma proposta e uma ação;
     * PARCIAL = há algum dado visível; SEM_DADOS = nada visível.
     */
    public List<CoberturaUf> cobertura() {
        List<UnidadeFederativa> ufs = ufRepository.findAllByOrderBySiglaAsc();
        Map<String, List<Politico>> porUf = politicoRepository.listarEstaduaisVisiveis(LocalDate.now(clock)).stream()
                .collect(Collectors.groupingBy(p -> p.getUf().getSigla()));
        Set<Long> comProposta = new HashSet<>(propostaRepository.listarPoliticoIdsComItemVisivel());
        Set<Long> comAcao = new HashSet<>(acaoRepository.listarPoliticoIdsComItemVisivel());

        return ufs.stream().map(uf -> {
            List<Politico> lista = porUf.getOrDefault(uf.getSigla(), List.of());
            Politico governador = lista.stream()
                    .filter(p -> CARGO_GOVERNADOR.equals(p.getCargo().getCodigo()))
                    .findFirst().orElse(null);
            int representantes = (int) lista.stream()
                    .filter(p -> !CARGO_GOVERNADOR.equals(p.getCargo().getCodigo())).count();
            int propostas = (int) lista.stream().filter(p -> comProposta.contains(p.getId())).count();
            int acoes = (int) lista.stream().filter(p -> comAcao.contains(p.getId())).count();

            boolean governadorCompleto = governador != null
                    && comProposta.contains(governador.getId()) && comAcao.contains(governador.getId());
            String situacao = lista.isEmpty() ? SEM_DADOS : (governadorCompleto ? COMPLETO : PARCIAL);
            return new CoberturaUf(uf.getSigla(), uf.getNome(),
                    governador == null ? null : governador.getNome(), representantes, propostas, acoes, situacao);
        }).toList();
    }

    /** Registros vinculados a cada fonte, para mostrar o que está retido por falta de aprovação (RN05). */
    public List<FontePendencia> pendenciasPorFonte() {
        return fonteRepository.findAllByOrderByNomeAsc().stream()
                .map(f -> new FontePendencia(f.getId(), f.getNome(), f.isAprovada(), f.isAtiva(),
                        politicoRepository.countByFonteId(f.getId()),
                        propostaRepository.countByFonteId(f.getId()),
                        acaoRepository.countByFonteId(f.getId()),
                        candidaturaRepository.countByFonteId(f.getId())))
                .toList();
    }

    public ResumoPainel resumo() {
        List<CoberturaUf> cobertura = cobertura();
        int comGovernador = (int) cobertura.stream().filter(c -> c.governador() != null).count();
        List<LogResponse> ultimas = logRepository.findTop50ByOrderByIniciadoEmDesc().stream()
                .limit(5).map(LogResponse::de).toList();
        return new ResumoPainel(
                politicoRepository.count(), propostaRepository.count(), acaoRepository.count(),
                fonteRepository.countByAprovadaTrueAndAtivaTrue(), fonteRepository.countByAprovadaFalseOrAtivaFalse(),
                comGovernador, cobertura.size(), ultimas);
    }

    /** CSV para Excel em português: separador ponto e vírgula, UTF-8 com BOM. */
    public String coberturaCsv() {
        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append("UF;Estado;Governador;Outros representantes;Políticos com propostas;Políticos com ações;Situação\r\n");
        for (CoberturaUf c : cobertura()) {
            sb.append(c.sigla()).append(';')
                    .append(celula(c.nome())).append(';')
                    .append(celula(c.governador())).append(';')
                    .append(c.representantes()).append(';')
                    .append(c.comPropostas()).append(';')
                    .append(c.comAcoes()).append(';')
                    .append(c.situacao()).append("\r\n");
        }
        return sb.toString();
    }

    private static String celula(String valor) {
        if (valor == null) {
            return "";
        }
        if (valor.contains(";") || valor.contains("\"") || valor.contains("\n")) {
            return '"' + valor.replace("\"", "\"\"") + '"';
        }
        return valor;
    }
}
