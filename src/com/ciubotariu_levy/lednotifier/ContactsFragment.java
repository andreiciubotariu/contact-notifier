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
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ciubotariu_levy.lednotifier.providers.LedContactInfo;
import com.ciubotariu_levy.lednotifier.providers.LedContacts;
import com.makeramen.RoundedTransformationBuilder;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

public class ContactsFragment extends ListFragment implements MainActivity.SearchReceiver, ColorVibrateDialog.ContactDetailsUpdateListener, DataFetcher.OnDataFetchedListener, LoaderManager.LoaderCallbacks<Cursor>
{
	//copied ListFragment Constants due to access issue.
	private static final int INTERNAL_EMPTY_ID = 0x00ff0001;
	private static final int INTERNAL_PROGRESS_CONTAINER_ID = 0x00ff0002;
	private static final int INTERNAL_LIST_CONTAINER_ID = 0x00ff0003;

	private static final String CONTACT_DIALOG_TAG = "color_vibrate_dialog";
	/*
	 * Defines an array that contains column names to move from
	 * the Cursor to the ListView.
	 */
	@SuppressLint("InlinedApi")
	private static final String CONTACT_NAME = Build.VERSION.SDK_INT
	>= Build.VERSION_CODES.HONEYCOMB ?
			Contacts.DISPLAY_NAME_PRIMARY :
				Contacts.DISPLAY_NAME;

	private static final String[] FROM_COLUMNS = {
		CONTACT_NAME, CommonDataKinds.Phone.NUMBER,Contacts._ID, Contacts.LOOKUP_KEY,Contacts._ID
	};

	private static final String[] PROJECTION = {
		Contacts._ID,
		Contacts.LOOKUP_KEY,
		CONTACT_NAME,
		CommonDataKinds.Phone.NUMBER,
		CommonDataKinds.Phone.TYPE,
		CommonDataKinds.Phone.CONTACT_ID
	};

	/*
	 * Defines an array that contains resource ids for the layout views
	 * that get the Cursor column contents. The id is pre-defined in
	 * the Android framework, so it is prefaced with "android.R.id"
	 */
	private static final int[] TO_IDS = {
		R.id.contact_name, R.id.contact_number,R.id.contact_vibrate, R.id.contact_display_color, R.id.contact_image
	};

	private static final String TAG = "ContactsFragment";

	private static final String bareQuery = CommonDataKinds.Phone.TYPE + "=?";
	private static final String query = bareQuery +" AND (" + CONTACT_NAME + " LIKE ? OR " + CommonDataKinds.Phone.NUMBER + " LIKE ?)";
	private static final String KEY_CONSTRAINT = "KEY_FILTER";
	private static final int LOADER_ID = 0;

	private SimpleCursorAdapter mCursorAdapter;

	private HashMap <String, LedContactInfo> mLedData;
	private DataFetcher mFetcher;

