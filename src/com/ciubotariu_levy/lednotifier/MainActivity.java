package com.ciubotariu_levy.lednotifier;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

public class MainActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //showColorDialog();
    }

    public void showColorDialog(){
    	FragmentManager fm = getSupportFragmentManager();
		ColorDialog cd = new ColorDialog();
		cd.show(fm, "Hello");
    }
    
}
