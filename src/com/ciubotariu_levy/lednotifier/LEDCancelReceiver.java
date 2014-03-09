package com.ciubotariu_levy.lednotifier;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

public class LEDCancelReceiver extends BroadcastReceiver{
	public static final String KEY_TITLE = "title";
	public static final String KEY_MSG = "msg";
	public static final String KEY_PEND_INTENT = "pending_intent";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String title = NotificationUtils.title;
		String message = NotificationUtils.message + " CHANGED";
		PendingIntent p = NotificationUtils.contentIntent;
		
		if (title == null || message == null || p == null){
			return;
		}
		
		Notification notif = new NotificationCompat.Builder(context).setContentTitle (title)
		.setContentText (message)
		.setContentIntent (p)
		.setSmallIcon(R.drawable.ic_stat_new_msg)
		.setAutoCancel(true)
		.build();
		
		NotificationUtils.notify(context, notif);
	}

}
