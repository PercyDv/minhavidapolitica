-- V5: foto e plano de governo das candidaturas.
-- Guarda só o caminho (assets/...) ou a URL; os arquivos ficam no front (frontend/assets).
ALTER TABLE candidatura ADD COLUMN foto_url VARCHAR(500) NULL;
ALTER TABLE candidatura ADD COLUMN plano_url VARCHAR(500) NULL;
