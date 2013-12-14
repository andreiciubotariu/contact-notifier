package com.ciubotariu_levy.lednotifier;

import android.annotation.SuppressLint;
import android.database.Cursor;
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

public class ContactsFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>
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
		COLUMN_NAME, CommonDataKinds.Phone.NUMBER
	};

	private static final String[] PROJECTION =
{
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
		android.R.id.text1, android.R.id.text2
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
		// Sets the adapter for the ListView
		setListAdapter(mCursorAdapter);
		
		//Initializes the loader
		getLoaderManager().initLoader(0, null, this);
	}
	
	@Override
    public void onListItemClick(ListView l, View item, int position, long rowID) {
    }
	
	@Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
        /*
         * Makes search string into pattern and
         * stores it in the selection array
         */
        mSelectionArgs[0] = "%" + mSearchString + "%";
        //String  selection = Contacts.HAS_PHONE_NUMBER + "=?";
        //String [] selectionArgs = new String [] {"1"};
        // Starts the query
        return new CursorLoader(
                getActivity(),
                CommonDataKinds.Phone.CONTENT_URI,
                PROJECTION,
                null,
                null,
                COLUMN_NAME + " ASC"
        );
    }
	
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Put the result Cursor in the adapter for the ListView
		Log.i(TAG, "Load finished " + cursor.getCount());
        mCursorAdapter.swapCursor(cursor);
        setEmptyText("Empty");
    }
	
	@Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Delete the reference to the existing Cursor
		Log.i(TAG, "Loader reset");
        mCursorAdapter.swapCursor(null);
    }

}
