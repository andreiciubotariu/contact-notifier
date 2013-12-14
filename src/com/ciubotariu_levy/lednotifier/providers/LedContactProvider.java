package com.ciubotariu_levy.lednotifier.providers;

import java.util.Arrays;
import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.CursorJoiner;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.util.Log;

public class LedContactProvider extends ContentProvider {

	//public static final String AUTHORITY = "content://ca.timedprofiles.profiles.contentprovider";
	//public static final Uri CONTENT_URI = Uri.parse(AUTHORITY);
	//public static final String SINGLE_RECORD_MIME_TYPE = "vnd.android.cursor.item/ca.timedprofiles.profiles";
	//public final String MULTIPLE_RECORDS_MIME_TYPE = "vnd.android.cursor.dir/ca.timedprofiles.profiles";

	private static final String TAG = "LedContactProvider";
	private final static String DATABASE_NAME  = "ledcontacts.db";
	private static final int DATABASE_VERSION  = 1;
	private static final String LEDCONTACTS_TABLE_NAME = "led_contacts";



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
					LedContacts.SYSTEM_CONTACT_ID +" INTEGER PRIMARY KEY,"+ LedContacts.COLOR + " TEXT, "
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
	public int delete (Uri uri, String selection, String []selectionArgs){
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		switch (sUriMatcher.match(uri)){
		case LEDCONTACTS:
			break;
		case LEDCONTACTS_ID:
			selection = selection + "_id = " + uri.getLastPathSegment();
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
		long rowId = db.insert(LEDCONTACTS_TABLE_NAME, null, values);
		if (rowId > 0) {
			Uri noteUri = ContentUris.withAppendedId(LedContacts.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(noteUri, null);
			return noteUri;
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
		int index = -1;
		for (int x = 0; x < projection.length;x++){
			if (projection [x].equals (LedContacts.PROJECTION_BREAK)){
				index = x;
				break;
			}
		}
		if (index == -1){
			return null;
		}
		System.out.println (index);
		String [] ledProjection = Arrays.copyOfRange (projection, 0, index);
		System.out.println (Arrays.toString(ledProjection));
		System.out.println ((index+1) + " | " + (projection.length-1));
		String [] phoneProjection = Arrays.copyOfRange (projection, index+1, projection.length);
		System.out.println (Arrays.toString(phoneProjection));

		Cursor phoneCursor = getContext().getContentResolver().query(CommonDataKinds.Phone.CONTENT_URI, phoneProjection, selection, selectionArgs, Contacts._ID + " ASC");
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(LEDCONTACTS_TABLE_NAME);
		qb.setProjectionMap(ledContactsProjectionMap);

		switch (sUriMatcher.match(uri)) {    
		case LEDCONTACTS:
			break;
		case LEDCONTACTS_ID:
			selection = selection + LedContacts.SYSTEM_CONTACT_ID +" = " + uri.getLastPathSegment();
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		SQLiteDatabase db = mDbHelper.getReadableDatabase();
		Cursor ledCursor = qb.query(db, ledProjection, selection, selectionArgs, null, null, LedContacts.SYSTEM_CONTACT_ID + " ASC");
		ledCursor.setNotificationUri(getContext().getContentResolver(), uri);

		String[] result = Arrays.copyOf(ledProjection, ledProjection.length + phoneProjection.length);
		System.arraycopy(phoneProjection, 0, result, ledProjection.length, phoneProjection.length);
		
		MatrixCursor both = new MatrixCursor (result);
		CursorJoiner joiner = new CursorJoiner(ledCursor, new String[] { LedContacts.SYSTEM_CONTACT_ID },
				phoneCursor, new String[] { Contacts._ID});
		for (CursorJoiner.Result joinerResult : joiner) {
			switch (joinerResult) {
			case LEFT:
				//delete from ledcursor
				break;
			case RIGHT:
				
				Object [] row = new Object [projection.length-1];
				
				System.out.println ("Row: " + row.length);
				
				System.out.println ("Result: " + result.length);
				int colIndex = index-1;
				int cursorIndex = -1;
				for (int x = 0; x < phoneProjection.length ;x++){
					cursorIndex = phoneCursor.getColumnIndex(phoneProjection[x]);
					System.out.println (phoneProjection[x]);
					row [++colIndex] = phoneCursor.getString(cursorIndex);
					System.out.println (row[x]);
				}
				both.addRow(row);
				break;
			case BOTH:
				row = new Object [projection.length-1];
				colIndex = -1;
				cursorIndex = -1;
				for (int x = 0; x <= index-1 ;x++){
					cursorIndex = ledCursor.getColumnIndex(phoneProjection[x]);
					row [++colIndex] = ledCursor.getString(cursorIndex);
				}
				for (int x = 0; x < phoneProjection.length ;x++){
					cursorIndex = phoneCursor.getColumnIndex(phoneProjection[x]);
					row [++colIndex] = phoneCursor.getString(cursorIndex);
					System.out.println (row[x]);
				}
				both.addRow(row);
				break;
			}
		}

		//MergeCursor m = new MergeCursor(new Cursor [] {phoneCursor});
		return both;
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
			where = where + "_id = " + uri.getLastPathSegment();
			count = db.update(LEDCONTACTS_TABLE_NAME, values, where, whereArgs);
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
		ledContactsProjectionMap.put(LedContacts.SYSTEM_CONTACT_ID, LedContacts.SYSTEM_CONTACT_ID);
		ledContactsProjectionMap.put(LedContacts.COLOR, LedContacts.COLOR);
		ledContactsProjectionMap.put(LedContacts.VIBRATE_PATTERN, LedContacts.VIBRATE_PATTERN);
	}
}
