package com.example.sign;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ws on 2018/2/2.
 */

public class MyView extends View {

    int width;
    int height;

    public MyView(Context context) {
        this(context, null);
    }

    public MyView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        width = wm.getDefaultDisplay().getWidth();
        height = wm.getDefaultDisplay().getHeight();

        paint.setStrokeWidth(5);
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);

        setBackgroundColor(Color.BLACK);

    }

    private List<List<PointF>> lines = new ArrayList<>();
    private List<PointF> currentLine;
    private Path path = new Path();


    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (lines == null || lines.isEmpty() || lines.size() < 2)
            return;
        canvas.drawBitmap(start(lines), 0, 0, paint);

        canvas.translate(0, 500);
        long time = System.currentTimeMillis();
        List<List<PointF>> ls = resetPointList(lines.get(0), start(lines));
        Log.i("---------- ", "onDraw: " + (System.currentTimeMillis() - time));

        for (List<PointF> l : ls) {
            draw(canvas, l, paint);
        }
        canvas.translate(0, -500);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        invalidate();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentLine = new ArrayList<>();
                lines.add(currentLine);
                currentLine.add(new PointF(event.getX(), event.getY()));
                return true;
            case MotionEvent.ACTION_MOVE:
                currentLine.add(new PointF(event.getX(), event.getY()));
                return true;
            case MotionEvent.ACTION_UP:
                if (lines.size() > 2)
                    lines.clear();
                return true;
        }

        return super.onTouchEvent(event);
    }

    private PorterDuffXfermode mXfermode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);

    static final int PREVENT_COORDINATE_OFFSET = 1;

    static final int PREVENT_COORDINATE_OFFSET_ = 2;

    Point point = new Point();

    List<List<PointF>> resetPointList(List<PointF> pencil, Bitmap bitmap) {
        Point point = this.point;

        List<List<PointF>> pencils = new ArrayList<>();
        List<PointF> current = new ArrayList<>();
        current.add(pencil.get(0));// 首先把线段开始节点加进去
        pencils.add(current);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(1);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.BLACK);
        Bitmap pencilBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8);
        Canvas canvas = new Canvas(pencilBitmap);

        final int count = pencil.size();
        boolean isAppearEnd = false;// current是否已经end，需要一个新的start
        for (int i = 1; i < count; i++) {
            PointF startPoint = pencil.get(i - 1);
            PointF endPoint = pencil.get(i);
            // 开始寻找尾节点
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            canvas.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y, paint);

            int xStart = (int) (startPoint.x);
            int xEnd = (int) (endPoint.x);
            float slope = (endPoint.y - startPoint.y) / (endPoint.x - startPoint.x);
            if (xStart < xEnd) {
                xStart -= PREVENT_COORDINATE_OFFSET;
                xStart = xStart >= 0 ? xStart : 0;
                xEnd += PREVENT_COORDINATE_OFFSET;
                xEnd = xEnd <= width ? xEnd : width;
                while (++xStart <= xEnd) {
                    long t = System.currentTimeMillis();
                    // 从pencil中获取该线段第一个点、第二个点、、、、
                    int yReference = (int) ((xStart - startPoint.x) * slope + startPoint.y);
                    yReference = yReference > PREVENT_COORDINATE_OFFSET_ ? yReference :
                            PREVENT_COORDINATE_OFFSET_;
                    yReference = yReference < height - PREVENT_COORDINATE_OFFSET_ ? yReference :
                            height - PREVENT_COORDINATE_OFFSET_;
                    getStartPointByX(pencilBitmap, xStart, xEnd, yReference, point);
                    if (point.x == -1 && point.y == -1)// 如果开始为null则停止循环
                        break;
                    //
                    boolean have = haveData(bitmap, point);
                    if (!have && !isAppearEnd) {// 找到尾节点
                        // 在该线段中存在尾节点
                        current.add(new PointF(point));
                        current = new ArrayList<>();
                        pencils.add(current);
                        isAppearEnd = true;
                    }
                    if (isAppearEnd && have) {// 找到头结点
                        isAppearEnd = false;
                        current.add(new PointF(point));
                    }
                }
                if (!isAppearEnd) {
                    current.add(pencil.get(i));
                }
            } else if (xStart > xEnd) {
                xStart += PREVENT_COORDINATE_OFFSET;
                xStart = xStart <= width ? xStart : width;
                xEnd -= PREVENT_COORDINATE_OFFSET;
                xEnd = xEnd >= 0 ? xEnd : 0;
                while (--xStart >= xEnd) {
                    // 从pencil中获取该线段第一个点、第二个点、、、、
                    int yReference = (int) ((xStart - startPoint.x) * slope + startPoint.y);
                    yReference = yReference > PREVENT_COORDINATE_OFFSET_ ? yReference :
                            PREVENT_COORDINATE_OFFSET_;
                    yReference = yReference < height - PREVENT_COORDINATE_OFFSET_ ? yReference :
                            height - PREVENT_COORDINATE_OFFSET_;
                    getStartPointByX(pencilBitmap, xStart, xEnd, yReference, point);
                    if (point.x == -1 && point.y == -1)// 如果开始为null则停止循环
                        break;
                    //
                    boolean have = haveData(bitmap, point);
                    if (!have && !isAppearEnd) {// 找到尾节点
                        // 在该线段中存在尾节点
                        current.add(new PointF(point));
                        current = new ArrayList<>();
                        pencils.add(current);
                        isAppearEnd = true;
                    }
                    if (isAppearEnd && have) {// 找到头结点
                        isAppearEnd = false;
                        current.add(new PointF(point));
                    }
                }
                if (!isAppearEnd) {
                    current.add(pencil.get(i));
                }
            } else {
                int yStart = (int) (pencil.get(i - 1).y);
                int yEnd = (int) (pencil.get(i).y);
                if (yStart < yEnd) {// 从上向下
                    yStart -= PREVENT_COORDINATE_OFFSET;
                    yStart = yStart >= 0 ? yStart : 0;
                    yEnd += PREVENT_COORDINATE_OFFSET;
                    yEnd = yEnd <= height ? yEnd : height;
                    while (++yStart <= yEnd) {
                        if (xStart != xEnd) {
                            throw new RuntimeException("xStart != xEnd");
                        }
                        getStartPointByY(pencilBitmap, yStart, yEnd, xStart, point);
                        if (point.x == -1 && point.y == -1)// 如果开始为null则停止循环
                            break;
                        boolean have = haveData(bitmap, point);
                        if (!have && !isAppearEnd) {// 找到尾节点
                            // 在该线段中存在尾节点
                            current.add(new PointF(point));
                            current = new ArrayList<>();
                            pencils.add(current);
                            isAppearEnd = true;
                        }
                        if (isAppearEnd && have) {// 找到头结点
                            isAppearEnd = false;
                            current.add(new PointF(point));
                        }
                    }
                } else {
                    yStart += PREVENT_COORDINATE_OFFSET;
                    yStart = yStart <= height ? yStart : height;
                    yEnd -= PREVENT_COORDINATE_OFFSET;
                    yEnd = yEnd >= 0 ? yEnd : 0;
                    while (--yStart >= yEnd) {
                        if (xStart != xEnd) {
                            throw new RuntimeException("xStart != xEnd");
                        }
                        getStartPointByY(pencilBitmap, yStart, yEnd, xStart, point);
                        if (point.x == -1 && point.y == -1)// 如果开始为null则停止循环
                            break;
                        boolean have = haveData(bitmap, point);
                        if (!have && !isAppearEnd) {// 找到尾节点
                            // 在该线段中存在尾节点
                            current.add(new PointF(point));
                            current = new ArrayList<>();
                            pencils.add(current);
                            isAppearEnd = true;
                        }
                        if (isAppearEnd && have) {// 找到头结点
                            isAppearEnd = false;
                            current.add(new PointF(point));
                        }
                    }
                }
            }
        }
        return pencils;
    }


    @Nullable
    Point getStartPoint(Bitmap bitmap, int x) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        ByteBuffer buffer = ByteBuffer.allocate(width * height);
        bitmap.copyPixelsToBuffer(buffer);
        int loop = x + 20 < width ? x + 10 : width;
        while (x < loop) {
            for (int y = 0; y < height; y++) {
                if (buffer.get(y * width + x) != 0) {
                    return new Point(x, y);
                }
            }
            x++;
        }
        return null;
    }

    @Nullable
    Point getStartPointByX(Bitmap bitmap, int start, int end) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        ByteBuffer buffer = ByteBuffer.allocate(width * height);
        bitmap.copyPixelsToBuffer(buffer);
        if (start < end) {
            while (start <= end) {
                for (int y = 0; y < height; y++) {
                    if (buffer.get(y * width + start) != 0) {
                        return new Point(start, y);
                    }
                }
                start++;
            }
        } else {
            while (start >= end) {
                for (int y = 0; y < height; y++) {
                    if (buffer.get(y * width + start) != 0) {
                        return new Point(start, y);
                    }
                }
                start--;
            }
        }
        return null;
    }

    /**
     * 找出当屏幕x坐标为xStart时像素位置
     */
    @Nullable
    Point getStartPointByX(Bitmap bitmap, int start, int end, int yBottom, int yTop) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        ByteBuffer buffer = ByteBuffer.allocate(width * height);
        bitmap.copyPixelsToBuffer(buffer);
        if (start < end) {
            while (start <= end) {
                for (int y = yBottom; y < yTop; y++) {
                    if (buffer.get(y * width + start) != 0) {
                        return new Point(start, y);
                    }
                }
                start++;
            }
        } else {
            while (start >= end) {
                for (int y = yBottom; y < yTop; y++) {
                    if (buffer.get(y * width + start) != 0) {
                        return new Point(start, y);
                    }
                }
                start--;
            }
        }
        return null;
    }


    void getStartPointByX(Bitmap bitmap, int start, int end, int yReference, Point point, int sdk) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        ByteBuffer buffer = ByteBuffer.allocate(width * height);
        bitmap.copyPixelsToBuffer(buffer);

        if (start < end) {
            while (start <= end) {
                int y = yReference;
                if (buffer.get(start + y * width) != 0) {
                    point.set(start, y);
                    return;
                }
                for (int i = 1; i <= PREVENT_COORDINATE_OFFSET_; i++) {
                    y = yReference - i;
                    if (buffer.get(start + y * width) != 0) {
                        point.set(start, y);
                        return;
                    }
                    y = yReference + i;
                    if (buffer.get(start + y * width) != 0) {
                        point.set(start, y);
                        return;
                    }
                }
                start++;
            }
        } else {
            while (start >= end) {
                int y = yReference;
                if (buffer.get(start + y * width) != 0) {
                    point.set(start, y);
                    return;
                }
                for (int i = 1; i <= PREVENT_COORDINATE_OFFSET_; i++) {
                    y = yReference - i;
                    if (buffer.get(start + y * width) != 0) {
                        point.set(start, y);
                        return;
                    }
                    y = yReference + i;
                    if (buffer.get(start + y * width) != 0) {
                        point.set(start, y);
                        return;
                    }
                }
                start--;
            }
        }
        point.set(-1, -1);
    }

    void getStartPointByX(Bitmap bitmap, int start, int end, int yReference, Point point) {
        if (start < end) {
            while (start <= end) {
                int y = yReference;
                if (getPixel(bitmap, start, y) != 0) {
                    point.set(start, y);
                    return;
                }
                for (int i = 1; i <= PREVENT_COORDINATE_OFFSET_; i++) {
                    y = yReference - i;
                    if (getPixel(bitmap, start, y) != 0) {
                        point.set(start, y);
                        return;
                    }
                    y = yReference + i;
                    if (getPixel(bitmap, start, y) != 0) {
                        point.set(start, y);
                        return;
                    }
                }
                start++;
            }
        } else {
            while (start >= end) {
                int y = yReference;
                if (getPixel(bitmap, start, y) != 0) {
                    point.set(start, y);
                    return;
                }
                for (int i = 1; i <= PREVENT_COORDINATE_OFFSET_; i++) {
                    y = yReference - i;
                    if (getPixel(bitmap, start, y) != 0) {
                        point.set(start, y);
                        return;
                    }
                    y = yReference + i;
                    if (getPixel(bitmap, start, y) != 0) {
                        point.set(start, y);
                        return;
                    }
                }
                start--;
            }
        }
        point.set(-1, -1);
    }

    /**
     * note 调用次方法的时候两个点的x值是一样的
     */
    @Nullable
    void getStartPointByY(Bitmap bitmap, int start, int end, int x, Point point) {
        if (start < end) {
            while (start <= end) {
                if (getPixel(bitmap, x, start) != 0) {
                    point.set(x, start);
                    return;
                }
                start++;
            }
        } else {
            while (start >= end) {
                if (getPixel(bitmap, x, start) != 0) {
                    point.set(x, start);
                    return;
                }
                start--;
            }
        }
        point.set(-1, -1);
    }

    boolean haveData(Bitmap bitmap, @NonNull Point point) {
        return getPixel(bitmap, point.x, point.y) != 0;
    }

    boolean haveData(Bitmap bitmap, @NonNull Point point, int sdk) {
        int width = bitmap.getWidth();


        return bitmap.getPixel(point.x, point.y) != 0;
    }

    Bitmap start(List<List<PointF>> lines) {
        // 获得Bitmap
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(5);
        paint.setStyle(Paint.Style.STROKE);

        paint.setColor(Color.BLACK);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8);
        Canvas canvas = new Canvas(bitmap);
        List<PointF> one = lines.get(0);
        List<PointF> two = lines.get(1);

        Path path = invalidatePath(one, null);
        canvas.drawPath(path, paint);

        path = invalidatePath(two, path);
        paint.setXfermode(mXfermode);
        paint.setStrokeWidth(20);
        canvas.drawPath(path, paint);
        paint.setXfermode(null);
        paint.setStrokeWidth(5);

        return bitmap;
    }

    Path invalidatePath(List<PointF> points, Path path) {
        if (points.size() <= 0)
            return null;
        if (path == null) {
            path = new Path();
        } else {
            path.rewind();
        }
        final int count = points.size();
        path.moveTo(points.get(0).x, points.get(0).y);
        for (int i = 1; i < count; i++) {
            path.lineTo(points.get(i).x, points.get(i).y);
        }
        return path;
    }

    void draw(Canvas canvas, List<PointF> points, Paint paint) {
        for (int i = 1; i < points.size(); i++) {
            canvas.drawLine(points.get(i - 1).x, points.get(i - 1).y, points.get(i).x, points.get
                    (i).y, paint);
        }
    }

    void logBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        ByteBuffer buffer = ByteBuffer.allocate(width * height);
        bitmap.copyPixelsToBuffer(buffer);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (0 != getPixel(bitmap, x, y)) {
                    Log.i("-----= ", "x: " + x + " y:" + y);
                }
            }
        }

        int i = 0;
        int loop = width * height;
        while (++i < loop / 3) {
            if (buffer.get(i) != 0) {
                Log.i("---------- ", "logBitmap: " + buffer.get(i) + " " + i);
            }
        }

    }

    void clearCanvas(Canvas canvas) {
        Paint paint = new Paint();
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawPaint(paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
    }


    int getPixel(Bitmap bitmap, int x, int y) {
        int[] value = new int[1];
        bitmap.getPixels(value, 0, 10, x, y, 1, 1);
        return value[0];
    }
}
