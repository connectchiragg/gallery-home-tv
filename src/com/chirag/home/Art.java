package com.chirag.home;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Random;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
final class Art {
    static final String CATALOG = "catalog.json";
    static final String DIR = "art";
    static final int FADE_MS = 2500;
    static final int MIN_EXTERNAL = 3;
    static final long PERIOD_MS = 30000;
    private static Bitmap current;
    private static int cursor;
    private static int generation;
    private static boolean looked;
    private static int shownId;
    private static Item shownItem;
    static long since;
    static final int[] RES = {R.drawable.monet, R.drawable.art_02, R.drawable.art_03, R.drawable.art_04, R.drawable.art_05, R.drawable.art_06, R.drawable.art_07, R.drawable.art_08, R.drawable.art_09, R.drawable.art_10, R.drawable.art_11, R.drawable.art_12, R.drawable.art_13};
    static final String[][] INFO = {new String[]{"Water Lilies", "Claude Monet", "1906"}, new String[]{"The Starry Night", "Vincent van Gogh", "1889"}, new String[]{"Wheat Field with Cypresses", "Vincent van Gogh", "1889"}, new String[]{"Impression, Sunrise", "Claude Monet", "1872"}, new String[]{"The Fighting Temeraire", "J. M. W. Turner", "1839"}, new String[]{"View of Delft", "Johannes Vermeer", "c. 1660"}, new String[]{"The Hay Wain", "John Constable", "1821"}, new String[]{"The Hunters in the Snow", "Pieter Bruegel the Elder", "1565"}, new String[]{"A Sunday on La Grande Jatte", "Georges Seurat", "1884"}, new String[]{"Boulevard Montmartre, Spring", "Camille Pissarro", "1897"}, new String[]{"Bal du moulin de la Galette", "Pierre-Auguste Renoir", "1876"}, new String[]{"Mont Sainte-Victoire", "Paul Cézanne", "c. 1904"}, new String[]{"The Birth of Venus", "Sandro Botticelli", "c. 1485"}};
    private static final Random RND = new Random();
    private static Item[] items = builtIn();
    private static int[] order = shuffle(items.length, -1);
    private static final ArrayList<Item> trail = new ArrayList<>();
    private static final ArrayList<Integer> trailIds = new ArrayList<>();
    private static int trailAt = -1;
    private static float boost = 1.0f;

    Art() {
    }

    static long fadeMs() {
        float durationScale = Build.VERSION.SDK_INT >= 33 ? ValueAnimator.getDurationScale() : 1.0f;
        if (durationScale > 0.05f) {
            return Math.round(2500.0f / durationScale);
        }
        return 2500L;
    }

    static final class Item {
        final String artist;
        final File file;
        final int res;
        final String title;
        final String year;
        String museum = "";
        String about = "";

        Item(int i, File file, String str, String str2, String str3) {
            this.res = i;
            this.file = file;
            this.title = str;
            this.artist = str2;
            this.year = str3;
        }
    }

    static final class Pick {
        int back = -1;
        final Bitmap bitmap;
        final float boost;
        final int id;
        final Item item;

        Pick(int i, Item item, Bitmap bitmap, float f) {
            this.id = i;
            this.item = item;
            this.bitmap = bitmap;
            this.boost = f;
        }
    }

    static synchronized Item shown() {
        return shownItem;
    }

    static Pick previous(Context context) {
        synchronized (Art.class) {
            if (trailAt <= 0) {
                return null;
            }
            int i = trailAt - 1;
            Item item = trail.get(i);
            int iIntValue = trailIds.get(i).intValue();
            Bitmap bitmapDecode = decode(context, item);
            if (bitmapDecode == null) {
                return null;
            }
            Pick pick = new Pick(iIntValue, item, bitmapDecode, boost(bitmapDecode));
            pick.back = i;
            return pick;
        }
    }

    private static Item[] builtIn() {
        int length = RES.length;
        Item[] itemArr = new Item[length];
        for (int i = 0; i < length; i++) {
            itemArr[i] = new Item(RES[i], null, INFO[i][0], INFO[i][1], INFO[i][2]);
        }
        return itemArr;
    }

