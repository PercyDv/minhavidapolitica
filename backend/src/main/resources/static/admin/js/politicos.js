import {
  api, $, esc, fmtDataHora, hojeISO, iniciarPagina, preencherSelect, toast, confirmar,
  mostrarErro, limparErro, debounce, chipVisibilidade,
} from './common.js';

const sessao = await iniciarPagina({ chave: 'politicos', titulo: 'Políticos' });

let catalogo = { ufs: [], cargos: [], fontes: [] };
let lista = [];
let editandoId = null;

const dlg = $('#dlg');
const form = $('#form');
const erroForm = $('#erro-form');

async function iniciar() {
  try {
    catalogo = await api('/api/admin/catalogos');
  } catch (e) {
    toast(e.message, 'erro');
    return;
  }
  const rotuloUf = (u) => `${u.sigla} · ${u.nome}`;
  preencherSelect($('#f-uf'), catalogo.ufs, { rotulo: rotuloUf, primeira: { valor: 0, texto: 'Todos os estados' } });
  preencherSelect($('#f-cargo'), catalogo.cargos, { primeira: { valor: 0, texto: 'Todos os cargos' } });
  preencherSelect(form.elements.cargoId, catalogo.cargos, { primeira: { valor: '', texto: 'Selecione…' } });
  preencherSelect(form.elements.ufId, catalogo.ufs, { rotulo: rotuloUf, primeira: { valor: '', texto: 'Não se aplica' } });
  preencherSelect(form.elements.fonteId, catalogo.fontes, {
    rotulo: (f) => f.nome + (f.aprovada && f.ativa ? '' : ' (sem aprovação do cliente)'),
    primeira: { valor: '', texto: 'Selecione…' },
  });

  $('#f-nome').addEventListener('input', debounce(carregar));
  $('#f-uf').addEventListener('change', carregar);
  $('#f-cargo').addEventListener('change', carregar);
  $('#btn-novo').addEventListener('click', () => abrirFormulario(null));
  $('#btn-cancelar').addEventListener('click', () => dlg.close());
  form.elements.cargoId.addEventListener('change', ajustarUf);
  form.addEventListener('submit', salvar);
  $('#corpo').addEventListener('click', aoClicarNaTabela);

  await carregar();
}

async function carregar() {
  const consulta = new URLSearchParams({
    nome: $('#f-nome').value.trim(),
    ufId: $('#f-uf').value,
    cargoId: $('#f-cargo').value,
  });
  try {
    lista = await api('/api/admin/politicos?' + consulta);
  } catch (e) {
    toast(e.message, 'erro');
    return;
  }
  desenhar();
}

function filtrosAtivos() {
  return $('#f-nome').value.trim() !== '' || $('#f-uf').value !== '0' || $('#f-cargo').value !== '0';
}

function desenhar() {
  const vazio = $('#vazio');
  $('#tabela-wrap').hidden = lista.length === 0;
  vazio.hidden = lista.length > 0;
  if (lista.length === 0) {
    vazio.innerHTML = filtrosAtivos()
      ? '<strong>Nenhum político encontrado</strong>Ajuste ou limpe os filtros para ver mais resultados.'
      : '<strong>Nenhum político cadastrado</strong>Use “Novo político” para cadastrar o primeiro. Antes, confira se a fonte já está cadastrada em Fontes de dados.';
    return;
  }
  const hoje = hojeISO();
  $('#corpo').innerHTML = lista.map((p) => {
    const iniciou = !p.inicioMandato || p.inicioMandato <= hoje;
    const visivel = p.emExercicio && p.fonteAprovada && iniciou && (!p.fimMandato || p.fimMandato >= hoje);
    const motivo = !p.emExercicio ? 'Fora de exercício'
      : (!p.fonteAprovada ? 'Retido: fonte sem aprovação'
        : (!iniciou ? 'Mandato ainda não começou' : 'Mandato encerrado'));
    return `
      <tr>
        <td><strong>${esc(p.nome)}</strong></td>
        <td>${esc(p.cargoNome)}</td>
        <td>${p.ufSigla ? esc(p.ufSigla) : 'Nacional'}</td>
        <td>${p.partido ? esc(p.partido) : '—'}</td>
        <td>${esc(p.fonteNome)}<span class="sub">${chipVisibilidade(visivel, motivo)}</span></td>
        <td>${fmtDataHora(p.atualizadoEm)}</td>
        <td class="acoes">
          <a class="btn-link" href="propostas-acoes.html?politicoId=${p.id}">Propostas e ações</a>
          <button class="btn-link" type="button" data-acao="editar" data-id="${p.id}">Editar</button>
          <button class="btn-link perigo" type="button" data-acao="excluir" data-id="${p.id}">Excluir</button>
        </td>
      </tr>`;
  }).join('');
}

