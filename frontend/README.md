# Front-end público — Minha VidaPolitica

Site do cidadão em **HTML, CSS e JavaScript puro**, feito a partir do protótipo do Figma
*KSI — Koda Software Inteligente — Protótipo*. Não depende de build nem de framework.
O back-end (`../backend`) fornece a API pública consumida aqui.

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
- `anoEleicao`: ano da eleição cujos candidatos aparecem (padrão `2026`).
- `modo`:
  - `"auto"` (padrão): usa a API; se ela estiver fora do ar, mostra o conteúdo do protótipo.
  - `"api"`: usa só a API. Recomendado para publicar.
  - `"demo"`: usa só o conteúdo do protótipo (`js/dados-demo.js`).

Também dá para forçar pela URL: `?modo=demo` ou `?modo=api`.

Endpoints usados (todos públicos, só leitura): `GET /api/presidente`, `/api/ufs`, `/api/ufs/{sigla}`,
`/api/ufs/{sigla}/representantes`, `/api/politicos/{id}/propostas`, `/api/politicos/{id}/acoes`,
`/api/eleicoes/{ano}/presidente` e `/api/eleicoes/{ano}/ufs/{sigla}/governador`.
Quando a API responde `disponivel: false`, a tela mostra a mensagem neutra que vem dela (RF12).
Os links das fontes abrem o original em nova aba (RF08).

O conteúdo de `js/dados-demo.js` é o texto do protótipo, para demonstração. Ele não passou
pela validação de fontes do back-end (RN05).

## Candidatos da eleição

Abaixo do cartão do mandato atual, cada página mostra **quem concorreu** na eleição de `anoEleicao`:

- `index.html`: candidatos à Presidência.
- `estado.html?uf=BA`: candidatos ao governo **daquele estado** (RN06).

Cada cartão traz a foto oficial (ou as iniciais, quando não há foto), o nome, o número da urna, o partido, o resultado final (Eleito(a), Não eleito(a), Foi ao 2º turno) e, quando existir, o link **Plano de governo (PDF)** registrado no TSE. A lista fica em **ordem alfabética**, sem votos nem ranking, e o resultado usa a mesma etiqueta neutra para todos (RN04). A seção mostra a fonte e a data de atualização (RF08). Sem dado em fonte aprovada, aparece a mensagem neutra (RF12). As fotos e os PDFs ficam em `assets/fotos/<ano>/` e `assets/planos/<ano>/`, gerados por `scripts/tse/ExtrairMidias.java`. Se uma foto não carregar, o cartão mostra as iniciais. O crédito do TSE (Creative Commons Atribuição) aparece abaixo da lista. No modo `demo`, ou com a API fora do ar no modo `auto`, a seção fica oculta.

**Atenção:** esta seção não existe no protótipo do Figma e foi pedida depois da validação com o cliente. Ela é uma extensão fora do escopo validado (RN01) e depende da decisão do cliente e da revisão do design.

A foto do cartão do mandato atual vem do campo `fotoUrl` do político, quando existe.

Na aba **Propostas**, quando a fonte de uma proposta aponta para um PDF do próprio site (`assets/planos/...`), o cartão ganha o botão **Ler o plano aqui**, que abre o PDF dentro da página, sem sair dela. O link da fonte continua abrindo o arquivo numa nova aba. O leitor usa o visualizador de PDF do navegador: em alguns celulares ele mostra só a primeira página ou oferece o download, e o link da fonte resolve.

Quando a API envia o `resumo` do político (por exemplo, "Governador interino desde…"), ele aparece abaixo do partido no cartão do mandato atual.

## Diferenças em relação ao protótipo

- A API ainda não tem **status** da proposta (Em andamento, Concluída, Apresentada). Com dados reais,
  o selo de status só aparece quando o campo existir; a faixa lateral fica azul.
- O cartão de resumo "Deputados federais e estaduais" só aparece no modo demo (a API não envia esses totais).
- As telas `noticia_*` do Figma imitam um portal de notícias externo (destino de um link de fonte)
  e não fazem parte deste site.
- A seção **Candidatos** (descrita acima) não faz parte do protótipo.
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
