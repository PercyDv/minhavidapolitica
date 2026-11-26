// Funções de apoio: segurança de texto e links, e formatação de datas.

/** Escapa texto para uso seguro em innerHTML. */
export function esc(valor) {
  const mapa = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
  return String(valor ?? '').replace(/[&<>"']/g, (c) => mapa[c]);
}

/** Só aceita links http(s); qualquer outra coisa vira "#". */
export function linkSeguro(url) {
  try {
    const u = new URL(url);
    return u.protocol === 'http:' || u.protocol === 'https:' ? u.href : '#';
  } catch {
    return '#';
  }
}

export function fmtData(iso) {
  if (!iso) return '';
  const [a, m, d] = String(iso).slice(0, 10).split('-');
  return `${d}/${m}/${a}`;
}

const MESES = ['jan.', 'fev.', 'mar.', 'abr.', 'mai.', 'jun.', 'jul.', 'ago.', 'set.', 'out.', 'nov.', 'dez.'];

/** "2023-01-01" vira "jan. 2023". */
export function mesAno(iso) {
  if (!iso) return '';
  const [a, m] = String(iso).slice(0, 10).split('-');
  return `${MESES[Number(m) - 1]} ${a}`;
}

export function iniciais(nome) {
  const partes = String(nome || '').trim().split(/\s+/).filter((p) => /^\p{L}/u.test(p));
  if (!partes.length) return '–';
  const primeira = partes[0][0];
  const ultima = partes.length > 1 ? partes[partes.length - 1][0] : '';
  return (primeira + ultima).toUpperCase();
}

export const ordemAlfabetica = new Intl.Collator('pt-BR', { sensitivity: 'base' });
