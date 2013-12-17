package com.ciubotariu_levy.lednotifier;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.provider.Telephony;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationService extends NotificationListenerService {
	private final static String TAG = "NotificationService";
	protected static boolean isNotificationListenerServiceOn = false;
	private NotificationManager mNotifyManager; 
	private Notification mCurrentNotification = null;
	private SMSReceiver mSmsReceiver = new SMSReceiver(){

		@Override
		public void onNotificationGenerated(Context context, Notification notif){
			context = NotificationService.this;
			if (notif.ledARGB == Color.GRAY){
				mCurrentNotification = null;
				Log.d(TAG, "returning");
				return;
			}
			notify (context, notif);
			mCurrentNotification = null;
		}
	};

	@TargetApi(19)
	@Override
	public void onCreate(){
		super.onCreate();
		String filterAction = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ?
				Telephony.Sms.Intents.SMS_RECEIVED_ACTION :
				"android.provider.Telephony.SMS_RECEIVED";
		mNotifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		IntentFilter filter = new IntentFilter (filterAction);
		registerReceiver(mSmsReceiver, filter);
	}
	
	@Override
	public IBinder onBind(Intent intent){
		isNotificationListenerServiceOn = true;
		return super.onBind(intent);
	}

	@Override
	public void onDestroy (){
		unregisterReceiver(mSmsReceiver);
		isNotificationListenerServiceOn = false;
		super.onDestroy();
	}

	@Override
	public void onNotificationPosted(StatusBarNotification sbn) {
		Log.i (TAG, "Notification Posted: " +
				"\n"+sbn.getNotification().tickerText +
				"\n"+sbn.getPackageName());
		if (mCurrentNotification != null && isMessagingApp(sbn.getPackageName())){
			SMSReceiver.notify (this, mCurrentNotification);
		}
	}


	@Override
	public void onNotificationRemoved(StatusBarNotification sbn) {
		if (isMessagingApp(sbn.getPackageName())){
			((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(SMSReceiver.NOTIFICATION_ID);
			mCurrentNotification = null;
		}
	}
	
	@TargetApi(Build.VERSION_CODES.KITKAT)
	private boolean isMessagingApp (String packageName){
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
			return packageName.equals(Telephony.Sms.getDefaultSmsPackage(this));
		}
		return (!packageName.equals(getPackageName())) && (packageName.contains("mms")||packageName.contains("sms") || packageName.contains("messaging")
				||packageName.contains("message"));
	}
}
