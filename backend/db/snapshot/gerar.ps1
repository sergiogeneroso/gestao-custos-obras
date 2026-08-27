# Regera o snapshot do banco (backend/db/snapshot/snapshot.sql) a partir do estado atual.
#
# O snapshot guarda schema E dados. Os arquivos de anexo e foto NÃO entram nele: eles vivem em
# backend/uploads/, que a restauração não toca — por isso as linhas de despesa_anexo, imovel_foto
# e *_documento continuam apontando para arquivos que seguem lá. Não apague backend/uploads/.
#
#   .\gerar.ps1

$ErrorActionPreference = 'Stop'

$raiz = Resolve-Path "$PSScriptRoot\..\..\.."
$propriedades = Get-Content "$raiz\backend\src\main\resources\application.properties"

function Valor($chave) {
    $linha = $propriedades | Where-Object { $_ -match "^$([regex]::Escape($chave))=" }
    $bruto = $linha -replace "^$([regex]::Escape($chave))=", ''
    # As propriedades usam ${VAR:padrao}; sem a env var, vale o padrão.
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

$pgDump = "C:\Program Files\PostgreSQL\18\bin\pg_dump.exe"
if (-not (Test-Path $pgDump)) { throw "pg_dump não encontrado em $pgDump" }

$destino = "$PSScriptRoot\snapshot.sql"
Write-Host "Gerando snapshot de $banco em $servidor`:$porta -> $destino"

$env:PGPASSWORD = Valor 'spring.datasource.password'
try {
    & $pgDump -h $servidor -p $porta -U $usuario -d $banco `
        --no-owner --no-privileges --encoding=UTF8 -f $destino
    if ($LASTEXITCODE -ne 0) { throw "pg_dump falhou (exit $LASTEXITCODE)" }
} finally {
    $env:PGPASSWORD = $null
}

$tamanho = [math]::Round((Get-Item $destino).Length / 1KB, 1)
Write-Host "Snapshot gerado: $tamanho KB" -ForegroundColor Green
