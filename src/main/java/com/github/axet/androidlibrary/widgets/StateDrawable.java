package com.github.axet.androidlibrary.widgets;

import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.util.StateSet;

import java.util.HashMap;

// Original StateListDrawable not respect drawable ColorFilter on old phones.
//
// http://stackoverflow.com/questions/6018602
//
public class StateDrawable extends StateListDrawable {
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
