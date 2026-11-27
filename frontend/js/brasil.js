/* Página Brasil: presidente em exercício, propostas, o que já foi feito (RF01, RF05, RF06) e candidatos à Presidência. */
(function () {
  "use strict";

  var ui = window.MVPUi;
  var prefs = window.MVPPreferencias.ler();
  var params = new URLSearchParams(window.location.search);

  // "O app abre direto nas informações deste estado" (Configurações > Estado padrão).
  // Só na primeira abertura da sessão, para o cidadão ainda conseguir voltar ao Brasil.
  try {
    if (prefs.estadoPadrao && !params.has("brasil") && !window.sessionStorage.getItem("mvp.abriuEstadoPadrao")) {
      window.sessionStorage.setItem("mvp.abriuEstadoPadrao", "1");
      window.location.replace("estado.html?uf=" + encodeURIComponent(prefs.estadoPadrao));
      return;
    }
    window.sessionStorage.setItem("mvp.abriuEstadoPadrao", "1");
  } catch (e) { /* sessionStorage indisponível */ }

  ui.iniciarMenuEstados({});
  ui.iniciarAbas(document.querySelector(".painel"));

  var elRep = document.getElementById("representante");
  var elPropostas = document.getElementById("lista-propostas");
  var elFeito = document.getElementById("lista-feito");

  // A seção de candidatos é opcional: se faltar código ou a chamada falhar, ela só não aparece.
  // Nada disso pode travar o resto da página.
  function carregarCandidatos(buscar, titulo) {
    try {
      if (typeof ui.preencherCandidatos !== "function") return;
      var promessa = buscar();
      if (!promessa || typeof promessa.then !== "function") return;
      promessa.then(function (resultado) {
        ui.preencherCandidatos(document.getElementById("candidatos"), resultado, titulo);
      }).catch(function () { /* a seção continua oculta */ });
    } catch (erro) { /* a seção continua oculta */ }
  }

  window.MVPApi.brasil().then(function (dados) {
    // Candidatos da eleição: depois do conteúdo principal, para saber se a API respondeu.
    carregarCandidatos(function () { return window.MVPApi.candidatosBrasil(); },
      "Candidatos à Presidência em " + window.MVPApi.anoEleicao);

    ui.preencherRepresentante(elRep, dados.presidente, dados.mensagem);
    ui.preencherPropostas(elPropostas, dados.propostas);
    ui.preencherFeitos(elFeito, dados.feitos);

    var rodape = document.getElementById("rodape-fontes");
    if (dados.fonte || dados.atualizacao) {
      document.getElementById("rodape-fonte").textContent = dados.fonte ? "Fonte: " + dados.fonte : "";
      document.getElementById("rodape-atualizacao").textContent = dados.atualizacao ? "Atualização: " + dados.atualizacao : "";
    } else {
      rodape.style.visibility = "hidden";
    }
  }).catch(function () {
    var msg = "Não foi possível carregar as informações agora. Tente novamente em instantes.";
    ui.preencherRepresentante(elRep, null, msg);
    ui.erroGeral(elPropostas, msg);
    ui.erroGeral(elFeito, msg);
    document.getElementById("rodape-fontes").style.visibility = "hidden";
  });
})();
