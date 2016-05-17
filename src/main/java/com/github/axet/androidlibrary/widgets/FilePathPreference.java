package com.github.axet.androidlibrary.widgets;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Environment;
import android.os.Parcelable;
import android.support.v7.preference.EditTextPreference;
import android.util.AttributeSet;

import java.io.File;

public class FilePathPreference extends EditTextPreference {
    public String def;
    public OpenFileDialog f;

    public FilePathPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public FilePathPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FilePathPreference(Context context) {
        this(context, null);
    }

    public String getDefault() {
        return Environment.getExternalStorageDirectory().getPath();
    }

    public void showDialog(Activity activity) {
        f = new OpenFileDialog(getContext());

        String path = getText();

        if (path == null || path.isEmpty()) {
            path = getDefault();
        }

        f.setCurrentPath(new File(path));
        f.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                File ff = f.getCurrentPath();
                String fileName = ff.getPath();
                if (callChangeListener(fileName)) {
                    setText(fileName);
                }
            }
        });
        f.show();
    }

    // load default value for sharedpropertiesmanager, or set it using xml.
    //
    // can't set dynamic values like '/sdcard'? he-he. so that what it for.
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        def = a.getString(index);

        // for file dialog we have (not selected) default value
        if(def.isEmpty())
            return "";

        return new File(getDefault(), def).getPath();
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
    }
}
