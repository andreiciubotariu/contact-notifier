package com.ciubotariu_levy.lednotifier;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import com.ciubotariu_levy.lednotifier.SMSAppAdapter.IconPackagePair;

public class SMSAppAdapter extends ArrayAdapter<IconPackagePair> {

	public static class IconPackagePair{
		public Drawable icon;
		public String appName;
	}
	
	private LayoutInflater inflater;
	private static final int LAYOUT_RESOURCE = R.layout.app_row;
	private static final int TEXTVIEW_RESOURCE = R.id.app_name;
	private static final int APP_ICON_RESOURCE = R.id.app_icon;
	
	public SMSAppAdapter(Context context, IconPackagePair[] objects) {
		super(context, LAYOUT_RESOURCE, TEXTVIEW_RESOURCE, objects);
		inflater = LayoutInflater.from(context);
	}
	
	@Override
	public View getView (int position, View convertView, ViewGroup parent){
		View view = convertView;
		if (view == null){
			view = inflater.inflate(LAYOUT_RESOURCE, parent, false);
		}
		
		IconPackagePair pair = getItem(position);
		ImageView appIcon = (ImageView) view.findViewById(APP_ICON_RESOURCE);
		appIcon.setScaleType(ScaleType.FIT_CENTER);
		appIcon.setImageDrawable(pair.icon);
		
		
		TextView appName = (TextView) view.findViewById(TEXTVIEW_RESOURCE);
		appName.setText (pair.appName);
		
		return view;
	}

}
