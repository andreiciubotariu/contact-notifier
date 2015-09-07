package com.ciubotariu_levy.lednotifier.notifications.controller;

import android.app.Notification;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;

import com.ciubotariu_levy.lednotifier.BuildConfig;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class NotificationControllerTest {

    @Test
    public void createsBaseNotificationControllerForLollipopMr0() {
        final int LOLLIPOP_MR0 = 21;
        Assert.assertFalse(NotificationController.createInstance(LOLLIPOP_MR0, RuntimeEnvironment.application, null) instanceof NotificationControllerLollipopMr1);
    }

    @Test
    public void createsExtendedNotificationControllerForLollipopMr1() {
        final int LOLLIPOP_MR1 = 22;
        Assert.assertTrue(NotificationController.createInstance(LOLLIPOP_MR1, RuntimeEnvironment.application, null) instanceof NotificationControllerLollipopMr1);
    }

    @Test
    public void onSmsNotificationDismissed_postsDelayedRunnableIfDelayEnabled() {
        NotificationController controller = spy(new UnitTestableNotificationController());
        controller.onSmsAppNotificationDismissed();
        verify(controller, times(1)).postDelayedDismissalRunnable();
    }

    @Test
    public void onSmsNotificationDismissed_dismissesIfNoDelaySet() {
        NotificationController controller = spy(new UnitTestableNotificationController() {
            @Override
            boolean isDelayDismissalEnabled() {
                return false;
            }
        });
        controller.onSmsAppNotificationDismissed();
        verify(controller, times(1)).dismissNotification();
        verify(controller, times(0)).postDelayedDismissalRunnable();
    }

    @Test
    public void postToNotificationService_createsLedTimeoutiIfPossible() {
        NotificationController controller = spy(new UnitTestableNotificationController());
        Notification notification = new Notification();
        notification.ledARGB = Color.RED;
        controller.postToNotificationService(notification);
        verify(controller, times(1)).createAndPostLedTimeout();
    }

    @Test
    public void postToNotificationService_doesNotCreateTimeoutIfNonCustomLed() {
        NotificationController controller = spy(new UnitTestableNotificationController());
        Notification notification = new Notification();
        notification.ledARGB = Color.GRAY;
        controller.postToNotificationService(notification);
        verify(controller, times(0)).createAndPostLedTimeout();
    }

    @Test
    public void postToNotificationService_doesNotCreateTimeoutIfTimeoutDisabled() {
        NotificationController controller = spy(new UnitTestableNotificationController() {
            @Override
            boolean hasLedTimeoutEnabled() {
                return false;
            }
        });
        Notification notification = new Notification();
        notification.ledARGB = Color.RED;
        controller.postToNotificationService(notification);
        verify(controller, times(0)).createAndPostLedTimeout();
    }

    private static class UnitTestableNotificationController extends NotificationController {

        UnitTestableNotificationController() {
            super(RuntimeEnvironment.application, new Handler(Looper.getMainLooper()));
        }

        @Override
        boolean hasLedTimeoutEnabled() {
            return true;
        }

        @Override
        boolean isDelayDismissalEnabled() {
            return true;
        }

        @Override
        void postDelayedDismissalRunnable() {
            // NOP
        }
    }
}
