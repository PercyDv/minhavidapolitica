package br.com.minhavidapolitica.api.repository;

import br.com.minhavidapolitica.api.domain.Candidatura;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * As consultas públicas só devolvem candidaturas de fonte aprovada e ativa (RN05) e ordenam por nome,
 * nunca por votos ou resultado (RN04).
 */
public interface CandidaturaRepository extends JpaRepository<Candidatura, Long> {

    @Query("""
            select c from Candidatura c
              join fetch c.cargo cg
              join fetch c.fonte f
             where c.anoEleicao = :ano and cg.codigo = :codigo and c.uf is null
               and f.aprovada = true and f.ativa = true
             order by c.nome
            """)
    List<Candidatura> listarNacionais(@Param("ano") int ano, @Param("codigo") String codigo);

    @Query("""
            select c from Candidatura c
              join fetch c.cargo cg
              join fetch c.uf u
              join fetch c.fonte f
             where c.anoEleicao = :ano and cg.codigo = :codigo and u.sigla = :sigla
               and f.aprovada = true and f.ativa = true
             order by c.nome
            """)
    List<Candidatura> listarPorUf(@Param("ano") int ano, @Param("codigo") String codigo, @Param("sigla") String sigla);

    /** Listagem do painel: todas as candidaturas do ano, qualquer fonte. */
    @Query("""
            select c from Candidatura c
              join fetch c.cargo cg
              left join fetch c.uf u
              join fetch c.fonte f
             where c.anoEleicao = :ano
             order by cg.codigo, u.sigla, c.nome
            """)
    List<Candidatura> listarAdmin(@Param("ano") int ano);

    Optional<Candidatura> findFirstByAnoEleicaoAndSqCandidato(int anoEleicao, String sqCandidato);

    long countByFonteId(Long fonteId);
}
