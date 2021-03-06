package com.ciubotariu_levy.lednotifier.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.util.Log;
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

import com.ciubotariu_levy.lednotifier.ui.widget.BorderedCircularColorView;
import com.ciubotariu_levy.lednotifier.R;
import com.ciubotariu_levy.lednotifier.providers.LedContactInfo;
import com.larswerkman.holocolorpicker.EndColorPicker;
import com.larswerkman.holocolorpicker.OnColorChangedListener;
import com.makeramen.RoundedTransformationBuilder;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

public class ColorVibrateDialog extends DialogFragment implements OnColorChangedListener {

    public static final String TAG = ColorVibrateDialog.class.getName();
    public static final String SILENT = "silent_ringtone";
    public static final String GLOBAL = "application_setting_ringtone";
    private static final String LOOKUP_URI = "lookup_uri";
    private static final String CONTACT_ID = "_id";
    private static final String CONTACT_NAME = "contact_name";
    private static final String CONTACT_NUM = "user_number";
    private static final String CONTACT_COLOR = "user_color";
    private static final String CONTACT_CURRENT_COLOR = "user_current_color";
    private static final String CONTACT_CUSTOM_VIB = "custom_vibrate_pattern";
    private static final String CONTACT_DATA = "contact_data";
    private static final int VIB_NO_REPEAT = -1;
    private static final int REQ_CODE = 1;
    //0xFF000000 to 0xFFFFFFFF
    private int mColor;
    private int mOriginalColor;
    private EndColorPicker mPicker;
    private String mVibratePattern;
    private String mPrevVibratePattern;
    private BorderedCircularColorView mColorState;
    private Vibrator mVibratorService;
    private Button mChooseRingtoneButton;
    private Intent mRingtonePickerIntent;
    private LedContactInfo mContactData;
    public ColorVibrateDialog() {
        //Required Empty Constructor
    }

    public static ColorVibrateDialog getInstance(String name, String number, String lookupUri, long id, int color, String vibratePattern) {
        ColorVibrateDialog dialog = new ColorVibrateDialog();
        Bundle args = new Bundle();
        args.putString(CONTACT_NAME, name);
        args.putString(CONTACT_NUM, number);
        args.putString(LOOKUP_URI, lookupUri);
        args.putLong(CONTACT_ID, id);
        args.putInt(CONTACT_COLOR, color);
        args.putString(CONTACT_CUSTOM_VIB, vibratePattern);
        dialog.setArguments(args);
        return dialog;
    }

    public static ColorVibrateDialog getInstance(LedContactInfo data) {
        ColorVibrateDialog dialog = new ColorVibrateDialog();
        Bundle args = new Bundle();
        args.putParcelable(CONTACT_DATA, data);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContactData = getArguments().getParcelable(CONTACT_DATA);
        mRingtonePickerIntent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        mRingtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        mRingtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        mRingtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        mRingtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        mRingtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Contact ringtone");
    }

