/**
 * 
 */
package com.ciubotariu_levy.lednotifier;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.PhoneLookup;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsMessage;
import android.text.TextUtils;

public class SMSReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Bundle bundle = intent.getExtras();
		if (bundle != null){
			Object[] pdus = (Object[]) bundle.get("pdus");
			SmsMessage sms = SmsMessage.createFromPdu((byte[])pdus[0]);
			onNewMessage (context, sms.getOriginatingAddress(), sms.getDisplayMessageBody());
		}
	}

	public void  onNewMessage (Context context, String number, String message){
		if (!TextUtils.isEmpty(number)){
			String displayName = getNameForNumber(number, context.getContentResolver());
			
			
			Intent i=new Intent(context, MainActivity.class);

			i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,i, PendingIntent.FLAG_UPDATE_CURRENT);
			Notification notif = new NotificationCompat.Builder(context)
			.setContentTitle (displayName)
			.setContentText ("Sent an SMS")
			.setContentIntent (pendingIntent)
			.setSmallIcon(R.drawable.ic_launcher) //replace later
			.setLights(0xFFFF0000, 500, 500) //should flash
			.build();
			
			((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(1, notif);
		}
	}

	private String getNameForNumber (String number, ContentResolver resolver){
		Cursor contactCursor = null;
		try{
			Uri phoneNumberUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
			contactCursor = resolver.query(phoneNumberUri, new String [] {PhoneLookup.DISPLAY_NAME}, null, null, null);
			if (contactCursor != null && contactCursor.moveToFirst()){
				return contactCursor.getString (0);
			}
			else {
				return number;
			}

		}
		finally {
			if (contactCursor != null){
				contactCursor.close();
			}
		}
	}

}
