package com.ciubotariu_levy.lednotifier.notifications.handlers;

import android.os.Process;
import android.content.Context;

public class NotificationHandlerFactory {
    private static NotificationHandler sNotificationHandler;

    public static synchronized NotificationHandler getNotificationHandler(Context context) {
        return new NonSmsNotificationHandlerLollipopMr1();
    }
}
