package com.chirag.home;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.HardwareBuffer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/* JADX INFO: loaded from: classes.dex */
final class ScreenReader {
    static final int MAX_ITEMS = 5;
    static final String TAG = "ScreenRead";
    static final int WIDTH = 960;
    private static VoiceClient.Config cfg;
    private static long cfgAt;
    static final ScreenRead.Decider decider = new ScreenRead.Decider();
    static final Handler h = new Handler(Looper.getMainLooper());
    static final ExecutorService POOL = Executors.newSingleThreadExecutor();
    static final Map<String, long[]> lastState = new HashMap();
    static final Map<String, ScreenRead.Record> resolved = new HashMap();

    private ScreenReader() {
    }

    private static synchronized VoiceClient.Config config(Context context) {
        VoiceClient.Config config;
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        config = null;
        if (cfgAt == 0 || jElapsedRealtime - cfgAt > 60000) {
            cfg = VoiceClient.Config.read(new File(context.getExternalFilesDir(null), "voice.json"));
            cfgAt = jElapsedRealtime;
        }
        if (cfg != null && cfg.screenRead && !cfg.groqKey.isEmpty() && Build.VERSION.SDK_INT >= 30) {
            config = cfg;
        }
        return config;
    }

    static void playback(Context context, String str, boolean z, long j, long j2, boolean z2) {
        if (ScreenRead.rule(str) == null) {
            return;
        }
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        synchronized (lastState) {
            lastState.put(str, new long[]{z ? 1L : 0L, jElapsedRealtime, j, j2});
        }
        if (config(context) == null) {
            return;
        }
        long jPlayback = decider.playback(str, z, j, j2, z2, jElapsedRealtime);
        if (jPlayback >= 0) {
            schedule(context, str, jPlayback);
        }
    }

    static void event(Context context, String str) {
        if (config(context) == null) {
            return;
        }
        long jEvent = decider.event(str, SystemClock.elapsedRealtime());
        if (jEvent >= 0) {
            schedule(context, str, jEvent);
        }
    }

    private static void schedule(Context context, final String str, long j) {
        final Context applicationContext = context.getApplicationContext();
        h.postDelayed(new Runnable() { // from class: com.chirag.home.ScreenReader.1
            @Override // java.lang.Runnable
            public void run() {
                ScreenReader.capture(applicationContext, str);
            }
        }, j);
    }

    private static long[] state(String str) {
        long[] jArr;
        synchronized (lastState) {
            jArr = lastState.get(str);
        }
        return jArr;
    }

