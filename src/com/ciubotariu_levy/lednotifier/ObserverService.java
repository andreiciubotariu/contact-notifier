package com.ciubotariu_levy.lednotifier;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
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
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.Telephony.Sms;
import android.util.Log;

import com.ciubotariu_levy.lednotifier.providers.LedContacts;

@TargetApi(19)
public class ObserverService extends Service {
	private Handler mHandler = new Handler();

	@SuppressLint("InlinedApi")
	private static final String CONTACT_NAME = Build.VERSION.SDK_INT
	>= Build.VERSION_CODES.HONEYCOMB ?
			Contacts.DISPLAY_NAME_PRIMARY :
				Contacts.DISPLAY_NAME;

	private static final String[] PROJECTION = {
		Contacts._ID,
		Contacts.LOOKUP_KEY,
		CONTACT_NAME,
		CommonDataKinds.Phone.NUMBER
	};


	private int mNumContacts;
	private ContactsChangeChecker mChecker;
	ContentObserver mContactContentObserver = new ContentObserver (mHandler){
		@Override
		public void onChange (boolean selfChange){
			onChange(selfChange, null);
		}

		@Override
		public void onChange (boolean selfChange, Uri uri){
			System.out.println ("changed " + uri);
			int newNumContacts = getNumContacts(mNumContacts);

			mNumContacts = newNumContacts;
			if (mChecker != null && !mChecker.isCancelled()){
				mChecker.cancel(true);
			}
			mChecker = new ContactsChangeChecker();
			mChecker.execute();
		}
	};

	int mUnread;
	int mUnseen;
	boolean registeredObserver = false;
	static final Uri SMS_CONTENT_URI =  Build.VERSION.SDK_INT	>= Build.VERSION_CODES.KITKAT ? Sms.CONTENT_URI : Uri.parse("content://sms/");
	static final Uri INBOX_URI =  Build.VERSION.SDK_INT	>= Build.VERSION_CODES.KITKAT ? Sms.Inbox.CONTENT_URI : Uri.parse("content://sms/inbox/");
	static final String READ =  Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? Sms.Inbox.READ : "read";	
	static final String SEEN =  Build.VERSION.SDK_INT 	>= Build.VERSION_CODES.KITKAT ? Sms.Inbox.SEEN : "seen";

	private static final String TAG = "ObserverService";	

	ContentObserver mSMSContentObserver = new ContentObserver (mHandler){
		@Override
		public void onChange (boolean selfChange){
			onChange(selfChange, null);
		}

		@Override
		public void onChange (boolean selfChange, Uri uri){
			int unseen = getUnseenSms();
			int unread = getUnreadSms();

			Log.i (TAG,"Stats " + unseen+ "|" + unread);
			if (unseen < mUnseen || unread < mUnread){
				NotificationUtils.cancel(ObserverService.this);
			}
			mUnseen = unseen;
			mUnread = unread;
		}
	};

	@Override
	public void onCreate(){
		super.onCreate();
		mNumContacts = getNumContacts(-1);
		getContentResolver().registerContentObserver(CommonDataKinds.Phone.CONTENT_URI, true, mContactContentObserver);
		try{
			mUnread = getUnreadSms();
			mUnseen = getUnseenSms();
			getContentResolver().registerContentObserver(SMS_CONTENT_URI, true, mSMSContentObserver);
			registeredObserver = true;
			Log.i(TAG,"Registered observer " + mUnseen +"|" + mUnread);
		}catch (Exception e){ //sms inbox not standardized on jellybean and older
			e.printStackTrace();
			throw new RuntimeException();
		}
		mChecker = new ContactsChangeChecker();
		mChecker.execute();
	}

