package com.ciubotariu_levy.lednotifier.providers;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.util.Log;

import com.ciubotariu_levy.lednotifier.AbstractContactsFragment;
import com.ciubotariu_levy.lednotifier.ColorVibrateDialog;

import java.util.HashMap;

public class LedContactProvider extends ContentProvider {

	private static final String TAG = "LedContactProvider";
	private static final String DATABASE_NAME  = "ledcontacts.db";
	private static final int DATABASE_VERSION  = 2;
	public static final String LEDCONTACTS_TABLE_NAME = "led_contacts";

	public static final String AUTHORITY = "com.ciubotariu_levy.lednotifier.providers.LedContactProvider";
	private static final UriMatcher sUriMatcher;

	private static final int LEDCONTACTS = 1;
	private static final int LEDCONTACTS_ID = 2;

	private static HashMap <String, String> ledContactsProjectionMap;

	//store our table
	private static class DatabaseHelper extends SQLiteOpenHelper {
		private Context mContext; 
		DatabaseHelper (Context context){
			super (context, DATABASE_NAME, null, DATABASE_VERSION);
			mContext =  context;
		}

		@Override
		public void onCreate (SQLiteDatabase db){
			String CREATE_PROFILES_TABLE = "CREATE TABLE " + LEDCONTACTS_TABLE_NAME + "(" +
					LedContacts._ID + " INTEGER PRIMARY KEY," +
					LedContacts.SYSTEM_CONTACT_LOOKUP_URI +" TEXT UNIQUE,"
                    + LedContacts.LAST_KNOWN_NAME + " TEXT,"
                    + LedContacts.LAST_KNOWN_NUMBER + " TEXT, "
					+ LedContacts.COLOR + " INTEGER, "
					+ LedContacts.VIBRATE_PATTERN + " TEXT," 
					+ LedContacts.RINGTONE_URI + " TEXT" + 
					")";
			db.execSQL(CREATE_PROFILES_TABLE);
		}

		private void upgradeLog (String text){
			if (text == null){
				text = "";
			}
			Log.i ("DB-Upgrade", text);
		}
		
		@Override
		public void onUpgrade (SQLiteDatabase db, int oldVersion, int newVersion){
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
			String tempTableName =  "TEMP_" + LEDCONTACTS_TABLE_NAME;
			if (newVersion == 2){
				ContentResolver resolver = mContext.getContentResolver();
				db.execSQL("ALTER TABLE "  + LEDCONTACTS_TABLE_NAME +  " RENAME TO " + tempTableName);
				upgradeLog ("Altered table to temp");
				onCreate(db);
				upgradeLog ("Created new table");
				Cursor cursor = db.query(tempTableName, null, null, null, null, null, null);
				if (cursor != null && cursor.moveToFirst()){
					do {
						String systemId =  cursor.getString(cursor.getColumnIndex(LedContacts.SYSTEM_CONTACT_ID_DEPRECATED));
						Uri contactUri = Contacts.getLookupUri(resolver, Uri.withAppendedPath(Contacts.CONTENT_LOOKUP_URI, systemId));
						if (contactUri != null){
							boolean canInsert = true;
							ContentValues values = new ContentValues();
							values.put(LedContacts.COLOR, cursor.getInt(cursor.getColumnIndex(LedContacts.COLOR)));
							values.put(LedContacts.VIBRATE_PATTERN, cursor.getString(cursor.getColumnIndex(LedContacts.VIBRATE_PATTERN_DEPRECATED)));
							values.put(LedContacts.SYSTEM_CONTACT_LOOKUP_URI, contactUri.toString());
							values.put(LedContacts.RINGTONE_URI, ColorVibrateDialog.GLOBAL);

							String contactId = contactUri.getLastPathSegment();
							Cursor contactNameCursor = resolver.query(Phone.CONTENT_URI, new String [] {AbstractContactsFragment.CONTACT_NAME, Phone.LOOKUP_KEY, Phone.CONTACT_ID, Phone.NUMBER, Phone.TYPE},Phone.CONTACT_ID + "=?", new String[] {contactId} , null);
							if (contactNameCursor != null && contactNameCursor.moveToFirst()){
								values.put(LedContacts.LAST_KNOWN_NAME, contactNameCursor.getString(contactNameCursor.getColumnIndex(AbstractContactsFragment.CONTACT_NAME)));
								values.put(LedContacts.LAST_KNOWN_NUMBER, contactNameCursor.getString(contactNameCursor.getColumnIndex(Phone.NUMBER)));
							} else {
								canInsert = false;
								Log.e("DB-Upgrade", "Contact details not found!");
							}

							if (contactNameCursor != null){
								contactNameCursor.close();
							}
							if (canInsert){
								db.insert(LEDCONTACTS_TABLE_NAME, null, values);
								upgradeLog("Inserted a contact");
							}
						} else {
							Log.e("DB-Upgrade","Skipping over contact. DNE");
						}
					} while (cursor.moveToNext());
				}
				if (cursor != null){
					cursor.close();
				}

				db.execSQL("DROP TABLE IF EXISTS " + tempTableName);
				upgradeLog("Dropped temp table");
				return;
			} 

			onCreate(db);
		}
	}
	
	

