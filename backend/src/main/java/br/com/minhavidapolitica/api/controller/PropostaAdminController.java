package br.com.minhavidapolitica.api.controller;

import br.com.minhavidapolitica.api.dto.ItemAdminResponse;
import br.com.minhavidapolitica.api.dto.ItemRequest;
import br.com.minhavidapolitica.api.service.PropostaService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Módulo ADS, tela "Propostas e ações": Propostas. */
@Tag(name = "ADS: propostas")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class PropostaAdminController {

    private final PropostaService service;

    @Operation(summary = "Lista de propostas de um político")
    @GetMapping("/politicos/{politicoId}/propostas")
    public List<ItemAdminResponse> listar(@PathVariable("politicoId") Long politicoId) {
        return service.listar(politicoId);
    }

    @Operation(summary = "Cadastra em um político")
    @PostMapping("/politicos/{politicoId}/propostas")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemAdminResponse criar(@PathVariable("politicoId") Long politicoId, @Valid @RequestBody ItemRequest req) {
        return service.criar(politicoId, req);
    }

    @Operation(summary = "Atualiza o registro")
    @PutMapping("/propostas/{id}")
    public ItemAdminResponse atualizar(@PathVariable("id") Long id, @Valid @RequestBody ItemRequest req) {
        return service.atualizar(id, req);
    }

    @Operation(summary = "Exclui o registro")
    @DeleteMapping("/propostas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable("id") Long id) {
        service.excluir(id);
    }
}
