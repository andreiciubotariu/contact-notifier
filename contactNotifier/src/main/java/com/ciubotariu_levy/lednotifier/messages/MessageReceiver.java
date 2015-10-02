package com.ciubotariu_levy.lednotifier.messages;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.ciubotariu_levy.lednotifier.constants.RequestCodes;
import com.ciubotariu_levy.lednotifier.preferences.Keys;
import com.ciubotariu_levy.lednotifier.preferences.Prefs;
import com.ciubotariu_levy.lednotifier.receivers.NotificationDismissReceiver;
import com.ciubotariu_levy.lednotifier.notifications.controller.NotificationController;
import com.ciubotariu_levy.lednotifier.R;
import com.ciubotariu_levy.lednotifier.providers.LedContactInfo;
import com.googlesource.android.mms.ContentType;
import com.makeramen.RoundedTransformationBuilder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;

/**
 * Handles SMS and MMS receive intents
 */
public class MessageReceiver extends BroadcastReceiver {
    private static final String TAG = MessageReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return;
        }

        LinkedHashMap<String, MessageInfo> receivedMessages = null;

        if (intent.getAction().equals(Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION)
                && ContentType.MMS_MESSAGE.equals(intent.getType())) {
            Log.v(TAG, "onReceive: received PUSH Intent: " + intent);
            receivedMessages = MessageUtils.createMessageInfosFromPushIntent(intent, context);
        } else if (intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
            Log.v(TAG, "onReceive: received SMS: " + intent);
            receivedMessages = MessageUtils.createMessageInfosFromSmsIntent(intent, context);
        }

        if (receivedMessages == null || receivedMessages.isEmpty()) {
            Log.w(TAG, "onReceive: no messages extracted");
            return;
        }

        processNewMessages(receivedMessages);
        Prefs prefs = Prefs.getInstance(context);
        boolean showAllNotifs = prefs.getBoolean(Keys.SHOW_ALL_NOTIFICATIONS, false);
        int defaultColor = prefs.getInt(Keys.DEFAULT_NOTIFICATION_COLOR, Color.GRAY);
        String defaultRingtoneUriString = null;

        if (prefs.getBoolean(Keys.NOTIFICATION_AND_SOUND, false)) {
            defaultRingtoneUriString = prefs.getString(Keys.NOTIFICATIONS_NEW_MESSAGE_RINGTONE, null);
        }

        boolean vibeForAllContacts = prefs.getBoolean(Keys.NOTIFICATIONS_NEW_MESSAGE_VIBRATE, false);
        Notification notification = generateNotificationIfNeeded(context, MessageHistory.getInstance(), showAllNotifs, defaultColor, defaultRingtoneUriString, vibeForAllContacts);
        postNotification(context, notification);
    }

    private void processNewMessages(LinkedHashMap<String, MessageInfo> newMessages) {
        MessageHistory.getInstance().addMessages(newMessages);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private Notification generateNotificationIfNeeded(Context context, MessageHistory history, boolean showAllNotifications, int defColor, String defRingtone, boolean vibeForAllContacts) {
        if (history.getMessages().isEmpty() || (!history.containsCustomMessages() && !showAllNotifications)) {
            Log.i(TAG, "generateNotificationIfNeeded: no need to create notification");
            return null;
        }

        Intent smsAppIntent = context.getPackageManager().getLaunchIntentForPackage(this.getClass().getPackage().getName());

        String smsAppPackageName = Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT ? Prefs.getInstance(context).getString(Keys.SMS_APP_PACKAGE, this.getClass().getPackage().getName())
                : Telephony.Sms.getDefaultSmsPackage(context);

        Intent betterSmsAppCandidateIntent = context.getPackageManager().getLaunchIntentForPackage(smsAppPackageName);
        if (betterSmsAppCandidateIntent != null) {
            smsAppIntent = betterSmsAppCandidateIntent;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(context, RequestCodes.SMS_APP.ordinal(), smsAppIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Bitmap contactPhoto = null;

        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(context);
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

        int counter = 0;
        StringBuilder notifBody = new StringBuilder();
        StringBuilder notifSummaryText = new StringBuilder();
        MessageInfo firstMessage = null;
        for (MessageInfo message : history.getMessages().values()) {
            if (message.isCustom() || (showAllNotifications && message.getNameOrAddress() != null)) {
                if (counter == 0) {
                    firstMessage = message;
                }
                counter++;

                if (message.getContactUriString() != null) {
                    notifBuilder.addPerson(message.getContactUriString());
                }
                notifBody.append(message.getNameOrAddress()).append(": ").append(message.getContentString()).append(", ");
                inboxStyle.addLine(message.getNameOrAddress() + ": " + message.getContentString());

                if (contactPhoto == null) {
                    Bitmap b = loadContactPhotoThumbnail(context, message.getContactUriString());
                    if (b != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            b = new RoundedTransformationBuilder().oval(true).build().transform(b);
                        }
                        contactPhoto = b;
                    }
                }
            }
        }

        String title = "";
        if (counter == 1) {
            notifBody = new StringBuilder(firstMessage.getContentString());
            title = firstMessage.getNameOrAddress();
        } else {
            title = "Multiple Senders";
            inboxStyle.setBigContentTitle(title);
            inboxStyle.setSummaryText(notifSummaryText.toString());
            notifBuilder.setStyle(inboxStyle);
        }

        notifBuilder.setContentTitle(title)
                .setContentText(notifBody.toString())
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_stat_new_msg)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setAutoCancel(true);

        if (contactPhoto != null) {
            notifBuilder.setLargeIcon(contactPhoto);
        }
        int customColor = history.getCustomColor() != Color.GRAY ? history.getCustomColor() : defColor;
        if (customColor != Color.GRAY) {
            notifBuilder.setLights(customColor, 1000, 1000); // flash
        }

        Prefs preferences = Prefs.getInstance(context);
        if (preferences.getBoolean(Keys.STATUS_BAR_PREVIEW, false)) {
            notifBuilder.setTicker(title + ": " + notifBuilder.toString());
        } else {
            notifBuilder.setTicker("New message");
        }
        String ringtone = history.getCustomRingtone() != null ? history.getCustomRingtone() : defRingtone;
        if (!TextUtils.isEmpty(ringtone)) {
            notifBuilder.setSound(Uri.parse(ringtone));
        }

        if (!TextUtils.isEmpty(history.getCustomVibPattern())) {
            notifBuilder.setVibrate(LedContactInfo.getVibratePattern(history.getCustomVibPattern()));
        }

        Intent delIntent = new Intent(context, NotificationDismissReceiver.class);
        PendingIntent deletePendIntent = PendingIntent.getBroadcast(context, RequestCodes.NOTIFICATION_DISMISSED.ordinal(), delIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notifBuilder.setDeleteIntent(deletePendIntent);

        Notification notif = notifBuilder.build();
        if (vibeForAllContacts) {
            notif.defaults |= Notification.DEFAULT_VIBRATE;
        }
        return notif;
    }

    public void postNotification(Context context, Notification notification) { // TODO when notif service enabled, do not use this. Make notif service mandatory too
        if (notification == null) {
            Log.i(TAG, "postNotification: null Notification");
            return;
        }
        NotificationController.getInstance(context).postNotification(notification);
    }

    private Bitmap loadContactPhotoThumbnail(Context context, String contactUri) {
        if (contactUri == null) {
            return null;
        }
        Cursor mCursor = context.getContentResolver().query((Uri.parse(contactUri)), new String[]{ContactsContract.Contacts._ID,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? ContactsContract.Contacts.PHOTO_THUMBNAIL_URI : ContactsContract.Contacts._ID}, null, null, null);

        if (mCursor == null || !mCursor.moveToFirst()) {
            Log.e(TAG, "loadContactPhotoThumbnail: cursor error | " + mCursor);
            return null;
        }

        int mThumbnailColumn;
        int mIdColumn = mCursor.getColumnIndex(ContactsContract.Contacts._ID);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mThumbnailColumn = mCursor.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI);
        } else {
            mThumbnailColumn = mIdColumn;
        }

        String photoData = mCursor.getString(mThumbnailColumn);
        if (photoData == null) {
            return null;
        }
        Log.v(TAG, "loadContactPhotoThumbnail: photoData is " + photoData);
        InputStream is = null;
        try {
            try {
                Uri thumbUri;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    thumbUri = Uri.parse(photoData);
                } else {
                    final Uri contactPhotoUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, photoData);
                    thumbUri = Uri.withAppendedPath(contactPhotoUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
                }

                is = context.getContentResolver().openInputStream(thumbUri);
                if (is != null) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    int height = (int) context.getResources().getDimension(android.R.dimen.notification_large_icon_height);
                    int width = (int) context.getResources().getDimension(android.R.dimen.notification_large_icon_width);
                    Bitmap bm = BitmapFactory.decodeStream(is);
                    return Bitmap.createScaledBitmap(bm, width, height, false);
                }
            } catch (FileNotFoundException e) {
                Log.e(TAG, "loadContactPhotoThumbnail: could not find file", e);
            }
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.e(TAG, "loadContactPhotoThumbnail: could not close input stream", e);
                }
            }
            if (mCursor != null) {
                mCursor.close();
            }
        }
        return null;
    }
}
