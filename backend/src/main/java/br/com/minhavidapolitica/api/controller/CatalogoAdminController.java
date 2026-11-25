package br.com.minhavidapolitica.api.controller;

import br.com.minhavidapolitica.api.dto.CatalogoResponse;
import br.com.minhavidapolitica.api.service.CatalogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Painel: apoio")
@RestController
@RequestMapping("/api/admin/catalogos")
@RequiredArgsConstructor
public class CatalogoAdminController {

    private final CatalogoService service;

    @Operation(summary = "UFs, cargos e fontes para preencher os formulários")
    @GetMapping
    public CatalogoResponse catalogos() {
        return service.catalogos();
    }
}
