/*
 * Configuração do front-end público.
 *
 * apiBase: endereço da API do back-end (Spring Boot). O back-end já libera CORS para
 *          http://localhost:5500 e http://127.0.0.1:5500 (Live Server do VS Code).
 *
 * modo:
 *   "auto" -> tenta a API; se ela não estiver no ar, mostra o conteúdo do protótipo do Figma.
 *   "api"  -> usa só a API (recomendado em produção).
 *   "demo" -> usa só o conteúdo do protótipo do Figma (js/dados-demo.js).
 *
 * Também dá para trocar pela URL: ?modo=demo ou ?modo=api
 */
window.MVP_CONFIG = {
  apiBase: "http://localhost:8080/api",
  modo: "auto",
  versao: "1.0.0"
};
