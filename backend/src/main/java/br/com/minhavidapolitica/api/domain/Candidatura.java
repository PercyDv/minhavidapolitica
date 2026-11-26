package br.com.minhavidapolitica.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Candidatura a presidente ou governador numa eleição. Extensão fora do escopo validado (RN01):
 * guarda quem concorreu, separada de {@link Politico}, que representa quem exerce mandato.
 */
@Entity
@Table(name = "candidatura")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candidatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ano_eleicao", nullable = false)
    private int anoEleicao;

    @Column(name = "sq_candidato", nullable = false, length = 30)
    private String sqCandidato;

    @Column(nullable = false, length = 10)
    private String numero;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(name = "nome_urna", length = 150)
    private String nomeUrna;

    @Column(length = 30)
    private String partido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cargo_id", nullable = false)
    private Cargo cargo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uf_id")
    private UnidadeFederativa uf;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private SituacaoCandidatura situacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fonte_id", nullable = false)
    private FonteDados fonte;

    @Column(name = "link_fonte", length = 500)
    private String linkFonte;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist
    void aoCriar() {
        if (atualizadoEm == null) {
            atualizadoEm = LocalDateTime.now();
        }
    }
}
