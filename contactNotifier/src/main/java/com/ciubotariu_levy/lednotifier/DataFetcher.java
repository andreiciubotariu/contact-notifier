package com.ciubotariu_levy.lednotifier;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;

import com.ciubotariu_levy.lednotifier.providers.LedContactInfo;
import com.ciubotariu_levy.lednotifier.providers.LedContacts;

import java.util.HashMap;

public class DataFetcher extends
        AsyncTask<Context, Void, HashMap<String, LedContactInfo>> {

    private Uri mUri;
    private OnDataFetchedListener mListener;
    private StringBuilder mExcludeQueryBuilder = new StringBuilder();
    public DataFetcher(OnDataFetchedListener listener, Uri uri) {
        mListener = listener;
        mUri = uri;
    }

    @Override
    protected HashMap<String, LedContactInfo> doInBackground(Context... params) {
        HashMap<String, LedContactInfo> map = new HashMap<String, LedContactInfo>();
        if (params[0] == null) {
            return map;
        }
        String[] projection = new String[]{LedContacts._ID, LedContacts.SYSTEM_CONTACT_LOOKUP_URI, LedContacts.LAST_KNOWN_NAME, LedContacts.LAST_KNOWN_NUMBER, LedContacts.COLOR, LedContacts.VIBRATE_PATTERN,
                LedContacts.RINGTONE_URI};
        Cursor c = params[0].getContentResolver().query(mUri, projection, null, null, null);
        if (c != null && c.moveToFirst()) {
            do {
                LedContactInfo info = new LedContactInfo();
                info.id = c.getInt(c.getColumnIndex(LedContacts._ID));
                info.systemLookupUri = c.getString(c.getColumnIndex(LedContacts.SYSTEM_CONTACT_LOOKUP_URI));
                info.lastKnownName = c.getString(c.getColumnIndex(LedContacts.LAST_KNOWN_NAME));
                info.lastKnownNumber = c.getString(c.getColumnIndex(LedContacts.LAST_KNOWN_NUMBER));
                info.color = c.getInt(c.getColumnIndex(LedContacts.COLOR));
                info.vibratePattern = c.getString(c.getColumnIndex(LedContacts.VIBRATE_PATTERN));
                info.ringtoneUri = c.getString(c.getColumnIndex(LedContacts.RINGTONE_URI));
                if (info.ringtoneUri != null && info.ringtoneUri.equalsIgnoreCase("null")) {
                    info.ringtoneUri = null;
                }
                map.put(String.valueOf(info.systemLookupUri), info);

                Cursor contactUriCursor = params[0].getContentResolver().query(Uri.parse(info.systemLookupUri), new String[]{ContactsContract.Contacts.LOOKUP_KEY}, null, null, null);
                if (contactUriCursor != null && contactUriCursor.moveToFirst()) {
                    mExcludeQueryBuilder.append(" AND ")
                            .append(ContactsContract.Contacts.LOOKUP_KEY)
                            .append(" != \"")
                            .append(contactUriCursor.getString(contactUriCursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)))
                            .append("\"");
                }

                if (contactUriCursor != null) {
                    contactUriCursor.close();
                }
            }
            while (c.moveToNext());
            c.close();
        }
        return map;
    }

    @Override
    protected void onPostExecute(HashMap<String, LedContactInfo> map) {
        if (mListener != null) {
            mListener.onDataFetched(mExcludeQueryBuilder.toString(), map);
        }
    }

    public interface OnDataFetchedListener {
        void onDataFetched(String excludeQuery, HashMap<String, LedContactInfo> fetchedData);
    }

}
