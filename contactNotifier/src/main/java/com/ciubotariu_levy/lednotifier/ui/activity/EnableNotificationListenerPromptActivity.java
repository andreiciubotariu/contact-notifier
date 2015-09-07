package com.ciubotariu_levy.lednotifier.ui.activity;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.ciubotariu_levy.lednotifier.R;

public class EnableNotificationListenerPromptActivity extends AppCompatActivity {
    private static final String TAG = EnableNotificationListenerPromptActivity.class.getName();

    /**
     * Notification listener settings screen action(http://stackoverflow.com/q/18212209)
     */
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 ? Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS : "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
    private static final String ENABLED_NOTIFICATION_LISTENERS_KEY = "enabled_notification_listeners";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enable_notification_listener_prompt);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isNotificationServiceEnabled(this)) {
            Intent mainActivityIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (mainActivityIntent != null) {
                try {
                    startActivity(mainActivityIntent);
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "onResume: could not launch main activity");
                }
            }
            finish();
        }
    }
    /**
     * Returns whether or not the notification service is enabled. (http://stackoverflow.com/a/21392852)
     */
    public static boolean isNotificationServiceEnabled(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return true;
        }
        ContentResolver contentResolver = context.getContentResolver();
        String enabledNotificationListeners = Settings.Secure.getString(contentResolver, ENABLED_NOTIFICATION_LISTENERS_KEY);
        String packageName = context.getPackageName();

        return enabledNotificationListeners != null && enabledNotificationListeners.contains(packageName);
    }

    public static void launchNotificationListenerPreference(Context context) {
        Intent i = new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS);
        try {
            context.startActivity(i);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "launchNotificationListenerPreference: could not launch notification listener setting", e);
            Toast.makeText(context, "Could not launch notification listener setting", Toast.LENGTH_SHORT).show();
        }
    }
    public void openNotificationListenerPreference(View view) {
        launchNotificationListenerPreference(this);
    }
}
