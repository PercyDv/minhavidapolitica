// Funções compartilhadas pelas telas do painel. JavaScript puro, sem bibliotecas.

export class ApiError extends Error {
  constructor(status, mensagem, campos) {
    super(mensagem);
    this.status = status;
    this.campos = campos || {};
  }
}

/** Chama a API do mesmo servidor, com o cookie de sessão. Em 401 volta para o login. */
export async function api(caminho, { method = 'GET', body } = {}) {
  const opcoes = { method, credentials: 'same-origin', headers: { Accept: 'application/json' } };
  if (body !== undefined) {
    opcoes.headers['Content-Type'] = 'application/json';
    opcoes.body = JSON.stringify(body);
  }
  let resposta;
  try {
    resposta = await fetch(caminho, opcoes);
  } catch {
    throw new ApiError(0, 'Não foi possível falar com o servidor. Verifique se a API está rodando.');
  }
  if (resposta.status === 401) {
    location.href = 'login.html';
    throw new ApiError(401, 'Sessão expirada. Entre novamente.');
  }
  if (!resposta.ok) {
    let mensagem = 'Algo deu errado (código ' + resposta.status + ').';
    let campos = {};
    try {
      const problema = await resposta.json();
      if (problema.detail) mensagem = problema.detail;
      if (problema.errors) campos = problema.errors;
    } catch { /* resposta sem corpo */ }
    if (resposta.status === 403) mensagem = 'Seu perfil não tem permissão para esta ação.';
    throw new ApiError(resposta.status, mensagem, campos);
  }
  if (resposta.status === 204) return null;
  return resposta.json();
}

export const $ = (seletor, raiz = document) => raiz.querySelector(seletor);
export const $$ = (seletor, raiz = document) => Array.from(raiz.querySelectorAll(seletor));

