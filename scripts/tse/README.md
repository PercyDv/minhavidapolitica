# Conversor do arquivo de eleitos do TSE

Transforma o "Arquivo de resultado de eleitos" (EA10) do TSE no arquivo de importação do sistema, para carregar governadores (e, se quiser, senadores e deputados federais) pela tela **Importação de dados** ou por `POST /api/admin/importacoes`.

## O que ele faz e o que não faz

- Faz: lê o JSON do TSE, pega nome, partido, cargo, UF e vice, e gera o `importacao-tse.json` com a vigência do mandato.
- Não traz: foto, propostas e ações concretizadas. O TSE publica dados de **eleição**, e essas informações vêm de outras fontes.
- **Eleito não é o mesmo que em exercício.** Renúncia, afastamento ou vice assumindo não aparecem no arquivo. Por isso cada registro sai com o resumo "Eleito(a) em 2026 pelo resultado oficial do TSE. Dado de eleição: não confirma o exercício atual do cargo."
- O Presidente não consta na tabela de cargos da especificação do arquivo de eleitos. O código aceita `PRESIDENTE` caso o TSE publique esse arquivo, mas isso não foi testado.
- **Foi testado apenas com arquivos sintéticos** (pasta `exemplo/`), escritos a partir da especificação oficial do EA10 (versão de março de 2026). Não foi testado com um arquivo real das Eleições 2026.

## Passo a passo

1. **Baixe o arquivo de eleitos** no site do TSE (Portal de Dados Abertos, `https://dadosabertos.tse.jus.br/`, e a página de informações técnicas da divulgação de resultados). Os nomes seguem o padrão `br-c0003-e<número da eleição>-e.json` (governador), `br-c0005-...` (senador) e `br-c0006-...` (deputado federal). Guarde fora do Git.
2. **Rode o conversor**, na raiz do repositório (PowerShell):

   ```powershell
   java scripts\tse\ConversorTse.java --entrada C:\caminho\br-c0003-e000000-e.json --saida importacao-tse.json
   ```

   Se os acentos aparecerem trocados no terminal, acrescente `-Dstdout.encoding=UTF-8` depois de `java`.
3. **Revise o `importacao-tse.json`**: nomes, partidos e datas. Os avisos impressos no terminal dizem o que foi ignorado e por quê.
4. No painel, em **Fontes de dados**, cadastre a fonte "TSE: resultado de eleitos" (tipo: arquivo de dados, endereço do portal do TSE, periodicidade: a cada eleição).
5. Em **Importação de dados**, escolha essa fonte e envie o `importacao-tse.json`.
6. O cliente precisa **aprovar a fonte** (RN05). Sem aprovação, os dados ficam retidos.
7. O registro só aparece ao cidadão **a partir da data de início do mandato** (ver tabela abaixo).

## Opções

| Opção | Para que serve |
|---|---|
| `--entrada <arquivo>` | Arquivo do TSE. Pode repetir a opção para vários arquivos |
| `--saida <arquivo>` | Arquivo gerado (padrão `importacao-tse.json`) |
| `--ano <aaaa>` | Ano da eleição (padrão 2026); define as datas de mandato |
| `--cargos <lista>` | `GOVERNADOR` (padrão), `SENADOR`, `DEPUTADO_FEDERAL`, `PRESIDENTE`, separados por vírgula |
| `--inicio`, `--fim` | Datas de mandato (`aaaa-mm-dd`) no lugar do padrão do ano |
| `--link <url>` | Link guardado em cada registro (padrão: portal de dados abertos do TSE) |
| `--nome-urna` | Usa o nome de urna em vez do nome completo |
| `--permitir-simulado` | Aceita arquivos do simulado do TSE (somente para testes) |

## Mapeamento

