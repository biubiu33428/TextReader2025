package com.my.textreader.view.animation;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.widget.Scroller;

//各种动画 翻页
public class NoneAnimation extends AnimationProvider {

    public NoneAnimation(Bitmap mCurrentBitmap, Bitmap mNextBitmap, int width, int height) {
        super(mCurrentBitmap, mNextBitmap, width, height);
    }

    @Override
    public void drawMove(Canvas canvas) {
        if (getCancel()) {
            canvas.drawBitmap(mCurPageBitmap, 0, 0, null);
        } else {
            canvas.drawBitmap(mNextPageBitmap, 0, 0, null);
        }
    }

    @Override
    public void drawStatic(Canvas canvas) {
        if (getCancel()) {
            canvas.drawBitmap(mCurPageBitmap, 0, 0, null);
        } else {
            canvas.drawBitmap(mNextPageBitmap, 0, 0, null);
        }
    }

    @Override
    public void startAnimation(Scroller scroller) {

    }

}
