package com.ciubotariu_levy.lednotifier;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ciubotariu_levy.lednotifier.providers.LedContactInfo;
import com.makeramen.RoundedTransformationBuilder;
import com.squareup.picasso.Transformation;

public abstract class AbstractContactsFragment extends ListFragment implements MainActivity.SearchReceiver, ColorVibrateDialog.ContactDetailsUpdateListener, LoaderManager.LoaderCallbacks<Cursor>{
    //copied ListFragment Constants due to access issue.
    private static final int INTERNAL_EMPTY_ID = 0x00ff0001;
    private static final int INTERNAL_PROGRESS_CONTAINER_ID = 0x00ff0002;
    private static final int INTERNAL_LIST_CONTAINER_ID = 0x00ff0003;

    @SuppressLint("InlinedApi")
    public static final String CONTACT_NAME = Build.VERSION.SDK_INT
            >= Build.VERSION_CODES.HONEYCOMB ?
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY :
            ContactsContract.Contacts.DISPLAY_NAME;

    protected abstract String[] getFromColumns();
    protected abstract String[] getProjection();
    protected static final String CONTACT_DIALOG_TAG = "color_vibrate_dialog";

    private static final int[] TO_IDS = {
            R.id.contact_name, R.id.contact_number,R.id.contact_ringtone,R.id.contact_vibrate, R.id.contact_display_color, R.id.contact_image
    };

    private String TAG = "AbsContactsFrag";

    protected abstract String getBareQuery();
    protected abstract String getQuery();

    protected abstract int getLoaderId();

    private static final String KEY_CONSTRAINT = "KEY_FILTER";
    private SimpleCursorAdapter mCursorAdapter;

    private Bundle mLoaderArgs = new Bundle();

    protected CursorAdapter getCursorAdapter(){
        return mCursorAdapter;
    }
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final Transformation transformation = new RoundedTransformationBuilder()
                .borderColor(Color.GRAY)
                .borderWidthDp(1)
                .cornerRadiusDp(30)
                .oval(false)
                .build();
        // Gets a CursorAdapter
        mCursorAdapter = new SectionedCursorAdapter(
                getActivity(),
                R.layout.contact_row,
                null,
                getFromColumns(),
                TO_IDS,
                0, CONTACT_NAME);
        mCursorAdapter.setViewBinder(getViewBinder(transformation));

        setListAdapter(mCursorAdapter);

        //change space between list items
        ListView listView = getListView();
        listView.setItemsCanFocus(true);
        int dividerSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
        listView.setDividerHeight(dividerSize);
        listView.setCacheColorHint(Color.TRANSPARENT);

        listSetupComplete();
    }

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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Fragment child = getChildFragmentManager().findFragmentByTag(CONTACT_DIALOG_TAG);
        if (child != null){
            child.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onSearchClosed() {
        mLoaderArgs.remove(KEY_CONSTRAINT);
        getLoaderManager().restartLoader(getLoaderId(), null, this);
    }

    @Override
    public void onSearchOpened() {
        mLoaderArgs.remove(KEY_CONSTRAINT);
    }

    @Override
    public void onQueryTextSubmit(String newText) {
        mLoaderArgs.putString(KEY_CONSTRAINT, newText);
        getLoaderManager().restartLoader(getLoaderId(), mLoaderArgs, this);
    }

    @Override
    public void onQueryTextChange(String query) {
        mLoaderArgs.putString(KEY_CONSTRAINT, query);
        getLoaderManager().restartLoader(getLoaderId(), mLoaderArgs, this);
    }

    @Override
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

    @Override //TODO: figure out if this needs to be subimplemented
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
        Log.d (TAG,"Creating Loader");
        getListView().setFastScrollEnabled(false);
        String constraint = "";
        if (args != null && args.getString(KEY_CONSTRAINT) != null){
            constraint = args.getString(KEY_CONSTRAINT);
        }

        return new CursorLoader(
                getActivity(),
                getContentUri(),
                getProjection(),
                getQuery(),
                filteredSelectionArgs(constraint),
                getSortColumn() + " ASC");
    }

    protected abstract AbstractViewBinder getViewBinder(Transformation transformation);
    protected abstract void listSetupComplete();
    protected abstract String[] filteredSelectionArgs(String constraint);
    protected abstract Uri getContentUri();
    protected abstract String getSortColumn();

    @Override
    public abstract void onContactDetailsUpdated(LedContactInfo newData);

    @Override
    public abstract void onListItemClick(ListView l, View item, int position, long rowID);
}
