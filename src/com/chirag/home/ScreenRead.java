package com.chirag.home;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
final class ScreenRead {
    static final int DAILY_CAP = 100;
    static final long FAILED_GAP_MS = 60000;
    static final String HOME_PROMPT = "This is a screenshot of a streaming app's home screen on a TV. Find the row headed \"Continue Watching\" (it may also be called \"Up Next\" or \"Continue watching for ...\"). Reply with JSON only, no other text: {\"items\": [{\"title\": string, \"season\": number or null, \"episode\": number or null, \"minutes_left\": number or null, \"watchlist\": true or false}]}, one entry per card of that row, left to right. \"title\" is the name written in the card's artwork. Small text on a card such as \"S1, E6 · 27m\" means season 1, episode 6, 27 minutes left; \"1h 23m\" means 83 minutes left with no season or episode; \"In Watchlist\" means watchlist true and minutes_left null. Do not include cards from any other row. If there is no such row, reply {\"items\": []}.";
    static final long NEW_PLAY_GAP_MS = 120000;
    static final String OVERLAY_PROMPT = "This is a screenshot of a streaming app's playback screen on a TV. The video itself may be black; read only the text overlay. Reply with JSON only, no other text: {\"title\": string or null, \"season\": number or null, \"episode\": number or null, \"episode_title\": string or null}. \"title\" is the name of the show or film exactly as written on screen. A line such as \"S2 E3 SOME NAME\" means season 2, episode 3, episode_title \"SOME NAME\". Use null for anything that is not shown. If no show or film title is visible, reply {\"title\": null}.";
    static final long RETRY_MS = 6000;
    static final long START_DELAY_MS = 1500;
    static final String PRIME = "com.amazon.amazonvideo.livingroom";
    static final String OVERLAY = "overlay";
    static final long OPEN_GAP_MS = 20000;
    static final String APPLE_TV = "com.apple.atve.androidtv.appletv";
    static final String HOME = "home";
    static final Rule[] RULES = {new Rule(PRIME, OVERLAY, false, 700, OPEN_GAP_MS, 3), new Rule(APPLE_TV, HOME, true, 8000, 300000, 1)};

    private ScreenRead() {
    }

    static final class Rule {
        final long delayMs;
        final String kind;
        final int maxPerPlay;
        final long minGapMs;
        final String pkg;
        final boolean strict;

        Rule(String str, String str2, boolean z, long j, long j2, int i) {
            this.pkg = str;
            this.kind = str2;
            this.strict = z;
            this.delayMs = j;
            this.minGapMs = j2;
            this.maxPerPlay = i;
        }
    }

    static Rule rule(String str) {
        for (Rule rule : RULES) {
            if (rule.pkg.equals(str)) {
                return rule;
            }
        }
        return null;
    }

    static String prompt(String str) {
        return str.equals(HOME) ? HOME_PROMPT : OVERLAY_PROMPT;
    }

    static final class Reading {
        int episode;
        int minutesLeft;
        int season;
        boolean watchlist;
        String title = "";
        String episodeTitle = "";

        Reading() {
        }

        public String toString() {
            return this.title + (this.season > 0 ? " S" + this.season : "") + (this.episode > 0 ? " E" + this.episode : "") + (this.episodeTitle.isEmpty() ? "" : " \"" + this.episodeTitle + "\"") + (this.minutesLeft > 0 ? " " + this.minutesLeft + "m left" : "") + (this.watchlist ? " watchlist" : "");
        }
    }

    static Object jsonIn(String str) {
        if (str == null) {
            return null;
        }
        String strReplaceAll = str.replaceAll("(?s)<think>.*?</think>", " ");
        for (int iIndexOf = strReplaceAll.indexOf(123); iIndexOf >= 0; iIndexOf = strReplaceAll.indexOf(123, iIndexOf + 1)) {
            for (int iLastIndexOf = strReplaceAll.lastIndexOf(125); iLastIndexOf > iIndexOf; iLastIndexOf = strReplaceAll.lastIndexOf(125, iLastIndexOf - 1)) {
                Object objTryParse = VoiceJson.tryParse(strReplaceAll.substring(iIndexOf, iLastIndexOf + 1));
                if (objTryParse instanceof Map) {
                    return objTryParse;
                }
            }
        }
        return null;
    }

