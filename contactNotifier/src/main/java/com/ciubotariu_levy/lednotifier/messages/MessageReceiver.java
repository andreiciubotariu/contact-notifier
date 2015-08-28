package com.ciubotariu_levy.lednotifier.messages;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.util.Log;

import com.google.android.mms.ContentType;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Class that handles SMS and MMS receive intents
 */
public class MessageReceiver extends BroadcastReceiver {
    private static final String TAG = MessageReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return;
        }

        LinkedHashMap<String, MessageInfo> receivedMessages = null;

        if (intent.getAction().equals(Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION)
                && ContentType.MMS_MESSAGE.equals(intent.getType())) {
            Log.v(TAG, "onReceive: received PUSH Intent: " + intent);
            receivedMessages = MessageUtils.createMessageInfosFromPushIntent(intent, context);
        } else if (intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
            Log.v(TAG, "onReceive: received SMS: " + intent);
            receivedMessages = MessageUtils.createMessageInfosFromSmsIntent(intent, context);
        }

        if (receivedMessages == null || receivedMessages.isEmpty()) {
            Log.w(TAG, "onReceive: no messages extracted");
            return;
        }

        processNewMessages(receivedMessages);
        Notification notification = getNotificationIfNeeded();
        postNotification(notification);

        onMessagesReceived(context, receivedMessages, customMessages);
    }

    private void processNewMessages(LinkedHashMap<String, MessageInfo> newMessages) {
        MessageHistory.addMessages(newMessages);
    }

    private Notification getNotificationIfNeeded(boolean showAllNotifications, int defColor, String defRingtone, String defaultVibPattern) {
        return null;
    }

    public void postNotification(Notification notification) {

    }

    private Bitmap loadContactPhotoThumbnail(Context context, String contactUri) {
        if (contactUri == null) {
            return null;
        }
        Cursor mCursor = context.getContentResolver().query((Uri.parse(contactUri)), new String[]{ContactsContract.Contacts._ID,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? ContactsContract.Contacts.PHOTO_THUMBNAIL_URI : ContactsContract.Contacts._ID}, null, null, null);

        if (mCursor == null || !mCursor.moveToFirst()) {
            Log.e(TAG, "loadContactPhotoThumbnail: cursor error | " + mCursor);
            return null;
        }

        int mThumbnailColumn;
        int mIdColumn = mCursor.getColumnIndex(ContactsContract.Contacts._ID);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mThumbnailColumn = mCursor.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI);
        } else {
            mThumbnailColumn = mIdColumn;
        }

        String photoData = mCursor.getString(mThumbnailColumn);
        if (photoData == null) {
            return null;
        }
        Log.v(TAG, "loadContactPhotoThumbnail: photoData is " + photoData);
        InputStream is = null;
        try {
            try {
                Uri thumbUri;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    thumbUri = Uri.parse(photoData);
                } else {
                    final Uri contactPhotoUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, photoData);
                    thumbUri = Uri.withAppendedPath(contactPhotoUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
                }

                is = context.getContentResolver().openInputStream(thumbUri);
                if (is != null) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    int height = (int) context.getResources().getDimension(android.R.dimen.notification_large_icon_height);
                    int width = (int) context.getResources().getDimension(android.R.dimen.notification_large_icon_width);
                    Bitmap bm = BitmapFactory.decodeStream(is);
                    return Bitmap.createScaledBitmap(bm, width, height, false);
                }
            } catch (FileNotFoundException e) {
                Log.e(TAG, "loadContactPhotoThumbnail: could not find file", e);
            }
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.e(TAG, "loadContactPhotoThumbnail: could not close input stream", e);
                }
            }
            if (mCursor != null) {
                mCursor.close();
            }
        }
        return null;
    }
}
