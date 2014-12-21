package com.ciubotariu_levy.lednotifier;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.contacts.common.lettertiles.SimpleLetterTileDrawable;
import com.ciubotariu_levy.lednotifier.providers.LedContactInfo;
import com.larswerkman.holocolorpicker.EndColorPicker;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

public abstract class AbstractContactViewBinder {

    protected abstract boolean hasColorView();
    protected abstract Uri getContactUri (Cursor cursor);
    protected abstract String getName(Cursor cursor);
    protected abstract int getColor (Cursor cursor, String contactUri);
    protected abstract String getRingtoneUri (Cursor cursor, String contactUri);
    protected abstract String getVibPattern (Cursor cursor, String contactUri);

    public static interface ContactListener {
        public void onContactSelected (int pos, long id);
        public void startForResult (Intent intent, int requestCode);
    }

    public static class ContactHolder extends RecyclerView.ViewHolder {
        public TextView mName;
        public ImageView mPic;
        public View mRowContainer,mVib, mRingtone,mContainer;
        public BorderedCircularColorView mColor;
        public View customControls, vibrateHint, vibrateInputContainer;
        public EndColorPicker colorPicker;
        public EditText vibrateInput;
        public Button insertCommaButton, testVibrate, chooseRingtoneButton,ringtoneButton;
        public CheckBox vibrateCheckbox, ringtoneCheckbox;


        public ContactHolder(View v, boolean hasColor) {
            super(v);
            mRowContainer = v.findViewById(R.id.contact_row_container);
            mName = (TextView) v.findViewById(R.id.contact_name);
            mPic = (ImageView)v.findViewById(R.id.contact_image);
            if (hasColor) {
                mColor = (BorderedCircularColorView) v.findViewById(R.id.contact_display_color);
            }
            mContainer = v.findViewById(R.id.custom_ring_vib_container);
            mRingtone = v.findViewById(R.id.contact_ringtone);
            mVib = v.findViewById(R.id.contact_vibrate);
            customControls = v.findViewById(R.id.test_view);

        }
    }

    public static final String SILENT = "silent_ringtone";
    public static final String GLOBAL = "application_setting_ringtone";
    private static final int VIB_NO_REPEAT = -1;
    private static final int REQ_CODE = 1;

    private Transformation mTransformation;
    private ContactListener mListener;

    int expPos = -1;
    boolean isExpanded = false;
    int expColor = Color.GRAY;
    String expRingtoneUri;
    String expVibPattern;
    ContactHolder expHolder;

    public AbstractContactViewBinder(Transformation t, ContactListener listener) {
        mTransformation = t;
        mListener = listener;
    }


    private void resetExpandedStatus() {
        expPos = -1;
        isExpanded = false;
        expColor = Color.GRAY;
        expRingtoneUri = null;
        expVibPattern = null;
        expHolder = null;
    }

