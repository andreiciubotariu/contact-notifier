package com.ciubotariu_levy.lednotifier;

import java.util.List;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends FragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		new SmsAppChooser().show(getSupportFragmentManager(), "APP_CHOOSER");
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
		}
		return super.onOptionsItemSelected(item);
	}

	public void showColorDialog(){
		FragmentManager fm = getSupportFragmentManager();
		ColorDialog cd = new ColorDialog();
		cd.show(fm, "Hello");
	}

}
