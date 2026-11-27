import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Converte o "Arquivo de resultado de eleitos" (EA10) do TSE para o formato de importação do
 * MinhaVidaPolitica. Roda direto com o JDK, sem instalar nada:
 *
 *   java scripts/tse/ConversorTse.java --entrada br-c0003-e000000-e.json --saida importacao-tse.json
 *
 * Veja scripts/tse/README.md para todas as opções.
 */
public class ConversorTse {

    /** Código de cargo do TSE para o código de cargo do sistema. */
    private static final Map<Integer, String> CARGOS = Map.of(
            1, "PRESIDENTE", 3, "GOVERNADOR", 5, "SENADOR", 6, "DEPUTADO_FEDERAL");

    private static final Set<String> PARTICULAS = Set.of("da", "de", "do", "das", "dos", "e");

    /** Vigência dos mandatos {início, fim} por ano de eleição e cargo (EC 111/2021 e regras gerais). */
    private static final Map<String, String[]> MANDATOS = Map.ofEntries(
            Map.entry("2026/GOVERNADOR", new String[] {"2027-01-06", "2031-01-05"}),
            Map.entry("2026/PRESIDENTE", new String[] {"2027-01-05", "2031-01-04"}),
            Map.entry("2026/SENADOR", new String[] {"2027-02-01", "2035-01-31"}),
            Map.entry("2026/DEPUTADO_FEDERAL", new String[] {"2027-02-01", "2031-01-31"}),
            Map.entry("2022/GOVERNADOR", new String[] {"2023-01-01", "2027-01-05"}),
            Map.entry("2022/PRESIDENTE", new String[] {"2023-01-01", "2027-01-04"}),
            Map.entry("2022/SENADOR", new String[] {"2023-02-01", "2031-01-31"}),
            Map.entry("2022/DEPUTADO_FEDERAL", new String[] {"2023-02-01", "2027-01-31"}));

    static final class Erro extends RuntimeException {
        Erro(String mensagem) {
            super(mensagem);
        }
    }

    static final class Config {
        final List<Path> entradas = new ArrayList<>();
        Path saida = Path.of("importacao-tse.json");
        int ano = 2026;
        Set<String> cargos = new LinkedHashSet<>(Set.of("GOVERNADOR"));
        String inicio;
        String fim;
        String link = "https://dadosabertos.tse.jus.br/";
        boolean nomeUrna;
        boolean permitirSimulado;
    }

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
        List<Map<String, Object>> politicos = new ArrayList<>();
        List<String> avisos = new ArrayList<>();
        Set<String> chaves = new HashSet<>();

        for (Path arquivo : cfg.entradas) {
            processarArquivo(arquivo, cfg, politicos, avisos, chaves);
        }
        if (politicos.isEmpty()) {
            avisos.forEach(a -> System.err.println("  aviso: " + a));
            throw new Erro("Nenhum registro foi gerado. Confira os arquivos, o cargo (--cargos) e os avisos acima.");
        }

        Map<String, Object> raiz = new LinkedHashMap<>();
        raiz.put("politicos", politicos);
        StringBuilder sb = new StringBuilder();
        Json.escrever(raiz, sb, 0);
        Files.writeString(cfg.saida, sb.append('\n').toString(), StandardCharsets.UTF_8);