    private void setExpandedData(final ContactHolder holder) {
        holder.colorPicker.setColor(expColor);
        //mPicker.setOnColorChangedListener(this);
        holder.vibrateInput.setMaxHeight(holder.vibrateInput.getHeight());

        holder.insertCommaButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                holder.vibrateInput.append(",");
            }
        });

        final Vibrator vibratorService = (Vibrator)holder.mName.getContext().getSystemService(Context.VIBRATOR_SERVICE);
        holder.testVibrate.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                long [] pattern = LedContactInfo.getVibratePattern(holder.vibrateInput.getText().toString());
                vibratorService.vibrate(pattern, VIB_NO_REPEAT);
            }
        });

        holder.vibrateCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    holder.vibrateHint.setVisibility(View.VISIBLE);
                    holder.vibrateInputContainer.setVisibility(View.VISIBLE);
                    holder.testVibrate.setVisibility(View.VISIBLE);
                    if (!TextUtils.isEmpty(expVibPattern)){
                        holder.vibrateInput.setText(expVibPattern);
                        holder.vibrateInput.setSelection(expVibPattern.length());
                    }
                }
                else{
                    holder.vibrateHint.setVisibility(View.GONE);
                    holder.vibrateInputContainer.setVisibility(View.GONE);
                    holder.testVibrate.setVisibility(View.GONE);
                }
            }
        });
        holder.vibrateCheckbox.setChecked(!TextUtils.isEmpty(expVibPattern));

        holder.chooseRingtoneButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Uri existingUri = Settings.System.DEFAULT_NOTIFICATION_URI;
                if (SILENT.equals(expRingtoneUri)){
                    Log.i("RingtonePicker", "silent picked");
                    existingUri = null;
                } else if (!GLOBAL.equals(expRingtoneUri)){
                    Log.i("RingtonePicker", "Custom ringtone. Updating Intent.");
                    existingUri =  expRingtoneUri == null ? existingUri : Uri.parse(expRingtoneUri);
                }
                Intent ringtonePickerIntent = new Intent (RingtoneManager.ACTION_RINGTONE_PICKER);
                ringtonePickerIntent.putExtra (RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
                ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
                ringtonePickerIntent.putExtra (RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                ringtonePickerIntent.putExtra (RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
                ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Contact ringtone");
                ringtonePickerIntent.putExtra (RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri);
                startActivityForResult(ringtonePickerIntent, REQ_CODE);

            }
        });

        holder.ringtoneCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                holder.chooseRingtoneButton.setVisibility (isChecked ? View.VISIBLE : View.GONE);
            }
        });

        boolean hasCustomRingtone =  !TextUtils.isEmpty(expRingtoneUri) && !GLOBAL.equals(expRingtoneUri);
        holder.ringtoneCheckbox.setChecked(hasCustomRingtone);
        if (!hasCustomRingtone){
            onRingtoneSelected(GLOBAL);
        } else {
            onRingtoneSelected(expRingtoneUri);
        }

    }

    private void onRingtoneSelected (String uriString){
        Button chooseRingtoneButton = expHolder.chooseRingtoneButton;
        expRingtoneUri = uriString;
        String buttonText = "No custom ringtone";
        if (SILENT.equals(uriString)){
            buttonText = "Force silent";
        } else {
            try {

                Uri uri = Uri.parse(uriString);
                if (uri != null && !GLOBAL.equals(uriString)){
                    Ringtone ringtone =  RingtoneManager.getRingtone(chooseRingtoneButton.getContext(), uri);
                    buttonText = ringtone.getTitle(chooseRingtoneButton.getContext());
                    ringtone.stop();
                }
            } catch (Exception e){
                Log.e("RingtoneTitle", "Error");
                e.printStackTrace();
            }
        }
        chooseRingtoneButton.setText(buttonText);
    }

    public void onActivityResult (int requestCode, int resultCode, Intent data){
        if (requestCode == REQ_CODE){
            if (resultCode == Activity.RESULT_OK){
                Uri ringtoneUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                onRingtoneSelected(ringtoneUri == null ? SILENT : ringtoneUri.toString());
            }
        }
    }

    public void bind(final RecyclerView.ViewHolder holder, Cursor cursor, Context context) {
        final ContactHolder viewHolder = (ContactHolder) holder;

        final int currentPos = holder.getPosition();

        int numberGone = 0;
        Uri contactUri = getContactUri(cursor);

        String name  = getName(cursor);
        if (name != null) {
            final SpannableStringBuilder str = new SpannableStringBuilder(name);
            str.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0,
                    name.indexOf(' ') != -1 ? name.indexOf(' ') : name.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            viewHolder.mName.setText(str);
        }

        SimpleLetterTileDrawable letterTileDrawable = new SimpleLetterTileDrawable(context.getResources());
        letterTileDrawable.setIsCircular(true);
        letterTileDrawable.setContactDetails(name, contactUri.toString());

        Picasso.with(context)
               .load(contactUri)
               .placeholder(letterTileDrawable)
               .fit()
               .transform(mTransformation)
               .into(viewHolder.mPic);

        if (hasColorView()) {
            int color = getColor(cursor, contactUri.toString());
            viewHolder.mColor.setColor(color);
        }

        final String ringtoneUri = getRingtoneUri (cursor, contactUri.toString());
        if (!TextUtils.isEmpty(ringtoneUri) && !ColorVibrateDialog.GLOBAL.equals(ringtoneUri)){
            viewHolder.mRingtone.setVisibility(View.VISIBLE);
            viewHolder.mRingtone.setBackgroundResource(R.drawable.ic_custom_ringtone);
            viewHolder.mContainer.setVisibility(View.VISIBLE);
        }
        else {
            viewHolder.mRingtone.setVisibility(View.GONE);
            numberGone++;
        }
        final String vibratePattern = getVibPattern (cursor, contactUri.toString());
        if (!TextUtils.isEmpty(vibratePattern)){
            viewHolder.mVib.setVisibility(View.VISIBLE);
            viewHolder.mVib.setBackgroundResource(R.drawable.ic_contact_vibrate);
            viewHolder.mContainer.setVisibility(View.VISIBLE);
        }
        else {
            viewHolder.mVib.setVisibility(View.GONE);
            numberGone++;
        }

        if (numberGone == 2){
            viewHolder.mContainer.setVisibility(View.GONE);
        }

        if (currentPos == expPos) {
            setExpandedData((ContactHolder)holder);
        }
        ((ContactHolder) holder).customControls.setVisibility(holder.getPosition() == expPos ? View.VISIBLE : View.GONE);

        viewHolder.mRowContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    if  (currentPos == expPos && isExpanded) {
                        expHolder.customControls.setVisibility(View.GONE);
                        resetExpandedStatus();
                    } else {
                        ((ContactHolder) holder).customControls.setVisibility(View.VISIBLE);
                        expPos = currentPos;
                        if (expHolder != null) {
                            expHolder.customControls.setVisibility(View.GONE);
                        }
                        expHolder = (ContactHolder) holder;
                        expRingtoneUri = ringtoneUri;
                        if (expHolder.mColor != null) {
                            expColor = expHolder.mColor.getColor();
                        }

                        expVibPattern = vibratePattern;
                        isExpanded = true;
                    }

                    //mListener.onContactSelected(viewHolder.getPosition(), viewHolder.getItemId());
                }
            }
        });
    }


}
