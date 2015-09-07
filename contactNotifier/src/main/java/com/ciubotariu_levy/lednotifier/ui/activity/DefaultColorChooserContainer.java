package com.ciubotariu_levy.lednotifier.ui.activity;

import android.annotation.TargetApi;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.ciubotariu_levy.lednotifier.preferences.Keys;
import com.ciubotariu_levy.lednotifier.preferences.Prefs;
import com.ciubotariu_levy.lednotifier.ui.fragment.ColorDialog;

public class DefaultColorChooserContainer extends FragmentActivity implements ColorDialog.OnColorChosenListener {
    private static final String COLOR_CHOOSER_DIALOG_TAG = "color_chooser";
    private Prefs mPrefs;

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPrefs = Prefs.getInstance(this);
        if (getSupportFragmentManager().findFragmentByTag(COLOR_CHOOSER_DIALOG_TAG) == null) {
            ColorDialog.getInstance(null, mPrefs.getInt(Keys.DEFAULT_NOTIFICATION_COLOR, Color.GRAY))
                    .show(getSupportFragmentManager(), COLOR_CHOOSER_DIALOG_TAG);
        }
    }

    @Override
    public void onColorChosen(int color, String lookupKey) {
        mPrefs.putInt(Keys.DEFAULT_NOTIFICATION_COLOR, color);
    }
}
