package com.chirag.home;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioRecord;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/* JADX INFO: loaded from: classes.dex */
public class VoiceSearchActivity extends Activity {
    static final int APP = 26;
    static final int CAPSULE_BODY = -1945235436;
    static final long CAPTURE_CAP_MS = 35000;
    static final String CONFIG_FILE = "voice.json";
    static final int DIM = -855638017;
    static final String EXTRA_QUERY = "query";
    static final long FAIL_CLOSE_MS = 8000;
    static final float GLASS_BAND = 0.5f;
    static final int GLASS_BODY = -1475473388;
    static final int PANEL_SIDE = 48;
    static final int PANEL_TOP = 30;
    static final int PANEL_W = 348;
    static final int RATE = 16000;
    static final int RELISTEN = 2;
    static final int REQ_MIC = 71;
    static final long RESTART_GUARD_MS = 2000;
    static final String SORRY = "Sorry, I didn’t quite catch that";
    static final String TAG = "Voice";
    static final int TEXT = -1;
    static final int THUMB_W = 108;
    View capsule;
    volatile int captureGen;
    long captureStart;
    volatile boolean capturing;
    VoiceClient.Config cfg;
    VoiceClient client;
    boolean closing;
    volatile int gen;
    VoiceGlow glow;
    TextView heard;
    TextView hint;
    EditText input;
    LinearLayout list;
    VoiceGlow.Mark mark;
    boolean metering;
    int misses;
    TextView pHeard;
    TextView pStatus;
    View panel;
    boolean panelOn;
    VoiceAgent.Outcome pendingOutcome;
    Runnable pendingPlay;
    SpeechRecognizer recognizer;
    /** Spoken replies (voice.json "speak"); null when off. */
    VoiceSpeaker speaker;
    /** An app was opened from here: let the last reply finish instead of cutting it. */
    boolean handedOff;
    View rootView;
    View scrim;
    String soft;
    volatile boolean speechOn;
    boolean started;
    TextView status;
    long tStart;
    TextView tip;
    final Handler ui = new Handler(Looper.getMainLooper());
    final ExecutorService worker = Executors.newSingleThreadExecutor();
    final Runnable pauseCancel = new Runnable() { // from class: com.chirag.home.VoiceSearchActivity.1
        @Override // java.lang.Runnable
        public void run() {
            VoiceSearchActivity.this.cancel();
        }
    };
    String state = "";
    final Runnable autoClose = new Runnable() { // from class: com.chirag.home.VoiceSearchActivity.2
        @Override // java.lang.Runnable
        public void run() {
            VoiceSearchActivity.this.dismiss("failure message timed out");
        }
    };

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        setTheme(R.style.VoiceOverlay);
        super.onCreate(bundle);
        try {
            setTranslucent(true);
        } catch (Throwable th) {
        }
        getWindow().setBackgroundDrawable(new ColorDrawable(0));
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setClipChildren(false);
        View view = new View(this);
        view.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{335544320, 520093696, -1728053248}));
        frameLayout.addView(view, new FrameLayout.LayoutParams(TEXT, TEXT));
        this.scrim = new View(this);
        this.scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{855638016, 1711276032, -1291845632}));
        this.scrim.setAlpha(0.0f);
        frameLayout.addView(this.scrim, new FrameLayout.LayoutParams(TEXT, TEXT));
        this.glow = new VoiceGlow(this);
        frameLayout.addView(this.glow, new FrameLayout.LayoutParams(TEXT, TEXT));
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setGravity(16);
        skin(linearLayout, TEXT, CAPSULE_BODY);
        linearLayout.setPadding(dp(12), dp(9), dp(APP), dp(9));
        linearLayout.setMinimumHeight(dp(58));
        this.mark = new VoiceGlow.Mark(this);
        linearLayout.addView(this.mark, new LinearLayout.LayoutParams(dp(42), dp(42)));
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(1);
        linearLayout2.setPadding(dp(12), 0, 0, 0);
        this.status = Ui.text(this, "", 18, TEXT, Ui.MEDIUM);
        this.status.setMaxLines(RELISTEN);
        this.status.setMaxWidth(dp(500));
        linearLayout2.addView(this.status);
        this.heard = Ui.text(this, "", 13, DIM, Ui.REGULAR);
        this.heard.setSingleLine(true);
        this.heard.setEllipsize(TextUtils.TruncateAt.END);
        this.heard.setMaxWidth(dp(500));
        this.heard.setPadding(0, dp(3), 0, 0);
        this.heard.setVisibility(8);
        linearLayout2.addView(this.heard);
        this.tip = Ui.text(this, "", 12, DIM, Ui.REGULAR);
        this.tip.setPadding(0, dp(3), 0, 0);
        this.tip.setVisibility(8);
        linearLayout2.addView(this.tip);
        linearLayout.addView(linearLayout2);
        this.capsule = linearLayout;
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-2, -2, 81);
        layoutParams.bottomMargin = dp(44);
        frameLayout.addView(linearLayout, layoutParams);
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setOrientation(1);
        linearLayout3.setClipChildren(false);
        linearLayout3.setClipToPadding(false);
        skin(linearLayout3, PANEL_TOP, GLASS_BODY);
        linearLayout3.setPadding(dp(18), dp(20), dp(18), dp(16));
        this.pHeard = Ui.text(this, "", 13, DIM, Ui.REGULAR);
        this.pHeard.setSingleLine(true);
        this.pHeard.setEllipsize(TextUtils.TruncateAt.END);
        this.pHeard.setPadding(dp(6), 0, dp(6), 0);
        linearLayout3.addView(this.pHeard);
        this.pStatus = Ui.text(this, "", 15, TEXT, Ui.MEDIUM);
        this.pStatus.setMaxLines(RELISTEN);
        this.pStatus.setPadding(dp(6), dp(4), dp(6), 0);
        this.pStatus.setVisibility(8);
        linearLayout3.addView(this.pStatus);
        ScrollView scrollView = new ScrollView(this);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setClipChildren(true);
        scrollView.setClipToPadding(true);
        scrollView.setVerticalFadingEdgeEnabled(true);
        scrollView.setFadingEdgeLength(dp(24));
        this.list = new LinearLayout(this);
        this.list.setOrientation(1);
        this.list.setClipChildren(false);
        this.list.setClipToPadding(false);
        this.list.setPadding(0, dp(12), 0, dp(4));
        scrollView.addView(this.list);
        linearLayout3.addView(scrollView, new LinearLayout.LayoutParams(TEXT, 0, 1.0f));
        LinearLayout linearLayout4 = new LinearLayout(this);
        linearLayout4.setGravity(16);
        linearLayout4.setClipChildren(false);
        linearLayout4.setPadding(dp(6), dp(8), dp(6), 0);
        linearLayout4.addView(round(14, "Speak again", new Runnable() { // from class: com.chirag.home.VoiceSearchActivity.3
            @Override // java.lang.Runnable
            public void run() {
                VoiceSearchActivity.this.listen();
            }
        }));
        this.input = new EditText(this);
        this.input.setSingleLine(true);
        this.input.setTextColor(TEXT);
        this.input.setHintTextColor(-1627389953);
        this.input.setTextSize(RELISTEN, 13.0f);
        this.input.setTypeface(Ui.REGULAR);
        this.input.setInputType(524289);
        this.input.setImeOptions(3);
        this.input.setContentDescription("Type a name");
        final Smoke smoke = new Smoke(this, TEXT, Smoke.FIELD);
        this.input.setBackground(smoke);
        final Glyph glyph = new Glyph(15, dp(16), DIM);
        this.input.setCompoundDrawablesRelativeWithIntrinsicBounds(glyph, (Drawable) null, (Drawable) null, (Drawable) null);
        this.input.setCompoundDrawablePadding(dp(10));
        this.input.setPadding(dp(10), 0, dp(10), 0);
        final LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(dp(36), dp(36));
        layoutParams2.leftMargin = dp(10);
        this.input.setOnFocusChangeListener(new View.OnFocusChangeListener() { // from class: com.chirag.home.VoiceSearchActivity.4
            @Override // android.view.View.OnFocusChangeListener
            public void onFocusChange(View view2, boolean z) {
                boolean z2 = z || VoiceSearchActivity.this.input.length() > 0;
                smoke.mode(z ? 1 : 0);
                glyph.color(z ? VoiceSearchActivity.TEXT : VoiceSearchActivity.DIM);
                VoiceSearchActivity.this.input.setHint(z2 ? "Type a name" : "");
                layoutParams2.width = z2 ? 0 : VoiceSearchActivity.this.dp(36);
                layoutParams2.weight = z2 ? 1.0f : 0.0f;
                VoiceSearchActivity.this.input.setLayoutParams(layoutParams2);
            }
        });
        this.input.setOnEditorActionListener(new TextView.OnEditorActionListener() { // from class: com.chirag.home.VoiceSearchActivity.5
            @Override // android.widget.TextView.OnEditorActionListener
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (keyEvent != null && keyEvent.getAction() != 0) {
                    return true;
                }
                String strTrim = textView.getText().toString().trim();
                if (strTrim.isEmpty()) {
                    return false;
                }
                VoiceSearchActivity.this.cancel();
                ((InputMethodManager) VoiceSearchActivity.this.getSystemService("input_method")).hideSoftInputFromWindow(textView.getWindowToken(), 0);
                textView.clearFocus();
                VoiceSearchActivity.this.onText(strTrim, "typed");
                return true;
            }
        });
        linearLayout4.addView(this.input, layoutParams2);
        linearLayout3.addView(linearLayout4);
        this.hint = Ui.text(this, "", 10, -1627389953, Ui.REGULAR);
        this.hint.setPadding(dp(6), dp(8), dp(6), 0);
        this.hint.setVisibility(8);
        linearLayout3.addView(this.hint);
        this.panel = linearLayout3;
        this.panel.setVisibility(4);
        FrameLayout.LayoutParams layoutParams3 = new FrameLayout.LayoutParams(dp(PANEL_W), TEXT, 8388613);
        layoutParams3.setMargins(0, dp(PANEL_TOP), dp(PANEL_SIDE), dp(PANEL_TOP));
        frameLayout.addView(this.panel, layoutParams3);
        setContentView(frameLayout);
        this.rootView = frameLayout;
        frameLayout.setAlpha(0.0f);
        frameLayout.animate().alpha(1.0f).setDuration(160L).start();
    }

    void skin(View view, int i, int i2) {
        if (!MainActivity.showing) {
            view.setBackground(new Smoke(this, i, Smoke.SHEET));
        } else {
            Glass.on(view, new Glass(this, i).body(i2, GLASS_BAND));
        }
    }

    void panel(boolean z) {
        if (z == this.panelOn) {
            return;
        }
        this.panelOn = z;
        this.panel.animate().cancel();
        this.capsule.animate().cancel();
        if (z) {
            this.panel.setVisibility(0);
            this.panel.setTranslationX(dp(396));
            this.panel.animate().translationX(0.0f).setDuration(200L).setInterpolator(new DecelerateInterpolator(1.6f)).start();
            this.capsule.animate().alpha(0.0f).setDuration(120L).start();
            this.scrim.animate().alpha(1.0f).setDuration(200L).start();
        } else {
            this.panel.setVisibility(4);
            this.capsule.animate().alpha(1.0f).setDuration(120L).start();
            this.scrim.animate().alpha(0.0f).setDuration(160L).start();
        }
        tune();
    }

    void tune() {
        int i;
        int i2 = 1;
        boolean z = this.metering || waiting();
        VoiceGlow voiceGlow = this.glow;
        if (this.panelOn) {
            i = 0;
        } else if (this.metering) {
            i = 1;
        } else if (waiting()) {
            i = RELISTEN;
        } else {
            i = 0;
        }
        voiceGlow.mode(i);
        boolean z2 = this.state.equals("Speak now") || this.state.equals("Listening…");
        VoiceGlow.Mark mark = this.mark;
        if (this.panelOn) {
            i2 = 0;
        } else if (!z2) {
            if (z) {
                i2 = RELISTEN;
            } else {
                i2 = 0;
            }
        }
        mark.mode(i2);
        String str = (this.soft == null || !(z2 || this.state.equals("Getting the mic ready…"))) ? "" : "I’m still listening";
        this.tip.setText(str);
        this.tip.setVisibility(str.isEmpty() ? 8 : 0);
    }

    boolean waiting() {
        return this.state.endsWith("…");
    }

    boolean idle() {
        return (this.panelOn || this.metering || waiting() || this.state.length() <= 0) ? false : true;
    }

    void level(float f) {
        this.glow.level(f);
        this.mark.level(f);
    }

    void said(String str) {
        this.heard.setText(str);
        this.heard.setVisibility(str.isEmpty() ? 8 : 0);
        this.pHeard.setText(str);
        this.pHeard.setVisibility(str.isEmpty() ? 8 : 0);
    }

    void clearResults() {
        this.list.removeAllViews();
        panel(false);
    }

    void typeHere() {
        panel(true);
        this.input.requestFocus();
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        loadConfig();
        String stringExtra = getIntent().getStringExtra(EXTRA_QUERY);
        getIntent().removeExtra(EXTRA_QUERY);
        this.ui.removeCallbacks(this.pauseCancel);
        if (stringExtra != null && !stringExtra.trim().isEmpty()) {
            cancel();
            onText(stringExtra.trim(), "typed");
        } else if (!this.started && !justStarted()) {
            listen();
        }
        this.started = true;
    }

    boolean justStarted() {
        return this.capturing && (this.speechOn || SystemClock.elapsedRealtime() - this.captureStart < RESTART_GUARD_MS);
    }

    @Override // android.app.Activity
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        this.started = false;
        if (this.closing) {
            this.closing = false;
            this.rootView.animate().cancel();
            this.rootView.setAlpha(1.0f);
        }
    }

    @Override // android.app.Activity
    protected void onPause() {
        this.ui.postDelayed(this.pauseCancel, 400L);
        super.onPause();
    }

    @Override // android.app.Activity
    protected void onStop() {
        this.ui.removeCallbacks(this.pauseCancel);
        cancel();
        this.glow.stop();
        this.mark.stop();
        super.onStop();
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        this.worker.shutdownNow();
        VoiceArt.clear();
        VoiceChime.release();
        if (this.speaker != null) {
            this.speaker.release(this.handedOff);
            this.speaker = null;
        }
        if (this.recognizer != null) {
            this.recognizer.destroy();
            this.recognizer = null;
        }
        super.onDestroy();
    }

    @Override // android.app.Activity, android.view.KeyEvent.Callback
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (i == 4) {
            hush();
        }
        if (this.pendingPlay != null && i == 4) {
            dropPending();
            choices(this.pendingOutcome);
            return true;
        }
        if (this.pendingPlay != null && (i == 23 || i == 66)) {
            Runnable runnable = this.pendingPlay;
            dropPending();
            runnable.run();
            return true;
        }
        if (micKey(i)) {
            if (keyEvent.getRepeatCount() == 0 && !justStarted()) {
                listen();
            }
            return true;
        }
        if (!this.panelOn && keyEvent.getRepeatCount() == 0 && !idle() && !justStarted() && this.pendingPlay == null && (i == 21 || i == 22 || i == 19 || i == 20)) {
            cancel();
            show("", TEXT, false);
            typeHere();
            return true;
        }
        if (idle() && keyEvent.getRepeatCount() == 0) {
            if (i == 23 || i == 66) {
                listen();
                return true;
            }
            if (i == 21 || i == 22 || i == 19 || i == 20) {
                typeHere();
                return true;
            }
        }
        return super.onKeyDown(i, keyEvent);
    }

    @Override // android.app.Activity, android.view.KeyEvent.Callback
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        return micKey(i) || super.onKeyUp(i, keyEvent);
    }

    static boolean micKey(int i) {
        return i == 84 || i == 231 || i == 219;
    }

    void loadConfig() {
        File file;
        String string = null;
        File externalFilesDir = getExternalFilesDir(null);
        String absolutePath = CONFIG_FILE;
        if (externalFilesDir == null) {
            file = null;
        } else {
            file = new File(externalFilesDir, CONFIG_FILE);
        }
        this.cfg = new VoiceClient.Config();
        if (file == null || !file.exists()) {
            StringBuilder sbAppend = new StringBuilder().append("Missing ");
            if (file != null) {
                absolutePath = file.getAbsolutePath();
            }
            string = sbAppend.append(absolutePath).toString();
        } else {
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                try {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    byte[] bArr = new byte[4096];
                    while (true) {
                        int i = fileInputStream.read(bArr);
                        if (i <= 0) {
                            break;
                        } else {
                            byteArrayOutputStream.write(bArr, 0, i);
                        }
                    }
                    this.cfg = VoiceClient.Config.parse(byteArrayOutputStream.toString("UTF-8"));
                    fileInputStream.close();
                } catch (Throwable th) {
                    fileInputStream.close();
                    throw th;
                }
            } catch (Exception e) {
                string = e.getMessage();
            }
        }
        this.cfg.installed = new java.util.HashSet<>();
        for (VoiceCore.App app : VoiceCore.APPS) {
            if (app.pkg != null && installed(app.pkg)) {
                this.cfg.installed.add(app.pkg);
            }
        }
        if (this.cfg.speak && this.speaker == null) {
            this.speaker = new VoiceSpeaker(this, this.cfg.deepgramKey, this.cfg.ttsVoice);
        } else if (!this.cfg.speak && this.speaker != null) {
            this.speaker.release(false);
            this.speaker = null;
        }
        this.cfg.file = file;
        this.cfg.dataDir = externalFilesDir;
        // The owner's Spotify taste loads in the background, so a music request finds it ready.
        SpotifyTaste.logger = new SpotifyTaste.Logger() {
            @Override
            public void log(String s) {
                Log.i(TAG, "taste " + s);
            }
        };
        SpotifyTaste.warm(this.cfg, VoiceClient.REAL);
        this.client = new VoiceClient(this.cfg, VoiceClient.REAL);
        this.hint.setText(string != null ? string + "  (copy voice.example.json there and add your keys)" : "Mic button: speak again · Back: close");
        this.hint.setVisibility(string == null ? 8 : 0);
    }

    void show(String str, int i, boolean z) {
        this.ui.removeCallbacks(this.autoClose);
        this.state = str;
        boolean z2 = true;
        boolean z3 = this.soft != null && (str.equals("Getting the mic ready…") || str.equals("Speak now") || str.equals("Listening…"));
        this.status.setText(z3 ? this.soft : str);
        TextView textView = this.status;
        if (z3) {
            i = TEXT;
        }
        textView.setTextColor(i);
        if (!str.equals("Results") && !str.isEmpty()) {
            z2 = false;
        }
        this.pStatus.setText(str);
        this.pStatus.setVisibility(z2 ? 8 : 0);
        this.metering = z;
        if (!z) {
            level(0.0f);
        }
        tune();
    }

    void error(String str) {
        Log.w(TAG, "error: " + str);
        show(str, TEXT, false);
        this.ui.postDelayed(this.autoClose, FAIL_CLOSE_MS);
    }

    void fail(String str, String str2) {
        Log.w(TAG, "error: " + str2);
        show(str, TEXT, false);
        this.ui.postDelayed(this.autoClose, FAIL_CLOSE_MS);
    }

    void dismiss(String str) {
        if (this.closing) {
            return;
        }
        this.closing = true;
        Log.i(TAG, "closing: " + str);
        hush();
        cancel();
        this.ui.removeCallbacks(this.autoClose);
        this.glow.mode(0);
        this.mark.stop();
        this.rootView.animate().alpha(0.0f).setDuration(220L).withEndAction(new Runnable() { // from class: com.chirag.home.VoiceSearchActivity.6
            @Override // java.lang.Runnable
            public void run() {
                VoiceSearchActivity.this.finish();
            }
        }).start();
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        this.ui.removeCallbacks(this.autoClose);
        return super.dispatchKeyEvent(keyEvent);
    }

    void missed(String str) {
        if (this.misses < RELISTEN) {
            this.misses++;
            Log.i(TAG, "missed (" + str + "): listening again " + this.misses + "/" + RELISTEN);
            this.soft = SORRY;
            show("Getting the mic ready…", DIM, false);
            final int i = this.gen;
            final Runnable again = new Runnable() {
                boolean ran;

                @Override
                public void run() {
                    if (!ran && VoiceSearchActivity.this.gen == i) {
                        ran = true;
                        VoiceSearchActivity.this.begin();
                    }
                }
            };
            if (this.speaker != null) {
                // Listen again only after the apology is said, so the mic does not hear it (or the user over it).
                this.speaker.say("Sorry, I didn't catch that", again);
                this.ui.postDelayed(again, 3500L);
            } else {
                VoiceChime.play(RELISTEN);
                this.ui.postDelayed(again, 280L);
            }
            return;
        }
        dismiss("missed (" + str + ") after " + RELISTEN + " re-listens");
    }

    void cancel() {
        this.gen++;
        dropPending();
        this.capturing = false;
        if (this.recognizer != null) {
            this.recognizer.cancel();
        }
    }

    void listen() {
        this.misses = 0;
        this.soft = null;
        begin();
    }

    /** Stops a spoken reply (Back, a new listen, closing). */
    void hush() {
        if (this.speaker != null) {
            this.speaker.stop();
        }
    }

    void say(String text) {
        if (this.speaker != null) {
            this.speaker.say(text, null);
        }
    }

    void begin() {
        hush();
        cancel();
        clearResults();
        said("");
        boolean zEquals = "system".equals(this.cfg.capture);
        if (!zEquals && !this.cfg.hasStt()) {
            error("Add a speech key to voice.json (groq_key), or type a name");
            typeHere();
        } else {
            if (checkSelfPermission("android.permission.RECORD_AUDIO") != 0) {
                show("Allow the microphone to use voice search", TEXT, false);
                requestPermissions(new String[]{"android.permission.RECORD_AUDIO"}, REQ_MIC);
                return;
            }
            this.tStart = SystemClock.elapsedRealtime();
            if (zEquals) {
                listenSystem();
            } else {
                listenRaw();
            }
        }
    }

    @Override // android.app.Activity
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        if (i != REQ_MIC) {
            return;
        }
        if (iArr.length <= 0 || iArr[0] != 0) {
            error("Microphone not allowed. You can still type a name");
            typeHere();
        } else {
            listen();
        }
    }

    void listenRaw() {
        final int i = this.gen;
        show("Getting the mic ready…", DIM, false);
        this.capturing = true;
        this.speechOn = false;
        this.captureGen = i;
        this.captureStart = SystemClock.elapsedRealtime();
        new Thread(new Runnable() { // from class: com.chirag.home.VoiceSearchActivity.8
            @Override // java.lang.Runnable
            public void run() {
                int minBufferSize = AudioRecord.getMinBufferSize(VoiceSearchActivity.RATE, 16, VoiceSearchActivity.RELISTEN);
                final VoiceCore.Endpointer endpointer = new VoiceCore.Endpointer(VoiceSearchActivity.RATE);
                // The ready chime plays the moment live audio arrives; keep it out of speech detection.
                endpointer.guardMs = VoiceChime.ENABLED ? 260 : 40;
                long jElapsedRealtime = SystemClock.elapsedRealtime();
                long j = -1;
                long j2 = -1;
                long j3 = -1;
                int iFeed = 0;
                String failure = null;
                AudioRecord audioRecord = null;
                try {
                    audioRecord = new AudioRecord(6, VoiceSearchActivity.RATE, 16, VoiceSearchActivity.RELISTEN, Math.max(minBufferSize, VoiceSearchActivity.RATE));
                    if (audioRecord.getState() != 1) {
                        throw new IllegalStateException("no audio input available");
                    }
                    audioRecord.startRecording();
                    if (audioRecord.getRecordingState() != 3) {
                        throw new IllegalStateException("microphone is busy");
                    }
                    short[] sArr = new short[320];
                    while (VoiceSearchActivity.this.gen == i && iFeed == 0) {
                        int i4 = audioRecord.read(sArr, 0, 320, 1);
                        if (i4 < 0) {
                            throw new IllegalStateException("audio read error " + i4);
                        }
                        long jElapsedRealtime2 = SystemClock.elapsedRealtime();
                        if (i4 != 0) {
                            iFeed = endpointer.feed(sArr, i4);
                            VoiceSearchActivity.this.speechOn = endpointer.heardSpeech();
                            if (j < 0 && endpointer.phase() != 0) {
                                // The mic is delivering real audio now (the first non-silent buffer), and from here
                                // every sample is kept. Only now ask for speech: "Speak now" and the ready chime.
                                // Asking earlier made people talk while a Bluetooth remote's mic was still waking up.
                                j = jElapsedRealtime2;
                                j2 = jElapsedRealtime2;
                                VoiceChime.play(0);
                                VoiceSearchActivity.this.ui.post(new Runnable() { // from class: com.chirag.home.VoiceSearchActivity.8.1
                                    @Override // java.lang.Runnable
                                    public void run() {
                                        if (VoiceSearchActivity.this.gen == i) {
                                            VoiceSearchActivity.this.show("Speak now", VoiceSearchActivity.TEXT, true);
                                        }
                                    }
                                });
                            }
                            if (j3 < 0 && endpointer.phase() == VoiceSearchActivity.RELISTEN) {
                                j3 = jElapsedRealtime2;
                            }
                            final float f = (float) endpointer.level;
                            VoiceSearchActivity.this.ui.post(new Runnable() { // from class: com.chirag.home.VoiceSearchActivity.8.2
                                @Override // java.lang.Runnable
                                public void run() {
                                    if (VoiceSearchActivity.this.gen == i) {
                                        VoiceSearchActivity.this.level(f);
                                    }
                                }
                            });
                        } else if (endpointer.phase() == 0 && jElapsedRealtime2 - jElapsedRealtime >= endpointer.liveTimeoutMs) {
                            iFeed = 3;
                            break;
                        } else {
                            if (jElapsedRealtime2 - jElapsedRealtime >= VoiceSearchActivity.CAPTURE_CAP_MS) {
                                iFeed = endpointer.heardSpeech() ? 1 : VoiceSearchActivity.RELISTEN;
                                break;
                            }
                            Thread.sleep(10L);
                        }
                    }
                } catch (Exception e) {
                    failure = e.getMessage() == null ? e.toString() : e.getMessage();
                } finally {
                    if (audioRecord != null) {
                        try {
                            audioRecord.stop();
                        } catch (Exception e2) {
                        }
                        audioRecord.release();
                    }
                    if (VoiceSearchActivity.this.captureGen == i) {
                        VoiceSearchActivity.this.capturing = false;
                    }
                }
                final String str = failure;
                final int i2 = iFeed;
                String strReason;
                if (VoiceSearchActivity.this.gen != i) {
                    strReason = "cancelled";
                } else if (str != null) {
                    strReason = "failure";
                } else if (i2 == 3) {
                    strReason = "no live audio";
                } else {
                    strReason = endpointer.reason();
                }
                Log.i(VoiceSearchActivity.TAG, "capture end=" + strReason + " openToLive=" + (j < 0 ? -1L : j - jElapsedRealtime) + "ms openToSpeakNow=" + (j2 < 0 ? -1L : j2 - jElapsedRealtime) + "ms openToSettled=" + (j3 < 0 ? -1L : j3 - jElapsedRealtime) + "ms speakNowToSpeech=" + endpointer.firstSpeechMs() + "ms speech=" + endpointer.utteranceMs() + "ms (loud " + endpointer.speechMs() + "ms) total=" + endpointer.millis() + "ms peak=" + endpointer.peak() + " floor=" + endpointer.noiseFloor() + " enter=" + endpointer.enterRms() + " endDelay=" + endpointer.endDelayMs() + "ms" + (str != null ? " failure=" + str : ""));
                if (VoiceSearchActivity.this.gen != i) {
                    return;
                }
                VoiceSearchActivity.this.ui.post(new Runnable() { // from class: com.chirag.home.VoiceSearchActivity.8.3
                    @Override // java.lang.Runnable
                    public void run() {
                        if (VoiceSearchActivity.this.gen != i) {
                            return;
                        }
                        boolean zEquals = "auto".equals(VoiceSearchActivity.this.cfg.capture);
                        if (str != null || i2 == 3) {
                            if (!zEquals || !SpeechRecognizer.isRecognitionAvailable(VoiceSearchActivity.this)) {
                                if (str != null) {
                                    VoiceSearchActivity.this.fail("I can’t reach the microphone right now. You can type a name instead", "Microphone problem: " + str);
                                } else {
                                    VoiceSearchActivity.this.error("The remote’s microphone didn’t start. Press the mic again, or type a name");
                                }
                                VoiceSearchActivity.this.typeHere();
                                return;
                            }
                            VoiceSearchActivity.this.listenSystem();
                            return;
                        }
                        if (i2 == VoiceSearchActivity.RELISTEN || endpointer.speechMs() < endpointer.minSpeechMs) {
                            if (i2 != VoiceSearchActivity.RELISTEN || !endpointer.reason().equals("no speech")) {
                                VoiceSearchActivity.this.missed(endpointer.reason());
                                return;
                            } else {
                                VoiceSearchActivity.this.dismiss("silence");
                                return;
                            }
                        }
                        VoiceSearchActivity.this.transcribe(endpointer.wav(), i);
                    }
                });
            }
        }, "voice-rec").start();
    }

    void transcribe(final byte[] bArr, final int i) {
        show("Transcribing…", TEXT, false);
        final long jElapsedRealtime = SystemClock.elapsedRealtime();
        this.worker.execute(new Runnable() { // from class: com.chirag.home.VoiceSearchActivity.9
            @Override // java.lang.Runnable
            public void run() {
                try {
                    final VoiceClient.Transcript transcriptTranscribe = VoiceSearchActivity.this.client.transcribe(bArr);
                    final boolean zIsSilenceText = VoiceCore.isSilenceText(transcriptTranscribe.text, VoiceSearchActivity.this.cfg.sttPrompt);
                    Log.i(VoiceSearchActivity.TAG, "stt " + transcriptTranscribe.provider + " " + (SystemClock.elapsedRealtime() - jElapsedRealtime) + "ms wav=" + bArr.length + "B text=" + transcriptTranscribe.text + (zIsSilenceText ? " (ignored: silence text)" : ""));
                    VoiceSearchActivity.this.ui.post(new Runnable() { // from class: com.chirag.home.VoiceSearchActivity.9.1
                        @Override // java.lang.Runnable
                        public void run() {
                            if (VoiceSearchActivity.this.gen != i) {
                                return;
                            }
                            if (zIsSilenceText) {
                                VoiceSearchActivity.this.missed("silence text");
                            } else {
                                VoiceSearchActivity.this.onText(transcriptTranscribe.text, transcriptTranscribe.provider);
                            }
                        }
                    });
                } catch (VoiceClient.VoiceException e) {
                    VoiceSearchActivity.this.ui.post(new Runnable() { // from class: com.chirag.home.VoiceSearchActivity.9.2
                        @Override // java.lang.Runnable
                        public void run() {
                            if (VoiceSearchActivity.this.gen == i) {
                                VoiceSearchActivity.this.fail("I couldn’t reach the speech service just now. You can type a name instead", e.getMessage());
                                VoiceSearchActivity.this.typeHere();
                            }
                        }
                    });
                }
            }
        });
    }

    void listenSystem() {
        final int i = this.gen;
        show("Getting the mic ready…", DIM, false);
        if (this.recognizer == null) {
            this.recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        }
        this.recognizer.setRecognitionListener(new RecognitionListener() { // from class: com.chirag.home.VoiceSearchActivity.10
            @Override // android.speech.RecognitionListener
            public void onReadyForSpeech(Bundle bundle) {
                if (VoiceSearchActivity.this.gen == i) {
                    VoiceChime.play(0);
                    VoiceSearchActivity.this.show("Speak now", VoiceSearchActivity.TEXT, true);
                }
            }

            @Override // android.speech.RecognitionListener
            public void onBeginningOfSpeech() {
            }

            @Override // android.speech.RecognitionListener
            public void onRmsChanged(float f) {
                if (VoiceSearchActivity.this.gen == i) {
                    VoiceSearchActivity.this.level(Math.max(0.0f, Math.min(1.0f, (f + 2.0f) / 12.0f)));
                }
            }

            @Override // android.speech.RecognitionListener
            public void onBufferReceived(byte[] bArr) {
            }

            @Override // android.speech.RecognitionListener
            public void onEndOfSpeech() {
                if (VoiceSearchActivity.this.gen == i) {
                    VoiceSearchActivity.this.show("Transcribing…", VoiceSearchActivity.TEXT, false);
                }
            }

            @Override // android.speech.RecognitionListener
            public void onError(int i2) {
                if (VoiceSearchActivity.this.gen != i) {
                    return;
                }
                Log.w(VoiceSearchActivity.TAG, "system recognizer error " + i2);
                if (i2 == 6) {
                    VoiceSearchActivity.this.dismiss("silence (system recogniser)");
                } else if (i2 == 7) {
                    VoiceSearchActivity.this.missed("system: no match");
                } else {
                    VoiceSearchActivity.this.error(i2 == 9 ? "Microphone not allowed" : "System voice recognition failed (code " + i2 + "). Type a name instead");
                }
            }

            @Override // android.speech.RecognitionListener
            public void onResults(Bundle bundle) {
                if (VoiceSearchActivity.this.gen != i) {
                    return;
                }
                ArrayList<String> stringArrayList = bundle.getStringArrayList("results_recognition");
                if (stringArrayList == null || stringArrayList.isEmpty()) {
                    VoiceSearchActivity.this.missed("system: empty result");
                } else {
                    VoiceSearchActivity.this.onText(stringArrayList.get(0), "system");
                }
            }

            @Override // android.speech.RecognitionListener
            public void onPartialResults(Bundle bundle) {
                ArrayList<String> stringArrayList = bundle.getStringArrayList("results_recognition");
                if (VoiceSearchActivity.this.gen != i || stringArrayList == null || stringArrayList.isEmpty()) {
                    return;
                }
                VoiceSearchActivity.this.said(stringArrayList.get(0));
            }

            @Override // android.speech.RecognitionListener
            public void onEvent(int i2, Bundle bundle) {
            }
        });
        try {
            this.recognizer.startListening(new Intent("android.speech.action.RECOGNIZE_SPEECH").putExtra("android.speech.extra.LANGUAGE_MODEL", "free_form").putExtra("android.speech.extra.LANGUAGE", "hi".equals(this.cfg.language) ? "hi-IN" : "en-IN").putExtra("android.speech.extra.PARTIAL_RESULTS", true).putExtra("calling_package", getPackageName()));
        } catch (Exception e) {
            error("System voice recognition is unavailable. Type a name instead");
        }
    }

    void onText(final String str, String str2) {
        final int i = this.gen + 1;
        this.gen = i;
        said("“" + str + "”");
        clearResults();
        final VoiceCore.Query queryUnderstand = VoiceCore.understand(str);
        Log.i(TAG, "text(" + str2 + ")=" + str + " -> " + queryUnderstand);
        final boolean zEquals = str2.equals("typed");
        final List<VoiceAgent.Play> listPlays = HistoryDb.get(this).plays(20);
        show("Searching…", TEXT, false);
        final long jElapsedRealtime = SystemClock.elapsedRealtime();
        this.worker.execute(new Runnable() { // from class: com.chirag.home.VoiceSearchActivity.11
            @Override // java.lang.Runnable
            public void run() {
                try {
                    final VoiceAgent.Outcome outcomeRun = new VoiceAgent(VoiceSearchActivity.this.cfg, VoiceClient.REAL).run(str, listPlays, System.currentTimeMillis());
                    Log.i(VoiceSearchActivity.TAG, "agent " + (SystemClock.elapsedRealtime() - jElapsedRealtime) + "ms " + outcomeRun.decision + " route=" + outcomeRun.route + " conf=" + outcomeRun.breakdown());
                    for (String str3 : outcomeRun.trace.split("\n")) {
                        Log.i(VoiceSearchActivity.TAG, "agent " + str3);
                    }
                    VoiceSearchActivity.this.ui.post(new Runnable() { // from class: com.chirag.home.VoiceSearchActivity.11.2
                        @Override // java.lang.Runnable
                        public void run() {
                            if (VoiceSearchActivity.this.gen == i) {
                                VoiceSearchActivity.this.outcome(outcomeRun, zEquals);
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.w(VoiceSearchActivity.TAG, "agent failed: " + e);
                    VoiceSearchActivity.this.ui.post(new Runnable() { // from class: com.chirag.home.VoiceSearchActivity.11.1
                        @Override // java.lang.Runnable
                        public void run() {
                            if (VoiceSearchActivity.this.gen == i) {
                                VoiceSearchActivity.this.fail("I couldn’t look that up just now", String.valueOf(e.getMessage()));
                                VoiceSearchActivity.this.say("Sorry, I couldn't look that up just now");
                                VoiceSearchActivity.this.addFallbackRow(queryUnderstand);
                            }
                        }
                    });
                }
            }
        });
    }

    void outcome(VoiceAgent.Outcome outcome, boolean z) {
        if (outcome.decision.equals("none") && outcome.candidates.isEmpty() && outcome.titles == null && ((outcome.query == null || outcome.query.intent.equals("empty")) && !z)) {
            missed("nothing usable in the text");
            return;
        }
        this.misses = 0;
        this.soft = null;
        if (outcome.decision.equals("play") && !outcome.candidates.isEmpty()) {
            final VoiceAgent.Candidate candidate = outcome.candidates.get(0);
            this.pendingOutcome = outcome;
            this.pendingPlay = new Runnable() { // from class: com.chirag.home.VoiceSearchActivity.12
                @Override // java.lang.Runnable
                public void run() {
                    VoiceSearchActivity.this.pendingPlay = null;
                    VoiceSearchActivity.this.open(candidate.links, candidate.title);
                }
            };
            show(outcome.line.isEmpty() ? candidate.line() : outcome.line, TEXT, false);
            VoiceChime.play(1);
            // Said alongside the 2 s countdown, never in front of it: the countdown starts now regardless.
            this.ui.postDelayed(this.pendingPlay, RESTART_GUARD_MS);
            say(VoiceAgent.spoken(outcome));
            return;
        }
        choices(outcome);
        say(VoiceAgent.spoken(outcome));
    }

    void dropPending() {
        if (this.pendingPlay != null) {
            this.ui.removeCallbacks(this.pendingPlay);
            this.pendingPlay = null;
        }
    }

    void choices(VoiceAgent.Outcome outcome) {
        if (outcome.titles != null && !outcome.titles.titles.isEmpty() && (outcome.route.equals("play_title") || outcome.candidates.isEmpty())) {
            results(outcome.titles);
            return;
        }
        ArrayList<VoiceAgent.Candidate> arrayList = new ArrayList(outcome.candidates);
        if (outcome.search != null && arrayList.size() < 3) {
            arrayList.add(outcome.search);
        }
        if (arrayList.isEmpty()) {
            error(outcome.message.isEmpty() ? "I didn’t find that" : outcome.message);
            if (outcome.query != null) {
                addFallbackRow(outcome.query);
                return;
            }
            return;
        }
        show(outcome.message.isEmpty() ? "Results" : outcome.message, TEXT, false);
        View view = null;
        for (final VoiceAgent.Candidate candidate : arrayList) {
            if (!candidate.links.isEmpty()) {
                ArrayList arrayList2 = new ArrayList();
                ArrayList arrayList3 = new ArrayList();
                ArrayList arrayList4 = new ArrayList();
                arrayList2.add(candidate.links.get(0).pkg);
                arrayList3.add(candidate.where);
                arrayList4.add(new Runnable() { // from class: com.chirag.home.VoiceSearchActivity.13
                    @Override // java.lang.Runnable
                    public void run() {
                        VoiceSearchActivity.this.open(candidate.links, candidate.title);
                    }
                });
                String strArtUrl = candidate.title == null ? "" : candidate.title.artUrl();
                View viewCard = card(strArtUrl, !strArtUrl.isEmpty(), candidate.label, candidate.detail, arrayList2, arrayList3, arrayList4);
                this.list.addView(viewCard);
                if (view == null) {
                    view = viewCard;
                }
            }
        }
        panel(true);
        VoiceChime.play(1);
        if (view != null) {
            view.requestFocus();
        }
    }

    void results(VoiceClient.Result result) {
        String string;
        final VoiceCore.Query query = result.query;
        if (result.titles.isEmpty()) {
            error("Nothing found for “" + query.text + "”");
            addFallbackRow(query);
            return;
        }
        show("Results", TEXT, false);
        View view = null;
        for (final VoiceCore.Title title : result.titles) {
            ArrayList arrayList = new ArrayList();
            ArrayList arrayList2 = new ArrayList();
            ArrayList arrayList3 = new ArrayList();
            ArrayList arrayList4 = new ArrayList();
            ArrayList arrayList5 = new ArrayList();
            Iterator<VoiceCore.Offer> it = title.apps(query.app).iterator();
            while (it.hasNext()) {
                arrayList5.add(it.next().app);
            }
            if (query.app != null && !arrayList5.contains(query.app) && VoiceCore.app(query.app).pkg != null) {
                arrayList5.add(query.app);
            }
            Iterator it2 = arrayList5.iterator();
            while (true) {
                string = "";
                if (!it2.hasNext()) {
                    break;
                }
                final String str2 = (String) it2.next();
                VoiceCore.App app = VoiceCore.app(str2);
                if (installed(app.pkg)) {
                    VoiceCore.Offer offer = title.offers.get(str2);
                    StringBuilder sbAppend2 = new StringBuilder().append(app.label);
                    if (offer == null) {
                        string = " (search)";
                    } else if (!offer.included()) {
                        string = " (" + offer.kind + ")";
                    } else if (offer.channel) {
                        string = " (channel)";
                    }
                    String string2 = sbAppend2.append(string).toString();
                    arrayList.add(string2);
                    arrayList2.add(app.pkg);
                    arrayList3.add(string2);
                    arrayList4.add(new Runnable() { // from class: com.chirag.home.VoiceSearchActivity.14
                        @Override // java.lang.Runnable
                        public void run() {
                            VoiceSearchActivity.this.open(VoiceCore.episodeLinks(title, str2, query.play, VoiceSearchActivity.this.cfg.searchLinks), title);
                        }
                    });
                }
            }
            final String str3 = title.name + (title.year.isEmpty() ? "" : " " + title.year);
            arrayList2.add(VoiceCore.app("youtube").pkg);
            arrayList3.add("YouTube search");
            arrayList4.add(new Runnable() { // from class: com.chirag.home.VoiceSearchActivity.15
                @Override // java.lang.Runnable
                public void run() {
                    VoiceSearchActivity.this.open(VoiceCore.searchLinks("youtube", str3, VoiceSearchActivity.this.cfg.searchLinks));
                }
            });
            List<String> listElsewhere = title.elsewhere();
            if (arrayList.isEmpty()) {
                string = "Not on your apps" + (listElsewhere.isEmpty() ? "" : " · on " + join(listElsewhere));
            } else if (!listElsewhere.isEmpty()) {
                string = "also on " + join(listElsewhere);
            }
            String strTargetLine = title.targetLine();
            String str;
            if (strTargetLine.isEmpty()) {
                str = string;
            } else {
                if (!string.isEmpty()) {
                    strTargetLine = strTargetLine + " · " + string;
                }
                str = strTargetLine;
            }
            View viewCard = card(title.artUrl(), true, title.line(query.app), str, arrayList2, arrayList3, arrayList4);
            this.list.addView(viewCard);
            if (view == null) {
                view = viewCard;
            }
        }
        panel(true);
        VoiceChime.play(1);
        if (view != null) {
            view.requestFocus();
        }
    }

    void addFallbackRow(final VoiceCore.Query query) {
        if (query.text.isEmpty()) {
            return;
        }
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        ArrayList arrayList3 = new ArrayList();
        for (final String str : "youtube".equals(query.app) ? new String[]{"youtube"} : new String[]{query.app, "youtube"}) {
            if (str != null) {
                VoiceCore.App app = VoiceCore.app(str);
                if (app.pkg != null && installed(app.pkg)) {
                    arrayList.add(app.pkg);
                    arrayList2.add(app.label);
                    arrayList3.add(new Runnable() { // from class: com.chirag.home.VoiceSearchActivity.16
                        @Override // java.lang.Runnable
                        public void run() {
                            VoiceSearchActivity.this.open(VoiceCore.searchLinks(str, query.text, VoiceSearchActivity.this.cfg.searchLinks));
                        }
                    });
                }
            }
        }
        panel(true);
        if (arrayList.isEmpty()) {
            this.input.requestFocus();
            return;
        }
        View viewCard = card("", false, "Search for “" + query.text + "”", "", arrayList, arrayList2, arrayList3);
        this.list.addView(viewCard);
        viewCard.requestFocus();
    }

    boolean installed(String str) {
        PackageManager packageManager = getPackageManager();
        return (packageManager.getLeanbackLaunchIntentForPackage(str) == null && packageManager.getLaunchIntentForPackage(str) == null) ? false : true;
    }

    void open(List<VoiceCore.Link> list) {
        open(list, null);
    }

    void open(List<VoiceCore.Link> list, VoiceCore.Title title) {
        Intent leanbackLaunchIntentForPackage;
        for (VoiceCore.Link link : list) {
            if (link.action.equals("LAUNCH")) {
                leanbackLaunchIntentForPackage = getPackageManager().getLeanbackLaunchIntentForPackage(link.pkg);
                if (leanbackLaunchIntentForPackage == null) {
                    leanbackLaunchIntentForPackage = getPackageManager().getLaunchIntentForPackage(link.pkg);
                }
                if (leanbackLaunchIntentForPackage == null) {
                    continue;
                }
            } else if (link.action.equals("SEARCH")) {
                leanbackLaunchIntentForPackage = new Intent("android.intent.action.SEARCH").setPackage(link.pkg);
            } else {
                leanbackLaunchIntentForPackage = new Intent("android.intent.action.VIEW", Uri.parse(link.uri)).setPackage(link.pkg);
            }
            for (Map.Entry<String, String> entry : link.extras.entrySet()) {
                leanbackLaunchIntentForPackage.putExtra(entry.getKey(), entry.getValue());
            }
            leanbackLaunchIntentForPackage.addFlags(268435456);
            try {
                startActivity(leanbackLaunchIntentForPackage);
                Log.i(TAG, "opened " + link);
                if (link.uri != null && link.uri.startsWith("spotify:track:")) {
                    // The launcher's own plays must not count as the owner's taste.
                    final java.io.File dir = this.cfg == null ? null : this.cfg.dataDir;
                    final String id = link.uri.substring("spotify:track:".length());
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            SpotifyTaste.played(dir, id);
                        }
                    }, "spotify-played").start();
                }
                this.handedOff = true;
                ArtStore.launched(this, link, title);
                finish();
                return;
            } catch (Exception e) {
                Log.w(TAG, "could not open " + link + ": " + e);
            }
        }
        error("Couldn't open that app");
    }

    int dp(int i) {
        return Math.round(i * getResources().getDisplayMetrics().density);
    }

    View card(String str, boolean z, String str2, String str3, List<String> list, final List<String> list2, final List<Runnable> list3) {
        final LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setGravity(16);
        linearLayout.setPadding(dp(8), dp(8), dp(12), dp(8));
        Smoke smoke = new Smoke(this, 20, Smoke.PLAIN);
        linearLayout.setBackground(smoke);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(TEXT, -2);
        layoutParams.bottomMargin = dp(10);
        linearLayout.setLayoutParams(layoutParams);
        int i = 1;
        int i2 = 0;
        if (z) {
            FrameLayout frameLayout = new FrameLayout(this);
            frameLayout.setBackground(Ui.shape(this, -15000027, 12, 0));
            frameLayout.setClipToOutline(true);
            frameLayout.setForeground(new Edge(dp(12), dp(1), 0));
            ImageView imageView = new ImageView(this);
            imageView.setImageDrawable(new Glyph(RELISTEN, dp(18), 1509949439));
            imageView.setScaleType(ImageView.ScaleType.CENTER);
            frameLayout.addView(imageView, new FrameLayout.LayoutParams(TEXT, TEXT));
            final ImageView imageView2 = new ImageView(this);
            imageView2.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView2.setAlpha(0.0f);
            frameLayout.addView(imageView2, new FrameLayout.LayoutParams(TEXT, TEXT));
            linearLayout.addView(frameLayout, new LinearLayout.LayoutParams(dp(THUMB_W), (dp(THUMB_W) * 9) / 16));
            VoiceArt.load(str, dp(THUMB_W), this.ui, new VoiceArt.Got() { // from class: com.chirag.home.VoiceSearchActivity.17
                @Override // com.chirag.home.VoiceArt.Got
                public void got(Bitmap bitmap) {
                    imageView2.setImageBitmap(bitmap);
                    imageView2.animate().alpha(1.0f).setDuration(150L).start();
                }
            });
        }
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(1);
        linearLayout2.setClipChildren(false);
        linearLayout2.setPadding(dp(z ? 14 : 6), 0, 0, 0);
        final TextView textViewText = Ui.text(this, str2, 14, TEXT, Ui.MEDIUM);
        textViewText.setMaxLines(RELISTEN);
        textViewText.setEllipsize(TextUtils.TruncateAt.END);
        linearLayout2.addView(textViewText);
        TextView textViewText2 = Ui.text(this, str3, 11, DIM, Ui.REGULAR);
        textViewText2.setSingleLine(true);
        textViewText2.setEllipsize(TextUtils.TruncateAt.END);
        textViewText2.setPadding(0, dp(RELISTEN), 0, 0);
        if (!str3.isEmpty()) {
            linearLayout2.addView(textViewText2);
        }
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setGravity(16);
        linearLayout3.setClipChildren(false);
        linearLayout3.setPadding(0, dp(8), 0, 0);
        final int size = list.size();
        final FrameLayout[] frameLayoutArr = new FrameLayout[size];
        final GradientDrawable[] gradientDrawableArr = new GradientDrawable[size];
        int i3 = 0;
        while (i3 < size) {
            FrameLayout frameLayout2 = new FrameLayout(this);
            GradientDrawable gradientDrawable = new GradientDrawable();
            gradientDrawable.setShape(i);
            gradientDrawable.setColor(i2);
            frameLayout2.setBackground(gradientDrawable);
            frameLayout2.setContentDescription(list2.get(i3));
            frameLayout2.addView(new Orb(this, list.get(i3), dp(APP), false), new FrameLayout.LayoutParams(dp(APP), dp(APP), 17));
            LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(dp(32), dp(32));
            layoutParams2.rightMargin = dp(6);
            linearLayout3.addView(frameLayout2, layoutParams2);
            frameLayoutArr[i3] = frameLayout2;
            gradientDrawableArr[i3] = gradientDrawable;
            i3++;
            textViewText2 = textViewText2;
            smoke = smoke;
            i = 1;
            i2 = 0;
        }
        final Smoke smoke2 = smoke;
        final TextView textView = textViewText2;
        linearLayout2.addView(linearLayout3);
        linearLayout.addView(linearLayout2, new LinearLayout.LayoutParams(0, -2, 1.0f));
        final int[] iArr = {0};
        final Runnable runnable = new Runnable() { // from class: com.chirag.home.VoiceSearchActivity.18
            @Override // java.lang.Runnable
            public void run() {
                boolean zIsFocused = linearLayout.isFocused();
                int i4 = 0;
                while (true) {
                    int i5 = -14869217;
                    if (i4 >= size) {
                        break;
                    }
                    boolean z2 = i4 == iArr[0];
                    GradientDrawable gradientDrawable2 = gradientDrawableArr[i4];
                    int iMax = Math.max(VoiceSearchActivity.RELISTEN, VoiceSearchActivity.this.dp(3) / VoiceSearchActivity.RELISTEN);
                    if (!z2) {
                        i5 = 0;
                    } else if (!zIsFocused) {
                        i5 = -419430401;
                    }
                    gradientDrawable2.setStroke(iMax, i5);
                    frameLayoutArr[i4].setAlpha(z2 ? 1.0f : 0.45f);
                    float f = 1.12f;
                    ViewPropertyAnimator viewPropertyAnimatorScaleX = frameLayoutArr[i4].animate().scaleX(z2 ? 1.12f : 0.9f);
                    if (!z2) {
                        f = 0.9f;
                    }
                    viewPropertyAnimatorScaleX.scaleY(f).setDuration(120L).start();
                    i4++;
                }
                if (size > 0) {
                    linearLayout.setContentDescription((CharSequence) list2.get(iArr[0]));
                }
                textViewText.setTextColor(zIsFocused ? -14869217 : VoiceSearchActivity.TEXT);
                Ui.shade(textViewText, !zIsFocused);
                textView.setTextColor(zIsFocused ? -1289937633 : VoiceSearchActivity.DIM);
                Ui.shade(textView, !zIsFocused);
            }
        };
        Ui.focusable(this, linearLayout, null, 0, 1.04f, new Runnable() { // from class: com.chirag.home.VoiceSearchActivity.19
            @Override // java.lang.Runnable
            public void run() {
                if (size > 0) {
                    ((Runnable) list3.get(iArr[0])).run();
                }
            }
        }, null, new Ui.OnFocus() { // from class: com.chirag.home.VoiceSearchActivity.20
            @Override // com.chirag.home.Ui.OnFocus
            public void changed(boolean z2) {
                smoke2.mode(z2 ? VoiceSearchActivity.RELISTEN : 0);
                linearLayout.invalidateOutline();
                linearLayout.setElevation(z2 ? VoiceSearchActivity.this.dp(12) : 0.0f);
                runnable.run();
            }
        });
        linearLayout.setOnKeyListener(new View.OnKeyListener() { // from class: com.chirag.home.VoiceSearchActivity.21
            @Override // android.view.View.OnKeyListener
            public boolean onKey(View view, int i4, KeyEvent keyEvent) {
                if (i4 != 21 && i4 != 22) {
                    return false;
                }
                if (keyEvent.getAction() == 0 && size > 0) {
                    iArr[0] = Math.max(0, Math.min(size - 1, iArr[0] + (i4 == 22 ? 1 : VoiceSearchActivity.TEXT)));
                    runnable.run();
                }
                return true;
            }
        });
        runnable.run();
        return linearLayout;
    }

    View round(int i, String str, Runnable runnable) {
        final ImageView imageView = new ImageView(this);
        final Glyph glyph = new Glyph(i, dp(16), TEXT);
        imageView.setImageDrawable(glyph);
        imageView.setScaleType(ImageView.ScaleType.CENTER);
        imageView.setContentDescription(str);
        final Smoke smoke = new Smoke(this, TEXT, Smoke.FIELD);
        imageView.setBackground(smoke);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(dp(36), dp(36)));
        Ui.focusable(this, imageView, null, 0, 1.1f, runnable, null, new Ui.OnFocus() { // from class: com.chirag.home.VoiceSearchActivity.22
            @Override // com.chirag.home.Ui.OnFocus
            public void changed(boolean z) {
                smoke.mode(z ? VoiceSearchActivity.RELISTEN : 0);
                imageView.invalidateOutline();
                glyph.color(z ? -14869217 : VoiceSearchActivity.TEXT);
                imageView.setElevation(z ? VoiceSearchActivity.this.dp(10) : 0.0f);
            }
        });
        return imageView;
    }

    static String join(List<String> list) {
        return VoiceCore.join(list, ", ");
    }
}
