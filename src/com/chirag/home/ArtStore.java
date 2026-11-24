package com.chirag.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
final class ArtStore {
    static final int BACKFILL = 20;
    static final int MAX_BYTES = 3145728;
    private static final String NO_STILL = "no_still";
    static final String TAG = "WatchHistory";
    static final int WIDTH = 640;
    static final ExecutorService POOL = Executors.newSingleThreadExecutor();
    static final Set<String> seen = Collections.synchronizedSet(new HashSet());
    static final Pattern SE = Pattern.compile("\\bS(\\d{1,2}) ?E(\\d{1,4})\\b");
    private static final Set<String> stillSeen = Collections.synchronizedSet(new HashSet());

    private ArtStore() {
    }

    private static String key(String str, String str2, String str3) {
        return str + "|" + str2 + "|" + str3;
    }

    static File file(Context context, String str, String str2, String str3) {
        File file = new File(context.getFilesDir(), "art");
        file.mkdirs();
        return new File(file, "jw_" + Integer.toHexString(key(str, str2, str3).hashCode()) + ".jpg");
    }

    static String download(String str, File file) throws Exception {
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }
        if (!str.startsWith("https://images.justwatch.com/") && !str.startsWith("https://i.ytimg.com/vi/") && !str.startsWith("https://static.tvmaze.com/")) {
            throw new IllegalArgumentException("unexpected image host");
        }
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
        byte[] byteArray;
        try {
            httpURLConnection.setConnectTimeout(8000);
            httpURLConnection.setReadTimeout(15000);
            httpURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0");
            if (httpURLConnection.getResponseCode() != 200) {
                throw new IllegalStateException("HTTP " + httpURLConnection.getResponseCode());
            }
            try (InputStream inputStream = httpURLConnection.getInputStream()) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] bArr = new byte[16384];
                while (true) {
                    int i = inputStream.read(bArr);
                    if (i <= 0) {
                        break;
                    }
                    byteArrayOutputStream.write(bArr, 0, i);
                    if (byteArrayOutputStream.size() > MAX_BYTES) {
                        throw new IllegalStateException("image too large");
                    }
                }
                byteArray = byteArrayOutputStream.toByteArray();
            }
        } finally {
            httpURLConnection.disconnect();
        }
        Bitmap bitmapDecodeByteArray = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        if (bitmapDecodeByteArray == null) {
            throw new IllegalStateException("not an image");
        }
        float fMin = Math.min(1.0f, 640.0f / bitmapDecodeByteArray.getWidth());
        if (fMin < 1.0f) {
            bitmapDecodeByteArray = Bitmap.createScaledBitmap(bitmapDecodeByteArray, WIDTH, Math.max(1, Math.round(bitmapDecodeByteArray.getHeight() * fMin)), true);
        }
        File file2 = new File(file.getPath() + ".tmp");
        try (FileOutputStream fileOutputStream = new FileOutputStream(file2)) {
            bitmapDecodeByteArray.compress(Bitmap.CompressFormat.JPEG, 82, fileOutputStream);
        }
        if (!file2.renameTo(file)) {
            file2.delete();
            throw new IllegalStateException("could not save");
        }
        return file.getAbsolutePath();
    }

    static void launched(Context context, VoiceCore.Link link, VoiceCore.Title title) {
        if (link == null || title == null || link.scope().isEmpty()) {
            return;
        }
        final Context applicationContext = context.getApplicationContext();
        final String str = link.pkg;
        final String strTrim = title.name.trim();
        final String strLaunchSubtitle = VoiceCore.launchSubtitle(title, link);
        final String strArtUrl = title.artUrl();
        if (strTrim.isEmpty()) {
            return;
        }
        try {
            HistoryDb.get(applicationContext).recordLaunch(str, strTrim, strLaunchSubtitle, VoiceCore.mediaId(link), System.currentTimeMillis());
            seen.add(key(str, strTrim, strLaunchSubtitle));
            POOL.execute(new Runnable() { // from class: com.chirag.home.ArtStore.1
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        HistoryDb historyDb = HistoryDb.get(applicationContext);
                        if (!strArtUrl.isEmpty()) {
                            historyDb.setArtIfNone(str, strTrim, strLaunchSubtitle, ArtStore.download(strArtUrl, ArtStore.file(applicationContext, str, strTrim, strLaunchSubtitle)));
                        }
                        historyDb.markArtTried(str, strTrim, strLaunchSubtitle, System.currentTimeMillis());
                    } catch (Exception e) {
                        Log.w(ArtStore.TAG, "no picture for " + strTrim + ": " + e);
                    }
                    ArtStore.sweep(applicationContext);
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "could not record launch", e);
        }
    }

    static void ensure(Context context, final String str, final String str2, final String str3, long j) {
        if (str2.isEmpty() || str.equals("com.google.android.youtube.tv") || str.equals("com.spotify.tv.android")) {
            return;
        }
        if ((j <= 0 || j >= 600000) && seen.add(key(str, str2, str3))) {
            final Context applicationContext = context.getApplicationContext();
            POOL.execute(new Runnable() { // from class: com.chirag.home.ArtStore.2
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        HistoryDb historyDb = HistoryDb.get(applicationContext);
                        HistoryDb.Item itemFind = historyDb.find(str, str2, str3);
                        if (itemFind != null && itemFind.art == null && itemFind.artTried == 0) {
                            String str4 = null;
                            VoiceClient.Config config = VoiceClient.Config.read(new File(applicationContext.getExternalFilesDir(null), "voice.json"));
                            if (config != null && config.justwatch) {
                                VoiceClient voiceClient = new VoiceClient(config, VoiceClient.REAL);
                                VoiceCore.App appAppForPkg = VoiceCore.appForPkg(str);
                                if (appAppForPkg != null) {
                                    str4 = appAppForPkg.key;
                                }
                                VoiceCore.Title titleArtMatch = VoiceCore.artMatch(str4, str2, voiceClient.justWatch(str2), false);
                                if (titleArtMatch == null && str4 != null && !str3.isEmpty()) {
                                    titleArtMatch = VoiceCore.artMatch(str4, str3, voiceClient.justWatch(str3), true);
                                }
                                if (titleArtMatch != null) {
                                    historyDb.setArtIfNone(str, str2, str3, ArtStore.download(titleArtMatch.artUrl(), ArtStore.file(applicationContext, str, str2, str3)));
                                }
                                historyDb.markArtTried(str, str2, str3, System.currentTimeMillis());
                                Log.i(ArtStore.TAG, "picture for \"" + str2 + "\" (" + str + "): " + (titleArtMatch == null ? "no sure match, keeping the app banner" : titleArtMatch.name + " " + titleArtMatch.year));
                            }
                        }
                    } catch (Exception e) {
                        Log.w(ArtStore.TAG, "picture lookup failed for " + str2 + ": " + e.getMessage());
                    }
                }
            });
        }
    }

    static int[] episode(String str) {
        if (str == null) {
            return null;
        }
        Matcher matcher = SE.matcher(str);
        if (matcher.find()) {
            return new int[]{Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))};
        }
        return null;
    }

    static File stillFile(Context context, String str, String str2, String str3) {
        File file = new File(context.getFilesDir(), "art");
        file.mkdirs();
        return new File(file, "ep_" + Integer.toHexString(key(str, str2, str3).hashCode()) + ".jpg");
    }

    private static String get(String str) throws Exception {
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
        try {
            httpURLConnection.setConnectTimeout(8000);
            httpURLConnection.setReadTimeout(12000);
            httpURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0");
            if (httpURLConnection.getResponseCode() == 200) {
                try (InputStream inputStream = httpURLConnection.getInputStream()) {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    byte[] bArr = new byte[8192];
                    while (true) {
                        int i = inputStream.read(bArr);
                        if (i <= 0) {
                            break;
                        }
                        byteArrayOutputStream.write(bArr, 0, i);
                    }
                    return byteArrayOutputStream.toString("UTF-8");
                }
            }
            return null;
        } finally {
            httpURLConnection.disconnect();
        }
    }

    private static String post(String str, String str2) throws Exception {
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
        try {
            httpURLConnection.setConnectTimeout(8000);
            httpURLConnection.setReadTimeout(12000);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setRequestProperty("Content-Type", "application/json");
            httpURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0");
            httpURLConnection.getOutputStream().write(str2.getBytes("UTF-8"));
            if (httpURLConnection.getResponseCode() == 200) {
                try (InputStream inputStream = httpURLConnection.getInputStream()) {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    byte[] bArr = new byte[8192];
                    while (true) {
                        int i = inputStream.read(bArr);
                        if (i <= 0) {
                            break;
                        }
                        byteArrayOutputStream.write(bArr, 0, i);
                    }
                    return byteArrayOutputStream.toString("UTF-8");
                }
            }
            return null;
        } finally {
            httpURLConnection.disconnect();
        }
    }

    static String stillUrl(String str, int i, int i2) throws Exception {
        String str2;
        int iOptInt;
        String str3;
        JSONObject jSONObjectOptJSONObject;
        JSONObject jSONObjectOptJSONObject2;
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("query", "query($q:String!){popularTitles(country:IN,first:5,filter:{searchQuery:$q,objectTypes:[SHOW]}){edges{node{content(country:IN,language:\"en\"){title externalIds{imdbId}}}}}}");
        jSONObject.put("variables", new JSONObject().put("q", str));
        String strPost = post("https://apis.justwatch.com/graphql", jSONObject.toString());
        if (strPost == null) {
            return "";
        }
        JSONArray jSONArrayOptJSONArray = new JSONObject(strPost).optJSONObject("data").optJSONObject("popularTitles").optJSONArray("edges");
        String strOptString = "";
        for (int i3 = 0; jSONArrayOptJSONArray != null && i3 < jSONArrayOptJSONArray.length() && strOptString.isEmpty(); i3++) {
            JSONObject jSONObject2 = jSONArrayOptJSONArray.getJSONObject(i3).getJSONObject("node").getJSONObject("content");
            if (VoiceCore.similarity(str, jSONObject2.optString("title")) >= 0.9d && (jSONObjectOptJSONObject2 = jSONObject2.optJSONObject("externalIds")) != null) {
                strOptString = jSONObjectOptJSONObject2.optString("imdbId", "");
            }
        }
        return (!strOptString.startsWith("tt") || (str2 = get(new StringBuilder().append("https://api.tvmaze.com/lookup/shows?imdb=").append(strOptString).toString())) == null || (iOptInt = new JSONObject(str2).optInt("id", 0)) == 0 || (str3 = get(new StringBuilder().append("https://api.tvmaze.com/shows/").append(iOptInt).append("/episodebynumber?season=").append(i).append("&number=").append(i2).toString())) == null || (jSONObjectOptJSONObject = new JSONObject(str3).optJSONObject("image")) == null) ? "" : jSONObjectOptJSONObject.optString("original", jSONObjectOptJSONObject.optString("medium", ""));
    }

    static void still(Context context, HistoryDb historyDb, HistoryDb.Item item) {
        String str = item.subtitle == null ? "" : item.subtitle;
        int[] iArrEpisode = episode(str);
        if (iArrEpisode == null || item.title.isEmpty()) {
            return;
        }
        if ((item.art == null || !new File(item.art).getName().startsWith("ep_")) && stillSeen.add(key(item.pkg, item.title, str))) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(NO_STILL, 0);
            String strKey = key(item.pkg, item.title, str);
            if (sharedPreferences.getBoolean(strKey, false)) {
                return;
            }
            try {
                String strStillUrl = stillUrl(item.title, iArrEpisode[0], iArrEpisode[1]);
                if (strStillUrl.isEmpty()) {
                    sharedPreferences.edit().putBoolean(strKey, true).apply();
                    Log.i(TAG, "no still for " + item.title + " " + str);
                } else {
                    historyDb.setArt(item.pkg, item.title, str, download(strStillUrl, stillFile(context, item.pkg, item.title, str)));
                    Log.i(TAG, "still for " + item.title + " " + str);
                }
            } catch (Exception e) {
                Log.w(TAG, "still lookup failed for " + item.title + ": " + e.getMessage());
            }
        }
    }

    static void stills(Context context) {
        final Context applicationContext = context.getApplicationContext();
        POOL.execute(new Runnable() { // from class: com.chirag.home.ArtStore.3
            @Override // java.lang.Runnable
            public void run() {
                HistoryDb historyDb = HistoryDb.get(applicationContext);
                Iterator<HistoryDb.Item> it = historyDb.recent(null, System.currentTimeMillis() - 2592000000L, 40, false).iterator();
                while (it.hasNext()) {
                    ArtStore.still(applicationContext, historyDb, it.next());
                }
            }
        });
    }

    static void backfill(Context context) {
        final Context applicationContext = context.getApplicationContext();
        POOL.execute(new Runnable() { // from class: com.chirag.home.ArtStore.4
            @Override // java.lang.Runnable
            public void run() {
                try {
                    for (HistoryDb.Item item : HistoryDb.get(applicationContext).missingArt(ArtStore.BACKFILL)) {
                        if (!item.pkg.equals("com.google.android.youtube.tv")) {
                            ArtStore.ensure(applicationContext, item.pkg, item.title, item.subtitle == null ? "" : item.subtitle, item.duration);
                        } else if (item.mediaId != null) {
                            ArtStore.youtube(applicationContext, item.title, item.subtitle, item.mediaId);
                        }
                    }
                } catch (Exception e) {
                    Log.w(ArtStore.TAG, "backfill failed", e);
                }
                ArtStore.sweep(applicationContext);
            }
        });
    }

    static void youtube(Context context, final String str, final String str2, final String str3) {
        if (str3 == null || !str3.matches("[A-Za-z0-9_-]{11}") || !seen.add(key("com.google.android.youtube.tv", str, str2))) {
            return;
        }
        final Context applicationContext = context.getApplicationContext();
        POOL.execute(new Runnable() { // from class: com.chirag.home.ArtStore.5
            @Override // java.lang.Runnable
            public void run() {
                try {
                    HistoryDb historyDb = HistoryDb.get(applicationContext);
                    HistoryDb.Item itemFind = historyDb.find("com.google.android.youtube.tv", str, str2);
                    if (itemFind != null && itemFind.art == null && itemFind.artTried == 0) {
                        historyDb.setArtIfNone("com.google.android.youtube.tv", str, str2, ArtStore.download("https://i.ytimg.com/vi/" + str3 + "/hqdefault.jpg", ArtStore.file(applicationContext, "com.google.android.youtube.tv", str, str2)));
                        historyDb.markArtTried("com.google.android.youtube.tv", str, str2, System.currentTimeMillis());
                    }
                } catch (Exception e) {
                    Log.w(ArtStore.TAG, "no thumbnail for " + str + ": " + e.getMessage());
                }
            }
        });
    }

    static void sweep(Context context) {
        try {
            File[] fileArrListFiles = new File(context.getFilesDir(), "art").listFiles();
            if (fileArrListFiles == null) {
                return;
            }
            Set<String> setArtPaths = HistoryDb.get(context).artPaths();
            long jCurrentTimeMillis = System.currentTimeMillis() - 86400000;
            for (File file : fileArrListFiles) {
                if (file.isFile() && file.lastModified() < jCurrentTimeMillis && !setArtPaths.contains(file.getAbsolutePath())) {
                    file.delete();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "sweep failed", e);
        }
    }
}
