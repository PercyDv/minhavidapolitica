/*
 * Conteúdo do protótipo do Figma (KSI — Koda Software Inteligente — Protótipo).
 * É usado no modo "demo" e no modo "auto" quando a API não está no ar.
 * São dados de demonstração do protótipo, não dados validados em fonte oficial (RN05).
 */
(function () {
  "use strict";

  var F = {
    plano: "Plano de governo 2022",
    tse: "TSE — Divulgação de Candidaturas",
    programa: "Programa de governo (site oficial)",
    dou: "Diário Oficial da União",
    transparencia: "Portal da Transparência",
    camara: "Câmara dos Deputados — Dados Abertos"
  };

  function p(status, fontes, titulo, descricao) {
    return { status: status, fontes: fontes.map(function (n) { return { nome: n, link: null }; }), titulo: titulo, descricao: descricao };
  }
  function r(data, fonte, descricao) {
    return { data: data, fonte: { nome: fonte, link: null }, descricao: descricao };
  }
  function rep(iniciais, nome, detalhe) {
    return { iniciais: iniciais, nome: nome, detalhe: detalhe, selo: "Mandato atual" };
  }
  function semDados(uf) {
    return { descricao: "Câmara dos Deputados", quantidade: "Dados não disponíveis", lista: "Lista por " + uf + " · Partido: não disponível" };
  }
  function estado(nome, gov, propostas, registros, representantes, resumo) {
    return {
      nome: nome,
      resumo: "Quem governa e representa seu estado agora.",
      governador: gov,
      propostas: propostas,
      feitos: registros,
      representantes: representantes,
      resumoRepresentantes: resumo
    };
  }
  function gov(iniciais, cargo, nome, mandato, partido) {
    return { iniciais: iniciais, cargo: cargo, nome: nome, mandato: mandato, partido: partido };
  }

  var ATE_2027 = "Mandato atual · até 06/01/2027";

  var T = {
    eduTec: ["Educação: Ampliação de vagas em escolas técnicas estaduais", "Ampliação de vagas em escolas técnicas estaduais, com foco em cursos de tecnologia e agropecuária."],
    saudeUbs: ["Saúde: Construção de novas unidades básicas de saúde nas regiões metropolitana e do interior.", "Construção de novas unidades básicas de saúde nas regiões metropolitana e do interior."],
    emprego: ["Geração de emprego: Programa de incentivo fiscal para instalação de indústrias de pequeno e médio porte.", "Programa de incentivo fiscal para instalação de indústrias de pequeno e médio porte."],
    turismo: ["Turismo: Requalificação de orlas e pontos turísticos para fomentar o turismo regional.", "Requalificação de orlas e pontos turísticos para fomentar o turismo regional."],
    seguranca: ["Segurança pública: Reforço do policiamento comunitário em bairros com maior índice de ocorrências.", "Reforço do policiamento comunitário em bairros com maior índice de ocorrências."],
    mangue: ["Meio ambiente: Plano estadual de recuperação de áreas de mangue e combate ao desmatamento costeiro.", "Plano estadual de recuperação de áreas de mangue e combate ao desmatamento costeiro."],
    habitacao: ["Habitação: Construção de unidades habitacionais populares em parceria com o governo federal.", "Construção de unidades habitacionais populares em parceria com o governo federal."],
    rodovias: ["Infraestrutura: Pavimentação e recuperação de rodovias estaduais que ligam o litoral ao sertão.", "Pavimentação e recuperação de rodovias estaduais que ligam o litoral ao sertão."]
  };
  function pt(status, fontes, tema) { return p(status, fontes, tema[0], tema[1]); }

  function relatorio(area) { return "Divulgação de relatório de acompanhamento das metas estaduais relacionadas a " + area + "."; }
  function recursos(area) { return "Liberação de recursos do orçamento estadual para ações de " + area + ", conforme balanço divulgado."; }
  function entrega(area) { return "Entrega parcial de equipamentos e reformas vinculadas ao programa estadual de " + area + "."; }
  function convenio(area) { return "Assinatura de convênio com o governo federal para repasse de verbas destinadas a " + area + "."; }
  function edital(area) { return "Publicação de edital para contratação de obras de " + area + " na capital e em municípios do interior."; }

  var brasil = {
    titulo: "Entenda quem decide por você, de forma simples.",
    resumo: "Informações oficiais sobre quem está no cargo agora: o que propôs e o que já fez. Sem opinião, sem rótulos.",
    presidente: gov("LL", "Presidente da República", "Luiz Inácio Lula da Silva", "Mandato atual · até 05/01/2027", "Partido: PT"),
    propostas: [
      p("Em andamento", [F.plano, F.tse], "Ampliar a rede pública de creches", "Em municípios com menos de 50 mil habitantes."),
      p("Concluída", [F.plano, F.tse], "Criar um cadastro nacional único", "Para programas de assistência social."),
      p("Em andamento", [F.plano, F.tse], "Ampliar o atendimento de saúde por telemedicina", "Em áreas rurais."),
      p("Apresentada", [F.plano, F.tse], "Revisar a tabela do imposto de renda", "Para a faixa de menor renda."),
      p("Apresentada", [F.plano, F.tse], "Implantar ensino em tempo integral", "Nas escolas públicas de ensino médio.")
    ],
    feitos: [
      r("18/08/2026", F.dou, "Sancionada a Lei n° 00.000/2026, que cria o cadastro nacional único de assistência social."),
      r("02/05/2025", F.dou, "Publicado decreto que regulamenta o atendimento por telemedicina no SUS em áreas rurais."),
      r("14/11/2024", F.transparencia, "Assinado convênio com 312 municípios para construção de creches."),
      r("27/03/2024", F.camara, "Enviado ao Congresso projeto de lei sobre ensino médio em tempo integral.")
    ],
    fonte: "Presidência da República",
    atualizacao: "16/10/2026"
  };
  // No celular, o protótipo junta título e descrição em uma frase só.
  brasil.propostas.forEach(function (item) {
    item.textoUnico = item.titulo + " " + item.descricao.charAt(0).toLowerCase() + item.descricao.slice(1);
  });

  var estados = {
    AL: estado("Alagoas",
      gov("FB", "Governador em exercício", "Fábio Bittencourt", "Em exercício · desde 14/10/2026", "Partido: sem partido"),
      [pt("Em andamento", [F.plano], T.eduTec), pt("Concluída", [F.programa], T.saudeUbs), pt("Em andamento", [F.tse], T.emprego)],
      [r("set/2026", F.transparencia, recursos("segurança pública")), r("out/2026", F.camara, recursos("geração de emprego")), r("mai/2026", F.dou, entrega("turismo"))],
      [rep("AB", "Ana Beatriz Lins", "Senador(a) · Partido D"), rep("PM", "Patrícia Moura", "Senador(a) · Partido D"), rep("RS", "Rafael Siqueira", "Senador(a) · Partido C")],
      semDados("AL")),
    BA: estado("Bahia",
      gov("JR", "Governador da Bahia", "Jerônimo Rodrigues", ATE_2027, "Partido: PT"),
      [pt("Apresentada", [F.plano, F.tse], T.eduTec), pt("Em andamento", [F.tse], T.turismo), pt("Concluída", [F.programa], T.seguranca)],
      [r("ago/2026", F.dou, relatorio("educação")), r("fev/2026", F.dou, relatorio("turismo")), r("out/2026", F.camara, recursos("saúde"))],
      [rep("MA", "Marina Albuquerque", "Senador(a) · Partido B"), rep("RS", "Rafael Siqueira", "Deputado(a) Federal · Partido B"), rep("FT", "Fernando Teixeira", "Senador(a) · Partido B")],
      { descricao: "Deputados federais e estaduais", quantidade: "39 federais · 63 estaduais", lista: "Lista por BA · Partido: não disponível" }),
    CE: estado("Ceará",
      gov("EF", "Governador(a)", "Elmano de Freitas", ATE_2027, "Partido: PT"),
      [pt("Apresentada", [F.programa], T.saudeUbs), pt("Em andamento", [F.programa], T.mangue), pt("Concluída", [F.tse], T.turismo)],
      [r("mai/2026", F.dou, convenio("infraestrutura")), r("ago/2026", F.transparencia, recursos("saúde")), r("fev/2026", F.dou, recursos("geração de emprego"))],
      [rep("TM", "Thiago Monteiro", "Senador(a) · Partido D"), rep("AB", "Ana Beatriz Lins", "Senador(a) · Partido C"), rep("CN", "Camila Nogueira", "Senador(a) · Partido D")],
      semDados("CE")),
    MA: estado("Maranhão",
      gov("CB", "Governador(a)", "Carlos Brandão", ATE_2027, "Partido: MDB"),
      [pt("Concluída", [F.plano], T.mangue), pt("Apresentada", [F.programa], T.saudeUbs), pt("Apresentada", [F.plano], T.turismo)],
      [r("fev/2026", F.dou, relatorio("educação")), r("mai/2026", F.transparencia, recursos("meio ambiente")), r("out/2026", F.camara, recursos("habitação"))],
      [rep("CE", "Carlos Eduardo Farias", "Senador(a) · Partido D"), rep("RS", "Rafael Siqueira", "Senador(a) · Partido B"), rep("BC", "Bruna Cavalcante", "Senador(a) · Partido C")],
      semDados("MA")),
    PB: estado("Paraíba",
      gov("LR", "Governador(a)", "Lucas Ribeiro", ATE_2027, "Partido: PP"),
      [pt("Concluída", [F.plano], T.habitacao), pt("Em andamento", [F.plano], T.mangue), pt("Apresentada", [F.tse], T.eduTec)],
      [r("out/2026", F.dou, entrega("turismo")), r("set/2026", F.camara, entrega("geração de emprego")), r("mai/2026", F.camara, recursos("segurança pública"))],
      [rep("BC", "Bruna Cavalcante", "Deputado(a) Federal · Partido A"), rep("JP", "Juliana Prado", "Senador(a) · Partido B"), rep("DA", "Diego Azevedo", "Deputado(a) Federal · Partido D")],
      semDados("PB")),
    PE: estado("Pernambuco",
      gov("RL", "Governador(a)", "Raquel Lyra", ATE_2027, "Partido: PSD"),
      [pt("Concluída", [F.programa], T.rodovias), pt("Apresentada", [F.tse], T.emprego), pt("Em andamento", [F.tse], T.turismo)],
      [r("out/2026", F.transparencia, entrega("habitação")), r("ago/2026", F.camara, relatorio("saúde")), r("fev/2026", F.transparencia, relatorio("educação"))],
      [rep("JP", "Juliana Prado", "Deputado(a) Federal · Partido D"), rep("FT", "Fernando Teixeira", "Senador(a) · Partido C"), rep("BC", "Bruna Cavalcante", "Deputado(a) Federal · Partido C")],
      semDados("PE")),
    PI: estado("Piauí",
      gov("RF", "Governador(a)", "Rafael Fonteles", ATE_2027, "Partido: PT"),
      [pt("Apresentada", [F.tse], T.eduTec), pt("Concluída", [F.tse], T.saudeUbs), pt("Concluída", [F.programa], T.rodovias)],
      [r("fev/2026", F.dou, entrega("educação")), r("out/2026", F.transparencia, convenio("infraestrutura")), r("mai/2026", F.transparencia, edital("turismo"))],
      [rep("CN", "Camila Nogueira", "Deputado(a) Federal · Partido B"), rep("TM", "Thiago Monteiro", "Senador(a) · Partido D"), rep("PM", "Patrícia Moura", "Deputado(a) Federal · Partido D")],
      semDados("PI")),
    RN: estado("Rio Grande do Norte",
      gov("FB", "Governador(a)", "Fátima Bezerra", ATE_2027, "Partido: PT"),
      [pt("Em andamento", [F.plano], T.eduTec), pt("Em andamento", [F.programa], T.habitacao), pt("Apresentada", [F.programa], T.rodovias)],
      [r("fev/2026", F.dou, edital("geração de emprego")), r("set/2026", F.transparencia, convenio("segurança pública")), r("ago/2026", F.transparencia, recursos("saúde"))],
      [rep("CE", "Carlos Eduardo Farias", "Senador(a) · Partido C"), rep("FT", "Fernando Teixeira", "Senador(a) · Partido C"), rep("AB", "Ana Beatriz Lins", "Senador(a) · Partido C")],
      semDados("RN")),
    SE: estado("Sergipe",
      gov("FM", "Governador(a)", "Fábio Mitidieri", ATE_2027, "Partido: PSD"),
      [pt("Em andamento", [F.programa], T.habitacao), pt("Apresentada", [F.programa], T.eduTec), pt("Concluída", [F.plano], T.seguranca)],
      [r("ago/2026", F.dou, relatorio("saúde")), r("out/2026", F.camara, entrega("turismo")), r("mai/2026", F.dou, relatorio("meio ambiente"))],
      [rep("MA", "Marina Albuquerque", "Senador(a) · Partido A"), rep("CE", "Carlos Eduardo Farias", "Senador(a) · Partido A"), rep("CN", "Camila Nogueira", "Deputado(a) Federal · Partido B")],
      semDados("SE"))
  };

  var ufs = [
    ["AC", "Acre"], ["AL", "Alagoas"], ["AP", "Amapá"], ["AM", "Amazonas"], ["BA", "Bahia"],
    ["CE", "Ceará"], ["DF", "Distrito Federal"], ["ES", "Espírito Santo"], ["GO", "Goiás"], ["MA", "Maranhão"],
    ["MT", "Mato Grosso"], ["MS", "Mato Grosso do Sul"], ["MG", "Minas Gerais"], ["PA", "Pará"], ["PB", "Paraíba"],
    ["PR", "Paraná"], ["PE", "Pernambuco"], ["PI", "Piauí"], ["RJ", "Rio de Janeiro"], ["RN", "Rio Grande do Norte"],
    ["RS", "Rio Grande do Sul"], ["RO", "Rondônia"], ["RR", "Roraima"], ["SC", "Santa Catarina"], ["SP", "São Paulo"],
    ["SE", "Sergipe"], ["TO", "Tocantins"]
  ].map(function (u) { return { sigla: u[0], nome: u[1], disponivel: !!estados[u[0]] }; });

  window.MVPDemo = { brasil: brasil, estados: estados, ufs: ufs, atualizacao: "16/10/2026" };
})();
