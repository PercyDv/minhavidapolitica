package br.com.minhavidapolitica.api.repository;

import br.com.minhavidapolitica.api.domain.FonteDados;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FonteDadosRepository extends JpaRepository<FonteDados, Long> {

    List<FonteDados> findAllByOrderByNomeAsc();

    long countByAprovadaTrueAndAtivaTrue();

    long countByAprovadaFalseOrAtivaFalse();
}
