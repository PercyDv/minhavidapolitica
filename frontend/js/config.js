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
  // Servido pelo próprio backend (Railway ou localhost:8080): usa a API da mesma origem.
  // Servido à parte (Live Server/http-server nas portas 5500 ou 3000): usa a API local na porta 8080.
  apiBase: (location.port === "5500" || location.port === "3000")
    ? "http://localhost:8080/api"
    : "/api",
  // "api": nunca mostra o conteúdo de demonstração no lugar de dados reais
  modo: "api",
  // Ano da eleição cujos candidatos aparecem nas páginas (também dá para trocar pela URL: ?ano=2026)
  anoEleicao: 2026,
  versao: "1.0.0"
};