/** Escapa texto para uso seguro em innerHTML. */
export function esc(valor) {
  const mapa = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
  return String(valor ?? '').replace(/[&<>"']/g, (c) => mapa[c]);
}

export function fmtData(iso) {
  if (!iso) return '—';
  const [a, m, d] = String(iso).slice(0, 10).split('-');
  return `${d}/${m}/${a}`;
}

export function fmtDataHora(iso) {
  if (!iso) return '—';
  const [data, hora = ''] = String(iso).split('T');
  return fmtData(data) + (hora ? ' ' + hora.slice(0, 5) : '');
}

export function hojeISO() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

export function debounce(fn, ms = 250) {
  let t;
  return (...args) => { clearTimeout(t); t = setTimeout(() => fn(...args), ms); };
}

/** Aviso rápido no canto da tela. tipo: 'ok' | 'erro' */
export function toast(mensagem, tipo = 'ok') {
  let area = document.getElementById('toasts');
  if (!area) {
    area = document.createElement('div');
    area.id = 'toasts';
    area.setAttribute('aria-live', 'polite');
    document.body.appendChild(area);
  }
  const el = document.createElement('div');
  el.className = 'toast ' + tipo;
  el.textContent = mensagem;
  area.appendChild(el);
  setTimeout(() => el.remove(), tipo === 'erro' ? 7000 : 4000);
}

/** Confirmação em diálogo nativo. Resolve true se o usuário confirmar. */
export function confirmar(mensagem, { titulo = 'Confirmar ação', rotulo = 'Confirmar', perigo = false } = {}) {
  return new Promise((resolver) => {
    const dlg = document.createElement('dialog');
    dlg.innerHTML = `
      <form method="dialog">
        <div class="dlg-topo"><h2>${esc(titulo)}</h2><p>${esc(mensagem)}</p></div>
        <div class="dlg-rodape">
          <button class="btn" value="cancelar">Cancelar</button>
          <button class="btn ${perigo ? 'btn-perigo' : 'btn-primario'}" value="ok">${esc(rotulo)}</button>
        </div>
      </form>`;
    dlg.addEventListener('close', () => { resolver(dlg.returnValue === 'ok'); dlg.remove(); });
    document.body.appendChild(dlg);
    dlg.showModal();
  });
}

/** Preenche um <select>. itens: lista de objetos; rotulo pode ser função. */
export function preencherSelect(select, itens, { valor = 'id', rotulo = 'nome', primeira = null } = {}) {
  const texto = typeof rotulo === 'function' ? rotulo : (i) => i[rotulo];
  const opcoes = itens.map((i) => `<option value="${esc(i[valor])}">${esc(texto(i))}</option>`);
  if (primeira) opcoes.unshift(`<option value="${esc(primeira.valor)}">${esc(primeira.texto)}</option>`);
  select.innerHTML = opcoes.join('');
}

/** Mostra o erro retornado pela API dentro de um formulário (inclui erros por campo). */
export function mostrarErro(elemento, erro) {
  const detalhes = Object.values(erro.campos || {});
  elemento.textContent = detalhes.length ? `${erro.message} ${detalhes.join(' ')}` : erro.message;
  elemento.hidden = false;
}

export function limparErro(elemento) {
  elemento.textContent = '';
  elemento.hidden = true;
}

const ICONE_SELO = '<svg width="20" height="20" viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="10" fill="currentColor" opacity=".16"/><path d="M7 12.5l3.2 3.2L17 9" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"/></svg>';
const ICONE_RELOGIO = '<svg width="20" height="20" viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="10" fill="currentColor" opacity=".16"/><path d="M12 7v5.2l3.4 2" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/></svg>';
const ICONE_PAUSA = '<svg width="20" height="20" viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="10" fill="currentColor" opacity=".16"/><path d="M9.5 8.5v7M14.5 8.5v7" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/></svg>';

/** Selo de situação da fonte: o coração da regra RN05 (só dado de fonte aprovada aparece ao cidadão). */
export function seloFonte(fonte) {
  if (!fonte.ativa) return `<span class="selo selo-inativo">${ICONE_PAUSA}Fonte inativa</span>`;
  if (fonte.aprovada) {
    const quando = fonte.dataAprovacao ? ` em ${fmtData(fonte.dataAprovacao)}` : '';
    return `<span class="selo selo-ok">${ICONE_SELO}Aprovada pelo cliente${quando}</span>`;
  }
  return `<span class="selo selo-pendente">${ICONE_RELOGIO}Aguardando aprovação do cliente</span>`;
}

const MENU = [
  { grupo: null, itens: [{ chave: 'inicio', rotulo: 'Visão geral', href: 'index.html' }] },
  {
    grupo: 'Módulo ADS',
    itens: [
      { chave: 'politicos', rotulo: 'Políticos', href: 'politicos.html' },
      { chave: 'propostas-acoes', rotulo: 'Propostas e ações', href: 'propostas-acoes.html' },
      { chave: 'relatorios', rotulo: 'Relatórios', href: 'relatorios.html' },
      { chave: 'usuarios', rotulo: 'Usuários', href: 'usuarios.html', soAdmin: true },
    ],
  },
  {
    grupo: 'Módulo BES',
    itens: [
      { chave: 'fontes', rotulo: 'Fontes de dados', href: 'fontes.html' },
      { chave: 'importacao', rotulo: 'Importação de dados', href: 'importacao.html' },
    ],
  },
];

function montarMenu(chaveAtual, sessao) {
  const ehAdmin = sessao.perfil === 'ADMIN';
  const grupos = MENU.map((g) => {
    const itens = g.itens
      .filter((i) => !i.soAdmin || ehAdmin)
      .map((i) => `<a href="${i.href}"${i.chave === chaveAtual ? ' aria-current="page"' : ''}>${esc(i.rotulo)}</a>`)
      .join('');
    return `<div class="menu-grupo">${g.grupo ? `<h2>${esc(g.grupo)}</h2>` : ''}<div class="menu">${itens}</div></div>`;
  }).join('');
  document.getElementById('sidebar').innerHTML = `
    <a class="marca" href="index.html"><mark>Minha</mark>VidaPolítica<small>Painel da equipe mantenedora</small></a>
    <nav aria-label="Menu principal">${grupos}</nav>`;
}

/**
 * Prepara a página: confirma a sessão, monta menu e barra superior.
 * Retorna a sessão, ou null quando a tela é exclusiva de administradores e o perfil não permite.
 */
export async function iniciarPagina({ chave, titulo, somenteAdmin = false }) {
  const sessao = await api('/api/auth/me');
  document.title = `${titulo} | Painel MinhaVidaPolítica`;
  montarMenu(chave, sessao);
  const perfil = sessao.perfil === 'ADMIN' ? 'Administrador' : 'Editor';
  document.getElementById('topbar').innerHTML = `
    <span>Dados oficiais, com fonte e sem viés</span>
    <span>${esc(sessao.nome)} (${perfil}) &nbsp;<button class="btn-link" id="btn-sair" type="button">Sair</button></span>`;
  document.getElementById('btn-sair').addEventListener('click', async () => {
    await fetch('/api/auth/logout', { method: 'POST', credentials: 'same-origin' });
    location.href = 'login.html';
  });
  if (somenteAdmin && sessao.perfil !== 'ADMIN') {
    document.getElementById('content').innerHTML =
      '<div class="aviso aviso-erro"><strong>Tela exclusiva de administradores.</strong> Peça a um administrador para fazer esta alteração.</div>';
    return null;
  }
  return sessao;
}

/** Chip de resultado de importação. */
export function chipStatus(status) {
  const mapa = {
    SUCESSO: ['chip-ok', 'Concluída'],
    PARCIAL: ['chip-pendente', 'Parcial'],
    ERRO: ['chip-erro', 'Com erros'],
  };
  const [classe, texto] = mapa[status] || ['chip-neutro', status];
  return `<span class="chip ${classe}">${texto}</span>`;
}

/** Chip de situação de um registro para o cidadão (mandato atual + fonte aprovada). */
export function chipVisibilidade(visivel, motivoRetido = 'Retido: fonte sem aprovação') {
  return visivel
    ? '<span class="chip chip-ok">Visível ao cidadão</span>'
    : `<span class="chip chip-pendente">${esc(motivoRetido)}</span>`;
}
