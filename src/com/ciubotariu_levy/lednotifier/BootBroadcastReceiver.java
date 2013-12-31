package com.ciubotariu_levy.lednotifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		context.startService (new Intent (context, ObserverService.class));
	}
}
