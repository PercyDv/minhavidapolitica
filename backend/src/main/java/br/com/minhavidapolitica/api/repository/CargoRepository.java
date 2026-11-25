package br.com.minhavidapolitica.api.repository;

import br.com.minhavidapolitica.api.domain.Cargo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CargoRepository extends JpaRepository<Cargo, Long> {

    Optional<Cargo> findByCodigoIgnoreCase(String codigo);

    List<Cargo> findAllByAtivoTrueOrderByIdAsc();
}
