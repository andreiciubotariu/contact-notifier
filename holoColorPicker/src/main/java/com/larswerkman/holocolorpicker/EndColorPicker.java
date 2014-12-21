package com.larswerkman.holocolorpicker;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class EndColorPicker extends View {

	/*
	 * Constants used to save/restore the instance state.
	 */
	private static final String STATE_PARENT = "parent";
	private static final String STATE_COLOR = "color";
	private static final String STATE_NOTIFY = "notify_on_restore";
	private static final String STATE_ORIENTATION = "orientation";

	/**
	 * Constants used to identify orientation.
	 */
	private static final boolean ORIENTATION_HORIZONTAL = true;
	private static final boolean ORIENTATION_VERTICAL = false;

	private static final int END_COLOR = Color.GRAY;
	/**
	 * Color array END_COLOR, RED, MAGENTA, BLUE, CYAN, GREEN, YELLOW, RED (decreasing order)
	 */
	private static final int[] GRADIENT_COLORS = new int[] {END_COLOR, 0xFFFF0000, 0xFFFF00FF,
		0xFF0000FF, 0xFF00FFFF, 0xFF00FF00, 0xFFFFFF00, 0xFFFF0000 };
	
	/**
	 * Default orientation of the bar.
	 */
	private static final boolean ORIENTATION_DEFAULT = ORIENTATION_HORIZONTAL;

	/**
	 * The thickness of the bar.
	 */
	private int mBarThickness;

	/**
	 * The length of the bar.
	 */
	private int mBarLength;
	private int mPreferredBarLength;

	/**
	 * The radius of the pointer.
	 */
	private int mBarPointerRadius;

	/**
	 * The radius of the halo of the pointer.
	 */
	private int mBarPointerHaloRadius;

	/**
	 * The position of the pointer on the bar.
	 */
	private int mBarPointerPosition;

	/**
	 * {@code Paint} instance used to draw the bar.
	 */
	private Paint mBarPaint;
	
	private Paint mDefColorPaint;

	/**
	 * {@code Paint} instance used to draw the pointer.
	 */
	private Paint mBarPointerPaint;

	/**
	 * {@code Paint} instance used to draw the halo of the pointer.
	 */
	private Paint mBarPointerHaloPaint;

	/**
	 * The rectangle enclosing the bar.
	 */
	private RectF mGradientBarRect = new RectF();

	private RectF mEndBarRect = new RectF();
	/**
	 * {@code Shader} instance used to fill the shader of the paint.
	 */
	private Shader shader;

	/**
	 * {@code true} if the user clicked on the pointer to start the move mode. <br>
	 * {@code false} once the user stops touching the screen.
	 * 
	 * @see #onTouchEvent(MotionEvent)
	 */
	private boolean mIsMovingPointer;

	/**
	 * The ARGB value of the currently selected color.
	 */
	private int mColor;

	/**
	 * An array of floats that can be build into a {@code Color} <br>
	 * Where we can extract the Saturation and Value from.
	 */
	private float[] mHSVColor = new float[3];

	/**
	 * Used to toggle orientation between vertical and horizontal.
	 */
	private boolean mOrientation;

	/**
	 * When true, restoring color from saved instance also notifies OnColorChangedListener if non-null
	 */
	private boolean mNotifyFromRestore;

	/**
	 * Flag when view is restoring values
	 */
	private boolean mRestoring;

	/**
	 * {@code onColorSelectedListener} instance of the onColorSelectedListener
	 */
	private OnColorChangedListener onColorChangedListener;

	public EndColorPicker(Context context) {
		super(context);
		init(null, 0);
	}

	public EndColorPicker(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs, 0);
	}

	public EndColorPicker(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs, defStyle);
	}

	private void init(AttributeSet attrs, int defStyle) {
		final TypedArray a = getContext().obtainStyledAttributes(attrs,
				R.styleable.ColorBars, defStyle, 0);
		final Resources b = getContext().getResources();

		mBarThickness = a.getDimensionPixelSize(
				R.styleable.ColorBars_bar_thickness,
				b.getDimensionPixelSize(R.dimen.bar_thickness));
		mBarLength = a.getDimensionPixelSize(R.styleable.ColorBars_bar_length,
				b.getDimensionPixelSize(R.dimen.bar_length));
		mPreferredBarLength = mBarLength;
		mBarPointerRadius = a.getDimensionPixelSize(
				R.styleable.ColorBars_bar_pointer_radius,
				b.getDimensionPixelSize(R.dimen.bar_pointer_radius));
		mBarPointerHaloRadius = a.getDimensionPixelSize(
				R.styleable.ColorBars_bar_pointer_halo_radius,
				b.getDimensionPixelSize(R.dimen.bar_pointer_halo_radius));
		mOrientation = a.getBoolean(
				R.styleable.ColorBars_bar_orientation_horizontal, ORIENTATION_DEFAULT);
		mNotifyFromRestore = a.getBoolean(R.styleable.ColorBars_notify_from_restore, true);
		a.recycle();

		mBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mBarPaint.setShader(shader);

		mDefColorPaint = new Paint (Paint.ANTI_ALIAS_FLAG);
		mDefColorPaint.setColor(END_COLOR);
		
		mBarPointerPosition = (mBarLength / 2) + mBarPointerHaloRadius;

		mBarPointerHaloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mBarPointerHaloPaint.setColor(Color.BLACK);
		mBarPointerHaloPaint.setAlpha(0x50);

		mBarPointerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mBarPointerPaint.setColor(0xff81ff00);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		final int intrinsicSize = mPreferredBarLength
				+ (mBarPointerHaloRadius * 2);

		// Variable orientation
		int measureSpec;
		if (mOrientation == ORIENTATION_HORIZONTAL) {
			measureSpec = widthMeasureSpec;
		}
		else {
			measureSpec = heightMeasureSpec;
		}
		int lengthMode = MeasureSpec.getMode(measureSpec);
		int lengthSize = MeasureSpec.getSize(measureSpec);

		int length;
		if (lengthMode == MeasureSpec.EXACTLY) {
			length = lengthSize;
		}
		else if (lengthMode == MeasureSpec.AT_MOST) {
			length = Math.min(intrinsicSize, lengthSize);
		}
		else {
			length = intrinsicSize;
		}

		int barPointerHaloRadiusx2 = mBarPointerHaloRadius * 2;
		mBarLength = length - barPointerHaloRadiusx2;
		if(mOrientation == ORIENTATION_VERTICAL) {
			setMeasuredDimension(barPointerHaloRadiusx2,
					(mBarLength + barPointerHaloRadiusx2));
		}
		else {
			setMeasuredDimension((mBarLength + barPointerHaloRadiusx2),
					barPointerHaloRadiusx2);
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		float totalColors = GRADIENT_COLORS.length - 1;
		// Fill the rectangle instance based on orientation
		int partThickness,x1, y1;
		if (mOrientation == ORIENTATION_HORIZONTAL) {
			x1 = mBarLength + mBarPointerHaloRadius;
			y1 = mBarThickness;
			partThickness = mBarPointerHaloRadius - (mBarThickness / 2);
			mBarLength = w - (mBarPointerHaloRadius * 2);
			mGradientBarRect.set(mBarPointerHaloRadius + Math.round(mBarLength/totalColors),
					(partThickness),
					(mBarLength + (mBarPointerHaloRadius)),
					(mBarPointerHaloRadius + (mBarThickness / 2)));
			
			mEndBarRect.set(mBarPointerHaloRadius, 
					partThickness, 
					mBarPointerHaloRadius + Math.round(mBarLength/totalColors), 
					mBarPointerHaloRadius + (mBarThickness / 2));
		}
		else {
			x1 = mBarThickness;
			y1 = mBarLength + mBarPointerHaloRadius;
			partThickness = mBarPointerHaloRadius - (mBarThickness / 2);
			mBarLength = h - (mBarPointerHaloRadius * 2);
			mGradientBarRect.set((mBarPointerHaloRadius - (mBarThickness / 2)),
					mBarPointerHaloRadius + Math.round(mBarLength/totalColors),
					(partThickness),
					(mBarLength + (mBarPointerHaloRadius)));
			mEndBarRect.set(mBarPointerHaloRadius, 
					mBarPointerHaloRadius + Math.round(mBarLength/totalColors), 
					partThickness,
					mBarPointerHaloRadius + (mBarThickness / 2));
		}

		// Update variables that depend of mBarLength.
		if(!isInEditMode()){
			shader = new LinearGradient(mBarPointerHaloRadius, 0,
					x1, y1, GRADIENT_COLORS,
					null, Shader.TileMode.CLAMP);
		} else {
			shader = new LinearGradient(mBarPointerHaloRadius, 0,
					x1, y1, GRADIENT_COLORS, null,
					Shader.TileMode.CLAMP);
			Color.colorToHSV(0xff81ff00, mHSVColor);
		}

		mBarPaint.setShader(shader);
		float[] hsvColor = new float[3];
		Color.colorToHSV(mColor, hsvColor);
		setPointerPosition (mColor);
		if(isInEditMode()){
			mBarPointerPosition = (mBarLength / 2) + mBarPointerHaloRadius;
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// Draw the bar.
		canvas.drawRect(mGradientBarRect, mBarPaint);
		canvas.drawRect(mEndBarRect, mDefColorPaint);

		// Calculate the center of the pointer.
		int cX, cY;
		if (mOrientation == ORIENTATION_HORIZONTAL) {
			cX = mBarPointerPosition;
			cY = mBarPointerHaloRadius;
		}
		else {
			cX = mBarPointerHaloRadius;
			cY = mBarPointerPosition;
		}

		// Draw the pointer halo.
		canvas.drawCircle(cX, cY, mBarPointerHaloRadius, mBarPointerHaloPaint);
		// Draw the pointer.
		canvas.drawCircle(cX, cY, mBarPointerRadius, mBarPointerPaint);
	};

	@Override
	public boolean onTouchEvent(MotionEvent event) {


		// Convert coordinates to our internal coordinate system
		float dimen;
		if (mOrientation == ORIENTATION_HORIZONTAL) {
			dimen = event.getX();
		}
		else {
			dimen = event.getY();
		}

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
            getParent().requestDisallowInterceptTouchEvent(true);
			mIsMovingPointer = true;
			// Check whether the user pressed on the pointer
			if (dimen >= (mBarPointerHaloRadius)
					&& dimen <= (mBarPointerHaloRadius + mBarLength)) {
				mBarPointerPosition = Math.round(dimen);
				calculateColor(Math.round(dimen));
				mBarPointerPaint.setColor(mColor);
				invalidate();
			}
			break;
		case MotionEvent.ACTION_MOVE:
			if (mIsMovingPointer) {
				// Move the the pointer on the bar.
				if (dimen >= mBarPointerHaloRadius
						&& dimen <= (mBarPointerHaloRadius + mBarLength)) {
					mBarPointerPosition = Math.round(dimen);
					calculateColor(Math.round(dimen));
					mBarPointerPaint.setColor(mColor);
					invalidate();
				} else if (dimen < mBarPointerHaloRadius) {
					mBarPointerPosition = mBarPointerHaloRadius;
					setColorInternal(GRADIENT_COLORS[0]);
					mBarPointerPaint.setColor(mColor);
					invalidate();
				} else if (dimen > (mBarPointerHaloRadius + mBarLength)) {
					mBarPointerPosition = mBarPointerHaloRadius + mBarLength;
					setColorInternal(GRADIENT_COLORS[GRADIENT_COLORS.length-1]);
					mBarPointerPaint.setColor(mColor);
					invalidate();
				}
			}
			break;
		case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            getParent().requestDisallowInterceptTouchEvent(false);
			mIsMovingPointer = false;
			break;
		}
		return true;
	}

	/**
	 * Set the current color. Closest approximation <br>
	 * <br>
	 * 
	 * @param color
	 */
	public void setColor(int color) {
		setColorInternal(color);
		float [] hsv = new float [3];
		Color.colorToHSV(color, hsv);
		setPointerPosition(color);
		calculateColor(mBarPointerPosition);
		Color.colorToHSV(mColor, mHSVColor);
		mBarPointerPaint.setColor(mColor);
		invalidate();
	}


	private void setColorInternal (int color){
		int oldColor = mColor;
		mColor = color;
		Color.colorToHSV(mColor, mHSVColor);
		if ((!mRestoring || (mRestoring && mNotifyFromRestore)) && onColorChangedListener != null && oldColor != mColor){
			onColorChangedListener.onColorChanged(mColor);
		}
	}

	/**
	 * Set a onColorChangedListener
	 * 
	 * @param {@code OnColorChangedListener}
	 */
	public void setOnColorChangedListener(OnColorChangedListener listener) {
		this.onColorChangedListener = listener;
	}

	private void setPointerPosition (int color){
		float totalColors = GRADIENT_COLORS.length-1;
		if (color == END_COLOR){
			mBarPointerPosition = Math.round(mBarPointerHaloRadius + mBarLength/(2*totalColors));
		} else {
			float [] hsv = new float [3];
			Color.colorToHSV(color, hsv);
			mBarPointerPosition = Math.round(mBarLength*(totalColors-1)/totalColors*(1 - hsv[0]/360) + mBarPointerHaloRadius + mBarLength/totalColors);
		}
	}

	private int ave(int s, int d, float p) {
		return s + java.lang.Math.round(p * (d - s));
	}

	/**
	 * Calculate the color selected by the pointer on the bar.
	 * 
	 * @param coord
	 *            Coordinate of the pointer.
	 */
	private void calculateColor(int coord) {
		coord = coord - mBarPointerHaloRadius;
		if (coord <= 0) {
			coord = 0;
			setColorInternal(GRADIENT_COLORS[0]);
		} else if (coord >= mBarLength) {
			coord = mBarLength;
			setColorInternal(GRADIENT_COLORS[GRADIENT_COLORS.length-1]);
		} else if (coord == mBarPointerHaloRadius) {
			setColorInternal(GRADIENT_COLORS[0]);
		} else if (coord == mBarPointerHaloRadius + mBarLength) {
			setColorInternal(GRADIENT_COLORS[GRADIENT_COLORS.length-1]);
		} else {
			float floatCoord = coord;
			float unit = (floatCoord / mBarLength);
			//COLORS.length - 1 since one end color does not signify a sector
			float actualSector = unit * (GRADIENT_COLORS.length - 1);
			int truncatedSector = (int) actualSector;
			float partSector =  actualSector - truncatedSector;

			if (truncatedSector == 0){
				setColorInternal (GRADIENT_COLORS[0]);
			}else {
				int c0 = GRADIENT_COLORS[truncatedSector];
				int c1 = GRADIENT_COLORS[truncatedSector + 1];
				int a = ave(Color.alpha(c0), Color.alpha(c1), partSector);
				int r = ave(Color.red(c0), Color.red(c1), partSector);
				int g = ave(Color.green(c0), Color.green(c1), partSector);
				int b = ave(Color.blue(c0), Color.blue(c1), partSector);

				setColorInternal(Color.argb(a, r, g, b));
			}
		}

	}

	/**
	 * Get the currently selected color.
	 * 
	 * @return The ARGB value of the currently selected color.
	 */
	public int getColor() {
		return mColor;
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();

		Bundle state = new Bundle();
		state.putParcelable(STATE_PARENT, superState);
		state.putFloatArray(STATE_COLOR, mHSVColor);
		state.putBoolean(STATE_NOTIFY, mNotifyFromRestore);
		return state;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		mRestoring = true;
		Bundle savedState = (Bundle) state;

		Parcelable superState = savedState.getParcelable(STATE_PARENT);
		super.onRestoreInstanceState(superState);

		mNotifyFromRestore = savedState.getBoolean(STATE_NOTIFY);
		setColor(Color.HSVToColor(savedState.getFloatArray(STATE_COLOR)));
		mRestoring = false;
	}
}
