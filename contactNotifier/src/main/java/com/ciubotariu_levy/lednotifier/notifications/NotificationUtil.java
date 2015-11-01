package com.ciubotariu_levy.lednotifier.notifications;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.provider.Telephony;

import com.ciubotariu_levy.lednotifier.BuildConfig;
import com.ciubotariu_levy.lednotifier.preferences.Keys;
import com.ciubotariu_levy.lednotifier.preferences.Prefs;

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

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static boolean isMessagingApp(Context context, String packageName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return packageName.equals(Telephony.Sms.getDefaultSmsPackage(context));
        }
        return !packageName.equals(BuildConfig.APPLICATION_ID)
                && (packageName.equals(Prefs.getInstance(context).getString(Keys.SMS_APP_PACKAGE, null))
                || packageName.contains("mms")
                || packageName.contains("sms")
                || packageName.contains("messaging")
                || packageName.contains("message")
                || packageName.contains("talk"));
    }
}
