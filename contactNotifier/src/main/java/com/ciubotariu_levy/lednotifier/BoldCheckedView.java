package com.ciubotariu_levy.lednotifier;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.CheckedTextView;

public class BoldCheckedView extends CheckedTextView {
	
	public BoldCheckedView(Context context) {
		super(context);
	}

	public BoldCheckedView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public BoldCheckedView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	@Override
	public void setChecked (boolean checked){
		if (isChecked() != checked){
			setTypeface (checked ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
		}
		super.setChecked(checked);
	}
}