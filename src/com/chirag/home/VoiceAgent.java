package com.chirag.home;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: classes.dex */
final class VoiceAgent {
    static final double CAP = 0.75d;
    static final String CHOOSE = "choose";
    static final int HISTORY_LINES = 15;
    static final double MARGIN_GATE = 0.25d;
    static final double MATCH_GATE = 0.9d;
    static final String MUSIC = "play_music";
    static final String NONE = "none";
    static final String OPEN_APP = "open_app";
    static final String PLAY = "play";
    static final String PLAY_TITLE = "play_title";
    static final String RESUME = "resume";
    static final int ROUTER_TIMEOUT_MS = 2500;
    static final double SHOW = 0.45d;
    static final String UNKNOWN = "unknown";
    static final int WEB_TIMEOUT_MS = 15000;
    static final String YT_CHANNEL = "youtube_channel";
    static final String YT_LATEST = "youtube_latest";
    static final String YT_SEARCH = "youtube_search";
    final VoiceClient.Config cfg;
    final VoiceClient client;
    final VoiceClient.Http http;
    private final List<String> trace = new ArrayList();
    private static final Pattern LATEST_BY = Pattern.compile("^(?:the )?(?:latest|newest|new|recent|naya|nayi|naye|taaza) (?:video|videos|vlog|upload|stream|episode)s? (?:by|of|from) (.+)$");
    private static final Pattern X_LATEST = Pattern.compile("^(.+?) (?:ka |ki |ke |s )?(?:latest|newest|new|recent|naya|nayi|naye|taaza) (?:video|videos|vlog|upload|stream)s?$");
    private static final Pattern CHANNEL = Pattern.compile("^(.+?)(?: ka| ki| ke| s)? channel$");
    private static final Pattern AGAIN = Pattern.compile("^(?:play |watch )?(?:it |that |this |wahi |vahi )?(?:again|once more|phir se|fir se|dobara|dubara)(?: chalao| lagao| play)?$|^replay(?: it| that)?$");
    private static final Pattern AGAIN_X = Pattern.compile("^(?:play |watch |put on )?(.+?) (?:again|once more|phir se|fir se|dobara|dubara)(?: chalao| lagao| play)?$");
    private static final Pattern CONTINUE = Pattern.compile("^(?:continue|resume|carry on|jaari rakho)(?: watching| playing| with)?(?: (?:it|my show|the show|my movie|the movie|what i was watching))?$|^(?:jo dekh raha tha|jo dekh rahe the)(?: wahi| vahi)?(?: chalao| lagao)?$|^(?:wahi|vahi) (?:chalao|lagao)$|^(?:the |my )?(?:show|movie|video|thing) i was watching$");
    private static final Pattern CONTINUE_X = Pattern.compile("^(?:continue|resume)(?: watching)? (.+)$");
    private static final Pattern WHAT_WAS = Pattern.compile("^what (?:was|were) (?:i|we) watching.*$|^(?:main |mai |hum )?kya dekh (?:raha|rahe|rahi) (?:tha|the|thi).*$");
    private static final Pattern NEXT_ONLY = Pattern.compile("^(?:play |watch |start )?(?:the )?(?:next|agla|agli|agle) (?:episode|ep|one)(?: chalao| lagao| play)?$");
    private static final Set<String> VIDEO_W = set("video", "videos", "vlog", "vlogs", "trailer", "teaser", "livestream", "podcast");
    private static final Set<String> MUSIC_W = set("song", "songs", "gaana", "gaane", "gana", "gane", "music", "playlist", "album", "bhajan", "ghazal");
    /** Genre, mood, era and activity words: "play some lo-fi", "sad songs", "90s hits", "workout". */
    static final Set<String> GENRE_W = set("lofi", "jazz", "blues", "edm", "techno", "trance", "house", "remix", "remixes", "instrumental", "beats", "hits", "chartbusters", "qawwali", "sufi", "bhajan", "bhajans", "ghazal", "ghazals", "aarti", "kirtan", "lullaby", "lullabies", "karaoke", "hiphop", "rap", "rock", "metal", "classical", "carnatic", "hindustani", "kpop", "indie", "acoustic", "unplugged", "devotional", "mashup", "jukebox", "dj");
    static final Set<String> MOOD_W = set("chill", "chilled", "sad", "romantic", "workout", "gym", "running", "study", "studying", "focus", "sleep", "sleeping", "relaxing", "calm", "peaceful", "meditation", "upbeat", "energetic", "motivational", "retro", "purane", "puraane", "evergreen", "80s", "90s", "70s", "60s", "2000s", "soothing");
    /** Words that say a request is about a film or a show, not music. */
    private static final Set<String> SCREEN_W = set("movie", "movies", "film", "films", "series", "show", "shows", "season", "episode", "episodes", "trailer", "webseries", "serial", "documentary", "anime");
    private static final Set<String> NEW_W = set("latest", "newest", "new", "recent", "naya", "nayi", "naye", "taaza");
    private static final Pattern SE = Pattern.compile("\\bS(\\d{1,2}) E(\\d{1,4})\\b");
    private static final Set<String> REF_FILLER = set("that", "the", "my", "this", "video", "show", "movie", "film", "series", "one", "i", "was", "watching", "wala", "wali", "vala", "vali", "again");

    static final class Play {
        long duration;
        long lastTs;
        String mediaId;
        long position;
        String pkg = "";
        String title = "";
        String subtitle = "";

        Play() {
        }

        boolean finished() {
            return this.duration > 0 && ((double) this.position) >= ((double) this.duration) * 0.95d;
        }
    }

    static final class Route {
        String app;
        int episode;
        boolean recent;
        int season;
        boolean strong;
        String name = VoiceAgent.UNKNOWN;
        String entity = "";
        String alt = "";
        String what = "";
        /** For play_music: song, artist, album, playlist, genre or mood (empty = not known). */
        String kind = "";
        /** For a genre or mood: a song that fits, "title - artist" (from the router), to start Spotify with. */
        String seed = "";
        String artist = "";
        boolean fromRules = true;

        Route() {
        }

        public String toString() {
            return this.name + (this.what.isEmpty() ? "" : ":" + this.what) + (this.entity.isEmpty() ? "" : " \"" + this.entity + "\"") + (this.season > 0 ? " S" + this.season : "") + (this.episode > 0 ? " E" + this.episode : "") + (this.recent ? " newest" : "") + (this.app != null ? " app=" + this.app : "");
        }
    }

    static final class Candidate {
        double score;
        VoiceCore.Title title;
        String label = "";
        String detail = "";
        String where = "";
        /** What to say aloud when this is played; empty = built from label and where. */
        String say = "";
        List<VoiceCore.Link> links = new ArrayList();

        Candidate() {
        }

        String line() {
            return this.label + (this.detail.isEmpty() ? "" : " · " + this.detail) + (this.where.isEmpty() ? "" : " on " + this.where);
        }

        public String toString() {
            return line();
        }
    }

    private static final class Found {
        boolean action;
        double available;
        final List<Candidate> cands;
        final List<String> caps;
        double history;
        double margin;
        double match;
        String note;
        Candidate search;
        VoiceClient.Result titles;

        private Found() {
            this.cands = new ArrayList();
            this.margin = 1.0d;
            this.available = 1.0d;
            this.caps = new ArrayList();
            this.note = "";
        }
    }

    static final class Outcome {
        double confidence;
        VoiceCore.Query query;
        Candidate search;
        VoiceClient.Result titles;
        String decision = VoiceAgent.NONE;
        String route = VoiceAgent.UNKNOWN;
        String message = "";
        String line = "";
        String trace = "";
        final Map<String, Double> parts = new LinkedHashMap();
        final List<String> caps = new ArrayList();
        final List<Candidate> candidates = new ArrayList();

