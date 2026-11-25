import {
  api, $, esc, hojeISO, iniciarPagina, toast, confirmar, mostrarErro, limparErro, seloFonte,
} from './common.js';

const sessao = await iniciarPagina({ chave: 'fontes', titulo: 'Fontes de dados' });

const TIPOS = { API: 'API oficial', SITE_OFICIAL: 'Site oficial', ARQUIVO: 'Arquivo de dados' };
const ehAdmin = sessao?.perfil === 'ADMIN';

let fontes = [];
let editandoId = null;
let aprovandoId = null;

const dlg = $('#dlg');
const form = $('#form');
const erroForm = $('#erro-form');
const dlgAprov = $('#dlg-aprovacao');
const formAprov = $('#form-aprovacao');
const erroAprov = $('#erro-aprovacao');

function iniciar() {
  $('#btn-novo').addEventListener('click', () => abrirFormulario(null));
  $('#btn-cancelar').addEventListener('click', () => dlg.close());
  $('#btn-cancelar-aprov').addEventListener('click', () => dlgAprov.close());
  form.addEventListener('submit', salvar);
  formAprov.addEventListener('submit', registrarAprovacao);
  $('#corpo').addEventListener('click', aoClicar);
  carregar();
}

async function carregar() {
  try {
    fontes = await api('/api/admin/fontes');
  } catch (e) {
    toast(e.message, 'erro');
    return;
  }
  $('#tabela-wrap').hidden = fontes.length === 0;
  $('#vazio').hidden = fontes.length > 0;
  $('#corpo').innerHTML = fontes.map((f) => `
    <tr>
      <td>
        <strong>${esc(f.nome)}</strong>
        <span class="sub"><a href="${esc(f.url)}" target="_blank" rel="noopener noreferrer">${esc(f.url)}</a></span>
        <span class="sub">${esc(TIPOS[f.tipo] || f.tipo)}</span>
      </td>
      <td>${seloFonte(f)}${f.aprovada && f.aprovadaPor ? `<span class="sub">Registrado por ${esc(f.aprovadaPor)}</span>` : ''}${f.observacao ? `<span class="sub">${esc(f.observacao)}</span>` : ''}</td>
      <td>${f.periodicidade ? esc(f.periodicidade) : '<span class="chip chip-pendente">Não definida</span>'}</td>
      <td class="acoes">
        <button class="btn-link" type="button" data-acao="editar" data-id="${f.id}">Editar</button>
        ${ehAdmin && !f.aprovada ? `<button class="btn-link" type="button" data-acao="aprovar" data-id="${f.id}">Registrar aprovação</button>` : ''}
        ${ehAdmin && f.aprovada ? `<button class="btn-link perigo" type="button" data-acao="revogar" data-id="${f.id}">Revogar aprovação</button>` : ''}
        ${ehAdmin ? `<button class="btn-link perigo" type="button" data-acao="excluir" data-id="${f.id}">Excluir</button>` : ''}
      </td>
    </tr>`).join('');
}

function aoClicar(ev) {
  const botao = ev.target.closest('button[data-acao]');
  if (!botao) return;
  const fonte = fontes.find((f) => String(f.id) === botao.dataset.id);
  if (!fonte) return;
  const acao = botao.dataset.acao;
  if (acao === 'editar') abrirFormulario(fonte);
  if (acao === 'aprovar') abrirAprovacao(fonte);
  if (acao === 'revogar') revogar(fonte);
  if (acao === 'excluir') excluir(fonte);
}

function abrirFormulario(f) {
  editandoId = f ? f.id : null;
  form.reset();
  limparErro(erroForm);
  $('#dlg-titulo').textContent = f ? 'Editar fonte' : 'Nova fonte';
  const e = form.elements;
  e.nome.value = f?.nome ?? '';
  e.url.value = f?.url ?? '';
  e.tipo.value = f?.tipo ?? 'API';
  e.periodicidade.value = f?.periodicidade ?? '';
  e.observacao.value = f?.observacao ?? '';
  e.ativa.checked = f ? f.ativa : true;
  dlg.showModal();
  e.nome.focus();
}

async function salvar(ev) {
  ev.preventDefault();
  limparErro(erroForm);
  const e = form.elements;
  const corpo = {
    nome: e.nome.value.trim(),
    url: e.url.value.trim(),
    tipo: e.tipo.value,
    periodicidade: e.periodicidade.value.trim() || null,
    observacao: e.observacao.value.trim() || null,
    ativa: e.ativa.checked,
  };
  try {
    if (editandoId) await api(`/api/admin/fontes/${editandoId}`, { method: 'PUT', body: corpo });
    else await api('/api/admin/fontes', { method: 'POST', body: corpo });
    dlg.close();
    toast('Fonte salva.');
    await carregar();
  } catch (erro) {
    mostrarErro(erroForm, erro);
  }
}

function abrirAprovacao(f) {
  aprovandoId = f.id;
  formAprov.reset();
  limparErro(erroAprov);
  $('#aprov-sub').textContent = `Fonte: ${f.nome}. Depois de registrada, os dados vinculados a ela passam a aparecer ao cidadão.`;
  const e = formAprov.elements;
  e.aprovadaPor.value = '';
  e.dataAprovacao.value = hojeISO();
  e.observacao.value = f.observacao ?? '';
  dlgAprov.showModal();
  e.aprovadaPor.focus();
}

async function registrarAprovacao(ev) {
  ev.preventDefault();
  limparErro(erroAprov);
  const e = formAprov.elements;
  const corpo = {
    aprovadaPor: e.aprovadaPor.value.trim(),
    dataAprovacao: e.dataAprovacao.value || null,
    observacao: e.observacao.value.trim() || null,
  };
  try {
    await api(`/api/admin/fontes/${aprovandoId}/aprovar`, { method: 'POST', body: corpo });
    dlgAprov.close();
    toast('Aprovação registrada. Os dados da fonte já podem aparecer ao cidadão.');
    await carregar();
  } catch (erro) {
    mostrarErro(erroAprov, erro);
  }
}

async function revogar(f) {
  const ok = await confirmar(
    `Revogar a aprovação de “${f.nome}”? Os dados desta fonte deixam de aparecer ao cidadão imediatamente.`,
    { titulo: 'Revogar aprovação', rotulo: 'Revogar aprovação', perigo: true });
  if (!ok) return;
  try {
    await api(`/api/admin/fontes/${f.id}/revogar`, { method: 'POST' });
    toast('Aprovação revogada.');
    await carregar();
  } catch (erro) {
    toast(erro.message, 'erro');
  }
}

async function excluir(f) {
  const ok = await confirmar(`Excluir a fonte “${f.nome}”? Só é possível se nenhum registro estiver ligado a ela.`,
    { titulo: 'Excluir fonte', rotulo: 'Excluir fonte', perigo: true });
  if (!ok) return;
  try {
    await api(`/api/admin/fontes/${f.id}`, { method: 'DELETE' });
    toast('Fonte excluída.');
    await carregar();
  } catch (erro) {
    toast(erro.message, 'erro');
  }
}

// A inicialização roda por último, depois de todas as declarações do módulo.
if (sessao) iniciar();
