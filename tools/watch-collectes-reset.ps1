# Watches the emulator for a 1-click reset from the "Reset Collectes" icon.
# Keep this window open while developing:
#   powershell -File tools/watch-collectes-reset.ps1

$ErrorActionPreference = "Continue"
$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
$serial = if ($env:ANDROID_SERIAL) { $env:ANDROID_SERIAL } else { "emulator-5554" }
$flag = "/data/data/com.collectes.reset/files/collectes-reset.flag"
$apk = "/data/local/tmp/collectes-debug.apk"

Write-Host "Collectes Reset Watcher"
Write-Host "  serial : $serial"
Write-Host "  Tap the red 'Reset Collectes' icon on the emulator."
Write-Host "  Ctrl+C to stop."
Write-Host ""

& $adb -s $serial wait-for-device | Out-Null
& $adb -s $serial root 2>$null | Out-Null
Start-Sleep -Milliseconds 500
& $adb -s $serial wait-for-device | Out-Null

while ($true) {
    $exists = & $adb -s $serial shell "if [ -f $flag ]; then echo YES; fi" 2>$null
    if ($exists -match "YES") {
        Write-Host "$(Get-Date -Format 'HH:mm:ss') Reset requested..."
        & $adb -s $serial shell "rm -f $flag" | Out-Null

        $hasApk = & $adb -s $serial shell "if [ -f $apk ]; then echo YES; fi" 2>$null
        if ($hasApk -notmatch "YES") {
            Write-Host "  ERROR: staged APK missing. Run :app:installDebug once, then retry."
            Start-Sleep -Seconds 1
            continue
        }

        & $adb -s $serial shell pm uninstall com.collectes.app 2>$null | Out-Null
        & $adb -s $serial shell pm install -r -t $apk
        if ($LASTEXITCODE -ne 0) {
            Write-Host "  ERROR: pm install failed"
        } else {
            & $adb -s $serial shell am start -n com.collectes.app/.MainActivity | Out-Null
            Write-Host "  OK - Collectes reinstalled fresh."
        }
    }
    Start-Sleep -Milliseconds 700
}
