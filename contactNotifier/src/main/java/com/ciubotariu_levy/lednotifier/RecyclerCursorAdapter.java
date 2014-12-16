package com.ciubotariu_levy.lednotifier;

import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.support.v7.widget.RecyclerView;

/**
 * Created by Andrei on 12/11/2014.
 */
public abstract class RecyclerCursorAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private boolean mDataValid;

    private boolean mAutoRequery;

    private Cursor mCursor;

    private Context mContext;

    private int mRowIDColumn;

    private static final String TAG = RecyclerCursorAdapter.class.getName();
//    private ChangeObserver mChangeObserver;

    private DataSetObserver mDataSetObserver;

    //private CusrorFilter mCursorFilter; //future-op
    //private FilterQueryProvider mFilterQueryProvider

    public RecyclerCursorAdapter(Cursor c, String idCol){
        init (c, idCol);
        setHasStableIds(true);
    }

    void init(Cursor c, String idCol) {
        boolean cursorPresent = c != null;
        mCursor = c;
        mDataValid = cursorPresent;
        mRowIDColumn = cursorPresent ? c.getColumnIndexOrThrow(idCol) : -1;
        mDataSetObserver = new MyDataSetObserver();
        mDataSetObserver = null;
        if (cursorPresent) {
            if (mDataSetObserver != null) c.registerDataSetObserver(mDataSetObserver);
        }
    }


    @Override
    public long getItemId (int pos) {
        if (moveToPos(pos) && mRowIDColumn != -1) {
            return mCursor.getLong(mRowIDColumn);
        }

        return RecyclerView.NO_ID;
    }

    public boolean moveToPos (int position) {
        if (mDataValid && mCursor != null) {
            mCursor.moveToPosition(position);
            return true;
        }
        return false;
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int pos) {
        if (moveToPos(pos)) {
            onBind(holder, pos, mCursor);
        }
    }

    public abstract void onBind(RecyclerView.ViewHolder holder, int pos, Cursor cursor);
    /**
     * Change the underlying cursor to a new cursor. If there is an existing cursor it will be
     * closed.
     *
     * @param cursor The new cursor to be used
     */
    public void changeCursor(Cursor cursor, String rowIdColumnName) {
        Cursor old = swapCursor(cursor, rowIdColumnName);
        if (old != null) {
            old.close();
        }
    }

    public Cursor getCursor() {
        return mCursor;
    }
    /**
     * Swap in a new Cursor, returning the old Cursor.  Unlike
     * {@link #changeCursor(Cursor)}, the returned old Cursor is <em>not</em>
     * closed.
     *
     * @param newCursor The new cursor to be used.
     * @return Returns the previously set Cursor, or null if there wasa not one.
     * If the given new Cursor is the same instance is the previously set
     * Cursor, null is also returned.
     */
    private Cursor swapCursor (Cursor newCursor,String rowIdColumnName){
        if (newCursor == mCursor) {
            return null;
        }
        Cursor old = mCursor;
        if (old != null) {
            if (mDataSetObserver != null) old.unregisterDataSetObserver (mDataSetObserver);
        }
        mCursor = newCursor;
        if (newCursor != null) {
            if (mDataSetObserver != null) newCursor.registerDataSetObserver (mDataSetObserver);
            mRowIDColumn = newCursor.getColumnIndexOrThrow (rowIdColumnName);
            mDataValid = true;

            notifyDataSetChanged();
        }
        else {
            mRowIDColumn = -1;
            mDataValid = false;
            notifyDataSetChanged();
        }
        return old;
    }

    @Override
    public int getItemCount() {
        return mDataValid && mCursor != null ? mCursor.getCount() : 0;
    }

    private class MyDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            mDataValid = true;
            notifyDataSetChanged();
        }
        @Override
        public void onInvalidated() {
            mDataValid = false;
            notifyDataSetChanged();
            //notifyDataSetInvalidated();
        }
    }
}
