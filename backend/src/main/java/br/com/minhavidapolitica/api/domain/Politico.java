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

/** Político em exercício no mandato atual (RN01). Cargo nacional não tem UF. */
@Entity
@Table(name = "politico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Politico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 30)
    private String partido;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    @Column(length = 1000)
    private String resumo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cargo_id", nullable = false)
    private Cargo cargo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uf_id")
    private UnidadeFederativa uf;

    @Column(name = "inicio_mandato")
    private LocalDate inicioMandato;

    @Column(name = "fim_mandato")
    private LocalDate fimMandato;

    @Builder.Default
    @Column(name = "em_exercicio", nullable = false)
    private boolean emExercicio = true;

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
