package br.com.minhavidapolitica.api.repository;

import br.com.minhavidapolitica.api.domain.Politico;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * As consultas "Visiveis" aplicam duas regras de negócio:
 * RN01 (somente mandato atual: em exercício e com a data de hoje entre o início e o fim do mandato) e
 * RN05 (somente fonte aprovada pelo cliente e ativa).
 */
public interface PoliticoRepository extends JpaRepository<Politico, Long> {

    @Query("""
            select p from Politico p
              join fetch p.cargo c
              join fetch p.fonte f
             where c.codigo = :codigo
               and p.uf is null
               and p.emExercicio = true
               and (p.inicioMandato is null or p.inicioMandato <= :hoje)
               and (p.fimMandato is null or p.fimMandato >= :hoje)
               and f.aprovada = true and f.ativa = true
             order by p.nome
            """)
    List<Politico> listarNacionaisVisiveis(@Param("codigo") String codigo, @Param("hoje") LocalDate hoje);

    @Query("""
            select p from Politico p
              join fetch p.cargo c
              join fetch p.uf u
              join fetch p.fonte f
             where u.sigla = :sigla
               and p.emExercicio = true
               and (p.inicioMandato is null or p.inicioMandato <= :hoje)
               and (p.fimMandato is null or p.fimMandato >= :hoje)
               and f.aprovada = true and f.ativa = true
             order by c.ordemExibicao, p.nome
            """)
    List<Politico> listarVisiveisPorUf(@Param("sigla") String sigla, @Param("hoje") LocalDate hoje);

    @Query("""
            select p from Politico p
              join fetch p.cargo c
              left join fetch p.uf u
              join fetch p.fonte f
             where p.id = :id
               and p.emExercicio = true
               and (p.inicioMandato is null or p.inicioMandato <= :hoje)
               and (p.fimMandato is null or p.fimMandato >= :hoje)
               and f.aprovada = true and f.ativa = true
            """)
    Optional<Politico> buscarVisivelPorId(@Param("id") Long id, @Param("hoje") LocalDate hoje);

    /** Listagem do painel (ADS). ufId e cargoId = 0 significam "todos". */
    @Query("""
            select p from Politico p
              join fetch p.cargo c
              left join fetch p.uf u
              join fetch p.fonte f
             where lower(p.nome) like lower(concat('%', :nome, '%'))
               and (:ufId = 0L or u.id = :ufId)
               and (:cargoId = 0L or c.id = :cargoId)
             order by p.nome
            """)
    List<Politico> buscarAdmin(@Param("nome") String nome, @Param("ufId") long ufId, @Param("cargoId") long cargoId);

    /** Todos os visíveis (relatório de cobertura). */
    @Query("""
            select p from Politico p
              join fetch p.cargo c
              join fetch p.uf u
              join fetch p.fonte f
             where p.emExercicio = true
               and (p.inicioMandato is null or p.inicioMandato <= :hoje)
               and (p.fimMandato is null or p.fimMandato >= :hoje)
               and f.aprovada = true and f.ativa = true
            """)
    List<Politico> listarEstaduaisVisiveis(@Param("hoje") LocalDate hoje);

    Optional<Politico> findFirstByNomeIgnoreCaseAndCargoIdAndUfIsNull(String nome, Long cargoId);

    Optional<Politico> findFirstByNomeIgnoreCaseAndCargoIdAndUfId(String nome, Long cargoId, Long ufId);

    long countByFonteId(Long fonteId);
}
