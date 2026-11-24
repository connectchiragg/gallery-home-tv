package com.chirag.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.os.SystemClock;
import android.view.View;
import java.lang.reflect.Array;

/* JADX INFO: loaded from: classes.dex */
final class VoiceGlow extends View {
    static final float CORNER_DP = 10.0f;
    static final int FRAME_MS = 33;
    static final int LISTEN = 1;
    static final int OFF = 0;
    static final float RIPPLE = 0.75f;
    static final int RIPPLE_MS = 700;
    static final float SPEED_LOUD = 110.0f;
    static final float SPEED_QUIET = 22.0f;
    static final float SPEED_THINK = 40.0f;
    static final int THINK = 2;
    private float angle;
    private final Paint[][] band;
    private SweepGradient colours;
    private final Paint core;
    private float cx;
    private float cy;
    private final float d;
    private final Runnable frame;
    private long last;
    private float level;
    private float lobeAngle;
    private SweepGradient lobes;
    private int mode;
    private final Matrix mx;
    private long rippleAt;
    private float shown;
    private float strength;
    private boolean ticking;
    static final float[] DEPTH_DP = {5.0f, 30.0f, 78.0f, 140.0f};
    static final float[] REST = {1.0f, 0.8f, 0.34f, 0.05f};
    static final float[] LOUD = {0.0f, 0.2f, 0.56f, 0.55f};
    static final int[] COLORS = {-4422925, -673302, -7495681, -9447681, -39048, -17807, -3766529, -4422925};

    VoiceGlow(Context context) {
        super(context);
        this.band = (Paint[][]) Array.newInstance((Class<?>) Paint.class, 4, 8);
        this.core = new Paint(LISTEN);
        this.mx = new Matrix();
        this.mode = OFF;
        this.frame = new Runnable() { // from class: com.chirag.home.VoiceGlow.1
            @Override // java.lang.Runnable
            public void run() {
                VoiceGlow.this.ticking = false;
                VoiceGlow.this.invalidate();
            }
        };
        this.d = context.getResources().getDisplayMetrics().density;
        this.core.setColor(-1);
        this.core.setStyle(Paint.Style.STROKE);
        this.core.setStrokeWidth(this.d * 1.5f);
    }

    void mode(int i) {
        if (i == LISTEN && this.mode != LISTEN) {
            this.rippleAt = SystemClock.uptimeMillis();
        }
        this.mode = i;
        if (i != LISTEN) {
            this.level = 0.0f;
        }
        if (!this.ticking) {
            this.last = 0L;
            invalidate();
        }
    }

    void level(float f) {
        this.level = Math.max(0.0f, Math.min(1.0f, f));
    }

    void stop() {
        this.mode = OFF;
        this.strength = 0.0f;
        removeCallbacks(this.frame);
        this.ticking = false;
        invalidate();
    }

