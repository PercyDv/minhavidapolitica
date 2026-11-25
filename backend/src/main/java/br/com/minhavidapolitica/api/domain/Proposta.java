package br.com.minhavidapolitica.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Proposta do político no mandato atual (RF05). */
@Entity
@Table(name = "proposta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proposta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "politico_id", nullable = false)
    private Politico politico;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 2000)
    private String descricao;

    @Column(name = "data_referencia")
    private LocalDate dataReferencia;

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
