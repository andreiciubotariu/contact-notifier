package com.ciubotariu_levy.lednotifier;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;

import com.ciubotariu_levy.lednotifier.ColorWheel.ColorListener;

public class ColorVibrateDialog extends DialogFragment implements ColorListener {
	//0xFF000000 to 0xFFFFFFFF
	private int color;
	private int originalColor;

	private ColorWheel sample;

	private String vibratePattern;
	private String prevVibratePattern;
	
	public interface ContactDetailsUpdateListener {
		public void onContactDetailsUpdated (String lookupKey, int color, String vibratePattern);
	}

	private final static String LOOKUP_KEY_VALUE = "row_id";
	private final static String USER_COLOR = "user_color";
	private final static String USER_CURRENT_COLOR = "user_color";
	private final static String USER_CUSTOM_VIB = "custom_vibrate_pattern";
	

	public static ColorVibrateDialog getInstance (String lookupKey, int color,String vibratePattern){
		ColorVibrateDialog dialog = new ColorVibrateDialog ();
		Bundle args = new Bundle();
		args.putString (LOOKUP_KEY_VALUE, lookupKey);
		args.putInt (USER_COLOR, color);		
		args.putString(USER_CUSTOM_VIB, vibratePattern);
		dialog.setArguments(args);
		return dialog;
	}

	public ColorVibrateDialog(){
		//Required Empty Constructor
	}

	@Override
	public void setColor(int color){
		this.color = color;
	}

	@Override
	public void onViewCreated (final View view, Bundle savedInstanceState){
		view.findViewById(R.id.submit_color).setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				vibratePattern = null;
				if (((CheckBox)view.findViewById(R.id.vibrate_checkbox)).isChecked()){
					vibratePattern = ((EditText)view.findViewById(R.id.vib_input)).getText().toString().trim();
				}
				onConfirm (color,vibratePattern);
				dismiss();
			}
		});
		view.findViewById(R.id.cancel).setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				onConfirm (originalColor,prevVibratePattern);
				dismiss();
			}
		});
		view.findViewById(R.id.reset_color).setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				//onColorChosen (Color.GRAY);
				sample.setColor(Color.GRAY);
				//dismiss();
			}
		});
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		View view = inflater.inflate(R.layout.color_vibrate_dialog, container,false);
		Bundle args = getArguments();
		originalColor = args.getInt(USER_COLOR,Color.GRAY);
		prevVibratePattern = args.getString(USER_CUSTOM_VIB);
		color = originalColor;
		vibratePattern = prevVibratePattern;
		if (savedInstanceState != null){
			color = savedInstanceState.getInt(USER_CURRENT_COLOR, originalColor);
			vibratePattern = savedInstanceState.getString(USER_CUSTOM_VIB);
		}		
		sample = (ColorWheel) view.findViewById(R.id.wheel);
		sample.setDialog(this);
		sample.lockWidth=true;
		sample.setColor(color);	
		final View vibrateHint = view.findViewById(R.id.vib_hint);
		final EditText vibrateInput = (EditText) view.findViewById(R.id.vib_input);
		CheckBox c = (CheckBox) view.findViewById(R.id.vibrate_checkbox);
		c.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked){
					vibrateHint.setVisibility(View.VISIBLE);
					vibrateInput.setVisibility(View.VISIBLE);
					if (!TextUtils.isEmpty(vibratePattern)){
						vibrateInput.setText(vibratePattern);
						vibrateInput.setSelection(vibratePattern.length());
					}
				}
				else{
					vibrateHint.setVisibility(View.GONE);
					vibrateInput.setVisibility(View.GONE);
				}
				
			}
		});
		c.setChecked(!TextUtils.isEmpty(vibratePattern));
		return view;
	}
	
	public void onSaveInstaceState (Bundle outState){
		super.onSaveInstanceState(outState);
		outState.putInt(USER_CURRENT_COLOR, color);
		outState.putString(USER_CUSTOM_VIB, vibratePattern);
	}

	//called when user chooses a color
	private void onConfirm (int color, String vibrate){
		ContactDetailsUpdateListener listener = null;
		try{
			listener =(ContactDetailsUpdateListener) getParentFragment();
		}
		catch (ClassCastException e){
			e.printStackTrace();
		}
		if (listener == null){
			try{
				listener = (ContactDetailsUpdateListener) getActivity();
			}
			catch (ClassCastException e){
				e.printStackTrace();
			}
		}
		if (listener != null){
			listener.onContactDetailsUpdated(getArguments().getString(LOOKUP_KEY_VALUE), color, vibrate);
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
