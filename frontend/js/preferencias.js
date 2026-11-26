/*
 * Preferências do cidadão (tela Configurações). Ficam só neste aparelho (localStorage),
 * sem cadastro. Este arquivo é carregado no <head> para aplicar tema e tamanho do texto
 * antes da página aparecer.
 */
(function () {
  "use strict";

  var CHAVE = "mvp.preferencias";
  var PADRAO = {
    estadoPadrao: "",
    tema: "claro",          // claro | escuro | automatico
    tamanho: "normal",      // pequeno | normal | grande
    contraste: false,
    notificacoes: { avisos: true, propostas: true, acoes: true }
  };

  function copiar(obj) { return JSON.parse(JSON.stringify(obj)); }

  function ler() {
    var prefs = copiar(PADRAO);
    try {
      var salvo = JSON.parse(window.localStorage.getItem(CHAVE) || "null");
      if (salvo && typeof salvo === "object") {
        Object.keys(PADRAO).forEach(function (k) {
          if (salvo[k] !== undefined) prefs[k] = salvo[k];
        });
        prefs.notificacoes = Object.assign(copiar(PADRAO.notificacoes), salvo.notificacoes || {});
      }
    } catch (e) { /* armazenamento indisponível: usa o padrão */ }
    return prefs;
  }

  function salvar(prefs) {
    try { window.localStorage.setItem(CHAVE, JSON.stringify(prefs)); } catch (e) { /* ignora */ }
  }

  function limpar() {
    try {
      window.localStorage.removeItem(CHAVE);
      window.sessionStorage.removeItem("mvp.abriuEstadoPadrao");
    } catch (e) { /* ignora */ }
  }

  var midiaEscura = window.matchMedia ? window.matchMedia("(prefers-color-scheme: dark)") : null;

  function aplicar(prefs) {
    var raiz = document.documentElement;
    var escuro = prefs.tema === "escuro" || (prefs.tema === "automatico" && midiaEscura && midiaEscura.matches);
    raiz.setAttribute("data-tema", escuro ? "escuro" : "claro");
    raiz.setAttribute("data-tamanho", prefs.tamanho || "normal");
    if (prefs.contraste) raiz.setAttribute("data-contraste", "alto");
    else raiz.removeAttribute("data-contraste");
  }

  if (midiaEscura && midiaEscura.addEventListener) {
    midiaEscura.addEventListener("change", function () { aplicar(ler()); });
  }

  window.MVPPreferencias = { ler: ler, salvar: salvar, limpar: limpar, aplicar: aplicar, padrao: copiar(PADRAO) };
  aplicar(ler());
})();
