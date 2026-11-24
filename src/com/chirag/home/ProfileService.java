package com.chirag.home;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public class ProfileService extends AccessibilityService {
    static final String AIRSCREEN = "com.ionitech.airscreen";
    static final String[][] AIRSCREEN_DIALOGS;
    static final Map<String, String[]> PROFILES = new HashMap();
    static volatile ProfileService inst;
    long idleSince;
    long lastDialog;
    final Map<String, Long> lastPick = new HashMap();
    final Handler h = new Handler(Looper.getMainLooper());
    final Runnable closeIdle = new Runnable() { // from class: com.chirag.home.ProfileService.1
        @Override // java.lang.Runnable
        public void run() {
            if (ProfileService.this.idleSince == 0 || !AirWake.weLaunched) {
                return;
            }
            ProfileService.this.idleSince = 0L;
            AccessibilityNodeInfo rootInActiveWindow = ProfileService.this.getRootInActiveWindow();
            if (rootInActiveWindow == null || rootInActiveWindow.getPackageName() == null || !ProfileService.AIRSCREEN.contentEquals(rootInActiveWindow.getPackageName())) {
                return;
            }
            ArrayDeque arrayDeque = new ArrayDeque();
            arrayDeque.add(rootInActiveWindow);
            boolean z = false;
            while (!arrayDeque.isEmpty() && !z) {
                AccessibilityNodeInfo accessibilityNodeInfo = (AccessibilityNodeInfo) arrayDeque.poll();
                for (int i = 0; i < accessibilityNodeInfo.getChildCount(); i++) {
                    AccessibilityNodeInfo child = accessibilityNodeInfo.getChild(i);
                    if (child != null) {
                        arrayDeque.add(child);
                    }
                }
                if (accessibilityNodeInfo.getText() != null && accessibilityNodeInfo.getText().toString().toLowerCase().startsWith("please share your content")) {
                    z = true;
                }
            }
            if (z) {
                AirWake.weLaunched = false;
                Log.i("AirWake", "closing idle AirScreen");
                ProfileService.this.performGlobalAction(1);
            }
        }
    };

    static {
        PROFILES.put("com.crunchyroll.crunchyroid", new String[]{"watching", "Chirag"});
        AIRSCREEN_DIALOGS = new String[][]{new String[]{"terminate this application", "Yes"}, new String[]{"unable to load advertising", "Use it for free"}, new String[]{"special app access required", "Cancel"}, new String[]{"cast conflict detected", "Cancel"}, new String[]{"grant permission", "Cancel"}};
    }

    @Override // android.accessibilityservice.AccessibilityService
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        AccessibilityNodeInfo rootInActiveWindow;
        CharSequence packageName = accessibilityEvent.getPackageName();
        if (packageName == null) {
            return;
        }
        String string = packageName.toString();
        if (string.equals(AIRSCREEN)) {
            airscreenDialog();
            return;
        }
        if (ScreenRead.rule(string) != null) {
            ScreenReader.event(this, string);
            return;
        }
        String[] strArr = PROFILES.get(string);
        if (strArr == null) {
            return;
        }
        Long l = this.lastPick.get(string);
        if ((l != null && SystemClock.elapsedRealtime() - l.longValue() < 20000) || (rootInActiveWindow = getRootInActiveWindow()) == null || rootInActiveWindow.getPackageName() == null || !string.contentEquals(rootInActiveWindow.getPackageName())) {
            return;
        }
        ArrayList arrayList = new ArrayList();
        ArrayDeque arrayDeque = new ArrayDeque();
        arrayDeque.add(rootInActiveWindow);
        boolean z = false;
        while (!arrayDeque.isEmpty()) {
            AccessibilityNodeInfo accessibilityNodeInfo = (AccessibilityNodeInfo) arrayDeque.poll();
            for (int i = 0; i < accessibilityNodeInfo.getChildCount(); i++) {
                AccessibilityNodeInfo child = accessibilityNodeInfo.getChild(i);
                if (child != null) {
                    arrayDeque.add(child);
                }
            }
            CharSequence text = accessibilityNodeInfo.getText() != null ? accessibilityNodeInfo.getText() : accessibilityNodeInfo.getContentDescription();
            if (text != null) {
                String strTrim = text.toString().trim();
                if (strTrim.toLowerCase().contains(strArr[0])) {
                    z = true;
                }
                if (strTrim.equals(strArr[1])) {
                    arrayList.add(accessibilityNodeInfo);
                }
            }
        }
        if (z) {
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                AccessibilityNodeInfo accessibilityNodeInfoTileAbove = tileAbove(rootInActiveWindow, (AccessibilityNodeInfo) it.next());
                if (accessibilityNodeInfoTileAbove != null && accessibilityNodeInfoTileAbove.performAction(16)) {
                    this.lastPick.put(string, Long.valueOf(SystemClock.elapsedRealtime()));
                    return;
                }
            }
        }
    }

    @Override // android.accessibilityservice.AccessibilityService
    protected void onServiceConnected() {
        inst = this;
    }

    @Override // android.app.Service
    public void onDestroy() {
        inst = null;
        super.onDestroy();
    }

    void closeAirscreen() {
        AccessibilityNodeInfo rootInActiveWindow = getRootInActiveWindow();
        if (rootInActiveWindow != null && rootInActiveWindow.getPackageName() != null && AIRSCREEN.contentEquals(rootInActiveWindow.getPackageName())) {
            performGlobalAction(1);
        }
    }

    void airscreenDialog() {
        AccessibilityNodeInfo rootInActiveWindow;
        boolean z;
        if (SystemClock.elapsedRealtime() - this.lastDialog < 1500 || (rootInActiveWindow = getRootInActiveWindow()) == null || rootInActiveWindow.getPackageName() == null || !AIRSCREEN.contentEquals(rootInActiveWindow.getPackageName())) {
            return;
        }
        HashMap map = new HashMap();
        ArrayDeque arrayDeque = new ArrayDeque();
        arrayDeque.add(rootInActiveWindow);
        while (true) {
            if (arrayDeque.isEmpty()) {
                break;
            }
            AccessibilityNodeInfo accessibilityNodeInfo = (AccessibilityNodeInfo) arrayDeque.poll();
            for (int i = 0; i < accessibilityNodeInfo.getChildCount(); i++) {
                AccessibilityNodeInfo child = accessibilityNodeInfo.getChild(i);
                if (child != null) {
                    arrayDeque.add(child);
                }
            }
            if (accessibilityNodeInfo.getText() != null) {
                map.put(accessibilityNodeInfo.getText().toString().trim().toLowerCase(), accessibilityNodeInfo);
            }
        }
        Iterator it = map.keySet().iterator();
        while (true) {
            if (it.hasNext()) {
                if (((String) it.next()).startsWith("please share your content")) {
                    z = true;
                    break;
                }
            } else {
                z = false;
                break;
            }
        }
        if (!z) {
            this.idleSince = 0L;
            this.h.removeCallbacks(this.closeIdle);
        } else if (this.idleSince == 0) {
            Log.i("AirWake", "AirScreen idle screen seen, wokenByUs=" + AirWake.weLaunched);
            if (AirWake.weLaunched) {
                this.idleSince = SystemClock.elapsedRealtime();
                this.h.postDelayed(this.closeIdle, 30000L);
            }
        }
        for (String[] strArr : AIRSCREEN_DIALOGS) {
            if (map.containsKey(strArr[0])) {
                AccessibilityNodeInfo parent = (AccessibilityNodeInfo) map.get(strArr[1].toLowerCase());
                while (parent != null && !parent.isClickable()) {
                    parent = parent.getParent();
                }
                if (parent == null || !parent.performAction(16)) {
                    return;
                }
                this.lastDialog = SystemClock.elapsedRealtime();
                return;
            }
        }
    }

    static AccessibilityNodeInfo tileAbove(AccessibilityNodeInfo accessibilityNodeInfo, AccessibilityNodeInfo accessibilityNodeInfo2) {
        Rect rect = new Rect();
        accessibilityNodeInfo2.getBoundsInScreen(rect);
        ArrayDeque arrayDeque = new ArrayDeque();
        arrayDeque.add(accessibilityNodeInfo);
        AccessibilityNodeInfo accessibilityNodeInfo3 = null;
        int i = Integer.MAX_VALUE;
        while (!arrayDeque.isEmpty()) {
            AccessibilityNodeInfo accessibilityNodeInfo4 = (AccessibilityNodeInfo) arrayDeque.poll();
            for (int i2 = 0; i2 < accessibilityNodeInfo4.getChildCount(); i2++) {
                AccessibilityNodeInfo child = accessibilityNodeInfo4.getChild(i2);
                if (child != null) {
                    arrayDeque.add(child);
                }
            }
            if (accessibilityNodeInfo4.isClickable()) {
                Rect rect2 = new Rect();
                accessibilityNodeInfo4.getBoundsInScreen(rect2);
                if (rect2.width() < 1500) {
                    int i3 = rect.top - rect2.bottom;
                    if (rect2.left <= rect.centerX() && rect.centerX() <= rect2.right && i3 >= -10 && i3 < 120 && i3 < i) {
                        accessibilityNodeInfo3 = accessibilityNodeInfo4;
                        i = i3;
                    }
                }
            }
        }
        return accessibilityNodeInfo3;
    }

    boolean inFront(String str) {
        try {
            AccessibilityNodeInfo rootInActiveWindow = getRootInActiveWindow();
            return (rootInActiveWindow == null || rootInActiveWindow.getPackageName() == null || !str.contentEquals(rootInActiveWindow.getPackageName())) ? false : true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override // android.accessibilityservice.AccessibilityService
    public void onInterrupt() {
    }
}
