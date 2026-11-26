# Cartão 6.4: Fichas das telas dos módulos ADS e BES

**Entregável:** prints das telas de cada módulo com breve descrição de função e fluxo operacional, para o documento no Overleaf (atividade 6.10).

**Mínimo exigido:** 4 telas funcionais no módulo ADS e 2 no módulo BES, sem contar a tela de login. Este documento descreve as 6 telas (ADS-1 a ADS-4 e BES-1 e BES-2).

**Prints:** devem ser tirados do sistema rodando de verdade e salvos em `docs/backend/telas/prints/` com os nomes indicados em cada ficha (ex.: `ADS-2_Politicos.png`). Sugestão para ter dados na tela: importar `docs/backend/api/exemplo-importacao.json` (dados fictícios, rotulados como demonstração) e aprovar a fonte antes de fotografar as telas de Políticos, Propostas e ações e Relatórios. Para o print da importação com rejeições, use `exemplo-importacao-com-erros.json`.

As telas são páginas HTML, CSS e JavaScript servidas pelo próprio back-end em `http://localhost:8080/admin/login.html`.

## ADS-1: Usuários (módulo ADS)

- **Print:** `prints/ADS-1_Usuarios.png` (a tirar com o sistema rodando)
- **Arquivo da tela:** `backend/src/main/resources/static/admin/usuarios.html`
- **Função:** Gerencia quem pode entrar no painel: cria usuários, altera nome, perfil e situação, e redefine senhas.
- **Perfil de acesso:** Somente ADMIN. O EDITOR não vê o item no menu e, se abrir a tela, recebe o aviso de tela exclusiva.
- **Fluxo operacional:**
  1. O administrador abre **Usuários** e vê a tabela com nome, login, perfil, situação e último acesso.
  2. Clica em **Novo usuário**, informa nome, login, perfil (Editor ou Administrador), senha com ao menos 8 caracteres e se o usuário está ativo, e salva.
  3. Para alterar, clica em **Editar**. O login não muda; deixar a senha em branco mantém a atual.
  4. O sistema recusa desativar o próprio usuário e recusa remover o último administrador ativo, explicando o motivo na própria tela.
- **Regras de negócio e requisitos:** Perfis ADMIN e EDITOR; senha guardada só como hash BCrypt; login único.
- **Endpoints utilizados:** `GET /api/admin/usuarios`, `POST /api/admin/usuarios`, `PUT /api/admin/usuarios/{id}`

## ADS-2: Políticos (módulo ADS)

- **Print:** `prints/ADS-2_Politicos.png` (a tirar com o sistema rodando)
- **Arquivo da tela:** `backend/src/main/resources/static/admin/politicos.html`
- **Função:** Controle dos dados de quem está em exercício no mandato atual: busca, cadastro, edição e exclusão.
- **Perfil de acesso:** ADMIN e EDITOR.
- **Fluxo operacional:**
  1. O usuário abre **Políticos** e vê a lista com cargo, estado, partido, fonte e situação de cada um (visível ao cidadão ou retido).
  2. Filtra por nome, estado ou cargo.
  3. Clica em **Novo político**. Ao escolher um cargo nacional, o campo Estado é desabilitado; para cargo estadual, o estado é obrigatório.
  4. Informa a fonte do dado (obrigatória) e, se houver, o link do registro na fonte. Salva.
  5. O sistema recusa duplicidade (mesmo nome, cargo e estado) e mandato com fim anterior ao início.
  6. Em **Excluir**, confirma a remoção. As propostas e ações do político são removidas junto.
  7. O atalho **Propostas e ações** leva à tela seguinte já com o político selecionado.
- **Regras de negócio e requisitos:** RN01 (mandato atual), RN02 (estado por cargo), RN05 (visibilidade pela fonte), RF07, RF08.
- **Endpoints utilizados:** `GET /api/admin/politicos`, `POST /api/admin/politicos`, `PUT /api/admin/politicos/{id}`, `DELETE /api/admin/politicos/{id}`, `GET /api/admin/catalogos`

## ADS-3: Propostas e ações (módulo ADS)

- **Print:** `prints/ADS-3_Propostas-e-acoes.png` (a tirar com o sistema rodando)
- **Arquivo da tela:** `backend/src/main/resources/static/admin/propostas-acoes.html`
- **Função:** Registra o que cada político propôs e o que já realizou no mandato atual.
- **Perfil de acesso:** ADMIN e EDITOR.
- **Fluxo operacional:**
  1. O usuário escolhe um político na lista.
  2. Alterna entre as abas **Propostas** e **Ações concretizadas**.
  3. Clica em **Nova proposta** ou **Nova ação**, informa título, descrição factual, data, fonte e link. Para ações, a data é obrigatória.
  4. A tabela indica, em cada registro, se ele já é visível ao cidadão ou está retido porque a fonte ainda não foi aprovada.
  5. Edita ou exclui registros com confirmação.
