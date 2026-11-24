# Gallery Home: an art-first launcher for Google TV

A replacement Home screen for Android / Google TV that turns the TV into a slowly changing gallery of
public-domain paintings, with a quiet glass interface on top: a big clock, your **Continue watching**
row, your apps, a Spotify now-playing widget and Hinglish-friendly voice search.

It is a plain-Java Android app (no Gradle, no libraries) built for a Skyworth Google TV on Android 14,
and it is meant to be read, changed and rebuilt.

---

## What it does

| | |
|---|---|
| **Painting wall** | 212 public-domain European paintings (Monet, Van Gogh, Vermeer, Turner, Friedrich and more) shuffle every 30 s with a 2.5 s cross-fade. The label under the clock fades with them. Press OK on the label to hide the launcher and read a short history of the painting. |
| **Liquid-glass UI** | Buttons, cards and the dock refract the painting behind them (a one-time per-painting computation, nothing per frame). Circular app tiles, subtle focus, wrap-around rows. |
| **Continue watching** | Built from the TV's own media sessions (YouTube, Netflix, Prime Video, Apple TV, JioHotstar, Crunchyroll): last 30 days, one card per series (newest episode), episode stills from TVmaze where available, exact-second resume where the app allows it. Long-press OK on a card to remove it. |
| **Spotify widget** | A small glass capsule under the painting label while Spotify is playing or paused: art, title, previous / play-pause / next and a clear button. |
| **Dark screen** | The moon button fades everything to black except a dimmed Spotify widget: music with a black TV. |
| **Voice search** | Press the remote's mic: Groq Whisper speech-to-text, then a small agent (understand, route, resolve, score, decide) using JustWatch, YouTube and your watch history. Confident answers auto-play after 2 s (Back cancels), otherwise up to 3 result cards. Falls back to YouTube when nothing on your apps matches. |
| **Inputs and PlayStation** | Inputs screen for HDMI sources; a PlayStation button switches to the console's input. |
| **AirPlay helper** | A tiny listener that wakes the AirScreen receiver app only when a Mac or iPhone wants to mirror, and closes it after. |
| **Housekeeping** | One minute after you return Home, apps you left are closed (anything playing is spared) through the TV's own network-adb, which you approve once on the TV. |
| **Screensaver** | The same paintings as a Daydream. |

## Repository layout

```
AndroidManifest.xml
build.sh              # aapt2 + javac + d8 + apksigner, no Gradle
install.sh            # installs over network adb and makes it the Home app
voice.example.json    # template for the voice-search keys (never commit the real one)
paintings/            # 212 public-domain paintings + catalog.json (title, artist, year, museum, about)
res/                  # 13 built-in paintings, strings, styles, accessibility configs
src/com/chirag/home/
  MainActivity, Ui, Wall, Glass, Lens, Orb, Edge, Smoke, Glyph   home screen and the glass look
  Art, ArtDream                                                  painting rotation and screensaver
  HistoryDb, WatchListener, WatchRule, ArtStore, YtLookup        watch history and Continue watching
  NowPlaying                                                     Spotify widget
  VoiceSearchActivity, VoiceCore, VoiceClient, VoiceAgent,
  YouTubeSource, VoiceGlow, VoiceChime, VoiceArt, VoiceJson      voice search
  HistoryActivity, InputsActivity, Inputs, Ps5, PsMark           other screens
  HomeKeyService, ProfileService, ScreenRead(er)                 accessibility helpers
  AirWake, AirRelay, AirForward                                  AirPlay wake-on-demand
  SelfAdb                                                        tiny adb client for closing apps
```

## Build

You need a JDK (17 or newer) and the Android SDK command-line pieces: `build-tools;35.0.0` and
`platforms;android-34`.

```bash
export ANDROID_HOME=~/Library/Android/sdk   # or wherever your SDK is
./build.sh                                  # -> out/home.apk
```

The first build creates a `debug.keystore`. Keep it: Android only accepts updates signed with the
same key.

## Install on a TV

1. On the TV: Settings → System → About → press OK on *Android TV OS build* 7 times, then turn on
   **Network debugging** in Developer options.
2. From your computer:

```bash
./install.sh <tv-ip>:5555
```

3. Accept the "Allow debugging?" prompt on the TV (tick *Always allow*). Press Home.
4. Optional, for the painting collection: `adb push paintings/. /sdcard/Android/data/com.chirag.home/files/art/`
5. Optional, for voice search: copy `voice.example.json` to `voice.json`, add your free
   [Groq](https://console.groq.com) key, then run `./install.sh` again (it pushes `voice.json`).

To go back to the stock launcher: Settings → Apps → Default apps → Home app.

## Making it yours

- **Paintings:** add JPEGs (1280×720) to `paintings/` and entries to `catalog.json`; rotation speed is
  `Art.PERIOD_MS`, fade length `Art.FADE_MS`.
- **Dock apps:** `MainActivity.FAVORITES`.
- **Gallery mode:** after `MainActivity.IDLE_MS` (60 s) with no remote key, everything but the painting
  and its label fades out; any key brings it back.
- **Glass look:** constants at the top of `Glass.java` (bezel width, depth, refraction index).
- **Profile auto-pick:** `ProfileService.PROFILES` picks a named profile in apps that ask "Who's
  watching?". Put your own profile name there.
- **AirPlay name:** `AirWake.NAME`.
- **Voice:** models and thresholds in `voice.json` (see `voice.example.json`).

## A note on this source

The original source was lost, so this code was **recovered by decompiling the last installed build**
with [jadx](https://github.com/skylot/jadx) and then fixed by hand until it compiled again. Logic and
names are intact, but the original comments are gone and some helper methods still carry
decompiler-generated names such as `m12lambda$next$0$...`. Cleaning those up is a good first
contribution.

## Privacy

Nothing leaves the TV except what a feature needs: voice audio and queries go to Groq when you use
voice search, title lookups go to JustWatch, YouTube and TVmaze. Watch history stays on the TV. There
is no analytics.

## Credits

Paintings: public domain, via Wikimedia Commons (see `paintings/catalog.json`). Liquid-glass
refraction model after kube.io's "Liquid Glass in the browser". PlayStation symbol path from Wikimedia.

## License

MIT, see [LICENSE](LICENSE). Use it only on TVs you own or may modify.
