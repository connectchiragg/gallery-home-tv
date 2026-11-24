package com.chirag.home;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/* JADX INFO: loaded from: classes.dex */
public class HistoryDb extends SQLiteOpenHelper {
    private static final String KEY = "pkg=? AND title=? AND subtitle=?";
    static final Set<String> TITLE_HIDDEN = new HashSet(Arrays.asList("com.netflix.ninja", "com.amazon.amazonvideo.livingroom", "com.apple.atve.androidtv.appletv"));
    private static HistoryDb inst;

    public static class Item {
        public String art;
        public long artTried;
        public long attachedTs;
        public long dismissedPos;
        public String dismissedSub;
        public long dismissedTs;
        public long duration;
        public long firstTs;
        public long id;
        public long lastTs;
        public String mediaId;
        public long openedTs;
        public String pkg;
        public long position;
        public int screen;
        public String subtitle;
        public String title;

        public float progress() {
            if (this.duration > 0) {
                return Math.min(1.0f, this.position / this.duration);
            }
            return 0.0f;
        }

        public boolean finished() {
            return this.duration > 0 && ((float) this.position) >= ((float) this.duration) * 0.95f;
        }
    }

    public static synchronized HistoryDb get(Context context) {
        if (inst == null) {
            inst = new HistoryDb(context.getApplicationContext());
        }
        return inst;
    }

    private HistoryDb(Context context) {
        super(context, "history.db", (SQLiteDatabase.CursorFactory) null, 4);
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = TITLE_HIDDEN.iterator();
        while (it.hasNext()) {
            sb.append(sb.length() == 0 ? "" : ",").append('\'').append(it.next()).append('\'');
        }
        getWritableDatabase().delete("plays", "title='' AND pkg NOT IN (" + ((Object) sb) + ")", null);
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE plays (id INTEGER PRIMARY KEY AUTOINCREMENT, pkg TEXT NOT NULL, title TEXT NOT NULL, subtitle TEXT NOT NULL DEFAULT '', media_id TEXT, art TEXT, duration INTEGER, position INTEGER, first_ts INTEGER, last_ts INTEGER, opened_ts INTEGER NOT NULL DEFAULT 0, attached_ts INTEGER NOT NULL DEFAULT 0, art_tried INTEGER NOT NULL DEFAULT 0, screen INTEGER NOT NULL DEFAULT 0, dismissed_ts INTEGER NOT NULL DEFAULT 0, dismissed_pos INTEGER NOT NULL DEFAULT 0, dismissed_sub TEXT, UNIQUE(pkg, title, subtitle))");
        sQLiteDatabase.execSQL("CREATE INDEX plays_last ON plays(last_ts)");
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        if (i < 2) {
            sQLiteDatabase.execSQL("ALTER TABLE plays ADD COLUMN opened_ts INTEGER NOT NULL DEFAULT 0");
            sQLiteDatabase.execSQL("ALTER TABLE plays ADD COLUMN attached_ts INTEGER NOT NULL DEFAULT 0");
            sQLiteDatabase.execSQL("ALTER TABLE plays ADD COLUMN art_tried INTEGER NOT NULL DEFAULT 0");
        }
        if (i < 3) {
            sQLiteDatabase.execSQL("ALTER TABLE plays ADD COLUMN screen INTEGER NOT NULL DEFAULT 0");
        }
        if (i < 4) {
            sQLiteDatabase.execSQL("ALTER TABLE plays ADD COLUMN dismissed_ts INTEGER NOT NULL DEFAULT 0");
            sQLiteDatabase.execSQL("ALTER TABLE plays ADD COLUMN dismissed_pos INTEGER NOT NULL DEFAULT 0");
            sQLiteDatabase.execSQL("ALTER TABLE plays ADD COLUMN dismissed_sub TEXT");
        }
    }

