// Minha Vida Política: site do cidadão. JavaScript puro, sem bibliotecas.
// Rotas (pelo # da URL): "#/" é a página do Brasil e "#/uf/BA" é a página de um estado.
import { CONFIG } from './config.js';
import { obter } from './api.js';
import { esc, linkSeguro, fmtData, mesAno, iniciais, ordemAlfabetica } from './util.js';

const MSG_INDISPONIVEL = 'Informação não disponível nas fontes oficiais consultadas.';
const TEXTO_SITUACAO = {
  ELEITO: 'Eleito(a)',
  NAO_ELEITO: 'Não eleito(a)',
  SEGUNDO_TURNO: 'Foi ao 2º turno',
  SEM_RESULTADO: 'Sem resultado',
};

const conteudo = document.getElementById('conteudo');
const botaoEstados = document.getElementById('btn-estados');
const dialogo = document.getElementById('dlg-estados');
const grade = document.getElementById('grade-uf');

let ufs = [];
let versao = 0; // descarta respostas de navegações antigas
let seq = 0; // ids únicos para ligar rótulos e cartões

/** GET que nunca lança: devolve { erro } para cada bloco decidir como se mostrar. */
async function seguro(caminho) {
  try {
    return await obter(caminho);
  } catch (erro) {
    return { erro };
  }
}

// ---------- blocos de interface ----------

function blocoFonte(fonte) {
  if (!fonte) {
    return '<p class="fonte"><span>Fonte: pendente</span><span class="pilula">Atualização: —</span></p>';
  }
  const data = fmtData(fonte.atualizadoEm);
  return `<p class="fonte"><span>Fonte: <a href="${esc(linkSeguro(fonte.link))}" target="_blank" rel="noopener noreferrer">${esc(fonte.nome)}</a></span>`
    + `<span class="pilula">Atualização: ${data ? esc(data) : '—'}</span></p>`;
}

function blocoMensagem(texto) {
  return `<p class="neutra">${esc(texto)}</p>`;
}

function textoMandato(p) {
  if (p.inicioMandato && p.fimMandato) return `Mandato atual: ${mesAno(p.inicioMandato)} a ${mesAno(p.fimMandato)}`;
  if (p.inicioMandato) return `Mandato atual: desde ${mesAno(p.inicioMandato)}`;
  return 'Mandato atual';
}

/** Cartão de quem exerce o cargo (presidente ou governador), com as abas de propostas e ações. */
function cartaoCargo(cargo, local, resultado) {
  const id = ++seq;
  if (resultado.erro || !resultado.disponivel || !resultado.dados) {
    const msg = resultado.erro ? resultado.erro.message : (resultado.mensagem || MSG_INDISPONIVEL);
    return `<section class="cartao" aria-labelledby="t${id}">
      <div class="pessoa">
        <div class="avatar" aria-hidden="true">–</div>
        <div>
          <p class="cargo">${esc(cargo)}</p>
          <h2 id="t${id}">Nome não disponível</h2>
          <p class="meta">Mandato atual: ${esc(local)}</p>
          <p class="meta">Partido: não disponível</p>
        </div>
      </div>
      ${blocoMensagem(msg)}
      ${blocoFonte(null)}
    </section>`;
  }
  const p = resultado.dados;
  const foto = p.fotoUrl && linkSeguro(p.fotoUrl) !== '#'
    ? `<img class="avatar" src="${esc(linkSeguro(p.fotoUrl))}" alt="" loading="lazy">`
    : `<div class="avatar" aria-hidden="true">${esc(iniciais(p.nome))}</div>`;
  return `<section class="cartao" aria-labelledby="t${id}" data-politico="${esc(p.id)}">
    <div class="pessoa">
      ${foto}
      <div>
        <p class="cargo">${esc(p.cargo || cargo)}</p>
        <h2 id="t${id}">${esc(p.nome)}</h2>
        <p class="meta">${esc(textoMandato(p))}</p>
        <p class="meta">Partido: ${p.partido ? esc(p.partido) : 'não disponível'}</p>
      </div>
    </div>
    ${p.resumo ? `<p class="resumo">${esc(p.resumo)}</p>` : ''}
    <div class="abas" role="tablist" aria-label="Propostas e ações de ${esc(p.nome)}">
      <button type="button" role="tab" class="aba" id="a${id}p" data-aba="propostas" aria-selected="true">Propostas</button>
      <button type="button" role="tab" class="aba" id="a${id}a" data-aba="acoes" aria-selected="false">O que já foi feito</button>
    </div>
    <div class="painel-aba" role="tabpanel" aria-live="polite"></div>
    ${blocoFonte(p.fonte)}
  </section>`;
}

