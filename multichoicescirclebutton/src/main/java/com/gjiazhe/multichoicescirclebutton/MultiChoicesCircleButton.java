package com.gjiazhe.multichoicescirclebutton;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gjz on 08/12/2016.
 */

public class MultiChoicesCircleButton extends View {
    private boolean isDragged = false;
    private float mCollapseRadius;
    private float mExpandRadius;
    private float mCircleCentreX;
    private float mCircleCentreY;

    private float mCurrentExpandProgress = 0f;
    private float mFromExpandProgress;
    private Animation expandAnimation;
    private Animation collapseAnimation;
    private int mDuration;

    private String mText;
    private float mTextSize;
    private int mTextColor;
    private int mButtonColor;

    private Paint mPaint;
    private Camera mCamera = new Camera();
    private Matrix mMatrix = new Matrix();

    private static DisplayMetrics mDisplayMetrics;

    private List<Item> mItems = new ArrayList<>();
    private int mSelectedItemIndex = -1;

    public MultiChoicesCircleButton(Context context) {
        this(context, null);
    }

    public MultiChoicesCircleButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MultiChoicesCircleButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mDisplayMetrics = context.getResources().getDisplayMetrics();

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MultiChoicesCircleButton);
        mCollapseRadius = typedArray.getDimension(R.styleable.MultiChoicesCircleButton_mccb_collapseRadius, dp2px(40));
        mExpandRadius = typedArray.getDimension(R.styleable.MultiChoicesCircleButton_mccb_expandRadius, dp2px(120));
        mText = typedArray.getString(R.styleable.MultiChoicesCircleButton_mccb_text);
        mTextSize = typedArray.getDimension(R.styleable.MultiChoicesCircleButton_mccb_textSize, sp2px(30));
        mTextColor = typedArray.getColor(R.styleable.MultiChoicesCircleButton_mccb_textColor, Color.GRAY);
        mButtonColor = typedArray.getColor(R.styleable.MultiChoicesCircleButton_mccb_buttonColor, Color.parseColor("#FC516A"));
        mDuration = typedArray.getInt(R.styleable.MultiChoicesCircleButton_mccb_duration, 200);
        typedArray.recycle();

        initPaint();
        initAnimation();
    }

    private void initPaint() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setTextAlign(Paint.Align.CENTER);
    }

    private void initAnimation() {
        expandAnimation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                mCurrentExpandProgress = mFromExpandProgress + (1 - mFromExpandProgress) * interpolatedTime;
                if (mCurrentExpandProgress > 1f) {
                    mCurrentExpandProgress = 1f;
                }
                invalidate();
            }
        };

        collapseAnimation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                mCurrentExpandProgress = mFromExpandProgress  * (1 - interpolatedTime);
                if (mCurrentExpandProgress < 0f) {
                    mCurrentExpandProgress = 0f;
                }
                invalidate();
            }
        };
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int viewWidth = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        int viewHeight = MeasureSpec.getSize(heightMeasureSpec) - getPaddingTop() - getPaddingBottom();
        mCircleCentreX = viewWidth / 2;
        mCircleCentreY = viewHeight;

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float eventY = event.getY();
        float eventX = event.getX();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (actionDownInCircle(eventX, eventY)) {
                    clearAnimation();
                    mFromExpandProgress = mCurrentExpandProgress;
                    startExpandAnimation();
                    return true;
                } else {
                    return false;
                }

            case MotionEvent.ACTION_MOVE:
                isDragged = true;
                rotate(eventX, eventY);
                if (actionDownInCircle(eventX, eventY)) {
                    mSelectedItemIndex = getSelectedItemIndex(eventX, eventY);
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragged = false;
                mSelectedItemIndex = -1;
                clearAnimation();
                mFromExpandProgress = mCurrentExpandProgress;
                startCollapseAnimation();
                return true;
        }

        return super.onTouchEvent(event);
    }

    private boolean actionDownInCircle(float x, float y) {
        final float currentRadius = (mExpandRadius - mCollapseRadius) * mCurrentExpandProgress + mCollapseRadius;
        double distance = Math.pow(x - mCircleCentreX, 2) + Math.pow(y - mCircleCentreY, 2);
        return distance <= currentRadius * currentRadius;
    }

    private int getSelectedItemIndex(float x, float y) {
        if (!mItems.isEmpty()) {
            for (int i = 0; i < mItems.size(); i++) {
                Item item = mItems.get(i);
                float offsetX = (float) (item.distance * Math.cos(Math.PI * item.angle / 180));
                float offsetY = (float) (item.distance * Math.sin(Math.PI * item.angle / 180));
                float itemCentreX = mCircleCentreX - offsetX * mCurrentExpandProgress;
                float itemCentreY = mCircleCentreY - offsetY * mCurrentExpandProgress;
                double distance = Math.pow(x - itemCentreX, 2) + Math.pow(y - itemCentreY, 2);
                if (distance < item.radius * item.radius) {
                    return i;
                }
            }
        }
        return  -1;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isDragged) {
            canvas.concat(mMatrix);
        }

        // Draw circle
        mPaint.setAlpha(255);
        mPaint.setColor(mButtonColor);
        final float radius = (mExpandRadius - mCollapseRadius) * mCurrentExpandProgress + mCollapseRadius;
        canvas.drawCircle(mCircleCentreX, mCircleCentreY, radius, mPaint);

        // Draw text
        String text = mSelectedItemIndex == -1 ? mText : mItems.get(mSelectedItemIndex).text;
        if (text != null && text.length() != 0) {
            mPaint.setTextSize(mTextSize * mCurrentExpandProgress);
            mPaint.setColor(mTextColor);
            Paint.FontMetrics fontMetrics = mPaint.getFontMetrics();
            final float textHeight = fontMetrics.bottom - fontMetrics.top;
            final float baseLineY = mCircleCentreY - radius - textHeight / 2
                    - (fontMetrics.descent - fontMetrics.ascent) / 2 - fontMetrics.ascent;
            canvas.drawText(text, mCircleCentreX, baseLineY, mPaint);
        }

        if (!mItems.isEmpty()) {
            mPaint.setColor(Color.WHITE);
            for (int i = 0; i < mItems.size(); i++) {
                Item item = mItems.get(i);

                mPaint.setAlpha(mSelectedItemIndex == i ? 255 : 255*8/10);

                float offsetX = (float) (item.distance * Math.cos(Math.PI * item.angle / 180));
                float offsetY = (float) (item.distance * Math.sin(Math.PI * item.angle / 180));
                float itemCentreX = mCircleCentreX - offsetX * mCurrentExpandProgress;
                float itemCentreY = mCircleCentreY - offsetY * mCurrentExpandProgress;
                canvas.drawCircle(itemCentreX, itemCentreY, item.radius * mCurrentExpandProgress, mPaint);

                if (item.icon != null) {
                    float size = item.radius * 2 / 3 * mCurrentExpandProgress;
                    int left = (int) (itemCentreX - size);
                    int top = (int) (itemCentreY - size);
                    int right = (int) (itemCentreX + size);
                    int bottom = (int) (itemCentreY + size);
                    item.icon.setBounds(left, top, right, bottom);
                    item.icon.draw(canvas);
                }
            }
        }
    }

    private void rotate(float eventX, float eventY) {
        final int width = getWidth() - getPaddingLeft() - getPaddingRight();
        final int height = getHeight() - getPaddingTop() - getPaddingBottom();
        final int size = Math.max(width, height);

        final float offsetY = mCircleCentreY - eventY;
        final float offsetX = mCircleCentreX - eventX;
        final float rotateX = offsetY / size * 45;
        final float rotateY = -offsetX / size * 45;
        mCamera.save();
        mCamera.rotateX(rotateX);
        mCamera.rotateY(rotateY);
        mCamera.getMatrix(mMatrix);
        mCamera.restore();
        mMatrix.preTranslate(-mCircleCentreX, -mCircleCentreY);
        mMatrix.postTranslate(mCircleCentreX, mCircleCentreY);
        invalidate();
    }

    private void startExpandAnimation() {
        expandAnimation.setDuration(mDuration);
        startAnimation(expandAnimation);
    }

    private void startCollapseAnimation() {
        collapseAnimation.setDuration(mDuration);
        startAnimation(collapseAnimation);
    }

    private static float dp2px(int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, mDisplayMetrics);
    }

    private static float sp2px(int sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, mDisplayMetrics);
    }

    public void setButtonItems(List<Item> items) {
        mSelectedItemIndex = -1;
        mItems.clear();
        mItems.addAll(items);
        invalidate();
    }

    public float getCollapseRadius() {
        return mCollapseRadius;
    }

    public void setCollapseRadius(float collapseRadius) {
        this.mCollapseRadius = collapseRadius;
        invalidate();
    }

    public float getExpandRadius() {
        return mExpandRadius;
    }

    public void setExpandRadius(float expandRadius) {
        this.mExpandRadius = expandRadius;
        invalidate();
    }

    public int getDuration() {
        return mDuration;
    }

    public void setDuration(int duration) {
        this.mDuration = duration;
    }

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        this.mText = text;
        invalidate();
    }

    public float getTextSize() {
        return mTextSize;
    }

    public void setTextSize(float textSize) {
        this.mTextSize = textSize;
        invalidate();
    }

    public int getTextColor() {
        return mTextColor;
    }

    public void setTextColor(int textColor) {
        this.mTextColor = textColor;
        invalidate();
    }

    public int getButtonColor() {
        return mButtonColor;
    }

    public void setButtonColor(int buttonColor) {
        this.mButtonColor = buttonColor;
        invalidate();
    }

    public static class Item {
        private String text;
        private float radius;
        private Drawable icon;
        private int angle;
        private float distance;

        public Item(String text, int radiusDP, Drawable icon , int angle, int distanceDP) {
            this.text = text;
            this.radius = dp2px(radiusDP);
            this.icon = icon;
            this.angle = angle;
            this.distance = dp2px(distanceDP);
        }
    }
}
