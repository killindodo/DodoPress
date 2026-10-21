package com.dodopress.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import com.dodopress.engine.SheetGeometry;

public class SheetGeometrySelectorView extends View {

    public interface OnRegionSelectedListener {
        void onRegionSelected(SheetGeometry.PlacementRegion region);
    }

    private SheetGeometry.PaperSize paperSize = SheetGeometry.PaperSize.A4;
    private SheetGeometry.Orientation orientation = SheetGeometry.Orientation.PORTRAIT;
    private SheetGeometry.PlacementRegion activeRegion = SheetGeometry.PlacementRegion.FULL_PAGE;
    private SheetGeometry.TilingMode tilingMode = SheetGeometry.TilingMode.ONE_UP;
    private OnRegionSelectedListener listener;

    private int primaryColor = 0xFF38BDF8;
    private int accentColor = 0xFF818CF8;

    private final Paint paperPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paperShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paperBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridGuidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint activeFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint activeStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint docWireframePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF sheetRect = new RectF();
    private final RectF activeRegionRect = new RectF();
    private float pulseAlpha = 0.35f;
    private ValueAnimator pulseAnimator;

    public SheetGeometrySelectorView(Context context) {
        super(context);
        init();
    }

    public SheetGeometrySelectorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SheetGeometrySelectorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paperPaint.setColor(Color.WHITE);
        paperPaint.setStyle(Paint.Style.FILL);

        paperShadowPaint.setColor(0x28000000);
        paperShadowPaint.setStyle(Paint.Style.FILL);

        paperBorderPaint.setColor(0xFFCBD5E1);
        paperBorderPaint.setStyle(Paint.Style.STROKE);
        paperBorderPaint.setStrokeWidth(2f);

        gridGuidePaint.setColor(0xFF94A3B8);
        gridGuidePaint.setStyle(Paint.Style.STROKE);
        gridGuidePaint.setStrokeWidth(1.5f);
        gridGuidePaint.setPathEffect(new DashPathEffect(new float[]{8f, 6f}, 0f));

        activeFillPaint.setStyle(Paint.Style.FILL);

        activeStrokePaint.setStyle(Paint.Style.STROKE);
        activeStrokePaint.setStrokeWidth(3f);

        docWireframePaint.setColor(0x55000000);
        docWireframePaint.setStyle(Paint.Style.FILL);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(26f);
        textPaint.setFakeBoldText(true);

