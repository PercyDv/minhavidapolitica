package br.com.minhavidapolitica.api.service;

import br.com.minhavidapolitica.api.domain.PerfilUsuario;
import br.com.minhavidapolitica.api.domain.UsuarioPainel;
import br.com.minhavidapolitica.api.dto.UsuarioRequest;
import br.com.minhavidapolitica.api.dto.UsuarioResponse;
import br.com.minhavidapolitica.api.exception.RecursoNaoEncontradoException;
import br.com.minhavidapolitica.api.exception.RegraNegocioException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.minhavidapolitica.api.repository.UsuarioPainelRepository;

/** Gestão dos usuários do painel (módulo ADS). */
@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioService {

    static final int SENHA_MIN = 8;
    static final int SENHA_MAX_BYTES = 72;

    private final UsuarioPainelRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return repository.findAllByOrderByNomeAsc().stream().map(UsuarioResponse::de).toList();
    }

    public UsuarioResponse criar(UsuarioRequest req) {
        String username = req.username().trim();
        if (repository.existsByUsernameIgnoreCase(username)) {
            throw RegraNegocioException.conflito("Já existe um usuário com o login \"" + username + "\".");
        }
        validarSenha(req.senha(), true);

        UsuarioPainel usuario = UsuarioPainel.builder()
                .nome(req.nome().trim())
                .username(username)
                .senhaHash(passwordEncoder.encode(req.senha()))
                .perfil(req.perfil())
                .ativo(req.ativo() == null || req.ativo())
                .build();
        return UsuarioResponse.de(repository.save(usuario));
    }

    /**
     * Atualiza nome, perfil, situação e (opcionalmente) a senha. O login não muda.
     * Protege o painel: não deixa desativar a si mesmo nem remover o último administrador ativo.
     */
    public UsuarioResponse atualizar(Long id, UsuarioRequest req, String usernameLogado) {
        UsuarioPainel usuario = buscar(id);
        boolean novoAtivo = req.ativo() == null || req.ativo();

        boolean eraAdminAtivo = usuario.getPerfil() == PerfilUsuario.ADMIN && usuario.isAtivo();
        boolean continuaAdminAtivo = req.perfil() == PerfilUsuario.ADMIN && novoAtivo;

        if (usuario.getUsername().equalsIgnoreCase(usernameLogado) && !novoAtivo) {
            throw new RegraNegocioException("Você não pode desativar o próprio usuário.");
        }
        if (eraAdminAtivo && !continuaAdminAtivo
                && repository.countByPerfilAndAtivoTrue(PerfilUsuario.ADMIN) <= 1) {
            throw new RegraNegocioException("É preciso manter ao menos um administrador ativo.");
        }
        if (req.senha() != null && !req.senha().isBlank()) {
            validarSenha(req.senha(), false);
            usuario.setSenhaHash(passwordEncoder.encode(req.senha()));
        }
        usuario.setNome(req.nome().trim());
        usuario.setPerfil(req.perfil());
        usuario.setAtivo(novoAtivo);
        return UsuarioResponse.de(repository.save(usuario));
    }

    /** Chamado após login bem-sucedido. */
    public void registrarAcesso(String username) {
        repository.findByUsernameIgnoreCase(username).ifPresent(u -> {
            u.setUltimoAcesso(LocalDateTime.now(clock));
            repository.save(u);
        });
    }

    private UsuarioPainel buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }

    private void validarSenha(String senha, boolean obrigatoria) {
        if (senha == null || senha.isBlank()) {
            if (obrigatoria) {
                throw new RegraNegocioException("Informe uma senha para o novo usuário.");
            }
            return;
        }
        if (senha.length() < SENHA_MIN) {
            throw new RegraNegocioException("A senha deve ter ao menos " + SENHA_MIN + " caracteres.");
        }
        if (senha.getBytes(StandardCharsets.UTF_8).length > SENHA_MAX_BYTES) {
            throw new RegraNegocioException("A senha é longa demais. Use até " + SENHA_MAX_BYTES
                    + " bytes (acentos e emojis ocupam mais de um).");
        }
    }
}
