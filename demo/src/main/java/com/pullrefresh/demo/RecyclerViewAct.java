package com.pullrefresh.demo;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pullrefreshlayout.PullRefreshRecyclerView;

/**
 * Created by 6a209 on 14/10/21.
 */
public class RecyclerViewAct extends Activity{

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);

        String [] datas = new String[50];
        for(int i = 0; i < 50; i++){
           datas[i] = "it is test data " + i;
        }
        MyAdapter adapter = new MyAdapter(datas);

        PullRefreshRecyclerView pullRefreshRecyclerView = new PullRefreshRecyclerView(this);
        RecyclerView recyclerView = (RecyclerView)pullRefreshRecyclerView.getRefreshView();
        recyclerView.setAdapter(adapter);

        setContentView(pullRefreshRecyclerView);

    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView mTextView;
        public ViewHolder(TextView v) {
            super(v);
            mTextView = v;
        }
    }

    public static class MyAdapter extends RecyclerView.Adapter<ViewHolder> {
        private String[] mDataset;


        public MyAdapter(String[] myDataset) {
            mDataset = myDataset;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView v = (TextView) LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            ViewHolder vh = new ViewHolder(v);
            return vh;
        }


        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.mTextView.setText(mDataset[position]);

        }

        @Override
        public int getItemCount() {
            return mDataset.length;
        }
    }
}
