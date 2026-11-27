/* Página estadual: governador em exercício, propostas, o que já foi feito, candidatos a governador e demais representantes (RF03 a RF06). */
(function () {
  "use strict";

  var ui = window.MVPUi;
  var sigla = (new URLSearchParams(window.location.search).get("uf") || "").trim().toUpperCase();
  var ufLocal = window.MVPDemo.ufs.filter(function (u) { return u.sigla === sigla; })[0];

  var elTitulo = document.getElementById("titulo-pagina");
  var elRep = document.getElementById("representante");
  var elPropostas = document.getElementById("lista-propostas");
  var elFeito = document.getElementById("lista-feito");
  var elRepresentantes = document.getElementById("lista-representantes");

  ui.iniciarMenuEstados({ atual: sigla, nomeAtual: ufLocal ? ufLocal.nome : "" });
  ui.iniciarAbas(document.querySelector(".painel"));

  function semEstado(mensagem) {
    elTitulo.textContent = "Estado não encontrado";
    document.title = "Minha VidaPolitica · Estado não encontrado";
    ui.preencherRepresentante(elRep, null, mensagem);
    ui.erroGeral(elPropostas, mensagem);
    ui.erroGeral(elFeito, mensagem);
    ui.erroGeral(elRepresentantes, mensagem);
  }

  if (!/^[A-Z]{2}$/.test(sigla)) {
    semEstado("Escolha um estado no seletor acima.");
    return;
  }
  if (ufLocal) {
    elTitulo.textContent = ufLocal.nome;
    document.title = "Minha VidaPolitica · " + ufLocal.nome;
  }

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

  // Candidatos ao governo do estado escolhido: só os desta UF (RN06).
  carregarCandidatos(function () { return window.MVPApi.candidatosEstado(sigla); },
    "Candidatos ao governo em " + window.MVPApi.anoEleicao);

  window.MVPApi.estado(sigla).then(function (dados) {
    if (!dados) { semEstado("UF não encontrada: " + sigla); return; }
    elTitulo.textContent = dados.nome;
    document.title = "Minha VidaPolitica · " + dados.nome;
    document.getElementById("resumo-pagina").textContent = dados.resumo;

    ui.preencherRepresentante(elRep, dados.governador, dados.mensagem);
    ui.preencherPropostas(elPropostas, dados.propostas);
    ui.preencherFeitos(elFeito, dados.feitos);

    var reps = dados.representantes;
    var html = reps.itens && reps.itens.length ? reps.itens.map(ui.cartaoRepresentante).join("") : ui.avisoNeutro(reps.mensagem);
    if (dados.resumoRepresentantes) html += ui.cartaoResumoRepresentantes(dados.resumoRepresentantes);
    elRepresentantes.innerHTML = html;
  }).catch(function (erro) {
    if (erro && erro.status === 404) { semEstado("UF não encontrada: " + sigla); return; }
    var msg = "Não foi possível carregar as informações agora. Tente novamente em instantes.";
    ui.preencherRepresentante(elRep, null, msg);
    ui.erroGeral(elPropostas, msg);
    ui.erroGeral(elFeito, msg);
    ui.erroGeral(elRepresentantes, msg);
  });
})();
