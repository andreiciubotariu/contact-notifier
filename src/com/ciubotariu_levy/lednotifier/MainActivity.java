package com.ciubotariu_levy.lednotifier;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);  
        showColorDialog();
    }

    public void showColorDialog(){
    	Intent intent = new Intent(this, ColorActivity.class);
    	startActivity(intent);
    }
    
}
