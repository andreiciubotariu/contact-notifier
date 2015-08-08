package com.ciubotariu_levy.lednotifier.messages;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class MessageHistory {
    protected static MessageInfo sCustomLedMessage, sCustomVibMessage, sCustomRingMessage;
    private static LinkedHashMap<String, MessageInfo> sMessages;
    private static List<String> sCustomMessageList;

    private static void ensureMap() {
        if (sMessages == null) {
            sMessages = new LinkedHashMap<>();
        }
    }

    private static void ensureList() {
        if (sCustomMessageList == null) {
            sCustomMessageList = new ArrayList<>();
        }
    }

    public static LinkedHashMap<String, MessageInfo> getMessages() {
        ensureMap();
        return sMessages;
    }

    public static List<String> getCustomMessages() {
        ensureList();
        return sCustomMessageList;
    }

    public static void clear() {
        ensureMap();
        ensureList();

        sMessages.clear();
        sCustomMessageList.clear();

        sCustomLedMessage = sCustomRingMessage = sCustomVibMessage = null;
    }

    public static void add(LinkedHashMap<String, MessageInfo> newMessages, List<String> customMessages) {
        ensureMap();
        ensureList();

        for (String key : newMessages.keySet()) {
            MessageInfo m = sMessages.get(key);
            MessageInfo newMessage = newMessages.get(key);
            if (m != null) {
                String additionalText = newMessage.text;
                if (m.text == null) {
                    m.text = additionalText;
                } else {
                    m.text += "\n" + additionalText;
                }
            } else {
                sMessages.put(key, newMessage);
            }
        }

        updateNotifMessages();
    }

    private static void updateNotifMessages() {
        ensureMap();
        for (String key : sMessages.keySet()) {
            MessageInfo message = sMessages.get(key);
            if (message.isCustom()) {
                if (message.customColor() && sCustomLedMessage == null) {
                    sCustomLedMessage = message;
                }
                if (message.customRing()) { //continually replace with most recent custom ring
                    sCustomRingMessage = message;
                }
                if (message.customVib()) { //and vib, since these are one-time alerts
                    sCustomVibMessage = message;
                }
            }
        }
    }
}