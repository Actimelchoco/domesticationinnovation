param(
    [string]$ProjectRoot = ".",
    [switch]$Refresh
)

$ErrorActionPreference = "Stop"
$normalizedProjectRoot = $ProjectRoot.Trim().Trim('"')
Set-Location -LiteralPath $normalizedProjectRoot

$sourceJson = Join-Path $normalizedProjectRoot "src\main\resources\data\domesticationinnovation\tameslevel\class_weights.json"
$exportScript = Join-Path $normalizedProjectRoot "scripts\export_class_weights_overview.py"
$buildScript = Join-Path $normalizedProjectRoot "scripts\build_class_weights_workbook.ps1"
$csvDir = Join-Path $normalizedProjectRoot "docs\class_weights_overview"
$workbookPath = Join-Path $csvDir "class_weights_overview.xlsm"
$csvPaths = @(
    (Join-Path $csvDir "meta.csv"),
    (Join-Path $csvDir "categories.csv"),
    (Join-Path $csvDir "base_stats.csv"),
    (Join-Path $csvDir "attributes.csv"),
    (Join-Path $csvDir "abilities.csv"),
    (Join-Path $csvDir "class_risk.csv")
)

function Test-UpToDate {
    param(
        [string]$Workbook,
        [string[]]$CsvFiles,
        [string[]]$Inputs
    )

    if (-not (Test-Path -LiteralPath $Workbook)) {
        return $false
    }
    foreach ($csv in $CsvFiles) {
        if (-not (Test-Path -LiteralPath $csv)) {
            return $false
        }
    }

    $workbookTime = (Get-Item -LiteralPath $Workbook).LastWriteTimeUtc
    foreach ($csv in $CsvFiles) {
        $csvTime = (Get-Item -LiteralPath $csv).LastWriteTimeUtc
        if ($csvTime -gt $workbookTime) {
            return $false
        }
    }

    foreach ($input in $Inputs) {
        if (-not (Test-Path -LiteralPath $input)) {
            return $false
        }
        $inputTime = (Get-Item -LiteralPath $input).LastWriteTimeUtc
        if ($inputTime -gt $workbookTime) {
            return $false
        }
    }

    return $true
}

if (-not $Refresh -and (Test-UpToDate -Workbook $workbookPath -CsvFiles $csvPaths -Inputs @($sourceJson, $exportScript, $buildScript))) {
    Start-Process -FilePath $workbookPath
    return
}

if (Get-Command python -ErrorAction SilentlyContinue) {
    python scripts/export_class_weights_overview.py
    if ($LASTEXITCODE -ne 0) {
        throw "CSV export failed."
    }
} elseif (Get-Command py -ErrorAction SilentlyContinue) {
    py -3 scripts/export_class_weights_overview.py
    if ($LASTEXITCODE -ne 0) {
        throw "CSV export failed."
    }
} else {
    throw "Python was not found in PATH."
}

powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build_class_weights_workbook.ps1 -Open
