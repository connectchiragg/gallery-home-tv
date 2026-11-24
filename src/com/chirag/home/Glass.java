package com.chirag.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.View;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
final class Glass extends Drawable {
    static final float BEZEL_DP = 24.0f;
    static final int CACHE_BYTES = 2097152;
    static final float CENTER_BLUR = 0.0f;
    static final int CLEAR = 0;
    static final float DEPTH_DP = 24.0f;
    static final float DIM = 0.16f;
    static final float DISPERSION = 0.025f;
    static final float EDGE_SHADE = 0.22f;
    static final float FADE = 0.3f;
    static final float GAIN = 1.08f;
    static final int HALF_RES_ABOVE_DP = 120;
    static final float IOR = 1.5f;
    static final int LIT = 2;
    static final float RIM = 0.4f;
    static final float SAT = 1.15f;
    static final int SELECTED = 1;
    static final float VEIL = 0.05f;
    static final boolean WHOLE = true;
    private final float d;
    private float fade;
    private int[] geo;
    private View host;
    private Bitmap lens;
    private Bitmap next;
    private boolean quiet;
    private final float rad;
    private int token;
    private final Paint tint = new Paint(SELECTED);
    private final Paint veil = new Paint(SELECTED);
    private final Paint lit = new Paint(SELECTED);
    private final Paint rim = new Paint(SELECTED);
    private final Paint body = new Paint(3);
    private final Paint incoming = new Paint(3);
    private final Matrix m = new Matrix();
    private final RectF r = new RectF();
    private int mode = CLEAR;
    private float band = 1.0f;

    Glass(Context context, int i) {
        this.d = context.getResources().getDisplayMetrics().density;
        this.rad = i < 0 ? -1.0f : i * this.d;
        int iRound = Math.round(63.118824f);
        this.tint.setColor(iRound | (Math.round(51.50999f) << 24) | (iRound << 16) | (iRound << 8));
        this.veil.setColor(956301311);
        this.rim.setStyle(Paint.Style.STROKE);
        this.rim.setStrokeWidth(Math.max(1.0f, this.d * 0.5f));
    }

    Glass quiet(boolean z) {
        this.quiet = z;
        rebuild();
        return this;
    }

    Glass body(int i, float f) {
        this.tint.setColor(i);
        this.band = f;
        invalidateSelf();
        return this;
    }

    Glass mode(int i) {
        if (i == this.mode) {
            return this;
        }
        this.mode = i;
        rebuild();
        invalidateSelf();
        return this;
    }

