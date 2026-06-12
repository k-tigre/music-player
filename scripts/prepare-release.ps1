# Bump version, write Play release notes, and finalize CHANGELOG for one app release.
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [ValidateSet("music", "book")]
    [string]$App,

    [Parameter(Mandatory = $true, Position = 1)]
    [string]$Version
)
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root
if ($Version -notmatch '^\d+\.\d+\.\d+$') {
    Write-Error "Invalid version: '$Version' (expected X.Y.Z)"
}
python scripts/changelog_tool.py bump-version --app $App --version $Version
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
python scripts/changelog_tool.py write-play-notes --app $App --version $Version
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
python scripts/changelog_tool.py finalize --app $App --version $Version
exit $LASTEXITCODE
