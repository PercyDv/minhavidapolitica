import { api, $, esc, fmtDataHora, iniciarPagina, chipStatus } from './common.js';

const sessao = await iniciarPagina({ chave: 'inicio', titulo: 'Visão geral' });

async function carregar() {
  let r;
  try {
    r = await api('/api/admin/resumo');
  } catch (e) {
    $('#destaque').textContent = e.message;
    return;
  }

  $('#destaque').innerHTML = `<span class="n">${r.ufsComGovernador}</span> de ${r.totalUfs} estados mostram o governador ao cidadão`;
  $('#destaque-sub').textContent = r.fontesAprovadas === 0
    ? 'Nenhuma fonte foi aprovada ainda. Até lá, o cidadão vê apenas a mensagem “Informação não disponível nas fontes oficiais consultadas”.'
    : 'A conta considera só o que passa pelas regras do sistema: mandato atual e fonte aprovada pelo cliente.';

  const numeros = [
    ['Políticos cadastrados', r.politicos],
    ['Propostas', r.propostas],
    ['Ações concretizadas', r.acoes],
    ['Fontes aprovadas', r.fontesAprovadas],
    ['Fontes sem aprovação', r.fontesPendentes],
  ];
  $('#numeros').innerHTML = numeros.map(([rotulo, valor]) => `<div><dt>${esc(rotulo)}</dt><dd>${valor}</dd></div>`).join('');

  const aviso = $('#aviso-fontes');
  if (r.fontesPendentes > 0) {
    const plural = r.fontesPendentes > 1;
    aviso.innerHTML = `Há <strong>${r.fontesPendentes}</strong> fonte${plural ? 's' : ''} sem aprovação ou inativa${plural ? 's' : ''}. Os dados ligados a ${plural ? 'elas' : 'ela'} não aparecem ao cidadão. <a href="fontes.html">Ver fontes</a>`;
    aviso.hidden = false;
  }

  const lista = $('#lista-importacoes');
  lista.innerHTML = r.ultimasImportacoes.length
    ? r.ultimasImportacoes.map((l) => `
        <li>
          <div><strong>${esc(l.fonteNome)}</strong>
            <span class="sub">${fmtDataHora(l.iniciadoEm)}, por ${esc(l.executadoPor)}</span>
            <span class="sub">Criados: ${l.criados}. Atualizados: ${l.atualizados}. Rejeitados: ${l.rejeitados}.</span></div>
          ${chipStatus(l.status)}
        </li>`).join('')
    : '<li><span class="sub">Nenhuma importação feita ainda.</span></li>';
}

// A inicialização roda por último, depois de todas as declarações do módulo.
if (sessao) carregar();
