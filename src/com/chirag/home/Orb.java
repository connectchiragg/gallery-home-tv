package com.chirag.home;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewOutlineProvider;
import java.util.HashMap;
import java.util.Locale;

/* JADX INFO: loaded from: classes.dex */
final class Orb extends View {
    static final int PLATE = -15065820;
    static final int PLATE_LIGHT = -1446671;
    static final float RENDER = 1.25f;
    private final Bitmap bmp;
    private final RectF dst;
    private final Paint pic;
    private static final HashMap<String, Bitmap> CACHE = new HashMap<>();
    private static final ColorFilter REST = new LightingColorFilter(-1118482, 0);
    private static final ColorFilter LIT = new LightingColorFilter(-1, 1184274);

    Orb(Context context, String str, int i, boolean z) {
        super(context);
        this.pic = new Paint(3);
        this.dst = new RectF();
        this.bmp = art(context, str, Math.round(i * RENDER), z);
        setOutlineProvider(new ViewOutlineProvider() { // from class: com.chirag.home.Orb.1
            @Override // android.view.ViewOutlineProvider
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
    }

    void lit(boolean z) {
        this.pic.setColorFilter(z ? LIT : REST);
        invalidate();
    }

    @Override // android.view.View
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        this.dst.set(0.0f, 0.0f, i, i2);
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(this.bmp, (Rect) null, this.dst, this.pic);
    }

    static Bitmap art(Context context, String str, int i, boolean z) {
        Drawable drawableLoadUnbadgedIcon;
        Drawable drawableBanner;
        Bitmap bitmap = CACHE.get(str);
        if (bitmap != null) {
            return bitmap;
        }
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i, i, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        PackageManager packageManager = context.getPackageManager();
        Drawable applicationIcon = null;
        boolean zPlain = false;
        try {
            Intent intentLaunchIntent = Ui.launchIntent(context, str);
            ActivityInfo activityInfo = (intentLaunchIntent == null || intentLaunchIntent.getComponent() == null) ? null : packageManager.getActivityInfo(intentLaunchIntent.getComponent(), 0);
            if (activityInfo != null ? activityInfo.getIconResource() != 0 : packageManager.getApplicationInfo(str, 0).icon != 0) {
                drawableLoadUnbadgedIcon = activityInfo != null ? activityInfo.loadUnbadgedIcon(packageManager) : packageManager.getApplicationInfo(str, 0).loadUnbadgedIcon(packageManager);
            } else {
                drawableLoadUnbadgedIcon = null;
            }
        } catch (Throwable th) {
            drawableLoadUnbadgedIcon = null;
        }
        try {
            if (drawableLoadUnbadgedIcon instanceof AdaptiveIconDrawable) {
                AdaptiveIconDrawable adaptiveIconDrawable = (AdaptiveIconDrawable) drawableLoadUnbadgedIcon;
                int iRound = Math.round(i * 0.25f);
                plate(canvas, i, false);
                Drawable background = adaptiveIconDrawable.getBackground();
                Drawable foreground = adaptiveIconDrawable.getForeground();
                if (background != null) {
                    int i2 = -iRound;
                    int i3 = i + iRound;
                    background.setBounds(i2, i2, i3, i3);
                    background.draw(canvas);
                }
                if (foreground != null) {
                    int i4 = -iRound;
                    int i5 = iRound + i;
                    foreground.setBounds(i4, i4, i5, i5);
                    foreground.draw(canvas);
                }
                if (background != null || foreground != null) {
                    zPlain = true;
                }
            } else if (drawableLoadUnbadgedIcon != null) {
                zPlain = plain(canvas, drawableLoadUnbadgedIcon, i);
            }
            if (!zPlain && (drawableBanner = Ui.banner(context, str)) != null) {
                zPlain = banner(canvas, drawableBanner, i);
            }
            if (!zPlain) {
                try {
                    applicationIcon = packageManager.getApplicationIcon(str);
                } catch (Throwable th2) {
                }
                if (applicationIcon != null && !(applicationIcon instanceof AdaptiveIconDrawable)) {
                    zPlain = plain(canvas, applicationIcon, i);
                }
            }
        } catch (Throwable th3) {
        }
        if (!zPlain) {
            monogram(canvas, Ui.label(context, str), i);
        }
        Paint paint = new Paint();
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));
        float f = i;
        paint.setShader(new LinearGradient(0.0f, 0.0f, 0.0f, f, -13355461, -15724268, Shader.TileMode.CLAMP));
        canvas.drawPaint(paint);
        Bitmap bitmapCreateBitmap2 = Bitmap.createBitmap(i, i, Bitmap.Config.ARGB_8888);
        Canvas canvas2 = new Canvas(bitmapCreateBitmap2);
        Paint paint2 = new Paint(1);
        paint2.setShader(new BitmapShader(bitmapCreateBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        float f2 = f / 2.0f;
        canvas2.drawCircle(f2, f2, f2, paint2);
        bitmapCreateBitmap.recycle();
        if (z) {
            CACHE.put(str, bitmapCreateBitmap2);
        }
        return bitmapCreateBitmap2;
    }

    private static void plate(Canvas canvas, int i, boolean z) {
        Paint paint = new Paint();
        paint.setShader(new LinearGradient(0.0f, 0.0f, 0.0f, i, z ? -1 : -13355461, z ? -1644308 : -15724268, Shader.TileMode.CLAMP));
        canvas.drawPaint(paint);
    }

    private static int[] thumb(Drawable drawable, int i, int i2) {
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i, i2, Bitmap.Config.ARGB_8888);
        drawable.setBounds(0, 0, i, i2);
        drawable.draw(new Canvas(bitmapCreateBitmap));
        int[] iArr = new int[i * i2];
        bitmapCreateBitmap.getPixels(iArr, 0, i, 0, 0, i, i2);
        bitmapCreateBitmap.recycle();
        return iArr;
    }

