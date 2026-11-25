# MinhaVidaPolitica.com

Plataforma web que reúne, de forma simples e sem viés, informações oficiais sobre políticos em exercício, por estado. Este repositório é um monorepo.

| Pasta | Conteúdo |
|---|---|
| `backend/` | API REST e painel da equipe (Java 17, Spring Boot, MySQL) |
| `frontend/` | Site público do cidadão (HTML, CSS e JavaScript) |
| `docs/backend/` | Diagramas, banco, contrato da API e fichas das telas (cartões 6.2, 6.4 e 6.5) |

## Stack do back-end

Java 17, Spring Boot 4.1, Spring Data JPA, Spring Security, Flyway, MySQL 8.4 LTS, Lombok, springdoc (Swagger), JUnit 5 e Mockito, Maven (wrapper) e Docker Compose para o banco local.

## Como rodar

Pré-requisitos: JDK 17 e Docker.

```bash
cp .env.example .env            # troque as senhas, inclusive ADMIN_PASSWORD (8+ caracteres)
docker compose up -d            # sobe o MySQL (porta 3307 por padrão)
cd backend
./mvnw test                     # testes unitários
./mvnw spring-boot:run          # API em http://localhost:8080
```

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Painel da equipe: `http://localhost:8080/admin/login.html` (usuário e senha do `.env`)
- Primeira execução e solução de problemas: [`docs/backend/PRIMEIRA-EXECUCAO.md`](docs/backend/PRIMEIRA-EXECUCAO.md)

## API pública (cidadão)

| Endpoint | Requisito |
|---|---|
| `GET /api/presidente` | RF01 |
| `GET /api/ufs` e `GET /api/ufs/{sigla}` | RF02, RF03 |
| `GET /api/ufs/{sigla}/representantes` | RF04 |
| `GET /api/politicos/{id}`, `/propostas`, `/acoes` | RF07, RF05, RF06 |

Toda resposta traz a fonte, o link e a data de atualização (RF08). Sem dado em fonte validada, a resposta traz `disponivel: false` e a mensagem neutra do RF12.

## Roteiro de demonstração (regra RN05 em ação)

Os dados de exemplo são fictícios e rotulados como demonstração (`docs/backend/api/exemplo-importacao.json`).

1. Entre no painel e abra **Fontes de dados**. Cadastre "Fonte de demonstração" com periodicidade "Mensal". Ela nasce **aguardando aprovação**.
2. Abra **Importação de dados**, escolha a fonte e envie `exemplo-importacao.json`. A tela avisa que os dados ficam retidos.
3. Consulte `http://localhost:8080/api/presidente`: resposta com `disponivel: false` e a mensagem neutra.
4. Volte em **Fontes de dados** e use **Registrar aprovação** (informe quem do cliente aprovou).
5. Consulte `/api/presidente` e `/api/ufs/BA` de novo: agora vêm os dados, com fonte, link e data.
6. Abra **Relatórios** e veja Bahia e São Paulo como "completo". Use `exemplo-importacao-com-erros.json` para mostrar a rejeição de registros inválidos.

## Regras de negócio no código

| Regra | Onde |
|---|---|
| RN01 só mandato atual | Consultas `...Visiveis` em `PoliticoRepository` |
| RN05 só fonte aprovada e ativa | Mesmas consultas e `FonteDados.liberaExibicao()` |
| RF12 mensagem neutra | `Resultado` e `PublicoService` |
| RF13 aprovação e periodicidade da fonte | `FonteService` e `ImportacaoService` |

## Convenções de trabalho

- `main` sempre estável. Mudanças por Pull Request com 1 aprovação.
- Branches com o número da atividade do roteiro: `feature/6.4-modulo-ads`, `docs/6.5-banco`.
- Commits no padrão *Conventional Commits*: `feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `chore:`.
- Migrations do Flyway nunca são editadas depois de aplicadas: mudanças entram em nova versão.
- Nenhuma senha ou chave no código. Tudo vai no `.env`, que não é versionado.

## Licença

A licença do projeto (RN07, código aberto) ainda será definida com o cliente.
