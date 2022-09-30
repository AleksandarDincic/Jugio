package com.thedinch.jugio;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import org.tensorflow.lite.task.gms.vision.detector.Detection;

import java.util.ArrayList;
import java.util.List;

public class OverlayView extends View {

    List<Detection> detections = new ArrayList<>();
    Paint boxPaint = new Paint();
    float scaleFactor = 1f;
    Rect bounds = new Rect();

    private void initPaint() {
        boxPaint.setColor(Color.BLUE);
        boxPaint.setStrokeWidth(8f);
        boxPaint.setStyle(Paint.Style.STROKE);
    }

    public OverlayView(Context context) {
        super(context);
        initPaint();
    }

    public OverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initPaint();
    }

    public OverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaint();
    }

    public OverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initPaint();
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        for (Detection detection : detections) {
            RectF boundingBox = detection.getBoundingBox();

            float top = boundingBox.top * scaleFactor;
            float bottom = boundingBox.bottom * scaleFactor;
            float left = boundingBox.left * scaleFactor;
            float right = boundingBox.right * scaleFactor;

            RectF drawableRect = new RectF(left, top, right, bottom);
            canvas.drawRect(drawableRect, boxPaint);
        }
    }

    public void setDetections(List<Detection> detections, int imgWidth, int imgHeight){
        this.detections = detections;
        scaleFactor = Math.max(getWidth() * 1f / imgWidth, getHeight() * 1f / imgHeight);
    }
}
