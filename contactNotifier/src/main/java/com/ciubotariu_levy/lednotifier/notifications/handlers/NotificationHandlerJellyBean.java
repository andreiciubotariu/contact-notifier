package com.ciubotariu_levy.lednotifier.notifications.handlers;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;
import android.provider.Telephony;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.ciubotariu_levy.lednotifier.BuildConfig;
import com.ciubotariu_levy.lednotifier.messages.MessageHistory;
import com.ciubotariu_levy.lednotifier.messages.MessageReceiver;
import com.ciubotariu_levy.lednotifier.messages.MessageUtils;
import com.ciubotariu_levy.lednotifier.notifications.Generator;
import com.ciubotariu_levy.lednotifier.notifications.NotificationUtil;
import com.ciubotariu_levy.lednotifier.notifications.controller.NotificationController;

// JellyBean 4.3 to Lollipop 5.0
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationHandlerJellyBean implements NotificationHandler {
    private static final String TAG = NotificationHandlerJellyBean.class.getName();

    private static final String SMS_RECEIVED_ACTION = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ?
            Telephony.Sms.Intents.SMS_RECEIVED_ACTION : "android.provider.Telephony.SMS_RECEIVED";
    private static final String MMS_RECEIVED_ACTION = "android.provider.Telephony.WAP_PUSH_RECEIVED";
    private static final String MMS_MIME_TYPE = "application/vnd.wap.mms-message";
    private static final int MAX_PRIORITY = 999;

    private MessageReceiver mSmsReceiver = new MessageReceiver() {
        @Override
        public void postNotification(Context context, Notification notif) {
            if (notif != null && context != null) {
                NotificationController.getInstance(context).postNotification(notif);
            }
        }
    };


    @Override
    public void onNotificationServiceCreated(Context context) {
        IntentFilter smsFilter = new IntentFilter(SMS_RECEIVED_ACTION);
        smsFilter.setPriority(MAX_PRIORITY);
        IntentFilter mmsFilter = new IntentFilter(MMS_RECEIVED_ACTION);
        try {
            mmsFilter.addDataType(MMS_MIME_TYPE);
        } catch (IntentFilter.MalformedMimeTypeException e) {
            Log.e(TAG, "onCreate: malformed mime type", e);
            if (BuildConfig.DEBUG) {
                throw new RuntimeException(e);
            }
        }
        mmsFilter.setPriority(MAX_PRIORITY);
        context.getApplicationContext().registerReceiver(mSmsReceiver, smsFilter);
    }

    @Override
    public void onNotificationServiceStopped(Context context) {
        context.getApplicationContext().unregisterReceiver(mSmsReceiver);
    }

    @Override
    public void onStatusBarNotificationPosted(Context context, StatusBarNotification sbn) {
        MessageUtils.getMessageInfoFromNotification(context, "asdsadasdasdasd");
        if (NotificationUtil.isMessagingApp(context, sbn.getPackageName())) {
            NotificationController.getInstance(context).onSmsAppNotificationPosted();
        }
    }

    @Override
    public void onStatusBarNotificationRemoved(Context context, StatusBarNotification sbn) {
        if (NotificationUtil.isMessagingApp(context, sbn.getPackageName())) {
            NotificationController.getInstance(context).onSmsAppNotificationDismissed();
        }
    }
}
