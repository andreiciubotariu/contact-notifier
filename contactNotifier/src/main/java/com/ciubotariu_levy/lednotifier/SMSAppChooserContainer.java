package com.ciubotariu_levy.lednotifier;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class SMSAppChooserContainer extends FragmentActivity {
    private static final String SMS_DIALOG_TAG = "sms_chooser";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportFragmentManager().findFragmentByTag(SMS_DIALOG_TAG) == null) {
            new SmsAppChooserDialog().show(getSupportFragmentManager(), SMS_DIALOG_TAG);
        }
    }
}