    @Override
    public void onColorChanged(int color) {
        mColor = color;
        mColorState.setColor(color);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        view.findViewById(R.id.submit_color).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mVibratePattern = "";
                if (((CheckBox) view.findViewById(R.id.vibrate_checkbox)).isChecked()) {
                    mVibratePattern = ((EditText) view.findViewById(R.id.vib_input)).getText().toString().trim();
                }
                if (!((CheckBox) view.findViewById(R.id.ringtone_checkbox)).isChecked()) {
                    mContactData.ringtoneUri = GLOBAL;
                }
                onConfirm(mColor, mVibratePattern);
                dismiss();
            }
        });
        view.findViewById(R.id.cancel).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //onConfirm (originalColor,prevVibratePattern);
                dismiss();
            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri ringtoneUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                onRingtoneSelected(ringtoneUri == null ? SILENT : ringtoneUri.toString());
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        View view = inflater.inflate(R.layout.custom_contact_dialog, container, false);

        ((TextView) view.findViewById(R.id.contact_name)).setText(mContactData.lastKnownName);
        ((TextView) view.findViewById(R.id.contact_number)).setText(mContactData.lastKnownNumber);

        Transformation transformation = new RoundedTransformationBuilder()
                .borderColor(Color.GRAY)
                .borderWidthDp(1)
                .cornerRadiusDp(30)
                .oval(false)
                .build();
        Uri contactUri = Uri.parse(mContactData.systemLookupUri);
        ImageView contactPic = (ImageView) view.findViewById(R.id.contact_image);
        Picasso.with(getActivity())
                .load(contactUri)
                .placeholder(R.drawable.contact_picture_placeholder)
                .fit()
                .transform(transformation)
                .into(contactPic);

        mOriginalColor = mContactData.color;
        mPrevVibratePattern = mContactData.vibratePattern;
        mColor = mOriginalColor;
        mVibratePattern = mPrevVibratePattern;
        if (savedInstanceState != null) {
            mColor = savedInstanceState.getInt(CONTACT_CURRENT_COLOR, mOriginalColor);
            mVibratePattern = savedInstanceState.getString(CONTACT_CUSTOM_VIB);
        }
        mColorState = (BorderedCircularColorView) view.findViewById(R.id.contact_display_color);
        mColorState.setColor(mColor);
        mPicker = (EndColorPicker) view.findViewById(R.id.colorbar);
        mPicker.setColor(mColor);
        mPicker.setOnColorChangedListener(this);
        final View vibrateHint = view.findViewById(R.id.vib_hint);
        final View vibrateInputContainer = view.findViewById(R.id.vib_input_container);
        final EditText vibrateInput = (EditText) view.findViewById(R.id.vib_input);
        vibrateInput.setMaxHeight(vibrateInput.getHeight());

        Button insertCommaButton = (Button) view.findViewById(R.id.insert_comma);
        insertCommaButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                vibrateInput.append(",");
            }
        });

        mVibratorService = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
        final Button testVibrate = (Button) view.findViewById(R.id.test_vibrate);
        testVibrate.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                long[] pattern = LedContactInfo.getVibratePattern(vibrateInput.getText().toString());
                mVibratorService.vibrate(pattern, VIB_NO_REPEAT);
            }
        });

        CheckBox vibrateCheckbox = (CheckBox) view.findViewById(R.id.vibrate_checkbox);
        vibrateCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    vibrateHint.setVisibility(View.VISIBLE);
                    vibrateInputContainer.setVisibility(View.VISIBLE);
                    testVibrate.setVisibility(View.VISIBLE);
                    if (!TextUtils.isEmpty(mVibratePattern)) {
                        vibrateInput.setText(mVibratePattern);
                        vibrateInput.setSelection(mVibratePattern.length());
                    }
                } else {
                    vibrateHint.setVisibility(View.GONE);
                    vibrateInputContainer.setVisibility(View.GONE);
                    testVibrate.setVisibility(View.GONE);
                }
            }
        });
        vibrateCheckbox.setChecked(!TextUtils.isEmpty(mVibratePattern));

        mChooseRingtoneButton = (Button) view.findViewById(R.id.choose_ringtone);
        mChooseRingtoneButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Uri existingUri = Settings.System.DEFAULT_NOTIFICATION_URI;
                if (SILENT.equals(mContactData.ringtoneUri)) {
                    Log.v(TAG, "mChooseRingtoneButton: onClick: silent picked");
                    existingUri = null;
                } else if (!GLOBAL.equals(mContactData.ringtoneUri)) {
                    Log.v(TAG, "mChooseRingtoneButton: onClick: custom ringtone, updating Intent");
                    existingUri = mContactData.ringtoneUri == null ? existingUri : Uri.parse(mContactData.ringtoneUri);
                }
                mRingtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri);
                startActivityForResult(mRingtonePickerIntent, REQ_CODE);

            }
        });
        CheckBox ringtoneCheckbox = (CheckBox) view.findViewById(R.id.ringtone_checkbox);
        ringtoneCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mChooseRingtoneButton.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });

        boolean hasCustomRingtone = !TextUtils.isEmpty(mContactData.ringtoneUri) && !GLOBAL.equals(mContactData.ringtoneUri);
        ringtoneCheckbox.setChecked(hasCustomRingtone);
        if (!hasCustomRingtone) {
            onRingtoneSelected(GLOBAL);
        } else {
            onRingtoneSelected(mContactData.ringtoneUri);
        }
        return view;
    }

    private void onRingtoneSelected(String uriString) {
        mContactData.ringtoneUri = uriString;
        String buttonText = "No custom ringtone";
        if (SILENT.equals(uriString)) {
            buttonText = "Force silent";
        } else {
            try {
                Uri uri = Uri.parse(uriString);
                if (uri != null && !GLOBAL.equals(uriString)) {
                    Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), uri);
                    buttonText = ringtone.getTitle(getActivity());
                    ringtone.stop();
                }
            } catch (Exception e) {
                Log.e(TAG, "onRingtoneSelected: error", e);
            }
        }
        mChooseRingtoneButton.setText(buttonText);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CONTACT_CURRENT_COLOR, mColor);
        outState.putString(CONTACT_CUSTOM_VIB, mVibratePattern);
    }

    //called when user chooses a color
    private void onConfirm(int color, String vibrate) {
        vibrate = LedContactInfo.addZeroesWhereEmpty(vibrate);
        ContactDetailsUpdateListener listener = null;
        try {
            listener = (ContactDetailsUpdateListener) getParentFragment();
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
        if (listener == null) {
            try {
                listener = (ContactDetailsUpdateListener) getActivity();
            } catch (ClassCastException e) {
                e.printStackTrace();
            }
        }

        if (listener == null) {
            return;
        }

        mContactData.color = color;
        if (TextUtils.isEmpty(vibrate)) {
            mContactData.vibratePattern = "";
        } else {
            mContactData.vibratePattern = vibrate;
        }
        listener.onContactDetailsUpdated(mContactData);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        cleanupAndFinish();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        cleanupAndFinish();
    }

    private void cleanupAndFinish() {
        if (mVibratorService != null) {
            mVibratorService.cancel();
        }
        if ((mContactData == null || mContactData.systemLookupUri == null) && getActivity() != null) {
            getActivity().finish();
        }
    }

    public interface ContactDetailsUpdateListener {
        void onContactDetailsUpdated(LedContactInfo updatedData);
    }
}
