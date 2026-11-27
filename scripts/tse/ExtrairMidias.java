import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * Liga as fotos e os planos de governo do TSE às candidaturas do MinhaVidaPolitica.
 *
 * <p>Lê os ZIPs "Fotos de candidatos" e "Proposta de governo" do Portal de Dados Abertos do TSE, acha o
 * arquivo de cada candidato pelo identificador (sqCandidato) que aparece no nome do arquivo, copia só os
 * candidatos do arquivo de candidaturas e escreve um JSON novo com "fotoUrl" e "planoUrl".
 *
 *   java scripts/tse/ExtrairMidias.java --candidaturas candidaturas-tse.json --fotos fotos.zip --planos planos.zip
 *
 * Use --listar para ver como os arquivos de dentro dos ZIPs se chamam. Veja scripts/tse/README.md.
 */
public class ExtrairMidias {

    static final class Erro extends RuntimeException {
        Erro(String mensagem) {
            super(mensagem);
        }
    }

    static final class Config {
        Path candidaturas;
        final List<Path> fotos = new ArrayList<>();
        final List<Path> planos = new ArrayList<>();
        Path saida = Path.of("frontend", "assets");
        Path jsonSaida = Path.of("candidaturas-tse-midia.json");
        Path mandatos;
        Path mandatosSaida = Path.of("importacao-mandato-atual-com-fotos.json");
        int limiteMbPlano = 25;
        int ladoFoto = 400;
        boolean listar;
    }

    /** Um arquivo encontrado dentro de um ZIP para um candidato. */
    record Achado(Path zip, String entrada, long data, long tamanho, int sequencia) {
    }

    private static final Pattern SEQUENCIAL = Pattern.compile("\\d{8,}");
    /** Número do arquivo no fim do nome, como em 2026BA50002536314_01.pdf. Sem número, vale o maior valor. */
    private static final Pattern NUMERO_DO_ARQUIVO = Pattern.compile("_(\\d{1,3})\\.[A-Za-z0-9]+$");

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
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
        if (cfg.listar) {
            for (Path zip : concat(cfg.fotos, cfg.planos)) {
                listar(zip);
            }
            return 0;
        }
        if (cfg.candidaturas == null) {
            throw new Erro("Informe o arquivo de candidaturas com --candidaturas.\n\n" + ajuda());
        }
        if (cfg.fotos.isEmpty() && cfg.planos.isEmpty()) {
            throw new Erro("Informe ao menos um ZIP com --fotos ou --planos.\n\n" + ajuda());
        }

        Map<String, Object> raiz = Json.mapa(new Json(Files.readString(cfg.candidaturas, StandardCharsets.UTF_8).replace("\uFEFF", "")).ler(), "raiz");
        List<Object> lista = Json.lista(raiz.get("candidaturas"));
        if (lista.isEmpty()) {
            throw new Erro("O arquivo não tem a lista \"candidaturas\".");
        }
        Integer ano = Json.inteiro(raiz.get("ano"));
        if (ano == null) {
            throw new Erro("O arquivo de candidaturas não informa o \"ano\".");
        }
        Map<String, Map<String, Object>> porSq = new LinkedHashMap<>();
        for (Object o : lista) {
            Map<String, Object> c = Json.mapa(o, "candidatura");
            porSq.put(String.valueOf(c.get("sqCandidato")), c);
        }

        Path pastaFotos = cfg.saida.resolve("fotos").resolve(String.valueOf(ano));
        Path pastaPlanos = cfg.saida.resolve("planos").resolve(String.valueOf(ano));
        List<String> avisos = new ArrayList<>();

        Map<String, String> fotoUrl = new HashMap<>();
        Map<String, String> planoUrl = new HashMap<>();
        long bytesFotos = 0;
        long bytesPlanos = 0;

