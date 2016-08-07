package com.github.axet.androidlibrary.widgets;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v7.preference.EditTextPreference;
import android.util.AttributeSet;

public class RingtonePreference extends EditTextPreference {
    public RingtonePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public RingtonePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RingtonePreference(Context context) {
        this(context, null);
    }

    public void showDialog(Fragment activity, int code) {
        Uri uri = null;

        if (!getText().isEmpty()) {
            uri = Uri.parse(getText());
        }

        activity.startActivityForResult(new Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Reminder")
                .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, uri), code);
    }

    public void activityResult() {
        ;
    }
}
