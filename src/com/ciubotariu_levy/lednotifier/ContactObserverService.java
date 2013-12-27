package com.ciubotariu_levy.lednotifier;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;

import com.ciubotariu_levy.lednotifier.providers.LedContacts;

public class ContactObserverService extends Service {

	@SuppressLint("InlinedApi")
	private final static String CONTACT_NAME = Build.VERSION.SDK_INT
	>= Build.VERSION_CODES.HONEYCOMB ?
			Contacts.DISPLAY_NAME_PRIMARY :
				Contacts.DISPLAY_NAME;

	private static final String[] PROJECTION = {
		Contacts._ID,
		Contacts.LOOKUP_KEY,
		CONTACT_NAME,
		CommonDataKinds.Phone.NUMBER
	};


	int mNumContacts;
	ContentObserver mContentObserver = new ContentObserver (new Handler()){
		@Override
		public void onChange (boolean selfChange){
			super.onChange(selfChange);
			int newNumContacts = getNumContacts(mNumContacts);
			
			if (mNumContacts != newNumContacts){
				mNumContacts = newNumContacts;
				new ContactsChangeChecker().execute();
			}
		}
	};

	@Override
	public void onCreate(){
		super.onCreate();
		mNumContacts = getNumContacts(-1);
		getContentResolver().registerContentObserver(CommonDataKinds.Phone.CONTENT_URI, true, mContentObserver);
	}
	
	@Override
	public void onDestroy(){
		getContentResolver().unregisterContentObserver(mContentObserver);
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	private int getNumContacts (int currentNum){
		int numContacts = currentNum;
		Cursor c = getContentResolver().query(CommonDataKinds.Phone.CONTENT_URI, 
				PROJECTION, 
				null,
				null, //watch for all contact changes, not just ones with a mobile number 
				CONTACT_NAME + " ASC");
		if (c != null){
			numContacts = c.getCount();
			c.close();
		}
		return numContacts;
	}

	private class ContactsChangeChecker extends AsyncTask <Void,Void,Void>{

		@Override
		protected Void doInBackground(Void... params) {
			List <String> toDelete = new ArrayList <String> ();
			String [] projection = new String [] {LedContacts._ID, LedContacts.SYSTEM_CONTACT_ID};
			ContentResolver resolver = getContentResolver();
			Cursor c = resolver.query(LedContacts.CONTENT_URI, projection, null, null,null);
			if (c != null && c.moveToFirst()){
				do {
					int id = c.getInt(c.getColumnIndex(LedContacts._ID));
					String systemLookupKey = c.getString(c.getColumnIndex(LedContacts.SYSTEM_CONTACT_ID));
					
					Uri lookupUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI,systemLookupKey);
					if (ContactsContract.Contacts.lookupContact(resolver, lookupUri) == null){
						toDelete.add(String.valueOf(id));
					}
				}
				while (c.moveToNext());
				c.close();
			}
			System.out.println (resolver.delete(LedContacts.CONTENT_URI, LedContacts._ID + " IN " + generateSelectionMarks(toDelete.size()), toDelete.toArray(new String[toDelete.size()])));
			
			return null;
		}

		private String generateSelectionMarks (int amount){
			StringBuilder s = new StringBuilder("(");
			for (int x=0; x<amount-1; x++){
				s.append("?, ");
			}
			if (amount>0){
				s.append ("?");
			}
			s.append (")");
			return s.toString();
		}
	}
}
