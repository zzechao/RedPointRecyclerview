package viewset.com.redpointrecyclerview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;

/**
 * QQ红点功能+recyclerview
 */
public class RedPointRecyclerview extends FrameLayout {

    RecyclerView rv;

    Paint qqPointPaint;
    Paint paint2;
    Paint textPaint;

    SparseArray<PointF> qqPoints = new SparseArray<>();

    private int currentTouchPos = -1;
    private String num;
    private float textSize;

    private PointF point0 = new PointF();
    private PointF point1 = new PointF();

    private PointF point2 = new PointF();
    private PointF point3 = new PointF();

    private PointF point4 = new PointF();
    private PointF point5 = new PointF();

    private boolean _isTouchIn; // 接触点是否是point0
    private boolean _isOut; // 是否达到消失距离
    private boolean _isAnimStart; //

    // point1和point的0的长度
    private double mWidth;
    // 阻力
    private float resistance;
    // point1的半径
    private float radiu1;
    // point0的半径
    private float radiu0;

    private Path path = new Path();

    private float MIN_RADIU = 2; // point0的最小半径
    private float MAX_RADIU = 6; // point的半径
    private final float TOUCH_RESISTANCE = 20; // 阻力

    public RedPointRecyclerview(@NonNull Context context) {
        this(context, null);
    }

    public RedPointRecyclerview(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RedPointRecyclerview(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.layout_qqpoint_recyclerview, this, true);
        rv = findViewById(R.id.rv);

        qqPointPaint = new Paint();
        qqPointPaint.setColor(Color.parseColor("#ffff4444"));
        qqPointPaint.setAntiAlias(true);
        qqPointPaint.setStyle(Paint.Style.FILL);

        paint2 = new Paint();
        paint2.setColor(Color.BLUE);
        paint2.setAntiAlias(true);
        paint2.setStyle(Paint.Style.FILL);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setAntiAlias(true);

        resistance = dip2px(context, TOUCH_RESISTANCE);
    }