    private static int[] shuffle(int i, int i2) {
        int[] iArr = new int[i];
        for (int i3 = 0; i3 < i; i3++) {
            iArr[i3] = i3;
        }
        int i4 = i - 1;
        for (int i5 = i4; i5 > 0; i5--) {
            int iNextInt = RND.nextInt(i5 + 1);
            int i6 = iArr[i5];
            iArr[i5] = iArr[iNextInt];
            iArr[iNextInt] = i6;
        }
        if (i > 1 && iArr[0] == i2) {
            int iNextInt2 = RND.nextInt(i4) + 1;
            int i7 = iArr[0];
            iArr[0] = iArr[iNextInt2];
            iArr[iNextInt2] = i7;
        }
        return iArr;
    }

    private static int id(int i) {
        return (generation * 100000) + i;
    }

    static synchronized int index() {
        return shownId;
    }

    static synchronized float boost() {
        return boost;
    }

    static synchronized String caption() {
        return shownItem == null ? "" : caption(shownItem);
    }

    private static String caption(Item item) {
        String str = "";
        String str2 = item.artist.isEmpty() ? "" : "  ·  " + item.artist;
        StringBuilder sbAppend = new StringBuilder().append(item.title).append(str2);
        if (!item.year.isEmpty()) {
            str = (str2.isEmpty() ? "  ·  " : ", ") + item.year;
        }
        return sbAppend.append(str).toString();
    }

