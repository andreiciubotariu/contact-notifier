package com.ciubotariu_levy.lednotifier;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.ciubotariu_levy.lednotifier.providers.LedContactInfo;
import com.larswerkman.holocolorpicker.EndColorPicker;
import com.larswerkman.holocolorpicker.OnColorChangedListener;
import com.makeramen.RoundedTransformationBuilder;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

public class ColorVibrateDialog extends DialogFragment implements OnColorChangedListener {
	//0xFF000000 to 0xFFFFFFFF
	private int mColor;
	private int originalColor;

	private EndColorPicker picker;

	private String vibratePattern;
	private String prevVibratePattern;
	private CircularColorView colorState;
	private Vibrator vibratorService;
	
	public interface ContactDetailsUpdateListener {
		public void onContactDetailsUpdated (String lookupKey, int color, String vibratePattern);
	}

	private static final String LOOKUP_URI = "lookup_uri";
	private static final String CONTACT_ID = "_id";
	private static final String USER_NAME = "user_name";
	private static final String USER_NUM = "user_number";
	private static final String USER_COLOR = "user_color";
	private static final String USER_CURRENT_COLOR = "user_current_color";
	private static final String USER_CUSTOM_VIB = "custom_vibrate_pattern";
	private static final int VIB_NO_REPEAT = -1;

	public static ColorVibrateDialog getInstance (String name, String number, String lookupUri,long id, int color,String vibratePattern){
		ColorVibrateDialog dialog = new ColorVibrateDialog ();
		Bundle args = new Bundle();
		args.putString(USER_NAME, name);
		args.putString(USER_NUM, number);
		args.putString (LOOKUP_URI, lookupUri);
		args.putLong(CONTACT_ID, id);
		args.putInt (USER_COLOR, color);		
		args.putString(USER_CUSTOM_VIB, vibratePattern);
		dialog.setArguments(args);
		return dialog;
	}

	public ColorVibrateDialog(){
		//Required Empty Constructor
	}

	@Override
	public void onColorChanged(int color) {
		mColor = color;
		colorState.setColor(color);
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
				onConfirm (mColor,vibratePattern);
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
	}

	private String getString (Bundle b, String key, String defValue){ //Bundle#getString (String, String) not available on API 11 and below
		if (b.getString(key) == null){
			return defValue;
		}
		
		return b.getString(key);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		View view = inflater.inflate(R.layout.color_vibrate_dialog, container,false);
		Bundle args = getArguments();
		
		((TextView)view.findViewById(R.id.contact_name)).setText (getString(args,USER_NAME, ""));
		((TextView)view.findViewById(R.id.contact_number)).setText (getString(args,USER_NUM, ""));
		
		Transformation transformation = new RoundedTransformationBuilder()
        .borderColor(Color.BLACK)
        .borderWidthDp(0)
        .cornerRadiusDp(30)
        .oval(false)
        .build();
		Uri contactUri = Uri.parse(getString(args, LOOKUP_URI, ""));
		ImageView contactPic = (ImageView) view.findViewById(R.id.contact_image);
		Picasso.with(getActivity())
	    	.load(contactUri)
	    	.placeholder(R.drawable.contact_picture_placeholder)
	    	.fit()
	    	.transform(transformation)
	    	.into(contactPic);
		
		originalColor = args.getInt(USER_COLOR,Color.GRAY);
		prevVibratePattern = args.getString(USER_CUSTOM_VIB);
		mColor = originalColor;
		vibratePattern = prevVibratePattern;
		if (savedInstanceState != null){
			mColor = savedInstanceState.getInt(USER_CURRENT_COLOR, originalColor);
			vibratePattern = savedInstanceState.getString(USER_CUSTOM_VIB);
		}	
		colorState = (CircularColorView) view.findViewById(R.id.contact_display_color);
		colorState.setColor(mColor);
		picker = (EndColorPicker) view.findViewById(R.id.colorbar);	
		picker.setColor(mColor);
		picker.setOnColorChangedListener(this);
		final View vibrateHint = view.findViewById(R.id.vib_hint);
		final EditText vibrateInput = (EditText) view.findViewById(R.id.vib_input);
		vibrateInput.setMaxHeight(vibrateInput.getHeight());
		
		vibratorService = (Vibrator)getActivity().getSystemService(Context.VIBRATOR_SERVICE);
		final Button testVibrate = (Button) view.findViewById(R.id.test_vibrate);
		testVibrate.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				long [] pattern = LedContactInfo.getVibratePattern(vibrateInput.getText().toString());
				vibratorService.vibrate(pattern, VIB_NO_REPEAT);
			}
		});
		
		CheckBox c = (CheckBox) view.findViewById(R.id.vibrate_checkbox);
		c.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked){
					vibrateHint.setVisibility(View.VISIBLE);
					vibrateInput.setVisibility(View.VISIBLE);
					testVibrate.setVisibility(View.VISIBLE);
					if (!TextUtils.isEmpty(vibratePattern)){
						vibrateInput.setText(vibratePattern);
						vibrateInput.setSelection(vibratePattern.length());
					}
				}
				else{
					vibrateHint.setVisibility(View.GONE);
					vibrateInput.setVisibility(View.GONE);
					testVibrate.setVisibility(View.GONE);
				}
				
			}
		});
		c.setChecked(!TextUtils.isEmpty(vibratePattern));
		return view;
	}
	
	@Override
	public void onSaveInstanceState (Bundle outState){
		super.onSaveInstanceState(outState);
		outState.putInt(USER_CURRENT_COLOR, mColor);
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
			listener.onContactDetailsUpdated(getArguments().getString(LOOKUP_URI), color, vibrate);
		}
	}
	
	@Override
	public void onCancel(DialogInterface dialog){
		super.onCancel(dialog);
		cleanupAndFinish();
	}

	@Override
	public void onDismiss(DialogInterface dialog){
		super.onDismiss(dialog);
		cleanupAndFinish();
	}

	private void cleanupAndFinish(){
		if (vibratorService != null){
			vibratorService.cancel();
		}
		if (getArguments().getString(LOOKUP_URI) == null && getActivity() != null){
			getActivity().finish();
		}
	}
}
