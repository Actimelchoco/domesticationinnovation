param(
    [string]$ProjectRoot = "."
)

$ErrorActionPreference = "Stop"
$normalizedProjectRoot = $ProjectRoot.Trim().Trim('"')
Set-Location -LiteralPath $normalizedProjectRoot

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
