package br.com.minhavidapolitica.api.controller;

import br.com.minhavidapolitica.api.dto.CoberturaUf;
import br.com.minhavidapolitica.api.dto.FontePendencia;
import br.com.minhavidapolitica.api.dto.ResumoPainel;
import br.com.minhavidapolitica.api.service.RelatorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Módulo ADS, tela "Relatórios", e números da tela inicial. */
@Tag(name = "ADS: relatórios")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class RelatorioAdminController {

    private final RelatorioService service;

    @Operation(summary = "Números da tela inicial do painel")
    @GetMapping("/resumo")
    public ResumoPainel resumo() {
        return service.resumo();
    }

    @Operation(summary = "Cobertura de dados exibíveis por UF")
    @GetMapping("/relatorios/cobertura")
    public List<CoberturaUf> cobertura() {
        return service.cobertura();
    }

    @Operation(summary = "Registros retidos por fonte (fontes não aprovadas)")
    @GetMapping("/relatorios/fontes")
    public List<FontePendencia> fontes() {
        return service.pendenciasPorFonte();
    }

    @Operation(summary = "Cobertura por UF em CSV (abre no Excel)")
    @GetMapping("/relatorios/cobertura.csv")
    public ResponseEntity<byte[]> coberturaCsv() {
        byte[] corpo = service.coberturaCsv().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("cobertura-por-uf.csv").build().toString())
                .body(corpo);
    }
}
