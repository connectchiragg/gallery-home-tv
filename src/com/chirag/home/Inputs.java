package com.chirag.home;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
final class Inputs {
    static final ComponentName LIVE_TV = new ComponentName("com.smartdevice.livetv", "skyworth.skyworthlivetv.osd.ui.mainActivity.LiveTvScreenActivity");

    Inputs() {
    }

    static final class Item {
        boolean connected;
        String id;
        String label;
        String sub;
        boolean tuner;

        Item() {
        }
    }

    static List<Item> list(Context context) {
        String string;
        String str;
        TvInputManager tvInputManager = (TvInputManager) context.getSystemService("tv_input");
        ArrayList arrayList = new ArrayList();
        if (tvInputManager == null) {
            return arrayList;
        }
        Iterator<TvInputInfo> it = tvInputManager.getTvInputList().iterator();
        boolean z = false;
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            TvInputInfo next = it.next();
            if (next.getType() == 0) {
                z = true;
            } else if (next.isPassthroughInput() && !next.isHidden(context)) {
                Item item = new Item();
                item.id = next.getId();
                CharSequence charSequenceLoadCustomLabel = next.loadCustomLabel(context);
                CharSequence charSequenceLoadLabel = next.loadLabel(context);
                if (charSequenceLoadCustomLabel == null || charSequenceLoadCustomLabel.length() <= 0) {
                    string = charSequenceLoadLabel != null ? charSequenceLoadLabel.toString() : item.id;
                } else {
                    string = charSequenceLoadCustomLabel.toString();
                }
                item.label = string;
                item.connected = tvInputManager.getInputState(item.id) == 0;
                if (next.getParentId() != null) {
                    str = "HDMI device";
                } else {
                    str = item.connected ? "Connected" : "Nothing connected";
                }
                item.sub = str;
                arrayList.add(item);
            }
        }
        if (z) {
            Item item2 = new Item();
            item2.tuner = true;
            item2.label = "Smart TV";
            item2.sub = "This TV's own channels and apps";
            item2.connected = true;
            arrayList.add(item2);
        }
        return arrayList;
    }

    static Item find(Context context, String str) {
        for (Item item : list(context)) {
            if (item.label.toLowerCase().contains(str.toLowerCase())) {
                return item;
            }
        }
        return null;
    }

    static Intent open(Item item) {
        Intent component = new Intent(item.tuner ? "android.intent.action.MAIN" : "android.intent.action.VIEW").setComponent(LIVE_TV);
        if (!item.tuner) {
            component.setData(TvContract.buildChannelUriForPassthroughInput(item.id));
        }
        return component;
    }
}
