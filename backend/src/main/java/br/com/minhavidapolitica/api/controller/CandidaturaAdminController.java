package br.com.minhavidapolitica.api.controller;

import br.com.minhavidapolitica.api.dto.CandidaturaAdminResponse;
import br.com.minhavidapolitica.api.dto.ImportacaoCandidaturasRequest;
import br.com.minhavidapolitica.api.dto.LogResponse;
import br.com.minhavidapolitica.api.service.CandidaturaService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Módulo BES: importação e conferência de candidaturas (somente por API, sem tela). */
@Tag(name = "BES: candidaturas")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class CandidaturaAdminController {

    private final CandidaturaService service;

    @Operation(summary = "Lista as candidaturas importadas de uma eleição (qualquer fonte)")
    @GetMapping("/candidaturas")
    public List<CandidaturaAdminResponse> listar(@RequestParam("ano") int ano) {
        return service.listarAdmin(ano);
    }

    @Operation(summary = "Importa candidaturas de uma eleição (cria ou atualiza; rejeita registros inválidos)")
    @PostMapping("/importacoes/candidaturas")
    @ResponseStatus(HttpStatus.CREATED)
    public LogResponse importar(@Valid @RequestBody ImportacaoCandidaturasRequest req, Authentication authentication) {
        return service.importar(req, authentication.getName());
    }
}
