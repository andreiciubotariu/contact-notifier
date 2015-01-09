package com.ciubotariu_levy.lednotifier;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends ActionBarActivity implements FragmentManager.OnBackStackChangedListener {
	private static final String KEY_SEARCH_TEXT = "KEY_SEARCH_TEXT";

	public interface SearchReceiver{
		public void onSearchClosed();
		public void onSearchOpened();
		public void onQueryTextSubmit (String newText);
		public void onQueryTextChange (String query);
	}

	private String[] mFragmentTitles;
	private SearchReceiver mSearchReceiver;
	private MenuItem mSearchItem;
	private String mSearchText="";
    private int mBackStackCount = 0;


	@TargetApi(19)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		startService (new Intent (this, ObserverService.class));

		mFragmentTitles = new String[] {"Custom contacts", "Select contact"};

		if (savedInstanceState != null){
			mSearchText = savedInstanceState.getString(KEY_SEARCH_TEXT);
		}
        else {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new CustomContactsFragment(), mFragmentTitles[0])
                    .commit();
        }

        getSupportFragmentManager().addOnBackStackChangedListener(this);
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

	@Override
	public boolean onOptionsItemSelected (MenuItem item){
        switch(item.getItemId()) {
            case android.R.id.home:
                getSupportFragmentManager().popBackStack();
                return true;
            case R.id.action_settings:
                startActivity (new Intent (this, SettingsActivity.class));
                return true;
            case R.id.action_help:
                startActivity (new Intent (Intent.ACTION_VIEW, Uri.parse("http://github.com/andreiciubotariu/led-notifier/wiki")));
            default:
                return super.onOptionsItemSelected(item);
        }

	}

	@Override
	public boolean onKeyDown (int keyCode, KeyEvent event){
		if (keyCode == KeyEvent.KEYCODE_SEARCH){
			MenuItemCompat.expandActionView(mSearchItem);
		}
		return super.onKeyDown(keyCode, event);
	}

    @Override
    public void onBackStackChanged() {
        int count = getSupportFragmentManager().getBackStackEntryCount();
        getSupportActionBar().setDisplayHomeAsUpEnabled(count > 0);

        Fragment f = getSupportFragmentManager().getFragments().get(count);
        setTitle(mFragmentTitles[count]);
        if (count < mBackStackCount) {
            mSearchReceiver = (SearchReceiver) f;
        }
        mBackStackCount = count;
    }
}