        Outcome() {
        }

        String breakdown() {
            StringBuilder sb = new StringBuilder(String.format(Locale.ROOT, "%.2f =", Double.valueOf(this.confidence)));
            for (Map.Entry<String, Double> entry : this.parts.entrySet()) {
                sb.append(String.format(Locale.ROOT, " %s %.2f", entry.getKey(), entry.getValue()));
            }
            if (!this.caps.isEmpty()) {
                sb.append("  capped at ").append(VoiceAgent.CAP).append(": ").append(this.caps);
            }
            return sb.toString();
        }
    }

    VoiceAgent(VoiceClient.Config config, VoiceClient.Http http) {
        this.cfg = config;
        this.http = http;
        this.client = new VoiceClient(config, http);
    }

    Outcome run(final String str, List<Play> list, long j) {
        ArrayList<Play> arrayList = new ArrayList<>();
        if (list != null) {
            for (Play play : list) {
                if (play != null && play.title != null && !play.title.trim().isEmpty()) {
                    arrayList.add(play);
                }
            }
        }
        Outcome outcome = new Outcome();
        long jNanoTime = System.nanoTime();
        VoiceCore.Query queryUnderstand = VoiceCore.understand(str);
        outcome.query = queryUnderstand;
        Route routeRules = rulesRoute(queryUnderstand, str);
        final VoiceClient.Routed[] routedArr = new VoiceClient.Routed[1];
        final String strSummary = summary(arrayList, j);
        final VoiceClient voiceClient = new VoiceClient(this.cfg, this.http);
        Thread thread = new Thread(new Runnable() { // from class: com.chirag.home.VoiceAgent.1
            @Override // java.lang.Runnable
            public void run() {
                routedArr[0] = voiceClient.route(str, strSummary, VoiceAgent.ROUTER_TIMEOUT_MS);
            }
        }, "voice-router");
        boolean useRouter = this.cfg.llm && !this.cfg.groqKey.isEmpty() && !queryUnderstand.intent.equals("empty");
        if (useRouter) {
            thread.start();
        }
        Found foundRules = null;
        if (!routeRules.name.equals(UNKNOWN)) {
            foundRules = resolve(routeRules, str, arrayList, j, false);
        }
        long jMs = ms(jNanoTime);
        if (useRouter) {
            try {
                thread.join(Math.max(1L, ROUTER_TIMEOUT_MS - jMs));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        VoiceClient.Routed routed = routedArr[0];
        this.trace.add("understand " + ms(jNanoTime) + "ms: rules=" + routeRules + (routeRules.strong ? " (explicit)" : "") + ", router=" + (routed != null ? routed.toString() : useRouter ? "no answer" + (voiceClient.trace.length() > 0 ? " (" + voiceClient.trace.toString().trim() + ")" : "") : "off"));
        long jNanoTime2 = System.nanoTime();
        double[] dArr = new double[2];
        boolean[] zArr = new boolean[1];
        Route route = reconcile(routeRules, routed, queryUnderstand, dArr, zArr);
        outcome.route = route.name;
        this.trace.add("route: " + route + (route == routeRules ? " (rules' reading" + (routed == null ? ")" : ", router " + (dArr[0] >= 1.0d ? "agrees)" : "differs)")) : " (router's reading)"));
        Found found = route == routeRules ? foundRules : resolve(route, str, arrayList, j, routed != null);
        boolean weak = found == null || found.cands.isEmpty() || found.match < MATCH_GATE;
        if (route == routeRules && routed != null && weak && !route.name.equals(RESUME) && (!route.alt.isEmpty() || route.name.equals(PLAY_TITLE))) {
            Found foundAgain = resolve(route, str, arrayList, j, true);
            if (foundAgain != null && (found == null || found.cands.isEmpty() || foundAgain.match > found.match)) {
                found = foundAgain;
            }
        }
        if (zArr[0] && ((found == null || found.cands.isEmpty()) && foundRules != null && !foundRules.cands.isEmpty())) {
            outcome.route = routeRules.name;
            dArr[0] = 0.5d;
            zArr[0] = false;
            found = foundRules;
            route = routeRules;
        }
        this.trace.add("resolve " + (route == routeRules ? jMs : ms(jNanoTime2)) + "ms: " + (found == null ? "nothing to look up" : found.cands.size() + " found" + (found.note.isEmpty() ? "" : ", " + found.note)) + " [" + this.client.trace.toString().trim() + "]");
        boolean alsoTitle = zArr[0] && foundRules != null && foundRules.titles != null && foundRules.titles.confident()
                && (!route.name.equals(MUSIC) || VoiceCore.similarity(foundRules.titles.titles.get(0).name, queryUnderstand.text) >= MATCH_GATE);
        if (alsoTitle && found != null && found.history <= 0.0d) {
            found.caps.add("could also be the title \"" + foundRules.titles.titles.get(0).name + "\"");
            dArr[0] = 0.3d;
            outcome.titles = foundRules.titles;
        }
        decide(outcome, route, found, dArr, queryUnderstand);
        boolean tryYoutube = route.name.equals(PLAY_TITLE) && (route.app == null || "youtube".equals(route.app)) && (outcome.decision.equals(NONE) || found == null || found.cands.isEmpty() || found.match < MATCH_GATE);
        if (tryYoutube) {
            long jNanoTime3 = System.nanoTime();
            Route routeYt = new Route();
            routeYt.fromRules = false;
            routeYt.name = YT_SEARCH;
            routeYt.app = "youtube";
            routeYt.entity = str.trim().replaceFirst("(?i)^(?:please )?(?:play|watch|put on|show me|show|open|search|search for|chalao|lagao)\\s+", "");
            Found foundYt = resolve(routeYt, str, arrayList, j, false);
            boolean noTitle = outcome.decision.equals(NONE) || found == null || found.cands.isEmpty();
            double titleMatch = 0.0d;
            if (found != null && !found.cands.isEmpty()) {
                titleMatch = found.match;
            }
            boolean weakTitle = !noTitle && titleMatch < 0.8d;
            boolean useYoutube = foundYt != null && !foundYt.cands.isEmpty() && (noTitle || weakTitle || (foundYt.match >= MATCH_GATE && foundYt.match > titleMatch));
            this.trace.add("youtube fallback " + ms(jNanoTime3) + "ms: " + (foundYt == null ? "no answer" : foundYt.cands.size() + " found, match " + String.format(Locale.ROOT, "%.2f", Double.valueOf(foundYt.match)) + " vs title " + String.format(Locale.ROOT, "%.2f", Double.valueOf(titleMatch))) + (useYoutube ? " -> YouTube" : " -> keep"));
            if (useYoutube) {
                if (!noTitle) {
                    foundYt.caps.add("could also be the title \"" + found.cands.get(0).label + "\"");
                }
                outcome.route = YT_SEARCH;
                outcome.titles = null;
                Candidate candidate = noTitle ? null : found.cands.get(0);
                decide(outcome, routeYt, foundYt, noTitle ? new double[]{1.0d, 1.0d} : new double[]{0.8d, 0.8d}, queryUnderstand);
                if (candidate != null && !outcome.decision.equals(PLAY)) {
                    while (outcome.candidates.size() >= 3) {
                        outcome.candidates.remove(outcome.candidates.size() - 1);
                    }
                    outcome.candidates.add(candidate);
                }
                route = routeYt;
            }
        }
        boolean weakChoice = outcome.decision.equals(CHOOSE) && route.name.equals(PLAY_TITLE) && found != null && found.match < 0.8d;
        if ((outcome.decision.equals(NONE) || weakChoice) && this.cfg.webSearch) {
            long jNanoTime4 = System.nanoTime();
            String[] strArrWebSearch = this.client.webSearch(str, WEB_TIMEOUT_MS);
            this.trace.add("web search " + ms(jNanoTime4) + "ms: " + (strArrWebSearch != null ? strArrWebSearch[0] + " \"" + strArrWebSearch[1] + "\"" : "no answer"));
            if (strArrWebSearch != null) {
                Route routeWeb = new Route();
                routeWeb.fromRules = false;
                routeWeb.name = strArrWebSearch[0].equals("channel") ? YT_LATEST : PLAY_TITLE;
                routeWeb.entity = strArrWebSearch[1];
                Found foundWeb = resolve(routeWeb, str, arrayList, j, false);
                if (foundWeb != null) {
                    foundWeb.caps.add("the name came from a web search, not from what was said");
                    foundWeb.match = Math.min(foundWeb.match, 0.5d);
                }
                if (foundWeb != null && !foundWeb.cands.isEmpty()) {
                    outcome.route = routeWeb.name;
                    outcome.titles = null;
                    decide(outcome, routeWeb, foundWeb, new double[]{0.5d, 0.5d}, queryUnderstand);
                }
            }
        }
        this.trace.add("decide: " + outcome.decision + (outcome.candidates.isEmpty() ? "" : " \"" + outcome.candidates.get(0).line() + "\"") + "  confidence " + outcome.breakdown());
        this.trace.add("total " + ms(jNanoTime) + "ms");
        outcome.trace = VoiceCore.join(this.trace, "\n");
        return outcome;
    }

    private static long ms(long j) {
        return (System.nanoTime() - j) / 1000000;
    }

    private static Set<String> set(String... strArr) {
        return new HashSet(Arrays.asList(strArr));
    }

    static Route rulesRoute(VoiceCore.Query query, String str) {
        Route route = new Route();
        String strJoin = VoiceCore.join(VoiceCore.words(str), " ");
        String str2 = query.text;
        route.app = query.app;
        if (query.intent.equals("empty") && strJoin.isEmpty()) {
            return route;
        }
        boolean z = true;
        if (AGAIN.matcher(strJoin).matches()) {
            route.name = RESUME;
            route.what = "again";
            route.strong = true;
            return route;
        }
        if (WHAT_WAS.matcher(strJoin).matches()) {
            route.name = RESUME;
            route.what = "list";
            route.strong = true;
            return route;
        }
        if (CONTINUE.matcher(strJoin).matches()) {
            route.name = RESUME;
            route.what = "continue";
            route.strong = true;
            return route;
        }
        if (NEXT_ONLY.matcher(strJoin).matches()) {
            route.name = RESUME;
            route.what = "next_episode";
            route.strong = true;
            return route;
        }
        if (query.intent.equals(OPEN_APP)) {
            route.name = OPEN_APP;
            route.entity = VoiceCore.app(query.app).label;
            route.strong = true;
            return route;
        }
        if (query.intent.equals("empty")) {
            return route;
        }
        Matcher matcher = AGAIN_X.matcher(strJoin);
        if (matcher.matches()) {
            route.name = RESUME;
            route.what = "again";
            route.entity = matcher.group(1).trim();
            route.strong = true;
            return route;
        }
        if (query.epRef.equals("next")) {
            route.name = RESUME;
            route.what = "next_episode";
            route.entity = str2;
            route.strong = true;
            return route;
        }
        boolean zEquals = "youtube".equals(query.app);
        boolean zEquals2 = "spotify".equals(query.app);
        boolean z2 = (query.app == null || zEquals || zEquals2) ? false : true;
        List<String> listWords = VoiceCore.words(str2);
        if (!z2 && !zEquals2) {
            Matcher latest = LATEST_BY.matcher(str2);
            if (!latest.matches()) {
                latest = X_LATEST.matcher(str2);
            }
            if (latest.matches()) {
                route.name = YT_LATEST;
                route.entity = latest.group(1).trim();
                route.recent = true;
                route.strong = true;
                return route;
            }
            Matcher channel = CHANNEL.matcher(str2);
            if (channel.matches() && (zEquals || listWords.size() <= 5)) {
                route.name = YT_CHANNEL;
                route.entity = channel.group(1).trim();
                route.strong = true;
                return route;
            }
        }
        boolean z3 = false;
        boolean z4 = false;
        boolean genre = false;
        boolean mood = false;
        boolean screen = false;
        String flat = " " + VoiceCore.join(listWords, " ") + " ";
        if (flat.contains(" lo fi ") || flat.contains(" hip hop ") || flat.contains(" old is gold ")) {
            genre = true;
        }
        for (String str3 : listWords) {
            if (MUSIC_W.contains(str3) || "gaano".equals(str3)) {
                z3 = true;
            }
            if (GENRE_W.contains(str3)) {
                genre = true;
            }
            if (MOOD_W.contains(str3)) {
                mood = true;
            }
            if (SCREEN_W.contains(str3)) {
                screen = true;
            }
            if (VIDEO_W.contains(str3)) {
                z4 = true;
            }
            if (NEW_W.contains(str3)) {
                route.recent = true;
            }
        }
        // Music: a music word ("songs", "gaane", "playlist"), a genre ("lo-fi", "jazz", "bhajan"), Spotify named,
        // or a mood/era/activity on its own ("something chill for studying", "90s bollywood", "workout").
        boolean musicy = z3 || (genre && !screen) || (mood && !screen && !z4);
        if (zEquals2 || (musicy && !z2 && !(zEquals && z4))) {
            route.name = MUSIC;
            route.entity = musicQuery(str2);
            route.kind = listWords.contains("album") ? "album" : listWords.contains("playlist") ? "playlist" : genre ? "genre" : mood ? "mood" : "";
            route.strong = zEquals2 || z3 || genre;
            return route;
        }
        if (zEquals || (z4 && !z2)) {
            route.name = YT_SEARCH;
            route.entity = str2;
            route.strong = true;
            return route;
        }
        if (CONTINUE_X.matcher(strJoin).matches() && query.season + query.episode == 0) {
            route.name = RESUME;
            route.what = "continue";
            route.entity = str2;
            route.strong = true;
            return route;
        }
        route.name = PLAY_TITLE;
        route.entity = str2;
        route.season = query.season;
        route.episode = query.episode;
        route.recent = false;
        if (query.app == null && !query.wantsEpisode()) {
            z = false;
        }
        route.strong = z;
        return route;
    }

    static String summary(List<Play> list, long j) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Play play : list) {
            if (i >= HISTORY_LINES) {
                break;
            }
            i++;
            String str = "";
            StringBuilder sbAppend = sb.append(i).append(". ").append(appLabel(play.pkg)).append(": ").append(clip(play.title)).append((play.subtitle == null || play.subtitle.isEmpty()) ? "" : " - " + clip(play.subtitle)).append(" (").append(ago(j - play.lastTs));
            if (play.duration > 0 && !play.finished()) {
                str = ", not finished";
            } else if (play.finished()) {
                str = ", finished";
            }
            sbAppend.append(str).append(")\n");
        }
        return sb.toString().trim();
    }

