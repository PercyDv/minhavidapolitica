package br.com.minhavidapolitica.api.controller;

import br.com.minhavidapolitica.api.dto.PoliticoAdminResponse;
import br.com.minhavidapolitica.api.dto.PoliticoRequest;
import br.com.minhavidapolitica.api.service.PoliticoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Módulo ADS, tela "Políticos". */
@Tag(name = "ADS: políticos")
@RestController
@RequestMapping("/api/admin/politicos")
@RequiredArgsConstructor
public class PoliticoAdminController {

    private final PoliticoService service;

    @Operation(summary = "Lista políticos com filtros por nome, UF e cargo")
    @GetMapping
    public List<PoliticoAdminResponse> listar(
            @RequestParam(name = "nome", required = false) String nome,
            @RequestParam(name = "ufId", required = false) Long ufId,
            @RequestParam(name = "cargoId", required = false) Long cargoId) {
        return service.listar(nome, ufId, cargoId);
    }

    @Operation(summary = "Busca um político")
    @GetMapping("/{id}")
    public PoliticoAdminResponse buscar(@PathVariable("id") Long id) {
        return service.buscar(id);
    }

    @Operation(summary = "Cadastra um político")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PoliticoAdminResponse criar(@Valid @RequestBody PoliticoRequest req) {
        return service.criar(req);
    }

    @Operation(summary = "Atualiza um político")
    @PutMapping("/{id}")
    public PoliticoAdminResponse atualizar(@PathVariable("id") Long id, @Valid @RequestBody PoliticoRequest req) {
        return service.atualizar(id, req);
    }

    @Operation(summary = "Exclui um político e suas propostas e ações")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable("id") Long id) {
        service.excluir(id);
    }
}
