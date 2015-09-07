package com.ciubotariu_levy.lednotifier.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.ciubotariu_levy.lednotifier.messages.MessageHistory;
import com.ciubotariu_levy.lednotifier.notifications.controller.NotificationController;

public class NotificationDismissReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationController.getInstance(context).dismissLedCancelAlarm();
        MessageHistory.getInstance().clear();
    }
}
