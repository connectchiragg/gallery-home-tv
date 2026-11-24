package com.chirag.home;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
final class NowPlaying {
    static final int ANIM_MS = 280;
    static final float CLOCK_SCALE = 0.83f;
    static final float DIM_ALPHA = 0.4f;
    static final int DIM_BODY = -15329509;
    static final int DIM_MS = 1000;
    static final long EQ_AWAKE_MS = 30000;
    static final int GAP = 4;
    static final long GRACE_MS = 2000;
    static final int H = 38;
    static final int LIFT = 28;
    static final String PKG = "com.spotify.tv.android";
    static volatile boolean cleared;
    final Activity a;
    private Bitmap art;
    private int artToken;
    private TextView caption;
    private TextView clock;
    private boolean dimmed;
    private long duration;
    private long eqUntil;
    private int extra;
    private FrameLayout head;
    private int headH0;
    private Pill home;
    private MediaController mc;
    private MediaSessionManager msm;
    Runnable onShowingChanged;
    private PlaybackState ps;
    private LinearLayout root;
    private boolean shown;
    private boolean started;
    private boolean userPaused;
    final Handler h = new Handler(Looper.getMainLooper());
    private String title = "";
    private String artist = "";
    final List<Pill> pills = new ArrayList();
    private final MediaSessionManager.OnActiveSessionsChangedListener sessions = new MediaSessionManager.OnActiveSessionsChangedListener() { // from class: com.chirag.home.NowPlaying$$ExternalSyntheticLambda2
        @Override // android.media.session.MediaSessionManager.OnActiveSessionsChangedListener
        public final void onActiveSessionsChanged(List list) {
            m43lambda$new$0$comchiraghomeNowPlaying(list);
        }
    };
    private final MediaController.Callback cb = new MediaController.Callback() { // from class: com.chirag.home.NowPlaying.1
        @Override // android.media.session.MediaController.Callback
        public void onPlaybackStateChanged(PlaybackState playbackState) {
            NowPlaying.this.ps = playbackState;
            if (NowPlaying.isPlaying(playbackState)) {
                NowPlaying.this.userPaused = false;
                NowPlaying.cleared = false;
            }
            NowPlaying.this.refreshPlayback();
            NowPlaying.this.evaluate();
        }

        @Override // android.media.session.MediaController.Callback
        public void onMetadataChanged(MediaMetadata mediaMetadata) {
            NowPlaying.this.readMeta(mediaMetadata);
        }

        @Override // android.media.session.MediaController.Callback
        public void onSessionDestroyed() {
            NowPlaying.this.bind(null);
        }
    };
    private final Runnable hide = new Runnable() { // from class: com.chirag.home.NowPlaying$$ExternalSyntheticLambda3
        @Override // java.lang.Runnable
        public final void run() {
            m44lambda$new$1$comchiraghomeNowPlaying();
        }
    };
    private int capDown = -1;
    private final List<View> below = new ArrayList();
    private int captionTop0 = Integer.MIN_VALUE;

    NowPlaying(Activity activity) {
        this.a = activity;
    }

    boolean isShowing() {
        return this.shown;
    }

    View view() {
        return this.home;
    }

    boolean focusControls() {
        if (this.home == null || !this.shown || this.home.getVisibility() != 0) {
            return false;
        }
        boolean zRequestFocus = this.home.requestFocus();
        this.home.cursor = 2;
        this.home.mark();
        return zRequestFocus;
    }

    void setDimmed(boolean z) {
        if (z == this.dimmed) {
            return;
        }
        this.dimmed = z;
        if (this.home != null) {
            this.home.dim(z, true);
        }
    }

    private final ComponentName listener() {
        return new ComponentName(this.a, (Class<?>) WatchListener.class);
    }

    /* JADX INFO: renamed from: lambda$new$0$com-chirag-home-NowPlaying, reason: not valid java name */
    /* synthetic */ void m43lambda$new$0$comchiraghomeNowPlaying(List list) {
        bind(pick(list));
    }

    void start() {
        if (this.started) {
            return;
        }
        this.started = true;
        try {
            this.msm = (MediaSessionManager) this.a.getSystemService(MediaSessionManager.class);
            this.msm.addOnActiveSessionsChangedListener(this.sessions, listener(), this.h);
            bind(pick(this.msm.getActiveSessions(listener())));
        } catch (Exception e) {
            this.msm = null;
            bind(null);
        }
        this.h.removeCallbacks(this.hide);
        this.shown = want();
        Log.i("NowPlaying", "start: session=" + (this.mc != null) + " state=" + (this.ps == null ? "none" : Integer.valueOf(this.ps.getState())) + " cleared=" + cleared + " shown=" + this.shown);
    }

    void stop() {
        if (this.started) {
            this.started = false;
            try {
                if (this.msm != null) {
                    this.msm.removeOnActiveSessionsChangedListener(this.sessions);
                }
            } catch (Exception e) {
            }
            if (this.mc != null) {
                try {
                    this.mc.unregisterCallback(this.cb);
                } catch (Exception e2) {
                }
            }
            this.mc = null;
            this.h.removeCallbacksAndMessages(null);
            this.artToken++;
            Iterator<Pill> it = this.pills.iterator();
            while (it.hasNext()) {
                it.next().halt();
            }
        }
    }

