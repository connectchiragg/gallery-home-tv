package com.chirag.home;

import android.graphics.Bitmap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/* JADX INFO: loaded from: classes.dex */
final class Lens {
    private static final int LUT = 128;
    private static int bytes;
    static final ExecutorService EXEC = Executors.newSingleThreadExecutor(new ThreadFactory() { // from class: com.chirag.home.Lens$$ExternalSyntheticLambda0
        @Override // java.util.concurrent.ThreadFactory
        public final Thread newThread(Runnable runnable) {
            return Lens.lambda$static$0(runnable);
        }
    });
    private static final LinkedHashMap<String, Bitmap> CACHE = new LinkedHashMap<>(64, 0.75f, true);

    Lens() {
    }

    static /* synthetic */ Thread lambda$static$0(Runnable runnable) {
        Thread thread = new Thread(runnable, "glass");
        thread.setPriority(2);
        thread.setDaemon(true);
        return thread;
    }

    static String key(int i, int[] iArr) {
        return i + ":" + iArr[0] + ":" + iArr[1] + ":" + iArr[2] + ":" + iArr[3] + ":" + iArr[4];
    }

    static synchronized Bitmap cached(String str) {
        return CACHE.get(str);
    }

    private static synchronized void put(String str, Bitmap bitmap) {
        Bitmap bitmapPut = CACHE.put(str, bitmap);
        if (bitmapPut != null) {
            bytes -= bitmapPut.getByteCount();
        }
        bytes += bitmap.getByteCount();
        Iterator<Map.Entry<String, Bitmap>> it = CACHE.entrySet().iterator();
        while (bytes > 2097152 && it.hasNext()) {
            Map.Entry<String, Bitmap> next = it.next();
            if (next.getValue() != bitmap) {
                bytes -= next.getValue().getByteCount();
                it.remove();
            }
        }
    }

    private static float[] bend(float f, float f2, float f3) {
        float[] fArr = new float[129];
        for (int i = 0; i <= LUT; i++) {
            double dMax = 1.0d - Math.max(1.0E-4d, ((double) i) / 128.0d);
            double d = dMax * dMax * dMax;
            double d2 = 1.0d - (dMax * d);
            double dPow = ((double) f) * Math.pow(d2, 0.25d);
            double dMin = Math.min(Math.atan(d * Math.pow(d2, -0.75d)), 1.55d);
            fArr[i] = (float) ((dPow + ((double) f2)) * Math.tan(dMin - Math.asin(Math.sin(dMin) / ((double) f3))));
        }
        return fArr;
    }

    private static float chan(int[] iArr, int i, int i2, float f, float f2, int i3) {
        if (f < 0.0f) {
            f = 0.0f;
        } else {
            float f3 = i - 1.001f;
            if (f > f3) {
                f = f3;
            }
        }
        if (f2 < 0.0f) {
            f2 = 0.0f;
        } else {
            float f4 = i2 - 1.001f;
            if (f2 > f4) {
                f2 = f4;
            }
        }
        int i4 = (int) f;
        int i5 = (int) f2;
        int i6 = (i5 * i) + i4;
        float f5 = f - i4;
        float f6 = f2 - i5;
        float f7 = (iArr[i6] >> i3) & 255;
        float f8 = (iArr[i6 + 1] >> i3) & 255;
        int i7 = i6 + i;
        float f9 = (iArr[i7] >> i3) & 255;
        return ((f7 + ((f8 - f7) * f5)) * (1.0f - f6)) + ((f9 + ((((iArr[i7 + 1] >> i3) & 255) - f9) * f5)) * f6);
    }

