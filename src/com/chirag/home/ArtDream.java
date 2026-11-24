package com.chirag.home;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.service.dreams.DreamService;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/* JADX INFO: loaded from: classes.dex */
public class ArtDream extends DreamService {
    static final int FADE_MS = 1200;
    private TextView caption;
    private TextView clock;
    private ValueAnimator fade;
    private LinearLayout label;
    private Picture picture;
    private boolean running;
    private int turns;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final Runnable turn = new Runnable() { // from class: com.chirag.home.ArtDream.1
        @Override // java.lang.Runnable
        public void run() {
            ArtDream.this.next();
        }
    };
    private final Runnable tick = new Runnable() { // from class: com.chirag.home.ArtDream.2
        @Override // java.lang.Runnable
        public void run() {
            ArtDream.this.time();
            ArtDream.this.h.postDelayed(this, 15000L);
        }
    };

    static final class Picture extends View {
        Bitmap art;
        private final Rect dst;
        float fade;
        Bitmap next;
        private final Paint pic;
        private final Paint picNext;
        private final Paint shade;

        Picture(Context context) {
            super(context);
            this.pic = new Paint(2);
            this.picNext = new Paint(2);
            this.shade = new Paint(4);
            this.dst = new Rect();
        }

        @Override // android.view.View
        protected void onSizeChanged(int i, int i2, int i3, int i4) {
            if (i2 > 0) {
                float f = i2;
                this.shade.setShader(new LinearGradient(0.0f, f * 0.6f, 0.0f, f, 0, -1946157056, Shader.TileMode.CLAMP));
            }
        }

        @Override // android.view.View
        protected void onDraw(Canvas canvas) {
            this.dst.set(0, 0, getWidth(), getHeight());
            if (this.art != null) {
                canvas.drawBitmap(this.art, (Rect) null, this.dst, this.pic);
            }
            if (this.next != null) {
                this.picNext.setAlpha(Math.round(this.fade * 255.0f));
                canvas.drawBitmap(this.next, (Rect) null, this.dst, this.picNext);
            }
            canvas.drawPaint(this.shade);
        }
    }

    @Override // android.service.dreams.DreamService, android.view.Window.Callback
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setBackgroundColor(-16777216);
        this.picture = new Picture(this);
        this.picture.art = Art.current(this);
        frameLayout.addView(this.picture, new FrameLayout.LayoutParams(-1, -1));
        this.label = new LinearLayout(this);
        this.label.setOrientation(1);
        this.clock = Ui.text(this, "", 44, -218103809, Ui.LIGHT);
        this.caption = Ui.text(this, Art.caption(), 13, -855638017, Ui.REGULAR);
        this.caption.setPadding(0, Ui.dp(this, 6), 0, 0);
        this.label.addView(this.clock);
        this.label.addView(this.caption);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-2, -2, 8388691);
        layoutParams.setMargins(Ui.dp(this, 60), 0, 0, Ui.dp(this, 44));
        frameLayout.addView(this.label, layoutParams);
        setContentView(frameLayout);
        time();
    }

    @Override // android.service.dreams.DreamService
    public void onDreamingStarted() {
        super.onDreamingStarted();
        this.running = true;
        this.h.postDelayed(this.turn, Math.max(4000L, 30000 - (SystemClock.uptimeMillis() - Art.since)));
        this.h.postDelayed(this.tick, 15000L);
    }

    @Override // android.service.dreams.DreamService
    public void onDreamingStopped() {
        this.running = false;
        this.h.removeCallbacksAndMessages(null);
        if (this.fade != null) {
            this.fade.end();
        }
        super.onDreamingStopped();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void time() {
        String str = new SimpleDateFormat("h:mm", Locale.ENGLISH).format(new Date());
        if (!str.contentEquals(this.clock.getText())) {
            this.clock.setText(str);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void next() {
        if (this.running) {
            final Context applicationContext = getApplicationContext();
            Lens.EXEC.execute(new Runnable() { // from class: com.chirag.home.ArtDream$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    m14lambda$next$2$comchiraghomeArtDream(applicationContext);
                }
            });
        }
    }

    /* JADX INFO: renamed from: lambda$next$2$com-chirag-home-ArtDream, reason: not valid java name */
    /* synthetic */ void m14lambda$next$2$comchiraghomeArtDream(Context context) {
        final Art.Pick next = Art.next(context);
        final Bitmap bitmap = next == null ? null : next.bitmap;
        this.h.post(new Runnable() { // from class: com.chirag.home.ArtDream$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                m13lambda$next$1$comchiraghomeArtDream(bitmap, next);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$next$1$com-chirag-home-ArtDream, reason: not valid java name */
    /* synthetic */ void m13lambda$next$1$comchiraghomeArtDream(final Bitmap bitmap, Art.Pick pick) {
        if (this.running) {
            if (bitmap == null) {
                this.h.postDelayed(this.turn, 30000L);
                return;
            }
            Art.show(pick);
            final String strCaption = Art.caption();
            this.picture.next = bitmap;
            this.picture.fade = 0.0f;
            this.turns++;
            final float fDp = Ui.dp(this, (this.turns * 37) % 97);
            final float f = -Ui.dp(this, (this.turns * 23) % 49);
            this.fade = ValueAnimator.ofFloat(0.0f, 1.0f);
            this.fade.setDuration(Art.fadeMs());
            this.fade.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.chirag.home.ArtDream$$ExternalSyntheticLambda1
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                    m12lambda$next$0$comchiraghomeArtDream(fDp, f, strCaption, valueAnimator);
                }
            });
            this.fade.addListener(new AnimatorListenerAdapter() { // from class: com.chirag.home.ArtDream.3

                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animator) {
                    ArtDream.this.picture.art = bitmap;
                    ArtDream.this.picture.next = null;
                    ArtDream.this.picture.fade = 0.0f;
                    ArtDream.this.picture.invalidate();
                    ArtDream.this.label.setAlpha(1.0f);
                }
            });
            this.fade.start();
            this.h.postDelayed(this.turn, 30000L);
        }
    }

    /* JADX INFO: renamed from: lambda$next$0$com-chirag-home-ArtDream, reason: not valid java name */
    /* synthetic */ void m12lambda$next$0$comchiraghomeArtDream(float f, float f2, String str, ValueAnimator valueAnimator) {
        float fFloatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
        this.picture.fade = fFloatValue;
        this.picture.invalidate();
        this.label.setAlpha(Math.abs(1.0f - (2.0f * fFloatValue)));
        if (fFloatValue >= 0.5f && this.label.getTranslationX() != f) {
            this.label.setTranslationX(f);
            this.label.setTranslationY(f2);
            this.caption.setText(str);
        }
    }
}
