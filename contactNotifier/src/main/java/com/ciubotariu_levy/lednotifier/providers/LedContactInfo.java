package com.ciubotariu_levy.lednotifier.providers;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class LedContactInfo implements Parcelable {
    public static final int TRUE = 1;
    public static final int FALSE = 0;
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
    public long id = -1;
    public int color;
    public String systemLookupUri;
    public String lastKnownName;
    public String lastKnownNumber;
    public String vibratePattern;
    public String ringtoneUri;

    private LedContactInfo(Parcel in) {
        id = in.readLong();
        systemLookupUri = in.readString();
        lastKnownName = in.readString();
        lastKnownNumber = in.readString();
        color = in.readInt();
        vibratePattern = in.readString();

        if (in.readInt() != FALSE) {
            ringtoneUri = in.readString();
        }
    }

    //required empty
    public LedContactInfo() {
    }

    //copy constructor
    public LedContactInfo(LedContactInfo other) {
        id = other.id;
        systemLookupUri = other.systemLookupUri;
        lastKnownName = other.lastKnownName;
        lastKnownNumber = other.lastKnownNumber;
        color = other.color;
        vibratePattern = other.vibratePattern;
        ringtoneUri = other.ringtoneUri;
    }

    public static long[] getVibratePattern(String s) {
        if (TextUtils.isEmpty(s)) {
            return new long[0];
        }
        String[] commaSplitString = s.split(",");
        long[] pattern = new long[commaSplitString.length];
        for (int x = 0; x < pattern.length; x++) {
            try {
                pattern[x] = Long.parseLong(commaSplitString[x]);
            } catch (NumberFormatException e) {
                pattern[x] = 0L;
            }
        }
        return pattern;
    }

    // adds zeroes to vibrate pattern if there are successive commas
    public static String addZeroesWhereEmpty(String s) {
        if (TextUtils.isEmpty(s)) {
            return "";
        }

        String[] commaSplitString = s.split(",");
        if (commaSplitString.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int x = 0; x < commaSplitString.length - 1; x++) {
            appendZeroIfNeeded(builder, commaSplitString[x]);
            builder.append(",");
        }
        appendZeroIfNeeded(builder, commaSplitString[commaSplitString.length - 1]);

        return builder.toString();
    }

    private static void appendZeroIfNeeded(StringBuilder builder, String toCheck) {
        if (TextUtils.isEmpty(toCheck)) {
            builder.append("0");
        } else {
            builder.append(toCheck);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(id);
        out.writeString(systemLookupUri);
        out.writeString(lastKnownName);
        out.writeString(lastKnownNumber);
        out.writeInt(color);
        out.writeString(vibratePattern);

        if (ringtoneUri != null) {
            out.writeInt(TRUE);
            out.writeString(ringtoneUri);
        } else {
            out.writeInt(FALSE);
        }
    }
}
