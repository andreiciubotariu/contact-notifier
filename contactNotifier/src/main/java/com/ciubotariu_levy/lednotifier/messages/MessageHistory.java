package com.ciubotariu_levy.lednotifier.messages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class MessageHistory {
    private static LinkedHashMap<String, MessageInfo> messages;
    private static List<String> customMessageList;
    protected static MessageInfo customLEDMessage, customVibMessage, customRingMessage;

    private static void ensureMap() {
        if (messages == null) {
            messages = new LinkedHashMap<>();
        }
    }

    private static void ensureList() {
        if (customMessageList == null) {
            customMessageList = new ArrayList<>();
        }
    }


    public static LinkedHashMap<String, MessageInfo> getMessages() {
        ensureMap();
        return messages;
    }

    public static List<String> getCustomMessages() {
        ensureList();
        return customMessageList;
    }


    public static void clear() {
        ensureMap();
        ensureList();

        messages.clear();
        customMessageList.clear();

        customLEDMessage = customRingMessage = customVibMessage = null;
    }

    public static void add(LinkedHashMap<String,MessageInfo> newMessages, List<String> customMessages){
        ensureMap();
        ensureList();


        for (String key:newMessages.keySet()) {
            MessageInfo m = messages.get(key);
            MessageInfo newMessage = newMessages.get(key);
            if (m != null) {
                String additionalText = newMessage.text;
                if (m.text == null) {
                    m.text = additionalText;
                }
                else {
                    m.text += "\n" + additionalText;
                }
            }
            else {
                messages.put(key, newMessage);
            }
        }

        updateNotifMessages();
    }

    private static void updateNotifMessages() {
        ensureMap();
        for (String key: messages.keySet()) {
            MessageInfo message = messages.get(key);
            if (message.isCustom()) {
                if (message.customColor() && customLEDMessage == null) {
                   customLEDMessage = message;
                }
                if (message.customRing()) { //continually replace with most recent custom ring
                    customRingMessage = message;
                }
                if (message.customVib()) { //and vib, since these are one-time alerts
                    customVibMessage = message;
                }
            }
        }
    }
}