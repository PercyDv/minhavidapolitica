package br.com.minhavidapolitica.api.repository;

import br.com.minhavidapolitica.api.domain.UnidadeFederativa;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnidadeFederativaRepository extends JpaRepository<UnidadeFederativa, Long> {

    Optional<UnidadeFederativa> findBySiglaIgnoreCase(String sigla);

    List<UnidadeFederativa> findAllByOrderBySiglaAsc();
}
