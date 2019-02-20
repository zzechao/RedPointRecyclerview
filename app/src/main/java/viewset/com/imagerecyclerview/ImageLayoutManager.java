package viewset.com.imagerecyclerview;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

public class ImageLayoutManager extends RecyclerView.LayoutManager {

    /**
     * 最大存储item信息存储数量，
     * 超过设置数量，则动态计算来获取
     */
    private final int MAX_RECT_COUNT = 100;

    /**
     * 滑动总偏移量
     */
    private int mOffsetAll = 0;

    /**
     * Item宽
     */
    private int mDecoratedChildWidth = 0;

    /**
     * Item高
     */
    private int mDecoratedChildHeight = 0;

    /**
     * 保存所有的Item的上下左右的偏移量信息
     */
    private SparseArray<Rect> mAllItemFrames = new SparseArray<>();

    /**
     * 记录Item是否出现过屏幕且还没有回收。true表示出现过屏幕上，并且还没被回收
     */
    private SparseBooleanArray mHasAttachedItems = new SparseBooleanArray();

    /**
     * Item间隔与item宽的比例
     */
    private float mIntervalRatio = 0.5f;

    /**
     * 总长度
     */
    private float mTotalWidth;

    /**
     * 起始ItemX坐标
     */
    private int mStartX = 0;

    /**
     * 起始Item Y坐标
     */
    private int mStartY = 0;

    /**
     * 转动最大值
     */
    private float M_MAX_ROTATION_Y = 49.0f;

    /**
     * 移动距离
     */
    private ValueAnimator mAnimation;

    /**
     * RecyclerView的Item回收器
     */
    private RecyclerView.Recycler mRecycle;

    /**
     * RecyclerView的状态器
     */
    private RecyclerView.State mState;

    /**
     * 正显示在中间的Item
     */
    private int mSelectPosition = 0;

    /**
     * 前一个正显示在中间的Item
     */
    private int mLastSelectPosition = 0;

    /**
     * 滑动的方向：左
     */
    private static int SCROLL_LEFT = 1;

    /**
     * 滑动的方向：右
     */
    private static int SCROLL_RIGHT = 2;

    /**
     * 选中监听
     */
    private OnSelected mSelectedListener;

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        super.onLayoutChildren(recycler, state);

        if (getItemCount() == 0 || state.isPreLayout()) {//没有Item，界面空着吧
            mOffsetAll = 0;
            return;
        }

        mAllItemFrames.clear();
        mHasAttachedItems.clear();

        if (mDecoratedChildWidth == 0 || mDecoratedChildHeight == 0) {
            //得到子view的宽和高，这边的item的宽高都是一样的，所以只需要进行一次测量
            View scrap = recycler.getViewForPosition(0);
            addView(scrap);
            measureChildWithMargins(scrap, 0, 0);
            //计算测量布局的宽高
            mDecoratedChildWidth = getDecoratedMeasuredWidth(scrap);
            mDecoratedChildHeight = getDecoratedMeasuredHeight(scrap);
            mStartX = Math.round((getHorizontalSpace() - mDecoratedChildWidth) * 1.0f / 2) - 100;
            mStartY = Math.round((getVerticalSpace() - mDecoratedChildHeight) * 1.0f / 2);
        }

        float offset = mStartX; //item X轴方向的位置坐标

        /**只存{@link MAX_RECT_COUNT}个item具体位置*/
        for (int i = 0; i < getItemCount() && i < MAX_RECT_COUNT; i++) {
            Rect frame = mAllItemFrames.get(i);
            if (frame == null) {
                frame = new Rect();
            }
            frame.set(Math.round(offset), mStartY, Math.round(offset + mDecoratedChildWidth), mStartY + mDecoratedChildHeight);
            mAllItemFrames.put(i, frame);
            //mHasAttachedItems.put(i, false);
            offset = offset + getIntervalDistance(); //原始位置累加，否则越后面误差越大
        }

        detachAndScrapAttachedViews(recycler); //在布局之前，将所有的子View先Detach掉，放入到Scrap缓存中
        if ((mRecycle == null || mState == null) && //在为初始化前调用smoothScrollToPosition 或者 scrollToPosition,只会记录位置
                mSelectPosition != 0) {                 //所以初始化时需要滚动到对应位置
            mOffsetAll = calculateOffsetForPosition(mSelectPosition);
            onSelectedCallBack();
        }

        mTotalWidth = Math.max(offset + mStartX + getIntervalDistance(), getHorizontalSpace());

        detachAndScrapAttachedViews(recycler);

