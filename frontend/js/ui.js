/*
 * Componentes visuais compartilhados (cartões, linha do tempo, representantes,
 * cabeçalho com menu de estados). Todo texto vindo da API passa por esc().
 */
(function () {
  "use strict";

  function esc(valor) {
    return String(valor == null ? "" : valor)
      .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;").replace(/'/g, "&#39;");
  }

  function linkSeguro(url) {
    if (!url) return null;
    try {
      var u = new URL(url, window.location.href);
      return (u.protocol === "http:" || u.protocol === "https:") ? u.href : null;
    } catch (e) { return null; }
  }

  /* Foto oficial ou, sem foto (ou se o arquivo não carregar), as iniciais. */
  function avatarHtml(foto, iniciaisTexto, classe) {
    var url = linkSeguro(foto);
    if (url) {
      return '<img class="' + classe + " " + classe + '--foto" src="' + esc(url) + '" alt="" loading="lazy" data-iniciais="' + esc(iniciaisTexto) + '">';
    }
    return '<span class="' + classe + '" aria-hidden="true">' + esc(iniciaisTexto) + "</span>";
  }
  function ativarFotos(raiz) {
    Array.prototype.forEach.call(raiz.querySelectorAll("img[data-iniciais]"), function (img) {
      img.addEventListener("error", function () {
        var span = document.createElement("span");
        span.className = img.className.replace(/\S+--foto/, "").trim();
        span.setAttribute("aria-hidden", "true");
        span.textContent = img.getAttribute("data-iniciais");
        img.replaceWith(span);
      }, { once: true });
    });
  }

  var SETA = '<svg width="8" height="8" viewBox="8.19 5.19 7.62 7.62" fill="none" aria-hidden="true"><path d="M9 12L15 6M15 11V6H10" stroke="currentColor" stroke-width="1.62" stroke-linecap="round" stroke-linejoin="round"/></svg>';

  var STATUS = {
    "em andamento": { classe: "andamento", icone: "andamento.svg", largura: 12, altura: 12 },
    "concluída": { classe: "concluida", icone: "concluida.svg", largura: 10, altura: 8 },
    "concluida": { classe: "concluida", icone: "concluida.svg", largura: 10, altura: 8 },
    "apresentada": { classe: "apresentada", icone: "apresentada.svg", largura: 11, altura: 13 }
  };

  function temaDaFonte(nome) {
    var n = String(nome || "").toLowerCase();
    if (n.indexOf("transpar") >= 0) return "verde";
    if (n.indexOf("câmara") >= 0 || n.indexOf("camara") >= 0 || n.indexOf("senado") >= 0) return "roxo";
    return "azul";
  }

  function pilula(fonte, comSeta, classe) {
    var link = linkSeguro(fonte.link);
    var conteudo = (comSeta ? SETA : "") + esc(fonte.nome);
    var cls = classe + (comSeta ? " " + classe + "--link" : "");
    if (link) {
      return '<a class="' + cls + '" href="' + esc(link) + '" target="_blank" rel="noopener noreferrer" title="Abrir a fonte oficial">' + conteudo + "</a>";
    }
    return '<span class="' + cls + '">' + conteudo + "</span>";
  }

  /* ---------- Proposta ---------- */
  /* PDF do próprio site (assets/planos/...): pode ser lido dentro do cartão, sem sair da página. */
  function pdfDoSite(url) {
    try {
      var u = new URL(url, window.location.href);
      return (u.origin === window.location.origin && /\.pdf$/i.test(u.pathname)) ? u.href : null;
    } catch (e) { return null; }
  }
  function ativarLeitores(raiz) {
    Array.prototype.forEach.call(raiz.querySelectorAll("button[data-pdf]"), function (botao) {
      botao.addEventListener("click", function () {
        var area = botao.parentNode;
        var quadro = area.querySelector("iframe");
        if (quadro) {
          quadro.remove();
          botao.textContent = "Ler o plano aqui";
          botao.setAttribute("aria-expanded", "false");
          return;
        }
        quadro = document.createElement("iframe");
        quadro.className = "cartao-proposta__pdf";
        quadro.src = botao.getAttribute("data-pdf");
        quadro.title = "Plano de governo (PDF)";
        area.appendChild(quadro);
        botao.textContent = "Fechar a leitura";
        botao.setAttribute("aria-expanded", "true");
      });
    });
  }

  function cartaoProposta(item) {
    var st = item.status ? STATUS[String(item.status).toLowerCase()] : null;
    var classes = "cartao-proposta" + (st ? " cartao-proposta--" + st.classe : "") + (item.textoUnico ? " cartao-proposta--com-texto-unico" : "");
    var selo = st
      ? '<span class="selo-status selo-status--' + st.classe + '"><img src="assets/icons/' + st.icone + '" width="' + st.largura + '" height="' + st.altura + '" alt="">' + esc(item.status) + "</span>"
      : "";
    var fontes = (item.fontes || []).map(function (f, i, todas) {
      return pilula(f, i === todas.length - 1, "pilula");
    }).join("");
    var pdf = (item.fontes || []).map(function (f) { return pdfDoSite(f.link); }).filter(Boolean)[0];
    var leitor = pdf
      ? '<div class="cartao-proposta__leitor"><button type="button" class="cartao-proposta__ler" data-pdf="' + esc(pdf) + '" aria-expanded="false">Ler o plano aqui</button></div>'
      : "";
    return '<article class="' + classes + '">' +
      '<span class="faixa" aria-hidden="true"></span>' +
      '<div class="cartao-proposta__topo">' + selo + '<div class="cartao-proposta__fontes">' + fontes + "</div></div>" +
      '<h3 class="cartao-proposta__titulo">' + esc(item.titulo) + "</h3>" +
      (item.descricao ? '<p class="cartao-proposta__descricao">' + esc(item.descricao) + "</p>" : "") +
      (item.textoUnico ? '<p class="cartao-proposta__texto-unico">' + esc(item.textoUnico) + "</p>" : "") +
      leitor +
      "</article>";
  }

  /* ---------- Registro (O que já foi feito) ---------- */
  function registro(item) {
    var tema = temaDaFonte(item.fonte && item.fonte.nome);
    return '<li class="registro registro--' + tema + '">' +
      '<span class="registro__marco" aria-hidden="true"></span>' +
      '<div class="registro__cartao">' +
      '<span class="faixa" aria-hidden="true"></span>' +
      '<div class="registro__topo">' +
      '<span class="registro__data">' + esc(item.data) + "</span>" +
      (item.fonte && item.fonte.nome ? pilula(item.fonte, true, "registro__fonte").replace("registro__fonte registro__fonte--link", "registro__fonte") : "") +
      "</div>" +
      '<p class="registro__descricao">' + esc(item.descricao) + "</p>" +
      "</div></li>";
  }

  /* ---------- Representantes ---------- */
  function cartaoRepresentante(rep) {
    return '<article class="cartao-rep">' +
      '<span class="faixa" aria-hidden="true"></span>' +
      '<span class="cartao-rep__avatar" aria-hidden="true">' + esc(rep.iniciais) + "</span>" +
      '<div class="cartao-rep__textos"><h3 class="cartao-rep__nome">' + esc(rep.nome) + '</h3><p class="cartao-rep__detalhe">' + esc(rep.detalhe) + "</p></div>" +
      '<span class="cartao-rep__selo">' + esc(rep.selo || "Mandato atual") + "</span>" +
      "</article>";
  }
  function cartaoResumoRepresentantes(r) {
    return '<article class="cartao-rep cartao-rep--dados">' +
      '<span class="faixa" aria-hidden="true"></span>' +
      '<span class="cartao-rep__avatar" aria-hidden="true">—</span>' +
      '<div class="cartao-rep__textos"><p class="cartao-rep__rotulo">' + esc(r.descricao) + '</p><p class="cartao-rep__quantidade">' + esc(r.quantidade) + '</p><p class="cartao-rep__lista">' + esc(r.lista) + "</p></div>" +
      "</article>";
  }

  /* ---------- Candidatos da eleição ---------- */
  function cartaoCandidato(c) {
    var plano = linkSeguro(c.plano);
    return '<article class="cartao-rep cartao-rep--candidato">' +
      '<span class="faixa" aria-hidden="true"></span>' +
      avatarHtml(c.foto, c.iniciais, "cartao-rep__avatar") +
      '<div class="cartao-rep__textos"><h3 class="cartao-rep__nome">' + esc(c.nome) + "</h3>" +
      (c.completo ? '<p class="cartao-rep__detalhe">' + esc(c.completo) + "</p>" : "") +
      '<p class="cartao-rep__detalhe"><span class="numero-urna"><span class="sr-only">Número na urna: </span>' + esc(c.numero) + "</span>" + esc(c.partido) + "</p>" +
      (plano ? '<a class="cartao-rep__plano" href="' + esc(plano) + '" target="_blank" rel="noopener noreferrer" title="Abrir o plano de governo registrado no TSE">' + SETA + "Plano de governo (PDF)</a>" : "") +
      "</div>" +
      '<span class="cartao-rep__selo">' + esc(c.situacao) + "</span>" +
      "</article>";
  }

  /* Preenche a seção #candidatos. Sem dado de candidatos (modo protótipo), a seção continua oculta. */
  function preencherCandidatos(secao, resultado, titulo) {
    if (!secao || !resultado || resultado.oculto) return;
    var ano = resultado.ano || (window.MVPApi && window.MVPApi.anoEleicao) || "";
    secao.querySelector(".demais__titulo").textContent = titulo;
    secao.querySelector(".candidatos__intro").textContent = "Quem concorreu na eleição de " + ano +
      ", em ordem alfabética. A lista não indica preferência nem ranking, e quem concorreu não é, necessariamente, quem exerce o cargo hoje.";
    var lista = secao.querySelector(".lista-representantes");
    var rodape = secao.querySelector(".candidatos__fonte");
    if (!resultado.itens || !resultado.itens.length) {
      lista.innerHTML = avisoNeutro(resultado.mensagem);
      rodape.innerHTML = "<span>Fonte: pendente</span><span>Atualização: —</span>";
    } else {
      lista.innerHTML = resultado.itens.map(cartaoCandidato).join("");
      var link = linkSeguro(resultado.linkFonte);
      var fonte = link
        ? '<a href="' + esc(link) + '" target="_blank" rel="noopener noreferrer">' + esc(resultado.fonte) + "</a>"
        : esc(resultado.fonte || "pendente");
      rodape.innerHTML = "<span>Fonte: " + fonte + "</span><span>Atualização: " + esc(resultado.atualizacao || "—") + "</span>" +
        (resultado.credito ? '<span class="candidatos__credito">Fotos e planos de governo registrados no TSE (licença Creative Commons Atribuição).</span>' : "");
      ativarFotos(lista);
    }
    secao.hidden = false;
  }

  function avisoNeutro(mensagem) {
    return '<p class="aviso-neutro">' + esc(mensagem || window.MVPApi.MENSAGEM_PADRAO) + "</p>";
  }

  /* ---------- Painel do mandato (representante + abas) ---------- */
  function preencherRepresentante(el, pessoa, mensagem) {
    if (!pessoa) {
      el.innerHTML = '<span class="avatar" aria-hidden="true">—</span><div class="identificacao"><p class="identificacao__linha">' + esc(mensagem) + "</p></div>";
      return;
    }
    el.innerHTML =
      avatarHtml(pessoa.foto, pessoa.iniciais, "avatar") +
      '<div class="identificacao">' +
      '<p class="identificacao__linha">' + esc(pessoa.cargo) + "</p>" +
      '<h2 class="identificacao__nome">' + esc(pessoa.nome) + "</h2>" +
      '<p class="identificacao__linha">' + esc(pessoa.mandato) + "</p>" +
      '<p class="identificacao__linha">' + esc(pessoa.partido) + "</p>" +
      (pessoa.resumo ? '<p class="identificacao__resumo">' + esc(pessoa.resumo) + "</p>" : "") +
      "</div>";
    ativarFotos(el);
  }

  function preencherPropostas(el, resultado) {
    if (!resultado.itens || !resultado.itens.length) { el.innerHTML = avisoNeutro(resultado.mensagem); return; }
    el.innerHTML = resultado.itens.map(cartaoProposta).join("");
    ativarLeitores(el);
  }
  function preencherFeitos(el, resultado) {
    if (!resultado.itens || !resultado.itens.length) { el.innerHTML = avisoNeutro(resultado.mensagem); return; }
    el.innerHTML = '<ol class="linha-tempo">' + resultado.itens.map(registro).join("") + "</ol>";
  }

  /* Abas Propostas / O que já foi feito (estado guardado em ?aba=feito) */
  function iniciarAbas(raiz) {
    var abas = Array.prototype.slice.call(raiz.querySelectorAll('[role="tab"]'));
    function ativar(aba, foco) {
      abas.forEach(function (a) {
        var ativa = a === aba;
        a.setAttribute("aria-selected", ativa ? "true" : "false");
        a.tabIndex = ativa ? 0 : -1;
        document.getElementById(a.getAttribute("aria-controls")).hidden = !ativa;
      });
      if (foco) aba.focus();
      var url = new URL(window.location.href);
      if (aba.dataset.aba === "feito") url.searchParams.set("aba", "feito");
      else url.searchParams.delete("aba");
      window.history.replaceState(null, "", url);
    }
    abas.forEach(function (aba, i) {
      aba.addEventListener("click", function () { ativar(aba); });
      aba.addEventListener("keydown", function (ev) {
        if (ev.key === "ArrowRight" || ev.key === "ArrowLeft") {
          ev.preventDefault();
          var prox = abas[(i + (ev.key === "ArrowRight" ? 1 : abas.length - 1)) % abas.length];
          ativar(prox, true);
        }
      });
    });
    var inicial = new URLSearchParams(window.location.search).get("aba") === "feito" ? abas[1] : abas[0];
    if (inicial) ativar(inicial);
  }

  /* ---------- Cabeçalho: menu "Escolha seu estado" ---------- */
  function iniciarMenuEstados(opcoes) {
    var botao = document.getElementById("seletor-estado");
    var menu = document.getElementById("menu-estados");
    if (!botao || !menu) return;
    var atual = opcoes && opcoes.atual;
    var valor = botao.querySelector(".seletor-estado__valor");
    var montado = false;

    function itemUf(u) {
      var corrente = u.sigla === atual ? ' aria-current="true"' : "";
      if (u.disponivel) {
        return '<a class="menu-estados__uf" href="estado.html?uf=' + esc(u.sigla) + '" title="' + esc(u.nome) + '"' + corrente + ">" + esc(u.sigla) + "</a>";
      }
      return '<span class="menu-estados__uf menu-estados__uf--inativa" title="' + esc(u.nome) + '">' + esc(u.sigla) + "</span>";
    }

    var celular = window.matchMedia("(max-width: 767px)");
    var ultimaLista = null;
    function montar(ufs) {
      ultimaLista = ufs;
      var porLinha = celular.matches ? 4 : 5;
      var resto = ufs.length % porLinha;
      var cheias = ufs.slice(0, ufs.length - resto);
      var ultimas = ufs.slice(ufs.length - resto);
      menu.querySelector(".menu-estados__opcoes").innerHTML =
        cheias.map(itemUf).join("") +
        (ultimas.length ? '<div class="menu-estados__ultima">' + ultimas.map(itemUf).join("") + "</div>" : "");
      montado = true;
    }
    if (celular.addEventListener) {
      celular.addEventListener("change", function () { if (ultimaLista) montar(ultimaLista); });
    }

    function abrir() {
      menu.hidden = false;
      botao.setAttribute("aria-expanded", "true");
      if (!montado) {
        montar(window.MVPDemo.ufs.map(function (u) { return { sigla: u.sigla, nome: u.nome, disponivel: window.MVPApi.modo() !== "demo" || u.disponivel }; }));
        window.MVPApi.ufs().then(montar).catch(function () { /* mantém a lista local */ });
      }
      var primeiro = menu.querySelector("a.menu-estados__uf");
      if (primeiro) primeiro.focus();
    }
    function fechar(devolverFoco) {
      menu.hidden = true;
      botao.setAttribute("aria-expanded", "false");
      if (devolverFoco) botao.focus();
    }

    botao.addEventListener("click", function (ev) {
      ev.stopPropagation();
      if (menu.hidden) abrir(); else fechar();
    });
    menu.querySelector(".menu-estados__fechar").addEventListener("click", function () { fechar(true); });
    document.addEventListener("click", function (ev) {
      if (!menu.hidden && !menu.contains(ev.target) && ev.target !== botao) fechar();
    });
    document.addEventListener("keydown", function (ev) {
      if (ev.key === "Escape" && !menu.hidden) fechar(true);
    });

    if (atual && valor && opcoes.nomeAtual) valor.textContent = opcoes.nomeAtual;
  }

  function erroGeral(el, mensagem) {
    el.innerHTML = avisoNeutro(mensagem);
  }

  window.MVPUi = {
    esc: esc,
    cartaoProposta: cartaoProposta,
    registro: registro,
    cartaoRepresentante: cartaoRepresentante,
    cartaoResumoRepresentantes: cartaoResumoRepresentantes,
    cartaoCandidato: cartaoCandidato,
    preencherCandidatos: preencherCandidatos,
    avisoNeutro: avisoNeutro,
    preencherRepresentante: preencherRepresentante,
    preencherPropostas: preencherPropostas,
    preencherFeitos: preencherFeitos,
    iniciarAbas: iniciarAbas,
    iniciarMenuEstados: iniciarMenuEstados,
    erroGeral: erroGeral
  };
})();
