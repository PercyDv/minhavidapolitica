package br.com.minhavidapolitica.api.controller;

import br.com.minhavidapolitica.api.domain.UsuarioPainel;
import br.com.minhavidapolitica.api.dto.SessaoResponse;
import br.com.minhavidapolitica.api.exception.RecursoNaoEncontradoException;
import br.com.minhavidapolitica.api.repository.UsuarioPainelRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Login e logout são tratados pelo Spring Security (POST /api/auth/login e /api/auth/logout). */
@Tag(name = "Sessão do painel")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioPainelRepository repository;

    @Operation(summary = "Dados do usuário logado (usado pelo painel para montar o menu)")
    @GetMapping("/me")
    public SessaoResponse me(Authentication authentication) {
        UsuarioPainel usuario = repository.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
        return new SessaoResponse(usuario.getUsername(), usuario.getNome(), usuario.getPerfil().name());
    }
}
