package com.ciubotariu_levy.lednotifier;

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
import android.provider.ContactsContract.Contacts;
import android.provider.Telephony.Sms;

import com.ciubotariu_levy.lednotifier.providers.LedContacts;

import java.util.ArrayList;
import java.util.List;

@TargetApi(19)
public class ObserverService extends Service {
	private static final String TAG = ObserverService.class.getName();	

	static final Uri SMS_CONTENT_URI =  Build.VERSION.SDK_INT	>= Build.VERSION_CODES.KITKAT ? Sms.CONTENT_URI : Uri.parse("content://sms/");
	static final Uri INBOX_URI =  Build.VERSION.SDK_INT	>= Build.VERSION_CODES.KITKAT ? Sms.Inbox.CONTENT_URI : Uri.parse("content://sms/inbox/");
	static final String READ =  Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? Sms.Inbox.READ : "read";	
	static final String SEEN =  Build.VERSION.SDK_INT 	>= Build.VERSION_CODES.KITKAT ? Sms.Inbox.SEEN : "seen";

	@SuppressLint("InlinedApi")
	private static final String CONTACT_NAME = Build.VERSION.SDK_INT
	>= Build.VERSION_CODES.HONEYCOMB ?
			Contacts.DISPLAY_NAME_PRIMARY :
				Contacts.DISPLAY_NAME;


	private static final String [] CONTACT_PROJ = new String [] {
		CONTACT_NAME, 
		Contacts.LOOKUP_KEY,
		Contacts.HAS_PHONE_NUMBER,
    };

	private static final String [] LED_CONTACTS_PROJ = new String [] {
		LedContacts._ID, 
		LedContacts.SYSTEM_CONTACT_LOOKUP_URI,
		LedContacts.LAST_KNOWN_NAME,
		LedContacts.LAST_KNOWN_NUMBER};

	private int mUnread;
	private int mUnseen;
	private ContactsChangeChecker mChecker;
	private Handler mHandler = new Handler();
	private boolean registeredObserver = false;

	private ContentObserver mContactContentObserver = new ContentObserver (mHandler){
		@Override
		public void onChange (boolean selfChange){
			onChange(selfChange, null);
		}

		@Override
		public void onChange (boolean selfChange, Uri uri){
			if (mChecker != null && !mChecker.isCancelled()){
				mChecker.cancel(true);
			}
			mChecker = new ContactsChangeChecker();
			mChecker.execute();
		}
	};

	private ContentObserver mSMSContentObserver = new ContentObserver (mHandler){
		@Override
		public void onChange (boolean selfChange){
			onChange(selfChange, null);
		}

		@Override
		public void onChange (boolean selfChange, Uri uri){
			int unseen = getUnseenSms();
			int unread = getUnreadSms();

//			Log.v(TAG, "**********************************************");
//			Log.v (TAG,"Current stats: Unseen - " + unseen+ "| Unread - " + unread);
//			Log.v (TAG,"Prev stats: Unseen - " + mUnseen+ "| Unread - " + mUnread);
//			Log.v(TAG, "**********************************************");
			if (unseen < mUnseen || unread < mUnread){
				NotificationUtils.cancel(ObserverService.this);
				unseen = 0;
				unread = 0;
			}
			mUnseen = unseen;
			mUnread = unread;
		}
	};

	@Override
	public void onCreate(){
		super.onCreate();
		getContentResolver().registerContentObserver(Contacts.CONTENT_URI, true, mContactContentObserver);
		try{
			mUnread = getUnreadSms();
			mUnseen = getUnseenSms();
			getContentResolver().registerContentObserver(SMS_CONTENT_URI, true, mSMSContentObserver);
			registeredObserver = true;
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

	private class ContactsChangeChecker extends AsyncTask <Void,Void,Void>{

		@Override
		protected Void doInBackground(Void... params) {
			List <String> toDelete = new ArrayList <String> ();
			ContentResolver resolver = getContentResolver();
			Cursor customContactsCursor = resolver.query(LedContacts.CONTENT_URI, LED_CONTACTS_PROJ, null, null,null);
			if (customContactsCursor != null && customContactsCursor.moveToFirst()){
				do {
					if (isCancelled()){
						break;
					}
					int id = customContactsCursor.getInt(customContactsCursor.getColumnIndex(LedContacts._ID));
					String systemLookupUri = customContactsCursor.getString(customContactsCursor.getColumnIndex(LedContacts.SYSTEM_CONTACT_LOOKUP_URI));

					Uri lookupUri = Uri.parse(systemLookupUri);
					Uri newLookupUri = ContactsContract.Contacts.getLookupUri(resolver, lookupUri);
					if (newLookupUri == null){
						toDelete.add(String.valueOf(id));
					} else {
						ContentValues values = new ContentValues();
						boolean needsUpdating = false;

						if (newLookupUri != null && !newLookupUri.equals(lookupUri)){
							values.put(LedContacts.SYSTEM_CONTACT_LOOKUP_URI, newLookupUri.toString());
							needsUpdating = true;
						} 

						Cursor contactNameCursor = getContentResolver().query(newLookupUri, CONTACT_PROJ,null,null , null);
						if (contactNameCursor != null && contactNameCursor.moveToFirst()){
							String name = contactNameCursor.getString(contactNameCursor.getColumnIndex(CONTACT_NAME));
							if (!name.equals(customContactsCursor.getString(customContactsCursor.getColumnIndex(LedContacts.LAST_KNOWN_NAME)))){
								values.put(LedContacts.LAST_KNOWN_NAME, name);	
								needsUpdating = true;
							}

//							String phoneNumber = contactNameCursor.getString(contactNameCursor.getColumnIndex(Phone.NUMBER));
//							if (!phoneNumber.equals(customContactsCursor.getString(customContactsCursor.getColumnIndex(LedContacts.LAST_KNOWN_NUMBER)))){
//								values.put(LedContacts.LAST_KNOWN_NUMBER, phoneNumber);
//								needsUpdating = true;
//							}
                            if (contactNameCursor.getInt(contactNameCursor.getColumnIndex(Contacts.HAS_PHONE_NUMBER)) == 0) {
                                needsUpdating = false;
                                toDelete.add(String.valueOf(id));
                            }

//							if (contactNameCursor.getInt(contactNameCursor.getColumnIndex(Phone.TYPE)) != Phone.TYPE_MOBILE){
//								needsUpdating = false;
//								toDelete.add(String.valueOf(id));
//							}
						}
						if (contactNameCursor != null){
							contactNameCursor.close();
						}

						if (needsUpdating){
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
