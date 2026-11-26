# Documentação do back-end

Tudo aqui alimenta os cartões 6.2, 6.4 e 6.5 do Trello. O mapeamento completo, com checklists e nomes de anexo, está em [`TRELLO.md`](TRELLO.md).

| Pasta ou arquivo | Cartão | Conteúdo |
|---|---|---|
| `diagramas/` | 6.2 | Diagrama de Classes (PNG e fonte PlantUML) e texto de apoio |
| `banco/` | 6.5 | Diagrama do banco (PNG e fonte PlantUML) e descrição técnica com dicionário de dados |
| `api/` | 6.4 | Contrato da API (YAML e JSON), matriz RF × endpoint e arquivos de exemplo de importação |
| `telas/` | 6.4 | Fichas das telas dos módulos ADS e BES e pasta `prints/` para as capturas |
| [`PRIMEIRA-EXECUCAO.md`](PRIMEIRA-EXECUCAO.md) | 6.4 | Passo a passo para rodar e conferir o sistema pela primeira vez |

**Regenerar os diagramas:** `java -jar plantuml.jar -tpng docs/backend/diagramas/*.puml docs/backend/banco/*.puml` (PlantUML e Graphviz instalados). Ao mudar o banco, crie uma nova migration, atualize o `.puml` e o dicionário e suba o número `vN` dos arquivos anexados.
