package com.ciubotariu_levy.lednotifier;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.v4.widget.SimpleCursorAdapter;
import android.widget.AlphabetIndexer;
import android.widget.SectionIndexer;

public class SectionedCursorAdapter extends SimpleCursorAdapter implements SectionIndexer{
	
	private AlphabetIndexer mIndexer;
	
	public SectionedCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags){
		super(context,layout,c,from, to, flags);
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
		System.out.println (mIndexer == null);
		return mIndexer.getSections();
	}
	
	@Override
	public Cursor swapCursor (Cursor c){
		if (c != null){
			mIndexer = new AlphabetIndexer(c, c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME),
	                " ABCDEFGHIJKLMNOPQRSTUVWXYZ");
		}
		return super.swapCursor(c);
	}
			 
}
