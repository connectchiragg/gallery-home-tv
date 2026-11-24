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
 * Short spoken replies through Android's own text-to-speech (free, offline with the Google engine): "Playing lo-fi
 * beats on Spotify", "Here's what I found". Prefers an English (India) voice that needs no network.
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

    VoiceSpeaker(Context context) {
        final long t0 = SystemClock.elapsedRealtime();
        tts = new TextToSpeech(context.getApplicationContext(), new TextToSpeech.OnInitListener() {
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
                    boolean india = "IN".equals(l.getCountry());
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
        if (best != null && "IN".equals(best.getLocale().getCountry())) {
            tts.setVoice(best);
            return best.getName();
        }
        int r = tts.setLanguage(EN_IN);
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
        return ready && tts != null && tts.isSpeaking();
    }

    /** Stops speaking at once; a waiting done callback is dropped. */
    void stop() {
        pending = null;
        pendingDone = null;
        done = null;
        utterance++;
        if (ready && tts != null) {
            tts.stop();
        }
    }

    /**
     * Releases the engine. With letFinish, a line still being said (the reply as an app opens) is allowed to end
     * first, up to a few seconds; otherwise speech stops now.
     */
    void release(boolean letFinish) {
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
