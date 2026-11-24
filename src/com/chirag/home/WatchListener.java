package com.chirag.home;

import android.content.ComponentName;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.service.notification.NotificationListenerService;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public class WatchListener extends NotificationListenerService {
    static final String TAG = "WatchHistory";
    static final long TICK_MS = 15000;
    ComponentName me;
    MediaSessionManager msm;
    final Handler h = new Handler(Looper.getMainLooper());
    final Map<MediaController.Callback, MediaController> watched = new HashMap();
    final MediaSessionManager.OnActiveSessionsChangedListener onChange = new MediaSessionManager.OnActiveSessionsChangedListener() { // from class: com.chirag.home.WatchListener$$ExternalSyntheticLambda0
        @Override // android.media.session.MediaSessionManager.OnActiveSessionsChangedListener
        public final void onActiveSessionsChanged(List list) {
            attach(list);
        }
    };
    final Runnable tick = new Runnable() { // from class: com.chirag.home.WatchListener.1
        @Override // java.lang.Runnable
        public void run() {
            Iterator<MediaController> it = WatchListener.this.watched.values().iterator();
            while (it.hasNext()) {
                WatchListener.this.record(it.next());
            }
            WatchListener.this.h.postDelayed(this, WatchListener.TICK_MS);
        }
    };

    @Override // android.service.notification.NotificationListenerService
    public void onListenerConnected() {
        AirWake.start(this);
        this.me = new ComponentName(this, (Class<?>) WatchListener.class);
        this.msm = (MediaSessionManager) getSystemService(MediaSessionManager.class);
        try {
            this.msm.addOnActiveSessionsChangedListener(this.onChange, this.me);
            attach(this.msm.getActiveSessions(this.me));
        } catch (SecurityException e) {
            Log.w(TAG, "no session access", e);
        }
        this.h.postDelayed(this.tick, TICK_MS);
        ArtStore.backfill(this);
        ArtStore.stills(this);
    }

    @Override // android.service.notification.NotificationListenerService
    public void onListenerDisconnected() {
        this.h.removeCallbacks(this.tick);
        if (this.msm != null) {
            this.msm.removeOnActiveSessionsChangedListener(this.onChange);
        }
        detachAll();
    }

    void detachAll() {
        for (Map.Entry<MediaController.Callback, MediaController> entry : this.watched.entrySet()) {
            entry.getValue().unregisterCallback(entry.getKey());
        }
        this.watched.clear();
    }

    void attach(List<MediaController> list) {
        detachAll();
        if (list == null) {
            return;
        }
        for (final MediaController mediaController : list) {
            String packageName = mediaController.getPackageName();
            if (!packageName.equals(getPackageName()) && !packageName.startsWith("com.android.bluetooth")) {
                MediaController.Callback callback = new MediaController.Callback() { // from class: com.chirag.home.WatchListener.2

                    @Override // android.media.session.MediaController.Callback
                    public void onMetadataChanged(MediaMetadata mediaMetadata) {
                        WatchListener.this.record(mediaController);
                    }

                    @Override // android.media.session.MediaController.Callback
                    public void onPlaybackStateChanged(PlaybackState playbackState) {
                        WatchListener.this.record(mediaController);
                    }
                };
                mediaController.registerCallback(callback, this.h);
                this.watched.put(callback, mediaController);
                record(mediaController);
            }
        }
    }

    void record(MediaController mediaController) {
        MediaMetadata metadata = mediaController.getMetadata();
        PlaybackState playbackState = mediaController.getPlaybackState();
        if (playbackState == null) {
            return;
        }
        int state = playbackState.getState();
        if (state == 3 || state == 2 || state == 6) {
            String packageName = mediaController.getPackageName();
            String str = null;
            String strFirst = metadata == null ? null : first(metadata, "android.media.metadata.DISPLAY_TITLE", "android.media.metadata.TITLE");
            if (strFirst == null || !strFirst.replace(" ", "").equalsIgnoreCase(Ui.label(this, packageName).replace(" ", ""))) {
                str = strFirst;
            }
            if (str == null || str.trim().isEmpty()) {
                if (state == 6) {
                    return;
                }
                long jCurrentTimeMillis = System.currentTimeMillis();
                long j = metadata == null ? 0L : metadata.getLong("android.media.metadata.DURATION");
                long jPosition = position(playbackState, j);
                boolean zPlayback = HistoryDb.get(this).playback(packageName, state == 3, j, jPosition, jCurrentTimeMillis);
                ScreenReader.playback(this, packageName, state == 3, jPosition, j, zPlayback);
                if (!zPlayback && HistoryDb.TITLE_HIDDEN.contains(packageName)) {
                    HistoryDb.get(this).upsert(packageName, "", "", null, null, 0L, -1L, jCurrentTimeMillis, state == 3);
                    return;
                }
                return;
            }
            if (Pending.matches(packageName, str) && state == 3) {
                mediaController.getTransportControls().seekTo(Pending.position);
                Pending.clear();
                return;
            }
            String strFirst2 = first(metadata, "android.media.metadata.DISPLAY_SUBTITLE", "android.media.metadata.ARTIST", "android.media.metadata.ALBUM");
            String string = metadata.getString("android.media.metadata.MEDIA_ID");
            long j2 = metadata.getLong("android.media.metadata.DURATION");
            long jPosition2 = position(playbackState, j2);
            String packageName2 = mediaController.getPackageName();
            String strSaveArt = saveArt(packageName2, str, strFirst2, metadata);
            String strTrim = strFirst2 != null ? strFirst2.trim() : "";
            long jCurrentTimeMillis2 = System.currentTimeMillis();
            HistoryDb.get(this).upsert(packageName2, str.trim(), strTrim, string, strSaveArt, j2, jPosition2, jCurrentTimeMillis2, state == 3);
            HistoryDb.get(this).titled(packageName2, str.trim(), strTrim, jCurrentTimeMillis2);
            if (packageName2.equals("com.google.android.youtube.tv") && string == null) {
                YtLookup.ensure(this, str.trim(), strTrim);
            }
            if (strSaveArt == null) {
                ArtStore.ensure(this, packageName2, str.trim(), strTrim, j2);
            }
            if (ArtStore.episode(strTrim) != null) {
                ArtStore.stills(this);
            }
        }
    }

    static long position(PlaybackState playbackState, long j) {
        long position = playbackState.getPosition();
        if (playbackState.getState() == 3 && playbackState.getLastPositionUpdateTime() > 0) {
            position += (long) ((SystemClock.elapsedRealtime() - playbackState.getLastPositionUpdateTime()) * playbackState.getPlaybackSpeed());
        }
        return (j <= 0 || position <= j) ? position : j;
    }

    String saveArt(String str, String str2, String str3, MediaMetadata mediaMetadata) {
        Bitmap bitmap = mediaMetadata.getBitmap("android.media.metadata.ART");
        if (bitmap == null) {
            bitmap = mediaMetadata.getBitmap("android.media.metadata.ALBUM_ART");
        }
        if (bitmap == null) {
            bitmap = mediaMetadata.getBitmap("android.media.metadata.DISPLAY_ICON");
        }
        if (bitmap == null) {
            return null;
        }
        File file = new File(getFilesDir(), "art");
        file.mkdirs();
        File file2 = new File(file, Integer.toHexString((str + "|" + str2 + "|" + str3).hashCode()) + ".jpg");
        if (file2.exists()) {
            return file2.getAbsolutePath();
        }
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file2);
            try {
                float fMin = Math.min(1.0f, 480.0f / Math.max(bitmap.getWidth(), bitmap.getHeight()));
                Bitmap.createScaledBitmap(bitmap, Math.max(1, (int) (bitmap.getWidth() * fMin)), Math.max(1, (int) (bitmap.getHeight() * fMin)), true).compress(Bitmap.CompressFormat.JPEG, 82, fileOutputStream);
                String absolutePath = file2.getAbsolutePath();
                fileOutputStream.close();
                return absolutePath;
            } catch (Throwable th) {
                try {
                    fileOutputStream.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        } catch (Exception e) {
            return null;
        }
    }

    static String first(MediaMetadata mediaMetadata, String... strArr) {
        for (String str : strArr) {
            CharSequence text = mediaMetadata.getText(str);
            if (text != null && text.length() > 0) {
                return text.toString();
            }
        }
        return null;
    }
}
