# Upload Play Store listing only (texts + graphics). Separate from AAB release.
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("music", "book", "all")]
    [string]$App
)
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

if (-not $env:ANDROID_PUBLISHER_CREDENTIALS) {
    Write-Error "Set ANDROID_PUBLISHER_CREDENTIALS (Play service account JSON)."
}
if (-not $env:PLAY_CONTACT_EMAIL) {
    Write-Error "Set PLAY_CONTACT_EMAIL (Play Console developer contact)."
}

$gradle = if (Test-Path ".\gradlew.bat") { ".\gradlew.bat" } else { ".\gradlew" }

function Publish-Listing {
    param([string]$Module)
    & $gradle ":$Module`:publishPlayListing"
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

switch ($App) {
    "music" { Publish-Listing "apps:PlayerApp" }
    "book" { Publish-Listing "apps:AudioBook" }
    "all" {
        Publish-Listing "apps:PlayerApp"
        Publish-Listing "apps:AudioBook"
    }
}
