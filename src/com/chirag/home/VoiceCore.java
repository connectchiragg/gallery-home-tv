package com.chirag.home;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.Normalizer;
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
final class VoiceCore {
    private static final Set<String> APP_POST;
    private static final Set<String> APP_PRE;
    static final String APP_SEARCH = "app_search";
    static final double CONFIDENT = 0.72d;
    static final String EMPTY = "empty";
    private static final Set<String> EPISODE_W;
    private static final Pattern EY;
    static final double GAP = 0.12d;
    private static final Pattern GTI;
    private static final Set<String> HINDI_VERB;
    static final String IMAGE_HOST = "https://images.justwatch.com";
    private static final Set<String> KIND;
    private static final Set<String> LAST_W;
    private static final Set<String> LEAD;
    private static final Set<String> LEAD_STOP_SAFE;
    static final int MAX_RESULTS = 3;
    static final double MIN_KEEP = 0.6d;
    private static final Pattern NETFLIX_ID;
    private static final Pattern NETFLIX_WATCH;
    private static final Set<String> NEXT_W;
    private static final Map<String, Integer> NUM;
    private static final Set<String> NUM_CTX;
    static final String OPEN_APP = "open_app";
    private static final Set<String> PLAY_WORDS;
    private static final String[][] ROMAN;
    static final Map<String, String> SEARCH_TEMPLATES;
    private static final Set<String> SEASON_W;
    private static final Set<String> SEQUEL_W;
    private static final Pattern SE_DIGITS;
    private static final Set<String> SE_JOIN;
    private static final Map<String, Integer> SE_NUM;
    static final double SIBLING_MIN = 0.4d;
    private static final Set<String> SILENCE_TEXT;
    private static final Set<String> STOP;
    private static final Pattern SX;
    private static final Pattern SXEY;
    static final String TITLE = "title";
    private static final Set<String> TRAIL;
    private static final Pattern YT_WATCH;
    static final App[] APPS = {new App("netflix", "Netflix", "com.netflix.ninja", new int[]{8, 175}, "netflix", "net flix"), new App("prime", "Prime Video", "com.amazon.amazonvideo.livingroom", new int[]{119, 2100, 10, 1968, 2243, 2792}, "prime video", "amazon prime", "?prime", "?amazon"), new App("hotstar", "JioHotstar", "in.startv.hotstar", new int[]{2336, 122}, "jiohotstar", "jio hotstar", "hotstar", "hot star", "disney hotstar", "?disney", "?jio"), new App("appletv", "Apple TV", "com.apple.atve.androidtv.appletv", new int[]{350, 2}, "apple tv", "?apple"), new App("crunchyroll", "Crunchyroll", "com.crunchyroll.crunchyroid", new int[]{283}, "crunchyroll", "crunchy roll"), new App("youtube", "YouTube", "com.google.android.youtube.tv", new int[]{192}, "youtube", "you tube"), new App("spotify", "Spotify", "com.spotify.tv.android", new int[0], "spotify"), new App("zee5", "ZEE5", null, new int[]{232}, new String[0]), new App("sonyliv", "Sony LIV", null, new int[]{237}, new String[0])};
    private static final String[] CONS = new String[37];
    private static final Map<Character, String> VOWEL = new LinkedHashMap();
    private static final Map<Character, String> MATRA = new LinkedHashMap();

    private VoiceCore() {
    }

    static final class App {
        final String[] aliases;
        final String key;
        final String label;
        final String pkg;
        final int[] providerIds;

        App(String str, String str2, String str3, int[] iArr, String... strArr) {
            this.key = str;
            this.label = str2;
            this.pkg = str3;
            this.providerIds = iArr;
            this.aliases = strArr;
        }
    }

    static {
        System.arraycopy(new String[]{"k", "kh", "g", "gh", "n", "ch", "chh", "j", "jh", "n", "t", "th", "d", "dh", "n", "t", "th", "d", "dh", "n", "n", "p", "ph", "b", "bh", "m", "y", "r", "r", "l", "l", "l", "v", "sh", "sh", "s", "h"}, 0, CONS, 0, 37);
        String[][] strArr = {new String[]{"अ", "a"}, new String[]{"आ", "aa"}, new String[]{"इ", "i"}, new String[]{"ई", "ee"}, new String[]{"उ", "u"}, new String[]{"ऊ", "oo"}, new String[]{"ऋ", "ri"}, new String[]{"ऍ", "e"}, new String[]{"ए", "e"}, new String[]{"ऐ", "ai"}, new String[]{"ऑ", "o"}, new String[]{"ओ", "o"}, new String[]{"औ", "au"}};
        for (int i = 0; i < 13; i++) {
            String[] strArr2 = strArr[i];
            VOWEL.put(Character.valueOf(strArr2[0].charAt(0)), strArr2[1]);
        }
        String[][] strArr3 = {new String[]{"ा", "aa"}, new String[]{"ि", "i"}, new String[]{"ी", "ee"}, new String[]{"ु", "u"}, new String[]{"ू", "oo"}, new String[]{"ृ", "ri"}, new String[]{"ॅ", "e"}, new String[]{"े", "e"}, new String[]{"ै", "ai"}, new String[]{"ॉ", "o"}, new String[]{"ो", "o"}, new String[]{"ौ", "au"}};
        for (int i2 = 0; i2 < 12; i2++) {
            String[] strArr4 = strArr3[i2];
            MATRA.put(Character.valueOf(strArr4[0].charAt(0)), strArr4[1]);
        }
        ROMAN = new String[][]{new String[]{"ii", "2"}, new String[]{"iii", "3"}, new String[]{"iv", "4"}};
        STOP = new HashSet(Arrays.asList("the", "te", "a", "an", "of", "and"));
        LEAD = set("play", "open", "search", "find", "show", "watch", "start", "launch", "put", "for", "me", "on", "please", "hey", "ok", "okay", "i", "want", "to", "see", "can", "you", "mujhe", "muje", "zara", "jara", "the");
        LEAD_STOP_SAFE = set("play", "open", "search", "find", "show", "watch", "start", "launch", "put", "please", "hey", "ok", "okay", "mujhe", "muje", "zara", "jara");
        TRAIL = set("chalao", "chalaao", "chalaa", "chala", "chalado", "chalaiye", "chalu", "lagao", "lagaao", "laga", "lagado", "lagaiye", "dikhao", "dikhaao", "dikha", "dikhado", "dikhaiye", "kholo", "khol", "kholdo", "dhundo", "dhoondo", "dhundho", "dhoondho", "khojo", "dekhna", "dekhni", "dekhna", "dekhenge", "dekhte", "hai", "hain", "he", "chahiye", "karo", "kar", "kariye", "karna", "dena", "dijiye", "do", "please", "na", "play", "search", "open", "start", "on");
        HINDI_VERB = set("chala", "chalaa", "laga", "dikha", "khol", "kar", "chalu", "play", "search", "open", "start", "on");
        PLAY_WORDS = set("play", "watch", "start", "chalao", "chalaao", "chala", "chalaa", "chalado", "chalaiye", "chalu", "lagao", "lagaao", "laga", "lagado", "lagaiye", "put");
        APP_PRE = set("on", "in", "from", "at", "open", "launch");
        APP_POST = set("pe", "par", "per", "pr", "mein", "me", "mai", "main", "se", "ko", "app", "kholo", "khol", "open");
        NUM_CTX = set("part", "season", "chapter", "bhaag", "bhag", "volume", "vol", "episode", "sequel");
        KIND = set("movie", "film", "picture", "series", "serial", "webseries");
        NUM = new LinkedHashMap();
        String[][] strArr5 = {new String[]{"ek", "one"}, new String[]{"do", "two", "too", "second", "doosra", "dusra"}, new String[]{"teen", "tin", "three", "third", "teesra", "tisra"}, new String[]{"char", "chaar", "four", "fourth"}, new String[]{"paanch", "panch", "five"}, new String[]{"chhe", "chhah", "cheh", "six"}, new String[]{"saat", "seven"}, new String[]{"aath", "eight"}};
        for (int i3 = 0; i3 < 8; i3++) {
            for (String str : strArr5[i3]) {
                NUM.put(str, Integer.valueOf(i3 + 1));
            }
        }
        SEASON_W = keys("season", "seasons", "seezan", "seejan", "sizan", "seasan");
        EPISODE_W = keys("episode", "episodes", "episod", "epeesod", "ep");
        LAST_W = keys("last", "latest", "newest", "new", "final", "aakhri", "aakhiri", "naya", "nayi", "nai");
        NEXT_W = keys("next", "agla", "agli", "agle");
        SE_JOIN = keys("ka", "ki", "ke", "of", "from", "in", "mein", "wala", "wali");
        SE_NUM = new LinkedHashMap();
        String[][] strArr6 = {new String[]{"ek", "one", "first", "pehla", "pehli", "pehle", "pahla", "pahli"}, new String[]{"do", "two", "too", "second", "doosra", "doosri", "doosre"}, new String[]{"teen", "tin", "three", "third", "teesra", "teesri", "teesre"}, new String[]{"char", "four", "fourth", "chautha", "chauthi", "chotha"}, new String[]{"paanch", "five", "fifth", "paanchva", "paanchvi", "paanchwa"}, new String[]{"chhe", "chhah", "six", "sixth", "chhatha", "chhathi"}, new String[]{"saat", "seven", "seventh", "saatva", "saatvi", "saatwa"}, new String[]{"aath", "eight", "eighth", "aathva", "aathvi", "aathwa"}, new String[]{"nau", "nine", "ninth", "nauva", "nauvi"}, new String[]{"das", "ten", "tenth", "dasva", "dasvi"}, new String[]{"gyarah", "eleven", "eleventh"}, new String[]{"barah", "twelve", "twelfth"}, new String[]{"terah", "thirteen"}, new String[]{"chaudah", "fourteen"}, new String[]{"pandrah", "fifteen"}, new String[]{"solah", "sixteen"}, new String[]{"satrah", "seventeen"}, new String[]{"atharah", "eighteen"}, new String[]{"unnis", "nineteen"}, new String[]{"bees", "twenty"}};
        for (int i4 = 0; i4 < 20; i4++) {
            for (String str2 : strArr6[i4]) {
                if (!SE_NUM.containsKey(seKey(str2))) {
                    SE_NUM.put(seKey(str2), Integer.valueOf(i4 + 1));
                }
            }
        }
        SE_DIGITS = Pattern.compile("(\\d{1,4})(?:st|nd|rd|th)?");
        SXEY = Pattern.compile("s(\\d{1,2})e(?:p)?(\\d{1,4})");
        SX = Pattern.compile("s(\\d{1,2})");
        EY = Pattern.compile("e(?:p)?(\\d{1,4})");
        SEQUEL_W = set("part", "chapter", "volume", "vol", "season", "bhag", "bhaag");
        YT_WATCH = Pattern.compile("youtube\\.com/watch\\?v=([A-Za-z0-9_-]{11})");
        SEARCH_TEMPLATES = new LinkedHashMap();
        SEARCH_TEMPLATES.put("youtube", "https://www.youtube.com/results?search_query={q}");
        SEARCH_TEMPLATES.put("spotify", "https://open.spotify.com/search/{q}");
        SEARCH_TEMPLATES.put("netflix", "https://www.netflix.com/search?q={q}");
        SEARCH_TEMPLATES.put("hotstar", "SEARCH");
        SEARCH_TEMPLATES.put("crunchyroll", "SEARCH");
        SEARCH_TEMPLATES.put("appletv", "https://tv.apple.com/search?term={q}");
        SEARCH_TEMPLATES.put("prime", "");
        NETFLIX_ID = Pattern.compile("netflix\\.com/(?:[a-z-]+/)?(?:title|watch)/(\\d+)");
        GTI = Pattern.compile("[?&]gti=([A-Za-z0-9._:-]+)");
        NETFLIX_WATCH = Pattern.compile("netflix\\.com/(?:[a-z-]+/)?watch/(\\d+)");
        SILENCE_TEXT = set("", "you", "thank you", "thanks", "thank you for watching", "thanks for watching", "thank you very much", "thank you so much", "bye", "bye bye", "music", "applause", "silence", "foreign", "blank audio", "no audio", "inaudible", "uh", "um", "hmm", "mm", "please subscribe", "like and subscribe", "see you next time", "see you in the next video");
    }

