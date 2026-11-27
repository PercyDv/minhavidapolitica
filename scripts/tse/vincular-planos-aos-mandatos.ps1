# Cadastra o plano de governo de 2026 como proposta de quem esta no cargo e tambem foi candidato.
#
# Uso (na raiz do repositorio, com a aplicacao no ar):
#   powershell -ExecutionPolicy Bypass -File scripts\tse\vincular-planos-aos-mandatos.ps1
#
# Este arquivo so tem caracteres ASCII de proposito: os textos com acento ficam em
# planos-no-mandato.json (UTF-8). Pode rodar de novo sem duplicar: o que ja existe e atualizado.
#
# As listas da API sao lidas com "foreach" e contadas uma a uma, porque o Windows PowerShell 5.1
# entrega uma lista JSON como um unico objeto e o filtro direto no pipeline engana a contagem.
param(
    [string]$Base = "http://localhost:8080",
    [string]$Usuario = "admin",
    [string]$Json = (Join-Path $PSScriptRoot "planos-no-mandato.json")
)

$segura = Read-Host "Senha do administrador" -AsSecureString
$senha = (New-Object System.Net.NetworkCredential("", $segura)).Password

try {
    Invoke-RestMethod -Uri "$Base/api/auth/login" -Method Post -SessionVariable sessao -Body @{ username = $Usuario; password = $senha } -ErrorAction Stop | Out-Null
} catch {
    Write-Host ("ERRO: nao consegui entrar em " + $Base + ".")
    Write-Host "Confira: (1) a aplicacao esta no ar na porta 8080? (2) a senha do administrador esta certa?"
    Write-Host ("Detalhe tecnico: " + $Error[0].Exception.Message)
    exit 1
}
$dados = Get-Content $Json -Raw -Encoding UTF8 | ConvertFrom-Json

try {
    $fontes = Invoke-RestMethod "$Base/api/admin/fontes" -WebSession $sessao -ErrorAction Stop
} catch {
    Write-Host "ERRO: entrei, mas nao consegui listar as fontes."
    Write-Host ("Detalhe tecnico: " + $Error[0].Exception.Message)
    exit 1
}

$fonteId = $null
$quantasFontes = 0
foreach ($f in $fontes) {
    if ($f.nome -eq $dados.fonte) {
        $quantasFontes++
        $fonteId = $f.id
    }
}
if ($quantasFontes -ne 1) {
    Write-Host ("ERRO: achei " + $quantasFontes + " fonte(s) chamada(s) '" + $dados.fonte + "', e o esperado e 1.")
    Write-Host "Fontes cadastradas:"
    foreach ($f in $fontes) { Write-Host ("  - id " + $f.id + ": " + $f.nome) }
    exit 1
}
Write-Host ("Fonte encontrada: id " + $fonteId)

foreach ($item in $dados.itens) {
    $busca = [uri]::EscapeDataString($item.busca)
    $achados = Invoke-RestMethod "$Base/api/admin/politicos?nome=$busca" -WebSession $sessao
    $quantos = 0
    $politico = $null
    foreach ($p in $achados) {
        $quantos++
        $politico = $p
    }
    if ($quantos -ne 1) {
        Write-Host ("PULADO: a busca '" + $item.busca + "' devolveu " + $quantos + " politico(s), e o esperado e 1.")
        continue
    }

    $corpo = @{ titulo = $dados.titulo; descricao = $dados.descricao; fonteId = $fonteId; linkFonte = $item.arquivo } | ConvertTo-Json
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($corpo)

    # Procura pelo caminho do PDF (so ASCII), para nao duplicar se o script rodar de novo
    $propostas = Invoke-RestMethod "$Base/api/admin/politicos/$($politico.id)/propostas" -WebSession $sessao
    $existenteId = $null
    foreach ($pr in $propostas) {
        if ($pr.linkFonte -eq $item.arquivo) { $existenteId = $pr.id }
    }

    if ($existenteId -ne $null) {
        $r = Invoke-RestMethod "$Base/api/admin/propostas/$existenteId" -Method Put -WebSession $sessao -ContentType "application/json; charset=utf-8" -Body $bytes
        Write-Host ("ATUALIZADO: " + $item.rotulo + " (proposta " + $r.id + ")")
    } else {
        $r = Invoke-RestMethod "$Base/api/admin/politicos/$($politico.id)/propostas" -Method Post -WebSession $sessao -ContentType "application/json; charset=utf-8" -Body $bytes
        Write-Host ("CRIADO: " + $item.rotulo + " (proposta " + $r.id + ")")
    }
}
Write-Host "Pronto. Confira em http://localhost:5500 (Ctrl+F5)."
