package com.chirag.home;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: classes.dex */
final class YouTubeSource {
    static final String PKG = "com.google.android.youtube.tv";
    private static final Pattern CH_ID = Pattern.compile("\"channelId\":\"(UC[A-Za-z0-9_-]{20,30})\"");
    private static final Pattern SIMPLE_TITLE = Pattern.compile("\"title\":\\{\"simpleText\":\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern SUBS = Pattern.compile("\"simpleText\":\"([0-9.,]+\\s?[KMB]?) subscribers?\"");
    private static final Pattern VID = Pattern.compile("^\"videoId\":\"([A-Za-z0-9_-]{11})\"");
    private static final Pattern RUN_TITLE = Pattern.compile("\"title\":\\{\"runs\":\\[\\{\"text\":\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern OWNER = Pattern.compile("\"(?:longBylineText|ownerText)\":\\{\"runs\":\\[\\{\"text\":\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern VIEWS = Pattern.compile("\"viewCountText\":\\{\"simpleText\":\"([0-9.,]+\\s?[KMB]?) views?\"");
    private static final Pattern AGO = Pattern.compile("\"publishedTimeText\":\\{\"simpleText\":\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern F_ID = Pattern.compile("<yt:videoId>([A-Za-z0-9_-]{11})</yt:videoId>");
    private static final Pattern F_TITLE = Pattern.compile("<title>(.*?)</title>", 32);
    private static final Pattern F_NAME = Pattern.compile("<name>(.*?)</name>", 32);
    private static final Pattern F_PUB = Pattern.compile("<published>(\\d{4})-(\\d{2})-(\\d{2})T");
    private static final Pattern F_VIEWS = Pattern.compile("views=\"(\\d+)\"");
    private static final Pattern AGE = Pattern.compile("(\\d+)\\s*(second|sec|minute|min|hour|hr|h|day|d|week|wk|w|month|mo|year|yr|y)s?\\b");
    private static final Set<String> COPYCAT = new HashSet(Arrays.asList("unofficial", "fan", "fans", "fanpage", "fanclub", "clips", "clip", "shorts", "highlights", "reupload", "reuploads", "tribute", "archive", "archives", "moments", "edits", "memes", "status", "dubbed", "reaction", "reactions", "updates", "best", "compilation", "compilations", "daily", "extra", "uploads"));
    private static final Set<String> FILLER = new HashSet(Arrays.asList("video", "videos", "song", "songs", "gaana", "gaane", "gana", "gane", "new", "latest", "newest", "naya", "nayi", "naye", "official", "full", "music", "ka", "ki", "ke", "by", "of", "the", "a"));

    private YouTubeSource() {
    }

    static final class Channel {
        int position;
        double score;
        double sim;
        boolean verified;
        String id = "";
        String name = "";
        long subscribers = -1;

        Channel() {
        }

        public String toString() {
            return this.name + " (" + (this.subscribers < 0 ? "?" : YouTubeSource.shortCount(this.subscribers)) + (this.verified ? ", verified" : "") + ")";
        }
    }

    static final class Video {
        double cover;
        int position;
        double score;
        boolean verified;
        String id = "";
        String title = "";
        String channel = "";
        String published = "";
        long views = -1;
        long ageDays = -1;

        Video() {
        }

        String thumb() {
            return "https://i.ytimg.com/vi/" + this.id + "/hqdefault.jpg";
        }

        String url() {
            return "https://www.youtube.com/watch?v=" + this.id;
        }

        public String toString() {
            return this.title + " · " + this.channel + (this.ageDays >= 0 ? " · " + YouTubeSource.age(this.ageDays) : "");
        }
    }

    private static Map<String, String> headers() {
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        linkedHashMap.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36");
        linkedHashMap.put("Accept-Language", "en");
        linkedHashMap.put("Accept", "text/html,application/xml,*/*");
        return linkedHashMap;
    }

    private static String get(VoiceClient.Http http, String str, int i) throws IOException {
        VoiceClient.Resp respCall = http.call("GET", str, headers(), null, i);
        if (respCall.code != 200) {
            throw new IOException("YouTube HTTP " + respCall.code);
        }
        return respCall.body;
    }

    static List<Channel> channels(VoiceClient.Http http, String str) throws IOException {
        return parseChannels(get(http, "https://www.youtube.com/results?search_query=" + VoiceCore.enc(str) + "&sp=EgIQAg%253D%253D", 8000));
    }

    static List<Video> feed(VoiceClient.Http http, String str) throws IOException {
        if (!str.matches("UC[A-Za-z0-9_-]{20,30}")) {
            throw new IOException("not a channel id");
        }
        return parseFeed(get(http, "https://www.youtube.com/feeds/videos.xml?channel_id=" + str, 8000));
    }

    static List<Video> search(VoiceClient.Http http, String str) throws IOException {
        return parseVideos(get(http, "https://www.youtube.com/results?search_query=" + VoiceCore.enc(str), 8000));
    }

    static String unescape(String str) {
        Object objTryParse = VoiceJson.tryParse("\"" + str + "\"");
        return objTryParse instanceof String ? ((String) objTryParse).trim() : str;
    }

    static List<Channel> parseChannels(String str) {
        ArrayList arrayList = new ArrayList();
        String[] strArrSplit = str.split("\"channelRenderer\":\\{");
        for (int i = 1; i < strArrSplit.length && arrayList.size() < 8; i++) {
            String strSubstring = strArrSplit[i].length() > 6000 ? strArrSplit[i].substring(0, 6000) : strArrSplit[i];
            Matcher matcher = CH_ID.matcher(strSubstring);
            Matcher matcher2 = SIMPLE_TITLE.matcher(strSubstring);
            Matcher matcher3 = SUBS.matcher(strSubstring);
            if (matcher.find() && matcher2.find()) {
                Channel channel = new Channel();
                channel.id = matcher.group(1);
                channel.name = unescape(matcher2.group(1));
                channel.position = arrayList.size();
                if (matcher3.find()) {
                    channel.subscribers = count(matcher3.group(1));
                }
                channel.verified = strSubstring.contains("BADGE_STYLE_TYPE_VERIFIED");
                if (!channel.name.isEmpty()) {
                    arrayList.add(channel);
                }
            }
        }
        return arrayList;
    }

    static List<Video> parseVideos(String str) {
        ArrayList arrayList = new ArrayList();
        String[] strArrSplit = str.split("\"videoRenderer\":\\{");
        for (int i = 1; i < strArrSplit.length && arrayList.size() < 12; i++) {
            String strSubstring = strArrSplit[i].length() > 9000 ? strArrSplit[i].substring(0, 9000) : strArrSplit[i];
            Matcher matcher = VID.matcher(strSubstring);
            Matcher matcher2 = RUN_TITLE.matcher(strSubstring);
            Matcher matcher3 = OWNER.matcher(strSubstring);
            Matcher matcher4 = VIEWS.matcher(strSubstring);
            Matcher matcher5 = AGO.matcher(strSubstring);
            if (matcher.find() && matcher2.find()) {
                Video video = new Video();
                video.id = matcher.group(1);
                video.title = unescape(matcher2.group(1));
                video.position = arrayList.size();
                if (matcher3.find()) {
                    video.channel = unescape(matcher3.group(1));
                }
                if (matcher4.find()) {
                    video.views = count(matcher4.group(1));
                }
                if (matcher5.find()) {
                    video.published = unescape(matcher5.group(1));
                    video.ageDays = ageDays(video.published);
                }
                video.verified = strSubstring.contains("BADGE_STYLE_TYPE_VERIFIED");
                if (!video.title.isEmpty()) {
                    arrayList.add(video);
                }
            }
        }
        return arrayList;
    }

    static String xml(String str) {
        return str.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'").replace("&apos;", "'").replace("&amp;", "&").trim();
    }

    static List<Video> parseFeed(String str) {
        ArrayList arrayList = new ArrayList();
        String[] strArrSplit = str.split("<entry>");
        for (int i = 1; i < strArrSplit.length && arrayList.size() < 15; i++) {
            String str2 = strArrSplit[i];
            Matcher matcher = F_ID.matcher(str2);
            Matcher matcher2 = F_TITLE.matcher(str2);
            Matcher matcher3 = F_NAME.matcher(str2);
            Matcher matcher4 = F_PUB.matcher(str2);
            Matcher matcher5 = F_VIEWS.matcher(str2);
            if (matcher.find() && matcher2.find()) {
                Video video = new Video();
                video.id = matcher.group(1);
                video.title = xml(matcher2.group(1));
                video.position = arrayList.size();
                if (matcher3.find()) {
                    video.channel = xml(matcher3.group(1));
                }
                if (matcher4.find()) {
                    video.published = matcher4.group(1) + "-" + matcher4.group(2) + "-" + matcher4.group(3);
                }
                if (matcher5.find()) {
                    video.views = Long.parseLong(matcher5.group(1));
                }
                arrayList.add(video);
            }
        }
        return arrayList;
    }

    static long count(String str) {
        double d;
        String strTrim = str.replace(",", "").trim();
        if (strTrim.endsWith("K")) {
            d = 1000.0d;
        } else if (strTrim.endsWith("M")) {
            d = 1000000.0d;
        } else {
            d = strTrim.endsWith("B") ? 1.0E9d : 1.0d;
        }
        try {
            return (long) (Double.parseDouble(strTrim.replaceAll("[KMB\\s]", "")) * d);
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    static String shortCount(long j) {
        if (j >= 1000000) {
            return String.format(Locale.ROOT, "%.1fM", Double.valueOf(j / 1000000.0d));
        }
        return j >= 1000 ? (j / 1000) + "K" : String.valueOf(j);
    }

    static long ageDays(String str) {
        long j;
        Matcher matcher = AGE.matcher(str.toLowerCase(Locale.ROOT));
        if (!matcher.find()) {
            return -1L;
        }
        long j2 = Long.parseLong(matcher.group(1));
        String strGroup = matcher.group(2);
        if (strGroup.startsWith("y")) {
            j = 365;
        } else if (strGroup.startsWith("mo")) {
            j = 30;
        } else {
            if (!strGroup.startsWith("w")) {
                if (strGroup.startsWith("d")) {
                    return j2;
                }
                return 0L;
            }
            j = 7;
        }
        return j2 * j;
    }

    static String age(long j) {
        StringBuilder sbAppend;
        String str;
        if (j <= 0) {
            return "today";
        }
        if (j == 1) {
            return "yesterday";
        }
        if (j < 14) {
            sbAppend = new StringBuilder().append(j);
            str = " days ago";
        } else if (j < 60) {
            sbAppend = new StringBuilder().append(j / 7);
            str = " weeks ago";
        } else if (j < 730) {
            sbAppend = new StringBuilder().append(j / 30);
            str = " months ago";
        } else {
            sbAppend = new StringBuilder().append(j / 365);
            str = " years ago";
        }
        return sbAppend.append(str).toString();
    }

    static long daysSince(String str, long j) {
        Matcher matcher = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})").matcher(str);
        if (!matcher.find()) {
            return -1L;
        }
        long j2 = Long.parseLong(matcher.group(1));
        long j3 = Long.parseLong(matcher.group(2));
        long j4 = Long.parseLong(matcher.group(3));
        long j5 = j2 - (j3 <= 2 ? 1L : 0L);
        long j6 = (j5 >= 0 ? j5 : j5 - 399) / 400;
        long j7 = j5 - (400 * j6);
        return Math.max(0L, (j / 86400000) - ((((((j6 * 146097) + (365 * j7)) + (j7 / 4)) - (j7 / 100)) + ((((((j3 + ((long) (j3 > 2 ? -3 : 9))) * 153) + 2) / 5) + j4) - 1)) - 719468));
    }

    static void rankChannels(List<Channel> list, String str, String str2, Set<String> set) {
        for (Channel channel : list) {
            channel.sim = VoiceCore.similarity(str, channel.name);
            if (str2 != null && !str2.isEmpty()) {
                channel.sim = Math.max(channel.sim, VoiceCore.similarity(str2, channel.name) * 0.9d);
            }
            if (copycat(channel.name, str, str2)) {
                channel.sim *= 0.6d;
            }
            double d = 0.0d;
            double dMin = (channel.sim * 0.55d) + ((channel.subscribers <= 0 ? 0.0d : Math.min(1.0d, Math.log10(channel.subscribers) / 8.0d)) * 0.3d) + (channel.verified ? 0.1d : 0.0d) + ((1.0d - (((double) Math.min(channel.position, 7)) / 7.0d)) * 0.05d);
            if (set.contains(channel.name.toLowerCase(Locale.ROOT))) {
                d = 0.05d;
            }
            channel.score = dMin + d;
        }
        Collections.sort(list, new Comparator<Channel>() { // from class: com.chirag.home.YouTubeSource.1
            @Override // java.util.Comparator
            public int compare(Channel channel2, Channel channel3) {
                return Double.compare(channel3.score, channel2.score);
            }
        });
    }

    static boolean copycat(String str, String str2, String str3) {
        HashSet hashSet = new HashSet(VoiceCore.words(str2));
        if (str3 != null) {
            hashSet.addAll(VoiceCore.words(str3));
        }
        for (String str4 : VoiceCore.words(str)) {
            if (COPYCAT.contains(str4) && !hashSet.contains(str4)) {
                return true;
            }
        }
        return false;
    }

    static String essence(String str) {
        ArrayList arrayList = new ArrayList();
        for (String str2 : VoiceCore.words(str)) {
            if (!FILLER.contains(str2)) {
                arrayList.add(str2);
            }
        }
        return arrayList.isEmpty() ? VoiceCore.join(VoiceCore.words(str), " ") : VoiceCore.join(arrayList, " ");
    }

    static void rankVideos(List<Video> list, String str, boolean z, Set<String> set) {
        String strEssence = essence(str);
        for (Video video : list) {
            video.cover = VoiceCore.covers(strEssence, video.title + " " + video.channel);
            double dCovers = (video.cover * 0.5d) + (VoiceCore.covers(essence(video.title), strEssence) * 0.12d) + ((1.0d - (((double) Math.min(video.position, 9)) / 9.0d)) * 0.13d) + ((video.views <= 0 ? 0.0d : Math.min(1.0d, Math.log10(video.views) / 8.0d)) * 0.1d) + (video.verified ? 0.05d : 0.0d) + (set.contains(video.channel.toLowerCase(Locale.ROOT)) ? 0.05d : 0.0d);
            if (z) {
                dCovers = (dCovers * 0.8d) + ((video.ageDays < 0 ? 0.3d : Math.max(0.0d, 1.0d - (video.ageDays / 365.0d))) * 0.2d);
            }
            video.score = dCovers;
        }
        Collections.sort(list, new Comparator<Video>() { // from class: com.chirag.home.YouTubeSource.2
            @Override // java.util.Comparator
            public int compare(Video video2, Video video3) {
                return Double.compare(video3.score, video2.score);
            }
        });
    }
}
