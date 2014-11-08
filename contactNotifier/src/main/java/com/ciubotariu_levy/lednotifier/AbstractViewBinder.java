package com.ciubotariu_levy.lednotifier;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

public abstract class AbstractViewBinder implements SimpleCursorAdapter.ViewBinder {
    private int numberGone;
    private View container;
    private int prevRow = -1;
    private Context context;
    private Transformation transformation;

    protected abstract Uri getContactUri (Cursor cursor);
    protected abstract String getName(Cursor cursor);
    protected abstract int getColor (Cursor cursor, String contactUri);
    protected abstract String getRingtoneUri (Cursor cursor, String contactUri);
    protected abstract String getVibPattern (Cursor cursor, String contactUri);

    public AbstractViewBinder(Context c, Transformation t) {
        context = c;
        transformation = t;
    }

    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        if (cursor.getPosition() != prevRow){
            prevRow = cursor.getPosition();
            numberGone = 0;
            container = null;
        }

        boolean overridden = false;
        Uri contactUri = getContactUri(cursor);
//                Uri contactUri = ContactsContract.Contacts.getLookupUri(cursor.getLong(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)), cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)));

        switch (view.getId()){
            case R.id.contact_name:
                String name  = getName(cursor);
                if (name != null) {
                    final SpannableStringBuilder str = new SpannableStringBuilder(name);
                    str.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, name.indexOf(' ') != -1 ? name.indexOf(' ') : name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ((TextView) view).setText(str);
                }
                overridden = true;
                break;
            case R.id.contact_image:
                Picasso.with(context)
                        .load(contactUri)
                        .placeholder(R.drawable.contact_picture_placeholder)
                        .fit()
                        .transform(transformation)
                        .into((ImageView)view);
                overridden = true;
                break;
            case R.id.contact_display_color:
                int color = getColor(cursor, contactUri.toString());
                ((BorderedCircularColorView)view).setColor(color);
                overridden = true;
                break;
            case R.id.contact_ringtone:
                String ringtoneUri = getRingtoneUri (cursor, contactUri.toString());
                container = (View) view.getParent();

                if (!TextUtils.isEmpty(ringtoneUri) && !ColorVibrateDialog.GLOBAL.equals(ringtoneUri)){
                    view.setVisibility(View.VISIBLE);
                    view.setBackgroundResource(R.drawable.ic_custom_ringtone);
                    container.setVisibility(View.VISIBLE);
                }
                else {
                    view.setVisibility(View.GONE);
                    numberGone++;
                }
                overridden = true;
                break;
            case R.id.contact_vibrate:
                String vibratePattern = getVibPattern (cursor, contactUri.toString());
                container = (View) view.getParent();
                if (!TextUtils.isEmpty(vibratePattern)){
                    view.setVisibility(View.VISIBLE);
                    view.setBackgroundResource(R.drawable.ic_contact_vibrate);
                    container.setVisibility(View.VISIBLE);
                }
                else {
                    view.setVisibility(View.GONE);
                    numberGone++;
                }
                overridden = true;
                break;
        }

        if (numberGone == 2 && container != null){
            container.setVisibility(View.GONE);
        }
        return overridden;
    }
}
