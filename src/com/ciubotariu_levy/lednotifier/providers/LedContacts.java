package com.ciubotariu_levy.lednotifier.providers;

import android.net.Uri;
import android.provider.BaseColumns;

public class LedContacts implements BaseColumns{
	public static final Uri CONTENT_URI = Uri.parse("content://" 
			+ LedContactProvider.AUTHORITY + "/led_contacts");
	
	public static final String LAST_KNOWN_NAME = "contact_last_known_name";
	public static final String LAST_KNOWN_NUMBER = "contact_last_known_number";

	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.ciubotariu_levy.ledcontacts";

	public static final String SYSTEM_CONTACT_LOOKUP_URI = "system_lookup_uri";

	public static final String COLOR = "color";

	public static final String VIBRATE_PATTERN = "custom_vibrate_pattern";
	
	public static final String RINGTONE_URI = "ringtone_uri";
	
	public static final String VIBRATE_PATTERN_OLD  = "VIBRATE_PATTERN";
	public static final String REPLACE_CONTENT = "PROJ_BREAK";
}