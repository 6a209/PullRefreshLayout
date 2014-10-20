package com.pullrefreshlayout;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
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
public class RefreshLayout extends ViewGroup{

    private static final String TAG = "PullRefreshLayout";

    private static final int PULL_TO_REFRESH_STATUS = 0x00;
    private static final int RELEASE_TO_REFRESH_STATUS = 0x01;
    private static final int REFRESHING_STATUS = 0x02;
    private static final int NORMAL_STATUS = 0x03;

    View mTargetView;
    View mRefreshHeaderView;
    ILoadingLayout mHeadView;

    private int mCurStatus;
    private float mLastMotionY;
    private float mActionDownY;
    private float mTouchSlop;
    private boolean mIsBeingDragged;
    private int mHeaderViewHeight;
    private int mMediumAnimationDuration;

    /** the distance of refresh*/
    private int mNeedRefreshDeltaY;


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
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int headHeight = mRefreshHeaderView.getMeasuredHeight();
        mRefreshHeaderView.layout(l, t - headHeight, r, t);
        mTargetView.layout(l, t, r, b);
    }


    private final Animation mAnimateToPosition = new Animation() {

        private int mToPosition;

        private int mOriginalOffsetTop;

        public void setToPos(int toPos){
            mToPosition = toPos;
        }

        public void setOriginalOffsetTop(int originalOffsetTop){
            mOriginalOffsetTop = originalOffsetTop;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            final int curTop = getCurTop();
            if(mToPosition == curTop){
                return;
            }
            int toTop = (int) (curTop + (mOriginalOffsetTop - curTop) * interpolatedTime);
            int offset = toTop - curTop;
            if(offset + curTop < 0){
                offset = 0 - curTop;
            }
            setOffsetTopAndBottom(offset);
        }
    };


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
                final float y = ev.getY();
                final float yDiff = y - mActionDownY;
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
                handleRelease((int) ev.getY());
                break;
            default:
                break;
        }

        return true;
    }

    private int getCurTop(){
        return getTop();
    }


    private void setOffsetTopAndBottom(int offset){
        this.offsetTopAndBottom(offset);
    }


    private void handleRelease(int y){
        int toPostion;
        if(RELEASE_TO_REFRESH_STATUS == mCurStatus){
            toPostion = mHeaderViewHeight;
        }else if(PULL_TO_REFRESH_STATUS == mCurStatus){
            toPostion = 0;
        }else {
            return;
        }
        mAnimateToPosition.reset();
        mAnimateToPosition.setDuration(mMediumAnimationDuration);
        mAnimateToPosition.setToPos(toPostion);
        mAnimateToPosition.setOriginalOffsetTop(getCurTop());
        this.startAnimation(mAnimateToPosition);
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
                mHeadView.pulltoRefresh();
                break;
            case RELEASE_TO_REFRESH_STATUS:
                mHeadView.releaseToRefresh();
                break;
            case REFRESHING_STATUS:
                mHeadView.refreshing();
                break;
            case NORMAL_STATUS:
                mHeadView.normal();
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