        if (!cfg.fotos.isEmpty()) {
            Map<String, Achado> achados = procurar(cfg.fotos, porSq, new String[] {"jpg", "jpeg", "png"}, avisos, "fotos");
            Files.createDirectories(pastaFotos);
            for (Map.Entry<String, Achado> e : achados.entrySet()) {
                byte[] bytes = ler(e.getValue());
                String tipo = tipoImagem(bytes);
                if (tipo == null) {
                    avisos.add("Foto de " + e.getKey() + " ignorada: o arquivo " + e.getValue().entrada() + " não é uma imagem JPEG ou PNG.");
                    continue;
                }
                byte[] reduzida = reduzir(bytes, cfg.ladoFoto, tipo.equals("jpg"));
                byte[] gravar = reduzida != null ? reduzida : bytes;
                String ext = reduzida != null ? "jpg" : tipo;
                Files.write(pastaFotos.resolve(e.getKey() + "." + ext), gravar);
                fotoUrl.put(e.getKey(), "assets/fotos/" + ano + "/" + e.getKey() + "." + ext);
                bytesFotos += gravar.length;
            }
        }
        if (!cfg.planos.isEmpty()) {
            Map<String, Achado> achados = procurar(cfg.planos, porSq, new String[] {"pdf"}, avisos, "planos");
            Files.createDirectories(pastaPlanos);
            for (Map.Entry<String, Achado> e : achados.entrySet()) {
                Achado a = e.getValue();
                if (a.tamanho() > cfg.limiteMbPlano * 1024L * 1024L) {
                    avisos.add("Plano de " + e.getKey() + " ignorado: " + (a.tamanho() / 1024 / 1024) + " MB passa do limite de "
                            + cfg.limiteMbPlano + " MB (--limite-mb-plano).");
                    continue;
                }
                byte[] bytes = ler(a);
                if (bytes.length < 5 || !new String(bytes, 0, 5, StandardCharsets.ISO_8859_1).startsWith("%PDF")) {
                    avisos.add("Plano de " + e.getKey() + " ignorado: o arquivo " + a.entrada() + " não é um PDF.");
                    continue;
                }
                Files.write(pastaPlanos.resolve(e.getKey() + ".pdf"), bytes);
                planoUrl.put(e.getKey(), "assets/planos/" + ano + "/" + e.getKey() + ".pdf");
                bytesPlanos += bytes.length;
            }
        }

        // Atualiza o JSON de candidaturas
        List<Object> novas = new ArrayList<>();
        for (Map<String, Object> c : porSq.values()) {
            Map<String, Object> nova = new LinkedHashMap<>();
            String sq = String.valueOf(c.get("sqCandidato"));
            for (Map.Entry<String, Object> e : c.entrySet()) {
                if (e.getKey().equals("fotoUrl") || e.getKey().equals("planoUrl")) {
                    continue;
                }
                nova.put(e.getKey(), e.getValue());
                if (e.getKey().equals("situacao")) {
                    if (fotoUrl.containsKey(sq)) {
                        nova.put("fotoUrl", fotoUrl.get(sq));
                    }
                    if (planoUrl.containsKey(sq)) {
                        nova.put("planoUrl", planoUrl.get(sq));
                    }
                }
            }
            novas.add(nova);
        }
        raiz.put("candidaturas", novas);
        escreverJson(raiz, cfg.jsonSaida);

