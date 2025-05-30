package com.example.gadgetinventory.ui.detection;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class DetectionOverlayView extends View {
    private List<BoundingBox> results = new ArrayList<>();
    private final Paint boxPaint = new Paint();
    private final Paint textBackgroundPaint = new Paint();
    private final Paint textPaint = new Paint();
    private final Rect bounds = new Rect();
    private static final int BOUNDING_RECT_TEXT_PADDING = 8;

    public DetectionOverlayView(Context context) {
        this(context, null);
    }

    public DetectionOverlayView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DetectionOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaints();
    }

    private void initPaints() {
        textBackgroundPaint.setColor(Color.BLACK);
        textBackgroundPaint.setStyle(Paint.Style.FILL);
        textBackgroundPaint.setTextSize(50f);

        textPaint.setColor(Color.WHITE);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(50f);

        boxPaint.setColor(Color.RED);
        boxPaint.setStrokeWidth(8f);
        boxPaint.setStyle(Paint.Style.STROKE);
    }

    public void clear() {
        results = new ArrayList<>();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (BoundingBox box : results) {
            float left = box.getX1() * getWidth();
            float top = box.getY1() * getHeight();
            float right = box.getX2() * getWidth();
            float bottom = box.getY2() * getHeight();

            // Draw bounding box
            canvas.drawRect(left, top, right, bottom, boxPaint);

            String drawableText = String.format("%s %.2f", box.getClsName(), box.getCnf());
            textPaint.getTextBounds(drawableText, 0, drawableText.length(), bounds);
            int textWidth = bounds.width();
            int textHeight = bounds.height();

            // Draw text background
            canvas.drawRect(
                    left,
                    top,
                    left + textWidth + BOUNDING_RECT_TEXT_PADDING,
                    top + textHeight + BOUNDING_RECT_TEXT_PADDING,
                    textBackgroundPaint
            );

            // Draw text
            canvas.drawText(drawableText, left, top + bounds.height(), textPaint);
        }
    }

    public void setResults(List<BoundingBox> boundingBoxes) {
        results = boundingBoxes;
        invalidate();
    }
}