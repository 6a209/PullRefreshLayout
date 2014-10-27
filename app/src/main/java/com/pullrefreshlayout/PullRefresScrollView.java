package com.pullrefreshlayout;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;

/**
 * Created by 6a209 on 14/10/21.
 */
public class PullRefresScrollView extends RefreshLayout{

    public PullRefresScrollView(Context context){
        this(context, null);
    }

    public PullRefresScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View initTargetView() {
        return new ScrollView(getContext());
    }



}