    private static Item item(Cursor cursor) {
        Item item = new Item();
        item.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        item.pkg = cursor.getString(cursor.getColumnIndexOrThrow("pkg"));
        item.title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
        item.subtitle = cursor.getString(cursor.getColumnIndexOrThrow("subtitle"));
        item.mediaId = cursor.getString(cursor.getColumnIndexOrThrow("media_id"));
        item.art = cursor.getString(cursor.getColumnIndexOrThrow("art"));
        item.duration = cursor.getLong(cursor.getColumnIndexOrThrow("duration"));
        item.position = cursor.getLong(cursor.getColumnIndexOrThrow("position"));
        item.firstTs = cursor.getLong(cursor.getColumnIndexOrThrow("first_ts"));
        item.lastTs = cursor.getLong(cursor.getColumnIndexOrThrow("last_ts"));
        item.openedTs = cursor.getLong(cursor.getColumnIndexOrThrow("opened_ts"));
        item.attachedTs = cursor.getLong(cursor.getColumnIndexOrThrow("attached_ts"));
        item.artTried = cursor.getLong(cursor.getColumnIndexOrThrow("art_tried"));
        item.screen = cursor.getInt(cursor.getColumnIndexOrThrow("screen"));
        item.dismissedTs = cursor.getLong(cursor.getColumnIndexOrThrow("dismissed_ts"));
        item.dismissedPos = cursor.getLong(cursor.getColumnIndexOrThrow("dismissed_pos"));
        item.dismissedSub = cursor.getString(cursor.getColumnIndexOrThrow("dismissed_sub"));
        return item;
    }

    private Item byId(long j) {
        try (Cursor cursorQuery = getReadableDatabase().query("plays", null, "id=?", new String[]{String.valueOf(j)}, null, null, null, "1")) {
            Item item = cursorQuery.moveToFirst() ? item(cursorQuery) : null;
            return item;
        }
    }

