# Cartão 6.4: Matriz de requisitos × endpoints

Mostra quais requisitos do Documento de Requisitos o back-end cobre e por qual endpoint. O contrato completo está em `MVP_6-4_Contrato-da-API_v2.yaml` (e `.json`). Com a aplicação rodando, o Swagger UI fica em `http://localhost:8080/swagger-ui.html`.

**Situação:** "Implementado" significa que o código existe no projeto. A confirmação por execução acontece na primeira vez que o time rodar `./mvnw test` e `./mvnw spring-boot:run`.

## Requisitos funcionais

| RF | Prioridade | Endpoint ou mecanismo | Situação |
|---|---|---|---|
| RF01 Página nacional | Deve | `GET /api/presidente` | Implementado |
| RF02 Selecionar estado | Deve | `GET /api/ufs` (as 27 UFs) | Implementado |
| RF03 Página estadual com governador | Deve | `GET /api/ufs/{sigla}` | Implementado |
| RF04 Demais representantes | Deveria | `GET /api/ufs/{sigla}/representantes` | Implementado (cargos a validar com o cliente) |
| RF05 Propostas do político | Deve | `GET /api/politicos/{id}/propostas` | Implementado |
| RF06 Ações concretizadas | Deve | `GET /api/politicos/{id}/acoes` | Implementado |
| RF07 Perfil resumido | Deveria | `GET /api/politicos/{id}` | Implementado |
| RF08 Fonte e data de atualização | Deve | Campo `fonte` (nome, link, atualizadoEm) em todo bloco das respostas públicas | Implementado |
| RF09 Informação neutra | Deve | Por modelagem: nenhum campo de nota, ranking ou adjetivo; ordenação só por cargo, nome ou data | Implementado (conferir em revisão) |
| RF10 Navegação nacional/estados | Deve | Front-end (fora do escopo do back) | Não se aplica |
| RF11 Lembrar o estado | Poderia | Front-end, sem geolocalização | Não se aplica |
| RF12 Ausência de dados | Deve | Envelope `Resultado`: `disponivel=false` e mensagem "Informação não disponível nas fontes oficiais consultadas" | Implementado |
| RF13 Obter e atualizar dados de fontes validadas | Deve | `POST /api/admin/importacoes`, `GET /api/admin/importacoes`, gestão de fontes em `/api/admin/fontes` | Parcial: importação manual por JSON; atualização agendada fica como "Poderia" |

## Regras de negócio

| RN | Como o back-end garante |
|---|---|
| RN01 Só mandato atual | Consultas públicas exigem `em_exercicio` e a data de hoje entre `inicio_mandato` e `fim_mandato` (nulos valem como sem limite) |
| RN02 Granularidade de estado | A API é organizada por UF; cargo estadual exige UF e cargo nacional não pode ter |
| RN03 Sem geolocalização | Nenhum endpoint recebe ou guarda localização do usuário |
| RN04 Sem viés | Sem campos de nota ou ranking; listas ordenadas por cargo, nome ou data |
| RN05 Só fontes aprovadas | Consultas públicas só devolvem registros de fonte aprovada e ativa; aprovação registrada com quem e quando |
| RN06 Conteúdo nacional e do estado escolhido | Endpoints por UF devolvem só aquele estado |
| RN07 Projeto aberto | Licença pendente de definição com o cliente (PO) |

## Endpoints do painel (módulos ADS e BES)

| Módulo | Tela | Endpoints |
|---|---|---|
| ADS | Usuários (somente ADMIN) | `GET/POST /api/admin/usuarios`, `PUT /api/admin/usuarios/{id}` |
| ADS | Políticos | `GET/POST /api/admin/politicos`, `GET/PUT/DELETE /api/admin/politicos/{id}` |
| ADS | Propostas e ações | `GET/POST /api/admin/politicos/{id}/propostas` e `/acoes`, `PUT/DELETE /api/admin/propostas/{id}` e `/acoes/{id}` |
| ADS | Relatórios | `GET /api/admin/relatorios/cobertura`, `/cobertura.csv`, `/fontes`, `GET /api/admin/resumo` |
| BES | Fontes de dados | `GET/POST /api/admin/fontes`, `PUT/DELETE /api/admin/fontes/{id}`, `POST /api/admin/fontes/{id}/aprovar` e `/revogar` |
| BES | Importação de dados | `GET/POST /api/admin/importacoes` |
| BES | Candidaturas (somente API, sem tela) | `GET /api/admin/candidaturas?ano=`, `POST /api/admin/importacoes/candidaturas` |

A tela de login não conta na pontuação mínima e usa `POST /api/auth/login`, `POST /api/auth/logout` e `GET /api/auth/me`.

## Extensão: candidaturas (fora do escopo validado)

Pedido da equipe: mostrar também quem **concorreu** à eleição de 2026, somente para **presidente e governador**. Isso vai além do RN01 (só mandato atual) e do Termo de Aceite, e **depende de decisão do cliente**, via PO. Fica separado de quem exerce mandato.

| Endpoint | O que devolve |
|---|---|
| `GET /api/eleicoes/{ano}/presidente` | Candidatos a presidente da eleição, em ordem alfabética |
| `GET /api/eleicoes/{ano}/ufs/{sigla}/governador` | Candidatos a governador da UF, em ordem alfabética |

Cada candidatura traz nome, nome de urna, número, partido, cargo, UF, situação final (`ELEITO`, `NAO_ELEITO`, `SEGUNDO_TURNO` ou `SEM_RESULTADO`) e o bloco de fonte (RF08).

| Requisito | Como é atendido na extensão |
|---|---|
| RN04 e RF09 | Sem votos, nota ou ranking; sempre em ordem alfabética |
| RN05 | Só aparece com fonte aprovada pelo cliente e ativa |
| RF12 | Sem dado, a resposta traz `disponivel: false` e a mensagem neutra |
| RF08 | Fonte, link e data de atualização em cada candidatura |
