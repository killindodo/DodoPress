package com.dodopress.ui;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

/**
 * High-performance interactive Zoom & Pan ImageView for DodoPress document preview.
 * Supports:
 * - Fluid pinch-to-zoom (1.0x to 5.0x)
 * - Double-tap to zoom in / reset zoom
 * - Smooth two-finger and single-finger pan when zoomed
 * - Automatic boundary checking to keep page centered
 */
public class ZoomableImageView extends ImageView {

    private final Matrix matrix = new Matrix();
    private final float[] matrixValues = new float[9];

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    private float currentScale = 1.0f;
    private float minScale = 1.0f;
    private float maxScale = 5.0f;

    private boolean isZoomed = false;

    public ZoomableImageView(Context context) {
        super(context);
        init(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        resetZoom();
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        resetZoom();
    }

    public void resetZoom() {
        Drawable d = getDrawable();
        if (d == null || d.getIntrinsicWidth() <= 0 || d.getIntrinsicHeight() <= 0) return;

        int viewW = getWidth();
        int viewH = getHeight();
        if (viewW <= 0 || viewH <= 0) return;

        int drawableW = d.getIntrinsicWidth();
        int drawableH = d.getIntrinsicHeight();

        float scaleX = (float) viewW / drawableW;
        float scaleY = (float) viewH / drawableH;
        float scale = Math.min(scaleX, scaleY);

        float dx = (viewW - drawableW * scale) / 2.0f;
        float dy = (viewH - drawableH * scale) / 2.0f;

        matrix.reset();
        matrix.postScale(scale, scale);
        matrix.postTranslate(dx, dy);
        setImageMatrix(matrix);

        minScale = scale;
        currentScale = scale;
        isZoomed = false;
    }

    public void toggleZoom() {
        if (isZoomed) {
            resetZoom();
        } else {
            zoomTo(getWidth() / 2.0f, getHeight() / 2.0f, minScale * 2.8f);
        }
    }

    public boolean isCurrentlyZoomed() {
        return isZoomed;
    }

    public void zoomTo(float focusX, float focusY, float targetScale) {
        float factor = targetScale / currentScale;
        currentScale = targetScale;
        matrix.postScale(factor, factor, focusX, focusY);
        checkBounds();
        setImageMatrix(matrix);
        isZoomed = (currentScale > minScale * 1.05f);
    }

    private void checkBounds() {
        Drawable d = getDrawable();
        if (d == null) return;

        matrix.getValues(matrixValues);
        float transX = matrixValues[Matrix.MTRANS_X];
        float transY = matrixValues[Matrix.MTRANS_Y];
        float scaleX = matrixValues[Matrix.MSCALE_X];
        float scaleY = matrixValues[Matrix.MSCALE_Y];

        float drawW = d.getIntrinsicWidth() * scaleX;
        float drawH = d.getIntrinsicHeight() * scaleY;

        int viewW = getWidth();
        int viewH = getHeight();

        float deltaX = 0;
        float deltaY = 0;

        if (drawW <= viewW) {
            deltaX = (viewW - drawW) / 2.0f - transX;
        } else {
            if (transX > 0) deltaX = -transX;
            else if (transX + drawW < viewW) deltaX = viewW - (transX + drawW);
        }

        if (drawH <= viewH) {
            deltaY = (viewH - drawH) / 2.0f - transY;
        } else {
            if (transY > 0) deltaY = -transY;
            else if (transY + drawH < viewH) deltaY = viewH - (transY + drawH);
        }

        matrix.postTranslate(deltaX, deltaY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Disallow parent ScrollView from stealing touch gestures when zooming or panning
        if (isZoomed || event.getPointerCount() > 1) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }

        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            getParent().requestDisallowInterceptTouchEvent(false);
        }
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float factor = detector.getScaleFactor();
            float newScale = currentScale * factor;
            if (newScale >= minScale && newScale <= minScale * maxScale) {
                currentScale = newScale;
                matrix.postScale(factor, factor, detector.getFocusX(), detector.getFocusY());
                checkBounds();
                setImageMatrix(matrix);
                isZoomed = (currentScale > minScale * 1.05f);
            }
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            toggleZoom();
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (isZoomed) {
                matrix.postTranslate(-distanceX, -distanceY);
                checkBounds();
                setImageMatrix(matrix);
                return true;
            }
            return false;
        }
    }
}