function listaItens(resultado) {
  if (resultado.erro) return blocoMensagem(resultado.erro.message);
  if (!resultado.disponivel || !resultado.dados || !resultado.dados.length) {
    return blocoMensagem(resultado.mensagem || MSG_INDISPONIVEL);
  }
  const itens = resultado.dados.map((i) => {
    const data = fmtData(i.data);
    return `<li class="item">
      <p class="item-titulo">${esc(i.titulo)}</p>
      ${i.descricao ? `<p class="item-texto">${esc(i.descricao)}</p>` : ''}
      <p class="item-meta">${data ? `${esc(data)}. ` : ''}Fonte: <a href="${esc(linkSeguro(i.fonte && i.fonte.link))}" target="_blank" rel="noopener noreferrer">${esc(i.fonte ? i.fonte.nome : 'pendente')}</a></p>
    </li>`;
  }).join('');
  return `<ul class="itens">${itens}</ul>`;
}

async function carregarAba(botao) {
  const cartao = botao.closest('.cartao');
  const painel = cartao.querySelector('.painel-aba');
  const aba = botao.dataset.aba;
  cartao.querySelectorAll('[role=tab]').forEach((b) => b.setAttribute('aria-selected', String(b === botao)));
  painel.setAttribute('aria-labelledby', botao.id);
  painel.dataset.aba = aba;
  painel.innerHTML = '<p class="carregando">Carregando…</p>';
  const resultado = await seguro(`/api/politicos/${encodeURIComponent(cartao.dataset.politico)}/${aba}`);
  if (painel.isConnected && painel.dataset.aba === aba) painel.innerHTML = listaItens(resultado);
}

function ativarAbas() {
  conteudo.querySelectorAll('.cartao[data-politico]').forEach((cartao) => {
    cartao.querySelector('[role=tablist]').addEventListener('click', (ev) => {
      const botao = ev.target.closest('[role=tab]');
      if (botao) carregarAba(botao);
    });
    carregarAba(cartao.querySelector('[role=tab]'));
  });
}

/** Candidatos da eleição, em ordem alfabética, sem votos e sem ranking. */
function secaoCandidatos(titulo, resultado) {
  const id = ++seq;
  const intro = `Quem concorreu na eleição de ${CONFIG.ano}, em ordem alfabética. A lista não indica preferência nem ranking, e quem concorreu não é, necessariamente, quem exerce o cargo hoje.`;
  const cabecalho = `<h2 id="s${id}">${esc(titulo)}</h2><p class="intro">${esc(intro)}</p>`;
  if (resultado.erro || !resultado.disponivel || !resultado.dados || !resultado.dados.length) {
    const msg = resultado.erro ? resultado.erro.message : (resultado.mensagem || MSG_INDISPONIVEL);
    return `<section class="secao" aria-labelledby="s${id}">${cabecalho}${blocoMensagem(msg)}${blocoFonte(null)}</section>`;
  }
  const lista = [...resultado.dados].sort((a, b) => ordemAlfabetica.compare(a.nomeUrna || a.nome, b.nomeUrna || b.nome));
  const itens = lista.map((c) => {
    const principal = c.nomeUrna || c.nome;
    const completo = c.nomeUrna && c.nomeUrna !== c.nome ? `<p class="completo">${esc(c.nome)}</p>` : '';
    return `<li class="candidato">
      <span class="numero"><span class="sr">Número na urna: </span>${esc(c.numero)}</span>
      <div class="candidato-info">
        <p class="nome">${esc(principal)}</p>
        ${completo}
        <p class="partido">${c.partido ? `Partido: ${esc(c.partido)}` : 'Partido não informado'}</p>
      </div>
      <span class="resultado">${esc(TEXTO_SITUACAO[c.situacao] || '')}</span>
    </li>`;
  }).join('');
  return `<section class="secao" aria-labelledby="s${id}">
    ${cabecalho}
    <p class="contagem">${lista.length} ${lista.length === 1 ? 'candidatura' : 'candidaturas'}</p>
    <ul class="candidatos">${itens}</ul>
    ${blocoFonte(lista[0].fonte)}
  </section>`;
}

/** Demais representantes do estado (RF04). */
function secaoRepresentantes(resultado) {
  const id = ++seq;
  const titulo = `<h2 id="r${id}">Demais representantes</h2>`;
  if (resultado.erro || !resultado.disponivel || !resultado.dados || !resultado.dados.length) {
    const msg = resultado.erro ? resultado.erro.message : (resultado.mensagem || MSG_INDISPONIVEL);
    return `<section class="secao" aria-labelledby="r${id}">${titulo}${blocoMensagem(msg)}${blocoFonte(null)}</section>`;
  }
  const itens = resultado.dados.map((r) => `<li class="rep">
      <p class="cargo">${esc(r.cargo)}</p>
      <p class="nome">${esc(r.nome)}</p>
      <p class="partido">${r.partido ? `Partido: ${esc(r.partido)}` : 'Partido não informado'}</p>
    </li>`).join('');
  return `<section class="secao" aria-labelledby="r${id}">${titulo}<ul class="reps">${itens}</ul>${blocoFonte(resultado.dados[0].fonte)}</section>`;
}

// ---------- páginas ----------

