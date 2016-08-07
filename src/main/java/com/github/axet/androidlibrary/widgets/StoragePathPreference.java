package com.github.axet.androidlibrary.widgets;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.view.View;

import java.io.File;

public class StoragePathPreference extends EditTextPreference {
    public String def;
    public OpenFileDialog f;

    public StoragePathPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public StoragePathPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StoragePathPreference(Context context) {
        this(context, null);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
    }

    public String getDefault() {
        return Environment.getExternalStorageDirectory().getPath();
    }

    @Override
    protected void showDialog(Bundle state) {
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
                if (!ff.isDirectory())
                    fileName = ff.getParent();
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
        return new File(getDefault(), def).getPath();
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
    }
}
