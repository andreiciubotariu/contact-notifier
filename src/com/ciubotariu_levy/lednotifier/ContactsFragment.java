package com.ciubotariu_levy.lednotifier;

import java.util.HashMap;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.ciubotariu_levy.lednotifier.providers.LedContactInfo;
import com.ciubotariu_levy.lednotifier.providers.LedContacts;

public class ContactsFragment extends ListFragment implements DataFetcher.OnDataFetchedListener, LoaderManager.LoaderCallbacks<Cursor>
{
	/*
	 * Defines an array that contains column names to move from
	 * the Cursor to the ListView.
	 */
	@SuppressLint("InlinedApi")
	private final static String COLUMN_NAME = Build.VERSION.SDK_INT
	>= Build.VERSION_CODES.HONEYCOMB ?
			Contacts.DISPLAY_NAME_PRIMARY :
				Contacts.DISPLAY_NAME;

	private final static String[] FROM_COLUMNS = {
		COLUMN_NAME, CommonDataKinds.Phone.NUMBER, Contacts._ID
	};

	private static final String[] PROJECTION = {
		Contacts._ID,
		Contacts.LOOKUP_KEY,
		COLUMN_NAME,
		CommonDataKinds.Phone.NUMBER
	};
	// The column index for the _ID column
	private static final int CONTACT_ID_INDEX = 0;
	// The column index for the LOOKUP_KEY column
	private static final int LOOKUP_KEY_INDEX = 1;

	// Defines the text expression
	@SuppressLint("InlinedApi")
	private static final String SELECTION =
	Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ?
			Contacts.DISPLAY_NAME_PRIMARY + " LIKE ?" :
				Contacts.DISPLAY_NAME + " LIKE ?";
	// Defines a variable for the search string
	private String mSearchString;
	// Defines the array to hold values that replace the ?
	private String[] mSelectionArgs = { mSearchString };

	/*
	 * Defines an array that contains resource ids for the layout views
	 * that get the Cursor column contents. The id is pre-defined in
	 * the Android framework, so it is prefaced with "android.R.id"
	 */
	private final static int[] TO_IDS = {
		android.R.id.text1, android.R.id.text2, R.id.contact_display_color
	};

	private static final String TAG = "ContactsFragment";

	// The contact's _ID value
	long mContactId;
	// The contact's LOOKUP_KEY
	String mContactKey;
	// A content URI for the selected contact
	Uri mContactUri;
	// An adapter that binds the result Cursor to the ListView
	private SimpleCursorAdapter mCursorAdapter;

	private HashMap <String, LedContactInfo> mLedData;
	private DataFetcher mFetcher;

	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// Gets a CursorAdapter
		mCursorAdapter = new SimpleCursorAdapter(
				getActivity(),
				R.layout.list_row,
				null,
				FROM_COLUMNS, 
				TO_IDS,
				0);
		mCursorAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {

			@Override
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				switch (view.getId()){
				case R.id.contact_display_color:
					System.out.println (cursor.getString(cursor.getColumnIndex(Contacts._ID)));
					LedContactInfo info = mLedData.get(cursor.getString(cursor.getColumnIndex(Contacts._ID)));
					int color = info == null ? Color.GRAY : info.color;
					view.setBackgroundColor(color);
					return true;
				}
				return false;
			}
		});
		// Sets the adapter for the ListView
		setListAdapter(mCursorAdapter);

		mFetcher = new DataFetcher(this, LedContacts.CONTENT_URI);
		mFetcher.execute(getActivity());
	}

	@Override
	public void onListItemClick(ListView l, View item, int position, long rowID) {
		LedContactInfo info = mLedData.get(String.valueOf(rowID));
		if (info == null){
			info = new LedContactInfo();
			info.systemId = String.valueOf(rowID);
			mLedData.put(info.systemId, info);
		}
		info.color = Color.CYAN;
		ContentValues values = new ContentValues();
		values.put(LedContacts.SYSTEM_CONTACT_ID, rowID);
		values.put(LedContacts.COLOR, Color.CYAN);
		System.out.println (getActivity().getContentResolver().insert(LedContacts.CONTENT_URI, values));
		item.findViewById(R.id.contact_display_color).setBackgroundColor(Color.CYAN);
		System.out.println (rowID);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
		return new CursorLoader(
				getActivity(),
				CommonDataKinds.Phone.CONTENT_URI,
				PROJECTION,
				null,
				null,
				COLUMN_NAME + " ASC");
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		Log.i(TAG, "Load finished " + cursor.getCount());
		mCursorAdapter.swapCursor(cursor);
		setEmptyText("Empty");
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		Log.i(TAG, "Loader reset");
		mCursorAdapter.swapCursor(null);
	}

	@Override
	public void onDataFetched(HashMap<String, LedContactInfo> fetchedData) {
		mFetcher = null;
		mLedData = fetchedData;
		System.out.println (mLedData);
		//Initializes the loader
		getLoaderManager().initLoader(0, null, this);
	}

}
