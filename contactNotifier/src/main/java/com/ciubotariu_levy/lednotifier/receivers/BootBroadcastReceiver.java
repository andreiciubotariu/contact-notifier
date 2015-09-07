package com.ciubotariu_levy.lednotifier.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.ciubotariu_levy.lednotifier.dataobserver.ObserverService;

public class BootBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, ObserverService.class));
    }
}