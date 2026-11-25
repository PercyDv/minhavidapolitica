import {
  api, $, esc, fmtDataHora, iniciarPagina, toast, mostrarErro, limparErro,
} from './common.js';

const sessao = await iniciarPagina({ chave: 'usuarios', titulo: 'Usuários', somenteAdmin: true });

let usuarios = [];
let editandoId = null;

const dlg = $('#dlg');
const form = $('#form');
const erroForm = $('#erro-form');

function iniciar() {
  $('#btn-novo').addEventListener('click', () => abrirFormulario(null));
  $('#btn-cancelar').addEventListener('click', () => dlg.close());
  form.addEventListener('submit', salvar);
  $('#corpo').addEventListener('click', (ev) => {
    const botao = ev.target.closest('button[data-id]');
    if (!botao) return;
    const usuario = usuarios.find((u) => String(u.id) === botao.dataset.id);
    if (usuario) abrirFormulario(usuario);
  });
  carregar();
}

async function carregar() {
  try {
    usuarios = await api('/api/admin/usuarios');
  } catch (e) {
    toast(e.message, 'erro');
    return;
  }
  $('#corpo').innerHTML = usuarios.map((u) => `
    <tr>
      <td><strong>${esc(u.nome)}</strong>${u.username === sessao.username ? '<span class="sub">Este é o seu usuário</span>' : ''}</td>
      <td>${esc(u.username)}</td>
      <td>${u.perfil === 'ADMIN' ? 'Administrador' : 'Editor'}</td>
      <td>${u.ativo ? '<span class="chip chip-ok">Ativo</span>' : '<span class="chip chip-neutro">Desativado</span>'}</td>
      <td>${u.ultimoAcesso ? fmtDataHora(u.ultimoAcesso) : 'Nunca entrou'}</td>
      <td class="acoes"><button class="btn-link" type="button" data-id="${u.id}">Editar</button></td>
    </tr>`).join('');
}

function abrirFormulario(u) {
  editandoId = u ? u.id : null;
  form.reset();
  limparErro(erroForm);
  const e = form.elements;
  $('#dlg-titulo').textContent = u ? 'Editar usuário' : 'Novo usuário';
  $('#dlg-sub').textContent = u ? 'O login não pode ser alterado. Deixe a senha em branco para mantê-la.' : 'O novo usuário entra com o login e a senha definidos aqui.';
  $('#rotulo-senha').textContent = u ? 'Nova senha' : 'Senha *';
  $('#dica-senha').textContent = u ? 'Preencha só se quiser trocar a senha. Mínimo de 8 caracteres.' : 'Mínimo de 8 caracteres.';
  e.senha.required = !u;
  e.username.readOnly = !!u;
  e.nome.value = u?.nome ?? '';
  e.username.value = u?.username ?? '';
  e.perfil.value = u?.perfil ?? 'EDITOR';
  e.ativo.checked = u ? u.ativo : true;
  dlg.showModal();
  e.nome.focus();
}

async function salvar(ev) {
  ev.preventDefault();
  limparErro(erroForm);
  const e = form.elements;
  const corpo = {
    nome: e.nome.value.trim(),
    username: e.username.value.trim(),
    senha: e.senha.value || null,
    perfil: e.perfil.value,
    ativo: e.ativo.checked,
  };
  try {
    if (editandoId) await api(`/api/admin/usuarios/${editandoId}`, { method: 'PUT', body: corpo });
    else await api('/api/admin/usuarios', { method: 'POST', body: corpo });
    dlg.close();
    toast('Usuário salvo.');
    await carregar();
  } catch (erro) {
    mostrarErro(erroForm, erro);
  }
}

// A inicialização roda por último, depois de todas as declarações do módulo.
if (sessao) iniciar();