    private static String clip(String str) {
        String strTrim = str.replaceAll("\\s+", " ").trim();
        return strTrim.length() > 60 ? strTrim.substring(0, 60) + "…" : strTrim;
    }

    static String ago(long j) {
        StringBuilder sbAppend;
        String str;
        long jMax = Math.max(0L, j) / 60000;
        if (jMax < 1) {
            return "just now";
        }
        if (jMax < 60) {
            sbAppend = new StringBuilder().append(jMax);
            str = " min ago";
        } else if (jMax < 1440) {
            sbAppend = new StringBuilder().append(jMax / 60);
            str = " h ago";
        } else {
            if (jMax < 2880) {
                return "yesterday";
            }
            sbAppend = new StringBuilder().append(jMax / 1440);
            str = " days ago";
        }
        return sbAppend.append(str).toString();
    }

    static String appLabel(String str) {
        VoiceCore.App appAppForPkg = VoiceCore.appForPkg(str);
        return appAppForPkg != null ? appAppForPkg.label : str;
    }

    private static String family(String str) {
        return (str.equals(YT_LATEST) || str.equals(YT_CHANNEL) || str.equals(YT_SEARCH) || str.equals(MUSIC)) ? "youtube" : str;
    }

    private static double same(String str, String str2) {
        String strEssence = YouTubeSource.essence(str);
        String strEssence2 = YouTubeSource.essence(str2);
        if (strEssence.isEmpty() && strEssence2.isEmpty()) {
            return 1.0d;
        }
        return VoiceCore.similarity(strEssence, strEssence2);
    }

