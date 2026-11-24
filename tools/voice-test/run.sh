#!/bin/bash
# Runs the voice pipeline on the Mac against the real APIs: tools/voice-test/run.sh [query filter]
# Keys: $VOICE_JSON (default ../tv-launcher/voice.json, then ./voice.json) or $GROQ_KEY. Never commit a key.
set -euo pipefail
cd "$(dirname "$0")/../.."
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
JAR="$SDK/platforms/android-${PLATFORM:-34}/android.jar"
OUT=tools/voice-test/out/classes
rm -rf "$OUT" && mkdir -p "$OUT"
# The voice classes are plain Java; android.jar is only needed to compile the few Android references.
javac -nowarn -encoding UTF-8 -cp "$JAR" -sourcepath src -d "$OUT" \
  tools/voice-test/VoiceTest.java src/com/chirag/home/VoiceAgent.java 2>&1 | grep -v '^Note:' || true
java -cp "$OUT:$JAR" -Dvoicetest.dir=tools/voice-test com.chirag.home.VoiceTest "$@"
