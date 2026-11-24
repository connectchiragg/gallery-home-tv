package com.chirag.home;

import android.graphics.Matrix;
import android.graphics.Path;

/* JADX INFO: loaded from: classes.dex */
final class Ps5 {
    static final float SCALE = 1.25f;

    Ps5() {
    }

    static void mark(Path path) {
        PsMark.build(path);
        Matrix matrix = new Matrix();
        matrix.setScale(0.08f, 0.08f);
        matrix.postTranslate(0.0f, 1.8072f);
        path.transform(matrix);
    }
}
