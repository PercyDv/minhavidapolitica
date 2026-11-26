import { CONFIG } from './config.js';

export class ErroApi extends Error {
  constructor(status, mensagem) {
    super(mensagem);
    this.status = status;
  }
}

/** GET na API pública. Não envia cookies nem credenciais. */
export async function obter(caminho) {
  let resposta;
  try {
    resposta = await fetch(CONFIG.api + caminho, { headers: { Accept: 'application/json' } });
  } catch {
    throw new ErroApi(0, 'Não foi possível carregar os dados agora. Confira se a API está no ar e tente de novo.');
  }
  if (resposta.status === 404) throw new ErroApi(404, 'Não encontrado.');
  if (!resposta.ok) throw new ErroApi(resposta.status, `Não foi possível carregar os dados (código ${resposta.status}).`);
  return resposta.json();
}
