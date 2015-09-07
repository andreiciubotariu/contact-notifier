package com.ciubotariu_levy.lednotifier.notifications.controller;

import android.app.Notification;

import com.ciubotariu_levy.lednotifier.BuildConfig;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class NotificationControllerLollipopMr1Test {

    @Test
    public void postNotification_storesNotification() {
        NotificationControllerLollipopMr1 controller = new UnitTestableNotificationControllerLollipopMr1();
        Notification notification = new Notification();
        Assert.assertNull(controller.mPendingMessageNotification);
        controller.postNotification(notification);
        Assert.assertNotNull(controller.mPendingMessageNotification);
    }

    @Test
    public void onSmsAppNotificationPosted_postsPendingNotificationIfAvailable() {
        NotificationControllerLollipopMr1 controller = spy(new UnitTestableNotificationControllerLollipopMr1());
        controller.postNotification(new Notification());
        Assert.assertNotNull(controller.mPendingMessageNotification);
        controller.onSmsAppNotificationPosted();
        Assert.assertNull(controller.mPendingMessageNotification);
        verify(controller, times(1)).clearDelayDismissRunnable();
        verify(controller, times(1)).postToNotificationService(any(Notification.class));
    }

    private static class UnitTestableNotificationControllerLollipopMr1 extends NotificationControllerLollipopMr1 {

        UnitTestableNotificationControllerLollipopMr1() {
            super(null, null);
        }

        @Override
        void clearDelayDismissRunnable() {
            // NOP
        }

        @Override
        void postToNotificationService(Notification notification) {
            // NOP
        }
    }
}