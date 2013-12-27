package com.ciubotariu_levy.lednotifier;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

//Graphical circle of color
public class ColorView extends View {
	int color;
	Paint paint;
	boolean lockWidth; //True if height depends on width, false if width depends on height

	public ColorView(Context context, AttributeSet attrs) {
		super(context, attrs);
		paint = new Paint();
		paint.setColor(color);
	}

	public void setColor(int color){
		this.color = color;
		paint.setColor(color);
		invalidate();
	}

	public int getColor(){
		return color;
	}

	public void onMeasure(int w, int h){
		if (lockWidth)
			super.onMeasure(w, w);
		else
			super.onMeasure(h, h);
		invalidate();
	}

	public void onDraw(Canvas c){
		c.drawCircle(getWidth()/2, getHeight()/2, getWidth()/2, paint);
	}
}
