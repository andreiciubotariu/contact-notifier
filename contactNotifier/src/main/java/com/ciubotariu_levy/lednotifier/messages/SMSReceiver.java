/**
 *
 */
package com.ciubotariu_levy.lednotifier.messages;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.Settings;
import android.provider.Telephony.Sms;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;

import com.ciubotariu_levy.lednotifier.AlarmDismissReceiver;
import com.ciubotariu_levy.lednotifier.ColorVibrateDialog;
import com.ciubotariu_levy.lednotifier.DefaultColorChooserContainer;
import com.ciubotariu_levy.lednotifier.MainActivity;
import com.ciubotariu_levy.lednotifier.NotificationService;
import com.ciubotariu_levy.lednotifier.NotificationUtils;
import com.ciubotariu_levy.lednotifier.R;
import com.ciubotariu_levy.lednotifier.SmsAppChooserDialog;
import com.ciubotariu_levy.lednotifier.providers.LedContactInfo;
import com.ciubotariu_levy.lednotifier.providers.LedContacts;
import com.google.android.mms.ContentType;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.PduParser;
import com.makeramen.RoundedTransformationBuilder;

import org.w3c.dom.Text;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class SMSReceiver extends BroadcastReceiver {
    public static final String TAG = SMSReceiver.class.getName();
    public static final int ACTIVITY_REQUEST_CODE = 0;
    public static final int DEL_REQUEST_CODE = 2;
    public static final String KEY_TIMEOUT_LED = "led_timeout";
    protected static final String SHOW_ALL_NOTIFS = "show_all_notifications";
    private static final String DEF_VIBRATE = "def_vibrate";

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return;
        }

        LinkedHashMap<String, MessageInfo> infoMap = new LinkedHashMap<>();
        List<String> customMessages = new ArrayList<>();

        if (intent.getAction().equals(Sms.Intents.WAP_PUSH_RECEIVED_ACTION)
                && ContentType.MMS_MESSAGE.equals(intent.getType())) {


            Log.v(TAG, "Received PUSH Intent: " + intent);
            infoMap = MessageUtils.getPushMessages(intent, context);
            Log.v(TAG, "PUSH Intent processed.");

        }
        else if  (intent.getAction().equals(Sms.Intents.SMS_RECEIVED_ACTION)) {
            infoMap = MessageUtils.getMessages(intent, context);
        }
        onMessagesReceived(context, infoMap, customMessages);
    }


    @TargetApi(Build.VERSION_CODES.KITKAT) //needs changin
    public void onMessagesReceived (Context context, LinkedHashMap<String,MessageInfo> infoMap, List<String> customMessages){
        Log.e(TAG, "Messages received");
        if (infoMap.isEmpty()) {
            Log.i("INFO-MAP" , "Empty");
            return;
        }

        MessageHistory.add(infoMap,customMessages);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean showAllNotifs = preferences.getBoolean(SHOW_ALL_NOTIFS, false);
        boolean showNotification = showAllNotifs;

        int color = preferences.getInt(DefaultColorChooserContainer.DEFAULT_COLOR, Color.GRAY);
        String vibratePattern = preferences.getBoolean("notifications_new_message_vibrate", false) ? DEF_VIBRATE : null;
        String ringtone = "";

        if (preferences.getBoolean("notif_and_sound", false)){
            ringtone = preferences.getString("notifications_new_message_ringtone", Settings.System.DEFAULT_NOTIFICATION_URI.toString());
        }

        if (MessageHistory.customLEDMessage != null) {
            color = MessageHistory.customLEDMessage.color;
        }
        if (MessageHistory.customRingMessage != null) {
            ringtone = MessageHistory.customRingMessage.ringtoneUri;
        }
        if (MessageHistory.customVibMessage != null) {
            vibratePattern = MessageHistory.customVibMessage.vibPattern;
        }

        if (MessageHistory.customLEDMessage != null || MessageHistory.customRingMessage != null || MessageHistory.customVibMessage != null) {
            showNotification = true;
        }

        if (showNotification){
            Intent i=new Intent(context, MainActivity.class);

            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            String smsAppPackageName = Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT ?  preferences.getString(SmsAppChooserDialog.KEY_SMS_APP_PACKAGE, this.getClass().getPackage().getName())
                    : Sms.getDefaultSmsPackage(context);

            Intent smsAppIntent = context.getPackageManager().getLaunchIntentForPackage(smsAppPackageName);
            if (smsAppIntent == null){
                smsAppIntent = context.getPackageManager().getLaunchIntentForPackage(this.getClass().getPackage().getName());
            }
            PendingIntent pendingIntent = PendingIntent.getActivity(context,ACTIVITY_REQUEST_CODE,smsAppIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            boolean foundNotifPhoto = false;
            LinkedHashMap <String, MessageInfo> allMessages = MessageHistory.getMessages();
            NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(context);
            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

            int counter = 0;
            int numMessages = allMessages.size();
            StringBuilder body = new StringBuilder(), summaryText = new StringBuilder();
            MessageInfo firstMessage = null;
            Log.e(TAG,"Starting loop");
            for (MessageInfo message: allMessages.values()) {
                if (message.isCustom() || (showAllNotifs && message.address != null)) {
                    if (counter == 0) {
                        firstMessage = message;
                    }
                    if (message.contactUri != null) {
                        notifBuilder.addPerson(message.contactUri);
                    }

                    body.append (message.name()).append(": ").append(message.text).append(" ");
                    summaryText.append(message.name()).append(" "); //TODO formatting, comma maybe?
                    inboxStyle.addLine(message.name() + ": " + message.text());

                    if (!foundNotifPhoto) {
                        Bitmap b = loadContactPhotoThumbnail(context, message.contactUri);
                        if (b != null) {
                            Bitmap large = b;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                large = new RoundedTransformationBuilder().oval(true).build().transform(b);
                            }
                            notifBuilder.setLargeIcon(large);
                            foundNotifPhoto = true;
                        }
                    }
                    counter++;
                }
            }

            if (counter == 0) {
                Log.d(TAG,"No notifications created");
                return;
            }

            String title = "";
            if (counter == 1) {
                body = new StringBuilder(firstMessage.text());
                title = firstMessage.name();
            }
            else if (counter >= 1) {
                title = "Multiple Senders";
                inboxStyle.setBigContentTitle(title);
                inboxStyle.setSummaryText(summaryText.toString());
                notifBuilder.setStyle(inboxStyle);
            }

            notifBuilder.setContentTitle(title)
                    .setContentText (body.toString())
                    .setContentIntent (pendingIntent)
                    .setSmallIcon(R.drawable.ic_stat_new_msg)
                    .setLights(color, 1000, 1000) //flash
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE) //TODO option
                    .setAutoCancel(true);

            if (preferences.getBoolean("status_bar_preview", false)){
                notifBuilder.setTicker(title + ": " + body.toString());
            } else {
                notifBuilder.setTicker("New message");
            }
            if (ringtone != null && ringtone.length() > 0){
                notifBuilder.setSound(Uri.parse(ringtone));
            }
            if (!TextUtils.isEmpty(vibratePattern) && !vibratePattern.equals(DEF_VIBRATE)){
                notifBuilder.setVibrate(LedContactInfo.getVibratePattern(vibratePattern));
            }
            NotificationUtils.title = title;
            NotificationUtils.message = body.toString();
            NotificationUtils.contentIntent = pendingIntent;

            Intent delIntent = new Intent (context, AlarmDismissReceiver.class);
            PendingIntent deletePendIntent = PendingIntent.getBroadcast(context, DEL_REQUEST_CODE, delIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            notifBuilder.setDeleteIntent(deletePendIntent);

            Notification notif = notifBuilder.build();
            if (DEF_VIBRATE.equals(vibratePattern)){
                notif.defaults|=Notification.DEFAULT_VIBRATE;
            }
            onNotificationReady(context, notif,preferences.getBoolean(KEY_TIMEOUT_LED, false));
        }
    }

    public void onNotificationReady (Context context, Notification notif, boolean ledTimeout){
        boolean isServiceOn = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && NotificationService.isNotificationListenerServiceOn;
        if (!isServiceOn && context != null && notif != null){
            NotificationUtils.notify (context, notif,ledTimeout);
        }
    }

    private Bitmap loadContactPhotoThumbnail(Context context, String contactUri) {
        if (contactUri == null) {
            return null;
        }
        Cursor mCursor = context.getContentResolver().query((Uri.parse(contactUri)),new String[]{Contacts._ID,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? Contacts.PHOTO_THUMBNAIL_URI : Contacts._ID},null,null,null);

        if (mCursor == null || !mCursor.moveToFirst()) {
            Log.e("TAG","Cursor error");
            return null;
        }

        int mThumbnailColumn;

        int mIdColumn = mCursor.getColumnIndex(Contacts._ID);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mThumbnailColumn =  mCursor.getColumnIndex(Contacts.PHOTO_THUMBNAIL_URI);
        } else {
            mThumbnailColumn = mIdColumn;
        }

        Log.i("TAG","column: " + mThumbnailColumn);

        String photoData = mCursor.getString(mThumbnailColumn);
        if (photoData == null) {
            return null;
        }
        Log.i("TAG", "photodata: " + photoData);
        InputStream is = null;
        try{

            try {
                Uri thumbUri;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    thumbUri = Uri.parse(photoData);
                } else {
                    final Uri contactPhotoUri = Uri.withAppendedPath(Contacts.CONTENT_URI, photoData);
                    thumbUri = Uri.withAppendedPath(contactPhotoUri, Contacts.Photo.CONTENT_DIRECTORY);
                }

                is = context.getContentResolver().openInputStream(thumbUri);
                if (is != null) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    int height = (int) context.getResources().getDimension(android.R.dimen.notification_large_icon_height);
                    int width =  (int) context.getResources().getDimension(android.R.dimen.notification_large_icon_width);
                    Bitmap bm = BitmapFactory.decodeStream(is);
                    return Bitmap.createScaledBitmap(bm, width, height, false);
                }
            } catch (FileNotFoundException e) {
                Log.e("TAG", "filenotfound");
            }
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {}
            }
            if (mCursor != null) {
                mCursor.close();
            }
        }
        return null;
    }
}