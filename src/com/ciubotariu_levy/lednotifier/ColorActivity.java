package com.ciubotariu_levy.lednotifier;

import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class ColorActivity extends FragmentActivity{
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		showColorDialog();
	}
	
	public void showColorDialog(){
		FragmentManager fm = getSupportFragmentManager();
		ColorDialog cd = new ColorDialog();
		cd.show(fm, "Hello");
	}
}
