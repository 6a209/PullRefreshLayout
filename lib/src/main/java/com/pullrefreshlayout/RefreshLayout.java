package com.pullrefreshlayout;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ScrollView;

/**
 * Created by 6a209 on 14/10/19.
 *
 * just support ScrollView & RecycleView
 *
 *
 */
public abstract class RefreshLayout extends ViewGroup{

    private static final String TAG = "PullRefreshLayout";

    private static final int PULL_TO_REFRESH_STATUS = 0x00;
    private static final int RELEASE_TO_REFRESH_STATUS = 0x01;
    private static final int REFRESHING_STATUS = 0x02;
    private static final int NORMAL_STATUS = 0x03;

    private static final int RELEASE_TO_NORMAL = 0x0;

    private static final int DEFAULT_HEAD_HEIGHT = 200;


    View mRefreshView;
    View mRefreshHeaderView;

//    LinearLayout mContentLy;

    private int mCurStatus;
    private float mLastMotionY;
    private float mActionDownY;
    private float mTouchSlop;
    private boolean mIsBeingDragged;
    private int mHeaderViewHeight = DEFAULT_HEAD_HEIGHT;
    private int mMediumAnimationDuration;

    private int mToPosition;
    private int mOriginalOffsetTop;
    private int mLayoutOffsetTop = 0;

    /** the distance of refresh*/
    private int mNeedRefreshDeltaY = 120;


    OnRefreshListener mRefreshListener;

    public interface OnRefreshListener {

        /**
         * on pull down status
         * @param y
         */
        void onPullDown(float y);

        /**
         * on refreshing status
         */
        void onRefresh();

        /**
         * on refresh over on normal status
         *
         * i suggest in this callback refresh the view data;
         *
         */
        void onRefreshOver(Object obj);
    }

    public void setOnRefreshListener(OnRefreshListener listener){
       mRefreshListener = listener;
    }

    public RefreshLayout(Context context){
        this(context, null);
    }

    public RefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mMediumAnimationDuration = getResources().getInteger(android.R.integer.config_mediumAnimTime);
        mRefreshHeaderView = (View)createHeaderView();
        mRefreshHeaderView.setBackgroundColor(Color.parseColor("#FF0000"));
        addView(mRefreshHeaderView);
        mRefreshView = createRefreshView();
        addView(mRefreshView);
        mRefreshView.setFadingEdgeLength(0);
        mRefreshView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        mCurStatus = NORMAL_STATUS;
    }

    public View getRefreshView(){
        return mRefreshView;
    }

    protected ILoadingLayout createHeaderView(){
       return new DefaultHeadView(getContext());
    }

    protected abstract View createRefreshView();



    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        Log.d("onMeasure", "onMeasure");
//        Log.d("heightMode", MeasureSpec.getMode(heightMeasureSpec) + "");
//        Log.d("heightSize", MeasureSpec.getSize(heightMeasureSpec) + "");
//        int headHeightSpec = MeasureSpec.makeMeasureSpec(mHeaderViewHeight, MeasureSpec.getMode(heightMeasureSpec));
        measureChildWithMargins(mRefreshHeaderView, widthMeasureSpec, 0, heightMeasureSpec, 0);
        MarginLayoutParams headLp = (MarginLayoutParams)mRefreshHeaderView.getLayoutParams();
        mHeaderViewHeight = mRefreshHeaderView.getMeasuredHeight() + headLp.bottomMargin + headLp.topMargin;
//        Log.d("the head height is => ", mHeaderViewHeight + "");


        MarginLayoutParams contentLp = (MarginLayoutParams)mRefreshView.getLayoutParams();
        int contentWidthSpec = getChildMeasureSpec(widthMeasureSpec, getPaddingLeft() + getPaddingRight() + contentLp.leftMargin + contentLp.rightMargin, contentLp.width);
        int contentHeightSpec = getChildMeasureSpec(heightMeasureSpec, getPaddingBottom() + getPaddingTop() + contentLp.topMargin + contentLp.bottomMargin, contentLp.height);

