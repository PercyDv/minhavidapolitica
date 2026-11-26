/* Página Brasil: presidente em exercício, propostas e o que já foi feito (RF01, RF05, RF06). */
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

  window.MVPApi.brasil().then(function (dados) {
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

  /* Candidatos a presidente da eleição (extensão: /api/eleicoes) */
  var elCandidatos = document.getElementById("lista-candidatos");
  document.getElementById("titulo-candidatos").textContent =
    "Candidatos a Presidente · Eleição " + window.MVPApi.anoEleicao();
  window.MVPApi.candidatosPresidente().then(function (r) {
    ui.preencherCandidatos(elCandidatos, r);
  }).catch(function () {
    ui.erroGeral(elCandidatos, "Não foi possível carregar os candidatos agora. Tente novamente em instantes.");
  });
})();
