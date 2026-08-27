# Restaura o banco ao estado do snapshot (backend/db/snapshot/snapshot.sql).
#
# DESTRUTIVO: apaga o schema `public` inteiro e o recria a partir do snapshot. Tudo que foi
# lançado depois de o snapshot ter sido gerado é perdido — é exatamente esse o objetivo.
#
# Os arquivos de anexo e foto em backend/uploads/ NÃO são tocados, e é isso que mantém os
# anexos restaurados funcionando. Não apague essa pasta junto.
#
# Pare o backend antes de rodar: com a aplicação no ar, o ddl-auto=update pode recriar tabelas
# em cima da restauração.
#
#   .\restaurar.ps1           # pede confirmação
#   .\restaurar.ps1 -Force    # sem confirmação

param([switch]$Force)

$ErrorActionPreference = 'Stop'

$raiz = Resolve-Path "$PSScriptRoot\..\..\.."
$propriedades = Get-Content "$raiz\backend\src\main\resources\application.properties"

function Valor($chave) {
    $linha = $propriedades | Where-Object { $_ -match "^$([regex]::Escape($chave))=" }
    $bruto = $linha -replace "^$([regex]::Escape($chave))=", ''
    if ($bruto -match '^\$\{([^:}]+):(.*)\}$') {
        $doAmbiente = [Environment]::GetEnvironmentVariable($Matches[1])
        if ($doAmbiente) { return $doAmbiente }
        return $Matches[2]
    }
    return $bruto
}

$url = Valor 'spring.datasource.url'
if ($url -notmatch 'postgresql://([^:/]+):(\d+)/(.+)$') { throw "Não consegui ler a URL do banco: $url" }
$servidor = $Matches[1]; $porta = $Matches[2]; $banco = $Matches[3]
$usuario = Valor 'spring.datasource.username'

$snapshot = "$PSScriptRoot\snapshot.sql"
if (-not (Test-Path $snapshot)) { throw "Snapshot não encontrado em $snapshot. Rode .\gerar.ps1 primeiro." }

$psql = "C:\Program Files\PostgreSQL\18\bin\psql.exe"
if (-not (Test-Path $psql)) { throw "psql não encontrado em $psql" }

$geradoEm = (Get-Item $snapshot).LastWriteTime
if (-not $Force) {
    Write-Host "Isto APAGA todos os dados de '$banco' em $servidor`:$porta" -ForegroundColor Yellow
    Write-Host "e restaura o snapshot gerado em $geradoEm." -ForegroundColor Yellow
    $resposta = Read-Host "Digite o nome do banco ($banco) para confirmar"
    if ($resposta -ne $banco) { Write-Host "Cancelado."; exit 1 }
}

$env:PGPASSWORD = Valor 'spring.datasource.password'
try {
    # DROP SCHEMA em vez de só rodar o dump: assim some também o que existe hoje e não existia
    # quando o snapshot foi tirado — tabela nova criada pelo ddl-auto, por exemplo.
    & $psql -h $servidor -p $porta -U $usuario -d $banco -v ON_ERROR_STOP=1 `
        -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
    if ($LASTEXITCODE -ne 0) { throw "Falha ao limpar o schema (exit $LASTEXITCODE)" }

    & $psql -h $servidor -p $porta -U $usuario -d $banco -v ON_ERROR_STOP=1 -f $snapshot
    if ($LASTEXITCODE -ne 0) { throw "Falha ao restaurar o snapshot (exit $LASTEXITCODE)" }
} finally {
    $env:PGPASSWORD = $null
}

Write-Host "Banco restaurado ao snapshot de $geradoEm" -ForegroundColor Green
