package com.ciubotariu_levy.lednotifier.notifications.handlers;

import android.content.Context;
import android.service.notification.StatusBarNotification;

public interface NotificationHandler { // cleanup method for switching handlers?

    void onNotificationServiceCreated(Context context);
    void onNotificationServiceStopped(Context context);
    void onStatusBarNotificationPosted(Context context, StatusBarNotification sbn);
    void onStatusBarNotificationRemoved(Context context, StatusBarNotification sbn);
}
