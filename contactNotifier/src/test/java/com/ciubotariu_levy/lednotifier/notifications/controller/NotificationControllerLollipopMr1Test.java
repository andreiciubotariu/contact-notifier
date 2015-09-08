package com.ciubotariu_levy.lednotifier.notifications.controller;

import android.app.Notification;
import android.os.Handler;
import android.os.Looper;

import com.ciubotariu_levy.lednotifier.BuildConfig;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
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
        Assert.assertEquals(0, controller.mNumSmsAppWaitTimeoutsHit);
        verify(controller, times(1)).updateWaitForSmsAppPreference(eq(true));
        verify(controller, times(1)).clearDelayDismissRunnable();
        verify(controller, times(1)).postToNotificationService(any(Notification.class));
    }

    @Test
    public void postNotification_postsDirectlyIfTimeoutThreshHit() {
        NotificationControllerLollipopMr1 controller = spy(new UnitTestableNotificationControllerLollipopMr1());
        for (int x = 0; x < NotificationControllerLollipopMr1.SMS_APP_TIMEOUT_MAX_HITS - 1; x++) {
            controller.postNotification(new Notification());
            controller.onSmsAppWaitTimeoutReached();
        }
        controller.postNotification(new Notification());
        controller.onSmsAppWaitTimeoutReached();
        verify(controller, times(NotificationControllerLollipopMr1.SMS_APP_TIMEOUT_MAX_HITS)).startSmsAppWaitTimeout();
        verify(controller, times(1)).updateWaitForSmsAppPreference(eq(false));
    }

    @Test
    public void postNotification_usesWaitIfReceivesSmsAppNotification() {
        NotificationControllerLollipopMr1 controller = spy(new UnitTestableNotificationControllerLollipopMr1() {
            @Override
            boolean shouldWaitForSmsApp() {
                return false;
            }
        });
        controller.postNotification(new Notification());
        verify(controller, times(1)).postToNotificationService(any(Notification.class));
        controller.onSmsAppNotificationPosted();
        verify(controller, times(1)).updateWaitForSmsAppPreference(eq(true));
    }

    private static class UnitTestableNotificationControllerLollipopMr1 extends NotificationControllerLollipopMr1 {

        UnitTestableNotificationControllerLollipopMr1() {
            super(RuntimeEnvironment.application, new Handler(Looper.getMainLooper()));
        }

        @Override
        void clearDelayDismissRunnable() {
            // NOP
        }

        @Override
        void postToNotificationService(Notification notification) {
            // NOP
        }

        @Override
        void startSmsAppWaitTimeout() {
            // NOP
        }

        @Override
        boolean shouldWaitForSmsApp() {
            return true;
        }
    }
}
