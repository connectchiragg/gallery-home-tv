#!/bin/bash
# Installs out/home.apk on a TV over network adb and makes it the Home app.
# Usage: ./install.sh 192.168.0.8:5555
set -euo pipefail
cd "$(dirname "$0")"
D="${1:?usage: ./install.sh <tv-ip>:5555}"
a() { adb -s "$D" shell "$@"; }
adb connect "$D" >/dev/null
adb -s "$D" install -r out/home.apk
a cmd role add-role-holder --user 0 android.app.role.HOME com.chirag.home 0
# accessibility services: Home-button handling, profile auto-pick, screen reading (optional)
a settings put secure enabled_accessibility_services com.chirag.home/com.chirag.home.HomeKeyService:com.chirag.home/com.chirag.home.ProfileService
a settings put secure accessibility_enabled 1
# notification access: reads media sessions for Continue watching and the now-playing widget
a cmd notification allow_listener com.chirag.home/com.chirag.home.WatchListener
a pm grant com.chirag.home android.permission.RECORD_AUDIO || true
[ -f voice.json ] && adb -s "$D" push voice.json /sdcard/Android/data/com.chirag.home/files/voice.json >/dev/null && echo "pushed voice.json"
echo "installed; press Home on the TV"
