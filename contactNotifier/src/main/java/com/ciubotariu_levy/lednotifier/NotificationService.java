package com.ciubotariu_levy.lednotifier;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.ciubotariu_levy.lednotifier.messages.SMSReceiver;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationService extends NotificationListenerService {
    private static final String TAG = NotificationService.class.getName();
    private static final String KEY_TIE_NOTIFICATION = "tie_to_sms_app";
    private static final String KEY_DELAY_DISMISS = "delay_dismissal";
    private static final int DELAY_MILLIS = 5000;

    public static boolean isNotificationListenerServiceOn = false;
    private boolean mTieNotification = false;
    private boolean mDelayDismissal = false;

    private Handler mHandler = new Handler();
    private Runnable mDismissNotification = new Runnable() {
        @Override
        public void run() {
            NotificationUtils.cancel(NotificationService.this);
        }
    };

    private SharedPreferences.OnSharedPreferenceChangeListener prefListener = new OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(
                SharedPreferences sharedPreferences, String key) {
            if (KEY_TIE_NOTIFICATION.equals(key)) {
                mTieNotification = sharedPreferences.getBoolean(KEY_TIE_NOTIFICATION, false);
            } else if (KEY_DELAY_DISMISS.equals(key)) {
                mDelayDismissal = sharedPreferences.getBoolean(KEY_DELAY_DISMISS, false);
                mHandler.removeCallbacks(mDismissNotification);
            }
        }
    };

    private SMSReceiver mSmsReceiver = new SMSReceiver() {

        @Override
        public void onNotificationReady(Context context, Notification notif, boolean ledTimeout) {
            if (notif != null && context != null) {
                NotificationUtils.notify(context, notif, ledTimeout);
            }
        }
    };

    @TargetApi(19)
    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPrefs.registerOnSharedPreferenceChangeListener(prefListener);
        mTieNotification = sharedPrefs.getBoolean(KEY_TIE_NOTIFICATION, false);
        mDelayDismissal = sharedPrefs.getBoolean(KEY_DELAY_DISMISS, false);
        String filterAction = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ?
                Telephony.Sms.Intents.SMS_RECEIVED_ACTION :
                "android.provider.Telephony.SMS_RECEIVED";
        IntentFilter filter = new IntentFilter(filterAction);
        filter.setPriority(999);
        registerReceiver(mSmsReceiver, filter);
    }

    @Override
    public IBinder onBind(Intent intent) {
        isNotificationListenerServiceOn = true;
        return super.onBind(intent);
    }

    @Override
    public void onDestroy() {
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(prefListener);
        unregisterReceiver(mSmsReceiver);
        isNotificationListenerServiceOn = false;
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.v(TAG, "onNotificationPosted: " + sbn);
        if (isMessagingApp(sbn.getPackageName())) {
            mHandler.removeCallbacks(mDismissNotification);
        }
        if (isMessagingApp(sbn.getPackageName()) && NotificationUtils.sPostNotificationRunnable != null) {
            NotificationUtils.sPostNotificationRunnable.run();
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (mTieNotification && isMessagingApp(sbn.getPackageName())) {
            if (mDelayDismissal) {
                mHandler.postAtTime(mDismissNotification, SystemClock.uptimeMillis() + DELAY_MILLIS);
            } else {
                mDismissNotification.run();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private boolean isMessagingApp(String packageName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return packageName.equals(Telephony.Sms.getDefaultSmsPackage(this));
        }
        return !packageName.equals(getPackageName())
                && (packageName.equals(PreferenceManager.getDefaultSharedPreferences(this).getString(SmsAppChooserDialog.KEY_SMS_APP_PACKAGE, null))
                || packageName.contains("mms")
                || packageName.contains("sms")
                || packageName.contains("messaging")
                || packageName.contains("message")
                || packageName.contains("talk"));
    }
}
