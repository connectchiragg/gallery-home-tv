package com.chirag.home;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/* JADX INFO: loaded from: classes.dex */
public class HomeKeyService extends AccessibilityService {
    static volatile HomeKeyService inst;

    @Override // android.accessibilityservice.AccessibilityService
    protected void onServiceConnected() {
        inst = this;
        AirWake.start(this);
    }

    @Override // android.app.Service
    public void onDestroy() {
        inst = null;
        super.onDestroy();
    }

    void filterKeys(boolean z) {
        try {
            AccessibilityServiceInfo serviceInfo = getServiceInfo();
            if (serviceInfo == null) {
                return;
            }
            int i = z ? serviceInfo.flags | 32 : serviceInfo.flags & (-33);
            if (i != serviceInfo.flags) {
                serviceInfo.flags = i;
                setServiceInfo(serviceInfo);
            }
        } catch (Exception e) {
        }
    }

    @Override // android.accessibilityservice.AccessibilityService
    protected boolean onKeyEvent(KeyEvent keyEvent) {
        int keyCode = keyEvent.getKeyCode();
        boolean z = keyEvent.getAction() == 1;
        if (keyCode == 176 || keyCode == 83) {
            if (z) {
                try {
                    startActivity(new Intent("android.settings.SETTINGS").addFlags(268468224));
                } catch (Exception e) {
                }
            }
            return true;
        }
        if (keyCode != 84) {
            return false;
        }
        if (z) {
            try {
                startActivity(new Intent(this, (Class<?>) VoiceSearchActivity.class).addFlags(335544320));
            } catch (Exception e2) {
            }
        }
        return true;
    }

    @Override // android.accessibilityservice.AccessibilityService
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
    }

    @Override // android.accessibilityservice.AccessibilityService
    public void onInterrupt() {
    }
}
