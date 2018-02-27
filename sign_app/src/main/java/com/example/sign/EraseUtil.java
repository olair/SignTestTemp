package com.example.sign;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;

/**
 * 用于擦除的工具类
 * <p>
 * Created by ws on 2018/2/3.
 */

public class EraseUtil {

    // 偏移 将点坐标转为int类型时的一个偏移范围
    private static final int PREVENT_COORDINATE_OFFSET = 1;

    // 偏移 计算线段内没一点坐标的偏移范围
    private static final int PREVENT_COORDINATE_OFFSET_SEARCH = 3;

    private static PorterDuffXfermode XFERMODE = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);

    private static Point CACHE_POINT = new Point();


    /**
     * 提供给外部调用的函数。
     *
     * @param signaturePaths 上层原始点数据。
     * @param rubberWidth    橡皮擦宽度。
     * @param width          可以容纳所有所有路径点一个width。
     * @param height         可以容纳所有路径点的一个height。
     * @return 返回计算完成的点集。
     */
    public static List<List<PointF>> calculateList(List<PDFSignaturePath> signaturePaths, float
            rubberWidth, int width, int height) {
        List<List<PointF>> result = new ArrayList<>();
        // 获得目标图形样式
        Bitmap targetBitmap;
        try {
            targetBitmap = createBitmap(width, height);
        } catch (OutOfMemoryError error) {
            return result;
        }
        initTargetBitmap(signaturePaths, rubberWidth, targetBitmap);
        // 开始计算
        Bitmap pencilBitmap;
        try {
            pencilBitmap = createBitmap(width, height);
        } catch (OutOfMemoryError error) {
            targetBitmap.recycle();
            return result;
        }

        for (PDFSignaturePath signaturePath : signaturePaths) {
            if (!signaturePath.isClear()) {
                List<List<PointF>> pencilLines = resetPointList(signaturePath.getPoints(),
                        targetBitmap, pencilBitmap);
                result.addAll(pencilLines);
            }
        }
        targetBitmap.recycle();
        pencilBitmap.recycle();
        return result;
    }

    private static void initTargetBitmap(List<PDFSignaturePath> signaturePaths, float rubberWidth,
                                         Bitmap bitmap) {
        // 创建画笔
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        // 构建canvas
//        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8);
        Canvas canvas = new Canvas(bitmap);
        // 创建一个Path
        Path path = new Path();
        // 开始绘制图形
        for (PDFSignaturePath signaturePath : signaturePaths) {
            if (!signaturePath.isClear()) {
                invalidatePath(signaturePath.getPoints(), path);
                drawPencil(path, 1, canvas, paint);
            } else {
                invalidatePath(signaturePath.getPoints(), path);
                erasePencil(path, rubberWidth, canvas, paint);
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static void drawPencil(Path pencilPath, float pencilLineWidth, Canvas canvas,
                                   Paint paint) {
        // 设置画笔属性
        paint.setXfermode(null);
        paint.setColor(Color.RED);
        paint.setStrokeWidth(pencilLineWidth);
        // 绘制
        canvas.drawPath(pencilPath, paint);
    }

    @SuppressWarnings("SameParameterValue")
    private static void erasePencil(Path rubberPath, float rubberWidth, Canvas canvas,
                                    Paint paint) {
        // 设置画笔属性
        paint.setXfermode(XFERMODE);
        paint.setStrokeWidth(rubberWidth);
        // 绘制
        canvas.drawPath(rubberPath, paint);
    }

    private static void invalidatePath(List<PointF> points, Path path) {
        if (points.size() <= 0)
            return;
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
    }

    private static List<List<PointF>> resetPointList(List<PointF> pencil, Bitmap targetBitmap,
                                                     Bitmap pencilBitmap) {
        Point point = CACHE_POINT;
        int width = targetBitmap.getWidth();
        int height = targetBitmap.getHeight();

        List<List<PointF>> pencils = new ArrayList<>();
        List<PointF> current = new ArrayList<>();
        pencils.add(current);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(1);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
        Canvas canvas = new Canvas(pencilBitmap);

        final int count = pencil.size();
        boolean isAppearEnd = true;// current是否已经end，需要一个新的start
        for (int i = 1; i < count; i++) {
            PointF startPoint = pencil.get(i - 1);
            PointF endPoint = pencil.get(i);
            // 如果超出bitmap不进行处理
            if (startPoint.equals(endPoint)) {
                continue;// 去除不必要点的计算(同时避免了计算slope时出现NaN)
            }

            // 如果超出bitmap不进行处理
            if (!isInBitmap(width, height, startPoint) && !isInBitmap(width, height, endPoint)) {
                continue;
            }
            // 开始寻找尾节点
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            canvas.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y, paint);
            float slope = (endPoint.y - startPoint.y) / (endPoint.x - startPoint.x);// 允许计算出无限大
            if (slope < 1 && slope > -1) {// 直线更偏向于x轴
                int xStart = (int) (startPoint.x);
                int xEnd = (int) (endPoint.x);
                if (xStart < xEnd) {
                    xStart -= PREVENT_COORDINATE_OFFSET;
                    xStart = xStart >= 0 ? xStart : 0;
                    xEnd += PREVENT_COORDINATE_OFFSET;
                    xEnd = xEnd < width ? xEnd : width - 1;
                    while (xStart <= xEnd) {
                        int yReference = (int) ((xStart - startPoint.x) * slope + startPoint.y);
                        // 从pencil中获取该线段第一个点、第二个点、、、、
                        getStartPointByXToRight(pencilBitmap, xStart, xEnd, yReference, point);
                        xStart++;
                        if (point.x == -1 && point.y == -1)// 如果开始为null则停止循环
                            break;
                        //
                        boolean have = haveData(targetBitmap, point);
                        if (!have && !isAppearEnd) {// 找到尾节点
                            // 在该线段中存在尾节点
                            current.add(new PointF(point));
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
                    xStart = xStart < width ? xStart : width - 1;
                    xEnd -= PREVENT_COORDINATE_OFFSET;
                    xEnd = xEnd >= 0 ? xEnd : 0;
                    while (xStart >= xEnd) {
                        int yReference = (int) ((xStart - startPoint.x) * slope + startPoint.y);
                        getStartPointByXToLeft(pencilBitmap, xStart, xEnd, yReference, point);
                        xStart--;
                        if (point.x == -1 && point.y == -1)// 如果开始为null则停止循环
                            break;
                        //
                        boolean have = haveData(targetBitmap, point);
                        if (!have && !isAppearEnd) {// 找到尾节点
                            // 在该线段中存在尾节点
                            current.add(new PointF(point));
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
                }
            } else {
                int yStart = (int) (startPoint.y);
                int yEnd = (int) (endPoint.y);
                if (yStart < yEnd) {// 从上向下
                    yStart -= PREVENT_COORDINATE_OFFSET;
                    yStart = yStart >= 0 ? yStart : 0;
                    yEnd += PREVENT_COORDINATE_OFFSET;
                    yEnd = yEnd < height ? yEnd : height - 1;
                    while (yStart <= yEnd) {
                        int xReference = (int) (startPoint.x + (yStart - startPoint.y) / slope);
                        getStartPointByYToBottom(pencilBitmap, yStart, yEnd, xReference, point);
                        yStart++;
                        if (point.x == -1 && point.y == -1)// 如果开始为null则停止循环
                            break;
                        boolean have = haveData(targetBitmap, point);
                        if (!have && !isAppearEnd) {// 找到尾节点
                            // 在该线段中存在尾节点
                            current.add(new PointF(point));
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
                    yStart += PREVENT_COORDINATE_OFFSET;
                    yStart = yStart < height ? yStart : height - 1;
                    yEnd -= PREVENT_COORDINATE_OFFSET;
                    yEnd = yEnd >= 0 ? yEnd : 0;
                    while (yStart >= yEnd) {
                        int xReference = (int) (startPoint.x + (yStart - startPoint.y) / slope);
                        getStartPointByYToTop(pencilBitmap, yStart, yEnd, xReference, point);
                        yStart--;
                        if (point.x == -1 && point.y == -1)// 如果开始为null则停止循环
                            break;
                        boolean have = haveData(targetBitmap, point);
                        if (!have && !isAppearEnd) {// 找到尾节点
                            // 在该线段中存在尾节点
                            current.add(new PointF(point));
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
                }
            }
        }
        return pencils;
    }

    private static boolean isInBitmap(int width, int height, PointF point) {
        return !(point.x >= width || point.x < 0 || point.y >= height || point.y < 0);
    }

    // 线段所属直线偏向于x轴并且从左到右
    private static void getStartPointByXToRight(Bitmap bitmap, int start, int end, int
            yReference, Point point) {
        while (start <= end) {
            if (findPointByX(bitmap, start, yReference, PREVENT_COORDINATE_OFFSET_SEARCH, point)) {
                return;
            }
            start++;
        }
        point.set(-1, -1);
    }

    private static void getStartPointByXToLeft(Bitmap bitmap, int start, int end, int yReference,
                                               Point point) {
        while (start >= end) {
            if (findPointByX(bitmap, start, yReference, PREVENT_COORDINATE_OFFSET_SEARCH, point)) {
                return;
            }
            start--;
        }
        point.set(-1, -1);
    }

    private static boolean findPointByX(Bitmap bitmap, int x, int yReference, int offset, Point
            point) {
        int max = bitmap.getHeight() - 1;
        int min = 0;
        int y = checkRules(max, min, yReference);
        if (bitmap.getPixel(x, y) != 0) {
            point.set(x, y);
            return true;
        }
        for (int i = 1; i < offset; i++) {
            y = checkRules(max, min, yReference - i);
            if (bitmap.getPixel(x, y) != 0) {
                point.set(x, y);
                return true;
            }
            y = checkRules(max, min, yReference + i);
            if (bitmap.getPixel(x, y) != 0) {
                point.set(x, y);
                return true;
            }
        }
        return false;
    }

    private static void getStartPointByYToBottom(Bitmap bitmap, int start, int end,
                                                 int xReference, Point point) {
        while (start <= end) {
            if (findPointByY(bitmap, start, xReference, PREVENT_COORDINATE_OFFSET_SEARCH, point)) {
                return;
            }
            start++;
        }
        point.set(-1, -1);
    }

    private static void getStartPointByYToTop(Bitmap bitmap, int start, int end, int xReference,
                                              Point point) {
        while (start >= end) {
            if (findPointByY(bitmap, start, xReference, PREVENT_COORDINATE_OFFSET_SEARCH, point)) {
                return;
            }
            start--;
        }
        point.set(-1, -1);
    }

    private static boolean findPointByY(Bitmap bitmap, int y, int xReference, int offset, Point
            point) {
        int max = bitmap.getWidth() - 1;
        int min = 0;
        int x = checkRules(max, min, xReference);
        if (bitmap.getPixel(x, y) != 0) {
            point.set(x, y);
            return true;
        }

        for (int i = 1; i < offset; i++) {
            x = checkRules(max, min, xReference - i);
            if (bitmap.getPixel(x, y) != 0) {
                point.set(x, y);
                return true;
            }
            x = checkRules(max, min, xReference + i);
            if (bitmap.getPixel(x, y) != 0) {
                point.set(x, y);
                return true;
            }
        }
        return false;
    }

    private static int checkRules(int max, int min, int value) {
        if (value > max) {
            value = max;
        }
        if (value < min) {
            value = min;
        }
        return value;
    }

    private static boolean haveData(Bitmap bitmap, Point point) {
        return bitmap.getPixel(point.x, point.y) != 0;
    }

    private static Bitmap createBitmap(float imageWidth, float imageHeight) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Bitmap.createBitmap((int) imageWidth, (int) imageHeight, Bitmap.Config
                    .ALPHA_8);
        } else {
            return Bitmap.createBitmap((int) imageWidth, (int) imageHeight, Bitmap.Config
                    .ARGB_8888);
        }
    }
}
