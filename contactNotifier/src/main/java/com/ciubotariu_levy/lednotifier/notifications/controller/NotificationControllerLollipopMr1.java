package com.ciubotariu_levy.lednotifier.notifications.controller;

import android.app.Notification;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.ciubotariu_levy.lednotifier.preferences.Keys;
import com.ciubotariu_levy.lednotifier.preferences.Prefs;

import java.util.concurrent.TimeUnit;

/**
 * Lollipop_mr1+ specific changes to notification handling
 * Prior to Android 5.1, the LED was locked by the first notification to use it.
 * On newer versions, the most recent incoming notification will have guaranteed LED priority
 */
public class NotificationControllerLollipopMr1 extends NotificationController {
    private static final String TAG = NotificationControllerLollipopMr1.class.getName();
    private static final long SMS_APP_WAIT_TIMEOUT = TimeUnit.SECONDS.toMillis(30);
    static final int SMS_APP_TIMEOUT_MAX_HITS = 3;

    // Package access for tests
    Notification mPendingMessageNotification;
    int mNumSmsAppWaitTimeoutsHit;

    private Handler mSmsAppWaitTimeoutHandler;
    private Runnable mSmsAppWaitTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            onSmsAppWaitTimeoutReached();
        }
    };
    private Context mContext;

    NotificationControllerLollipopMr1(Context applicationContext, Handler delayedRunnableHandler) {
        super(applicationContext, delayedRunnableHandler);
        mContext = applicationContext;
        mNumSmsAppWaitTimeoutsHit = 0;
        mSmsAppWaitTimeoutHandler = delayedRunnableHandler;
    }

    boolean shouldWaitForSmsApp() {
        return Prefs.getInstance(mContext).getBoolean(Keys.WAIT_FOR_SMS_APP, true);
    }

    @Override
    public void postNotification(Notification notification) {
        if (shouldWaitForSmsApp()) {
            mPendingMessageNotification = notification;
            startSmsAppWaitTimeout();
            return;
        }
        showNotification(notification);
    }

    void updateWaitForSmsAppPreference(boolean waitForSmsApp) {
        Prefs.getInstance(mContext).putBoolean(Keys.WAIT_FOR_SMS_APP, waitForSmsApp);
    }

    void onSmsAppWaitTimeoutReached() {
        clearDelayDismissRunnable();
        if (mPendingMessageNotification == null) {
            Log.e(TAG, "onSmsAppWaitTimeoutReached: pending message notification is null");
            return;
        }
        showNotification(mPendingMessageNotification);
        mNumSmsAppWaitTimeoutsHit++;
        if (mNumSmsAppWaitTimeoutsHit >= SMS_APP_TIMEOUT_MAX_HITS) {
            Log.i(TAG, "onSmsAppWaitTimeoutReached: not waiting for sms app to post its notification");
            updateWaitForSmsAppPreference(false /* do not wait for sms app to post its notification */);
        }
    }

    void startSmsAppWaitTimeout() {
        mSmsAppWaitTimeoutHandler.removeCallbacks(mSmsAppWaitTimeoutRunnable);
        mSmsAppWaitTimeoutHandler.postDelayed(mSmsAppWaitTimeoutRunnable, SMS_APP_WAIT_TIMEOUT);
    }

    private void showNotification(Notification notification) {
        mSmsAppWaitTimeoutHandler.removeCallbacks(mSmsAppWaitTimeoutRunnable);
        clearDelayDismissRunnable();
        postToNotificationService(notification);
        mPendingMessageNotification = null;
    }

    @Override
    public void onSmsAppNotificationPosted() {
        mNumSmsAppWaitTimeoutsHit = 0; // sms notification posted, reset counters and update preference
        updateWaitForSmsAppPreference(true /* sms app is posting notifications, we should wait */);
        if (mPendingMessageNotification == null) {
            Log.w(TAG, "onSmsAppNotificationPosted: no pendingMessageNotification available");
            return;
        }
        showNotification(mPendingMessageNotification);
    }

    @Override
    public void dismissNotification() {
        mPendingMessageNotification = null;
        super.dismissNotification();
    }

    @Override
    public void onSmsAppNotificationDismissed() {
        mPendingMessageNotification = null;
        super.onSmsAppNotificationDismissed();
    }
}
