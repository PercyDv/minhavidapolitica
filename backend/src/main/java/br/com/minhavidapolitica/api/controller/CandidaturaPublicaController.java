package br.com.minhavidapolitica.api.controller;

import br.com.minhavidapolitica.api.dto.CandidaturaPublica;
import br.com.minhavidapolitica.api.dto.Resultado;
import br.com.minhavidapolitica.api.service.CandidaturaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Candidaturas de uma eleição (presidente e governador). Extensão fora do escopo validado (RN01). */
@Tag(name = "API pública (cidadão)")
@RestController
@RequestMapping("/api/eleicoes")
@RequiredArgsConstructor
public class CandidaturaPublicaController {

    private final CandidaturaService service;

    @Operation(summary = "Candidatos a presidente da eleição, em ordem alfabética")
    @GetMapping("/{ano}/presidente")
    public Resultado<List<CandidaturaPublica>> presidente(@PathVariable("ano") int ano) {
        return service.presidente(ano);
    }

    @Operation(summary = "Candidatos a governador da UF na eleição, em ordem alfabética")
    @GetMapping("/{ano}/ufs/{sigla}/governador")
    public Resultado<List<CandidaturaPublica>> governador(@PathVariable("ano") int ano,
                                                          @PathVariable("sigla") String sigla) {
        return service.governador(ano, sigla);
    }
}
