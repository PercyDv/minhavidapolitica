-- V2: estrutura base do MinhaVidaPolitica (gerada a partir de docs/backend/banco). Nunca edite após aplicada;
-- novas alterações entram em V4, V5... (regra do Flyway).

-- Unidades Federativas (26 estados + DF), usadas no seletor de estado (RF02, RN02).
CREATE TABLE unidade_federativa (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sigla VARCHAR(2) NOT NULL,
    nome VARCHAR(60) NOT NULL,
    regiao VARCHAR(20) NOT NULL,
    CONSTRAINT pk_unidade_federativa PRIMARY KEY (id),
    CONSTRAINT uq_unidade_federativa_sigla UNIQUE (sigla)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Cargos eletivos exibidos pelo sistema. Tabela configurável (risco R04; RF04 depende da definição do cliente).
CREATE TABLE cargo (
    id BIGINT NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(30) NOT NULL,
    nome VARCHAR(60) NOT NULL,
    esfera VARCHAR(10) NOT NULL,
    ordem_exibicao INT NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_cargo PRIMARY KEY (id),
    CONSTRAINT uq_cargo_codigo UNIQUE (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Fontes de dados oficiais/confiáveis e o registro de aprovação do cliente (RN05, RF13).
CREATE TABLE fonte_dados (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(150) NOT NULL,
    url VARCHAR(500) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    periodicidade VARCHAR(60) NULL,
    aprovada BOOLEAN NOT NULL DEFAULT FALSE,
    data_aprovacao DATE NULL,
    aprovada_por VARCHAR(120) NULL,
    observacao VARCHAR(500) NULL,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em DATETIME(6) NOT NULL,
    CONSTRAINT pk_fonte_dados PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Político em exercício (mandato atual, RN01). Perfil resumido (RF07).
CREATE TABLE politico (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(150) NOT NULL,
    partido VARCHAR(30) NULL,
    foto_url VARCHAR(500) NULL,
    resumo VARCHAR(1000) NULL,
    cargo_id BIGINT NOT NULL,
    uf_id BIGINT NULL,
    inicio_mandato DATE NULL,
    fim_mandato DATE NULL,
    em_exercicio BOOLEAN NOT NULL DEFAULT TRUE,
    fonte_id BIGINT NOT NULL,
    link_fonte VARCHAR(500) NULL,
    atualizado_em DATETIME(6) NOT NULL,
    CONSTRAINT pk_politico PRIMARY KEY (id),
    CONSTRAINT fk_politico_cargo FOREIGN KEY (cargo_id) REFERENCES cargo (id),
    CONSTRAINT fk_politico_uf FOREIGN KEY (uf_id) REFERENCES unidade_federativa (id),
    CONSTRAINT fk_politico_fonte FOREIGN KEY (fonte_id) REFERENCES fonte_dados (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Propostas do político vinculadas ao mandato atual (RF05).
CREATE TABLE proposta (
    id BIGINT NOT NULL AUTO_INCREMENT,
    politico_id BIGINT NOT NULL,
    titulo VARCHAR(200) NOT NULL,
    descricao VARCHAR(2000) NULL,
    data_referencia DATE NULL,
    fonte_id BIGINT NOT NULL,
    link_fonte VARCHAR(500) NULL,
    atualizado_em DATETIME(6) NOT NULL,
    CONSTRAINT pk_proposta PRIMARY KEY (id),
    CONSTRAINT fk_proposta_politico FOREIGN KEY (politico_id) REFERENCES politico (id) ON DELETE CASCADE,
    CONSTRAINT fk_proposta_fonte FOREIGN KEY (fonte_id) REFERENCES fonte_dados (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Ações já realizadas no mandato atual (RF06).
CREATE TABLE acao_concretizada (
    id BIGINT NOT NULL AUTO_INCREMENT,
    politico_id BIGINT NOT NULL,
    titulo VARCHAR(200) NOT NULL,
    descricao VARCHAR(2000) NULL,
    data_acao DATE NULL,
    fonte_id BIGINT NOT NULL,
    link_fonte VARCHAR(500) NULL,
    atualizado_em DATETIME(6) NOT NULL,
    CONSTRAINT pk_acao_concretizada PRIMARY KEY (id),
    CONSTRAINT fk_acao_concretizada_politico FOREIGN KEY (politico_id) REFERENCES politico (id) ON DELETE CASCADE,
    CONSTRAINT fk_acao_concretizada_fonte FOREIGN KEY (fonte_id) REFERENCES fonte_dados (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Histórico das importações de dados executadas no módulo BES (RF13).
CREATE TABLE log_atualizacao (
    id BIGINT NOT NULL AUTO_INCREMENT,
    fonte_id BIGINT NOT NULL,
    iniciado_em DATETIME(6) NOT NULL,
    finalizado_em DATETIME(6) NULL,
    status VARCHAR(20) NOT NULL,
    registros_criados INT NOT NULL DEFAULT 0,
    registros_atualizados INT NOT NULL DEFAULT 0,
    registros_rejeitados INT NOT NULL DEFAULT 0,
    mensagem VARCHAR(1000) NULL,
    executado_por VARCHAR(60) NOT NULL,
    CONSTRAINT pk_log_atualizacao PRIMARY KEY (id),
    CONSTRAINT fk_log_atualizacao_fonte FOREIGN KEY (fonte_id) REFERENCES fonte_dados (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Usuários da equipe mantenedora com acesso ao painel (módulo ADS).
CREATE TABLE usuario_painel (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(120) NOT NULL,
    username VARCHAR(60) NOT NULL,
    senha_hash VARCHAR(100) NOT NULL,
    perfil VARCHAR(20) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em DATETIME(6) NOT NULL,
    ultimo_acesso DATETIME(6) NULL,
    CONSTRAINT pk_usuario_painel PRIMARY KEY (id),
    CONSTRAINT uq_usuario_painel_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_politico_uf_cargo ON politico (uf_id, cargo_id);
CREATE INDEX idx_politico_nome ON politico (nome);
CREATE INDEX idx_proposta_politico ON proposta (politico_id);
CREATE INDEX idx_acao_politico ON acao_concretizada (politico_id);
CREATE INDEX idx_log_iniciado ON log_atualizacao (iniciado_em);
