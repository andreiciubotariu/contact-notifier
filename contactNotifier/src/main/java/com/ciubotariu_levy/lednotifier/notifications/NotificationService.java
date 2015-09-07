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

import com.ciubotariu_levy.lednotifier.notifications.controller.NotificationController;
import com.ciubotariu_levy.lednotifier.preferences.Keys;
import com.ciubotariu_levy.lednotifier.preferences.Prefs;
import com.ciubotariu_levy.lednotifier.messages.MessageReceiver;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationService extends NotificationListenerService {
    private static final String TAG = NotificationService.class.getName();
    private static final String SMS_RECEIVED_ACTION = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ?
            Telephony.Sms.Intents.SMS_RECEIVED_ACTION : "android.provider.Telephony.SMS_RECEIVED";

    private MessageReceiver mSmsReceiver = new MessageReceiver() {
        @Override
        public void postNotification(Context context, Notification notif) {
            if (notif != null && context != null) {
                NotificationController.getInstance(context).postNotification(notif);
            }
        }
    };

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filter = new IntentFilter(SMS_RECEIVED_ACTION);
        filter.setPriority(999);
        //registerReceiver(mSmsReceiver, filter);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onDestroy() {
       // unregisterReceiver(mSmsReceiver);
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.v(TAG, "onNotificationPosted: " + sbn);
        if (isMessagingApp(sbn.getPackageName())) {
            NotificationController.getInstance(this).onSmsAppNotificationPosted();
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (isMessagingApp(sbn.getPackageName())) {
            NotificationController.getInstance(this).onSmsAppNotificationDismissed();
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private boolean isMessagingApp(String packageName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return packageName.equals(Telephony.Sms.getDefaultSmsPackage(this));
        }
        return !packageName.equals(getPackageName())
                && (packageName.equals(Prefs.getInstance(this).getString(Keys.SMS_APP_PACKAGE, null))
                || packageName.contains("mms")
                || packageName.contains("sms")
                || packageName.contains("messaging")
                || packageName.contains("message")
                || packageName.contains("talk"));
    }
}
