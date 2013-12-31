package com.ciubotariu_levy.lednotifier;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends ActionBarActivity {

	@TargetApi(19)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		startService (new Intent (this, ObserverService.class));
	}

	@Override
	public boolean onCreateOptionsMenu (Menu menu){
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected (MenuItem item){
		switch (item.getItemId()){
		case R.id.action_settings:
			startActivity (new Intent (this, SettingsActivity.class));
			return true;
		case R.id.action_help:
			startActivity (new Intent (Intent.ACTION_VIEW, Uri.parse("http://github.com/andreiciubotariu/led-notifier/wiki")));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
