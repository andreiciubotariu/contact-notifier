package com.ciubotariu_levy.lednotifier.providers;

import android.net.Uri;
import android.provider.BaseColumns;

public class LedContacts implements BaseColumns{
	public static final Uri CONTENT_URI = Uri.parse("content://" 
			+ LedContactProvider.AUTHORITY + "/led_contacts");

	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.ciubotariu_levy.ledcontacts";

	public static final String SYSTEM_CONTACT_ID= "system_id";

	public static final String COLOR = "color";

	public static final String VIBRATE_PATTERN  = "VIBRATE_PATTERN";
	
	public static final String REPLACE_CONTENT = "PROJ_BREAK";
}
