package com.github.axet.androidlibrary.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class PathMax extends ViewGroup {
    public static final String ROOT = "/";
    public static final String MID = "...";

    String s = ROOT;

    boolean ignore = false;

    // xml call
    public PathMax(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    // xml call
    public PathMax(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    // created manualu
    public PathMax(Context context, TextView text) {
        super(context);

        addView(text);
    }

    @Override
    public void addView(View child, int index, LayoutParams params) {
        super.addView(child, index, params);

        attach(child);
    }

    void attach(View v) {
        if (v instanceof TextView) {
            ((TextView) v).addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    save();
                }
            });
            save();
        }
    }

    void save() {
        if (ignore)
            return;
        TextView text = (TextView) getChildAt(0);
        s = text.getText().toString();
    }

    public int getTextWidth(String text, Paint paint) {
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        return bounds.left + bounds.width();
    }

    public String makePath(List<String> ss) {
        if (ss.size() == 0)
            return ROOT;
        return TextUtils.join(File.separator, ss);
    }

    public List<String> splitPath(String s) {
        return new ArrayList<String>(Arrays.asList(s.split(Pattern.quote(File.separator))));
    }

    int getMaxWidth() {
        TextView text = (TextView) getChildAt(0);

        return text.getWidth() - text.getPaddingLeft() - text.getPaddingRight() - getPaddingLeft() - getPaddingRight();
    }

    public void updateText(int max) {
        TextView text = (TextView) getChildAt(0);

        List<String> ss = splitPath(s);

        // when s == "/"
        if (ss.size() == 0) {
            set(s);
            return;
        }

        // check if file not actual path, but filename itself
        boolean single = new File(s).getName().equals(s);

        List<String> ssdots = ss;

        String sdots = makePath(ssdots);

        while (getTextWidth(sdots, text.getPaint()) >= max) {
            if (ss.size() == 1) {
                String sdot = ss.get(0);

                // we reached empty string, very low actual TextView Width.
                if (sdot.length() == 0) {
                    set(MID);
                    return;
                }

                int mid;
                // cant go lower return
                if (sdot.length() <= 2) {
                    set(MID);
                    return;
                }
                mid = sdot.length() / 2;
                // cut mid char
                sdot = sdot.substring(0, mid) + sdot.substring(mid + 1, sdot.length());

                ss.set(0, sdot);

                sdot = sdot.substring(0, mid) + MID + sdot.substring(mid, sdot.length());

                if (!single) {
                    sdot = MID + File.separator + sdot;
                }

                sdots = sdot;
            } else {
                int mid = (ss.size() - 1) / 2;
                ssdots = new ArrayList<>(ss);
                ssdots.set(mid, MID);
                ss.remove(mid);
                sdots = makePath(ssdots);
            }
        }

        set(sdots);
    }

    void set(String sdots) {
        TextView text = (TextView) getChildAt(0);

        String old = text.getText().toString();
        if (!old.equals(sdots)) {
            ignore = true;
            text.setText(sdots);
            ignore = false;
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        TextView text = (TextView) getChildAt(0);

        text.layout(l, t, r, b);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        TextView text = (TextView) getChildAt(0);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        updateText(widthSize - text.getPaddingLeft() - text.getPaddingRight());

        text.measure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY), heightMeasureSpec);

        setMeasuredDimension(text.getMeasuredWidth(), text.getMeasuredHeight());
    }
}
