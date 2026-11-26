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
- Entram só candidaturas **deferidas**. As demais aparecem contadas no terminal, e `--todas-situacoes` as inclui.
- Quem foi ao segundo turno fica com o resultado do **maior turno** disponível no arquivo.
- A eleição já terminou (1º turno em 4/10 e 2º em 25/10 de 2026): são candidatos que **concorreram**.
- **Não foi testado com o arquivo real do TSE.** Os nomes das colunas (`DS_CARGO`, `SG_UF`, `SQ_CANDIDATO`, `NM_CANDIDATO`, `NR_CANDIDATO`, `DS_SIT_TOT_TURNO` etc.) seguem o layout usual do TSE, e o teste usou um ZIP sintético (`exemplo/consulta_cand_2026_exemplo.zip`). Se alguma coluna obrigatória faltar, o conversor para e lista as colunas que encontrou.

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
   $corpo = [System.IO.File]::ReadAllBytes("candidaturas-tse.json")
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
