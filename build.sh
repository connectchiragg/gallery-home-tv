#!/bin/bash
# Builds out/home.apk with the plain Android SDK tools: no Gradle, no Android Studio.
# Needs: JDK 17+ (javac, keytool), Android SDK with build-tools 35 and platform android-34.
set -euo pipefail
cd "$(dirname "$0")"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
BT="$SDK/build-tools/${BUILD_TOOLS:-35.0.0}"
JAR="$SDK/platforms/android-${PLATFORM:-34}/android.jar"
[ -f "$JAR" ] || { echo "android.jar not found at $JAR (set ANDROID_HOME)"; exit 1; }
rm -rf out && mkdir -p out/res out/gen out/classes

# 1. resources -> compiled resources + R.java
"$BT/aapt2" compile --dir res -o out/res/
"$BT/aapt2" link -I "$JAR" --manifest AndroidManifest.xml -o out/unsigned.apk --java out/gen out/res/*.flat --auto-add-overlay

# 2. Java -> classes -> dex
javac --release 8 -nowarn -Xmaxerrs 1000 -XDshould-stop.ifError=FLOW -encoding UTF-8 -cp "$JAR" -d out/classes $(find src out/gen -name '*.java') 2>&1 | grep -v '^warning: \[options\]' || true
[ -n "$(find out/classes -name '*.class' | head -1)" ] || { echo "javac failed"; exit 1; }
(cd out/classes && jar cf ../classes.jar .)
"$BT/d8" --min-api 28 --lib "$JAR" --output out out/classes.jar
(cd out && zip -q -j unsigned.apk classes.dex)

# 3. align + sign (a debug key is created on first build; keep it to update the app in place)
"$BT/zipalign" -f 4 out/unsigned.apk out/aligned.apk
KS="${KEYSTORE:-debug.keystore}"
[ -f "$KS" ] || keytool -genkeypair -keystore "$KS" -storepass android -keypass android -alias androiddebugkey \
    -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US" >/dev/null 2>&1
"$BT/apksigner" sign --ks "$KS" --ks-pass pass:android --key-pass pass:android --out out/home.apk out/aligned.apk
echo "built out/home.apk"
