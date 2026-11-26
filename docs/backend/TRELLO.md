# Trello: o que o back-end entrega em cada cartão

Guia para a Equipe de Backend manter os cartões **6.2**, **6.4** e **6.5** da Fase 06. Cronograma (6.6), processo Scrum (6.7), participantes (6.8), termo de aceite (6.9) e slides (7.1) são do SM e do PO e não estão aqui.

Legenda das checklists: `[x]` pronto no projeto; `[ ]` falta executar, validar ou anexar. Itens marcados com **(validar)** estão escritos, mas só ficam confirmados depois da primeira execução (`docs/backend/PRIMEIRA-EXECUCAO.md`).

## Cartão 6.2: Modelagem de Classes

Etiqueta: Equipe de Backend. Membros: os dois devs back-end.

**Anexar ao cartão**

| Arquivo | Onde está |
|---|---|
| `MVP_6-2_Diagrama-de-Classes_v2.png` | `docs/backend/diagramas/` |
| `MVP_6-2_Diagrama-de-Classes_v2.puml` (fonte) | `docs/backend/diagramas/` |
| `MVP_6-2_Texto-de-Apoio_v2.md` | `docs/backend/diagramas/` |

**Checklist**
- [x] Levantar entidades a partir dos RF01–RF13 e das regras RN01–RN07
- [x] Definir atributos, tipos e relacionamentos com multiplicidade
- [x] Desenhar o Diagrama de Classes (PlantUML)
- [ ] Revisar com o outro dev back-end e com o André
- [x] Exportar a imagem em alta resolução para `docs/backend/`
- [x] Escrever o texto curto de apoio para o documento
- [x] Conferir consistência com o modelo do banco (6.5): as 8 entidades batem coluna a coluna com a migration V2

Ao mudar o status para "Em Revisão", anexe a PR do GitHub ao cartão.

## Cartão 6.4: Desenvolvimento do Back-End (módulos ADS e BES)

Etiqueta: Equipe de Backend. Membros: os dois devs back-end; André em testes e análise de código.

**Anexar ao cartão**

| Arquivo | Onde está |
|---|---|
| `MVP_6-4_Contrato-da-API_v2.yaml` e `.json` | `docs/backend/api/` |
| `MVP_6-4_Matriz-RF-Endpoint_v2.md` | `docs/backend/api/` |
| `MVP_6-4_Fichas-das-Telas_v1.md` | `docs/backend/telas/` |
| Prints das 6 telas (`ADS-1_...` a `BES-2_...`) | `docs/backend/telas/prints/` (a tirar) |

**Checklist A: Fundação técnica**
- [x] Projeto Spring Boot criado em `backend/` (Java 17, Maven, Lombok)
- [ ] MySQL + Docker Compose + `.env` no ar: arquivos prontos, falta subir e conectar **(validar)**
- [ ] `application.yml` e baseline do Flyway: arquivos prontos, falta aplicar V1, V2 e V3 **(validar)**
- [x] Estrutura de pacotes e tratamento padronizado de erros
- [x] Segurança do painel (login por sessão, perfis ADMIN e EDITOR; login não conta na pontuação)

**Checklist B: API pública** (alimenta o front do 6.3)
- [x] Presidente em exercício (RF01) **(validar)**
- [x] Lista das 27 UFs e página estadual com governador (RF02, RF03) **(validar)**
- [x] Demais representantes e perfil resumido (RF04, RF07) **(validar)**
- [x] Propostas e ações concretizadas (RF05, RF06) **(validar)**
- [x] Fonte e data de atualização em toda resposta (RF08) **(validar)**
- [x] Mensagem neutra quando não há dado validado (RF12) **(validar)**

**Checklist C: Módulo ADS** (mínimo de 4 telas funcionais, sem contar login)
- [x] Tela 1: Usuários (somente ADMIN)
- [x] Tela 2: Políticos
- [x] Tela 3: Propostas e ações
- [x] Tela 4: Relatórios (cobertura por UF, registros por fonte, CSV)

**Checklist D: Módulo BES** (mínimo de 2 telas funcionais)
- [x] Tela 1: Fontes de dados (aprovação do cliente, RN05)
- [x] Tela 2: Importação de dados (RF13, importação manual por JSON)
- [x] Conversor do arquivo de eleitos do TSE para o formato de importação (`scripts/tse`, testado com arquivos sintéticos)
- [x] Consulta pública considera o início do mandato (eleitos de 2026 só aparecem na posse, em 2027)
- [x] **Extensão pedida pela equipe, fora do escopo validado:** candidaturas de presidente e governador (`/api/eleicoes/{ano}/...`, importação em `/api/admin/importacoes/candidaturas`, conversor `scripts/tse/ConversorCandidatos.java`). **Registrar no cartão a decisão do cliente (via PO)**; sem ela, não incluir nos slides como requisito atendido.

