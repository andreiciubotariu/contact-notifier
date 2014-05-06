package com.ciubotariu_levy.lednotifier;

import java.util.HashMap;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
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
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ciubotariu_levy.lednotifier.providers.LedContactInfo;
import com.ciubotariu_levy.lednotifier.providers.LedContacts;

public class ContactsFragment extends ListFragment implements ColorVibrateDialog.ContactDetailsUpdateListener, DataFetcher.OnDataFetchedListener, LoaderManager.LoaderCallbacks<Cursor>
{
	//copied ListFragment Constants due to access issue.
	private static final int INTERNAL_EMPTY_ID = 0x00ff0001;
	private static final int INTERNAL_PROGRESS_CONTAINER_ID = 0x00ff0002;
	private static final int INTERNAL_LIST_CONTAINER_ID = 0x00ff0003;

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

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		// Gets a CursorAdapter
		mCursorAdapter = new SectionedCursorAdapter(
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

	//copied from support ListFragment source to include FastScrollThemedListView. Swapped FILL_PARENT for MATCH_PARENT
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		final Context context = getActivity();
		FrameLayout root = new FrameLayout(context);
		// ------------------------------------------------------------------
		LinearLayout pframe = new LinearLayout(context);
		pframe.setId(INTERNAL_PROGRESS_CONTAINER_ID);
		pframe.setOrientation(LinearLayout.VERTICAL);
		pframe.setVisibility(View.GONE);
		pframe.setGravity(Gravity.CENTER);
		ProgressBar progress = new ProgressBar(context, null,
				android.R.attr.progressBarStyleLarge);
		pframe.addView(progress, new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		root.addView(pframe, new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		// ------------------------------------------------------------------
		FrameLayout lframe = new FrameLayout(context);
		lframe.setId(INTERNAL_LIST_CONTAINER_ID);

		TextView tv = new TextView(getActivity());
		tv.setId(INTERNAL_EMPTY_ID);
		tv.setGravity(Gravity.CENTER);
		lframe.addView(tv, new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		ListView lv = new FastScrollThemedListView(getActivity());
		lv.setId(android.R.id.list);
		lv.setDrawSelectorOnTop(false);
		lframe.addView(lv, new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		root.addView(lframe, new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		// ------------------------------------------------------------------
		root.setLayoutParams(new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		return root;
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
	public void onCreateOptionsMenu (Menu menu, MenuInflater inflater){
		inflater.inflate(R.menu.contacts_frag, menu);
		MenuItem searchItem = menu.findItem(R.id.action_search);
		SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
		searchView.setOnCloseListener(new SearchView.OnCloseListener() {

			@Override
			public boolean onClose() {
				getLoaderManager().restartLoader(LOADER_ID, null, ContactsFragment.this);
				return false;
			}
		});
		searchView.setOnQueryTextListener(new OnQueryTextListener() {

			@Override
			public boolean onQueryTextSubmit(String newText) {
				Bundle args = new Bundle();
				args.putString(KEY_CONSTRAINT, newText);
				getLoaderManager().restartLoader(LOADER_ID, args, ContactsFragment.this);
				return false;
			}

			@Override
			public boolean onQueryTextChange(String query) {
				Bundle args = new Bundle();
				args.putString(KEY_CONSTRAINT, query);
				getLoaderManager().restartLoader(LOADER_ID, args, ContactsFragment.this);
				return false;
			}
		});
		MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {

			@Override
			public boolean onMenuItemActionExpand(MenuItem item) {
				getLoaderManager().restartLoader(LOADER_ID, null, ContactsFragment.this);
				return true;
			}

			@Override
			public boolean onMenuItemActionCollapse(MenuItem item) {
				getLoaderManager().restartLoader(LOADER_ID, null, ContactsFragment.this);
				return true;
			}
		});
	}

	private final static String bareQuery = CommonDataKinds.Phone.TYPE + "=?";
	private final static String query = bareQuery +" AND (" + CONTACT_NAME + " LIKE ? OR " + CommonDataKinds.Phone.NUMBER + " LIKE ?)";
	private final static String KEY_CONSTRAINT = "KEY_FILTER";
	private final static int LOADER_ID = 0;


	@Override
	public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
		getListView().setFastScrollEnabled(false);
		String constraint = "";
		if (args != null && args.getString(KEY_CONSTRAINT) != null){
			constraint = args.getString(KEY_CONSTRAINT);
		}

		String [] filteredSelectionArgs = new String [] {String.valueOf(CommonDataKinds.Phone.TYPE_MOBILE), "%"+constraint+"%", "%"+constraint+"%"};

		return new CursorLoader(
				getActivity(),
				CommonDataKinds.Phone.CONTENT_URI,
				PROJECTION,
				query,
				filteredSelectionArgs,
				CONTACT_NAME + " ASC");
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mCursorAdapter.swapCursor(cursor);
		getListView().setFastScrollEnabled(true);
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
		if (getActivity() != null){
			getLoaderManager().initLoader(LOADER_ID, null, this);
		}
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
