package com.chirag.home;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.StateListAnimator;
import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Property;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: classes.dex */
final class Ui {
    static final int ANIM = 150;
    static final int BG = -16052975;
    static final int CARD_H = 78;
    static final int CARD_W = 260;
    static final int CHIP = 1;
    static final int CHIP_ON = 2;
    static final int DISC = 44;
    static final int DOCK_PAD = 14;
    static final int EDGE = 60;
    static final int GAP = 13;
    static final int ICON = 76;
    static final int ICON_CELL = 98;
    static final float ICON_FOCUS = 1.07f;
    static final int INK = -14869217;
    static final int INK_2 = -1289937633;
    static final int OK = -8463206;
    static final int PILL_H = 40;
    static final int RADIUS = 14;
    static final int SHADOW = Integer.MIN_VALUE;
    static final int SIDE = 77;
    static final int SURFACE = -15197406;
    static final int TEXT = -1;
    static final int TEXT_2 = -855638017;
    static final int TEXT_3 = -1627389953;
    static final int TILE_RADIUS = 22;
    static final int WARN = -11654;
    static final int PLAIN = 0;
    static final Typeface LIGHT = Typeface.create("sans-serif-light", PLAIN);
    static final Typeface REGULAR = Typeface.create("sans-serif", PLAIN);
    static final Typeface MEDIUM = Typeface.create("sans-serif-medium", PLAIN);
    static final ComponentName GOOGLE_TV = new ComponentName("com.google.android.apps.tv.launcherx", "com.google.android.apps.tv.launcherx.home.HomeActivity");

    interface OnFocus {
        void changed(boolean z);
    }

    Ui() {
    }

    static int dp(Context context, int i) {
        return Math.round(i * context.getResources().getDisplayMetrics().density);
    }

    static TextView text(Context context, String str, int i, int i2, Typeface typeface) {
        TextView textView = new TextView(context);
        textView.setText(str);
        textView.setTextSize(i);
        textView.setTextColor(i2);
        textView.setTypeface(typeface);
        textView.setIncludeFontPadding(false);
        shade(textView, true);
        return textView;
    }

    static void shade(TextView textView, boolean z) {
        float f = textView.getResources().getDisplayMetrics().density;
        if (z) {
            textView.setShadowLayer(3.0f * f, 0.0f, f / 2.0f, SHADOW);
        } else {
            textView.setShadowLayer(0.0f, 0.0f, 0.0f, PLAIN);
        }
    }

    private static void ink(TextView textView, boolean z, int i, int i2) {
        if (!z) {
            i = i2;
        }
        textView.setTextColor(i);
        shade(textView, !z);
    }