        Map<String, Integer> porCargo = new LinkedHashMap<>();
        for (Map<String, Object> p : politicos) {
            porCargo.merge((String) p.get("cargo"), 1, Integer::sum);
        }
        System.out.println("Registros gerados: " + politicos.size() + " " + porCargo);
        System.out.println("Arquivo de saída:  " + cfg.saida.toAbsolutePath());
        if (!avisos.isEmpty()) {
            System.out.println("Avisos (" + avisos.size() + "):");
            avisos.forEach(a -> System.out.println("  - " + a));
        }
        System.out.println();
        System.out.println("Próximos passos: revise o arquivo gerado, cadastre a fonte do TSE em \"Fontes de dados\" e use");
        System.out.println("\"Importação de dados\" (ou POST /api/admin/importacoes). Os dados só aparecem ao cidadão");
        System.out.println("depois que o cliente aprovar a fonte e a data de início do mandato chegar.");
        return 0;
    }

    private static void processarArquivo(Path arquivo, Config cfg, List<Map<String, Object>> saida,
                                         List<String> avisos, Set<String> chaves) throws IOException {
        String texto = Files.readString(arquivo, StandardCharsets.UTF_8);
        if (texto.startsWith("\uFEFF")) {
            texto = texto.substring(1);
        }
        Map<String, Object> raiz = Json.mapa(new Json(texto).ler(), arquivo.getFileName().toString());
        String nomeArquivo = arquivo.getFileName().toString();

        if ("s".equalsIgnoreCase(Json.texto(raiz.get("f"))) && !cfg.permitirSimulado) {
            throw new Erro(nomeArquivo + ": arquivo gerado no SIMULADO do TSE, não é resultado real. "
                    + "Use --permitir-simulado só para testes.");
        }
        Integer codigoTse = Json.inteiro(raiz.get("cdcar"));
        String cargo = codigoTse == null ? null : CARGOS.get(codigoTse);
        if (cargo == null) {
            avisos.add(nomeArquivo + ": código de cargo " + raiz.get("cdcar") + " não é tratado por este conversor. Arquivo ignorado.");
            return;
        }
        if (!cfg.cargos.contains(cargo)) {
            avisos.add(nomeArquivo + ": cargo " + cargo + " ignorado (use --cargos para incluí-lo).");
            return;
        }
        String[] mandato = definirMandato(cfg, cargo);
        boolean nacional = cargo.equals("PRESIDENTE");
        boolean cargoUnico = cargo.equals("GOVERNADOR") || cargo.equals("PRESIDENTE");

        for (Object item : Json.lista(raiz.get("abr"))) {
            Map<String, Object> abr = Json.mapa(item, "abr");
            String uf = Json.texto(abr.get("cdabr"));
            String local = nacional ? "Brasil" : String.valueOf(uf);
            if (!nacional && !"uf".equalsIgnoreCase(Json.texto(abr.get("tpabr")))) {
                avisos.add(nomeArquivo + ": abrangência " + local + " não é de UF. Ignorada.");
                continue;
            }
            if ("s".equalsIgnoreCase(Json.texto(abr.get("esae")))) {
                avisos.add(cargo + " " + local + ": a totalização não atribuiu eleito (esae = s). Ignorado.");
                continue;
            }
            List<Object> candidatos = Json.lista(abr.get("cand"));
            if (candidatos.isEmpty()) {
                avisos.add(cargo + " " + local + ": nenhum eleito listado no arquivo. Ignorado.");
                continue;
            }
            if (cargoUnico && candidatos.size() != 1) {
                avisos.add(cargo + " " + local + ": " + candidatos.size() + " candidatos listados (segundo turno ainda não "
                        + "decidido?). Ignorado; use o arquivo do 2º turno quando ele sair.");
                continue;
            }
            for (Object c : candidatos) {
                Map<String, Object> cand = Json.mapa(c, "cand");
                String nome = cfg.nomeUrna ? primeiro(cand.get("nmu"), cand.get("nm")) : primeiro(cand.get("nm"), cand.get("nmu"));
                if (nome == null) {
                    avisos.add(cargo + " " + local + ": candidato sem nome. Ignorado.");
                    continue;
                }
                String nomeFinal = tituloCase(nome);
                String chave = cargo + "|" + (nacional ? "" : uf) + "|" + nomeFinal.toLowerCase(Locale.ROOT);
                if (!chaves.add(chave)) {
                    avisos.add(cargo + " " + local + ": \"" + nomeFinal + "\" repetido. Mantido apenas o primeiro.");
                    continue;
                }
                saida.add(montar(cfg, cargo, nacional ? null : uf, nomeFinal, cand, mandato));
            }
        }
    }

    private static Map<String, Object> montar(Config cfg, String cargo, String uf, String nome,
                                              Map<String, Object> cand, String[] mandato) {
        StringBuilder resumo = new StringBuilder("Eleito(a) em " + cfg.ano
                + " pelo resultado oficial do TSE. Dado de eleição: não confirma o exercício atual do cargo.");
        for (Object v : Json.lista(cand.get("vs"))) {
            Map<String, Object> vice = Json.mapa(v, "vs");
            String nomeVice = primeiro(vice.get("nm"), vice.get("nmu"));
            if ("v".equalsIgnoreCase(Json.texto(vice.get("tp"))) && nomeVice != null) {
                resumo.append(" Vice eleito(a): ").append(tituloCase(nomeVice)).append('.');
            }
        }
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("nome", nome);
        String partido = Json.texto(cand.get("sgp"));
        if (partido != null && !partido.isBlank()) {
            p.put("partido", partido.trim());
        }
        p.put("cargo", cargo);
        if (uf != null) {
            p.put("uf", uf.toUpperCase(Locale.ROOT));
        }
        p.put("resumo", resumo.toString());
        p.put("inicioMandato", mandato[0]);
        p.put("fimMandato", mandato[1]);
        p.put("linkFonte", cfg.link);
        return p;
    }

    private static String[] definirMandato(Config cfg, String cargo) {
        if (cfg.inicio != null && cfg.fim != null) {
            return new String[] {cfg.inicio, cfg.fim};
        }
        String[] padrao = MANDATOS.get(cfg.ano + "/" + cargo);
        if (padrao == null) {
            throw new Erro("Não há datas de mandato padrão para a eleição de " + cfg.ano + ". Informe --inicio e --fim (aaaa-mm-dd).");
        }
        return new String[] {cfg.inicio != null ? cfg.inicio : padrao[0], cfg.fim != null ? cfg.fim : padrao[1]};
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
                case "--ano" -> cfg.ano = numero(valor(args, ++i, a), a);
                case "--cargos" -> {
                    cfg.cargos = new LinkedHashSet<>();
                    for (String c : valor(args, ++i, a).split(",")) {
                        String codigo = c.trim().toUpperCase(Locale.ROOT);
                        if (!CARGOS.containsValue(codigo)) {
                            throw new Erro("Cargo desconhecido em --cargos: " + c + ". Use PRESIDENTE, GOVERNADOR, SENADOR ou DEPUTADO_FEDERAL.");
                        }
                        cfg.cargos.add(codigo);
                    }
                }
                case "--inicio" -> cfg.inicio = data(valor(args, ++i, a), a);
                case "--fim" -> cfg.fim = data(valor(args, ++i, a), a);
                case "--link" -> cfg.link = valor(args, ++i, a);
                case "--nome-urna" -> cfg.nomeUrna = true;
                case "--permitir-simulado" -> cfg.permitirSimulado = true;
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

    private static int numero(String v, String opcao) {
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            throw new Erro("Valor inválido para " + opcao + ": " + v);
        }
    }

    private static String data(String v, String opcao) {
        if (!v.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new Erro("Data inválida em " + opcao + ": " + v + " (use aaaa-mm-dd).");
        }
        return v;
    }

    private static String ajuda() {
        return """
                Uso: java scripts/tse/ConversorTse.java --entrada <arquivo.json> [opções]

                  --entrada <arquivo>     Arquivo de eleitos do TSE (EA10). Pode repetir a opção.
                  --saida <arquivo>       Arquivo gerado (padrão: importacao-tse.json)
                  --ano <aaaa>            Ano da eleição (padrão: 2026). Define as datas de mandato.
                  --cargos <lista>        GOVERNADOR (padrão), SENADOR, DEPUTADO_FEDERAL, PRESIDENTE; separe por vírgula
                  --inicio <aaaa-mm-dd>   Início do mandato (substitui o padrão do ano)
                  --fim <aaaa-mm-dd>      Fim do mandato (substitui o padrão do ano)
                  --link <url>            Link guardado em cada registro (padrão: portal de dados abertos do TSE)
                  --nome-urna             Usa o nome de urna em vez do nome completo
                  --permitir-simulado     Aceita arquivos do simulado (somente testes)""";
    }

    private static String primeiro(Object a, Object b) {
        String x = Json.texto(a);
        if (x != null && !x.isBlank()) {
            return x.trim();
        }
        String y = Json.texto(b);
        return (y != null && !y.isBlank()) ? y.trim() : null;
    }

    /** "FULANO DE TAL" vira "Fulano de Tal". */
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
            if (i > 0 && PARTICULAS.contains(p)) {
                sb.append(p);
            } else {
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
            }
        }
        return sb.toString();
    }

    /** Leitor e escritor de JSON mínimos, para o conversor não depender de bibliotecas. */
    static final class Json {
        private final String s;
        private int i;

        Json(String s) {
            this.s = s;
        }

        Object ler() {
            Object v = valor();
            espaco();
            if (i < s.length()) {
                throw falha("conteúdo extra depois do fim do JSON");
            }
            return v;
        }

        private Object valor() {
            espaco();
            if (i >= s.length()) {
                throw falha("o JSON terminou antes do esperado");
            }
            char c = s.charAt(i);
            if (c == '{') {
                return objeto();
            }
            if (c == '[') {
                return array();
            }
            if (c == '"') {
                return string();
            }
            if (s.startsWith("true", i)) {
                i += 4;
                return Boolean.TRUE;
            }
            if (s.startsWith("false", i)) {
                i += 5;
                return Boolean.FALSE;
            }
            if (s.startsWith("null", i)) {
                i += 4;
                return null;
            }
            return numero();
        }

        private Map<String, Object> objeto() {
            Map<String, Object> m = new LinkedHashMap<>();
            i++;
            espaco();
            if (consumir('}')) {
                return m;
            }
            do {
                espaco();
                String chave = string();
                espaco();
                if (!consumir(':')) {
                    throw falha("faltou ':' depois da chave \"" + chave + "\"");
                }
                m.put(chave, valor());
                espaco();
            } while (consumir(','));
            if (!consumir('}')) {
                throw falha("faltou '}' ao fim do objeto");
            }
            return m;
        }

        private List<Object> array() {
            List<Object> l = new ArrayList<>();
            i++;
            espaco();
            if (consumir(']')) {
                return l;
            }
            do {
                l.add(valor());
                espaco();
            } while (consumir(','));
            if (!consumir(']')) {
                throw falha("faltou ']' ao fim da lista");
            }
            return l;
        }

        private String string() {
            if (i >= s.length() || s.charAt(i) != '"') {
                throw falha("era esperado um texto entre aspas");
            }
            i++;
            StringBuilder sb = new StringBuilder();
            while (i < s.length()) {
                char c = s.charAt(i++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    if (i >= s.length()) {
                        break;
                    }
                    char e = s.charAt(i++);
                    switch (e) {
                        case '"', '\\', '/' -> sb.append(e);
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            if (i + 4 > s.length()) {
                                throw falha("sequência \\u incompleta");
                            }
                            sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                            i += 4;
                        }
                        default -> throw falha("sequência de escape inválida: \\" + e);
                    }
                } else {
                    sb.append(c);
                }
            }
            throw falha("texto sem aspas de fechamento");
        }

        private Object numero() {
            int ini = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) {
                i++;
            }
            if (ini == i) {
                throw falha("valor inesperado '" + s.charAt(i) + "'");
            }
            return Double.valueOf(s.substring(ini, i));
        }

        private boolean consumir(char c) {
            if (i < s.length() && s.charAt(i) == c) {
                i++;
                return true;
            }
            return false;
        }

        private void espaco() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
                i++;
            }
        }

        private Erro falha(String motivo) {
            int linha = 1;
            for (int k = 0; k < Math.min(i, s.length()); k++) {
                if (s.charAt(k) == '\n') {
                    linha++;
                }
            }
            return new Erro("JSON inválido na linha " + linha + ": " + motivo + ".");
        }

        @SuppressWarnings("unchecked")
        static Map<String, Object> mapa(Object o, String onde) {
            if (o instanceof Map) {
                return (Map<String, Object>) o;
            }
            throw new Erro("Estrutura inesperada em \"" + onde + "\": era esperado um objeto JSON.");
        }

        @SuppressWarnings("unchecked")
        static List<Object> lista(Object o) {
            return o instanceof List ? (List<Object>) o : List.of();
        }

        static String texto(Object o) {
            if (o == null) {
                return null;
            }
            if (o instanceof Double d && d == Math.rint(d)) {
                return String.valueOf(d.longValue());
            }
            return String.valueOf(o);
        }

        static Integer inteiro(Object o) {
            if (o instanceof Number n) {
                return n.intValue();
            }
            if (o instanceof String t && t.trim().matches("\\d+")) {
                return Integer.parseInt(t.trim());
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        static void escrever(Object o, StringBuilder sb, int nivel) {
            String recuo = "  ".repeat(nivel + 1);
            String recuoFim = "  ".repeat(nivel);
            if (o instanceof Map<?, ?> m) {
                sb.append("{\n");
                int n = 0;
                for (Map.Entry<String, Object> e : ((Map<String, Object>) m).entrySet()) {
                    sb.append(recuo).append(texto(e.getKey(), true)).append(": ");
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
                sb.append(texto(t, true));
            } else if (o instanceof Double d && d == Math.rint(d) && !Double.isInfinite(d)) {
                sb.append(d.longValue()); // 2026.0 vira 2026: a API espera inteiros
            } else {
                sb.append(o);
            }
        }

        private static String texto(String t, boolean aspas) {
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