    private static String text(Object obj, String str) {
        Object objAt = VoiceJson.at(obj, str);
        if (!(objAt instanceof String)) {
            return "";
        }
        String strTrim = ((String) objAt).replaceAll("\\s+", " ").trim();
        String lowerCase = strTrim.toLowerCase(Locale.ROOT);
        return (strTrim.length() > 120 || lowerCase.equals("null") || lowerCase.equals("none") || lowerCase.equals("unknown") || lowerCase.equals("n/a") || lowerCase.equals("string")) ? "" : strTrim;
    }

    private static int number(Object obj, String str, int i) {
        double dDoubleValue;
        Object objAt = VoiceJson.at(obj, str);
        if (objAt instanceof Double) {
            dDoubleValue = ((Double) objAt).doubleValue();
        } else {
            if (!(objAt instanceof String)) {
                return 0;
            }
            try {
                dDoubleValue = Double.parseDouble(((String) objAt).trim());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        if (dDoubleValue < 1.0d || dDoubleValue > i || dDoubleValue != Math.rint(dDoubleValue)) {
            return 0;
        }
        return (int) dDoubleValue;
    }

    private static Reading reading(Object obj) {
        if (!(obj instanceof Map)) {
            return null;
        }
        Reading reading = new Reading();
        reading.title = text(obj, "title");
        if (reading.title.isEmpty()) {
            return null;
        }
        reading.season = number(obj, "season", 99);
        reading.episode = number(obj, "episode", 9999);
        reading.episodeTitle = text(obj, "episode_title");
        reading.minutesLeft = number(obj, "minutes_left", 600);
        reading.watchlist = VoiceJson.bool(obj, false, "watchlist");
        return reading;
    }

    static Reading parseOverlay(String str) {
        return reading(jsonIn(str));
    }

    static List<Reading> parseHome(String str) {
        ArrayList arrayList = new ArrayList();
        Iterator<Object> it = VoiceJson.list(jsonIn(str), "items").iterator();
        while (it.hasNext()) {
            Reading reading = reading(it.next());
            if (reading != null && arrayList.size() < 8) {
                arrayList.add(reading);
            }
        }
        return arrayList;
    }

    static boolean inProgress(Reading reading) {
        return reading.minutesLeft > 0 && !reading.watchlist;
    }

    static String left(int i) {
        StringBuilder sbAppend;
        if (i <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (i >= 60) {
            StringBuilder sbAppend2 = new StringBuilder().append(i / 60).append(" h ");
            int i2 = i % 60;
            sbAppend = sbAppend2.append(i2 > 0 ? i2 + " min" : "");
        } else {
            sbAppend = new StringBuilder().append(i).append(" min");
        }
        return sb.append(sbAppend.toString().trim()).append(" left").toString();
    }

    static String subtitle(Reading reading, String str, String str2) {
        String str3;
        String strNice;
        StringBuilder sbAppend = new StringBuilder().append(reading.season > 0 ? "S" + reading.season : "");
        if (reading.episode > 0) {
            str3 = (reading.season > 0 ? " " : "") + "E" + reading.episode;
        } else {
            str3 = "";
        }
        String string = sbAppend.append(str3).toString();
        if (reading.minutesLeft > 0) {
            strNice = left(reading.minutesLeft);
        } else {
            if (!reading.episodeTitle.isEmpty()) {
                str = reading.episodeTitle;
            } else if (str == null) {
                str = "";
            }
            strNice = VoiceCore.nice(str);
        }
        if (string.isEmpty()) {
            return strNice.isEmpty() ? str2 : strNice;
        }
        return strNice.isEmpty() ? string : string + " · " + strNice;
    }

    static final class Record {
        String mediaId;
        String title = "";
        String subtitle = "";
        String artUrl = "";

        Record() {
        }

        public String toString() {
            return this.title + " | " + this.subtitle + " | " + this.mediaId + " | " + this.artUrl;
        }
    }

    static Record record(Reading reading, VoiceCore.Title title, String str, boolean z) {
        Record record = new Record();
        if (title == null) {
            if (z) {
                return null;
            }
            record.title = VoiceCore.nice(reading.title);
            record.subtitle = subtitle(reading, null, "");
            return record;
        }
        record.title = title.name;
        record.subtitle = subtitle(reading, title.target != null && !title.target.seasonOnly ? title.target.ep.name : null, title.year);
        for (VoiceCore.Link link : VoiceCore.episodeLinks(title, str, true, null)) {
            if (!link.scope().isEmpty()) {
                record.mediaId = VoiceCore.mediaId(link);
                break;
            }
        }
        record.artUrl = title.artUrl();
        return record;
    }

    static final class Decider {
        private final Map<String, App> apps = new HashMap();
        private long dayStart;
        private int today;

        private static final class App {
            int attempts;
            boolean identified;
            long lastCapture;
            long lastDur;
            long lastEvent;
            long lastPos;
            long lastRead;
            long lastSighting;
            boolean pending;
            long playStart;
            boolean retried;
            boolean startTried;
            boolean wasPaused;

            private App() {
                this.lastPos = -1L;
                this.lastCapture = -4611686018427387904L;
                this.lastRead = -4611686018427387904L;
            }
        }

        Decider() {
        }

        private App app(String str) {
            App app = this.apps.get(str);
            if (app == null) {
                App app2 = new App();
                this.apps.put(str, app2);
                return app2;
            }
            return app;
        }

        synchronized long playback(String str, boolean z, long j, long j2, boolean z2, long j3) {
            boolean z3;
            boolean z4;
            long j4;
            Rule rule = ScreenRead.rule(str);
            if (rule != null && rule.kind.equals(ScreenRead.OVERLAY)) {
                App app = app(str);
                if (app.lastSighting != 0 && j3 - app.lastSighting <= ScreenRead.NEW_PLAY_GAP_MS) {
                    z3 = (WatchRule.plausible(j2) && WatchRule.plausible(app.lastDur) && Math.abs(j2 - app.lastDur) > Math.max(5000L, app.lastDur / 50)) || (j >= 0 && app.lastPos >= 0 && ScreenRead.FAILED_GAP_MS + j < app.lastPos);
                } else {
                    z3 = true;
                }
                if (z3) {
                    z4 = false;
                    app.attempts = 0;
                    app.startTried = false;
                    app.playStart = j3;
                    app.wasPaused = false;
                } else {
                    z4 = false;
                }
                app.identified = z2;
                if (!app.identified && !app.pending && app.attempts < rule.maxPerPlay && j3 - app.lastCapture >= rule.minGapMs && budget(j3)) {
                    if (!z && !app.wasPaused) {
                        j4 = rule.delayMs;
                    } else if (z && z3 && !app.startTried) {
                        app.startTried = true;
                        j4 = ScreenRead.START_DELAY_MS;
                    } else {
                        j4 = -1;
                    }
                } else {
                    j4 = -1;
                }
                if (j4 >= 0) {
                    app.pending = true;
                    app.attempts++;
                    app.lastCapture = j3;
                }
                app.wasPaused = !z ? true : z4;
                app.lastSighting = j3;
                app.lastPos = j;
                app.lastDur = j2;
                return j4;
            }
            return -1L;
        }

        synchronized long event(String str, long j) {
            Rule rule = ScreenRead.rule(str);
            if (rule != null && rule.kind.equals(ScreenRead.HOME)) {
                App app = app(str);
                boolean z = app.lastEvent == 0 || j - app.lastEvent > ScreenRead.OPEN_GAP_MS;
                app.lastEvent = j;
                if (z && !app.pending && j - app.lastRead >= rule.minGapMs && j - app.lastCapture >= ScreenRead.FAILED_GAP_MS && budget(j)) {
                    app.pending = true;
                    app.retried = false;
                    app.lastCapture = j;
                    return rule.delayMs;
                }
                return -1L;
            }
            return -1L;
        }

        synchronized boolean confirm(String str, boolean z, boolean z2) {
            boolean z3;
            Rule rule = ScreenRead.rule(str);
            App app = app(str);
            if (rule == null || !app.pending || !z) {
                z3 = false;
            } else if (rule.kind.equals(ScreenRead.HOME)) {
                if (z2) {
                    z3 = false;
                } else {
                    z3 = true;
                }
            } else if (app.identified) {
                z3 = false;
            } else {
                z3 = true;
            }
            if (!z3) {
                app.pending = false;
                if (rule != null && rule.kind.equals(ScreenRead.OVERLAY) && app.attempts > 0) {
                    app.attempts--;
                }
            }
            return z3;
        }

        synchronized long result(String str, boolean z, boolean z2, long j) {
            Rule rule = ScreenRead.rule(str);
            App app = app(str);
            app.pending = false;
            if (z) {
                count(j);
            }
            if (rule == null) {
                return -1L;
            }
            if (rule.kind.equals(ScreenRead.OVERLAY)) {
                if (z2) {
                    app.identified = true;
                } else if (!z && app.attempts > 0) {
                    app.attempts--;
                }
                return -1L;
            }
            if (z) {
                app.lastRead = j;
                return -1L;
            }
            if (app.retried) {
                return -1L;
            }
            app.retried = true;
            app.pending = true;
            return ScreenRead.RETRY_MS;
        }

        private boolean budget(long j) {
            if (j - this.dayStart >= 86400000) {
                this.dayStart = j;
                this.today = 0;
            }
            return this.today < ScreenRead.DAILY_CAP;
        }

        private void count(long j) {
            budget(j);
            this.today++;
        }
    }

    static final class Row {
        final long id;
        final String subtitle;
        final String title;

        Row(long j, String str, String str2) {
            this.id = j;
            this.title = str;
            this.subtitle = str2;
        }
    }

    static final class Op {
        static final String DELETE = "delete";
        static final String INSERT = "insert";
        static final String UPDATE = "update";
        final long id;
        final int index;
        final String what;

        Op(String str, long j, int i) {
            this.what = str;
            this.id = j;
            this.index = i;
        }

        public String toString() {
            return this.what + (this.id > 0 ? " #" + this.id : "") + (this.index >= 0 ? " <- item " + this.index : "");
        }
    }

    static List<Op> plan(List<Row> list, List<String> list2, List<String> list3) {
        Row next;
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        for (int i = 0; i < list2.size(); i++) {
            if (!list2.subList(0, i).contains(list2.get(i))) {
                Iterator<Row> it = list.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        next = null;
                        break;
                    }
                    next = it.next();
                    if (next.title.equalsIgnoreCase(list2.get(i)) && !arrayList2.contains(Long.valueOf(next.id))) {
                        break;
                    }
                }
                if (next != null) {
                    arrayList2.add(Long.valueOf(next.id));
                    arrayList.add(new Op("update", next.id, i));
                } else {
                    arrayList.add(new Op("insert", 0L, i));
                }
            }
        }
        if (list3.isEmpty()) {
            return arrayList;
        }
        for (Row row : list) {
            if (!arrayList2.contains(Long.valueOf(row.id))) {
                Iterator<String> it2 = list3.iterator();
                boolean z = false;
                while (it2.hasNext()) {
                    if (VoiceCore.similarity(it2.next(), row.title) >= 0.6d) {
                        z = true;
                    }
                }
                if (!z) {
                    arrayList.add(new Op("delete", row.id, -1));
                }
            }
        }
        return arrayList;
    }
}
