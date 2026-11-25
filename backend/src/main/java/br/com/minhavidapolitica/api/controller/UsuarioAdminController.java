package br.com.minhavidapolitica.api.controller;

import br.com.minhavidapolitica.api.dto.UsuarioRequest;
import br.com.minhavidapolitica.api.dto.UsuarioResponse;
import br.com.minhavidapolitica.api.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Módulo ADS, tela "Usuários". Somente perfil ADMIN (regra em SecurityConfig). */
@Tag(name = "ADS: usuários do painel")
@RestController
@RequestMapping("/api/admin/usuarios")
@RequiredArgsConstructor
public class UsuarioAdminController {

    private final UsuarioService service;

    @Operation(summary = "Lista os usuários do painel")
    @GetMapping
    public List<UsuarioResponse> listar() {
        return service.listar();
    }

    @Operation(summary = "Cria um usuário")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse criar(@Valid @RequestBody UsuarioRequest req) {
        return service.criar(req);
    }

    @Operation(summary = "Atualiza nome, perfil, situação e, opcionalmente, a senha")
    @PutMapping("/{id}")
    public UsuarioResponse atualizar(@PathVariable("id") Long id, @Valid @RequestBody UsuarioRequest req,
                                     Authentication authentication) {
        return service.atualizar(id, req, authentication.getName());
    }
}