        // Resumo por cargo
        Map<String, int[]> total = new TreeMap<>();
        List<String> semFoto = new ArrayList<>();
        List<String> semPlano = new ArrayList<>();
        for (Map<String, Object> c : porSq.values()) {
            String sq = String.valueOf(c.get("sqCandidato"));
            String chave = String.valueOf(c.get("cargo"));
            int[] t = total.computeIfAbsent(chave, k -> new int[3]);
            t[0]++;
            if (fotoUrl.containsKey(sq)) {
                t[1]++;
            } else {
                semFoto.add(rotulo(c));
            }
            if (planoUrl.containsKey(sq)) {
                t[2]++;
            } else {
                semPlano.add(rotulo(c));
            }
        }
        System.out.println("Candidaturas lidas: " + porSq.size() + " (ano " + ano + ")");
        for (Map.Entry<String, int[]> e : total.entrySet()) {
            System.out.println("  " + e.getKey() + ": " + e.getValue()[0] + " candidaturas, "
                    + (cfg.fotos.isEmpty() ? "fotos não processadas" : e.getValue()[1] + " com foto") + ", "
                    + (cfg.planos.isEmpty() ? "planos não processados" : e.getValue()[2] + " com plano"));
        }
        if (!cfg.fotos.isEmpty()) {
            System.out.printf(Locale.ROOT, "Fotos gravadas: %d (%.1f MB) em %s%n", fotoUrl.size(), bytesFotos / 1048576.0, pastaFotos);
        }
        if (!cfg.planos.isEmpty()) {
            System.out.printf(Locale.ROOT, "Planos gravados: %d (%.1f MB) em %s%n", planoUrl.size(), bytesPlanos / 1048576.0, pastaPlanos);
            if (bytesPlanos > 200L * 1024 * 1024) {
                System.out.println("Atenção: os planos somam mais de 200 MB. Pense antes de colocá-los no Git.");
            }
        }
        if (!cfg.fotos.isEmpty() && !semFoto.isEmpty()) {
            imprimirLista("Sem foto", semFoto);
        }
        if (!cfg.planos.isEmpty() && !semPlano.isEmpty()) {
            imprimirLista("Sem plano", semPlano);
        }
        if (!avisos.isEmpty()) {
            System.out.println("Avisos (" + avisos.size() + "):");
            avisos.stream().limit(30).forEach(a -> System.out.println("  - " + a));
        }
        System.out.println("Arquivo de candidaturas atualizado: " + cfg.jsonSaida.toAbsolutePath());

