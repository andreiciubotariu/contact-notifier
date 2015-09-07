package com.ciubotariu_levy.lednotifier.dataobserver;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.Telephony.Sms;
import android.util.Log;

import com.ciubotariu_levy.lednotifier.notifications.controller.NotificationController;
import com.ciubotariu_levy.lednotifier.providers.LedContacts;

import java.util.ArrayList;
import java.util.List;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class ObserverService extends Service {
    static final Uri SMS_CONTENT_URI = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? Sms.CONTENT_URI : Uri.parse("content://sms/");
    static final Uri INBOX_URI = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? Sms.Inbox.CONTENT_URI : Uri.parse("content://sms/inbox/");
    static final String READ = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? Sms.Inbox.READ : "read";
    static final String SEEN = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? Sms.Inbox.SEEN : "seen";
    private static final String TAG = ObserverService.class.getName();
    @SuppressLint("InlinedApi")
    private static final String CONTACT_NAME = Build.VERSION.SDK_INT
            >= Build.VERSION_CODES.HONEYCOMB ?
            Contacts.DISPLAY_NAME_PRIMARY :
            Contacts.DISPLAY_NAME;


    private static final String[] CONTACT_PROJ = new String[]{
            CONTACT_NAME,
            Contacts.LOOKUP_KEY,
            Contacts.HAS_PHONE_NUMBER,
    };

    private static final String[] LED_CONTACTS_PROJ = new String[]{
            LedContacts._ID,
            LedContacts.SYSTEM_CONTACT_LOOKUP_URI,
            LedContacts.LAST_KNOWN_NAME,
            LedContacts.LAST_KNOWN_NUMBER};

    private int mUnreadCount;
    private int mUnseenCount;
    private ContactsChangeChecker mContactsChangeChecker;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mRegisteredObserver = false;

    private ContentObserver mContactContentObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (mContactsChangeChecker != null && !mContactsChangeChecker.isCancelled()) {
                mContactsChangeChecker.cancel(true);
            }
            mContactsChangeChecker = new ContactsChangeChecker();
            mContactsChangeChecker.execute();
        }
    };

    private ContentObserver mSMSContentObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            int unseen = getUnseenSms();
            int unread = getUnreadSms();

            if (unseen < mUnseenCount || unread < mUnreadCount) {
                NotificationController.getInstance(ObserverService.this).dismissNotification();
                unseen = 0;
                unread = 0;
            }
            mUnseenCount = unseen;
            mUnreadCount = unread;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        getContentResolver().registerContentObserver(Contacts.CONTENT_URI, true, mContactContentObserver);
        try {
            mUnreadCount = getUnreadSms();
            mUnseenCount = getUnseenSms();
            getContentResolver().registerContentObserver(SMS_CONTENT_URI, true, mSMSContentObserver);
            mRegisteredObserver = true;
        } catch (Exception e) { //sms inbox not standardized on jellybean and older
            Log.e(TAG, "onCreate: Could not register observer", e);
            throw new RuntimeException(e);
        }
        mContactsChangeChecker = new ContactsChangeChecker();
        mContactsChangeChecker.execute();
    }

    @Override
    public void onDestroy() {
        getContentResolver().unregisterContentObserver(mContactContentObserver);
        if (mRegisteredObserver) {
            getContentResolver().unregisterContentObserver(mSMSContentObserver);
            mRegisteredObserver = false;
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    private int getUnreadSms() {
        return getSmsBasedOnProperty(READ, mUnreadCount);
    }

    private int getUnseenSms() {
        return getSmsBasedOnProperty(SEEN, mUnseenCount);
    }

    private int getSmsBasedOnProperty(String prop, int currentCount) {
        int count = currentCount;
        Cursor c = getContentResolver().query(INBOX_URI, null, prop + "=?", new String[]{"0"}, null);
        if (c != null) {
            count = c.getCount();
            c.close();
        }
        return count;
    }

    private class ContactsChangeChecker extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            List<String> toDelete = new ArrayList<String>();
            ContentResolver resolver = getContentResolver();
            Cursor customContactsCursor = resolver.query(LedContacts.CONTENT_URI, LED_CONTACTS_PROJ, null, null, null);
            if (customContactsCursor != null && customContactsCursor.moveToFirst()) {
                do {
                    if (isCancelled()) {
                        break;
                    }
                    int id = customContactsCursor.getInt(customContactsCursor.getColumnIndex(LedContacts._ID));
                    String systemLookupUri = customContactsCursor.getString(customContactsCursor.getColumnIndex(LedContacts.SYSTEM_CONTACT_LOOKUP_URI));

                    Uri lookupUri = Uri.parse(systemLookupUri);
                    Uri newLookupUri = ContactsContract.Contacts.getLookupUri(resolver, lookupUri);
                    if (newLookupUri == null) {
                        toDelete.add(String.valueOf(id));
                    } else {
                        ContentValues values = new ContentValues();
                        boolean needsUpdating = false;

                        if (newLookupUri != null && !newLookupUri.equals(lookupUri)) {
                            values.put(LedContacts.SYSTEM_CONTACT_LOOKUP_URI, newLookupUri.toString());
                            needsUpdating = true;
                        }

                        Cursor contactNameCursor = getContentResolver().query(newLookupUri, CONTACT_PROJ, null, null, null);
                        if (contactNameCursor != null && contactNameCursor.moveToFirst()) {
                            String name = contactNameCursor.getString(contactNameCursor.getColumnIndex(CONTACT_NAME));
                            if (!name.equals(customContactsCursor.getString(customContactsCursor.getColumnIndex(LedContacts.LAST_KNOWN_NAME)))) {
                                values.put(LedContacts.LAST_KNOWN_NAME, name);
                                needsUpdating = true;
                            }
                            if (contactNameCursor.getInt(contactNameCursor.getColumnIndex(Contacts.HAS_PHONE_NUMBER)) == 0) {
                                needsUpdating = false;
                                toDelete.add(String.valueOf(id));
                            }
                        }
                        if (contactNameCursor != null) {
                            contactNameCursor.close();
                        }
                        if (needsUpdating) {
                            Uri updateUri = Uri.withAppendedPath(LedContacts.CONTENT_URI, String.valueOf(id));
                            resolver.update(updateUri, values, null, null);
                        }

                    }

                }
                while (customContactsCursor.moveToNext());
                customContactsCursor.close();
            }
            resolver.delete(LedContacts.CONTENT_URI, LedContacts._ID + " IN " + generateSelectionMarks(toDelete.size()), toDelete.toArray(new String[toDelete.size()]));

            return null;
        }

        private String generateSelectionMarks(int amount) {
            StringBuilder s = new StringBuilder("(");
            for (int x = 0; x < amount - 1; x++) {
                s.append("?, ");
            }
            if (amount > 0) {
                s.append("?");
            }
            s.append(")");
            return s.toString();
        }
    }
}
