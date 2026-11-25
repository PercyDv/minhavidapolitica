package br.com.minhavidapolitica.api.controller;

import br.com.minhavidapolitica.api.dto.ItemPublico;
import br.com.minhavidapolitica.api.dto.PoliticoPublico;
import br.com.minhavidapolitica.api.dto.Resultado;
import br.com.minhavidapolitica.api.dto.UfPublica;
import br.com.minhavidapolitica.api.dto.UfResumo;
import br.com.minhavidapolitica.api.service.PublicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API pública consumida pelo front do cidadão (somente leitura, sem login). */
@Tag(name = "API pública (cidadão)")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PublicoController {

    private final PublicoService service;

    @Operation(summary = "RF01: presidente em exercício (página nacional)")
    @GetMapping("/presidente")
    public Resultado<PoliticoPublico> presidente() {
        return service.presidente();
    }

    @Operation(summary = "RF02: lista das 27 UFs para o seletor de estado")
    @GetMapping("/ufs")
    public List<UfResumo> ufs() {
        return service.listarUfs();
    }

    @Operation(summary = "RF03: página estadual com o governador em exercício")
    @GetMapping("/ufs/{sigla}")
    public UfPublica uf(@PathVariable("sigla") String sigla) {
        return service.uf(sigla);
    }

    @Operation(summary = "RF04: demais representantes da UF")
    @GetMapping("/ufs/{sigla}/representantes")
    public Resultado<List<PoliticoPublico>> representantes(@PathVariable("sigla") String sigla) {
        return service.representantes(sigla);
    }

    @Operation(summary = "RF07: perfil resumido do político")
    @GetMapping("/politicos/{id}")
    public PoliticoPublico perfil(@PathVariable("id") Long id) {
        return service.perfil(id);
    }

    @Operation(summary = "RF05: propostas do mandato atual")
    @GetMapping("/politicos/{id}/propostas")
    public Resultado<List<ItemPublico>> propostas(@PathVariable("id") Long id) {
        return service.propostas(id);
    }

    @Operation(summary = "RF06: ações concretizadas no mandato atual")
    @GetMapping("/politicos/{id}/acoes")
    public Resultado<List<ItemPublico>> acoes(@PathVariable("id") Long id) {
        return service.acoes(id);
    }
}
