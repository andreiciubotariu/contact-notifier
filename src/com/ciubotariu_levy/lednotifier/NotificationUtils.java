package com.ciubotariu_levy.lednotifier;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

public class NotificationUtils {
	public static final String TAG = NotificationUtils.class.getName();
	public static final int RECEIVER_REQUEST_CODE = 1;
	public static final int NOTIFICATION_ID = 1;
	public static final int DELAY_TIME = 10*60*1000;

	
	public static String title;
	public static String message;
	public static PendingIntent contentIntent;
	
	@TargetApi(19)
	public static void notify (Context context, Notification notif, boolean timeoutLED){
		dismissAlarm (context);
		if (notif.ledARGB == Color.GRAY){ //ensure LED is turned off
			notif.ledARGB = 0;
			notif.ledOnMS = 0;
			notif.ledOffMS = 0;
			notif.flags = notif.flags & ~Notification.FLAG_SHOW_LIGHTS;
			timeoutLED = false;
		}
		if (timeoutLED){
			Intent i = new Intent (context,LEDCancelReceiver.class);
			PendingIntent p = PendingIntent.getBroadcast(context, RECEIVER_REQUEST_CODE, i, PendingIntent.FLAG_UPDATE_CURRENT);
			AlarmManager a = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT){
				a.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+DELAY_TIME, p);
			}
			else {
				a.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+DELAY_TIME, p);
			}
		}
		notify (context,notif);
	}
	
	public static void notify (Context context, Notification notif){
		((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, notif);
	}
	
	public static void dismissAlarm (Context context){
		Intent i = new Intent (context,LEDCancelReceiver.class);
		PendingIntent p = PendingIntent.getBroadcast(context, RECEIVER_REQUEST_CODE, i, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager a = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		a.cancel(p);
	}
	
	public static void cancel (Context context){
		dismissAlarm(context);
		((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(NOTIFICATION_ID);
		title = null;
		message = null;
		contentIntent = null;
	}
}
