# Installs the "Reset Collectes" launcher icon on the emulator and starts the on-device daemon.
# Usage: powershell -File tools/install-reset-icon.ps1

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$resetDir = Join-Path $repoRoot "tools\collectes-reset"
$androidDir = Join-Path $repoRoot "android"
$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
$serial = if ($env:ANDROID_SERIAL) { $env:ANDROID_SERIAL } else { "emulator-5554" }
$sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"

# Same JBR as Android Studio (Gradle needs 17+)
if (-not $env:JAVA_HOME -or -not (Test-Path $env:JAVA_HOME)) {
    $jbr = "C:\Program Files\Android\Android Studio\jbr"
    if (Test-Path $jbr) { $env:JAVA_HOME = $jbr }
}

Set-Content -Path (Join-Path $resetDir "local.properties") -Value "sdk.dir=$($sdk -replace '\\','/')" -Encoding ASCII

Write-Host "Serial: $serial"
Write-Host "JAVA_HOME: $env:JAVA_HOME"
& $adb -s $serial root | Out-Null
& $adb -s $serial wait-for-device | Out-Null

$env:ANDROID_SERIAL = $serial
& (Join-Path $androidDir "gradlew.bat") -p $resetDir assembleDebug
if ($LASTEXITCODE -ne 0) { throw "Build reset helper failed" }

$apkDir = Join-Path $resetDir "build\outputs\apk\debug"
$apk = Get-ChildItem $apkDir -Filter "*.apk" -ErrorAction Stop | Select-Object -First 1 -ExpandProperty FullName
Write-Host "Installing reset icon: $apk"
& $adb -s $serial install -r -t $apk
if ($LASTEXITCODE -ne 0) { throw "install reset helper failed" }

& (Join-Path $PSScriptRoot "ensure-reset-daemon.ps1")
if ($LASTEXITCODE -ne 0) { throw "ensure reset daemon failed" }

Write-Host "OK - icon Reset Collectes installed + daemon on-device."
Write-Host "Each installDebug stages the Collectes APK and keeps the daemon alive."
