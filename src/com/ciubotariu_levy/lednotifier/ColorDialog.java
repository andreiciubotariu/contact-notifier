package com.ciubotariu_levy.lednotifier;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ColorDialog extends DialogFragment  {
	public ColorDialog(){
		//Required Empty Constructor
	}
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance){
		View view = inflater.inflate(R.layout.color_dialog, container);		
		return view;
		
	}
}
