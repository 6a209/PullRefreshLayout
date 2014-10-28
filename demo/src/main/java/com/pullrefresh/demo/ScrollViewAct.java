package com.pullrefresh.demo;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ScrollView;
import android.widget.TextView;

import com.pullrefreshlayout.PullRefresScrollView;
import com.pullrefreshlayout.RefreshLayout;


public class ScrollViewAct extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final PullRefresScrollView scrollView = new PullRefresScrollView(this);
        setContentView(scrollView);

        TextView tv = new TextView(this);
        tv.setBackgroundColor(Color.BLACK);
        tv.setTextSize(300);
        tv.setText("it is test");
        ((ScrollView)scrollView.getTargetView()).addView(tv);


        scrollView.setOnRefreshListener(new RefreshLayout.OnRefreshListener() {
            @Override
            public void onPullDown(float y) {

            }

            @Override
            public void onRefresh() {
                scrollView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                       scrollView.refreshOver();
                    }
                }, 2000);
            }

            @Override
            public void onRefreshOver() {

            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
