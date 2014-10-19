package com.ciubotariu_levy.lednotifier;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

//Graphical circle of color
public class CircularColorView extends View {
	private int mColor;
	private Paint mPaint;

	public CircularColorView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setColor(mColor);
	}

	public void setColor(int color){
		this.mColor = color;
		mPaint.setColor(color);
		invalidate();
	}

	public int getColor(){
		return mColor;
	}

	@Override
	public void onDraw(Canvas c){
		c.drawCircle(getWidth()/2, getHeight()/2, getWidth()/2, mPaint);
	}
}