	//instance of our table
	private DatabaseHelper mDbHelper;

	@Override
	public int delete (Uri uri, String selection, String [] selectionArgs){ 
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		switch (sUriMatcher.match(uri)){
		case LEDCONTACTS:
			break;
		case LEDCONTACTS_ID:
			selection = LedContacts._ID + " = " + uri.getLastPathSegment();
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		int count = db.delete(LEDCONTACTS_TABLE_NAME, selection, selectionArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public String getType (Uri uri){
		switch (sUriMatcher.match(uri)){
		case LEDCONTACTS:
			return LedContacts.CONTENT_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		if (sUriMatcher.match(uri) != LEDCONTACTS) {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}

		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		long rowId = db.insertWithOnConflict(LEDCONTACTS_TABLE_NAME, null, values,SQLiteDatabase.CONFLICT_REPLACE);
		if (rowId > 0) {
			Uri contactUri = ContentUris.withAppendedId(LedContacts.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(contactUri, null);
			return contactUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public boolean onCreate() {
		mDbHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(LEDCONTACTS_TABLE_NAME);
		qb.setProjectionMap(ledContactsProjectionMap);

		switch (sUriMatcher.match(uri)) {    
		case LEDCONTACTS:
			break;
		case LEDCONTACTS_ID:
			selection = (selection != null ? selection + " " : "") + LedContacts._ID + " = " + uri.getLastPathSegment(); //TODO Check if this query is valid when selection != null
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		SQLiteDatabase db = mDbHelper.getReadableDatabase();
		Cursor ledCursor = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
		ledCursor.setNotificationUri(getContext().getContentResolver(), uri);
		return ledCursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)) {
		case LEDCONTACTS:
			count = db.updateWithOnConflict(LEDCONTACTS_TABLE_NAME, values, where, whereArgs, SQLiteDatabase.CONFLICT_REPLACE);
			break;
		case LEDCONTACTS_ID:
			where = LedContacts._ID + " = " + uri.getLastPathSegment();
			count = db.updateWithOnConflict(LEDCONTACTS_TABLE_NAME, values, where, null, SQLiteDatabase.CONFLICT_REPLACE);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(AUTHORITY, LEDCONTACTS_TABLE_NAME, LEDCONTACTS);
		sUriMatcher.addURI(AUTHORITY, LEDCONTACTS_TABLE_NAME + "/#", LEDCONTACTS_ID);

		ledContactsProjectionMap = new HashMap<String, String>();
		ledContactsProjectionMap.put(LedContacts._ID, LedContacts._ID);
		ledContactsProjectionMap.put(LedContacts.SYSTEM_CONTACT_LOOKUP_URI, LedContacts.SYSTEM_CONTACT_LOOKUP_URI);
		ledContactsProjectionMap.put(LedContacts.LAST_KNOWN_NAME, LedContacts.LAST_KNOWN_NAME);
		ledContactsProjectionMap.put(LedContacts.LAST_KNOWN_NUMBER, LedContacts.LAST_KNOWN_NUMBER);
		ledContactsProjectionMap.put(LedContacts.COLOR, LedContacts.COLOR);
		ledContactsProjectionMap.put(LedContacts.VIBRATE_PATTERN, LedContacts.VIBRATE_PATTERN);
		ledContactsProjectionMap.put(LedContacts.RINGTONE_URI, LedContacts.RINGTONE_URI);
	}
}