    Route reconcile(Route route, VoiceClient.Routed routed, VoiceCore.Query query, double[] dArr, boolean[] zArr) {
        double d;
        if (routed == null) {
            dArr[0] = route.strong ? 0.8d : 0.6d;
            dArr[1] = 0.7d;
            return route;
        }
        Route route2 = new Route();
        route2.fromRules = false;
        route2.name = routed.tool;
        route2.app = query.app;
        route2.recent = route.recent;
        if (route2.name.equals(PLAY_TITLE)) {
            route2.entity = routed.arg("title");
            route2.season = routed.num("season");
            route2.episode = routed.num("episode");
        } else if (route2.name.equals(YT_LATEST) || route2.name.equals(YT_CHANNEL)) {
            route2.entity = routed.arg("channel");
            route2.recent = route2.name.equals(YT_LATEST);
        } else if (route2.name.equals(YT_SEARCH)) {
            route2.entity = routed.arg("query");
        } else if (route2.name.equals(MUSIC)) {
            route2.entity = routed.arg("query");
            route2.kind = routed.arg("kind").toLowerCase(Locale.ROOT);
            route2.seed = routed.arg("seed");
            route2.artist = routed.arg("artist");
            if (route2.entity.isEmpty()) {
                route2.entity = VoiceCore.join(java.util.Arrays.asList(routed.arg("artist"), routed.arg("genre")), " ").trim();
            }
        } else if (route2.name.equals(RESUME)) {
            route2.what = routed.arg("what");
            route2.entity = routed.arg("reference");
            if (route2.what.isEmpty()) {
                route2.what = "continue";
            }
        } else if (route2.name.equals(OPEN_APP)) {
            VoiceCore.Query queryUnderstand = VoiceCore.understand("open " + routed.arg("app"));
            if (queryUnderstand.app == null) {
                route2.name = UNKNOWN;
            } else {
                route2.app = queryUnderstand.app;
                route2.entity = VoiceCore.app(queryUnderstand.app).label;
            }
        } else {
            route2.name = UNKNOWN;
        }
        if (route2.name.equals(MUSIC) && !route2.entity.isEmpty() && route.name.equals(YT_SEARCH) && !route.entity.isEmpty()) {
            // "play Coldplay on YouTube": the rules only know YouTube was named; the router knows it is music.
            dArr[0] = 0.8d;
            dArr[1] = same(route.entity, route2.entity);
            return route2;
        }
        if (route2.name.equals(RESUME) && route.name.equals(PLAY_TITLE) && !route.strong && !route2.entity.isEmpty()
                && same(route.entity, route2.entity) >= 0.9d) {
            // "play Panchayat" while it is in the history: naming the title is enough, the app resumes it.
            dArr[0] = 0.8d;
            dArr[1] = 1.0d;
            return route;
        }
        if (!route2.name.equals(UNKNOWN)) {
            if (!route.name.equals(UNKNOWN) || !route2.entity.isEmpty() || route2.name.equals(RESUME)) {
                if (route.name.equals(UNKNOWN)) {
                    dArr[0] = 0.6d;
                    dArr[1] = 0.7d;
                    return route2;
                }
                dArr[1] = (route.name.equals(RESUME) && route2.name.equals(RESUME) && route.entity.isEmpty()) ? 1.0d : same(route.entity, route2.entity);
                if (!route2.name.equals(route.name)) {
                    if (family(route2.name).equals(family(route.name))) {
                        dArr[0] = 0.8d;
                        if (route.strong && (!route.name.equals(YT_SEARCH) || !route2.name.equals(YT_LATEST) || !route.recent)) {
                            route.alt = route2.entity;
                            return route;
                        }
                        return route2;
                    }
                    if (route.strong) {
                        dArr[0] = 0.5d;
                        return route;
                    }
                    dArr[0] = 0.6d;
                    zArr[0] = true;
                    return route2;
                }
                dArr[0] = (!route.name.equals(RESUME) || route.what.equals(route2.what) || route.what.equals("list")) ? 1.0d : 0.8d;
                if (route.name.equals(MUSIC)) {
                    if (!route2.kind.isEmpty()) {
                        route.kind = route2.kind;
                    }
                    route.seed = route2.seed;
                    route.artist = route2.artist;
                }
                if (!route2.entity.isEmpty() && !route2.entity.equalsIgnoreCase(route.entity)) {
                    route.alt = route2.entity;
                }
                if (route.name.equals(PLAY_TITLE) && route.season + route.episode == 0 && route2.season + route2.episode > 0 && dArr[1] >= 0.72d) {
                    route.season = route2.season;
                    route.episode = route2.episode;
                }
                return route;
            }
            d = 0.8d;
        } else {
            d = 0.8d;
        }
        if (!route.strong) {
            d = 0.6d;
        }
        dArr[0] = d;
        dArr[1] = 0.7d;
        return route;
    }

    private Found resolve(Route route, String str, List<Play> list, long j, boolean z) {
        try {
            if (route.name.equals(PLAY_TITLE)) {
                return titles(route, str, list, z);
            }
            if (!route.name.equals(YT_LATEST) && !route.name.equals(YT_CHANNEL)) {
                if (route.name.equals(YT_SEARCH)) {
                    return videos(route, list);
                }
                if (route.name.equals(MUSIC)) {
                    return music(route, list);
                }
                if (route.name.equals(RESUME)) {
                    Found foundResume = resume(route, list, j);
                    if (foundResume.cands.isEmpty() && !route.entity.isEmpty()) {
                        Route route2 = new Route();
                        route2.name = PLAY_TITLE;
                        route2.entity = route.entity;
                        route2.app = route.app;
                        route2.fromRules = route.fromRules && !route.what.equals("again");
                        Found foundTitles = titles(route2, str, list, z);
                        foundTitles.note = foundResume.note + "; looked up as a title";
                        return foundTitles;
                    }
                    return foundResume;
                }
                if (route.name.equals(OPEN_APP)) {
                    return action("Open " + route.entity, route.app, "");
                }
                return null;
            }
            return channel(route, list, j);
        } catch (VoiceClient.VoiceException e) {
            return failed(e.getMessage());
        } catch (IOException e2) {
            return failed(e2.getMessage());
        }
    }

    private static Found failed(String str) {
        Found found = new Found();
        found.note = str;
        return found;
    }

    private Found action(String str, String str2, String str3) {
        Found found = new Found();
        Candidate candidate = new Candidate();
        candidate.label = str;
        candidate.links = VoiceCore.searchLinks(str2, str3, this.cfg.searchLinks);
        if (candidate.links.isEmpty()) {
            return failed("that app is not on this TV");
        }
        found.cands.add(candidate);
        found.action = true;
        found.match = 1.0d;
        return found;
    }

    private static Play played(List<Play> list, String str) {
        for (Play play : list) {
            if (play.title.equalsIgnoreCase(str)) {
                return play;
            }
        }
        return null;
    }

