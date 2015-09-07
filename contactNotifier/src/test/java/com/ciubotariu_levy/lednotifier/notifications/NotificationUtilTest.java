package com.ciubotariu_levy.lednotifier.notifications;

import android.app.Notification;
import android.graphics.Color;
import android.net.Uri;

import com.ciubotariu_levy.lednotifier.BuildConfig;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class NotificationUtilTest {

    @Test
    public void clearRingtone() {
        Notification notification = new Notification();
        notification.sound = Uri.EMPTY;
        Assert.assertNotNull(notification.sound);
        NotificationUtil.clearRingtone(notification);
        Assert.assertNull(notification.sound);
    }

    @Test
    public void clearVibrate() {
        Notification notification = new Notification();
        notification.vibrate = new long[0];
        Assert.assertNotNull(notification.vibrate);
        NotificationUtil.clearVibrate(notification);
        Assert.assertNull(notification.vibrate);
    }

    @Test
    public void clearLed() {
        final int CUSTOM_COLOR = Color.RED;
        final int LED_MS = 1000;
        Notification notification = new Notification();
        notification.ledARGB = CUSTOM_COLOR;
        notification.ledOnMS = LED_MS;
        notification.ledOffMS = LED_MS;

        Assert.assertEquals(CUSTOM_COLOR, notification.ledARGB);
        Assert.assertEquals(LED_MS, notification.ledOnMS);
        Assert.assertEquals(LED_MS, notification.ledOffMS);

        NotificationUtil.clearLedFlagsForNotification(notification);
        Assert.assertEquals(Color.GRAY, notification.ledARGB);
        Assert.assertEquals(0, notification.ledOnMS);
        Assert.assertEquals(0, notification.ledOffMS);
    }
}