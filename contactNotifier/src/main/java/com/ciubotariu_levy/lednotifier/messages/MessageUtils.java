package com.ciubotariu_levy.lednotifier.messages;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;

import com.ciubotariu_levy.lednotifier.ColorVibrateDialog;
import com.ciubotariu_levy.lednotifier.providers.LedContacts;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.PduParser;

import java.util.LinkedHashMap;

public class MessageUtils {
    private static final String TAG = MessageUtils.class.getName();

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static SmsMessage[] getSMSMessagesFromIntent(Intent intent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            Object[] messages = (Object[]) intent.getSerializableExtra("pdus");
            int pduCount = messages.length;
            SmsMessage[] msgs = new SmsMessage[pduCount];
            for (int i = 0; i < pduCount; i++) {
                byte[] pdu = (byte[]) messages[i];
                msgs[i] = SmsMessage.createFromPdu(pdu);
            }
            return msgs;
        }

        return Telephony.Sms.Intents.getMessagesFromIntent(intent);
    }

    private static void setNameAndUri(MessageInfo info, ContentResolver resolver) {
        String number = info.address;
        Cursor contactCursor = null;
        try {
            Uri phoneNumberUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
            contactCursor = resolver.query(phoneNumberUri, new String[]{ContactsContract.Contacts.LOOKUP_KEY, ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
            if (contactCursor != null && contactCursor.moveToFirst()) {
                Uri contactUri = ContactsContract.Contacts.getLookupUri(contactCursor.getLong(contactCursor.getColumnIndex(ContactsContract.PhoneLookup._ID)), contactCursor.getString(contactCursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)));
                String contactUriString = contactUri == null ? null : contactUri.toString();
                info.contactUri = contactUriString;
                info.name = contactCursor.getString(contactCursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
            } else {
                Log.i(TAG, "setNameAndUri: " + info.address + " is not a contact in phone db");
                info.name = null;
                info.contactUri = null;
            }
        } finally {
            if (contactCursor != null) {
                contactCursor.close();
            }
        }
    }

    private static MessageInfo getInfo(SmsMessage message, Context context) {
        return getInfo(message.getOriginatingAddress(), message.getDisplayMessageBody(), context);
    }

    private static MessageInfo getInfo(String address, String text, Context context) {
        MessageInfo info = new MessageInfo();
        info.address = address;
        info.contentString = text;

        setNameAndUri(info, context.getContentResolver());

        String[] projection = new String[]{LedContacts.COLOR, LedContacts.SYSTEM_CONTACT_LOOKUP_URI, LedContacts.VIBRATE_PATTERN, LedContacts.RINGTONE_URI};
        String selection = null;
        String[] selectionArgs = null;
        selection = LedContacts.SYSTEM_CONTACT_LOOKUP_URI + " = ?";
        if (info.contactUri != null) {
            selectionArgs = new String[]{info.contactUri};

            Cursor c = context.getContentResolver().query(LedContacts.CONTENT_URI, projection, selection, selectionArgs, null);

            if (c != null && c.moveToFirst()) {
                try {
                    int customColor = c.getInt(c.getColumnIndex(LedContacts.COLOR));

                    if (customColor != Color.GRAY) {
                        info.color = customColor;
                    }
                    String customRingtone = c.getString(c.getColumnIndex(LedContacts.RINGTONE_URI));
                    if (!ColorVibrateDialog.GLOBAL.equals(customRingtone)) {
                        info.ringtoneUri = customRingtone;
                    }
                    String customVib = c.getString(c.getColumnIndex(LedContacts.VIBRATE_PATTERN));

                    if (!TextUtils.isEmpty(customVib)) {
                        info.vibPattern = customVib;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (c != null) {
                c.close();
            }
        }
        return info;
    }

    public static LinkedHashMap<String, MessageInfo> createMessageInfosFromSmsIntent(Intent intent, Context context) {
        SmsMessage[] sms = getSMSMessagesFromIntent(intent);
        LinkedHashMap<String, MessageInfo> infoMap = new LinkedHashMap<>();
        for (int x = 0; x < sms.length; x++) {
            String address = sms[x].getOriginatingAddress();
            if (address != null) {
                if (infoMap.get(address) == null) {
                    MessageInfo i = getInfo(sms[x], context);
                    infoMap.put(address, i);
                } else {
                    String moreText = sms[x].getDisplayMessageBody();
                    if (infoMap.get(address).contentString != null) {
                        infoMap.get(address).contentString += moreText;
                    } else {
                        infoMap.get(address).contentString = moreText;
                    }
                }
            }
        }
        return infoMap;
    }

    public static LinkedHashMap<String, MessageInfo> createMessageInfosFromPushIntent(Intent intent, Context context) {
        LinkedHashMap<String, MessageInfo> infoMap = new LinkedHashMap<>();

        // Get raw PDU push-data from the message and parse it
        byte[] pushData = intent.getByteArrayExtra("data");
        PduParser parser = new PduParser(pushData);
        GenericPdu pdu = parser.parse();
        if (null == pdu) {
            Log.e(TAG, "createMessageInfosFromPushIntent: pdu is null");
            return infoMap;
        }
        int type = pdu.getMessageType();
        long threadId = -1;
        String address = pdu.getFrom().getString();
        Log.v(TAG, "createMessageInfosFromPushIntent: pdu address is " + address);
        Log.v(TAG, "createMessageInfosFromPushIntent: pdu message type is " + pdu.getMessageType());

        MessageInfo i = getInfo(address, "New MMS", context);
        infoMap.put(address, i);
        return infoMap;
    }
}