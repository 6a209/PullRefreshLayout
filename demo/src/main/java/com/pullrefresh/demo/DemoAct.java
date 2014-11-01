package com.pullrefresh.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

/**
 * Created by 6a209 on 14/10/29.
 */
public class DemoAct extends Activity{

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);
    }



    public void toScrollAct(View view){
        Intent intent = new Intent(this, ScrollViewAct.class);
        startActivity(intent);
    }

    public void toRecyclerAct(View view){
        Intent intent = new Intent(this, RecyclerViewAct.class);
        startActivity(intent);
    }
}
