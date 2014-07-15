package com.ciubotariu_levy.lednotifier.providers;

import java.util.HashMap;

import android.content.ContentProvider;
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
import android.util.Log;

public class LedContactProvider extends ContentProvider {

	private static final String TAG = "LedContactProvider";
	private static final String DATABASE_NAME  = "ledcontacts.db";
	private static final int DATABASE_VERSION  = 2;//TODO Add boolean column for custom vibrate
	public static final String LEDCONTACTS_TABLE_NAME = "led_contacts";

	public static final String AUTHORITY = "com.ciubotariu_levy.lednotifier.providers.LedContactProvider";
	private static final UriMatcher sUriMatcher;

	private static final int LEDCONTACTS = 1;
	private static final int LEDCONTACTS_ID = 2;

	private static HashMap <String, String> ledContactsProjectionMap;

	//store our table
	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper (Context context){
			super (context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate (SQLiteDatabase db){
			String CREATE_PROFILES_TABLE = "CREATE TABLE " + LEDCONTACTS_TABLE_NAME + "(" +
					LedContacts._ID + " INTEGER PRIMARY KEY," +
					LedContacts.SYSTEM_CONTACT_LOOKUP_URI +" TEXT UNIQUE,"+ LedContacts.COLOR + " TEXT, "
					+ LedContacts.VIBRATE_PATTERN + " TEXT" +
					")";
			db.execSQL(CREATE_PROFILES_TABLE);
		}

		@Override
		public void onUpgrade (SQLiteDatabase db, int oldVersion, int newVersion){
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + LEDCONTACTS_TABLE_NAME);
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
			selection = selection + LedContacts._ID + " = " + uri.getLastPathSegment(); //TODO fix this up. If no space. If null, this fails
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
			selection = (selection != null ? selection + " " : "") + LedContacts._ID + " = " + uri.getLastPathSegment();
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		SQLiteDatabase db = mDbHelper.getReadableDatabase();
		Cursor ledCursor = qb.query(db, projection, selection, selectionArgs, null, null, LedContacts.SYSTEM_CONTACT_LOOKUP_URI + " ASC");
		ledCursor.setNotificationUri(getContext().getContentResolver(), uri);
		return ledCursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)) {
		case LEDCONTACTS:
			count = db.update(LEDCONTACTS_TABLE_NAME, values, where, whereArgs);
			break;
		case LEDCONTACTS_ID:
			where = /*where +*/ LedContacts._ID + " = " + uri.getLastPathSegment(); //TODO see if using whereArgs throws odd error like last time. Probably was an implementation issue
			count = db.update(LEDCONTACTS_TABLE_NAME, values, where, null);
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
		ledContactsProjectionMap.put(LedContacts.COLOR, LedContacts.COLOR);
		ledContactsProjectionMap.put(LedContacts.VIBRATE_PATTERN, LedContacts.VIBRATE_PATTERN);
	}
}
