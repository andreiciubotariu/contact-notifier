package com.ciubotariu_levy.lednotifier;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

//Graphical circle of color
public class ColorWheel extends View {
	private int color;
	private float cx, cy;
	private Paint paint;
	boolean lockWidth; //True if height depends on width, false if width depends on height
	private int[]wheelColors;
	private ColorListener cd;
	
	public interface ColorListener{
		public void setColor (int color);
	}
	
	public ColorWheel(Context context, AttributeSet attrs) {
		super(context, attrs);
		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setColor(color);
		wheelColors = new int[360];
		for (int i = 0; i<360; i++){
			float[] hsv = {i, 1, 1};
			wheelColors[i] = Color.HSVToColor(hsv);
		}
		setOnTouchListener(new View.OnTouchListener(){
			public boolean onTouch(View v, MotionEvent event){
				ColorWheel.this.onTouch(event);
				return true;
			}
		});
	}

	public void setColor(int color){
		this.color = color;
		cd.setColor(color);
		invalidate();
	}

	public int getColor(){
		return color;
	}

	public void setDialog(ColorListener cd){
		this.cd = cd;
	}
	
	@Override
	public void onMeasure(int w, int h){
		if (lockWidth)
			super.onMeasure(w, w);
		else
			super.onMeasure(h, h);
		cx = getWidth()/2;
		cy = getWidth()/2;
		invalidate();
	}

	public void onTouch(MotionEvent me){
		float x = me.getX() - cx; //X relative to cx
		float y = me.getY() - cy; //X relative to cy
		float r = (float)Math.sqrt(x*x+y*y);
		if (r<=cx && r >= cx/2){ //Touch in circle
			int theta = (int)Math.round(Math.toDegrees(Math.atan2(y, x)));
			if (theta < 0)
				theta += 360;
			setColor(wheelColors[theta]);
		}
	}
	
	@Override
	public void onDraw(Canvas c){
		SweepGradient sg = new SweepGradient(cx, cy, wheelColors, null);
		paint.setColor(Color.WHITE);
		paint.setShader(sg);
		paint.setStyle(Paint.Style.FILL);
		c.drawCircle(cx, cy, cx, paint);
		paint.setShader(null);
		paint.setColor(color);
		c.drawCircle(cx, cy, cx/2, paint);
	}
}
