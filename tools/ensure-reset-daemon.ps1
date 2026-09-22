# Push + start (or restart) the on-device Collectes reset daemon.
# Usage: powershell -File tools/ensure-reset-daemon.ps1
# Called by install-reset-icon.ps1 and :app:installDebug.

$ErrorActionPreference = "Continue"
$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
$serial = if ($env:ANDROID_SERIAL) { $env:ANDROID_SERIAL } else { "emulator-5554" }
$localScript = Join-Path $PSScriptRoot "collectes-reset-daemon.sh"
$remoteScript = "/data/local/tmp/collectes-reset-daemon.sh"
$pidFile = "/data/local/tmp/collectes-reset-daemon.pid"

if (-not (Test-Path $localScript)) {
    throw "Daemon script missing: $localScript"
}

function Invoke-Adb {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Args)
    & $adb -s $serial @Args
    return $LASTEXITCODE
}

Invoke-Adb root 2>$null | Out-Null
Invoke-Adb wait-for-device | Out-Null

# Ensure LF line endings on device (Windows checkout may be CRLF).
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
$lfText = [System.IO.File]::ReadAllText($localScript) -replace "`r`n", "`n" -replace "`r", "`n"
$tmpLf = Join-Path $env:TEMP "collectes-reset-daemon.sh"
[System.IO.File]::WriteAllText($tmpLf, $lfText, $utf8NoBom)

Invoke-Adb push $tmpLf $remoteScript | Out-Null
Invoke-Adb shell "chmod 755 $remoteScript" | Out-Null

$oldPid = ((& $adb -s $serial shell "cat $pidFile 2>/dev/null") | Out-String).Trim()
if ($oldPid -match '^\d+$') {
    Invoke-Adb shell "kill $oldPid 2>/dev/null" | Out-Null
}
Invoke-Adb shell "pkill -f collectes-reset-daemon.sh 2>/dev/null" | Out-Null
Start-Sleep -Milliseconds 300

# Detach from adb shell session so the daemon survives.
Invoke-Adb shell "toybox nohup sh $remoteScript >/data/local/tmp/collectes-reset-daemon.log 2>&1 </dev/null &" | Out-Null
if ($LASTEXITCODE -ne 0) {
    Invoke-Adb shell "sh -c 'nohup sh $remoteScript >/data/local/tmp/collectes-reset-daemon.log 2>&1 </dev/null &'" | Out-Null
}
Start-Sleep -Milliseconds 700

$pg = ((& $adb -s $serial shell "pgrep -f collectes-reset-daemon.sh") | Out-String).Trim()
$pidFromFile = ((& $adb -s $serial shell "cat $pidFile 2>/dev/null") | Out-String).Trim()
if (-not $pg -and $pidFromFile -notmatch '^\d+$') {
    $log = ((& $adb -s $serial shell "cat /data/local/tmp/collectes-reset-daemon.log 2>/dev/null") | Out-String).Trim()
    Write-Error "Reset daemon failed to start. log=$log"
    exit 1
}

Write-Host "Reset daemon running on $serial (pid $(if ($pg) { $pg } else { $pidFromFile }))"
exit 0
