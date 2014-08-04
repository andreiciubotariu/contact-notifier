package com.ciubotariu_levy.lednotifier;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.widget.AlphabetIndexer;
import android.widget.SectionIndexer;

public class SectionedCursorAdapter extends SimpleCursorAdapter implements SectionIndexer{
	private static final String TAG = SectionedCursorAdapter.class.getName();
	
	private AlphabetIndexer mIndexer;
	private String mAlphabetColumn;
	public SectionedCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags, String alphabetColumn){
		super(context,layout,c,from, to, flags);
		
		mAlphabetColumn = alphabetColumn;
	}

	@Override
	public int getPositionForSection(int sectionIndex) {
		return mIndexer.getPositionForSection(sectionIndex);
	}

	@Override
	public int getSectionForPosition(int position) {
		return mIndexer.getSectionForPosition(position);
	}

	@Override
	public Object[] getSections() {
		return mIndexer.getSections();
	}

	@Override
	public Cursor swapCursor (Cursor c){
		Log.i(TAG, "swapping to " + c);
		if (c != null){
			Log.v(TAG, "Columns: " + c.getColumnCount());
			Log.v(TAG, "Rows: " + c.getCount());
			if (mIndexer != null){
				mIndexer.setCursor(c);
			}
			else {
				mIndexer = new AlphabetIndexer(c, c.getColumnIndex(mAlphabetColumn),
						" 0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ");
			}
		}
		return super.swapCursor(c);
	}		 
}
