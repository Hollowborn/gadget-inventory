package com.example.gadgetinventory.ui.detection;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class DetectionOverlay extends View {
    private final Paint boxPaint;
    private final Paint textPaint;
    private List<DetectionInfo> detections = new ArrayList<>();

    public static class DetectionInfo {
        RectF boundingBox;
        String label;
        float confidence;

        public DetectionInfo(RectF boundingBox, String label, float confidence) {
            this.boundingBox = boundingBox;
            this.label = label;
            this.confidence = confidence;
        }
    }

    public DetectionOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);

        boxPaint = new Paint();
        boxPaint.setColor(Color.RED);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(4.0f);

        textPaint = new Paint();
        textPaint.setColor(Color.RED);
        textPaint.setTextSize(32.0f);
    }

    public void setDetections(List<DetectionInfo> detections) {
        this.detections = detections;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (DetectionInfo detection : detections) {
            // Draw bounding box
            canvas.drawRect(detection.boundingBox, boxPaint);

            // Draw label and confidence
            String text = String.format("%s %.2f", detection.label, detection.confidence);
            canvas.drawText(text, detection.boundingBox.left, detection.boundingBox.top - 10, textPaint);
        }
    }
}