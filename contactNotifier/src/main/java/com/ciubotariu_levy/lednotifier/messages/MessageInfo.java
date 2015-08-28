package com.ciubotariu_levy.lednotifier.messages;

import android.graphics.Color;
import android.text.TextUtils;

/** Class that contains information about SMS/MMS messages + Contact Notifier specific data */
public class MessageInfo {
    public String name, address, ringtoneUri, contactUri, vibPattern, contentString;
    public int color = Color.GRAY;

    public boolean isCustom() {
        return contactUri != null && (hasCustomColor() || hasCustomRing() || hasCustomVib());
    }

    public String getNameOrAddress() {
        if (name != null) {
            return name;
        }
        if (address != null) {
            return address;
        }

        return "Unknown Sender";
    }

    public void addContentString(String content) {
        if (TextUtils.isEmpty(contentString)) {
            contentString = content;
        } else {
            contentString += "\n" + content;
        }
    }

    public String getContentString() {
        return contentString != null ? contentString : "";
    }

    public boolean hasCustomColor() {
        return color != Color.GRAY;
    }

    public boolean hasCustomRing() {
        return ringtoneUri != null && !TextUtils.isEmpty(ringtoneUri);
    }

    public boolean hasCustomVib() {
        return vibPattern != null && !TextUtils.isEmpty(vibPattern);
    }
}
