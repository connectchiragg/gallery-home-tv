package com.chirag.home;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/* JADX INFO: loaded from: classes.dex */
public class MainActivity extends Activity {
    static final String AIRPLAY = "com.ionitech.airscreen";
    static final int CAPTION_REST = -1275068417;
    static final int CLOCK_ALPHA = 184;
    static final int CLOCK_SP = 168;
    static final long CLOSE_AFTER_MS = 60000;
    static final long DARK_FADE_MS = 1000;
    static final int HEAD_H = 262;
    /** Gallery mode: after this long with no remote key, only the painting and its label stay. */
    static final long IDLE_MS = 60_000;
    static final long IDLE_FADE_MS = 1200;
    static final long WAKE_FADE_MS = 400;
    static final long MONTH = 2592000000L;
    static final float STORY_COLUMN = 0.39f;
    static final int STORY_GOLD = -1848426;
    static volatile boolean showing;
    boolean artInstant;
    TextView caption;
    TextView clock;
    View dark;
    TextView date;
    View dialog;
    View dialogFor;
    View inputsPill;
    View keepLit;
    Ui.Page page;
    View ps5;
    LinearLayout root;
    TextView sAbout;
    TextView sBy;
    TextView sMuseum;
    TextView sTitle;
    boolean showMore;
    Ui.Stage stage;
    View story;
    static final List<String> FAVORITES = Arrays.asList("com.apple.atve.androidtv.appletv", "com.netflix.ninja", "com.google.android.youtube.tv", "in.startv.hotstar", "com.crunchyroll.crunchyroid", "com.spotify.tv.android", "com.amazon.amazonvideo.livingroom", "com.phlox.tvwebbrowser", "com.android.vending");
    static final ComponentName LIVE_TV = new ComponentName("com.smartdevice.livetv", "skyworth.skyworthlivetv.osd.ui.mainActivity.LiveTvScreenActivity");
    static final Typeface SERIF = Typeface.create("serif", 0);
    static final Typeface SERIF_ITALIC = Typeface.create("serif", 2);
    final NowPlaying nowPlaying = new NowPlaying(this);
    final Handler h = new Handler(Looper.getMainLooper());
    final Runnable tick = new Runnable() { // from class: com.chirag.home.MainActivity.1
        @Override // java.lang.Runnable
        public void run() {
            MainActivity.this.updateHeader();
            MainActivity.this.h.postDelayed(this, 15000L);
        }
    };
    final Runnable closeApps = new Runnable() { // from class: com.chirag.home.MainActivity$$ExternalSyntheticLambda5
        @Override // java.lang.Runnable
        public final void run() {
            m38lambda$new$4$comchiraghomeMainActivity();
        }
    };
    final List<View> faded = new ArrayList();
    int focusCard = -1;

    // Gallery mode state: overlay views faded out, the focus to give back, and the key that woke
    // the screen (its repeats and key-up are swallowed too, so the wake press does nothing else).
    boolean idle;
    boolean resumed;
    final List<View> idleFaded = new ArrayList<>();
    View idleFocus;
    int wakeKey = KeyEvent.KEYCODE_UNKNOWN;
    final Runnable sleep = new Runnable() {
        @Override
        public void run() {
            enterIdle();
        }
    };

    void updateHeader() {
        Date date = new Date();
        set(this.date, new SimpleDateFormat("EEEE, d MMMM", Locale.ENGLISH).format(date));
        set(this.clock, new SimpleDateFormat("h:mm", Locale.ENGLISH).format(date));
    }

