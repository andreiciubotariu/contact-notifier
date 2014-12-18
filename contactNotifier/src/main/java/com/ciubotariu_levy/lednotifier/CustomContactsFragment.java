package com.ciubotariu_levy.lednotifier;

import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ciubotariu_levy.lednotifier.providers.LedContactInfo;
import com.ciubotariu_levy.lednotifier.providers.LedContacts;
import com.melnykov.fab.FloatingActionButton;
import com.squareup.picasso.Transformation;


public class CustomContactsFragment extends AbstractContactsFragment {

    public static final String SELECT_CONTACTS_TAG = "select_contacts";
    public static final String SELECT_CONTACTS_TRANSACTION = "transaction_select_contacts";
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
    protected AbstractContactViewBinder getViewBinder(Transformation transformation) {
        return new AbstractContactViewBinder(transformation, this) {
            @Override
            protected boolean hasColorView() {
                return true;
            }

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_contact_list_wfab, container, false);
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
    protected String getRowIDColumn() {
        return LedContacts._ID;
    }

    @Override
    protected int getRowResID() {
        return R.layout.custom_contact_row;
    }

    @Override
    public void onContactSelected(int position, long id) {
        LedContactInfo data = new LedContactInfo();
        data.id = id;
        if (!getCursorAdapter().moveToPos(position)) {
            return;
        }
        Cursor c = getCursorAdapter().getCursor();
        data.lastKnownName = c.getString(c.getColumnIndex(LedContacts.LAST_KNOWN_NAME));
        data.lastKnownNumber = c.getString(c.getColumnIndex(LedContacts.LAST_KNOWN_NUMBER));
        data.systemLookupUri = c.getString(c.getColumnIndex(LedContacts.SYSTEM_CONTACT_LOOKUP_URI));
        data.color = c.getInt(c.getColumnIndex(LedContacts.COLOR));
        data.vibratePattern = c.getString(c.getColumnIndex(LedContacts.VIBRATE_PATTERN));
        data.ringtoneUri = c.getString(c.getColumnIndex(LedContacts.RINGTONE_URI));       if (TextUtils.isEmpty(data.ringtoneUri)) {
            data.ringtoneUri = ColorVibrateDialog.GLOBAL;
        }
        if (getChildFragmentManager().findFragmentByTag(CONTACT_DIALOG_TAG) == null) {
            ColorVibrateDialog.getInstance(data)
                    .show(getChildFragmentManager(), CONTACT_DIALOG_TAG);
        }
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
            getActivity().getContentResolver()
                    .update(Uri.withAppendedPath(LedContacts.CONTENT_URI,
                                    String.valueOf(newData.id)),
                            values,
                            null,
                            null);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getLoaderManager().initLoader(LOADER_ID, null, this);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.contact_list);
        FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fab);
        fab.attachToRecyclerView(recyclerView);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().beginTransaction()
                             .replace(R.id.content_frame, new AllContactsFragment(), SELECT_CONTACTS_TAG)
                             .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                             .addToBackStack(SELECT_CONTACTS_TRANSACTION).commit();
            }
        });
    }
}
