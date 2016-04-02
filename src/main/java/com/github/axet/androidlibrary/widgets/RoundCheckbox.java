package com.github.axet.androidlibrary.widgets;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;

import com.github.axet.androidlibrary.R;

// old phones does not allow to refer colors from xml shapes.
//
// better to create simple class. java always better then xml (note to google)
//
public class RoundCheckbox extends CheckBox {

    public RoundCheckbox(Context context) {
        super(context);

        create();
    }

    public RoundCheckbox(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        create();
    }

    public RoundCheckbox(Context context, AttributeSet attrs) {
        super(context, attrs);

        create();
    }

    void create() {
        ShapeDrawable checkbox_on = new ShapeDrawable(new OvalShape());
        checkbox_on.setColorFilter(ThemeUtils.getThemeColor(getContext(), R.attr.colorAccent), PorterDuff.Mode.SRC_ATOP);

        ShapeDrawable checkbox_off = new ShapeDrawable(new OvalShape());
        checkbox_off.setColorFilter(ThemeUtils.getThemeColor(getContext(), android.R.attr.colorBackgroundFloating), PorterDuff.Mode.SRC_ATOP);

        StateListDrawable background = new StateListDrawable();
        background.addState(new int[]{android.R.attr.state_checked}, checkbox_on);
        background.addState(new int[]{-android.R.attr.state_checked}, checkbox_off);

        if (Build.VERSION.SDK_INT >= 16)
            setBackground(background);
        else
            setBackgroundDrawable(background);

        setButtonDrawable(android.R.color.transparent);

        setGravity(Gravity.CENTER);

        setTypeface(null, Typeface.BOLD);

        ColorStateList colors = new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{-android.R.attr.state_checked}},
                new int[]{Color.WHITE,
                        ThemeUtils.getThemeColor(getContext(), android.R.attr.textColorHint)});

        setTextColor(colors);
    }
}