    static void set(TextView textView, String str) {
        if (str.contentEquals(textView.getText().toString())) {
            return;
        }
        textView.setText(str);
    }

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().getDecorView().setBackgroundColor(-16052975);
        Ui.Page page = new Ui.Page(this);
        this.root = new LinearLayout(this);
        this.root.setOrientation(1);
        this.root.setClipChildren(false);
        this.root.setClipToPadding(false);
        this.root.setPadding(0, 0, 0, Ui.dp(this, 28));
        page.addView(this.root);
        this.stage = new Ui.Stage(this, page);
        this.page = page;
        this.stage.onArt = new Runnable() { // from class: com.chirag.home.MainActivity$$ExternalSyntheticLambda27
            @Override // java.lang.Runnable
            public final void run() {
                m40lambda$onCreate$2$comchiraghomeMainActivity();
            }
        };
        setContentView(this.stage);
    }

    /* JADX INFO: renamed from: lambda$onCreate$2$com-chirag-home-MainActivity, reason: not valid java name */
    /* synthetic */ void m40lambda$onCreate$2$comchiraghomeMainActivity() {
        if (this.artInstant) {
            if (this.caption != null) {
                set(this.caption, Art.caption());
            }
            if (this.story != null) {
                fillStory();
                return;
            }
            return;
        }
        final long jFadeMs = Art.fadeMs() / 2;
        if (this.caption != null && !Art.caption().contentEquals(this.caption.getText().toString())) {
            final TextView textView = this.caption;
            textView.animate().cancel();
            textView.animate().alpha(0.0f).setDuration(jFadeMs).withEndAction(new Runnable() { // from class: com.chirag.home.MainActivity$$ExternalSyntheticLambda22
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.lambda$onCreate$0(textView, jFadeMs);
                }
            }).start();
        }
        if (this.story != null && this.sTitle != null && (this.sTitle.getParent() instanceof View)) {
            final View view = (View) this.sTitle.getParent();
            view.animate().cancel();
            view.animate().alpha(0.0f).setDuration(jFadeMs).withEndAction(new Runnable() { // from class: com.chirag.home.MainActivity$$ExternalSyntheticLambda23
                @Override // java.lang.Runnable
                public final void run() {
                    m39lambda$onCreate$1$comchiraghomeMainActivity(view, jFadeMs);
                }
            }).start();
        }
    }

    static /* synthetic */ void lambda$onCreate$0(TextView textView, long j) {
        set(textView, Art.caption());
        textView.animate().alpha(1.0f).setDuration(j).withEndAction(null).start();
    }

    /* JADX INFO: renamed from: lambda$onCreate$1$com-chirag-home-MainActivity, reason: not valid java name */
    /* synthetic */ void m39lambda$onCreate$1$comchiraghomeMainActivity(View view, long j) {
        if (this.story != null) {
            fillStory();
        }
        view.animate().alpha(1.0f).setDuration(j).withEndAction(null).start();
    }

    @Override // android.app.Activity
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        this.resumed = true;
        this.wakeKey = KeyEvent.KEYCODE_UNKNOWN;
        if (getIntent() != null && getIntent().getBooleanExtra("undo_remove", false)) {
            getIntent().removeExtra("undo_remove");
            HistoryDb.get(this).restoreLastDismissed();
        }
        if (HomeKeyService.inst != null) {
            HomeKeyService.inst.filterKeys(false);
        }
        if (this.dialog != null) {
            this.stage.removeView(this.dialog);
            this.dialog = null;
            this.dialogFor = null;
        }
        if (this.story != null) {
            this.stage.removeView(this.story);
            this.story = null;
            this.page.animate().cancel();
            this.page.setAlpha(1.0f);
            this.page.setVisibility(0);
            this.stage.bare(false);
            this.stage.hold(false);
        }
        this.nowPlaying.start();
        this.artInstant = true;
        this.stage.resume();
        this.artInstant = false;
        try {
            ((ActivityManager) getSystemService(ActivityManager.class)).killBackgroundProcesses(AIRPLAY);
        } catch (Exception e) {
        }
        this.showMore = false;
        leaveIdle(0);
        build();
        armIdle();
        this.h.removeCallbacks(this.tick);
        this.h.postDelayed(this.tick, 15000L);
        this.h.removeCallbacks(this.closeApps);
        this.h.postDelayed(this.closeApps, CLOSE_AFTER_MS);
        for (HistoryDb.Item item : HistoryDb.get(this).missingYoutubeIds()) {
            YtLookup.ensure(this, item.title, item.subtitle);
        }
    }

    @Override // android.app.Activity
    protected void onStart() {
        super.onStart();
        showing = true;
    }

    @Override // android.app.Activity
    protected void onStop() {
        showing = false;
        super.onStop();
    }

    /* JADX INFO: renamed from: lambda$new$4$com-chirag-home-MainActivity, reason: not valid java name */
    /* synthetic */ void m38lambda$new$4$comchiraghomeMainActivity() {
        final HashSet hashSet = new HashSet();
        try {
            for (MediaController mediaController : ((MediaSessionManager) getSystemService(MediaSessionManager.class)).getActiveSessions(new ComponentName(this, (Class<?>) WatchListener.class))) {
                PlaybackState playbackState = mediaController.getPlaybackState();
                if (playbackState != null && (playbackState.getState() == 3 || playbackState.getState() == 6)) {
                    hashSet.add(mediaController.getPackageName());
                } else if ("com.spotify.tv.android".equals(mediaController.getPackageName()) && NowPlaying.loaded(playbackState) && !NowPlaying.cleared) {
                    hashSet.add(mediaController.getPackageName());
                }
            }
            LinkedHashSet linkedHashSet = new LinkedHashSet(FAVORITES);
            for (ResolveInfo resolveInfo : getPackageManager().queryIntentActivities(new Intent("android.intent.action.MAIN").addCategory("android.intent.category.LEANBACK_LAUNCHER"), 0)) {
                if ((resolveInfo.activityInfo.applicationInfo.flags & 1) == 0) {
                    linkedHashSet.add(resolveInfo.activityInfo.packageName);
                }
            }
            linkedHashSet.remove(getPackageName());
            linkedHashSet.remove(AIRPLAY);
            linkedHashSet.removeAll(hashSet);
            if (linkedHashSet.isEmpty()) {
                return;
            }
            final StringBuilder sb = new StringBuilder("for p in");
            Iterator it = linkedHashSet.iterator();
            while (it.hasNext()) {
                sb.append(' ').append((String) it.next());
            }
            sb.append("; do pidof $p >/dev/null && am force-stop $p && echo $p; done");
            final Context applicationContext = getApplicationContext();
            new Thread(new Runnable() { // from class: com.chirag.home.MainActivity$$ExternalSyntheticLambda20
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.lambda$new$3(applicationContext, sb, hashSet);
                }
            }, "close-apps").start();
        } catch (Exception e) {
        }
    }

    static /* synthetic */ void lambda$new$3(Context context, StringBuilder sb, Set set) {
        String str;
        String strShell = SelfAdb.shell(context, sb.toString(), 60000);
        if (strShell == null) {
            str = "could not close apps (adb not allowed yet?)";
        } else {
            str = "closed after a minute at home: " + strShell.trim().replace('\n', ' ') + (set.isEmpty() ? "" : " | kept playing " + set);
        }
        Log.i("Home", str);
    }

    @Override // android.app.Activity
    protected void onPause() {
        super.onPause();
        this.nowPlaying.stop();
        leaveDark(false);
        this.resumed = false;
        this.h.removeCallbacks(this.sleep);
        leaveIdle(0);
        this.wakeKey = KeyEvent.KEYCODE_UNKNOWN;
        this.h.removeCallbacks(this.closeApps);
        if (HomeKeyService.inst != null) {
            HomeKeyService.inst.filterKeys(false);
        }
        this.stage.pause();
        this.h.removeCallbacks(this.tick);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int code = event.getKeyCode();
        if (code == this.wakeKey && event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            this.wakeKey = KeyEvent.KEYCODE_UNKNOWN; // a fresh press: the waking one's key-up was lost
        }
        if (this.wakeKey != KeyEvent.KEYCODE_UNKNOWN && code == this.wakeKey) {
            // Rest of the press that woke the screen: repeats and the key-up (which is where OK
            // clicks and Back fires) must not reach the views.
            if (event.getAction() == KeyEvent.ACTION_UP) {
                this.wakeKey = KeyEvent.KEYCODE_UNKNOWN;
            }
            armIdle();
            return true;
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            armIdle();
            if (this.idle) {
                leaveIdle(WAKE_FADE_MS);
                if (isVolumeKey(code)) {
                    return super.dispatchKeyEvent(event);
                }
                this.wakeKey = code;
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    static boolean isVolumeKey(int code) {
        return code == KeyEvent.KEYCODE_VOLUME_UP || code == KeyEvent.KEYCODE_VOLUME_DOWN
                || code == KeyEvent.KEYCODE_VOLUME_MUTE;
    }

    /** (Re)start the idle countdown. Only runs while resumed: onPause removes it. */
    void armIdle() {
        this.h.removeCallbacks(this.sleep);
        if (this.resumed) {
            this.h.postDelayed(this.sleep, IDLE_MS);
        }
    }

    /** Fade every overlay part except the painting's label; the wall keeps rotating underneath. */
    void enterIdle() {
        if (this.idle || !this.resumed) {
            return;
        }
        if (this.story != null || this.dark != null || this.dialog != null) {
            armIdle();
            return;
        }
        this.idle = true;
        this.wakeKey = KeyEvent.KEYCODE_UNKNOWN;
        this.idleFocus = getCurrentFocus();
        this.idleFaded.clear();
        View head = this.caption == null ? null : (View) this.caption.getParent();
        View capsule = this.nowPlaying.view();
        for (int i = 0; i < this.root.getChildCount(); i++) {
            View child = this.root.getChildAt(i);
            if (child == head) {
                ViewGroup group = (ViewGroup) head;
                for (int j = 0; j < group.getChildCount(); j++) {
                    View part = group.getChildAt(j);
                    if (part != this.caption && part != capsule) {
                        this.idleFaded.add(part);
                    }
                }
            } else {
                this.idleFaded.add(child);
            }
        }
        AccelerateDecelerateInterpolator ease = new AccelerateDecelerateInterpolator();
        for (View v : this.idleFaded) {
            v.animate().cancel();
            v.animate().alpha(0.0f).setStartDelay(0L).setDuration(IDLE_FADE_MS).setInterpolator(ease).withEndAction(null).start();
        }
        this.nowPlaying.setIdle(true, IDLE_FADE_MS);
        Log.i("Home", "gallery mode on");
    }

    /** Bring the overlay back over {@code ms} (0 = at once) and hand focus back to where it was. */
    void leaveIdle(long ms) {
        if (!this.idle) {
            return;
        }
        this.idle = false;
        AccelerateDecelerateInterpolator ease = new AccelerateDecelerateInterpolator();
        for (View v : this.idleFaded) {
            v.animate().cancel();
            if (ms <= 0) {
                v.setAlpha(1.0f);
            } else {
                v.animate().alpha(1.0f).setStartDelay(0L).setDuration(ms).setInterpolator(ease).withEndAction(null).start();
            }
        }
        this.idleFaded.clear();
        this.nowPlaying.setIdle(false, ms);
        View f = this.idleFocus;
        this.idleFocus = null;
        if (ms > 0 && f != null && f.isAttachedToWindow() && f.isShown() && getCurrentFocus() != f) {
            f.requestFocus();
        }
        Log.i("Home", "gallery mode off");
    }

    void enterDark() {
        if (this.dark != null) {
            return;
        }
        final View view = new View(this);
        this.dark = view;
        view.setBackgroundColor(-16777216);
        view.setAlpha(0.0f);
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.setOnKeyListener(new View.OnKeyListener() { // from class: com.chirag.home.MainActivity$$ExternalSyntheticLambda24
            @Override // android.view.View.OnKeyListener
            public final boolean onKey(View view2, int i, KeyEvent keyEvent) {
                return m32lambda$enterDark$5$comchiraghomeMainActivity(view2, i, keyEvent);
            }
        });
        this.stage.addView(view, 1, new FrameLayout.LayoutParams(-1, -1));
        this.stage.hold(true);
        AccelerateDecelerateInterpolator accelerateDecelerateInterpolator = new AccelerateDecelerateInterpolator();
        view.animate().alpha(1.0f).setDuration(DARK_FADE_MS).setInterpolator(accelerateDecelerateInterpolator).start();
        this.keepLit = this.nowPlaying.view();
        this.faded.clear();
        for (int i = 0; i < this.root.getChildCount(); i++) {
            View childAt = this.root.getChildAt(i);
            if (this.keepLit != null && isAncestor(childAt, this.keepLit) && (childAt instanceof ViewGroup)) {
                ViewGroup viewGroup = (ViewGroup) childAt;
                for (int i2 = 0; i2 < viewGroup.getChildCount(); i2++) {
                    if (!isAncestor(viewGroup.getChildAt(i2), this.keepLit)) {
                        this.faded.add(viewGroup.getChildAt(i2));
                    }
                }
            } else {
                this.faded.add(childAt);
            }
        }
        Iterator<View> it = this.faded.iterator();
        while (it.hasNext()) {
            it.next().animate().alpha(0.0f).setDuration(DARK_FADE_MS).setInterpolator(accelerateDecelerateInterpolator).start();
        }
        this.nowPlaying.setDimmed(true);
        this.nowPlaying.onShowingChanged = new Runnable() { // from class: com.chirag.home.MainActivity$$ExternalSyntheticLambda25
            @Override // java.lang.Runnable
            public final void run() {
                m33lambda$enterDark$6$comchiraghomeMainActivity();
            }
        };
        for (View view2 : this.faded) {
            if (view2 instanceof ViewGroup) {
                ((ViewGroup) view2).setDescendantFocusability(393216);
            }
        }
        Iterator<View> it2 = this.faded.iterator();
        while (it2.hasNext()) {
            it2.next().setFocusable(false);
        }
        view.post(new Runnable() { // from class: com.chirag.home.MainActivity$$ExternalSyntheticLambda26
            @Override // java.lang.Runnable
            public final void run() {
                m34lambda$enterDark$7$comchiraghomeMainActivity(view);
            }
        });
        Log.i("Home", "dark screen on");
    }

    /* JADX INFO: renamed from: lambda$enterDark$5$com-chirag-home-MainActivity, reason: not valid java name */
    /* synthetic */ boolean m32lambda$enterDark$5$comchiraghomeMainActivity(View view, int i, KeyEvent keyEvent) {
        if (keyEvent.getAction() != 0 || i == 4) {
            return false;
        }
        if ((this.keepLit != null && this.keepLit.getVisibility() == 0) || !(i == 23 || i == 66 || i == 19 || i == 20 || i == 21 || i == 22)) {
            return false;
        }
        leaveDark(true);
        return true;
    }

    /* JADX INFO: renamed from: lambda$enterDark$6$com-chirag-home-MainActivity, reason: not valid java name */
    /* synthetic */ void m33lambda$enterDark$6$comchiraghomeMainActivity() {
        if (this.dark != null) {
            if (this.nowPlaying.isShowing()) {
                this.nowPlaying.focusControls();
            } else {
                this.dark.requestFocus();
            }
        }
    }

    /* JADX INFO: renamed from: lambda$enterDark$7$com-chirag-home-MainActivity, reason: not valid java name */
    /* synthetic */ void m34lambda$enterDark$7$comchiraghomeMainActivity(View view) {
        if (this.dark != view || this.nowPlaying.focusControls()) {
            return;
        }
        view.requestFocus();
    }

    static boolean isAncestor(View view, View view2) {
        while (view2 != null) {
            if (view2 == view) {
                return true;
            }
            view2 = view2.getParent() instanceof View ? (View) view2.getParent() : null;
        }
        return false;
    }

    void leaveDark(boolean z) {
        if (this.dark == null) {
            return;
        }
        final View view = this.dark;
        this.dark = null;
        long j = z ? 500L : 0L;
        view.animate().cancel();
        view.animate().alpha(0.0f).setDuration(j).withEndAction(new Runnable() { // from class: com.chirag.home.MainActivity$$ExternalSyntheticLambda21
            @Override // java.lang.Runnable
            public final void run() {
                m37lambda$leaveDark$8$comchiraghomeMainActivity(view);
            }
        }).start();
        for (View view2 : this.faded) {
            view2.animate().cancel();
            view2.animate().alpha(1.0f).setDuration(j).start();
            if (view2 instanceof ViewGroup) {
                ((ViewGroup) view2).setDescendantFocusability(262144);
            } else if (view2 == this.caption) {
                view2.setFocusable(true);
            }
        }
        this.faded.clear();
        this.nowPlaying.onShowingChanged = null;
        this.nowPlaying.setDimmed(false);
        Log.i("Home", "dark screen off");
        this.stage.hold(false);
        if (this.caption != null) {
            this.caption.requestFocus();
        }
    }

    /* JADX INFO: renamed from: lambda$leaveDark$8$com-chirag-home-MainActivity, reason: not valid java name */
    /* synthetic */ void m37lambda$leaveDark$8$comchiraghomeMainActivity(View view) {
        this.stage.removeView(view);
    }

    @Override // android.app.Activity
    public void onBackPressed() {
        if (this.dark != null) {
            leaveDark(true);
            return;
        }
        if (this.story != null) {
            closeStory();
            return;
        }
        if (this.dialog != null) {
            closeDialog();
        } else if (this.showMore) {
            this.showMore = false;
            build();
        }
    }

    void openStory() {
        if (this.story != null) {
            return;
        }
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setFocusable(true);
        View view = new View(this);
        view.setBackground(new GradientDrawable(GradientDrawable.Orientation.BL_TR, new int[]{-436207616, -1728053248, 637534208, 0}));
        frameLayout.addView(view, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        this.sTitle = Ui.text(this, "", 21, -1, SERIF);
        this.sTitle.setAllCaps(true);
        this.sTitle.setLetterSpacing(0.14f);
        this.sTitle.setLineSpacing(0.0f, 1.15f);
        linearLayout.addView(this.sTitle);
        View view2 = new View(this);
        view2.setBackgroundColor(STORY_GOLD);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(Ui.dp(this, 54), Math.max(1, Ui.dp(this, 1)));
        layoutParams.setMargins(0, Ui.dp(this, 12), 0, Ui.dp(this, 10));
        linearLayout.addView(view2, layoutParams);
        this.sBy = Ui.text(this, "", 14, -218103809, SERIF);
        linearLayout.addView(this.sBy);
        this.sMuseum = Ui.text(this, "", 12, -855638017, SERIF_ITALIC);
        this.sMuseum.setPadding(0, Ui.dp(this, 3), 0, 0);
        linearLayout.addView(this.sMuseum);
        this.sAbout = Ui.text(this, "", 14, -218103809, SERIF_ITALIC);
        this.sAbout.setLineHeight(Math.round(this.sAbout.getPaint().getFontSpacing() * 1.38f));
        this.sAbout.setMaxLines(15);
        this.sAbout.setEllipsize(TextUtils.TruncateAt.END);
        this.sAbout.setPadding(0, Ui.dp(this, 14), 0, 0);
        linearLayout.addView(this.sAbout);
        FrameLayout.LayoutParams layoutParams2 = new FrameLayout.LayoutParams(Math.round(getResources().getDisplayMetrics().widthPixels * STORY_COLUMN), -2, 8388691);
        layoutParams2.setMargins(Ui.dp(this, 60), 0, 0, Ui.dp(this, 44));
        frameLayout.addView(linearLayout, layoutParams2);
        frameLayout.setOnKeyListener(new View.OnKeyListener() { // from class: com.chirag.home.MainActivity$$ExternalSyntheticLambda0
            @Override // android.view.View.OnKeyListener
            public final boolean onKey(View view3, int i, KeyEvent keyEvent) {
                return m42lambda$openStory$9$comchiraghomeMainActivity(view3, i, keyEvent);
            }
        });
        this.story = frameLayout;
        fillStory();
        this.stage.addView(frameLayout, new FrameLayout.LayoutParams(-1, -1));
        frameLayout.setAlpha(0.0f);
        frameLayout.animate().alpha(1.0f).setDuration(260L).start();
        frameLayout.requestFocus();
        this.page.animate().alpha(0.0f).setDuration(220L).withEndAction(new Runnable() { // from class: com.chirag.home.MainActivity$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                m41lambda$openStory$10$comchiraghomeMainActivity();
            }
        }).start();
        this.stage.bare(true);
        this.stage.hold(true);
    }

    /* JADX INFO: renamed from: lambda$openStory$9$com-chirag-home-MainActivity, reason: not valid java name */
    /* synthetic */ boolean m42lambda$openStory$9$comchiraghomeMainActivity(View view, int i, KeyEvent keyEvent) {
        if (keyEvent.getAction() != 0) {
            return i != 4;
        }
        if (i == 22) {
            this.stage.step(1);
            return true;
        }
        if (i == 21) {
            this.stage.step(-1);
            return true;
        }
        if (i != 23 && i != 66) {
            return i == 19 || i == 20;
        }
        closeStory();
        return true;
    }

    /* JADX INFO: renamed from: lambda$openStory$10$com-chirag-home-MainActivity, reason: not valid java name */
    /* synthetic */ void m41lambda$openStory$10$comchiraghomeMainActivity() {
        if (this.story != null) {
            this.page.setVisibility(4);
        }
    }

    void fillStory() {
        Art.Item itemShown = Art.shown();
        if (itemShown == null || this.story == null) {
            return;
        }
        this.sTitle.setText(itemShown.title);
        TextView textView = this.sBy;
        StringBuilder sbAppend = new StringBuilder().append(itemShown.artist);
        String str = "";
        if (!itemShown.year.isEmpty()) {
            str = (itemShown.artist.isEmpty() ? "" : ", ") + itemShown.year;
        }
        textView.setText(sbAppend.append(str).toString());
        this.sMuseum.setText(itemShown.museum);
        this.sMuseum.setVisibility(itemShown.museum.isEmpty() ? 8 : 0);
        if (!itemShown.about.isEmpty()) {
            SpannableString spannableString = new SpannableString(itemShown.about);
            spannableString.setSpan(new RelativeSizeSpan(2.3f), 0, 1, 0);
            spannableString.setSpan(new StyleSpan(0), 0, 1, 0);
            spannableString.setSpan(new ForegroundColorSpan(STORY_GOLD), 0, 1, 0);
            this.sAbout.setText(spannableString);
            this.sAbout.setVisibility(0);
            return;
        }
        this.sAbout.setVisibility(8);
    }

    void closeStory() {
        if (this.story == null) {
            return;
        }
        final View view = this.story;
        this.story = null;
        view.animate().alpha(0.0f).setDuration(200L).withEndAction(new Runnable() { // from class: com.chirag.home.MainActivity$$ExternalSyntheticLambda19
            @Override // java.lang.Runnable
            public final void run() {
                m31lambda$closeStory$11$comchiraghomeMainActivity(view);
            }
        }).start();
        this.page.animate().cancel();
        this.page.setVisibility(0);
        this.page.animate().alpha(1.0f).setDuration(240L).start();
        this.stage.bare(false);
        this.stage.hold(false);
        if (this.caption != null) {
            this.caption.requestFocus();
        }
    }

    /* JADX INFO: renamed from: lambda$closeStory$11$com-chirag-home-MainActivity, reason: not valid java name */
    /* synthetic */ void m31lambda$closeStory$11$comchiraghomeMainActivity(View view) {
        this.stage.removeView(view);
    }

    void closeDialog() {
        Ui.closeDialog(this.dialog);
        this.dialog = null;
        if (this.dialogFor != null) {
            this.dialogFor.requestFocus();
        }
        this.dialogFor = null;
    }

    void askRemove(final HistoryDb.Item item, final View view) {
        if (this.dialog != null) {
            return;
        }
        this.dialogFor = view;
        this.dialog = Ui.dialog(this.stage, (item.title.isEmpty() ? "Continue in " + Ui.label(this, item.pkg) : item.title) + ((item.subtitle == null || item.subtitle.isEmpty()) ? "" : "  ·  " + item.subtitle), "Remove", new Runnable() { // from class: com.chirag.home.MainActivity$$ExternalSyntheticLambda16
            @Override // java.lang.Runnable
            public final void run() {
                m21lambda$askRemove$12$comchiraghomeMainActivity(view, item);
            }
        }, new Runnable() { // from class: com.chirag.home.MainActivity$$ExternalSyntheticLambda17
            @Override // java.lang.Runnable
            public final void run() {
                closeDialog();
            }
        });
    }

    /* JADX INFO: renamed from: lambda$askRemove$12$com-chirag-home-MainActivity, reason: not valid java name */
    /* synthetic */ void m21lambda$askRemove$12$comchiraghomeMainActivity(View view, HistoryDb.Item item) {
        LinearLayout linearLayout = (LinearLayout) view.getParent();
        this.focusCard = linearLayout == null ? 0 : linearLayout.indexOfChild(view);
        try {
            HistoryDb.get(this).dismiss(item);
        } catch (Throwable th) {
        }
        Ui.closeDialog(this.dialog);
        this.dialog = null;
        this.dialogFor = null;
        view.animate().alpha(0.0f).setDuration(150L).withEndAction(new Runnable() { // from class: com.chirag.home.MainActivity$$ExternalSyntheticLambda18
            @Override // java.lang.Runnable
            public final void run() {
                build();
            }
        }).start();
    }

    LinearLayout header() {
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setClipChildren(false);
        frameLayout.setClipToPadding(false);
        this.clock = Ui.text(this, "", CLOCK_SP, -1191182337, Ui.LIGHT);
        this.clock.setLetterSpacing(-0.02f);
        this.clock.setShadowLayer(Ui.dp(this, 10), 0.0f, Ui.dp(this, 1), 1073741824);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-2, -2, 1);
        layoutParams.topMargin = Ui.dp(this, 66);
        frameLayout.addView(this.clock, layoutParams);
        this.date = Ui.text(this, "", 16, -335544321, Ui.MEDIUM);
        FrameLayout.LayoutParams layoutParams2 = new FrameLayout.LayoutParams(-2, -2, 1);
        layoutParams2.topMargin = Ui.dp(this, 54);
        frameLayout.addView(this.date, layoutParams2);
        this.caption = Ui.text(this, Art.caption(), 12, CAPTION_REST, Ui.MEDIUM);
        this.caption.setShadowLayer(Ui.dp(this, 5), 0.0f, Ui.dp(this, 1), -654311424);
        this.caption.setPadding(Ui.dp(this, 12), Ui.dp(this, 5), Ui.dp(this, 12), Ui.dp(this, 5));
        this.caption.setId(View.generateViewId());
        Ui.focusable(this, this.caption, null, 0, 1.0f, new Runnable() { // from class: com.chirag.home.MainActivity$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                openStory();
            }
        }, null, new Ui.OnFocus() { // from class: com.chirag.home.MainActivity$$ExternalSyntheticLambda3
            @Override // com.chirag.home.Ui.OnFocus
            public final void changed(boolean z) {
                m35lambda$header$13$comchiraghomeMainActivity(z);
            }
        });
        this.caption.setNextFocusLeftId(this.caption.getId());
        this.caption.setNextFocusRightId(this.caption.getId());
        FrameLayout.LayoutParams layoutParams3 = new FrameLayout.LayoutParams(-2, -2, 1);
        layoutParams3.topMargin = Ui.dp(this, 239);
        frameLayout.addView(this.caption, layoutParams3);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setGravity(16);
        linearLayout.setClipChildren(false);
        linearLayout.setClipToPadding(false);
        linearLayout.setPadding(Ui.dp(this, 52), 0, Ui.dp(this, 52), 0);
        this.inputsPill = Ui.pill(this, 1, "Inputs", 0, new Runnable() { // from class: com.chirag.home.MainActivity$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                m36lambda$header$14$comchiraghomeMainActivity();
            }
        });
        ((LinearLayout.LayoutParams) this.inputsPill.getLayoutParams()).setMargins(0, 0, 0, 0);
        linearLayout.addView(this.inputsPill);
        linearLayout.addView(new View(this), new LinearLayout.LayoutParams(0, 0, 1.0f));
        FrameLayout.LayoutParams layoutParams4 = new FrameLayout.LayoutParams(-1, -2);
        layoutParams4.topMargin = Ui.dp(this, 32);
        frameLayout.addView(linearLayout, layoutParams4);
        this.root.addView(frameLayout, new LinearLayout.LayoutParams(-1, Ui.dp(this, HEAD_H)));
        return linearLayout;
    }

    /* JADX INFO: renamed from: lambda$header$13$com-chirag-home-MainActivity, reason: not valid java name */
    /* synthetic */ void m35lambda$header$13$comchiraghomeMainActivity(boolean z) {
        this.caption.setTextColor(z ? -1 : CAPTION_REST);
        this.caption.setShadowLayer(Ui.dp(this, z ? 9 : 5), 0.0f, Ui.dp(this, 1), z ? -16777216 : -654311424);
    }

    /* JADX INFO: renamed from: lambda$header$14$com-chirag-home-MainActivity, reason: not valid java name */
    /* synthetic */ void m36lambda$header$14$comchiraghomeMainActivity() {
        startActivity(new Intent(this, (Class<?>) InputsActivity.class));
    }

    void build() {
        View childAt;
        this.root.removeAllViews();
        LinearLayout linearLayoutHeader = header();
        updateHeader();
        linearLayoutHeader.addView(Ui.disc(this, 17, "Dark screen", new Runnable() { // from class: com.chirag.home.MainActivity$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                enterDark();
            }
        }));
        linearLayoutHeader.addView(Ui.disc(this, 4, "Settings", new Runnable() { // from class: com.chirag.home.MainActivity$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                m22lambda$build$15$comchiraghomeMainActivity();
            }
        }));
        View viewDisc = Ui.disc(this, this.showMore ? 6 : 5, this.showMore ? "Hide apps" : "More apps", new Runnable() { // from class: com.chirag.home.MainActivity$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                m23lambda$build$16$comchiraghomeMainActivity();
            }
        });
        linearLayoutHeader.addView(viewDisc);
        final Inputs.Item itemFind = Inputs.find(this, "PlayStation");
        this.ps5 = null;
        if (itemFind != null) {
            this.ps5 = Ui.disc(this, 13, "PlayStation", new Runnable() { // from class: com.chirag.home.MainActivity$$ExternalSyntheticLambda9
                @Override // java.lang.Runnable
                public final void run() {
                    m24lambda$build$17$comchiraghomeMainActivity(itemFind);
                }
            });
            linearLayoutHeader.addView(this.ps5);
            this.ps5.setId(View.generateViewId());
            viewDisc.setId(View.generateViewId());
            viewDisc.setNextFocusRightId(this.ps5.getId());
            this.ps5.setNextFocusLeftId(viewDisc.getId());
        }
        List<HistoryDb.Item> listRecent = HistoryDb.get(this).recent(null, System.currentTimeMillis() - MONTH, 15, true);
        Ui.heading(this, this.root, "Continue watching", 79, 16);
        if (!listRecent.isEmpty()) {
            LinearLayout linearLayoutRow = Ui.row(this, this.root, 77, 8, 0);
            childAt = null;
            for (final HistoryDb.Item item : listRecent) {
                final View[] viewArr = new View[1];
                View viewResumeCard = Ui.resumeCard(this, item, new Runnable() { // from class: com.chirag.home.MainActivity$$ExternalSyntheticLambda10
                    @Override // java.lang.Runnable
                    public final void run() {
                        m25lambda$build$18$comchiraghomeMainActivity(item);
                    }
                }, new Runnable() { // from class: com.chirag.home.MainActivity$$ExternalSyntheticLambda11
                    @Override // java.lang.Runnable
                    public final void run() {
                        m26lambda$build$19$comchiraghomeMainActivity(item, viewArr);
                    }
                });
                viewArr[0] = viewResumeCard;
                viewResumeCard.setNextFocusUpId(this.caption.getId());
                linearLayoutRow.addView(viewResumeCard);
                if (childAt == null) {
                    childAt = viewResumeCard;
                }
            }
            if (this.focusCard >= 0) {
                childAt = linearLayoutRow.getChildAt(Math.min(this.focusCard, linearLayoutRow.getChildCount() - 1));
            }
            Ui.loop(linearLayoutRow);
        } else {
            if (!listenerEnabled()) {
                this.root.addView(Ui.notice(this, "Watch history is off", "Notification access for Home isn't granted, so nothing can be remembered yet.", -11654, 77));
            } else {
                this.root.addView(Ui.notice(this, "Nothing to continue", "Whatever you start watching will wait for you here.", -1, 77));
            }
            childAt = null;
        }
        this.root.addView(new View(this), new LinearLayout.LayoutParams(-1, 0, 1.0f));
        LinearLayout linearLayoutDock = Ui.dock(this, this.root);
        View view = null;
        for (final String str : FAVORITES) {
            View viewOrb = Ui.orb(this, str, true, new Runnable() { // from class: com.chirag.home.MainActivity$$ExternalSyntheticLambda12
                @Override // java.lang.Runnable
                public final void run() {
                    openFavorite(str);
                }
            }, new Runnable() { // from class: com.chirag.home.MainActivity$$ExternalSyntheticLambda13
                @Override // java.lang.Runnable
                public final void run() {
                    m28lambda$build$21$comchiraghomeMainActivity(str);
                }
            });
            if (viewOrb != null) {
                linearLayoutDock.addView(viewOrb);
                if (view == null) {
                    view = viewOrb;
                }
            }
        }
        Ui.loop(linearLayoutDock);
        Ui.ends(this.inputsPill, this.ps5 != null ? this.ps5 : viewDisc);
        for (int i = 0; i < linearLayoutHeader.getChildCount(); i++) {
            linearLayoutHeader.getChildAt(i).setNextFocusDownId(this.caption.getId());
        }
        this.caption.setNextFocusUpId(this.inputsPill.getId());
        this.focusCard = -1;
        if (childAt == null) {
            childAt = view;
        }
        if (childAt == null) {
            childAt = this.inputsPill;
        }
        if (this.showMore) {
            Ui.heading(this, this.root, "More apps", 79, 22);
            LinearLayout linearLayoutRow2 = Ui.row(this, this.root, 66, 16, 14);
            List<ResolveInfo> listQueryIntentActivities = getPackageManager().queryIntentActivities(new Intent("android.intent.action.MAIN").addCategory("android.intent.category.LEANBACK_LAUNCHER"), 0);
            ArrayList<String> arrayList = new ArrayList();
            Iterator<ResolveInfo> it = listQueryIntentActivities.iterator();
            while (it.hasNext()) {
                String str2 = it.next().activityInfo.packageName;
                if (!FAVORITES.contains(str2) && !str2.equals(getPackageName()) && !arrayList.contains(str2) && !str2.equals(AIRPLAY) && !str2.equals("com.android.tv.settings") && !str2.equals("com.smartdevice.livetv")) {
                    arrayList.add(str2);
                }
            }
            Collections.sort(arrayList, new Comparator() { // from class: com.chirag.home.MainActivity$$ExternalSyntheticLambda14
                @Override // java.util.Comparator
                public final int compare(Object obj, Object obj2) {
                    return m29lambda$build$22$comchiraghomeMainActivity((String) obj, (String) obj2);
                }
            });
            View view2 = null;
            for (final String str3 : arrayList) {
                View viewOrb2 = Ui.orb(this, str3, false, new Runnable() { // from class: com.chirag.home.MainActivity$$ExternalSyntheticLambda15
                    @Override // java.lang.Runnable
                    public final void run() {
                        m30lambda$build$23$comchiraghomeMainActivity(str3);
                    }
                }, null);
                if (viewOrb2 != null) {
                    linearLayoutRow2.addView(viewOrb2);
                    if (view2 == null) {
                        view2 = viewOrb2;
                    }
                }
            }
            Ui.loop(linearLayoutRow2);
            if (view2 != null) {
                viewDisc = view2;
            }
            childAt = viewDisc;
        }
        this.nowPlaying.attach(this.root, this.caption, this.clock);
        if (childAt != null) {
            childAt.requestFocus();
        }
    }

    /* JADX INFO: renamed from: lambda$build$15$com-chirag-home-MainActivity, reason: not valid java name */
    /* synthetic */ void m22lambda$build$15$comchiraghomeMainActivity() {
        Ui.launch(this, new Intent("android.settings.SETTINGS").addFlags(32768));
    }

    /* JADX INFO: renamed from: lambda$build$16$com-chirag-home-MainActivity, reason: not valid java name */
    /* synthetic */ void m23lambda$build$16$comchiraghomeMainActivity() {
        this.showMore = !this.showMore;
        build();
    }

    /* JADX INFO: renamed from: lambda$build$17$com-chirag-home-MainActivity, reason: not valid java name */
    /* synthetic */ void m24lambda$build$17$comchiraghomeMainActivity(Inputs.Item item) {
        Ui.launch(this, Inputs.open(item));
    }

    /* JADX INFO: renamed from: lambda$build$18$com-chirag-home-MainActivity, reason: not valid java name */
    /* synthetic */ void m25lambda$build$18$comchiraghomeMainActivity(HistoryDb.Item item) {
        Ui.resume(this, item);
    }

    /* JADX INFO: renamed from: lambda$build$19$com-chirag-home-MainActivity, reason: not valid java name */
    /* synthetic */ void m26lambda$build$19$comchiraghomeMainActivity(HistoryDb.Item item, View[] viewArr) {
        askRemove(item, viewArr[0]);
    }

    /* JADX INFO: renamed from: lambda$build$21$com-chirag-home-MainActivity, reason: not valid java name */
    /* synthetic */ void m28lambda$build$21$comchiraghomeMainActivity(String str) {
        Ui.launch(this, Ui.launchIntent(this, str));
    }

    /* JADX INFO: renamed from: lambda$build$22$com-chirag-home-MainActivity, reason: not valid java name */
    /* synthetic */ int m29lambda$build$22$comchiraghomeMainActivity(String str, String str2) {
        return Ui.label(this, str).compareToIgnoreCase(Ui.label(this, str2));
    }

    /* JADX INFO: renamed from: lambda$build$23$com-chirag-home-MainActivity, reason: not valid java name */
    /* synthetic */ void m30lambda$build$23$comchiraghomeMainActivity(String str) {
        Ui.launch(this, Ui.launchIntent(this, str));
    }

    public void openFavorite(String str) {
        if (HistoryDb.get(this).count(str) == 0) {
            Ui.launch(this, Ui.launchIntent(this, str));
        } else {
            startActivity(new Intent(this, (Class<?>) HistoryActivity.class).putExtra("pkg", str));
        }
    }

    boolean listenerEnabled() {
        String string = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return string != null && string.contains(getPackageName());
    }
}
