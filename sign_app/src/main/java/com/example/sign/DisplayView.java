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
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * 用于展示擦除结果的View
 *
 * Created by ws on 2018/2/9.
 */

public class DisplayView extends View {

    Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private List<List<PointF>> pointsList = new ArrayList<>();

    Path mPath = new Path();

    public DisplayView(Context context) {
        this(context, null);
    }

    public DisplayView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DisplayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialization(context, attrs, defStyleAttr, 0);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public DisplayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int
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


    public void setPointsList(List<PDFSignaturePath> signaturePaths) {
        pointsList = EraseUtil.calculateList(signaturePaths, 30, 100, 100);
        invalidate();
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


}
