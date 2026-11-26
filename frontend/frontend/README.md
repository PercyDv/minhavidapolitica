# Site do cidadão (front-end)

Páginas em **HTML, CSS e JavaScript puros** (sem bibliotecas nem build), como pede o roteiro da II Unidade. O visual segue o protótipo validado no Figma.

## O que o site mostra

- **Página do Brasil** (`#/`): o presidente em exercício, com abas de propostas e do que já foi feito, e os **candidatos à Presidência** da eleição de 2026.
- **Página do estado** (`#/uf/BA`): o governador em exercício, os **candidatos a governador** daquele estado e os demais representantes. Ao clicar num estado, o cidadão vê só o que é daquele estado (RN06).
- **Seletor de estado** no topo, com as 27 UFs, disponível em todas as páginas (RF02 e RF10). A escolha é manual: o site não usa a localização (RN03).

Regras que a interface respeita:

- Cada bloco mostra a **fonte, o link e a data de atualização** (RF08).
- Sem dado em fonte aprovada, aparece "Informação não disponível nas fontes oficiais consultadas" (RF12).
- Candidatos em **ordem alfabética**, sem votos, notas ou ranking (RN04 e RF09). "Eleito(a)" e "Não eleito(a)" aparecem na mesma etiqueta neutra.
- Os candidatos são uma **extensão fora do escopo validado** (RN01): dependem da decisão do cliente.

## Como rodar

Pré-requisitos: o back-end rodando em `http://localhost:8080`, os dados importados e as fontes aprovadas.

```powershell
cd frontend
python -m http.server 5500 --bind 127.0.0.1
```

Abra `http://localhost:5500`. Também funciona com o Live Server do VS Code, que usa a porta 5500.

**Sirva só a pasta `frontend`.** Servir a raiz do repositório expõe o `.env` e a pasta `.git`. O `--bind 127.0.0.1` limita o acesso ao seu computador.

## Configuração

Em `js/config.js`:

| Campo | Para que serve |
|---|---|
| `api` | Endereço do back-end (padrão `http://localhost:8080`) |
| `ano` | Ano da eleição dos candidatos exibidos (padrão `2026`) |

A API só aceita consultas do site vindas de `http://localhost:5500`, `http://127.0.0.1:5500` e `http://localhost:3000`. Se usar outra porta, defina no `.env` do back-end `CORS_ALLOWED_ORIGINS=http://localhost:PORTA` e reinicie. Abrir o `index.html` direto do disco (`file://`) não funciona.

## Endereços da API usados

| Tela | Endereços |
|---|---|
| Todas | `GET /api/ufs` |
| Brasil | `GET /api/presidente`, `GET /api/eleicoes/2026/presidente` |
| Estado | `GET /api/ufs/{sigla}`, `GET /api/ufs/{sigla}/representantes`, `GET /api/eleicoes/2026/ufs/{sigla}/governador` |
| Abas do cartão | `GET /api/politicos/{id}/propostas` e `/acoes` |

## Se algo não aparece

| Sintoma | Causa provável |
|---|---|
| "Não foi possível carregar os dados agora" | A API não está no ar, ou o navegador bloqueou por CORS (veja o console, F12) |
| "Informação não disponível" nos candidatos | A fonte `TSE: candidatos 2026` não está aprovada e ativa, ou as candidaturas não foram importadas |
| "Informação não disponível" no governador | A fonte dos mandatos atuais não está aprovada, ou o `inicioMandato` ainda não chegou |
| Lista de estados vazia | `GET /api/ufs` falhou: confira o back-end |

## Arquivos

```
frontend/
├── index.html        estrutura, topo e seletor de estado
├── css/estilo.css    visual (cores do protótipo)
└── js/
    ├── config.js     endereço da API e ano da eleição
    ├── api.js        chamadas à API
    ├── util.js       escape de texto, links seguros e datas
    └── app.js        rotas, páginas e seletor
```

## Limites desta versão

- Testado no navegador contra uma API simulada que serve os mesmos JSONs importados, e **não** contra o back-end Spring real, que ainda não foi executado.
- Sem fotos (a API ainda não traz foto dos políticos) e sem "lembrar o estado" (RF11, prioridade "Poderia").
- A lista de demais representantes só aparece quando houver senadores ou deputados importados.
