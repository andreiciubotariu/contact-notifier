package com.ciubotariu_levy.lednotifier.providers;

import com.ciubotariu_levy.lednotifier.GlobalConstants;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class LedContactInfo implements Parcelable{

	public long id = -1;
	public String systemLookupUri;
	public String lastKnownName;
	public String lastKnownNumber;
	public int color;
	public int hasCustomVibrate;
	public String vibratePattern;
	public int hasCustomRingtone;
	public String ringtoneUri;

	public static long [] getVibratePattern (String s){
		if (TextUtils.isEmpty(s)){
			return new long[0];
		}
		String [] sPattern= s.split(",");
		long [] pattern = new long [sPattern.length];
		for (int x=0;x<pattern.length;x++){
			try{
				pattern[x] = Long.parseLong(sPattern[x]);
			}
			catch (NumberFormatException e){
				pattern[x] = 0L;
			}
		}
		return pattern;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeLong (id);
		out.writeString(systemLookupUri);
		out.writeString(lastKnownName);
		out.writeString(lastKnownNumber);
		out.writeInt(color);
		out.writeInt(hasCustomVibrate);
		out.writeString(vibratePattern);

		out.writeInt(hasCustomRingtone);
		if (ringtoneUri != null){
			out.writeInt(GlobalConstants.TRUE);
			out.writeString(ringtoneUri);
		} else {
			out.writeInt(GlobalConstants.FALSE);
		}	
	}

	public static final Parcelable.Creator<LedContactInfo> CREATOR = new Parcelable.Creator<LedContactInfo>() {
		@Override
		public LedContactInfo createFromParcel(Parcel in) {
			return new LedContactInfo(in);
		}

		@Override
		public LedContactInfo[] newArray(int size) {
			return new LedContactInfo[size];
		}
	};

	private LedContactInfo (Parcel in){
		id = in.readLong();
		systemLookupUri = in.readString();
		lastKnownName = in.readString();
		lastKnownNumber = in.readString();
		color = in.readInt();
		hasCustomVibrate = in.readInt();
		vibratePattern = in.readString();

		hasCustomRingtone = in.readInt();
		if (in.readInt() != GlobalConstants.FALSE){
			ringtoneUri = in.readString();
		}
	}

	public LedContactInfo() { //required
	}
}
