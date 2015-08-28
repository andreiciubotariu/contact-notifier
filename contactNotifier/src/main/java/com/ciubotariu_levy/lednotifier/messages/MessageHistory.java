package com.ciubotariu_levy.lednotifier.messages;

import android.graphics.Color;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MessageHistory {
    private static MessageHistory sInstance;

    private MessageInfo mCustomLedMessage, mCustomVibMessage, mCustomRingMessage;
    private LinkedHashMap<String, MessageInfo> mMessages;

    private MessageHistory() {
        mMessages = new LinkedHashMap<>();
    }

    private void reset() {
        mMessages.clear();
        mCustomLedMessage = null;
        mCustomRingMessage = null;
        mCustomVibMessage = null;
    }

    private void addNewMessages(LinkedHashMap<String, MessageInfo> newMessages) {
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

    private void setNotificationMessages() {
        for (MessageInfo message : mMessages.values()) {
            if (message.isCustom()) {
                if (message.hasCustomColor() && mCustomLedMessage == null) { // use the first found led color
                    mCustomLedMessage = message;
                }
                if (message.hasCustomRing()) { //continually replace with most recent custom ring
                    mCustomRingMessage = message;
                }
                if (message.hasCustomVib()) { //and vib, since these are one-time alerts
                    mCustomVibMessage = message;
                }
            }
        }
    }

    private static MessageHistory getInstance() {
        if (sInstance == null) {
            sInstance = new MessageHistory();
        }
        return sInstance;
    }

    public static LinkedHashMap<String, MessageInfo> getMessages() {
        return getInstance().mMessages;
    }

    public static int getCustomColor() {
        return getInstance().mCustomLedMessage == null ? Color.GRAY : getInstance().mCustomLedMessage.color;
    }

    public static String getCustomRingtone() {
        return getInstance().mCustomRingMessage == null ? null : getInstance().mCustomRingMessage.ringtoneUri;
    }

    public static String getCustomVibPattern() {
        return getInstance().mCustomRingMessage == null ? null : getInstance().mCustomVibMessage.vibPattern;
    }

    public static void clear() {
        getInstance().reset();
    }

    public static void addMessages(LinkedHashMap<String, MessageInfo> messages) {
        getInstance().addNewMessages(messages);
        updateNotifMessages();
    }

    private static void updateNotifMessages() {
        getInstance().setNotificationMessages();
    }
}