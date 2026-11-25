package br.com.minhavidapolitica.api.controller;

import br.com.minhavidapolitica.api.dto.AprovacaoRequest;
import br.com.minhavidapolitica.api.dto.FonteRequest;
import br.com.minhavidapolitica.api.dto.FonteResponse;
import br.com.minhavidapolitica.api.service.FonteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Módulo BES, tela "Fontes de dados". Aprovar e revogar: somente ADMIN. */
@Tag(name = "BES: fontes de dados")
@RestController
@RequestMapping("/api/admin/fontes")
@RequiredArgsConstructor
public class FonteAdminController {

    private final FonteService service;

    @Operation(summary = "Lista as fontes e o status de aprovação do cliente")
    @GetMapping
    public List<FonteResponse> listar() {
        return service.listar();
    }

    @Operation(summary = "Cadastra uma fonte (nasce não aprovada)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FonteResponse criar(@Valid @RequestBody FonteRequest req) {
        return service.criar(req);
    }

    @Operation(summary = "Atualiza os dados cadastrais da fonte")
    @PutMapping("/{id}")
    public FonteResponse atualizar(@PathVariable("id") Long id, @Valid @RequestBody FonteRequest req) {
        return service.atualizar(id, req);
    }

    @Operation(summary = "Registra a aprovação formal do cliente (RN05)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/aprovar")
    public FonteResponse aprovar(@PathVariable("id") Long id, @Valid @RequestBody AprovacaoRequest req) {
        return service.aprovar(id, req);
    }

    @Operation(summary = "Revoga a aprovação: os dados da fonte voltam a ficar retidos")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/revogar")
    public FonteResponse revogar(@PathVariable("id") Long id) {
        return service.revogar(id);
    }

    @Operation(summary = "Exclui a fonte (somente se não tiver registros vinculados)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable("id") Long id) {
        service.excluir(id);
    }
}
