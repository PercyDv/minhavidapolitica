/* Tela Configurações: preferências salvas só neste aparelho (sem cadastro). */
(function () {
  "use strict";

  var P = window.MVPPreferencias;
  var prefs = P.ler();
  var elAviso = document.getElementById("aviso");
  var timerAviso = null;

  function aviso(texto) {
    elAviso.textContent = texto;
    elAviso.hidden = false;
    clearTimeout(timerAviso);
    timerAviso = setTimeout(function () { elAviso.hidden = true; }, 3200);
  }

  function gravar() {
    P.salvar(prefs);
    P.aplicar(prefs);
  }

  /* Voltar para a página anterior do próprio site, quando houver */
  document.getElementById("voltar").addEventListener("click", function (ev) {
    if (document.referrer && document.referrer.indexOf(window.location.origin) === 0 && window.history.length > 1) {
      ev.preventDefault();
      window.history.back();
    }
  });

  /* Estado padrão */
  var select = document.getElementById("estado-padrao");
  window.MVPDemo.ufs.forEach(function (u) {
    var op = document.createElement("option");
    op.value = u.sigla;
    op.textContent = u.nome;
    select.appendChild(op);
  });
  select.value = prefs.estadoPadrao || "";
  select.addEventListener("change", function () {
    prefs.estadoPadrao = select.value;
    gravar();
    try { window.sessionStorage.removeItem("mvp.abriuEstadoPadrao"); } catch (e) { /* ignora */ }
    aviso(select.value ? "O app vai abrir em " + select.options[select.selectedIndex].text + "." : "O app vai abrir na página do Brasil.");
  });

  /* Segmentos: tema e tamanho do texto */
  Array.prototype.forEach.call(document.querySelectorAll(".segmentos"), function (grupo) {
    var chave = grupo.dataset.pref;
    var opcoes = Array.prototype.slice.call(grupo.querySelectorAll(".segmentos__opcao"));
    function marcar() {
      opcoes.forEach(function (o) {
        var ativo = o.dataset.valor === prefs[chave];
        o.setAttribute("aria-checked", ativo ? "true" : "false");
        o.tabIndex = ativo ? 0 : -1;
      });
    }
    opcoes.forEach(function (o, i) {
      o.addEventListener("click", function () { prefs[chave] = o.dataset.valor; gravar(); marcar(); });
      o.addEventListener("keydown", function (ev) {
        if (ev.key !== "ArrowRight" && ev.key !== "ArrowLeft") return;
        ev.preventDefault();
        var prox = opcoes[(i + (ev.key === "ArrowRight" ? 1 : opcoes.length - 1)) % opcoes.length];
        prox.click();
        prox.focus();
      });
    });
    marcar();
  });

  /* Interruptores */
  function iniciarInterruptor(botao, ler, escrever) {
    function marcar() { botao.setAttribute("aria-checked", ler() ? "true" : "false"); }
    botao.addEventListener("click", function () { escrever(!ler()); gravar(); marcar(); });
    marcar();
  }
  iniciarInterruptor(document.querySelector('[data-pref="contraste"]'),
    function () { return !!prefs.contraste; },
    function (v) { prefs.contraste = v; });
  Array.prototype.forEach.call(document.querySelectorAll("[data-notificacao]"), function (botao) {
    var chave = botao.dataset.notificacao;
    iniciarInterruptor(botao,
      function () { return !!prefs.notificacoes[chave]; },
      function (v) { prefs.notificacoes[chave] = v; });
  });

  /* Limpar dados */
  document.getElementById("limpar-dados").addEventListener("click", function () {
    if (!window.confirm("Apagar o estado escolhido e as preferências deste aparelho?")) return;
    P.limpar();
    window.location.reload();
  });

  /* Linhas informativas */
  Array.prototype.forEach.call(document.querySelectorAll("[data-aviso]"), function (botao) {
    botao.addEventListener("click", function () { aviso(botao.dataset.aviso); });
  });

  /* Compartilhar */
  document.getElementById("compartilhar").addEventListener("click", function () {
    var url = new URL("index.html", window.location.href).href;
    var dados = { title: "Minha VidaPolítica", text: "Informações oficiais sobre quem decide por você, sem opinião.", url: url };
    if (navigator.share) {
      navigator.share(dados).catch(function () { /* cancelado */ });
    } else if (navigator.clipboard) {
      navigator.clipboard.writeText(url).then(function () { aviso("Link copiado."); }, function () { aviso(url); });
    } else {
      aviso(url);
    }
  });

  /* Versão e data de atualização */
  document.getElementById("versao-app").textContent = (window.MVP_CONFIG && window.MVP_CONFIG.versao) || "1.0.0";
  window.MVPApi.brasil().then(function (dados) {
    document.getElementById("dados-atualizados").textContent = dados.atualizacao || "Não disponível";
  }).catch(function () {
    document.getElementById("dados-atualizados").textContent = "Não disponível";
  });
})();
