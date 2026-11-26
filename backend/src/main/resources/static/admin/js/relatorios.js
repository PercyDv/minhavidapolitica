import { api, $, $$, esc, iniciarPagina, toast } from './common.js';

const sessao = await iniciarPagina({ chave: 'relatorios', titulo: 'Relatórios' });

const CLASSE = { COMPLETO: 'uf-completo', PARCIAL: 'uf-parcial', SEM_DADOS: 'uf-sem' };
const TEXTO = { COMPLETO: 'Completo', PARCIAL: 'Parcial', SEM_DADOS: 'Sem dados visíveis' };

let cobertura = [];

async function carregar() {
  let fontes;
  try {
    [cobertura, fontes] = await Promise.all([
      api('/api/admin/relatorios/cobertura'),
      api('/api/admin/relatorios/fontes'),
    ]);
  } catch (e) {
    toast(e.message, 'erro');
    return;
  }
  desenharCobertura();
  desenharFontes(fontes);
}

function desenharCobertura() {
  const contar = (s) => cobertura.filter((c) => c.situacao === s).length;
  $('#resumo-cobertura').textContent =
    `${contar('COMPLETO')} completos, ${contar('PARCIAL')} parciais e ${contar('SEM_DADOS')} sem dados visíveis, num total de ${cobertura.length} estados. Conta só o que o cidadão realmente vê.`;
  $('#grade').innerHTML = cobertura.map((c) =>
    `<button type="button" class="uf ${CLASSE[c.situacao]}" data-sigla="${esc(c.sigla)}" aria-pressed="false" title="${esc(c.nome)}: ${TEXTO[c.situacao]}">${esc(c.sigla)}</button>`).join('');
  $('#grade').addEventListener('click', (ev) => {
    const botao = ev.target.closest('button.uf');
    if (botao) mostrarDetalhe(botao.dataset.sigla);
  });
}

function mostrarDetalhe(sigla) {
  $$('.uf').forEach((b) => b.setAttribute('aria-pressed', String(b.dataset.sigla === sigla)));
  const c = cobertura.find((x) => x.sigla === sigla);
  const chip = c.situacao === 'COMPLETO' ? 'chip-ok' : (c.situacao === 'PARCIAL' ? 'chip-pendente' : 'chip-neutro');
  const detalhe = $('#detalhe');
  detalhe.innerHTML = `
    <h3>${esc(c.nome)} (${esc(c.sigla)})</h3>
    <dl>
      <dt>Situação</dt><dd><span class="chip ${chip}">${TEXTO[c.situacao]}</span></dd>
      <dt>Governador</dt><dd>${c.governador ? esc(c.governador) : 'Nenhum governador visível ao cidadão'}</dd>
      <dt>Outros representantes</dt><dd>${c.representantes}</dd>
      <dt>Políticos com propostas</dt><dd>${c.comPropostas}</dd>
      <dt>Políticos com ações</dt><dd>${c.comAcoes}</dd>
    </dl>`;
  detalhe.hidden = false;
}

function desenharFontes(fontes) {
  const retido = (f) => !(f.aprovada && f.ativa) && (f.politicos + f.propostas + f.acoes + f.candidaturas) > 0;
  const totalRetido = fontes.filter(retido).reduce((s, f) => s + f.politicos + f.propostas + f.acoes + f.candidaturas, 0);
  $('#resumo-fontes').textContent = totalRetido > 0
    ? `${totalRetido} registro${totalRetido > 1 ? 's estão retidos' : ' está retido'} por falta de aprovação da fonte e não aparecem ao cidadão.`
    : 'Nenhum registro está retido por falta de aprovação.';
  $('#corpo-fontes').innerHTML = fontes.length
    ? fontes.map((f) => {
      const chip = !f.ativa ? '<span class="chip chip-neutro">Inativa</span>'
        : (f.aprovada ? '<span class="chip chip-ok">Aprovada</span>' : '<span class="chip chip-pendente">Sem aprovação</span>');
      return `<tr>
        <td><strong>${esc(f.fonteNome)}</strong>${retido(f) ? '<span class="sub">Registros retidos até a aprovação</span>' : ''}</td>
        <td>${chip}</td>
        <td class="num">${f.politicos}</td><td class="num">${f.propostas}</td><td class="num">${f.acoes}</td><td class="num">${f.candidaturas}</td>
      </tr>`;
    }).join('')
    : '<tr><td colspan="6"><span class="sub">Nenhuma fonte cadastrada.</span></td></tr>';
}

// A inicialização roda por último, depois de todas as declarações do módulo.
if (sessao) carregar();
