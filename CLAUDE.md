# CLAUDE.md: setting up Gallery Home on someone's TV

You are helping the owner of an Android / Google TV build this launcher, personalise it, and install it on
their own TV from their computer. Read README.md for what the launcher does. This file is the procedure.

Work with the owner step by step. Some steps happen on the TV or in their browser and only they can do
them (sign-ins, accepting terms, pressing "Allow" on the TV); ask for those and wait.

## Ground rules

- Only install on a TV the owner owns or is allowed to modify. If it's a rented TV, say so and keep every
  change reversible (this app can always be uninstalled; switch Home back in Settings).
- **Never commit secrets.** `voice.json` (API keys, Spotify tokens) and `*.keystore` are git-ignored; keep
  it that way. Don't paste keys into chat, logs, commits or issues; read them from files.
- **Keep the owner's `debug.keystore`.** Android only accepts updates signed with the same key. Losing it
  means uninstalling the app (which deletes its data: history, paintings, voice.json on the TV) before the
  next install. Suggest the owner backs it up somewhere safe.
- Ask before anything destructive on the TV: uninstalling, disabling the stock launcher, `pm clear`.
- Don't type the owner's passwords anywhere. Sign-ins are theirs.

## 1. Computer prerequisites (macOS, Linux or WSL)

- A JDK, version 17 or newer (`java -version`).
- `adb` and the Android SDK pieces: `platform-tools`, `build-tools;35.0.0`, `platforms;android-34`.
  With the command-line tools installed:
  ```bash
  sdkmanager "platform-tools" "build-tools;35.0.0" "platforms;android-34"
  export ANDROID_HOME=<sdk path>      # build.sh also checks ANDROID_SDK_ROOT and ~/Library/Android/sdk
  ```
- Build once to prove the toolchain works: `./build.sh` must end with `built out/home.apk`.
  The first build creates `debug.keystore` (see ground rules).

## 2. Personalise before installing

These are the owner's details baked into the code. Ask the owner and change them:

| What | Where | Default |
|---|---|---|
| Profile auto-picked in apps that ask "Who's watching?" | `ProfileService.PROFILES` (Crunchyroll entry, `"Chirag"`) | the original owner's name; replace or remove |
| AirPlay receiver name (only if they use the AirScreen app) | `AirWake.NAME` | `"Hall's TV"` |
| Dock apps, in order | `MainActivity.FAVORITES` (package names) | Apple TV, Netflix, YouTube, JioHotstar, Crunchyroll, Spotify, Prime Video, TV Bro, Play Store |
| Region for title lookups | `voice.json` `region` | `IN` |
| Idle time before gallery mode | `MainActivity.IDLE_MS` | 60 s |

To find package names of the apps on their TV: `adb shell pm list packages -3`.
The package id `com.chirag.home` can stay as it is.

## 3. Prepare the TV

1. On the TV: Settings → System → About → press OK on **Android TV OS build** seven times to unlock
   Developer options. Then Settings → System → Developer options → turn on **USB debugging** and
   **Network debugging** (wording varies by TV; some call it "ADB over network").
2. Find the TV's IP: Settings → Network & Internet → the Wi-Fi network → IP address.
3. `adb connect <tv-ip>:5555`. The TV shows **Allow debugging?**: the owner ticks **Always allow** and
   presses Allow. `adb devices` should list it as `device`.

## 4. Voice search keys (`voice.json`)

Voice search, title lookups and music need a `voice.json` next to `build.sh`:

```bash
cp voice.example.json voice.json
```

- **Groq (required for voice):** the owner creates a free key at https://console.groq.com → API Keys, and
  puts it in `groq_key`. Free tier is enough for a household.
- **Speech replies:** `speak: true` uses the TV's built-in text-to-speech; nothing to set up.

### Spotify (so music plays in the Spotify app, using their taste)

