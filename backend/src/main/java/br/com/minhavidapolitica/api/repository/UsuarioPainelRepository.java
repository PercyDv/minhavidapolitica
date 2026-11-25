package br.com.minhavidapolitica.api.repository;

import br.com.minhavidapolitica.api.domain.PerfilUsuario;
import br.com.minhavidapolitica.api.domain.UsuarioPainel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioPainelRepository extends JpaRepository<UsuarioPainel, Long> {

    Optional<UsuarioPainel> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    List<UsuarioPainel> findAllByOrderByNomeAsc();

    long countByPerfilAndAtivoTrue(PerfilUsuario perfil);
}