    public static int dip2px(Context context, float dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public void setLayoutManager(RecyclerView.LayoutManager layoutManager) {
        rv.setLayoutManager(layoutManager);

        QQpointItemDecoration itemDecoration1 = new QQpointItemDecoration(getContext());
        rv.addItemDecoration(itemDecoration1);
    }

    public void setAdapter(RecyclerView.Adapter adapter) {
        rv.setAdapter(adapter);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                float preX = event.getX(event.findPointerIndex(0));
                float preY = event.getY(event.findPointerIndex(0));
                for (int i = 0; i < qqPoints.size(); i++) {
                    PointF point = qqPoints.get(qqPoints.keyAt(i));
                    if (point != null) {
                        float x = preX - point.x;
                        float y = preY - point.y;
                        if (Math.abs(x) <= 10 && Math.abs(y) <= 10 && !_isTouchIn) {
                            _isTouchIn = true;
                            currentTouchPos = qqPoints.keyAt(i);
                            point0.set(point.x, point.y);
                            View child = rv.getLayoutManager().findViewByPosition(currentTouchPos);
                            RecyclerAdapter.NormalHolder normalHolder = (RecyclerAdapter.NormalHolder) rv.getChildViewHolder(child);
                            MAX_RADIU = normalHolder.qqpoint.getWidth() / 2;
                            MIN_RADIU = MAX_RADIU / 2;
                            radiu1 = MAX_RADIU;
                            radiu0 = MAX_RADIU;
                            num = normalHolder.qqpoint.getText().toString();
                            textSize = normalHolder.qqpoint.getTextSize();
                            textPaint.setTextSize(textSize);

                            event.setAction(MotionEvent.ACTION_MOVE);
                            boolean touch = this.onTouchEvent(event);
                            normalHolder.qqpoint.setVisibility(INVISIBLE);
                            return touch;
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                if (_isTouchIn) {
                    return this.onTouchEvent(event);
                }
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                if (_isTouchIn) {
                    point1.set((int) event.getX(event.findPointerIndex(0))
                            , (int) event.getY(event.findPointerIndex(0)));
                    postInvalidate();
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (_isTouchIn) {
                    if (_isOut) { // 消失状态时
                        View child = rv.getLayoutManager().findViewByPosition(currentTouchPos);
                        RecyclerAdapter.NormalHolder normalHolder = (RecyclerAdapter.NormalHolder) rv.getChildViewHolder(child);
                        normalHolder.qqpoint.setVisibility(GONE);
                        qqPoints.remove(currentTouchPos);
                        reset();
                    } else {
                        startResetAnim();
                    }
                    return true;
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    private void startResetAnim() {
        _isAnimStart = true;
        ValueAnimator animatorX = ValueAnimator.ofFloat(point1.x, point0.x);
        animatorX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                point1.x = (float) animation.getAnimatedValue();
                postInvalidate();
            }
        });
        ValueAnimator animatorY = ValueAnimator.ofFloat(point1.y, point0.y);
        animatorY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                point1.y = (float) animation.getAnimatedValue();
                postInvalidate();
            }
        });
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(animatorX, animatorY);
        animatorSet.setInterpolator(new OvershootInterpolator());
        animatorSet.start();
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                View child = rv.getLayoutManager().findViewByPosition(currentTouchPos);
                RecyclerAdapter.NormalHolder normalHolder = (RecyclerAdapter.NormalHolder) rv.getChildViewHolder(child);
                normalHolder.qqpoint.setVisibility(VISIBLE);
                reset();
            }
        });
    }

    private void reset() {
        _isAnimStart = false;
        _isTouchIn = false;
        _isOut = false;
        radiu0 = radiu1;
        point0.set(0, 0);
        point1.set(0, 0);

        currentTouchPos = -1;
        postInvalidate();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (point1.x != 0 && point1.y != 0) { // 如果point1的xy有值
            resetPoint(); // 重新计算每个点
            if (!_isOut) { // 超出拖拽的最大距离，不用绘制贝塞尔曲线
                //canvas.drawCircle(point2.x, point2.y, 2, paint2);
                //canvas.drawCircle(point3.x, point3.y, 2, paint2);
                //canvas.drawCircle(point4.x, point4.y, 2, paint2);
                //canvas.drawCircle(point5.x, point5.y, 2, paint2);

                path.reset();
                path.moveTo(point2.x, point2.y);
                path.quadTo((point0.x + point1.x) / 2, (point0.y + point1.y) / 2, point4.x, point4.y);
                path.lineTo(point5.x, point5.y);
                path.quadTo((point0.x + point1.x) / 2, (point0.y + point1.y) / 2, point3.x, point3.y);
                path.lineTo(point2.x, point2.y);
                path.close();
                canvas.drawPath(path, qqPointPaint);
            }

            canvas.drawCircle(point1.x, point1.y, radiu1, qqPointPaint);

            if (!_isOut && point0.x != 0 && point0.y != 0) {
                canvas.drawCircle(point0.x, point0.y, radiu0, qqPointPaint);
            }

            if (!_isAnimStart) {
                int textWidth = (int) textPaint.measureText(num);
                canvas.drawText(num, point1.x - textWidth / 2, point1.y + textSize / 2, textPaint);
            }
        }

    }

    private void resetPoint() {
        double a = Math.atan((point1.y - point0.y) * 1F / (point1.x - point0.x));

        mWidth = Math.abs((point1.y - point0.y) / Math.sin(a));
        radiu0 = (radiu1 - mWidth / resistance) < MIN_RADIU ? MIN_RADIU : (float) (radiu1 - mWidth / resistance);
        point2.x = (float) (point0.x + (radiu0 - 1) * Math.sin(a));
        point2.y = (float) (point0.y - (radiu0 - 1) * Math.cos(a));

        point3.x = (float) (point0.x - (radiu0 - 1) * Math.sin(a));
        point3.y = (float) (point0.y + (radiu0 - 1) * Math.cos(a));

        point4.x = (float) (point1.x + radiu1 * Math.sin(a));
        point4.y = (float) (point1.y - radiu1 * Math.cos(a));

        point5.x = (float) (point1.x - radiu1 * Math.sin(a));
        point5.y = (float) (point1.y + radiu1 * Math.cos(a));
        if ((radiu1 - mWidth / resistance) < 3) {
            _isOut = true;
        }
    }

    class QQpointItemDecoration extends RecyclerView.ItemDecoration {

        private final Bitmap mMedalBmp;
        Paint paint;

        public QQpointItemDecoration(Context context) {
            paint = new Paint();
            paint.setColor(Color.parseColor("#ff669900"));
            paint.setAntiAlias(true);

            BitmapFactory.Options options = new BitmapFactory.Options();
            // 缩小至原大小的1/2
            options.inSampleSize = 2;
            mMedalBmp = BitmapFactory.decodeResource(context.getResources(), R.mipmap.jiang, options);
        }

        @Override
        public void onDraw(@NonNull Canvas canvas, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            super.onDraw(canvas, parent, state);
        }

        @Override
        public void onDrawOver(@NonNull Canvas canvas, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            //Log.e("ttt", "--onDrawOver");
            super.onDrawOver(canvas, parent, state);

            int childCount = parent.getChildCount();
            int left;
            for (int i = 0; i < childCount; i++) {
                View child = parent.getChildAt(i);

                // 根据child获取对应 adapter中的position
                int position = parent.getChildAdapterPosition(child);
                RecyclerView.LayoutManager manager = parent.getLayoutManager();
                left = manager.getLeftDecorationWidth(child);

                if (parent.getAdapter().getItemViewType(position) == RecyclerAdapter.ITEM_TYPE.ITEM_TYPE_SECTION.ordinal()) {
                    canvas.drawBitmap(mMedalBmp, left + mMedalBmp.getWidth() / 2, child.getTop() + child.getHeight() / 2 - mMedalBmp.getHeight() / 2, paint);
                }

                // 调用外部去构建
                RedPointRecyclerview.this.drawQQPoint(canvas, parent, position, child);
            }
        }


        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
        }
    }

    /**
     * 根据position绘制出红点
     *
     * @param canvas
     * @param parent
     * @param position
     * @param child
     */
    private void drawQQPoint(Canvas canvas, RecyclerView parent, int position, View child) {
        // 每次滑动都重新计算每个红点的坐标
        if (position % 2 == 0 && parent.getAdapter().getItemViewType(position) == RecyclerAdapter.ITEM_TYPE.ITEM_TYPE_ITEM.ordinal()) {
            RecyclerAdapter.NormalHolder normalHolder = (RecyclerAdapter.NormalHolder) parent.getChildViewHolder(child);
            float cx = child.getLeft() + normalHolder.qqpoint.getLeft() + normalHolder.qqpoint.getWidth() / 2;
            float cy = child.getTop() + normalHolder.qqpoint.getTop() + normalHolder.qqpoint.getHeight() / 2;
            qqPoints.put(position, new PointF(cx, cy));
        }
    }
}