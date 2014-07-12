package com.ciubotariu_levy.lednotifier.providers;

import android.text.TextUtils;

public class LedContactInfo {

	public long id = -1;
	public String systemLookupUri;
	public int color;
	public String vibratePattern;
	
	public static long [] getVibratePattern (String s){
		if (TextUtils.isEmpty(s)){
			return new long[0];
		}
		String [] sPattern= s.split(",");
		//System.out.println (sPattern);
		long [] pattern = new long [sPattern.length];
		for (int x=0;x<pattern.length;x++){
			try{
				pattern[x]=Long.parseLong(sPattern[x]);
			}
			catch (NumberFormatException e){
				pattern[x] = 0L;
			}
		}
		return pattern;
	}
}
