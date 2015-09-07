package com.ciubotariu_levy.lednotifier.preferences;

import com.ciubotariu_levy.lednotifier.R;

public enum Keys {
    SMS_APP_PACKAGE(R.string.pref_key_sms_app_package),
    STATUS_BAR_PREVIEW(R.string.pref_key_status_bar_preview),
    LED_TIMEOUT(R.string.pref_key_led_timeout),
    SHOW_ALL_NOTIFICATIONS(R.string.pref_key_show_all_notifications),
    DEFAULT_NOTIFICATION_COLOR(R.string.pref_key_default_notification_color),
    NOTIFICATION_AND_SOUND(R.string.pref_key_notif_and_sound),
    NOTIFICATIONS_NEW_MESSAGE_RINGTONE(R.string.pref_key_notifications_new_message_ringtone),
    NOTIFICATIONS_NEW_MESSAGE_VIBRATE(R.string.pref_key_notifications_new_message_vibrate),
    NOTIFICATIONS_SYSTEM_NOTIFICATION_SETTINGS(R.string.pref_key_system_notif_settings),
    DELAY_DISMISSAL(R.string.pref_key_delay_dismissal);

    private int mKeyResId;
    Keys(int keyResId) {
        mKeyResId = keyResId;
    }

    public int getResId() {
        return mKeyResId;
    }
}
