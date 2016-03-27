package com.github.axet.androidlibrary.animations;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Transformation;

public class MarginBottomAnimation extends StepAnimation {

    ViewGroup.MarginLayoutParams viewLp;
    ViewGroup.MarginLayoutParams viewLpOrig;

    int vh;

    public static void apply(final View v, final boolean expand, boolean animate) {
        apply(new LateCreator() {
            @Override
            public MarginBottomAnimation create() {
                return new MarginBottomAnimation(v, expand);
            }
        }, v, expand, animate);
    }

    public MarginBottomAnimation(View v, boolean expand) {
        super(v, expand);

        setDuration(500);

        viewLp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        viewLpOrig = new ViewGroup.MarginLayoutParams(viewLp);
    }

    @Override
    public void init() {
        super.init();

        ViewGroup parent = (ViewGroup) view.getParent();
        int parentWidth = parent.getWidth();
        int parentHeight = parent.getHeight();

        int width = view.getWidth();
        int height = view.getHeight();

        int h;
        int w;

        if (height == 0 && parentHeight == 0)
            h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        else
            h = View.MeasureSpec.makeMeasureSpec(Math.max(height, parentHeight), View.MeasureSpec.AT_MOST);

        w = View.MeasureSpec.makeMeasureSpec(Math.max(width, parentWidth), View.MeasureSpec.AT_MOST);

        view.measure(w, h);

        vh = view.getMeasuredHeight();
    }

    @Override
    public void calc(float i, Transformation t) {
        super.calc(i, t);

        i = expand ? 1 - i : i;

        viewLp.bottomMargin = (int) (-vh * i);
        view.requestLayout();
    }

    @Override
    public void restore() {
        super.restore();

        viewLp.bottomMargin = viewLpOrig.bottomMargin;
    }

    @Override
    public void end() {
        super.end();

        view.setVisibility(expand ? View.VISIBLE : View.GONE);
        view.requestLayout();
    }

}