    private static boolean near(int i, int i2, int i3) {
        return Math.abs(Color.red(i) - Color.red(i2)) <= i3 && Math.abs(Color.green(i) - Color.green(i2)) <= i3 && Math.abs(Color.blue(i) - Color.blue(i2)) <= i3;
    }

    private static int average(int[] iArr) {
        int iRed = 0;
        int iGreen = 0;
        int iBlue = 0;
        for (int i : iArr) {
            iRed += Color.red(i);
            iGreen += Color.green(i);
            iBlue += Color.blue(i);
        }
        return Color.rgb(iRed / iArr.length, iGreen / iArr.length, iBlue / iArr.length);
    }

    private static boolean plain(Canvas canvas, Drawable drawable, int i) {
        int i2;
        float fMin;
        int i3 = 48;
        int[] iArrThumb = thumb(drawable, 48, 48);
        int i4 = -1;
        int i5 = 48;
        int i6 = 48;
        long jRed = 0;
        int i7 = 0;
        int i8 = 0;
        int i9 = -1;
        while (i7 < i3) {
            int i10 = 0;
            while (i10 < i3) {
                int i11 = iArrThumb[(i7 * 48) + i10];
                if (Color.alpha(i11) > i3) {
                    if (i10 < i5) {
                        i5 = i10;
                    }
                    if (i10 > i4) {
                        i4 = i10;
                    }
                    if (i7 < i6) {
                        i6 = i7;
                    }
                    if (i7 > i9) {
                        i9 = i7;
                    }
                    jRed += (long) ((((Color.red(i11) * 3) + (Color.green(i11) * 6)) + Color.blue(i11)) / 10);
                    i8++;
                }
                i10++;
                iArrThumb = iArrThumb;
                i3 = 48;
            }
            i7++;
            i3 = 48;
        }
        int[] iArr = iArrThumb;
        if (i4 < 0) {
            return false;
        }
        float f = (i4 - i5) + 1;
        float f2 = (i9 - i6) + 1;
        float fMax = Math.max(f, f2);
        int i12 = (int) (f2 * 0.08f);
        float f3 = f / 2.0f;
        int i13 = (int) f3;
        float f4 = f2 / 2.0f;
        int i14 = (((int) f4) + i6) * 48;
        int i15 = (int) (f * 0.08f);
        long j = jRed;
        int[] iArr2 = {iArr[((i6 + i12) * 48) + i5 + i13], iArr[(i14 + i4) - i15], iArr[((i9 - i12) * 48) + i5 + i13], iArr[i14 + i5 + i15]};
        int i16 = (int) (f2 * 0.22f);
        int i17 = (i6 + i16) * 48;
        int i18 = (int) (0.22f * f);
        int i19 = (i9 - i16) * 48;
        int[] iArr3 = {iArr[i17 + i5 + i18], iArr[(i17 + i4) - i18], iArr[i19 + i5 + i18], iArr[(i19 + i4) - i18]};
        float f5 = f / f2;
        boolean z = f5 > 0.8f && f5 < RENDER;
        for (int i20 = 0; i20 < 4; i20++) {
            z &= Color.alpha(iArr2[i20]) > 230;
        }
        for (int i21 = 0; i21 < 4; i21++) {
            z &= Color.alpha(iArr3[i21]) > 230;
        }
        if (!z) {
            i2 = i;
            plate(canvas, i2, i8 > 0 && j / ((long) i8) < 70);
            fMin = 0.58f;
        } else {
            int iAverage = average(iArr2);
            for (int i22 = 0; i22 < 4; i22++) {
                near(iArr2[i22], iAverage, 28);
            }
            boolean z2 = Color.alpha(iArr[(((((int) (f2 * 0.1f)) + i6) * 48) + i5) + ((int) (0.1f * f))]) < 128;
            canvas.drawColor((-16777216) | iAverage);
            fMin = z2 ? 1.04f : (1.05f * fMax) / Math.min(f, f2);
            i2 = i;
        }
        float f6 = i2;
        float f7 = ((fMin * f6) * 48.0f) / fMax;
        float f8 = f6 / 2.0f;
        float f9 = f8 - (((i5 + f3) / 48.0f) * f7);
        float f10 = f8 - (((i6 + f4) / 48.0f) * f7);
        drawable.setBounds(Math.round(f9), Math.round(f10), Math.round(f9 + f7), Math.round(f10 + f7));
        drawable.draw(canvas);
        return true;
    }