    static Bitmap make(Bitmap bitmap, float f, String str, int[] iArr, float f2) {
        int i;
        int i2;
        float f3;
        float f4;
        float f5;
        float f6;
        int i3;
        float f7;
        int i4;
        float f8;
        float fSqrt;
        float f9;
        float f10;
        float f11;
        Bitmap bitmapCached = cached(str);
        if (bitmapCached != null) {
            return bitmapCached;
        }
        try {
            int i5 = iArr[0];
            int i6 = iArr[1];
            int i7 = iArr[2];
            int i8 = iArr[3];
            int i9 = iArr[5];
            int i10 = iArr[6];
            float f12 = iArr[4];
            if (i7 > 0 && i8 > 0 && i9 > 0 && i10 > 0 && bitmap != null) {
                float f13 = ((float) Math.max(i7, i8)) > 120.0f * f2 ? 0.5f : 1.0f;
                float f14 = i7;
                int iCeil = (int) Math.ceil(f14 * f13);
                float f15 = i8;
                int iCeil2 = (int) Math.ceil(f15 * f13);
                float f16 = f14 / 2.0f;
                float f17 = f15 / 2.0f;
                float f18 = 24.0f * f2;
                float fMax = Math.max(Math.min(f18, f12), Math.min(f16, f17));
                if (fMax < 1.0f) {
                    return null;
                }
                float[] fArrBend = bend(fMax, f18, 1.4625001f);
                float[] fArrBend2 = bend(fMax, f18, 1.5f);
                float[] fArrBend3 = bend(fMax, f18, 1.5374999f);
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                float f19 = i9;
                float f20 = width / f19;
                float f21 = i10;
                float f22 = height / f21;
                float f23 = i5;
                int iMax = Math.max(0, Math.min(width - 2, ((int) Math.floor(f23 * f20)) - 1));
                float f24 = i6;
                int iMax2 = Math.max(0, Math.min(height - 2, ((int) Math.floor(f24 * f22)) - 1));
                int iMax3 = (Math.max(iMax + 1, Math.min(width - 1, ((int) Math.ceil((i5 + i7) * f20)) + 1)) - iMax) + 1;
                int iMax4 = (Math.max(iMax2 + 1, Math.min(height - 1, ((int) Math.ceil((i6 + i8) * f22)) + 1)) - iMax2) + 1;
                int[] iArr2 = new int[iMax3 * iMax4];
                float f25 = f17;
                int i11 = iMax;
                bitmap.getPixels(iArr2, 0, iMax3, iMax, iMax2, iMax3, iMax4);
                float fHypot = ((float) Math.hypot(i9, i10)) * 0.62f;
                int i12 = iCeil2;
                int i13 = iCeil;
                int[] iArr3 = new int[i13 * i12];
                int i14 = 0;
                while (i14 < i12) {
                    float f26 = ((i14 + 0.5f) / f13) - f25;
                    float fAbs = Math.abs(f26) - (f25 - f12);
                    float f27 = f26 < 0.0f ? -1.0f : 1.0f;
                    int i15 = i12;
                    int i16 = 0;
                    while (i16 < i13) {
                        int[] iArr4 = iArr3;
                        float f28 = ((i16 + 0.5f) / f13) - f16;
                        float fAbs2 = Math.abs(f28) - (f16 - f12);
                        float f29 = f28 < 0.0f ? -1.0f : 1.0f;
                        if (fAbs2 <= 0.0f || fAbs <= 0.0f) {
                            i = i13;
                            i2 = i16;
                            if (fAbs2 > fAbs) {
                                f3 = f12 - fAbs2;
                                f4 = f29;
                                f5 = 0.0f;
                            } else {
                                f3 = f12 - fAbs;
                                f4 = 0.0f;
                                f5 = f27;
                            }
                            if (f3 >= fMax) {
                                f7 = fHypot;
                                i3 = i14;
                                i4 = i11;
                            } else {
                                if (f3 < 0.0f) {
                                    f6 = 0.0f;
                                } else {
                                    f6 = f3 / fMax;
                                }
                                float f30 = 1.0f - f6;
                                float f31 = f6 * 128.0f;
                                int i17 = (int) f31;
                                i3 = i14;
                                f7 = fHypot;
                                int iMin = Math.min(LUT, i17 + 1);
                                float f32 = f31 - i17;
                                float f33 = f28 + f23 + f16;
                                float f34 = f24 + f26 + f25;
                                float f35 = fArrBend[i17] + ((fArrBend[iMin] - fArrBend[i17]) * f32);
                                float f36 = fArrBend2[i17] + ((fArrBend2[iMin] - fArrBend2[i17]) * f32);
                                float f37 = fArrBend3[i17] + ((fArrBend3[iMin] - fArrBend3[i17]) * f32);
                                i4 = i11;
                                float f38 = i4;
                                float f39 = iMax2;
                                float fChan = chan(iArr2, iMax3, iMax4, (((f33 - (f4 * f35)) * f20) - 0.5f) - f38, (((f34 - (f35 * f5)) * f22) - 0.5f) - f39, 16);
                                float f40 = f33 - (f4 * f36);
                                f8 = f34 - (f36 * f5);
                                float fChan2 = chan(iArr2, iMax3, iMax4, ((f40 * f20) - 0.5f) - f38, ((f8 * f22) - 0.5f) - f39, 8);
                                float fChan3 = chan(iArr2, iMax3, iMax4, (((f33 - (f4 * f37)) * f20) - 0.5f) - f38, (((f34 - (f5 * f37)) * f22) - 0.5f) - f39, 0);
                                float f41 = f40 - (f19 * 0.5f);
                                float f42 = f8 - (f21 * 0.42f);
                                fSqrt = ((float) Math.sqrt((f41 * f41) + (f42 * f42))) / f7;
                                if (fSqrt <= 0.5f) {
                                    f9 = 0.0f;
                                } else if (fSqrt >= 1.0f) {
                                    f9 = 1.0f;
                                } else {
                                    f9 = (fSqrt - 0.5f) / 0.5f;
                                }
                                if (f8 < 0.0f) {
                                    f10 = 0.0f;
                                } else if (f8 > f21) {
                                    f10 = 1.0f;
                                } else {
                                    f10 = f8 / f21;
                                }
                                float fScrim = (1.0f - Wall.scrim(f10, f)) * (1.0f - (f9 * 0.45f));
                                float f43 = ((fChan * 1.0f) + 0.0f) * fScrim;
                                float f44 = ((fChan2 * 1.0f) + 0.0f) * fScrim;
                                float f45 = ((fChan3 * 1.0f) + 0.0f) * fScrim;
                                float f46 = (f43 * 0.3f) + (0.59f * f44) + (0.11f * f45);
                                float f47 = f30 * f30;
                                float f48 = (1.0f - (((0.22f * f47) * f47) * f47)) * 1.08f;
                                int iClamp = clamp((((f43 - f46) * 1.15f) + f46) * f48);
                                int iClamp2 = clamp((f46 + ((f44 - f46) * 1.15f)) * f48);
                                int iClamp3 = clamp((f46 + ((f45 - f46) * 1.15f)) * f48);
                                f11 = f30 / 0.3f;
                                if (f11 > 1.0f) {
                                    f11 = 1.0f;
                                }
                                iArr4[(i3 * i) + i2] = iClamp3 | (Math.round(((f11 * f11) * (3.0f - (f11 * 2.0f))) * 255.0f) << 24) | (iClamp << 16) | (iClamp2 << 8);
                            }
                            i16 = i2 + 1;
                            f25 = f25;
                            i13 = i;
                            i11 = i4;
                            iArr3 = iArr4;
                            i14 = i3;
                            fHypot = f7;
                            f23 = f23;
                            fAbs = fAbs;
                        } else {
                            i2 = i16;
                            i = i13;
                            float fSqrt2 = (float) Math.sqrt((fAbs2 * fAbs2) + (fAbs * fAbs));
                            if (fSqrt2 < 0.001f) {
                                fSqrt2 = 0.001f;
                            }
                            f3 = f12 - fSqrt2;
                            f29 *= fAbs2 / fSqrt2;
                            f5 = (fAbs / fSqrt2) * f27;
                        }
                        f4 = f29;
                        if (f3 >= fMax) {
                            f7 = fHypot;
                            i3 = i14;
                            i4 = i11;
                        } else {
                            if (f3 < 0.0f) {
                                f6 = 0.0f;
                            } else {
                                f6 = f3 / fMax;
                            }
                            float f310 = 1.0f - f6;
                            float f311 = f6 * 128.0f;
                            int i18 = (int) f311;
                            i3 = i14;
                            f7 = fHypot;
                            int iMin2 = Math.min(LUT, i18 + 1);
                            float f312 = f311 - i18;
                            float f313 = f28 + f23 + f16;
                            float f314 = f24 + f26 + f25;
                            float f315 = fArrBend[i18] + ((fArrBend[iMin2] - fArrBend[i18]) * f312);
                            float f316 = fArrBend2[i18] + ((fArrBend2[iMin2] - fArrBend2[i18]) * f312);
                            float f317 = fArrBend3[i18] + ((fArrBend3[iMin2] - fArrBend3[i18]) * f312);
                            i4 = i11;
                            float f318 = i4;
                            float f319 = iMax2;
                            float fChan4 = chan(iArr2, iMax3, iMax4, (((f313 - (f4 * f315)) * f20) - 0.5f) - f318, (((f314 - (f315 * f5)) * f22) - 0.5f) - f319, 16);
                            float f49 = f313 - (f4 * f316);
                            f8 = f314 - (f316 * f5);
                            float fChan5 = chan(iArr2, iMax3, iMax4, ((f49 * f20) - 0.5f) - f318, ((f8 * f22) - 0.5f) - f319, 8);
                            float fChan6 = chan(iArr2, iMax3, iMax4, (((f313 - (f4 * f317)) * f20) - 0.5f) - f318, (((f314 - (f5 * f317)) * f22) - 0.5f) - f319, 0);
                            float f410 = f49 - (f19 * 0.5f);
                            float f411 = f8 - (f21 * 0.42f);
                            fSqrt = ((float) Math.sqrt((f410 * f410) + (f411 * f411))) / f7;
                            if (fSqrt <= 0.5f) {
                                f9 = 0.0f;
                            } else if (fSqrt >= 1.0f) {
                                f9 = 1.0f;
                            } else {
                                f9 = (fSqrt - 0.5f) / 0.5f;
                            }
                            if (f8 < 0.0f) {
                                f10 = 0.0f;
                            } else if (f8 > f21) {
                                f10 = 1.0f;
                            } else {
                                f10 = f8 / f21;
                            }
                            float fScrim2 = (1.0f - Wall.scrim(f10, f)) * (1.0f - (f9 * 0.45f));
                            float f412 = ((fChan4 * 1.0f) + 0.0f) * fScrim2;
                            float f413 = ((fChan5 * 1.0f) + 0.0f) * fScrim2;
                            float f414 = ((fChan6 * 1.0f) + 0.0f) * fScrim2;
                            float f415 = (f412 * 0.3f) + (0.59f * f413) + (0.11f * f414);
                            float f416 = f310 * f310;
                            float f417 = (1.0f - (((0.22f * f416) * f416) * f416)) * 1.08f;
                            int iClamp4 = clamp((((f412 - f415) * 1.15f) + f415) * f417);
                            int iClamp5 = clamp((f415 + ((f413 - f415) * 1.15f)) * f417);
                            int iClamp6 = clamp((f415 + ((f414 - f415) * 1.15f)) * f417);
                            f11 = f310 / 0.3f;
                            if (f11 > 1.0f) {
                                f11 = 1.0f;
                            }
                            iArr4[(i3 * i) + i2] = iClamp6 | (Math.round(((f11 * f11) * (3.0f - (f11 * 2.0f))) * 255.0f) << 24) | (iClamp4 << 16) | (iClamp5 << 8);
                        }
                        i16 = i2 + 1;
                        f25 = f25;
                        i13 = i;
                        i11 = i4;
                        iArr3 = iArr4;
                        i14 = i3;
                        fHypot = f7;
                        f23 = f23;
                        fAbs = fAbs;
                    }
                    i14++;
                    i13 = i13;
                    i11 = i11;
                    i12 = i15;
                }
                Bitmap bitmapCreateBitmap = Bitmap.createBitmap(iArr3, i13, i12, Bitmap.Config.ARGB_8888);
                put(str, bitmapCreateBitmap);
                return bitmapCreateBitmap;
            }
            return null;
        } catch (Throwable th) {
            return null;
        }
    }

    private static int clamp(float f) {
        if (f <= 0.0f) {
            return 0;
        }
        if (f >= 255.0f) {
            return 255;
        }
        return Math.round(f);
    }
}
