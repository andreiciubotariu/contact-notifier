package com.ciubotariu_levy.lednotifier.messages;

import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;

/** Class that contains information about SMS/MMS messages + Contact Notifier specific data */
public class MessageInfo {
    private static final String TAG = MessageInfo.class.getName();
    static final String UNKNOWN_SENDER = "Unknown Sender";

    private String mName;
    private String mAddress;
    private String mRingtoneUriString;
    private String mContactUriString;
    private String mVibPattern;
    private String mContentString;
    private int mColor = Color.GRAY;

    /**
     * A message pertains to a custom contact if that contact exists in the system db AND any of the custom properties are set
     */
    public boolean isCustom() {
        return mContactUriString != null && (hasCustomColor() || hasCustomRing() || hasCustomVib());
    }

    public String getNameOrAddress() {
        if (mName != null) {
            return mName;
        }
        if (mAddress != null) {
            return mAddress;
        }
        Log.e(TAG, "getNameOrAddress: both name & address null, returning unknown");
        return UNKNOWN_SENDER;
    }

    public void addContentString(String content) {
        if (TextUtils.isEmpty(mContentString)) {
            mContentString = content;
        } else {
            mContentString += "\n" + content;
        }
    }

    public void setName(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }

    public void setAddress(String address) {
        mAddress = address;
    }

    public String getAddress() {
        return mAddress;
    }

    public String getContentString() {
        return mContentString != null ? mContentString : "";
    }

    public void setVibPattern(String vibPattern) {
        mVibPattern = vibPattern;
    }

    public String getVibPattern() {
        return mVibPattern;
    }

    public void setContactUriString(String contactUriString) {
        mContactUriString = contactUriString;
    }

    public String getContactUriString() {
        return mContactUriString;
    }

    public void setRingtoneUriString(String ringtoneUriString) {
        mRingtoneUriString = ringtoneUriString;
    }

    public String getRingtoneUriString() {
        return mRingtoneUriString;
    }

    public void setColor(int color) {
        mColor = color;
    }

    public int getColor() {
        return mColor;
    }

    public boolean hasCustomColor() {
        return mColor != Color.GRAY;
    }

    public boolean hasCustomRing() {
        return mRingtoneUriString != null && !TextUtils.isEmpty(mRingtoneUriString);
    }

    public boolean hasCustomVib() {
        return mVibPattern != null && !TextUtils.isEmpty(mVibPattern);
    }
}
