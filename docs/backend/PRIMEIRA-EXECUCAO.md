# Primeira execução: passo a passo e o que verificar

O código do back-end foi escrito e conferido estaticamente (sintaxe, referências entre classes, correspondência entre entidades e tabelas), mas **ainda não foi compilado nem executado**, porque o ambiente em que foi gerado não alcança o Maven Central. Esta lista é o caminho mais curto para confirmar tudo.

## Passo a passo

1. `cp .env.example .env` e troque as senhas (inclusive `ADMIN_PASSWORD`, com 8 ou mais caracteres).
2. `docker compose up -d` e confira `docker compose ps` até aparecer *healthy*.
3. `cd backend`
4. `./mvnw test`
5. `./mvnw spring-boot:run`
6. No log, procure: Flyway aplicando V1, V2 e V3; "Administrador inicial 'admin' criado"; "Started MinhaVidaPoliticaApplication".
7. Abra `http://localhost:8080/swagger-ui.html` e `http://localhost:8080/api/ufs` (deve listar 27 UFs).
8. Abra `http://localhost:8080/admin/login.html` e entre com `ADMIN_USERNAME` e `ADMIN_PASSWORD`.
9. Se tudo estiver certo, mude `JPA_DDL_AUTO=validate` no `.env` e reinicie: o Hibernate passa a conferir as entidades contra o banco.

## Se algo falhar

| Sintoma | O que fazer |
|---|---|
| `Non-resolvable parent POM` ou versão do Spring Boot inexistente | Ajuste `<version>` do parent no `pom.xml` para a versão 4.1.x estável que o Spring Initializr mostrar |
| `./mvnw` não executa | Rode `mvn -N wrapper:wrapper` uma vez (com Maven instalado) ou use o Maven da IDE |
| Erros de "cannot find symbol" em `getX()`, `builder()` ou `log` | O Lombok não está processando: ative *annotation processing* na IDE |
| Erro de conexão ou `Access denied` | Confira `docker compose ps`, a porta `MYSQL_PORT` e se as senhas do `.env` não mudaram depois do primeiro `up` (se mudaram: `docker compose down -v`) |
| Flyway falha ao aplicar V2 | Banco com tabelas de tentativas anteriores: recrie com `docker compose down -v` e suba de novo |
| `Schema-validation` falha com `JPA_DDL_AUTO=validate` | Volte para `none`, anote a coluna indicada no erro e corrija a entidade ou crie a migration V4 |
| `.env` não é lido | Rode a aplicação a partir de `backend/` (na IDE, *working directory* = pasta `backend`) |
| `/swagger-ui.html` com erro ou página em branco | Confira a versão do springdoc no `pom.xml` (linha 3.x é a do Spring Boot 4) |

## Fora do `pom.xml` de propósito

- **Testcontainers:** os testes atuais são unitários e não precisam de Docker. Quando o time criar testes de integração, adicione as dependências correspondentes à versão do Spring Boot em uso.
- **Jsoup, RestClient e `@Scheduled`:** a atualização automática de dados (RF13) ficou como "Poderia". A versão atual importa dados por arquivo JSON, com registro de aprovação da fonte.
