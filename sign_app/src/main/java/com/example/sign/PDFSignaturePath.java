// ###WS@M Project:PDFelement ###
package com.example.sign;

import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * 签名路径
 * Created by Xiang Zhicheng on 2018/1/9.
 */
@SuppressWarnings("all")
public class PDFSignaturePath {
    private final boolean mClear;
    private final ArrayList<PointF> mPoints;

    public PDFSignaturePath(boolean clear) {
        mClear = clear;
        mPoints = new ArrayList<>();
    }

    public boolean isClear() {
        return mClear;
    }

    public ArrayList<PointF> getPoints() {
        return mPoints;
    }

    void add(PointF point) {
        mPoints.add(point);
    }

}
