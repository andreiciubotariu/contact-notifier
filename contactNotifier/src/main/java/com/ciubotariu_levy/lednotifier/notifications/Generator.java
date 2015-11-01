package com.ciubotariu_levy.lednotifier.notifications;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.Telephony;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.ciubotariu_levy.lednotifier.R;
import com.ciubotariu_levy.lednotifier.constants.RequestCodes;
import com.ciubotariu_levy.lednotifier.preferences.Keys;
import com.ciubotariu_levy.lednotifier.preferences.Prefs;
import com.ciubotariu_levy.lednotifier.receivers.NotificationDismissReceiver;

import java.util.ArrayList;

public class Generator {

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static Notification generateNotification(Context context, int ledColor, String ringtoneUriString, long[] vibrate, boolean defaultVibrate,
                                                    ArrayList<String> headings, ArrayList<String> messages, Bitmap icon) {

        Intent smsAppIntent = context.getPackageManager().getLaunchIntentForPackage(Generator.class.getPackage().getName());

        String smsAppPackageName = Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT ? Prefs.getInstance(context).getString(Keys.SMS_APP_PACKAGE, Generator.class.getPackage().getName())
                : Telephony.Sms.getDefaultSmsPackage(context);

        Intent betterSmsAppCandidateIntent = context.getPackageManager().getLaunchIntentForPackage(smsAppPackageName);
        if (betterSmsAppCandidateIntent != null) {
            smsAppIntent = betterSmsAppCandidateIntent;
        }

        if (smsAppIntent == null) { // fallback: launch our app
            smsAppIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(context, RequestCodes.SMS_APP.ordinal(), smsAppIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(context);
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

//        StringBuilder notifBody = new StringBuilder();
//        for (int x = 0; x < headings.size(); x++) {
//            notifBody.append(headings.get(x)).append(": ").append(messages.get(x));
//            inboxStyle.addLine(headings.get(x) + ": " + messages.get(x));
//        }
//
//        String title = "";
//        if (headings.size() == 1) {
//            notifBody = new StringBuilder(messages.get(0));
//            title = headings.get(0);
//        } else {
//            title = "Multiple Senders";
//            inboxStyle.setBigContentTitle(title);
//            inboxStyle.setSummaryText();
//            notifBuilder.setStyle(inboxStyle);
//        }

        notifBuilder.setContentTitle("Custom Contacts")
                .setContentText("Custom contacts")
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_stat_new_msg)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setAutoCancel(true);

        if (icon != null) {
            notifBuilder.setLargeIcon(icon);
        }
        if (ledColor != Color.GRAY) {
            notifBuilder.setLights(ledColor, 1000, 1000); // flash
        }

//        Prefs preferences = Prefs.getInstance(context);
//        if (preferences.getBoolean(Keys.STATUS_BAR_PREVIEW, false)) {
//            notifBuilder.setTicker(title + ": " + notifBuilder.toString());
//        } else {
        notifBuilder.setTicker("Custom contact");
//        }
        if (!TextUtils.isEmpty(ringtoneUriString)) {
            notifBuilder.setSound(Uri.parse(ringtoneUriString));
        }

        if (vibrate != null) {
            notifBuilder.setVibrate(vibrate);
        }

        Intent delIntent = new Intent(context, NotificationDismissReceiver.class);
        PendingIntent deletePendIntent = PendingIntent.getBroadcast(context, RequestCodes.NOTIFICATION_DISMISSED.ordinal(), delIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notifBuilder.setDeleteIntent(deletePendIntent);

        Notification notif = notifBuilder.build();
        if (defaultVibrate) {
            notif.defaults |= Notification.DEFAULT_VIBRATE;
        }
        return notif;
    }
}
