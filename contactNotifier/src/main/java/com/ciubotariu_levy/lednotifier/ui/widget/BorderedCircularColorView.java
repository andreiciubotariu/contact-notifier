package com.ciubotariu_levy.lednotifier.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

//Graphical circle of color
public class BorderedCircularColorView extends View {
    private static final Paint WHITE;
    private static int sDpInPx = 0;

    static {
        WHITE = new Paint();
        WHITE.setAntiAlias(true);
        WHITE.setColor(Color.WHITE);
    }

    private int mColor;
    private Paint mPaint;

    public BorderedCircularColorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(mColor);
        if (sDpInPx == 0) {
            sDpInPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getContext().getResources().getDisplayMetrics());
        }
    }

    public int getColor() {
        return mColor;
    }

    public void setColor(int color) {
        this.mColor = color;
        mPaint.setColor(color);
        invalidate();
    }

    @Override
    public void onDraw(Canvas c) {
        c.drawCircle(getWidth() / 2, getHeight() / 2, getWidth() / 2, WHITE);
        c.drawCircle(getWidth() / 2, getHeight() / 2, getWidth() / 2 - sDpInPx, mPaint);
    }
}
