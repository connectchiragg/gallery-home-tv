package com.chirag.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/* JADX INFO: loaded from: classes.dex */
final class Smoke extends Drawable {
    static final float BEZEL = 0.1f;
    static final float BEZEL_DP = 14.0f;
    static final int LIT = 2;
    static final int REST = 0;
    static final int RING = 1;
    private final int[] colors;
    private final float d;
    private final float rad;
    static final int[] SHEET = {-1206511587, -871625456};
    static final int[] CARD = {620756991, 352321535};
    static final int[] FIELD = {452984831, 234881023};
    static final int[] PLAIN = {16777215, 16777215};
    private final Paint body = new Paint(RING);
    private final Paint rim = new Paint(RING);
    private final Paint top = new Paint(RING);
    private final Paint bezel = new Paint(RING);
    private final RectF r = new RectF();
    private int mode = REST;

    Smoke(Context context, int i, int[] iArr) {
        this.d = context.getResources().getDisplayMetrics().density;
        this.rad = i < 0 ? -1.0f : i * this.d;
        this.colors = iArr;
        this.rim.setStyle(Paint.Style.STROKE);
        this.rim.setStrokeWidth(Math.max(1.0f, this.d * 0.75f));
        this.top.setStyle(Paint.Style.STROKE);
        this.top.setStrokeWidth(Math.max(1.0f, this.d));
        this.bezel.setStyle(Paint.Style.STROKE);
    }

    Smoke mode(int i) {
        if (i == this.mode) {
            return this;
        }
        this.mode = i;
        rebuild();
        invalidateSelf();
        return this;
    }

    @Override // android.graphics.drawable.Drawable
    protected void onBoundsChange(Rect rect) {
        rebuild();
    }

    private void rebuild() {
        Rect bounds = getBounds();
        if (bounds.isEmpty()) {
            return;
        }
        float f = bounds.top;
        float f2 = bounds.bottom;
        float fExactCenterX = bounds.exactCenterX();
        float fMin = Math.min(bounds.width(), bounds.height()) * 0.2f;
        if (this.mode != LIT) {
            this.body.setShader(new LinearGradient(fExactCenterX, f, fExactCenterX, f2, this.colors[REST], this.colors[RING], Shader.TileMode.CLAMP));
            float f3 = this.mode == RING ? 1.0f : 0.62f;
            float f4 = fExactCenterX - fMin;
            float f5 = fMin + fExactCenterX;
            this.rim.setShader(new LinearGradient(f4, f, f5, f2, new int[]{white(0.85f * f3), white(0.3f * f3), white(0.16f * f3), white(f3 * 0.55f)}, new float[]{0.0f, 0.38f, 0.68f, 1.0f}, Shader.TileMode.CLAMP));
            this.bezel.setShader(new LinearGradient(f4, f, f5, f2, new int[]{white(BEZEL), white(0.035f), white(0.025f), white(0.07f)}, new float[]{0.0f, 0.38f, 0.68f, 1.0f}, Shader.TileMode.CLAMP));
            this.top.setShader(new LinearGradient(fExactCenterX, f, fExactCenterX, f + Math.max(1.0f, bounds.height() * 0.25f), white(BEZEL), REST, Shader.TileMode.CLAMP));
            return;
        }
        this.body.setShader(new LinearGradient(fExactCenterX, f, fExactCenterX, f2, -1, -1447186, Shader.TileMode.CLAMP));
        this.rim.setShader(new LinearGradient(fExactCenterX - fMin, f, fExactCenterX + fMin, f2, -1, -2828580, Shader.TileMode.CLAMP));
    }

    private static int white(float f) {
        return (Math.round(Math.max(0.0f, Math.min(1.0f, f)) * 255.0f) << 24) | 16777215;
    }

    private void round(Canvas canvas, Rect rect, float f, float f2, Paint paint) {
        this.r.set(rect.left + f, rect.top + f, rect.right - f, rect.bottom - f);
        float fMax = Math.max(0.0f, f2 - f);
        canvas.drawRoundRect(this.r, fMax, fMax, paint);
    }

    @Override // android.graphics.drawable.Drawable
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        float fMin = this.rad < 0.0f ? Math.min(bounds.width(), bounds.height()) / 2.0f : Math.min(this.rad, Math.min(bounds.width(), bounds.height()) / 2.0f);
        if (this.colors == PLAIN && this.mode == 0) {
            return;
        }
        round(canvas, bounds, 0.0f, fMin, this.body);
        if (this.mode != LIT) {
            if (this.colors == SHEET) {
                for (int i = RING; i <= 3; i += RING) {
                    float f = ((this.d * BEZEL_DP) * i) / 3.0f;
                    this.bezel.setStrokeWidth(f);
                    round(canvas, bounds, f / 2.0f, fMin, this.bezel);
                }
            }
            round(canvas, bounds, this.d * 1.5f, fMin, this.top);
        }
        round(canvas, bounds, this.rim.getStrokeWidth() / 2.0f, fMin, this.rim);
    }

    @Override // android.graphics.drawable.Drawable
    public void getOutline(Outline outline) {
        Rect bounds = getBounds();
        outline.setRoundRect(bounds, this.rad < 0.0f ? Math.min(bounds.width(), bounds.height()) / 2.0f : this.rad);
        outline.setAlpha(this.mode == LIT ? 1.0f : 0.0f);
    }

    @Override // android.graphics.drawable.Drawable
    public void setAlpha(int i) {
    }

    @Override // android.graphics.drawable.Drawable
    public void setColorFilter(ColorFilter colorFilter) {
    }

    @Override // android.graphics.drawable.Drawable
    public int getOpacity() {
        return -3;
    }
}