function aoClicarNaTabela(ev) {
  const botao = ev.target.closest('button[data-acao]');
  if (!botao) return;
  const politico = lista.find((p) => String(p.id) === botao.dataset.id);
  if (!politico) return;
  if (botao.dataset.acao === 'editar') abrirFormulario(politico);
  if (botao.dataset.acao === 'excluir') excluir(politico);
}

function ajustarUf() {
  const cargo = catalogo.cargos.find((c) => String(c.id) === form.elements.cargoId.value);
  const uf = form.elements.ufId;
  const dica = $('#dica-uf');
  if (!cargo) {
    uf.disabled = false;
    uf.required = false;
    dica.textContent = '';
  } else if (cargo.esfera === 'NACIONAL') {
    uf.value = '';
    uf.disabled = true;
    uf.required = false;
    dica.textContent = 'Cargo nacional não tem estado.';
  } else {
    uf.disabled = false;
    uf.required = true;
    dica.textContent = 'Obrigatório para cargo estadual.';
  }
}

function abrirFormulario(p) {
  editandoId = p ? p.id : null;
  form.reset();
  limparErro(erroForm);
  $('#dlg-titulo').textContent = p ? 'Editar político' : 'Novo político';
  const e = form.elements;
  e.nome.value = p?.nome ?? '';
  e.cargoId.value = p ? String(p.cargoId) : '';
  e.ufId.value = p?.ufId ? String(p.ufId) : '';
  e.partido.value = p?.partido ?? '';
  e.fotoUrl.value = p?.fotoUrl ?? '';
  e.resumo.value = p?.resumo ?? '';
  e.inicioMandato.value = p?.inicioMandato ?? '';
  e.fimMandato.value = p?.fimMandato ?? '';
  e.fonteId.value = p ? String(p.fonteId) : '';
  e.linkFonte.value = p?.linkFonte ?? '';
  e.emExercicio.checked = p ? p.emExercicio : true;
  ajustarUf();
  dlg.showModal();
  e.nome.focus();
}

async function salvar(ev) {
  ev.preventDefault();
  limparErro(erroForm);
  const e = form.elements;
  const corpo = {
    nome: e.nome.value.trim(),
    partido: e.partido.value.trim() || null,
    fotoUrl: e.fotoUrl.value.trim() || null,
    resumo: e.resumo.value.trim() || null,
    cargoId: Number(e.cargoId.value),
    ufId: e.ufId.value ? Number(e.ufId.value) : null,
    inicioMandato: e.inicioMandato.value || null,
    fimMandato: e.fimMandato.value || null,
    emExercicio: e.emExercicio.checked,
    fonteId: Number(e.fonteId.value),
    linkFonte: e.linkFonte.value.trim() || null,
  };
  try {
    if (editandoId) await api(`/api/admin/politicos/${editandoId}`, { method: 'PUT', body: corpo });
    else await api('/api/admin/politicos', { method: 'POST', body: corpo });
    dlg.close();
    toast('Político salvo.');
    await carregar();
  } catch (erro) {
    mostrarErro(erroForm, erro);
  }
}

async function excluir(p) {
  const ok = await confirmar(
    `Excluir “${p.nome}”? As propostas e ações deste político também serão excluídas. Isso não pode ser desfeito.`,
    { titulo: 'Excluir político', rotulo: 'Excluir político', perigo: true });
  if (!ok) return;
  try {
    await api(`/api/admin/politicos/${p.id}`, { method: 'DELETE' });
    toast('Político excluído.');
    await carregar();
  } catch (erro) {
    toast(erro.message, 'erro');
  }
}

// A inicialização roda por último, depois de todas as declarações do módulo.
if (sessao) iniciar();
