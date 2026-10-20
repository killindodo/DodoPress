package com.dodopress.ui;

import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

public class TouchFeedback {

    private static final OvershootInterpolator SPRING_INTERPOLATOR = new OvershootInterpolator(2.0f);

    public static void apply(View view) {
        if (view == null) return;
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate()
                     .scaleX(0.95f)
                     .scaleY(0.95f)
                     .setDuration(80)
                     .start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate()
                     .scaleX(1.0f)
                     .scaleY(1.0f)
                     .setDuration(160)
                     .setInterpolator(SPRING_INTERPOLATOR)
                     .start();
                    break;
            }
            return false; // Allow standard onClick to fire
        });
    }

    public static void applyAll(View... views) {
        for (View v : views) {
            apply(v);
        }
    }
}
