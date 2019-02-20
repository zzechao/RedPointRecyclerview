package viewset.com.imagerecyclerview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

public class ImageRecyclerView extends RecyclerView {
    public ImageRecyclerView(@NonNull Context context) {
        this(context, null);
    }

    public ImageRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ImageRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setChildrenDrawingOrderEnabled(true); //开启重新排序
    }

    /**
     * 获取LayoutManger，并强制转换为ImageLayoutManager
     */
    public ImageLayoutManager getImageLayoutManager() {
        return ((ImageLayoutManager) getLayoutManager());
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        int center = getImageLayoutManager().getCenterPosition() - getImageLayoutManager().getFirstVisiblePosition();
        int order;

        if (i == center) {
            order = childCount - 1;
        } else if (i > center) {
            order = center + childCount - 1 - i;
        } else {
            order = i;
        }
        return order;
    }
}
