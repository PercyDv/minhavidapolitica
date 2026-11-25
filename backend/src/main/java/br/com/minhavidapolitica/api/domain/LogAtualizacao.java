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
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Registro de uma importação de dados (RF13), consultado no módulo BES. */
@Entity
@Table(name = "log_atualizacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogAtualizacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fonte_id", nullable = false)
    private FonteDados fonte;

    @Column(name = "iniciado_em", nullable = false)
    private LocalDateTime iniciadoEm;

    @Column(name = "finalizado_em")
    private LocalDateTime finalizadoEm;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private StatusImportacao status;

    @Column(name = "registros_criados", nullable = false)
    private int registrosCriados;

    @Column(name = "registros_atualizados", nullable = false)
    private int registrosAtualizados;

    @Column(name = "registros_rejeitados", nullable = false)
    private int registrosRejeitados;

    @Column(length = 1000)
    private String mensagem;

    @Column(name = "executado_por", nullable = false, length = 60)
    private String executadoPor;
}
