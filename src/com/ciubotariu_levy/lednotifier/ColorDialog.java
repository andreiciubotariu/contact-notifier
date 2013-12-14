package com.ciubotariu_levy.lednotifier;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

public class ColorDialog extends DialogFragment  {
	ColorView colorView;
	ColorView colorView2;
	ColorView colorView3;
	ColorView colorView4;

	public ColorDialog(){
		//Required Empty Constructor
	}
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance){
		getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		View view = inflater.inflate(R.layout.color_dialog, container);
		colorView = (ColorView) view.findViewById(R.id.color_view);
		colorView.setColor(0xFF00FF00);
		colorView.lockWidth=true;
		colorView2 = (ColorView) view.findViewById(R.id.color_view2);
		colorView2.setColor(0xFFFF0000);
		colorView2.lockWidth=true;
		colorView3 = (ColorView) view.findViewById(R.id.color_view3);
		colorView3.setColor(0xFF0000FF);
		colorView3.lockWidth=true;
		colorView4 = (ColorView) view.findViewById(R.id.color_view4);
		colorView4.setColor(0xFFFFFF00);
		colorView4.lockWidth=true;
		return view;
		
	}
}
