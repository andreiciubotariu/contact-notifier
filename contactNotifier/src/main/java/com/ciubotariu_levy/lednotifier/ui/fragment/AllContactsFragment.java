package com.ciubotariu_levy.lednotifier.ui.fragment;

import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;

import com.ciubotariu_levy.lednotifier.dataobserver.DataFetcher;
import com.ciubotariu_levy.lednotifier.R;
import com.ciubotariu_levy.lednotifier.providers.LedContactInfo;
import com.ciubotariu_levy.lednotifier.providers.LedContacts;
import com.ciubotariu_levy.lednotifier.ui.widget.AbstractContactViewBinder;
import com.squareup.picasso.Transformation;

import java.util.HashMap;

public class AllContactsFragment extends AbstractContactsFragment implements DataFetcher.OnDataFetchedListener {

    private static final String[] PROJECTION = {
            ContactsContract.Contacts.LOOKUP_KEY,
            CONTACT_NAME,
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID
    };

    private static final String TAG = AllContactsFragment.class.getName();
    private static final String BARE_QUERY = ContactsContract.Contacts.HAS_PHONE_NUMBER + "=?";
    private static final String QUERY = "(" + CONTACT_NAME + " LIKE ? OR " + ContactsContract.CommonDataKinds.Phone.NUMBER + " LIKE ?)";
    private static final int LOADER_ID = 0;

    private HashMap<String, LedContactInfo> mLedData;
    private String mModifiedQuery = QUERY;
    private DataFetcher mFetcher;

    @Override
    protected String[] getProjection() {
        return PROJECTION;
    }

    @Override
    protected String getQuery() {
        return mModifiedQuery;
    }

    @Override
    protected int getLoaderId() {
        return LOADER_ID;
    }

    @Override
    protected AbstractContactViewBinder getViewBinder(final Transformation transformation) {
        return new AbstractContactViewBinder(transformation, this) {
            @Override
            public boolean hasColorView() {
                return false;
            }

            @Override
            protected Uri getContactUri(Cursor cursor) {
                return ContactsContract.Contacts.getLookupUri(
                        cursor.getLong(cursor.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID)),
                        cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)));
            }

            @Override
            protected String getName(Cursor cursor) {
                return cursor.getString(cursor.getColumnIndex(CONTACT_NAME));
            }

            @Override
            protected int getColor(Cursor cursor, String contactUri) {
                LedContactInfo info = mLedData.get(contactUri);
                return info == null ? Color.GRAY : info.color;
            }

            @Override
            protected String getRingtoneUri(Cursor cursor, String contactUri) {
                LedContactInfo info = mLedData.get(contactUri);
                return info == null ? null : info.ringtoneUri;
            }

            @Override
            protected String getVibPattern(Cursor cursor, String contactUri) {
                LedContactInfo info = mLedData.get(contactUri);
                return info == null ? null : info.vibratePattern;
            }
        };
    }

    @Override
    protected void listSetupComplete() {
        mFetcher = new DataFetcher(this, LedContacts.CONTENT_URI);
        mFetcher.execute(getActivity());
    }

    @Override
    protected String[] filteredSelectionArgs(String constraint) {
        return new String[]{"%" + constraint + "%", "%" + constraint + "%"};
    }

    @Override
    protected Uri getContentUri() {
        return ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
    }

    @Override
    protected String getSortColumn() {
        return CONTACT_NAME;
    }

    @Override
    protected String getRowIDColumn() {
        return ContactsContract.CommonDataKinds.Phone.CONTACT_ID;
    }

    @Override
    protected int getRowResID() {
        return R.layout.contact_row;
    }

    @Override
    public void onContactDetailsUpdated(LedContactInfo newData) {
        LedContactInfo info = mLedData.get(newData.systemLookupUri);
        if (newData.color == Color.GRAY && TextUtils.isEmpty(newData.vibratePattern) && (TextUtils.isEmpty(newData.ringtoneUri) || ColorVibrateDialog.GLOBAL.equals(newData.ringtoneUri))) {
            getActivity().getContentResolver().delete(LedContacts.CONTENT_URI, LedContacts.SYSTEM_CONTACT_LOOKUP_URI + "=?", new String[]{newData.systemLookupUri});
            if (info != null) {
                mLedData.remove(newData.systemLookupUri);
            }
        } else {
            ContentValues values = new ContentValues();
            values.put(LedContacts.LAST_KNOWN_NAME, newData.lastKnownName);
            values.put(LedContacts.LAST_KNOWN_NUMBER, newData.lastKnownNumber);
            values.put(LedContacts.COLOR, newData.color);
            values.put(LedContacts.VIBRATE_PATTERN, newData.vibratePattern);
            values.put(LedContacts.RINGTONE_URI, newData.ringtoneUri);

            if (newData.id == -1) {
                values.put(LedContacts.SYSTEM_CONTACT_LOOKUP_URI, newData.systemLookupUri);
                Uri uri = getActivity().getContentResolver().insert(LedContacts.CONTENT_URI, values);
                newData.id = Long.parseLong(uri.getLastPathSegment());
            } else {
                Uri uri = Uri.withAppendedPath(LedContacts.CONTENT_URI, String.valueOf(newData.id));
                getActivity().getContentResolver().update(uri, values, null, null);
            }

            StringBuilder addExcludeQuery = new StringBuilder(mModifiedQuery);
            Cursor contactUriCursor = getActivity().getContentResolver().query(Uri.parse(newData.systemLookupUri), new String[]{ContactsContract.Contacts.LOOKUP_KEY}, null, null, null);
            if (contactUriCursor != null && contactUriCursor.moveToFirst()) {
                addExcludeQuery.append(" AND ")
                        .append(ContactsContract.Contacts.LOOKUP_KEY)
                        .append(" != \"")
                        .append(contactUriCursor.getString(contactUriCursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)))
                        .append("\"");
            }
            mModifiedQuery = addExcludeQuery.toString();

            if (contactUriCursor != null) {
                contactUriCursor.close();
            }

            getLoaderManager().restartLoader(LOADER_ID, null, this);
            mLedData.put(newData.systemLookupUri, newData);

        }
        getCursorAdapter().notifyDataSetChanged();
    }

    @Override
    public LedContactInfo onContactSelected(int position, long id) {
        LedContactInfo data = null;
        if (!getCursorAdapter().moveToPos(position)) {
            return null;
        }
        Cursor c = getCursorAdapter().getCursor();
        long contactID = c.getLong(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
        String lookupValue = ContactsContract.Contacts.getLookupUri(contactID, c.getString(c.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY))).toString();
        if (mLedData.get(lookupValue) != null) {
            data = new LedContactInfo(mLedData.get(lookupValue));
        } else {
            data = new LedContactInfo();
            data.systemLookupUri = lookupValue;
            data.color = Color.GRAY;
            data.vibratePattern = "";
            data.ringtoneUri = ColorVibrateDialog.GLOBAL;
        }
        data.lastKnownName = c.getString(c.getColumnIndex(CONTACT_NAME));
        return data;
    }

    @Override
    public void onDataFetched(String excludeQuery, HashMap<String, LedContactInfo> fetchedData) {
        Log.d(TAG, "onDataFetched: data fetched = " + fetchedData);
        mFetcher = null;
        mLedData = fetchedData;
        if (getActivity() != null) {
            mModifiedQuery = QUERY + excludeQuery;
            getLoaderManager().initLoader(LOADER_ID, null, this);
        }
    }
}