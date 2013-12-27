package com.ciubotariu_levy.lednotifier;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class AboutActivity extends ActionBarActivity {
	private String versionName = "";
	private CharSequence appName = "";
	private static String GH_URL = "http://www.github.com/";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		TextView appNameTextView = (TextView) findViewById (R.id.app_name);
		TextView appVersionTextView = (TextView) findViewById (R.id.app_version);
		try {
			versionName = getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
			appName = getPackageManager().getApplicationLabel(getApplicationInfo())+ " ";
		} catch (NameNotFoundException e) {
			//should not happen
		}

		appNameTextView.setText (appName);
		appVersionTextView.setText (versionName);
	}

	public void viewSite (View v){
		String fragment = null;
		switch (v.getId()){
		case R.id.andrei:
			fragment = "andreiciubotariu";
			break;
		case R.id.matthew:
			fragment = "LevyMatthew";
			break;
		default:
			fragment = "andreiciubotariu/led-notifier";
		}
		try{
			startActivity (new Intent (Intent.ACTION_VIEW,Uri.parse(GH_URL+fragment)));
		}
		catch (ActivityNotFoundException e){
			//usually won't happen.
			e.printStackTrace();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent i = NavUtils.getParentActivityIntent(this);    
			i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity (i);
			return true;
		default: return super.onOptionsItemSelected(item);	
		}
	}
}
