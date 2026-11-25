package com.chirag.home;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * The owner's Spotify taste, for music requests that do not name a song or an artist.
 *
 * Signs in as the owner with the refresh token from tools/spotify-auth/auth.py (read-only scopes), builds a small
 * profile from their recent plays, top tracks and artists and saved songs, and picks the seed track Spotify's
 * autoplay continues from: one of their own songs for "play my music", one of their songs that fits for a genre or
 * mood ("lo-fi", "something chill"), else a well-known song of that genre in their usual language.
 *
 * Plain Java (runs in the Mac test harness too). Tokens are never logged.
 */
final class SpotifyTaste {
    static final long PROFILE_FRESH_MS = 4 * 3600000L;
    static final long PROFILE_MAX_AGE_MS = 3 * 86400000L;
    static final long PLAYED_KEEP_MS = 14 * 86400000L;
    static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    static final String API = "https://api.spotify.com/v1/";
    static final String PROFILE_FILE = "spotify_taste.json";
    static final String PLAYED_FILE = "spotify_played.txt";

    /** Where notes go (Log.i on the TV, stderr on the Mac). Never given a token. */
    interface Logger {
        void log(String s);
    }

    static volatile Logger logger = new Logger() {
        @Override
        public void log(String s) {
            System.err.println("SpotifyTaste: " + s);
        }
    };

    static void log(String s) {
        try {
            logger.log(s);
        } catch (Throwable ignored) {
        }
    }

    // ---------------------------------------------------------------- the owner's token

    private static final Object TOKEN_LOCK = new Object();
    private static String userToken = "";
    private static long userTokenUntil;
    private static String userTokenFor = "";
    private static long refreshBackoffUntil;

    /** The owner's access token (refreshed about hourly), or null when there is no sign-in or it failed. */
    static String userToken(VoiceClient c, int timeoutMs) {
        VoiceClient.Config cfg = c.cfg;
        if (cfg.spotifyRefresh.isEmpty() || !cfg.hasSpotifyApi()) {
            return null;
        }
        synchronized (TOKEN_LOCK) {
            long now = System.currentTimeMillis();
            if (!userToken.isEmpty() && now < userTokenUntil && userTokenFor.equals(cfg.spotifyRefresh)) {
                return userToken;
            }
            if (now < refreshBackoffUntil) {
                return null;
            }
            try {
                Object o = c.json("Spotify user sign-in", "POST", TOKEN_URL,
                        VoiceClient.headers("Authorization", "Basic " + VoiceClient.base64(VoiceClient.utf8(cfg.spotifyId + ":" + cfg.spotifySecret)),
                                "Content-Type", "application/x-www-form-urlencoded"),
                        VoiceClient.utf8("grant_type=refresh_token&refresh_token=" + VoiceCore.enc(cfg.spotifyRefresh)), timeoutMs);
                String token = VoiceJson.str(o, "access_token");
                if (token.isEmpty()) {
                    throw new VoiceClient.VoiceException("Spotify user sign-in gave no token");
                }
                String rotated = VoiceJson.str(o, "refresh_token");
                if (!rotated.isEmpty() && !rotated.equals(cfg.spotifyRefresh)) {
                    // Spotify sometimes hands out a new refresh token; the old one may stop working, so keep it.
                    saveRefreshToken(cfg, cfg.spotifyRefresh, rotated);
                    cfg.spotifyRefresh = rotated;
                }
                userToken = token;
                userTokenFor = cfg.spotifyRefresh;
                userTokenUntil = now + Math.max(60, (long) VoiceJson.num(o, "expires_in") - 120) * 1000;
                return token;
            } catch (VoiceClient.VoiceException e) {
                log("owner sign-in failed, using the app-only token (no taste) for 10 min: " + e.getMessage());
                refreshBackoffUntil = now + 10 * 60000L;
                return null;
            }
        }
    }

    /** Swaps the refresh token in voice.json, keeping the rest of the file as it is. */
    static void saveRefreshToken(VoiceClient.Config cfg, String old, String rotated) {
        File f = cfg.file;
        if (f == null || !f.exists()) {
            log("Spotify rotated the refresh token but voice.json is not known; it lives in memory only");
            return;
        }
        try {
            String text = readText(f);
            if (!text.contains(old)) {
                log("Spotify rotated the refresh token but voice.json has a different one; not saved");
                return;
            }
            File tmp = new File(f.getPath() + ".tmp");
            writeText(tmp, text.replace(old, rotated));
            if (!tmp.renameTo(f)) {
                writeText(f, text.replace(old, rotated));
                tmp.delete();
            }
            log("saved Spotify's new refresh token to voice.json");
        } catch (Exception e) {
            log("could not save the new refresh token: " + e);
        }
    }

