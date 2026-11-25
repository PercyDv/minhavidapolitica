package br.com.minhavidapolitica.api.config;

import br.com.minhavidapolitica.api.domain.UsuarioPainel;
import br.com.minhavidapolitica.api.repository.UsuarioPainelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Carrega o usuário do painel no login. Perfil ADMIN/EDITOR vira a autoridade ROLE_ADMIN/ROLE_EDITOR. */
@Service
@RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioPainelRepository repository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UsuarioPainel usuario = repository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        return User.withUsername(usuario.getUsername())
                .password(usuario.getSenhaHash())
                .roles(usuario.getPerfil().name())
                .disabled(!usuario.isAtivo())
                .build();
    }
}
