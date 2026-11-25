import {
  api, $, esc, fmtDataHora, iniciarPagina, preencherSelect, toast, debounce, chipStatus,
} from './common.js';

const sessao = await iniciarPagina({ chave: 'importacao', titulo: 'Importação de dados' });

const MODELO = {
  politicos: [
    {
      nome: 'Nome do político (exemplo)',
      partido: 'SIGLA',
      cargo: 'GOVERNADOR',
      uf: 'BA',
      resumo: 'Texto factual curto, sem adjetivos.',
      inicioMandato: '2023-01-01',
      fimMandato: '2026-12-31',
      linkFonte: 'https://exemplo.gov.br/registro',
      propostas: [
        { titulo: 'Título da proposta (exemplo)', descricao: 'Descrição factual.', data: '2022-08-15', linkFonte: 'https://exemplo.gov.br/proposta' },
      ],
      acoes: [
        { titulo: 'Título da ação (exemplo)', descricao: 'Descrição factual.', data: '2024-03-10', linkFonte: 'https://exemplo.gov.br/acao' },
      ],
    },
  ],
};

let fontes = [];

async function iniciar() {
  try {
    const catalogo = await api('/api/admin/catalogos');
    fontes = catalogo.fontes;
    const codigos = catalogo.cargos.map((c) => c.codigo).join(', ');
    $('#resumo-arquivo').textContent = `Códigos de cargo aceitos: ${codigos}. Cargos estaduais exigem a sigla da UF.`;
  } catch (e) {
    toast(e.message, 'erro');
    return;
  }
  preencherSelect($('#sel-fonte'), fontes, {
    rotulo: (f) => f.nome + (f.ativa ? '' : ' (inativa)'),
    primeira: { valor: '', texto: fontes.length ? 'Selecione a fonte…' : 'Cadastre uma fonte antes' },
  });

  $('#sel-fonte').addEventListener('change', avisarFonte);
  $('#arquivo').addEventListener('change', lerArquivo);
  $('#conteudo-json').addEventListener('input', debounce(conferirConteudo, 300));
  $('#btn-modelo').addEventListener('click', baixarModelo);
  $('#form').addEventListener('submit', importar);
  await carregarHistorico();
}

function avisarFonte() {
  const f = fontes.find((x) => String(x.id) === $('#sel-fonte').value);
  $('#dica-fonte').textContent = !f ? ''
    : (f.aprovada && f.ativa ? 'Fonte aprovada: os dados importados já podem aparecer ao cidadão.'
      : 'Fonte sem aprovação do cliente: os dados serão importados, mas ficarão retidos.');
}

function lerArquivo(ev) {
  const arquivo = ev.target.files[0];
  if (!arquivo) return;
  const leitor = new FileReader();
  leitor.onload = () => {
    $('#conteudo-json').value = String(leitor.result);
    conferirConteudo();
  };
  leitor.readAsText(arquivo, 'UTF-8');
}

/** Lê o JSON e devolve a lista de políticos, ou lança Error com a explicação. */
function extrairPoliticos() {
  const texto = $('#conteudo-json').value.trim();
  if (!texto) throw new Error('Envie um arquivo ou cole o conteúdo JSON.');
  let dados;
  try {
    dados = JSON.parse(texto);
  } catch (e) {
    throw new Error('O conteúdo não é um JSON válido: ' + e.message);
  }
  const lista = Array.isArray(dados) ? dados : dados.politicos;
  if (!Array.isArray(lista) || lista.length === 0) {
    throw new Error('O arquivo precisa ter a lista "politicos" com ao menos um registro. Baixe o arquivo modelo para ver o formato.');
  }
  return lista;
}

function conferirConteudo() {
  const erro = $('#erro-form');
  erro.hidden = true;
  if (!$('#conteudo-json').value.trim()) return;
  try {
    const lista = extrairPoliticos();
    const propostas = lista.reduce((s, p) => s + (p.propostas?.length || 0), 0);
    const acoes = lista.reduce((s, p) => s + (p.acoes?.length || 0), 0);
    toast(`Arquivo lido: ${lista.length} político(s), ${propostas} proposta(s), ${acoes} ação(ões).`);
  } catch (e) {
    erro.textContent = e.message;
    erro.hidden = false;
  }
}

function baixarModelo() {
  const blob = new Blob([JSON.stringify(MODELO, null, 2)], { type: 'application/json' });
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = 'modelo-importacao.json';
  link.click();
  URL.revokeObjectURL(link.href);
}

async function importar(ev) {
  ev.preventDefault();
  const erro = $('#erro-form');
  erro.hidden = true;
  let lista;
  try {
    lista = extrairPoliticos();
  } catch (e) {
    erro.textContent = e.message;
    erro.hidden = false;
    return;
  }
  const botao = $('#btn-importar');
  botao.disabled = true;
  botao.textContent = 'Importando…';
  try {
    const r = await api('/api/admin/importacoes', {
      method: 'POST',
      body: { fonteId: Number($('#sel-fonte').value), politicos: lista },
    });
    mostrarResultado(r);
    await carregarHistorico();
  } catch (e) {
    erro.textContent = e.message + (Object.keys(e.campos || {}).length ? ' ' + Object.values(e.campos).join(' ') : '');
    erro.hidden = false;
  } finally {
    botao.disabled = false;
    botao.textContent = 'Importar dados';
  }
}

function mostrarResultado(r) {
  const classe = r.status === 'SUCESSO' ? 'aviso-ok' : (r.status === 'ERRO' ? 'aviso-erro' : '');
  const titulo = r.status === 'SUCESSO' ? 'Importação concluída.' : (r.status === 'PARCIAL' ? 'Importação parcial.' : 'Importação com erros.');
  const caixa = $('#resultado');
  caixa.className = 'aviso ' + classe;
  caixa.innerHTML = `<strong>${titulo}</strong> ${esc(r.mensagem || '')}`;
  caixa.hidden = false;
}

async function carregarHistorico() {
  const alvo = $('#historico');
  let logs;
  try {
    logs = await api('/api/admin/importacoes');
  } catch (e) {
    alvo.textContent = e.message;
    return;
  }
  if (!logs.length) {
    alvo.innerHTML = '<p class="vazio"><strong>Nenhuma importação ainda</strong>As cargas feitas aparecem aqui, com o resultado de cada uma.</p>';
    return;
  }
  alvo.innerHTML = `
    <div class="tabela-wrap"><table class="tabela">
      <thead><tr><th>Quando</th><th>Fonte</th><th>Resultado</th><th class="num">Criados</th><th class="num">Atualiz.</th><th class="num">Rejeit.</th></tr></thead>
      <tbody>${logs.map((l) => `
        <tr>
          <td>${fmtDataHora(l.iniciadoEm)}<span class="sub">${esc(l.executadoPor)}</span></td>
          <td>${esc(l.fonteNome)}${l.mensagem ? `<span class="sub">${esc(l.mensagem)}</span>` : ''}</td>
          <td>${chipStatus(l.status)}</td>
          <td class="num">${l.criados}</td><td class="num">${l.atualizados}</td><td class="num">${l.rejeitados}</td>
        </tr>`).join('')}</tbody>
    </table></div>`;
}

// A inicialização roda por último, depois de todas as declarações do módulo.
if (sessao) iniciar();