        if (cfg.mandatos != null) {
            vincularMandatos(cfg, porSq, fotoUrl);
        }
        System.out.println();
        System.out.println("Próximo passo: envie o arquivo atualizado para POST /api/admin/importacoes/candidaturas (troque o fonteId se preciso).");
        return 0;
    }

    /** Procura, nos ZIPs, o melhor arquivo de cada candidato (o mais recente, e o maior se houver empate). */
    private static Map<String, Achado> procurar(List<Path> zips, Map<String, Map<String, Object>> porSq,
                                               String[] extensoes, List<String> avisos, String tipo) throws IOException {
        Map<String, Achado> melhores = new LinkedHashMap<>();
        Map<String, List<String>> todos = new HashMap<>();
        int arquivos = 0;
        int semCandidato = 0;
        List<String> exemplos = new ArrayList<>();
        for (Path zip : zips) {
            try (ZipFile zf = new ZipFile(zip.toFile())) {
                var entradas = zf.entries();
                while (entradas.hasMoreElements()) {
                    ZipEntry e = entradas.nextElement();
                    if (e.isDirectory() || !temExtensao(e.getName(), extensoes)) {
                        continue;
                    }
                    arquivos++;
                    String base = e.getName().substring(e.getName().lastIndexOf('/') + 1);
                    String sq = null;
                    Matcher m = SEQUENCIAL.matcher(base);
                    while (m.find()) {
                        if (porSq.containsKey(m.group())) {
                            sq = m.group();
                            break;
                        }
                    }
                    if (sq == null) {
                        semCandidato++;
                        if (exemplos.size() < 6) {
                            exemplos.add(e.getName());
                        }
                        continue;
                    }
                    long data = e.getLastModifiedTime() != null ? e.getLastModifiedTime().toMillis() : 0L;
                    long tamanho = e.getSize() >= 0 ? e.getSize() : 0L;
                    Matcher numero = NUMERO_DO_ARQUIVO.matcher(base);
                    int sequencia = numero.find() ? Integer.parseInt(numero.group(1)) : Integer.MAX_VALUE;
                    Achado novo = new Achado(zip, e.getName(), data, tamanho, sequencia);
                    todos.computeIfAbsent(sq, k -> new ArrayList<>()).add(e.getName());
                    Achado atual = melhores.get(sq);
                    if (atual == null || melhor(novo, atual)) {
                        melhores.put(sq, novo);
                    }
                }
            }
        }
        if (arquivos == 0) {
            avisos.add("Nenhum arquivo ." + String.join("/.", extensoes) + " nos ZIPs de " + tipo + ". Use --listar para ver o conteúdo.");
        } else if (melhores.isEmpty()) {
            avisos.add("Nenhum arquivo de " + tipo + " casou com os candidatos: os nomes dos " + arquivos
                    + " arquivos não trazem o identificador (sqCandidato). Exemplos: " + String.join(", ", exemplos)
                    + ". Me mande esses nomes para eu ajustar a ferramenta.");
        }
        for (Map.Entry<String, List<String>> e : todos.entrySet()) {
            if (e.getValue().size() > 1) {
                avisos.add("Candidato " + e.getKey() + " tem " + e.getValue().size() + " arquivos de " + tipo + " " + e.getValue()
                        + ". Usado: " + melhores.get(e.getKey()).entrada() + ". Confira se é o certo.");
            }
        }
        System.out.println("ZIPs de " + tipo + ": " + arquivos + " arquivos lidos, " + melhores.size() + " ligados a candidatos, "
                + semCandidato + " de outros candidatos (ignorados).");
        return melhores;
    }

    /** Menor número de arquivo vence; depois o mais recente; depois o maior. */
    private static boolean melhor(Achado novo, Achado atual) {
        if (novo.sequencia() != atual.sequencia()) {
            return novo.sequencia() < atual.sequencia();
        }
        if (novo.data() != atual.data()) {
            return novo.data() > atual.data();
        }
        return novo.tamanho() > atual.tamanho();
    }

    private static byte[] ler(Achado a) throws IOException {
        try (ZipFile zf = new ZipFile(a.zip().toFile())) {
            ZipEntry e = zf.getEntry(a.entrada());
            try (InputStream in = zf.getInputStream(e)) {
                return in.readAllBytes();
            }
        }
    }

    private static boolean temExtensao(String nome, String[] extensoes) {
        String n = nome.toLowerCase(Locale.ROOT);
        for (String ext : extensoes) {
            if (n.endsWith("." + ext)) {
                return true;
            }
        }
        return false;
    }

    private static String tipoImagem(byte[] b) {
        if (b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (b.length > 4 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "png";
        }
        return null;
    }

    /** Reduz a imagem para no máximo "lado" pixels e regrava em JPEG; JPEG que já cabe fica como está. Devolve null se não conseguir ler. */
    static byte[] reduzir(byte[] bytes, int lado, boolean jpeg) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null) {
                return null;
            }
            int w = img.getWidth();
            int h = img.getHeight();
            double escala = Math.min(1.0, (double) lado / Math.max(w, h));
            if (escala >= 1.0 && jpeg) {
                return bytes; // já cabe: mantém o original, sem recomprimir
            }
            int nw = Math.max(1, (int) Math.round(w * escala));
            int nh = Math.max(1, (int) Math.round(h * escala));
            BufferedImage rgb = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgb.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, nw, nh);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(img, 0, 0, nw, nh, null);
            g.dispose();
            ImageWriter escritor = ImageIO.getImageWritersByFormatName("jpeg").next();
            ImageWriteParam param = escritor.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.85f);
            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(saida)) {
                escritor.setOutput(ios);
                escritor.write(null, new IIOImage(rgb, null, null), param);
            } finally {
                escritor.dispose();
            }
            return saida.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    /** Dá a foto aos mandatos atuais que são, sem dúvida, a mesma pessoa de uma candidatura (mesmo cargo, UF e nome). */
    private static void vincularMandatos(Config cfg, Map<String, Map<String, Object>> porSq, Map<String, String> fotoUrl) throws IOException {
        Map<String, Object> raiz = Json.mapa(new Json(Files.readString(cfg.mandatos, StandardCharsets.UTF_8).replace("\uFEFF", "")).ler(), "raiz");
        Map<String, List<String>> indice = new HashMap<>();
        for (Map<String, Object> c : porSq.values()) {
            String sq = String.valueOf(c.get("sqCandidato"));
            if (!fotoUrl.containsKey(sq)) {
                continue;
            }
            for (String campo : new String[] {"nome", "nomeUrna"}) {
                if (c.get(campo) != null) {
                    indice.computeIfAbsent(String.valueOf(c.get("cargo")) + "|" + String.valueOf(c.get("uf")) + "|" + norm(String.valueOf(c.get(campo))), k -> new ArrayList<>()).add(sq);
                }
            }
        }
        List<Object> novos = new ArrayList<>();
        List<String> ligados = new ArrayList<>();
        List<String> sem = new ArrayList<>();
        for (Object o : Json.lista(raiz.get("politicos"))) {
            Map<String, Object> p = Json.mapa(o, "politico");
            String chave = p.get("cargo") + "|" + p.get("uf") + "|" + norm(String.valueOf(p.get("nome")));
            List<String> sqs = indice.getOrDefault(chave, List.of()).stream().distinct().toList();
            Map<String, Object> novo = new LinkedHashMap<>();
            boolean ligou = sqs.size() == 1;
            for (Map.Entry<String, Object> e : p.entrySet()) {
                if (e.getKey().equals("fotoUrl")) {
                    continue;
                }
                novo.put(e.getKey(), e.getValue());
                if (ligou && e.getKey().equals("nome")) {
                    novo.put("fotoUrl", fotoUrl.get(sqs.get(0)));
                }
            }
            String nome = String.valueOf(p.get("nome")) + " (" + (p.get("uf") == null ? "Brasil" : p.get("uf")) + ")";
            if (ligou) {
                ligados.add(nome + " -> candidatura " + sqs.get(0));
            } else {
                sem.add(nome + (sqs.size() > 1 ? " [mais de uma candidatura com o mesmo nome: revise à mão]" : ""));
            }
            novos.add(novo);
        }
        raiz.put("politicos", novos);
        escreverJson(raiz, cfg.mandatosSaida);
        System.out.println();
        System.out.println("Mandatos atuais com foto (mesmo cargo, UF e nome de uma candidatura): " + ligados.size() + " de " + novos.size());
        imprimirLista("Revise: confira se cada par é a mesma pessoa", ligados);
        imprimirLista("Sem foto (não foram candidatos com foto, ou o nome difere)", sem);
        System.out.println("Arquivo de mandatos atualizado: " + cfg.mandatosSaida.toAbsolutePath());
    }

    private static void listar(Path zip) throws IOException {
        try (ZipFile zf = new ZipFile(zip.toFile())) {
            Map<String, Integer> porExtensao = new TreeMap<>();
            List<String> exemplos = new ArrayList<>();
            int total = 0;
            var entradas = zf.entries();
            while (entradas.hasMoreElements()) {
                ZipEntry e = entradas.nextElement();
                if (e.isDirectory()) {
                    continue;
                }
                total++;
                String n = e.getName();
                int ponto = n.lastIndexOf('.');
                porExtensao.merge(ponto < 0 ? "(sem extensão)" : n.substring(ponto + 1).toLowerCase(Locale.ROOT), 1, Integer::sum);
                if (exemplos.size() < 12) {
                    exemplos.add(n + " (" + e.getSize() / 1024 + " KB)");
                }
            }
            System.out.println(zip.getFileName() + ": " + total + " arquivos " + porExtensao);
            exemplos.forEach(n -> System.out.println("  " + n));
        }
    }

    private static void imprimirLista(String titulo, List<String> itens) {
        if (itens.isEmpty()) {
            return;
        }
        System.out.println(titulo + " (" + itens.size() + "):");
        itens.stream().limit(40).forEach(i -> System.out.println("  - " + i));
        if (itens.size() > 40) {
            System.out.println("  ... e mais " + (itens.size() - 40));
        }
    }

    private static String rotulo(Map<String, Object> c) {
        Object uf = c.get("uf");
        return c.get("cargo") + (uf == null ? "" : " " + uf) + ": " + (c.get("nomeUrna") != null ? c.get("nomeUrna") : c.get("nome"));
    }

    private static void escreverJson(Map<String, Object> raiz, Path destino) throws IOException {
        StringBuilder sb = new StringBuilder();
        Json.escrever(raiz, sb, 0);
        Files.writeString(destino, sb.append('\n').toString(), StandardCharsets.UTF_8);
    }

    static String norm(String texto) {
        String sem = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sem.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }

    private static List<Path> concat(List<Path> a, List<Path> b) {
        List<Path> l = new ArrayList<>(a);
        l.addAll(b);
        return l;
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
                case "--candidaturas" -> cfg.candidaturas = Path.of(valor(args, ++i, a));
                case "--fotos" -> cfg.fotos.add(Path.of(valor(args, ++i, a)));
                case "--planos" -> cfg.planos.add(Path.of(valor(args, ++i, a)));
                case "--saida" -> cfg.saida = Path.of(valor(args, ++i, a));
                case "--json-saida" -> cfg.jsonSaida = Path.of(valor(args, ++i, a));
                case "--mandatos" -> cfg.mandatos = Path.of(valor(args, ++i, a));
                case "--mandatos-saida" -> cfg.mandatosSaida = Path.of(valor(args, ++i, a));
                case "--limite-mb-plano" -> cfg.limiteMbPlano = inteiro(valor(args, ++i, a), a);
                case "--lado-foto" -> cfg.ladoFoto = inteiro(valor(args, ++i, a), a);
                case "--listar" -> cfg.listar = true;
                default -> throw new Erro("Opção desconhecida: " + a + "\n\n" + ajuda());
            }
        }
        if (cfg.listar && cfg.fotos.isEmpty() && cfg.planos.isEmpty()) {
            throw new Erro("Com --listar, informe os ZIPs em --fotos e/ou --planos.");
        }
        return cfg;
    }

    private static String valor(String[] args, int i, String opcao) {
        if (i >= args.length) {
            throw new Erro("A opção " + opcao + " precisa de um valor.");
        }
        return args[i];
    }

    private static int inteiro(String v, String opcao) {
        try {
            int n = Integer.parseInt(v.trim());
            if (n <= 0) {
                throw new NumberFormatException();
            }
            return n;
        } catch (NumberFormatException e) {
            throw new Erro("Valor inválido para " + opcao + ": " + v);
        }
    }

    private static String ajuda() {
        return """
                Uso: java scripts/tse/ExtrairMidias.java --candidaturas <candidaturas-tse.json> [--fotos <zip>] [--planos <zip>] [opções]

                  --candidaturas <arq>   JSON gerado pelo ConversorCandidatos (precisa do sqCandidato de cada um)
                  --fotos <zip>          ZIP "Fotos de candidatos" do TSE (BR e UFs). Pode repetir a opção.
                  --planos <zip>         ZIP "Proposta de governo" do TSE (BR e UFs). Pode repetir a opção.
                  --saida <pasta>        Pasta base dos arquivos (padrão: frontend/assets)
                  --json-saida <arq>     JSON atualizado com fotoUrl e planoUrl (padrão: candidaturas-tse-midia.json)
                  --mandatos <arq>       importacao-mandato-atual.json: dá foto a quem for a mesma pessoa de uma candidatura
                  --mandatos-saida <arq> Saída dos mandatos (padrão: importacao-mandato-atual-com-fotos.json)
                  --lado-foto <px>       Maior lado da foto reduzida (padrão: 400)
                  --limite-mb-plano <n>  Ignora PDFs maiores que n MB (padrão: 25)
                  --listar               Só mostra o conteúdo dos ZIPs (nomes dos arquivos), sem gravar nada""";
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
