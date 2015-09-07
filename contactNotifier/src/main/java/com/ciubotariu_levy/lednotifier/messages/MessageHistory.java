package com.ciubotariu_levy.lednotifier.messages;

import android.graphics.Color;
import android.text.TextUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class MessageHistory {
    private static MessageHistory sInstance;

    private MessageInfo mCustomLedMessage;
    private MessageInfo mCustomVibMessage;
    private MessageInfo mCustomRingMessage;
    private LinkedHashMap<String, MessageInfo> mMessages; // LinkedHashMap to keep message order but also provide an easy lookup api

    MessageHistory() {
        mMessages = new LinkedHashMap<>();
    }

    public static MessageHistory getInstance() {
        if (sInstance == null) {
            sInstance = new MessageHistory();
        }
        return sInstance;
    }

    private void reset() {
        mMessages.clear();
        mCustomLedMessage = null;
        mCustomRingMessage = null;
        mCustomVibMessage = null;
    }

    void addNewMessages(LinkedHashMap<String, MessageInfo> newMessages) {
        for (Map.Entry<String, MessageInfo> messageInfoEntry : newMessages.entrySet()) {
            MessageInfo existingMessage = mMessages.get(messageInfoEntry.getKey());
            MessageInfo newMessage = messageInfoEntry.getValue();
            if (existingMessage != null) {
                existingMessage.addContentString(newMessage.getContentString());
            } else {
                mMessages.put(messageInfoEntry.getKey(), newMessage);
            }
        }
    }

    void setNotificationMessages() {
        for (MessageInfo message : mMessages.values()) {
            if (message.isCustom()) {
                if (message.hasCustomColor() && mCustomLedMessage == null) { // Use the first found led color
                    mCustomLedMessage = message;
                }
                if (message.hasCustomRing()) { // [1/2] Continually replace with most recent custom ring
                    mCustomRingMessage = message;
                }
                if (message.hasCustomVib()) { // [2/2] and vib, since these are one-time alerts
                    mCustomVibMessage = message;
                }
            }
        }
    }

    public LinkedHashMap<String, MessageInfo> getMessages() {
        return mMessages;
    }

    public int getCustomColor() {
        return mCustomLedMessage == null ? Color.GRAY : mCustomLedMessage.getColor();
    }

    public String getCustomRingtone() {
        return mCustomRingMessage == null ? null : mCustomRingMessage.getRingtoneUriString();
    }

    public String getCustomVibPattern() {
        return mCustomVibMessage == null ? null : mCustomVibMessage.getVibPattern();
    }

    public boolean containsCustomMessages() {
        return getCustomColor() != Color.GRAY || !TextUtils.isEmpty(getCustomRingtone()) || !TextUtils.isEmpty(getCustomVibPattern());
    }

    public void clear() {
        reset();
    }

    public void addMessages(LinkedHashMap<String, MessageInfo> messages) {
        addNewMessages(messages);
        setNotificationMessages();
    }
}