    public synchronized void dismiss(Item item) {
        if (item == null) {
            return;
        }
        Item itemById = byId(item.id);
        if (itemById == null) {
            itemById = find(item.pkg, item.title, item.subtitle == null ? "" : item.subtitle);
        }
        if (itemById == null) {
            return;
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put("dismissed_ts", Long.valueOf(System.currentTimeMillis()));
        contentValues.put("dismissed_pos", Long.valueOf(Math.max(0L, itemById.position)));
        contentValues.put("dismissed_sub", itemById.subtitle);
        getWritableDatabase().update("plays", contentValues, "id=?", new String[]{String.valueOf(itemById.id)});
    }

    public synchronized void restore(Item item) {
        if (item == null) {
            return;
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put("dismissed_ts", (Integer) 0);
        if (getWritableDatabase().update("plays", contentValues, "id=?", new String[]{String.valueOf(item.id)}) == 0) {
            getWritableDatabase().update("plays", contentValues, KEY, new String[]{item.pkg, item.title, item.subtitle == null ? "" : item.subtitle});
        }
    }

    public synchronized void recordLaunch(String str, String str2, String str3, String str4, long j) {
        SQLiteDatabase writableDatabase = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        if (str4 != null) {
            contentValues.put("media_id", str4);
        }
        contentValues.put("last_ts", Long.valueOf(j));
        contentValues.put("opened_ts", Long.valueOf(j));
        contentValues.put("attached_ts", (Integer) 0);
        contentValues.put("dismissed_ts", (Integer) 0);
        if (writableDatabase.update("plays", contentValues, KEY, new String[]{str, str2, str3}) == 0) {
            contentValues.put("pkg", str);
            contentValues.put("title", str2);
            contentValues.put("subtitle", str3);
            contentValues.put("first_ts", Long.valueOf(j));
            writableDatabase.insert("plays", null, contentValues);
        }
    }

    public Item latestLaunch(String str) {
        try (Cursor cursorQuery = getReadableDatabase().query("plays", null, "pkg=? AND opened_ts>0", new String[]{str}, null, null, "opened_ts DESC", "1")) {
            Item item = cursorQuery.moveToFirst() ? item(cursorQuery) : null;
            return item;
        }
    }

    private static WatchRule.Launch launch(Item item) {
        if (item == null) {
            return null;
        }
        return new WatchRule.Launch(item.openedTs, item.attachedTs, item.lastTs, item.duration);
    }

    public synchronized boolean playback(String str, boolean z, long j, long j2, long j3) {
        Item itemLatestLaunch = latestLaunch(str);
        WatchRule.Launch launch = launch(itemLatestLaunch);
        int iPlayback = WatchRule.playback(launch, z, j, j3);
        boolean z2 = false;
        if (itemLatestLaunch == null) {
            return false;
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put("attached_ts", Long.valueOf(launch.attachedTs));
        if (iPlayback == 1) {
            contentValues.put("last_ts", Long.valueOf(j3));
        }
        if (iPlayback == 1 && WatchRule.revived(itemLatestLaunch.dismissedTs, itemLatestLaunch.dismissedPos, z, j2, j, j3)) {
            contentValues.put("dismissed_ts", (Integer) 0);
        }
        if (iPlayback != 0 && launch.attachedTs > 0 && WatchRule.plausible(j, j2)) {
            contentValues.put("duration", Long.valueOf(j));
            contentValues.put("position", Long.valueOf(j2));
        }
        getWritableDatabase().update("plays", contentValues, "id=?", new String[]{String.valueOf(itemLatestLaunch.id)});
        if (iPlayback != 0) {
            z2 = true;
        }
        return z2;
    }

    public synchronized void titled(String str, String str2, String str3, long j) {
        Item itemLatestLaunch = latestLaunch(str);
        if (itemLatestLaunch != null && WatchRule.live(launch(itemLatestLaunch), j)) {
            if (itemLatestLaunch.title.equals(str2) && itemLatestLaunch.subtitle.equals(str3)) {
                return;
            }
            SQLiteDatabase writableDatabase = getWritableDatabase();
            writableDatabase.execSQL("UPDATE plays SET media_id = COALESCE(NULLIF(media_id, ''), ?), art = COALESCE(art, ?) WHERE pkg=? AND title=? AND subtitle=?", new Object[]{itemLatestLaunch.mediaId, itemLatestLaunch.art, str, str2, str3});
            writableDatabase.delete("plays", "id=?", new String[]{String.valueOf(itemLatestLaunch.id)});
        }
    }

    public synchronized long identified(String str, String str2, String str3, String str4, long j, long j2, long j3) {
        Item itemFind;
        SQLiteDatabase writableDatabase = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        if (str4 != null) {
            contentValues.put("media_id", str4);
        }
        contentValues.put("last_ts", Long.valueOf(j3));
        contentValues.put("opened_ts", Long.valueOf(j3));
        contentValues.put("attached_ts", Long.valueOf(j3));
        contentValues.put("screen", (Integer) 2);
        contentValues.put("dismissed_ts", (Integer) 0);
        if (WatchRule.plausible(j, j2)) {
            contentValues.put("duration", Long.valueOf(j));
            contentValues.put("position", Long.valueOf(j2));
        }
        if (writableDatabase.update("plays", contentValues, KEY, new String[]{str, str2, str3}) == 0) {
            contentValues.put("pkg", str);
            contentValues.put("title", str2);
            contentValues.put("subtitle", str3);
            contentValues.put("first_ts", Long.valueOf(j3));
            writableDatabase.insert("plays", null, contentValues);
        }
        itemFind = find(str, str2, str3);
        return itemFind == null ? -1L : itemFind.id;
    }

    public List<Item> screenRows(String str) {
        ArrayList arrayList = new ArrayList();
        try (Cursor cursorQuery = getReadableDatabase().query("plays", null, "pkg=? AND screen=1", new String[]{str}, null, null, "last_ts DESC", null)) {
            while (cursorQuery.moveToNext()) {
                arrayList.add(item(cursorQuery));
            }
        }
        return arrayList;
    }

    public synchronized long[] applyHome(String str, List<ScreenRead.Op> list, List<ScreenRead.Record> list2, long j) {
        long[] jArr;
        SQLiteDatabase writableDatabase = getWritableDatabase();
        jArr = new long[list2.size()];
        writableDatabase.beginTransaction();
        try {
            for (ScreenRead.Op op : list) {
                if (op.what.equals("delete")) {
                    writableDatabase.delete("plays", "id=? AND screen=1", new String[]{String.valueOf(op.id)});
                } else {
                    ScreenRead.Record record = list2.get(op.index);
                    writableDatabase.delete("plays", "pkg=? AND screen<>1 AND opened_ts>0 AND title=? COLLATE NOCASE", new String[]{str, record.title});
                    ContentValues contentValues = new ContentValues();
                    contentValues.put("subtitle", record.subtitle);
                    contentValues.put("last_ts", Long.valueOf(j - (((long) op.index) * 1000)));
                    if (record.mediaId != null) {
                        contentValues.put("media_id", record.mediaId);
                    }
                    if (op.what.equals("update")) {
                        Item itemById = byId(op.id);
                        if (itemById != null && itemById.dismissedTs > 0 && WatchRule.progressed(itemById.dismissedSub, record.subtitle)) {
                            contentValues.put("dismissed_ts", (Integer) 0);
                        }
                        writableDatabase.updateWithOnConflict("plays", contentValues, "id=?", new String[]{String.valueOf(op.id)}, 4);
                        jArr[op.index] = op.id;
                    } else {
                        contentValues.put("pkg", str);
                        contentValues.put("title", record.title);
                        contentValues.put("first_ts", Long.valueOf(j));
                        contentValues.put("screen", (Integer) 1);
                        jArr[op.index] = Math.max(0L, writableDatabase.insertWithOnConflict("plays", null, contentValues, 4));
                    }
                }
            }
            writableDatabase.setTransactionSuccessful();
            writableDatabase.endTransaction();
        } catch (Throwable th) {
            writableDatabase.endTransaction();
            throw th;
        }
        return jArr;
    }

    public void setArtById(long j, String str) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("art", str);
        getWritableDatabase().update("plays", contentValues, "id=? AND art IS NULL", new String[]{String.valueOf(j)});
    }

    public Item find(String str, String str2, String str3) {
        try (Cursor cursorQuery = getReadableDatabase().query("plays", null, KEY, new String[]{str, str2, str3}, null, null, null, "1")) {
            Item item = cursorQuery.moveToFirst() ? item(cursorQuery) : null;
            return item;
        }
    }

    public void setArtIfNone(String str, String str2, String str3, String str4) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("art", str4);
        getWritableDatabase().update("plays", contentValues, "pkg=? AND title=? AND subtitle=? AND art IS NULL", new String[]{str, str2, str3});
    }

    public void restoreLastDismissed() {
        getWritableDatabase().execSQL("UPDATE plays SET dismissed_ts=0 WHERE dismissed_ts>0 AND dismissed_ts=(SELECT MAX(dismissed_ts) FROM plays)");
    }

    public void setArt(String str, String str2, String str3, String str4) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("art", str4);
        getWritableDatabase().update("plays", contentValues, KEY, new String[]{str, str2, str3});
    }

