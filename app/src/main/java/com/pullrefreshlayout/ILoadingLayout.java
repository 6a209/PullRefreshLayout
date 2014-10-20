package com.pullrefreshlayout;

/**
 * Created by 6a209 on 14/10/19.
 */
public interface ILoadingLayout {

    public void pulltoRefresh();
    public void releaseToRefresh();
    public void refreshing();
    public void normal();
}
