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
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.contacts.common.lettertiles.SimpleLetterTileDrawable;
import com.ciubotariu_levy.lednotifier.providers.LedContactInfo;
import com.larswerkman.holocolorpicker.EndColorPicker;
import com.larswerkman.holocolorpicker.OnColorChangedListener;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.ValueAnimator;
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
        public LedContactInfo onContactSelected (int pos, long id);
        public void startForResult (Intent intent, int requestCode);
    }

    public static class ContactHolder extends RecyclerView.ViewHolder {
        public TextView mName;
        public ImageView mPic;
        public View mRowContainer,mVib, mRingtone,mContainer;
        public BorderedCircularColorView mColor;
        public View customControls, vibrateHint, vibrateInputContainer;
        public EndColorPicker mColorPicker;
        public EditText mVibrateInput;
        public Button mInsertCommaButton, mTestVibrate, mChooseRingtoneButton;
        public CheckBox mVibrateCheckbox, mRingtoneCheckbox;

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

            customControls = v.findViewById(R.id.custom_controls);
            vibrateHint = v.findViewById(R.id.vib_hint);
            vibrateInputContainer = v.findViewById(R.id.vib_input_container);

            mColorPicker = (EndColorPicker) v.findViewById(R.id.colorbar);

            mVibrateInput = (EditText) v.findViewById(R.id.vib_input);

            mInsertCommaButton = (Button) v.findViewById(R.id.insert_comma);
            mTestVibrate = (Button) v.findViewById(R.id.test_vibrate);
            mChooseRingtoneButton = (Button) v.findViewById(R.id.choose_ringtone);

            mVibrateCheckbox = (CheckBox) v.findViewById(R.id.vibrate_checkbox);
            mRingtoneCheckbox = (CheckBox) v.findViewById(R.id.ringtone_checkbox);
        }
    }

    public static final String SILENT = "silent_ringtone";
    public static final String GLOBAL = "application_setting_ringtone";
    private static final int VIB_NO_REPEAT = -1;
    private static final int REQ_CODE = 1;

    private Transformation mTransformation;
    private ContactListener mListener;
    private int expPos = -1, mExpColor = Color.GRAY;
    private boolean mIsExpanded = false;
    private String mExpRingtoneUri, mExpVibPattern;
    private ContactHolder mExpHolder;
    private LedContactInfo mInfo;
    private Animator mCurrentAnimator = null;

    public AbstractContactViewBinder(Transformation t, ContactListener listener) {
        mTransformation = t;
        mListener = listener;
    }

    private Animator createRowAnimator(int originalValue, int finalValue, final View toModify, Animator.AnimatorListener listener) {
        ValueAnimator valueAnimator = ValueAnimator.ofInt(originalValue, finalValue);
        if(listener != null) {
            valueAnimator.addListener(listener);
        }
        valueAnimator.setDuration(300);
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Integer value = (Integer) animation.getAnimatedValue();
                ((ViewGroup.MarginLayoutParams) toModify.getLayoutParams()).topMargin = value.intValue();
                toModify.requestLayout();
            }
        });
        mCurrentAnimator = valueAnimator;
        return valueAnimator;
    }

    private void openRow(int currentPos, ContactHolder holder, String ringtoneUri, String vibratePattern) {
        Log.v("ROW", "opening row");
        expPos = currentPos;
        mExpHolder = holder;
        mExpRingtoneUri = ringtoneUri;
        if (mExpHolder.mColor != null) {
            mExpColor = mExpHolder.mColor.getColor();
        }

        mExpVibPattern = vibratePattern;
        mIsExpanded = true;
        mInfo = mListener.onContactSelected(expPos, mExpHolder.getItemId());
        mExpColor = mInfo.color;
        mExpRingtoneUri = mInfo.ringtoneUri;
        mExpVibPattern = mInfo.vibratePattern;

        setExpandedData(mExpHolder);

        holder.customControls.setVisibility(View.VISIBLE);
        int height = -holder.customControls.getHeight();
        if (height <= 0) {
            height = -500; //TODO: change to use dip
        }
        createRowAnimator(height, holder.mRowContainer.getHeight(), holder.customControls, null).start();
    }

    private void resetExpandedStatus(final Runnable onceFinished) {
        Animator animator = createRowAnimator((mExpHolder).mRowContainer.getHeight(),
                -(mExpHolder).customControls.getHeight(),
                (mExpHolder).customControls,
                new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mExpHolder.customControls.setVisibility(View.GONE);
                if (mExpHolder != null) {
                    ((Vibrator) mExpHolder.mName.getContext().getSystemService(Context.VIBRATOR_SERVICE)).cancel();
                }
                expPos = -1;
                mIsExpanded = false;
                mExpColor = Color.GRAY;
                mExpRingtoneUri = null;
                mExpVibPattern = null;

                mExpHolder.mRingtoneCheckbox.setChecked(false);
                mExpHolder.mVibrateCheckbox.setChecked(false);
                mExpHolder.mVibrateInput.setText("");
                mExpHolder.mColorPicker.setColor(Color.GRAY);
                mExpHolder.mChooseRingtoneButton.setText("No custom ringtone");
                mExpHolder = null;

                if (onceFinished != null) {
                    onceFinished.run();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });

        animator.start();
    }

    private void setExpandedData(final ContactHolder holder) {
        holder.mColorPicker.setColor(mExpColor);
        holder.mColorPicker.setOnColorChangedListener(new OnColorChangedListener() {
            @Override
            public void onColorChanged(int color) {
                mExpColor = color;
            }
        });
        //mPicker.setOnColorChangedListener(this);
        holder.mVibrateInput.setMaxHeight(holder.mVibrateInput.getHeight());

        holder.mInsertCommaButton.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View v) {
                holder.mVibrateInput.append(",");
            }
        });

        final Vibrator vibratorService = (Vibrator)holder.mName.getContext().getSystemService(Context.VIBRATOR_SERVICE);
        holder.mTestVibrate.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                long[] pattern = LedContactInfo.getVibratePattern(holder.mVibrateInput.getText().toString());
                vibratorService.vibrate(pattern, VIB_NO_REPEAT);
            }
        });

        holder.mVibrateCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    holder.vibrateHint.setVisibility(View.VISIBLE);
                    holder.vibrateInputContainer.setVisibility(View.VISIBLE);
                    holder.mTestVibrate.setVisibility(View.VISIBLE);
                    if (!TextUtils.isEmpty(mExpVibPattern)){
                        holder.mVibrateInput.setText(mExpVibPattern);
                        holder.mVibrateInput.setSelection(mExpVibPattern.length());
                    }
                }
                else{
                    holder.vibrateHint.setVisibility(View.GONE);
                    holder.vibrateInputContainer.setVisibility(View.GONE);
                    holder.mTestVibrate.setVisibility(View.GONE);
                }
            }
        });
        holder.mVibrateCheckbox.setChecked(!TextUtils.isEmpty(mExpVibPattern));

        holder.mChooseRingtoneButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Uri existingUri = Settings.System.DEFAULT_NOTIFICATION_URI;
                if (SILENT.equals(mExpRingtoneUri)) {
                    Log.i("RingtonePicker", "silent picked");
                    existingUri = null;
                } else if (!GLOBAL.equals(mExpRingtoneUri)) {
                    Log.i("RingtonePicker", "Custom ringtone. Updating Intent.");
                    existingUri = mExpRingtoneUri == null ? existingUri : Uri.parse(mExpRingtoneUri);
                }
                Intent ringtonePickerIntent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,
                        RingtoneManager.TYPE_NOTIFICATION);

                ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

                ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
                ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Contact ringtone");
                ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri);
                if (mListener != null) {
                    mListener.startForResult(ringtonePickerIntent, REQ_CODE);
                }
            }
        });

        holder.mRingtoneCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                holder.mChooseRingtoneButton.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });

        boolean hasCustomRingtone =  !TextUtils.isEmpty(mExpRingtoneUri) && !GLOBAL.equals(mExpRingtoneUri);
        holder.mRingtoneCheckbox.setChecked(hasCustomRingtone);
        if (!hasCustomRingtone){
            onRingtoneSelected(GLOBAL);
        } else {
            onRingtoneSelected(mExpRingtoneUri);
        }
    }

    private void onRingtoneSelected (String uriString){
        Button chooseRingtoneButton = mExpHolder.mChooseRingtoneButton;
        mExpRingtoneUri = uriString;
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

    public void onResult (int requestCode, int resultCode, Intent data){
        if (requestCode == REQ_CODE){
            if (resultCode == Activity.RESULT_OK){
                Uri ringtoneUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                onRingtoneSelected(ringtoneUri == null ? SILENT : ringtoneUri.toString());
            }
        }
    }

    private void onConfirm (){
        mExpVibPattern = "";
        if (mExpHolder.mVibrateCheckbox.isChecked()){
            mExpVibPattern = mExpHolder.mVibrateInput.getText().toString().trim();
        }
        if (!mExpHolder.mRingtoneCheckbox.isChecked()){
            mExpRingtoneUri = GLOBAL;
        }

        //
        mExpVibPattern = LedContactInfo.addZeroesWhereEmpty (mExpVibPattern);
        if (TextUtils.isEmpty(mExpVibPattern)){
            mExpVibPattern = "";
        }

        mInfo.ringtoneUri = mExpRingtoneUri;
        mInfo.color = mExpColor;
        mInfo.vibratePattern = mExpVibPattern;
        ((ColorVibrateDialog.ContactDetailsUpdateListener)mListener).onContactDetailsUpdated(mInfo);
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

        ((ContactHolder) holder).customControls.setVisibility(
                holder.getPosition() == expPos ? View.VISIBLE : View.GONE);

        viewHolder.mRowContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentAnimator != null && mCurrentAnimator.isStarted()) {
                    return;
                }
                if (mListener != null) {
                    if  (currentPos == expPos && mIsExpanded) {
                        onConfirm();
                        resetExpandedStatus(null);
                    } else {
                        if (mExpHolder != null) {
                            resetExpandedStatus(new Runnable(){

                                @Override
                                public void run() {
                                    openRow(currentPos, (ContactHolder)holder, ringtoneUri,vibratePattern);
                                }
                            });
                        } else {
                            openRow(currentPos, (ContactHolder)holder, ringtoneUri,vibratePattern);
                        }
                    }
                }
            }
        });
    }
}