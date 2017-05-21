package com.zz.android.utils;

import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class TextViewUtils {
    private TextViewUtils() {
        /* cannot be instantiated */
        throw new UnsupportedOperationException("cannot be instantiated");
    }

    public static void append(TextView tv, String msg) {
        if (tv.getMaxLines() != 200) {
            tv.setMaxLines(200);
        }
        if (tv.getGravity() != android.view.Gravity.BOTTOM) {
            tv.setGravity(android.view.Gravity.BOTTOM);
        }
        if (tv.getMovementMethod() == null) {
            tv.setMovementMethod(new ScrollingMovementMethod());
        }

        tv.append(TimeUtils.getCurTimeString(new SimpleDateFormat("hh:mm:ss", Locale.ENGLISH)) + " " + msg + "\n");
    }
}
