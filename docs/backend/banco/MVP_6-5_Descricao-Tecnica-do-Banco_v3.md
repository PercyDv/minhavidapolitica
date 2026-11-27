# Cartão 6.5: Estruturação do Banco de Dados (descrição técnica)

**Entregável:** diagrama do banco (`MVP_6-5_Diagrama-do-Banco_v3.png`) e esta descrição técnica, para o documento no Overleaf (atividade 6.10).

## Visão geral

| Item | Definição |
|---|---|
| SGBD | MySQL 8.4 LTS, motor InnoDB |
| Codificação | `utf8mb4` com `utf8mb4_0900_ai_ci`, para acentos corretos nos nomes |
| Versionamento do schema | Flyway. Os scripts ficam em `backend/src/main/resources/db/migration` |
| Acesso pela aplicação | Spring Data JPA (Hibernate). O Hibernate não cria nem altera tabelas: o schema nasce só das migrations |
| Tabelas | 9 |

**Migrations:** `V1__baseline.sql` (valida a conexão), `V2__estrutura_base.sql` (as 8 tabelas, chaves e índices) e `V3__dados_de_referencia.sql` (27 UFs e 4 cargos) `V4__candidatura.sql` (tabela de candidaturas) e `V5__candidatura_foto_plano.sql` (colunas de foto e plano de governo das candidaturas). Uma migration já aplicada nunca é editada: mudanças entram em `V6`, `V7` e assim por diante.

## Dicionário de dados

### Tabela `unidade_federativa`

Unidades Federativas (26 estados + DF), usadas no seletor de estado (RF02, RN02).

| Campo | Tipo | Obrigatório | Chave | Padrão | Descrição |
|---|---|---|---|---|---|
| `id` | BIGINT | Sim | PK |  | Identificador (autoincremento). |
| `sigla` | VARCHAR(2) | Sim | Único |  | Sigla da UF (ex.: BA). |
| `nome` | VARCHAR(60) | Sim |  |  | Nome da UF. |
| `regiao` | VARCHAR(20) | Sim |  |  | Região geográfica. |

### Tabela `cargo`

Cargos eletivos exibidos pelo sistema. Tabela configurável (risco R04; RF04 depende da definição do cliente).

| Campo | Tipo | Obrigatório | Chave | Padrão | Descrição |
|---|---|---|---|---|---|
| `id` | BIGINT | Sim | PK |  | Identificador (autoincremento). |
| `codigo` | VARCHAR(30) | Sim | Único |  | Código estável usado nas importações (ex.: GOVERNADOR). |
| `nome` | VARCHAR(60) | Sim |  |  | Nome de exibição do cargo. |
| `esfera` | VARCHAR(10) | Sim |  |  | NACIONAL ou ESTADUAL. Cargo nacional não tem UF; estadual exige UF. |
| `ordem_exibicao` | INT | Sim |  |  | Ordem de exibição dentro da página (sem juízo de mérito, RN04). |
| `ativo` | BOOLEAN | Sim |  | TRUE | Cargo habilitado para uso. |

### Tabela `fonte_dados`

Fontes de dados oficiais/confiáveis e o registro de aprovação do cliente (RN05, RF13).

| Campo | Tipo | Obrigatório | Chave | Padrão | Descrição |
|---|---|---|---|---|---|
| `id` | BIGINT | Sim | PK |  | Identificador (autoincremento). |
| `nome` | VARCHAR(150) | Sim |  |  | Nome da fonte (exibido ao cidadão, RF08). |
| `url` | VARCHAR(500) | Sim |  |  | Endereço da fonte (link para o original, RF08). |
| `tipo` | VARCHAR(20) | Sim |  |  | API, SITE_OFICIAL ou ARQUIVO. |
| `periodicidade` | VARCHAR(60) | Não |  |  | Periodicidade de atualização acordada (RF13). |
| `aprovada` | BOOLEAN | Sim |  | FALSE | Aprovada pelo cliente. Só dados de fontes aprovadas são exibidos (RN05). |
| `data_aprovacao` | DATE | Não |  |  | Data do registro de aprovação. |
| `aprovada_por` | VARCHAR(120) | Não |  |  | Quem registrou a aprovação do cliente. |
| `observacao` | VARCHAR(500) | Não |  |  | Observações sobre a fonte ou a aprovação. |
| `ativa` | BOOLEAN | Sim |  | TRUE | Fonte em uso. Fonte inativa não exibe dados. |
| `criado_em` | DATETIME(6) | Sim |  |  | Data/hora do cadastro. |

