# Front-end público — Minha VidaPolitica

Site do cidadão em **HTML, CSS e JavaScript puro**, feito a partir do protótipo do Figma
*KSI — Koda Software Inteligente — Protótipo*. Não depende de build nem de framework.
O back-end (`../backend`) não foi alterado.

## Telas

| Arquivo | Tela do Figma |
|---|---|
| `index.html` | Brasil / Propostas e Brasil / O que já foi feito (desktop e celular) |
| `estado.html?uf=BA` | {Estado} / Propostas e {Estado} / O que já foi feito, com Demais representantes |
| `configuracoes.html` | configuracoes_desktop e configuracoes_mobile |
| menu no cabeçalho | Menu de estados — desktop e celular |

As abas "Propostas" e "O que já foi feito" trocam na mesma página (`?aba=feito` abre direto na segunda).
Abaixo de 768px de largura o layout muda para a versão de celular do protótipo.

## Como rodar

1. Suba a API como no README da raiz (`./mvnw spring-boot:run`, porta 8080).
2. No VS Code, abra a pasta `frontend` com a extensão **Live Server** (porta 5500).
   O back-end já libera CORS para `http://localhost:5500` e `http://127.0.0.1:5500`.
3. Acesse `http://localhost:5500/index.html`.

Sem Live Server: `python -m http.server 5500` dentro de `frontend/`.

## De onde vêm os dados

Configure em `js/config.js`:

- `apiBase`: endereço da API (padrão `http://localhost:8080/api`).
- `modo`:
  - `"auto"` (padrão): usa a API; se ela estiver fora do ar, mostra o conteúdo do protótipo.
  - `"api"`: usa só a API. Recomendado para publicar.
  - `"demo"`: usa só o conteúdo do protótipo (`js/dados-demo.js`).

Também dá para forçar pela URL: `?modo=demo` ou `?modo=api`.

Endpoints usados (todos públicos, só leitura): `GET /api/presidente`, `/api/ufs`, `/api/ufs/{sigla}`,
`/api/ufs/{sigla}/representantes`, `/api/politicos/{id}/propostas` e `/api/politicos/{id}/acoes`.
Candidatos da eleição (extensão fora do escopo validado): `/api/eleicoes/{ano}/presidente` na página Brasil e
`/api/eleicoes/{ano}/ufs/{sigla}/governador` na página do estado. O ano vem de `anoEleicao` em `js/config.js`
(ou `?ano=` na URL). No modo demo essa seção mostra a mensagem neutra: não há candidatos de demonstração.
Quando a API responde `disponivel: false`, a tela mostra a mensagem neutra que vem dela (RF12).
Os links das fontes abrem o original em nova aba (RF08).

O conteúdo de `js/dados-demo.js` é o texto do protótipo, para demonstração. Ele não passou
pela validação de fontes do back-end (RN05).

## Diferenças em relação ao protótipo

- A API ainda não tem **status** da proposta (Em andamento, Concluída, Apresentada). Com dados reais,
  o selo de status só aparece quando o campo existir; a faixa lateral fica azul.
- O cartão de resumo "Deputados federais e estaduais" só aparece no modo demo (a API não envia esses totais).
- As telas `noticia_*` do Figma imitam um portal de notícias externo (destino de um link de fonte)
  e não fazem parte deste site.
- Política de privacidade, Termos de uso e Enviar sugestão mostram "em preparação" até existirem.

## Estrutura

```
frontend/
├── index.html, estado.html, configuracoes.html
├── css/estilo.css          # tokens de cor do Figma, layout desktop e celular, tema escuro
├── js/config.js            # endereço da API e modo
├── js/preferencias.js      # preferências salvas no aparelho (tema, tamanho do texto, estado padrão)
├── js/api.js               # chamadas à API e conversão para as telas
├── js/dados-demo.js        # conteúdo do protótipo
├── js/ui.js                # componentes (cartões, linha do tempo, menu de estados, abas)
├── js/brasil.js, js/estado.js, js/configuracoes.js
└── assets/                 # ícones exportados do Figma e fontes (Atkinson Hyperlegible, Public Sans, Inter — licença OFL)
```
