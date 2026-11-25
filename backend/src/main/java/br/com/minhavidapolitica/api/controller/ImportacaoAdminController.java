package br.com.minhavidapolitica.api.controller;

import br.com.minhavidapolitica.api.dto.ImportacaoRequest;
import br.com.minhavidapolitica.api.dto.LogResponse;
import br.com.minhavidapolitica.api.service.ImportacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Módulo BES, tela "Importação de dados" (RF13). */
@Tag(name = "BES: importação de dados")
@RestController
@RequestMapping("/api/admin/importacoes")
@RequiredArgsConstructor
public class ImportacaoAdminController {

    private final ImportacaoService service;

    @Operation(summary = "Histórico das últimas 50 importações")
    @GetMapping
    public List<LogResponse> historico() {
        return service.historico();
    }

    @Operation(summary = "Importa dados de uma fonte (cria ou atualiza; rejeita registros inválidos)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LogResponse importar(@Valid @RequestBody ImportacaoRequest req, Authentication authentication) {
        return service.importar(req, authentication.getName());
    }
}