    private Found titles(Route route, String str, List<Play> list, boolean z) throws VoiceClient.VoiceException {
        VoiceCore.Query query;
        String str2;
        Found found = new Found();
        VoiceCore.Query queryUnderstand = route.fromRules ? VoiceCore.understand(str) : VoiceCore.understand(route.entity);
        if (queryUnderstand.text.isEmpty()) {
            return failed("no title");
        }
        if (route.season > 0) {
            queryUnderstand.season = route.season;
        }
        if (route.episode > 0) {
            queryUnderstand.episode = route.episode;
        }
        if (queryUnderstand.app == null) {
            queryUnderstand.app = route.app;
        }
        ArrayList<String> arrayList = new ArrayList(queryUnderstand.variants);
        boolean z4 = !route.alt.isEmpty() && VoiceCore.similarity(route.alt, queryUnderstand.text) < 0.95d;
        if (z4) {
            queryUnderstand.variants.add(route.alt);
        }
        VoiceClient.Result resultLookup = this.client.lookup(queryUnderstand, z && !z4);
        found.titles = resultLookup;
        List<VoiceCore.Title> list2 = resultLookup.titles;
        if (list2.isEmpty()) {
            return found;
        }
        if (list2.size() > 1) {
            query = queryUnderstand;
            if (list2.get(0).sim - list2.get(1).sim < 0.05d && played(list, list2.get(0).name) == null && played(list, list2.get(1).name) != null) {
                list2.add(0, list2.remove(1));
            }
        } else {
            query = queryUnderstand;
        }
        for (VoiceCore.Title title : list2) {
            Play playPlayed = played(list, title.name);
            VoiceCore.App appAppForPkg = playPlayed == null ? null : VoiceCore.appForPkg(playPlayed.pkg);
            VoiceCore.Query query2 = query;
            List<VoiceCore.Offer> listApps = title.apps(query2.app);
            if (query2.app != null && title.offers.containsKey(query2.app)) {
                str2 = query2.app;
            } else if (appAppForPkg == null || !title.offers.containsKey(appAppForPkg.key) || appAppForPkg.pkg == null) {
                str2 = listApps.isEmpty() ? null : listApps.get(0).app;
            } else {
                str2 = appAppForPkg.key;
            }
            Candidate candidate = new Candidate();
            candidate.label = title.name + (title.target != null ? " · " + title.target.label() : title.year.isEmpty() ? "" : " (" + title.year + ")");
            candidate.title = title;
            candidate.score = title.sim;
            if (str2 != null) {
                candidate.where = VoiceCore.app(str2).label;
                candidate.links = VoiceCore.episodeLinks(title, str2, true, this.cfg.searchLinks);
            }
            found.cands.add(candidate);
            query = query2;
        }
        VoiceCore.Query query3 = query;
        VoiceCore.Title title2 = list2.get(0);
        for (String str3 : arrayList) {
            found.match = Math.max(found.match, Math.max(VoiceCore.covers(str3, title2.name), VoiceCore.similarity(str3, title2.name)));
            list2 = list2;
            query3 = query3;
        }
        List<VoiceCore.Title> list3 = list2;
        VoiceCore.Query query4 = query3;
        if (z4) {
            found.match = Math.max(found.match, Math.max(VoiceCore.covers(route.alt, title2.name), VoiceCore.similarity(route.alt, title2.name)) * MATCH_GATE);
        }
        found.margin = 1.0d;
        int i2 = 1;
        while (i2 < list3.size()) {
            List<VoiceCore.Title> list4 = list3;
            VoiceCore.Title title3 = list4.get(i2);
            if ((!query4.wantsEpisode() || title3.type.equals("tv")) && (!found.cands.get(i2).links.isEmpty() || found.cands.get(0).links.isEmpty())) {
                found.margin = Math.min(1.0d, Math.max(0.0d, title2.sim - title3.sim) / 0.2d);
                break;
            }
            i2++;
            list3 = list4;
        }
        if (played(list, title2.name) != null) {
            found.history = 0.05d;
            found.margin = 1.0d;
        }
        found.available = found.cands.get(0).links.isEmpty() ? 0.3d : 1.0d;
        if (found.available < 1.0d) {
            found.caps.add("not on an installed app");
        }
        if (query4.wantsEpisode() && title2.type.equals("tv") && (title2.target == null || (!title2.target.note.isEmpty() && title2.target.seasonOnly))) {
            found.caps.add("that episode was not found");
        }
        return found;
    }

    private static Set<String> playedChannels(List<Play> list) {
        HashSet hashSet = new HashSet();
        for (Play play : list) {
            if ("com.google.android.youtube.tv".equals(play.pkg) && play.subtitle != null && !play.subtitle.isEmpty()) {
                hashSet.add(play.subtitle.toLowerCase(Locale.ROOT));
            }
        }
        return hashSet;
    }

    private Candidate video(YouTubeSource.Video video, long j) {
        Candidate candidate = new Candidate();
        candidate.label = video.title;
        candidate.detail = video.channel + (j >= 0 ? " · " + YouTubeSource.age(j) : "");
        candidate.links.add(new VoiceCore.Link("VIEW", "com.google.android.youtube.tv", video.url(), PLAY));
        candidate.links.addAll(VoiceCore.searchLinks("youtube", video.title, this.cfg.searchLinks));
        candidate.title = new VoiceCore.Title();
        candidate.title.name = video.title;
        candidate.title.subOverride = video.channel;
        candidate.title.artOverride = video.thumb();
        candidate.score = video.score;
        return candidate;
    }

    private Candidate youtubeSearch(String str) {
        Candidate candidate = new Candidate();
        candidate.label = "Search YouTube for “" + str + "”";
        candidate.links = VoiceCore.searchLinks("youtube", str, this.cfg.searchLinks);
        return candidate;
    }

    private Found channel(Route route, List<Play> list, long j) throws IOException {
        Found found = new Found();
        found.search = youtubeSearch(route.entity);
        Set<String> setPlayedChannels = playedChannels(list);
        List<YouTubeSource.Channel> listChannels = YouTubeSource.channels(this.http, route.entity);
        YouTubeSource.rankChannels(listChannels, route.entity, route.alt, setPlayedChannels);
        if (listChannels.isEmpty() || listChannels.get(0).sim < MATCH_GATE) {
            HashSet hashSet = new HashSet();
            Iterator<YouTubeSource.Channel> it = listChannels.iterator();
            while (it.hasNext()) {
                hashSet.add(it.next().id);
            }
            for (YouTubeSource.Channel channel : YouTubeSource.channels(this.http, route.entity)) {
                if (hashSet.add(channel.id)) {
                    listChannels.add(channel);
                }
            }
            YouTubeSource.rankChannels(listChannels, route.entity, route.alt, setPlayedChannels);
        }
        if (listChannels.isEmpty()) {
            return found;
        }
        YouTubeSource.Channel channel2 = listChannels.get(0);
        found.match = channel2.sim;
        found.margin = listChannels.size() >= 2 ? Math.min(1.0d, Math.max(0.0d, channel2.score - listChannels.get(1).score) / 0.2d) : 1.0d;
        if (setPlayedChannels.contains(channel2.name.toLowerCase(Locale.ROOT))) {
            found.history = 0.05d;
        }
        if (route.name.equals(YT_CHANNEL)) {
            for (int i = 0; i < listChannels.size() && i < 3; i++) {
                Candidate candidate = new Candidate();
                candidate.label = listChannels.get(i).name;
                candidate.detail = "YouTube channel";
                candidate.score = listChannels.get(i).score;
                candidate.links.add(new VoiceCore.Link("VIEW", "com.google.android.youtube.tv", "https://www.youtube.com/channel/" + listChannels.get(i).id, "open channel"));
                candidate.links.addAll(VoiceCore.searchLinks("youtube", listChannels.get(i).name, this.cfg.searchLinks));
                found.cands.add(candidate);
            }
            return found;
        }
        int i2 = channel2.sim < MATCH_GATE ? 3 : 1;
        for (int i3 = 0; i3 < listChannels.size() && i3 < i2; i3++) {
            if (i3 <= 0 || listChannels.get(i3).sim >= 0.5d) {
                try {
                    List<YouTubeSource.Video> listFeed = YouTubeSource.feed(this.http, listChannels.get(i3).id);
                    if (!listFeed.isEmpty()) {
                        YouTubeSource.Video video = listFeed.get(0);
                        if (video.channel.isEmpty()) {
                            video.channel = listChannels.get(i3).name;
                        }
                        Candidate candidateVideo = video(video, YouTubeSource.daysSince(video.published, j));
                        candidateVideo.score = listChannels.get(i3).score;
                        found.cands.add(candidateVideo);
                    }
                } catch (IOException e) {
                    if (i3 == 0) {
                        throw e;
                    }
                }
            }
        }
        found.note = "channel " + channel2 + " name match " + String.format(Locale.ROOT, "%.2f", Double.valueOf(channel2.sim));
        return found;
    }

