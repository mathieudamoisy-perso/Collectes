#!/system/bin/sh
# Daemon emulateur : reset Collectes quand le flag de com.collectes.reset apparait.
# Sans uninstall : install -r + pm clear → l’icône reste à la même place sur le lanceur.
# Fonctionne sans adb root (image Play Store) via run-as sur l’app debug Reset.

RESET_PKG=com.collectes.reset
FLAG_REL=files/collectes-reset.flag
APK=/data/local/tmp/collectes-debug.apk
PIDFILE=/data/local/tmp/collectes-reset-daemon.pid
PKG=com.collectes.app
ACTIVITY=com.collectes.app/.MainActivity

echo $$ > "$PIDFILE"

while true; do
  if run-as "$RESET_PKG" cat "$FLAG_REL" >/dev/null 2>&1; then
    run-as "$RESET_PKG" rm -f "$FLAG_REL"
    if [ ! -f "$APK" ]; then
      echo "collectes-reset-daemon: staged APK missing" >&2
      sleep 1
      continue
    fi
    echo "collectes-reset-daemon: reset $(date)" >> /data/local/tmp/collectes-reset-daemon.log
    # Pas de pm uninstall : ça retire l’icône du home. -r + clear suffit.
    pm install -r -t "$APK"
    pm clear "$PKG" >/dev/null 2>&1
    am start -n "$ACTIVITY" >/dev/null 2>&1
  fi
  sleep 1
done
