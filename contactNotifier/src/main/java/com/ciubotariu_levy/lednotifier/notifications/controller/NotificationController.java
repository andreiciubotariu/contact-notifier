package com.ciubotariu_levy.lednotifier.notifications.controller;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.ciubotariu_levy.lednotifier.constants.RequestCodes;
import com.ciubotariu_levy.lednotifier.messages.MessageHistory;
import com.ciubotariu_levy.lednotifier.notifications.NotificationUtil;
import com.ciubotariu_levy.lednotifier.preferences.Keys;
import com.ciubotariu_levy.lednotifier.preferences.Prefs;
import com.ciubotariu_levy.lednotifier.receivers.LedCancelReceiver;

import java.util.concurrent.TimeUnit;

public class NotificationController {
    private static final String TAG = NotificationController.class.getName();
    private static final int NOTIFICATION_ID = 1;
    private static final long DELAY_MILLIS = TimeUnit.SECONDS.toMillis(5);
    private static final long LED_CANCEL_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(10);

    private static NotificationController sInstance;
    private Context mApplicationContext;
    private Notification mPostedNotification;
    private Handler mDelayDismissNotificationHandler;
    private final Runnable mDelayDismissNotificationRunnable = new Runnable() {
        @Override
        public void run() {
            onDismissalDelayReached();
        }
    };

    NotificationController(Context applicationContext, Handler delayDismissNotificationHandler) {
        mApplicationContext = applicationContext;
        mDelayDismissNotificationHandler = delayDismissNotificationHandler;
    }

    static NotificationController createInstance(int sdkVersion, Context applicationContext, Handler delayDismissNotificationHandler) {
        if (sdkVersion < Build.VERSION_CODES.LOLLIPOP_MR1) {
            return new NotificationController(applicationContext, delayDismissNotificationHandler);
        }
        return new NotificationControllerLollipopMr1(applicationContext, delayDismissNotificationHandler);
    }

    public static synchronized NotificationController getInstance(Context context) {
        if (sInstance == null) {
            sInstance = createInstance(Build.VERSION.SDK_INT, context.getApplicationContext(), new Handler(Looper.getMainLooper()));
        }
        return sInstance;
    }

    boolean hasLedTimeoutEnabled() {
        return Prefs.getInstance(mApplicationContext).getBoolean(Keys.LED_TIMEOUT, false);
    }

    void setFutureAlarm(PendingIntent pendingIntent, long alarmTimeMs) {
        AlarmManager a = (AlarmManager) mApplicationContext.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            a.set(AlarmManager.RTC_WAKEUP, alarmTimeMs, pendingIntent);
        } else {
            a.setExact(AlarmManager.RTC_WAKEUP, alarmTimeMs, pendingIntent);
        }
    }

    void createAndPostLedTimeout() {
        Intent i = new Intent(mApplicationContext, LedCancelReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mApplicationContext, RequestCodes.LED_CANCEL_RECEIVER.ordinal(), i, PendingIntent.FLAG_UPDATE_CURRENT);
        long postTimeMs = System.currentTimeMillis() + LED_CANCEL_TIMEOUT_MS;
        setFutureAlarm(pendingIntent, postTimeMs);
    }

    void clearRingtone(Notification postedNotification) {
        NotificationUtil.clearRingtone(postedNotification);
    }

    void clearVibrate(Notification postedNotification) {
        NotificationUtil.clearVibrate(postedNotification);
    }

    void clearLedFlagsForNotification(Notification postedNotification) {
        NotificationUtil.clearLedFlagsForNotification(postedNotification);
    }

    public void cancelLedTimeout() {
        if (mPostedNotification == null) {
            Log.e(TAG, "cancelLedTimeout: no posted notification");
            return;
        }
        clearRingtone(mPostedNotification);
        clearVibrate(mPostedNotification);
        clearLedFlagsForNotification(mPostedNotification);
        postToNotificationService(mPostedNotification);
    }

    void clearDelayDismissRunnable() {
        mDelayDismissNotificationHandler.removeCallbacks(mDelayDismissNotificationRunnable);
    }

    void postToNotificationService(Notification notification) {
        ((NotificationManager) mApplicationContext.getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, notification);
        mPostedNotification = notification;
        if (notification.ledARGB != Color.GRAY && hasLedTimeoutEnabled()) {
            createAndPostLedTimeout();
        }
    }

    public void postNotification(Notification notification) {
        clearDelayDismissRunnable();
        postToNotificationService(notification);
    }

    public void onSmsAppNotificationPosted() {
        // NOP
    }

    boolean isDelayDismissalEnabled() {
        return Prefs.getInstance(mApplicationContext).getBoolean(Keys.DELAY_DISMISSAL, false);
    }

    void onDismissalDelayReached() {
        dismissNotification();
    }

    void postDelayedDismissalRunnable() {
        mDelayDismissNotificationHandler.postDelayed(mDelayDismissNotificationRunnable, DELAY_MILLIS);
    }

    /**
     * Called by {@link com.ciubotariu_levy.lednotifier.notifications.NotificationService}
     */
    public void onSmsAppNotificationDismissed() {
        if (!isDelayDismissalEnabled()) {
            dismissNotification();
            return;
        }
        postDelayedDismissalRunnable();
    }

    public void dismissNotification() {
        ((NotificationManager) mApplicationContext.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(NOTIFICATION_ID);
        mPostedNotification = null;
        mDelayDismissNotificationHandler.removeCallbacks(mDelayDismissNotificationRunnable);
        dismissLedCancelAlarm();
        MessageHistory.getInstance().clear();
    }

    public void dismissLedCancelAlarm() {
        Intent i = new Intent(mApplicationContext, LedCancelReceiver.class);
        PendingIntent p = PendingIntent.getBroadcast(mApplicationContext, RequestCodes.LED_CANCEL_RECEIVER.ordinal(), i, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager a = (AlarmManager) mApplicationContext.getSystemService(Context.ALARM_SERVICE);
        a.cancel(p);
    }
}
