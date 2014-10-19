package com.ciubotariu_levy.lednotifier;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.widget.ListView;

public class FastScrollThemedListView extends ListView {

	public FastScrollThemedListView(Context context) {
		super(new ContextThemeWrapper(context, R.style.FastscrollThemedListView));
	}
	
	public FastScrollThemedListView(Context context, AttributeSet attrs) {
		super(new ContextThemeWrapper(context, R.style.FastscrollThemedListView), attrs);
	}

	public FastScrollThemedListView(Context context, AttributeSet attrs, int defStyle) {
		super(new ContextThemeWrapper(context, R.style.FastscrollThemedListView), attrs, defStyle);
	}

}