Without this, music requests play on YouTube. On Android TV, Spotify only starts playing when it's handed
a specific track, and finding that track needs Spotify's Web API. The owner does these steps themselves
in their browser (it needs their Spotify login and acceptance of Spotify's developer terms):

1. Go to https://developer.spotify.com/dashboard, sign in with the **same Spotify account the TV uses**.
2. **Create app**:
   - App name: anything, e.g. `Gallery Home TV`
   - Redirect URI: exactly `http://127.0.0.1:8888/callback` (click Add)
   - APIs used: tick **Web API**
   - Tick the terms box, Save.
   If **Create app** is greyed out, the owner usually still has to verify their email address on the
   Spotify account, or accept the developer terms first.
3. Open the app → **Settings**: copy the **Client ID**, click **View client secret** and copy it. The
   owner pastes both into `voice.json` as `spotify_client_id` and `spotify_client_secret`
   (have them edit the file directly; don't ask for the values in chat).
4. **Taste access (one-time sign-in):** run
   ```bash
   python3 tools/spotify-auth/auth.py
   ```
   It opens Spotify's consent page; the owner approves (read-only: top artists/tracks, recently played,
   saved songs, playlists). It saves `spotify_refresh_token` into `voice.json` and prints their top
   artists as a check. The launcher refreshes its access token by itself after that. The refresh token
   has a limited life (the app's Settings page shows "Refresh Token Lifetime", 180 days at the time of
   writing): when taste features stop working, run `auth.py` again and re-push `voice.json`.
5. The app stays in Spotify's **Development mode**, which is fine for one household: the account that
   created the app can use it. To let other Spotify accounts use it, add them under the app's
   **User Management**.

## 5. Build and install

```bash
./build.sh
./install.sh <tv-ip>:5555
```

`install.sh` installs the APK, makes it the Home app, enables its accessibility services (Home-button
handling, profile auto-pick) and notification access (watch history, Spotify widget), grants the
microphone, and pushes `voice.json`. Then push the paintings (about 50 MB, once):

```bash
adb -s <tv-ip>:5555 shell mkdir -p /sdcard/Android/data/com.chirag.home/files/art
adb -s <tv-ip>:5555 push paintings/. /sdcard/Android/data/com.chirag.home/files/art/
```

Press Home on the remote. On first use of "close apps after a minute at Home" the TV asks **Allow
debugging?** once more (the launcher's own key): the owner allows it.

To update later: edit, `./build.sh`, `./install.sh <tv-ip>:5555` again. Same keystore, data is kept.

## 6. Check it works

- Home shows a painting, clock, Continue watching (fills up as they watch), dock.
- Leave the remote for 60 s: only the painting and its label remain (gallery mode); any key wakes it.
- Press the remote's mic button and say "play some lo-fi" or a show name. Logs:
  `adb -s <tv-ip>:5555 logcat -s Voice Home WatchHistory`.
- Voice test harness on the computer (uses `voice.json` keys, real APIs):
  `tools/voice-test/run.sh` (and `--selftest` for offline checks).

## Troubleshooting

| Symptom | Fix |
|---|---|
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Signed with a different key. Use the original `debug.keystore`, or (asking first) `adb uninstall com.chirag.home` and install again: this wipes the app's data. |
| The TV's own launcher keeps coming back | `adb shell cmd role add-role-holder --user 0 android.app.role.HOME com.chirag.home 0`. Some TVs also need the stock launcher disabled (`pm disable-user --user 0 <package>`); ask first, it's reversible with `pm enable`. |
| Mic button opens Google Assistant instead | The TV routes the mic to Assistant. Disabling the Assistant app (`com.google.android.katniss`) hands it to the launcher; ask first, reversible. |
| Voice says it can't look things up | Check `groq_key` in `voice.json` on the TV: `adb shell cat /sdcard/Android/data/com.chirag.home/files/voice.json` (careful: shows keys). |
| Music plays on YouTube instead of Spotify | Spotify keys missing or wrong in `voice.json`, or the TV's Spotify is signed in to a different account than the developer app. |
| Continue watching is empty | Notification access: `adb shell cmd notification allow_listener com.chirag.home/com.chirag.home.WatchListener`. |
| Network debugging turns off after a reboot | Some TVs reset it; turn it on again in Developer options. |

## Going back

Settings → Apps → Default apps → Home app → the TV's original launcher. To remove completely:
`adb uninstall com.chirag.home`.
