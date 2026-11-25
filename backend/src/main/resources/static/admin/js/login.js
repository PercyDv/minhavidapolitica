import { $ } from './common.js';

const form = $('#form');
const erro = $('#erro-form');

// Quem já está logado vai direto para o painel.
fetch('/api/auth/me', { credentials: 'same-origin' }).then((r) => { if (r.ok) location.href = 'index.html'; });

form.addEventListener('submit', async (ev) => {
  ev.preventDefault();
  erro.hidden = true;
  const botao = $('#btn-entrar');
  botao.disabled = true;
  try {
    const resposta = await fetch('/api/auth/login', {
      method: 'POST',
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams(new FormData(form)),
    });
    if (resposta.ok) {
      location.href = 'index.html';
      return;
    }
    erro.textContent = resposta.status === 401
      ? 'Login ou senha incorretos, ou usuário desativado.'
      : `Não foi possível entrar (código ${resposta.status}).`;
  } catch {
    erro.textContent = 'Não foi possível falar com o servidor. Verifique se a API está rodando.';
  }
  erro.hidden = false;
  botao.disabled = false;
});
