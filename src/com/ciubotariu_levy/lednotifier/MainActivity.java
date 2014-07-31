package com.ciubotariu_levy.lednotifier;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends ActionBarActivity {
	private static final String KEY_FIRST_RUN = "first_run";
	private static final String KEY_SEARCH_TEXT = "KEY_SEARCH_TEXT";
	public static final String KEY_DELAY_DISMISS = "delay_dismissal";

	public interface SearchReceiver{
		public void onSearchClosed();
		public void onSearchOpened();
		public void onQueryTextSubmit (String newText);
		public void onQueryTextChange (String query);
	}

	private String[] mFragmentTitles;
	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mDrawerToggle;
	private ListView mDrawerList;
	private SearchReceiver searchReceiver;
	private MenuItem searchItem;
	private String searchText="";
	private CharSequence mDrawerTitle;
	private CharSequence mTitle;

	private class DrawerItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(@SuppressWarnings("rawtypes") AdapterView parent, View view, int position, long id) {
			selectItem(position);
		}
	}

	@TargetApi(19)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		startService (new Intent (this, ObserverService.class));

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		mTitle = mDrawerTitle = getTitle();
		mFragmentTitles = new String[] {"Custom contacts", "All Mobile"};
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);

		mDrawerList.setAdapter(new ArrayAdapter<String>(this,
				R.layout.drawer_bold_checked, android.R.id.text1, mFragmentTitles));
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		mDrawerToggle = new ActionBarDrawerToggle(
				this,                  /* host Activity */
				mDrawerLayout,         /* DrawerLayout object */
				R.drawable.ic_drawer,  /* nav drawer icon to replace 'Up' caret */
				R.string.drawer_open,  /* "open drawer" description */
				R.string.drawer_close  /* "close drawer" description */
				) {

			/** Called when a drawer has settled in a completely closed state. */
			public void onDrawerClosed(View view) {
				super.onDrawerClosed(view);
				getSupportActionBar().setTitle(mTitle);
				supportInvalidateOptionsMenu();
			}

			/** Called when a drawer has settled in a completely open state. */
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
				searchText = "";
				getSupportActionBar().setTitle(mDrawerTitle);
				supportInvalidateOptionsMenu();
				Log.i("SEARCH-RELATED", "invalidated menu");
				if (searchItem != null){
					SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
					if (searchView != null){
						searchView.setQuery(searchText, true);
						Log.i("SEARCH-RELATED", "set blank text");
					}
					MenuItemCompat.collapseActionView(searchItem);
					Log.i("SEARCH-RELATED", "collapsed search view");
				}
			}
		};

		// Set the drawer toggle as the DrawerListener
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (!prefs.contains(KEY_FIRST_RUN)){
			if (Build.BRAND.toLowerCase().contains("samsung")){
				prefs.edit().putBoolean(KEY_DELAY_DISMISS, true).apply();
			}
			prefs.edit().putBoolean(KEY_FIRST_RUN, true).apply();
		}

		if (savedInstanceState != null){
			searchText = savedInstanceState.getString(KEY_SEARCH_TEXT);
			Fragment frag = getSupportFragmentManager().findFragmentById(R.id.content_frame);
			if (frag != null && frag.getTag().equals(mFragmentTitles[1])){
				selectItem(1);
			} else {
				selectItem(0);
			}
		}
		else {
			selectItem(0);
		}
	}

	/** Swaps fragments in the main content view */
	private void selectItem(int position) {
		// Create a new fragment and specify the planet to show based on position
		FragmentManager fragmentManager = getSupportFragmentManager();
		Fragment fragment = fragmentManager.findFragmentByTag(mFragmentTitles[position]);
		if (fragment == null){
			switch (position){
			case 0:
				fragment = new CustomContactsFragment();
				break;
			case 1:
			default:
				fragment = new ContactsFragment();
				break;
			}

			fragmentManager.beginTransaction()
			.replace(R.id.content_frame, fragment, mFragmentTitles[position])
			.commit();
		}

		// Highlight the selected item, update the title, and close the drawer
		mDrawerList.setItemChecked(position, true);
		setTitle(mFragmentTitles[position]);
		mDrawerLayout.closeDrawer(mDrawerList);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
		getSupportActionBar().setTitle(mTitle);
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
	public boolean onPrepareOptionsMenu(Menu menu) {
		// If the nav drawer is open, hide action items related to the content view
		boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
		menu.findItem(R.id.action_search).setVisible(!drawerOpen);
		return super.onPrepareOptionsMenu(menu);
	}


	@Override
	public boolean onCreateOptionsMenu (Menu menu){
		getMenuInflater().inflate(R.menu.main, menu);
		searchItem = menu.findItem(R.id.action_search); 
		SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
		if (!TextUtils.isEmpty(searchText)){
			MenuItemCompat.expandActionView(searchItem);
			searchView.setQuery(searchText, true);
		}
		searchView.setOnCloseListener(new SearchView.OnCloseListener() {

			@Override
			public boolean onClose() {
				searchText = "";
				Log.i("SEARCH-RELATED","closed");
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
				searchText = "";
				return true;
			}
		});
		return true;
	}

	@Override
	public boolean onOptionsItemSelected (MenuItem item){
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

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
		if (keyCode == KeyEvent.KEYCODE_SEARCH && !mDrawerLayout.isDrawerOpen(GravityCompat.START)){
			MenuItemCompat.expandActionView(searchItem);
		}
		return super.onKeyDown(keyCode, event);
	}
}
