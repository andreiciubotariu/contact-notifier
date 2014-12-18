package com.ciubotariu_levy.lednotifier;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.contacts.common.lettertiles.SimpleLetterTileDrawable;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

public abstract class AbstractContactViewBinder {

    protected abstract boolean hasColorView();
    protected abstract Uri getContactUri (Cursor cursor);
    protected abstract String getName(Cursor cursor);
    protected abstract int getColor (Cursor cursor, String contactUri);
    protected abstract String getRingtoneUri (Cursor cursor, String contactUri);
    protected abstract String getVibPattern (Cursor cursor, String contactUri);

    public static interface ContactClickListener {
        public void onContactSelected (int pos, long id);
    }
    public static class ContactHolder extends RecyclerView.ViewHolder {
        public TextView mName;
        public ImageView mPic;
        public View mRowContainer,mVib, mRingtone,mContainer;
        public BorderedCircularColorView mColor;
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
        }
    }

    private Transformation mTransformation;
    private ContactClickListener mListener;
    public AbstractContactViewBinder(Transformation t, ContactClickListener listener) {
        mTransformation = t;
        mListener = listener;
    }

    public void bind(RecyclerView.ViewHolder holder, Cursor cursor, Context context) {
        final ContactHolder viewHolder = (ContactHolder) holder;
        viewHolder.mRowContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onContactSelected(viewHolder.getPosition(), viewHolder.getItemId());
                }
            }
        });
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

        String ringtoneUri = getRingtoneUri (cursor, contactUri.toString());
        if (!TextUtils.isEmpty(ringtoneUri) && !ColorVibrateDialog.GLOBAL.equals(ringtoneUri)){
            viewHolder.mRingtone.setVisibility(View.VISIBLE);
            viewHolder.mRingtone.setBackgroundResource(R.drawable.ic_custom_ringtone);
            viewHolder.mContainer.setVisibility(View.VISIBLE);
        }
        else {
            viewHolder.mRingtone.setVisibility(View.GONE);
            numberGone++;
        }
        String vibratePattern = getVibPattern (cursor, contactUri.toString());
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
    }
}