	@Override
	public void onDestroy(){
		getContentResolver().unregisterContentObserver(mContactContentObserver);
		if (registeredObserver){
			getContentResolver().unregisterContentObserver(mSMSContentObserver);
			registeredObserver = false;
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	private int getUnreadSms(){
		return getSmsBasedOnProperty(READ, mUnread);
	}

	private int getUnseenSms(){
		return getSmsBasedOnProperty(SEEN, mUnseen);
	}

	private int getSmsBasedOnProperty (String prop, int currentCount){
		int count = currentCount;
		Cursor c = getContentResolver().query(INBOX_URI, null, prop +"=?", new String[]{"0"},null);
		if (c != null){
			count = c.getCount();
			c.close();
		}
		return count;
	}

	private int getNumContacts (int currentNum){
		int numContacts = currentNum;
		Cursor c = getContentResolver().query(CommonDataKinds.Phone.CONTENT_URI, 
				PROJECTION, 
				null,
				null, //watch for all contact changes, not just ones with a Phone.TYPE_MOBILE number 
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
			String [] projection = new String [] {LedContacts._ID, LedContacts.SYSTEM_CONTACT_LOOKUP_URI, LedContacts.LAST_KNOWN_NAME, LedContacts.LAST_KNOWN_NUMBER};
			ContentResolver resolver = getContentResolver();
			Cursor customContactsCursor = resolver.query(LedContacts.CONTENT_URI, projection, null, null,null);
			if (customContactsCursor != null && customContactsCursor.moveToFirst()){
				do {
					if (isCancelled()){
						break;
					}
					int id = customContactsCursor.getInt(customContactsCursor.getColumnIndex(LedContacts._ID));
					String systemLookupUri = customContactsCursor.getString(customContactsCursor.getColumnIndex(LedContacts.SYSTEM_CONTACT_LOOKUP_URI));

					Uri lookupUri = Uri.parse(systemLookupUri);
					System.out.println ("Parsed URI is " + lookupUri);
					Uri newLookupUri = ContactsContract.Contacts.getLookupUri(resolver, lookupUri);
					if (/*ContactsContract.Contacts.lookupContact(resolver, lookupUri)*/ newLookupUri== null){
						System.out.println ("deleting contact from our db");
						toDelete.add(String.valueOf(id));
					} else {
						ContentValues values = new ContentValues();
						boolean needsUpdating = false;

						if (newLookupUri != null && !newLookupUri.equals(lookupUri)){
							System.out.println("Different URIs now. Must update our DB");
							values.put(LedContacts.SYSTEM_CONTACT_LOOKUP_URI, newLookupUri.toString());
							needsUpdating = true;
						} 
						
						String contactId = newLookupUri.getLastPathSegment();
						System.out.println ("Path segments: " + newLookupUri.getPathSegments());
						Cursor contactNameCursor = getContentResolver().query(Phone.CONTENT_URI, new String [] {CONTACT_NAME, Phone.LOOKUP_KEY, Phone.CONTACT_ID, Phone.NUMBER, Phone.TYPE},Phone.CONTACT_ID + "=?", new String[] {contactId} , null);
						if (contactNameCursor != null && contactNameCursor.moveToFirst()){
							String name = contactNameCursor.getString(contactNameCursor.getColumnIndex(CONTACT_NAME));
							if (!name.equals(customContactsCursor.getString(customContactsCursor.getColumnIndex(LedContacts.LAST_KNOWN_NAME)))){
								System.out.println ("Name change");
								values.put(LedContacts.LAST_KNOWN_NAME, name);	
								needsUpdating = true;
							}
							
							String phoneNumber = contactNameCursor.getString(contactNameCursor.getColumnIndex(Phone.NUMBER));
							if (!phoneNumber.equals(customContactsCursor.getString(customContactsCursor.getColumnIndex(LedContacts.LAST_KNOWN_NUMBER)))){
								System.out.println ("Number change");
								values.put(LedContacts.LAST_KNOWN_NUMBER, phoneNumber);
								needsUpdating = true;
							}
							
							if (contactNameCursor.getInt(contactNameCursor.getColumnIndex(Phone.TYPE)) != Phone.TYPE_MOBILE){
								Log.i(TAG,"Not a mobile number, deletion pending");
								needsUpdating = false;
								toDelete.add(String.valueOf(id));
							}
						}
						if (contactNameCursor != null){
							contactNameCursor.close();
						}
						
						if (needsUpdating){
							System.out.println ("updating database...");
							Uri updateUri = Uri.withAppendedPath(LedContacts.CONTENT_URI, String.valueOf(id));
							resolver.update(updateUri, values, null, null);
						}

					}

				}
				while (customContactsCursor.moveToNext());
				customContactsCursor.close();
			}
			resolver.delete(LedContacts.CONTENT_URI, LedContacts._ID + " IN " + generateSelectionMarks(toDelete.size()), toDelete.toArray(new String[toDelete.size()]));

			return null;
		}

		private String generateSelectionMarks (int amount){
			StringBuilder s = new StringBuilder("(");
			for (int x = 0; x < amount-1; x++){
				s.append("?, ");
			}
			if (amount > 0){
				s.append ("?");
			}
			s.append (")");
			return s.toString();
		}
	}
}
