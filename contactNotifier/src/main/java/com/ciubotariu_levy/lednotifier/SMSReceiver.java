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
import android.content.res.AssetFileDescriptor;
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

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

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
        if (bundle != null){
            Object[] pdus = (Object[]) bundle.get("pdus");
            SmsMessage sms = SmsMessage.createFromPdu((byte[])pdus[0]);
            onMessageReceived (context, sms.getOriginatingAddress(), sms.getDisplayMessageBody());
        }
    }


    @TargetApi(19)
    public void onMessageReceived (Context context, String number, String message){
        if (TextUtils.isEmpty(number) || message == null){
            return;
        }

        String [] sender = getNameForNumber(number, context.getContentResolver());

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean showNotification = preferences.getBoolean(SHOW_ALL_NOTIFS, false);

        int color = preferences.getInt(DefaultColorChooserContainer.DEFAULT_COLOR, Color.GRAY);
        String vibratePattern = preferences.getBoolean("notifications_new_message_vibrate", false) ? DEF_VIBRATE : null;
        String ringtone = "";

        if (preferences.getBoolean("notif_and_sound", false)){
            ringtone = preferences.getString("notifications_new_message_ringtone", Settings.System.DEFAULT_NOTIFICATION_URI.toString());
        }

        String [] projection = new String [] {LedContacts.COLOR,LedContacts.SYSTEM_CONTACT_LOOKUP_URI, LedContacts.VIBRATE_PATTERN, LedContacts.RINGTONE_URI};
        String selection = null;
        String [] selectionArgs = null;
        selection = LedContacts.SYSTEM_CONTACT_LOOKUP_URI + " = ?" ;
        if (sender [0] != null){
            selectionArgs = new String [] {	sender [0] };

            Cursor c = context.getContentResolver().query(LedContacts.CONTENT_URI, projection, selection, selectionArgs,null);

            if (c != null && c.moveToFirst()){
                try {
                    int customColor = c.getInt(c.getColumnIndex(LedContacts.COLOR));

                    if (customColor != Color.GRAY){
                        color = customColor;
                        showNotification = true;
                    }

                    String customRingtone = c.getString(c.getColumnIndex(LedContacts.RINGTONE_URI));
                    if (!ColorVibrateDialog.GLOBAL.equals(customRingtone)){
                        showNotification = true;
                        ringtone = customRingtone;
                    }
                    String customVib = c.getString(c.getColumnIndex(LedContacts.VIBRATE_PATTERN));

                    if (!TextUtils.isEmpty(customVib)){
                        vibratePattern = customVib;
                    }

                } catch (Exception e){

                    e.printStackTrace();
                }
            }
            if (c != null){
                c.close();
            }
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
            NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(context)
                    .setContentTitle (sender [1])
                    .setContentText (message)
                    .setContentIntent (pendingIntent)
                    .setSmallIcon(R.drawable.ic_stat_new_msg)
                    .setLights(color, 1000, 1000) //flash
                    .setAutoCancel(true);

            if (sender [0] != null) {
                Bitmap b = loadContactPhotoThumbnail(context, sender[0]);
                if (b != null) {
                    notifBuilder.setLargeIcon(b);
                    Log.i("TAG","set large icon");
                } else {
                    Log.e("TAG", " b is null");
                }
                //notifBuilder.setLargeIcon(BitmapFactory.decodeStream(Contacts.openContactPhotoInputStream(context.getContentResolver(),Uri.parse(sender[0]),false)));
                notifBuilder.addPerson(sender[0]);
            }
            if (preferences.getBoolean("status_bar_preview", false)){
                notifBuilder.setTicker(sender[1]+": " + message);
            } else {
                notifBuilder.setTicker("New message");
            }
            if (ringtone != null && ringtone.length() > 0){
                notifBuilder.setSound(Uri.parse(ringtone));
            }
            if (!TextUtils.isEmpty(vibratePattern) && !vibratePattern.equals(DEF_VIBRATE)){
                notifBuilder.setVibrate(LedContactInfo.getVibratePattern(vibratePattern));
            }
            NotificationUtils.title = sender[1];
            NotificationUtils.message = message;
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

    /**
     *
     * @param number
     * @param resolver
     * @return [Contact's lookup key or null, Display name or number]
     */
    private String [] getNameForNumber (String number, ContentResolver resolver){
        Cursor contactCursor = null;
        try{
            Uri phoneNumberUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
            contactCursor = resolver.query(phoneNumberUri, new String [] {Contacts.LOOKUP_KEY,PhoneLookup._ID,PhoneLookup.DISPLAY_NAME}, null, null, null);
            if (contactCursor != null && contactCursor.moveToFirst()){
                Uri contactUri = Contacts.getLookupUri(contactCursor.getLong(contactCursor.getColumnIndex(PhoneLookup._ID)), contactCursor.getString (contactCursor.getColumnIndex(Contacts.LOOKUP_KEY)));

                String contactUriString = contactUri == null ? null : contactUri.toString();
                return new String [] {contactUriString,
                        contactCursor.getString (contactCursor.getColumnIndex(PhoneLookup.DISPLAY_NAME))};
            }
            else {
                Log.i(TAG,"Not a contact in phone's database");
                return new String [] {null,number};
            }
        }
        finally {
            if (contactCursor != null){
                contactCursor.close();
            }
        }
    }

    /**
     * Load a contact photo thumbnail and return it as a Bitmap,
     * resizing the image to the provided image dimensions as needed.
     * @param photoData photo ID Prior to Honeycomb, the contact's _ID value.
     * For Honeycomb and later, the value of PHOTO_THUMBNAIL_URI.
     * @return A thumbnail Bitmap, sized to the provided width and height.
     * Returns null if the thumbnail is not found.
     */
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
