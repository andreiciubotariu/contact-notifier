package com.ciubotariu_levy.lednotifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmDismissReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		NotificationUtils.dismissAlarm(context);
	}
}
