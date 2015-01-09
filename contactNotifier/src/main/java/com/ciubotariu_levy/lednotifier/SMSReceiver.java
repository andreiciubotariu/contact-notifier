/**
 *
 */
package com.ciubotariu_levy.lednotifier;

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

import com.ciubotariu_levy.lednotifier.providers.LedContactInfo;
import com.ciubotariu_levy.lednotifier.providers.LedContacts;
import com.google.android.mms.ContentType;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.PduParser;
import com.makeramen.RoundedTransformationBuilder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SMSReceiver extends BroadcastReceiver {
    public static final String TAG = SMSReceiver.class.getName();
    public static final int ACTIVITY_REQUEST_CODE = 0;
    public static final int DEL_REQUEST_CODE = 2;
    public static final String KEY_TIMEOUT_LED = "led_timeout";
    protected static final String SHOW_ALL_NOTIFS = "show_all_notifications";
    private static final String DEF_VIBRATE = "def_vibrate";

    private static class MessageInfo {
        String name, address, contactUri, ringtoneUri, vibPattern, text;
        int color = Color.GRAY;

        boolean isCustom() {
            return contactUri != null && (ringtoneUri != null || vibPattern != null || color != Color.GRAY);
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static SmsMessage[] getSMSMessagesFromIntent(Intent intent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            Object[] messages = (Object[]) intent.getSerializableExtra("pdus");
            int pduCount = messages.length;
            SmsMessage[] msgs = new SmsMessage[pduCount];
            for (int i = 0; i < pduCount; i++) {
                byte[] pdu = (byte[]) messages[i];
                msgs[i] = SmsMessage.createFromPdu(pdu);
            }
            return msgs;
        }

        return Sms.Intents.getMessagesFromIntent(intent);
    }

    private void getNameAndUri(MessageInfo info, ContentResolver resolver){
        String number = info.address;
        Cursor contactCursor = null;
        try{
            Uri phoneNumberUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
            contactCursor = resolver.query(phoneNumberUri, new String [] {Contacts.LOOKUP_KEY,PhoneLookup._ID,PhoneLookup.DISPLAY_NAME}, null, null, null);
            if (contactCursor != null && contactCursor.moveToFirst()){
                Uri contactUri = Contacts.getLookupUri(contactCursor.getLong(contactCursor.getColumnIndex(PhoneLookup._ID)), contactCursor.getString (contactCursor.getColumnIndex(Contacts.LOOKUP_KEY)));

                String contactUriString = contactUri == null ? null : contactUri.toString();

                info.contactUri = contactUriString;
                info.name = contactCursor.getString (contactCursor.getColumnIndex(PhoneLookup.DISPLAY_NAME));
            }
            else {
                Log.i(TAG,"Not a contact in phone's database");
                info.name = null;
                info.contactUri = null;
            }
        }
        finally {
            if (contactCursor != null){
                contactCursor.close();
            }
        }
    }

    private MessageInfo getInfoForMessage (SmsMessage message, Context context) {
        if (message == null || message.getOriginatingAddress() == null) {
            return null;
        }

        return getInfoForMessage(message.getOriginatingAddress(),message.getDisplayMessageBody(), context);
    }

    private MessageInfo getInfoForMessage(String address, String text, Context context) {

        MessageInfo info = new MessageInfo();
        info.address = address;
        info.text = text;

        getNameAndUri(info, context.getContentResolver());

        String[] projection = new String [] {LedContacts.COLOR,LedContacts.SYSTEM_CONTACT_LOOKUP_URI, LedContacts.VIBRATE_PATTERN, LedContacts.RINGTONE_URI};
        String selection = null;
        String [] selectionArgs = null;
        selection = LedContacts.SYSTEM_CONTACT_LOOKUP_URI + " = ?" ;
        if (info.contactUri != null){
            selectionArgs = new String [] {	info.contactUri };

            Cursor c = context.getContentResolver().query(LedContacts.CONTENT_URI, projection, selection, selectionArgs,null);

            if (c != null && c.moveToFirst()){
                try {
                    int customColor = c.getInt(c.getColumnIndex(LedContacts.COLOR));

                    if (customColor != Color.GRAY){
                        info.color = customColor;
                    }

                    String customRingtone = c.getString(c.getColumnIndex(LedContacts.RINGTONE_URI));
                    if (!ColorVibrateDialog.GLOBAL.equals(customRingtone)){
                        info.ringtoneUri = customRingtone;
                    }
                    String customVib = c.getString(c.getColumnIndex(LedContacts.VIBRATE_PATTERN));

                    if (!TextUtils.isEmpty(customVib)){
                        info.vibPattern = customVib;
                    }

                } catch (Exception e){

                    e.printStackTrace();
                }
            }
            if (c != null){
                c.close();
            }
        }
        return info;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return;
        }

        HashMap<String, MessageInfo> infoMap = new HashMap<String, MessageInfo>();
        List<String> customMessages = new ArrayList<String>();

        if (intent.getAction().equals(Sms.Intents.WAP_PUSH_RECEIVED_ACTION)
                && ContentType.MMS_MESSAGE.equals(intent.getType())) {


            Log.v(TAG, "Received PUSH Intent: " + intent);

            // Get raw PDU push-data from the message and parse it
            byte[] pushData = intent.getByteArrayExtra("data");
            PduParser parser = new PduParser(pushData);
            GenericPdu pdu = parser.parse();
            if (null == pdu) {
                Log.e(TAG, "Invalid PUSH data");
                return;
            }
            int type = pdu.getMessageType();
            long threadId = -1;
            Log.v(TAG, "Sender: " + pdu.getFrom().getString());
            Log.v(TAG, "Sender: " + pdu.getFrom().toString());
            Log.v(TAG, "Sender: " + pdu.getFrom().getTextString().toString());
            Log.v(TAG,"TYPE: " + pdu.getMessageType());

            String address = pdu.getFrom().getString();
            MessageInfo i = getInfoForMessage(address,"New MMS", context);
            infoMap.put(address, i);
            if (i.isCustom()) {
                customMessages.add(address);
            }
            //TODO: determine which type corresponds to normal message MMS
//            try {
//                switch (type) {
//                    case MESSAGE_TYPE_DELIVERY_IND:
//                    case MESSAGE_TYPE_READ_ORIG_IND:
//
//                    //handle message
//                    Log.v(TAG, "Received message");
//                    break;
//
//
//                    default:
//                        Log.e(TAG, "Received but not processing PDU.");
//                }
//            } catch (RuntimeException e) {
//                Log.e(TAG, "Unexpected RuntimeException.", e);
//            }

            Log.v(TAG, "PUSH Intent processed.");

        }
        else if  (intent.getAction().equals(Sms.Intents.SMS_RECEIVED_ACTION)) {


            SmsMessage[] sms = getSMSMessagesFromIntent(intent);


            for (int x = 0; x < sms.length; x++) {
                String address = sms[x].getOriginatingAddress();
                if (address != null) {
                    if (infoMap.get(address) == null) {
                        MessageInfo i = getInfoForMessage(sms[x], context);
                        infoMap.put(address, i);
                        if (i.isCustom()) {
                            customMessages.add(address);
                        }
                    } else {
                        String moreText = sms[x].getDisplayMessageBody();
                        if (infoMap.get(address).text != null) {
                            infoMap.get(address).text += moreText;
                        } else {
                            infoMap.get(address).text = moreText;
                        }
                    }
                }
            }
        }
        onMessagesReceived(context, infoMap, customMessages);
    }


    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void onMessagesReceived (Context context, HashMap<String,MessageInfo> infoMap, List<String> customMessages){
        if (infoMap.isEmpty()) {
            Log.i("INFO-MAP" , "Empty");
            return;
        }
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean showNotification = preferences.getBoolean(SHOW_ALL_NOTIFS, false);

        int color = preferences.getInt(DefaultColorChooserContainer.DEFAULT_COLOR, Color.GRAY);
        String vibratePattern = preferences.getBoolean("notifications_new_message_vibrate", false) ? DEF_VIBRATE : null;
        String ringtone = "";

        if (preferences.getBoolean("notif_and_sound", false)){
            ringtone = preferences.getString("notifications_new_message_ringtone", Settings.System.DEFAULT_NOTIFICATION_URI.toString());
        }


        boolean ringtoneSet = false, vibSet = false, colorSet = false;
        for (int x = 0; x < customMessages.size(); x++) {
            MessageInfo info = infoMap.get(customMessages.get(x));

            if (!ringtoneSet && !TextUtils.isEmpty(info.ringtoneUri)) {
                ringtone = info.ringtoneUri;
                ringtoneSet = true;
            }

            if (!vibSet && !TextUtils.isEmpty(info.vibPattern)) {
                vibratePattern = info.vibPattern;
                vibSet = true;
            }

            if (!colorSet && info.color != Color.GRAY) {
                color = info.color;
                colorSet = true;
            }
        }

        if (colorSet || ringtoneSet) {
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
            MessageInfo[] messages = infoMap.values().toArray(new MessageInfo[0]); //0 size is temp

            MessageInfo firstMessage = messages[0];
            String title = messages.length > 1 ? "Multiple senders" : firstMessage.name;
            Log.i("FIRST MESSAGE", firstMessage.name + " " + firstMessage.address);
            if (TextUtils.isEmpty(title)) {
                title = !TextUtils.isEmpty(firstMessage.address) ? firstMessage.address : "Unknown Sender";
            }


            String body =  firstMessage.text;
            if (body == null) {
                body = "";
            }

            NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(context)
                    .setContentTitle (title)
                    .setContentText (body)
                    .setContentIntent (pendingIntent)
                    .setSmallIcon(R.drawable.ic_stat_new_msg)
                    .setLights(color, 1000, 1000) //flash
                    .setAutoCancel(true);

            if (firstMessage.contactUri != null) {
                Log.i("TAG", firstMessage.contactUri);
                Bitmap b = loadContactPhotoThumbnail(context, firstMessage.contactUri);
                if (b != null) {
                    Bitmap large = b;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        large = new RoundedTransformationBuilder().oval(true).build().transform(b);
                    }
                    notifBuilder.setLargeIcon(large);
                    Log.i("TAG","set large icon");
                } else {
                    Log.e("TAG", " b is null");
                }
                notifBuilder.addPerson(firstMessage.contactUri);
            }
            if (preferences.getBoolean("status_bar_preview", false)){
                notifBuilder.setTicker(title +": " + body);
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
            NotificationUtils.message = body;
            NotificationUtils.contentIntent = pendingIntent;

            Intent delIntent = new Intent (context, AlarmDismissReceiver.class);
            PendingIntent deletePendIntent = PendingIntent.getBroadcast(context, DEL_REQUEST_CODE, delIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            notifBuilder.setDeleteIntent(deletePendIntent);

            Notification notif = notifBuilder.build();
            if (DEF_VIBRATE.equals(vibratePattern)){
                notif.defaults|=Notification.DEFAULT_VIBRATE;
            }
            onNotificationReady(context, notif,preferences.getBoolean(KEY_TIMEOUT_LED, false));
        } else {
            boolean inFullSilentMode = ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE)).getRingerMode() == AudioManager.RINGER_MODE_SILENT;
            if (!inFullSilentMode && !TextUtils.isEmpty(vibratePattern) && !vibratePattern.equals(DEF_VIBRATE)){
                Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(LedContactInfo.getVibratePattern(vibratePattern), -1 /*no repeat*/);
            }
            onNotificationReady(null, null, false);
        }
    }

    public void onNotificationReady (Context context, Notification notif, boolean ledTimeout){
        boolean isServiceOn = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 ?
                NotificationService.isNotificationListenerServiceOn : false;
        if (!isServiceOn && context != null && notif != null){
            NotificationUtils.notify (context, notif,ledTimeout);
        }
    }

    private Bitmap loadContactPhotoThumbnail(Context context, String contactUri) {

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