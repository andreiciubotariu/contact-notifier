package com.ciubotariu_levy.lednotifier;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NotificationCompat;

public class MainActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
	}
   
    public void showColorDialog(){
    	FragmentManager fm = getSupportFragmentManager();
		ColorDialog cd = new ColorDialog();
		cd.show(fm, "Hello");
    }
    
}