    // ---------------------------------------------------------------- what the launcher itself played

    /** Notes a track the launcher started, so its own plays are not taken for taste. */
    static synchronized void played(File dir, String trackId) {
        if (dir == null || trackId == null || !trackId.matches("[A-Za-z0-9]{22}")) {
            return;
        }
        try {
            File f = new File(dir, PLAYED_FILE);
            List<String> lines = new ArrayList<>();
            long now = System.currentTimeMillis();
            if (f.exists()) {
                for (String l : readText(f).split("\n")) {
                    String[] p = l.trim().split(" ");
                    if (p.length == 2 && now - parseLong(p[0]) < PLAYED_KEEP_MS) {
                        lines.add(l.trim());
                    }
                }
            }
            lines.add(now + " " + trackId);
            while (lines.size() > 100) {
                lines.remove(0);
            }
            writeText(f, VoiceCore.join(lines, "\n") + "\n");
        } catch (Exception e) {
            log("could not note the played track: " + e);
        }
    }

    /** Track ids the launcher started in the last two weeks. */
    static synchronized Set<String> playedByLauncher(File dir) {
        Set<String> ids = new HashSet<>();
        if (dir == null) {
            return ids;
        }
        File f = new File(dir, PLAYED_FILE);
        if (!f.exists()) {
            return ids;
        }
        try {
            long now = System.currentTimeMillis();
            for (String l : readText(f).split("\n")) {
                String[] p = l.trim().split(" ");
                if (p.length == 2 && now - parseLong(p[0]) < PLAYED_KEEP_MS) {
                    ids.add(p[1]);
                }
            }
        } catch (Exception ignored) {
        }
        return ids;
    }

    // ---------------------------------------------------------------- the profile

    static final class Item {
        String id = "";
        String name = "";
        String artist = "";
        String lang = "";
        double weight;
        /** Where it came from: recent, top (short_term), top-6mo (medium_term), saved. */
        final Set<String> from = new java.util.LinkedHashSet<>();

        String line() {
            return name + " — " + artist;
        }
    }

    static final class Profile {
        long builtAt;
        /** Highest weight first. */
        final List<Item> tracks = new ArrayList<>();
        /** Artist name (lower case) -> language code: en, hi, pa, ..., none (instrumental), other. */
        final Map<String, String> lang = new HashMap<>();
        /** The language most of the recent listening is in ("" when mixed). */
        String dominant = "";

        String langOf(Item it) {
            String l = lang.get(it.artist.toLowerCase(Locale.ROOT));
            return l == null ? "" : l;
        }
    }

    private static final Object PROFILE_LOCK = new Object();
    private static Profile profile;
    private static boolean building;

