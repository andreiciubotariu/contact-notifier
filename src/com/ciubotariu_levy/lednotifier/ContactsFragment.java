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
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ListView;

import com.ciubotariu_levy.lednotifier.providers.LedContactInfo;
import com.ciubotariu_levy.lednotifier.providers.LedContacts;

public class ContactsFragment extends ListFragment implements ColorVibrateDialog.ContactDetailsUpdateListener, DataFetcher.OnDataFetchedListener, LoaderManager.LoaderCallbacks<Cursor>
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
		CONTACT_NAME, CommonDataKinds.Phone.NUMBER,Contacts._ID, Contacts.LOOKUP_KEY
	};

	private static final String[] PROJECTION = {
		Contacts._ID,
		Contacts.LOOKUP_KEY,
		CONTACT_NAME,
		CommonDataKinds.Phone.NUMBER,
		CommonDataKinds.Phone.TYPE
	};

	/*
	 * Defines an array that contains resource ids for the layout views
	 * that get the Cursor column contents. The id is pre-defined in
	 * the Android framework, so it is prefaced with "android.R.id"
	 */
	private final static int[] TO_IDS = {
		android.R.id.text1, android.R.id.text2,R.id.contact_vibrate, R.id.contact_display_color
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
			@Override
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				switch (view.getId()){
				case R.id.contact_display_color:
					LedContactInfo info = mLedData.get(cursor.getString(cursor.getColumnIndex(Contacts.LOOKUP_KEY)));
					int color = info == null ? Color.GRAY : info.color;
					view.setBackgroundColor(color);
					return true;
				
				case R.id.contact_vibrate:
					info = mLedData.get(cursor.getString(cursor.getColumnIndex(Contacts.LOOKUP_KEY)));
					if (info != null && !TextUtils.isEmpty(info.vibratePattern)){
						view.setVisibility(View.VISIBLE);
						view.setBackgroundResource(R.drawable.ic_contact_vibrate);
					}
					else {
						view.setVisibility(View.GONE);
					}
				return true;
				}
				return false;
			}
		});
		// Sets the adapter for the ListView
		setListAdapter(mCursorAdapter);
		
		//change space between list items
		ListView listView = getListView();
		listView.setDivider(null);
		int dividerSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
		listView.setDividerHeight(dividerSize);
		listView.setCacheColorHint(Color.TRANSPARENT);
		
		mFetcher = new DataFetcher(this, LedContacts.CONTENT_URI);
		mFetcher.execute(getActivity());
	}

	@Override
	public void onListItemClick(ListView l, View item, int position, long rowID) {
		String lookupValue = mCursorAdapter.getCursor().getString(mCursorAdapter.getCursor().getColumnIndex(Contacts.LOOKUP_KEY));
		int color = Color.GRAY;
		String vibratePattern = null;
		if (mLedData.get(lookupValue)!=null){
			color = mLedData.get(lookupValue).color;
			vibratePattern = mLedData.get(lookupValue).vibratePattern;
		}
			ColorVibrateDialog.getInstance(lookupValue, color,vibratePattern)
			.show(getChildFragmentManager(), "color_vibrate_dialog");
	}

	@Override
	public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
		return new CursorLoader(
				getActivity(),
				CommonDataKinds.Phone.CONTENT_URI,
				PROJECTION,
				CommonDataKinds.Phone.TYPE + "=?",
				new String [] {String.valueOf(CommonDataKinds.Phone.TYPE_MOBILE)},
				CONTACT_NAME + " ASC");
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mCursorAdapter.swapCursor(cursor);
		setEmptyText("No contacts found");
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
		//Initializes the loader
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public void onContactDetailsUpdated(String lookupKey,int color,String vibratePattern) {
		LedContactInfo info = mLedData.get(lookupKey);
		if (color == Color.GRAY && TextUtils.isEmpty(vibratePattern)){
			getActivity().getContentResolver().delete(LedContacts.CONTENT_URI, LedContacts.SYSTEM_CONTACT_ID + "=?", new String [] {lookupKey});
			System.out.println ("deleting");
			if (info != null){
				mLedData.put(lookupKey, null);
			}
		}
		else {
			if (info == null){
				info = new LedContactInfo();
				info.systemId = lookupKey;
				mLedData.put(info.systemId, info);
			}
			info.color = color;
			info.vibratePattern = vibratePattern;
			ContentValues values = new ContentValues();
			if (info.id != -1){
				values.put(LedContacts._ID, info.id);
			}
			values.put(LedContacts.SYSTEM_CONTACT_ID, lookupKey);
			values.put(LedContacts.COLOR, color);
			values.put(LedContacts.VIBRATE_PATTERN, vibratePattern);
			Uri uri = getActivity().getContentResolver().insert(LedContacts.CONTENT_URI, values);
			info.id = Long.parseLong (uri.getLastPathSegment());
		}
		mCursorAdapter.notifyDataSetChanged();
	}
}
