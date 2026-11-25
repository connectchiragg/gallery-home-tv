package com.chirag.home;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Log;
import java.util.Locale;
import java.util.Set;

/**
 * Short spoken replies: "Playing lo-fi beats on Spotify", "Here's what I found".
 * With a Deepgram key (voice.json "deepgram_key") the line is spoken by a natural Aura-2 voice ("tts_voice",
 * default aura-2-thalia-en), streamed as 24 kHz PCM and played with an AudioTrack. Without a key, or when the
 * network call fails, Android's own text-to-speech says it instead (Google engine, works offline).
 */
final class VoiceSpeaker {
    static final String TAG = "Voice";
    /** A line asked for before the engine is ready is still said if the engine comes up within this time. */
    static final long LATE_MS = 2500;
    static final Locale EN_IN = new Locale("en", "IN");

    private final Handler main = new Handler(Looper.getMainLooper());
    private TextToSpeech tts;
    private boolean ready;
    private boolean dead;
    private String pending;
    private Runnable pendingDone;
    private long pendingAt;
    private Runnable done;
    private int utterance;

    // ---- Deepgram Aura-2 (cloud voice) --------------------------------------------------------------
    static final int CLOUD_RATE = 24000;
    private final String cloudKey, cloudVoice;
    private final java.util.concurrent.ExecutorService cloud = java.util.concurrent.Executors.newSingleThreadExecutor();
    private volatile android.media.AudioTrack track;
    private volatile int cloudTurn;
    /** Fixed lines are fetched once and kept. */
    private static final java.util.Map<String, byte[]> CACHE = java.util.Collections.synchronizedMap(new java.util.HashMap<String, byte[]>());

    VoiceSpeaker(Context context) {
        this(context, null, null);
    }

    VoiceSpeaker(Context context, String deepgramKey, String voice) {
        this.cloudKey = deepgramKey == null ? "" : deepgramKey.trim();
        this.cloudVoice = voice == null || voice.trim().isEmpty() ? "aura-2-thalia-en" : voice.trim();
        this.appContext = context.getApplicationContext();
        // The TV's own engine takes ~2.5 s to start and starting it while the mic opens delayed real audio; with a
        // cloud voice it is only started if a cloud line fails.
        if (this.cloudKey.isEmpty()) {
            startLocal();
        }
    }

    private final Context appContext;