    static App app(String str) {
        for (App app : APPS) {
            if (app.key.equals(str)) {
                return app;
            }
        }
        return null;
    }

    static App appForPkg(String str) {
        for (App app : APPS) {
            if (app.pkg != null && app.pkg.equals(str)) {
                return app;
            }
        }
        return null;
    }

    static App appForProvider(int i) {
        for (App app : APPS) {
            for (int i2 : app.providerIds) {
                if (i2 == i) {
                    return app;
                }
            }
        }
        return null;
    }

    static boolean isChannel(int i) {
        return i == 1968 || i == 2243 || i == 2792;
    }

    static boolean hasDevanagari(String str) {
        for (int i = 0; i < str.length(); i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt >= 2304 && cCharAt <= 2431) {
                return true;
            }
        }
        return false;
    }

    static String translit(String str) {
        String str2;
        String strNormalize = Normalizer.normalize(str, Normalizer.Form.NFC);
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < strNormalize.length()) {
            char cCharAt = strNormalize.charAt(i);
            int i2 = i + 1;
            char cCharAt2 = i2 < strNormalize.length() ? strNormalize.charAt(i2) : ' ';
            if (cCharAt < 2325 || cCharAt > 2361) {
                str2 = (cCharAt < 2392 || cCharAt > 2399) ? null : new String[]{"q", "kh", "g", "z", "r", "rh", "f", "y"}[cCharAt - 2392];
            } else {
                str2 = CONS[cCharAt - 2325];
            }
            if (str2 != null) {
                if (cCharAt2 == 2364) {
                    if (str2.equals("j")) {
                        str2 = "z";
                    } else if (str2.equals("ph")) {
                        str2 = "f";
                    } else if (str2.equals("d")) {
                        str2 = "r";
                    }
                    int i3 = i2 + 1;
                    char cCharAt3 = i3 < strNormalize.length() ? strNormalize.charAt(i3) : ' ';
                    i = i2;
                    cCharAt2 = cCharAt3;
                }
                sb.append(str2);
                boolean z = cCharAt2 < 2304 || cCharAt2 > 2431;
                if (cCharAt2 == 2381) {
                    i++;
                } else if (!MATRA.containsKey(Character.valueOf(cCharAt2)) && !z) {
                    sb.append('a');
                }
            } else if (MATRA.containsKey(Character.valueOf(cCharAt))) {
                sb.append(MATRA.get(Character.valueOf(cCharAt)));
            } else if (VOWEL.containsKey(Character.valueOf(cCharAt))) {
                sb.append(VOWEL.get(Character.valueOf(cCharAt)));
            } else if (cCharAt == 2306 || cCharAt == 2305) {
                sb.append((cCharAt2 == 2346 || cCharAt2 == 2348 || cCharAt2 == 2349 || cCharAt2 == 2350) ? 'm' : 'n');
            } else if (cCharAt == 2307) {
                sb.append('h');
            } else if (cCharAt >= 2406 && cCharAt <= 2415) {
                sb.append((char) ((cCharAt + '0') - 2406));
            } else if (cCharAt < 2304 || cCharAt > 2431) {
                sb.append(cCharAt);
            }
            i++;
        }
        return sb.toString();
    }

    static List<String> words(String str) {
        String strTrim = Normalizer.normalize(translit(str).toLowerCase(Locale.ROOT).replace("&", " and "), Normalizer.Form.NFD).replaceAll("\\p{M}+", "").replaceAll("['’`]", "").replaceAll("[^a-z0-9]+", " ").trim();
        return strTrim.isEmpty() ? new ArrayList() : new ArrayList(Arrays.asList(strTrim.split(" ")));
    }

    static String fold(String str) {
        for (String[] strArr : ROMAN) {
            if (str.equals(strArr[0])) {
                return strArr[1];
            }
        }
        if (str.matches("[0-9]+")) {
            return str;
        }
        String strReplaceAll = str.replace("chh", "ch").replace("ph", "f").replace("ck", "k").replace("x", "ks").replace("sh", "s").replace("w", "v").replace("z", "j").replace("q", "k").replace("aa", "a").replace("ee", "i").replace("oo", "u").replace("ou", "u").replace("ai", "e").replace("ei", "e").replaceAll("([bdgkjt])h", "$1").replaceAll("(.)\\1+", "$1");
        return (strReplaceAll.length() <= MAX_RESULTS || !strReplaceAll.endsWith("y")) ? strReplaceAll : strReplaceAll.substring(0, strReplaceAll.length() - 1) + "i";
    }

    static List<String> foldAll(List<String> list) {
        ArrayList arrayList = new ArrayList();
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            arrayList.add(fold(it.next()));
        }
        return arrayList;
    }

    static int lev(String str, String str2) {
        int[] iArr = new int[str2.length() + 1];
        int[] iArr2 = new int[str2.length() + 1];
        for (int i = 0; i <= str2.length(); i++) {
            iArr[i] = i;
        }
        int i2 = 1;
        while (i2 <= str.length()) {
            iArr2[0] = i2;
            for (int i3 = 1; i3 <= str2.length(); i3++) {
                int i4 = i3 - 1;
                iArr2[i3] = Math.min(Math.min(iArr2[i4], iArr[i3]) + 1, iArr[i4] + (str.charAt(i2 + (-1)) == str2.charAt(i4) ? 0 : 1));
            }
            i2++;
            int[] iArr3 = iArr2;
            iArr2 = iArr;
            iArr = iArr3;
        }
        return iArr[str2.length()];
    }

    static double ratio(String str, String str2) {
        int iMax = Math.max(str.length(), str2.length());
        if (iMax == 0) {
            return 0.0d;
        }
        return 1.0d - (((double) lev(str, str2)) / ((double) iMax));
    }

    private static boolean numeric(String str) {
        return !str.isEmpty() && Character.isDigit(str.charAt(0));
    }

    private static double coverage(List<String> list, List<String> list2) {
        int i = 0;
        double d = 0.0d;
        for (String str : list) {
            if (!STOP.contains(str) || list.size() <= 1) {
                i++;
                double d2 = 0.0d;
                for (String str2 : list2) {
                    double dRatio = (numeric(str) || numeric(str2)) ? str.equals(str2) ? 1.0d : 0.0d : ratio(str, str2);
                    if (dRatio > d2) {
                        d2 = dRatio;
                    }
                }
                if (d2 >= CONFIDENT) {
                    d += d2;
                }
            }
        }
        if (i == 0) {
            return 0.0d;
        }
        return d / ((double) i);
    }

    static double similarity(String str, String str2) {
        List<String> listFoldAll = foldAll(words(str));
        List<String> listFoldAll2 = foldAll(words(str2));
        if (listFoldAll.isEmpty() || listFoldAll2.isEmpty()) {
            return 0.0d;
        }
        return Math.max(ratio(join(listFoldAll, ""), join(listFoldAll2, "")), (coverage(listFoldAll, listFoldAll2) * 0.75d) + (coverage(listFoldAll2, listFoldAll) * 0.25d));
    }

    static String join(List<String> list, String str) {
        StringBuilder sb = new StringBuilder();
        for (String str2 : list) {
            if (sb.length() > 0) {
                sb.append(str);
            }
            sb.append(str2);
        }
        return sb.toString();
    }

    static final class Query {
        String app;
        int episode;
        boolean lastSeason;
        boolean play;
        int season;
        String raw = "";
        String intent = VoiceCore.EMPTY;
        String text = "";
        String epRef = "";
        final List<String> variants = new ArrayList();

        Query() {
        }

        boolean wantsEpisode() {
            return this.season > 0 || this.episode > 0 || this.lastSeason || this.epRef.equals("last");
        }

        String episodeText() {
            String str = this.season > 0 ? "S" + this.season : this.lastSeason ? "latest season" : "";
            String string = this.episode > 0 ? "E" + this.episode : this.epRef.isEmpty() ? "" : (this.epRef.equals("last") ? "latest" : "next") + " episode";
            if (str.isEmpty()) {
                return string;
            }
            if (string.isEmpty()) {
                return str;
            }
            return str + " " + string;
        }

        public String toString() {
            return this.intent + (this.play ? "+play" : "") + (this.app != null ? " app=" + this.app : "") + (this.season > 0 ? " season=" + this.season : "") + (this.episode > 0 ? " episode=" + this.episode : "") + (this.lastSeason ? " lastSeason" : "") + (this.epRef.isEmpty() ? "" : " ep=" + this.epRef) + " text=\"" + this.text + "\" variants=" + this.variants;
        }
    }

    private static Set<String> set(String... strArr) {
        return new HashSet(Arrays.asList(strArr));
    }

    static String seKey(String str) {
        return fold(str).replace("a", "");
    }

    private static Set<String> keys(String... strArr) {
        HashSet hashSet = new HashSet();
        for (String str : strArr) {
            hashSet.add(seKey(str));
        }
        return hashSet;
    }

    static int seNumber(String str) {
        Matcher matcher = SE_DIGITS.matcher(str);
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(1));
        }
        Integer num = SE_NUM.get(seKey(str));
        if (num == null) {
            return 0;
        }
        return num.intValue();
    }

    private static List<Tok> seasonEpisode(Query query, List<Tok> list) {
        int size = list.size();
        boolean[] zArr = new boolean[size];
        for (int i = 0; i < size; i++) {
            if (zArr[i]) {
                continue;
            }
            String str = list.get(i).lat;
            String strSeKey = seKey(str);
            String str2 = i + 1 < size ? list.get(i + 1).lat : "";
            String str3 = (i <= 0 || zArr[i - 1]) ? "" : list.get(i - 1).lat;
            if (i > 0) {
                Matcher matcher = SXEY.matcher(str);
                if (matcher.matches()) {
                    query.season = Integer.parseInt(matcher.group(1));
                    query.episode = Integer.parseInt(matcher.group(2));
                    zArr[i] = true;
                    continue;
                }
            }
            if (i > 0) {
                Matcher matcher2 = SX.matcher(str);
                if (matcher2.matches() && (i == size - 1 || EY.matcher(str2).matches() || EPISODE_W.contains(seKey(str2)))) {
                    query.season = Integer.parseInt(matcher2.group(1));
                    zArr[i] = true;
                    continue;
                }
            }
            if (i > 0) {
                Matcher matcher3 = EY.matcher(str);
                if (matcher3.matches() && (i == size - 1 || zArr[i - 1])) {
                    query.episode = Integer.parseInt(matcher3.group(1));
                    zArr[i] = true;
                    continue;
                }
            }
            if (!SEASON_W.contains(strSeKey) && (!EPISODE_W.contains(strSeKey) || (str.equals("ep") && seNumber(str2) == 0))) {
                continue;
            }
            boolean zContains = SEASON_W.contains(strSeKey);
            int iSeNumber = seNumber(str2);
            int iSeNumber2 = str3.isEmpty() ? 0 : seNumber(str3);
            if (zContains && iSeNumber > 99) {
                iSeNumber = 0;
            }
            String strSeKey2 = i + 2 < size ? seKey(list.get(i + 2).lat) : "";
            if (iSeNumber2 > 0 && iSeNumber > 0 && (SEASON_W.contains(strSeKey2) || EPISODE_W.contains(strSeKey2))) {
                iSeNumber = 0;
            }
            if (iSeNumber > 0) {
                zArr[i + 1] = true;
                zArr[i] = true;
                if (zContains) {
                    query.season = iSeNumber;
                } else {
                    query.episode = iSeNumber;
                }
            } else if (iSeNumber2 > 0 && size > 2) {
                zArr[i - 1] = true;
                zArr[i] = true;
                if (zContains) {
                    query.season = iSeNumber2;
                } else {
                    query.episode = iSeNumber2;
                }
            } else if (!str3.isEmpty() && LAST_W.contains(seKey(str3))) {
                zArr[i - 1] = true;
                zArr[i] = true;
                if (zContains) {
                    query.lastSeason = true;
                } else {
                    query.epRef = "last";
                }
            } else if (!str3.isEmpty() && NEXT_W.contains(seKey(str3)) && !zContains) {
                zArr[i - 1] = true;
                zArr[i] = true;
                query.epRef = "next";
            }
        }
        if (!query.wantsEpisode() && query.epRef.isEmpty()) {
            return list;
        }
        boolean z = true;
        while (z) {
            z = false;
            for (int i2 = 0; i2 < size; i2++) {
                if (zArr[i2]) {
                    continue;
                }
                boolean z2 = (i2 > 0 && zArr[i2 - 1]) || (i2 + 1 < size && zArr[i2 + 1]);
                String str4 = list.get(i2).lat;
                if (z2 && (SE_JOIN.contains(seKey(str4)) || (str4.equals("the") && i2 + 1 < size && zArr[i2 + 1] && (i2 == 0 || zArr[i2 - 1])))) {
                    zArr[i2] = true;
                    z = true;
                }
            }
        }
        ArrayList<Tok> arrayList = new ArrayList<>();
        for (int i3 = 0; i3 < size; i3++) {
            if (!zArr[i3]) {
                arrayList.add(list.get(i3));
            }
        }
        if (arrayList.isEmpty()) {
            return list;
        }
        if (query.epRef.equals("next")) {
            query.play = true;
        }
        return arrayList;
    }

    private static final class Tok {
        boolean gone;
        final String lat;
        final String orig;

        Tok(String str, String str2) {
            this.orig = str;
            this.lat = str2;
        }
    }

    static Query understand(String str) {
        int i;
        String str2;
        String str3;
        int i2;
        boolean zEquals;
        String str4;
        Query query = new Query();
        String str5 = "";
        query.raw = str == null ? "" : str.trim();
        ArrayList arrayList = new ArrayList();
        for (String str6 : query.raw.split("[\\s,.!?;:\"()\\[\\]।]+")) {
            if (!str6.isEmpty()) {
                List<String> listWords = words(str6);
                if (!listWords.isEmpty()) {
                    arrayList.add(new Tok(str6, join(listWords, "")));
                }
            }
        }
        Iterator it = arrayList.iterator();
        while (true) {
            i = 1;
            if (!it.hasNext()) {
                break;
            }
            if (PLAY_WORDS.contains(((Tok) it.next()).lat)) {
                query.play = true;
            }
        }
        Iterator it2 = arrayList.iterator();
        boolean z = false;
        while (true) {
            str2 = "do";
            if (!it2.hasNext()) {
                break;
            }
            Tok tok = (Tok) it2.next();
            if (LEAD_STOP_SAFE.contains(tok.lat) || (TRAIL.contains(tok.lat) && !tok.lat.equals("do"))) {
                z = true;
            }
        }
        int i3 = 2;
        loop3: while (true) {
            if (i3 < i) {
                str3 = str5;
                str2 = str2;
                break;
            }
            int i4 = 0;
            while (true) {
                int i5 = i4 + i3;
                if (i5 <= arrayList.size()) {
                    StringBuilder sb = new StringBuilder();
                    for (int i6 = i4; i6 < i5; i6++) {
                        sb.append(fold(((Tok) arrayList.get(i6)).lat));
                    }
                    String string = sb.toString();
                    int i7 = (i4 <= 0 || !APP_PRE.contains(((Tok) arrayList.get(i4 + (-1))).lat)) ? 0 : i;
                    int i8 = (i5 >= arrayList.size() || !APP_POST.contains(((Tok) arrayList.get(i5)).lat)) ? 0 : i;
                    App[] appArr = APPS;
                    int length = appArr.length;
                    int i9 = 0;
                    while (i9 < length) {
                        App app = appArr[i9];
                        String[] strArr = app.aliases;
                        App[] appArr2 = appArr;
                        int length2 = strArr.length;
                        int i10 = length;
                        int i11 = 0;
                        while (i11 < length2) {
                            int i12 = length2;
                            String strSubstring = strArr[i11];
                            String[] strArr2 = strArr;
                            boolean zStartsWith = strSubstring.startsWith("?");
                            if (zStartsWith) {
                                strSubstring = strSubstring.substring(1);
                            }
                            List<String> listWords2 = words(strSubstring);
                            if (listWords2.size() != i3) {
                                str3 = str5;
                                str4 = string;
                            } else {
                                String strJoin = join(foldAll(listWords2), str5);
                                if (zStartsWith) {
                                    zEquals = string.equals(strJoin);
                                    str3 = str5;
                                } else {
                                    str3 = str5;
                                    zEquals = ratio(string, strJoin) >= (strJoin.length() >= 7 ? 0.75d : 0.99d);
                                }
                                if (zEquals) {
                                    boolean z2 = true;
                                    int i13 = 0;
                                    while (i13 < i3) {
                                        String str7 = string;
                                        List<String> list = listWords2;
                                        if (ratio(fold(((Tok) arrayList.get(i4 + i13)).lat), fold(listWords2.get(i13))) < MIN_KEEP) {
                                            z2 = false;
                                        }
                                        i13++;
                                        string = str7;
                                        listWords2 = list;
                                    }
                                    str4 = string;
                                    if (z2 && ((!zStartsWith || i7 != 0 || i8 != 0 || arrayList.size() == i3 || (z && (i4 == 0 || i5 == arrayList.size()))) && (zStartsWith || i7 != 0 || i8 != 0 || i4 <= 0 || i5 >= arrayList.size()))) {
                                        query.app = app.key;
                                        for (int i14 = i4; i14 < i5; i14++) {
                                            ((Tok) arrayList.get(i14)).gone = true;
                                        }
                                        if (i7 != 0) {
                                            ((Tok) arrayList.get(i4 - 1)).gone = true;
                                        }
                                        if (i8 == 0) {
                                            break loop3;
                                        }
                                        ((Tok) arrayList.get(i5)).gone = true;
                                        break loop3;
                                    }
                                } else {
                                    str4 = string;
                                }
                            }
                            i11++;
                            length2 = i12;
                            strArr = strArr2;
                            str2 = str2;
                            string = str4;
                            str5 = str3;
                        }
                        i9++;
                        appArr = appArr2;
                        length = i10;
                    }
                    i4++;
                    i = 1;
                } else {
                    break;
                }
            }
            i3--;
            i = 1;
        }
        List<Tok> listAlive = alive(arrayList);
        int size = listAlive.size();
        int i15 = 0;
        while (i15 < size && LEAD_STOP_SAFE.contains(listAlive.get(i15).lat)) {
            do {
                i15++;
                if (i15 >= size - 1 || !LEAD.contains(listAlive.get(i15).lat) || LEAD_STOP_SAFE.contains(listAlive.get(i15).lat)) {
                    break;
                }
            } while (set("for", "me", "on", "the").contains(listAlive.get(i15).lat));
        }
        if (i15 < size - 3 && listAlive.get(i15).lat.equals("i") && listAlive.get(i15 + 1).lat.equals("want") && listAlive.get(i15 + 2).lat.equals("to") && (i15 = i15 + 3) < size - 1 && set("watch", "see", "play").contains(listAlive.get(i15).lat)) {
            i15++;
        }
        while (size > i15) {
            int i16 = size - 1;
            String str8 = listAlive.get(i16).lat;
            if (!TRAIL.contains(str8)) {
                break;
            }
            String str9 = str2;
            if (str8.equals(str9) || str8.equals("on") || str8.equals("kar") || str8.equals("na") || str8.equals("he")) {
                int i17 = size - 2;
                if (i17 < i15 || (!HINDI_VERB.contains(listAlive.get(i17).lat) && (!TRAIL.contains(listAlive.get(i17).lat) || i17 <= i15))) {
                    break;
                }
                size--;
                str2 = str9;
            } else {
                if ((str8.equals("play") || str8.equals("search") || str8.equals("open") || str8.equals("start")) && i16 == i15) {
                    break;
                }
                size--;
                str2 = str9;
            }
        }
        List arrayList2 = new ArrayList(listAlive.subList(i15, size));
        if (!"youtube".equals(query.app) && !"spotify".equals(query.app)) {
            arrayList2 = seasonEpisode(query, arrayList2);
        }
        while (true) {
            if (arrayList2.size() <= 1 || !KIND.contains(((Tok) arrayList2.get(arrayList2.size() - 1)).lat)) {
                break;
            }
            arrayList2.remove(arrayList2.size() - 1);
        }
        // "play the movie Jawan", "watch the series Panchayat": the kind word in front is not part of the name.
        while (arrayList2.size() > 1) {
            boolean theKind = arrayList2.size() > 2 && ((Tok) arrayList2.get(0)).lat.equals("the") && KIND.contains(((Tok) arrayList2.get(1)).lat);
            if (theKind) {
                arrayList2.remove(0);
            } else if (KIND.contains(((Tok) arrayList2.get(0)).lat)) {
                arrayList2.remove(0);
            } else {
                break;
            }
        }
        for (i2 = 1; arrayList2.size() > i2 && set("ki", "ka", "wali", "wala", "vali", "vala").contains(((Tok) arrayList2.get(arrayList2.size() - 1)).lat); i2 = 1) {
            arrayList2.remove(arrayList2.size() - 1);
        }
        ArrayList<String> arrayList3 = new ArrayList();
        ArrayList arrayList4 = new ArrayList();
        ArrayList arrayList5 = new ArrayList();
        int i18 = 0;
        while (i18 < arrayList2.size()) {
            String str10 = ((Tok) arrayList2.get(i18)).lat;
            Integer numValueOf = NUM.get(str10);
            if (numValueOf == null && str10.matches("[0-9]{1,2}")) {
                numValueOf = Integer.valueOf(str10);
            }
            String str11 = i18 > 0 ? ((Tok) arrayList2.get(i18 - 1)).lat : str3;
            boolean z3 = "youtube".equals(query.app) || "spotify".equals(query.app);
            boolean z4 = i18 == arrayList2.size() - 1;
            boolean z5 = NUM_CTX.contains(str11) && !(z3 && (str11.equals("season") || str11.equals("episode")));
            if (numValueOf != null && i18 > 0 && (z4 || z5)) {
                if (z5) {
                    arrayList3.remove(arrayList3.size() - 1);
                    arrayList4.remove(arrayList4.size() - 1);
                }
                if (z3 || !(str11.equals("season") || str11.equals("episode"))) {
                    arrayList3.add(String.valueOf(numValueOf));
                    arrayList4.add(String.valueOf(numValueOf));
                } else if (str11.equals("season")) {
                    query.season = numValueOf.intValue();
                }
                i18++;
                continue;
            }
            arrayList3.add(str10);
            arrayList4.add(((Tok) arrayList2.get(i18)).orig);
            i18++;
        }
        query.text = join(arrayList3, " ");
        if (query.text.isEmpty()) {
            query.intent = query.app != null ? OPEN_APP : EMPTY;
            return query;
        }
        query.intent = ("youtube".equals(query.app) || "spotify".equals(query.app)) ? APP_SEARCH : TITLE;
        String strJoin2 = join(arrayList4, " ");
        if (hasDevanagari(strJoin2)) {
            query.variants.add(strJoin2);
        }
        query.variants.add(query.text);
        for (String strValueOf : arrayList3) {
            if (NUM.containsKey(strValueOf)) {
                strValueOf = String.valueOf(NUM.get(strValueOf));
            }
            arrayList5.add(strValueOf);
        }
        String strJoin3 = join(arrayList5, " ");
        if (!query.variants.contains(strJoin3)) {
            query.variants.add(strJoin3);
        }
        return query;
    }

    private static List<Tok> alive(List<Tok> list) {
        ArrayList arrayList = new ArrayList();
        for (Tok tok : list) {
            if (!tok.gone) {
                arrayList.add(tok);
            }
        }
        return arrayList;
    }

    static final class Offer {
        final String app;
        String kind;
        String deeplink = "";
        String web = "";
        boolean channel = true;
        String webDeep = "";

        Offer(String str, String str2) {
            this.app = str;
            this.kind = str2;
        }

        boolean included() {
            return (this.kind.equals("rent") || this.kind.equals("buy")) ? false : true;
        }
    }

    static final class Season {
        final int count;
        final String id;
        final int number;

        Season(String str, int i, int i2) {
            this.id = str;
            this.number = i;
            this.count = i2;
        }
    }

    static final class Episode {
        int number;
        int season;
        String name = "";
        boolean released = true;
        final Map<String, Offer> offers = new LinkedHashMap();

        Episode() {
        }

        void offer(String str, String str2, boolean z, String str3, String str4, String str5) {
            Offer offer = this.offers.get(str);
            if (offer == null) {
                offer = new Offer(str, str2);
                this.offers.put(str, offer);
            } else if (VoiceCore.rankKind(str2) < VoiceCore.rankKind(offer.kind)) {
                offer.kind = str2;
            }
            if (offer.channel && !z) {
                offer.channel = false;
                offer.deeplink = "";
                offer.web = "";
                offer.webDeep = "";
            } else if (!offer.channel && z) {
                return;
            }
            if (offer.deeplink.isEmpty() && str3 != null) {
                offer.deeplink = str3;
            }
            if (offer.web.isEmpty() && str4 != null) {
                offer.web = str4;
            }
            if (!offer.webDeep.isEmpty() || str5 == null) {
                return;
            }
            offer.webDeep = str5;
        }
    }

    static final class Target {
        final Episode ep;
        final String note;
        final boolean seasonOnly;

        Target(Episode episode, boolean z, String str) {
            this.ep = episode;
            this.seasonOnly = z;
            this.note = str;
        }

        String label() {
            return "S" + this.ep.season + (this.seasonOnly ? "" : " E" + this.ep.number);
        }
    }

    static final class Title {
        double popularity;
        double score;
        double sim;
        String subOverride;
        Target target;
        long tmdbId;
        String type = "movie";
        String jwId = "";
        String name = "";
        String original = "";
        String year = "";
        String artOverride = "";
        String poster = "";
        String backdrop = "";
        final List<String> alts = new ArrayList();
        final Map<String, Offer> offers = new LinkedHashMap();
        final List<Season> seasons = new ArrayList();
        String targetNote = "";

        Title() {
        }

        void offer(String str, String str2, boolean z, String str3, String str4) {
            Offer offer = this.offers.get(str);
            if (offer == null) {
                offer = new Offer(str, str2);
                this.offers.put(str, offer);
            } else if (VoiceCore.rankKind(str2) < VoiceCore.rankKind(offer.kind)) {
                offer.kind = str2;
            }
            if (offer.channel && !z) {
                offer.channel = false;
                offer.deeplink = "";
                offer.web = "";
            } else if (!offer.channel && z) {
                return;
            }
            if (offer.deeplink.isEmpty() && str3 != null) {
                offer.deeplink = str3;
            }
            if (!offer.web.isEmpty() || str4 == null) {
                return;
            }
            offer.web = str4;
        }

        List<Offer> apps(String str) {
            ArrayList<Offer> arrayList = new ArrayList();
            for (int i = 0; i < 4; i++) {
                for (App app : VoiceCore.APPS) {
                    Offer offer = this.offers.get(app.key);
                    if (offer != null && app.pkg != null && !arrayList.contains(offer)) {
                        if (i == 0) {
                            if (app.key.equals(str)) {
                                arrayList.add(offer);
                            }
                        } else if (i == 1) {
                            if (offer.included() && !offer.channel) {
                                arrayList.add(offer);
                            }
                        } else if (i != 2 || offer.included()) {
                            arrayList.add(offer);
                        }
                    }
                }
            }
            if (this.target != null) {
                ArrayList arrayList2 = new ArrayList();
                ArrayList arrayList3 = new ArrayList();
                for (Offer offer2 : arrayList) {
                    ((this.target.ep.offers.containsKey(offer2.app) || offer2.app.equals(str)) ? arrayList2 : arrayList3).add(offer2);
                }
                arrayList.clear();
                arrayList.addAll(arrayList2);
                arrayList.addAll(arrayList3);
            }
            return arrayList;
        }

        List<String> elsewhere() {
            ArrayList arrayList = new ArrayList();
            for (Offer offer : this.offers.values()) {
                if (VoiceCore.app(offer.app).pkg == null) {
                    arrayList.add(VoiceCore.app(offer.app).label);
                }
            }
            return arrayList;
        }

        String line() {
            return this.name + (this.year.isEmpty() ? "" : " (" + this.year + ")") + " · " + (this.type.equals("tv") ? "Series" : "Movie");
        }

        String line(String str) {
            String str2;
            if (this.target == null) {
                return line();
            }
            str2 = "";
            for (Offer offer : apps(str)) {
                if (this.target.ep.offers.containsKey(offer.app)) {
                    str2 = " · " + VoiceCore.app(offer.app).label;
                    break;
                }
            }
            return this.name + " · " + this.target.label() + str2;
        }

        String artUrl() {
            String str;
            String str2;
            if (!this.artOverride.isEmpty()) {
                return this.artOverride;
            }
            if (!this.backdrop.isEmpty()) {
                str = this.backdrop;
                str2 = "s640";
            } else {
                if (this.poster.isEmpty()) {
                    return "";
                }
                str = this.poster;
                str2 = "s592";
            }
            return VoiceCore.imageUrl(str, str2);
        }

        String targetLine() {
            String str = (this.target == null || this.target.seasonOnly || this.target.ep.name.isEmpty()) ? "" : "“" + this.target.ep.name + "”";
            String str2 = (this.target == null || this.target.note.isEmpty()) ? this.targetNote : this.target.note;
            if (str.isEmpty()) {
                return str2;
            }
            return str2.isEmpty() ? str : str + " · " + str2;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int rankKind(String str) {
        if (str.equals("stream")) {
            return 0;
        }
        if (str.equals("free")) {
            return 1;
        }
        if (str.equals("ads")) {
            return 2;
        }
        if (str.equals("rent")) {
            return MAX_RESULTS;
        }
        return 4;
    }

    static double match(Query query, List<String> list) {
        double dMax = 0.0d;
        for (String str : query.variants) {
            ArrayList arrayList = new ArrayList();
            for (String str2 : words(str)) {
                if (!numeric(str2)) {
                    arrayList.add(str2);
                }
            }
            String strJoin = arrayList.size() < words(str).size() ? join(arrayList, " ") : "";
            for (String str3 : list) {
                dMax = Math.max(dMax, similarity(str, str3));
                if (!strJoin.isEmpty()) {
                    dMax = Math.max(dMax, similarity(strJoin, str3) * 0.8d);
                }
            }
        }
        return dMax;
    }

    static void rank(Query query, List<Title> list) {
        for (int i = 0; i < list.size(); i++) {
            Title title = list.get(i);
            ArrayList arrayList = new ArrayList(title.alts);
            arrayList.add(0, title.name);
            if (!title.original.isEmpty()) {
                arrayList.add(1, title.original);
            }
            title.sim = match(query, arrayList);
            title.score = (title.sim * 0.85d) + ((1.0d - (((double) Math.min(i, 19)) / 19.0d)) * 0.1d) + (Math.min(1.0d, Math.log10(Math.max(0.0d, title.popularity) + 1.0d) / 2.5d) * 0.05d) + availability(query, title);
            if (query.season + query.episode > 0 && title.type.equals("tv")) {
                title.score += 0.03d;
            }
        }
        Collections.sort(list, new Comparator<Title>() { // from class: com.chirag.home.VoiceCore.1
            @Override // java.util.Comparator
            public int compare(Title title2, Title title3) {
                return Double.compare(title3.score, title2.score);
            }
        });
    }

    static double availability(Query query, Title title) {
        double d = 0.0d;
        double dMax = 0.0d;
        for (Offer offer : title.offers.values()) {
            App app = app(offer.app);
            if (app != null && app.pkg != null) {
                dMax = Math.max(dMax, (!offer.included() || offer.channel) ? 0.02d : 0.04d);
            }
        }
        if (query.app != null && title.offers.containsKey(query.app)) {
            d = 0.05d;
        }
        return dMax + d;
    }

    static String franchise(String str) {
        int length = str.length();
        String[] strArr = {":", " - ", " – ", " — ", "("};
        for (int i = 0; i < 5; i++) {
            int iIndexOf = str.indexOf(strArr[i]);
            if (iIndexOf > 0 && iIndexOf < length) {
                length = iIndexOf;
            }
        }
        ArrayList arrayList = new ArrayList();
        for (String str2 : foldAll(words(str.substring(0, length)))) {
            if (numeric(str2) || SEQUEL_W.contains(str2)) {
                break;
            }
            arrayList.add(str2);
        }
        return join(arrayList, " ");
    }

    static List<Title> select(Query query, List<Title> list, int i) {
        ArrayList arrayList = new ArrayList();
        Title title = null;
        for (Title title2 : list) {
            if (title == null || title2.sim > title.sim) {
                title = title2;
            }
        }
        if (title == null || title.sim < MIN_KEEP) {
            return arrayList;
        }
        String strFranchise = franchise(title.name);
        int iMax = Math.max(1, Math.min(MAX_RESULTS, i));
        for (Title title3 : list) {
            boolean z = false;
            boolean z2 = title3.sim >= MIN_KEEP && title3.sim >= title.sim - GAP;
            if (title3.sim >= SIBLING_MIN && !strFranchise.isEmpty() && strFranchise.equals(franchise(title3.name))) {
                z = true;
            }
            if (z2 || z) {
                if (arrayList.size() < iMax) {
                    arrayList.add(title3);
                }
            }
        }
        return arrayList;
    }

    static final class Spot {
        final boolean absolute;
        final int position;
        final Season season;

        Spot(Season season, int i, boolean z) {
            this.season = season;
            this.position = i;
            this.absolute = z;
        }
    }

    static Spot locate(Query query, Title title) {
        if (title.seasons.isEmpty() || !query.wantsEpisode()) {
            return null;
        }
        if (query.season > 0) {
            for (Season season : title.seasons) {
                if (season.number == query.season) {
                    return new Spot(season, query.episode, false);
                }
            }
            return null;
        }
        Season season2 = null;
        for (Season season3 : title.seasons) {
            if (season3.count > 0) {
                season2 = season3;
            }
        }
        if (query.lastSeason || query.epRef.equals("last")) {
            if (season2 == null) {
                return null;
            }
            return new Spot(season2, query.episode, false);
        }
        int i = 0;
        for (Season season4 : title.seasons) {
            if (query.episode <= season4.count + i) {
                return new Spot(season4, query.episode - i, i > 0);
            }
            i += season4.count;
        }
        return null;
    }

    static Target pick(Query query, Spot spot, List<Episode> list, int i) {
        boolean z;
        boolean z2;
        if (list.isEmpty()) {
            return null;
        }
        Episode episode = list.get(0);
        if (i != 0) {
            z = false;
        } else {
            z = true;
        }
        if (query.epRef.equals("last") && query.episode == 0) {
            Episode episode2 = null;
            for (Episode episode3 : list) {
                if (episode3.released && !episode3.offers.isEmpty()) {
                    episode2 = episode3;
                }
            }
            if (episode2 == null) {
                for (Episode episode4 : list) {
                    if (episode4.released) {
                        episode2 = episode4;
                    }
                }
            }
            if (episode2 == null) {
                return null;
            }
            return new Target(episode2, false, "latest episode");
        }
        if (query.episode == 0) {
            if (z) {
                return new Target(episode, true, "");
            }
            return null;
        }
        int i2 = spot.absolute ? query.episode : spot.position;
        for (Episode episode5 : list) {
            if (episode5.number == i2) {
                return new Target(episode5, false, "");
            }
        }
        if (spot.absolute) {
            for (Episode episode6 : list) {
                if (episode6.number == spot.position) {
                    return new Target(episode6, false, "episode " + query.episode + " counted from season 1");
                }
            }
        }
        int i3 = (spot.position - 1) - i;
        if (episode.number == i + 1) {
            z2 = false;
        } else {
            z2 = true;
        }
        if (z2 && !spot.absolute && i3 >= 0 && i3 < list.size()) {
            return new Target(list.get(i3), false, "episode " + spot.position + " of the season");
        }
        if (!z || spot.absolute) {
            return null;
        }
        return new Target(episode, true, "episode " + query.episode + " not found");
    }

    static final class Link {
        static final String LAUNCH = "LAUNCH";
        static final String SEARCH = "SEARCH";
        static final String VIEW = "VIEW";
        final String action;
        final Map<String, String> extras = new LinkedHashMap();
        final String pkg;
        final String uri;
        final String what;

        Link(String str, String str2, String str3, String str4) {
            this.action = str;
            this.pkg = str2;
            this.uri = str3;
            this.what = str4;
        }

        Link extra(String str, String str2) {
            this.extras.put(str, str2);
            return this;
        }

        String scope() {
            if (!this.action.equals(VIEW) || this.uri == null || this.what.equals("search in app")) {
                return "";
            }
            if (this.what.startsWith("episode")) {
                return "episode";
            }
            return this.what.startsWith("season") ? "season" : VoiceCore.TITLE;
        }

        public String toString() {
            return String.format("%-6s %-34s %s%s  [%s]", this.action, this.pkg, this.uri == null ? "" : this.uri, this.extras.isEmpty() ? "" : " " + this.extras, this.what);
        }
    }

    static String imageUrl(String str, String str2) {
        if (str == null || !str.startsWith("/") || str.contains("..")) {
            return "";
        }
        return IMAGE_HOST + str.replace("{profile}", str2).replace("{format}", "jpg");
    }

    static String mediaId(Link link) {
        if (link == null || link.uri == null) {
            return null;
        }
        App appAppForPkg = appForPkg(link.pkg);
        if (appAppForPkg != null && appAppForPkg.key.equals("netflix")) {
            Matcher matcher = NETFLIX_ID.matcher(link.uri);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        if (appAppForPkg != null && appAppForPkg.key.equals("prime")) {
            Matcher matcher2 = GTI.matcher(link.uri);
            if (matcher2.find()) {
                return matcher2.group(1);
            }
        }
        if (appAppForPkg != null && appAppForPkg.key.equals("youtube")) {
            Matcher matcher3 = YT_WATCH.matcher(link.uri);
            if (matcher3.find()) {
                return matcher3.group(1);
            }
        }
        return link.uri;
    }

    static double covers(String str, String str2) {
        List<String> listFoldAll = foldAll(words(str));
        List<String> listFoldAll2 = foldAll(words(str2));
        if (listFoldAll.isEmpty() || listFoldAll2.isEmpty()) {
            return 0.0d;
        }
        return coverage(listFoldAll, listFoldAll2);
    }

    static List<Link> historyLinks(String str, String str2, String str3, long j, boolean z) {
        ArrayList arrayList = new ArrayList();
        App appAppForPkg = appForPkg(str);
        String strTrim = str2 == null ? "" : str2.trim();
        String str4 = appAppForPkg == null ? "" : appAppForPkg.key;
        if (strTrim.contains("://")) {
            arrayList.add(new Link("VIEW", str, strTrim, "play"));
        } else if (str4.equals("youtube") && strTrim.matches("[A-Za-z0-9_-]{11}")) {
            long jMax = z ? 0L : Math.max(0L, (j / 1000) - 3);
            arrayList.add(new Link("VIEW", str, "https://www.youtube.com/watch?v=" + strTrim + (jMax > 0 ? "&t=" + jMax + "s" : ""), "play"));
        } else if (str4.equals("spotify") && strTrim.matches("spotify:[a-z]+:[A-Za-z0-9]+")) {
            String[] strArrSplit = strTrim.split(":");
            arrayList.add(new Link("VIEW", str, "https://open.spotify.com/" + strArrSplit[1] + "/" + strArrSplit[2], "play"));
        } else if (str4.equals("netflix") && strTrim.matches("\\d{6,}")) {
            arrayList.add(new Link("VIEW", str, "https://www.netflix.com/watch/" + strTrim, "play").extra("source", "30"));
        } else if (str4.equals("prime") && strTrim.startsWith("amzn1.dv.gti.")) {
            arrayList.add(new Link("VIEW", str, "https://app.primevideo.com/watch?gti=" + strTrim + "&time=0&territory=IN", "play"));
        } else if ((str4.equals("youtube") || str4.equals("spotify")) && !str3.isEmpty()) {
            arrayList.addAll(searchLinks(str4, str3, null));
        }
        Iterator it = arrayList.iterator();
        boolean z2 = false;
        while (it.hasNext()) {
            if (((Link) it.next()).action.equals("LAUNCH")) {
                z2 = true;
            }
        }
        if (!z2) {
            arrayList.add(new Link("LAUNCH", str, null, "open app"));
        }
        return arrayList;
    }

    static String launchSubtitle(Title title, Link link) {
        if (title.subOverride != null) {
            return title.subOverride;
        }
        String strScope = link.scope();
        if (title.target == null || !strScope.equals("episode")) {
            return (title.target == null || !strScope.equals("season")) ? title.year : "Season " + title.target.ep.season;
        }
        return title.target.label() + (title.target.ep.name.isEmpty() ? "" : " · " + nice(title.target.ep.name));
    }

    static String nice(String str) {
        if (!str.equals(str.toUpperCase(Locale.ROOT)) || str.equals(str.toLowerCase(Locale.ROOT))) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        char[] charArray = str.toCharArray();
        int length = charArray.length;
        boolean z = true;
        for (int i = 0; i < length; i++) {
            char c = charArray[i];
            sb.append(z ? Character.toUpperCase(c) : Character.toLowerCase(c));
            z = (Character.isLetterOrDigit(c) || c == '\'' || c == 8217) ? false : true;
        }
        return sb.toString();
    }

    static Title artMatch(String str, String str2, List<Title> list, boolean z) {
        return artMatch(str, str2, list, z, CONFIDENT);
    }

    static Title artMatch(String str, String str2, List<Title> list, boolean z, double d) {
        Query query = new Query();
        query.variants.add(str2);
        String str3 = understand(str2).text;
        if (!str3.isEmpty() && !query.variants.contains(str3)) {
            query.variants.add(str3);
        }
        ArrayList arrayList = new ArrayList();
        if (str != null) {
            for (Title title : list) {
                if (title.offers.containsKey(str)) {
                    arrayList.add(title);
                }
            }
        }
        if (!arrayList.isEmpty()) {
            list = arrayList;
        } else {
            if (z) {
                return null;
            }
            d = 0.95d;
        }
        Title title2 = null;
        for (Title title3 : list) {
            ArrayList arrayList2 = new ArrayList();
            arrayList2.add(title3.name);
            if (!title3.original.isEmpty()) {
                arrayList2.add(title3.original);
            }
            title3.sim = match(query, arrayList2);
            if (title2 == null || title3.sim > title2.sim) {
                title2 = title3;
            }
        }
        if (title2 == null || title2.sim < d || title2.artUrl().isEmpty()) {
            return null;
        }
        for (Title title4 : list) {
            if (title4 != title2 && title4.sim >= title2.sim - 0.02d) {
                return null;
            }
        }
        return title2;
    }

    static String enc(String str) {
        try {
            return URLEncoder.encode(str, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    static List<Link> searchLinks(String str, String str2, Map<String, String> map) {
        ArrayList arrayList = new ArrayList();
        App app = app(str);
        if (app == null || app.pkg == null) {
            return arrayList;
        }
        if (map == null || !map.containsKey(str)) {
            map = SEARCH_TEMPLATES;
        }
        String str3 = map.get(str);
        if (str3 != null && !str3.isEmpty() && !str2.isEmpty()) {
            if (str3.equals("SEARCH")) {
                arrayList.add(new Link("SEARCH", app.pkg, null, "search in app").extra("query", str2));
            } else {
                Link link = new Link("VIEW", app.pkg, str3.replace("{q}", enc(str2)), "search in app");
                if (str.equals("netflix")) {
                    link.extra("source", "30");
                }
                arrayList.add(link);
            }
        }
        arrayList.add(new Link("LAUNCH", app.pkg, null, "open app"));
        return arrayList;
    }

    /**
     * Links for one Spotify track. On Spotify for Android TV a spotify:track: link starts playing the track (and
     * Spotify's autoplay carries on with similar songs); playlist and search links only open a page, and the app
     * ignores play-from-search from other apps (tested on the Skyworth Google TV, Spotify 9.1.84).
     */
    static List<Link> spotifyTrackLinks(String trackId, String q, Map<String, String> map) {
        ArrayList<Link> links = new ArrayList();
        App app = app("spotify");
        links.add(new Link("VIEW", app.pkg, "spotify:track:" + trackId, "play"));
        links.addAll(searchLinks("spotify", q, map));
        return links;
    }

    static List<Link> titleLinks(Title title, String str, boolean z, Map<String, String> map) {
        ArrayList arrayList = new ArrayList();
        App app = app(str);
        if (app == null || app.pkg == null) {
            return arrayList;
        }
        Offer offer = title.offers.get(str);
        if (offer != null) {
            Link linkFromIntentUri = fromIntentUri(offer.deeplink, app.pkg);
            String str2 = offer.deeplink + " " + offer.web;
            if (str.equals("netflix")) {
                Matcher matcher = NETFLIX_ID.matcher(str2);
                if (matcher.find()) {
                    String strGroup = matcher.group(1);
                    arrayList.add(new Link("VIEW", app.pkg, "https://www.netflix.com/" + (z ? "watch/" : "title/") + strGroup, z ? "play" : "title page").extra("source", "30"));
                    arrayList.add(new Link("VIEW", app.pkg, "https://www.netflix.com/" + (z ? "title/" : "watch/") + strGroup, z ? "title page" : "play").extra("source", "30"));
                    linkFromIntentUri = null;
                }
            } else if (str.equals("prime") && !z) {
                Matcher matcher2 = GTI.matcher(str2);
                if (matcher2.find()) {
                    arrayList.add(new Link("VIEW", app.pkg, "https://app.primevideo.com/detail?gti=" + matcher2.group(1), "title page"));
                }
            }
            if (linkFromIntentUri != null) {
                arrayList.add(linkFromIntentUri);
            } else if (arrayList.isEmpty() && offer.web.startsWith("https://") && !str.equals("prime")) {
                arrayList.add(new Link("VIEW", app.pkg, offer.web, "web link"));
            }
        }
        arrayList.addAll(searchLinks(str, title.name, map));
        return arrayList;
    }

    static List<Link> episodeLinks(Title title, String str, boolean z, Map<String, String> map) {
        ArrayList arrayList = new ArrayList();
        App app = app(str);
        if (app == null || app.pkg == null) {
            return arrayList;
        }
        Offer offer = title.target == null ? null : title.target.ep.offers.get(str);
        if (offer != null) {
            String string = (title.target.seasonOnly ? new StringBuilder().append("season ").append(title.target.ep.season) : new StringBuilder().append("episode ").append(title.target.label())).toString();
            Link linkFromIntentUri = fromIntentUri(offer.deeplink, app.pkg, string);
            if (str.equals("netflix")) {
                Matcher matcher = NETFLIX_WATCH.matcher(offer.webDeep);
                Matcher matcher2 = NETFLIX_ID.matcher(offer.web + " " + offer.deeplink);
                if (matcher.find() && (!matcher2.find() || !matcher2.group(1).equals(matcher.group(1)))) {
                    arrayList.add(new Link("VIEW", app.pkg, "https://www.netflix.com/watch/" + matcher.group(1), string).extra("source", "30"));
                }
            } else if (str.equals("prime")) {
                Matcher matcher3 = GTI.matcher(offer.deeplink + " " + offer.web);
                Link link = matcher3.find() ? new Link("VIEW", app.pkg, "https://app.primevideo.com/detail?gti=" + matcher3.group(1), string + " page") : null;
                boolean z2 = title.target.seasonOnly && !z;
                if (z2 && link != null) {
                    arrayList.add(link);
                }
                if (linkFromIntentUri != null) {
                    arrayList.add(linkFromIntentUri);
                }
                if (!z2 && link != null) {
                    arrayList.add(link);
                }
            } else {
                if (linkFromIntentUri != null) {
                    arrayList.add(linkFromIntentUri);
                }
                String strStripAffiliate = stripAffiliate(offer.web);
                if (strStripAffiliate.startsWith("https://") && (linkFromIntentUri == null || !strStripAffiliate.equals(linkFromIntentUri.uri))) {
                    arrayList.add(new Link("VIEW", app.pkg, strStripAffiliate, string + " (web link)"));
                }
            }
        }
        for (Link link2 : titleLinks(title, str, z, map)) {
            Iterator it = arrayList.iterator();
            while (true) {
                if (!it.hasNext()) {
                    arrayList.add(link2);
                    break;
                }
                Link link3 = (Link) it.next();
                if (link3.action.equals(link2.action) && String.valueOf(link3.uri).equals(String.valueOf(link2.uri))) {
                    break;
                }
            }
        }
        return arrayList;
    }

    static String stripAffiliate(String str) {
        int iIndexOf = str.indexOf(63);
        if (iIndexOf < 0) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        for (String str2 : str.substring(iIndexOf + 1).split("&")) {
            if (!str2.matches("(?:at|ct|itscg|itsct)=.*") && !str2.isEmpty()) {
                sb.append(sb.length() == 0 ? '?' : '&').append(str2);
            }
        }
        return str.substring(0, iIndexOf) + ((Object) sb);
    }

    static Link fromIntentUri(String str, String str2) {
        return fromIntentUri(str, str2, "deep link");
    }

    static Link fromIntentUri(String str, String str2, String str3) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        if (!str.startsWith("intent:")) {
            if (str.contains("://")) {
                return new Link("VIEW", str2, str, str3);
            }
            return null;
        }
        int iIndexOf = str.indexOf("#Intent;");
        if (iIndexOf < 0) {
            return null;
        }
        String strSubstring = str.substring("intent:".length(), iIndexOf);
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        String str4 = "";
        for (String str5 : str.substring(iIndexOf + 8).split(";")) {
            int iIndexOf2 = str5.indexOf(61);
            if (iIndexOf2 >= 0) {
                String strSubstring2 = str5.substring(0, iIndexOf2);
                String strSubstring3 = str5.substring(iIndexOf2 + 1);
                try {
                    strSubstring3 = URLDecoder.decode(strSubstring3.replace("+", "%2B"), "UTF-8");
                } catch (Exception e) {
                }
                if (strSubstring2.equals("scheme")) {
                    str4 = strSubstring3;
                } else if (strSubstring2.startsWith("S.")) {
                    linkedHashMap.put(strSubstring2.substring(2), strSubstring3);
                }
            }
        }
        if (!str4.matches("[a-zA-Z][a-zA-Z0-9+.-]*")) {
            return null;
        }
        Link link = new Link("VIEW", str2, str4 + ":" + strSubstring, str3);
        link.extras.putAll(linkedHashMap);
        return link;
    }

    static final class Endpointer {
        static final int DEAD_INPUT = 3;
        static final int DONE = 1;
        static final double ENTER = 3.0d;
        static final int FLOOR_PERCENTILE = 15;
        static final int FLOOR_WINDOW_MS = 1500;
        static final int LISTENING = 0;
        static final double MIN_RMS = 120.0d;
        static final double MODULATION = 1.8d;
        static final int NO_SPEECH = 2;
        static final int OPEN = 2;
        static final int SETTLE = 1;
        static final double STAY = 1.8d;
        static final int WAIT = 0;
        private int bursts;
        private int endDelay;
        final int frame;
        private int frameFill;
        private int framePeak;
        private long frameSum;
        private int frames;
        double level;
        private int loudRun;
        private int peak;
        private int quietRun;
        final int rate;
        private int recentAt;
        private int ringAt;
        private int ringFill;
        private double runLow;
        private double runMax;
        private double segLow;
        private double segMax;
        private int speechFrames;
        int liveTimeoutMs = 2500;
        int settleMs = 300;
        int settleMaxMs = 800;
        int sustainMs = 120;
        int minSpeechMs = 400;
        int startWindowMs = 15000;
        int maxMs = 30000;
        int endSilenceMs = 650;
        int shortEndSilenceMs = 900;
        int shortSpeechMs = 1000;
        /** Audio kept in front of the first loud frame, so a soft or early first syllable is not cut. */
        int preRollMs = 300;
        /**
         * After the first live audio, frames that may hold the ready chime (played at that moment) do not count as
         * speech or noise floor. Their audio is still kept and the pre-roll reaches back into them.
         */
        int guardMs = 40;
        private final ByteArrayOutputStream pcm = new ByteArrayOutputStream();
        private int phase = LISTENING;
        private int liveFrame = -1;
        private int openFrame = -1;
        private int speechStart = -1;
        private int lastSpeech = -1;
        private final double[] ring = new double[75];
        private final double[] sorted = new double[75];
        private final double[] recent = new double[5];
        private double floor = 40.0d;
        private String reason = "";

        Endpointer(int i) {
            this.rate = i;
            this.frame = i / 50;
        }

        int feed(short[] sArr, int i) {
            int iEndFrame;
            int i2 = LISTENING;
            for (int i3 = LISTENING; i3 < i; i3++) {
                short s = sArr[i3];
                this.pcm.write(s & 255);
                this.pcm.write((s >> 8) & 255);
                long j = s;
                this.frameSum += j * j;
                this.framePeak = Math.max(this.framePeak, Math.abs((int) s));
                int i4 = this.frameFill + 1;
                this.frameFill = i4;
                if (i4 == this.frame && (iEndFrame = endFrame()) != 0 && i2 == 0) {
                    i2 = iEndFrame;
                }
            }
            return i2;
        }

        private double learn(double d) {
            this.ring[this.ringAt] = d;
            this.ringAt = (this.ringAt + 1) % this.ring.length;
            if (this.ringFill < this.ring.length) {
                this.ringFill++;
            }
            System.arraycopy(this.ring, LISTENING, this.sorted, LISTENING, this.ringFill);
            Arrays.sort(this.sorted, LISTENING, this.ringFill);
            return Math.max(1.0d, this.sorted[Math.min(this.ringFill - 1, (this.ringFill * FLOOR_PERCENTILE) / 100)]);
        }

        private int endFrame() {
            boolean z;
            double dSqrt = Math.sqrt(this.frameSum / ((double) this.frame));
            int i = this.framePeak;
            this.frameSum = 0L;
            this.frameFill = LISTENING;
            this.framePeak = LISTENING;
            int i2 = this.frames;
            this.frames = i2 + 1;
            this.peak = Math.max(this.peak, i);
            double d = 0.0d;
            this.level = this.phase != 0 ? Math.max(0.0d, Math.min(1.0d, ((Math.log10(Math.max(dSqrt, 1.0d)) * 20.0d) - 30.0d) / 50.0d)) : 0.0d;
            if (this.phase == 0) {
                if (i > 4) {
                    this.phase = 1;
                    this.liveFrame = i2;
                } else {
                    if (this.frames * 20 < this.liveTimeoutMs) {
                        return LISTENING;
                    }
                    this.reason = "no live audio";
                    return DEAD_INPUT;
                }
            }
            boolean z2 = i2 < this.liveFrame + Math.max(2, this.guardMs / 20);
            if (!z2) {
                this.floor = learn(dSqrt);
            }
            this.recent[this.recentAt] = dSqrt;
            this.recentAt = (this.recentAt + 1) % this.recent.length;
            double[] dArr = this.recent;
            int length = dArr.length;
            for (int i3 = LISTENING; i3 < length; i3++) {
                d += dArr[i3];
            }
            double length2 = d / ((double) this.recent.length);
            boolean z3 = this.speechStart >= 0 || this.loudRun > 0;
            if (z2) {
                z = false;
            } else if (dSqrt > Math.max(this.floor * (z3 ? 1.8d : ENTER), MIN_RMS)) {
                z = true;
            } else {
                z = false;
            }
            if (this.phase == 1) {
                if (z) {
                    if (this.loudRun == 0) {
                        this.runMax = dSqrt;
                        this.runLow = dSqrt;
                    } else {
                        this.runMax = Math.max(this.runMax, dSqrt);
                        this.runLow = Math.min(this.runLow, length2);
                    }
                    this.loudRun++;
                } else {
                    this.loudRun = LISTENING;
                }
                int i4 = (this.frames - this.liveFrame) * 20;
                boolean z4 = i4 >= 200 && dSqrt < this.ring[((this.ringAt + this.ring.length) - 6) % this.ring.length] * 0.8d && this.ringFill > 6;
                if (i4 >= this.settleMaxMs || (i4 >= this.settleMs && !z4)) {
                    this.phase = 2;
                    this.openFrame = this.frames;
                    if (this.loudRun * 20 >= this.sustainMs) {
                        this.speechStart = this.frames - this.loudRun;
                        this.speechFrames = this.loudRun;
                        this.lastSpeech = i2;
                        this.segMax = this.runMax;
                        this.segLow = this.runLow;
                    }
                    // A shorter loud run is kept going: someone who started talking just before the mic settled
                    // carries on into the open phase and is counted from the first loud frame.
                }
                return LISTENING;
            }
            if (z) {
                this.loudRun++;
                this.quietRun = LISTENING;
            } else {
                this.quietRun++;
                this.loudRun = LISTENING;
            }
            String str = "only a short sound";
            if (this.speechStart < 0) {
                if (z) {
                    if (this.loudRun == 1) {
                        this.runMax = dSqrt;
                        this.runLow = dSqrt;
                    } else {
                        this.runMax = Math.max(this.runMax, dSqrt);
                        this.runLow = Math.min(this.runLow, length2);
                    }
                }
                if (this.loudRun * 20 >= this.sustainMs) {
                    this.speechStart = this.frames - this.loudRun;
                    this.speechFrames = this.loudRun;
                    this.lastSpeech = i2;
                    this.segMax = this.runMax;
                    this.segLow = this.runLow;
                } else if (this.loudRun == 0 && (this.frames - this.openFrame) * 20 >= this.startWindowMs) {
                    this.reason = this.bursts > 0 ? str : "no speech";
                    return 2;
                }
            } else {
                str = "only a short sound";
                if (z) {
                    this.speechFrames++;
                    this.lastSpeech = i2;
                    this.segMax = Math.max(this.segMax, dSqrt);
                }
                if (this.quietRun * 20 <= 100) {
                    this.segLow = Math.min(this.segLow, length2);
                }
                this.endDelay = this.speechFrames * 20 < this.shortSpeechMs ? this.shortEndSilenceMs : this.endSilenceMs;
                if (this.quietRun * 20 >= this.endDelay) {
                    boolean z5 = this.segMax >= this.segLow * 1.8d;
                    if (z5 && this.speechFrames * 20 >= this.minSpeechMs) {
                        this.reason = "silence after speech";
                        return 1;
                    }
                    if (z5) {
                        this.bursts++;
                    }
                    this.speechStart = -1;
                    this.speechFrames = LISTENING;
                }
            }
            if ((this.frames - this.openFrame) * 20 >= this.maxMs) {
                boolean z6 = this.speechStart >= 0 && this.speechFrames * 20 >= this.minSpeechMs && this.segMax >= this.segLow * 1.8d;
                this.reason = z6 ? "maximum length" : "no speech";
                if (!z6) {
                    this.speechStart = -1;
                    this.speechFrames = LISTENING;
                }
                return z6 ? 1 : 2;
            }
            if (this.speechStart >= 0 || (this.frames - this.openFrame) * 20 < this.startWindowMs || this.loudRun * 20 < 2000) {
                return LISTENING;
            }
            this.reason = this.bursts > 0 ? str : "no speech";
            return 2;
        }

        int phase() {
            return this.phase;
        }

        int peak() {
            return this.peak;
        }

        int millis() {
            return this.frames * 20;
        }

        int liveMs() {
            if (this.liveFrame < 0) {
                return -1;
            }
            return this.liveFrame * 20;
        }

        int openMs() {
            if (this.openFrame < 0) {
                return -1;
            }
            return this.openFrame * 20;
        }

        int firstSpeechMs() {
            if (this.speechStart < 0) {
                return -1;
            }
            return (this.speechStart - this.openFrame) * 20;
        }

        boolean heardSpeech() {
            return this.speechStart >= 0;
        }

        int speechMs() {
            return this.speechFrames * 20;
        }

        int utteranceMs() {
            return this.speechStart < 0 ? LISTENING : ((this.lastSpeech + 1) - this.speechStart) * 20;
        }

        String reason() {
            return this.reason;
        }

        int noiseFloor() {
            return (int) Math.round(this.floor);
        }

        int enterRms() {
            return (int) Math.round(Math.max(this.floor * ENTER, MIN_RMS));
        }

        int endDelayMs() {
            return this.reason.equals("silence after speech") ? this.endDelay : LISTENING;
        }

        byte[] wav() {
            byte[] byteArray = this.pcm.toByteArray();
            if (this.speechStart < 0) {
                return VoiceCore.wav(new byte[LISTENING], this.rate);
            }
            int i = this.frame * 2;
            int iMax = Math.max(Math.max(LISTENING, this.liveFrame + 1), this.speechStart - (this.preRollMs / 20));
            return VoiceCore.wav(Arrays.copyOfRange(byteArray, iMax * i, Math.max(iMax, Math.min(this.frames, this.lastSpeech + 1 + FLOOR_PERCENTILE)) * i), this.rate);
        }
    }

    static boolean isSilenceText(String str, String str2) {
        if (str == null) {
            str = "";
        }
        List<String> listWords = words(str);
        String strJoin = join(listWords, " ");
        if (SILENCE_TEXT.contains(strJoin) || strJoin.contains("amara org") || strJoin.startsWith("subtitles by") || strJoin.startsWith("subtitles provided") || strJoin.startsWith("subtitled by") || strJoin.startsWith("transcribed by") || strJoin.startsWith("transcription by") || strJoin.startsWith("captions by") || strJoin.startsWith("translated by")) {
            return true;
        }
        return listWords.size() >= 5 && str2 != null && new StringBuilder().append(" ").append(join(words(str2), " ")).append(" ").toString().contains(new StringBuilder().append(" ").append(strJoin).append(" ").toString());
    }

    static byte[] wav(byte[] bArr, int i) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(bArr.length + 44);
        ascii(byteArrayOutputStream, "RIFF");
        le32(byteArrayOutputStream, bArr.length + 36);
        ascii(byteArrayOutputStream, "WAVEfmt ");
        le32(byteArrayOutputStream, 16);
        le16(byteArrayOutputStream, 1);
        le16(byteArrayOutputStream, 1);
        le32(byteArrayOutputStream, i);
        le32(byteArrayOutputStream, i * 2);
        le16(byteArrayOutputStream, 2);
        le16(byteArrayOutputStream, 16);
        ascii(byteArrayOutputStream, "data");
        le32(byteArrayOutputStream, bArr.length);
        byteArrayOutputStream.write(bArr, 0, bArr.length);
        return byteArrayOutputStream.toByteArray();
    }

    private static void ascii(ByteArrayOutputStream byteArrayOutputStream, String str) {
        for (int i = 0; i < str.length(); i++) {
            byteArrayOutputStream.write(str.charAt(i));
        }
    }

    private static void le16(ByteArrayOutputStream byteArrayOutputStream, int i) {
        byteArrayOutputStream.write(i & 255);
        byteArrayOutputStream.write((i >> 8) & 255);
    }

    private static void le32(ByteArrayOutputStream byteArrayOutputStream, int i) {
        le16(byteArrayOutputStream, i & 65535);
        le16(byteArrayOutputStream, (i >> 16) & 65535);
    }
}
