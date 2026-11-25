package br.com.minhavidapolitica.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Unidade Federativa (26 estados + DF). Carregada pela migration V3. */
@Entity
@Table(name = "unidade_federativa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnidadeFederativa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 2)
    private String sigla;

    @Column(nullable = false, length = 60)
    private String nome;

    @Column(nullable = false, length = 20)
    private String regiao;
}
