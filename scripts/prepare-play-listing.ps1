# Sync Play listing texts and rebuild marketing screenshots for one or both apps.
param(
    [Parameter(Mandatory = $false)]
    [ValidateSet("music", "book", "all")]
    [string]$App = "all",

    [Parameter(Mandatory = $false)]
    [switch]$TextsOnly
)
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

$python = if (Get-Command python -ErrorAction SilentlyContinue) { "python" } else { "python3" }
& $python scripts/sync-play-listing-texts.py --app $App
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if ($TextsOnly) {
    Write-Host "Listing texts updated (screenshots skipped)."
    exit 0
}

$gradle = if (Test-Path ".\gradlew.bat") { ".\gradlew.bat" } else { ".\gradlew" }

function Invoke-Marketing {
    param([string]$Module)
    & $gradle ":$Module`:recordMarketingScreenshots"
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

switch ($App) {
    "music" { Invoke-Marketing "apps:PlayerApp" }
    "book" { Invoke-Marketing "apps:AudioBook" }
    "all" {
        Invoke-Marketing "apps:PlayerApp"
        Invoke-Marketing "apps:AudioBook"
    }
}

Write-Host "Play listing assets updated under apps/*/src/main/play/listings/"
