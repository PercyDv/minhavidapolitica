/*
 * Camada de dados: conversa com a API pública do back-end (somente leitura) e
 * transforma as respostas no formato que as telas usam.
 *
 * Endpoints usados (ver PublicoController no back-end):
 *   GET /api/presidente                    -> Resultado<PoliticoPublico>
 *   GET /api/ufs                           -> List<UfResumo>
 *   GET /api/ufs/{sigla}                   -> UfPublica (governador em Resultado)
 *   GET /api/ufs/{sigla}/representantes    -> Resultado<List<PoliticoPublico>>
 *   GET /api/politicos/{id}/propostas      -> Resultado<List<ItemPublico>>
 *   GET /api/politicos/{id}/acoes          -> Resultado<List<ItemPublico>>
 *   GET /api/eleicoes/{ano}/presidente                 -> Resultado<List<CandidaturaPublica>>
 *   GET /api/eleicoes/{ano}/ufs/{sigla}/governador     -> Resultado<List<CandidaturaPublica>>
 *
 * Quando a API responde "disponivel: false", a tela mostra a mensagem neutra que vem
 * dela (RF12), sem inventar dado.
 */
(function () {
  "use strict";

  var cfg = window.MVP_CONFIG || {};
  var MENSAGEM_PADRAO = "Informação não disponível nas fontes oficiais consultadas";
  var MESES = ["jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"];

  function modoAtual() {
    var daUrl = new URLSearchParams(window.location.search).get("modo");
    var modo = daUrl || cfg.modo || "auto";
    return ["auto", "api", "demo"].indexOf(modo) >= 0 ? modo : "auto";
  }

  function ErroApi(status, mensagem) {
    this.name = "ErroApi";
    this.status = status;
    this.message = mensagem || ("Erro " + status);
  }
  ErroApi.prototype = Object.create(Error.prototype);

  function SemConexao(causa) {
    this.name = "SemConexao";
    this.message = "API fora do ar";
    this.causa = causa;
  }
  SemConexao.prototype = Object.create(Error.prototype);

  function obter(caminho) {
    var base = (cfg.apiBase || "/api").replace(/\/$/, "");
    var controle = typeof AbortController !== "undefined" ? new AbortController() : null;
    var timer = controle ? setTimeout(function () { controle.abort(); }, 8000) : null;
    return fetch(base + caminho, { headers: { Accept: "application/json" }, signal: controle ? controle.signal : undefined })
      .catch(function (erro) { throw new SemConexao(erro); })
      .then(function (resposta) {
        if (timer) clearTimeout(timer);
        if (!resposta.ok) {
          return resposta.json().catch(function () { return {}; }).then(function (corpo) {
            throw new ErroApi(resposta.status, corpo && (corpo.mensagem || corpo.message));
          });
        }
        return resposta.json();
      });
  }

  /* ---------- Formatação ---------- */
  function partesData(valor) {
    if (!valor) return null;
    var m = String(valor).match(/^(\d{4})-(\d{2})-(\d{2})/);
    return m ? { ano: m[1], mes: m[2], dia: m[3] } : null;
  }
  function dataCompleta(valor) {
    var d = partesData(valor);
    return d ? d.dia + "/" + d.mes + "/" + d.ano : "";
  }
  function dataMesAno(valor) {
    var d = partesData(valor);
    return d ? MESES[parseInt(d.mes, 10) - 1] + "/" + d.ano : "";
  }
  var LIGACOES = ["da", "de", "do", "das", "dos", "e"];
  function iniciais(nome) {
    var partes = String(nome || "").trim().split(/\s+/).filter(function (p) { return p && LIGACOES.indexOf(p.toLowerCase()) < 0; });
    if (!partes.length) return "—";
    var primeira = partes[0].charAt(0);
    var ultima = partes.length > 1 ? partes[partes.length - 1].charAt(0) : "";
    return (primeira + ultima).toUpperCase();
  }

  /* ---------- Conversões da API para as telas ---------- */
  function politicoParaTela(p) {
    var mandato = "Mandato atual";
    if (p.fimMandato) mandato += " · até " + dataCompleta(p.fimMandato);
    return {
      id: p.id,
      iniciais: iniciais(p.nome),
      cargo: p.cargo || "",
      nome: p.nome || "",
      resumo: p.resumo || "",
      mandato: mandato,
      partido: "Partido: " + (p.partido || "não informado"),
      fonte: p.fonte || null
    };
  }
  function itemParaProposta(item) {
    return {
      status: item.status || null,
      fontes: item.fonte ? [{ nome: item.fonte.nome, link: item.fonte.link || null }] : [],
      titulo: item.titulo || "",
      descricao: item.descricao || ""
    };
  }
  function itemParaRegistro(formatoData) {
    return function (item) {
      return {
        data: formatoData(item.data) || dataCompleta(item.fonte && item.fonte.atualizadoEm),
        fonte: item.fonte ? { nome: item.fonte.nome, link: item.fonte.link || null } : { nome: "", link: null },
        descricao: item.titulo + (item.descricao ? " — " + item.descricao : "")
      };
    };
  }
  function representanteParaTela(p) {
    return {
      iniciais: iniciais(p.nome),
      nome: p.nome,
      detalhe: [p.cargo, p.partido ? "Partido " + p.partido : "Partido: não informado"].filter(Boolean).join(" · "),
      selo: "Mandato atual"
    };
  }
  function lista(resultado, conversor) {
    if (!resultado || !resultado.disponivel) return { itens: null, mensagem: (resultado && resultado.mensagem) || MENSAGEM_PADRAO };
    return { itens: (resultado.dados || []).map(conversor), mensagem: null };
  }
  function listaDemo(itens) { return { itens: itens, mensagem: null }; }
  function indisponivel(mensagem) { return { itens: null, mensagem: mensagem || MENSAGEM_PADRAO }; }

  function atividadesDoPolitico(id, formatoData) {
    return Promise.all([
      obter("/politicos/" + id + "/propostas").catch(tratarItem),
      obter("/politicos/" + id + "/acoes").catch(tratarItem)
    ]).then(function (r) {
      return { propostas: lista(r[0], itemParaProposta), feitos: lista(r[1], itemParaRegistro(formatoData)) };
    });
  }
  function tratarItem(erro) {
    if (erro instanceof SemConexao) throw erro;
    return { disponivel: false, mensagem: MENSAGEM_PADRAO };
  }

  /* ---------- Brasil ---------- */
  function brasilDemo() {
    var d = window.MVPDemo.brasil;
    return {
      origem: "demo",
      titulo: d.titulo, resumo: d.resumo,
      presidente: d.presidente, mensagem: null,
      propostas: listaDemo(d.propostas), feitos: listaDemo(d.feitos),
      fonte: d.fonte, atualizacao: d.atualizacao
    };
  }
  function brasilApi() {
    var d = window.MVPDemo.brasil;
    return obter("/presidente").then(function (res) {
      var base = { origem: "api", titulo: d.titulo, resumo: d.resumo, fonte: "", atualizacao: "" };
      if (!res || !res.disponivel || !res.dados) {
        base.presidente = null;
        base.mensagem = (res && res.mensagem) || MENSAGEM_PADRAO;
        base.propostas = indisponivel(base.mensagem);
        base.feitos = indisponivel(base.mensagem);
        return base;
      }
      var pres = politicoParaTela(res.dados);
      base.presidente = pres;
      base.mensagem = null;
      if (pres.fonte) {
        base.fonte = pres.fonte.nome || "";
        base.atualizacao = dataCompleta(pres.fonte.atualizadoEm);
        base.linkFonte = pres.fonte.link || null;
      }
      return atividadesDoPolitico(pres.id, dataCompleta).then(function (a) {
        base.propostas = a.propostas;
        base.feitos = a.feitos;
        return base;
      });
    });
  }

  /* ---------- Estado ---------- */
  function estadoDemo(sigla) {
    var e = window.MVPDemo.estados[sigla];
    var uf = window.MVPDemo.ufs.filter(function (u) { return u.sigla === sigla; })[0];
    if (!uf) return null;
    if (!e) {
      return {
        origem: "demo", sigla: sigla, nome: uf.nome, resumo: "Quem governa e representa seu estado agora.",
        governador: null, mensagem: MENSAGEM_PADRAO,
        propostas: indisponivel(), feitos: indisponivel(), representantes: indisponivel(), resumoRepresentantes: null
      };
    }
    return {
      origem: "demo", sigla: sigla, nome: e.nome, resumo: e.resumo,
      governador: e.governador, mensagem: null,
      propostas: listaDemo(e.propostas), feitos: listaDemo(e.feitos),
      representantes: listaDemo(e.representantes), resumoRepresentantes: e.resumoRepresentantes
    };
  }
  function estadoApi(sigla) {
    return Promise.all([
      obter("/ufs/" + encodeURIComponent(sigla)),
      obter("/ufs/" + encodeURIComponent(sigla) + "/representantes").catch(tratarItem)
    ]).then(function (r) {
      var uf = r[0];
      var base = {
        origem: "api", sigla: uf.sigla, nome: uf.nome, resumo: "Quem governa e representa seu estado agora.",
        representantes: lista(r[1], representanteParaTela), resumoRepresentantes: null
      };
      var g = uf.governador;
      if (!g || !g.disponivel || !g.dados) {
        base.governador = null;
        base.mensagem = (g && g.mensagem) || MENSAGEM_PADRAO;
        base.propostas = indisponivel(base.mensagem);
        base.feitos = indisponivel(base.mensagem);
        return base;
      }
      base.governador = politicoParaTela(g.dados);
      base.mensagem = null;
      return atividadesDoPolitico(base.governador.id, dataMesAno).then(function (a) {
        base.propostas = a.propostas;
        base.feitos = a.feitos;
        return base;
      });
    });
  }

  /* ---------- UFs (seletor) ---------- */
  function ufsApi() {
    return obter("/ufs").then(function (lista) {
      return lista.map(function (u) { return { sigla: u.sigla, nome: u.nome, disponivel: true }; });
    });
  }

  /* ---------- Candidatos da eleição (extensão fora do escopo validado, RN01) ---------- */
  var ANO_ELEICAO = cfg.anoEleicao || 2026;
  var TEXTO_SITUACAO = { ELEITO: "Eleito(a)", NAO_ELEITO: "Não eleito(a)", SEGUNDO_TURNO: "Foi ao 2º turno", SEM_RESULTADO: "Sem resultado" };
  var ORDEM = typeof Intl !== "undefined" && Intl.Collator ? new Intl.Collator("pt-BR", { sensitivity: "base" }) : null;

  function nomeExibido(c) { return c.nomeUrna || c.nome || ""; }
  function comparar(a, b) {
    var x = nomeExibido(a), y = nomeExibido(b);
    return ORDEM ? ORDEM.compare(x, y) : (x < y ? -1 : (x > y ? 1 : 0));
  }
  function candidatoParaTela(c) {
    return {
      numero: String(c.numero || ""),
      nome: nomeExibido(c),
      completo: c.nomeUrna && c.nomeUrna !== c.nome ? c.nome : "",
      partido: c.partido ? "Partido: " + c.partido : "Partido não informado",
      situacao: TEXTO_SITUACAO[c.situacao] || ""
    };
  }
  function candidatos(caminho) {
    var modo = modoAtual();
    if (modo === "demo") return Promise.resolve({ oculto: true });
    return obter(caminho).then(function (res) {
      var vazio = { itens: null, fonte: "", linkFonte: null, atualizacao: "", ano: ANO_ELEICAO };
      if (!res || !res.disponivel || !res.dados || !res.dados.length) {
        vazio.mensagem = (res && res.mensagem) || MENSAGEM_PADRAO;
        return vazio;
      }
      var lista = res.dados.slice().sort(comparar);
      var fonte = lista[0].fonte || null;
      return {
        itens: lista.map(candidatoParaTela), mensagem: null, ano: ANO_ELEICAO,
        fonte: fonte ? fonte.nome || "" : "", linkFonte: fonte ? fonte.link || null : null,
        atualizacao: fonte ? dataCompleta(fonte.atualizadoEm) : ""
      };
    }).catch(function (erro) {
      // API fora do ar no modo "auto": a página já mostra o protótipo, então a seção não aparece.
      if (modo === "auto" && erro instanceof SemConexao) return { oculto: true };
      return { itens: null, mensagem: "Não foi possível carregar os candidatos agora.", fonte: "", linkFonte: null, atualizacao: "", ano: ANO_ELEICAO };
    });
  }

  /* ---------- Escolha entre API e protótipo ---------- */
  function comModo(daApi, doDemo) {
    var modo = modoAtual();
    if (modo === "demo") return Promise.resolve(doDemo());
    return daApi().catch(function (erro) {
      if (modo === "auto" && erro instanceof SemConexao) {
        if (window.console) console.info("[MinhaVidaPolitica] API fora do ar em " + cfg.apiBase + ": mostrando o conteúdo do protótipo.");
        return doDemo();
      }
      throw erro;
    });
  }

  window.MVPApi = {
    modo: modoAtual,
    brasil: function () { return comModo(brasilApi, brasilDemo); },
    estado: function (sigla) { return comModo(function () { return estadoApi(sigla); }, function () { return estadoDemo(sigla); }); },
    anoEleicao: ANO_ELEICAO,
    candidatosBrasil: function () { return candidatos("/eleicoes/" + ANO_ELEICAO + "/presidente"); },
    candidatosEstado: function (sigla) { return candidatos("/eleicoes/" + ANO_ELEICAO + "/ufs/" + encodeURIComponent(sigla) + "/governador"); },
    ufs: function () { return comModo(ufsApi, function () { return window.MVPDemo.ufs; }); },
    ErroApi: ErroApi,
    SemConexao: SemConexao,
    formatar: { dataCompleta: dataCompleta, dataMesAno: dataMesAno, iniciais: iniciais },
    MENSAGEM_PADRAO: MENSAGEM_PADRAO
  };
})();
