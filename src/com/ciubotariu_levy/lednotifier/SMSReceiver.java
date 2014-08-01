/**
 * 
 */
package com.ciubotariu_levy.lednotifier;

import java.util.Arrays;

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
import android.util.Log;

import com.ciubotariu_levy.lednotifier.providers.LedContactInfo;
import com.ciubotariu_levy.lednotifier.providers.LedContacts;

public class SMSReceiver extends BroadcastReceiver {
	public static final String TAG = SMSReceiver.class.getName();
	public static final int ACTIVITY_REQUEST_CODE = 0;
	public static final int DEL_REQUEST_CODE = 2;
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
		if (TextUtils.isEmpty(number)){
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

		String [] projection = new String [] {LedContacts.COLOR,LedContacts.SYSTEM_CONTACT_LOOKUP_URI, LedContacts.HAS_CUSTOM_VIBRATE,LedContacts.VIBRATE_PATTERN,
				LedContacts.HAS_CUSTOM_RINGTONE, LedContacts.RINGTONE_URI};
		String selection = null;
		String [] selectionArgs = null;
		selection = LedContacts.SYSTEM_CONTACT_LOOKUP_URI + " = ?" ;
		if (sender [0] != null){
			selectionArgs = new String [] {	sender [0] };

			Cursor c = context.getContentResolver().query(LedContacts.CONTENT_URI, projection, selection, selectionArgs,null);

			if (c != null && c.moveToFirst()){
				Log.v(TAG,"Cursor non-null");
				try {
					int customColor = c.getInt(c.getColumnIndex(LedContacts.COLOR));

					if (customColor != Color.GRAY){
						color = customColor;
						showNotification = true;
					}

					boolean hasRingtone =  c.getInt(c.getColumnIndex(LedContacts.HAS_CUSTOM_VIBRATE)) != GlobalConstants.FALSE;
					String customRing = "";

//					if (hasRingtone){
//						customRing = c.getString(c.getColumnIndex(LedContacts.RINGTONE_URI));
//						if (customRing.equalsIgnoreCase("null")){
//							customRing = null;
//						}
//
//						if (customRing == null || customRing.trim().length() > 0){
//							ringtone = customRing;
//							showNotification = true;
//						}
//					}

					boolean hasVibrate = c.getInt(c.getColumnIndex(LedContacts.HAS_CUSTOM_VIBRATE)) != GlobalConstants.FALSE;
					String customVib = null;
					if (hasVibrate){
						customVib = c.getString(c.getColumnIndex(LedContacts.VIBRATE_PATTERN));
					} else {
						Log.i("VIB-PATTERN", "no custom");
					}

					if (!TextUtils.isEmpty(customVib)){
						vibratePattern = customVib;
						Log.i("VIB-PATTERN", ""+vibratePattern);
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
			onNotificationReady(context, notif,preferences.getBoolean("led_timeout", false));
		} else {
			boolean inFullSilentMode = ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE)).getRingerMode() == AudioManager.RINGER_MODE_SILENT;
			if (!inFullSilentMode && !TextUtils.isEmpty(vibratePattern) && !vibratePattern.equals(DEF_VIBRATE)){
				Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
				v.vibrate(LedContactInfo.getVibratePattern(vibratePattern), -1 /*no repeat*/);
			}
			onNotificationReady(null, null,false);
		}
	}
	
//	@TargetApi(19)
//	public void  onNewMessage (Context context, String number, String message){
//		if (!TextUtils.isEmpty(number)){
//			String [] sender = getNameForNumber(number, context.getContentResolver());
//			System.out.println ("Sender: " + Arrays.toString(sender));
//
//			int color = Color.GRAY;
//			String vibratePattern = null;
//
//			Intent i=new Intent(context, MainActivity.class);
//
//			i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//			i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//
//			String [] projection = new String [] {LedContacts.COLOR,LedContacts.SYSTEM_CONTACT_LOOKUP_URI, LedContacts.VIBRATE_PATTERN};
//			String selection = null;
//			String [] selectionArgs = null;
//			selection = LedContacts.SYSTEM_CONTACT_LOOKUP_URI + " = ?" ;
//			if (sender [0] != null){
//				selectionArgs = new String [] {	sender [0] };
//
//				Cursor c = context.getContentResolver().query(LedContacts.CONTENT_URI, projection, selection, selectionArgs,null);
//
//				if (c != null && c.moveToFirst()){
//					Log.v(TAG,"Cursor non-null");
//					try {
//						color = c.getInt(c.getColumnIndex(LedContacts.COLOR));
//						vibratePattern = c.getString(c.getColumnIndex(LedContacts.VIBRATE_PATTERN));
//					}
//					catch (Exception e){
//
//						e.printStackTrace();
//					}
//				}
//				if (c != null){
//					c.close();
//				}
//			}
//			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
//			String smsAppPackageName = Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT ? 
//					preferences.getString(SmsAppChooserDialog.KEY_SMS_APP_PACKAGE, this.getClass().getPackage().getName())
//					: Sms.getDefaultSmsPackage(context);
//					Intent smsAppIntent = context.getPackageManager().getLaunchIntentForPackage(smsAppPackageName);
//					if (smsAppIntent == null){
//						smsAppIntent = context.getPackageManager().getLaunchIntentForPackage(this.getClass().getPackage().getName());
//					}
//					PendingIntent pendingIntent = PendingIntent.getActivity(context,ACTIVITY_REQUEST_CODE,smsAppIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//					NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(context)
//					.setContentTitle (sender [1])
//					.setContentText (message)
//					.setContentIntent (pendingIntent)
//					.setSmallIcon(R.drawable.ic_stat_new_msg)
//					.setLights(color, 1000, 1000) //flash
//					.setAutoCancel(true);
//
//					if (preferences.getBoolean("status_bar_preview", false)){
//						notifBuilder.setTicker(sender[1]+": " + message);
//					}
//					else {
//						notifBuilder.setTicker("New message");
//					}
//					if (preferences.getBoolean("notif_and_sound", false)){
//						notifBuilder.setSound(Uri.parse(preferences.getString("notifications_new_message_ringtone", Settings.System.DEFAULT_NOTIFICATION_URI.toString())));
//					}
//
//					NotificationUtils.title = sender[1];
//					NotificationUtils.message = message;
//					NotificationUtils.contentIntent = pendingIntent;
//
//					Intent delIntent = new Intent (context, AlarmDismissReceiver.class);
//					PendingIntent deletePendIntent = PendingIntent.getBroadcast(context, DEL_REQUEST_CODE, delIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//					notifBuilder.setDeleteIntent(deletePendIntent);
//					if (!TextUtils.isEmpty(vibratePattern)){
//						notifBuilder.setVibrate(LedContactInfo.getVibratePattern(vibratePattern));
//					}
//					Notification notif = notifBuilder.build();
//					if (TextUtils.isEmpty(vibratePattern) && preferences.getBoolean("notifications_new_message_vibrate", false)){
//						notif.defaults|=Notification.DEFAULT_VIBRATE;
//					}
//					//System.out.println ("Vibrate: " + Arrays.toString(notif.vibrate));
//					onNotificationGenerated(context, notif);
//		}
//	}
//
	
	
	public void onNotificationReady (Context context, Notification notif, boolean ledTimeout){
		boolean isServiceOn = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 ?
				NotificationService.isNotificationListenerServiceOn : false;
		if (!isServiceOn && context != null && notif != null){
			NotificationUtils.notify (context, notif,ledTimeout);
		}
	}
	
	
//	public void onNotificationGenerated (Context context, Notification notif){
//		boolean isServiceOn = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 ?
//				NotificationService.isNotificationListenerServiceOn : false;
//		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
//		boolean showAllNotifications = prefs.getBoolean(SHOW_ALL_NOTIFS, false);
//		boolean inFullSilentMode = ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE)).getRingerMode() == AudioManager.RINGER_MODE_SILENT;
//
//		if (showAllNotifications && notif.ledARGB == Color.GRAY){
//			notif.ledARGB = prefs.getInt(DefaultColorChooserContainer.DEFAULT_COLOR, Color.GRAY);
//		}
//		if (notif.ledARGB == Color.GRAY && notif.vibrate != null && !inFullSilentMode){
//			Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
//			v.vibrate(notif.vibrate, -1 /*no repeat*/);
//			notif.vibrate = null;
//		}
//		if (!isServiceOn && (showAllNotifications || notif.ledARGB != Color.GRAY)){
//			boolean timeoutLED = prefs.getBoolean("led_timeout", false);
//			NotificationUtils.notify (context, notif,timeoutLED);
//		}
//	}

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
				Log.w(TAG,"Not a contact in phone's database");
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