    /** Starts loading the profile in the background (call when the voice screen opens). Never blocks. */
    static void warm(final VoiceClient.Config cfg, final VoiceClient.Http http) {
        if (cfg == null || cfg.spotifyRefresh.isEmpty() || !cfg.hasSpotifyApi()) {
            return;
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                profile(new VoiceClient(cfg, http), 0);
            }
        }, "spotify-taste");
        t.setDaemon(true);
        t.start();
    }

    /**
     * The owner's profile: the cached one when fresh, else a stale one while a new one is built in the background,
     * else waits up to waitMs for the first build. Null when there is no sign-in or nothing came in time.
     */
    static Profile profile(final VoiceClient c, int waitMs) {
        final VoiceClient.Config cfg = c.cfg;
        if (cfg.spotifyRefresh.isEmpty() || !cfg.hasSpotifyApi()) {
            return null;
        }
        synchronized (PROFILE_LOCK) {
            long now = System.currentTimeMillis();
            if (profile == null) {
                profile = loadProfile(cfg.dataDir);
            }
            if (profile != null && now - profile.builtAt < PROFILE_FRESH_MS) {
                return profile;
            }
            if (!building) {
                building = true;
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Profile p = null;
                        try {
                            p = build(new VoiceClient(cfg, c.http));
                        } catch (Throwable e) {
                            log("taste profile failed: " + e);
                        }
                        synchronized (PROFILE_LOCK) {
                            if (p != null) {
                                profile = p;
                            }
                            building = false;
                            PROFILE_LOCK.notifyAll();
                        }
                    }
                }, "spotify-taste-build");
                t.setDaemon(true);
                t.start();
            }
            if (profile != null && now - profile.builtAt < PROFILE_MAX_AGE_MS) {
                return profile;
            }
            long until = now + waitMs;
            while (building && System.currentTimeMillis() < until) {
                try {
                    PROFILE_LOCK.wait(Math.max(1, until - System.currentTimeMillis()));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            return profile;
        }
    }

    /** Fetches the owner's listening (7 calls at once) and weighs it. */
    static Profile build(VoiceClient c) throws VoiceClient.VoiceException {
        final VoiceClient.Config cfg = c.cfg;
        final VoiceClient.Http http = c.http;
        long t0 = System.currentTimeMillis();
        final String token = userToken(c, 8000);
        if (token == null) {
            return null;
        }
        final String[] urls = {
                "me/player/recently-played?limit=50",
                "me/top/tracks?time_range=short_term&limit=50",
                "me/top/tracks?time_range=medium_term&limit=50",
                "me/top/artists?time_range=short_term&limit=30",
                "me/top/artists?time_range=medium_term&limit=30",
                "me/tracks?limit=50&offset=0",
                "me/tracks?limit=50&offset=50",
        };
        final Object[] got = new Object[urls.length];
        final String[] err = new String[urls.length];
        Thread[] threads = new Thread[urls.length];
        for (int i = 0; i < urls.length; i++) {
            final int k = i;
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    // Spotify now and then answers 503 "no healthy upstream" when asked for several things at once: retry.
                    for (int attempt = 0; attempt < 3 && got[k] == null; attempt++) {
                        try {
                            if (attempt > 0) {
                                Thread.sleep(400L * attempt + k * 80L);
                            }
                            got[k] = new VoiceClient(cfg, http).json("Spotify " + urls[k].replaceAll("\\?.*", ""), "GET", API + urls[k],
                                    VoiceClient.headers("Authorization", "Bearer " + token), null, 8000);
                            err[k] = null;
                        } catch (VoiceClient.VoiceException e) {
                            err[k] = e.getMessage();
                            String m = String.valueOf(e.getMessage());
                            if (!m.contains("HTTP 5") && !m.contains("timed out") && !m.contains("network error")) {
                                break;
                            }
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            }, "spotify-taste-" + i);
            threads[i].start();
        }
        for (Thread t : threads) {
            try {
                t.join(10000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        int failed = 0;
        for (int i = 0; i < urls.length; i++) {
            if (got[i] == null) {
                failed++;
            }
            if (err[i] != null) {
                log("taste: " + urls[i].replaceAll("\\?.*", "") + " failed: " + err[i]);
            }
        }
        if (got[0] == null && got[1] == null && got[5] == null) {
            return null;
        }
        Set<String> mine = playedByLauncher(cfg.dataDir);
        Map<String, Item> byId = new LinkedHashMap<>();
        int skipped = 0;
        // Recent plays count most (newest first, every play adds), then this month's top, the 6-month top, saved.
        List<Object> recent = VoiceJson.list(got[0], "items");
        for (int i = 0; i < recent.size(); i++) {
            Object tr = VoiceJson.at(recent.get(i), "track");
            if (mine.contains(VoiceJson.str(tr, "id"))) {
                skipped++;
                continue;
            }
            add(byId, tr, 3.0 * (1.0 - 0.5 * i / Math.max(1, recent.size())), "recent");
        }
        List<Object> top = VoiceJson.list(got[1], "items");
        for (int i = 0; i < top.size(); i++) {
            add(byId, top.get(i), 3.0 - i * 0.04, "top this month");
        }
        top = VoiceJson.list(got[2], "items");
        for (int i = 0; i < top.size(); i++) {
            add(byId, top.get(i), 1.5 - i * 0.02, "top 6 months");
        }
        for (int s = 5; s <= 6; s++) {
            List<Object> saved = VoiceJson.list(got[s], "items");
            for (int i = 0; i < saved.size(); i++) {
                add(byId, VoiceJson.at(saved.get(i), "track"), s == 5 && i < 20 ? 1.0 : 0.6, "saved");
            }
        }
        Map<String, Double> artistBoost = new HashMap<>();
        List<Object> arts = VoiceJson.list(got[3], "items");
        for (int i = 0; i < arts.size(); i++) {
            artistBoost.merge(VoiceJson.str(arts.get(i), "name").toLowerCase(Locale.ROOT), 1.5 * (1.0 - i / 30.0), Double::sum);
        }
        arts = VoiceJson.list(got[4], "items");
        for (int i = 0; i < arts.size(); i++) {
            artistBoost.merge(VoiceJson.str(arts.get(i), "name").toLowerCase(Locale.ROOT), 0.7 * (1.0 - i / 30.0), Double::sum);
        }
        Profile p = new Profile();
        p.builtAt = System.currentTimeMillis();
        for (Item it : byId.values()) {
            if (mine.contains(it.id)) {
                skipped++;
                continue;
            }
            Double b = artistBoost.get(it.artist.toLowerCase(Locale.ROOT));
            it.weight += b == null ? 0 : b;
            p.tracks.add(it);
        }
        Collections.sort(p.tracks, (a, b) -> Double.compare(b.weight, a.weight));
        Profile old = profile != null ? profile : loadProfile(cfg.dataDir);
        if (old != null) {
            p.lang.putAll(old.lang);
        }
        classifyLanguages(c, p);
        p.dominant = dominant(p);
        if (failed > 0) {
            // Partial: use it, but build again in 10 minutes rather than in 4 hours.
            p.builtAt = System.currentTimeMillis() - PROFILE_FRESH_MS + 10 * 60000L;
        }
        saveProfile(cfg.dataDir, p);
        log("taste profile: " + p.tracks.size() + " songs, mostly " + (p.dominant.isEmpty() ? "mixed languages" : langName(p.dominant))
                + (skipped > 0 ? ", " + skipped + " launcher plays left out" : "") + (failed > 0 ? ", partial (" + failed + " of 7 lists missing)" : "") + " (" + (System.currentTimeMillis() - t0) + " ms)");
        return p;
    }

    private static void add(Map<String, Item> byId, Object tr, double w, String from) {
        String id = VoiceJson.str(tr, "id");
        if (!id.matches("[A-Za-z0-9]{22}")) {
            return;
        }
        Item it = byId.get(id);
        if (it == null) {
            it = new Item();
            it.id = id;
            it.name = VoiceJson.str(tr, "name");
            it.artist = VoiceJson.str(tr, "artists", 0, "name");
            byId.put(id, it);
        }
        it.weight += Math.max(0.1, w);
        it.from.add(from);
    }

    /** Asks the LLM once for the language of each new artist (kept across rebuilds); a script check without it. */
    static void classifyLanguages(VoiceClient c, Profile p) {
        List<String> need = new ArrayList<>();
        for (Item it : p.tracks) {
            String a = it.artist.toLowerCase(Locale.ROOT);
            if (!a.isEmpty() && !p.lang.containsKey(a) && !need.contains(it.artist)) {
                need.add(it.artist);
            }
            if (need.size() >= 80) {
                break;
            }
        }
        if (!need.isEmpty() && !c.cfg.groqKey.isEmpty()) {
            try {
                Object o = llm(c, "Spotify taste languages",
                        "For each music artist, give the main language of their songs' vocals as a code: en (English), hi (Hindi, Urdu, Bollywood), pa (Punjabi), ta, te, ml, kn, bn, mr, ja, ko, es, other, or none when their music is mostly instrumental (film scores, beats, lo-fi, classical, jazz instrumentals). Reply with JSON only: {\"Artist\": \"code\", ...} using the names exactly as given.",
                        VoiceCore.join(need, "\n"), 15000);
                if (o instanceof Map) {
                    for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) {
                        String code = String.valueOf(e.getValue()).toLowerCase(Locale.ROOT).trim();
                        if (code.matches("[a-z]{2,5}")) {
                            p.lang.put(String.valueOf(e.getKey()).toLowerCase(Locale.ROOT), code);
                        }
                    }
                }
            } catch (VoiceClient.VoiceException e) {
                log("taste: artist languages failed: " + e.getMessage());
            }
        }
        for (Item it : p.tracks) {
            String a = it.artist.toLowerCase(Locale.ROOT);
            if (!p.lang.containsKey(a)) {
                String s = it.artist + " " + it.name;
                // Only a guess from the script; filled in properly the next time the LLM answers.
                if (s.matches(".*[\\u0900-\\u097F].*")) {
                    it.lang = "hi";
                } else if (s.matches(".*[\\u0A00-\\u0A7F].*")) {
                    it.lang = "pa";
                } else if (s.matches(".*[\\u3040-\\u30FF].*")) {
                    it.lang = "ja";
                }
            }
        }
        for (Item it : p.tracks) {
            String l = p.lang.get(it.artist.toLowerCase(Locale.ROOT));
            if (l != null) {
                it.lang = l;
            }
        }
    }

    /** The language with most of the weight of recent and this month's listening, if it has at least half of it. */
    static String dominant(Profile p) {
        Map<String, Double> by = new HashMap<>();
        double total = 0;
        for (Item it : p.tracks) {
            if (!it.from.contains("recent") && !it.from.contains("top this month")) {
                continue;
            }
            if (it.lang.isEmpty() || it.lang.equals("none") || it.lang.equals("other")) {
                continue;
            }
            by.merge(it.lang, it.weight, Double::sum);
            total += it.weight;
        }
        String best = "";
        double bw = 0;
        for (Map.Entry<String, Double> e : by.entrySet()) {
            if (e.getValue() > bw) {
                best = e.getKey();
                bw = e.getValue();
            }
        }
        return total > 0 && bw / total >= 0.5 ? best : "";
    }

    // ---------------------------------------------------------------- disk cache (the TV's app files folder)

    static void saveProfile(File dir, Profile p) {
        if (dir == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{\"built\":").append(p.builtAt).append(",\"dominant\":").append(VoiceJson.quote(p.dominant)).append(",\"lang\":{");
        boolean first = true;
        for (Map.Entry<String, String> e : p.lang.entrySet()) {
            sb.append(first ? "" : ",").append(VoiceJson.quote(e.getKey())).append(':').append(VoiceJson.quote(e.getValue()));
            first = false;
        }
        sb.append("},\"tracks\":[");
        for (int i = 0; i < p.tracks.size(); i++) {
            Item it = p.tracks.get(i);
            sb.append(i == 0 ? "" : ",").append("{\"id\":").append(VoiceJson.quote(it.id)).append(",\"name\":").append(VoiceJson.quote(it.name))
                    .append(",\"artist\":").append(VoiceJson.quote(it.artist)).append(",\"lang\":").append(VoiceJson.quote(it.lang))
                    .append(",\"w\":").append(String.format(Locale.ROOT, "%.3f", it.weight)).append(",\"from\":").append(VoiceJson.quote(VoiceCore.join(new ArrayList<>(it.from), ",")))
                    .append('}');
        }
        sb.append("]}");
        try {
            dir.mkdirs();
            writeText(new File(dir, PROFILE_FILE), sb.toString());
        } catch (Exception e) {
            log("could not save the taste profile: " + e);
        }
    }

    static Profile loadProfile(File dir) {
        if (dir == null) {
            return null;
        }
        File f = new File(dir, PROFILE_FILE);
        if (!f.exists()) {
            return null;
        }
        try {
            Object o = VoiceJson.tryParse(readText(f));
            if (o == null) {
                return null;
            }
            Profile p = new Profile();
            p.builtAt = (long) VoiceJson.num(o, "built");
            p.dominant = VoiceJson.str(o, "dominant");
            Object lang = VoiceJson.at(o, "lang");
            if (lang instanceof Map) {
                for (Map.Entry<?, ?> e : ((Map<?, ?>) lang).entrySet()) {
                    p.lang.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
                }
            }
            for (Object t : VoiceJson.list(o, "tracks")) {
                Item it = new Item();
                it.id = VoiceJson.str(t, "id");
                it.name = VoiceJson.str(t, "name");
                it.artist = VoiceJson.str(t, "artist");
                it.lang = VoiceJson.str(t, "lang");
                it.weight = VoiceJson.num(t, "w");
                for (String s : VoiceJson.str(t, "from").split(",")) {
                    if (!s.isEmpty()) {
                        it.from.add(s);
                    }
                }
                p.tracks.add(it);
            }
            return p;
        } catch (Exception e) {
            return null;
        }
    }

    // ---------------------------------------------------------------- the request

    /** Words that may surround "play my music" without making it about anything in particular. */
    private static final Set<String> TASTE_FILLER = new HashSet<>(Arrays.asList(
            "play", "put", "on", "start", "some", "something", "anything", "any", "a", "the", "few", "please", "for", "me", "us",
            "music", "song", "songs", "track", "tracks", "tunes", "gaana", "gaane", "gana", "gane", "geet",
            "my", "mine", "own", "i", "d", "id", "would", "will", "might", "you", "think", "like", "love", "liked", "enjoy", "usual", "stuff",
            "favourite", "favourites", "favorite", "favorites", "fav", "favs", "faves", "good", "nice", "great", "best", "from", "library", "saved",
            "accha", "achha", "acha", "achhe", "ache", "acche", "kuch", "kuchh", "koi", "lagao", "chalao", "sunao", "suna", "bajao", "baja", "do",
            "mera", "mere", "meri", "pasand", "wala", "wale", "waale", "vala", "vale", "ka", "ke", "ki", "hai", "spotify", "to", "listen", "of"));
    private static final Set<String> TASTE_SIGNAL = new HashSet<>(Arrays.asList(
            "music", "song", "songs", "gaana", "gaane", "gana", "gane", "geet", "tunes", "favourite", "favourites", "favorite", "favorites",
            "fav", "favs", "faves", "pasand", "like", "love", "liked"));

    /** "play music", "play something I'd like", "kuch accha gaana lagao", "play my favourites" (a language word may join). */
    static boolean isTasteRequest(String text) {
        List<String> w = VoiceCore.words(text);
        boolean signal = false;
        for (String x : w) {
            if (TASTE_SIGNAL.contains(x)) {
                signal = true;
            } else if (!TASTE_FILLER.contains(x) && !LANGUAGES.contains(x)) {
                return false;
            }
        }
        return signal;
    }

    private static final Map<String, String> LANG_WORDS = new HashMap<>();
    /** Plain language names may join a taste request ("hindi songs I like"); era or genre words (purane, bhajan) may not. */
    private static final Set<String> LANGUAGES = new HashSet<>(Arrays.asList("hindi", "bollywood", "desi", "punjabi", "tamil", "telugu",
            "malayalam", "kannada", "bengali", "bangla", "marathi", "english", "korean", "japanese", "spanish"));

    static {
        for (String s : new String[]{"hindi", "bollywood", "filmi", "purane", "puraane", "bhajan", "bhajans", "ghazal", "ghazals", "qawwali", "sufi", "aarti", "kirtan", "devotional", "desi"}) {
            LANG_WORDS.put(s, "hi");
        }
        LANG_WORDS.put("punjabi", "pa");
        LANG_WORDS.put("tamil", "ta");
        LANG_WORDS.put("kollywood", "ta");
        LANG_WORDS.put("telugu", "te");
        LANG_WORDS.put("tollywood", "te");
        LANG_WORDS.put("malayalam", "ml");
        LANG_WORDS.put("kannada", "kn");
        LANG_WORDS.put("bengali", "bn");
        LANG_WORDS.put("bangla", "bn");
        LANG_WORDS.put("marathi", "mr");
        LANG_WORDS.put("english", "en");
        LANG_WORDS.put("western", "en");
        LANG_WORDS.put("korean", "ko");
        LANG_WORDS.put("kpop", "ko");
        LANG_WORDS.put("japanese", "ja");
        LANG_WORDS.put("spanish", "es");
        LANG_WORDS.put("latin", "es");
    }

    /** The language the request itself names ("sad Hindi songs" -> hi), "" when none. */
    static String requestedLanguage(String text) {
        List<String> w = VoiceCore.words(text);
        for (int i = 0; i < w.size(); i++) {
            if (w.get(i).equals("k") && i + 1 < w.size() && w.get(i + 1).equals("pop")) {
                return "ko";
            }
            String l = LANG_WORDS.get(w.get(i));
            if (l != null) {
                return l;
            }
        }
        return "";
    }

    /** Lower-case letters and digits only, single spaces: for matching "Title - Artist" however it is punctuated. */
    static String normName(String s) {
        return s == null ? "" : s.toLowerCase(java.util.Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }

    static String langName(String code) {
        switch (code) {
            case "en": return "English";
            case "hi": return "Hindi";
            case "pa": return "Punjabi";
            case "ta": return "Tamil";
            case "te": return "Telugu";
            case "ml": return "Malayalam";
            case "kn": return "Kannada";
            case "bn": return "Bengali";
            case "mr": return "Marathi";
            case "ja": return "Japanese";
            case "ko": return "Korean";
            case "es": return "Spanish";
            default: return code;
        }
    }

    /** A seed track chosen for a request, with what to call the music and why it was chosen. */
    static final class Pick {
        VoiceClient.Track track;
        /** What is playing, for "Playing <label> on Spotify": "jazzy lo-fi", "your music". */
        String label = "";
        String why = "";
    }

    static final Random RANDOM = new Random();

    /**
     * Chooses the seed for a taste request ("play my music") or a genre or mood ("lo-fi", "something chill").
     * Null when there is nothing better than the plain search (no sign-in, no LLM and nothing in the profile).
     */
    static Pick pick(VoiceClient c, String said, String phrase, boolean taste, int budgetMs) {
        long t0 = System.currentTimeMillis();
        Profile p = profile(c, budgetMs);
        String asked = requestedLanguage(said);
        String lang = !asked.isEmpty() ? asked : p != null ? p.dominant : "";
        Set<String> mine = playedByLauncher(c.cfg.dataDir);
        String langNote = lang.isEmpty() ? "" : asked.isEmpty() ? "your usual " + langName(lang) : langName(lang) + " as asked";
        if (taste) {
            if (p == null) {
                return null;
            }
            List<Item> pool = new ArrayList<>();
            for (Item it : p.tracks) {
                if (mine.contains(it.id)) {
                    continue;
                }
                if (lang.isEmpty() || it.lang.equals(lang) || (asked.isEmpty() && (it.lang.equals("none") || it.lang.isEmpty()))) {
                    pool.add(it);
                }
                if (pool.size() >= 30) {
                    break;
                }
            }
            if (pool.isEmpty() && !asked.isEmpty()) {
                // "kuch accha Punjabi gaana" with no Punjabi in the library: a well-liked song of that language.
                return genre(c, p, said, langName(lang) + " songs", lang, asked, mine, t0, budgetMs);
            }
            if (pool.isEmpty()) {
                return null;
            }
            Item it = weighted(pool, 1.0);
            Pick pk = new Pick();
            pk.track = track(it);
            pk.label = asked.isEmpty() ? "your music" : "your " + langName(lang) + " music";
            pk.why = "taste: one of your songs (" + VoiceCore.join(new ArrayList<>(it.from), ", ") + "; weight "
                    + String.format(Locale.ROOT, "%.1f", it.weight) + ", random among your top " + pool.size()
                    + (langNote.isEmpty() ? "" : ", " + langNote) + ")";
            return pk;
        }
        return genre(c, p, said, phrase, lang, asked, mine, t0, budgetMs);
    }

    private static Pick genre(VoiceClient c, Profile p, String said, String phrase, String lang, String asked, Set<String> mine, long t0, int budgetMs) {
        if (c.cfg.groqKey.isEmpty()) {
            return null;
        }
        List<Item> list = new ArrayList<>();
        if (p != null) {
            for (Item it : p.tracks) {
                if (mine.contains(it.id)) {
                    continue;
                }
                if (!asked.isEmpty() && !it.lang.equals(asked) && !it.lang.equals("none")) {
                    continue;
                }
                list.add(it);
                if (list.size() >= 70) {
                    break;
                }
            }
        }
        StringBuilder songs = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            songs.append(i + 1).append(". ").append(list.get(i).line()).append('\n');
        }
        String langRule = lang.isEmpty() ? "any language"
                : asked.isEmpty() ? langName(lang) + " (the user mostly listens to " + langName(lang) + "; instrumental music is fine too; avoid other languages since the request names none)"
                : langName(lang) + " (the request asks for it)";
        String system = "You choose music for a voice request on a TV. The user asked for: \"" + said.trim() + "\""
                + (phrase.isEmpty() ? "" : " (search phrase: \"" + phrase + "\")") + ". Language: " + langRule + ".\n"
                + (list.isEmpty() ? "" : "Below are songs from the user's own Spotify listening, most-played first. Pick the ones that genuinely fit the request's genre, mood, era or activity, judging from what you know of each song and artist. Be strict: a song fits only if a listener would agree it is that kind of music; for an era (90s, 80s, old) the song must come from it; when songs are asked for, prefer songs with vocals; leave out anything unsure. Neighbouring styles do NOT count: for 'jazz' pick real jazz (vocal or instrumental jazz artists), never lo-fi hip hop, jazz-hop, chillhop or 'jazzy beats' (Nujabes, Uyama Hiroto, jazztek and similar are lo-fi hip hop, not jazz); for 'lo-fi' pick lo-fi, not jazz standards; for 'rock' no pop. Only when the request itself names a blend (jazzy lo-fi, jazz hop) may blends fit.\n")
                + "Reply with JSON only: {\"fit\": [the fitting songs copied exactly as \"Title - Artist\" from the list, best first, at most 6, [] if none], "
                + "\"label\": \"2 to 4 words naming this music for a spoken reply, like jazzy lo-fi, 90s rock, chill jazz, workout music\", "
                + "\"seeds\": [\"Title - Artist\", three real, well-known songs that are clearly this kind of music, in the language above, close to the user's taste], "
                + "\"search\": \"a short Spotify track search phrase for this kind of music in that language\"}";
        Object o;
        try {
            o = llm(c, "Groq taste", system, list.isEmpty() ? "(no songs from the user's library)" : songs.toString(), 9000);
        } catch (VoiceClient.VoiceException e) {
            log("taste pick failed: " + e.getMessage());
            return null;
        }
        if (o == null) {
            return null;
        }
        String label = VoiceJson.str(o, "label").replaceAll("[\u2010-\u2015]", "-").replaceAll("[^\\p{L}\\p{N} '&-]", "").trim();
        if (label.isEmpty() || label.length() > 30) {
            label = phrase;
        }
        List<Item> fits = new ArrayList<>();
        // Picks come back as "Title - Artist" and are matched against the list by name: a model that miscounts
        // list numbers (it once mapped "jazz" to The Clash) can no longer pick an unrelated song.
        for (Object n : VoiceJson.list(o, "fit")) {
            String want = normName(String.valueOf(n));
            if (want.isEmpty()) continue;
            Item hit = null;
            for (Item cand : list) {
                String have = normName(cand.line());
                if (have.equals(want) || have.startsWith(want) || want.startsWith(have)) { hit = cand; break; }
            }
            if (hit == null) {
                for (Item cand : list) {
                    String title = normName(cand.name);
                    if (title.length() > 2 && want.startsWith(title)) { hit = cand; break; }
                }
            }
            if (hit != null && !fits.contains(hit)) fits.add(hit);
        }
        Pick pk = new Pick();
        pk.label = label;
        if (!fits.isEmpty()) {
            List<Item> top = fits.subList(0, Math.min(4, fits.size()));
            Item it = weighted(top, 0.5);
            pk.track = track(it);
            pk.why = "from your listening (" + VoiceCore.join(new ArrayList<>(it.from), ", ") + "): the LLM found " + fits.size()
                    + " of your songs that fit \"" + label + "\", random among the best " + top.size();
            return pk;
        }
        List<String> seeds = new ArrayList<>();
        for (Object s : VoiceJson.list(o, "seeds")) {
            String x = String.valueOf(s).trim();
            if (x.contains(" - ")) {
                seeds.add(x);
            }
        }
        Collections.shuffle(seeds, RANDOM);
        for (String seed : seeds) {
            if (System.currentTimeMillis() - t0 > budgetMs + 9000) {
                break;
            }
            int cut = seed.lastIndexOf(" - ");
            String title = seed.substring(0, cut).trim();
            String artist = seed.substring(cut + 3).trim();
            VoiceClient.Track t = c.spotifyTrack(title, "song", artist, VoiceAgent.SPOTIFY_TIMEOUT_MS);
            if (t != null && !mine.contains(t.id) && VoiceCore.similarity(title, VoiceAgent.songName(t.name)) >= 0.6
                    && VoiceCore.covers(artist, t.artist) >= 0.5) {
                pk.track = t;
                pk.why = "none of your songs fit; a well-known " + label + " song the LLM named (" + seed + "), found on Spotify"
                        + (lang.isEmpty() ? "" : ", in " + langName(lang));
                return pk;
            }
        }
        String search = VoiceJson.str(o, "search").trim();
        if (!search.isEmpty()) {
            VoiceClient.Track t = c.spotifyTrack(search, "genre", "", VoiceAgent.SPOTIFY_TIMEOUT_MS);
            if (t != null) {
                pk.track = t;
                pk.why = "none of your songs fit and the named songs were not found; Spotify's top track for \"" + search + "\"";
                return pk;
            }
        }
        return null;
    }

    /** Weighted random pick; power < 1 flattens the weights so the same song does not win every time. */
    static Item weighted(List<Item> pool, double power) {
        double total = 0;
        for (Item it : pool) {
            total += Math.pow(Math.max(0.05, it.weight), power);
        }
        double r = RANDOM.nextDouble() * total;
        for (Item it : pool) {
            r -= Math.pow(Math.max(0.05, it.weight), power);
            if (r <= 0) {
                return it;
            }
        }
        return pool.get(pool.size() - 1);
    }

    static VoiceClient.Track track(Item it) {
        VoiceClient.Track t = new VoiceClient.Track();
        t.id = it.id;
        t.name = it.name;
        t.artist = it.artist;
        return t;
    }

    /** One JSON-mode Groq call; returns the parsed object (null when the reply has none). */
    static Object llm(VoiceClient c, String label, String system, String user, int timeoutMs) throws VoiceClient.VoiceException {
        String model = c.cfg.groqTasteModel;
        String body = "{\"model\":" + VoiceJson.quote(model) + ",\"temperature\":0.3,\"max_completion_tokens\":1500,"
                + (model.startsWith("openai/gpt-oss") ? "\"reasoning_effort\":\"low\"," : "")
                + "\"response_format\":{\"type\":\"json_object\"},\"messages\":[{\"role\":\"system\",\"content\":" + VoiceJson.quote(system)
                + "},{\"role\":\"user\",\"content\":" + VoiceJson.quote(user) + "}]}";
        Object o = c.json(label, "POST", "https://api.groq.com/openai/v1/chat/completions",
                VoiceClient.headers("Authorization", "Bearer " + c.cfg.groqKey, "Content-Type", "application/json"), VoiceClient.utf8(body), timeoutMs);
        return ScreenRead.jsonIn(VoiceJson.str(o, "choices", 0, "message", "content"));
    }

    // ---------------------------------------------------------------- files

    static String readText(File f) throws java.io.IOException {
        FileInputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            for (int n; (n = in.read(b)) > 0; ) {
                out.write(b, 0, n);
            }
            return out.toString("UTF-8");
        } finally {
            in.close();
        }
    }

    static void writeText(File f, String s) throws java.io.IOException {
        FileOutputStream out = new FileOutputStream(f);
        try {
            out.write(s.getBytes("UTF-8"));
        } finally {
            out.close();
        }
    }

    private static long parseLong(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
