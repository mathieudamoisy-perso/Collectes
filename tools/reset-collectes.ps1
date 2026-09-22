# Full Collectes reset on emulator from PC: installDebug + clear + start (no uninstall).
# Usage: powershell -File tools/reset-collectes.ps1

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$androidDir = Join-Path $repoRoot "android"
$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
$serial = if ($env:ANDROID_SERIAL) { $env:ANDROID_SERIAL } else { "emulator-5554" }

Write-Host "Reset Collectes on $serial"
& $adb -s $serial wait-for-device | Out-Null

$env:ANDROID_SERIAL = $serial
Push-Location $androidDir
try {
    & .\gradlew.bat :app:installDebug
    if ($LASTEXITCODE -ne 0) { throw "installDebug failed" }
} finally {
    Pop-Location
}

# clear après install -r : données à zéro, icône home inchangée
& $adb -s $serial shell pm clear com.collectes.app | Out-Null
& $adb -s $serial shell am force-stop com.collectes.app
& $adb -s $serial shell am start -n com.collectes.app/.MainActivity
Write-Host "OK - Collectes reinstalled fresh (launcher icon kept)."
