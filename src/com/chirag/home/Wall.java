package com.chirag.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.view.View;

/* JADX INFO: loaded from: classes.dex */
final class Wall {
    static final int GRAIN = 128;
    static final int GRAIN_DARK = 70;
    static final int GRAIN_LIGHT = 26;
    static final int MOTTLE = 64;
    static final int MOTTLE_DARK = 46;
    static final int MOTTLE_LIGHT = 11;
    static final int MOTTLE_SCALE = 6;
    static final int MOTTLE_TILT = 19;
    static final int PAINTING_ALPHA = 255;
    static final int PORE = 56;
    static final float SCRIM_BOOST_FROM = 95.0f;
    static final float SCRIM_BOOST_MAX = 1.6f;
    static final float SCRIM_BOTTOM = 0.5f;
    static final float SCRIM_MID = 0.05f;
    static final float SCRIM_TOP = 0.3f;
    static final float VIGNETTE = 0.45f;
    private static Bitmap grain;
    private static Bitmap mottle;

    Wall() {
    }

    static float scrim(float f, float f2) {
        float f3;
        if (f < 0.22f) {
            f3 = ((f * (-0.25f)) / 0.22f) + SCRIM_TOP;
        } else {
            f3 = f < VIGNETTE ? 0.05f : (((f - VIGNETTE) * VIGNETTE) / 0.55f) + SCRIM_MID;
        }
        return Math.min(0.85f, f3 * f2);
    }

    static float hash(int i, int i2, int i3) {
        int i4 = (i * 374761393) + (i2 * 668265263) + (i3 * 1274126177);
        int i5 = (i4 ^ (i4 >>> 13)) * 1274126177;
        return ((i5 ^ (i5 >>> 16)) & 65535) / 65535.0f;
    }

    static float noise(float f, float f2, int i, int i2, int i3) {
        int iFloor = (int) Math.floor(f);
        int iFloor2 = (int) Math.floor(f2);
        float f3 = f - iFloor;
        float f4 = f2 - iFloor2;
        float f5 = f3 * f3 * (3.0f - (f3 * 2.0f));
        float f6 = f4 * f4 * (3.0f - (f4 * 2.0f));
        int i4 = iFloor % i;
        int i5 = (iFloor + 1) % i;
        int i6 = iFloor2 % i2;
        int i7 = (iFloor2 + 1) % i2;
        float fHash = hash(i4, i6, i3);
        float fHash2 = hash(i5, i6, i3);
        float fHash3 = hash(i4, i7, i3);
        return ((fHash2 - fHash) * f5) + fHash + ((fHash3 - fHash) * f6) + ((((fHash - fHash2) - fHash3) + hash(i5, i7, i3)) * f5 * f6);
    }

    private static int px(float f, int i, int i2) {
        return f >= 0.0f ? (Math.min(PAINTING_ALPHA, (int) (f * i)) << 24) | 16777215 : Math.min(PAINTING_ALPHA, (int) ((-f) * i2)) << 24;
    }

    static Bitmap grain() {
        if (grain != null) {
            return grain;
        }
        int[] iArr = new int[16384];
        for (int i = 0; i < GRAIN; i++) {
            int i2 = 0;
            while (i2 < GRAIN) {
                int i3 = i2 + 1;
                iArr[(i * GRAIN) + i2] = hash(i2, i, 4) > 0.9965f ? 939524096 : px((((((hash((i2 + 127) & 127, i, 1) * 0.25f) + (hash(i2, i, 1) * SCRIM_BOTTOM)) + (hash(i3 & 127, i, 1) * 0.25f)) - SCRIM_BOTTOM) * SCRIM_BOOST_MAX) + ((noise(i2 / 8.0f, i / 2.0f, 16, MOTTLE, 2) - SCRIM_BOTTOM) * 0.6f), GRAIN_LIGHT, GRAIN_DARK);
                i2 = i3;
            }
        }
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(iArr, GRAIN, GRAIN, Bitmap.Config.ARGB_8888);
        grain = bitmapCreateBitmap;
        return bitmapCreateBitmap;
    }

    static Bitmap mottle() {
        if (mottle != null) {
            return mottle;
        }
        int[] iArr = new int[4096];
        for (int i = 0; i < MOTTLE; i++) {
            for (int i2 = 0; i2 < MOTTLE; i2++) {
                float f = i2;
                float f2 = i;
                iArr[(i * MOTTLE) + i2] = px(((noise(f / 16.0f, f2 / 16.0f, 4, 4, 5) - SCRIM_BOTTOM) * 1.4f) + ((noise(f / 8.0f, f2 / 8.0f, 8, 8, MOTTLE_SCALE) - SCRIM_BOTTOM) * 0.6f), MOTTLE_LIGHT, MOTTLE_DARK);
            }
        }
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(iArr, MOTTLE, MOTTLE, Bitmap.Config.ARGB_8888);
        mottle = bitmapCreateBitmap;
        return bitmapCreateBitmap;
    }