    static Glass on(View view, final Glass glass) {
        view.setBackground(glass);
        glass.host = view;
        view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() { // from class: com.chirag.home.Glass$$ExternalSyntheticLambda1
            @Override // android.view.View.OnLayoutChangeListener
            public final void onLayoutChange(View view2, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                glass.place(view2);
            }
        });
        return glass;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void place(final View view) {
        Ui.Stage stage;
        int width;
        int height;
        int i;
        int i2;
        int height2;
        int i3;
        View view2 = view;
        int scrollX = CLEAR;
        int scrollY = scrollX;
        while (true) {
            if (view2 instanceof Ui.Stage) {
                stage = (Ui.Stage) view2;
                width = view2.getWidth();
                height = view2.getHeight();
                i = scrollX;
                i2 = scrollY;
                break;
            }
            int left = scrollX + view2.getLeft();
            int top = scrollY + view2.getTop();
            Object parent = view2.getParent();
            if (!(parent instanceof View)) {
                width = CLEAR;
                i = left;
                i2 = top;
                stage = null;
                height = width;
                break;
            }
            view2 = (View) parent;
            scrollX = left - view2.getScrollX();
            scrollY = top - view2.getScrollY();
        }
        if (stage != null) {
            height2 = height;
            i3 = width;
        } else {
            View rootView = view.getRootView();
            int width2 = rootView.getWidth();
            height2 = rootView.getHeight();
            i3 = width2;
        }
        if (i3 <= 0 || height2 <= 0 || view.getWidth() <= 0 || view.getHeight() <= 0) {
            return;
        }
        final int[] iArr = {i, i2, view.getWidth(), view.getHeight(), Math.round(this.rad < CENTER_BLUR ? Math.min(view.getWidth(), view.getHeight()) / 2.0f : Math.min(this.rad, Math.min(view.getWidth(), view.getHeight()) / 2.0f)), i3, height2};
        if (this.geo == null || !Arrays.equals(this.geo, iArr) || this.lens == null) {
            this.geo = iArr;
            if (stage != null) {
                stage.add(this);
            }
            final Bitmap bitmapCurrent = Art.current(view.getContext());
            final float fBoost = Art.boost();
            final String strKey = Lens.key(Art.index(), iArr);
            Bitmap bitmapCached = Lens.cached(strKey);
            if (bitmapCached != null) {
                show(bitmapCached);
                return;
            }
            final int i4 = this.token + SELECTED;
            this.token = i4;
            Lens.EXEC.execute(new Runnable() { // from class: com.chirag.home.Glass$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    m16lambda$place$2$comchiraghomeGlass(bitmapCurrent, fBoost, strKey, iArr, view, i4);
                }
            });
        }
    }

    /* JADX INFO: renamed from: lambda$place$2$com-chirag-home-Glass, reason: not valid java name */
    /* synthetic */ void m16lambda$place$2$comchiraghomeGlass(Bitmap bitmap, float f, String str, int[] iArr, View view, final int i) {
        final Bitmap bitmapMake = Lens.make(bitmap, f, str, iArr, this.d);
        if (bitmapMake != null) {
            view.post(new Runnable() { // from class: com.chirag.home.Glass$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    m15lambda$place$1$comchiraghomeGlass(i, bitmapMake);
                }
            });
        }
    }

    /* JADX INFO: renamed from: lambda$place$1$com-chirag-home-Glass, reason: not valid java name */
    /* synthetic */ void m15lambda$place$1$comchiraghomeGlass(int i, Bitmap bitmap) {
        if (i == this.token && this.next == null) {
            show(bitmap);
        }
    }

    int[] geo() {
        if (this.host == null || !this.host.isAttachedToWindow()) {
            return null;
        }
        return this.geo;
    }

    private BitmapShader shader(Bitmap bitmap) {
        BitmapShader bitmapShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        Rect bounds = getBounds();
        this.m.setScale(bounds.width() / bitmap.getWidth(), bounds.height() / bitmap.getHeight());
        this.m.postTranslate(bounds.left, bounds.top);
        bitmapShader.setLocalMatrix(this.m);
        return bitmapShader;
    }

    private void show(Bitmap bitmap) {
        this.lens = bitmap;
        this.next = null;
        this.fade = CENTER_BLUR;
        this.token += SELECTED;
        this.body.setShader(shader(bitmap));
        invalidateSelf();
    }

    void fadeTo(Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }
        this.next = bitmap;
        this.fade = CENTER_BLUR;
        this.token += SELECTED;
        this.incoming.setShader(shader(bitmap));
    }

    void fade(float f) {
        if (this.next != null) {
            this.fade = f;
            invalidateSelf();
        }
    }

    void settle() {
        if (this.next != null) {
            show(this.next);
        }
    }

    private static int white(float f) {
        return (Math.round(Math.max(CENTER_BLUR, Math.min(1.0f, f)) * 255.0f) << 24) | 16777215;
    }

    @Override // android.graphics.drawable.Drawable
    protected void onBoundsChange(Rect rect) {
        rebuild();
        if (this.lens != null) {
            this.body.setShader(shader(this.lens));
        }
        if (this.next != null) {
            this.incoming.setShader(shader(this.next));
        }
    }

    private void rebuild() {
        Rect bounds = getBounds();
        if (bounds.isEmpty()) {
            return;
        }
        float f = bounds.top;
        float f2 = bounds.bottom;
        float fExactCenterX = bounds.exactCenterX();
        float f3 = (this.quiet ? 0.55f : 1.0f) * RIM;
        float fMin = Math.min(bounds.height(), bounds.width()) * 0.2f;
        if (this.mode == LIT) {
            this.lit.setShader(new LinearGradient(fExactCenterX, f, fExactCenterX, f2, -1, -1447186, Shader.TileMode.CLAMP));
            this.rim.setShader(new LinearGradient(fExactCenterX - fMin, f, fExactCenterX + fMin, f2, -1, -2828580, Shader.TileMode.CLAMP));
        } else {
            this.rim.setShader(new LinearGradient(fExactCenterX - fMin, f, fExactCenterX + fMin, f2, new int[]{white(0.85f * f3), white(FADE * f3), white(DIM * f3), white(f3 * 0.55f)}, new float[]{CENTER_BLUR, 0.38f, 0.68f, 1.0f}, Shader.TileMode.CLAMP));
        }
    }

    private void round(Canvas canvas, Rect rect, float f, float f2, Paint paint) {
        this.r.set(rect.left + f, rect.top + f, rect.right - f, rect.bottom - f);
        float fMax = Math.max(CENTER_BLUR, f2 - f);
        canvas.drawRoundRect(this.r, fMax, fMax, paint);
    }

    @Override // android.graphics.drawable.Drawable
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        float fMin = this.rad < CENTER_BLUR ? Math.min(bounds.width(), bounds.height()) / 2.0f : this.rad;
        if (this.mode == LIT) {
            round(canvas, bounds, CENTER_BLUR, fMin, this.lit);
        } else {
            round(canvas, bounds, CENTER_BLUR, fMin, this.tint);
            int iRound = Math.round((this.quiet ? 150 : 255) * this.band);
            if (this.lens != null) {
                this.body.setAlpha(this.next != null ? Math.round(iRound * (1.0f - this.fade)) : iRound);
                round(canvas, bounds, CENTER_BLUR, fMin, this.body);
            }
            if (this.next != null) {
                this.incoming.setAlpha(Math.round(iRound * this.fade));
                round(canvas, bounds, CENTER_BLUR, fMin, this.incoming);
            }
            if (this.mode == SELECTED) {
                round(canvas, bounds, CENTER_BLUR, fMin, this.veil);
            }
        }
        round(canvas, bounds, this.rim.getStrokeWidth() / 2.0f, fMin, this.rim);
    }

    @Override // android.graphics.drawable.Drawable
    public void getOutline(Outline outline) {
        Rect bounds = getBounds();
        outline.setRoundRect(bounds, this.rad < CENTER_BLUR ? Math.min(bounds.width(), bounds.height()) / 2.0f : this.rad);
        outline.setAlpha(1.0f);
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
