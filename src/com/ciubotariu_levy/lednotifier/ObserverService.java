package com.ciubotariu_levy.lednotifier;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
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
import android.provider.Telephony.Sms;

import com.ciubotariu_levy.lednotifier.providers.LedContacts;

@TargetApi(19)
public class ObserverService extends Service {
	private Handler mHandler = new Handler();

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
	ContentObserver mContactContentObserver = new ContentObserver (mHandler){
		@Override
		public void onChange (boolean selfChange){
			onChange(selfChange, null);
		}

		@Override
		public void onChange (boolean selfChange, Uri uri){
			System.out.println ("changed");
			int newNumContacts = getNumContacts(mNumContacts);

			if (mNumContacts != newNumContacts){
				mNumContacts = newNumContacts;
				new ContactsChangeChecker().execute();
			}
		}
	};

	int mUnread;
	int mUnseen;
	boolean registeredObserver = false;
	final static Uri SMS_CONTENT_URI =  Build.VERSION.SDK_INT	>= Build.VERSION_CODES.KITKAT ? Sms.CONTENT_URI : Uri.parse("content://sms/");
	final static Uri INBOX_URI =  Build.VERSION.SDK_INT	>= Build.VERSION_CODES.KITKAT ? Sms.Inbox.CONTENT_URI : Uri.parse("content://sms/inbox/");
	final static String READ =  Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? Sms.Inbox.READ : "read";	
	final static String SEEN =  Build.VERSION.SDK_INT 	>= Build.VERSION_CODES.KITKAT ? Sms.Inbox.SEEN : "seen";	
	
	ContentObserver mSMSContentObserver = new ContentObserver (mHandler){
		@Override
		public void onChange (boolean selfChange){
			onChange(selfChange, null);
		}

		@Override
		public void onChange (boolean selfChange, Uri uri){
			int unseen = getUnseenSms();
			int unread = getUnreadSms();

			System.out.println ("Stats " + unseen+ "|" + unread);
			if (unseen <mUnseen || unread<mUnread){
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
			System.out.println ("Registered observer " + mUnseen +"|" + mUnread);
		}catch (Exception e){ //sms inbox not standardized on jellybean and older
			e.printStackTrace();
			throw new RuntimeException();
		}
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
			resolver.delete(LedContacts.CONTENT_URI, LedContacts._ID + " IN " + generateSelectionMarks(toDelete.size()), toDelete.toArray(new String[toDelete.size()]));

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
