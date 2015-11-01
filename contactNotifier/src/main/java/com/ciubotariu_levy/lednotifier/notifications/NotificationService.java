package com.ciubotariu_levy.lednotifier.notifications;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.provider.Telephony;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.ciubotariu_levy.lednotifier.BuildConfig;
import com.ciubotariu_levy.lednotifier.notifications.controller.NotificationController;
import com.ciubotariu_levy.lednotifier.notifications.handlers.NotificationHandler;
import com.ciubotariu_levy.lednotifier.notifications.handlers.NotificationHandlerFactory;
import com.ciubotariu_levy.lednotifier.preferences.Keys;
import com.ciubotariu_levy.lednotifier.preferences.Prefs;
import com.ciubotariu_levy.lednotifier.messages.MessageReceiver;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationService extends NotificationListenerService {
    private static final String TAG = NotificationService.class.getName();

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHandlerFactory.getNotificationHandler(this).onNotificationServiceCreated(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onDestroy() {
        NotificationHandlerFactory.getNotificationHandler(this).onNotificationServiceStopped(this);
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.v(TAG, "onNotificationPosted: " + sbn);
        NotificationHandlerFactory.getNotificationHandler(this).onStatusBarNotificationPosted(this, sbn);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        NotificationHandlerFactory.getNotificationHandler(this).onStatusBarNotificationRemoved(this, sbn);
    }
}
