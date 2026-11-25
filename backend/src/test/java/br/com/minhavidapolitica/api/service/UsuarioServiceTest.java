package br.com.minhavidapolitica.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.minhavidapolitica.api.domain.PerfilUsuario;
import br.com.minhavidapolitica.api.domain.UsuarioPainel;
import br.com.minhavidapolitica.api.dto.UsuarioRequest;
import br.com.minhavidapolitica.api.dto.UsuarioResponse;
import br.com.minhavidapolitica.api.exception.RegraNegocioException;
import br.com.minhavidapolitica.api.repository.UsuarioPainelRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioPainelRepository repository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UsuarioService service;

    @BeforeEach
    void preparar() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-25T12:00:00Z"), ZoneId.of("America/Bahia"));
        service = new UsuarioService(repository, passwordEncoder, clock);
    }

    @Test
    void criarGravaSomenteOHashDaSenha() {
        when(repository.existsByUsernameIgnoreCase("ana")).thenReturn(false);
        when(passwordEncoder.encode("senha-segura-1")).thenReturn("HASH");
        when(repository.save(any(UsuarioPainel.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse resposta = service.criar(
                new UsuarioRequest("Ana Souza", "ana", "senha-segura-1", PerfilUsuario.EDITOR, true));

        assertThat(resposta.username()).isEqualTo("ana");
        assertThat(resposta.perfil()).isEqualTo("EDITOR");
        verify(passwordEncoder).encode("senha-segura-1");
    }

    @Test
    void senhaCurtaEhRejeitada() {
        when(repository.existsByUsernameIgnoreCase("ana")).thenReturn(false);

        UsuarioRequest req = new UsuarioRequest("Ana Souza", "ana", "curta", PerfilUsuario.EDITOR, true);

        assertThrows(RegraNegocioException.class, () -> service.criar(req));
    }

    @Test
    void loginDuplicadoEhConflito() {
        when(repository.existsByUsernameIgnoreCase("ana")).thenReturn(true);

        UsuarioRequest req = new UsuarioRequest("Ana Souza", "ana", "senha-segura-1", PerfilUsuario.EDITOR, true);
        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> service.criar(req));

        assertThat(ex.getStatus().value()).isEqualTo(409);
    }

    @Test
    void naoPermiteRebaixarOUltimoAdministradorAtivo() {
        UsuarioPainel admin = usuario(1L, "admin", PerfilUsuario.ADMIN);
        when(repository.findById(1L)).thenReturn(Optional.of(admin));
        when(repository.countByPerfilAndAtivoTrue(PerfilUsuario.ADMIN)).thenReturn(1L);

        UsuarioRequest req = new UsuarioRequest("Admin", "admin", null, PerfilUsuario.EDITOR, true);
        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> service.atualizar(1L, req, "outro.usuario"));

        assertThat(ex.getMessage()).contains("administrador");
    }

    @Test
    void naoPermiteDesativarOProprioUsuario() {
        UsuarioPainel editor = usuario(2L, "bia", PerfilUsuario.EDITOR);
        when(repository.findById(2L)).thenReturn(Optional.of(editor));

        UsuarioRequest req = new UsuarioRequest("Bia", "bia", null, PerfilUsuario.EDITOR, false);

        assertThrows(RegraNegocioException.class, () -> service.atualizar(2L, req, "bia"));
    }

    private static UsuarioPainel usuario(Long id, String username, PerfilUsuario perfil) {
        return UsuarioPainel.builder().id(id).nome(username).username(username).senhaHash("HASH")
                .perfil(perfil).ativo(true).build();
    }
}
