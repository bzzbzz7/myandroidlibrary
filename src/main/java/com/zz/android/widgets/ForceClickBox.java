package com.zz.android.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/**
 * 点击加强View
 *
 * @author sky
 */
public class ForceClickBox extends FrameLayout {

    private long actionDownTime;
    private float actionDownX;
    private float actionDownY;
    private OnClickListener mOnCliClickListener;

    /**
     * 构造函数
     *
     * @param context
     * @param attrs
     */
    public ForceClickBox(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * 构造函数
     *
     * @param context
     */
    public ForceClickBox(Context context) {
        super(context);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        super.dispatchTouchEvent(ev);
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                actionDownTime = System.currentTimeMillis();
                actionDownX = ev.getRawX();
                actionDownY = ev.getRawY();
                break;
            case MotionEvent.ACTION_UP:
                if (System.currentTimeMillis() - actionDownTime < 500) {
                    if (Math.abs(ev.getRawX() - actionDownX) < 10
                            && Math.abs(ev.getRawY() - actionDownY) < 10) {
                        if (mOnCliClickListener != null) {
                            mOnCliClickListener.onClick(this);
                        }
                    }
                }
                break;
        }
        return true;
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        mOnCliClickListener = l;
    }
}
