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

public class ContactsFragment extends ListFragment implements ColorDialog.OnColorChosenListener, DataFetcher.OnDataFetchedListener, LoaderManager.LoaderCallbacks<Cursor>
{
	/*
	 * Defines an array that contains column names to move from
	 * the Cursor to the ListView.
	 */
	@SuppressLint("InlinedApi")
	private final static String CONTACT_NAME = Build.VERSION.SDK_INT
	>= Build.VERSION_CODES.HONEYCOMB ?
			Contacts.DISPLAY_NAME_PRIMARY :
				Contacts.DISPLAY_NAME;

	private final static String[] FROM_COLUMNS = {
		CONTACT_NAME, CommonDataKinds.Phone.NUMBER, Contacts.LOOKUP_KEY
	};

	private static final String[] PROJECTION = {
		Contacts._ID,
		Contacts.LOOKUP_KEY,
		CONTACT_NAME,
		CommonDataKinds.Phone.NUMBER
	};

	/*
	 * Defines an array that contains resource ids for the layout views
	 * that get the Cursor column contents. The id is pre-defined in
	 * the Android framework, so it is prefaced with "android.R.id"
	 */
	private final static int[] TO_IDS = {
		android.R.id.text1, android.R.id.text2, R.id.contact_display_color
	};

	private static final String TAG = "ContactsFragment";

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
			//TODO delete orphaned LedContacts (eg as a result from a contact delete)
			@Override
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				switch (view.getId()){
				case R.id.contact_display_color:

					LedContactInfo info = mLedData.get(cursor.getString(cursor.getColumnIndex(Contacts.LOOKUP_KEY)));
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
		String lookupValue = mCursorAdapter.getCursor().getString(mCursorAdapter.getCursor().getColumnIndex(Contacts.LOOKUP_KEY));
		if (mLedData.get(lookupValue)==null){
			ColorDialog.getInstance(lookupValue, Color.GRAY)
			.show(getChildFragmentManager(), "color_dialog");
		}
		else{
			ColorDialog.getInstance(lookupValue, mLedData.get(lookupValue).color)
			.show(getChildFragmentManager(), "color_dialog");
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
		return new CursorLoader(
				getActivity(),
				CommonDataKinds.Phone.CONTENT_URI,
				PROJECTION,
				null,
				null,
				CONTACT_NAME + " ASC");
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

	@Override
	public void onColorChosen(int color, String lookupKey) {
		Log.i(TAG,"lookupKey = "+lookupKey);
		LedContactInfo info = mLedData.get(lookupKey);
		if (info == null){
			info = new LedContactInfo();
			info.systemId = lookupKey;
			mLedData.put(info.systemId, info);
		}
		info.color = color;
		ContentValues values = new ContentValues();
		if (info.id != -1){
			values.put(LedContacts._ID, info.id);
		}
		values.put(LedContacts.SYSTEM_CONTACT_ID, lookupKey);
		values.put(LedContacts.COLOR, color);
		Uri uri = getActivity().getContentResolver().insert(LedContacts.CONTENT_URI, values);
		info.id = Long.parseLong (uri.getLastPathSegment());
		mCursorAdapter.notifyDataSetChanged();
	}

}