    private static boolean playing(String str) {
        long[] jArrState = state(str);
        return jArrState != null && jArrState[0] == 1 && SystemClock.elapsedRealtime() - jArrState[1] < 40000;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void done(Context context, String str, boolean z, boolean z2) {
        long jResult = decider.result(str, z, z2, SystemClock.elapsedRealtime());
        if (jResult >= 0) {
            schedule(context, str, jResult);
        }
    }

    static void capture(final Context context, final String str) {
        final ScreenRead.Rule rule = ScreenRead.rule(str);
        final VoiceClient.Config config = config(context);
        ProfileService profileService = ProfileService.inst;
        if (decider.confirm(str, (rule == null || config == null || profileService == null || !profileService.inFront(str)) ? false : true, playing(str))) {
            try {
                profileService.takeScreenshot(0, POOL, new AccessibilityService.TakeScreenshotCallback() { // from class: com.chirag.home.ScreenReader.2
                    @Override // android.accessibilityservice.AccessibilityService.TakeScreenshotCallback
                    public void onSuccess(AccessibilityService.ScreenshotResult screenshotResult) {
                        byte[] bArrEncode;
                        try {
                            bArrEncode = ScreenReader.encode(screenshotResult);
                        } catch (Throwable th) {
                            Log.i(ScreenReader.TAG, str + ": screenshot unusable (" + th + ")");
                            bArrEncode = null;
                        }
                        boolean z = false;
                        if (bArrEncode == null) {
                            Log.i(ScreenReader.TAG, str + ": screenshot empty or black (protected window?), nothing sent");
                            ScreenReader.done(context, str, false, false);
                        } else {
                            try {
                                z = ScreenReader.read(context, config, rule, bArrEncode);
                            } catch (Throwable th2) {
                                Log.i(ScreenReader.TAG, str + ": reading failed: " + th2.getMessage());
                            }
                            ScreenReader.done(context, str, true, z);
                        }
                    }

                    @Override // android.accessibilityservice.AccessibilityService.TakeScreenshotCallback
                    public void onFailure(int i) {
                        Log.i(ScreenReader.TAG, str + ": screenshot refused (code " + i + "), nothing sent");
                        ScreenReader.done(context, str, false, false);
                    }
                });
            } catch (Throwable th) {
                Log.i(TAG, str + ": cannot take screenshots: " + th);
                done(context, str, false, false);
            }
        }
    }

    static byte[] encode(AccessibilityService.ScreenshotResult screenshotResult) {
        HardwareBuffer hardwareBuffer = screenshotResult.getHardwareBuffer();
        Bitmap bitmapWrapHardwareBuffer = Bitmap.wrapHardwareBuffer(hardwareBuffer, screenshotResult.getColorSpace());
        byte[] byteArray = null;
        Bitmap bitmapCopy = bitmapWrapHardwareBuffer == null ? null : bitmapWrapHardwareBuffer.copy(Bitmap.Config.ARGB_8888, false);
        hardwareBuffer.close();
        if (bitmapWrapHardwareBuffer != null) {
            bitmapWrapHardwareBuffer.recycle();
        }
        if (bitmapCopy == null || bitmapCopy.getWidth() < 16) {
            return null;
        }
        if (bitmapCopy.getWidth() > WIDTH) {
            Bitmap bitmapCreateScaledBitmap = Bitmap.createScaledBitmap(bitmapCopy, WIDTH, Math.max(1, Math.round(bitmapCopy.getHeight() * (960.0f / bitmapCopy.getWidth()))), true);
            bitmapCopy.recycle();
            bitmapCopy = bitmapCreateScaledBitmap;
        }
        int width = bitmapCopy.getWidth();
        int height = bitmapCopy.getHeight();
        int[] iArr = new int[width];
        int i = 0;
        int i2 = 0;
        int i3 = 0;
        int i4 = 255;
        while (i2 < height) {
            bitmapCopy.getPixels(iArr, 0, width, 0, i2, width, 1);
            i = i;
            int iMax = i3;
            int iMin = i4;
            for (int i5 = 0; i5 < width; i5 += 4) {
                int i6 = iArr[i5];
                int i7 = (((((i6 >> 16) & 255) * 3) + (((i6 >> 8) & 255) * 6)) + (i6 & 255)) / 10;
                if (i7 > 96) {
                    i++;
                }
                iMin = Math.min(iMin, i7);
                iMax = Math.max(iMax, i7);
            }
            i2 += 4;
            i3 = iMax;
            i4 = iMin;
        }
        if (i >= 40 && i3 - i4 >= 32) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(98304);
            bitmapCopy.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
            byteArray = byteArrayOutputStream.toByteArray();
        }
        bitmapCopy.recycle();
        return byteArray;
    }

