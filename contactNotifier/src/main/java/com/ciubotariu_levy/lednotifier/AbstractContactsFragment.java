package com.ciubotariu_levy.lednotifier;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
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

public abstract class AbstractContactsFragment extends Fragment implements MainActivity.SearchReceiver, ColorVibrateDialog.ContactDetailsUpdateListener, LoaderManager.LoaderCallbacks<Cursor>, AbstractContactViewBinder.ContactListener {
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
    private static final String LIST_STATE = "list_state";
    private Parcelable mListState;
    private RecyclerView.LayoutManager layoutManager;

    private Bundle mLoaderArgs = new Bundle();

    private AbstractContactViewBinder viewBinder;

    protected RecyclerCursorAdapter getCursorAdapter(){
        return mCursorAdapter;
    }

    public static RecyclerView r;
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
        viewBinder = getViewBinder(transformation);

        mCursorAdapter = new RecyclerCursorAdapter(null,"_id") {

            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

                // create a new view
                View v = LayoutInflater.from(parent.getContext()).inflate(getRowResID(), parent, false);

                AbstractContactViewBinder.ContactHolder vh = new AbstractContactViewBinder.ContactHolder(v,viewBinder.hasColorView());
                return vh;
            }

            @Override
            public void onBind(RecyclerView.ViewHolder holder, int pos, Cursor cursor) {
                viewBinder.bind(holder, cursor, getActivity());
            }
        };
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mListState = savedInstanceState.getParcelable(LIST_STATE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_contact_list_default, container, false);
    }

    @Override
    public void onViewCreated (View view, Bundle savedInstanceState) {
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.contact_list);

        r = recyclerView;
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(false);

        // use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        recyclerView.setAdapter(mCursorAdapter);

        listSetupComplete();
    }



    @Override
    public void startForResult(Intent intent, int requestCode) {
        startActivityForResult(intent,requestCode);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        Fragment child = getChildFragmentManager().findFragmentByTag(CONTACT_DIALOG_TAG);
//        if (child != null){
//            child.onActivityResult(requestCode, resultCode, data);
//        }
        viewBinder.onResult(requestCode, resultCode, data);
    }

    @Override
    public void onSaveInstanceState (Bundle outState) {
        if (layoutManager != null) {
            mListState = layoutManager.onSaveInstanceState();
        }
        if (mListState != null) {
            outState.putParcelable(LIST_STATE, mListState);
        }
        super.onSaveInstanceState(outState);
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
        View emptyText = getView().findViewById(R.id.empty_text);
        View list = getView().findViewById(R.id.contact_list);
        if (emptyText != null && list != null) {
            if (mCursorAdapter.getItemCount() <= 0) {
                 emptyText.setVisibility(View.VISIBLE);
                 list.setVisibility(View.GONE);
            } else {
                 emptyText.setVisibility(View.GONE);
                 list.setVisibility(View.VISIBLE);
            }
        }

        RecyclerView r = (RecyclerView) getView().findViewById(R.id.contact_list);
        layoutManager = r.getLayoutManager();
        if (mListState != null) {
            System.out.println (mListState);
            layoutManager.onRestoreInstanceState(mListState);
            mListState = null;
        }
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

    protected abstract AbstractContactViewBinder getViewBinder(Transformation transformation);
    protected abstract void listSetupComplete();
    protected abstract String[] filteredSelectionArgs(String constraint);
    protected abstract Uri getContentUri();
    protected abstract String getSortColumn();
    protected abstract String getRowIDColumn();
    protected abstract int getRowResID();

    @Override
    public abstract LedContactInfo onContactSelected(int position, long id);

    @Override
    public abstract void onContactDetailsUpdated(LedContactInfo newData);
}
