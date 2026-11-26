import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Converte a base de candidatos do TSE (consulta_cand_AAAA.zip ou um CSV) no arquivo de importação de
 * candidaturas do MinhaVidaPolitica, somente para presidente e governador. Roda direto com o JDK:
 *
 *   java scripts/tse/ConversorCandidatos.java --entrada consulta_cand_2026.zip --fonte-id 3
 *
 * O conversor lê só as colunas necessárias. CPF, e-mail e demais dados pessoais nunca são lidos nem gravados.
 * Veja scripts/tse/README.md.
 */
public class ConversorCandidatos {

    static final class Erro extends RuntimeException {
        Erro(String mensagem) {
            super(mensagem);
        }
    }

    static final class Config {
        final List<Path> entradas = new ArrayList<>();
        Path saida = Path.of("candidaturas-tse.json");
        Integer ano;
        Long fonteId;
        String link;
        boolean todasSituacoes;
    }

    /** Aliases aceitos para cada coluna (os nomes do TSE vêm primeiro). */
    private static final Map<String, List<String>> COLUNAS = new LinkedHashMap<>();
    static {
        COLUNAS.put("cargo", List.of("DS_CARGO"));
        COLUNAS.put("uf", List.of("SG_UF"));
        COLUNAS.put("sq", List.of("SQ_CANDIDATO"));
        COLUNAS.put("nome", List.of("NM_CANDIDATO"));
        COLUNAS.put("numero", List.of("NR_CANDIDATO"));
        COLUNAS.put("nomeUrna", List.of("NM_URNA_CANDIDATO"));
        COLUNAS.put("partido", List.of("SG_PARTIDO"));
        COLUNAS.put("turno", List.of("NR_TURNO"));
        COLUNAS.put("ano", List.of("ANO_ELEICAO"));
        COLUNAS.put("situacaoCand", List.of("DS_SITUACAO_CANDIDATURA"));
        COLUNAS.put("detalheSituacao", List.of("DS_DETALHE_SITUACAO_CAND"));
        COLUNAS.put("situacaoTotal", List.of("DS_SIT_TOT_TURNO"));
    }
    private static final List<String> OBRIGATORIAS = List.of("cargo", "uf", "sq", "nome", "numero");