**Checklist E: Fechamento**
- [ ] Rodar os testes (`./mvnw test`): 4 classes de teste escritas **(validar)**
- [ ] Prints das telas de cada módulo com breve descrição de função e fluxo (as fichas já estão escritas)
- [ ] Sistema rodando de ponta a ponta para a demonstração (roteiro no `README.md`)

Observação: as telas do painel foram escritas, mas só passaram por um teste parcial no navegador, contra uma API simulada. A confirmação acontece ao rodar o sistema de verdade.

## Cartão 6.5: Estruturação do Banco de Dados (obrigatório: grupo ADS)

Etiqueta: Equipe de Backend. Membros: os dois devs back-end.

**Anexar ao cartão**

| Arquivo | Onde está |
|---|---|
| `MVP_6-5_Diagrama-do-Banco_v2.png` | `docs/backend/banco/` |
| `MVP_6-5_Diagrama-do-Banco_v2.puml` (fonte) | `docs/backend/banco/` |
| `MVP_6-5_Descricao-Tecnica-do-Banco_v2.md` (dicionário de dados incluído) | `docs/backend/banco/` |
| Migrations V1 a V4 (evidência) | `backend/src/main/resources/db/migration/` |

**Checklist**
- [x] Diagrama do banco (tabelas, chaves e relacionamentos)
- [x] Dicionário de dados: tabelas e principais campos
- [x] Descrição técnica, com os scripts do Flyway como evidência
- [x] Exportar a imagem
- [ ] Anexar ao cartão e entregar ao SM para o documento no Overleaf
- [ ] Opcional: conferir o diagrama contra o banco real (engenharia reversa no Workbench ou DBeaver)

## Entregas ao SM para o Overleaf (6.10)

Sugestão de meta interna: diagramas e descrição do banco (6.2 e 6.5) na **sexta, 27/11**; prints e fichas das telas (6.4) no **sábado, 28/11**, para sobrar tempo de consolidar e imprimir antes de domingo. Se o SM preferir, o texto dos `.md` pode ser convertido para `.tex` no padrão do documento da I Unidade.

## Git: branches e commits sugeridos

Branches com o número da atividade, uma PR por branch, com 1 aprovação:

| Branch | Conteúdo | Mensagem de commit sugerida |
|---|---|---|
| `feature/6.4-fundacao` | `pom.xml`, `application.yml`, Docker Compose, `.env.example`, `.gitignore`, template de PR | `chore: cria fundação do back-end e ambiente local` |
| `feature/6.2-modelo-de-dominio` | Entidades, enums, repositórios e diagrama de classes | `feat: cria modelo de domínio e diagrama de classes` |
| `feature/6.5-banco` | Migrations V1, V2 e V3, diagrama do banco e descrição técnica | `feat: cria estrutura do banco com Flyway` |
| `feature/6.4-api-publica` | Serviço e controller públicos, DTOs e tratamento de erros | `feat: cria API pública de consulta (RF01 a RF08, RF12)` |
| `feature/6.4-modulo-ads` | Segurança, usuários, políticos, propostas, ações e relatórios | `feat: cria módulo ADS` |
| `feature/6.4-modulo-bes` | Fontes, aprovação e importação de dados | `feat: cria módulo BES` |
| `feature/6.4-candidaturas` | Candidaturas de presidente e governador, migration V4, conversor, diagramas e contrato v2 | `feat: adiciona candidaturas de eleição (extensão de escopo)` |
| `docs/6.4-contrato-e-fichas` | Contrato da API, matriz RF × endpoint e fichas das telas | `docs: documenta API e telas dos módulos` |

## Dependências de outras pessoas (não são do back-end)

Pedir ao PO ou ao SM: fontes de dados aprovadas pelo cliente (sem aprovação, o cidadão só vê a mensagem neutra); cargos dos "demais representantes" (RF04); definição de "ação concretizada" e do critério mínimo de comprovação (RF06); periodicidade de atualização de cada fonte; licença do projeto (RN07); e a resposta do professor sobre a regra de HTML, CSS e JavaScript exclusivos e sobre o módulo BES para grupo ADS.
