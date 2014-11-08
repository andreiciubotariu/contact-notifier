package com.ciubotariu_levy.lednotifier;

import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ListView;

import com.ciubotariu_levy.lednotifier.providers.LedContactInfo;
import com.ciubotariu_levy.lednotifier.providers.LedContacts;
import com.squareup.picasso.Transformation;


public class CustomContactsFragment extends AbstractContactsFragment {

    private static final String[] FROM_COLUMNS = {
            LedContacts.LAST_KNOWN_NAME, LedContacts.LAST_KNOWN_NUMBER, LedContacts.RINGTONE_URI, LedContacts.VIBRATE_PATTERN, LedContacts.COLOR, LedContacts.SYSTEM_CONTACT_LOOKUP_URI
    };

    private static final String[] PROJECTION = {
            LedContacts._ID,
            LedContacts.SYSTEM_CONTACT_LOOKUP_URI,
            LedContacts.LAST_KNOWN_NAME,
            LedContacts.LAST_KNOWN_NUMBER,
            LedContacts.COLOR,
            LedContacts.VIBRATE_PATTERN,
            LedContacts.RINGTONE_URI
    };

    private static final int LOADER_ID = 1;

    @Override
    protected String[] getFromColumns() {
        return FROM_COLUMNS;
    }

    @Override
    protected String[] getProjection() {
        return PROJECTION;
    }

    //not used
    @Override
    protected String getBareQuery() {
        return null;
    }

    @Override
    protected String getQuery() {
        return LedContacts.LAST_KNOWN_NAME + " LIKE ? OR " + LedContacts.LAST_KNOWN_NUMBER + " LIKE ?";
    }

    @Override
    protected int getLoaderId() {
        return LOADER_ID;
    }

    @Override
    protected AbstractViewBinder getViewBinder(Transformation transformation) {
        return new AbstractViewBinder(getActivity(), transformation) {
            @Override
            protected Uri getContactUri(Cursor cursor) {
                return Uri.parse(cursor.getString(cursor.getColumnIndex(LedContacts.SYSTEM_CONTACT_LOOKUP_URI)));
            }

            @Override
            protected String getName(Cursor cursor) {
                return cursor.getString(cursor.getColumnIndex(LedContacts.LAST_KNOWN_NAME));
            }

            @Override
            protected int getColor(Cursor cursor, String contactUri) {
                return cursor.getInt(cursor.getColumnIndex(LedContacts.COLOR));
            }

            @Override
            protected String getRingtoneUri(Cursor cursor, String contactUri) {
                return cursor.getString(cursor.getColumnIndex(LedContacts.RINGTONE_URI));
            }

            @Override
            protected String getVibPattern(Cursor cursor, String contactUri) {
                return cursor.getString(cursor.getColumnIndex(LedContacts.VIBRATE_PATTERN));
            }
        };
    }

    @Override
    protected void listSetupComplete() {
        //not used
    }

    @Override
    protected String[] filteredSelectionArgs(String constraint) {
        return new String[]{"%" + constraint + "%", "%" + constraint + "%"};
    }

    @Override
    protected Uri getContentUri() {
        return LedContacts.CONTENT_URI;
    }

    @Override
    protected String getSortColumn() {
        return LedContacts.LAST_KNOWN_NAME;
    }

    @Override
    public void onContactDetailsUpdated(LedContactInfo newData) {
        if (newData.color == Color.GRAY && TextUtils.isEmpty(newData.vibratePattern) && (TextUtils.isEmpty(newData.ringtoneUri) || ColorVibrateDialog.GLOBAL.equals(newData.ringtoneUri))) {
            getActivity().getContentResolver().delete(Uri.withAppendedPath(LedContacts.CONTENT_URI, String.valueOf(newData.id)), null, null);
            System.out.println("deleting");
        } else {
            ContentValues values = new ContentValues();
            values.put(LedContacts.COLOR, newData.color);
            values.put(LedContacts.VIBRATE_PATTERN, newData.vibratePattern);
            values.put(LedContacts.RINGTONE_URI, newData.ringtoneUri);
            getActivity().getContentResolver().update(Uri.withAppendedPath(LedContacts.CONTENT_URI, String.valueOf(newData.id)), values, null, null);
        }
    }

    @Override
    public void onListItemClick(ListView l, View item, int position, long rowID) {
        LedContactInfo data = new LedContactInfo();
        data.id = rowID;
        Cursor c = getCursorAdapter().getCursor();
        data.lastKnownName = c.getString(c.getColumnIndex(LedContacts.LAST_KNOWN_NAME));
        data.lastKnownNumber = c.getString(c.getColumnIndex(LedContacts.LAST_KNOWN_NUMBER));
        data.systemLookupUri = c.getString(c.getColumnIndex(LedContacts.SYSTEM_CONTACT_LOOKUP_URI));
        data.color = c.getInt(c.getColumnIndex(LedContacts.COLOR));
        data.vibratePattern = c.getString(c.getColumnIndex(LedContacts.VIBRATE_PATTERN));
        data.ringtoneUri = c.getString(c.getColumnIndex(LedContacts.RINGTONE_URI));
        if (TextUtils.isEmpty(data.ringtoneUri)) {
            data.ringtoneUri = ColorVibrateDialog.GLOBAL;
        }
        if (getChildFragmentManager().findFragmentByTag(CONTACT_DIALOG_TAG) == null) {
            ColorVibrateDialog.getInstance(data)
                    .show(getChildFragmentManager(), CONTACT_DIALOG_TAG);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getLoaderManager().initLoader(LOADER_ID, null, this);
    }
}
