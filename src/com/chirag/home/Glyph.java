package com.chirag.home;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/* JADX INFO: loaded from: classes.dex */
final class Glyph extends Drawable {
    static final int ALERT = 12;
    static final int ANTENNA = 9;
    static final int CAST = 3;
    static final int CHECK = 7;
    static final int CLOCK = 11;
    static final int CLOSE = 6;
    static final int DOTS = 5;
    static final int HDMI = 8;
    static final int INPUT = 1;
    static final int KEYS = 15;
    static final int MIC = 14;
    static final int MOON = 17;
    static final int NEXT = 19;
    static final int NOTE = 21;
    static final int PAD = 10;
    static final int PAUSE = 18;
    static final int PLAY = 2;
    static final int PREV = 20;
    static final int PS = 13;
    static final int SLIDERS = 4;
    static final int TV = 16;
    private final int size;
    private final Path line = new Path();
    private final Path solid = new Path();
    private final Paint pl = new Paint(INPUT);
    private final Paint ps = new Paint(INPUT);

    Glyph(int i, int i2, int i3) {
        this.size = i2;
        Path path = this.line;
        Path path2 = this.solid;
        switch (i) {
            case INPUT /* 1 */:
                path.moveTo(6.5f, 3.0f);
                path.lineTo(13.0f, 3.0f);
                path.lineTo(13.0f, 13.0f);
                path.lineTo(6.5f, 13.0f);
                path.moveTo(2.0f, 8.0f);
                path.lineTo(9.5f, 8.0f);
                path.moveTo(7.0f, 5.5f);
                path.lineTo(9.5f, 8.0f);
                path.lineTo(7.0f, 10.5f);
                break;
            case PLAY /* 2 */:
                path2.moveTo(5.0f, 3.5f);
                path2.lineTo(12.5f, 8.0f);
                path2.lineTo(5.0f, 12.5f);
                path2.close();
                path.addPath(path2);
                break;
            case CAST /* 3 */:
                path.moveTo(4.5f, 11.0f);
                path.lineTo(2.5f, 11.0f);
                path.lineTo(2.5f, 3.0f);
                path.lineTo(13.5f, 3.0f);
                path.lineTo(13.5f, 11.0f);
                path.lineTo(11.5f, 11.0f);
                path2.moveTo(8.0f, 9.5f);
                path2.lineTo(11.0f, 13.5f);
                path2.lineTo(5.0f, 13.5f);
                path2.close();
                break;
            case SLIDERS /* 4 */:
                path.moveTo(2.0f, 5.0f);
                path.lineTo(14.0f, 5.0f);
                path.moveTo(2.0f, 11.0f);
                path.lineTo(14.0f, 11.0f);
                path2.addCircle(10.0f, 5.0f, 2.0f, Path.Direction.CW);
                path2.addCircle(6.0f, 11.0f, 2.0f, Path.Direction.CW);
                break;
            case DOTS /* 5 */:
                path2.addCircle(3.5f, 8.0f, 1.4f, Path.Direction.CW);
                path2.addCircle(8.0f, 8.0f, 1.4f, Path.Direction.CW);
                path2.addCircle(12.5f, 8.0f, 1.4f, Path.Direction.CW);
                break;
            case CLOSE /* 6 */:
                path.moveTo(4.0f, 4.0f);
                path.lineTo(12.0f, 12.0f);
                path.moveTo(12.0f, 4.0f);
                path.lineTo(4.0f, 12.0f);
                break;
            case CHECK /* 7 */:
                path.moveTo(3.0f, 8.5f);
                path.lineTo(6.5f, 12.0f);
                path.lineTo(13.0f, 4.5f);
                break;
            case HDMI /* 8 */:
                path.moveTo(2.0f, 5.0f);
                path.lineTo(14.0f, 5.0f);
                path.lineTo(14.0f, 8.5f);
                path.lineTo(11.5f, 11.0f);
                path.lineTo(4.5f, 11.0f);
                path.lineTo(2.0f, 8.5f);
                path.close();
                path.moveTo(5.5f, 8.0f);
                path.lineTo(10.5f, 8.0f);
                break;
            case ANTENNA /* 9 */:
                path.moveTo(8.0f, 7.5f);
                path.lineTo(8.0f, 14.0f);
                path.moveTo(5.0f, 14.0f);
                path.lineTo(11.0f, 14.0f);
                path.addArc(new RectF(4.5f, 2.0f, 11.5f, 9.0f), -50.0f, 100.0f);
                path.addArc(new RectF(4.5f, 2.0f, 11.5f, 9.0f), 130.0f, 100.0f);
                path.addArc(new RectF(1.5f, -1.0f, 14.5f, 12.0f), -40.0f, 80.0f);
                path.addArc(new RectF(1.5f, -1.0f, 14.5f, 12.0f), 140.0f, 80.0f);
                path2.addCircle(8.0f, 5.5f, 1.4f, Path.Direction.CW);
                break;
            case PAD /* 10 */:
                path.addRoundRect(new RectF(1.5f, 4.5f, 14.5f, 11.5f), 3.5f, 3.5f, Path.Direction.CW);
                path.moveTo(5.0f, 6.6f);
                path.lineTo(5.0f, 9.4f);
                path.moveTo(3.6f, 8.0f);
                path.lineTo(6.4f, 8.0f);
                path2.addCircle(11.0f, 8.0f, 1.2f, Path.Direction.CW);
                break;
            case CLOCK /* 11 */:
                path.addCircle(8.0f, 8.0f, 6.0f, Path.Direction.CW);
                path.moveTo(8.0f, 4.5f);
                path.lineTo(8.0f, 8.0f);
                path.lineTo(10.5f, 9.5f);
                break;
            case ALERT /* 12 */:
            default:
                path.addCircle(8.0f, 8.0f, 6.0f, Path.Direction.CW);
                path.moveTo(8.0f, 4.7f);
                path.lineTo(8.0f, 8.8f);
                path2.addCircle(8.0f, 11.2f, 0.9f, Path.Direction.CW);
                break;
            case PS /* 13 */:
                Ps5.mark(path2);
                break;
            case MIC /* 14 */:
                path.addRoundRect(new RectF(6.0f, 2.0f, 10.0f, 9.5f), 2.0f, 2.0f, Path.Direction.CW);
                path.addArc(new RectF(3.5f, 3.5f, 12.5f, 12.0f), 0.0f, 180.0f);
                path.moveTo(8.0f, 12.0f);
                path.lineTo(8.0f, 14.2f);
                path.moveTo(5.5f, 14.2f);
                path.lineTo(10.5f, 14.2f);
                break;
            case KEYS /* 15 */:
                path.addRoundRect(new RectF(1.5f, 4.0f, 14.5f, 12.0f), 2.0f, 2.0f, Path.Direction.CW);
                path.moveTo(5.5f, 10.0f);
                path.lineTo(10.5f, 10.0f);
                path2.addCircle(4.5f, 7.0f, 0.7f, Path.Direction.CW);
                path2.addCircle(6.8f, 7.0f, 0.7f, Path.Direction.CW);
                path2.addCircle(9.2f, 7.0f, 0.7f, Path.Direction.CW);
                path2.addCircle(11.5f, 7.0f, 0.7f, Path.Direction.CW);
                break;
            case TV /* 16 */:
                path.addRoundRect(new RectF(1.8f, 3.0f, 14.2f, 11.2f), 1.5f, 1.5f, Path.Direction.CW);
                path.moveTo(5.5f, 13.6f);
                path.lineTo(10.5f, 13.6f);
                path.moveTo(8.0f, 11.2f);
                path.lineTo(8.0f, 13.6f);
                break;
            case MOON /* 17 */:
                Path path3 = new Path();
                path2.addCircle(8.0f, 8.0f, 5.5f, Path.Direction.CW);
                path3.addCircle(10.8f, 6.0f, 4.6f, Path.Direction.CW);
                path2.op(path3, Path.Op.DIFFERENCE);
                break;
            case PAUSE /* 18 */:
                path2.addRoundRect(new RectF(4.0f, 3.0f, 6.8f, 13.0f), 1.0f, 1.0f, Path.Direction.CW);
                path2.addRoundRect(new RectF(9.2f, 3.0f, 12.0f, 13.0f), 1.0f, 1.0f, Path.Direction.CW);
                break;
            case NEXT /* 19 */:
                path2.moveTo(3.0f, 3.5f);
                path2.lineTo(10.0f, 8.0f);
                path2.lineTo(3.0f, 12.5f);
                path2.close();
                path.addPath(path2);
                path2.addRoundRect(new RectF(11.0f, 3.5f, 13.0f, 12.5f), 0.8f, 0.8f, Path.Direction.CW);
                break;
            case PREV /* 20 */:
                path2.moveTo(13.0f, 3.5f);
                path2.lineTo(6.0f, 8.0f);
                path2.lineTo(13.0f, 12.5f);
                path2.close();
                path.addPath(path2);
                path2.addRoundRect(new RectF(3.0f, 3.5f, 5.0f, 12.5f), 0.8f, 0.8f, Path.Direction.CW);
                break;
            case NOTE /* 21 */:
                path2.addOval(new RectF(4.0f, 9.4f, 9.4f, 13.6f), Path.Direction.CW);
                path.moveTo(9.0f, 11.5f);
                path.lineTo(9.0f, 2.5f);
                path.lineTo(12.8f, 4.2f);
                break;
        }
        Matrix matrix = new Matrix();
        matrix.setScale(this.size / 16.0f, this.size / 16.0f);
        path.transform(matrix);
        path2.transform(matrix);
        this.pl.setStyle(Paint.Style.STROKE);
        this.pl.setStrokeWidth((this.size * 1.4f) / 16.0f);
        this.pl.setStrokeCap(Paint.Cap.ROUND);
        this.pl.setStrokeJoin(Paint.Join.ROUND);
        this.ps.setStyle(Paint.Style.FILL);
        color(i3);
    }

    void color(int i) {
        this.pl.setColor(i);
        this.ps.setColor(i);
        invalidateSelf();
    }

    @Override // android.graphics.drawable.Drawable
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        canvas.save();
        canvas.translate(bounds.left + ((bounds.width() - this.size) / 2.0f), bounds.top + ((bounds.height() - this.size) / 2.0f));
        canvas.drawPath(this.line, this.pl);
        canvas.drawPath(this.solid, this.ps);
        canvas.restore();
    }

    @Override // android.graphics.drawable.Drawable
    public int getIntrinsicWidth() {
        return this.size;
    }

    @Override // android.graphics.drawable.Drawable
    public int getIntrinsicHeight() {
        return this.size;
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
