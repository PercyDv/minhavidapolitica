package br.com.minhavidapolitica.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Fonte de dados e registro de aprovação do cliente (RN05, RF13).
 * Só dados de fontes {@code aprovada} e {@code ativa} aparecem na API pública.
 */
@Entity
@Table(name = "fonte_dados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FonteDados {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 500)
    private String url;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private TipoFonte tipo;

    @Column(length = 60)
    private String periodicidade;

    @Builder.Default
    @Column(nullable = false)
    private boolean aprovada = false;

    @Column(name = "data_aprovacao")
    private LocalDate dataAprovacao;

    @Column(name = "aprovada_por", length = 120)
    private String aprovadaPor;

    @Column(length = 500)
    private String observacao;

    @Builder.Default
    @Column(nullable = false)
    private boolean ativa = true;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void aoCriar() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }

    /** Regra RN05: a fonte só libera dados para exibição quando aprovada e ativa. */
    public boolean liberaExibicao() {
        return aprovada && ativa;
    }
}