	private Bundle args = new Bundle();

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		final Transformation transformation = new RoundedTransformationBuilder()
		.borderColor(Color.BLACK)
		.borderWidthDp(0)
		.cornerRadiusDp(30)
		.oval(false)
		.build();
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
				Uri contactUri = Contacts.getLookupUri(cursor.getLong(cursor.getColumnIndex(Phone.CONTACT_ID)), cursor.getString(cursor.getColumnIndex(Contacts.LOOKUP_KEY)));
				switch (view.getId()){
				case R.id.contact_image:
					Picasso.with(getActivity())
					.load(contactUri)
					.placeholder(R.drawable.contact_picture_placeholder)
					.fit()
					.transform(transformation)
					.into((ImageView)view);
					return true;
				case R.id.contact_display_color:
					LedContactInfo info = mLedData.get(contactUri.toString());
					int color = info == null ? Color.GRAY : info.color;
					((CircularColorView)view).setColor(color);
					return true;

				case R.id.contact_vibrate:
					info = mLedData.get(contactUri.toString());
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

		setListAdapter(mCursorAdapter);

		//change space between list items
		ListView listView = getListView();
		listView.setItemsCanFocus(true);
		listView.setDivider(null);
		int dividerSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, getResources().getDisplayMetrics());
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
		Cursor c = mCursorAdapter.getCursor();
		long contactID = c.getLong(c.getColumnIndex(CommonDataKinds.Phone.CONTACT_ID));
		System.out.println ("Clicked on ID " + contactID + " (rowID) " + rowID);
		String name = c.getString(c.getColumnIndex(CONTACT_NAME));
		String number = c.getString(c.getColumnIndex(CommonDataKinds.Phone.NUMBER));
		String lookupValue = /*c.getString(mCursorAdapter.getCursor().getColumnIndex(Contacts.LOOKUP_KEY));*/Contacts.getLookupUri(contactID, c.getString(c.getColumnIndex(Contacts.LOOKUP_KEY))).toString();
		int color = Color.GRAY;
		String vibratePattern = null;
		if (mLedData.get(lookupValue)!=null){
			color = mLedData.get(lookupValue).color;
			vibratePattern = mLedData.get(lookupValue).vibratePattern;
		}

		if (getChildFragmentManager().findFragmentByTag(CONTACT_DIALOG_TAG) == null){
			ColorVibrateDialog.getInstance(name, number, lookupValue,rowID, color,vibratePattern)
			.show(getChildFragmentManager(), CONTACT_DIALOG_TAG);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
		Log.d (TAG,"Creating Loader");
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
		Log.d (TAG,"Load finished");
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
		Log.d(TAG, "Data Fetched");
		mFetcher = null;
		mLedData = fetchedData;
		//Initializes the loader
		if (getActivity() != null){
			getLoaderManager().initLoader(LOADER_ID, null, this);
		}
	}

	@Override
	public void onContactDetailsUpdated(String lookupUri,int color,String vibratePattern) {
		LedContactInfo info = mLedData.get(lookupUri);
		if (color == Color.GRAY && TextUtils.isEmpty(vibratePattern)){
			getActivity().getContentResolver().delete(LedContacts.CONTENT_URI, LedContacts.SYSTEM_CONTACT_LOOKUP_URI + "=?", new String [] {lookupUri});
			System.out.println ("deleting");
			if (info != null){
				mLedData.put(lookupUri, null);
			}
		}
		else {
			if (info == null){
				info = new LedContactInfo();
				info.systemLookupUri = lookupUri;
				mLedData.put(info.systemLookupUri, info);
			}
			info.color = color;
			info.vibratePattern = vibratePattern;
			ContentValues values = new ContentValues();
			if (info.id != -1){
				values.put(LedContacts._ID, info.id);
			}
			values.put(LedContacts.SYSTEM_CONTACT_LOOKUP_URI, lookupUri);
			values.put(LedContacts.COLOR, color);
			values.put(LedContacts.VIBRATE_PATTERN, vibratePattern);
			Uri uri = getActivity().getContentResolver().insert(LedContacts.CONTENT_URI, values);
			info.id = Long.parseLong (uri.getLastPathSegment());
		}
		mCursorAdapter.notifyDataSetChanged();
	}

	@Override
	public void onSearchClosed() {
		getLoaderManager().restartLoader(LOADER_ID, null, ContactsFragment.this);
	}

	@Override
	public void onSearchOpened() {
		// TODO Auto-generated method stub
	}

	@Override
	public void onQueryTextSubmit(String newText) {
		args.putString(KEY_CONSTRAINT, newText);
		getLoaderManager().restartLoader(LOADER_ID, args, ContactsFragment.this);
	}

	@Override
	public void onQueryTextChange(String query) {
		args.putString(KEY_CONSTRAINT, query);
		getLoaderManager().restartLoader(LOADER_ID, args, ContactsFragment.this);
	}
}
