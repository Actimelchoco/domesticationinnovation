param(
    [string]$ProjectRoot = "."
)

$ErrorActionPreference = "Stop"
Set-Location $ProjectRoot

if (Get-Command python -ErrorAction SilentlyContinue) {
    python scripts/export_class_weights_overview.py import
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
    python scripts/export_class_weights_overview.py export
    exit $LASTEXITCODE
}

if (Get-Command py -ErrorAction SilentlyContinue) {
    py -3 scripts/export_class_weights_overview.py import
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
    py -3 scripts/export_class_weights_overview.py export
    exit $LASTEXITCODE
}

throw "Python was not found in PATH."
