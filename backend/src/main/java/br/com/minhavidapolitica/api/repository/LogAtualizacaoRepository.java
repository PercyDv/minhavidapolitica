package br.com.minhavidapolitica.api.repository;

import br.com.minhavidapolitica.api.domain.LogAtualizacao;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogAtualizacaoRepository extends JpaRepository<LogAtualizacao, Long> {

    @EntityGraph(attributePaths = "fonte")
    List<LogAtualizacao> findTop50ByOrderByIniciadoEmDesc();

    boolean existsByFonteId(Long fonteId);
}
