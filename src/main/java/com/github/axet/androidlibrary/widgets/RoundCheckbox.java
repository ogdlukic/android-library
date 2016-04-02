package com.github.axet.androidlibrary.widgets;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.util.Log;
import android.util.StateSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;

import com.github.axet.androidlibrary.R;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;

// old phones does not allow to refer colors from xml shapes.
//
// better to create simple class. java always better then xml (note to google)
//
public class RoundCheckbox extends CheckBox {

    public static class StateDrawable extends StateListDrawable {
        HashMap<Drawable, PorterDuffColorFilter> map = new HashMap<>();
        HashMap<Drawable, int[]> map2 = new HashMap<>();

        public StateDrawable() {
        }

        public void addState(int[] stateSet, Drawable drawable, PorterDuffColorFilter p) {
            super.addState(stateSet, drawable);
            map.put(drawable, p);
            map2.put(drawable, stateSet);
        }

        @Override
        protected boolean onStateChange(int[] states) {
            if (map != null) {
                for (Drawable d : map.keySet()) {
                    if (StateSet.stateSetMatches(map2.get(d), states)) {
                        setColorFilter(map.get(d));
                        break;
                    }
                }
            }
            return super.onStateChange(states);
        }

    }

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
        ShapeDrawable checkbox_off = new ShapeDrawable(new OvalShape());

        StateDrawable background = new StateDrawable();
        background.addState(new int[]{android.R.attr.state_checked},
                checkbox_on,
                new PorterDuffColorFilter(ThemeUtils.getThemeColor(getContext(), R.attr.colorAccent), PorterDuff.Mode.SRC_ATOP));
        background.addState(new int[]{-android.R.attr.state_checked},
                checkbox_off,
                new PorterDuffColorFilter(0x22222222, PorterDuff.Mode.MULTIPLY));

        if (Build.VERSION.SDK_INT >= 16)
            setBackground(background);
        else
            setBackgroundDrawable(background);

        // reset padding set by previous background from constructor
        setPadding(0, 0, 0, 0);

        setButtonDrawable(android.R.color.transparent);

        setGravity(Gravity.CENTER);

        setTypeface(null, Typeface.BOLD);

        ColorStateList colors = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{-android.R.attr.state_checked},
                },
                new int[]{
                        Color.WHITE,
                        ThemeUtils.getThemeColor(getContext(), android.R.attr.textColorHint),
                });

        setTextColor(colors);
    }
}
