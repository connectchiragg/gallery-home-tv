package com.chirag.home;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/* JADX INFO: loaded from: classes.dex */
final class VoiceArt {
    static final int KEEP = 4;
    static final int MAX_BYTES = 3145728;
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(new ThreadFactory() { // from class: com.chirag.home.VoiceArt$$ExternalSyntheticLambda0
        @Override // java.util.concurrent.ThreadFactory
        public final Thread newThread(Runnable runnable) {
            return VoiceArt.lambda$static$0(runnable);
        }
    });
    private static final LinkedHashMap<String, Bitmap> CACHE = new LinkedHashMap<>(8, 0.75f, true);

    interface Got {
        void got(Bitmap bitmap);
    }

    private VoiceArt() {
    }

    static /* synthetic */ Thread lambda$static$0(Runnable runnable) {
        Thread thread = new Thread(runnable, "voice-art");
        thread.setDaemon(true);
        return thread;
    }

    static void load(final String str, final int i, final Handler handler, final Got got) {
        Bitmap bitmap;
        if (str != null) {
            if (!str.startsWith("https://images.justwatch.com/") && !str.startsWith("https://i.ytimg.com/")) {
                return;
            }
            synchronized (CACHE) {
                bitmap = CACHE.get(str);
            }
            if (bitmap != null) {
                got.got(bitmap);
            } else {
                POOL.execute(new Runnable() { // from class: com.chirag.home.VoiceArt$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        VoiceArt.lambda$load$2(str, i, handler, got);
                    }
                });
            }
        }
    }

    static /* synthetic */ void lambda$load$2(String str, int i, Handler handler, final Got got) {
        try {
            HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
            byte[] byteArray;
            try {
                httpURLConnection.setConnectTimeout(6000);
                httpURLConnection.setReadTimeout(10000);
                httpURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0");
                if (httpURLConnection.getResponseCode() != 200) {
                    return;
                }
                try (InputStream inputStream = httpURLConnection.getInputStream()) {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    byte[] bArr = new byte[16384];
                    while (true) {
                        int i2 = inputStream.read(bArr);
                        if (i2 <= 0) {
                            break;
                        }
                        byteArrayOutputStream.write(bArr, 0, i2);
                        if (byteArrayOutputStream.size() > MAX_BYTES) {
                            return;
                        }
                    }
                    byteArray = byteArrayOutputStream.toByteArray();
                }
            } finally {
                httpURLConnection.disconnect();
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            int i3 = 1;
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length, options);
            while (true) {
                int i4 = i3 * 2;
                if (options.outWidth / i4 < i) {
                    break;
                } else {
                    i3 = i4;
                }
            }
            BitmapFactory.Options options2 = new BitmapFactory.Options();
            options2.inSampleSize = i3;
            options2.inPreferredConfig = Bitmap.Config.RGB_565;
            final Bitmap bitmapDecodeByteArray = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length, options2);
            if (bitmapDecodeByteArray == null) {
                return;
            }
            synchronized (CACHE) {
                CACHE.put(str, bitmapDecodeByteArray);
                Iterator<String> it = CACHE.keySet().iterator();
                while (CACHE.size() > KEEP && it.hasNext()) {
                    it.next();
                    it.remove();
                }
            }
            handler.post(new Runnable() { // from class: com.chirag.home.VoiceArt$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    got.got(bitmapDecodeByteArray);
                }
            });
        } catch (Throwable th) {
        }
    }

    static void clear() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }
}
