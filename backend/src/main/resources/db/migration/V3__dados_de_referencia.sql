-- V3: dados de referência. Não há dados políticos aqui: eles entram apenas por fontes aprovadas pelo cliente (RN05).

INSERT INTO unidade_federativa (sigla, nome, regiao) VALUES
    ('AC', 'Acre', 'Norte'),
    ('AL', 'Alagoas', 'Nordeste'),
    ('AP', 'Amapá', 'Norte'),
    ('AM', 'Amazonas', 'Norte'),
    ('BA', 'Bahia', 'Nordeste'),
    ('CE', 'Ceará', 'Nordeste'),
    ('DF', 'Distrito Federal', 'Centro-Oeste'),
    ('ES', 'Espírito Santo', 'Sudeste'),
    ('GO', 'Goiás', 'Centro-Oeste'),
    ('MA', 'Maranhão', 'Nordeste'),
    ('MT', 'Mato Grosso', 'Centro-Oeste'),
    ('MS', 'Mato Grosso do Sul', 'Centro-Oeste'),
    ('MG', 'Minas Gerais', 'Sudeste'),
    ('PA', 'Pará', 'Norte'),
    ('PB', 'Paraíba', 'Nordeste'),
    ('PR', 'Paraná', 'Sul'),
    ('PE', 'Pernambuco', 'Nordeste'),
    ('PI', 'Piauí', 'Nordeste'),
    ('RJ', 'Rio de Janeiro', 'Sudeste'),
    ('RN', 'Rio Grande do Norte', 'Nordeste'),
    ('RS', 'Rio Grande do Sul', 'Sul'),
    ('RO', 'Rondônia', 'Norte'),
    ('RR', 'Roraima', 'Norte'),
    ('SC', 'Santa Catarina', 'Sul'),
    ('SP', 'São Paulo', 'Sudeste'),
    ('SE', 'Sergipe', 'Nordeste'),
    ('TO', 'Tocantins', 'Norte');

-- Cargos iniciais. Presidente e Governador vêm do escopo do cliente; Senador e Deputado Federal
-- são candidatos para RF04 e dependem de validação do PO/cliente (Documento de Requisitos, Seção 7).
INSERT INTO cargo (codigo, nome, esfera, ordem_exibicao, ativo) VALUES
    ('PRESIDENTE', 'Presidente da República', 'NACIONAL', 1, TRUE),
    ('GOVERNADOR', 'Governador', 'ESTADUAL', 1, TRUE),
    ('SENADOR', 'Senador', 'ESTADUAL', 2, TRUE),
    ('DEPUTADO_FEDERAL', 'Deputado Federal', 'ESTADUAL', 3, TRUE);
