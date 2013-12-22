/**
 * 
 */
package com.ciubotariu_levy.lednotifier;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.Settings;
import android.provider.Telephony.Sms;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsMessage;
import android.text.TextUtils;

import com.ciubotariu_levy.lednotifier.providers.LedContacts;

public class SMSReceiver extends BroadcastReceiver {
	public static final int NOTIFICATION_ID = 1;
	
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

			String [] projection = new String [] {LedContacts.COLOR,LedContacts.SYSTEM_CONTACT_ID, LedContacts.VIBRATE_PATTERN};
			String selection = null;
			String [] selectionArgs = null;
			//if (sender [0] != null){
				selection = LedContacts.SYSTEM_CONTACT_ID + " = ?" ;
				if (sender [0] != null){
					selectionArgs = new String [] {	sender [0] };
				}
				/*else {
					selectionArgs = new String [] {	 };
				}*/
			//}
			Cursor c = context.getContentResolver().query(LedContacts.CONTENT_URI, projection, selection, selectionArgs,null);
			int color = Color.GRAY;
			if (c != null && c.moveToFirst()){
				try {
					color = c.getInt(c.getColumnIndex(LedContacts.COLOR));
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
			PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,smsAppIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			Notification notif = new NotificationCompat.Builder(context)
			.setContentTitle (sender [1])
			.setContentText (message + " (Notification LED color should be " + color + ")")
			.setContentIntent (pendingIntent)
			.setSmallIcon(R.drawable.ic_launcher) //replace later
			.setLights(color, 1000, 1000) //should flash
			.setAutoCancel(true)
			.setSound(Uri.parse(preferences.getString("notifications_new_message_ringtone", Settings.System.DEFAULT_NOTIFICATION_URI.toString())))
			.build();

			onNotificationGenerated(context, notif);

		}
	}

	public void onNotificationGenerated (Context context, Notification notif){
		boolean isServiceOn = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 ?
				NotificationService.isNotificationListenerServiceOn : false;
		if (!isServiceOn && notif.ledARGB != Color.GRAY){
			notify (context, notif);
		}
	}
	
	public static void notify (Context context, Notification notif){
		((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, notif);
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
				return new String [] {contactCursor.getString (contactCursor.getColumnIndex(Contacts.LOOKUP_KEY)),
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
