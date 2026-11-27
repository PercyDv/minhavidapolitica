# Cadastra o plano de governo de 2026 como proposta de quem esta no cargo e tambem foi candidato.
#
# Uso (na raiz do repositorio, com a aplicacao no ar):
#   powershell -ExecutionPolicy Bypass -File scripts\tse\vincular-planos-aos-mandatos.ps1
#
# Este arquivo so tem caracteres ASCII de proposito: os textos com acento ficam em
# planos-no-mandato.json (UTF-8). Pode rodar de novo sem duplicar: o que ja existe e atualizado.
param(
    [string]$Base = "http://localhost:8080",
    [string]$Usuario = "admin",
    [string]$Json = (Join-Path $PSScriptRoot "planos-no-mandato.json")
)

$segura = Read-Host "Senha do administrador" -AsSecureString
$senha = (New-Object System.Net.NetworkCredential("", $segura)).Password

Invoke-RestMethod -Uri "$Base/api/auth/login" -Method Post -SessionVariable sessao -Body @{ username = $Usuario; password = $senha } | Out-Null
$dados = Get-Content $Json -Raw -Encoding UTF8 | ConvertFrom-Json

$fonte = @(Invoke-RestMethod "$Base/api/admin/fontes" -WebSession $sessao | Where-Object nome -eq $dados.fonte)
if ($fonte.Count -ne 1) {
    Write-Host ("ERRO: fonte '" + $dados.fonte + "' nao encontrada (ou repetida). Cadastre-a em Fontes de dados.")
    exit 1
}
$fonteId = $fonte[0].id

foreach ($item in $dados.itens) {
    $busca = [uri]::EscapeDataString($item.busca)
    $achados = @(Invoke-RestMethod "$Base/api/admin/politicos?nome=$busca" -WebSession $sessao)
    if ($achados.Count -ne 1) {
        Write-Host ("PULADO: a busca '" + $item.busca + "' devolveu " + $achados.Count + " politico(s), e o esperado e 1.")
        continue
    }
    $politico = $achados[0]
    $corpo = @{ titulo = $dados.titulo; descricao = $dados.descricao; fonteId = $fonteId; linkFonte = $item.arquivo } | ConvertTo-Json
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($corpo)

    # Procura pelo caminho do PDF (so ASCII), para nao duplicar se o script rodar de novo
    $existentes = @(Invoke-RestMethod "$Base/api/admin/politicos/$($politico.id)/propostas" -WebSession $sessao | Where-Object linkFonte -eq $item.arquivo)
    if ($existentes.Count -gt 0) {
        $r = Invoke-RestMethod "$Base/api/admin/propostas/$($existentes[0].id)" -Method Put -WebSession $sessao -ContentType "application/json; charset=utf-8" -Body $bytes
        Write-Host ("ATUALIZADO: " + $item.rotulo + " (proposta " + $r.id + ")")
    } else {
        $r = Invoke-RestMethod "$Base/api/admin/politicos/$($politico.id)/propostas" -Method Post -WebSession $sessao -ContentType "application/json; charset=utf-8" -Body $bytes
        Write-Host ("CRIADO: " + $item.rotulo + " (proposta " + $r.id + ")")
    }
}
Write-Host "Pronto. Confira em http://localhost:5500 (Ctrl+F5)."