    private Found videos(Route route, List<Play> list) throws IOException {
        Found found = new Found();
        String str = !route.alt.isEmpty() ? route.alt : route.entity;
        boolean anyGood = false;
        if (route.name.equals(MUSIC)) {
            String q = musicSearch(route);
            List<String> w = VoiceCore.words(q);
            boolean said = w.contains("song") || w.contains("songs") || w.contains("music") || w.contains("playlist") || w.contains("album") || w.contains("jukebox");
            anyGood = isBroad(route.kind) || "artist".equals(route.kind);
            if (said) {
                str = q;
            } else if (anyGood) {
                str = q + " music";
            } else if ("artist".equals(route.kind)) {
                str = q + " songs";
            } else {
                str = q + (route.recent ? " new song" : " song");
            }
        }
        found.search = youtubeSearch(str);
        Set<String> setPlayedChannels = playedChannels(list);
        List<YouTubeSource.Video> listSearch = YouTubeSource.search(this.http, str);
        YouTubeSource.rankVideos(listSearch, route.entity, route.recent, setPlayedChannels);
        int i = 0;
        for (int i2 = 1; i2 < listSearch.size() && i2 < 3; i2++) {
            if (listSearch.get(i2).cover >= listSearch.get(i).cover + MARGIN_GATE) {
                i = i2;
            }
        }
        if (i > 0) {
            listSearch.add(0, listSearch.remove(i));
        }
        for (int i3 = 0; i3 < listSearch.size() && i3 < 3; i3++) {
            found.cands.add(video(listSearch.get(i3), listSearch.get(i3).ageDays));
        }
        if (listSearch.isEmpty()) {
            return found;
        }
        found.match = listSearch.get(0).cover;
        found.margin = listSearch.size() >= 2 ? Math.min(1.0d, Math.max(0.0d, listSearch.get(0).score - listSearch.get(1).score) / 0.15d) : 1.0d;
        if (anyGood) {
            // A genre or a mood: any of the top results is what was asked for.
            found.match = Math.max(found.match, MATCH_GATE);
            found.margin = 1.0d;
        } else if (route.name.equals(MUSIC) && found.match >= MATCH_GATE) {
            // A named song: other uploads of the same song scoring close is not a real doubt.
            found.margin = 1.0d;
        }
        if (setPlayedChannels.contains(listSearch.get(0).channel.toLowerCase(Locale.ROOT))) {
            found.history = 0.05d;
        }
        return found;
    }

    static boolean isBroad(String kind) {
        return "genre".equals(kind) || "mood".equals(kind) || "playlist".equals(kind);
    }

    /** The words to search a music app with: the router's phrase when there is one, else the cleaned request. */
    static String musicSearch(Route route) {
        String q = !route.alt.isEmpty() ? route.alt : route.entity;
        q = musicQuery(q);
        return q.isEmpty() ? route.entity : q;
    }

    private static final Set<String> MUSIC_FILLER = set("some", "something", "any", "kuch", "koi", "me", "for", "please", "play", "put", "on", "kuchh", "acha", "achha", "accha", "achhe", "ache", "acche", "lagao", "chalao", "sunao", "suna", "bajao", "baja", "sunna", "hai");

    /** "some sad hindi gaane" -> "sad hindi songs", "AR Rahman ke gaane" -> "ar rahman songs", "purane gaane" -> "old hindi songs". */
    static String musicQuery(String text) {
        List<String> out = new ArrayList();
        List<String> w = VoiceCore.words(text);
        boolean old = false;
        for (int i = 0; i < w.size(); i++) {
            String x = w.get(i);
            if (out.isEmpty() && (MUSIC_FILLER.contains(x) || x.equals("the") || x.equals("album") || x.equals("playlist"))) {
                continue;
            }
            if ((x.equals("ke") || x.equals("ka") || x.equals("ki")) && i + 1 < w.size() && MUSIC_W.contains(w.get(i + 1))) {
                continue;
            }
            if (x.equals("gaane") || x.equals("gane") || x.equals("gaano") || x.equals("geet")) {
                x = "songs";
            } else if (x.equals("gaana") || x.equals("gana")) {
                x = "song";
            } else if (x.equals("purane") || x.equals("puraane") || x.equals("purana")) {
                x = "old";
                old = true;
            } else if (x.equals("naye") || x.equals("naya") || x.equals("nayi")) {
                x = "new";
            } else if (x.equals("lo") && i + 1 < w.size() && w.get(i + 1).equals("fi")) {
                x = "lofi";
                i++;
            }
            out.add(x);
        }
        while (!out.isEmpty() && MUSIC_FILLER.contains(out.get(out.size() - 1))) {
            out.remove(out.size() - 1);
        }
        if (old && !out.contains("hindi") && !out.contains("english") && !out.contains("punjabi")) {
            out.add(out.indexOf("old") + 1, "hindi");
        }
        return VoiceCore.join(out, " ");
    }

    static final int SPOTIFY_TIMEOUT_MS = 4000;

