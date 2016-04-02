package com.github.axet.androidlibrary.widgets;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ImageButton;

import com.github.axet.androidlibrary.R;

// old phones does not allow to refer colors from xml shapes.
//
// better to create simple class. java always better then xml (note to google)
//
public class RoundButton extends ImageButton {

    public RoundButton(Context context) {
        super(context);

        create();
    }

    public RoundButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        create();
    }

    public RoundButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        create();
    }

    void create() {
        ShapeDrawable d = new ShapeDrawable(new OvalShape());

        d.setColorFilter(ThemeUtils.getThemeColor(getContext(), R.attr.colorAccent), PorterDuff.Mode.SRC_ATOP);

        if (Build.VERSION.SDK_INT >= 16)
            setBackground(d);
        else
            setBackgroundDrawable(d);
    }
}