    public static void main(String[] args) {
        try {
            System.exit(executar(args));
        } catch (Erro e) {
            boolean ajuda = e.getMessage().startsWith("Uso:");
            System.err.println(ajuda ? e.getMessage() : "ERRO: " + e.getMessage());
            System.exit(1);
        } catch (java.nio.file.NoSuchFileException e) {
            System.err.println("ERRO: arquivo não encontrado: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("ERRO de leitura ou escrita: " + e.getMessage());
            System.exit(1);
        }
    }

    static int executar(String[] args) throws IOException {
        Config cfg = lerArgumentos(args);
        Map<String, Map<String, String>> finais = new HashMap<>(); // sq -> linha do maior turno
        Map<String, Integer> turnoDe = new HashMap<>();
        Map<String, Integer> situacoesVistas = new TreeMap<>();
        Map<String, Integer> excluidas = new TreeMap<>();
        Integer anoArquivo = null;
        int linhasLidas = 0;

        for (Path entrada : cfg.entradas) {
            for (String[] conteudo : lerCsvs(entrada)) {
                List<String[]> linhas = Csv.ler(conteudo[1]);
                if (linhas.isEmpty()) {
                    continue;
                }
                Map<String, Integer> idx = indices(linhas.get(0), conteudo[0]);
                for (int i = 1; i < linhas.size(); i++) {
                    String[] l = linhas.get(i);
                    if (l.length == 1 && l[0].isBlank()) {
                        continue;
                    }
                    linhasLidas++;
                    String cargo = norm(valor(l, idx, "cargo"));
                    if (!cargo.equals("PRESIDENTE") && !cargo.equals("GOVERNADOR")) {
                        continue;
                    }
                    String sit = norm(primeiroNaoVazio(valor(l, idx, "detalheSituacao"), valor(l, idx, "situacaoCand")));
                    // Depois da eleição o TSE costuma preencher "#NE" (não divulgado): isso não informa nada.
                    boolean informativa = !(sit.isEmpty() || sit.equals("NE") || sit.equals("NULO"));
                    situacoesVistas.merge(informativa ? sit : "(não informada)", 1, Integer::sum);
                    if (informativa && !cfg.todasSituacoes && !(sit.startsWith("DEFERIDO") || sit.equals("APTO"))) {
                        excluidas.merge(sit, 1, Integer::sum);
                        continue;
                    }
                    String sq = valor(l, idx, "sq").trim();
                    int turno = inteiro(valor(l, idx, "turno"), 1);
                    if (turnoDe.getOrDefault(sq, 0) > turno) {
                        continue;
                    }
                    turnoDe.put(sq, turno);
                    Map<String, String> reg = new HashMap<>();
                    for (String chave : COLUNAS.keySet()) {
                        reg.put(chave, valor(l, idx, chave).trim());
                    }
                    finais.put(sq, reg);
                    if (anoArquivo == null) {
                        anoArquivo = inteiro(reg.get("ano"), 0);
                    }
                }
            }
        }

        Integer ano = cfg.ano != null ? cfg.ano : (anoArquivo != null && anoArquivo > 0 ? anoArquivo : null);
        if (ano == null) {
            throw new Erro("Não foi possível descobrir o ano da eleição. Informe --ano.");
        }
        System.out.println("Linhas lidas: " + linhasLidas);
        System.out.println("Situação da candidatura nas linhas de presidente e governador: " + situacoesVistas);
        if (!excluidas.isEmpty()) {
            System.out.println("Fora do arquivo por situação não deferida: " + excluidas
                    + " (use --todas-situacoes para incluí-las)");
        }
        if (finais.isEmpty()) {
            throw new Erro("Nenhuma candidatura de presidente ou governador foi encontrada. Confira o arquivo e a situação das candidaturas acima.");
        }

        boolean temResultado = finais.values().stream()
                .anyMatch(r -> !situacao(r.get("situacaoTotal")).equals("SEM_RESULTADO"));
        List<String> semResultado = new ArrayList<>();
        if (temResultado && !cfg.todasSituacoes) {
            java.util.Iterator<Map<String, String>> it = finais.values().iterator();
            while (it.hasNext()) {
                Map<String, String> r = it.next();
                if (situacao(r.get("situacaoTotal")).equals("SEM_RESULTADO")) {
                    boolean pres = norm(r.get("cargo")).equals("PRESIDENTE");
                    semResultado.add((pres ? "PRESIDENTE" : "GOVERNADOR " + r.get("uf").toUpperCase(Locale.ROOT))
                            + ": " + tituloCase(r.get("nome")));
                    it.remove();
                }
            }
            if (!semResultado.isEmpty()) {
                System.out.println("Sem resultado apurado (candidatura indeferida, substituída, cancelada ou desistente), fora do arquivo: "
                        + semResultado.size() + " (use --todas-situacoes para incluí-las)");
                semResultado.stream().sorted().forEach(n -> System.out.println("  - " + n));
            }
        }

        List<Map<String, Object>> lista = new ArrayList<>();
        for (Map<String, String> r : finais.values()) {
            boolean presidente = norm(r.get("cargo")).equals("PRESIDENTE");
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("sqCandidato", r.get("sq"));
            c.put("nome", tituloCase(r.get("nome")));
            if (!r.get("nomeUrna").isEmpty()) {
                c.put("nomeUrna", tituloCase(r.get("nomeUrna")));
            }
            c.put("numero", r.get("numero"));
            if (!r.get("partido").isEmpty()) {
                c.put("partido", r.get("partido"));
            }
            c.put("cargo", presidente ? "PRESIDENTE" : "GOVERNADOR");
            if (!presidente) {
                c.put("uf", r.get("uf").toUpperCase(Locale.ROOT));
            }
            c.put("situacao", situacao(r.get("situacaoTotal")));
            if (cfg.link != null) {
                c.put("linkFonte", cfg.link);
            }
            lista.add(c);
        }
        lista.sort(Comparator.comparing((Map<String, Object> m) -> (String) m.get("cargo"))
                .thenComparing(m -> String.valueOf(m.get("uf")))
                .thenComparing(m -> (String) m.get("nome")));

        Map<String, Object> raiz = new LinkedHashMap<>();
        if (cfg.fonteId != null) {
            raiz.put("fonteId", cfg.fonteId);
        }
        raiz.put("ano", ano);
        raiz.put("candidaturas", lista);
        StringBuilder sb = new StringBuilder();
        Json.escrever(raiz, sb, 0);
        Files.writeString(cfg.saida, sb.append('\n').toString(), StandardCharsets.UTF_8);

        Map<String, Integer> porSituacao = new TreeMap<>();
        Map<String, Integer> porCargo = new TreeMap<>();
        for (Map<String, Object> c : lista) {
            porSituacao.merge((String) c.get("situacao"), 1, Integer::sum);
            porCargo.merge((String) c.get("cargo"), 1, Integer::sum);
        }
        System.out.println("Candidaturas geradas: " + lista.size() + " " + porCargo + " | situação final: " + porSituacao);
        System.out.println("Arquivo de saída: " + cfg.saida.toAbsolutePath());
        if (!temResultado) {
            System.out.println("Aviso: nenhuma candidatura trouxe resultado final. O arquivo do TSE pode não ter sido atualizado depois da eleição, e todas ficaram como SEM_RESULTADO.");
        }
        if (cfg.fonteId == null) {
            System.out.println("Aviso: sem --fonte-id, o arquivo ainda precisa do campo \"fonteId\" antes de ser enviado à API.");
        }
        System.out.println();
        System.out.println("Próximo passo: envie para POST /api/admin/importacoes/candidaturas (veja scripts/tse/README.md).");
        System.out.println("Só aparece ao cidadão depois que o cliente aprovar a fonte.");
        return 0;
    }

    /** Resultado final a partir do texto do TSE. Sem votos e sem ranking. */
    static String situacao(String totalizacao) {
        String n = norm(totalizacao);
        if (n.startsWith("NAO ELEITO")) {
            return "NAO_ELEITO";
        }
        if (n.startsWith("ELEITO")) {
            return "ELEITO";
        }
        if (n.startsWith("2 TURNO")) {
            return "SEGUNDO_TURNO";
        }
        return "SEM_RESULTADO";
    }

    private static Map<String, Integer> indices(String[] cabecalho, String origem) {
        Map<String, Integer> nomes = new HashMap<>();
        for (int i = 0; i < cabecalho.length; i++) {
            nomes.put(norm(cabecalho[i]).replace(' ', '_'), i);
        }
        Map<String, Integer> idx = new HashMap<>();
        for (Map.Entry<String, List<String>> e : COLUNAS.entrySet()) {
            for (String alias : e.getValue()) {
                Integer pos = nomes.get(alias);
                if (pos != null) {
                    idx.put(e.getKey(), pos);
                    break;
                }
            }
        }
        for (String obrigatoria : OBRIGATORIAS) {
            if (!idx.containsKey(obrigatoria)) {
                throw new Erro(origem + ": coluna " + COLUNAS.get(obrigatoria).get(0) + " não encontrada. "
                        + "Colunas do arquivo: " + String.join(", ", cabecalho));
            }
        }
        return idx;
    }

    private static String valor(String[] linha, Map<String, Integer> idx, String chave) {
        Integer pos = idx.get(chave);
        return (pos == null || pos >= linha.length || linha[pos] == null) ? "" : linha[pos];
    }

    private static String primeiroNaoVazio(String a, String b) {
        return (a != null && !a.isBlank()) ? a : (b == null ? "" : b);
    }

    private static int inteiro(String texto, int padrao) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            return padrao;
        }
    }

    /** Sem acento, em maiúsculas, só letras, números e espaço único. */
    static String norm(String texto) {
        if (texto == null) {
            return "";
        }
        String sem = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sem.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }

    /** Primeira letra em maiúscula, e também depois de '.', '-' e apóstrofo (ex.: "d'ávila" vira "D'Ávila"). */
    private static String maiusculaNoInicio(String palavra) {
        StringBuilder sb = new StringBuilder();
        boolean maiuscula = true;
        for (char c : palavra.toCharArray()) {
            sb.append(maiuscula && Character.isLetter(c) ? Character.toUpperCase(c) : c);
            maiuscula = c == '.' || c == '-' || c == '\'' || (maiuscula && !Character.isLetter(c));
        }
        return sb.toString();
    }

    static String tituloCase(String texto) {
        String[] partes = texto.trim().toLowerCase(Locale.forLanguageTag("pt-BR")).split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < partes.length; i++) {
            String p = partes[i];
            if (p.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            String sigla = p.replaceAll("[^\\p{L}]", "");
            boolean titulo = p.contains(".") || sigla.equals("jr") || sigla.equals("sr") || sigla.equals("sra")
                    || sigla.equals("dr") || sigla.equals("dra");
            boolean semVogal = !titulo && !sigla.isEmpty() && sigla.length() <= 4
                    && !sigla.matches(".*[aeiouáéíóúâêôãõà].*");
            boolean romano = sigla.matches("(?i)ii|iii|iv|vi|vii|viii|ix");
            if (semVogal || romano) {
                sb.append(p.toUpperCase(Locale.forLanguageTag("pt-BR")));
                continue;
            }
            boolean particula = p.equals("da") || p.equals("de") || p.equals("do") || p.equals("das")
                    || p.equals("dos") || p.equals("e");
            if (i > 0 && particula) {
                sb.append(p);
            } else {
                sb.append(maiusculaNoInicio(p));
            }
        }
        return sb.toString();
    }

    /** Devolve pares {nome do arquivo, conteúdo em texto} dos CSVs da entrada (ZIP ou CSV). */
    private static List<String[]> lerCsvs(Path entrada) throws IOException {
        List<String[]> saida = new ArrayList<>();
        String nome = entrada.getFileName().toString();
        if (nome.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            try (InputStream in = Files.newInputStream(entrada); ZipInputStream zip = new ZipInputStream(in)) {
                ZipEntry e;
                while ((e = zip.getNextEntry()) != null) {
                    if (!e.isDirectory() && e.getName().toLowerCase(Locale.ROOT).endsWith(".csv")) {
                        saida.add(new String[] {e.getName(), decodificar(zip.readAllBytes())});
                    }
                }
            }
            if (saida.isEmpty()) {
                throw new Erro(nome + ": o ZIP não contém arquivos CSV.");
            }
        } else {
            saida.add(new String[] {nome, decodificar(Files.readAllBytes(entrada))});
        }
        return saida;
    }

    /** O TSE publica em ISO-8859-1; se o arquivo for UTF-8 válido, usa UTF-8. */
    private static String decodificar(byte[] bytes) {
        try {
            String texto = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
            return texto.startsWith("\uFEFF") ? texto.substring(1) : texto;
        } catch (CharacterCodingException e) {
            return new String(bytes, StandardCharsets.ISO_8859_1);
        }
    }

    private static Config lerArgumentos(String[] args) {
        if (args.length == 0) {
            throw new Erro(ajuda());
        }
        Config cfg = new Config();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            switch (a) {
                case "--ajuda", "-h", "--help" -> throw new Erro(ajuda());
                case "--entrada" -> cfg.entradas.add(Path.of(valor(args, ++i, a)));
                case "--saida" -> cfg.saida = Path.of(valor(args, ++i, a));
                case "--ano" -> cfg.ano = numero(valor(args, ++i, a), a).intValue();
                case "--fonte-id" -> cfg.fonteId = numero(valor(args, ++i, a), a);
                case "--link" -> cfg.link = valor(args, ++i, a);
                case "--todas-situacoes" -> cfg.todasSituacoes = true;
                default -> throw new Erro("Opção desconhecida: " + a + "\n\n" + ajuda());
            }
        }
        if (cfg.entradas.isEmpty()) {
            throw new Erro("Informe ao menos um arquivo com --entrada.\n\n" + ajuda());
        }
        return cfg;
    }

    private static String valor(String[] args, int i, String opcao) {
        if (i >= args.length) {
            throw new Erro("A opção " + opcao + " precisa de um valor.");
        }
        return args[i];
    }

    private static Long numero(String v, String opcao) {
        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            throw new Erro("Valor inválido para " + opcao + ": " + v);
        }
    }

    private static String ajuda() {
        return """
                Uso: java scripts/tse/ConversorCandidatos.java --entrada <consulta_cand_AAAA.zip ou .csv> [opções]

                  --entrada <arquivo>   ZIP ou CSV de candidatos do TSE. Pode repetir a opção.
                  --saida <arquivo>     Arquivo gerado (padrão: candidaturas-tse.json)
                  --ano <aaaa>          Ano da eleição (padrão: lido do arquivo)
                  --fonte-id <n>        Id da fonte cadastrada no sistema; deixa o arquivo pronto para enviar à API
                  --link <url>          Link guardado em cada candidatura
                  --todas-situacoes     Inclui também candidaturas indeferidas, canceladas etc.""";
    }

    /** Leitor de CSV com ponto e vírgula, aspas e quebras de linha dentro de campos. */
    static final class Csv {
        static List<String[]> ler(String texto) {
            List<String[]> linhas = new ArrayList<>();
            List<String> campos = new ArrayList<>();
            StringBuilder campo = new StringBuilder();
            boolean aspas = false;
            for (int i = 0; i < texto.length(); i++) {
                char c = texto.charAt(i);
                if (aspas) {
                    if (c == '"') {
                        if (i + 1 < texto.length() && texto.charAt(i + 1) == '"') {
                            campo.append('"');
                            i++;
                        } else {
                            aspas = false;
                        }
                    } else {
                        campo.append(c);
                    }
                } else if (c == '"') {
                    aspas = true;
                } else if (c == ';') {
                    campos.add(campo.toString());
                    campo.setLength(0);
                } else if (c == '\n' || c == '\r') {
                    if (c == '\r' && i + 1 < texto.length() && texto.charAt(i + 1) == '\n') {
                        i++;
                    }
                    campos.add(campo.toString());
                    campo.setLength(0);
                    linhas.add(campos.toArray(new String[0]));
                    campos = new ArrayList<>();
                } else {
                    campo.append(c);
                }
            }
            if (campo.length() > 0 || !campos.isEmpty()) {
                campos.add(campo.toString());
                linhas.add(campos.toArray(new String[0]));
            }
            return linhas;
        }
    }

    /** Escritor de JSON mínimo. */
    static final class Json {
        @SuppressWarnings("unchecked")
        static void escrever(Object o, StringBuilder sb, int nivel) {
            String recuo = "  ".repeat(nivel + 1);
            String recuoFim = "  ".repeat(nivel);
            if (o instanceof Map<?, ?> m) {
                sb.append("{\n");
                int n = 0;
                for (Map.Entry<String, Object> e : ((Map<String, Object>) m).entrySet()) {
                    sb.append(recuo).append(texto(e.getKey())).append(": ");
                    escrever(e.getValue(), sb, nivel + 1);
                    sb.append(++n < m.size() ? ",\n" : "\n");
                }
                sb.append(recuoFim).append('}');
            } else if (o instanceof List<?> l) {
                sb.append("[\n");
                for (int k = 0; k < l.size(); k++) {
                    sb.append(recuo);
                    escrever(l.get(k), sb, nivel + 1);
                    sb.append(k + 1 < l.size() ? ",\n" : "\n");
                }
                sb.append(recuoFim).append(']');
            } else if (o instanceof String t) {
                sb.append(texto(t));
            } else {
                sb.append(o);
            }
        }

        private static String texto(String t) {
            StringBuilder sb = new StringBuilder("\"");
            for (char c : t.toCharArray()) {
                switch (c) {
                    case '"' -> sb.append("\\\"");
                    case '\\' -> sb.append("\\\\");
                    case '\n' -> sb.append("\\n");
                    case '\r' -> sb.append("\\r");
                    case '\t' -> sb.append("\\t");
                    default -> {
                        if (c < 0x20) {
                            sb.append(String.format("\\u%04x", (int) c));
                        } else {
                            sb.append(c);
                        }
                    }
                }
            }
            return sb.append('"').toString();
        }
    }
}
