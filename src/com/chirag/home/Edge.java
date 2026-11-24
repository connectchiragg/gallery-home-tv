package com.chirag.home;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/* JADX INFO: loaded from: classes.dex */
final class Edge extends Drawable {
    private final int color;
    private final Paint p = new Paint(1);
    private final RectF r = new RectF();
    private final float rad;
    private final float w;

    Edge(float f, float f2, int i) {
        this.rad = f;
        this.w = f2;
        this.color = i;
        this.p.setStyle(Paint.Style.STROKE);
        this.p.setStrokeWidth(this.w);
        if (i != 0) {
            this.p.setColor(i);
        }
    }

    @Override // android.graphics.drawable.Drawable
    protected void onBoundsChange(Rect rect) {
        if (this.color == 0) {
            this.p.setShader(new LinearGradient(0.0f, rect.top, 0.0f, rect.bottom, new int[]{956301311, 352321535, 536870911}, new float[]{0.0f, 0.5f, 1.0f}, Shader.TileMode.CLAMP));
        }
    }

    @Override // android.graphics.drawable.Drawable
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        float f = this.w / 2.0f;
        this.r.set(bounds.left + f, bounds.top + f, bounds.right - f, bounds.bottom - f);
        canvas.drawRoundRect(this.r, this.rad - f, this.rad - f, this.p);
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
