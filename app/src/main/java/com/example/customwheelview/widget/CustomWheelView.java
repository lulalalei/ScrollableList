package com.example.customwheelview.widget;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.util.Pools;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.OverScroller;

import com.example.customwheelview.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2018/5/29.
 */

public class CustomWheelView extends View {

    private static final boolean DEBUG = false;
    private static final int DEFAULT_VISIBILITY_COUNT = 5;
    private static final int AUTO_VISIBILITY_COUNT = -1;
    private static final int INVALID_POINTER = -1;
    private static final long MAXIMUM_FLING_DURATION = 600L;

    private List<String> mDataSources;
    private int mVisibilityCount = AUTO_VISIBILITY_COUNT;
    private float mTextSize = sp2px(16);
    private int mTextVerticalSpacing = (int) dp2px(10);

    private int mNormalTextColor = Color.LTGRAY;
    private int mSelectedTextColor = Color.BLACK;
    private int mSelectedLineColor = Color.BLACK;
    private int mTextGravity = Gravity.CENTER;

    private CallBack mCallBack;
    private int mSelectedPosition;
    private boolean mForceSelectedPosition;

    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mDrawPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect mContentRect = new Rect();

    //textRect,selectedRect都以contentRect为基准
    private final Rect mTextRect = new Rect();
    private final Rect mSelectedRect = new Rect();

    //单个文字最大的尺寸
    private int mMaxTextWidth, mMaxTextHeight;
    private int mDistanceY;
    private int mMaximumDistanceY;
    private int mMinimumDistanceY;

    /*
    * 避免在onDraw方法中重复多次绘制*/
    private boolean mNeedCalculate;

    /*
    * touch
    * */
    private boolean mIsBeingDragged;
    private final int mTouchSlop;
    private VelocityTracker mVelocityTracker;
    private final int mMaximumFlingVelocity;
    private final int mMinimumFlingVelocity;
    private final OverScroller mScroller;
    private int mActivePointerId = INVALID_POINTER;
    private float mDownY;
    private float mLastY;
    private boolean mIsBeingFling;
    private long mStartFlingTime;
    private long mFlingDuration = MAXIMUM_FLING_DURATION;
    private float mFlingY;
    private ValueAnimator mRunAnimator;
    private boolean mNeedCheckDistanceY;

    private static final Pools.Pool<Rect> RECT_POOL = new Pools.SimplePool<>(20);

    public CustomWheelView(Context context) {
        this(context, null);
    }

    public CustomWheelView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomWheelView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (isInEditMode()) {
            mDataSources = new ArrayList<>();
            for (int i = 0; i < 30; i++) {
                mDataSources.add("测试" + i);
            }
        }

        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        mTouchSlop = viewConfiguration.getScaledTouchSlop();
        mMinimumFlingVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
        mMaximumFlingVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
        mScroller = new OverScroller(context);

