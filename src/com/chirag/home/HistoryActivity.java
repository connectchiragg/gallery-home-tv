package com.chirag.home;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class HistoryActivity extends Activity {
    static final int CARD_GAP = 24;
    static final int CARD_W = 264;
    static final int COLS = 3;
    int filter = 1;
    String pkg;
    LinearLayout root;
    Ui.Stage stage;
    static final String[] NAMES = {"Last day", "Last week", "Last month", "All"};
    static final long DAY = 86400000;
    static final long[] SPANS = {DAY, 604800000, 2592000000L, 0};

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.pkg = getIntent().getStringExtra("pkg");
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
        super.onResume();
        this.stage.resume();
        build(-1);
    }

    @Override // android.app.Activity
    protected void onPause() {
        super.onPause();
        this.stage.pause();
    }

    void build(int i) {
        String str;
        LinearLayout linearLayout;
        this.root.removeAllViews();
        String strLabel = Ui.label(this, this.pkg);
        List<HistoryDb.Item> listRecent = HistoryDb.get(this).recent(this.pkg, SPANS[this.filter] != 0 ? System.currentTimeMillis() - SPANS[this.filter] : 0L, 60, false);
        String str2 = this.filter == COLS ? "all time" : "the " + NAMES[this.filter].toLowerCase();
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setGravity(16);
        linearLayout2.setClipChildren(false);
        linearLayout2.setClipToPadding(false);
        linearLayout2.setPadding(Ui.dp(this, 60), 0, Ui.dp(this, 60), 0);
        View orb = new Orb(this, this.pkg, Ui.dp(this, 76), true);
        Ui.liftIcon(orb, false);
        linearLayout2.addView(orb, new LinearLayout.LayoutParams(Ui.dp(this, 56), Ui.dp(this, 56)));
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setOrientation(1);
        linearLayout3.setPadding(Ui.dp(this, 16), 0, 0, 0);
        linearLayout3.addView(Ui.text(this, strLabel, CARD_GAP, -1, Ui.MEDIUM));
        StringBuilder sbAppend = new StringBuilder().append("Watch history · ");
        if (listRecent.isEmpty()) {
            str = "nothing in " + str2;
        } else {
            str = listRecent.size() + (listRecent.size() == 1 ? " title in " : " titles in ") + str2;
        }
        TextView textViewText = Ui.text(this, sbAppend.append(str).toString(), 13, -855638017, Ui.REGULAR);
        textViewText.setPadding(0, Ui.dp(this, 6), 0, 0);
        linearLayout3.addView(textViewText);
        linearLayout2.addView(linearLayout3);
        this.root.addView(linearLayout2, new LinearLayout.LayoutParams(-1, Ui.dp(this, 56)));
        LinearLayout linearLayoutRow = Ui.row(this, this.root, 20, 14);
        View viewPill = Ui.pill(this, 2, "Open " + strLabel, 0, new Runnable() { // from class: com.chirag.home.HistoryActivity$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                m17lambda$build$0$comchiraghomeHistoryActivity();
            }
        });
        linearLayoutRow.addView(viewPill);
        ((LinearLayout.LayoutParams) viewPill.getLayoutParams()).setMargins(0, 0, Ui.dp(this, 26), 0);
        View view = viewPill;
        for (int i2 = 0; i2 < NAMES.length; i2++) {
            final int i3 = i2;
            View viewPill2 = Ui.pill(this, i2 == this.filter ? 7 : 0, NAMES[i2], i2 == this.filter ? 2 : 1, new Runnable() { // from class: com.chirag.home.HistoryActivity$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    m18lambda$build$1$comchiraghomeHistoryActivity(i3);
                }
            });
            linearLayoutRow.addView(viewPill2);
            if (i2 == i) {
                view = viewPill2;
            }
        }
        if (listRecent.isEmpty()) {
            LinearLayout linearLayout4 = new LinearLayout(this);
            linearLayout4.setOrientation(1);
            linearLayout4.setGravity(1);
            linearLayout4.setPadding(0, Ui.dp(this, 72), 0, 0);
            ImageView imageViewBadge = Ui.badge(this, 11, 56, CARD_GAP, -1, false);
            ((LinearLayout.LayoutParams) imageViewBadge.getLayoutParams()).setMargins(0, 0, 0, Ui.dp(this, 18));
            linearLayout4.addView(imageViewBadge);
            linearLayout4.addView(Ui.text(this, "Nothing watched in " + str2, 22, -1, Ui.MEDIUM));
            TextView textViewText2 = Ui.text(this, this.filter == COLS ? "Open " + strLabel + " and whatever you play will show up here." : "Try a wider range, or open " + strLabel + " to start something.", 13, -855638017, Ui.REGULAR);
            textViewText2.setPadding(0, Ui.dp(this, 10), 0, 0);
            linearLayout4.addView(textViewText2);
            this.root.addView(linearLayout4, new LinearLayout.LayoutParams(-1, -2));
        } else {
            LinearLayout linearLayout5 = null;
            int i3 = 0;
            while (i3 < listRecent.size()) {
                int i4 = i3 % COLS;
                if (i4 == 0) {
                    LinearLayout linearLayout6 = new LinearLayout(this);
                    linearLayout6.setClipChildren(false);
                    linearLayout6.setClipToPadding(false);
                    linearLayout6.setPadding(Ui.dp(this, 60), Ui.dp(this, 10), 0, Ui.dp(this, 16));
                    this.root.addView(linearLayout6);
                    linearLayout = linearLayout6;
                } else {
                    linearLayout = linearLayout5;
                }
                final HistoryDb.Item item = listRecent.get(i3);
                linearLayout.addView(Ui.historyCard(this, item, false, CARD_W, i4 == 2 ? 0 : CARD_GAP, new Runnable() { // from class: com.chirag.home.HistoryActivity$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        m19lambda$build$2$comchiraghomeHistoryActivity(item);
                    }
                }));
                i3++;
                linearLayout5 = linearLayout;
            }
        }
        view.requestFocus();
    }

    /* JADX INFO: renamed from: lambda$build$0$com-chirag-home-HistoryActivity, reason: not valid java name */
    /* synthetic */ void m17lambda$build$0$comchiraghomeHistoryActivity() {
        Ui.launch(this, Ui.launchIntent(this, this.pkg));
    }

    /* JADX INFO: renamed from: lambda$build$1$com-chirag-home-HistoryActivity, reason: not valid java name */
    /* synthetic */ void m18lambda$build$1$comchiraghomeHistoryActivity(int i) {
        this.filter = i;
        build(i);
    }

    /* JADX INFO: renamed from: lambda$build$2$com-chirag-home-HistoryActivity, reason: not valid java name */
    /* synthetic */ void m19lambda$build$2$comchiraghomeHistoryActivity(HistoryDb.Item item) {
        Ui.resume(this, item);
    }
}