| TSE | Sistema |
|---|---|
| `nm` (ou `nmu` com `--nome-urna`) | `nome` (convertido de MAIÚSCULAS para "Nome Próprio") |
| `sgp` | `partido` |
| `cdcar` (3, 5, 6, 1) | `cargo` (`GOVERNADOR`, `SENADOR`, `DEPUTADO_FEDERAL`, `PRESIDENTE`) |
| `cdabr` de cada `abr` | `uf` |
| `vs` com `tp = v` | citado no `resumo` como vice eleito |

## O que é ignorado, com aviso

- Arquivo do simulado (erro, a menos que use `--permitir-simulado`).
- Governador com **2 candidatos** na UF: é segundo turno ainda sem definição. Use o arquivo do 2º turno quando sair.
- UF sem atribuição de eleito (`esae = s`) ou sem candidatos listados.
- Cargos fora da opção `--cargos`, abrangências que não sejam de UF e registros repetidos.

## Datas de mandato padrão

| Eleição | Cargo | Início | Fim |
|---|---|---|---|
| 2026 | Governador | 2027-01-06 | 2031-01-05 |
| 2026 | Presidente | 2027-01-05 | 2031-01-04 |
| 2026 | Senador | 2027-02-01 | 2035-01-31 |
| 2026 | Deputado Federal | 2027-02-01 | 2031-01-31 |
| 2022 | Governador | 2023-01-01 | 2027-01-05 |
| 2022 | Presidente | 2023-01-01 | 2027-01-04 |
| 2022 | Senador | 2023-02-01 | 2031-01-31 |
| 2022 | Deputado Federal | 2023-02-01 | 2027-01-31 |

As posses de presidente (5 de janeiro) e governadores (6 de janeiro) de 2027 e o fim dos mandatos de 2022 vêm da Emenda Constitucional 111/2021; deputados federais e senadores tomam posse em 1º de fevereiro. As durações de 4 anos (governador, presidente, deputado) e de 8 anos (senador) são as regras gerais da Constituição. Confira as datas antes de importar.

## Como o sistema trata a virada de mandato

- Quem tem `inicioMandato` no futuro **não aparece** ao cidadão até a data chegar. Por isso dá para importar os eleitos de 2026 agora, sem eles substituírem os atuais antes da posse.
- **Reeleição:** se o sistema já tem a mesma pessoa, no mesmo cargo e UF, com mandato vigente hoje, a importação só **estende o fim do mandato**. Fonte, partido e demais dados do registro vigente não mudam, para a pessoa não sumir da consulta por causa de uma fonte ainda não aprovada.
- Os **mandatos de 2022** (os atuais, até janeiro de 2027) dependem de dados de 2022. Este conversor só lê o formato do arquivo de eleitos; se o arquivo de 2022 tiver a mesma estrutura, use `--ano 2022`. Caso contrário, cadastre-os pela tela de Políticos ou peça um conversor para o formato CSV do Portal de Dados Abertos.

---

# Candidatos de uma eleição (presidente e governador)

**Extensão pedida pela equipe, fora do escopo validado (RN01):** mostrar quem concorreu, e não só quem exerce mandato. Depende de decisão do cliente.

## O que vem e o que não vem

- Vem: nome, nome de urna, número, partido, cargo, UF e a situação final (`ELEITO`, `NAO_ELEITO`, `SEGUNDO_TURNO` ou `SEM_RESULTADO`).
- Não vem, por escolha: votos e qualquer ordenação por resultado (RN04). A lista é sempre alfabética.
- **Dados pessoais:** o CSV do TSE tem CPF, e-mail e outros campos. O conversor só lê as colunas necessárias e nunca grava esses dados.
- Entram só candidaturas com **resultado apurado**. Quem ficou sem resultado (candidatura indeferida, substituída, cancelada ou desistente) é listado no terminal e fica de fora; `--todas-situacoes` inclui essas candidaturas como `SEM_RESULTADO`. O arquivo publicado depois da eleição traz a situação da candidatura como `#NE` (não divulgado), então ela não serve de filtro.
- Quem foi ao segundo turno fica com o resultado do **maior turno** disponível no arquivo.
- A eleição já terminou (1º turno em 4/10 e 2º em 25/10 de 2026): são candidatos que **concorreram**.
- **Testado com o arquivo real** `consulta_cand_2026.zip` de 26/11/2026: 204 candidaturas (13 a presidente e 191 a governador), 28 eleitos (27 governadores e o presidente) e 11 sem resultado apurado. Se o TSE mudar o layout e faltar uma coluna obrigatória, o conversor para e lista as colunas que encontrou. `exemplo/consulta_cand_2026_exemplo.zip` é um arquivo sintético para testes.