    private static MediaController pick(List<MediaController> list) {
        if (list != null) {
            for (MediaController mediaController : list) {
                if (PKG.equals(mediaController.getPackageName())) {
                    return mediaController;
                }
            }
            return null;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void bind(MediaController mediaController) {
        if (mediaController == null || this.mc == null || !mediaController.getSessionToken().equals(this.mc.getSessionToken())) {
            if (this.mc != null) {
                try {
                    this.mc.unregisterCallback(this.cb);
                } catch (Exception e) {
                }
            }
            this.mc = mediaController;
            if (mediaController != null) {
                try {
                    mediaController.registerCallback(this.cb, this.h);
                } catch (Exception e2) {
                }
                this.ps = mediaController.getPlaybackState();
                readMeta(mediaController.getMetadata());
            } else {
                this.ps = null;
            }
            refreshPlayback();
            evaluate();
        }
    }

    static boolean isPlaying(PlaybackState playbackState) {
        if (playbackState == null) {
            return false;
        }
        switch (playbackState.getState()) {
            case 3:
            case GAP /* 4 */:
            case 5:
            case 6:
            case 8:
            case 9:
            case 10:
            case 11:
                return true;
            case 7:
            default:
                return false;
        }
    }

    private boolean anyFocused() {
        Iterator<Pill> it = this.pills.iterator();
        while (it.hasNext()) {
            if (it.next().hasFocus()) {
                return true;
            }
        }
        return false;
    }

    static boolean loaded(PlaybackState playbackState) {
        return playbackState != null && (isPlaying(playbackState) || playbackState.getState() == 2);
    }

    private boolean want() {
        return (this.mc == null || cleared || !loaded(this.ps)) ? false : true;
    }

    /* JADX INFO: renamed from: lambda$new$1$com-chirag-home-NowPlaying, reason: not valid java name */
    /* synthetic */ void m44lambda$new$1$comchiraghomeNowPlaying() {
        if (want()) {
            return;
        }
        setShown(false);
    }

    void evaluate() {
        if (this.started) {
            if (want()) {
                this.h.removeCallbacks(this.hide);
                if (!this.shown) {
                    setShown(true);
                    return;
                }
                return;
            }
            if (this.shown) {
                this.h.removeCallbacks(this.hide);
                this.h.postDelayed(this.hide, GRACE_MS);
            }
        }
    }

    private void setShown(boolean z) {
        if (z == this.shown) {
            return;
        }
        this.shown = z;
        if (z) {
            this.eqUntil = SystemClock.uptimeMillis() + EQ_AWAKE_MS;
        }
        showHome(z, true);
        if (this.onShowingChanged != null) {
            this.onShowingChanged.run();
        }
    }

    private static String first(MediaMetadata mediaMetadata, String... strArr) {
        for (String str : strArr) {
            CharSequence text = mediaMetadata.getText(str);
            if (text != null && text.length() > 0) {
                return text.toString();
            }
        }
        return "";
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void readMeta(MediaMetadata mediaMetadata) {
        Bitmap bitmap;
        final String strFirst;
        Bitmap bitmapSquare;
        String strFirst2 = mediaMetadata == null ? "" : first(mediaMetadata, "android.media.metadata.TITLE", "android.media.metadata.DISPLAY_TITLE");
        String strFirst3 = mediaMetadata != null ? first(mediaMetadata, "android.media.metadata.ARTIST", "android.media.metadata.ALBUM_ARTIST", "android.media.metadata.DISPLAY_SUBTITLE", "android.media.metadata.ALBUM") : "";
        this.duration = mediaMetadata != null ? Math.max(0L, mediaMetadata.getLong("android.media.metadata.DURATION")) : 0L;
        boolean z = (strFirst2.equals(this.title) && strFirst3.equals(this.artist)) ? false : true;
        this.title = strFirst2;
        this.artist = strFirst3;
        if (mediaMetadata == null) {
            bitmap = null;
            strFirst = null;
        } else {
            String[] strArr = {"android.media.metadata.ALBUM_ART", "android.media.metadata.ART", "android.media.metadata.DISPLAY_ICON"};
            bitmap = null;
            for (int i = 0; i < 3; i++) {
                try {
                    bitmap = mediaMetadata.getBitmap(strArr[i]);
                } catch (Exception e) {
                }
                if (bitmap != null) {
                    break;
                }
            }
            strFirst = bitmap == null ? first(mediaMetadata, "android.media.metadata.ALBUM_ART_URI", "android.media.metadata.ART_URI", "android.media.metadata.DISPLAY_ICON_URI") : null;
        }
        final int i2 = this.artToken + 1;
        this.artToken = i2;
        if (bitmap == null) {
            bitmapSquare = null;
        } else {
            bitmapSquare = square(bitmap, Ui.dp(this.a, 48));
        }
        if (bitmap != null || strFirst == null || strFirst.isEmpty()) {
            setArt(bitmapSquare, z);
        } else {
            if (z) {
                setArt(null, true);
            }
            final Context applicationContext = this.a.getApplicationContext();
            final int iDp = Ui.dp(this.a, 48);
            new Thread(new Runnable() { // from class: com.chirag.home.NowPlaying$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    m46lambda$readMeta$3$comchiraghomeNowPlaying(applicationContext, strFirst, iDp, i2);
                }
            }, "np-art").start();
        }
        if (z) {
            this.eqUntil = SystemClock.uptimeMillis() + EQ_AWAKE_MS;
            for (Pill pill : this.pills) {
                pill.setText(true);
                pill.eq.invalidate();
            }
        }
        refreshPlayback();
    }

    /* JADX INFO: renamed from: lambda$readMeta$3$com-chirag-home-NowPlaying, reason: not valid java name */
    /* synthetic */ void m46lambda$readMeta$3$comchiraghomeNowPlaying(Context context, String str, int i, final int i2) {
        final Bitmap bitmapLoad = load(context, str, i);
        if (bitmapLoad != null) {
            this.h.post(new Runnable() { // from class: com.chirag.home.NowPlaying$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    m45lambda$readMeta$2$comchiraghomeNowPlaying(i2, bitmapLoad);
                }
            });
        }
    }

    /* JADX INFO: renamed from: lambda$readMeta$2$com-chirag-home-NowPlaying, reason: not valid java name */
    /* synthetic */ void m45lambda$readMeta$2$comchiraghomeNowPlaying(int i, Bitmap bitmap) {
        if (i == this.artToken) {
            setArt(bitmap, false);
        }
    }

    private void setArt(Bitmap bitmap, boolean z) {
        if (z || bitmap == null || this.art == null || !bitmap.sameAs(this.art)) {
            if (!z && bitmap == null && this.art == null) {
                return;
            }
            this.art = bitmap;
            Iterator<Pill> it = this.pills.iterator();
            while (it.hasNext()) {
                it.next().cover.set(bitmap, true);
            }
        }
    }

    static Bitmap square(Bitmap bitmap, int i) {
        try {
            int iMin = Math.min(bitmap.getWidth(), bitmap.getHeight());
            if (iMin <= 0) {
                return null;
            }
            return Bitmap.createScaledBitmap(Bitmap.createBitmap(bitmap, (bitmap.getWidth() - iMin) / 2, (bitmap.getHeight() - iMin) / 2, iMin, iMin), i, i, true);
        } catch (Exception e) {
            return null;
        }
    }

    private static Bitmap load(Context context, String str, int i) {
        InputStream inputStream = null;
        try {
            if (str.startsWith("http")) {
                HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setReadTimeout(5000);
                inputStream = httpURLConnection.getInputStream();
            } else {
                inputStream = context.getContentResolver().openInputStream(Uri.parse(str));
            }
            Bitmap bitmapDecodeStream = BitmapFactory.decodeStream(inputStream);
            return bitmapDecodeStream == null ? null : square(bitmapDecodeStream, i);
        } catch (Exception e) {
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e2) {
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void refreshPlayback() {
        Iterator<Pill> it = this.pills.iterator();
        while (it.hasNext()) {
            it.next().playback();
        }
    }

    long position() {
        if (this.ps == null || this.ps.getPosition() < 0) {
            return -1L;
        }
        long position = this.ps.getPosition();
        if (this.ps.getState() == 3) {
            position += (long) ((SystemClock.elapsedRealtime() - this.ps.getLastPositionUpdateTime()) * this.ps.getPlaybackSpeed());
        }
        return this.duration > 0 ? Math.max(0L, Math.min(this.duration, position)) : position;
    }

    void act(int i) {
        if (i == 0) {
            Ui.launch(this.a, this.a.getPackageManager().getLeanbackLaunchIntentForPackage(PKG));
            return;
        }
        if (this.mc == null) {
            return;
        }
        MediaController.TransportControls transportControls = this.mc.getTransportControls();
        if (i == GAP) {
            cleared = true;
            this.userPaused = false;
            try {
                if (isPlaying(this.ps)) {
                    transportControls.pause();
                }
            } catch (Exception e) {
            }
            this.h.removeCallbacks(this.hide);
            setShown(false);
            return;
        }
        try {
            if (i != 1) {
                if (i != 3) {
                    if (!isPlaying(this.ps)) {
                        this.userPaused = false;
                        transportControls.play();
                        Iterator<Pill> it = this.pills.iterator();
                        while (it.hasNext()) {
                            it.next().playing(true);
                        }
                        return;
                    }
                    this.userPaused = true;
                    transportControls.pause();
                    Iterator<Pill> it2 = this.pills.iterator();
                    while (it2.hasNext()) {
                        it2.next().playing(false);
                    }
                    return;
                }
                transportControls.skipToNext();
                return;
            }
            transportControls.skipToPrevious();
        } catch (Exception e2) {
        }
    }

    void attach(LinearLayout linearLayout, TextView textView, TextView textView2) {
        if (this.home != null) {
            this.home.halt();
            this.pills.remove(this.home);
        }
        this.root = linearLayout;
        this.caption = textView;
        this.clock = textView2;
        this.head = (FrameLayout) textView.getParent();
        this.headH0 = this.head.getLayoutParams().height;
        this.capDown = textView.getNextFocusDownId();
        this.below.clear();
        collect(linearLayout, textView.getId());
        this.captionTop0 = Integer.MIN_VALUE;
        this.home = new Pill();
        textView.measure(0, 0);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-2, Ui.dp(this.a, H), 1);
        layoutParams.topMargin = ((((FrameLayout.LayoutParams) textView.getLayoutParams()).topMargin + textView.getMeasuredHeight()) - Ui.dp(this.a, LIFT)) + Ui.dp(this.a, GAP);
        this.extra = Math.max(0, ((layoutParams.topMargin + Ui.dp(this.a, H)) + Ui.dp(this.a, 2)) - this.headH0);
        this.head.addView(this.home, layoutParams);
        this.home.setVisibility(8);
        if (this.dimmed) {
            this.home.dim(true, false);
        }
        textView2.addOnLayoutChangeListener(new View.OnLayoutChangeListener() { // from class: com.chirag.home.NowPlaying$$ExternalSyntheticLambda5
            @Override // android.view.View.OnLayoutChangeListener
            public final void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                NowPlaying.lambda$attach$4(view, i, i2, i3, i4, i5, i6, i7, i8);
            }
        });
        if (this.shown) {
            showHome(true, false);
        }
    }

    static /* synthetic */ void lambda$attach$4(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        view.setPivotX((i3 - i) / 2.0f);
        view.setPivotY(0.0f);
    }

    private void collect(View view, int i) {
        if (view != this.caption && view.getNextFocusUpId() == i) {
            this.below.add(view);
        }
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i2 = 0; i2 < viewGroup.getChildCount(); i2++) {
                collect(viewGroup.getChildAt(i2), i);
            }
        }
    }

    View homePill() {
        if (this.home == null || !this.shown) {
            return null;
        }
        return this.home;
    }

    private void showHome(boolean z, boolean z2) {
        if (this.home == null || this.caption == null) {
            return;
        }
        if (!this.root.isAttachedToWindow()) {
            z2 = false;
        }
        final Pill pill = this.home;
        pill.animate().cancel();
        float f = z ? CLOCK_SCALE : 1.0f;
        float f2 = z ? -Ui.dp(this.a, LIFT) : 0.0f;
        if (z) {
            pill.setVisibility(0);
            pill.setNextFocusUpId(this.caption.getId());
            this.caption.setNextFocusDownId(pill.getId());
            Iterator<View> it = this.below.iterator();
            while (it.hasNext()) {
                it.next().setNextFocusUpId(pill.getId());
            }
            resizeHead(this.headH0 + this.extra, z2);
            pill.setAlpha(0.0f);
            pill.setTranslationY(Ui.dp(this.a, 6));
            pill.animate().alpha(this.dimmed ? DIM_ALPHA : 1.0f).translationY(0.0f).setStartDelay(z2 ? 120L : 60L).setDuration(280L).setInterpolator(new DecelerateInterpolator()).withEndAction(null).start();
        } else {
            this.caption.setNextFocusDownId(this.capDown);
            Iterator<View> it2 = this.below.iterator();
            while (it2.hasNext()) {
                it2.next().setNextFocusUpId(this.caption.getId());
            }
            if (pill.hasFocus()) {
                this.caption.requestFocus();
            }
            pill.animate().alpha(0.0f).setStartDelay(0L).setDuration(180L).withEndAction(new Runnable() { // from class: com.chirag.home.NowPlaying$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    m47lambda$showHome$5$comchiraghomeNowPlaying(pill);
                }
            }).start();
        }
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) this.caption.getLayoutParams();
        if (this.captionTop0 == Integer.MIN_VALUE) {
            this.captionTop0 = layoutParams.topMargin;
        }
        int iRound = this.captionTop0 + Math.round(f2);
        if (layoutParams.topMargin != iRound) {
            layoutParams.topMargin = iRound;
            this.caption.setLayoutParams(layoutParams);
        }
        this.caption.setTranslationY(0.0f);
        if (!z2) {
            this.clock.setScaleX(f);
            this.clock.setScaleY(f);
        } else {
            this.clock.animate().scaleX(f).scaleY(f).setDuration(340L).setInterpolator(new DecelerateInterpolator()).start();
        }
    }

    /* JADX INFO: renamed from: lambda$showHome$5$com-chirag-home-NowPlaying, reason: not valid java name */
    /* synthetic */ void m47lambda$showHome$5$comchiraghomeNowPlaying(Pill pill) {
        pill.setVisibility(8);
        resizeHead(this.headH0, true);
    }

    private void resizeHead(int i, boolean z) {
        ViewGroup.LayoutParams layoutParams = this.head.getLayoutParams();
        if (layoutParams.height == i) {
            return;
        }
        final int iIndexOfChild = this.root.indexOfChild(this.head);
        final int childCount = this.root.getChildCount();
        final int[] iArr = new int[childCount];
        for (int i2 = 0; i2 < childCount; i2++) {
            iArr[i2] = this.root.getChildAt(i2).getTop();
        }
        layoutParams.height = i;
        this.head.setLayoutParams(layoutParams);
        if (!z || !this.root.isLaidOut()) {
            return;
        }
        this.root.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() { // from class: com.chirag.home.NowPlaying.2

            @Override // android.view.ViewTreeObserver.OnPreDrawListener
            public boolean onPreDraw() {
                NowPlaying.this.root.getViewTreeObserver().removeOnPreDrawListener(this);
                for (int i3 = iIndexOfChild + 1; i3 < childCount && i3 < NowPlaying.this.root.getChildCount(); i3++) {
                    View childAt = NowPlaying.this.root.getChildAt(i3);
                    int top = iArr[i3] - childAt.getTop();
                    if (top != 0) {
                        childAt.setTranslationY(top);
                        childAt.animate().translationY(0.0f).setDuration(280L).setInterpolator(new DecelerateInterpolator()).start();
                    }
                }
                return true;
            }
        });
    }

    static int mixColor(int i, int i2, float f) {
        int iRound = 0;
        for (int i3 = 0; i3 <= 24; i3 += 8) {
            int i4 = (i >>> i3) & 255;
            iRound |= (Math.round(i4 + ((((i2 >>> i3) & 255) - i4) * f)) & 255) << i3;
        }
        return iRound;
    }

    final class Pill extends LinearLayout {
        static final int CLEAR = 4;
        static final int INFO = 0;
        static final int NEXT = 3;
        static final int PLAY = 2;
        static final int PREV = 1;
        final ImageView[] btn;
        final Cover cover;
        int cursor;
        private boolean dim;
        private ValueAnimator dimAnim;
        final Eq eq;
        private boolean focused;
        final Glyph[] gl;
        private Glass glass;
        final View info;
        final Line line;
        private boolean lit;
        final View sep;
        final TextView t1;
        final TextView t2;
        final View[] zones;

        Pill() {
            super(NowPlaying.this.a);
            this.btn = new ImageView[CLEAR];
            this.gl = new Glyph[CLEAR];
            this.cursor = PLAY;
            setId(View.generateViewId());
            setGravity(16);
            setPadding(Ui.dp(NowPlaying.this.a, CLEAR), INFO, Ui.dp(NowPlaying.this.a, CLEAR), INFO);
            setClipChildren(false);
            setClipToPadding(false);
            setContentDescription("Now playing");
            LinearLayout linearLayout = new LinearLayout(NowPlaying.this.a);
            linearLayout.setGravity(16);
            linearLayout.setPadding(INFO, INFO, Ui.dp(NowPlaying.this.a, 10), INFO);
            this.info = linearLayout;
            this.cover = new Cover(Ui.dp(NowPlaying.this.a, 30));
            linearLayout.addView(this.cover, new LinearLayout.LayoutParams(Ui.dp(NowPlaying.this.a, 30), Ui.dp(NowPlaying.this.a, 30)));
            LinearLayout linearLayout2 = new LinearLayout(NowPlaying.this.a);
            linearLayout2.setOrientation(PREV);
            this.t1 = Ui.text(NowPlaying.this.a, "", 12, -1, Ui.MEDIUM);
            this.t2 = Ui.text(NowPlaying.this.a, "", 10, -855638017, Ui.REGULAR);
            TextView[] textViewArr = {this.t1, this.t2};
            for (int i = INFO; i < PLAY; i += PREV) {
                TextView textView = textViewArr[i];
                textView.setSingleLine(true);
                textView.setEllipsize(TextUtils.TruncateAt.END);
                textView.setMaxWidth(Ui.dp(NowPlaying.this.a, 130));
            }
            this.t2.setPadding(INFO, Ui.dp(NowPlaying.this.a, PREV), INFO, INFO);
            linearLayout2.addView(this.t1);
            linearLayout2.addView(this.t2);
            this.line = new Line();
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, Ui.dp(NowPlaying.this.a, PLAY));
            layoutParams.topMargin = Ui.dp(NowPlaying.this.a, CLEAR);
            linearLayout2.addView(this.line, layoutParams);
            LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(Ui.dp(NowPlaying.this.a, 130), -2);
            layoutParams2.leftMargin = Ui.dp(NowPlaying.this.a, 9);
            linearLayout.addView(linearLayout2, layoutParams2);
            this.eq = new Eq();
            LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(Ui.dp(NowPlaying.this.a, 12), Ui.dp(NowPlaying.this.a, 12));
            layoutParams3.leftMargin = Ui.dp(NowPlaying.this.a, 10);
            linearLayout.addView(this.eq, layoutParams3);
            addView(linearLayout, new LinearLayout.LayoutParams(-2, -1));
            this.sep = new View(NowPlaying.this.a);
            LinearLayout.LayoutParams layoutParams4 = new LinearLayout.LayoutParams(Math.max(PREV, Ui.dp(NowPlaying.this.a, PREV) / PLAY), Ui.dp(NowPlaying.this.a, 16));
            layoutParams4.setMargins(Ui.dp(NowPlaying.this.a, PLAY), INFO, Ui.dp(NowPlaying.this.a, CLEAR), INFO);
            addView(this.sep, layoutParams4);
            int[] iArr = {20, 18, 19, 6};
            int i2 = INFO;
            while (i2 < CLEAR) {
                ImageView imageView = new ImageView(NowPlaying.this.a);
                this.gl[i2] = new Glyph(iArr[i2], Ui.dp(NowPlaying.this.a, i2 == PREV ? 14 : i2 == NEXT ? 10 : 12), -1);
                imageView.setImageDrawable(this.gl[i2]);
                imageView.setScaleType(ImageView.ScaleType.CENTER);
                LinearLayout.LayoutParams layoutParams5 = new LinearLayout.LayoutParams(Ui.dp(NowPlaying.this.a, 30), Ui.dp(NowPlaying.this.a, 30));
                layoutParams5.leftMargin = Ui.dp(NowPlaying.this.a, PREV);
                addView(imageView, layoutParams5);
                this.btn[i2] = imageView;
                i2 += PREV;
            }
            this.zones = new View[]{this.info, this.btn[INFO], this.btn[PREV], this.btn[PLAY], this.btn[NEXT]};
            this.btn[INFO].setContentDescription("Previous");
            this.btn[PLAY].setContentDescription("Next");
            this.btn[NEXT].setContentDescription("Clear");
            this.glass = Ui.glass(this, -1);
            Ui.focusable(NowPlaying.this.a, this, null, INFO, 1.06f, new Runnable() { // from class: com.chirag.home.NowPlaying$Pill$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    m49lambda$new$0$comchiraghomeNowPlaying$Pill();
                }
            }, null, new Ui.OnFocus() { // from class: com.chirag.home.NowPlaying$Pill$$ExternalSyntheticLambda1
                @Override // com.chirag.home.Ui.OnFocus
                public final void changed(boolean z) {
                    m50lambda$new$1$comchiraghomeNowPlaying$Pill(z);
                }
            });
            setOnKeyListener(new View.OnKeyListener() { // from class: com.chirag.home.NowPlaying$Pill$$ExternalSyntheticLambda2
                @Override // android.view.View.OnKeyListener
                public final boolean onKey(View view, int i3, KeyEvent keyEvent) {
                    return m51lambda$new$2$comchiraghomeNowPlaying$Pill(view, i3, keyEvent);
                }
            });
            lit(false);
            setText(false);
            this.cover.set(NowPlaying.this.art, false);
            playback();
            NowPlaying.this.pills.add(this);
        }

        /* JADX INFO: renamed from: lambda$new$0$com-chirag-home-NowPlaying$Pill, reason: not valid java name */
        /* synthetic */ void m49lambda$new$0$comchiraghomeNowPlaying$Pill() {
            NowPlaying.this.act(this.cursor);
        }

        /* JADX INFO: renamed from: lambda$new$1$com-chirag-home-NowPlaying$Pill, reason: not valid java name */
        /* synthetic */ void m50lambda$new$1$comchiraghomeNowPlaying$Pill(boolean z) {
            boolean z2 = false;
            this.glass.mode((!z || this.dim) ? INFO : PLAY);
            if (z && !this.dim) {
                z2 = true;
            }
            Ui.liftGlass(this, z2);
            lit(z);
            focus(z);
        }

        /* JADX INFO: renamed from: lambda$new$2$com-chirag-home-NowPlaying$Pill, reason: not valid java name */
        /* synthetic */ boolean m51lambda$new$2$comchiraghomeNowPlaying$Pill(View view, int i, KeyEvent keyEvent) {
            boolean z = keyEvent.getAction() == 0;
            if (i != 21 && i != 22) {
                return this.dim && (i == 19 || i == 20);
            }
            if (z) {
                move(i == 22 ? PREV : -1);
            }
            return true;
        }

        private void focus(boolean z) {
            this.focused = z;
            if (z) {
                this.cursor = PLAY;
                NowPlaying.this.eqUntil = Math.max(NowPlaying.this.eqUntil, SystemClock.uptimeMillis() + NowPlaying.EQ_AWAKE_MS);
            } else if (NowPlaying.this.userPaused) {
                NowPlaying.this.userPaused = false;
                Handler handler = NowPlaying.this.h;
                final NowPlaying nowPlaying = NowPlaying.this;
                handler.post(new Runnable() { // from class: com.chirag.home.NowPlaying$Pill$$ExternalSyntheticLambda5
                    @Override // java.lang.Runnable
                    public final void run() {
                        nowPlaying.evaluate();
                    }
                });
            }
            mark();
            this.eq.invalidate();
        }

        private void move(int i) {
            int iMax = Math.max(INFO, Math.min(CLEAR, this.cursor + i));
            if (iMax == this.cursor) {
                return;
            }
            this.cursor = iMax;
            mark();
        }

        void mark() {
            int i = this.lit ? 522001695 : 704643071;
            for (int i2 = INFO; i2 < this.zones.length; i2 += PREV) {
                if (!this.focused || i2 != this.cursor) {
                    this.zones[i2].setBackground(null);
                } else {
                    GradientDrawable gradientDrawable = new GradientDrawable();
                    gradientDrawable.setColor(i);
                    if (i2 == 0) {
                        gradientDrawable.setCornerRadius(Ui.dp(NowPlaying.this.a, 15));
                        this.zones[i2].setBackground(new InsetDrawable((Drawable) gradientDrawable, INFO, Ui.dp(NowPlaying.this.a, CLEAR), INFO, Ui.dp(NowPlaying.this.a, CLEAR)));
                    } else {
                        gradientDrawable.setShape(PREV);
                        this.zones[i2].setBackground(gradientDrawable);
                    }
                }
            }
        }

        void dim(boolean z, boolean z2) {
            this.dim = z;
            if (this.dimAnim != null) {
                this.dimAnim.cancel();
            }
            boolean zHasFocus = hasFocus();
            this.glass.mode((!zHasFocus || z) ? INFO : PLAY);
            Ui.liftGlass(this, zHasFocus && !z);
            lit(zHasFocus);
            ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(z ? INFO : 1.0f, z ? 1.0f : 0.0f);
            this.dimAnim = valueAnimatorOfFloat;
            valueAnimatorOfFloat.setDuration(z2 ? 1000L : 0L);
            valueAnimatorOfFloat.setInterpolator(new AccelerateDecelerateInterpolator());
            valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.chirag.home.NowPlaying$Pill$$ExternalSyntheticLambda4
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                    m48lambda$dim$3$comchiraghomeNowPlaying$Pill(valueAnimator);
                }
            });
            valueAnimatorOfFloat.start();
            this.eq.invalidate();
        }

        /* JADX INFO: renamed from: lambda$dim$3$com-chirag-home-NowPlaying$Pill, reason: not valid java name */
        /* synthetic */ void m48lambda$dim$3$comchiraghomeNowPlaying$Pill(ValueAnimator valueAnimator) {
            float fFloatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
            this.glass.body(NowPlaying.mixColor(859782975, NowPlaying.DIM_BODY, fFloatValue), 1.0f - fFloatValue);
            if (getVisibility() == 0) {
                setAlpha(1.0f - (fFloatValue * 0.6f));
            }
        }

        private void lit(boolean z) {
            this.lit = z && !this.dim;
            this.t1.setTextColor(this.lit ? -14869217 : -1);
            Ui.shade(this.t1, !this.lit);
            this.t2.setTextColor(this.lit ? -1289937633 : -855638017);
            Ui.shade(this.t2, !this.lit);
            Glyph[] glyphArr = this.gl;
            int length = glyphArr.length;
            for (int i = INFO; i < length; i += PREV) {
                glyphArr[i].color(this.lit ? -14869217 : -1);
            }
            this.sep.setBackgroundColor(this.lit ? 857546015 : 1090519039);
            this.cover.invalidate();
            this.line.invalidate();
            this.eq.invalidate();
            mark();
        }

        void setText(boolean z) {
            final String str = NowPlaying.this.title.isEmpty() ? "Spotify" : NowPlaying.this.title;
            final String str2 = NowPlaying.this.artist;
            if (!z || getVisibility() != 0 || getAlpha() == 0.0f) {
                put(str, str2);
                return;
            }
            final View view = (View) this.t1.getParent();
            view.animate().cancel();
            view.animate().alpha(0.0f).setDuration(140L).withEndAction(new Runnable() { // from class: com.chirag.home.NowPlaying$Pill$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    m52lambda$setText$4$comchiraghomeNowPlaying$Pill(str, str2, view);
                }
            }).start();
        }

        /* JADX INFO: renamed from: lambda$setText$4$com-chirag-home-NowPlaying$Pill, reason: not valid java name */
        /* synthetic */ void m52lambda$setText$4$comchiraghomeNowPlaying$Pill(String str, String str2, View view) {
            put(str, str2);
            view.animate().alpha(1.0f).setDuration(200L).withEndAction(null).start();
        }

        private void put(String str, String str2) {
            this.t1.setText(str);
            this.t2.setText(str2);
            this.t2.setVisibility(str2.isEmpty() ? 8 : INFO);
            this.line.invalidate();
        }

        void playing(boolean z) {
            this.gl[PREV] = new Glyph(z ? 18 : PLAY, Ui.dp(NowPlaying.this.a, 14), this.lit ? -14869217 : -1);
            this.btn[PREV].setImageDrawable(this.gl[PREV]);
            this.btn[PREV].setContentDescription(z ? "Pause" : "Play");
            this.eq.invalidate();
            this.line.invalidate();
        }

        void playback() {
            playing(NowPlaying.isPlaying(NowPlaying.this.ps));
        }

        void halt() {
            animate().cancel();
            this.cover.halt();
        }

        final class Cover extends View {
            private Bitmap cur;
            private final Paint edge;
            private float mix;
            private final Glyph note;
            private Bitmap old;
            private final Paint pa;
            private final Paint pb;
            private final Paint ph;
            private final int size;
            private ValueAnimator va;

            Cover(int i) {
                super(NowPlaying.this.a);
                this.pa = new Paint(Pill.NEXT);
                this.pb = new Paint(Pill.NEXT);
                this.ph = new Paint(Pill.PREV);
                this.edge = new Paint(Pill.PREV);
                this.mix = 1.0f;
                this.size = i;
                this.note = new Glyph(21, Ui.dp(NowPlaying.this.a, 13), -855638017);
                this.note.setBounds(Pill.INFO, Pill.INFO, i, i);
                this.edge.setStyle(Paint.Style.STROKE);
                this.edge.setStrokeWidth(Math.max(1.0f, Ui.dp(NowPlaying.this.a, Pill.PREV) / 2.0f));
                this.edge.setColor(956301311);
            }

            private BitmapShader shader(Bitmap bitmap) {
                BitmapShader bitmapShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                Matrix matrix = new Matrix();
                matrix.setScale(this.size / bitmap.getWidth(), this.size / bitmap.getHeight());
                bitmapShader.setLocalMatrix(matrix);
                return bitmapShader;
            }

            void set(Bitmap bitmap, boolean z) {
                if (this.va != null) {
                    this.va.cancel();
                }
                this.old = this.cur;
                this.cur = bitmap;
                this.pb.setShader(this.old == null ? null : shader(this.old));
                this.pa.setShader(this.cur == null ? null : shader(this.cur));
                if (!z || !isAttachedToWindow()) {
                    this.mix = 1.0f;
                    this.old = null;
                    invalidate();
                } else {
                    this.va = ValueAnimator.ofFloat(0.0f, 1.0f);
                    this.va.setDuration(320L);
                    this.va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.chirag.home.NowPlaying$Pill$Cover$$ExternalSyntheticLambda0
                        @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                        public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                            m53lambda$set$0$comchiraghomeNowPlaying$Pill$Cover(valueAnimator);
                        }
                    });
                    this.va.addListener(new AnimatorListenerAdapter() { // from class: com.chirag.home.NowPlaying.Pill.Cover.1
                        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                        public void onAnimationEnd(Animator animator) {
                            Cover.this.old = null;
                            Cover.this.mix = 1.0f;
                            Cover.this.invalidate();
                        }
                    });
                    this.va.start();
                }
            }

            /* JADX INFO: renamed from: lambda$set$0$com-chirag-home-NowPlaying$Pill$Cover, reason: not valid java name */
            /* synthetic */ void m53lambda$set$0$comchiraghomeNowPlaying$Pill$Cover(ValueAnimator valueAnimator) {
                this.mix = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                invalidate();
            }

            void halt() {
                if (this.va != null) {
                    this.va.end();
                }
            }

            private void layer(Canvas canvas, Bitmap bitmap, Paint paint, float f) {
                float f2 = this.size / 2.0f;
                if (bitmap == null) {
                    this.ph.setColor(Pill.this.lit ? 337452319 : 788529151);
                    this.ph.setAlpha(Math.round(this.ph.getAlpha() * f));
                    canvas.drawCircle(f2, f2, f2, this.ph);
                    this.note.color(Pill.this.lit ? -1289937633 : -855638017);
                    if (f > 0.5f) {
                        this.note.draw(canvas);
                        return;
                    }
                    return;
                }
                paint.setAlpha(Math.round(f * 255.0f));
                canvas.drawCircle(f2, f2, f2, paint);
            }

            @Override // android.view.View
            protected void onDraw(Canvas canvas) {
                if (this.old != null || this.mix < 1.0f) {
                    layer(canvas, this.old, this.pb, 1.0f - this.mix);
                }
                layer(canvas, this.cur, this.pa, this.mix);
                this.edge.setColor(Pill.this.lit ? 639442207 : 956301311);
                float f = this.size / 2.0f;
                canvas.drawCircle(f, f, f - (this.edge.getStrokeWidth() / 2.0f), this.edge);
            }
        }

        final class Line extends View {
            private final Paint bg;
            private final Paint fg;
            private final Runnable next;
            private final RectF r;

            Line() {
                super(NowPlaying.this.a);
                this.bg = new Paint(Pill.PREV);
                this.fg = new Paint(Pill.PREV);
                this.r = new RectF();
                this.next = new Runnable() { // from class: com.chirag.home.NowPlaying$Pill$Line$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        invalidate();
                    }
                };
            }

            @Override // android.view.View
            protected void onDraw(Canvas canvas) {
                removeCallbacks(this.next);
                long jPosition = NowPlaying.this.position();
                if (NowPlaying.this.duration <= 0 || jPosition < 0) {
                    return;
                }
                float width = getWidth();
                float height = getHeight();
                float f = height / 2.0f;
                this.bg.setColor(Pill.this.lit ? 689773855 : 1207959551);
                this.fg.setColor(Pill.this.lit ? -14869217 : -1);
                this.r.set(0.0f, 0.0f, width, height);
                canvas.drawRoundRect(this.r, f, f, this.bg);
                this.r.set(0.0f, 0.0f, Math.max(height, (jPosition * width) / NowPlaying.this.duration), height);
                canvas.drawRoundRect(this.r, f, f, this.fg);
                if (NowPlaying.this.started && NowPlaying.this.ps != null && NowPlaying.this.ps.getState() == Pill.NEXT && width > 0.0f) {
                    postDelayed(this.next, Math.max(250L, Math.min(5000L, (long) ((NowPlaying.this.duration / width) / Math.max(0.1f, Math.abs(NowPlaying.this.ps.getPlaybackSpeed()))))));
                }
            }

            @Override // android.view.View
            protected void onDetachedFromWindow() {
                removeCallbacks(this.next);
                super.onDetachedFromWindow();
            }
        }

        final class Eq extends View {
            private long last;
            private final Runnable next;
            private final Paint p;
            private final RectF r;
            private float t;

            Eq() {
                super(NowPlaying.this.a);
                this.p = new Paint(Pill.PREV);
                this.r = new RectF();
                this.next = new Runnable() { // from class: com.chirag.home.NowPlaying$Pill$Eq$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        invalidate();
                    }
                };
            }

            private boolean moving() {
                return NowPlaying.this.started && NowPlaying.isPlaying(NowPlaying.this.ps) && (Pill.this.focused || Pill.this.dim || SystemClock.uptimeMillis() < NowPlaying.this.eqUntil);
            }

            @Override // android.view.View
            protected void onDraw(Canvas canvas) {
                removeCallbacks(this.next);
                long jUptimeMillis = SystemClock.uptimeMillis();
                boolean zMoving = moving();
                if (zMoving && this.last != 0) {
                    this.t += Math.min(100L, jUptimeMillis - this.last) / 1000.0f;
                }
                if (!zMoving) {
                    jUptimeMillis = 0;
                }
                this.last = jUptimeMillis;
                float width = getWidth();
                float height = getHeight();
                float f = width / 4.2f;
                float f2 = (width - (3.0f * f)) / 2.0f;
                this.p.setColor(Pill.this.lit ? -14869217 : -1);
                this.p.setAlpha(NowPlaying.isPlaying(NowPlaying.this.ps) ? 235 : 140);
                for (int i = Pill.INFO; i < Pill.NEXT; i += Pill.PREV) {
                    float f3 = i;
                    float fMax = ((Math.max(0.0f, Math.min(1.0f, (((float) Math.sin((this.t * ((1.7f * f3) + 5.1f)) + (1.9f * f3))) * 0.5f * ((((float) Math.sin(this.t * ((NowPlaying.DIM_ALPHA * f3) + 1.3f))) * 0.25f) + 0.75f)) + 0.5f)) * 0.72f) + 0.28f) * height;
                    float f4 = f3 * (f + f2);
                    this.r.set(f4, height - fMax, f4 + f, height);
                    float f5 = f / 2.0f;
                    canvas.drawRoundRect(this.r, f5, f5, this.p);
                }
                if (zMoving) {
                    postDelayed(this.next, 42L);
                }
            }

            @Override // android.view.View
            public void onVisibilityAggregated(boolean z) {
                super.onVisibilityAggregated(z);
                if (z) {
                    invalidate();
                } else {
                    removeCallbacks(this.next);
                }
            }

            @Override // android.view.View
            protected void onDetachedFromWindow() {
                removeCallbacks(this.next);
                super.onDetachedFromWindow();
            }
        }
    }
}
