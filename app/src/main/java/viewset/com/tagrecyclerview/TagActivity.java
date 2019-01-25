package viewset.com.tagrecyclerview;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;

import viewset.com.R;


public class TagActivity extends AppCompatActivity {

    private ArrayList<String> mDatas = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_itemdecoration2);

        generateDatas();
        final RecyclerView mRecyclerView = findViewById(R.id.linear_recycler_view);

        //线性布局
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(linearLayoutManager);

        LinearItemDecoration linearItemDecoration2 = new LinearItemDecoration(this);
        mRecyclerView.addItemDecoration(linearItemDecoration2);

        RecyclerAdapter adapter = new RecyclerAdapter(this, mDatas);
        mRecyclerView.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void generateDatas() {
        for (int i = 0; i < 200; i++) {
            mDatas.add("第 " + i + " 个item");
        }
    }
}
