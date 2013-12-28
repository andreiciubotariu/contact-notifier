package com.ciubotariu_levy.lednotifier;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;

public class DefaultColorChooserContainer extends ActionBarActivity implements ColorDialog.OnColorChosenListener{
	private final static String COLOR_CHOOSER_DIALOG_TAG = "color_chooser";
	protected final static String DEFAULT_COLOR = "default_notification_color";
	private SharedPreferences mPrefs;
	@Override
	protected void onCreate (Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (getSupportActionBar() != null){
			getSupportActionBar().hide();
		}
		if (getSupportFragmentManager().findFragmentByTag(COLOR_CHOOSER_DIALOG_TAG) == null){
			ColorDialog.getInstance(null, mPrefs.getInt(DEFAULT_COLOR, Color.GRAY)).show(getSupportFragmentManager(), COLOR_CHOOSER_DIALOG_TAG);
		}
	}
	@Override
	public void onColorChosen(int color, String lookupKey) {
		System.out.println (color);
		mPrefs.edit().putInt(DEFAULT_COLOR, color).apply();
	}
}
