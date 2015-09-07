package com.ciubotariu_levy.lednotifier.ui.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.ciubotariu_levy.lednotifier.R;

public class AboutActivity extends AppCompatActivity {
    private static final String TAG = AboutActivity.class.getName();
    private static final String GH_URL = "https://www.github.com/";
    private String mVersionName = "";
    private CharSequence mAppName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        TextView appNameTextView = (TextView) findViewById(R.id.app_name);
        TextView appVersionTextView = (TextView) findViewById(R.id.app_version);
        try {
            mVersionName = getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
            mAppName = getPackageManager().getApplicationLabel(getApplicationInfo()) + " ";
        } catch (NameNotFoundException e) {
            Log.e(TAG, "onCreate: could not get app info", e);
        }

        appNameTextView.setText(mAppName);
        appVersionTextView.setText(mVersionName);
    }

    public void viewSite(View v) {
        String fragment = null;
        switch (v.getId()) {
            case R.id.andrei:
                fragment = "andreiciubotariu";
                break;
            case R.id.matthew:
                fragment = "LevyMatthew";
                break;
            default:
                fragment = "andreiciubotariu/contact-notifier";
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(GH_URL + fragment)));
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "viewSite: could not find activity to handle url", e);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent i = NavUtils.getParentActivityIntent(this);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
