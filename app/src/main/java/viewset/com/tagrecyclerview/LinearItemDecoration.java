package viewset.com.tagrecyclerview;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextPaint;
import android.view.View;

import viewset.com.DensityUtil;

public class LinearItemDecoration extends RecyclerView.ItemDecoration {

    private final Paint.FontMetrics fontMetrics;
    private final int mTopHeight;
    private final int alignBottom;
    Paint paint;

    Paint paintT;

    public LinearItemDecoration(Context context) {
        Resources res = context.getResources();

        paint = new Paint();
        paint.setColor(Color.parseColor("#ff669900"));
        paint.setAntiAlias(true);
        BitmapFactory.Options options = new BitmapFactory.Options();

        //
        paintT = new TextPaint();
        paintT.setAntiAlias(true);
        paintT.setStyle(Paint.Style.FILL);
        paintT.setStrokeWidth(8);
        paintT.setTextSize(DensityUtil.dp2px(context, 14));
        paintT.setColor(Color.DKGRAY);
        paintT.setTextAlign(Paint.Align.CENTER);
        fontMetrics = paintT.getFontMetrics();

        //决定悬浮栏的高度等
        mTopHeight = DensityUtil.dp2px(context, 40);
        //决定文本的显示位置等
        alignBottom = DensityUtil.dp2px(context, 10);
    }

    @Override
    public void onDraw(@NonNull Canvas canvas, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.onDraw(canvas, parent, state);

        int left = parent.getPaddingLeft();
        int right = parent.getMeasuredWidth() - parent.getPaddingRight();

        int parentWidth = parent.getMeasuredWidth();

        for (int i = 0; i != parent.getChildCount(); i++) {
            // 直接获得的child只有当前显示的，所以就算i是0的index也只是当前第一个，而不是所有第一个
            View child = parent.getChildAt(i);
            int pos = parent.getChildAdapterPosition(child);
            if (isFirstInGroup(pos)) {
                // 每组第一个item都留有空间来绘制
                int top = child.getTop() - mTopHeight;
                int bottom = child.getTop();
                // 绘制背景色
                canvas.drawRect(left, top, right, bottom, paint);
                // 绘制组名,居中,paintT.setTextAlign(Paint.Align.CENTER);对应
                String groupName = getGroupName(pos);
                float textLeft = left + parentWidth / 2;
                float distance = (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.bottom;
                float textBottom = top + mTopHeight / 2 + distance;
                canvas.drawText(groupName, textLeft, textBottom, paintT);
            }
        }
    }

    @Override
    public void onDrawOver(@NonNull Canvas canvas, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.onDrawOver(canvas, parent, state);

        int pos = ((LinearLayoutManager) (parent.getLayoutManager())).findFirstVisibleItemPosition();
        int left = parent.getPaddingLeft();
        int right = parent.getWidth() - parent.getPaddingRight();
        int parentWidth = parent.getMeasuredWidth();

        // 文字
        float textLeft = left + parentWidth / 2;
        float distance = (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.bottom;
        float textBottom = mTopHeight / 2 + distance;

        // jiang图片的设置
        float bitLeft = left;
        String groupName = getGroupName(pos);

        //此处开始不同
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            //判断item是不是该组的最后一个元素
            if (isLastInGroup(pos)) {
                int bottom = child.getBottom();
                //滑动过程中，若分组中最后一个item的bottom小于label的高度，就把label的绘制位置往上提。
                if (bottom <= mTopHeight) {
                    canvas.drawRect(left, 0, right, bottom, paint);
                    textBottom = textBottom - (mTopHeight - bottom);
                    canvas.drawText(groupName, textLeft, textBottom, paintT);
                    return;
                }
            }
        }


        canvas.drawRect(left, 0, right, mTopHeight, paint);
        canvas.drawText(groupName, textLeft, textBottom, paintT);
    }


    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        int pos = parent.getChildAdapterPosition(view);
        //只有是同一组的第一个才显示悬浮栏
        if (isFirstInGroup(pos)) {
            outRect.top = mTopHeight;
        } else {
            outRect.top = 0;
        }
    }

    /**
     * 判断是不是组中的第一个位置
     *
     * @param pos
     * @return
     */
    private boolean isFirstInGroup(int pos) {
        return pos == 0 || pos % 10 == 0;
    }

    /**
     * 判断是否最后一个
     *
     * @param pos
     * @return
     */
    private boolean isLastInGroup(int pos) {
        return (pos / 10) != ((pos + 1) / 10);
    }

    /**
     * 获取数组的名字
     *
     * @param pos
     * @return
     */
    private String getGroupName(int pos) {
        return "第" + pos / 10 + "组";
    }
}
