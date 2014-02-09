package com.ciubotariu_levy.lednotifier;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;

import com.ciubotariu_levy.lednotifier.ColorWheel.ColorListener;

public class ColorDialog extends DialogFragment implements ColorListener{
	//0xFF000000 to 0xFFFFFFFF
	private int color;

	private int originalColor;
	
	private ColorWheel sample;


	public interface OnColorChosenListener {
		public void onColorChosen (int color, String lookupKey);
	}

	private final static String LOOKUP_KEY_VALUE = "row_id";
	private final static String USER_COLOR = "user_color";
	private final static String USER_CURRENT_COLOR = "user_color";

	public static ColorDialog getInstance (String lookupKey, int color){
		ColorDialog dialog = new ColorDialog ();
		Bundle args = new Bundle();
		args.putString (LOOKUP_KEY_VALUE, lookupKey);
		args.putInt (USER_COLOR, color);		
		dialog.setArguments(args);
		return dialog;
	}

	public ColorDialog(){
		//Required Empty Constructor
	}

	@Override
	public void setColor(int color){
		this.color = color;
	}

	@Override
	public void onViewCreated (View view, Bundle savedInstanceState){
		view.findViewById(R.id.submit_color).setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				onColorChosen (color);
				dismiss();
			}
		});
		view.findViewById(R.id.cancel).setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				onColorChosen (originalColor);
				dismiss();
			}
		});
		view.findViewById(R.id.reset_color).setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				onColorChosen (Color.GRAY);
				dismiss();
			}
		});
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		View view = inflater.inflate(R.layout.color_dialog, container,false);
		Bundle args = getArguments();
		originalColor = args.getInt(USER_COLOR,Color.GRAY);
		color = originalColor;
		if (savedInstanceState != null){
			color = savedInstanceState.getInt(USER_CURRENT_COLOR, originalColor);
		}		
		sample = (ColorWheel) view.findViewById(R.id.wheel);
		sample.setDialog(this);
		sample.lockWidth=true;
		sample.setColor(color);		
		return view;
	}
	
	public void onSaveInstaceState (Bundle outState){
		super.onSaveInstanceState(outState);
		outState.putInt(USER_CURRENT_COLOR, color);
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
		if (listener == null){
			try{
				listener = (OnColorChosenListener) getActivity();
			}
			catch (ClassCastException e){
				e.printStackTrace();
			}
		}
		if (listener != null){
			listener.onColorChosen(color, getArguments().getString(LOOKUP_KEY_VALUE));
		}
	}
	
	@Override
	public void onCancel(DialogInterface dialog){
		super.onCancel(dialog);
		finishHostActivity();
	}

	@Override
	public void onDismiss(DialogInterface dialog){
		super.onDismiss(dialog);
		finishHostActivity();
	}

	private void finishHostActivity(){
		if (getArguments().getString(LOOKUP_KEY_VALUE) == null && getActivity() != null){
			getActivity().finish();
		}
	}
}
