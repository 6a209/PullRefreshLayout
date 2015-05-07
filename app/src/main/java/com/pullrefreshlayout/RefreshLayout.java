package com.pullrefreshlayout;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

/**
 * Created by 6a209 on 14/10/19.
 *
 * just support ScrollView & RecycleView
 *
 *
 */
public abstract class RefreshLayout extends FrameLayout{

    private static final String TAG = "PullRefreshLayout";

    private static final int PULL_TO_REFRESH_STATUS = 0x00;
    private static final int RELEASE_TO_REFRESH_STATUS = 0x01;
    private static final int REFRESHING_STATUS = 0x02;
    private static final int NORMAL_STATUS = 0x03;

    private static final int DEFAULT_HEAD_HEIGHT = 200;

    View mRefreshView;
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
        mContentLy = new LinearLayout(context);
        mContentLy.setOrientation(LinearLayout.VERTICAL);
        mRefreshHeaderView = (View)createHeaderView();
        mContentLy.addView(mRefreshHeaderView);
        mRefreshView = createRefreshView();
        mContentLy.addView(mRefreshView);
        mRefreshView.setFadingEdgeLength(0);
        mRefreshView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        mCurStatus = NORMAL_STATUS;
        addView(mContentLy);
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
        int headMeasureSpec = MeasureSpec.makeMeasureSpec(mHeaderViewHeight, MeasureSpec.EXACTLY);
        mRefreshHeaderView.measure(widthMeasureSpec, headMeasureSpec);
        mRefreshView.measure(widthMeasureSpec, heightMeasureSpec);
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int headHeight = mHeaderViewHeight;
        mContentLy.layout(l, t - headHeight, r, b);
    }


    Animation mAnimateToPosition = new Animation() {


        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
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
        mContentLy.startAnimation(mAnimateToPosition);
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
        mAnimateToPosition.reset();
        mAnimateToPosition.setDuration(mMediumAnimationDuration);
        mToPosition = -mHeaderViewHeight;
        mOriginalOffsetTop = getCurTop();
        mContentLy.startAnimation(mAnimateToPosition);

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
                mLastMotionY = mActionDownY = ev.getY();
                mIsBeingDragged = false;
                break;
            case MotionEvent.ACTION_MOVE:
                final float y = ev.getY();
                final float yDiff = y - mLastMotionY;

                if(!mIsBeingDragged && yDiff > mTouchSlop){
                    mIsBeingDragged = true;
                }
                int curTop = getCurTop();
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
        return mContentLy.getTop();
    }


    private void setOffsetTopAndBottom(int offset){
        mContentLy.offsetTopAndBottom(offset);
    }


    private void handleRelease(){
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
        mAnimateToPosition.setAnimationListener(new SimpleAnimationListener(){
            @Override
            public void onAnimationEnd(Animation animation) {
                if(PULL_TO_REFRESH_STATUS == mCurStatus){
                    updateStatus(NORMAL_STATUS);
                }else if(RELEASE_TO_REFRESH_STATUS == mCurStatus){
                    updateStatus(REFRESHING_STATUS);
                    if(null != mRefreshListener){
                        mRefreshListener.onRefresh();
                    }
                }

            }
        });
        mContentLy.startAnimation(mAnimateToPosition);
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
}