- **Regras de negócio e requisitos:** RF05, RF06 (toda ação tem fonte e data), RF08, RN05.
- **Endpoints utilizados:** `GET /api/admin/politicos/{id}/propostas`, `POST /api/admin/politicos/{id}/propostas`, `PUT /api/admin/propostas/{id}`, `DELETE /api/admin/propostas/{id}`, `GET /api/admin/politicos/{id}/acoes`, `POST /api/admin/politicos/{id}/acoes`, `PUT /api/admin/acoes/{id}`, `DELETE /api/admin/acoes/{id}`

## ADS-4: Relatórios (módulo ADS)

- **Print:** `prints/ADS-4_Relatorios.png` (a tirar com o sistema rodando)
- **Arquivo da tela:** `backend/src/main/resources/static/admin/relatorios.html`
- **Função:** Gera relatórios de cobertura: o que o cidadão vê em cada estado e quais registros estão retidos por falta de aprovação de fonte.
- **Perfil de acesso:** ADMIN e EDITOR.
- **Fluxo operacional:**
  1. O usuário abre **Relatórios** e vê a grade com as 27 UFs coloridas por situação: completo (governador com proposta e ação), parcial ou sem dados visíveis.
  2. Clica em uma UF para ver o governador visível, a quantidade de outros representantes e quantos políticos têm propostas e ações.
  3. Na tabela **Registros por fonte**, vê quantos registros cada fonte sustenta e quais estão retidos.
  4. Clica em **Baixar cobertura em CSV** para abrir o relatório no Excel.
- **Regras de negócio e requisitos:** RN05, RF12 (onde falta dado, o cidadão vê a mensagem neutra).
- **Endpoints utilizados:** `GET /api/admin/relatorios/cobertura`, `GET /api/admin/relatorios/cobertura.csv`, `GET /api/admin/relatorios/fontes`

## BES-1: Fontes de dados (módulo BES)

- **Print:** `prints/BES-1_Fontes-de-dados.png` (a tirar com o sistema rodando)
- **Arquivo da tela:** `backend/src/main/resources/static/admin/fontes.html`
- **Função:** Cadastra as fontes oficiais e registra a aprovação formal do cliente, que libera os dados da fonte para o cidadão.
- **Perfil de acesso:** ADMIN e EDITOR cadastram e editam. Registrar ou revogar aprovação e excluir: somente ADMIN.
- **Fluxo operacional:**
  1. O usuário abre **Fontes de dados** e vê cada fonte com o selo de situação: aprovada pelo cliente, aguardando aprovação ou inativa.
  2. Clica em **Nova fonte** e informa nome, endereço, tipo e periodicidade de atualização. A fonte nasce sem aprovação.
  3. O administrador clica em **Registrar aprovação**, informa quem do cliente aprovou e a data. O sistema exige periodicidade definida.
  4. Com a fonte aprovada, os dados ligados a ela passam a aparecer ao cidadão. **Revogar aprovação** faz os dados voltarem a ficar retidos.
  5. **Excluir** só é possível se nenhum registro estiver ligado à fonte.
- **Regras de negócio e requisitos:** RN05 (só fontes aprovadas), RF13 (aprovação registrada e periodicidade definida).
- **Endpoints utilizados:** `GET /api/admin/fontes`, `POST /api/admin/fontes`, `PUT /api/admin/fontes/{id}`, `POST /api/admin/fontes/{id}/aprovar`, `POST /api/admin/fontes/{id}/revogar`, `DELETE /api/admin/fontes/{id}`

## BES-2: Importação de dados (módulo BES)

- **Print:** `prints/BES-2_Importacao-de-dados.png` (a tirar com o sistema rodando)
- **Arquivo da tela:** `backend/src/main/resources/static/admin/importacao.html`
- **Função:** Carrega dados de uma fonte a partir de um arquivo JSON e mantém o histórico de cada carga.
- **Perfil de acesso:** ADMIN e EDITOR.
- **Fluxo operacional:**
  1. O usuário escolhe a fonte. Se ela não estiver aprovada, a tela avisa que os dados ficarão retidos.
  2. Envia o arquivo JSON ou cola o conteúdo. A tela confere o formato antes de enviar e explica qualquer erro.
  3. Clica em **Importar dados**. O sistema cria ou atualiza os registros (reimportar não duplica) e recusa os inválidos sem derrubar a carga.
  4. O resultado mostra quantos registros foram criados, atualizados e rejeitados, com o motivo de cada rejeição.
  5. O **Histórico** lista as últimas importações com data, usuário e resultado. O botão **Baixar arquivo modelo** entrega o formato esperado.
- **Regras de negócio e requisitos:** RF13, RN05, RF06 (ação sem data é rejeitada).
- **Endpoints utilizados:** `GET /api/admin/importacoes`, `POST /api/admin/importacoes`, `GET /api/admin/catalogos`

## Telas de apoio (não entram na contagem mínima)

- **Login** (`login.html`): autentica o usuário da equipe mantenedora em `POST /api/auth/login`. Login ou senha incorretos, ou usuário desativado, mostram mensagem na própria tela.
- **Visão geral** (`index.html`): resume quantos estados já mostram o governador ao cidadão, os números de cadastros, o aviso de fontes sem aprovação e as últimas importações (`GET /api/admin/resumo`).