### Tabela `politico`

Político em exercício (mandato atual, RN01). Perfil resumido (RF07).

| Campo | Tipo | Obrigatório | Chave | Padrão | Descrição |
|---|---|---|---|---|---|
| `id` | BIGINT | Sim | PK |  | Identificador (autoincremento). |
| `nome` | VARCHAR(150) | Sim |  |  | Nome do político. |
| `partido` | VARCHAR(30) | Não |  |  | Sigla do partido, quando disponível. |
| `foto_url` | VARCHAR(500) | Não |  |  | Endereço da foto, quando disponível. |
| `resumo` | VARCHAR(1000) | Não |  |  | Texto factual curto, sem adjetivação (RF09). |
| `cargo_id` | BIGINT | Sim | FK → cargo |  | Cargo exercido. |
| `uf_id` | BIGINT | Não | FK → unidade_federativa |  | UF do cargo. Nulo para cargos nacionais. |
| `inicio_mandato` | DATE | Não |  |  | Início do mandato atual. |
| `fim_mandato` | DATE | Não |  |  | Fim do mandato atual. Após esta data o político deixa de ser exibido (RN01). |
| `em_exercicio` | BOOLEAN | Sim |  | TRUE | Indica mandato em curso. |
| `fonte_id` | BIGINT | Sim | FK → fonte_dados |  | Fonte que sustenta o registro (RF08). |
| `link_fonte` | VARCHAR(500) | Não |  |  | Link específico do registro na fonte; se vazio, usa a URL da fonte. |
| `atualizado_em` | DATETIME(6) | Sim |  |  | Data/hora da última atualização (RF08). |

### Tabela `proposta`

Propostas do político vinculadas ao mandato atual (RF05).

| Campo | Tipo | Obrigatório | Chave | Padrão | Descrição |
|---|---|---|---|---|---|
| `id` | BIGINT | Sim | PK |  | Identificador (autoincremento). |
| `politico_id` | BIGINT | Sim | FK → politico (cascata) |  | Político autor da proposta. |
| `titulo` | VARCHAR(200) | Sim |  |  | Título da proposta. |
| `descricao` | VARCHAR(2000) | Não |  |  | Descrição factual da proposta. |
| `data_referencia` | DATE | Não |  |  | Data da proposta na fonte. |
| `fonte_id` | BIGINT | Sim | FK → fonte_dados |  | Fonte que sustenta o registro (RF08). |
| `link_fonte` | VARCHAR(500) | Não |  |  | Link específico do registro na fonte. |
| `atualizado_em` | DATETIME(6) | Sim |  |  | Data/hora da última atualização (RF08). |

### Tabela `acao_concretizada`

Ações já realizadas no mandato atual (RF06).

| Campo | Tipo | Obrigatório | Chave | Padrão | Descrição |
|---|---|---|---|---|---|
| `id` | BIGINT | Sim | PK |  | Identificador (autoincremento). |
| `politico_id` | BIGINT | Sim | FK → politico (cascata) |  | Político responsável pela ação. |
| `titulo` | VARCHAR(200) | Sim |  |  | Título da ação. |
| `descricao` | VARCHAR(2000) | Não |  |  | Descrição factual da ação. |
| `data_acao` | DATE | Não |  |  | Data da ação (RF06 exige fonte e data). |
| `fonte_id` | BIGINT | Sim | FK → fonte_dados |  | Fonte que sustenta o registro (RF08). |
| `link_fonte` | VARCHAR(500) | Não |  |  | Link específico do registro na fonte. |
| `atualizado_em` | DATETIME(6) | Sim |  |  | Data/hora da última atualização (RF08). |

### Tabela `log_atualizacao`

Histórico das importações de dados executadas no módulo BES (RF13).

| Campo | Tipo | Obrigatório | Chave | Padrão | Descrição |
|---|---|---|---|---|---|
| `id` | BIGINT | Sim | PK |  | Identificador (autoincremento). |
| `fonte_id` | BIGINT | Sim | FK → fonte_dados |  | Fonte da importação. |
| `iniciado_em` | DATETIME(6) | Sim |  |  | Início da execução. |
| `finalizado_em` | DATETIME(6) | Não |  |  | Fim da execução. |
| `status` | VARCHAR(20) | Sim |  |  | SUCESSO, PARCIAL ou ERRO. |
| `registros_criados` | INT | Sim |  | 0 | Registros novos. |
| `registros_atualizados` | INT | Sim |  | 0 | Registros alterados. |
| `registros_rejeitados` | INT | Sim |  | 0 | Registros recusados na validação. |
| `mensagem` | VARCHAR(1000) | Não |  |  | Resumo e motivos das rejeições. |
| `executado_por` | VARCHAR(60) | Sim |  |  | Usuário do painel que executou. |

