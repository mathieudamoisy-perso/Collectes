#!/system/bin/sh
# Daemon emulateur : reset Collectes quand le flag de com.collectes.reset apparait.
# Demarre via tools/install-reset-icon.ps1 ou :app:installDebug (pas de watcher PC).

FLAG=/data/data/com.collectes.reset/files/collectes-reset.flag
APK=/data/local/tmp/collectes-debug.apk
PIDFILE=/data/local/tmp/collectes-reset-daemon.pid
PKG=com.collectes.app
ACTIVITY=com.collectes.app/.MainActivity

echo $$ > "$PIDFILE"

while true; do
  if [ -f "$FLAG" ]; then
    rm -f "$FLAG"
    if [ ! -f "$APK" ]; then
      echo "collectes-reset-daemon: staged APK missing" >&2
      sleep 1
      continue
    fi
    pm uninstall "$PKG" >/dev/null 2>&1
    pm install -r -t "$APK"
    am start -n "$ACTIVITY" >/dev/null 2>&1
  fi
  sleep 1
done