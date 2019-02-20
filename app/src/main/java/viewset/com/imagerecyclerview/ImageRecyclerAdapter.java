package viewset.com.imagerecyclerview;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.util.Arrays;
import java.util.List;

import viewset.com.R;

import static android.support.v7.widget.RecyclerView.Adapter;
import static android.support.v7.widget.RecyclerView.ViewHolder;

public class ImageRecyclerAdapter extends Adapter<ViewHolder> {

    private Context mContext;
    private List<Integer> mDatas = Arrays.asList(R.mipmap.game1, R.mipmap.game2);
    private int mCreatedHolder;


    public ImageRecyclerAdapter(Context context) {
        mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.e("ttt", "onCreateViewHolder" + mCreatedHolder);
        mCreatedHolder++;
        LayoutInflater inflater = LayoutInflater.from(mContext);
        return new NormalHolder(inflater.inflate(R.layout.item_image_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        //Log.e("ttt", "onBindViewHolder");
        NormalHolder normalHolder = (NormalHolder) holder;
        Glide.with(mContext).load(mDatas.get(position % 2)).into(normalHolder.mIv);
        //GlideUtil.loadRoundImage(holder.itemView.getContext(),mDatas.get(position % 6),normalHolder.mIv);
        //normalHolder.mIv.setImageResource(mDatas.get(position % 6));
    }

    @Override
    public int getItemCount() {
        return 24;
    }

    public class NormalHolder extends ViewHolder {
        public ImageView mIv;

        public NormalHolder(View itemView) {
            super(itemView);
            mIv = itemView.findViewById(R.id.iv);
        }
    }
}
