package br.com.minhavidapolitica.api.repository;

import br.com.minhavidapolitica.api.domain.AcaoConcretizada;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AcaoConcretizadaRepository extends JpaRepository<AcaoConcretizada, Long> {

    /** Itens exibíveis ao cidadão: somente de fonte aprovada e ativa (RN05). */
    @Query("""
            select ac from AcaoConcretizada ac
              join fetch ac.fonte f
             where ac.politico.id = :politicoId
               and f.aprovada = true and f.ativa = true
             order by ac.dataAcao desc, ac.id desc
            """)
    List<AcaoConcretizada> listarVisiveis(@Param("politicoId") Long politicoId);

    /** Listagem do painel: todos os itens do político, qualquer fonte. */
    @Query("""
            select ac from AcaoConcretizada ac
              join fetch ac.fonte
             where ac.politico.id = :politicoId
             order by ac.dataAcao desc, ac.id desc
            """)
    List<AcaoConcretizada> listarDoPolitico(@Param("politicoId") Long politicoId);

    Optional<AcaoConcretizada> findFirstByPoliticoIdAndTituloIgnoreCase(Long politicoId, String titulo);

    long countByFonteId(Long fonteId);

    /** Ids de políticos que têm ao menos um item visível (relatório de cobertura). */
    @Query("""
            select ac.politico.id from AcaoConcretizada ac
              join ac.fonte f
             where f.aprovada = true and f.ativa = true
            """)
    List<Long> listarPoliticoIdsComItemVisivel();
}
