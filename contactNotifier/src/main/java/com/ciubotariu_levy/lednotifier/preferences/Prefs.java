package com.ciubotariu_levy.lednotifier.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

public class Prefs {
    private static final String TAG = Prefs.class.getName();

    private static Prefs sInstance;
    private Resources mResources;
    private SharedPreferences mSharedPrefs;

    private Prefs(Resources resources, SharedPreferences sharedPrefs) {
        mResources = resources;
        mSharedPrefs = sharedPrefs;
    }

    public static synchronized Prefs getInstance(Context context) {
        Context applicationContext = context.getApplicationContext();
        if (sInstance == null) {
            sInstance = new Prefs(applicationContext.getResources(), PreferenceManager.getDefaultSharedPreferences(applicationContext));
        }
        return sInstance;
    }

    private String getStringFromResource(Keys preferenceKey) {
        return mResources.getString(preferenceKey.getResId());
    }

    public String getString(Keys key, String defaultValue) {
        return mSharedPrefs.getString(getStringFromResource(key), defaultValue);
    }

    public int getInt(Keys key, int defaultValue) {
        return mSharedPrefs.getInt(getStringFromResource(key), defaultValue);
    }

    public boolean getBoolean(Keys key, boolean defaultValue) {
        return mSharedPrefs.getBoolean(getStringFromResource(key), defaultValue);
    }

    public void putBoolean(Keys key, boolean value) {
        mSharedPrefs.edit().putBoolean(getStringFromResource(key), value).apply();
    }

    public void putInt(Keys key, int value) {
        mSharedPrefs.edit().putInt(getStringFromResource(key), value).apply();
    }

    public void putString(Keys key, String value) {
        mSharedPrefs.edit().putString(getStringFromResource(key), value).apply();
    }
}