    /**
     * Music: Spotify by default, YouTube when it was named. Spotify on the TV only starts playing from a track link,
     * so the request is turned into a track with the Spotify Web API (the song, the artist's top match, or for a
     * genre or mood a fitting seed song, after which Spotify's autoplay keeps similar music going). Without Spotify
     * keys, or when nothing is found, the music plays from YouTube instead, unless Spotify was asked for by name.
     */
    private Found music(Route route, List<Play> list) throws IOException {
        String q = musicSearch(route);
        boolean wantYoutube = "youtube".equals(route.app);
        boolean askedSpotify = "spotify".equals(route.app);
        String why = "";
        if (!wantYoutube && this.cfg.has("spotify")) {
            boolean broad = isBroad(route.kind);
            String lookFor = broad && !route.seed.isEmpty() ? route.seed.replace(" - ", " ") : broad ? q : withoutSongWord(q);
            VoiceClient.Track track = this.client.spotifyTrack(lookFor, broad && !route.seed.isEmpty() ? "song" : route.kind, route.artist, SPOTIFY_TIMEOUT_MS);
            if (track == null && broad && !route.seed.isEmpty()) {
                track = this.client.spotifyTrack(q, route.kind, route.artist, SPOTIFY_TIMEOUT_MS);
            }
            if (track != null) {
                Found found = new Found();
                Candidate c = new Candidate();
                boolean named = !broad && !"artist".equals(route.kind)
                        && Math.max(VoiceCore.similarity(lookFor, track.name), VoiceCore.covers(lookFor, track.name + " " + track.artist)) >= 0.6d;
                if (broad) {
                    c.label = "Playing " + q + " on Spotify";
                    c.detail = "starting with " + track.name + " · " + track.artist;
                    c.say = "Playing " + speakable(q) + " on Spotify";
                } else if ("artist".equals(route.kind)) {
                    c.label = "Playing " + track.artist + " on Spotify";
                    c.detail = songName(track.name);
                    c.say = "Playing " + track.artist + " on Spotify";
                } else {
                    c.label = "Playing " + track.name + " on Spotify";
                    c.detail = track.artist;
                    c.say = "Playing " + songName(track.name) + " by " + track.artist + " on Spotify";
                }
                c.links = VoiceCore.spotifyTrackLinks(track.id, q, this.cfg.searchLinks);
                found.cands.add(c);
                found.action = true;
                found.match = (broad || "artist".equals(route.kind) || named) ? 1.0d : 0.7d;
                if (!named && !broad && !"artist".equals(route.kind)) {
                    found.caps.add("Spotify's track \"" + track.name + "\" is not quite what was said");
                }
                found.note = "Spotify track " + track + " for \"" + q + "\"" + (route.kind.isEmpty() ? "" : " as " + route.kind) + (route.seed.isEmpty() ? "" : ", seed \"" + route.seed + "\"");
                return found;
            }
            why = this.cfg.hasSpotifyApi() ? "no Spotify track found" : "no Spotify keys in voice.json";
            if (askedSpotify) {
                // Spotify was asked for by name: open it on a search for the words (it cannot be started from here).
                Found found = new Found();
                Candidate c = new Candidate();
                c.label = "Open Spotify for " + q;
                c.say = "Opening Spotify";
                c.links = VoiceCore.searchLinks("spotify", q, this.cfg.searchLinks);
                found.cands.add(c);
                found.action = true;
                found.match = 1.0d;
                found.note = why + "; opening Spotify";
                return found;
            }
        } else if (!wantYoutube) {
            why = "Spotify is not installed";
        }
        Found found = videos(route, list);
        if (found.cands.isEmpty() && !wantYoutube && this.cfg.has("spotify")) {
            Found open = new Found();
            Candidate c = new Candidate();
            c.label = "Open Spotify for " + q;
            c.say = "Opening Spotify";
            c.links = VoiceCore.searchLinks("spotify", q, this.cfg.searchLinks);
            open.cands.add(c);
            open.action = true;
            open.match = 1.0d;
            open.note = why + "; nothing on YouTube either, opening Spotify";
            return open;
        }
        found.note = (found.note.isEmpty() ? "" : found.note + "; ") + "music on YouTube" + (why.isEmpty() ? "" : " (" + why + ")");
        if (!found.cands.isEmpty()) {
            found.cands.get(0).say = "Playing " + speakable(q) + " on YouTube";
        }
        if (!wantYoutube && this.cfg.has("spotify") && found.cands.size() < 3) {
            Candidate c = new Candidate();
            c.label = "Spotify: " + q;
            c.links = VoiceCore.searchLinks("spotify", q, this.cfg.searchLinks);
            found.cands.add(c);
        }
        return found;
    }

    /** "Kesariya (From \"Brahmastra\")" -> "Kesariya": the part worth saying aloud. */
    static String songName(String name) {
        String s = name.replaceAll("\\s*[(\\[][^)\\]]*[)\\]]", "").replaceAll("\\s+-\\s+.*$", "").trim();
        return s.isEmpty() ? name : s;
    }

    /** "tum hi ho song" -> "tum hi ho": the word song does not help find a song by name. */
    static String withoutSongWord(String q) {
        List<String> w = VoiceCore.words(q);
        List<String> out = new ArrayList();
        for (String x : w) {
            if (!x.equals("song") && !x.equals("songs") && !x.equals("track")) {
                out.add(x);
            }
        }
        return out.isEmpty() ? q : VoiceCore.join(out, " ");
    }

    /** "lofi" is said "lo-fi"; keep everything else as it is. */
    static String speakable(String q) {
        return q.replaceAll("\\blofi\\b", "lo-fi").replaceAll("\\bedm\\b", "E D M");
    }

    private static String refWords(String str) {
        ArrayList arrayList = new ArrayList();
        for (String str2 : VoiceCore.words(str)) {
            if (!REF_FILLER.contains(str2)) {
                arrayList.add(str2);
            }
        }
        return VoiceCore.join(arrayList, " ");
    }

    private Candidate reopen(Play play) {
        Candidate candidate = new Candidate();
        candidate.label = play.title;
        candidate.detail = play.subtitle == null ? "" : play.subtitle;
        candidate.where = appLabel(play.pkg);
        candidate.links = VoiceCore.historyLinks(play.pkg, play.mediaId, play.title, play.position, play.finished());
        candidate.title = new VoiceCore.Title();
        candidate.title.name = play.title;
        candidate.title.subOverride = play.subtitle != null ? play.subtitle : "";
        return candidate;
    }

    private Found resume(Route route, List<Play> list, long j) throws VoiceClient.VoiceException {
        double dMax;
        Candidate candidate;
        Found found = new Found();
        if (list.isEmpty()) {
            return failed("the watch history is empty");
        }
        String strRefWords = refWords(route.entity);
        boolean zEquals = route.what.equals("next_episode");
        ArrayList arrayList = new ArrayList();
        for (Play play : list) {
            if (!zEquals || (play.subtitle != null && SE.matcher(play.subtitle).find())) {
                if (!route.what.equals("continue") || !strRefWords.isEmpty() || !play.finished()) {
                    arrayList.add(play);
                }
            }
        }
        if (arrayList.isEmpty()) {
            return failed(zEquals ? "no episode in the watch history" : "nothing unfinished in the watch history");
        }
        final LinkedHashMap linkedHashMap = new LinkedHashMap();
        Iterator it = arrayList.iterator();
        while (true) {
            dMax = 0.0d;
            if (!it.hasNext()) {
                break;
            }
            Play play2 = (Play) it.next();
            String str = play2.title + " " + (play2.subtitle == null ? "" : play2.subtitle);
            if (!strRefWords.isEmpty()) {
                dMax = Math.max(VoiceCore.similarity(strRefWords, play2.title), VoiceCore.covers(strRefWords, str));
            }
            linkedHashMap.put(play2, Double.valueOf(dMax));
        }
        if (!strRefWords.isEmpty()) {
            Collections.sort(arrayList, new Comparator<Play>() { // from class: com.chirag.home.VoiceAgent.2

                @Override // java.util.Comparator
                public int compare(Play play3, Play play4) {
                    return Double.compare(((Double) linkedHashMap.get(play4)).doubleValue(), ((Double) linkedHashMap.get(play3)).doubleValue());
                }
            });
        }
        Play play3 = (Play) arrayList.get(0);
        if (strRefWords.isEmpty()) {
            long j2 = j - play3.lastTs;
            if (!route.what.equals("list")) {
                dMax = j2 <= 129600000 ? 1.0d : j2 <= 604800000 ? MATCH_GATE : 0.7d;
            }
            found.match = dMax;
        } else {
            found.match = ((Double) linkedHashMap.get(play3)).doubleValue();
            found.margin = arrayList.size() < 2 ? 1.0d : Math.min(1.0d, Math.max(0.0d, ((Double) linkedHashMap.get(play3)).doubleValue() - ((Double) linkedHashMap.get(arrayList.get(1))).doubleValue()) / 0.2d);
            if (found.match < SHOW) {
                return failed("nothing like \"" + strRefWords + "\" in the watch history");
            }
        }
        if (route.what.equals("list")) {
            found.caps.add("a question, not a request to play");
        }
        for (int i = 0; i < arrayList.size() && i < 3; i++) {
            Candidate candidateReopen = reopen((Play) arrayList.get(i));
            candidateReopen.score = strRefWords.isEmpty() ? 1.0d - (((double) i) * 0.1d) : ((Double) linkedHashMap.get(arrayList.get(i))).doubleValue();
            found.cands.add(candidateReopen);
        }
        if (!zEquals) {
            return found;
        }
        Matcher matcher = SE.matcher(play3.subtitle);
        matcher.find();
        int i2 = Integer.parseInt(matcher.group(1));
        int i3 = Integer.parseInt(matcher.group(2));
        VoiceCore.App appAppForPkg = VoiceCore.appForPkg(play3.pkg);
        VoiceCore.Title titleArtMatch = appAppForPkg == null ? null : VoiceCore.artMatch(appAppForPkg.key, play3.title, this.client.justWatch(play3.title), false);
        if (titleArtMatch == null || !titleArtMatch.type.equals("tv")) {
            candidate = null;
        } else {
            int i4 = 0;
            Candidate candidate2 = null;
            while (i4 < 2 && candidate2 == null) {
                VoiceCore.Query query = new VoiceCore.Query();
                query.season = i4 == 0 ? i2 : i2 + 1;
                query.episode = i4 == 0 ? i3 + 1 : 1;
                titleArtMatch.target = null;
                this.client.resolveEpisode(query, titleArtMatch);
                if (titleArtMatch.target != null && !titleArtMatch.target.seasonOnly) {
                    if (titleArtMatch.target.ep.offers.containsKey(appAppForPkg.key)) {
                        candidate2 = new Candidate();
                        candidate2.label = titleArtMatch.name + " · " + titleArtMatch.target.label();
                        candidate2.detail = VoiceCore.nice(titleArtMatch.target.ep.name);
                        candidate2.where = appAppForPkg.label;
                        candidate2.links = VoiceCore.episodeLinks(titleArtMatch, appAppForPkg.key, true, this.cfg.searchLinks);
                        candidate2.title = titleArtMatch;
                        candidate2.score = 1.0d;
                    }
                }
                i4++;
            }
            candidate = candidate2;
        }
        if (candidate != null) {
            found.cands.add(0, candidate);
        } else {
            found.caps.add("the next episode after S" + i2 + " E" + i3 + " could not be found; offering the show");
        }
        while (found.cands.size() > 3) {
            found.cands.remove(found.cands.size() - 1);
        }
        return found;
    }