    static final class Surface extends View {
        private Bitmap art;
        private float bare;
        private float boost;
        private final Rect dst;
        private float fade;
        private final Paint g;
        private final Paint m;
        private Bitmap next;
        private final Paint pic;
        private final Paint picNext;
        private final Paint sc;
        private final Paint scNext;
        private final Paint v;

        Surface(Context context) {
            super(context);
            this.g = new Paint();
            this.m = new Paint(2);
            this.v = new Paint(4);
            this.sc = new Paint(4);
            this.scNext = new Paint(4);
            this.pic = new Paint(2);
            this.picNext = new Paint(2);
            this.dst = new Rect();
            this.boost = 1.0f;
            this.art = Art.current(context);
            this.boost = Art.boost();
            this.pic.setAlpha(Wall.PAINTING_ALPHA);
            this.g.setShader(new BitmapShader(Wall.grain(), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
            BitmapShader bitmapShader = new BitmapShader(Wall.mottle(), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
            Matrix matrix = new Matrix();
            matrix.setScale(6.0f, 6.0f);
            matrix.postRotate(19.0f);
            bitmapShader.setLocalMatrix(matrix);
            this.m.setShader(bitmapShader);
        }

        private Shader scrimShader(float f, int i) {
            return new LinearGradient(0.0f, 0.0f, 0.0f, i, new int[]{Math.round(Wall.scrim(0.0f, f) * 255.0f) << 24, Math.round(Wall.scrim(0.22f, f) * 255.0f) << 24, Math.round(Wall.scrim(Wall.VIGNETTE, f) * 255.0f) << 24, Math.round(Wall.scrim(1.0f, f) * 255.0f) << 24}, new float[]{0.0f, 0.22f, Wall.VIGNETTE, 1.0f}, Shader.TileMode.CLAMP);
        }

        @Override // android.view.View
        protected void onSizeChanged(int i, int i2, int i3, int i4) {
            if (i <= 0 || i2 <= 0) {
                return;
            }
            this.v.setShader(new RadialGradient(Wall.SCRIM_BOTTOM * i, 0.42f * i2, ((float) Math.hypot(i, i2)) * 0.62f, new int[]{0, 0, Math.round(114.75f) << 24}, new float[]{0.0f, Wall.SCRIM_BOTTOM, 1.0f}, Shader.TileMode.CLAMP));
            this.sc.setShader(scrimShader(this.boost, i2));
        }

        void sync() {
            Bitmap bitmapCurrent = Art.current(getContext());
            if (bitmapCurrent == this.art && this.next == null) {
                return;
            }
            this.art = bitmapCurrent;
            this.next = null;
            this.boost = Art.boost();
            this.fade = 0.0f;
            this.sc.setAlpha(Wall.PAINTING_ALPHA);
            if (getHeight() > 0) {
                this.sc.setShader(scrimShader(this.boost, getHeight()));
            }
            invalidate();
        }

        void show(Bitmap bitmap, float f) {
            this.next = bitmap;
            this.boost = f;
            this.fade = 0.0f;
            if (getHeight() > 0) {
                this.scNext.setShader(scrimShader(f, getHeight()));
            }
        }

        void fade(float f) {
            this.fade = f;
            invalidate();
        }

        void bare(float f) {
            this.bare = f;
            invalidate();
        }

        void settle() {
            if (this.next == null) {
                return;
            }
            this.art = this.next;
            this.next = null;
            this.fade = 0.0f;
            this.sc.setShader(this.scNext.getShader());
            invalidate();
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
            if (this.bare >= 1.0f) {
                return;
            }
            float f = 1.0f - this.bare;
            float f2 = f * 255.0f;
            this.m.setAlpha(Math.round(f2));
            this.g.setAlpha(Math.round(f2));
            this.v.setAlpha(Math.round(f2));
            canvas.drawPaint(this.m);
            canvas.drawPaint(this.g);
            if (this.next != null) {
                this.sc.setAlpha(Math.round((1.0f - this.fade) * 255.0f * f));
                this.scNext.setAlpha(Math.round(this.fade * 255.0f * f));
                canvas.drawPaint(this.sc);
                canvas.drawPaint(this.scNext);
            } else {
                this.sc.setAlpha(Math.round(f2));
                canvas.drawPaint(this.sc);
            }
            canvas.drawPaint(this.v);
        }
    }
}