## Passo a passo

1. Baixe `consulta_cand_2026.zip` no dataset de candidatos de 2026 do Portal de Dados Abertos (`https://dadosabertos.tse.jus.br/dataset/candidatos-2026`).
2. Em **Fontes de dados**, cadastre a fonte "TSE: candidatos 2026" (tipo arquivo, periodicidade "A cada eleição") e anote o **id**.
3. Gere o arquivo (PowerShell, na raiz do repositório; troque o `3` pelo id da fonte):

   ```powershell
   java scripts\tse\ConversorCandidatos.java --entrada C:\caminho\consulta_cand_2026.zip --fonte-id 3 --link https://dadosabertos.tse.jus.br/dataset/candidatos-2026 --saida candidaturas-tse.json
   ```

4. Revise o `candidaturas-tse.json` e as situações impressas no terminal.
5. Envie para a API (não há tela para isso):

   ```powershell
   $base = "http://localhost:8080"
   Invoke-RestMethod -Uri "$base/api/auth/login" -Method Post -SessionVariable sessao -Body @{ username = "admin"; password = "SUA_SENHA" }
   $corpo = Get-Content .\candidaturas-tse.json -Raw -Encoding UTF8
   Invoke-RestMethod -Uri "$base/api/admin/importacoes/candidaturas" -Method Post -WebSession $sessao -ContentType "application/json; charset=utf-8" -Body $corpo
   ```

   A resposta informa quantas candidaturas foram criadas, atualizadas e rejeitadas, com o motivo das rejeições. Reenviar atualiza, sem duplicar.
6. Confira em `GET /api/admin/candidaturas?ano=2026`.
7. O cliente precisa **aprovar a fonte** (RN05). Depois disso, o cidadão consulta `GET /api/eleicoes/2026/presidente` e `GET /api/eleicoes/2026/ufs/BA/governador`.

## Opções do conversor

| Opção | Para que serve |
|---|---|
| `--entrada <arquivo>` | ZIP ou CSV do TSE. Pode repetir a opção |
| `--saida <arquivo>` | Arquivo gerado (padrão `candidaturas-tse.json`) |
| `--ano <aaaa>` | Ano da eleição (padrão: lido do arquivo) |
| `--fonte-id <n>` | Id da fonte no sistema; deixa o arquivo pronto para enviar |
| `--link <url>` | Link guardado em cada candidatura |
| `--todas-situacoes` | Inclui também candidaturas indeferidas, canceladas etc. |

---

# Fotos e planos de governo dos candidatos

Liga a **foto** e o **plano de governo** registrados no TSE a cada candidatura, para aparecerem nos cartões do site. O plano entra como **link para o PDF oficial**, sem resumo nem interpretação (RN04).

## O que a ferramenta faz