    static boolean read(Context context, VoiceClient.Config config, ScreenRead.Rule rule, byte[] bArr) throws Exception {
        VoiceClient voiceClient = new VoiceClient(config, VoiceClient.REAL);
        String strVision = voiceClient.vision(ScreenRead.prompt(rule.kind), bArr, "image/jpeg");
        VoiceCore.App appAppForPkg = VoiceCore.appForPkg(rule.pkg);
        String str = appAppForPkg == null ? null : appAppForPkg.key;
        HistoryDb historyDb = HistoryDb.get(context);
        long jCurrentTimeMillis = System.currentTimeMillis();
        boolean z = false;
        if (rule.kind.equals("overlay")) {
            ScreenRead.Reading overlay = ScreenRead.parseOverlay(strVision);
            if (overlay == null) {
                Log.i(TAG, rule.pkg + ": no title on screen");
                return false;
            }
            ScreenRead.Record recordResolve = resolve(voiceClient, config, overlay, str, rule.strict);
            if (recordResolve == null) {
                Log.i(TAG, rule.pkg + ": read \"" + overlay + "\", not sure enough to keep");
                return false;
            }
            long[] jArrState = state(rule.pkg);
            if (jArrState != null && SystemClock.elapsedRealtime() - jArrState[1] < 60000) {
                z = true;
            }
            picture(context, historyDb, historyDb.identified(rule.pkg, recordResolve.title, recordResolve.subtitle, recordResolve.mediaId, z ? jArrState[3] : 0L, z ? jArrState[2] : -1L, jCurrentTimeMillis), rule.pkg, recordResolve);
            Log.i(TAG, rule.pkg + ": playing " + recordResolve.title + " · " + recordResolve.subtitle + (recordResolve.mediaId == null ? " (no link)" : ""));
            return true;
        }
        List<ScreenRead.Reading> home = ScreenRead.parseHome(strVision);
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        ArrayList<ScreenRead.Record> arrayList3 = new ArrayList<>();
        for (ScreenRead.Reading reading : home) {
            arrayList.add(reading.title);
            if (ScreenRead.inProgress(reading) && arrayList3.size() < MAX_ITEMS) {
                ScreenRead.Record recordResolve2 = resolve(voiceClient, config, reading, str, rule.strict);
                if (recordResolve2 != null && !arrayList2.contains(recordResolve2.title)) {
                    arrayList3.add(recordResolve2);
                    arrayList2.add(recordResolve2.title);
                }
            }
        }
        ArrayList arrayList4 = new ArrayList();
        for (HistoryDb.Item item : historyDb.screenRows(rule.pkg)) {
            arrayList4.add(new ScreenRead.Row(item.id, item.title, item.subtitle));
        }
        long[] jArrApplyHome = historyDb.applyHome(rule.pkg, ScreenRead.plan(arrayList4, arrayList2, arrayList), arrayList3, jCurrentTimeMillis);
        for (int i = 0; i < jArrApplyHome.length; i++) {
            if (jArrApplyHome[i] > 0) {
                picture(context, historyDb, jArrApplyHome[i], rule.pkg, arrayList3.get(i));
            }
        }
        Log.i(TAG, rule.pkg + ": Continue Watching row read, " + home.size() + " cards, " + arrayList3.size() + " kept " + arrayList2);
        return !arrayList3.isEmpty();
    }

    private static ScreenRead.Record resolve(VoiceClient voiceClient, VoiceClient.Config config, ScreenRead.Reading reading, String str, boolean z) {
        ScreenRead.Record record;
        VoiceCore.Title titleIdentify;
        String str2 = str + "|" + reading.title.toLowerCase(Locale.ROOT) + "|" + reading.season + "|" + reading.episode;
        synchronized (resolved) {
            record = resolved.get(str2);
        }
        if (record == null) {
            boolean z2 = false;
            if (config.justwatch) {
                try {
                    titleIdentify = voiceClient.identify(reading, str, z);
                } catch (VoiceClient.VoiceException e) {
                    z2 = true;
                    titleIdentify = null;
                }
            } else {
                titleIdentify = null;
            }
            ScreenRead.Record record2 = ScreenRead.record(reading, titleIdentify, str, z);
            if (record2 != null && !z2) {
                synchronized (resolved) {
                    if (resolved.size() > 200) {
                        resolved.clear();
                    }
                    resolved.put(str2, record2);
                }
            }
            if (record2 == null) {
                return null;
            }
            record = record2;
        }
        ScreenRead.Record record3 = new ScreenRead.Record();
        record3.title = record.title;
        record3.mediaId = record.mediaId;
        record3.artUrl = record.artUrl;
        record3.subtitle = reading.minutesLeft > 0 ? ScreenRead.subtitle(reading, null, record.subtitle) : record.subtitle;
        return record3;
    }

    private static void picture(Context context, HistoryDb historyDb, long j, String str, ScreenRead.Record record) {
        if (j <= 0 || record.artUrl.isEmpty()) {
            return;
        }
        try {
            historyDb.setArtById(j, ArtStore.download(record.artUrl, ArtStore.file(context, str, record.title, "")));
        } catch (Exception e) {
            Log.i(TAG, "no picture for " + record.title + ": " + e.getMessage());
        }
    }
}
