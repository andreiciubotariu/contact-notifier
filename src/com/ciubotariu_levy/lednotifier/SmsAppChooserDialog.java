package com.ciubotariu_levy.lednotifier;

import java.util.List;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.view.ContextThemeWrapper;

public class SmsAppChooserDialog extends DialogFragment {
	public static final String KEY_SMS_APP_PACKAGE = "sms_app_package";
	
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
	public Dialog onCreateDialog (Bundle savedInstanceState){
		Intent intent = new Intent(Intent.ACTION_SENDTO);
		intent.setData(Uri.parse("smsto:" + Uri.encode("0")));
		final PackageManager pm = getActivity().getPackageManager();
		final List <ResolveInfo> smsApps = pm.queryIntentActivities(intent, 0);
		CharSequence [] userList = new CharSequence [smsApps.size()];
		for (int x = 0; x < userList.length;x++){
			userList [x] = smsApps.get(x).loadLabel(pm);
		}

		Context context = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH ? 
			new ContextThemeWrapper(getActivity(), android.R.style.Theme_Holo_Light_DarkActionBar):
			getActivity();
			
		return new AlertDialog.Builder(context)
		.setTitle("Choose your current SMS app")
		.setItems(userList, new OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				PreferenceManager.getDefaultSharedPreferences(getActivity())
				.edit()
				.putString(KEY_SMS_APP_PACKAGE, smsApps.get(which).activityInfo.packageName)
				.apply();
			}
		})
		.create();
	}

	@Override
	public void onCancel(DialogInterface dialog){
		super.onCancel(dialog);
		finishHostActivity();
	}

	@Override
	public void onDismiss(DialogInterface dialog){
		super.onDismiss(dialog);
		finishHostActivity();
	}

	private void finishHostActivity(){
		if (getActivity() != null){
			getActivity().finish();
		}
	}
}
