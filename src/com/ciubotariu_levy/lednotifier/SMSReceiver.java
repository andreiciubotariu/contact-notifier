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

import com.ciubotariu_levy.lednotifier.providers.LedContactInfo;
import com.ciubotariu_levy.lednotifier.providers.LedContacts;

public class SMSReceiver extends BroadcastReceiver {
	public static final int ACTIVITY_REQUEST_CODE = 0;
	public static final int DEL_REQUEST_CODE = 2;
	protected static final String SHOW_ALL_NOTIFS = "show_all_notifications";

	@Override
	public void onReceive(Context context, Intent intent) {
		Bundle bundle = intent.getExtras();
		if (bundle != null){
			Object[] pdus = (Object[]) bundle.get("pdus");
			SmsMessage sms = SmsMessage.createFromPdu((byte[])pdus[0]);
			onNewMessage (context, sms.getOriginatingAddress(), sms.getDisplayMessageBody());
		}
	}

	@TargetApi(19)
	public void  onNewMessage (Context context, String number, String message){
		if (!TextUtils.isEmpty(number)){
			String [] sender = getNameForNumber(number, context.getContentResolver());
			//System.out.println (Arrays.toString(sender));

			Intent i=new Intent(context, MainActivity.class);

			i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			String [] projection = new String [] {LedContacts.COLOR,LedContacts.SYSTEM_CONTACT_LOOKUP_URI, LedContacts.VIBRATE_PATTERN};
			String selection = null;
			String [] selectionArgs = null;
			selection = LedContacts.SYSTEM_CONTACT_LOOKUP_URI + " = ?" ;
			if (sender [0] != null){
				selectionArgs = new String [] {	sender [0] };
			}
			Cursor c = context.getContentResolver().query(LedContacts.CONTENT_URI, projection, selection, selectionArgs,null);
			int color = Color.GRAY;
			String vibratePattern = null;
			if (c != null && c.moveToFirst()){
				try {
					color = c.getInt(c.getColumnIndex(LedContacts.COLOR));
					vibratePattern = c.getString(c.getColumnIndex(LedContacts.VIBRATE_PATTERN));
				}
				catch (Exception e){

					e.printStackTrace();
				}
			}
			if (c != null){
				c.close();
			}
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
			String smsAppPackageName = Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT ? 
					preferences.getString(SmsAppChooserDialog.KEY_SMS_APP_PACKAGE, this.getClass().getPackage().getName())
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

					if (preferences.getBoolean("status_bar_preview", false)){
						notifBuilder.setTicker(sender[1]+": " + message);
					}
					else {
						notifBuilder.setTicker("New message");
					}
					if (preferences.getBoolean("notif_and_sound", false)){
						notifBuilder.setSound(Uri.parse(preferences.getString("notifications_new_message_ringtone", Settings.System.DEFAULT_NOTIFICATION_URI.toString())));
					}

					NotificationUtils.title = sender[1];
					NotificationUtils.message = message;
					NotificationUtils.contentIntent = pendingIntent;

					Intent delIntent = new Intent (context, AlarmDismissReceiver.class);
					PendingIntent deletePendIntent = PendingIntent.getBroadcast(context, DEL_REQUEST_CODE, delIntent, PendingIntent.FLAG_UPDATE_CURRENT);
					notifBuilder.setDeleteIntent(deletePendIntent);

					//			boolean showAllNotifs = preferences.getBoolean("notifications_new_message_vibrate", false);
					//			if (showAllNotifs){
					//				//use vibrator only if no color needed
					//			}
					//			if (!TextUtils.isEmpty(vibratePattern)){
					//				long [] pattern = LedContactInfo.getVibratePattern(vibratePattern);
					//				if (showAllNotifs || color != Color.GRAY){
					//				  notifBuilder.setVibrate(pattern);
					//				}
					//				else {
					//					Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
					//					v.vibrate(pattern, -1 /*no repeat*/);
					//				}
					//			}
					//			Notification notif = notifBuilder.build();
					//			if (TextUtils.isEmpty(vibratePattern) && showAllNotifs){
					//				notif.defaults|=Notification.DEFAULT_VIBRATE;
					//			}
					if (!TextUtils.isEmpty(vibratePattern)){
						notifBuilder.setVibrate(LedContactInfo.getVibratePattern(vibratePattern));
					}
					Notification notif = notifBuilder.build();
					if (TextUtils.isEmpty(vibratePattern) && preferences.getBoolean("notifications_new_message_vibrate", false)){
						notif.defaults|=Notification.DEFAULT_VIBRATE;
					}
					//System.out.println ("Vibrate: " + Arrays.toString(notif.vibrate));
					onNotificationGenerated(context, notif);
		}
	}

	public void onNotificationGenerated (Context context, Notification notif){
		boolean isServiceOn = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 ?
				NotificationService.isNotificationListenerServiceOn : false;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		boolean showAllNotifications = prefs.getBoolean(SHOW_ALL_NOTIFS, false);
		boolean inFullSilentMode = ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE)).getRingerMode() == AudioManager.RINGER_MODE_SILENT;
		
		if (showAllNotifications && notif.ledARGB == Color.GRAY){
			notif.ledARGB = prefs.getInt(DefaultColorChooserContainer.DEFAULT_COLOR, Color.GRAY);
		}
		if (notif.ledARGB == Color.GRAY && notif.vibrate != null && !inFullSilentMode){
			Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
			v.vibrate(notif.vibrate, -1 /*no repeat*/);
			notif.vibrate = null;
		}
		if (!isServiceOn && (showAllNotifications || notif.ledARGB != Color.GRAY)){
			boolean timeoutLED = prefs.getBoolean("led_timeout", false);
			NotificationUtils.notify (context, notif,timeoutLED);
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
//				Uri incompleteUri = Uri.withAppendedPath(Contacts.CONTENT_LOOKUP_URI,contactCursor.getString (contactCursor.getColumnIndex(Contacts.LOOKUP_KEY)));
				Uri contactUri = Contacts.getLookupUri(contactCursor.getLong(contactCursor.getColumnIndex(PhoneLookup._ID)), contactCursor.getString (contactCursor.getColumnIndex(Contacts.LOOKUP_KEY)));
				String contactUriString = contactUri == null ? null : contactUri.toString();
				System.out.println ("Relookup of shallow contact uri: " + Contacts.getLookupUri(resolver, Uri.withAppendedPath(Contacts.CONTENT_LOOKUP_URI, contactCursor.getString (contactCursor.getColumnIndex(Contacts.LOOKUP_KEY)))));
				return new String [] {contactUriString,
						contactCursor.getString (contactCursor.getColumnIndex(PhoneLookup.DISPLAY_NAME))};
			}
			else {
				return new String [] {null,number};
			}

		}
		finally {
			if (contactCursor != null){
				contactCursor.close();
			}
		}
	}

}
