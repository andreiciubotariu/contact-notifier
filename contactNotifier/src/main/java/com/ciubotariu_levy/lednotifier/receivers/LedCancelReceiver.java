package com.ciubotariu_levy.lednotifier.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.ciubotariu_levy.lednotifier.notifications.controller.NotificationController;

public class LedCancelReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationController.getInstance(context).cancelLedTimeout();
    }
}
