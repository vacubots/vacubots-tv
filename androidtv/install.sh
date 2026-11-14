#!/bin/bash
# Install a VacuBots TV APK on the streamer.
# Usage:  bash install.sh ~/Downloads/VacuBotsTV.apk 192.168.1.50:PORT
# PORT is the "IP address & Port" shown under Developer options > Wireless debugging (it changes after a reboot).
# Pairing is only needed once; if connect fails, pair again with:  adb pair IP:PAIRPORT
ADB="$HOME/platform-tools/adb"; [ -x "$ADB" ] || ADB="$(command -v adb)"
APK="$1"; DEV="$2"
[ -f "$APK" ] && [ -n "$DEV" ] || { echo "Usage: bash install.sh <apk> <ip:port>"; exit 1; }
"$ADB" disconnect >/dev/null 2>&1          # drop the duplicate "_tcp" entries that cause "more than one device"
"$ADB" connect "$DEV" || exit 1
"$ADB" -s "$DEV" install -r "$APK" && echo "Installed. Open VacuBots on the streamer."