### Tabela `candidatura`

Candidaturas de uma eleição (somente presidente e governador). Extensão pedida pela equipe, fora do escopo validado (RN01): depende de decisão do cliente.

| Campo | Tipo | Obrigatório | Chave | Padrão | Descrição |
|---|---|---|---|---|---|
| `id` | BIGINT | Sim | PK |  | Identificador (autoincremento). |
| `ano_eleicao` | INT | Sim |  |  | Ano da eleição (ex.: 2026). |
| `sq_candidato` | VARCHAR(30) | Sim |  |  | Sequencial do candidato no TSE. Com o ano, identifica a candidatura (único), e reimportar atualiza. |
| `numero` | VARCHAR(10) | Sim |  |  | Número do candidato na urna. |
| `nome` | VARCHAR(150) | Sim |  |  | Nome completo do candidato. |
| `nome_urna` | VARCHAR(150) | Não |  |  | Nome na urna. |
| `partido` | VARCHAR(30) | Não |  |  | Sigla do partido. |
| `cargo_id` | BIGINT | Sim | FK → cargo |  | Cargo disputado (PRESIDENTE ou GOVERNADOR). |
| `uf_id` | BIGINT | Não | FK → unidade_federativa |  | UF da disputa. Nulo para presidente. |
| `situacao` | VARCHAR(20) | Sim |  |  | ELEITO, NAO_ELEITO, SEGUNDO_TURNO ou SEM_RESULTADO. Sem votos e sem ranking (RN04). |
| `fonte_id` | BIGINT | Sim | FK → fonte_dados |  | Fonte que sustenta o registro (RF08). |
| `link_fonte` | VARCHAR(500) | Não |  |  | Link do registro na fonte. |
| `atualizado_em` | DATETIME(6) | Sim |  |  | Data/hora da última atualização (RF08). |
| `foto_url` | VARCHAR(500) | Não |  |  | Caminho (assets/...) ou URL da foto oficial do candidato, quando houver. |
| `plano_url` | VARCHAR(500) | Não |  |  | Caminho (assets/...) ou URL do plano de governo registrado, quando houver. |

Restrição adicional: `uq_candidatura_ano_sq` torna único o par (`ano_eleicao`, `sq_candidato`).

### Tabela `usuario_painel`

Usuários da equipe mantenedora com acesso ao painel (módulo ADS).

| Campo | Tipo | Obrigatório | Chave | Padrão | Descrição |
|---|---|---|---|---|---|
| `id` | BIGINT | Sim | PK |  | Identificador (autoincremento). |
| `nome` | VARCHAR(120) | Sim |  |  | Nome do usuário. |
| `username` | VARCHAR(60) | Sim | Único |  | Login (único). |
| `senha_hash` | VARCHAR(100) | Sim |  |  | Senha com hash BCrypt (nunca em texto puro). |
| `perfil` | VARCHAR(20) | Sim |  |  | ADMIN (tudo) ou EDITOR (dados, sem usuários/aprovação de fontes). |
| `ativo` | BOOLEAN | Sim |  | TRUE | Usuário habilitado. |
| `criado_em` | DATETIME(6) | Sim |  |  | Data/hora do cadastro. |
| `ultimo_acesso` | DATETIME(6) | Não |  |  | Último login com sucesso. |

## Relacionamentos

| Campo (filha) | Referencia (pai) | Cardinalidade | Obrigatoriedade | Ao excluir o pai |
|---|---|---|---|---|
| `politico.cargo_id` | `cargo.id` | N:1 | Obrigatório | RESTRICT (padrão) |
| `politico.uf_id` | `unidade_federativa.id` | N:1 | Opcional | RESTRICT (padrão) |
| `politico.fonte_id` | `fonte_dados.id` | N:1 | Obrigatório | RESTRICT (padrão) |
| `proposta.politico_id` | `politico.id` | N:1 | Obrigatório | ON DELETE CASCADE |
| `proposta.fonte_id` | `fonte_dados.id` | N:1 | Obrigatório | RESTRICT (padrão) |
| `acao_concretizada.politico_id` | `politico.id` | N:1 | Obrigatório | ON DELETE CASCADE |
| `acao_concretizada.fonte_id` | `fonte_dados.id` | N:1 | Obrigatório | RESTRICT (padrão) |
| `log_atualizacao.fonte_id` | `fonte_dados.id` | N:1 | Obrigatório | RESTRICT (padrão) |
| `candidatura.cargo_id` | `cargo.id` | N:1 | Obrigatório | RESTRICT (padrão) |
| `candidatura.uf_id` | `unidade_federativa.id` | N:1 | Opcional | RESTRICT (padrão) |
| `candidatura.fonte_id` | `fonte_dados.id` | N:1 | Obrigatório | RESTRICT (padrão) |