    @Override // android.view.View
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        if (i <= 0 || i2 <= 0) {
            return;
        }
        float f = i;
        this.cx = f / 2.0f;
        float f2 = i2;
        this.cy = f2 / 2.0f;
        this.colours = new SweepGradient(this.cx, this.cy, COLORS, (float[]) null);
        this.lobes = new SweepGradient(this.cx, this.cy, new int[]{-1, 822083583, -637534209, 536870911, -1, 1090519039, -1275068417, 654311423, -419430401, 872415231, -1}, new float[]{0.0f, 0.09f, 0.21f, 0.33f, 0.42f, 0.52f, 0.63f, 0.72f, 0.83f, 0.92f, 1.0f});
        ComposeShader composeShader = new ComposeShader(this.colours, this.lobes, PorterDuff.Mode.DST_IN);
        int[] iArr = {-1, -1627389953, 1308622847, 402653183, 16777215};
        char c = 5;
        float[] fArr = {0.0f, 0.22f, 0.5f, 0.8f, 1.0f};
        int i5 = OFF;
        while (i5 < 4) {
            float f3 = DEPTH_DP[i5] * this.d;
            float f4 = f3 + (this.d * CORNER_DP);
            Shader shader = i5 >= THINK ? composeShader : this.colours;
            float f5 = (f4 - f3) / f4;
            int[] iArr2 = {16777215, 16777215, 402653183, 1308622847, -1627389953, -1};
            float f6 = 1.0f - f5;
            float[] fArr2 = new float[6];
            fArr2[OFF] = OFF;
            fArr2[LISTEN] = f5;
            fArr2[THINK] = f5 + (0.2f * f6);
            fArr2[3] = f5 + (0.5f * f6);
            fArr2[4] = (f6 * 0.78f) + f5;
            fArr2[c] = 1.0f;
            Shader[] shaderArr = new Shader[8];
            Shader shader2 = shader;
            float[] fArr3 = fArr;
            shaderArr[OFF] = new LinearGradient(0.0f, 0.0f, 0.0f, f3, iArr, fArr3, Shader.TileMode.CLAMP);
            shaderArr[LISTEN] = new LinearGradient(0.0f, f2, 0.0f, f2 - f3, iArr, fArr3, Shader.TileMode.CLAMP);
            shaderArr[THINK] = new LinearGradient(0.0f, 0.0f, f3, 0.0f, iArr, fArr3, Shader.TileMode.CLAMP);
            int i6 = i5;
            float[] fArr4 = fArr;
            char c2 = c;
            ComposeShader composeShader2 = composeShader;
            float f7 = f2;
            shaderArr[3] = new LinearGradient(f, 0.0f, f - f3, 0.0f, iArr, fArr4, Shader.TileMode.CLAMP);
            shaderArr[4] = new RadialGradient(f4, f4, f4, iArr2, fArr2, Shader.TileMode.CLAMP);
            float f8 = f - f4;
            shaderArr[c2] = new RadialGradient(f8, f4, f4, iArr2, fArr2, Shader.TileMode.CLAMP);
            float f9 = f7 - f4;
            shaderArr[6] = new RadialGradient(f4, f9, f4, iArr2, fArr2, Shader.TileMode.CLAMP);
            shaderArr[7] = new RadialGradient(f8, f9, f4, iArr2, fArr2, Shader.TileMode.CLAMP);
            for (int i7 = OFF; i7 < 8; i7 += LISTEN) {
                this.band[i6][i7] = new Paint();
                this.band[i6][i7].setShader(new ComposeShader(shader2, shaderArr[i7], PorterDuff.Mode.DST_IN));
            }
            i5 = i6 + LISTEN;
            fArr = fArr4;
            c = c2;
            composeShader = composeShader2;
            f2 = f7;
        }
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        float fSin;
        if (this.colours == null) {
            return;
        }
        int width = getWidth();
        int height = getHeight();
        long jUptimeMillis = SystemClock.uptimeMillis();
        float f = 0.0f;
        float fMin = this.last == 0 ? OFF : Math.min(0.1f, (jUptimeMillis - this.last) / 1000.0f);
        this.last = jUptimeMillis;
        this.shown += (this.level - this.shown) * (this.level > this.shown ? 0.45f : 0.12f);
        float f2 = 1.0f;
        this.strength += ((this.mode == 0 ? OFF : 1.0f) - this.strength) * Math.min(1.0f, 5.0f * fMin);
        if (this.mode != 0 || this.strength >= 0.02f) {
            this.angle += (this.mode == THINK ? SPEED_THINK : SPEED_QUIET + (this.shown * 88.0f)) * fMin;
            this.lobeAngle -= fMin * ((this.shown * 30.0f) + 9.0f);
            if (this.angle > 360.0f) {
                this.angle -= 360.0f;
            }
            if (this.lobeAngle < -360.0f) {
                this.lobeAngle += 360.0f;
            }
            this.mx.setRotate(this.angle, this.cx, this.cy);
            this.colours.setLocalMatrix(this.mx);
            this.mx.setRotate(this.lobeAngle, this.cx, this.cy);
            this.lobes.setLocalMatrix(this.mx);
            float f3 = 0.5f;
            float fSin2 = this.mode == THINK ? (((float) Math.sin(jUptimeMillis / 900.0d)) * 0.5f) + 0.5f : OFF;
            long j = jUptimeMillis - this.rippleAt;
            if (this.rippleAt == 0 || j >= 700) {
                fSin = OFF;
            } else {
                float f4 = j / 700.0f;
                fSin = ((float) Math.sin(Math.sqrt(f4) * 3.141592653589793d)) * RIPPLE * (1.0f - f4);
            }
            int i = 3;
            while (i >= 0) {
                int iRound = Math.round(Math.max(f, Math.min(f2, (REST[i] + (LOUD[i] * this.shown) + (i >= THINK ? 0.14f * fSin2 : f) + (i == 3 ? fSin : i == THINK ? fSin * f3 : f)) * this.strength)) * 255.0f);
                float f5 = DEPTH_DP[i] * this.d;
                float f6 = f5 + (this.d * CORNER_DP);
                Paint[] paintArr = this.band[i];
                int length = paintArr.length;
                for (int i2 = OFF; i2 < length; i2 += LISTEN) {
                    paintArr[i2].setAlpha(iRound);
                }
                float f7 = width;
                float f8 = f7 - f6;
                canvas.drawRect(f6, 0.0f, f8, f5, paintArr[OFF]);
                float f9 = height;
                canvas.drawRect(f6, f9 - f5, f8, f9, paintArr[LISTEN]);
                float f10 = f9 - f6;
                canvas.drawRect(0.0f, f6, f5, f10, paintArr[THINK]);
                canvas.drawRect(f7 - f5, f6, f7, f10, paintArr[3]);
                canvas.drawRect(0.0f, 0.0f, f6, f6, paintArr[4]);
                canvas.drawRect(f8, 0.0f, f7, f6, paintArr[5]);
                canvas.drawRect(0.0f, f10, f6, f9, paintArr[6]);
                canvas.drawRect(f8, f10, f7, f9, paintArr[7]);
                i--;
                f = 0.0f;
                f3 = 0.5f;
                f2 = 1.0f;
            }
            this.core.setAlpha(Math.round(this.strength * 140.0f));
            canvas.drawRect(0.0f, 0.0f, width, height, this.core);
            if (!this.ticking) {
                this.ticking = true;
                postDelayed(this.frame, 33L);
                return;
            }
            return;
        }
        this.strength = 0.0f;
    }

    static final class Mark extends View {
        static final int LISTEN = 1;
        static final int REST = 0;
        private static final float[] SPEED = {0.9f, -0.6f, 0.45f, -1.15f};
        private static final float[] SWELL = {5.3f, 6.9f, 4.4f, 7.7f};
        static final int THINK = 2;
        private final Paint core;
        private final Runnable frame;
        private final Paint halo;
        private long last;
        private float level;
        private int mode;
        private final Paint[] petal;
        private float shown;
        private boolean ticking;
        private float time;

        Mark(Context context) {
            super(context);
            this.petal = new Paint[4];
            this.core = new Paint(LISTEN);
            this.halo = new Paint(LISTEN);
            this.mode = REST;
            this.frame = new Runnable() { // from class: com.chirag.home.VoiceGlow.Mark.1
                @Override // java.lang.Runnable
                public void run() {
                    Mark.this.ticking = false;
                    Mark.this.invalidate();
                }
            };
            int[] iArr = {VoiceGlow.COLORS[REST], VoiceGlow.COLORS[4], VoiceGlow.COLORS[THINK], VoiceGlow.COLORS[5]};
            for (int i = REST; i < 4; i += LISTEN) {
                int i2 = 16777215 & iArr[i];
                this.petal[i] = new Paint(LISTEN);
                this.petal[i].setShader(new RadialGradient(0.0f, 0.0f, 1.0f, new int[]{(-234881024) | i2, (-1291845632) | i2, 855638016 | i2, i2}, new float[]{0.0f, 0.45f, 0.8f, 1.0f}, Shader.TileMode.CLAMP));
            }
            this.core.setShader(new RadialGradient(0.0f, 0.0f, 1.0f, new int[]{-1, -855638017, 16777215}, new float[]{0.0f, 0.45f, 1.0f}, Shader.TileMode.CLAMP));
            this.halo.setShader(new RadialGradient(0.0f, 0.0f, 1.0f, new int[]{1308622847, 452984831, 16777215}, new float[]{0.0f, 0.6f, 1.0f}, Shader.TileMode.CLAMP));
        }

        void mode(int i) {
            this.mode = i;
            if (i != LISTEN) {
                this.level = 0.0f;
            }
            if (this.ticking) {
                return;
            }
            this.last = 0L;
            invalidate();
        }

        void level(float f) {
            this.level = Math.max(0.0f, Math.min(1.0f, f));
        }

        void stop() {
            this.mode = REST;
            removeCallbacks(this.frame);
            this.ticking = false;
            invalidate();
        }

        private void disc(Canvas canvas, float f, float f2, float f3, Paint paint) {
            canvas.save();
            canvas.translate(f, f2);
            canvas.scale(f3, f3);
            canvas.drawCircle(0.0f, 0.0f, 1.0f, paint);
            canvas.restore();
        }

        @Override // android.view.View
        protected void onDraw(Canvas canvas) {
            float f;
            long jUptimeMillis = SystemClock.uptimeMillis();
            float fMin = this.last == 0 ? REST : Math.min(0.1f, (jUptimeMillis - this.last) / 1000.0f);
            this.last = jUptimeMillis;
            this.shown += (this.level - this.shown) * (this.level > this.shown ? 0.5f : 0.15f);
            float f2 = this.time;
            if (this.mode == THINK) {
                f = 2.6f;
            } else {
                f = this.mode == LISTEN ? (this.shown * 2.2f) + 0.8f : REST;
            }
            this.time = f2 + (fMin * f);
            float width = getWidth() / 2.0f;
            float height = getHeight() / 2.0f;
            float fMin2 = Math.min(width, height);
            float fSin = this.mode == THINK ? (((float) Math.sin(jUptimeMillis / 260.0d)) * 0.5f) + 0.5f : 0.0f;
            int i = this.mode == 0 ? 120 : 255;
            this.halo.setAlpha(i);
            disc(canvas, width, height, fMin2, this.halo);
            for (int i2 = REST; i2 < 4; i2 += LISTEN) {
                double d = (((double) (this.time * SPEED[i2])) * 1.6d) + (((double) i2) * 1.5708d);
                float fSin2 = (float) Math.sin((((this.time * 6.2832f) / SWELL[i2]) * 2.2f) + i2);
                float f3 = ((this.shown * 0.08f) + 0.26f + (0.05f * fSin2)) * fMin2;
                float f4 = ((this.shown * 0.1f) + 0.62f + (fSin2 * 0.08f)) * fMin2;
                this.petal[i2].setAlpha(i);
                disc(canvas, width + (((float) Math.cos(d)) * f3), height + (f3 * ((float) Math.sin(d))), f4, this.petal[i2]);
            }
            this.core.setAlpha(this.mode == 0 ? 150 : 255);
            disc(canvas, width, height, fMin2 * ((this.shown * 0.14f) + 0.24f + (fSin * 0.08f)), this.core);
            if (this.mode != 0 && !this.ticking) {
                this.ticking = true;
                postDelayed(this.frame, 33L);
            }
        }
    }
}
