package br.com.minhavidapolitica.api.repository;

import br.com.minhavidapolitica.api.domain.Proposta;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PropostaRepository extends JpaRepository<Proposta, Long> {

    /** Itens exibíveis ao cidadão: somente de fonte aprovada e ativa (RN05). */
    @Query("""
            select pr from Proposta pr
              join fetch pr.fonte f
             where pr.politico.id = :politicoId
               and f.aprovada = true and f.ativa = true
             order by pr.dataReferencia desc, pr.id desc
            """)
    List<Proposta> listarVisiveis(@Param("politicoId") Long politicoId);

    /** Listagem do painel: todos os itens do político, qualquer fonte. */
    @Query("""
            select pr from Proposta pr
              join fetch pr.fonte
             where pr.politico.id = :politicoId
             order by pr.dataReferencia desc, pr.id desc
            """)
    List<Proposta> listarDoPolitico(@Param("politicoId") Long politicoId);

    Optional<Proposta> findFirstByPoliticoIdAndTituloIgnoreCase(Long politicoId, String titulo);

    long countByFonteId(Long fonteId);

    /** Ids de políticos que têm ao menos um item visível (relatório de cobertura). */
    @Query("""
            select pr.politico.id from Proposta pr
              join pr.fonte f
             where f.aprovada = true and f.ativa = true
            """)
    List<Long> listarPoliticoIdsComItemVisivel();
}