//        Log.d("heightSize", MeasureSpec.getSize(contentHeightSpec) + "");
        mRefreshView.measure(contentWidthSpec, contentHeightSpec);
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        Log.d("onLayout", "onLayout");


        MarginLayoutParams headLp = (MarginLayoutParams)mRefreshHeaderView.getLayoutParams();
        int left = getPaddingLeft() + headLp.leftMargin;
        int top = getPaddingTop() + headLp.topMargin - mHeaderViewHeight + mLayoutOffsetTop;
        int right = left + mRefreshHeaderView.getMeasuredWidth();
        int bottom = top + mRefreshHeaderView.getMeasuredHeight();

        mRefreshHeaderView.layout(left, top, right, bottom);


        MarginLayoutParams contentLp = (MarginLayoutParams)mRefreshView.getLayoutParams();
        left = getPaddingLeft() + contentLp.leftMargin;
        top = contentLp.topMargin + mLayoutOffsetTop;
        right = left + mRefreshView.getMeasuredWidth();
        bottom = top + mRefreshView.getMeasuredHeight();
        mRefreshView.layout(left, top, right, bottom);
    }


    Animation mAnimateToPosition = new Animation() {


        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            Log.d("Animation", "in=> Animation");
            final int curTop = getCurTop();
            if(mToPosition == curTop){
                return;
            }
            int toTop = (int) (mOriginalOffsetTop - (mOriginalOffsetTop - mToPosition) * interpolatedTime);
            if(toTop <= -mHeaderViewHeight){
                toTop = -mHeaderViewHeight;
            }
            int offset = toTop - curTop;
            setOffsetTopAndBottom(offset);
        }
    };

    public void setToRefreshing(){
        if(NORMAL_STATUS != mCurStatus) {
            return;
        }
        updateStatus(PULL_TO_REFRESH_STATUS);
        mAnimateToPosition.reset();
        mAnimateToPosition.setDuration(mMediumAnimationDuration);
        mToPosition = 0;
        mOriginalOffsetTop = getCurTop();
        startAnimation(mAnimateToPosition);
        mAnimateToPosition.setAnimationListener(new SimpleAnimationListener(){
            @Override
            public void onAnimationEnd(Animation animation){

                updateStatus(REFRESHING_STATUS);
                if(null != mRefreshListener){
                    mRefreshListener.onRefresh();
                }
            }
        });
    }


    public void refreshOver(final Object obj){
        if(REFRESHING_STATUS != mCurStatus){
            return;
        }
        updateStatus(RELEASE_TO_NORMAL);
        mAnimateToPosition.reset();
        mAnimateToPosition.setDuration(mMediumAnimationDuration);
        mToPosition = -mHeaderViewHeight;
        mOriginalOffsetTop = getCurTop();
        startAnimation(mAnimateToPosition);

        mAnimateToPosition.setAnimationListener(new SimpleAnimationListener(){
            @Override
            public void onAnimationEnd(Animation animation) {
                updateStatus(NORMAL_STATUS);
                if(null != mRefreshListener){
                    mRefreshListener.onRefreshOver(obj);
                }
            }
        });
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev){
        Log.d("on intercept ", "****");
        if(!childIsOnTop()){
            return super.onInterceptTouchEvent(ev);
        }

        final int action = ev.getAction();

        switch (action){
            case MotionEvent.ACTION_DOWN:
                mLastMotionY = mActionDownY = ev.getY();
                mIsBeingDragged = false;
                break;
            case MotionEvent.ACTION_MOVE:
                final float y = ev.getY();
                final float yDiff = y - mActionDownY;
                if(yDiff > mTouchSlop){
                    mLastMotionY = y;
                    mIsBeingDragged = true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:

                Log.d("******", "up or cancel");
                mIsBeingDragged = false;
                break;
        }

        if(mIsBeingDragged){
            return true;
        }else{
            return super.onInterceptTouchEvent(ev);
        }
    };


    @Override
    public boolean onTouchEvent(MotionEvent ev){

        final int aciont = ev.getAction();
        if(!childIsOnTop()){
            return super.onTouchEvent(ev);
        }

        switch (aciont){
            case MotionEvent.ACTION_DOWN:
                Log.d("on touche down", "****");
                mLastMotionY = mActionDownY = ev.getY();
                mIsBeingDragged = false;
                break;
            case MotionEvent.ACTION_MOVE:
                final float y = ev.getY();
                final float yDiff = y - mLastMotionY;


                Log.d("y && ydiff", y + " __ " + yDiff);
                Log.d("mIsBeginDra" , mIsBeingDragged + "");

                if(!mIsBeingDragged && yDiff > mTouchSlop){
                    mIsBeingDragged = true;
                }
                int curTop = getCurTop();
                Log.d("curTop", curTop + "");
                if(curTop <= -mHeaderViewHeight && yDiff < 0 ){
                    mIsBeingDragged = false;
                }

                if(mCurStatus == REFRESHING_STATUS){
                    mIsBeingDragged = false;
                }
                if(mIsBeingDragged){

                    float offset = yDiff / 2;
                    if(offset < 0 && curTop + offset <= -mHeaderViewHeight){
                        offset = -mHeaderViewHeight - curTop;
                    }

                    Log.d("isDragged", offset + "");
                    setOffsetTopAndBottom((int) (offset));
                    if(mRefreshListener != null){
                       mRefreshListener.onPullDown(y);
                    }
                    if(mCurStatus != REFRESHING_STATUS){
                        if(curTop >= mNeedRefreshDeltaY){
                            updateStatus(RELEASE_TO_REFRESH_STATUS);
                        }else if(curTop > -mHeaderViewHeight && curTop < mNeedRefreshDeltaY){
                            updateStatus(PULL_TO_REFRESH_STATUS);
                        }else{
                            updateStatus(NORMAL_STATUS);
                        }
                    }
                    mLastMotionY = y;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:

                Log.d("on touche up or cancel", "****");
                handleRelease();

                break;
            default:
                break;
        }
        if(mIsBeingDragged){
            return true;
        }else{
            return super.onTouchEvent(ev);
        }
    }

    private int getCurTop(){
        return mRefreshHeaderView.getTop();
    }



    private void setOffsetTopAndBottom(int offset){
//        offsetTopAndBottom(offset);
        mLayoutOffsetTop = mHeaderViewHeight + getCurTop();
        mRefreshHeaderView.offsetTopAndBottom(offset);
        mRefreshView.offsetTopAndBottom(offset);
        invalidate();
    }


    private void handleRelease(){
        int toPostion;
        if(RELEASE_TO_REFRESH_STATUS == mCurStatus){
            toPostion = 0;
        }else if(PULL_TO_REFRESH_STATUS == mCurStatus){
            toPostion = -mHeaderViewHeight;
            updateStatus(RELEASE_TO_NORMAL);
        }else {
            return;
        }
        mAnimateToPosition.reset();
        mAnimateToPosition.setDuration(mMediumAnimationDuration);
        mToPosition = toPostion;
        mOriginalOffsetTop = getCurTop();
        mAnimateToPosition.setAnimationListener(new SimpleAnimationListener(){
            @Override
            public void onAnimationEnd(Animation animation) {
                if(RELEASE_TO_NORMAL == mCurStatus){
                    updateStatus(NORMAL_STATUS);
                }else if(RELEASE_TO_REFRESH_STATUS == mCurStatus){
                    updateStatus(REFRESHING_STATUS);
                    if(null != mRefreshListener){
                        mRefreshListener.onRefresh();
                    }
                }

            }
        });
        startAnimation(mAnimateToPosition);
    }

    private static class SimpleAnimationListener implements Animation.AnimationListener{

        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {

        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    }


    /**
     *  update the pull status
     * @param status
     */
    private void updateStatus(int status){
        if(mCurStatus == status){
            return;
        }

        mCurStatus = status;
        switch (mCurStatus){
            case PULL_TO_REFRESH_STATUS:
                ((ILoadingLayout)mRefreshHeaderView).pullToRefresh();
                break;
            case RELEASE_TO_REFRESH_STATUS:
                ((ILoadingLayout)mRefreshHeaderView).releaseToRefresh();
                break;
            case REFRESHING_STATUS:
                ((ILoadingLayout)mRefreshHeaderView).refreshing();
                break;
            case NORMAL_STATUS:
                ((ILoadingLayout)mRefreshHeaderView).normal();
                break;
            default:
                break;
        }

    }

    protected boolean childIsOnTop(){
       if(mRefreshView instanceof ScrollView){
           return mRefreshView.getScrollY() <= 0;
       }else if(mRefreshView instanceof RecyclerView){
           RecyclerView recyclerView = (RecyclerView) mRefreshView;
           View child = recyclerView.getChildAt(0);
           if(null != child){
               return child.getTop() >= 0;
           }
          // RecycleView
          return false;
       }else {
           return false;
       }
    }




    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }
    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }
    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }
    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }



    public static class LayoutParams extends MarginLayoutParams {
        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }
        public LayoutParams(int width, int height) {
            super(width, height);
        }
        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }
        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }
}