    static synchronized Bitmap current(Context context) {
        if (current == null) {
            for (int i = 0; i < items.length && current == null; i++) {
                current = decode(context, items[order[cursor]]);
                if (current == null) {
                    cursor = (cursor + 1) % order.length;
                }
            }
            shownItem = items[order[cursor]];
            shownId = id(order[cursor]);
            trail.add(shownItem);
            trailIds.add(Integer.valueOf(shownId));
            trailAt = 0;
            boost = boost(current);
            since = SystemClock.uptimeMillis();
        }
        if (!looked) {
            looked = true;
            final Context applicationContext = context.getApplicationContext();
            Lens.EXEC.execute(new Runnable() { // from class: com.chirag.home.Art.1
                @Override // java.lang.Runnable
                public void run() {
                    Art.lookForExternal(applicationContext);
                }
            });
        }
        return current;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void lookForExternal(Context context) {
        try {
            File externalFilesDir = context.getExternalFilesDir(null);
            if (externalFilesDir == null) {
                return;
            }
            File file = new File(externalFilesDir, DIR);
            File file2 = new File(file, CATALOG);
            if (file2.isFile() && file2.length() <= 4194304) {
                int length = (int) file2.length();
                byte[] bArr = new byte[length];
                FileInputStream fileInputStream = new FileInputStream(file2);
                int i = 0;
                while (i < length) {
                    try {
                        int i2 = fileInputStream.read(bArr, i, length - i);
                        if (i2 < 0) {
                            break;
                        } else {
                            i += i2;
                        }
                    } catch (Throwable th) {
                        fileInputStream.close();
                        throw th;
                    }
                }
                fileInputStream.close();
                JSONArray jSONArray = new JSONArray(new String(bArr, "UTF-8"));
                ArrayList arrayList = new ArrayList();
                for (int i3 = 0; i3 < jSONArray.length(); i3++) {
                    JSONObject jSONObjectOptJSONObject = jSONArray.optJSONObject(i3);
                    if (jSONObjectOptJSONObject != null) {
                        String strOptString = jSONObjectOptJSONObject.optString("file", "");
                        if (!strOptString.isEmpty() && !strOptString.contains("/") && !strOptString.contains("..")) {
                            File file3 = new File(file, strOptString);
                            if (file3.isFile() && file3.length() != 0) {
                                Item item = new Item(0, file3, jSONObjectOptJSONObject.optString("title", "").trim(), jSONObjectOptJSONObject.optString("artist", "").trim(), jSONObjectOptJSONObject.optString("year", "").trim());
                                item.museum = jSONObjectOptJSONObject.optString("museum", "").trim();
                                item.about = jSONObjectOptJSONObject.optString("about", "").trim();
                                arrayList.add(item);
                            }
                        }
                    }
                }
                if (arrayList.size() < MIN_EXTERNAL) {
                    return;
                }
                synchronized (Art.class) {
                    Item item2 = shownItem != null ? shownItem : items[order[cursor]];
                    for (int i4 = 0; i4 < arrayList.size(); i4++) {
                        if (((Item) arrayList.get(i4)).title.equalsIgnoreCase(item2.title)) {
                            if (item2.museum.isEmpty()) {
                                item2.museum = ((Item) arrayList.get(i4)).museum;
                            }
                            if (item2.about.isEmpty()) {
                                item2.about = ((Item) arrayList.get(i4)).about;
                            }
                            arrayList.remove(i4);
                            break;
                        }
                    }
                    int size = arrayList.size() + 1;
                    Item[] itemArr = new Item[size];
                    itemArr[0] = item2;
                    int i5 = 0;
                    while (i5 < arrayList.size()) {
                        int i6 = i5 + 1;
                        itemArr[i6] = (Item) arrayList.get(i5);
                        i5 = i6;
                    }
                    items = itemArr;
                    generation = 1;
                    order = shuffle(size, -1);
                    for (int i7 = 0; i7 < order.length; i7++) {
                        if (order[i7] == 0) {
                            order[i7] = order[0];
                            order[0] = 0;
                            break;
                        }
                    }
                    cursor = 0;
                    if (shownItem == item2) {
                        shownId = id(0);
                    }
                }
                Log.i("Art", "external collection: " + arrayList.size() + " paintings in " + file);
            }
        } catch (Throwable th2) {
            Log.w("Art", "external collection not used: " + th2);
        }
    }

    static Pick next(Context context) {
        int i;
        Item item;
        int iIntValue;
        int i2;
        Item item2;
        Bitmap bitmapDecode;
        synchronized (Art.class) {
            if (trailAt < 0 || trailAt >= trail.size() - 1) {
                i = -1;
                item = null;
                iIntValue = 0;
            } else {
                i = trailAt + 1;
                item = trail.get(i);
                iIntValue = trailIds.get(i).intValue();
            }
        }
        if (item != null && (bitmapDecode = decode(context, item)) != null) {
            Pick pick = new Pick(iIntValue, item, bitmapDecode, boost(bitmapDecode));
            pick.back = i;
            return pick;
        }
        for (int i3 = 0; i3 < 8; i3++) {
            synchronized (Art.class) {
                cursor++;
                if (cursor >= order.length) {
                    order = shuffle(items.length, order[order.length - 1]);
                    cursor = 0;
                }
                i2 = order[cursor];
                item2 = items[i2];
            }
            Bitmap bitmapDecode2 = decode(context, item2);
            if (bitmapDecode2 != null) {
                return new Pick(id(i2), item2, bitmapDecode2, boost(bitmapDecode2));
            }
        }
        return null;
    }

    static synchronized void show(Pick pick) {
        current = pick.bitmap;
        boost = pick.boost;
        shownItem = pick.item;
        shownId = pick.id;
        if (pick.back < 0 || pick.back >= trail.size()) {
            trail.add(pick.item);
            trailIds.add(Integer.valueOf(pick.id));
            if (trail.size() > 24) {
                trail.remove(0);
                trailIds.remove(0);
            }
            trailAt = trail.size() - 1;
        } else {
            trailAt = pick.back;
        }
        since = SystemClock.uptimeMillis();
    }

    private static Bitmap decode(Context context, Item item) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            if (item.res != 0) {
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                return BitmapFactory.decodeResource(context.getResources(), item.res, options);
            }
            int i = 1;
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(item.file.getPath(), options);
            if (options.outWidth <= 0) {
                return null;
            }
            while (true) {
                int i2 = i * 2;
                if (options.outWidth / i2 < 1280) {
                    BitmapFactory.Options options2 = new BitmapFactory.Options();
                    options2.inSampleSize = i;
                    options2.inPreferredConfig = Bitmap.Config.RGB_565;
                    return BitmapFactory.decodeFile(item.file.getPath(), options2);
                }
                i = i2;
            }
        } catch (Throwable th) {
            return null;
        }
    }

    static float boost(Bitmap bitmap) {
        if (bitmap == null) {
            return 1.0f;
        }
        try {
            Bitmap bitmapCreateScaledBitmap = Bitmap.createScaledBitmap(bitmap, 32, 18, true);
            int[] iArr = new int[576];
            bitmapCreateScaledBitmap.getPixels(iArr, 0, 32, 0, 0, 32, 18);
            if (bitmapCreateScaledBitmap != bitmap) {
                bitmapCreateScaledBitmap.recycle();
            }
            long j = 0;
            for (int i = 0; i < 576; i++) {
                int i2 = iArr[i];
                j += (long) ((((i2 >> 16) & 255) * 30) + (((i2 >> 8) & 255) * 59) + ((i2 & 255) * 11));
            }
            return Math.max(1.0f, Math.min(1.6f, (((j / (576 * 100.0f)) - 95.0f) / 60.0f) + 1.0f));
        } catch (Throwable th) {
            return 1.0f;
        }
    }
}
