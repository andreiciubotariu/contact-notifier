package com.ciubotariu_levy.lednotifier;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends ActionBarActivity {
	private static final String KEY_FIRST_RUN = "first_run";
	private static final String KEY_SEARCH_TEXT = "KEY_SEARCH_TEXT";
	
	public interface SearchReceiver{
		public void onSearchClosed();
		public void onSearchOpened();
		public void onQueryTextSubmit (String newText);
		public void onQueryTextChange (String query);
	}
	
	private SearchReceiver searchReceiver;
	private MenuItem searchItem;
	private String searchText="";
	
	
	@TargetApi(19)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		startService (new Intent (this, ObserverService.class));
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (!prefs.contains(KEY_FIRST_RUN)){
			if (Build.BRAND.toLowerCase().contains("samsung") && Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
				prefs.edit().putBoolean("delay_dismissal", true).commit();
			}
			prefs.edit().putBoolean(KEY_FIRST_RUN, true).commit();
		}
		
		if (savedInstanceState != null){
			searchText = savedInstanceState.getString(KEY_SEARCH_TEXT);
		}
	}
	
	@Override
	protected void onSaveInstanceState (Bundle outState){
		super.onSaveInstanceState(outState);
		outState.putString(KEY_SEARCH_TEXT, searchText);
	}
	
	@Override
	public void onAttachFragment (Fragment fragment){
		super.onAttachFragment(fragment);
		searchReceiver = (SearchReceiver) fragment;
	}

	@Override
	public boolean onCreateOptionsMenu (Menu menu){
		getMenuInflater().inflate(R.menu.main, menu);
		searchItem = menu.findItem(R.id.action_search); 
		SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
		if (!TextUtils.isEmpty(searchText)){
			MenuItemCompat.expandActionView(searchItem);
			searchView.setQuery(searchText, false);
		}
		searchView.setOnCloseListener(new SearchView.OnCloseListener() {

			@Override
			public boolean onClose() {
				
				return false;
			}
		});
		searchView.setOnQueryTextListener(new OnQueryTextListener() {

			@Override
			public boolean onQueryTextSubmit(String newText) {
				searchText = newText;
				if (searchReceiver != null){
					searchReceiver.onQueryTextSubmit(newText);
				}
				return false;
			}

			@Override
			public boolean onQueryTextChange(String query) {
				searchText = query;
				if (searchReceiver != null){
					searchReceiver.onQueryTextChange(query);
				}
				return false;
			}
		});
		MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {

			@Override
			public boolean onMenuItemActionExpand(MenuItem item) {
				if (searchReceiver != null){
					searchReceiver.onSearchOpened();
				}
				return true;
			}

			@Override
			public boolean onMenuItemActionCollapse(MenuItem item) {
				if (searchReceiver != null){
					searchReceiver.onSearchClosed();
				}
				return true;
			}
		});
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
	
	@Override
	public boolean onKeyDown (int keyCode, KeyEvent event){
		if (keyCode == KeyEvent.KEYCODE_SEARCH){
			MenuItemCompat.expandActionView(searchItem);
		}
		return super.onKeyDown(keyCode, event);
	}
}