    public void markArtTried(String str, String str2, String str3, long j) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("art_tried", Long.valueOf(j));
        getWritableDatabase().update("plays", contentValues, KEY, new String[]{str, str2, str3});
    }

    public List<Item> missingArt(int i) {
        ArrayList arrayList = new ArrayList();
        try (Cursor cursorQuery = getReadableDatabase().query("plays", null, "title<>'' AND art IS NULL AND art_tried=0", null, null, null, "last_ts DESC", String.valueOf(i))) {
            while (cursorQuery.moveToNext()) {
                arrayList.add(item(cursorQuery));
            }
        }
        return arrayList;
    }

    public Set<String> artPaths() {
        HashSet hashSet = new HashSet();
        try (Cursor cursorRawQuery = getReadableDatabase().rawQuery("SELECT DISTINCT art FROM plays WHERE art IS NOT NULL", null)) {
            while (cursorRawQuery.moveToNext()) {
                hashSet.add(cursorRawQuery.getString(0));
            }
        }
        return hashSet;
    }

    public void upsert(String str, String str2, String str3, String str4, String str5, long j, long j2, long j3) {
        upsert(str, str2, str3, str4, str5, j, j2, j3, false);
    }

    public synchronized void upsert(String str, String str2, String str3, String str4, String str5, long j, long j2, long j3, boolean z) {
        Item itemFind;
        if (z) {
            Item itemFind2 = find(str, str2, str3);
            if (itemFind2 != null) {
                if (WatchRule.revived(itemFind2.dismissedTs, itemFind2.dismissedPos, true, j2, j > 0 ? j : itemFind2.duration, j3)) {
                    restore(itemFind2);
                }
            }
        }
        SQLiteDatabase writableDatabase = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        if (str4 != null) {
            contentValues.put("media_id", str4);
        }
        if (str5 != null && ((itemFind = find(str, str2, str3)) == null || itemFind.art == null || !new File(itemFind.art).getName().startsWith("ep_"))) {
            contentValues.put("art", str5);
        }
        if (j > 0) {
            contentValues.put("duration", Long.valueOf(j));
        }
        if (j2 >= 0) {
            contentValues.put("position", Long.valueOf(j2));
        }
        contentValues.put("last_ts", Long.valueOf(j3));
        if (writableDatabase.update("plays", contentValues, KEY, new String[]{str, str2, str3}) == 0) {
            contentValues.put("pkg", str);
            contentValues.put("title", str2);
            contentValues.put("subtitle", str3);
            contentValues.put("first_ts", Long.valueOf(j3));
            writableDatabase.insert("plays", null, contentValues);
        }
    }

    public List<Item> recent(String str, long j, int i, boolean z) {
        String str2 = "last_ts >= ?" + (str != null ? " AND pkg = ?" : "");
        String[] strArr = str != null ? new String[]{String.valueOf(j), str} : new String[]{String.valueOf(j)};
        ArrayList arrayList = new ArrayList();
        HashSet hashSet = new HashSet();
        HashSet hashSet2 = new HashSet();
        try (Cursor cursorRawQuery = getReadableDatabase().rawQuery("SELECT DISTINCT pkg FROM plays WHERE screen=1", null)) {
            while (cursorRawQuery.moveToNext()) {
                hashSet2.add(cursorRawQuery.getString(0));
            }
        }
        try (Cursor cursorQuery = getReadableDatabase().query("plays", null, str2, strArr, null, null, "last_ts DESC", null)) {
            while (cursorQuery.moveToNext() && arrayList.size() < i) {
                Item item = item(cursorQuery);
                if (item.openedTs > 0) {
                    hashSet2.add(item.pkg);
                }
                if (!item.title.isEmpty() || !hashSet2.contains(item.pkg)) {
                    if (!z || item.dismissedTs <= 0) {
                        if (!z || !item.finished()) {
                            if (!z || item.title.isEmpty() || ArtStore.episode(item.subtitle) == null || hashSet.add(item.pkg + "|" + item.title)) {
                                if (!z || !item.pkg.equals("com.spotify.tv.android")) {
                                    arrayList.add(item);
                                }
                            }
                        }
                    }
                }
            }
        }
        return arrayList;
    }

    public List<VoiceAgent.Play> plays(int i) {
        ArrayList arrayList = new ArrayList();
        for (Item item : recent(null, System.currentTimeMillis() - 5184000000L, i * 2, false)) {
            if (item.title != null && !item.title.isEmpty() && arrayList.size() < i && item.dismissedTs <= 0) {
                VoiceAgent.Play play = new VoiceAgent.Play();
                play.pkg = item.pkg;
                play.title = item.title;
                play.subtitle = item.subtitle == null ? "" : item.subtitle;
                play.mediaId = item.mediaId;
                play.lastTs = item.lastTs;
                play.duration = item.duration;
                play.position = item.position;
                arrayList.add(play);
            }
        }
        return arrayList;
    }

    public void setMediaId(String str, String str2, String str3, String str4) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("media_id", str4);
        getWritableDatabase().update("plays", contentValues, KEY, new String[]{str, str2, str3});
    }

    public List<Item> missingYoutubeIds() {
        ArrayList arrayList = new ArrayList();
        for (Item item : recent("com.google.android.youtube.tv", 0L, 200, false)) {
            if (item.mediaId == null || !item.mediaId.matches("[A-Za-z0-9_-]{11}")) {
                arrayList.add(item);
            }
        }
        return arrayList;
    }

    public int count(String str) {
        try (Cursor cursorRawQuery = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM plays WHERE pkg=?", new String[]{str})) {
            int i = cursorRawQuery.moveToFirst() ? cursorRawQuery.getInt(0) : 0;
            return i;
        }
    }
}
