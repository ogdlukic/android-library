package com.github.axet.androidlibrary.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.github.axet.androidlibrary.R;

public class UnreadCountDrawable extends Drawable implements Drawable.Callback {
    Context context;
    Drawable background;
    Callback backgroundCallback;
    Drawable badge;
    UnreadCount count;
    Rect rect = new Rect(0, 0, 100, 100);
    int padding;

    public interface UnreadCount {
        int getUnreadCount();
    }

    public UnreadCountDrawable(Context context, int resId, UnreadCount count) {
        this.context = context;
        this.count = count;

        background = ContextCompat.getDrawable(context, resId);
        background.setCallback(this);

        createDrawable();
    }

    public UnreadCountDrawable(Context context, Drawable dr, UnreadCount count) {
        this.context = context;
        this.count = count;
        this.rect = dr.getBounds();

        this.backgroundCallback = dr.getCallback();
        setCallback(backgroundCallback);

        this.background = dr;
        this.background.setCallback(this);

        createDrawable();
    }

    public void setPadding(int px) {
        this.padding = px;
    }

    @Override
    public void jumpToCurrentState() {
        // backgroundCallback may be cleared by reuse call
        if (backgroundCallback != null)
            background.setCallback(this);

        background.jumpToCurrentState();
    }

    @Override
    public Drawable getCurrent() {
        return super.getCurrent();
    }

    public void update() {
        createDrawable();
        invalidateSelf();
    }

    void createDrawable() {
        LayoutInflater inflater = LayoutInflater.from(context);
        View badge = inflater.inflate(R.layout.badge, null);
        badge.setPadding(padding, padding, padding, padding);

        TextView text = (TextView) badge.findViewById(R.id.badge_unread);
        text.setText("" + count.getUnreadCount());

        int widthSpec = View.MeasureSpec.makeMeasureSpec(rect.width(), View.MeasureSpec.AT_MOST);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(rect.height(), View.MeasureSpec.AT_MOST);
        badge.measure(widthSpec, heightSpec);
        badge.layout(0, 0, rect.width(), rect.height());
        badge.setDrawingCacheEnabled(true);
        badge.buildDrawingCache();
        this.badge = new BitmapDrawable(context.getResources(), badge.getDrawingCache());
        this.badge.setCallback(this);
        this.badge.setBounds(rect);
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, bottom);
        rect = new Rect(left, top, right, bottom);
        background.setBounds(left, top, right, bottom);
        createDrawable();
    }

    @Override
    public void setChangingConfigurations(int configs) {
        super.setChangingConfigurations(configs);
        background.setChangingConfigurations(configs);
        badge.setChangingConfigurations(configs);
    }

    @Override
    public void setAlpha(int alpha) {
        background.setAlpha(alpha);
    }

    @Override
    public void setFilterBitmap(boolean filter) {
        super.setFilterBitmap(filter);
        background.setFilterBitmap(filter);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public boolean isFilterBitmap() {
        return background.isFilterBitmap();
    }

    @Override
    public void draw(Canvas canvas) {
        background.draw(canvas);
        if (count.getUnreadCount() > 0)
            badge.draw(canvas);
    }

    @Override
    public int getOpacity() {
        return background.getOpacity();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        background.setColorFilter(colorFilter);
    }

    @Override
    public boolean setState(int[] stateSet) {
        return background.setState(stateSet);
    }

    @Override
    public int[] getState() {
        return background.getState();
    }

    @Override
    public void clearColorFilter() {
        super.clearColorFilter();
        background.clearColorFilter();
    }

    @Override
    public boolean isStateful() {
        return background.isStateful();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void applyTheme(Resources.Theme t) {
        super.applyTheme(t);
        background.applyTheme(t);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void setTint(int tintColor) {
        background.setTint(tintColor);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void setTintMode(PorterDuff.Mode tintMode) {
        background.setTintMode(tintMode);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void setTintList(ColorStateList tint) {
        background.setTintList(tint);
    }

    @Override
    public void invalidateDrawable(Drawable who) {
        Callback c = getCallback();
        if (c != null)
            c.invalidateDrawable(this);
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        Callback c = getCallback();
        if (c != null)
            c.scheduleDrawable(this, what, when);
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
        Callback c = getCallback();
        if (c != null)
            c.unscheduleDrawable(this, what);
    }
}
