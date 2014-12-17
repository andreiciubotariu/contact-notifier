package com.ciubotariu_levy.lednotifier;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ciubotariu_levy.lednotifier.providers.LedContactInfo;
import com.makeramen.RoundedTransformationBuilder;
import com.squareup.picasso.Transformation;

public abstract class AbstractContactsFragment extends Fragment implements MainActivity.SearchReceiver, ColorVibrateDialog.ContactDetailsUpdateListener, LoaderManager.LoaderCallbacks<Cursor>, AbstractRecyclerViewBinder.ContactClickListener{
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
    private RecyclerCursorAdapter mCursorAdapter;

    private Bundle mLoaderArgs = new Bundle();

    protected RecyclerCursorAdapter getCursorAdapter(){
        return mCursorAdapter;
    }


    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
        final Transformation transformation = new RoundedTransformationBuilder()
                .borderColor(Color.GRAY)
                .borderWidthDp(1)
                .cornerRadiusDp(30)
                .oval(false)
                .build();
        final AbstractRecyclerViewBinder viewBinder = getViewBinder(transformation);

        mCursorAdapter = new RecyclerCursorAdapter(null,"_id") {

            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

                // create a new view
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.contact_row, parent, false);
                // set the view's size, margins, paddings and layout parameters
                AbstractRecyclerViewBinder.ContactHolder vh = new AbstractRecyclerViewBinder.ContactHolder(v);
                return vh;
            }

            @Override
            public void onBind(RecyclerView.ViewHolder holder, int pos, Cursor cursor) {
                System.out.println (holder.getPosition() + " ... "  + pos);
                viewBinder.bind(holder, cursor, getActivity());
            }
        };
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Gets a CursorAdapter
//        mCursorAdapter = new SectionedCursorAdapter(
//                getActivity(),
//                R.layout.contact_row,
//                null,
//                getFromColumns(),
//                TO_IDS,
//                0, CONTACT_NAME);
//        mCursorAdapter.setViewBinder(getViewBinder(transformation));

        //setListAdapter(mCursorAdapter);

        //change space between list items
//        ListView listView = getListView();
//        listView.setItemsCanFocus(true);
//        int dividerSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
//        listView.setDividerHeight(dividerSize);
//        listView.setCacheColorHint(Color.TRANSPARENT);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_contact_list_default, container, false);
    }

    @Override
    public void onViewCreated (View view, Bundle savedInstanceState) {
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.contact_list);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        recyclerView.setAdapter(mCursorAdapter);

        listSetupComplete();
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
        mCursorAdapter.changeCursor(cursor, getRowIDColumn());
//        getListView().setFastScrollEnabled(true);
//        setEmptyText("Add custom contacts. Choose \'All Mobile\'");
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.i(TAG, "Loader reset");
        mCursorAdapter.changeCursor(null, getRowIDColumn());
    }

    @Override //TODO: figure out if this needs to be subimplemented
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
        Log.d (TAG,"Creating Loader");
//        getListView().setFastScrollEnabled(false);
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

    protected abstract AbstractRecyclerViewBinder getViewBinder(Transformation transformation);
    protected abstract void listSetupComplete();
    protected abstract String[] filteredSelectionArgs(String constraint);
    protected abstract Uri getContentUri();
    protected abstract String getSortColumn();
    protected abstract String getRowIDColumn();

    @Override
    public abstract void onContactSelected(int position, long id);

    @Override
    public abstract void onContactDetailsUpdated(LedContactInfo newData);
}
