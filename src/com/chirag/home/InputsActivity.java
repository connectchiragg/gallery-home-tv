package com.chirag.home;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
public class InputsActivity extends Activity {
    static final int COLS = 3;
    static final int TILE_GAP = 24;
    static final int TILE_W = 264;
    LinearLayout root;
    Ui.Stage stage;

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().getDecorView().setBackgroundColor(-16052975);
        Ui.Page page = new Ui.Page(this);
        this.root = new LinearLayout(this);
        this.root.setOrientation(1);
        this.root.setClipChildren(false);
        this.root.setClipToPadding(false);
        this.root.setPadding(0, Ui.dp(this, 32), 0, Ui.dp(this, 28));
        page.addView(this.root);
        this.stage = new Ui.Stage(this, page);
        setContentView(this.stage);
    }

    @Override // android.app.Activity
    protected void onResume() {
        LinearLayout r4;
        super.onResume();
        this.stage.resume();
        this.root.removeAllViews();
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        for (Inputs.Item item : Inputs.list(this)) {
            (item.connected ? arrayList : arrayList2).add(item);
        }
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setGravity(16);
        boolean z = false;
        linearLayout.setClipChildren(false);
        linearLayout.setClipToPadding(false);
        int i = 60;
        linearLayout.setPadding(Ui.dp(this, 60), 0, Ui.dp(this, 60), 0);
        linearLayout.addView(Ui.badge(this, 1, 56, TILE_GAP, -1, false));
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(1);
        linearLayout2.setPadding(Ui.dp(this, 16), 0, 0, 0);
        linearLayout2.addView(Ui.text(this, "Inputs", TILE_GAP, -1, Ui.MEDIUM));
        TextView textViewText = Ui.text(this, new StringBuilder().append("Pick where the picture comes from · ").append(arrayList.isEmpty() ? "nothing connected" : arrayList.size() + (arrayList.size() == 1 ? " source ready" : " sources ready")).toString(), 13, -855638017, Ui.REGULAR);
        textViewText.setPadding(0, Ui.dp(this, 6), 0, 0);
        linearLayout2.addView(textViewText);
        linearLayout.addView(linearLayout2);
        this.root.addView(linearLayout, new LinearLayout.LayoutParams(-1, Ui.dp(this, 56)));
        if (arrayList.isEmpty() && arrayList2.isEmpty()) {
            this.root.addView(Ui.notice(this, "No inputs found", "This TV did not report any HDMI, AV or antenna inputs.", -1, 60));
        }
        int i2 = 0;
        View view = null;
        while (true) {
            int i3 = 2;
            if (i2 >= 2) {
                break;
            }
            ArrayList arrayList3 = i2 == 0 ? arrayList : arrayList2;
            if (!arrayList3.isEmpty()) {
                Ui.heading(this, this.root, i2 == 0 ? "Connected" : "Not connected", i, i2 == 0 ? TILE_GAP : 14);
                View view2 = view;
                int i4 = z ? 1 : 0;
                LinearLayout r0 = null;
                while (i4 < arrayList3.size()) {
                    int i5 = i4 % COLS;
                    if (i5 == 0) {
                        LinearLayout linearLayout3 = new LinearLayout(this);
                        linearLayout3.setClipChildren(false);
                        linearLayout3.setClipToPadding(false);
                        linearLayout3.setPadding(Ui.dp(this, i), Ui.dp(this, i4 == 0 ? 12 : 4), 0, Ui.dp(this, 12));
                        this.root.addView(linearLayout3);
                        r4 = linearLayout3;
                    } else {
                        r4 = r0;
                    }
                    final Inputs.Item item2 = (Inputs.Item) arrayList3.get(i4);
                    LinearLayout r12 = r4;
                    int i6 = i4;
                    ArrayList arrayList4 = arrayList3;
                    int i7 = i3;
                    View viewInputTile = Ui.inputTile(this, glyph(item2), item2.label, item2.sub, item2.connected, TILE_W, i5 == i3 ? 0 : TILE_GAP, new Runnable() { // from class: com.chirag.home.InputsActivity$$ExternalSyntheticLambda0
                        @Override // java.lang.Runnable
                        public final void run() {
                            m20lambda$onResume$0$comchiraghomeInputsActivity(item2);
                        }
                    });
                    r12.addView(viewInputTile);
                    if (view2 == null) {
                        view2 = viewInputTile;
                    }
                    i4 = i6 + 1;
                    r0 = r12;
                    i3 = i7;
                    arrayList3 = arrayList4;
                    i = 60;
                }
                view = view2;
            }
            i2++;
            z = false;
            i = 60;
        }
        if (view != null) {
            view.requestFocus();
        }
    }

    /* JADX INFO: renamed from: lambda$onResume$0$com-chirag-home-InputsActivity, reason: not valid java name */
    /* synthetic */ void m20lambda$onResume$0$comchiraghomeInputsActivity(Inputs.Item item) {
        Ui.launch(this, Inputs.open(item));
    }

    @Override // android.app.Activity
    protected void onPause() {
        super.onPause();
        this.stage.pause();
    }

    static int glyph(Inputs.Item item) {
        if (item.tuner) {
            return 16;
        }
        String lowerCase = item.label.toLowerCase();
        return (lowerCase.contains("playstation") || lowerCase.contains("xbox") || lowerCase.contains("switch") || lowerCase.contains("console")) ? 10 : 8;
    }
}
