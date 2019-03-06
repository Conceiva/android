package com.handwerkcloud.client;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceView;

import com.google.android.gms.vision.text.TextBlock;

public class CameraOverlaySurfaceView extends SurfaceView {
    private static final String TAG = "CameraOverlay";
    private SparseArray<TextBlock> mItems;
    private boolean mDrawOutline = false;
    private boolean mDrawText = false;

    public boolean getDrawText() {
        return mDrawText;
    }

    public boolean getDrawOutline() {
        return mDrawOutline;
    }

    public void setDrawOutline(boolean b) {
        mDrawOutline = b;
    }

    public void setDrawText(boolean b) {
        mDrawText = b;
    }

    public CameraOverlaySurfaceView(Context ctx) { /* Do init */
        super(ctx);
        setWillNotDraw(false);
    }

    public CameraOverlaySurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mItems == null) {
            return;
        }
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        for (int i = 0; i < mItems.size(); i++) {

            for (int j = 0; j < mItems.valueAt(i).getComponents().size(); j++) {
                String text = mItems.valueAt(i).getComponents().get(j).getValue();
                Rect rect = mItems.valueAt(i).getComponents().get(j).getBoundingBox();

                float textSize = 8;
                paint.setTextSize(textSize);

                paint.setStyle(Paint.Style.STROKE);
                if (mDrawOutline) {
                    canvas.drawRect(rect, paint);
                }

                if (mDrawText) {
                    canvas.save();
                    TextPaint textpaint = new TextPaint();
                    textSize = OCRActivity.calculateMaxTextSize(text, textpaint, (rect.right - rect.left) - 5, (rect.bottom - rect.top) - 1);

                    textpaint.setTextSize(textSize);

                    StaticLayout mTextLayout = new StaticLayout(text, textpaint, rect.right - rect.left, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                    canvas.translate(rect.left, rect.top);
                    Log.d(TAG, text + " left: " + rect.left);
                    mTextLayout.draw(canvas);
                    canvas.restore();
                }
            }
        }
    }

    public void setItems(SparseArray<TextBlock> items) {
        mItems = items;
        invalidate();
    }
}
