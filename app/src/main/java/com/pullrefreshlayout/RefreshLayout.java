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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

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

    private static final int DEFAULT_HEAD_HEIGHT = 200;

    View mTargetView;
    View mRefreshHeaderView;

    LinearLayout mContentLy;

    private int mCurStatus;
    private float mLastMotionY;
    private float mActionDownY;
    private float mTouchSlop;
    private boolean mIsBeingDragged;
    private int mHeaderViewHeight = DEFAULT_HEAD_HEIGHT;
    private int mMediumAnimationDuration;

    private int mToPosition;
    private int mOriginalOffsetTop;

    /** the distance of refresh*/
    private int mNeedRefreshDeltaY = 400;



    public interface OnRefreshListener{

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
         *
         */
        void onRefreshOver();
    }

    public RefreshLayout(Context context){
        this(context, null);
    }

    public RefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mMediumAnimationDuration = getResources().getInteger(android.R.integer.config_mediumAnimTime);
        mContentLy = new LinearLayout(context);
        mContentLy.setOrientation(LinearLayout.VERTICAL);
        mRefreshHeaderView = new TestHeadView(context);
        mContentLy.addView(mRefreshHeaderView);
        mTargetView = initTargetView();
        mContentLy.addView(mTargetView);
        mTargetView.setBackgroundColor(Color.BLUE);

        addView(mContentLy);
    }

    public View getTargetView(){
        return mTargetView;
    }

    protected abstract View initTargetView();


    private class TestHeadView extends TextView implements ILoadingLayout{

        public TestHeadView(Context context) {
            super(context);
            setBackgroundColor(Color.RED);
            setHeight(mHeaderViewHeight);
        }

        @Override
        public void pulltoRefresh() {
            setText("Pull to Refresh");
        }

        @Override
        public void releaseToRefresh() {
            setText("release to refresh");
        }

        @Override
        public void refreshing() {
            setText("refreshing");
        }

        @Override
        public void normal() {
            setText("normal");
        }
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int headMeasureSpec = MeasureSpec.makeMeasureSpec(mHeaderViewHeight, MeasureSpec.EXACTLY);
        mRefreshHeaderView.measure(widthMeasureSpec, headMeasureSpec);
        mTargetView.measure(widthMeasureSpec, heightMeasureSpec);
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int headHeight = mHeaderViewHeight;
        mContentLy.layout(l, t - headHeight, r, b);
//        mRefreshHeaderView.layout(l, t - headHeight, r, t);
//        mTargetView.layout(l, t, r, b);
    }


    Animation mAnimateToPosition = new Animation() {


        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            Log.d("toPosition ==>>", mToPosition + "");
            final int curTop = getCurTop();
            Log.d("curTop ==>>", curTop + "");
            if(mToPosition == curTop){
                return;
            }
            Log.d("the interpolatedTime", "" + interpolatedTime);
            int toTop = (int) (mOriginalOffsetTop - (mOriginalOffsetTop - mToPosition) * interpolatedTime);

            int offset = toTop - curTop;
            setOffsetTopAndBottom(offset);
        }
    };


    public void refreshOver(){
        mAnimateToPosition.reset();
        mAnimateToPosition.setDuration(mMediumAnimationDuration);
        mToPosition = -mHeaderViewHeight;
        mOriginalOffsetTop = getCurTop();
        mContentLy.startAnimation(mAnimateToPosition);

    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev){
        if(!childIsOnTop()){
            return false;
        }

        final int action = ev.getAction();

        switch (action){
            case MotionEvent.ACTION_DOWN:
                mLastMotionY = mActionDownY = ev.getY();
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
                mIsBeingDragged = false;
                break;
        }

        return mIsBeingDragged;
    };


    @Override
    public boolean onTouchEvent(MotionEvent ev){
        final int aciont = ev.getAction();
        if(!childIsOnTop()){
           return false;
        }

        switch (aciont){
            case MotionEvent.ACTION_DOWN:
                mLastMotionY = mActionDownY = ev.getY();
                mIsBeingDragged = false;
                break;
            case MotionEvent.ACTION_MOVE:
                Log.d("action_move", ev.getY() + "");
                final float y = ev.getY();
                final float yDiff = y - mLastMotionY;
                if(!mIsBeingDragged && yDiff > mTouchSlop){
                    mIsBeingDragged = true;
                }
                if(mIsBeingDragged){
                    setOffsetTopAndBottom((int) (yDiff / 2));
                    int curTop = getCurTop();
                    if(mCurStatus != REFRESHING_STATUS){
                        if(curTop >= mNeedRefreshDeltaY){
                            updateStatus(RELEASE_TO_REFRESH_STATUS);
                        }else if(curTop > 0 && curTop < mNeedRefreshDeltaY){
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

                Log.d("action_up", ev.getY() + "");
                handleRelease();
                break;
            default:
                break;
        }

        return true;
    }

    private int getCurTop(){
        return mContentLy.getTop();
    }


    private void setOffsetTopAndBottom(int offset){
        Log.d("the offset >>> ", offset + "");
        mContentLy.offsetTopAndBottom(offset);
    }


    private void handleRelease(){
        Log.d("handleRelease", "=====");
        int toPostion;
        if(RELEASE_TO_REFRESH_STATUS == mCurStatus){
            toPostion = 0;
        }else if(PULL_TO_REFRESH_STATUS == mCurStatus){
            toPostion = -mHeaderViewHeight;
        }else {
            return;
        }
        mAnimateToPosition.reset();
        mAnimateToPosition.setDuration(mMediumAnimationDuration);
        mToPosition = toPostion;
        mOriginalOffsetTop = getCurTop();
        mContentLy.startAnimation(mAnimateToPosition);
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
                ((ILoadingLayout)mRefreshHeaderView).pulltoRefresh();
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

    private boolean childIsOnTop(){
       if(mTargetView instanceof ScrollView){
          return mTargetView.getScrollY() <= 0;
       }else if(mTargetView instanceof RecyclerView){
          // RecycleView
          return false;
       }else {
           return false;
       }
    }
}
