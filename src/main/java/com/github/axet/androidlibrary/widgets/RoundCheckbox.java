package com.github.axet.androidlibrary.widgets;

import android.animation.ObjectAnimator;
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
import android.util.Property;
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

    public static int SECOND_BACKGROUND = 0x22222222;

    ObjectAnimator animator;

    float stateAnimator;

    // Original StateListDrawable not respect drawable ColorFilter on old phones.
    //
    // http://stackoverflow.com/questions/6018602
    //
    public static class StateDrawable extends StateListDrawable {
        HashMap<Drawable, State> map = new HashMap<>();

        public static class State {
            PorterDuffColorFilter colorFilter;
            int[] states;

            public State(PorterDuffColorFilter c, int[] s) {
                this.colorFilter = c;
                this.states = s;
            }
        }

        public StateDrawable() {
        }

        public void addState(int[] stateSet, Drawable drawable, PorterDuffColorFilter p) {
            super.addState(stateSet, drawable);
            map.put(drawable, new State(p, stateSet));
        }

        @Override
        protected boolean onStateChange(int[] states) {
            // 19 not working, 20 untested, 21 working
            if (Build.VERSION.SDK_INT < 21) {
                if (map != null) {
                    for (Drawable d : map.keySet()) {
                        if (StateSet.stateSetMatches(map.get(d).states, states)) {
                            setColorFilter(map.get(d).colorFilter);
                            break;
                        }
                    }
                }
            }
            return super.onStateChange(states);
        }
    }

    Property<RoundCheckbox, Float> STATE_ANIMATOR = new Property<RoundCheckbox, Float>(Float.class, "stateAnimator") {
        @Override
        public Float get(RoundCheckbox object) {
            return object.stateAnimator;
        }

        @Override
        public void set(RoundCheckbox object, Float value) {
            object.stateAnimator = value;
            object.invalidate();
        }
    };

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

    public void create() {
        ShapeDrawable checkbox_on = new ShapeDrawable(new OvalShape());
        PorterDuffColorFilter checkbox_on_filter = new PorterDuffColorFilter(ThemeUtils.getThemeColor(getContext(), R.attr.colorAccent), PorterDuff.Mode.SRC_ATOP);
        checkbox_on.setColorFilter(checkbox_on_filter);
        ShapeDrawable checkbox_off = new ShapeDrawable(new OvalShape());
        PorterDuffColorFilter checkbox_off_filter = new PorterDuffColorFilter(SECOND_BACKGROUND, PorterDuff.Mode.MULTIPLY);
        checkbox_off.setColorFilter(checkbox_off_filter);

        StateDrawable background = new StateDrawable();
        background.addState(new int[]{android.R.attr.state_checked}, checkbox_on, checkbox_on_filter);
        background.addState(new int[]{-android.R.attr.state_checked}, checkbox_off, checkbox_off_filter);

        background.setExitFadeDuration(500);
        background.setEnterFadeDuration(500);

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

    @Override
    public void setChecked(boolean checked) {
        super.setChecked(checked);

        animator = ObjectAnimator.ofFloat(this, STATE_ANIMATOR, 1f);
        animator.setDuration(500);
        if (Build.VERSION.SDK_INT >= 18)
            animator.setAutoCancel(true);
        animator.start();
    }

}