function mostrar(html, titulo) {
  conteudo.innerHTML = html;
  document.title = titulo ? `${titulo} | Minha Vida Política` : 'Minha Vida Política';
  window.scrollTo(0, 0);
  conteudo.focus({ preventScroll: true });
}

function paginaErro(titulo, texto) {
  mostrar(`<div class="erro-pagina"><h1 class="titulo">${esc(titulo)}</h1><p>${esc(texto)}</p>
    <p><a class="voltar" href="#/">Voltar ao Brasil</a></p></div>`, titulo);
}

async function paginaBrasil() {
  const v = ++versao;
  marcarEstado(null);
  conteudo.innerHTML = '<p class="carregando">Carregando…</p>';
  const [presidente, candidatos] = await Promise.all([
    seguro('/api/presidente'),
    seguro(`/api/eleicoes/${CONFIG.ano}/presidente`),
  ]);
  if (v !== versao) return;
  mostrar(`
    <h1 class="titulo">Entenda quem decide por você, de forma simples.</h1>
    <p class="subtitulo">Informações oficiais sobre quem está no cargo agora e sobre quem concorreu nas eleições. Sem opinião e sem rótulos.</p>
    ${cartaoCargo('Presidente da República', 'Brasil', presidente)}
    ${secaoCandidatos(`Candidatos à Presidência em ${CONFIG.ano}`, candidatos)}
    <p class="aviso">Para ver o governo e os candidatos a governador, escolha o seu estado em “Seu estado”, no alto da página.</p>
  `, '');
  ativarAbas();
}

async function paginaEstado(sigla) {
  const v = ++versao;
  conteudo.innerHTML = '<p class="carregando">Carregando…</p>';
  const [dadosUf, representantes, candidatos] = await Promise.all([
    seguro(`/api/ufs/${encodeURIComponent(sigla)}`),
    seguro(`/api/ufs/${encodeURIComponent(sigla)}/representantes`),
    seguro(`/api/eleicoes/${CONFIG.ano}/ufs/${encodeURIComponent(sigla)}/governador`),
  ]);
  if (v !== versao) return;
  if (dadosUf.erro && dadosUf.erro.status === 404) {
    marcarEstado(null);
    paginaErro('Estado não encontrado', `Não encontramos o estado “${sigla}”. Escolha um estado na lista.`);
    return;
  }
  const conhecida = ufs.find((u) => u.sigla === sigla);
  const nome = (dadosUf && dadosUf.nome) || (conhecida && conhecida.nome) || sigla;
  marcarEstado(sigla, nome);
  const governador = dadosUf.erro ? dadosUf : dadosUf.governador;
  mostrar(`
    <p><a class="voltar" href="#/">Voltar ao Brasil</a></p>
    <h1 class="titulo">${esc(nome)}</h1>
    <p class="subtitulo">Quem governa o estado hoje e quem concorreu ao governo em ${CONFIG.ano}.</p>
    ${cartaoCargo('Governador', nome, governador)}
    ${secaoCandidatos(`Candidatos ao governo em ${CONFIG.ano}`, candidatos)}
    ${secaoRepresentantes(representantes)}
  `, nome);
  ativarAbas();
}

function rotear() {
  const m = location.hash.match(/^#\/uf\/([A-Za-z]{2})$/);
  if (m) paginaEstado(m[1].toUpperCase());
  else paginaBrasil();
}

// ---------- seletor de estado ----------

function marcarEstado(sigla, nome) {
  botaoEstados.textContent = sigla ? `${nome} (${sigla})` : 'Escolher…';
  grade.querySelectorAll('.uf').forEach((b) => {
    if (b.dataset.uf === sigla) b.setAttribute('aria-current', 'true');
    else b.removeAttribute('aria-current');
  });
}

function desenharGrade() {
  if (!ufs.length) {
    grade.innerHTML = '<p class="neutra">Não foi possível carregar a lista de estados.</p>';
    return;
  }
  grade.innerHTML = ufs.map((u) =>
    `<button type="button" class="uf" data-uf="${esc(u.sigla)}" title="${esc(u.nome)}" aria-label="${esc(u.nome)}">${esc(u.sigla)}</button>`).join('');
}

function iniciarSeletor() {
  botaoEstados.addEventListener('click', () => dialogo.showModal());
  document.getElementById('btn-fechar').addEventListener('click', () => dialogo.close());
  dialogo.addEventListener('click', (ev) => { if (ev.target === dialogo) dialogo.close(); });
  grade.addEventListener('click', (ev) => {
    const botao = ev.target.closest('button[data-uf]');
    if (!botao) return;
    dialogo.close();
    location.hash = `#/uf/${botao.dataset.uf}`;
  });
}

async function iniciar() {
  iniciarSeletor();
  window.addEventListener('hashchange', rotear);
  const lista = await seguro('/api/ufs');
  ufs = Array.isArray(lista) ? lista : [];
  desenharGrade();
  rotear();
}

iniciar();
