package com.ciubotariu_levy.lednotifier.notifications;

import android.app.Notification;
import android.graphics.Color;

/**
 * Collection of static utility methods acting on Notiifications
 */
public class NotificationUtil {

    public static void clearRingtone(Notification notification) {
        notification.sound = null;
    }

    public static void clearVibrate(Notification notification) {
        notification.vibrate = null;
    }

    public static void clearLedFlagsForNotification(Notification notification) {
        notification.ledARGB = Color.GRAY;
        notification.ledOnMS = 0;
        notification.ledOffMS = 0;
        notification.flags &= ~Notification.FLAG_SHOW_LIGHTS;
    }
}
