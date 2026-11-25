package br.com.minhavidapolitica.api.dto;

import br.com.minhavidapolitica.api.domain.UsuarioPainel;
import java.time.LocalDateTime;

/** Nunca expõe o hash da senha. */
public record UsuarioResponse(Long id, String nome, String username, String perfil, boolean ativo,
                              LocalDateTime criadoEm, LocalDateTime ultimoAcesso) {

    public static UsuarioResponse de(UsuarioPainel u) {
        return new UsuarioResponse(u.getId(), u.getNome(), u.getUsername(), u.getPerfil().name(),
                u.isAtivo(), u.getCriadoEm(), u.getUltimoAcesso());
    }
}