    private void decide(Outcome outcome, Route route, Found found, double[] dArr, VoiceCore.Query query) {
        String strCapital;
        outcome.candidates.clear();
        outcome.parts.clear();
        outcome.caps.clear();
        outcome.search = found == null ? null : found.search;
        if (found != null && found.titles != null && outcome.titles == null) {
            outcome.titles = found.titles;
        }
        if (found == null || found.cands.isEmpty()) {
            outcome.decision = NONE;
            outcome.confidence = 0.0d;
            if (found == null || found.note.isEmpty() || found.titles != null) {
                strCapital = "Nothing found for “" + (route.entity.isEmpty() ? query.text : route.entity) + "”";
            } else {
                strCapital = capital(found.note);
            }
            outcome.message = strCapital;
            return;
        }
        for (Candidate candidate : found.cands) {
            if (outcome.candidates.size() < 3) {
                outcome.candidates.add(candidate);
            }
        }
        outcome.parts.put("route", Double.valueOf(dArr[0]));
        outcome.parts.put("entity", Double.valueOf(dArr[1]));
        outcome.parts.put("match", Double.valueOf(found.match));
        outcome.parts.put("margin", Double.valueOf(found.margin));
        outcome.parts.put("available", Double.valueOf(found.available));
        if (found.history > 0.0d) {
            outcome.parts.put("history", Double.valueOf(found.history));
        }
        double dMin = (dArr[0] * 0.2d) + (dArr[1] * 0.15d) + (found.match * 0.4d) + (found.margin * 0.15d) + (found.available * 0.1d) + found.history;
        outcome.caps.addAll(found.caps);
        if (!found.action) {
            if (found.match < MATCH_GATE) {
                outcome.caps.add("what was found is not quite what was said");
            }
            if (found.margin < MARGIN_GATE && outcome.candidates.size() > 1) {
                outcome.caps.add("another result is about as good");
            }
        }
        if (!outcome.caps.isEmpty()) {
            dMin = Math.min(dMin, CAP);
        }
        outcome.confidence = Math.max(0.0d, Math.min(1.0d, dMin));
        Candidate candidate2 = outcome.candidates.get(0);
        if (outcome.confidence >= this.cfg.autoPlay && !candidate2.links.isEmpty()) {
            outcome.decision = PLAY;
            outcome.line = found.action ? candidate2.label : "Playing " + candidate2.line();
            return;
        }
        if (outcome.confidence >= SHOW) {
            if (route.name.equals(YT_SEARCH) && "youtube".equals(query.app) && found.search != null && this.cfg.autoPlay <= 1.0d) {
                outcome.decision = PLAY;
                outcome.line = found.search.label;
                outcome.candidates.add(0, found.search);
                while (outcome.candidates.size() > 3) {
                    outcome.candidates.remove(outcome.candidates.size() - 1);
                }
                return;
            }
            outcome.decision = CHOOSE;
            outcome.message = (route.name.equals(RESUME) && route.what.equals("list")) ? "You were watching" : "";
            return;
        }
        outcome.decision = NONE;
        outcome.message = "Nothing found for “" + (route.entity.isEmpty() ? query.text : route.entity) + "”";
    }

    private static final Pattern SPOKEN_SE = Pattern.compile("\\bS(\\d{1,2}) E(\\d{1,4})\\b");

    /** A short line to say aloud for an outcome: "Playing lo-fi beats on Spotify", "Here's what I found". */
    static String spoken(Outcome outcome) {
        if (outcome.decision.equals(PLAY) && !outcome.candidates.isEmpty()) {
            Candidate c = outcome.candidates.get(0);
            if (!c.say.isEmpty()) {
                return c.say;
            }
            if (c.title == null && c.label.startsWith("Search YouTube for")) {
                return "Searching YouTube for " + plain(c.label.substring("Search YouTube for".length()));
            }
            if (c.label.startsWith("Open ")) {
                return "Opening " + c.label.substring(5);
            }
            String where = c.where;
            if (where.isEmpty() && !c.links.isEmpty() && "com.google.android.youtube.tv".equals(c.links.get(0).pkg)) {
                where = "YouTube";
            }
            return "Playing " + clipWords(plain(c.label), 8) + (where.isEmpty() ? "" : " on " + where);
        }
        if (outcome.decision.equals(CHOOSE) || !outcome.candidates.isEmpty() || outcome.search != null
                || (outcome.titles != null && !outcome.titles.titles.isEmpty())) {
            return "You were watching".equals(outcome.message) ? "Here's what you were watching" : "Here's what I found";
        }
        return "Sorry, I couldn't find that";
    }

    /** Text fit for speech: no quote marks or separators, "S2 E3" read as words. */
    static String plain(String str) {
        String s = SPOKEN_SE.matcher(str).replaceAll("season $1 episode $2").replaceAll("\\bS(\\d{1,2})\\b", "season $1").replaceAll(" \\((?:19|20)\\d\\d\\)", "");
        s = s.replace(" · ", ", ").replaceAll("[“”\"#|]", "").replaceAll("\\s+", " ").trim();
        return s;
    }

    static String clipWords(String str, int max) {
        String[] w = str.split(" ");
        if (w.length <= max) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < max; i++) {
            sb.append(i == 0 ? "" : " ").append(w[i]);
        }
        return sb.toString().replaceAll("[,:;\\-]+$", "");
    }

    private static String capital(String str) {
        return str.isEmpty() ? str : Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