    static GradientDrawable shape(Context context, int i, int i2, int i3) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(i);
        gradientDrawable.setCornerRadius(dp(context, i2));
        if (i3 != 0) {
            gradientDrawable.setStroke(dp(context, CHIP), i3);
        }
        return gradientDrawable;
    }

    static final class Stage extends FrameLayout {
        private ValueAnimator fade;
        private Glass[] fading;
        private final ArrayList<Glass> glass;
        private boolean held;
        Runnable onArt;
        private boolean running;
        private final Runnable turn;
        private int turnId;
        private final Wall.Surface wall;

        Stage(Context context, View view) {
            super(context);
            this.glass = new ArrayList<>();
            this.turn = new Runnable() { // from class: com.chirag.home.Ui.Stage.1
                @Override // java.lang.Runnable
                public void run() {
                    if (Stage.this.held) {
                        return;
                    }
                    Stage.this.rotate(Ui.CHIP);
                }
            };
            setBackgroundColor(Ui.BG);
            this.wall = new Wall.Surface(context);
            addView(this.wall, new FrameLayout.LayoutParams(Ui.TEXT, Ui.TEXT));
            addView(view, new FrameLayout.LayoutParams(Ui.TEXT, Ui.TEXT));
        }

        static Stage of(View view) {
            for (ViewParent parent = view.getParent(); parent != null; parent = parent.getParent()) {
                if (parent instanceof Stage) {
                    return (Stage) parent;
                }
            }
            return null;
        }

        void add(Glass glass) {
            if (this.glass.size() > 96) {
                prune();
            }
            if (!this.glass.contains(glass)) {
                this.glass.add(glass);
            }
        }

        private void prune() {
            for (int size = this.glass.size() + Ui.TEXT; size >= 0; size += Ui.TEXT) {
                if (this.glass.get(size).geo() == null) {
                    this.glass.remove(size);
                }
            }
        }

        void resume() {
            this.running = true;
            this.wall.sync();
            if (this.onArt != null) {
                this.onArt.run();
            }
            schedule();
        }

        void pause() {
            this.running = false;
            this.turnId += Ui.CHIP;
            removeCallbacks(this.turn);
            if (this.fade != null) {
                this.fade.end();
            }
        }

        void hold(boolean z) {
            this.held = z;
            if (z) {
                removeCallbacks(this.turn);
            } else if (this.running) {
                schedule();
            }
        }

        void step(int i) {
            rotate(i);
        }

        void bare(boolean z) {
            ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(z ? Ui.PLAIN : 1.0f, z ? 1.0f : 0.0f);
            valueAnimatorOfFloat.setDuration(260L);
            valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.chirag.home.Ui$Stage$$ExternalSyntheticLambda1
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                    m54lambda$bare$0$comchiraghomeUi$Stage(valueAnimator);
                }
            });
            valueAnimatorOfFloat.start();
        }

        /* JADX INFO: renamed from: lambda$bare$0$com-chirag-home-Ui$Stage, reason: not valid java name */
        /* synthetic */ void m54lambda$bare$0$comchiraghomeUi$Stage(ValueAnimator valueAnimator) {
            this.wall.bare(((Float) valueAnimator.getAnimatedValue()).floatValue());
        }

        private void schedule() {
            removeCallbacks(this.turn);
            if (this.held) {
                return;
            }
            postDelayed(this.turn, Math.max(1500L, 30000 - (SystemClock.uptimeMillis() - Art.since)));
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void rotate(final int i) {
            if (this.running) {
                if (this.fade != null && this.fade.isRunning()) {
                    return;
                }
                prune();
                final Context applicationContext = getContext().getApplicationContext();
                final float f = getResources().getDisplayMetrics().density;
                final int i2 = this.turnId + Ui.CHIP;
                this.turnId = i2;
                ArrayList<Glass> arrayList = this.glass;
                final Glass[] glassArr = (Glass[]) arrayList.toArray(new Glass[Ui.PLAIN]);
                final int[][] iArr = new int[glassArr.length][];
                for (int i3 = Ui.PLAIN; i3 < glassArr.length; i3 += Ui.CHIP) {
                    iArr[i3] = glassArr[i3].geo();
                }
                Lens.EXEC.execute(new Runnable() { // from class: com.chirag.home.Ui$Stage$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        m57lambda$rotate$3$comchiraghomeUi$Stage(i, applicationContext, glassArr, iArr, f, i2);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$rotate$3$com-chirag-home-Ui$Stage, reason: not valid java name */
        /* synthetic */ void m57lambda$rotate$3$comchiraghomeUi$Stage(int i, Context context, final Glass[] glassArr, int[][] iArr, float f, final int i2) {
            final Art.Pick pickPrevious = i < 0 ? Art.previous(context) : Art.next(context);
            final Bitmap bitmap = pickPrevious == null ? null : pickPrevious.bitmap;
            final float f2 = pickPrevious == null ? 1.0f : pickPrevious.boost;
            final Bitmap[] bitmapArr = new Bitmap[glassArr.length];
            if (bitmap != null) {
                for (int i3 = Ui.PLAIN; i3 < glassArr.length; i3 += Ui.CHIP) {
                    if (iArr[i3] != null) {
                        bitmapArr[i3] = Lens.make(bitmap, f2, Lens.key(pickPrevious.id, iArr[i3]), iArr[i3], f);
                    }
                }
            }
            post(new Runnable() { // from class: com.chirag.home.Ui$Stage$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    m56lambda$rotate$2$comchiraghomeUi$Stage(i2, bitmap, pickPrevious, f2, glassArr, bitmapArr);
                }
            });
        }

        /* JADX INFO: renamed from: lambda$rotate$2$com-chirag-home-Ui$Stage, reason: not valid java name */
        /* synthetic */ void m56lambda$rotate$2$comchiraghomeUi$Stage(int i, Bitmap bitmap, Art.Pick pick, float f, Glass[] glassArr, Bitmap[] bitmapArr) {
            if (!this.running || i != this.turnId) {
                return;
            }
            if (bitmap == null) {
                schedule();
                return;
            }
            Art.show(pick);
            this.wall.show(bitmap, f);
            for (int i2 = Ui.PLAIN; i2 < glassArr.length; i2 += Ui.CHIP) {
                glassArr[i2].fadeTo(bitmapArr[i2]);
            }
            this.fading = glassArr;
            this.fade = ValueAnimator.ofFloat(0.0f, 1.0f);
            this.fade.setDuration(Art.fadeMs());
            this.fade.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.chirag.home.Ui$Stage$$ExternalSyntheticLambda3
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                    m55lambda$rotate$1$comchiraghomeUi$Stage(valueAnimator);
                }
            });
            this.fade.addListener(new AnimatorListenerAdapter() { // from class: com.chirag.home.Ui.Stage.2
                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animator) {
                    Stage.this.wall.settle();
                    if (Stage.this.fading != null) {
                        Glass[] glassArr2 = Stage.this.fading;
                        int length = glassArr2.length;
                        for (int i3 = Ui.PLAIN; i3 < length; i3 += Ui.CHIP) {
                            glassArr2[i3].settle();
                        }
                    }
                    Stage.this.fading = null;
                }
            });
            this.fade.start();
            if (this.onArt != null) {
                this.onArt.run();
            }
            schedule();
        }

        /* JADX INFO: renamed from: lambda$rotate$1$com-chirag-home-Ui$Stage, reason: not valid java name */
        /* synthetic */ void m55lambda$rotate$1$comchiraghomeUi$Stage(ValueAnimator valueAnimator) {
            float fFloatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
            this.wall.fade(fFloatValue);
            if (this.fading != null) {
                Glass[] glassArr = this.fading;
                int length = glassArr.length;
                for (int i = Ui.PLAIN; i < length; i += Ui.CHIP) {
                    glassArr[i].fade(fFloatValue);
                }
            }
        }
    }

    static final class Page extends ScrollView {
        Page(Context context) {
            super(context);
            setFillViewport(true);
            setVerticalScrollBarEnabled(false);
            setClipChildren(false);
        }

        @Override // android.widget.ScrollView
        protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
            return getChildCount() == 0 ? Ui.PLAIN : Math.max(Ui.PLAIN, Math.min(Math.max(Ui.PLAIN, getChildAt(Ui.PLAIN).getHeight() - getHeight()), rect.centerY() - (getHeight() / Ui.CHIP_ON))) - getScrollY();
        }
    }

    static final class Rail extends HorizontalScrollView {
        Rail(Context context) {
            super(context);
            setHorizontalScrollBarEnabled(false);
            setClipToPadding(false);
            setClipChildren(false);
        }

        @Override // android.widget.HorizontalScrollView, android.view.ViewGroup, android.view.View
        public boolean dispatchKeyEvent(KeyEvent keyEvent) {
            View viewFindViewById;
            int keyCode = keyEvent.getKeyCode();
            if (keyEvent.getAction() == 0 && (keyCode == Ui.TILE_RADIUS || keyCode == 21)) {
                View viewFindFocus = findFocus();
                while (viewFindFocus != null && (viewFindFocus.getParent() instanceof View) && viewFindFocus.getParent() != getChildAt(Ui.PLAIN)) {
                    viewFindFocus = (View) viewFindFocus.getParent();
                }
                if (viewFindFocus != null && viewFindFocus.getParent() == getChildAt(Ui.PLAIN)) {
                    int nextFocusRightId = keyCode == Ui.TILE_RADIUS ? viewFindFocus.getNextFocusRightId() : viewFindFocus.getNextFocusLeftId();
                    if (nextFocusRightId != Ui.TEXT && nextFocusRightId != viewFindFocus.getId() && (viewFindViewById = ((View) viewFindFocus.getParent()).findViewById(nextFocusRightId)) != null && viewFindViewById.requestFocus()) {
                        return true;
                    }
                }
            }
            return super.dispatchKeyEvent(keyEvent);
        }

        @Override // android.widget.HorizontalScrollView
        protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
            return getChildCount() == 0 ? Ui.PLAIN : Math.max(Ui.PLAIN, Math.min(Math.max(Ui.PLAIN, ((getChildAt(Ui.PLAIN).getWidth() + getPaddingLeft()) + getPaddingRight()) - getWidth()), rect.left - getPaddingLeft())) - getScrollX();
        }
    }

    static TextView heading(Context context, LinearLayout linearLayout, String str, int i, int i2) {
        TextView textViewText = text(context, str, GAP, -335544321, MEDIUM);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(TEXT, -2);
        layoutParams.setMargins(dp(context, i), dp(context, i2), dp(context, i), PLAIN);
        linearLayout.addView(textViewText, layoutParams);
        return textViewText;
    }

    static LinearLayout row(Context context, LinearLayout linearLayout, int i, int i2) {
        return row(context, linearLayout, EDGE, i, i2);
    }

    static LinearLayout row(Context context, LinearLayout linearLayout, int i, int i2, int i3) {
        Rail rail = new Rail(context);
        rail.setPadding(dp(context, i), dp(context, i2), dp(context, i), dp(context, i3));
        LinearLayout linearLayout2 = new LinearLayout(context);
        linearLayout2.setClipChildren(false);
        linearLayout2.setGravity(16);
        rail.addView(linearLayout2);
        linearLayout.addView(rail);
        return linearLayout2;
    }

    static void loop(LinearLayout linearLayout) {
        int childCount = linearLayout.getChildCount();
        if (childCount == 0) {
            return;
        }
        View childAt = linearLayout.getChildAt(PLAIN);
        View childAt2 = linearLayout.getChildAt(childCount + TEXT);
        if (childAt.getId() == TEXT) {
            childAt.setId(View.generateViewId());
        }
        if (childAt2.getId() == TEXT) {
            childAt2.setId(View.generateViewId());
        }
        childAt.setNextFocusLeftId(childAt2.getId());
        childAt2.setNextFocusRightId(childAt.getId());
    }

    static void ends(View view, View view2) {
        if (view.getId() == TEXT) {
            view.setId(View.generateViewId());
        }
        if (view2.getId() == TEXT) {
            view2.setId(View.generateViewId());
        }
        view.setNextFocusLeftId(view.getId());
        view2.setNextFocusRightId(view2.getId());
    }

    static View focusable(final Context context, View view, final View view2, final int i, float f, final Runnable runnable, final Runnable runnable2, final OnFocus onFocus) {
        view.setFocusable(true);
        view.setClickable(true);
        view.setOnClickListener(new View.OnClickListener() { // from class: com.chirag.home.Ui$$ExternalSyntheticLambda5
            @Override // android.view.View.OnClickListener
            public final void onClick(View view3) {
                runnable.run();
            }
        });
        if (runnable2 != null) {
            view.setOnLongClickListener(new View.OnLongClickListener() { // from class: com.chirag.home.Ui$$ExternalSyntheticLambda6
                @Override // android.view.View.OnLongClickListener
                public final boolean onLongClick(View view3) {
                    return Ui.lambda$focusable$1(runnable2, view3);
                }
            });
        }
        if (view2 != null) {
            view2.setForeground(new Edge(dp(context, i), dp(context, CHIP), PLAIN));
            lift(view2, false);
        }
        view.setStateListAnimator(motion(f));
        view.setOnFocusChangeListener(new View.OnFocusChangeListener() { // from class: com.chirag.home.Ui$$ExternalSyntheticLambda7
            @Override // android.view.View.OnFocusChangeListener
            public final void onFocusChange(View view3, boolean z) {
                Ui.lambda$focusable$2(view2, context, i, onFocus, view3, z);
            }
        });
        return view;
    }

    static /* synthetic */ boolean lambda$focusable$1(Runnable runnable, View view) {
        runnable.run();
        return true;
    }

    static /* synthetic */ void lambda$focusable$2(View view, Context context, int i, OnFocus onFocus, View view2, boolean z) {
        if (view != null) {
            view.setForeground(new Edge(dp(context, i), z ? dp(context, 3) / 2.0f : dp(context, CHIP), z ? -218103809 : PLAIN));
            lift(view, z);
        }
        if (onFocus != null) {
            onFocus.changed(z);
        }
    }

    static StateListAnimator motion(float f) {
        return motion(f, 0.0f);
    }

    static StateListAnimator motion(float f, float f2) {
        StateListAnimator stateListAnimator = new StateListAnimator();
        float f3 = -f2;
        stateListAnimator.addState(new int[]{android.R.attr.state_focused, android.R.attr.state_pressed}, scaleTo(f - 0.07f, f3, 80));
        stateListAnimator.addState(new int[]{android.R.attr.state_focused}, scaleTo(f, f3, ANIM));
        stateListAnimator.addState(new int[PLAIN], scaleTo(1.0f, 0.0f, ANIM));
        return stateListAnimator;
    }

    private static ObjectAnimator scaleTo(float f, float f2, int i) {
        ObjectAnimator objectAnimator = new ObjectAnimator();
        objectAnimator.setValues(PropertyValuesHolder.ofFloat((Property<?, Float>) View.SCALE_X, f), PropertyValuesHolder.ofFloat((Property<?, Float>) View.SCALE_Y, f), PropertyValuesHolder.ofFloat((Property<?, Float>) View.TRANSLATION_Y, f2));
        objectAnimator.setDuration(i);
        return objectAnimator;
    }

    static Glass glass(View view, int i) {
        Glass glassOn = Glass.on(view, new Glass(view.getContext(), i));
        liftGlass(view, false);
        return glassOn;
    }

    static void liftGlass(View view, boolean z) {
        view.setOutlineSpotShadowColor(-16777216);
        view.setOutlineAmbientShadowColor(-16777216);
        view.setElevation(z ? dp(view.getContext(), 14) : 0.0f);
    }

    static void lift(View view, boolean z) {
        view.setOutlineSpotShadowColor(-16777216);
        view.setOutlineAmbientShadowColor(-16777216);
        view.setElevation(dp(view.getContext(), z ? 14 : CHIP_ON));
    }

    static void liftIcon(View view, boolean z) {
        view.setOutlineSpotShadowColor(-16777216);
        view.setOutlineAmbientShadowColor(-16777216);
        view.setElevation(dp(view.getContext(), z ? 11 : 6));
    }

    static LinearLayout.LayoutParams cell(Context context, int i, int i2, int i3) {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(i, i2);
        layoutParams.setMargins(PLAIN, PLAIN, dp(context, i3), PLAIN);
        return layoutParams;
    }

    static TextView choice(final Context context, String str, Runnable runnable) {
        final TextView textViewText = text(context, str, 14, TEXT, MEDIUM);
        final Smoke smoke = new Smoke(context, TEXT, Smoke.FIELD);
        textViewText.setBackground(smoke);
        textViewText.setGravity(17);
        textViewText.setPadding(dp(context, 20), PLAIN, dp(context, 20), PLAIN);
        focusable(context, textViewText, null, PLAIN, 1.06f, runnable, null, new OnFocus() { // from class: com.chirag.home.Ui$$ExternalSyntheticLambda3
            @Override // com.chirag.home.Ui.OnFocus
            public final void changed(boolean z) {
                Ui.lambda$choice$3(smoke, textViewText, context, z);
            }
        });
        return textViewText;
    }

    static /* synthetic */ void lambda$choice$3(Smoke smoke, TextView textView, Context context, boolean z) {
        smoke.mode(z ? CHIP_ON : PLAIN);
        textView.invalidateOutline();
        ink(textView, z, INK, TEXT);
        textView.setElevation(z ? dp(context, 10) : 0.0f);
    }

    static View dialog(Stage stage, String str, String str2, Runnable runnable, Runnable runnable2) {
        Context context = stage.getContext();
        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setClickable(true);
        frameLayout.setBackgroundColor(1493172224);
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(CHIP);
        linearLayout.setGravity(CHIP);
        linearLayout.setClipChildren(false);
        linearLayout.setClipToPadding(false);
        linearLayout.setPadding(dp(context, 30), dp(context, 20), dp(context, 30), dp(context, TILE_RADIUS));
        Glass.on(linearLayout, new Glass(context, 26).body(-1475473388, 0.5f));
        linearLayout.setClipToOutline(false);
        TextView textViewText = text(context, str, GAP, TEXT_2, REGULAR);
        textViewText.setSingleLine(true);
        textViewText.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        textViewText.setMaxWidth(dp(context, 300));
        linearLayout.addView(textViewText);
        LinearLayout linearLayout2 = new LinearLayout(context);
        linearLayout2.setClipChildren(false);
        linearLayout2.setPadding(dp(context, 8), dp(context, 14), dp(context, 8), dp(context, 6));
        linearLayout2.setClipToPadding(false);
        TextView textViewChoice = choice(context, "Cancel", runnable2);
        TextView textViewChoice2 = choice(context, str2, runnable);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dp(context, 120), dp(context, PILL_H));
        layoutParams.setMargins(PLAIN, PLAIN, dp(context, 16), PLAIN);
        linearLayout2.addView(textViewChoice, layoutParams);
        linearLayout2.addView(textViewChoice2, new LinearLayout.LayoutParams(dp(context, 120), dp(context, PILL_H)));
        linearLayout.addView(linearLayout2, new LinearLayout.LayoutParams(-2, -2));
        textViewChoice.setId(View.generateViewId());
        textViewChoice2.setId(View.generateViewId());
        View[] viewArr = {textViewChoice, textViewChoice2};
        for (int i = PLAIN; i < CHIP_ON; i += CHIP) {
            View view = viewArr[i];
            view.setNextFocusUpId(view.getId());
            view.setNextFocusDownId(view.getId());
        }
        textViewChoice.setNextFocusLeftId(textViewChoice2.getId());
        textViewChoice.setNextFocusRightId(textViewChoice2.getId());
        textViewChoice2.setNextFocusLeftId(textViewChoice.getId());
        textViewChoice2.setNextFocusRightId(textViewChoice.getId());
        FrameLayout.LayoutParams layoutParams2 = new FrameLayout.LayoutParams(-2, -2, 81);
        layoutParams2.bottomMargin = dp(context, ANIM);
        frameLayout.addView(linearLayout, layoutParams2);
        stage.addView(frameLayout, new FrameLayout.LayoutParams(TEXT, TEXT));
        frameLayout.setAlpha(0.0f);
        linearLayout.setScaleX(0.96f);
        linearLayout.setScaleY(0.96f);
        frameLayout.animate().alpha(1.0f).setDuration(140L).start();
        linearLayout.animate().scaleX(1.0f).scaleY(1.0f).setDuration(140L).start();
        textViewChoice.requestFocus();
        return frameLayout;
    }

    static void closeDialog(final View view) {
        if (view == null || view.getParent() == null) {
            return;
        }
        view.animate().alpha(0.0f).setDuration(120L).withEndAction(new Runnable() { // from class: com.chirag.home.Ui.1
            @Override // java.lang.Runnable
            public void run() {
                if (view.getParent() instanceof ViewGroup) {
                    ((ViewGroup) view.getParent()).removeView(view);
                }
            }
        }).start();
    }

    static View pill(Context context, String str, int i, Runnable runnable) {
        return pill(context, PLAIN, str, i, runnable);
    }

    static View pill(Context context, int i, String str, int i2, Runnable runnable) {
        final int i3 = i2 == CHIP ? TEXT_2 : TEXT;
        final TextView textViewText = text(context, str, 14, i3, MEDIUM);
        textViewText.setGravity(17);
        textViewText.setSingleLine(true);
        int iDp = dp(context, i != 0 ? 16 : 18);
        int iDp2 = dp(context, 18);
        int i4 = PLAIN;
        textViewText.setPadding(iDp, PLAIN, iDp2, PLAIN);
        final Glyph glyph = i != 0 ? new Glyph(i, dp(context, 15), i3) : null;
        if (glyph != null) {
            textViewText.setCompoundDrawablesRelativeWithIntrinsicBounds(glyph, (Drawable) null, (Drawable) null, (Drawable) null);
            textViewText.setCompoundDrawablePadding(dp(context, 8));
        }
        final Glass glass = glass(textViewText, TEXT);
        if (i2 == CHIP_ON) {
            i4 = CHIP;
        }
        glass.mode(i4);
        textViewText.setLayoutParams(cell(context, -2, dp(context, PILL_H), 10));
        final int i5 = i4;
        return focusable(context, textViewText, null, PLAIN, 1.08f, runnable, null, new OnFocus() { // from class: com.chirag.home.Ui$$ExternalSyntheticLambda4
            @Override // com.chirag.home.Ui.OnFocus
            public final void changed(boolean z) {
                Ui.lambda$pill$4(glass, i5, textViewText, i3, glyph, z);
            }
        });
    }

    static /* synthetic */ void lambda$pill$4(Glass glass, int i, TextView textView, int i2, Glyph glyph, boolean z) {
        if (z) {
            i = CHIP_ON;
        }
        glass.mode(i);
        ink(textView, z, INK, i2);
        if (glyph != null) {
            if (z) {
                i2 = INK;
            }
            glyph.color(i2);
        }
        liftGlass(textView, z);
    }

    static View disc(Context context, int i, String str, Runnable runnable) {
        final ImageView imageView = new ImageView(context);
        final Glyph glyph = new Glyph(i, Math.round(dp(context, 18) * (i == GAP ? 1.25f : 1.0f)), TEXT);
        imageView.setImageDrawable(glyph);
        imageView.setScaleType(ImageView.ScaleType.CENTER);
        final Glass glass = glass(imageView, TEXT);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dp(context, DISC), dp(context, DISC));
        layoutParams.setMargins(dp(context, 10), PLAIN, PLAIN, PLAIN);
        imageView.setLayoutParams(layoutParams);
        imageView.setContentDescription(str);
        return focusable(context, imageView, null, PLAIN, 1.14f, runnable, null, new OnFocus() { // from class: com.chirag.home.Ui$$ExternalSyntheticLambda9
            @Override // com.chirag.home.Ui.OnFocus
            public final void changed(boolean z) {
                Ui.lambda$disc$5(glass, glyph, imageView, z);
            }
        });
    }

    static /* synthetic */ void lambda$disc$5(Glass glass, Glyph glyph, ImageView imageView, boolean z) {
        glass.mode(z ? CHIP_ON : PLAIN);
        glyph.color(z ? INK : TEXT);
        liftGlass(imageView, z);
    }

    static View orb(Context context, String str, boolean z, Runnable runnable, Runnable runnable2) {
        if (launchIntent(context, str) == null) {
            return null;
        }
        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setClipChildren(false);
        frameLayout.setClipToPadding(false);
        frameLayout.setLayoutParams(new LinearLayout.LayoutParams(dp(context, ICON_CELL), dp(context, 104)));
        frameLayout.setContentDescription(label(context, str));
        final Orb orb = new Orb(context, str, dp(context, ICON), z);
        liftIcon(orb, false);
        orb.lit(false);
        orb.setDuplicateParentStateEnabled(true);
        orb.setStateListAnimator(motion(ICON_FOCUS));
        frameLayout.addView(orb, new FrameLayout.LayoutParams(dp(context, ICON), dp(context, ICON), 17));
        focusable(context, frameLayout, null, PLAIN, 1.0f, runnable, runnable2, new OnFocus() { // from class: com.chirag.home.Ui$$ExternalSyntheticLambda1
            @Override // com.chirag.home.Ui.OnFocus
            public final void changed(boolean z2) {
                Ui.lambda$orb$6(orb, z2);
            }
        });
        frameLayout.setStateListAnimator(null);
        return frameLayout;
    }

    static /* synthetic */ void lambda$orb$6(Orb orb, boolean z) {
        liftIcon(orb, z);
        orb.lit(z);
    }

    static LinearLayout dock(Context context, LinearLayout linearLayout) {
        Rail rail = new Rail(context);
        rail.setPadding(dp(context, 11), PLAIN, dp(context, 11), PLAIN);
        glass(rail, 32);
        LinearLayout linearLayout2 = new LinearLayout(context);
        linearLayout2.setClipChildren(false);
        rail.addView(linearLayout2);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -2);
        layoutParams.gravity = CHIP;
        linearLayout.addView(rail, layoutParams);
        return linearLayout2;
    }

    static ImageView badge(Context context, int i, int i2, int i3, int i4, boolean z) {
        ImageView imageView = new ImageView(context);
        imageView.setImageDrawable(new Glyph(i, dp(context, i3), i4));
        imageView.setScaleType(ImageView.ScaleType.CENTER);
        if (z) {
            imageView.setBackground(shape(context, 704643071, i2 / CHIP_ON, PLAIN));
        } else {
            glass(imageView, TEXT);
        }
        imageView.setLayoutParams(new LinearLayout.LayoutParams(dp(context, i2), dp(context, i2)));
        return imageView;
    }

    static View inputTile(Context context, int i, String str, String str2, final boolean z, int i2, int i3, Runnable runnable) {
        final LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setGravity(16);
        linearLayout.setPadding(dp(context, 16), PLAIN, dp(context, 16), PLAIN);
        final Glass glassQuiet = glass(linearLayout, TILE_RADIUS).quiet(!z);
        int iDp = dp(context, 18);
        int i4 = TEXT;
        int i5 = TEXT_3;
        final Glyph glyph = new Glyph(i, iDp, z ? TEXT : TEXT_3);
        ImageView imageView = new ImageView(context);
        imageView.setImageDrawable(glyph);
        imageView.setScaleType(ImageView.ScaleType.CENTER);
        final GradientDrawable gradientDrawableShape = shape(context, 704643071, 20, PLAIN);
        imageView.setBackground(gradientDrawableShape);
        linearLayout.addView(imageView, new LinearLayout.LayoutParams(dp(context, PILL_H), dp(context, PILL_H)));
        LinearLayout linearLayout2 = new LinearLayout(context);
        linearLayout2.setOrientation(CHIP);
        linearLayout2.setPadding(dp(context, 14), PLAIN, PLAIN, PLAIN);
        if (!z) {
            i4 = TEXT_2;
        }
        final TextView textViewText = text(context, str, 15, i4, MEDIUM);
        textViewText.setSingleLine(true);
        textViewText.setEllipsize(TextUtils.TruncateAt.END);
        linearLayout2.addView(textViewText);
        LinearLayout linearLayout3 = new LinearLayout(context);
        linearLayout3.setGravity(16);
        linearLayout3.setPadding(PLAIN, dp(context, 5), PLAIN, PLAIN);
        View view = new View(context);
        view.setBackground(z ? shape(context, OK, 3, PLAIN) : shape(context, PLAIN, 3, TEXT_3));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dp(context, 6), dp(context, 6));
        layoutParams.setMargins(PLAIN, PLAIN, dp(context, 7), PLAIN);
        linearLayout3.addView(view, layoutParams);
        if (z) {
            i5 = TEXT_2;
        }
        final TextView textViewText2 = text(context, str2, 12, i5, REGULAR);
        textViewText2.setSingleLine(true);
        textViewText2.setEllipsize(TextUtils.TruncateAt.END);
        linearLayout3.addView(textViewText2);
        linearLayout2.addView(linearLayout3);
        linearLayout.addView(linearLayout2, new LinearLayout.LayoutParams(PLAIN, -2, 1.0f));
        linearLayout.setLayoutParams(cell(context, dp(context, i2), dp(context, 72), i3));
        return focusable(context, linearLayout, null, PLAIN, 1.05f, runnable, null, new OnFocus() { // from class: com.chirag.home.Ui$$ExternalSyntheticLambda2
            @Override // com.chirag.home.Ui.OnFocus
            public final void changed(boolean z2) {
                Ui.lambda$inputTile$7(glassQuiet, gradientDrawableShape, glyph, z, textViewText, textViewText2, linearLayout, z2);
            }
        });
    }

    static /* synthetic */ void lambda$inputTile$7(Glass glass, GradientDrawable gradientDrawable, Glyph glyph, boolean z, TextView textView, TextView textView2, LinearLayout linearLayout, boolean z2) {
        int i;
        glass.mode(z2 ? CHIP_ON : PLAIN);
        gradientDrawable.setColor(z2 ? 520093696 : 704643071);
        int i2 = TEXT;
        int i3 = TEXT_3;
        if (z2) {
            i = INK;
        } else {
            i = z ? TEXT : TEXT_3;
        }
        glyph.color(i);
        if (!z) {
            i2 = TEXT_2;
        }
        ink(textView, z2, INK, i2);
        if (z) {
            i3 = TEXT_2;
        }
        ink(textView2, z2, INK_2, i3);
        liftGlass(linearLayout, z2);
    }

    static View notice(Context context, String str, String str2, int i, int i2) {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setGravity(16);
        linearLayout.setPadding(dp(context, 18), PLAIN, dp(context, TILE_RADIUS), PLAIN);
        glass(linearLayout, 20);
        linearLayout.addView(badge(context, i == WARN ? 12 : 11, DISC, 20, i == WARN ? WARN : TEXT, true));
        LinearLayout linearLayout2 = new LinearLayout(context);
        linearLayout2.setOrientation(CHIP);
        linearLayout2.setPadding(dp(context, 14), PLAIN, PLAIN, PLAIN);
        linearLayout2.addView(text(context, str, 15, i, MEDIUM));
        TextView textViewText = text(context, str2, 12, TEXT_2, REGULAR);
        textViewText.setPadding(PLAIN, dp(context, 4), PLAIN, PLAIN);
        linearLayout2.addView(textViewText);
        linearLayout.addView(linearLayout2);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(TEXT, dp(context, CARD_H));
        layoutParams.setMargins(dp(context, i2), dp(context, 8), dp(context, i2), PLAIN);
        linearLayout.setLayoutParams(layoutParams);
        return linearLayout;
    }

    static String label(Context context, String str) {
        PackageManager packageManager = context.getPackageManager();
        try {
            return packageManager.getApplicationLabel(packageManager.getApplicationInfo(str, PLAIN)).toString();
        } catch (Exception e) {
            return str;
        }
    }

    static Intent launchIntent(Context context, String str) {
        PackageManager packageManager = context.getPackageManager();
        Intent leanbackLaunchIntentForPackage = packageManager.getLeanbackLaunchIntentForPackage(str);
        return leanbackLaunchIntentForPackage == null ? packageManager.getLaunchIntentForPackage(str) : leanbackLaunchIntentForPackage;
    }

    static Drawable banner(Context context, String str) {
        PackageManager packageManager = context.getPackageManager();
        try {
            Intent intentLaunchIntent = launchIntent(context, str);
            Drawable activityBanner = intentLaunchIntent != null ? packageManager.getActivityBanner(intentLaunchIntent) : null;
            return activityBanner == null ? packageManager.getApplicationInfo(str, PLAIN).loadBanner(packageManager) : activityBanner;
        } catch (Exception e) {
            return null;
        }
    }

    static Bitmap art(String str, int i) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            int i2 = CHIP;
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(str, options);
            while (true) {
                int i3 = options.outWidth;
                int i4 = i2 * CHIP_ON;
                if (i3 / i4 < i) {
                    BitmapFactory.Options options2 = new BitmapFactory.Options();
                    options2.inSampleSize = i2;
                    options2.inPreferredConfig = Bitmap.Config.RGB_565;
                    return BitmapFactory.decodeFile(str, options2);
                }
                i2 = i4;
            }
        } catch (Throwable th) {
            return null;
        }
    }

    private static FrameLayout picture(Context context, HistoryDb.Item item, int i, int i2, int i3) {
        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setBackground(shape(context, SURFACE, i3, PLAIN));
        frameLayout.setClipToOutline(true);
        ImageView imageView = new ImageView(context);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Bitmap bitmapArt = item.art != null ? art(item.art, i) : null;
        if (bitmapArt != null) {
            imageView.setImageBitmap(bitmapArt);
            frameLayout.addView(imageView, new FrameLayout.LayoutParams(TEXT, TEXT));
        } else {
            Drawable drawableBanner = banner(context, item.pkg);
            if (drawableBanner != null) {
                frameLayout.addView(imageView, new FrameLayout.LayoutParams(TEXT, TEXT));
            } else {
                try {
                    drawableBanner = context.getPackageManager().getApplicationIcon(item.pkg);
                } catch (Exception e) {
                }
                int i4 = (i2 * 5) / 8;
                frameLayout.addView(imageView, new FrameLayout.LayoutParams(i4, i4, 17));
            }
            imageView.setImageDrawable(drawableBanner);
        }
        return frameLayout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class Progress extends FrameLayout {
        private final View fill;

        Progress(Context context, float f, int i) {
            super(context);
            setBackground(Ui.shape(context, 1207959551, Ui.CHIP_ON, Ui.PLAIN));
            setClipToOutline(true);
            this.fill = new View(context);
            this.fill.setBackground(Ui.shape(context, Ui.TEXT, Ui.CHIP_ON, Ui.PLAIN));
            addView(this.fill, new FrameLayout.LayoutParams(Math.max(Ui.dp(context, 3), (int) (i * f)), Ui.TEXT));
        }

        void lit(boolean z) {
            ((GradientDrawable) getBackground()).setColor(z ? 689773855 : 1207959551);
            ((GradientDrawable) this.fill.getBackground()).setColor(z ? Ui.INK : Ui.TEXT);
        }
    }

    static View resumeCard(Context context, HistoryDb.Item item, Runnable runnable, Runnable runnable2) {
        Glass glass;
        String strAgo;
        int i;
        int iDp = dp(context, 7);
        int iDp2 = dp(context, CARD_H);
        int i2 = iDp * CHIP_ON;
        int i3 = iDp2 - i2;
        int i4 = (i3 * 16) / 9;
        final LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setGravity(16);
        linearLayout.setPadding(iDp, iDp, iDp, iDp);
        Glass glass2 = glass(linearLayout, 20);
        FrameLayout frameLayoutPicture = picture(context, item, i4, i3, GAP);
        frameLayoutPicture.setForeground(new Edge(dp(context, GAP), dp(context, CHIP), PLAIN));
        linearLayout.addView(frameLayoutPicture, new LinearLayout.LayoutParams(i4, i3));
        LinearLayout linearLayout2 = new LinearLayout(context);
        linearLayout2.setOrientation(CHIP);
        linearLayout2.setPadding(dp(context, 12), PLAIN, dp(context, 8), PLAIN);
        boolean zIsEmpty = item.title.isEmpty();
        final TextView textViewText = text(context, label(context, item.pkg).toUpperCase(Locale.ENGLISH), 10, TEXT_2, MEDIUM);
        textViewText.setLetterSpacing(0.04f);
        textViewText.setSingleLine(true);
        textViewText.setEllipsize(TextUtils.TruncateAt.END);
        linearLayout2.addView(textViewText);
        final TextView textViewText2 = text(context, zIsEmpty ? "Continue watching" : item.title, 14, TEXT, MEDIUM);
        textViewText2.setSingleLine(true);
        textViewText2.setEllipsize(TextUtils.TruncateAt.END);
        textViewText2.setPadding(PLAIN, dp(context, 3), PLAIN, PLAIN);
        linearLayout2.addView(textViewText2);
        String strRemaining = remaining(item);
        if (zIsEmpty) {
            glass = glass2;
            strAgo = "Watched " + ago(item.lastTs);
        } else {
            glass = glass2;
            strAgo = strRemaining.isEmpty() ? ago(item.lastTs) : strRemaining.substring(3);
        }
        if (!zIsEmpty && item.subtitle != null && !item.subtitle.isEmpty()) {
            strAgo = item.subtitle + " · " + strAgo;
        }
        final TextView textViewText3 = text(context, strAgo, 11, TEXT_2, REGULAR);
        textViewText3.setSingleLine(true);
        textViewText3.setEllipsize(TextUtils.TruncateAt.END);
        textViewText3.setPadding(PLAIN, dp(context, 3), PLAIN, PLAIN);
        linearLayout2.addView(textViewText3);
        float fProgress = item.progress();
        int iDp3 = ((dp(context, CARD_W) - i2) - i4) - dp(context, 20);
        final Progress progress = fProgress > 0.0f ? new Progress(context, fProgress, iDp3) : null;
        if (progress == null) {
            i = PLAIN;
        } else {
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(iDp3, dp(context, 3));
            int iDp4 = dp(context, 7);
            i = PLAIN;
            layoutParams.setMargins(PLAIN, iDp4, PLAIN, PLAIN);
            linearLayout2.addView(progress, layoutParams);
        }
        linearLayout.addView(linearLayout2, new LinearLayout.LayoutParams(i, -2, 1.0f));
        linearLayout.setLayoutParams(cell(context, dp(context, CARD_W), dp(context, CARD_H), GAP));
        final Glass glass3 = glass;
        return focusable(context, linearLayout, null, PLAIN, 1.06f, runnable, runnable2, new OnFocus() { // from class: com.chirag.home.Ui$$ExternalSyntheticLambda0
            @Override // com.chirag.home.Ui.OnFocus
            public final void changed(boolean z) {
                Ui.lambda$resumeCard$8(glass3, textViewText, textViewText2, textViewText3, progress, linearLayout, z);
            }
        });
    }

    static /* synthetic */ void lambda$resumeCard$8(Glass glass, TextView textView, TextView textView2, TextView textView3, Progress progress, LinearLayout linearLayout, boolean z) {
        glass.mode(z ? CHIP_ON : PLAIN);
        ink(textView, z, INK_2, TEXT_2);
        ink(textView2, z, INK, TEXT);
        ink(textView3, z, INK_2, TEXT_2);
        if (progress != null) {
            progress.lit(z);
        }
        liftGlass(linearLayout, z);
    }

    static View historyCard(Context context, HistoryDb.Item item, boolean z, int i, int i2, Runnable runnable) {
        String str;
        int iDp = dp(context, i);
        int i3 = (iDp * 9) / 16;
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(CHIP);
        linearLayout.setClipChildren(false);
        linearLayout.setClipToPadding(false);
        FrameLayout frameLayoutPicture = picture(context, item, iDp, i3, 14);
        float fProgress = item.progress();
        if (fProgress > 0.0f) {
            View view = new View(context);
            view.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{PLAIN, SHADOW}));
            frameLayoutPicture.addView(view, new FrameLayout.LayoutParams(TEXT, dp(context, 36), 80));
            int iDp2 = iDp - dp(context, 20);
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(iDp2, dp(context, 3), 80);
            layoutParams.setMargins(dp(context, 10), PLAIN, dp(context, 10), dp(context, 9));
            frameLayoutPicture.addView(new Progress(context, fProgress, iDp2), layoutParams);
        }
        linearLayout.addView(frameLayoutPicture, new LinearLayout.LayoutParams(iDp, i3));
        boolean zIsEmpty = item.title.isEmpty();
        final TextView textViewText = text(context, zIsEmpty ? "Continue in " + label(context, item.pkg) : item.title, 14, -335544321, MEDIUM);
        textViewText.setSingleLine(true);
        textViewText.setEllipsize(TextUtils.TruncateAt.END);
        textViewText.setPadding(dp(context, CHIP_ON), dp(context, 9), dp(context, CHIP_ON), PLAIN);
        linearLayout.addView(textViewText);
        if (zIsEmpty) {
            str = "Watched " + ago(item.lastTs) + " · opens the app";
        } else {
            str = (z ? label(context, item.pkg) + " · " : "") + ago(item.lastTs) + remaining(item);
        }
        if (item.subtitle != null && !item.subtitle.isEmpty()) {
            str = item.subtitle + " · " + str;
        }
        final TextView textViewText2 = text(context, str, 12, TEXT_3, REGULAR);
        textViewText2.setSingleLine(true);
        textViewText2.setEllipsize(TextUtils.TruncateAt.END);
        textViewText2.setPadding(dp(context, CHIP_ON), dp(context, 4), dp(context, CHIP_ON), PLAIN);
        linearLayout.addView(textViewText2);
        linearLayout.setLayoutParams(cell(context, iDp, -2, i2));
        linearLayout.setPivotX(iDp / 2.0f);
        linearLayout.setPivotY(i3 / 2.0f);
        return focusable(context, linearLayout, frameLayoutPicture, 14, 1.06f, runnable, null, new OnFocus() { // from class: com.chirag.home.Ui$$ExternalSyntheticLambda8
            @Override // com.chirag.home.Ui.OnFocus
            public final void changed(boolean z2) {
                Ui.lambda$historyCard$9(textViewText, textViewText2, z2);
            }
        });
    }

    static /* synthetic */ void lambda$historyCard$9(TextView textView, TextView textView2, boolean z) {
        textView.setTextColor(z ? TEXT : -335544321);
        textView2.setTextColor(z ? TEXT_2 : TEXT_3);
    }

    static String remaining(HistoryDb.Item item) {
        if (item.duration <= 0) {
            return "";
        }
        if (item.finished()) {
            return " · watched";
        }
        long j = (item.duration - item.position) / 60000;
        return j >= 1 ? " · " + j + " min left" : "";
    }

    static String ago(long j) {
        long jCurrentTimeMillis = (System.currentTimeMillis() - j) / 60000;
        if (jCurrentTimeMillis < 1) {
            return "just now";
        }
        if (jCurrentTimeMillis < 60) {
            return jCurrentTimeMillis + " min ago";
        }
        if (jCurrentTimeMillis < 1440) {
            return (jCurrentTimeMillis / 60) + " h ago";
        }
        long j2 = jCurrentTimeMillis / 1440;
        return j2 == 1 ? "yesterday" : j2 + " days ago";
    }

    static void resume(Context context, HistoryDb.Item item) {
        if (item.title.isEmpty()) {
            launch(context, launchIntent(context, item.pkg));
            return;
        }
        if (item.pkg.equals("com.spotify.tv.android") && item.position > 5000 && !item.finished()) {
            Pending.set(item.pkg, item.title, item.position);
        }
        Intent intentResumeIntent = resumeIntent(item);
        if (intentResumeIntent != null) {
            if (item.openedTs > 0) {
                HistoryDb.get(context).recordLaunch(item.pkg, item.title, item.subtitle, null, System.currentTimeMillis());
            }
            try {
                context.startActivity(intentResumeIntent.addFlags(268435456));
                return;
            } catch (Exception e) {
            }
        }
        if (findAndOpen(context, item)) {
            return;
        }
        launch(context, launchIntent(context, item.pkg));
    }

    /**
     * No stored link for this card (Apple TV and a few other apps don't report a usable id): look the title up
     * on JustWatch now, open the app's own page for it, and keep the link on the row so the next press is instant.
     * Falls back to opening the app when nothing sure is found. Returns false when no lookup applies.
     */
    static boolean findAndOpen(final Context context, final HistoryDb.Item item) {
        final VoiceCore.App app = VoiceCore.appForPkg(item.pkg);
        if (app == null || item.title.isEmpty()) {
            return false;
        }
        final Context appContext = context.getApplicationContext();
        final android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
        Lens.EXEC.execute(new Runnable() {
            public void run() {
                Intent found = null;
                try {
                    VoiceClient.Config config = VoiceClient.Config.read(new java.io.File(appContext.getExternalFilesDir(null), "voice.json"));
                    if (config != null && config.justwatch) {
                        VoiceClient client = new VoiceClient(config, VoiceClient.REAL);
                        VoiceCore.Title title = VoiceCore.artMatch(app.key, item.title, client.justWatch(item.title), false);
                        if (title != null) {
                            for (VoiceCore.Link link : VoiceCore.titleLinks(title, app.key, true, config.searchLinks)) {
                                if ("VIEW".equals(link.action) && link.uri != null && link.uri.contains("://")) {
                                    String uri = link.uri;
                                    // Apple TV's show links point at episode 1 (and would start it); its show page has the "Continue" button.
                                    java.util.regex.Matcher show = java.util.regex.Pattern.compile("showId=(umc\\.cmc\\.[a-z0-9]+)").matcher(uri);
                                    if (app.key.equals("appletv") && show.find()) uri = "https://tv.apple.com/in/show/x/" + show.group(1);
                                    HistoryDb.get(appContext).setMediaId(item.pkg, item.title, item.subtitle == null ? "" : item.subtitle, uri);
                                    found = view(item.pkg, uri);
                                    for (java.util.Map.Entry<String, String> e : link.extras.entrySet()) found.putExtra(e.getKey(), e.getValue());
                                    break;
                                }
                            }
                        }
                        android.util.Log.i("WatchHistory", "card link for \"" + item.title + "\" (" + app.key + "): " + (found == null ? "none, opening the app" : found.getData()));
                    }
                } catch (Exception e) {
                    android.util.Log.w("WatchHistory", "card link lookup failed for " + item.title + ": " + e.getMessage());
                }
                final Intent open = found;
                main.post(new Runnable() {
                    public void run() {
                        if (open != null) {
                            try {
                                appContext.startActivity(open.addFlags(268435456));
                                return;
                            } catch (Exception ignored) {
                            }
                        }
                        launch(context, launchIntent(context, item.pkg));
                    }
                });
            }
        });
        return true;
    }

    static Intent resumeIntent(HistoryDb.Item item) {
        String strTrim = item.mediaId == null ? "" : item.mediaId.trim();
        if (strTrim.contains("://")) {
            return view(item.pkg, strTrim);
        }
        String str = item.title + ((item.subtitle == null || item.subtitle.isEmpty()) ? "" : " " + item.subtitle);
        switch (item.pkg) {
            case "com.google.android.youtube.tv":
                if (strTrim.matches("[A-Za-z0-9_-]{11}")) {
                    long jMax = item.finished() ? 0L : Math.max(0L, (item.position / 1000) - 3);
                    return view(item.pkg, "https://www.youtube.com/watch?v=" + strTrim + (jMax > 0 ? "&t=" + jMax + "s" : ""));
                }
                return view(item.pkg, "https://www.youtube.com/results?search_query=" + Uri.encode(str));
            case "com.spotify.tv.android":
                if (strTrim.matches("spotify:[a-z]+:[A-Za-z0-9]+")) {
                    String[] strArrSplit = strTrim.split(":");
                    return view(item.pkg, "https://open.spotify.com/" + strArrSplit[CHIP] + "/" + strArrSplit[CHIP_ON]);
                }
                return view(item.pkg, "https://open.spotify.com/search/" + Uri.encode(str));
            case "com.netflix.ninja":
                Matcher matcher = Pattern.compile("(\\d{6,})").matcher(strTrim);
                if (matcher.find()) {
                    return view(item.pkg, "https://www.netflix.com/watch/" + matcher.group(CHIP)).putExtra("source", "30");
                }
                return null;
            case "com.amazon.amazonvideo.livingroom":
                if (strTrim.startsWith("amzn1.dv.gti.")) {
                    return view(item.pkg, "https://app.primevideo.com/watch?gti=" + strTrim + "&time=0&territory=IN");
                }
                return null;
            case "com.apple.atve.androidtv.appletv":
                if (strTrim.startsWith("umc.")) {
                    return view(item.pkg, "https://tv.apple.com/" + (strTrim.contains(".cmc.") ? "movie" : "episode") + "/x/" + strTrim);
                }
                return null;
            case "in.startv.hotstar":
                return null;
            case "com.crunchyroll.crunchyroid":
                return null;
            default:
                return null;
        }
    }

    static Intent view(String str, String str2) {
        return new Intent("android.intent.action.VIEW", Uri.parse(str2)).setPackage(str);
    }

    static void openGoogleTv(Context context, String str) {
        try {
            context.startActivity(new Intent("android.intent.action.MAIN").addCategory("android.intent.category.HOME").setComponent(GOOGLE_TV).addFlags(268435456));
        } catch (Exception e) {
            launch(context, launchIntent(context, str));
        }
    }

    static void launch(Context context, Intent intent) {
        if (intent == null) {
            Toast.makeText(context, "Can't open that right now", PLAIN).show();
            return;
        }
        try {
            context.startActivity(new Intent(intent).addFlags(268435456));
        } catch (Exception e) {
            Toast.makeText(context, "Can't open that right now", PLAIN).show();
        }
    }
}
