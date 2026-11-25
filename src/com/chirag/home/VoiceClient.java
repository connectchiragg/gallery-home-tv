package com.chirag.home;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
final class VoiceClient {
    private static final String BOUNDARY = "----voice7MA4YWxkTrZu0gW";
    private static final String INT = "{\"type\":\"integer\"}";
    static final String JW_QUERY = "query S($c: Country!, $l: Language!, $f: TitleFilter){ popularTitles(country:$c, first:8, filter:$f){ edges{ node{ id objectType content(country:$c, language:$l){ title originalTitle originalReleaseYear externalIds{ imdbId tmdbId } posterUrl backdrops{ backdropUrl } } offers(country:$c, platform:WEB){ monetizationType presentationType standardWebURL deeplinkURL(platform:ANDROID_TV) package{ packageId clearName technicalName } } ... on Show { seasons(sortDirection:ASC){ id totalEpisodeCount content(country:$c, language:$l){ seasonNumber } } } } } } }";
    static final String JW_SEASON_QUERY = "query E($id: ID!, $c: Country!, $l: Language!, $n: Int, $o: Int){ node(id:$id){ id ... on Season { totalEpisodeCount content(country:$c, language:$l){ seasonNumber } episodes(sortDirection:ASC, limit:$n, offset:$o){ id content(country:$c, language:$l){ title episodeNumber seasonNumber isReleased } offers(country:$c, platform:WEB){ monetizationType presentationType standardWebURL deeplinkURL(platform:ANDROID_TV) webDeeplinkURL: deeplinkURL(platform:WEB) package{ packageId clearName technicalName } } } } } }";
    static final String JW_URL = "https://apis.justwatch.com/graphql";
    static final String ROUTER_SYSTEM = "You route voice requests on an Android TV in India. The user speaks English, Hindi or Hinglish (chalao / lagao = play, dikhao = show, naya / latest = newest, agla = next, phir se / dobara = again, gaana = song). Call exactly one tool. Speech recognition may misspell names: correct an obvious misspelling of a well-known name, otherwise keep the user's words. Never guess facts: do not decide which app has a title, and do not replace a description (\"the new Shah Rukh Khan movie\") with a title you think it is - pass the words on. Set app only when the user named one. Use resume when the user means something from the history without naming a new title (continue, resume, next episode, play it again, that video I was watching). Anything to listen to is play_music: songs, singers, bands, albums, playlists, and genres or moods such as lo-fi, sad songs, 90s Bollywood, chill music for studying, workout music, bhajans; a well-known song title alone (Kesariya, Shape of You, Tum Hi Ho) is music, not a film. Films, series, anime and YouTube videos or creators are not music. Hinglish: gaana / gaane = song(s), purane = old, sunao / bajao = play.";
    private static final String STR = "{\"type\":\"string\"}";
    final Config cfg;
    final Http http;
    final StringBuilder trace = new StringBuilder();
    static final Http REAL = new Http() { // from class: com.chirag.home.VoiceClient.1
        @Override // com.chirag.home.VoiceClient.Http
        public Resp call(String str, String str2, Map<String, String> map, byte[] bArr, int i) throws IOException {
            HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str2).openConnection();
            try {
                httpURLConnection.setConnectTimeout(6000);
                httpURLConnection.setReadTimeout(i);
                httpURLConnection.setRequestMethod(str);
                httpURLConnection.setRequestProperty("Accept", "application/json");
                httpURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0");
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    httpURLConnection.setRequestProperty(entry.getKey(), entry.getValue());
                }
                if (bArr != null) {
                    httpURLConnection.setDoOutput(true);
                    httpURLConnection.setFixedLengthStreamingMode(bArr.length);
                    OutputStream outputStream = httpURLConnection.getOutputStream();
                    try {
                        outputStream.write(bArr);
                        outputStream.close();
                    } catch (Throwable th) {
                        outputStream.close();
                        throw th;
                    }
                }
                int responseCode = httpURLConnection.getResponseCode();
                InputStream errorStream = responseCode >= 400 ? httpURLConnection.getErrorStream() : httpURLConnection.getInputStream();
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                if (errorStream != null) {
                    try {
                        byte[] bArr2 = new byte[8192];
                        while (true) {
                            int i2 = errorStream.read(bArr2);
                            if (i2 <= 0) {
                                break;
                            }
                            byteArrayOutputStream.write(bArr2, 0, i2);
                        }
                        errorStream.close();
                    } catch (Throwable th2) {
                        errorStream.close();
                        throw th2;
                    }
                }
                Resp resp = new Resp(responseCode, byteArrayOutputStream.toString("UTF-8"));
                httpURLConnection.disconnect();
                return resp;
            } catch (Throwable th3) {
                httpURLConnection.disconnect();
                throw th3;
            }
        }
    };
    private static final char[] B64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
    static final String ROUTER_TOOLS = "[" + tool("play_title", "Play or find a film, series or anime on a streaming app (Netflix, Prime Video, JioHotstar, Apple TV, Crunchyroll).", "\"title\":{\"type\":\"string\"},\"season\":{\"type\":\"integer\"},\"episode\":{\"type\":\"integer\"},\"app\":{\"type\":\"string\"}", "title") + "," + tool("youtube_latest", "Play the newest upload of a YouTube channel or creator.", "\"channel\":{\"type\":\"string\"}", "channel") + "," + tool("youtube_channel", "Open a YouTube channel's page.", "\"channel\":{\"type\":\"string\"}", "channel") + "," + tool("youtube_search", "Play a specific YouTube video, or search YouTube for a topic.", "\"query\":{\"type\":\"string\"}", "query") + "," + tool("play_music", "Play music: a song (even named without the word song), an artist, an album, a playlist, or a genre, mood, era, language or activity. query = a short phrase a music app's search would match well, in English letters: a song or artist name as is (\"kesariya\", \"arijit singh\", \"coldplay\"); for a genre or mood a playlist-style phrase (\"lofi beats\", \"sad hindi songs\", \"90s bollywood hits\", \"chill study music\", \"workout music\", \"old hindi songs\").", "\"query\":{\"type\":\"string\"},\"kind\":{\"type\":\"string\",\"description\":\"song, artist, album, playlist, genre or mood\"},\"artist\":{\"type\":\"string\"},\"seed\":{\"type\":\"string\",\"description\":\"for a genre, mood or era only: one popular song that fits it well, as title - artist\"},\"app\":{\"type\":\"string\"}", "query") + "," + tool("resume", "Continue something from the watch history below: continue = carry on with it, next_episode = the episode after the one last watched, again = play the last thing again. reference = the user's words for which item, empty if none.", "\"what\":{\"type\":\"string\",\"enum\":[\"continue\",\"next_episode\",\"again\"]},\"reference\":{\"type\":\"string\"}", "what") + "," + tool("open_app", "Just open an app.", "\"app\":{\"type\":\"string\"}", "app") + "]";

    interface Http {
        Resp call(String str, String str2, Map<String, String> map, byte[] bArr, int i) throws IOException;
    }

    static final class VoiceException extends Exception {
        VoiceException(String str) {
            super(str);
        }
    }

    static final class Config {
        List<String> sttOrder = new ArrayList(Arrays.asList("groq", "sarvam", "deepgram", "gemini"));
        String groqKey = "";
        String groqSttModel = "whisper-large-v3";
        String groqLlmModel = "openai/gpt-oss-20b";
        String sarvamKey = "";
        String sarvamModel = "saaras:v4";
        String sarvamMode = "translit";
        String deepgramKey = "";
        String deepgramModel = "nova-3";
        String geminiKey = "";
        String geminiModel = "gemini-3.5-flash-lite";
        String tmdbKey = "";
        /** Spotify Web API app (free, developer.spotify.com): lets music play on Spotify by finding the track. */
        String spotifyId = "";
        String spotifySecret = "";
        /** Spotify user sign-in (tools/spotify-auth/auth.py): read-only access to the owner's listening, for taste. */
        String spotifyRefresh = "";
        /** Groq model that judges which of the owner's songs fit a genre or mood request. */
        String groqTasteModel = "openai/gpt-oss-120b";
        /** voice.json itself (to save a rotated Spotify refresh token) and a folder for small caches; null = none. */
        File file;
        File dataDir;
        boolean justwatch = false;
        boolean llm = true;
        double autoPlay = 0.8d;
        boolean webSearch = false;
        String groqRouterModel = "openai/gpt-oss-20b";
        String groqWebModel = "openai/gpt-oss-120b";
        boolean screenRead = false;
        String groqVisionModel = "qwen/qwen3.8-27b";
        String capture = "auto";
        String language = "";
        String region = "IN";
        int maxResults = 3;
        /** Speak a short reply for each outcome (voice.json "speak", default true). */
        boolean speak = true;
        /** Packages installed on this TV (filled in by the activity); null = unknown, assume all are. */
        java.util.Set<String> installed;
        String sttPrompt = "Movie and web series names, Hindi names written in English letters: Pushpa 2, Panchayat, Mirzapur, Stree 2, Jawan, Kantara, Dangal, Sholay, Animal, The Family Man.";
        final Map<String, String> searchLinks = new LinkedHashMap();

        Config() {
        }

        static Config parse(String str) throws VoiceException {
            Object objTryParse = VoiceJson.tryParse(str);
            if (!(objTryParse instanceof Map)) {
                throw new VoiceException("voice.json is not valid JSON");
            }
            Config config = new Config();
            List<Object> list = VoiceJson.list(objTryParse, "stt_order");
            if (!list.isEmpty()) {
                config.sttOrder.clear();
                Iterator<Object> it = list.iterator();
                while (it.hasNext()) {
                    config.sttOrder.add(String.valueOf(it.next()).toLowerCase());
                }
            }
            config.groqKey = key(objTryParse, "groq_key");
            config.sarvamKey = key(objTryParse, "sarvam_key");
            config.deepgramKey = key(objTryParse, "deepgram_key");
            config.geminiKey = key(objTryParse, "gemini_key");
            config.tmdbKey = key(objTryParse, "tmdb_key");
            config.spotifyId = key(objTryParse, "spotify_client_id");
            config.spotifySecret = key(objTryParse, "spotify_client_secret");
            config.spotifyRefresh = key(objTryParse, "spotify_refresh_token");
            config.groqTasteModel = opt(objTryParse, "groq_taste_model", config.groqTasteModel);
            config.groqSttModel = opt(objTryParse, "groq_stt_model", config.groqSttModel);
            config.groqLlmModel = opt(objTryParse, "groq_llm_model", config.groqLlmModel);
            config.sarvamModel = opt(objTryParse, "sarvam_model", config.sarvamModel);
            config.sarvamMode = opt(objTryParse, "sarvam_mode", config.sarvamMode);
            config.deepgramModel = opt(objTryParse, "deepgram_model", config.deepgramModel);
            config.geminiModel = opt(objTryParse, "gemini_model", config.geminiModel);
            config.justwatch = VoiceJson.bool(objTryParse, config.justwatch, "justwatch");
            config.llm = VoiceJson.bool(objTryParse, config.llm, "llm");
            double dNum = VoiceJson.num(objTryParse, "auto_play_confidence");
            if (dNum > 0.0d) {
                config.autoPlay = Math.max(0.5d, Math.min(1.01d, dNum));
            }
            config.webSearch = VoiceJson.bool(objTryParse, config.webSearch, "web_search");
            config.groqRouterModel = opt(objTryParse, "groq_router_model", config.groqRouterModel);
            config.groqWebModel = opt(objTryParse, "groq_web_model", config.groqWebModel);
            config.screenRead = VoiceJson.bool(objTryParse, config.screenRead, "screen_read");
            config.groqVisionModel = opt(objTryParse, "groq_vision_model", config.groqVisionModel);
            config.capture = opt(objTryParse, "capture", config.capture);
            config.language = VoiceJson.str(objTryParse, "language").trim();
            config.region = opt(objTryParse, "region", config.region);
            config.sttPrompt = opt(objTryParse, "stt_prompt", config.sttPrompt);
            config.speak = VoiceJson.bool(objTryParse, config.speak, "speak");
            double dNum2 = VoiceJson.num(objTryParse, "max_results");
            if (dNum2 >= 1.0d) {
                config.maxResults = (int) Math.min(3.0d, dNum2);
            }
            Object objAt = VoiceJson.at(objTryParse, "search_links");
            if (objAt instanceof Map) {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) objAt).entrySet()) {
                    config.searchLinks.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                }
            }
            return config;
        }

        static Config read(File file) {
            if (file == null || !file.exists()) {
                return null;
            }
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                try {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    byte[] bArr = new byte[4096];
                    while (true) {
                        int i = fileInputStream.read(bArr);
                        if (i <= 0) {
                            Config config = parse(byteArrayOutputStream.toString("UTF-8"));
                            config.file = file;
                            return config;
                        }
                        byteArrayOutputStream.write(bArr, 0, i);
                    }
                } finally {
                    fileInputStream.close();
                }
            } catch (Exception e) {
                return null;
            }
        }

        private static String key(Object obj, String str) {
            String strTrim = VoiceJson.str(obj, str).trim();
            return (strTrim.isEmpty() || strTrim.toUpperCase().startsWith("PASTE")) ? "" : strTrim;
        }

        private static String opt(Object obj, String str, String str2) {
            String strTrim = VoiceJson.str(obj, str).trim();
            return strTrim.isEmpty() ? str2 : strTrim;
        }

        String keyFor(String str) {
            if (str.equals("groq")) {
                return this.groqKey;
            }
            if (str.equals("sarvam")) {
                return this.sarvamKey;
            }
            if (str.equals("deepgram")) {
                return this.deepgramKey;
            }
            return str.equals("gemini") ? this.geminiKey : "";
        }

        boolean hasStt() {
            Iterator<String> it = this.sttOrder.iterator();
            while (it.hasNext()) {
                if (!keyFor(it.next()).isEmpty()) {
                    return true;
                }
            }
            return false;
        }

        /** True when the app is on this TV, or when that is not known. */
        boolean has(String appKey) {
            VoiceCore.App app = VoiceCore.app(appKey);
            if (app == null || app.pkg == null) {
                return false;
            }
            return this.installed == null || this.installed.contains(app.pkg) || this.installed.contains(appKey);
        }

        boolean hasSpotifyApi() {
            return !this.spotifyId.isEmpty() && !this.spotifySecret.isEmpty();
        }

        boolean hasIndex() {
            return !this.tmdbKey.isEmpty() || this.justwatch;
        }
    }

    static final class Resp {
        final String body;
        final int code;

        Resp(int i, String str) {
            this.code = i;
            this.body = str;
        }
    }

    VoiceClient(Config config, Http http) {
        this.cfg = config;
        this.http = http;
    }

    static Map<String, String> headers(String... strArr) {
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        int i = 0;
        while (true) {
            int i2 = i + 1;
            if (i2 >= strArr.length) {
                return linkedHashMap;
            }
            linkedHashMap.put(strArr[i], strArr[i2]);
            i += 2;
        }
    }

    Object json(String str, String str2, String str3, Map<String, String> map, byte[] bArr, int i) throws VoiceException {
        long jNanoTime = System.nanoTime();
        try {
            Resp respCall = this.http.call(str2, str3, map, bArr, i);
            this.trace.append(str).append(' ').append((System.nanoTime() - jNanoTime) / 1000000).append("ms; ");
            if (respCall.code == 401 || respCall.code == 403) {
                throw new VoiceException(str + " rejected the key (HTTP " + respCall.code + "). Check voice.json");
            }
            if (respCall.code == 429) {
                throw new VoiceException(str + " free limit reached (HTTP 429). Try again in a minute");
            }
            if (respCall.code >= 400) {
                throw new VoiceException(str + " error HTTP " + respCall.code + ": " + snippet(respCall.body));
            }
            Object objTryParse = VoiceJson.tryParse(respCall.body);
            if (objTryParse == null) {
                throw new VoiceException(str + " sent an unreadable reply");
            }
            return objTryParse;
        } catch (SocketTimeoutException e) {
            throw new VoiceException(str + " timed out");
        } catch (UnknownHostException e2) {
            throw new VoiceException("No internet (cannot reach " + str + ")");
        } catch (IOException e3) {
            throw new VoiceException(str + " network error: " + e3.getMessage());
        }
    }

    private static String snippet(String str) {
        String strTrim = str.replaceAll("\\s+", " ").trim();
        return strTrim.length() > 140 ? strTrim.substring(0, 140) + "…" : strTrim;
    }

    private static byte[] multipart(Map<String, String> map, String str, String str2, byte[] bArr) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(bArr.length + 1024);
            for (Map.Entry<String, String> entry : map.entrySet()) {
                byteArrayOutputStream.write(("------voice7MA4YWxkTrZu0gW\r\nContent-Disposition: form-data; name=\"" + entry.getKey() + "\"\r\n\r\n" + entry.getValue() + "\r\n").getBytes("UTF-8"));
            }
            byteArrayOutputStream.write(("------voice7MA4YWxkTrZu0gW\r\nContent-Disposition: form-data; name=\"" + str + "\"; filename=\"" + str2 + "\"\r\nContent-Type: audio/wav\r\n\r\n").getBytes("UTF-8"));
            byteArrayOutputStream.write(bArr);
            byteArrayOutputStream.write("\r\n------voice7MA4YWxkTrZu0gW--\r\n".getBytes("UTF-8"));
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static byte[] utf8(String str) {
        try {
            return str.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    static final class Transcript {
        final String provider;
        final String text;

        Transcript(String str, String str2) {
            this.text = str;
            this.provider = str2;
        }
    }

    Transcript transcribe(byte[] bArr) throws VoiceException {
        String strSttGemini;
        StringBuilder sb = new StringBuilder();
        boolean z = false;
        for (String str : this.cfg.sttOrder) {
            if (!this.cfg.keyFor(str).isEmpty()) {
                try {
                    if (str.equals("groq")) {
                        strSttGemini = sttGroq(bArr);
                    } else if (str.equals("sarvam")) {
                        strSttGemini = sttSarvam(bArr);
                    } else if (str.equals("deepgram")) {
                        strSttGemini = sttDeepgram(bArr);
                    } else {
                        strSttGemini = str.equals("gemini") ? sttGemini(bArr) : null;
                    }
                    if (strSttGemini == null) {
                        throw new VoiceException("Unknown speech provider \"" + str + "\" in voice.json");
                    }
                    String strTrim = strSttGemini.trim();
                    if (!strTrim.isEmpty()) {
                        return new Transcript(strTrim, str);
                    }
                    sb.append(str).append(": heard nothing. ");
                    z = true;
                } catch (VoiceException e) {
                    sb.append(e.getMessage()).append(". ");
                }
            }
        }
        if (z) {
            throw new VoiceException(sb.toString().trim());
        }
        throw new VoiceException("No speech-to-text key in voice.json (add groq_key)");
    }

    private String sttGroq(byte[] bArr) throws VoiceException {
        Map<String, String> mapHeaders = headers("model", this.cfg.groqSttModel, "response_format", "json", "temperature", "0", "prompt", this.cfg.sttPrompt);
        if (!this.cfg.language.isEmpty()) {
            mapHeaders.put("language", this.cfg.language);
        }
        return VoiceJson.str(json("Groq", "POST", "https://api.groq.com/openai/v1/audio/transcriptions", headers("Authorization", "Bearer " + this.cfg.groqKey, "Content-Type", "multipart/form-data; boundary=----voice7MA4YWxkTrZu0gW"), multipart(mapHeaders, "file", "speech.wav", bArr), 20000), "text");
    }

    private String sttSarvam(byte[] bArr) throws VoiceException {
        String str;
        if (this.cfg.language.equals("hi")) {
            str = "hi-IN";
        } else {
            str = this.cfg.language.equals("en") ? "en-IN" : "unknown";
        }
        return VoiceJson.str(json("Sarvam", "POST", "https://api.sarvam.ai/speech-to-text", headers("api-subscription-key", this.cfg.sarvamKey, "Content-Type", "multipart/form-data; boundary=----voice7MA4YWxkTrZu0gW"), multipart(headers("model", this.cfg.sarvamModel, "mode", this.cfg.sarvamMode, "language_code", str), "file", "speech.wav", bArr), 20000), "transcript");
    }

    private String sttDeepgram(byte[] bArr) throws VoiceException {
        return VoiceJson.str(json("Deepgram", "POST", "https://api.deepgram.com/v1/listen?model=" + VoiceCore.enc(this.cfg.deepgramModel) + "&language=" + (this.cfg.language.isEmpty() ? "multi" : this.cfg.language) + "&smart_format=false&mip_opt_out=true", headers("Authorization", "Token " + this.cfg.deepgramKey, "Content-Type", "audio/wav"), bArr, 20000), "results", "channels", 0, "alternatives", 0, "transcript");
    }

    private String sttGemini(byte[] bArr) throws VoiceException {
        return VoiceJson.str(json("Gemini", "POST", "https://generativelanguage.googleapis.com/v1beta/models/" + this.cfg.geminiModel + ":generateContent", headers("x-goog-api-key", this.cfg.geminiKey, "Content-Type", "application/json"), utf8("{\"contents\":[{\"parts\":[{\"text\":" + VoiceJson.quote("Transcribe this short voice search for a movie or TV show. The speaker is Indian and may mix Hindi and English. Write Hindi words in English letters. Reply with the transcript only.") + "},{\"inlineData\":{\"mimeType\":\"audio/wav\",\"data\":\"" + base64(bArr) + "\"}}]}],\"generationConfig\":{\"temperature\":0}}"), 20000), "candidates", 0, "content", "parts", 0, "text");
    }

    static String base64(byte[] bArr) {
        StringBuilder sb = new StringBuilder(((bArr.length + 2) / 3) * 4);
        for (int i = 0; i < bArr.length; i += 3) {
            int i2 = i + 1;
            int i3 = i + 2;
            int i4 = ((bArr[i] & 255) << 16) | (i2 < bArr.length ? (bArr[i2] & 255) << 8 : 0) | (i3 < bArr.length ? bArr[i3] & 255 : 0);
            char c = '=';
            StringBuilder sbAppend = sb.append(B64[(i4 >> 18) & 63]).append(B64[(i4 >> 12) & 63]).append(i2 < bArr.length ? B64[(i4 >> 6) & 63] : '=');
            if (i3 < bArr.length) {
                c = B64[i4 & 63];
            }
            sbAppend.append(c);
        }
        return sb.toString();
    }

    static final class Result {
        final VoiceCore.Query query;
        final List<VoiceCore.Title> titles = new ArrayList();
        boolean usedLlm;

        Result(VoiceCore.Query query) {
            this.query = query;
        }

        boolean confident() {
            return !this.titles.isEmpty() && this.titles.get(0).sim >= 0.72d;
        }
    }

    Result lookup(VoiceCore.Query query) throws VoiceException {
        return lookup(query, this.cfg.llm);
    }

    Result lookup(VoiceCore.Query query, boolean z) throws VoiceException {
        if (!this.cfg.hasIndex()) {
            throw new VoiceException("No title index: add tmdb_key to voice.json");
        }
        Result result = new Result(query);
        boolean z2 = !this.cfg.tmdbKey.isEmpty();
        VoiceException voiceException = null;
        for (int i = 0; i < 2; i++) {
            for (String str : query.variants) {
                try {
                    merge(result.titles, z2 ? tmdbSearch(str) : justWatch(str));
                } catch (VoiceException e) {
                    if (voiceException == null) {
                        voiceException = e;
                    }
                }
                VoiceCore.rank(query, result.titles);
                if (result.confident()) {
                    break;
                }
            }
            if (result.confident() || i == 1 || !z) {
                break;
            }
            List<String> listLlmTitles = llmTitles(query);
            if (listLlmTitles.isEmpty()) {
                break;
            }
            result.usedLlm = true;
            query.variants.clear();
            query.variants.addAll(listLlmTitles);
        }
        if (result.titles.isEmpty()) {
            if (voiceException != null) {
                throw voiceException;
            }
            return result;
        }
        if (z2) {
            while (result.titles.size() > 5) {
                result.titles.remove(result.titles.size() - 1);
            }
            enrich(result.titles.subList(0, Math.min(3, result.titles.size())));
            if (this.cfg.justwatch) {
                try {
                    addDeepLinks(result.titles, justWatch(result.titles.get(0).name));
                } catch (VoiceException e2) {
                    this.trace.append("justwatch failed: ").append(e2.getMessage()).append("; ");
                }
            }
            VoiceCore.rank(query, result.titles);
        }
        List<VoiceCore.Title> listSelect = VoiceCore.select(query, result.titles, this.cfg.maxResults);
        this.trace.append("kept ").append(listSelect.size()).append(" of ").append(result.titles.size()).append("; ");
        result.titles.clear();
        result.titles.addAll(listSelect);
        if (this.cfg.justwatch && query.wantsEpisode()) {
            for (VoiceCore.Title title : result.titles) {
                if (title.type.equals("tv")) {
                    resolveEpisode(query, title);
                }
            }
        }
        return result;
    }

    private static void merge(List<VoiceCore.Title> list, List<VoiceCore.Title> list2) {
        for (VoiceCore.Title title : list2) {
            Iterator<VoiceCore.Title> it = list.iterator();
            while (true) {
                if (it.hasNext()) {
                    VoiceCore.Title next = it.next();
                    if (next.type.equals(title.type)) {
                        if (title.tmdbId != 0) {
                            if (next.tmdbId == title.tmdbId) {
                                break;
                            }
                        } else {
                            if (!title.jwId.isEmpty() && next.jwId.equals(title.jwId)) {
                                break;
                            }
                        }
                    }
                } else {
                    list.add(title);
                    break;
                }
            }
        }
    }

    private String tmdbUrl(String str, String str2) {
        return "https://api.themoviedb.org/3" + str + "?" + str2 + (this.cfg.tmdbKey.length() > 40 ? "" : "&api_key=" + this.cfg.tmdbKey);
    }

    private Map<String, String> tmdbHeaders() {
        return this.cfg.tmdbKey.length() > 40 ? headers("Authorization", "Bearer " + this.cfg.tmdbKey) : headers(new String[0]);
    }

    List<VoiceCore.Title> tmdbSearch(String str) throws VoiceException {
        Object objJson = json("TMDB", "GET", tmdbUrl("/search/multi", "query=" + VoiceCore.enc(str) + "&include_adult=false&language=en-US&region=" + this.cfg.region + "&page=1"), tmdbHeaders(), null, 10000);
        ArrayList arrayList = new ArrayList();
        for (Object obj : VoiceJson.list(objJson, "results")) {
            String str2 = VoiceJson.str(obj, "media_type");
            if (str2.equals("movie") || str2.equals("tv")) {
                VoiceCore.Title title = new VoiceCore.Title();
                title.type = str2;
                title.tmdbId = (long) VoiceJson.num(obj, "id");
                boolean zEquals = str2.equals("tv");
                title.name = VoiceJson.str(obj, zEquals ? "name" : "title");
                title.original = VoiceJson.str(obj, zEquals ? "original_name" : "original_title");
                if (title.original.equals(title.name)) {
                    title.original = "";
                }
                String str3 = VoiceJson.str(obj, zEquals ? "first_air_date" : "release_date");
                title.year = str3.length() >= 4 ? str3.substring(0, 4) : "";
                title.popularity = VoiceJson.num(obj, "popularity");
                if (!title.name.isEmpty()) {
                    arrayList.add(title);
                }
            }
        }
        return arrayList;
    }

    private void enrich(List<VoiceCore.Title> list) {
        ArrayList arrayList = new ArrayList();
        for (final VoiceCore.Title title : list) {
            Thread thread = new Thread(new Runnable() { // from class: com.chirag.home.VoiceClient.2

                @Override // java.lang.Runnable
                public void run() {
                    try {
                        VoiceClient.this.tmdbDetails(title);
                    } catch (VoiceException e) {
                        synchronized (VoiceClient.this.trace) {
                            VoiceClient.this.trace.append("details failed: ").append(e.getMessage()).append("; ");
                        }
                    }
                }
            });
            thread.start();
            arrayList.add(thread);
        }
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            try {
                ((Thread) it.next()).join(12000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    void tmdbDetails(VoiceCore.Title title) throws VoiceException {
        Object objJson = json("TMDB", "GET", tmdbUrl("/" + title.type + "/" + title.tmdbId, "language=en-US&append_to_response=watch%2Fproviders,alternative_titles"), tmdbHeaders(), null, 10000);
        Object objAt = VoiceJson.at(objJson, "watch/providers", "results", this.cfg.region);
        String[][] strArr = {new String[]{"flatrate", "stream"}, new String[]{"free", "free"}, new String[]{"ads", "ads"}, new String[]{"rent", "rent"}, new String[]{"buy", "buy"}};
        for (int i = 0; i < 5; i++) {
            String[] strArr2 = strArr[i];
            Iterator<Object> it = VoiceJson.list(objAt, strArr2[0]).iterator();
            while (it.hasNext()) {
                int iNum = (int) VoiceJson.num(it.next(), "provider_id");
                VoiceCore.App appAppForProvider = VoiceCore.appForProvider(iNum);
                if (appAppForProvider != null) {
                    title.offer(appAppForProvider.key, strArr2[1], VoiceCore.isChannel(iNum), null, null);
                }
            }
        }
        Object objAt2 = VoiceJson.at(objJson, "alternative_titles");
        List<Object> list = VoiceJson.list(objAt2, "titles");
        if (list.isEmpty()) {
            list = VoiceJson.list(objAt2, "results");
        }
        Iterator<Object> it2 = list.iterator();
        while (it2.hasNext()) {
            String str = VoiceJson.str(it2.next(), "title");
            if (!str.isEmpty() && title.alts.size() < 15 && !title.alts.contains(str)) {
                title.alts.add(str);
            }
        }
    }

    private static String jwKind(String str) {
        if (str.equals("FLATRATE")) {
            return "stream";
        }
        if (str.equals("FREE")) {
            return "free";
        }
        if (str.equals("ADS")) {
            return "ads";
        }
        if (str.equals("RENT")) {
            return "rent";
        }
        if (str.equals("BUY")) {
            return "buy";
        }
        return null;
    }

    List<VoiceCore.Title> justWatch(String str) throws VoiceException {
        Object objJson = json("JustWatch", "POST", JW_URL, headers("Content-Type", "application/json"), utf8("{\"query\":" + VoiceJson.quote(JW_QUERY) + ",\"variables\":{\"c\":" + VoiceJson.quote(this.cfg.region) + ",\"l\":\"en\",\"f\":{\"searchQuery\":" + VoiceJson.quote(str) + "}}}"), 10000);
        ArrayList arrayList = new ArrayList();
        Iterator<Object> it = VoiceJson.list(objJson, "data", "popularTitles", "edges").iterator();
        while (it.hasNext()) {
            Object objAt = VoiceJson.at(it.next(), "node");
            VoiceCore.Title title = new VoiceCore.Title();
            title.jwId = VoiceJson.str(objAt, "id");
            title.type = VoiceJson.str(objAt, "objectType").equals("SHOW") ? "tv" : "movie";
            title.name = VoiceJson.str(objAt, "content", "title");
            title.original = VoiceJson.str(objAt, "content", "originalTitle");
            if (title.original.equals(title.name)) {
                title.original = "";
            }
            title.year = VoiceJson.str(objAt, "content", "originalReleaseYear");
            title.tmdbId = (long) VoiceJson.num(objAt, "content", "externalIds", "tmdbId");
            title.poster = VoiceJson.str(objAt, "content", "posterUrl");
            title.backdrop = VoiceJson.str(objAt, "content", "backdrops", 0, "backdropUrl");
            for (Object obj : VoiceJson.list(objAt, "offers")) {
                int iNum = (int) VoiceJson.num(obj, "package", "packageId");
                VoiceCore.App appAppForProvider = VoiceCore.appForProvider(iNum);
                String strJwKind = jwKind(VoiceJson.str(obj, "monetizationType"));
                if (appAppForProvider != null && strJwKind != null) {
                    title.offer(appAppForProvider.key, strJwKind, VoiceCore.isChannel(iNum), VoiceJson.str(obj, "deeplinkURL"), VoiceJson.str(obj, "standardWebURL"));
                }
            }
            for (Object obj2 : VoiceJson.list(objAt, "seasons")) {
                String str2 = VoiceJson.str(obj2, "id");
                int iNum2 = (int) VoiceJson.num(obj2, "content", "seasonNumber");
                if (!str2.isEmpty() && iNum2 > 0) {
                    title.seasons.add(new VoiceCore.Season(str2, iNum2, (int) VoiceJson.num(obj2, "totalEpisodeCount")));
                }
            }
            if (!title.name.isEmpty()) {
                arrayList.add(title);
            }
        }
        return arrayList;
    }

    List<VoiceCore.Episode> justWatchEpisodes(String str, int i, int i2) throws VoiceException {
        Object objJson = json("JustWatch", "POST", JW_URL, headers("Content-Type", "application/json"), utf8("{\"query\":" + VoiceJson.quote(JW_SEASON_QUERY) + ",\"variables\":{\"id\":" + VoiceJson.quote(str) + ",\"c\":" + VoiceJson.quote(this.cfg.region) + ",\"l\":\"en\"" + (i > 0 ? ",\"n\":" + i + ",\"o\":" + i2 : "") + "}}"), 10000);
        ArrayList arrayList = new ArrayList();
        for (Object obj : VoiceJson.list(objJson, "data", "node", "episodes")) {
            VoiceCore.Episode episode = new VoiceCore.Episode();
            episode.season = (int) VoiceJson.num(obj, "content", "seasonNumber");
            episode.number = (int) VoiceJson.num(obj, "content", "episodeNumber");
            episode.name = VoiceJson.str(obj, "content", "title");
            episode.released = VoiceJson.bool(obj, true, "content", "isReleased");
            for (Object obj2 : VoiceJson.list(obj, "offers")) {
                int iNum = (int) VoiceJson.num(obj2, "package", "packageId");
                VoiceCore.App appAppForProvider = VoiceCore.appForProvider(iNum);
                String strJwKind = jwKind(VoiceJson.str(obj2, "monetizationType"));
                if (appAppForProvider != null && strJwKind != null) {
                    episode.offer(appAppForProvider.key, strJwKind, VoiceCore.isChannel(iNum), VoiceJson.str(obj2, "deeplinkURL"), VoiceJson.str(obj2, "standardWebURL"), VoiceJson.str(obj2, "webDeeplinkURL"));
                }
            }
            arrayList.add(episode);
        }
        return arrayList;
    }

    void resolveEpisode(VoiceCore.Query query, VoiceCore.Title title) {
        StringBuilder sbAppend;
        int i;
        VoiceCore.Spot spotLocate = VoiceCore.locate(query, title);
        if (spotLocate == null) {
            if (!title.seasons.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                if (query.season > 0) {
                    sbAppend = new StringBuilder().append("season ");
                    i = query.season;
                } else {
                    sbAppend = new StringBuilder().append("episode ");
                    i = query.episode;
                }
                title.targetNote = sb.append(sbAppend.append(i).toString()).append(" not found, opening the show").toString();
                return;
            }
            return;
        }
        int i2 = spotLocate.season.count;
        int i3 = 1;
        int iMax = 0;
        boolean z = query.epRef.equals("last") && query.episode == 0;
        if (query.episode != 0 || z) {
            if (i2 <= 100) {
                i3 = 0;
            } else {
                i3 = 60;
                iMax = Math.max(0, (z || spotLocate.position > i2) ? i2 - 60 : spotLocate.position - 11);
            }
        }
        try {
            title.target = VoiceCore.pick(query, spotLocate, justWatchEpisodes(spotLocate.season.id, i3, iMax), iMax);
            if (title.target == null) {
                title.targetNote = "episode not found, opening the show";
            }
        } catch (VoiceException e) {
            this.trace.append("episodes failed: ").append(e.getMessage()).append("; ");
            title.targetNote = "could not look up the episode, opening the show";
        }
    }

    static void addDeepLinks(List<VoiceCore.Title> list, List<VoiceCore.Title> list2) {
        for (VoiceCore.Title title : list) {
            for (VoiceCore.Title title2 : list2) {
                if (title2.tmdbId == title.tmdbId && title2.type.equals(title.type)) {
                    for (VoiceCore.Offer offer : title2.offers.values()) {
                        title.offer(offer.app, offer.kind, offer.channel, offer.deeplink, offer.web);
                    }
                    if (title.jwId.isEmpty()) {
                        title.jwId = title2.jwId;
                        title.seasons.addAll(title2.seasons);
                        title.poster = title2.poster;
                        title.backdrop = title2.backdrop;
                    }
                }
            }
        }
    }

    List<String> llmTitles(VoiceCore.Query query) {
        String str;
        String str2 = "Transcript: " + query.raw + "\nCleaned: " + query.text;
        try {
            if (!this.cfg.groqKey.isEmpty()) {
                str = VoiceJson.str(json("Groq LLM", "POST", "https://api.groq.com/openai/v1/chat/completions", headers("Authorization", "Bearer " + this.cfg.groqKey, "Content-Type", "application/json"), utf8("{\"model\":" + VoiceJson.quote(this.cfg.groqLlmModel) + ",\"temperature\":0,\"max_completion_tokens\":600,\"messages\":[{\"role\":\"system\",\"content\":" + VoiceJson.quote("You fix speech-recognition errors in voice searches on an Indian smart TV. The user says the name of a movie, TV show, web series or anime in English, Hindi or Hinglish, sometimes with a command (play, chalao, dikhao) and an app name. Hindi number words after a name mean a sequel (do = 2, teen = 3). Reply with JSON only: {\"titles\":[up to 3 official titles as listed on TMDB, most likely first]}. If you cannot tell, reply {\"titles\":[]}.") + "},{\"role\":\"user\",\"content\":" + VoiceJson.quote(str2) + "}]}"), 12000), "choices", 0, "message", "content");
            } else if (!this.cfg.geminiKey.isEmpty()) {
                str = VoiceJson.str(json("Gemini LLM", "POST", "https://generativelanguage.googleapis.com/v1beta/models/" + this.cfg.geminiModel + ":generateContent", headers("x-goog-api-key", this.cfg.geminiKey, "Content-Type", "application/json"), utf8("{\"contents\":[{\"parts\":[{\"text\":" + VoiceJson.quote("You fix speech-recognition errors in voice searches on an Indian smart TV. The user says the name of a movie, TV show, web series or anime in English, Hindi or Hinglish, sometimes with a command (play, chalao, dikhao) and an app name. Hindi number words after a name mean a sequel (do = 2, teen = 3). Reply with JSON only: {\"titles\":[up to 3 official titles as listed on TMDB, most likely first]}. If you cannot tell, reply {\"titles\":[]}.\n\n" + str2) + "}]}],\"generationConfig\":{\"temperature\":0}}"), 12000), "candidates", 0, "content", "parts", 0, "text");
            } else {
                return new ArrayList();
            }
            return parseLlmTitles(str);
        } catch (VoiceException e) {
            this.trace.append("llm failed: ").append(e.getMessage()).append("; ");
            return new ArrayList();
        }
    }

    static final class Routed {
        String tool = "";
        final Map<String, String> args = new LinkedHashMap();

        Routed() {
        }

        String arg(String str) {
            String str2 = this.args.get(str);
            return str2 == null ? "" : str2.trim();
        }

        int num(String str) {
            try {
                return Integer.parseInt(arg(str));
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        public String toString() {
            return this.tool + this.args;
        }
    }

    private static String tool(String str, String str2, String str3, String str4) {
        return "{\"type\":\"function\",\"function\":{\"name\":\"" + str + "\",\"description\":" + VoiceJson.quote(str2) + ",\"parameters\":{\"type\":\"object\",\"properties\":{" + str3 + "},\"required\":[\"" + str4 + "\"]}}}";
    }

    Routed route(String str, String str2, int i) {
        if (!this.cfg.llm || this.cfg.groqKey.isEmpty()) {
            return null;
        }
        long start = System.nanoTime();
        for (int attempt = 0; ; attempt++) {
            int left = i - (int) ((System.nanoTime() - start) / 1000000);
            try {
                return routeOnce(str, str2, Math.max(500, left));
            } catch (VoiceException e) {
                this.trace.append("router failed: ").append(e.getMessage()).append("; ");
                // gpt-oss now and then answers without a tool call, or with bad arguments; one quick retry fixes it.
                String m = e.getMessage() == null ? "" : e.getMessage();
                boolean toolSlip = m.contains("tool_use_failed") || m.contains("did not call a tool") || m.contains("validation failed");
                left = i - (int) ((System.nanoTime() - start) / 1000000);
                if (attempt > 0 || !toolSlip || left < 900) {
                    return null;
                }
            }
        }
    }

    private Routed routeOnce(String str, String str2, int i) throws VoiceException {
        Object objAt = VoiceJson.at(json("Groq router", "POST", "https://api.groq.com/openai/v1/chat/completions", headers("Authorization", "Bearer " + this.cfg.groqKey, "Content-Type", "application/json"), utf8("{\"model\":" + VoiceJson.quote(this.cfg.groqRouterModel) + ",\"temperature\":0,\"max_completion_tokens\":400," + (this.cfg.groqRouterModel.startsWith("openai/gpt-oss") ? "\"reasoning_effort\":\"low\"," : "") + "\"tool_choice\":\"required\",\"tools\":" + ROUTER_TOOLS + ",\"messages\":[{\"role\":\"system\",\"content\":" + VoiceJson.quote(ROUTER_SYSTEM + (str2.isEmpty() ? "" : "\nRecently watched on this TV, newest first:\n" + str2)) + "},{\"role\":\"user\",\"content\":" + VoiceJson.quote(str) + "}]}"), i), "choices", 0, "message", "tool_calls", 0, "function");
        Routed routed = new Routed();
        routed.tool = VoiceJson.str(objAt, "name");
        Object objTryParse = VoiceJson.tryParse(VoiceJson.str(objAt, "arguments"));
        if (routed.tool.isEmpty() || !(objTryParse instanceof Map)) {
            return null;
        }
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) objTryParse).entrySet()) {
            String str3 = VoiceJson.str(objTryParse, String.valueOf(entry.getKey()));
            if (!str3.isEmpty() && str3.length() <= 200) {
                routed.args.put(String.valueOf(entry.getKey()), str3);
            }
        }
        return routed;
    }

    /** A Spotify track found for a music request. */
    static final class Track {
        String id = "";
        String name = "";
        String artist = "";
        int popularity;

        public String toString() {
            return this.name + " - " + this.artist + " (" + this.id + ", popularity " + this.popularity + ")";
        }
    }

    private static String spotifyToken = "";
    private static long spotifyTokenUntil;

    private synchronized String spotifyToken(int timeoutMs) throws VoiceException {
        // The owner's own sign-in when there is one (search then also knows their market); else the app-only token.
        String user = SpotifyTaste.userToken(this, timeoutMs);
        if (user != null) {
            return user;
        }
        if (!spotifyToken.isEmpty() && System.currentTimeMillis() < spotifyTokenUntil) {
            return spotifyToken;
        }
        Object o = json("Spotify sign-in", "POST", "https://accounts.spotify.com/api/token", headers("Authorization", "Basic " + base64(utf8(this.cfg.spotifyId + ":" + this.cfg.spotifySecret)), "Content-Type", "application/x-www-form-urlencoded"), utf8("grant_type=client_credentials"), timeoutMs);
        String token = VoiceJson.str(o, "access_token");
        if (token.isEmpty()) {
            throw new VoiceException("Spotify sign-in gave no token");
        }
        spotifyToken = token;
        spotifyTokenUntil = System.currentTimeMillis() + Math.max(60, (long) VoiceJson.num(o, "expires_in") - 60) * 1000;
        return token;
    }

    /**
     * Finds the Spotify track to start: the best match for a song, the top match for an artist, or for a genre or
     * mood the given seed song (else the phrase itself). Null when nothing fits or Spotify is not set up.
     */
    Track spotifyTrack(String q, String kind, String artist, int timeoutMs) {
        if (!this.cfg.hasSpotifyApi() || q.trim().isEmpty()) {
            return null;
        }
        try {
            String token = spotifyToken(timeoutMs);
            String search = q;
            if ("artist".equals(kind)) {
                search = "artist:\"" + q + "\"";
            } else if (!artist.isEmpty() && "song".equals(kind) && !VoiceCore.words(q).containsAll(VoiceCore.words(artist))) {
                search = q + " artist:\"" + artist + "\"";
            }
            Object o = json("Spotify search", "GET", "https://api.spotify.com/v1/search?type=track&limit=8&market=" + VoiceCore.enc(this.cfg.region.isEmpty() ? "IN" : this.cfg.region) + "&q=" + VoiceCore.enc(search), headers("Authorization", "Bearer " + token), null, timeoutMs);
            Track best = null;
            double bestScore = -1;
            int rank = 0;
            for (Object item : VoiceJson.list(o, "tracks", "items")) {
                Track t = new Track();
                t.id = VoiceJson.str(item, "id");
                t.name = VoiceJson.str(item, "name");
                t.artist = VoiceJson.str(item, "artists", 0, "name");
                t.popularity = (int) VoiceJson.num(item, "popularity");
                if (!t.id.matches("[A-Za-z0-9]{22}")) {
                    continue;
                }
                // Spotify's own order first, then popularity; for a named song, prefer a name that matches.
                double score = 1.0 - rank * 0.08 + t.popularity / 250.0;
                if ("song".equals(kind) || kind.isEmpty()) {
                    score += VoiceCore.similarity(q, t.name) * 0.5;
                }
                rank++;
                if (score > bestScore) {
                    best = t;
                    bestScore = score;
                }
            }
            return best;
        } catch (VoiceException e) {
            this.trace.append("Spotify failed: ").append(e.getMessage()).append("; ");
            return null;
        }
    }

    String[] webSearch(String str, int i) {
        if (!this.cfg.webSearch || this.cfg.groqKey.isEmpty()) {
            return null;
        }
        try {
            Object objJsonIn = ScreenRead.jsonIn(VoiceJson.str(json("Groq web search", "POST", "https://api.groq.com/openai/v1/chat/completions", headers("Authorization", "Bearer " + this.cfg.groqKey, "Content-Type", "application/json"), utf8("{\"model\":" + VoiceJson.quote(this.cfg.groqWebModel) + ",\"temperature\":0,\"max_completion_tokens\":800,\"tools\":[{\"type\":\"browser_search\"}],\"messages\":[{\"role\":\"user\",\"content\":" + VoiceJson.quote("A TV voice search could not resolve this request: \"" + str + "\". Search the web if needed and reply with JSON only: {\"kind\": \"title\" or \"channel\", \"name\": the exact official name of the one film, series or YouTube channel meant}. If you cannot tell, reply {\"kind\": null}.") + "}]}"), i), "choices", 0, "message", "content"));
            String str2 = VoiceJson.str(objJsonIn, "kind");
            String strTrim = VoiceJson.str(objJsonIn, "name").trim();
            if ((str2.equals("title") || str2.equals("channel")) && !strTrim.isEmpty() && strTrim.length() < 120) {
                return new String[]{str2, strTrim};
            }
            return null;
        } catch (VoiceException e) {
            this.trace.append("web search failed: ").append(e.getMessage()).append("; ");
            return null;
        }
    }

    VoiceCore.Title identify(ScreenRead.Reading reading, String str, boolean z) throws VoiceException {
        VoiceCore.Title titleArtMatch = VoiceCore.artMatch(str, reading.title, justWatch(reading.title), z, z ? 0.85d : 0.72d);
        if (titleArtMatch == null) {
            return null;
        }
        if (reading.season > 0 || reading.episode > 0) {
            if (!titleArtMatch.type.equals("tv")) {
                return null;
            }
            VoiceCore.Query query = new VoiceCore.Query();
            query.season = reading.season;
            query.episode = reading.episode;
            resolveEpisode(query, titleArtMatch);
        }
        return titleArtMatch;
    }

    String vision(String str, byte[] bArr, String str2) throws VoiceException {
        if (this.cfg.groqKey.isEmpty()) {
            throw new VoiceException("No groq_key in voice.json");
        }
        return VoiceJson.str(json("Groq vision", "POST", "https://api.groq.com/openai/v1/chat/completions", headers("Authorization", "Bearer " + this.cfg.groqKey, "Content-Type", "application/json"), utf8("{\"model\":" + VoiceJson.quote(this.cfg.groqVisionModel) + ",\"temperature\":0,\"max_completion_tokens\":700,\"messages\":[{\"role\":\"user\",\"content\":[{\"type\":\"text\",\"text\":" + VoiceJson.quote(str) + "},{\"type\":\"image_url\",\"image_url\":{\"url\":\"data:" + str2 + ";base64," + base64(bArr) + "\"}}]}]}"), 20000), "choices", 0, "message", "content");
    }

    static List<String> parseLlmTitles(String str) {
        ArrayList arrayList = new ArrayList();
        int iIndexOf = str.indexOf(123);
        int iLastIndexOf = str.lastIndexOf(125);
        if (iIndexOf < 0 || iLastIndexOf <= iIndexOf) {
            return arrayList;
        }
        Iterator<Object> it = VoiceJson.list(VoiceJson.tryParse(str.substring(iIndexOf, iLastIndexOf + 1)), "titles").iterator();
        while (it.hasNext()) {
            String strTrim = String.valueOf(it.next()).trim();
            if (!strTrim.isEmpty() && strTrim.length() < 120 && arrayList.size() < 3) {
                arrayList.add(strTrim);
            }
        }
        return arrayList;
    }
}