## Índices

Além das chaves primárias, únicas e estrangeiras:

| Índice | Tabela | Colunas |
|---|---|---|
| `idx_politico_uf_cargo` | `politico` | uf_id, cargo_id |
| `idx_politico_nome` | `politico` | nome |
| `idx_proposta_politico` | `proposta` | politico_id |
| `idx_acao_politico` | `acao_concretizada` | politico_id |
| `idx_log_iniciado` | `log_atualizacao` | iniciado_em |
| `idx_candidatura_ano_cargo_uf` | `candidatura` | ano_eleicao, cargo_id, uf_id |

Os índices atendem às consultas mais frequentes: políticos por estado e cargo (páginas estaduais), busca por nome (painel), histórico de importações por data e candidaturas por ano, cargo e UF.

## Decisões técnicas

1. **Valores de enumeração em `VARCHAR`.** `esfera`, `tipo`, `status` e `perfil` guardam o nome do valor como texto, o que facilita a leitura direta no banco e evita `ENUM` nativo, que exigiria migration a cada novo valor.
2. **Datas com precisão de microssegundos.** Campos de data e hora usam `DATETIME(6)`, e campos só de data usam `DATE`.
3. **Cascata somente onde o pai é dono.** `proposta` e `acao_concretizada` são excluídas junto com o `politico`. As demais chaves estrangeiras seguem `RESTRICT`: não se exclui uma fonte, um cargo ou uma UF que ainda tenha registros ligados.
4. **Senhas.** `usuario_painel.senha_hash` guarda somente hash BCrypt. Nenhuma senha aparece nas migrations; o administrador inicial é criado na primeira execução a partir do arquivo `.env`.
5. **Candidaturas separadas dos políticos.** `candidatura` guarda quem concorreu a presidente ou governador numa eleição, sem mandato, sem votos e sem ranking (RN04). É uma extensão pedida pela equipe, fora do escopo validado (RN01), e depende de decisão do cliente.
6. **Foto e plano guardados como caminho.** `candidatura.foto_url` e `plano_url` guardam só o caminho (`assets/...`) ou a URL; os arquivos ficam no front. A aplicação recusa qualquer valor que não seja URL http(s) ou caminho em `assets/`, porque o valor vira link e imagem na página pública. Fotos e planos vêm do TSE, com licença Creative Commons Atribuição.
7. **Sem dados políticos na carga inicial.** A V3 contém só dados de referência. Políticos, propostas e ações entram por fontes aprovadas pelo cliente (RN05).

## Regras garantidas pela aplicação (e não pelo banco)

| Regra | Onde é aplicada |
|---|---|
| Cargo nacional sem UF; cargo estadual com UF (RN02) | Serviços de políticos e de importação |
| Só mandato atual (hoje entre início e fim) e só fonte aprovada e ativa aparecem ao cidadão (RN01, RN05) | Consultas dos repositórios (`...Visiveis`) |
| Aprovar fonte exige periodicidade definida (RF13) | `FonteService` |
| Toda ação tem data (RF06) | Cadastro de ações e importação |
| Não remover o último administrador ativo | `UsuarioService` |
| Reeleição na importação: mandato vigente só tem o fim estendido | `ImportacaoService` |
| Candidaturas só de presidente e governador; presidente sem UF e governador com UF | `CandidaturaService` |
| Candidaturas só aparecem com fonte aprovada e ativa, em ordem alfabética | Consultas de `CandidaturaRepository` |

## Observação sobre o diagrama

O diagrama e o dicionário foram gerados a partir da mesma definição que produziu a migration V2, e a correspondência entre as entidades JPA e as colunas foi conferida campo a campo. Para uma versão gerada do banco real, é possível usar a engenharia reversa do MySQL Workbench ou do DBeaver depois que as migrations forem aplicadas.