    private static boolean banner(Canvas canvas, Drawable drawable, int i) {
        int[] iArrThumb = thumb(drawable, 32, 18);
        int[] iArr = new int[96];
        int i2 = 0;
        for (int i3 = 0; i3 < 32; i3++) {
            int i4 = i2 + 1;
            iArr[i2] = iArrThumb[i3];
            i2 = i4 + 1;
            iArr[i4] = iArrThumb[i3 + 544];
        }
        for (int i5 = 1; i5 < 17; i5++) {
            int i6 = i2 + 1;
            int i7 = i5 * 32;
            iArr[i2] = iArrThumb[i7];
            i2 = i6 + 1;
            iArr[i6] = iArrThumb[(i7 + 32) - 1];
        }
        int iAverage = average(iArr);
        int i8 = 0;
        for (int i9 = 0; i9 < 96; i9++) {
            int i10 = iArr[i9];
            if (Color.alpha(i10) > 200 && near(i10, iAverage, 16)) {
                i8++;
            }
        }
        if (i8 >= 96 * 0.95f) {
            canvas.drawColor(iAverage | (-16777216));
            int iRound = Math.round(i * 0.88f);
            int i11 = (iRound * 9) / 16;
            int i12 = (i - iRound) / 2;
            int i13 = (i - i11) / 2;
            drawable.setBounds(i12, i13, iRound + i12, i11 + i13);
        } else {
            int i14 = (i * 16) / 9;
            canvas.drawColor(PLATE);
            int i15 = (i - i14) / 2;
            drawable.setBounds(i15, 0, i14 + i15, i);
        }
        drawable.draw(canvas);
        return true;
    }

    private static void monogram(Canvas canvas, String str, int i) {
        plate(canvas, i, false);
        Paint paint = new Paint(1);
        paint.setColor(-1);
        paint.setTypeface(Typeface.create("sans-serif-light", 0));
        float f = i;
        paint.setTextSize(0.46f * f);
        paint.setTextAlign(Paint.Align.CENTER);
        float f2 = f / 2.0f;
        canvas.drawText((str == null || str.isEmpty()) ? "?" : str.substring(0, 1).toUpperCase(Locale.ENGLISH), f2, f2 - ((paint.ascent() + paint.descent()) / 2.0f), paint);
    }
}
