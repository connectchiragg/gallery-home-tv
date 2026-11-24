package com.chirag.home;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/* JADX INFO: loaded from: classes.dex */
final class VoiceChime {
    static final boolean ENABLED = true;
    static final int FOUND = 1;
    static final int GAP_AFTER_MISS_MS = 280;
    static final int MISS = 2;
    static final int OPEN = 0;
    private static final int RATE = 44100;
    static final float VOL_FOUND = 0.09f;
    static final float VOL_MISS = 0.08f;
    static final float VOL_OPEN = 0.16f;
    private static final AudioTrack[] TRACK = new AudioTrack[3];
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(new ThreadFactory() { // from class: com.chirag.home.VoiceChime$$ExternalSyntheticLambda1
        @Override // java.util.concurrent.ThreadFactory
        public final Thread newThread(Runnable runnable) {
            return VoiceChime.lambda$static$0(runnable);
        }
    });

    private VoiceChime() {
    }

    static /* synthetic */ Thread lambda$static$0(Runnable runnable) {
        Thread thread = new Thread(runnable, "voice-chime");
        thread.setDaemon(ENABLED);
        return thread;
    }

    static void play(final int i) {
        POOL.execute(new Runnable() { // from class: com.chirag.home.VoiceChime$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                VoiceChime.lambda$play$1(i);
            }
        });
    }

    static /* synthetic */ void lambda$play$1(int i) {
        try {
            AudioTrack audioTrack = TRACK[i];
            if (audioTrack == null) {
                AudioTrack[] audioTrackArr = TRACK;
                AudioTrack audioTrackBuild = build(i);
                audioTrackArr[i] = audioTrackBuild;
                audioTrack = audioTrackBuild;
            }
            if (audioTrack == null) {
                return;
            }
            if (audioTrack.getPlayState() == 3) {
                audioTrack.stop();
            }
            audioTrack.reloadStaticData();
            audioTrack.play();
        } catch (Throwable th) {
        }
    }

    static void release() {
        POOL.execute(new Runnable() { // from class: com.chirag.home.VoiceChime$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                VoiceChime.lambda$release$2();
            }
        });
    }

    static /* synthetic */ void lambda$release$2() {
        for (int i = OPEN; i < TRACK.length; i += FOUND) {
            try {
                if (TRACK[i] != null) {
                    TRACK[i].release();
                }
            } catch (Throwable th) {
            }
            TRACK[i] = null;
        }
    }

    private static void note(float[] fArr, int i, int i2, double d, float f) {
        int i3 = (i * RATE) / 1000;
        int i4 = (RATE * i2) / 1000;
        int i5 = OPEN;
        while (i5 < i4) {
            int i6 = i3 + i5;
            if (i6 < fArr.length) {
                double d2 = i5;
                double d3 = d2 / 44100.0d;
                double d4 = 1.0d;
                double dCos = (i5 < 441 ? 0.5d - (Math.cos((d2 * 3.141592653589793d) / ((double) 441)) * 0.5d) : 1.0d) * Math.exp((-d3) * 16.0d);
                int i7 = i4 - i5;
                if (i7 < 441) {
                    d4 = ((double) i7) / ((double) 441);
                }
                double d5 = 6.283185307179586d * d * d3;
                fArr[i6] = fArr[i6] + ((float) (((double) f) * dCos * d4 * (Math.sin(d5) + (Math.sin(2.0d * d5) * 0.22d) + (Math.sin(d5 * 3.0d) * 0.06d))));
                i5 += FOUND;
            } else {
                return;
            }
        }
    }

    private static AudioTrack build(int i) {
        int i2 = ((i == MISS ? 160 : 240) * RATE) / 1000;
        float[] fArr = new float[i2];
        if (i == 0) {
            note(fArr, OPEN, 150, 659.25d, VOL_OPEN);
            note(fArr, 75, 165, 987.77d, VOL_OPEN);
        } else if (i == FOUND) {
            note(fArr, OPEN, 150, 880.0d, VOL_FOUND);
            note(fArr, 80, 160, 659.25d, VOL_FOUND);
        } else {
            note(fArr, OPEN, 160, 523.25d, VOL_MISS);
        }
        short[] sArr = new short[i2];
        for (int i3 = OPEN; i3 < i2; i3 += FOUND) {
            sArr[i3] = (short) Math.max(-32767, Math.min(32767, Math.round(fArr[i3] * 32767.0f)));
        }
        AudioTrack audioTrackBuild = new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(13).setContentType(4).build()).setAudioFormat(new AudioFormat.Builder().setSampleRate(RATE).setEncoding(MISS).setChannelMask(4).build()).setTransferMode(OPEN).setBufferSizeInBytes(i2 * MISS).build();
        audioTrackBuild.write(sArr, OPEN, i2);
        return audioTrackBuild;
    }
}
