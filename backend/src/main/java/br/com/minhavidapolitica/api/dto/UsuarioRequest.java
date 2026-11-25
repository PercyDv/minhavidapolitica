package br.com.minhavidapolitica.api.dto;

import br.com.minhavidapolitica.api.domain.PerfilUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Criação/edição de usuário. Na edição, "senha" vazia mantém a senha atual. */
public record UsuarioRequest(
        @NotBlank(message = "O nome é obrigatório") @Size(max = 120, message = "O nome aceita até 120 caracteres") String nome,
        @NotBlank(message = "O login é obrigatório") @Size(min = 3, max = 60, message = "O login deve ter de 3 a 60 caracteres") String username,
        @Size(max = 72, message = "A senha aceita até 72 caracteres") String senha,
        @NotNull(message = "O perfil é obrigatório") PerfilUsuario perfil,
        Boolean ativo) {
}
