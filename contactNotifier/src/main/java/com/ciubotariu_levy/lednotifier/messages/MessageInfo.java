package com.ciubotariu_levy.lednotifier.messages;

import android.graphics.Color;
import android.text.TextUtils;

public class MessageInfo {
    public String name, address, contactUri, ringtoneUri, vibPattern, text;
    public int color = Color.GRAY;

    public boolean isCustom() {
        return contactUri != null && (customColor() || customRing() || customVib());
    }

    public String name() {
        if (name != null) {
            return name;
        }
        if (address != null) {
            return address;
        }

        return "Unknown Sender";
    }

    public String text() {
        return text != null ? text : "";
    }

    public boolean customColor() {
        return color != Color.GRAY;
    }

    public boolean customRing() {
        return ringtoneUri != null && !TextUtils.isEmpty(ringtoneUri);
    }

    public boolean customVib() {
        return vibPattern != null && !TextUtils.isEmpty(vibPattern);
    }
}
