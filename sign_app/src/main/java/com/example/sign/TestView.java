package com.example.sign;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * 绘制路径的View
 *
 * Created by ws on 2018/2/7.
 */

public class TestView extends View {

    Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    Path mPath = new Path();
    private PerformClick mPerformClick;

    public TestView(Context context) {
        this(context, null);
    }

    public TestView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TestView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialization(context, attrs, defStyleAttr, 0);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public TestView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int
            defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialization(context, attrs, defStyleAttr, defStyleRes);
    }

    void initialization(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mPaint.setStrokeWidth(5);
        mPaint.setColor(Color.RED);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStyle(Paint.Style.STROKE);
    }

    private List<List<PointF>> pointsList = new ArrayList<>();

    private List<PointF> currentPoints;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentPoints = new ArrayList<>();
                pointsList.add(currentPoints);
                return true;
            case MotionEvent.ACTION_MOVE:
                currentPoints.add(new PointF(event.getX(), event.getY()));
                break;
            case MotionEvent.ACTION_UP:
                if (mPerformClick == null) {
                    mPerformClick = new PerformClick();
                }
                if (!post(mPerformClick)) {
                    performClick();
                }
                break;
        }
        invalidate();
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (List<PointF> points : pointsList) {
            invalidatePath(points, mPath);
            canvas.drawPath(mPath, mPaint);
        }
    }

    void invalidatePath(List<PointF> points, Path path) {
        if (path == null || points == null) {
            throw new NullPointerException();
        }
        path.reset();
        final int count = points.size();
        if (count == 0) {
            return;
        }
        PointF first = points.get(0);
        path.moveTo(first.x, first.y);
        for (int i = 0; i < count; i++) {
            PointF point = points.get(i);
            path.lineTo(point.x, point.y);
        }
    }

    private final class PerformClick implements Runnable {
        @Override
        public void run() {
            performClick();
        }
    }
}
