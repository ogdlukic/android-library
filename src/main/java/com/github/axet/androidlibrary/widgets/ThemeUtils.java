package com.github.axet.androidlibrary.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.TypedValue;

import com.github.axet.androidlibrary.R;

public class ThemeUtils {

    public static int getThemeColor(Context context, int id) {
        TypedValue typedValue = new TypedValue();
        TypedArray a = context.obtainStyledAttributes(typedValue.data, new int[]{id});
        int color = a.getColor(0, 0);
        a.recycle();
        return color;
    }
}