    private void startLocal() {
        if (tts != null || dead) {
            return;
        }
        final long t0 = SystemClock.elapsedRealtime();
        tts = new TextToSpeech(appContext, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        init(status, SystemClock.elapsedRealtime() - t0);
                    }
                });
            }
        });
    }

    private void init(int status, long ms) {
        if (dead || tts == null) {
            return;
        }
        if (status != TextToSpeech.SUCCESS) {
            Log.w(TAG, "tts: engine failed to start (" + status + ")");
            dead = true;
            runPendingDone();
            return;
        }
        tts.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
        tts.setSpeechRate(1.05f);
        String picked = pickVoice();
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String id) {
            }

            @Override
            public void onDone(String id) {
                finished(id);
            }

            @Override
            public void onError(String id) {
                finished(id);
            }

            @Override
            public void onStop(String id, boolean interrupted) {
                finished(id);
            }
        });
        ready = true;
        Log.i(TAG, "tts: ready in " + ms + " ms, engine " + tts.getDefaultEngine() + ", voice " + picked);
        if (pending != null) {
            String text = pending;
            Runnable d = pendingDone;
            boolean fresh = SystemClock.elapsedRealtime() - pendingAt <= LATE_MS;
            pending = null;
            pendingDone = null;
            if (fresh) {
                say(text, d);
            } else if (d != null) {
                d.run();
            }
        }
    }

    /** An en-IN voice installed on the TV (no network), else any en-IN voice, else the engine's English. */
    private String pickVoice() {
        Voice best = null;
        int bestScore = -1;
        try {
            Set<Voice> voices = tts.getVoices();
            if (voices != null) {
                for (Voice v : voices) {
                    Locale l = v.getLocale();
                    if (l == null || !"en".equals(l.getLanguage())) {
                        continue;
                    }
                    boolean india = "US".equals(l.getCountry()); // fallback voice: US English sounds most natural on this engine
                    boolean installed = v.getFeatures() == null || !v.getFeatures().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED);
                    int score = (india ? 100 : 0) + (v.isNetworkConnectionRequired() ? 0 : 40) + (installed ? 20 : 0) + v.getQuality() / 100;
                    if (score > bestScore) {
                        best = v;
                        bestScore = score;
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "tts: could not list voices: " + e);
        }
        if (best != null && "US".equals(best.getLocale().getCountry())) {
            tts.setVoice(best);
            return best.getName();
        }
        int r = tts.setLanguage(Locale.US);
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
            if (best != null) {
                tts.setVoice(best);
                return best.getName() + " (no en-IN voice)";
            }
            tts.setLanguage(Locale.US);
            return "en-US default";
        }
        return "en-IN default";
    }

    /** Says text, replacing anything being said. done (may be null) runs on the main thread when it ends or fails. */
    void say(String text, Runnable whenDone) {
        if (text != null && !text.trim().isEmpty() && !dead && !cloudKey.isEmpty()) {
            sayCloud(text.trim(), whenDone);
            return;
        }
        sayLocal(text, whenDone);
    }

    private void sayCloud(final String text, final Runnable whenDone) {
        stopTrack();
        final int turn = ++cloudTurn;
        done = whenDone;
        utterance++;
        final String id = "v" + utterance;
        cloud.execute(new Runnable() {
            public void run() {
                long t0 = SystemClock.elapsedRealtime();
                byte[] pcm = CACHE.get(cloudVoice + "|" + text);
                try {
                    if (pcm == null) {
                        pcm = fetch(text);
                        if (text.length() < 40) CACHE.put(cloudVoice + "|" + text, pcm);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "tts: Deepgram failed (" + e.getMessage() + "), using the TV's voice");
                    main.post(new Runnable() { public void run() { if (turn == cloudTurn && !dead) sayLocal(text, whenDone); } });
                    return;
                }
                if (turn != cloudTurn || dead) return;
                Log.i(TAG, "tts: \"" + text + "\" (" + cloudVoice + ", " + (SystemClock.elapsedRealtime() - t0) + " ms)");
                play(pcm, turn);
                finished(id);
            }
        });
    }

    private byte[] fetch(String text) throws Exception {
        java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL("https://api.deepgram.com/v1/speak?model="
                + java.net.URLEncoder.encode(cloudVoice, "UTF-8") + "&encoding=linear16&sample_rate=" + CLOUD_RATE + "&container=none").openConnection();
        try {
            c.setConnectTimeout(4000);
            c.setReadTimeout(8000);
            c.setDoOutput(true);
            c.setRequestProperty("Authorization", "Token " + cloudKey);
            c.setRequestProperty("Content-Type", "application/json");
            org.json.JSONObject body = new org.json.JSONObject().put("text", text);
            c.getOutputStream().write(body.toString().getBytes("UTF-8"));
            if (c.getResponseCode() != 200) throw new java.io.IOException("HTTP " + c.getResponseCode());
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            try (java.io.InputStream in = c.getInputStream()) {
                byte[] buf = new byte[8192];
                for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            c.disconnect();
        }
    }

    /** Plays 16-bit mono PCM and returns when it has been heard (or was stopped). Worker thread. */
    private void play(byte[] pcm, int turn) {
        android.media.AudioTrack t;
        try {
            t = new android.media.AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    .setAudioFormat(new android.media.AudioFormat.Builder().setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(CLOUD_RATE).setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO).build())
                    .setTransferMode(android.media.AudioTrack.MODE_STATIC).setBufferSizeInBytes(Math.max(pcm.length, 2)).build();
        } catch (Exception e) {
            Log.w(TAG, "tts: audio output failed: " + e);
            return;
        }
        t.write(pcm, 0, pcm.length);
        track = t;
        t.play();
        long ms = pcm.length / 2 * 1000L / CLOUD_RATE;
        long end = SystemClock.elapsedRealtime() + ms + 150;
        while (SystemClock.elapsedRealtime() < end && turn == cloudTurn && !dead) {
            SystemClock.sleep(40);
        }
        stopTrack();
    }

    private void stopTrack() {
        android.media.AudioTrack t = track;
        track = null;
        if (t != null) {
            try { t.stop(); } catch (Exception ignored) {}
            t.release();
        }
    }

    private void sayLocal(String text, Runnable whenDone) {
        if (tts == null && !dead) {
            startLocal();
        }
        if (text == null || text.trim().isEmpty() || dead || tts == null) {
            if (whenDone != null) {
                main.post(whenDone);
            }
            return;
        }
        if (!ready) {
            pending = text;
            pendingDone = whenDone;
            pendingAt = SystemClock.elapsedRealtime();
            return;
        }
        done = whenDone;
        utterance++;
        String id = "v" + utterance;
        Log.i(TAG, "tts: \"" + text + "\"");
        if (tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id) != TextToSpeech.SUCCESS) {
            finished(id);
        }
    }

    private void finished(final String id) {
        main.post(new Runnable() {
            @Override
            public void run() {
                if (!id.equals("v" + utterance)) {
                    return;
                }
                Runnable d = done;
                done = null;
                if (d != null) {
                    d.run();
                }
            }
        });
    }

    private void runPendingDone() {
        Runnable d = pendingDone;
        pending = null;
        pendingDone = null;
        if (d != null) {
            d.run();
        }
    }

    boolean speaking() {
        return track != null || (ready && tts != null && tts.isSpeaking());
    }

    /** Stops speaking at once; a waiting done callback is dropped. */
    void stop() {
        pending = null;
        pendingDone = null;
        done = null;
        utterance++;
        cloudTurn++;
        stopTrack();
        if (ready && tts != null) {
            tts.stop();
        }
    }

    /**
     * Releases the engine. With letFinish, a line still being said (the reply as an app opens) is allowed to end
     * first, up to a few seconds; otherwise speech stops now.
     */
    void release(boolean letFinish) {
        if (!letFinish) {
            cloudTurn++;
            stopTrack();
        }
        cloud.shutdown(); // a line already playing (the reply as an app opens) finishes on its own
        final TextToSpeech t = tts;
        tts = null;
        dead = true;
        pending = null;
        pendingDone = null;
        done = null;
        if (t == null) {
            return;
        }
        if (letFinish && ready && t.isSpeaking()) {
            main.postDelayed(new Runnable() {
                int tries;

                @Override
                public void run() {
                    if (t.isSpeaking() && ++tries < 15) {
                        main.postDelayed(this, 200);
                    } else {
                        t.shutdown();
                    }
                }
            }, 200);
        } else {
            try {
                t.stop();
            } catch (Exception e) {
            }
            t.shutdown();
        }
    }
}
