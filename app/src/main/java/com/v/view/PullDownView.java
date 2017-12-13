package com.v.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.view.animation.PathInterpolatorCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * Created by Administrator on 2017/12/12.
 */

public class PullDownView extends View {
    //可以拖动的高度
    private int mDragHeight = 300;

    public PullDownView(Context context) {
        this(context, null);
    }

    public PullDownView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PullDownView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private Paint mCirclePaint;

    private float mCircleRadius = 40;
    private float mCirclePointX;
    private float mCirclePointY;
    private float mProgress;

    private int mContentMargin = 0;
    private Drawable mContent = null;

    //目标宽度
    private int targetWidth = 400;

    //贝塞尔曲线的路径 以及画笔
    private Path mPath = new Path();
    private Paint mPathPaint;
    //重心点最终高度 决定重心点的Y坐标
    private int mTargetGravityHeight = 10;
    //角度变换 0-135度
    private int mTargetAngle = 105;

    private android.view.animation.Interpolator mProgressIntetpolator = new DecelerateInterpolator();
    private android.view.animation.Interpolator mTangentAngleIntetpolator;

    private void init(AttributeSet attrs) {

        //得到用户设置的参数


        final Context context = getContext();
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.PullDownView);

        int color = array.getColor(R.styleable.PullDownView_pColor, 0x20000000);//默认半透明黑色
        mCircleRadius = array.getDimension(R.styleable.PullDownView_pRadius, mCircleRadius);
        mDragHeight = array.getDimensionPixelOffset(R.styleable.PullDownView_pDragHeight, mDragHeight);
        mTargetAngle = array.getInteger(R.styleable.PullDownView_pTangentAngle, 100);
        targetWidth = array.getDimensionPixelOffset(R.styleable.PullDownView_pTargetWidth, targetWidth);
        mTargetGravityHeight = array.getDimensionPixelOffset(R.styleable.PullDownView_pTargetGravityHeight, mTargetGravityHeight);
        mContent = array.getDrawable(R.styleable.PullDownView_pContentDrawable);
        mContentMargin = array.getDimensionPixelOffset(R.styleable.PullDownView_pContentDrawableMargin, 0);
        //销毁
        array.recycle();


        //初始化圆画笔
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setDither(true);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
//        paint.setStrokeWidth(5);
        paint.setColor(Color.BLUE);
        mCirclePaint = paint;

        //初始化路径部分画笔
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setDither(true);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
//        paint.setStrokeWidth(5);
        paint.setColor(Color.BLUE);
        mPathPaint = paint;

        //切角路径插值器
        mTangentAngleIntetpolator = PathInterpolatorCompat.create(
                (mCircleRadius * 2) / mDragHeight, 90.0f / mTargetAngle
        );


    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //进行基础坐标参数
        int count = canvas.save();
        float tranx = (getWidth() - getValueByLine(getWidth(), targetWidth, mProgress)) / 2;
        canvas.translate(tranx, 0);
        //画贝塞尔曲线
        canvas.drawPath(mPath, mPathPaint);

        //画圆
        canvas.drawCircle(mCirclePointX, mCirclePointY, mCircleRadius, mCirclePaint);