        startPulseAnimation();
    }

    public void setThemeColors(int primary, int accent) {
        this.primaryColor = primary;
        this.accentColor = accent;
        invalidate();
    }

    public void setPaperSize(SheetGeometry.PaperSize size) {
        this.paperSize = size;
        requestLayout();
        invalidate();
    }

    public void setPlacementRegion(SheetGeometry.PlacementRegion region) {
        this.activeRegion = region;
        this.tilingMode = SheetGeometry.TilingMode.ONE_UP;
        invalidate();
    }

    public SheetGeometry.PlacementRegion getPlacementRegion() {
        return activeRegion;
    }

    public void setOrientation(SheetGeometry.Orientation orientation) {
        if (orientation == null || this.orientation == orientation) return;
        this.orientation = orientation;
        if (getWidth() > 0 && getHeight() > 0) {
            onSizeChanged(getWidth(), getHeight(), getWidth(), getHeight());
        }
        invalidate();
    }

    public SheetGeometry.Orientation getOrientation() {
        return orientation;
    }

    public void setTilingMode(SheetGeometry.TilingMode mode) {
        this.tilingMode = (mode != null) ? mode : SheetGeometry.TilingMode.ONE_UP;
        invalidate();
    }

    public SheetGeometry.TilingMode getTilingMode() {
        return tilingMode;
    }

    public void setOnRegionSelectedListener(OnRegionSelectedListener listener) {
        this.listener = listener;
    }

    private void startPulseAnimation() {
        pulseAnimator = ValueAnimator.ofFloat(0.20f, 0.45f);
        pulseAnimator.setDuration(1600);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.addUpdateListener(anim -> {
            pulseAlpha = (float) anim.getAnimatedValue();
            invalidate();
        });
        pulseAnimator.start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        // Desired sheet aspect ratio
        float aspect = paperSize.heightPts / (float) paperSize.widthPts;
        int height = (int) (width * 0.70f); // Fixed compact height container
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float pad = 24f;
        float availW = w - (pad * 2f);
        float availH = h - (pad * 2f);

        float paperAspect = paperSize.getWidthPts(orientation) / (float) paperSize.getHeightPts(orientation);
        float drawW, drawH;

        if (availW / paperAspect <= availH) {
            drawW = availW;
            drawH = drawW / paperAspect;
        } else {
            drawH = availH;
            drawW = drawH * paperAspect;
        }

        float left = (w - drawW) / 2f;
        float top = (h - drawH) / 2f;
        sheetRect.set(left, top, left + drawW, top + drawH);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();

            if (sheetRect.contains(x, y)) {
                float relX = (x - sheetRect.left) / sheetRect.width();
                float relY = (y - sheetRect.top) / sheetRect.height();

                SheetGeometry.PlacementRegion newRegion;

                if (relY < 0.5f) {
                    if (relX < 0.33f) {
                        newRegion = (activeRegion == SheetGeometry.PlacementRegion.QUAD_TOP_LEFT) ? SheetGeometry.PlacementRegion.FULL_PAGE : SheetGeometry.PlacementRegion.QUAD_TOP_LEFT;
                    } else if (relX > 0.66f) {
                        newRegion = (activeRegion == SheetGeometry.PlacementRegion.QUAD_TOP_RIGHT) ? SheetGeometry.PlacementRegion.FULL_PAGE : SheetGeometry.PlacementRegion.QUAD_TOP_RIGHT;
                    } else {
                        newRegion = (activeRegion == SheetGeometry.PlacementRegion.TOP_HALF) ? SheetGeometry.PlacementRegion.FULL_PAGE : SheetGeometry.PlacementRegion.TOP_HALF;
                    }
                } else {
                    if (relX < 0.33f) {
                        newRegion = (activeRegion == SheetGeometry.PlacementRegion.QUAD_BOTTOM_LEFT) ? SheetGeometry.PlacementRegion.FULL_PAGE : SheetGeometry.PlacementRegion.QUAD_BOTTOM_LEFT;
                    } else if (relX > 0.66f) {
                        newRegion = (activeRegion == SheetGeometry.PlacementRegion.QUAD_BOTTOM_RIGHT) ? SheetGeometry.PlacementRegion.FULL_PAGE : SheetGeometry.PlacementRegion.QUAD_BOTTOM_RIGHT;
                    } else {
                        newRegion = (activeRegion == SheetGeometry.PlacementRegion.BOTTOM_HALF) ? SheetGeometry.PlacementRegion.FULL_PAGE : SheetGeometry.PlacementRegion.BOTTOM_HALF;
                    }
                }

                activeRegion = newRegion;
                tilingMode = SheetGeometry.TilingMode.ONE_UP;
                if (listener != null) {
                    listener.onRegionSelected(activeRegion);
                }
                // Spring bounce feedback
                animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).withEndAction(() -> {
                    animate().scaleX(1.0f).scaleY(1.0f).setDuration(160).setInterpolator(new OvershootInterpolator()).start();
                }).start();

                invalidate();
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (sheetRect.isEmpty()) return;

        // 1. Draw Paper Shadow
        RectF shadow = new RectF(sheetRect.left + 4f, sheetRect.top + 6f, sheetRect.right + 4f, sheetRect.bottom + 6f);
        canvas.drawRoundRect(shadow, 8f, 8f, paperShadowPaint);

        // 2. Draw Pure White Physical Sheet
        canvas.drawRoundRect(sheetRect, 8f, 8f, paperPaint);
        canvas.drawRoundRect(sheetRect, 8f, 8f, paperBorderPaint);

        // 3. Draw Dashed Grid Dividers (Half Horizontal & Half Vertical)
        float midX = sheetRect.centerX();
        float midY = sheetRect.centerY();
        canvas.drawLine(sheetRect.left + 4f, midY, sheetRect.right - 4f, midY, gridGuidePaint);
        canvas.drawLine(midX, sheetRect.top + 4f, midX, sheetRect.bottom - 4f, gridGuidePaint);

        // 4. Calculate Selected Region Coordinates
        if (tilingMode != null && tilingMode != SheetGeometry.TilingMode.ONE_UP) {
            Rect[] boxes = SheetGeometry.calculateNUpBoxes(tilingMode, (int) sheetRect.width(), (int) sheetRect.height(), 4);
            int alphaInt = (int) (pulseAlpha * 255);
            activeFillPaint.setColor(Color.argb(alphaInt, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor)));
            activeStrokePaint.setColor(primaryColor);

            Paint wireLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            wireLinePaint.setColor(primaryColor);
            wireLinePaint.setStrokeWidth(1.5f);

            for (int i = 0; i < boxes.length; i++) {
                Rect b = boxes[i];
                RectF r = new RectF(sheetRect.left + b.left, sheetRect.top + b.top, sheetRect.left + b.right, sheetRect.top + b.bottom);
                canvas.drawRoundRect(r, 4f, 4f, activeFillPaint);
                canvas.drawRoundRect(r, 4f, 4f, activeStrokePaint);

                float wireW = r.width() * 0.70f;
                float wireH = wireW * 1.414f;
                if (wireH > r.height() * 0.78f) {
                    wireH = r.height() * 0.78f;
                    wireW = wireH / 1.414f;
                }
                float wLeft = r.centerX() - (wireW / 2f);
                float wTop = r.centerY() - (wireH / 2f);
                RectF wireRect = new RectF(wLeft, wTop, wLeft + wireW, wTop + wireH);
                docWireframePaint.setColor(Color.argb(70, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)));
                canvas.drawRoundRect(wireRect, 3f, 3f, docWireframePaint);

                float lineStep = wireRect.height() / 5f;
                for (int l = 1; l <= 3; l++) {
                    float lineY = wireRect.top + (l * lineStep);
                    canvas.drawLine(wireRect.left + 4f, lineY, wireRect.right - 4f, lineY, wireLinePaint);
                }
            }

            textPaint.setColor(primaryColor);
            textPaint.setTextSize(Math.max(18f, sheetRect.height() * 0.055f));
            String statusText = paperSize.name() + " • " + tilingMode.title;
            canvas.drawText(statusText, sheetRect.centerX(), sheetRect.bottom + 22f, textPaint);
        } else {
            float rx = sheetRect.left + (activeRegion.relX * sheetRect.width());
            float ry = sheetRect.top + (activeRegion.relY * sheetRect.height());
            float rw = activeRegion.relW * sheetRect.width();
            float rh = activeRegion.relH * sheetRect.height();
            activeRegionRect.set(rx, ry, rx + rw, ry + rh);

            // 5. Draw Active Region Highlight
            int alphaInt = (int) (pulseAlpha * 255);
            activeFillPaint.setColor(Color.argb(alphaInt, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor)));
            canvas.drawRoundRect(activeRegionRect, 6f, 6f, activeFillPaint);

            activeStrokePaint.setColor(primaryColor);
            canvas.drawRoundRect(activeRegionRect, 6f, 6f, activeStrokePaint);

            // 6. Draw Centered Document Wireframe (Preserving Aspect Ratio)
            float wireW = activeRegionRect.width() * 0.72f;
            float wireH = wireW * 1.414f;
            if (wireH > activeRegionRect.height() * 0.82f) {
                wireH = activeRegionRect.height() * 0.82f;
                wireW = wireH / 1.414f;
            }
            float wLeft = activeRegionRect.centerX() - (wireW / 2f);
            float wTop = activeRegionRect.centerY() - (wireH / 2f);
            RectF wireRect = new RectF(wLeft, wTop, wLeft + wireW, wTop + wireH);

            docWireframePaint.setColor(Color.argb(70, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)));
            canvas.drawRoundRect(wireRect, 3f, 3f, docWireframePaint);

            // Draw mini document text lines in wireframe
            Paint wireLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            wireLinePaint.setColor(primaryColor);
            wireLinePaint.setStrokeWidth(2f);
            float lineStep = wireRect.height() / 6f;
            for (int i = 1; i <= 4; i++) {
                float lineY = wireRect.top + (i * lineStep);
                canvas.drawLine(wireRect.left + 6f, lineY, wireRect.right - 6f, lineY, wireLinePaint);
            }

            // 7. Label on Sheet (e.g. "A4 Sheet — Top Half")
            textPaint.setColor(primaryColor);
            textPaint.setTextSize(Math.max(18f, sheetRect.height() * 0.055f));
            String statusText = paperSize.name() + " • " + activeRegion.title;
            canvas.drawText(statusText, sheetRect.centerX(), sheetRect.bottom + 22f, textPaint);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }
    }
}
