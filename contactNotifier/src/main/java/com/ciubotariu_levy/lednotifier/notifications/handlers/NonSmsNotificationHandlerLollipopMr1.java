package com.ciubotariu_levy.lednotifier.notifications.handlers;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Binder;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.ciubotariu_levy.lednotifier.messages.MessageInfo;
import com.ciubotariu_levy.lednotifier.messages.MessageUtils;
import com.ciubotariu_levy.lednotifier.notifications.Generator;

import java.util.ArrayList;

@TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
public class NonSmsNotificationHandlerLollipopMr1 implements NotificationHandler {
    private static final String TAG = NonSmsNotificationHandlerLollipopMr1.class.getName();
    @Override
    public void onNotificationServiceCreated(Context context) {

    }

    @Override
    public void onNotificationServiceStopped(Context context) {

    }

    @Override
    public void onStatusBarNotificationPosted(Context context, StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        Log.i(TAG, "onStatusBarNotificationPosted: " + sbn);
        if (notification.extras == null) {
            Log.i(TAG, "onStatusBarNotificationPosted: no notification extras");
            return;
        }
        if (!notification.extras.containsKey(Notification.EXTRA_PEOPLE)) {
            Log.i(TAG, "onStatusBarNotificationPosted: notification does not contain PEOPLE extra");
            return;
        }
        String[] people =  notification.extras.getStringArray(Notification.EXTRA_PEOPLE);
        if (people == null) {
            Log.w(TAG, "onStatusBarNotificationPosted: PEOPLE extra null!");
        }

        for (String personLookupUri : people) {
            final long token = Binder.clearCallingIdentity(); // http://stackoverflow.com/a/20645908/1815485 Seems the NotificationListenerService is within a different process?
            MessageInfo info;
            try {
                info = MessageUtils.getMessageInfoFromNotification(context.getApplicationContext(), personLookupUri);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
            if (info != null && info.isCustom()) {
                Notification generated = Generator.generateNotification(context.getApplicationContext(), info.getColor(), info.getRingtoneUriString(),new long[0], false, new ArrayList<String>(), new ArrayList<String>(), null);
                ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(23, generated);
            }
        }
    }

    @Override
    public void onStatusBarNotificationRemoved(Context context, StatusBarNotification sbn) {

    }
}
