package com.ciubotariu_levy.lednotifier.notifications.controller;

import android.app.Notification;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

/**
 * Lollipop_mr1+ specific changes to notification handling
 * Prior to Android 5.1, the LED was locked by the first notification to use it.
 * On newer versions, the most recent incoming notification will have guaranteed LED priority
 */
public class NotificationControllerLollipopMr1 extends NotificationController {
    private static final String TAG = NotificationControllerLollipopMr1.class.getName();

    // Package access for tests
    Notification mPendingMessageNotification;

    NotificationControllerLollipopMr1(Context applicationContext, Handler delayDismissNotificationHandler) {
        super(applicationContext, delayDismissNotificationHandler);
    }

    @Override
    public void postNotification(Notification notification) {
        mPendingMessageNotification = notification;
    }

    @Override
    public void onSmsAppNotificationPosted() {
        if (mPendingMessageNotification == null) {
            Log.w(TAG, "onSmsAppNotificationPosted: no pendingMessageNotification available");
            return;
        }

        clearDelayDismissRunnable();
        postToNotificationService(mPendingMessageNotification);
        mPendingMessageNotification = null;
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