        init(context, attrs);
    }

    private void init(Context context, AttributeSet attributeSet) {
        TypedArray typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.WheelView);
        try {
            setTextSize(typedArray.getDimension(R.styleable.WheelView_textSize, mTextSize));
            setTextVerticalSpacing(typedArray.getDimensionPixelSize(
                    R.styleable.WheelView_textVerticalSpacing,
                    mTextVerticalSpacing
            ));
            setNormalTextColor(typedArray.getColor(
                    R.styleable.WheelView_normalTextColor,
                    mNormalTextColor
            ));
            setSelectedTextColor(typedArray.getColor(
                    R.styleable.WheelView_selectedTextColor,
                    mSelectedTextColor
            ));
            setSelectedLineColor(typedArray.getColor(
                    R.styleable.WheelView_selectedLineColor,
                    mSelectedLineColor
            ));
            setTextGravity(typedArray.getInt(R.styleable.WheelView_textGravity, mTextGravity));
            setSelectPosition(typedArray.getInt(
                    R.styleable.WheelView_selectPosition,
                    mSelectedPosition
            ));
            setVisibilityCount(typedArray.getInt(
                    R.styleable.WheelView_visibilityCount,
                    mVisibilityCount
            ));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (typedArray != null) {
                typedArray.recycle();
            }
        }
    }

    public void setVisibilityCount(int visibilityCount) {
        if (mVisibilityCount == visibilityCount) {
            return;
        }
        mVisibilityCount = visibilityCount;
        requestLayout();
        postInvalidate();
    }

    public void isEqual(List<String> dataSources) {
        if (mDataSources == dataSources) {
            Log.e("====equal===", "====" + true);
            return;
        }
        Log.e("====equal===", "====" + false);
    }

    public void setDataSources(List<String> dataSources) {
        if (mDataSources == dataSources) {
            return;
        }
        mDataSources = dataSources;
        mSelectedPosition = 0;
        requestLayout();
        postInvalidate();
    }

    public void setSelectPosition(int selectPosition) {
        if (!hasDataSource()) {
            return;
        }
        if (selectPosition > (mDataSources.size() - 1) || selectPosition < 0) {
            return;
        }
        mSelectedPosition = selectPosition;
        mNeedCheckDistanceY = true;
        mForceSelectedPosition = true;
    }

    private boolean hasDataSource() {
        return mDataSources != null && !mDataSources.isEmpty();
    }

    public void setTextGravity(int textGravity) {
        if (mTextGravity == textGravity) {
            return;
        }
        mTextGravity = textGravity;
        postInvalidate();
    }

    private void setSelectedLineColor(int selectedLineColor) {
        if (mSelectedLineColor == selectedLineColor) {
            return;
        }
        mSelectedLineColor = selectedLineColor;
        postInvalidate();
    }

    public void setSelectedTextColor(int selectedTextColor) {
        if (mSelectedTextColor == selectedTextColor) {
            return;
        }
        mSelectedTextColor = selectedTextColor;
        postInvalidate();
    }

    private void setNormalTextColor(int normalTextColor) {
        if (mNormalTextColor == normalTextColor) {
            return;
        }
        mNormalTextColor = normalTextColor;
        postInvalidate();
    }

    private void setTextVerticalSpacing(int textVerticalSpacing) {
        if (mTextVerticalSpacing == textVerticalSpacing) {
            return;
        }
        mTextVerticalSpacing = textVerticalSpacing;
        requestLayout();
        postInvalidate();
    }

    public void setTextSize(float textSize) {
        if (mTextSize == textSize) {
            return;
        }
        mTextSize = textSize;
        requestLayout();
        postInvalidate();
    }

    public void setmCallBack(CallBack mCallBack) {
        this.mCallBack = mCallBack;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int wantWidth = getPaddingLeft() + getPaddingRight();
        int wantHeight = getPaddingTop() + getPaddingBottom();
        calculateTextSize();
        wantWidth += mTextRect.width();
        if (mVisibilityCount > 0) {
            wantHeight += mTextRect.height() * mVisibilityCount;
        } else {
            wantHeight += mTextRect.height() * DEFAULT_VISIBILITY_COUNT;
        }

        setMeasuredDimension(resolveSize(wantWidth, widthMeasureSpec),
                resolveSize(wantHeight, heightMeasureSpec));

        mNeedCalculate = true;
        Log.e("===onMeasure====", "==============");
    }

    private void calculateTextSize() {
        mMaxTextHeight = mMaxTextWidth = 0;
        if (!hasDataSource()) {
            return;
        }
        mTextPaint.setTextSize(mTextSize);
        final Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        //获得文字的高度
        mMaxTextHeight = (int) (fontMetrics.bottom - fontMetrics.top);
        for (String text : mDataSources) {
            //获得文字的最大宽度
            mMaxTextWidth = (int) Math.max(mTextPaint.measureText(text), mMaxTextWidth);
        }
        //给文本分配区域
        mTextRect.set(0, 0, mMaxTextWidth, mMaxTextHeight + 2 * mTextVerticalSpacing);
        calculateDistanceY();
    }

    /*
    * 记录最大可移动的距离
    * */
    private void calculateDistanceY() {
        mMaximumDistanceY = mMinimumDistanceY = 0;
        if (!hasDataSource()) {
            return;
        }
        mMaximumDistanceY = mTextRect.height() * (mDataSources.size() - 1);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mNeedCalculate) {
            mNeedCalculate = false;
            calculate();
        }

        if (mNeedCheckDistanceY) {
            mNeedCheckDistanceY = false;
            int needDistanceY = mTextRect.height() * mSelectedPosition;
            animChangeDistanceY(needDistanceY);
            Log.e("===onDraw====", "====needDistanceY====" + needDistanceY +
                    " ,textRectHeight===" + mTextRect.height());
        }

        //裁剪画布,在区域以外的将不再显示
        canvas.clipRect(mContentRect);

        if (hasDataSource()) {
            //得到新的选中条目的位置
            int selctPosition = Math.max(0, mDistanceY / mTextRect.height());
            final int remainder = mDistanceY % mTextRect.height();
            if (remainder > mTextRect.height() / 2f) {
                selctPosition++;
            }
            selctPosition = Math.min(selctPosition, mDataSources.size() - 1);

            if (!mIsBeingDragged && !mIsBeingFling && mSelectedPosition != selctPosition &&
                    (mRunAnimator == null || !mRunAnimator.isRunning())) {
                if (mCallBack != null) {
                    mCallBack.onPositionSelect(selctPosition);
                }
                mSelectedPosition = selctPosition;
            }
            int drawCount = mContentRect.height() / mTextRect.height() + 2;
            int invisibleCount = 0;
            int dy = -mDistanceY;
            //这里所有的计算偏移都以ContentRect为基准
            if (mDistanceY > mSelectedRect.top) {
                invisibleCount = (mDistanceY - mSelectedRect.top) / mTextRect.height();
                dy = -(mDistanceY - invisibleCount * mTextRect.height());
            }
            int saveCount = canvas.save();
            //padding top
            canvas.translate(mContentRect.left, mContentRect.top);
            canvas.translate(0, mSelectedRect.top);
            canvas.translate(0, dy);
            for (int i = 0; (i < drawCount && mDataSources.size() > (invisibleCount + i)); i++) {
                int position = invisibleCount + i;
                String text = mDataSources.get(position);
                if (i > 0) {
                    canvas.translate(0, mTextRect.height());
                }
                PointF pointF = calculateTextGravity(text);
                mTextPaint.setTextSize(mTextSize);
                if (position == selctPosition) {
                    mTextPaint.setColor(mSelectedTextColor);
                } else {
                    mTextPaint.setColor(mNormalTextColor);
                }
                canvas.drawText(text, pointF.x, pointF.y, mTextPaint);
            }
            canvas.restoreToCount(saveCount);
        }

        int saveCount = canvas.save();
        mDrawPaint.setColor(mSelectedLineColor);
        canvas.translate(mContentRect.left, mContentRect.top);
        canvas.drawLine(mSelectedRect.left, mSelectedRect.top,
                mSelectedRect.right, mSelectedRect.top, mDrawPaint);
        canvas.drawLine(mSelectedRect.left, mSelectedRect.bottom,
                mSelectedRect.right, mSelectedRect.bottom, mDrawPaint);
        canvas.restoreToCount(saveCount);
    }

    private PointF calculateTextGravity(String text) {
        PointF pointF = new PointF();
        Rect textSizeRect = acquireRect();
        mTextPaint.getTextBounds(text, 0, text.length(), textSizeRect);
        switch (mTextGravity & Gravity.VERTICAL_GRAVITY_MASK) {
            case Gravity.TOP:
                pointF.y = mTextRect.top + textSizeRect.height() - Math.abs(textSizeRect.bottom);
                break;
            case Gravity.BOTTOM:
                pointF.y = textSizeRect.bottom;
                break;
            case Gravity.CENTER_VERTICAL:
                pointF.y = mTextRect.exactCenterY() + textSizeRect.height() / 2f -
                        Math.abs(textSizeRect.bottom);
                break;
            default:
                break;
        }
        switch (mTextGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
            case Gravity.LEFT:
            case Gravity.START:
                pointF.x = 0;
                break;
            case Gravity.RIGHT:
            case Gravity.END:
                pointF.x = mTextRect.right - textSizeRect.width();
                break;
            case Gravity.CENTER_HORIZONTAL:
                pointF.x = mTextRect.exactCenterX() - textSizeRect.width() / 2f;
                break;
            default:
                break;
        }
        releaseRect(textSizeRect);
        return pointF;
    }

    private void releaseRect(Rect rect) {
        rect.setEmpty();
        RECT_POOL.release(rect);
    }

    private Rect acquireRect() {
        Rect rect = RECT_POOL.acquire();
        if (rect == null) {
            rect = new Rect();
        }
        return rect;
    }

    private void animChangeDistanceY(int newDistanceY) {
        if (newDistanceY > mMaximumDistanceY) {
            newDistanceY = mMaximumDistanceY;
        }
        if (newDistanceY < mMinimumDistanceY) {
            newDistanceY = mMinimumDistanceY;
        }
        if (newDistanceY != mDistanceY) {
            if (mRunAnimator != null && mRunAnimator.isRunning()) {
                mRunAnimator.cancel();
            }
            Log.e("===animChangeDistanceY", "===mDistanceY=" + mDistanceY);
            mRunAnimator = ValueAnimator.ofInt(mDistanceY, newDistanceY);
            mRunAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mDistanceY = (int) animation.getAnimatedValue();
                    Log.e("===anim==", "===mDistanceY=" + mDistanceY);
                    postInvalidate();
                }
            });
            mRunAnimator.start();
        }
    }

    private void calculate() {
        //分配整个内容区域
        mContentRect.set(getPaddingLeft(), getPaddingTop(), getMeasuredWidth() - getPaddingRight(),
                getMeasuredHeight() - getPaddingBottom());
        Log.e("===calculate===", "======height==" + getMeasuredHeight() +
                " ,screenheight==" + getScreenHeight());
        //根据实际可见的区域,重新计算每个文本所占的宽和高
        if (mVisibilityCount > 0) {
            mTextRect.set(0, 0,
                    mContentRect.width(), (int) (mContentRect.height() * 1.0 / mVisibilityCount));
        } else {
            mTextRect.set(0, 0, mContentRect.width(), mMaxTextHeight + 2 * mTextVerticalSpacing);
        }
        int contentCentY = mContentRect.centerY();
        int position = contentCentY / mTextRect.height();
        if (contentCentY % mTextRect.height() > 0) {
            position++;
        }
        //计算应位于屏幕中心显示的矩形区域
        mSelectedRect.set(0, mTextRect.height() * (position - 1),
                mContentRect.width(), mTextRect.height() * position);

        calculateDistanceY();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!hasDataSource()) {
            return super.onTouchEvent(event);
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                initOrResetVelocityTracker();
                mIsBeingFling = false;
                mScroller.computeScrollOffset();
                if (mIsBeingDragged = !mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                if (mRunAnimator != null) {
                    mRunAnimator.cancel();
                }
                if (mIsBeingDragged) {
                    ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }
                mActivePointerId = event.getPointerId(0);
                mDownY = event.getY(0);
                mLastY = mDownY;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                /*
                * 只有在多点触摸时才会产生此事件,在一个触摸事件序列中,除第一个
                * 触摸屏幕的手指外,其他手指触摸屏幕时会产生此事件*/
                Log.e("===onTouchEvent====","===ACTION_POINTER_DOWN==");
                //当前触摸的手指更新先触摸的手指
                int actionIndex = event.getActionIndex();
                mActivePointerId = event.getPointerId(actionIndex);
                mDownY = event.getY(actionIndex);
                mLastY = mDownY;
                break;
            case MotionEvent.ACTION_MOVE:
                int activePointerId = mActivePointerId;
                if (activePointerId == INVALID_POINTER) {
                    break;
                }
                int pointerIndex = event.findPointerIndex(activePointerId);
                if (pointerIndex == -1) {
                    break;
                }
                float moveY = event.getY(pointerIndex);
                if (!mIsBeingDragged && Math.abs(mDownY - moveY) > mTouchSlop) {
                    mIsBeingDragged = true;
                    ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }
                if (mIsBeingDragged) {
                    mDistanceY += mLastY - moveY;
                    Log.e("===onTouchEvent===","===ACTION_MOVE==mLastY=="+mLastY+" ,moveY==="+moveY);
                    if (mDistanceY >= mMaximumDistanceY) {
                        mDistanceY = mMaximumDistanceY;
                    }
                    if (mDistanceY <= mMinimumDistanceY) {
                        mDistanceY = mMinimumDistanceY;
                    }
                    postInvalidate();
                }
                mLastY = moveY;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                /*
                * 只有在多点触摸时才会产生此事件,在一个触屏事件序列中,除最后一个
                * 离开屏幕的手指外,其他手指离开屏幕时会产生此事件*/
                Log.e("===onTouchEvent====","===ACTION_POINTER_UP==");
                onSecondaryPointerUp(event);
                break;
            case MotionEvent.ACTION_UP:
                VelocityTracker velocityTracker = this.mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
                if (Math.abs(velocityTracker.getYVelocity()) > mMinimumFlingVelocity) {
                    int yVelocity = (int) velocityTracker.getYVelocity();
                    mFlingDuration = Math.max(MAXIMUM_FLING_DURATION, getSplineFlingDuration(yVelocity));
                    mScroller.fling(0, 0, 0, yVelocity,
                            0, 0, -mMaximumDistanceY, mMaximumDistanceY);
                    mFlingY = mScroller.getStartY();
                    if (Math.abs(mMaximumDistanceY - mDistanceY) < getHeight()) {
                        mFlingDuration = mFlingDuration / 3;
                    }
                    mIsBeingFling = true;
                    mStartFlingTime = SystemClock.elapsedRealtime();
                    postInvalidate();
                } else {
                    correctionDistanceY();
                }
                mActivePointerId = INVALID_POINTER;
                mIsBeingDragged = false;
                resetVelocityTracker();
                break;
            case MotionEvent.ACTION_CANCEL:
                mIsBeingFling = false;
                mActivePointerId = INVALID_POINTER;
                mIsBeingDragged = false;
                resetVelocityTracker();
                correctionDistanceY();
                break;
            default:
                break;
        }
        if (mVelocityTracker != null) {
            mVelocityTracker.addMovement(event);
        }
        return true;
    }

    private void resetVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.clear();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        final float ppi = getContext().getResources().getDisplayMetrics().density * 160.0f;
        mPhysicalCoeff = SensorManager.GRAVITY_EARTH // g (m/s^2)
                * 39.37f // inch/meter
                * ppi
                * 0.84f; // look and feel tuning
    }

    private static final float INFLEXION = 0.35f; // Tension lines cross at (INFLEXION, 1)

    // A context-specific coefficient adjusted to physical values.
    private float mPhysicalCoeff;

    private static float DECELERATION_RATE = (float) (Math.log(0.78) / Math.log(0.9));

    // Fling friction
    private float mFlingFriction = ViewConfiguration.getScrollFriction();

    private int getSplineFlingDuration(int velocity) {
        double l = getSplineDeceleration(velocity);
        double decelMinusOne = DECELERATION_RATE - 1.0;
        return (int) (1000.0 * Math.exp(l / decelMinusOne));
    }

    private double getSplineDeceleration(int velocity) {
        return Math.log(INFLEXION * Math.abs(velocity) / (mFlingFriction * mPhysicalCoeff));
    }

    private void onSecondaryPointerUp(MotionEvent event) {
        int actionIndex = event.getActionIndex();
        int pointerId = event.getPointerId(actionIndex);
        if (pointerId == mActivePointerId) {
            actionIndex = actionIndex == 0 ? 1 : 0;
            mActivePointerId = event.getPointerId(actionIndex);
            mDownY = event.getY(actionIndex);
            mLastY = mDownY;
            mVelocityTracker.clear();
        }
    }

    private void initOrResetVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            int currY = mScroller.getCurrY();
            mDistanceY += mFlingY - currY;
            if (mDistanceY >= mMaximumDistanceY) {
                mDistanceY = mMaximumDistanceY;
            }
            if (mDistanceY <= mMinimumDistanceY) {
                mDistanceY = mMinimumDistanceY;
            }
            mFlingY = currY;
            if ((SystemClock.elapsedRealtime() - mStartFlingTime) >= mFlingDuration || currY == mScroller.getFinalY()) {
                mScroller.abortAnimation();
            }
            postInvalidate();
        } else if (mIsBeingFling) {
            mIsBeingFling = false;
            correctionDistanceY();
        }
    }

    private void correctionDistanceY() {
        if (mDistanceY % mTextRect.height() != 0) {
            int position = mDistanceY / mTextRect.height();
            int remainder = mDistanceY % mTextRect.height();
            if (remainder >= mTextRect.height() / 2f) {
                position++;
            }
            int newDistanceY = position * mTextRect.height();
            animChangeDistanceY(newDistanceY);
        }
    }

    /*private void initOrResetVelocityTracker() {

    }*/

    public interface CallBack {

        void onPositionSelect(int position);
    }

    private int getScreenHeight() {
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        return wm.getDefaultDisplay().getHeight();
    }

    private float dp2px(float dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }

    private float sp2px(float sp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                sp,
                getResources().getDisplayMetrics()
        );
    }
}