- Lê os ZIPs "Fotos de candidatos" e "Proposta de governo" do Portal de Dados Abertos do TSE (licença Creative Commons Atribuição).
- Acha o arquivo de cada candidato pelo **identificador do TSE (`sqCandidato`) que aparece no nome do arquivo**, seja qual for o formato do nome. Os nomes reais seguem `FBA50002536624_div.jpg` (foto) e `BA/2026BA50002536314_01.pdf` (plano). Se houver mais de um arquivo do mesmo candidato, fica o de **menor número** (`_01`), e a ferramenta avisa. Um candidato do DF, por exemplo, tem um `_02` que é um documento de outro processo, não o plano.
- Copia **só os candidatos do seu arquivo** (presidente e governador), para `frontend/assets/fotos/<ano>/` e `frontend/assets/planos/<ano>/`.
- Reduz fotos maiores que 400 px (JPEG). As fotos do TSE já são pequenas (cerca de 160×225 px, poucos KB) e ficam **exatamente como o TSE publicou**. PDFs acima de 25 MB são ignorados, com aviso. O `leiame.pdf` que vem nos ZIPs é ignorado.
- Confere se o conteúdo é mesmo imagem ou PDF, e ignora o que não for.
- Escreve um JSON novo com `fotoUrl` e `planoUrl` em cada candidatura.
- Com `--mandatos`, dá a foto a quem está no cargo **somente** se for sem dúvida a mesma pessoa de uma candidatura (mesmo cargo, mesma UF e mesmo nome). Os pares aparecem na tela para você revisar. Quem não foi candidato fica com as iniciais: o TSE não tem a foto dessa pessoa.

## Passo a passo

1. Baixe os ZIPs na página `https://dadosabertos.tse.jus.br/dataset/candidatos-2026`. Cada recurso tem o botão "Ir para recurso":
   - **BR - Fotos de candidatos** e **BR - Proposta de governo** (presidente);
   - **UF - Fotos de candidatos** e **UF - Proposta de governo** de cada estado que você quiser.

   Guarde as fotos em `C:\tse\fotos` e os planos em `C:\tse\planos`. Os ZIPs de UF trazem candidatos de outros cargos e podem ser grandes.
2. Para ver como os arquivos de dentro se chamam, sem gravar nada:

   ```powershell
   java scripts\tse\ExtrairMidias.java --listar --fotos C:\tse\fotos\AC.zip --planos C:\tse\planos\AC.zip
   ```
3. Na raiz do repositório, gere tudo. O PowerShell não expande `*.zip` para o Java, então use a lista abaixo:

   ```powershell
   $fotos  = Get-ChildItem C:\tse\fotos\*.zip  | ForEach-Object { "--fotos";  $_.FullName }
   $planos = Get-ChildItem C:\tse\planos\*.zip | ForEach-Object { "--planos"; $_.FullName }
   java scripts\tse\ExtrairMidias.java --candidaturas candidaturas-tse.json @fotos @planos --mandatos importacao-mandato-atual.json
   ```

   Use o `candidaturas-tse.json` que já tem o `fonteId` correto. O arquivo novo mantém esse valor.
4. A tela mostra quantas candidaturas ficaram com foto e com plano, quem ficou sem, e os avisos. Os arquivos gerados são:
   - `candidaturas-tse-midia.json`: candidaturas com `fotoUrl` e `planoUrl`;
   - `importacao-mandato-atual-com-fotos.json`: mandatos atuais com `fotoUrl` quando há par seguro;
   - as pastas `frontend/assets/fotos/<ano>` e `frontend/assets/planos/<ano>`.
5. Reimporte os dois arquivos pelo mesmo caminho de antes (a importação atualiza, sem duplicar), com o back-end reiniciado para o Flyway aplicar a migration V5:
   - candidaturas: `POST /api/admin/importacoes/candidaturas` com o `candidaturas-tse-midia.json`;
   - mandatos: tela **Importação de dados**, na fonte dos mandatos atuais, com o `importacao-mandato-atual-com-fotos.json`.
6. Recarregue o site (Ctrl+F5).

## Opções

