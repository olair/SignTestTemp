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
public class PDFSignaturePath implements Parcelable {
    private final boolean mClear;
    private final ArrayList<PointF> mPoints;

    public PDFSignaturePath(boolean clear, ArrayList<PointF> points) {
        mClear = clear;
        mPoints = points;
    }

    public boolean isClear() {
        return mClear;
    }

    public ArrayList<PointF> getPoints() {
        return mPoints;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(this.mClear ? (byte) 1 : (byte) 0);
        dest.writeTypedList(this.mPoints);
    }

    protected PDFSignaturePath(Parcel in) {
        this.mClear = in.readByte() != 0;
        this.mPoints = in.createTypedArrayList(PointF.CREATOR);
    }

    public static final Creator<PDFSignaturePath> CREATOR = new Creator<PDFSignaturePath>() {
        @Override
        public PDFSignaturePath createFromParcel(Parcel source) {
            return new PDFSignaturePath(source);
        }

        @Override
        public PDFSignaturePath[] newArray(int size) {
            return new PDFSignaturePath[size];
        }
    };
}