        Drawable mDrawable = mContent;
        if (mDrawable != null) {
            canvas.save();
            //剪切矩形区域
            canvas.clipRect(mDrawable.getBounds());
            //绘制Drawable
            mDrawable.draw(canvas);
            canvas.restore();
        }
        canvas.restoreToCount(count);
    }

    public void setProgress(float progress) {
        mProgress = progress;
        //请求进行重新测量
        requestLayout();
    }

    /**
     * 测量
     *
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int modeWidth = MeasureSpec.getMode(widthMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);


        int modeHeight = MeasureSpec.getMode(heightMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        int iwidth = (int) (2 * mCircleRadius + getPaddingLeft() + getPaddingRight());
        int iHeight = (int) ((mDragHeight * mProgress + 0.5f) + getPaddingTop() + getPaddingBottom());
        int measureWidth, measureHeight;
        if (modeWidth == MeasureSpec.EXACTLY) {
            //确切的
            measureWidth = width;

        } else if (modeWidth == MeasureSpec.AT_MOST) {
            //最多
            measureWidth = Math.min(iwidth, width);

        } else {
            measureWidth = iwidth;
        }

        if (modeHeight == MeasureSpec.EXACTLY) {
            //确切的
            measureHeight = height;

        } else if (modeHeight == MeasureSpec.AT_MOST) {
            //最多
            measureHeight = Math.min(iHeight, height);
        } else {
            measureHeight = iHeight;
        }

        //设置测量的宽高
        setMeasuredDimension(measureWidth, measureHeight);
    }

    /**
     * 当大小改变时触发
     *
     * @param w
     * @param h
     * @param oldw
     * @param oldh
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        //当高度变化时进行路径更新
        updatePathLayout();
//        //控件的中心区域
//        mCirclePointX = getWidth() >> 1;
//        mCirclePointY = getHeight() >> 1;
    }

    /**
     * 更新我们 的路径的相关操作
     */
    private void updatePathLayout() {

        final float progress = mProgressIntetpolator.getInterpolation(mProgress);

        //获取可绘制区域高度宽度
        final float w = getValueByLine(getWidth(), targetWidth, mProgress);
        final float h = getValueByLine(0, mDragHeight, mProgress);
        //X对称轴的参数 圆心X
        final float cPointx = w / 2.0f;
        //圆的半径
        final float cRadius = mCircleRadius;
        //圆心Y坐标
        final float cPointy = h - cRadius;
        //控制点结束Y的值
        final float endControlY = mTargetGravityHeight;

        //更新圆的坐标
        mCirclePointX = cPointx;
        mCirclePointY = cPointy;
        //路径
        final Path path = mPath;

        //复位操作
        path.reset();
        path.moveTo(0, 0);

        //左边部分结束点和控制点
        float lEndPointX, lEndPointY;
        float lControlPointX, lControlPointY;

        //获取当前切线的弧度

        float angle = mTargetAngle * mTangentAngleIntetpolator.getInterpolation(progress);

        double radian = Math.toRadians(angle);
        float x = (float) (Math.sin(radian) * cRadius);
        float y = (float) (Math.cos(radian) * cRadius);

        lEndPointX = cPointx - x;
        lEndPointY = cPointy + y;
        //控制点的Y坐标变化
        lControlPointY = getValueByLine(0, endControlY, mProgress);
        //控制点与结束点之间的高度
        float tHeight = lEndPointY - lControlPointY;
        //控制点与X的坐标距离
        float tWidth = (float) (tHeight / Math.tan(radian));
        lControlPointX = lEndPointX - tWidth;

        //贝塞尔曲线
        path.quadTo(lControlPointX, lControlPointY, lEndPointX, lEndPointY);
        //连接到右边
        path.lineTo(cPointx + (cPointx - lEndPointX), lEndPointY);
        //画右边的贝塞尔曲线
        path.quadTo(cPointx + (cPointx - lControlPointX), lControlPointY, w, 0);

        //更新内容部分Drawable
        updateContentLayout(cPointx, cPointy, cRadius);

    }

    /**
     * 对内容部分进行测量并设置
     * @param cx
     * @param cy
     * @param radius
     */
    private void updateContentLayout(float cx, float cy, float radius) {
        Drawable drawable = mContent;
        if(drawable!=null){
            int margin = mContentMargin;
            int l = (int) (cx - radius + margin);
            int r = (int) (cx + radius - margin);
            int t = (int) (cy - radius + margin);
            int b = (int) (cy + radius - margin);

            drawable.setBounds(l,t,r,b);
        }
    }

    /**
     * 获取当前值
     *
     * @param start
     * @param end
     * @param progress
     * @return
     */
    private float getValueByLine(float start, float end, float progress) {
        float v = start + (end - start) * progress;
        return v;
    }

    private ValueAnimator valueAnimator;

    //添加释放动画
    public void release() {
        if (valueAnimator == null) {
            final ValueAnimator animator = ValueAnimator.ofFloat(mProgress, 0f);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.setDuration(400);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    Object animatedValue = animator.getAnimatedValue();
                    if (animatedValue instanceof Float) {
                        setProgress((Float) animatedValue);
                    }
                }
            });
            valueAnimator = animator;
        } else {
            valueAnimator.cancel();
            valueAnimator.setFloatValues(mCirclePointX, 0f);
        }
        valueAnimator.start();
    }
}
