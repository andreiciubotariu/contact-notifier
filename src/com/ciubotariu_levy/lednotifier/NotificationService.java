package com.ciubotariu_levy.lednotifier;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationService extends NotificationListenerService {
	private final static String TAG = "NotificationService";
	private final static String KEY_REPLACE_NOTIFICATION = "replace_notification";
	private final static String KEY_TIE_NOTIFICATION = "tie_to_sms_app";

	protected static boolean isNotificationListenerServiceOn = false;
	private Notification mCurrentNotification = null;
	private boolean mReplaceNotification = false;
	private boolean mTieNotification = false;

	private SharedPreferences.OnSharedPreferenceChangeListener prefListener = new OnSharedPreferenceChangeListener (){

		@Override
		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, String key) {
			if (KEY_REPLACE_NOTIFICATION.equals(key)){
				mReplaceNotification = sharedPreferences.getBoolean(key, false);
			}
			else if (KEY_TIE_NOTIFICATION.equals(key)){
				mTieNotification = sharedPreferences.getBoolean(key, false);
			}
		}
	};

	private SMSReceiver mSmsReceiver = new SMSReceiver(){

		@Override
		public void onNotificationGenerated(Context context, Notification notif){
			context = NotificationService.this;
			if (notif.ledARGB == Color.GRAY && !PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SHOW_ALL_NOTIFS, true)){
				mCurrentNotification = null;
				return;
			}
			if (mReplaceNotification){
				StatusBarNotification [] notifications = getActiveNotifications();
				for (StatusBarNotification sb: notifications){
					if (isMessagingApp(sb.getPackageName()) && sb.isClearable()){
						int color = notif.ledARGB;
						notif = copyNotification(context, sb.getNotification(),color);
						cancelNotification(sb.getPackageName(), sb.getTag(), sb.getId());
						System.out.println ("Notification replaced");
						break;
					}
				}
			}
			else {
				mCurrentNotification = notif;
			}
			notify (context, notif);
		}
	};

	@TargetApi(19)
	@Override
	public void onCreate(){
		super.onCreate();
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		sharedPrefs.registerOnSharedPreferenceChangeListener(prefListener);
		mReplaceNotification = sharedPrefs.getBoolean(KEY_REPLACE_NOTIFICATION, false);
		mTieNotification = sharedPrefs.getBoolean(KEY_TIE_NOTIFICATION, false);
		String filterAction = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ?
				Telephony.Sms.Intents.SMS_RECEIVED_ACTION :
					"android.provider.Telephony.SMS_RECEIVED";
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
		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(prefListener);
		unregisterReceiver(mSmsReceiver);
		isNotificationListenerServiceOn = false;
		super.onDestroy();
	}

	@Override
	public void onNotificationPosted(StatusBarNotification sbn) {
		if (sbn.getPackageName().equals(getPackageName())){
			mCurrentNotification = sbn.getNotification();
		}
		if (mCurrentNotification != null && isMessagingApp(sbn.getPackageName())){
			if (mReplaceNotification){
				System.out.println ("Replacing notification");
				int color = mCurrentNotification.ledARGB;
				mCurrentNotification = copyNotification(this, sbn.getNotification(),color);
				cancelNotification(sbn.getPackageName(), sbn.getTag(), sbn.getId());
			}

			SMSReceiver.notify (this, mCurrentNotification);
		}
	}


	@Override
	public void onNotificationRemoved(StatusBarNotification sbn) {
		if (mReplaceNotification && sbn.getPackageName().equals(getPackageName())){
			mCurrentNotification = null;
		}
		else if (mTieNotification && !mReplaceNotification && isMessagingApp(sbn.getPackageName())){
			((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(SMSReceiver.NOTIFICATION_ID);
			mCurrentNotification = null;
		}
	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
	private boolean isMessagingApp (String packageName){
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
			return packageName.equals(Telephony.Sms.getDefaultSmsPackage(this));
		}
		return !packageName.equals(getPackageName())
				&&(packageName.equals(PreferenceManager.getDefaultSharedPreferences(this).getString(SmsAppChooserDialog.KEY_SMS_APP_PACKAGE, null))
				||packageName.contains("mms")
				||packageName.contains("sms") 
				||packageName.contains("messaging")
				||packageName.contains("message"));
	}

	private static Notification copyNotification (Context context, Notification toCopy, int color){
		return new NotificationCompat.Builder(context)
		.setSmallIcon(R.drawable.ic_launcher)
		.setTicker(toCopy.tickerText, toCopy.tickerView)
		.setContent(toCopy.contentView)
		.setAutoCancel(true)
		.setContentIntent(toCopy.contentIntent)
		//.setSound(toCopy.sound)
		.setLights(color, 1000, 1000)
		.build();
	}
}
