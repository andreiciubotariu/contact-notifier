package com.ciubotariu_levy.lednotifier;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;

public class ColorDialog extends DialogFragment {
	
	public interface OnColorChosenListener {
		public void onColorChosen (int color, long rowID);
	}
	
	private final static String ROW_ID = "row_id";
	
	private ColorView colorView;
	private ColorView colorView2;
	private ColorView colorView3;
	private ColorView colorView4;

	public static ColorDialog getInstance (long rowID){
		ColorDialog dialog = new ColorDialog ();
		Bundle args = new Bundle();
		args.putLong (ROW_ID, rowID);
		dialog.setArguments(args);
		return dialog;
	}
	public ColorDialog(){
		//Required Empty Constructor
	}
	
	@Override
	public void onViewCreated (View view, Bundle savedInstanceState){
		view.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				onColorChosen (Color.CYAN);
				dismiss();
			}
		});
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
	
	//called when user chooses a color
	private void onColorChosen (int color){
		OnColorChosenListener listener = null;
		try{
			listener =(OnColorChosenListener) getParentFragment();
				
		}
		catch (ClassCastException e){
			e.printStackTrace();
		}
		if (listener != null){
			listener.onColorChosen(color, getArguments().getLong(ROW_ID));
		}
	}
}
