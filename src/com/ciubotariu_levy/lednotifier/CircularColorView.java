package com.ciubotariu_levy.lednotifier;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

//Graphical circle of color
public class CircularColorView extends View {
	private int color;
	private Paint paint;
	private boolean lockWidth; //True if height depends on width, false if width depends on height

	public CircularColorView(Context context, AttributeSet attrs) {
		super(context, attrs);
		paint = new Paint();
		paint.setAntiAlias(true);
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

//	@Override
//	public void onMeasure(int w, int h){
//		if (lockWidth){
//			super.onMeasure(w, w);
//		} else{
//			super.onMeasure(h, h);
//		}
//		invalidate();
//	}

	@Override
	public void onDraw(Canvas c){
		c.drawCircle(getWidth()/2, getHeight()/2, getWidth()/2, paint);
	}
}
