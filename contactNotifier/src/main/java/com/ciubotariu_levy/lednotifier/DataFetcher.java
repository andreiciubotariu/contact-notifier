package com.ciubotariu_levy.lednotifier;

import java.util.HashMap;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import com.ciubotariu_levy.lednotifier.providers.LedContactInfo;
import com.ciubotariu_levy.lednotifier.providers.LedContacts;

public class DataFetcher extends
		AsyncTask<Context, Void, HashMap<String,LedContactInfo>> {
	
	public interface OnDataFetchedListener {
		public void onDataFetched (HashMap <String, LedContactInfo> fetchedData);
	}
	
	private Uri mUri;
	private OnDataFetchedListener mListener;

	public DataFetcher (OnDataFetchedListener listener, Uri uri){
		mListener = listener;
		mUri = uri;
	}
	
	@Override
	protected HashMap<String, LedContactInfo> doInBackground(Context... params) {
		HashMap <String, LedContactInfo> map = new HashMap <String,LedContactInfo> ();
		if (params [0] == null){
			return map;
		}
		String [] projection = new String [] {LedContacts._ID, LedContacts.SYSTEM_CONTACT_LOOKUP_URI, LedContacts.LAST_KNOWN_NAME, LedContacts.LAST_KNOWN_NUMBER, LedContacts.COLOR, LedContacts.VIBRATE_PATTERN,
				LedContacts.RINGTONE_URI};
		Cursor c = params[0].getContentResolver().query(mUri, projection, null, null,null);
		if (c != null && c.moveToFirst()){
			do {
				LedContactInfo info = new LedContactInfo();
				info.id = c.getInt(c.getColumnIndex(LedContacts._ID));
				info.systemLookupUri = c.getString(c.getColumnIndex(LedContacts.SYSTEM_CONTACT_LOOKUP_URI));
				info.lastKnownName = c.getString (c.getColumnIndex(LedContacts.LAST_KNOWN_NAME));
				info.lastKnownNumber = c.getString(c.getColumnIndex(LedContacts.LAST_KNOWN_NUMBER));
				info.color = c.getInt(c.getColumnIndex(LedContacts.COLOR));
				info.vibratePattern = c.getString(c.getColumnIndex(LedContacts.VIBRATE_PATTERN));
				info.ringtoneUri = c.getString(c.getColumnIndex(LedContacts.RINGTONE_URI));
				if (info.ringtoneUri != null && info.ringtoneUri.equalsIgnoreCase("null")){
					info.ringtoneUri = null;
				}
				map.put (String.valueOf(info.systemLookupUri),info);
			}
			while (c.moveToNext());
			c.close();
		}
		return map;
	}
	
	@Override
	protected void onPostExecute (HashMap <String, LedContactInfo> map){
		if (mListener != null){
			mListener.onDataFetched(map);
		}
	}

}
