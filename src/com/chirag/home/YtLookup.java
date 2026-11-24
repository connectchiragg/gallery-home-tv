package com.chirag.home;

import android.content.Context;
import android.net.Uri;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;

/* JADX INFO: loaded from: classes.dex */
final class YtLookup {
    static final String PKG = "com.google.android.youtube.tv";
    static final ExecutorService POOL = Executors.newSingleThreadExecutor();
    static final Set<String> tried = Collections.synchronizedSet(new HashSet());
    static final Pattern ID = Pattern.compile("^\"videoId\":\"([A-Za-z0-9_-]{11})\"");
    static final Pattern TITLE = Pattern.compile("\"title\":\\{\"runs\":\\[\\{\"text\":\"((?:[^\"\\\\]|\\\\.)*)\"");
    static final Pattern OWNER = Pattern.compile("\"ownerText\":\\{\"runs\":\\[\\{\"text\":\"((?:[^\"\\\\]|\\\\.)*)\"");

    YtLookup() {
    }

    static void ensure(Context context, final String str, final String str2) {
        final Context applicationContext = context.getApplicationContext();
        if (tried.add(str + "|" + str2)) {
            POOL.execute(new Runnable() { // from class: com.chirag.home.YtLookup$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    YtLookup.lambda$ensure$0(str, str2, applicationContext);
                }
            });
        }
    }

    static /* synthetic */ void lambda$ensure$0(String str, String str2, Context context) {
        try {
            String strFind = find(str, str2);
            if (strFind != null) {
                HistoryDb.get(context).setMediaId(PKG, str, str2, strFind);
                ArtStore.youtube(context, str, str2, strFind);
            }
        } catch (Exception e) {
            tried.remove(str + "|" + str2);
        }
    }

    static String find(String str, String str2) throws Exception {
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL("https://www.youtube.com/results?search_query=" + Uri.encode(str + (str2.isEmpty() ? "" : " " + str2))).openConnection();
        httpURLConnection.setConnectTimeout(8000);
        httpURLConnection.setReadTimeout(15000);
        httpURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0");
        httpURLConnection.setRequestProperty("Accept-Language", "en");
        String string;
        try {
            try (InputStream inputStream = httpURLConnection.getInputStream()) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] bArr = new byte[16384];
                while (true) {
                    int i = inputStream.read(bArr);
                    if (i <= 0) {
                        break;
                    }
                    byteArrayOutputStream.write(bArr, 0, i);
                }
                string = byteArrayOutputStream.toString("UTF-8");
            }
        } finally {
            httpURLConnection.disconnect();
        }
        String[] strArrSplit = string.split("\"videoRenderer\":\\{");
        for (int i2 = 1; i2 < strArrSplit.length && i2 <= 20; i2++) {
            String str3 = strArrSplit[i2];
            Matcher matcher = ID.matcher(str3);
            if (matcher.find()) {
                Matcher matcher2 = TITLE.matcher(str3);
                Matcher matcher3 = OWNER.matcher(str3);
                if (matcher2.find()) {
                    String strTrim = unescape(matcher2.group(1)).trim();
                    String strTrim2 = matcher3.find() ? unescape(matcher3.group(1)).trim() : "";
                    if (strTrim.equals(str.trim()) && (str2.isEmpty() || strTrim2.equals(str2.trim()))) {
                        return matcher.group(1);
                    }
                } else {
                    continue;
                }
            }
        }
        return null;
    }

    static String unescape(String str) {
        try {
            return new JSONArray("[\"" + str + "\"]").getString(0);
        } catch (Exception e) {
            return str;
        }
    }
}