        layoutItems(recycler, state, SCROLL_RIGHT);
//        int visableCount = (int) Math.ceil((getHorizontalSpace() - mStartX) * 1f / getIntervalDistance());
//        int count;
//        if (offset > getHorizontalSpace()) {
//            count = visableCount;
//        } else {
//            count = state.getItemCount();
//        }
//
//        for (int i = 0; i < count; i++) {
//            View view = recycler.getViewForPosition(i);
//            measureChildWithMargins(view, 0, 0);
//            addView(view);
//            int pos = getPosition(view);
//            Rect rect = getFrame(pos);
//            layoutItem(view, rect);
//            mHasAttachedItems.put(pos, true);
//        }

        mRecycle = recycler;
        mState = state;
    }

    @Override
    public boolean canScrollHorizontally() {
        return true;
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (mAnimation != null && mAnimation.isRunning()) mAnimation.cancel();

        int travel = dx;
        if (mOffsetAll + dx < 0) {
            travel = -mOffsetAll;
        } else if (mOffsetAll + dx > mTotalWidth - getHorizontalSpace()) {
            travel = (int) (mTotalWidth - getHorizontalSpace() - mOffsetAll);
        }

        mOffsetAll += travel;
        layoutItems(recycler, state, dx > 0 ? SCROLL_RIGHT : SCROLL_LEFT);
        return travel;
    }

    private void handleView(View view, int viewCenter, int center) {
        float scale = measureScale(viewCenter, center);
        view.setScaleX(scale);
        view.setScaleY(scale);

        float rolate = measureRotation(viewCenter, center);
        view.setRotationY(rolate);
    }

    private float measureRotation(int viewCenter, int center) {
        float rotationY = -M_MAX_ROTATION_Y * (viewCenter - center) / getIntervalDistance();
        rotationY = 2 * rotationY;
        if (Math.abs(rotationY) > M_MAX_ROTATION_Y) {
            if (rotationY > 0) {
                rotationY = M_MAX_ROTATION_Y;
            } else {
                rotationY = -M_MAX_ROTATION_Y;
            }
        }
        return rotationY;
    }

    private float measureScale(int viewCenter, int center) {
        float scale = Math.abs((viewCenter - center) * 1F / getIntervalDistance()) / 5;
        if (scale < 0) {
            scale = 0;
        } else if (scale > 0.3f) {
            scale = 0.3f;
        }
        return 1F - scale;
    }

    private int getHorizontalSpace() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    private int getVerticalSpace() {
        return getHeight() - getPaddingBottom() - getPaddingTop();
    }

    /**
     * 有效区域
     *
     * @return
     */
    private Rect getVisibleArea() {
        Rect result = new Rect(mOffsetAll, getPaddingTop(), getWidth() - getPaddingRight() - getPaddingLeft() + mOffsetAll, getVerticalSpace());
        return result;
    }

    /**
     * 布局所有items
     *
     * @param recycler
     * @param state
     * @param scrollDirection
     */
    private void layoutItems(RecyclerView.Recycler recycler, RecyclerView.State state, int scrollDirection) {
        if (state.isPreLayout()) return;
        Rect visibleRect = getVisibleArea();
        int position = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            position = getPosition(child);
            Rect rect = getFrame(position);

            if (!Rect.intersects(rect, visibleRect)) {
                Log.e("ttt", "--1--" + position + "--" + Rect.intersects(rect, visibleRect) + "--removeAndRecycleView");
                removeAndRecycleView(child, recycler); // remove和回收超出屏幕的view
                mHasAttachedItems.delete(position);
            } else {
                layoutItem(position, child, rect); //更新Item位置
                mHasAttachedItems.put(position, true);
            }
        }

        if (position == 0) position = mSelectPosition;

        int visableCount = (int) Math.ceil(getWidth() * 1f / getIntervalDistance()) + 1;
        int min = position - visableCount >= 0 ? position - visableCount : 0;
        int max = position + visableCount < getItemCount() ? position + visableCount : getItemCount();


        for (int i = min; i < max; i++) {
            Rect rect = getFrame(i);
            if (Rect.intersects(visibleRect, rect) &&
                    !mHasAttachedItems.get(i)) { //重新加载可见范围内的Item
                View scrap = recycler.getViewForPosition(i);
                measureChildWithMargins(scrap, 0, 0);
                if (scrollDirection == SCROLL_LEFT) { //向左滚动，新增的Item需要添加在最前面
                    addView(scrap, 0);
                } else { //向右滚动，新增的item要添加在最后面
                    addView(scrap);
                }
                layoutItem(scrap, rect); //将这个Item布局出来
                mHasAttachedItems.put(i, true);
            }
        }
    }

    /**
     * @param child
     * @param rect
     */
    private void layoutItem(View child, Rect rect) {
        int viewCenter = rect.left + mDecoratedChildWidth / 2;
        int center = getScreenCenterOffset();
        handleView(child, viewCenter, center);
        layoutDecoratedWithMargins(child, rect.left - mOffsetAll, rect.top, rect.right - mOffsetAll, rect.bottom);
    }

    /**
     * @param position
     * @param child
     * @param rect
     */
    private void layoutItem(int position, View child, Rect rect) {
        int viewCenter = rect.left + mDecoratedChildWidth / 2;
        int center = getScreenCenterOffset();
        float scale = measureScale(viewCenter, center);
        child.setScaleX(scale);
        child.setScaleY(scale);
        // 先等中间进行转动再到下一个或者上一个转动
        if (center > rect.left && center < rect.right && getCenterPosition() == position) {
            float rolate = measureRotation(viewCenter, center);
            child.setRotationY(rolate);
        }
        layoutDecoratedWithMargins(child, rect.left - mOffsetAll, rect.top, rect.right - mOffsetAll, rect.bottom);
    }

    /**
     * 动态获取Item的位置信息
     *
     * @param index item位置
     * @return item的Rect信息
     */
    private Rect getFrame(int index) {
        Rect frame = mAllItemFrames.get(index);
        if (frame == null) {
            frame = new Rect();
            float offset = mStartX + getIntervalDistance() * index; //原始位置累加（即累计间隔距离）
            frame.set(Math.round(offset), mStartY, Math.round(offset + mDecoratedChildWidth), mStartY + mDecoratedChildHeight);
        }

        return frame;
    }

    /**
     * 获取中间
     *
     * @return
     */
    public int getCenterPosition() {
        int pos = (int) (mOffsetAll / getIntervalDistance());
        int more = (int) (mOffsetAll % getIntervalDistance());
        if (more > getIntervalDistance() * 0.5f) pos++;
        return pos;
    }

    /**
     * 获取Item间隔
     */
    private float getIntervalDistance() {
        return mDecoratedChildWidth * mIntervalRatio;
    }

    /**
     * 获取第一个
     *
     * @return
     */
    public int getFirstVisiblePosition() {
        if (getChildCount() <= 0) {
            return 0;
        }

        View view = getChildAt(0);
        int pos = getPosition(view);

        return pos;
    }

    /**
     * 获取中线位置
     */
    private int getScreenCenterOffset() {
        return (int) (mOffsetAll + mStartX + getIntervalDistance());
    }

    /**
     * 计算Item所在的位置偏移
     *
     * @param position 要计算Item位置
     */
    private int calculateOffsetForPosition(int position) {
        return Math.round(getIntervalDistance() * position);
    }

    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);
        switch (state) {
            case RecyclerView.SCROLL_STATE_IDLE:
                //滚动停止时
                fixOffsetWhenFinishScroll();
                break;
            case RecyclerView.SCROLL_STATE_DRAGGING:
                //拖拽滚动时
                break;
            case RecyclerView.SCROLL_STATE_SETTLING:
                //动画滚动时
                break;
        }
    }

    /**
     * 修正停止滚动后，Item滚动到中间位置
     */
    private void fixOffsetWhenFinishScroll() {
        int scrollN = (int) (mOffsetAll * 1.0f / getIntervalDistance());
        float moreDx = (mOffsetAll % getIntervalDistance());
        if (moreDx > (getIntervalDistance() * 0.5)) {
            scrollN++;
        }
        int finalOffset = (int) (scrollN * getIntervalDistance());
        startScroll(mOffsetAll, finalOffset);
    }

    /**
     * 滚动到中间
     *
     * @param from
     * @param to
     */
    private void startScroll(int from, int to) {
        if (mAnimation != null && mAnimation.isRunning()) {
            mAnimation.cancel();
        }
        final int direction = from < to ? SCROLL_RIGHT : SCROLL_LEFT;
        mAnimation = ValueAnimator.ofFloat(from, to);
        mAnimation.setDuration(500);
        mAnimation.setInterpolator(new DecelerateInterpolator());
        mAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mOffsetAll = Math.round((float) animation.getAnimatedValue());
                layoutItems(mRecycle, mState, direction);
            }
        });
        mAnimation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                onSelectedCallBack();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mAnimation.start();
    }

    /**
     * 计算当前选中位置，并回调
     */
    private void onSelectedCallBack() {
        mSelectPosition = Math.round(mOffsetAll / getIntervalDistance());
        if (mSelectedListener != null && mSelectPosition != mLastSelectPosition) {
            mSelectedListener.onItemSelected(mSelectPosition);
        }
        mLastSelectPosition = mSelectPosition;
    }

    /**
     * 选中监听接口
     */
    public interface OnSelected {
        /**
         * 监听选中回调
         *
         * @param position 显示在中间的Item的位置
         */
        void onItemSelected(int position);
    }
}
