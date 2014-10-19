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

import com.larswerkman.holocolorpicker.EndColorPicker;
import com.larswerkman.holocolorpicker.OnColorChangedListener;

public class ColorDialog extends DialogFragment implements OnColorChangedListener{
	public interface OnColorChosenListener {
		public void onColorChosen (int color, String lookupKey);
	}

	private static final String LOOKUP_KEY_VALUE = "row_id";
	private static final String USER_COLOR = "user_color";
	private static final String USER_CURRENT_COLOR = "user_current_color";
	
	//0xFF000000 to 0xFFFFFFFF
	private int mColor;

	private int mOriginalColor;
	
	private EndColorPicker mPicker;
	private View mColorState;

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
	public void onColorChanged(int color){
		this.mColor = color;
		mColorState.setBackgroundColor(color);
	}

	@Override
	public void onViewCreated (View view, Bundle savedInstanceState){
		view.findViewById(R.id.submit_color).setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				onColorChosen (mColor);
				dismiss();
			}
		});
		view.findViewById(R.id.cancel).setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				onColorChosen (mOriginalColor);
				dismiss();
			}
		});
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		View view = inflater.inflate(R.layout.color_dialog, container,false);
		Bundle args = getArguments();
		mOriginalColor = args.getInt(USER_COLOR,Color.GRAY);
		mColor = mOriginalColor;
		if (savedInstanceState != null){
			mColor = savedInstanceState.getInt(USER_CURRENT_COLOR, mOriginalColor);
		}		
		mColorState = view.findViewById(R.id.display_color);
		mColorState.setBackgroundColor(mColor);
		
		mPicker = (EndColorPicker) view.findViewById(R.id.colorbar);
		mPicker.setColor(mColor);		
		mPicker.setOnColorChangedListener(this);
		return view;
	}
	
	@Override
	public void onSaveInstanceState (Bundle outState){
		super.onSaveInstanceState(outState);
		outState.putInt(USER_CURRENT_COLOR, mColor);
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
