package com.ciubotariu_levy.lednotifier;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.support.v4.app.Fragment;
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

public class CustomContactsFragment extends ListFragment implements MainActivity.SearchReceiver, ColorVibrateDialog.ContactDetailsUpdateListener, LoaderManager.LoaderCallbacks<Cursor>
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
		LedContacts.LAST_KNOWN_NAME, LedContacts.LAST_KNOWN_NUMBER,LedContacts.RINGTONE_URI,LedContacts.VIBRATE_PATTERN, LedContacts.COLOR ,LedContacts.SYSTEM_CONTACT_LOOKUP_URI
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

	/*
	 * Defines an array that contains resource ids for the layout views
	 * that get the Cursor column contents. The id is pre-defined in
	 * the Android framework, so it is prefaced with "android.R.id"
	 */
	private static final int[] TO_IDS = {
		R.id.contact_name, R.id.contact_number,R.id.contact_ringtone,R.id.contact_vibrate, R.id.contact_display_color, R.id.contact_image
	};

	private static final String TAG = "CustomContactsFragment";
	private static final String filterQuery = LedContacts.LAST_KNOWN_NAME + " LIKE ? OR " + LedContacts.LAST_KNOWN_NUMBER + " LIKE ?";
	private static final String KEY_CONSTRAINT = "KEY_FILTER";
	private static final int LOADER_ID = 1;

	private SimpleCursorAdapter mCursorAdapter;

	private Bundle mLoaderArgs = new Bundle();

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
				0, LedContacts.LAST_KNOWN_NAME);
		mCursorAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
			@Override
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				Uri contactUri = Uri.parse(cursor.getString(cursor.getColumnIndex(LedContacts.SYSTEM_CONTACT_LOOKUP_URI)));
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
					int color = cursor.getInt(cursor.getColumnIndex(LedContacts.COLOR));
					((CircularColorView)view).setColor(color);
					return true;
				case R.id.contact_ringtone:
					String ringtone = cursor.getString(cursor.getColumnIndex(LedContacts.RINGTONE_URI));
					if (!TextUtils.isEmpty(ringtone) && !ColorVibrateDialog.GLOBAL.equals(ringtone)){
						view.setVisibility(View.VISIBLE);
						view.setBackgroundResource(R.drawable.ic_custom_ringtone);
					}
					else {
						view.setVisibility(View.GONE);
					}
					return true;
				case R.id.contact_vibrate:
					String vibratePattern = cursor.getString(cursor.getColumnIndex(LedContacts.VIBRATE_PATTERN));
					if (!TextUtils.isEmpty(vibratePattern)){
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
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		getLoaderManager().initLoader(LOADER_ID, null, this);
	}

	@Override
	public void onListItemClick(ListView l, View item, int position, long rowID) {
		LedContactInfo data = new LedContactInfo();
		data.id = rowID;
		Cursor c = mCursorAdapter.getCursor();
		data.lastKnownName = c.getString(c.getColumnIndex(LedContacts.LAST_KNOWN_NAME));
		data.lastKnownNumber = c.getString(c.getColumnIndex(LedContacts.LAST_KNOWN_NUMBER));
		data.systemLookupUri = c.getString(c.getColumnIndex(LedContacts.SYSTEM_CONTACT_LOOKUP_URI));
		data.color = c.getInt(c.getColumnIndex(LedContacts.COLOR));
		data.vibratePattern = c.getString(c.getColumnIndex(LedContacts.VIBRATE_PATTERN));
		data.ringtoneUri = c.getString(c.getColumnIndex(LedContacts.RINGTONE_URI));
		if (TextUtils.isEmpty(data.ringtoneUri)){
			data.ringtoneUri = ColorVibrateDialog.GLOBAL;
		}
		if (getChildFragmentManager().findFragmentByTag(CONTACT_DIALOG_TAG) == null){
			ColorVibrateDialog.getInstance(data)
			.show(getChildFragmentManager(), CONTACT_DIALOG_TAG);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Fragment child = getChildFragmentManager().findFragmentByTag(CONTACT_DIALOG_TAG);
		if (child != null){
			child.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
		getListView().setFastScrollEnabled(false);
		String constraint = "";
		if (args != null && args.getString(KEY_CONSTRAINT) != null){
			constraint = args.getString(KEY_CONSTRAINT);
		}

		String [] filteredSelectionArgs = new String [] {"%"+constraint+"%","%"+constraint+"%"};

		return new CursorLoader(
				getActivity(),
				LedContacts.CONTENT_URI,
				PROJECTION,
				filterQuery,
				filteredSelectionArgs,
				LedContacts.LAST_KNOWN_NAME + " ASC");
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mCursorAdapter.swapCursor(cursor);
		getListView().setFastScrollEnabled(true);
		setEmptyText("Add custom contacts. Choose \'All Mobile\'");
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		Log.i(TAG, "Loader reset");
		mCursorAdapter.swapCursor(null);
	}

	@Override
	public void onContactDetailsUpdated(LedContactInfo newData) {
		if (newData.color == Color.GRAY && TextUtils.isEmpty(newData.vibratePattern) && (TextUtils.isEmpty(newData.ringtoneUri) || ColorVibrateDialog.GLOBAL.equals(newData.ringtoneUri))){
			getActivity().getContentResolver().delete(Uri.withAppendedPath(LedContacts.CONTENT_URI, String.valueOf(newData.id)), null, null);
			System.out.println ("deleting");
		}
		else {
			ContentValues values = new ContentValues();
			values.put(LedContacts.COLOR, newData.color);
			values.put(LedContacts.VIBRATE_PATTERN, newData.vibratePattern);
			values.put(LedContacts.RINGTONE_URI, newData.ringtoneUri);
			getActivity().getContentResolver().update(Uri.withAppendedPath(LedContacts.CONTENT_URI, String.valueOf(newData.id)), values,null, null);
		}
	}

	@Override
	public void onSearchClosed() {
		mLoaderArgs.remove(KEY_CONSTRAINT);
		getLoaderManager().restartLoader(LOADER_ID, null, CustomContactsFragment.this);
	}

	@Override
	public void onSearchOpened() {
		mLoaderArgs.remove(KEY_CONSTRAINT);
	}

	@Override
	public void onQueryTextSubmit(String newText) {
		mLoaderArgs.putString(KEY_CONSTRAINT, newText);
		getLoaderManager().restartLoader(LOADER_ID, mLoaderArgs, CustomContactsFragment.this);
	}

	@Override
	public void onQueryTextChange(String query) {
		mLoaderArgs.putString(KEY_CONSTRAINT, query);
		getLoaderManager().restartLoader(LOADER_ID, mLoaderArgs, CustomContactsFragment.this);
	}
}
