-- V4: candidaturas de eleição (presidente e governador). Extensão fora do escopo validado (RN01): depende de decisão do cliente.

-- Candidaturas de uma eleição (somente presidente e governador). Extensão pedida pela equipe, fora do escopo validado (RN01): depende de decisão do cliente.
CREATE TABLE candidatura (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ano_eleicao INT NOT NULL,
    sq_candidato VARCHAR(30) NOT NULL,
    numero VARCHAR(10) NOT NULL,
    nome VARCHAR(150) NOT NULL,
    nome_urna VARCHAR(150) NULL,
    partido VARCHAR(30) NULL,
    cargo_id BIGINT NOT NULL,
    uf_id BIGINT NULL,
    situacao VARCHAR(20) NOT NULL,
    fonte_id BIGINT NOT NULL,
    link_fonte VARCHAR(500) NULL,
    atualizado_em DATETIME(6) NOT NULL,
    CONSTRAINT pk_candidatura PRIMARY KEY (id),
    CONSTRAINT fk_candidatura_cargo FOREIGN KEY (cargo_id) REFERENCES cargo (id),
    CONSTRAINT fk_candidatura_uf FOREIGN KEY (uf_id) REFERENCES unidade_federativa (id),
    CONSTRAINT fk_candidatura_fonte FOREIGN KEY (fonte_id) REFERENCES fonte_dados (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_candidatura_ano_cargo_uf ON candidatura (ano_eleicao, cargo_id, uf_id);

ALTER TABLE candidatura ADD CONSTRAINT uq_candidatura_ano_sq UNIQUE (ano_eleicao, sq_candidato);