| Opção | Para que serve |
|---|---|
| `--candidaturas <arq>` | JSON de candidaturas (precisa do `sqCandidato`) |
| `--fotos <zip>` / `--planos <zip>` | ZIPs do TSE. Repita a opção para vários |
| `--saida <pasta>` | Pasta base (padrão `frontend/assets`) |
| `--json-saida <arq>` | JSON atualizado (padrão `candidaturas-tse-midia.json`) |
| `--mandatos <arq>` | `importacao-mandato-atual.json`, para dar foto a quem está no cargo |
| `--mandatos-saida <arq>` | Saída dos mandatos (padrão `importacao-mandato-atual-com-fotos.json`) |
| `--lado-foto <px>` | Maior lado da foto (padrão 400) |
| `--limite-mb-plano <n>` | Ignora PDFs maiores que n MB (padrão 25) |
| `--listar` | Só lista o conteúdo dos ZIPs |

## Cuidados

- **Testada com os ZIPs reais de Brasil, Bahia e Distrito Federal** (26/11/2026): 30 fotos e 30 planos casados (13 da Presidência, 6 da Bahia e 11 do DF), todos os PDFs abrem, nenhum é protegido. Se o TSE mudar o formato dos nomes e nenhum arquivo casar, a ferramenta avisa e mostra exemplos de nomes.
- **Tamanho:** Brasil, Bahia e DF somam cerca de 39 MB de planos (as fotos, 0,2 MB). Para os 27 estados, a estimativa é de algumas centenas de MB. Se passarem de 200 MB, ela avisa; avalie se vale colocar no Git (o GitHub recusa arquivos acima de 100 MB, e a ferramenta já ignora PDFs acima de 25 MB).
- **Crédito:** a licença exige citar o TSE. O site já mostra "Fotos e planos de governo registrados no TSE (licença Creative Commons Atribuição)" quando há foto ou plano.
- **Nada de adivinhação:** a ferramenta nunca escolhe foto por semelhança de nome. Quem não casa pelo identificador fica sem foto.

---

# Plano de governo na aba "Propostas" de quem está no cargo

Quem está no cargo e também foi candidato em 2026 (hoje: Lula, Jerônimo Rodrigues e Celina Leão) ganha, na aba **Propostas**, um cartão com o **plano de governo registrado no TSE** e o botão **Ler o plano aqui**, que abre o PDF dentro da própria página. Não há resumo nem interpretação: é o documento oficial.

**Atenção ao sentido:** esse plano é o da **eleição de 2026**, ou seja, vale para o próximo mandato, e não para o mandato atual. O texto do cartão diz isso de forma explícita. As propostas do mandato atual seriam as de 2022 (base do TSE de 2022, que não foi carregada). Isso também toca a regra RN01 (sem projeções futuras) e depende da decisão do cliente.

## Como cadastrar

1. Tenha os PDFs em `frontend/assets/planos/2026/` (gerados pelo `ExtrairMidias.java`) e a fonte `TSE: candidatos 2026` cadastrada e aprovada.
2. Com a aplicação no ar, na raiz do repositório:

   ```powershell
   powershell -ExecutionPolicy Bypass -File scripts\tse\vincular-planos-aos-mandatos.ps1
   ```

   O script pede a senha do administrador e, para cada pessoa de `scripts/tse/planos-no-mandato.json`, cria a proposta (ou atualiza, se ela já existir: pode rodar mais de uma vez sem duplicar). O resultado são linhas `CRIADO` ou `ATUALIZADO`. Se alguma busca não achar exatamente um político, a linha é `PULADO`.
3. Recarregue o site (Ctrl+F5).

## Para incluir mais pessoas

Acrescente itens em `planos-no-mandato.json` com `busca` (parte do nome, só letras sem acento, que identifique uma única pessoa) e `arquivo` (`assets/planos/2026/<sqCandidato>.pdf`). O `sqCandidato` aparece no nome do arquivo do plano e no `candidaturas-tse-midia.json`. O script em si só tem caracteres ASCII de propósito, e os textos com acento ficam no arquivo `.json` (UTF-8).

