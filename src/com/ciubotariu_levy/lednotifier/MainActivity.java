package com.ciubotariu_levy.lednotifier;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class MainActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);  
        //showColorDialog();
    }

    public void showColorDialog(){
    	Intent intent = new Intent(this, ColorActivity.class);
    	startActivity(intent);
    }
    
}
