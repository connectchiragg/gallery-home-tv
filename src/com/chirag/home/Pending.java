package com.chirag.home;

import android.os.SystemClock;

/* JADX INFO: loaded from: classes.dex */
final class Pending {
    static long deadline;
    static String pkg;
    static long position;
    static String title;

    Pending() {
    }

    static void set(String str, String str2, long j) {
        pkg = str;
        title = str2;
        position = j;
        deadline = SystemClock.elapsedRealtime() + 60000;
    }

    static boolean matches(String str, String str2) {
        return pkg != null && SystemClock.elapsedRealtime() < deadline && pkg.equals(str) && str2.trim().equals(title);
    }

    static void clear() {
        pkg = null;
        title = null;
    }
}
