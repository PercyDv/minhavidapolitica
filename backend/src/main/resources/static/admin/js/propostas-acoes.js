import {
  api, $, esc, fmtData, fmtDataHora, iniciarPagina, preencherSelect, toast, confirmar,
  mostrarErro, limparErro, chipVisibilidade,
} from './common.js';

const sessao = await iniciarPagina({ chave: 'propostas-acoes', titulo: 'Propostas e ações' });

// A chave da aba é também o trecho do caminho na API: /api/admin/politicos/{id}/{aba}
const ABAS = {
  propostas: {
    novo: 'Nova proposta', editar: 'Editar proposta', salvar: 'Salvar proposta',
    data: 'Data de referência', dataObrigatoria: false,
    dica: 'Propostas do mandato atual. A data é a da proposta na fonte, quando houver.',
    vazio: 'Nenhuma proposta registrada para este político.',
    sub: 'Registre somente propostas do mandato atual.',
  },
  acoes: {
    novo: 'Nova ação', editar: 'Editar ação', salvar: 'Salvar ação',
    data: 'Data da ação', dataObrigatoria: true,
    dica: 'Ações já realizadas no mandato atual. Toda ação precisa de fonte e data.',
    vazio: 'Nenhuma ação concretizada registrada para este político.',
    sub: 'Informe o que foi realizado, a data e a fonte que comprova.',
  },
};

let politicoId = null;
let aba = 'propostas';
let itens = [];
let editandoId = null;

const dlg = $('#dlg');
const form = $('#form');
const erroForm = $('#erro-form');

async function iniciar() {
  let politicos;
  let catalogo;
  try {
    [politicos, catalogo] = await Promise.all([api('/api/admin/politicos'), api('/api/admin/catalogos')]);
  } catch (e) {
    toast(e.message, 'erro');
    return;
  }
  preencherSelect($('#sel-politico'), politicos, {
    rotulo: (p) => `${p.nome}, ${p.cargoNome}${p.ufSigla ? ' (' + p.ufSigla + ')' : ''}`,
    primeira: { valor: '', texto: politicos.length ? 'Selecione um político…' : 'Nenhum político cadastrado' },
  });
  preencherSelect(form.elements.fonteId, catalogo.fontes, {
    rotulo: (f) => f.nome + (f.aprovada && f.ativa ? '' : ' (sem aprovação do cliente)'),
    primeira: { valor: '', texto: 'Selecione…' },
  });

  $('#sel-politico').addEventListener('change', (ev) => selecionar(ev.target.value));
  $('#aba-propostas').addEventListener('click', () => trocarAba('propostas'));
  $('#aba-acoes').addEventListener('click', () => trocarAba('acoes'));
  $('#btn-novo').addEventListener('click', () => abrirFormulario(null));
  $('#btn-cancelar').addEventListener('click', () => dlg.close());
  form.addEventListener('submit', salvar);
  $('#corpo').addEventListener('click', aoClicarNaTabela);

  const inicial = new URLSearchParams(location.search).get('politicoId');
  if (inicial && politicos.some((p) => String(p.id) === inicial)) {
    $('#sel-politico').value = inicial;
    selecionar(inicial);
  }
}

function selecionar(id) {
  politicoId = id ? Number(id) : null;
  $('#area').hidden = !politicoId;
  $('#sem-selecao').hidden = !!politicoId;
  if (politicoId) {
    history.replaceState(null, '', '?politicoId=' + politicoId);
    carregar();
  }
}

function trocarAba(nova) {
  aba = nova;
  $('#aba-propostas').setAttribute('aria-selected', String(aba === 'propostas'));
  $('#aba-acoes').setAttribute('aria-selected', String(aba === 'acoes'));
  carregar();
}

async function carregar() {
  const cfg = ABAS[aba];
  $('#dica').textContent = cfg.dica;
  $('#btn-novo').textContent = cfg.novo;
  $('#th-data').textContent = cfg.data;
  try {
    itens = await api(`/api/admin/politicos/${politicoId}/${aba}`);
  } catch (e) {
    toast(e.message, 'erro');
    return;
  }
  $('#tabela-wrap').hidden = itens.length === 0;
  $('#vazio').hidden = itens.length > 0;
  $('#vazio').innerHTML = `<strong>${esc(cfg.vazio)}</strong>Use “${esc(cfg.novo)}” para cadastrar.`;
  $('#corpo').innerHTML = itens.map((i) => `
    <tr>
      <td><strong>${esc(i.titulo)}</strong>${i.descricao ? `<span class="sub">${esc(i.descricao)}</span>` : ''}</td>
      <td>${fmtData(i.data)}</td>
      <td>${esc(i.fonteNome)}<span class="sub">${chipVisibilidade(i.fonteAprovada)}</span></td>
      <td>${fmtDataHora(i.atualizadoEm)}</td>
      <td class="acoes">
        <button class="btn-link" type="button" data-acao="editar" data-id="${i.id}">Editar</button>
        <button class="btn-link perigo" type="button" data-acao="excluir" data-id="${i.id}">Excluir</button>
      </td>
    </tr>`).join('');
}

function aoClicarNaTabela(ev) {
  const botao = ev.target.closest('button[data-acao]');
  if (!botao) return;
  const item = itens.find((i) => String(i.id) === botao.dataset.id);
  if (!item) return;
  if (botao.dataset.acao === 'editar') abrirFormulario(item);
  if (botao.dataset.acao === 'excluir') excluir(item);
}

function abrirFormulario(item) {
  const cfg = ABAS[aba];
  editandoId = item ? item.id : null;
  form.reset();
  limparErro(erroForm);
  $('#dlg-titulo').textContent = item ? cfg.editar : cfg.novo;
  $('#dlg-sub').textContent = cfg.sub;
  $('#rotulo-data').textContent = cfg.data + (cfg.dataObrigatoria ? ' *' : '');
  const e = form.elements;
  e.data.required = cfg.dataObrigatoria;
  e.titulo.value = item?.titulo ?? '';
  e.descricao.value = item?.descricao ?? '';
  e.data.value = item?.data ?? '';
  e.fonteId.value = item ? String(item.fonteId) : '';
  e.linkFonte.value = item?.linkFonte ?? '';
  dlg.querySelector('button[type=submit]').textContent = cfg.salvar;
  dlg.showModal();
  e.titulo.focus();
}

async function salvar(ev) {
  ev.preventDefault();
  limparErro(erroForm);
  const e = form.elements;
  const corpo = {
    titulo: e.titulo.value.trim(),
    descricao: e.descricao.value.trim() || null,
    data: e.data.value || null,
    fonteId: Number(e.fonteId.value),
    linkFonte: e.linkFonte.value.trim() || null,
  };
  try {
    if (editandoId) await api(`/api/admin/${aba}/${editandoId}`, { method: 'PUT', body: corpo });
    else await api(`/api/admin/politicos/${politicoId}/${aba}`, { method: 'POST', body: corpo });
    dlg.close();
    toast('Registro salvo.');
    await carregar();
  } catch (erro) {
    mostrarErro(erroForm, erro);
  }
}

async function excluir(item) {
  const ok = await confirmar(`Excluir “${item.titulo}”? Isso não pode ser desfeito.`,
    { titulo: 'Excluir registro', rotulo: 'Excluir', perigo: true });
  if (!ok) return;
  try {
    await api(`/api/admin/${aba}/${item.id}`, { method: 'DELETE' });
    toast('Registro excluído.');
    await carregar();
  } catch (erro) {
    toast(erro.message, 'erro');
  }
}

// A inicialização roda por último, depois de todas as declarações do módulo.
if (sessao) iniciar();
