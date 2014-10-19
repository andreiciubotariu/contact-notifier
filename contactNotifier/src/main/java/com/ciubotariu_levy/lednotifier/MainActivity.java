package com.ciubotariu_levy.lednotifier;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarDrawerToggle;
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
	private static final String KEY_FIRST_TIME_DRAWER = "first_time_drawer";
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
	private View mDrawer;
	private ListView mDrawerList;
	private SearchReceiver mSearchReceiver;
	private MenuItem mSearchItem;
	private String mSearchText="";
	private CharSequence mDrawerTitle;
	private CharSequence mTitle;
	private boolean mOpenDrawer = false;

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
		mDrawer = findViewById(R.id.left_drawer);
		mDrawerList = (ListView) findViewById(R.id.fragment_list);

		mDrawerList.setAdapter(new ArrayAdapter<String>(this,
				R.layout.drawer_bold_checked, android.R.id.text1, mFragmentTitles));
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		mDrawerToggle = new ActionBarDrawerToggle(
				this,                  /* host Activity */
				mDrawerLayout,         /* DrawerLayout object */
				R.string.drawer_open,  /* "open drawer" description */
				R.string.drawer_close  /* "close drawer" description */
				) {

			/** Called when a drawer has settled in a completely closed state. */
			public void onDrawerClosed(View view) {
				super.onDrawerClosed(view);
				getSupportActionBar().setTitle(mTitle);
				supportInvalidateOptionsMenu();
				if (mSearchReceiver != null){
					mSearchReceiver.onQueryTextSubmit("");
				}
			}

			/** Called when a drawer has settled in a completely open state. */
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
				mSearchText = "";
				getSupportActionBar().setTitle(mDrawerTitle);
				supportInvalidateOptionsMenu();
				Log.i("SEARCH-RELATED", "invalidated menu");
				if (mSearchItem != null){
					SearchView searchView = (SearchView) MenuItemCompat.getActionView(mSearchItem);
					if (searchView != null){
						searchView.setQuery(mSearchText, true);
						Log.i("SEARCH-RELATED", "set blank text");
					}
					if (mSearchReceiver != null){
						mSearchReceiver.onQueryTextSubmit("");
					}
					MenuItemCompat.collapseActionView(mSearchItem);
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

		if (!prefs.contains(KEY_FIRST_TIME_DRAWER)){
			mOpenDrawer =  true;
			prefs.edit().putBoolean(KEY_FIRST_TIME_DRAWER, true).apply();
		} 

		if (savedInstanceState != null){
			mSearchText = savedInstanceState.getString(KEY_SEARCH_TEXT);
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
		mDrawerLayout.closeDrawer(mDrawer);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	protected void onResume(){
		super.onResume();
		if (mOpenDrawer){
			mDrawerLayout.openDrawer(mDrawer);
			mOpenDrawer = false;
		}
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
		outState.putString(KEY_SEARCH_TEXT, mSearchText);
	}

	@Override
	public void onAttachFragment (Fragment fragment){
		super.onAttachFragment(fragment);
		mSearchReceiver = (SearchReceiver) fragment;
	}


	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// If the nav drawer is open, hide action items related to the content view
		boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawer);
		menu.findItem(R.id.action_search).setVisible(!drawerOpen);
		return super.onPrepareOptionsMenu(menu);
	}


	@Override
	public boolean onCreateOptionsMenu (Menu menu){
		getMenuInflater().inflate(R.menu.main, menu);
		mSearchItem = menu.findItem(R.id.action_search); 
		SearchView searchView = (SearchView) MenuItemCompat.getActionView(mSearchItem);
		if (!TextUtils.isEmpty(mSearchText)){
			MenuItemCompat.expandActionView(mSearchItem);
			searchView.setQuery(mSearchText, true);
		}
		searchView.setOnCloseListener(new SearchView.OnCloseListener() {

			@Override
			public boolean onClose() {
				mSearchText = "";
				Log.i("SEARCH-RELATED","closed");
				if (mSearchReceiver != null){
					mSearchReceiver.onSearchClosed();
				}
				return false;
			}
		});
		searchView.setOnQueryTextListener(new OnQueryTextListener() {

			@Override
			public boolean onQueryTextSubmit(String newText) {
				mSearchText = newText;
				if (mSearchReceiver != null){
					mSearchReceiver.onQueryTextSubmit(newText);
				}
				return false;
			}

			@Override
			public boolean onQueryTextChange(String query) {
				mSearchText = query;
				if (mSearchReceiver != null){
					mSearchReceiver.onQueryTextChange(query);
				}
				return false;
			}
		});
		MenuItemCompat.setOnActionExpandListener(mSearchItem, new MenuItemCompat.OnActionExpandListener() {

			@Override
			public boolean onMenuItemActionExpand(MenuItem item) {
				if (mSearchReceiver != null){
					mSearchReceiver.onSearchOpened();
				}
				return true;
			}

			@Override
			public boolean onMenuItemActionCollapse(MenuItem item) {
				if (mSearchReceiver != null){
					mSearchReceiver.onSearchClosed();
				}
				mSearchText = "";
				return true;
			}
		});
		return true;
	}

	public void drawerOptions (View v){
		switch (v.getId()){
		case R.id.settings:
			startActivity (new Intent (this, SettingsActivity.class));
			break;
		case R.id.help:
			startActivity (new Intent (Intent.ACTION_VIEW, Uri.parse("http://github.com/andreiciubotariu/led-notifier/wiki")));
			break;
		}
		mDrawerLayout.closeDrawer(mDrawer);
	}
	@Override
	public boolean onOptionsItemSelected (MenuItem item){
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onKeyDown (int keyCode, KeyEvent event){
		if (keyCode == KeyEvent.KEYCODE_SEARCH && !mDrawerLayout.isDrawerOpen(GravityCompat.START)){
			MenuItemCompat.expandActionView(mSearchItem);
		}
		return super.onKeyDown(keyCode, event);
	}
}
