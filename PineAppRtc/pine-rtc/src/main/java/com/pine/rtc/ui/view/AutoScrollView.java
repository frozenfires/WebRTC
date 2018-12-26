package com.pine.rtc.ui.view;


import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

/**
 * 监听ScrollView滚动到顶部或者底部做相关事件拦截
 */
public class AutoScrollView extends ScrollView {

    private boolean isScrolledToTop = true; // 初始化的时候设置一下值
    private boolean isScrolledToBottom = false;
    private int paddingTop = 0;
    private final int MSG_SCROLL = 10;
    private final int MSG_SCROLL_Loop = 11;
    private boolean scrollAble = false;//是否能滑动

    //三个可设置的属性
    private boolean autoToScroll = true;   //是否自动滚动
    private boolean scrollLoop = false; //是否循环滚动
    private int fistTimeScroll = 5000;//多少秒后开始滚动，默认5秒
    private int scrollRate = 50;//多少毫秒滚动一个像素点


    public AutoScrollView(Context context) {
        this(context, null);
    }

    public AutoScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AutoScrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

    }


    private ISmartScrollChangedListener mSmartScrollChangedListener;

    /**
     * 定义监听接口
     */
    public interface ISmartScrollChangedListener {
        void onScrolledToBottom(); //滑动到底部

        void onScrolledToTop();//滑动到顶部

    }

    //设置滑动到顶部或底部的监听
    public void setScanScrollChangedListener(ISmartScrollChangedListener smartScrollChangedListener) {
        mSmartScrollChangedListener = smartScrollChangedListener;
    }

    //ScrollView内的视图进行滑动时的回调方法，据说是API 9后都是调用这个方法，但是我测试过并不准确
    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
        if (scrollY == 0) {
            isScrolledToTop = clampedY;
            isScrolledToBottom = false;
        } else {
            isScrolledToTop = false;
            isScrolledToBottom = clampedY;//系统回调告诉你什么时候滑动到底部
        }

        notifyScrollChangedListeners();
    }

    int lastY;

    //触摸事件
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int y = (int) event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 记录触摸点坐标
                lastY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                // 计算偏移量
                int offsetY = y - lastY;
                // 在当前left、top、right、bottom的基础上加上偏移量
                paddingTop = paddingTop - offsetY / 10;
                //不要问我上面10怎么来的，我大概估算的，正常一点应该是7或8吧，我故意让手动滑动的时候少一丢
                scrollTo(0, paddingTop);
                break;
        }
        return true;
    }


    //ScrollView内的视图进行滑动时的回调方法，据说是API 9前都是调用这个方法，我新版的SDK也是或回调这个方法
    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);

//        if (android.os.Build.VERSION.SDK_INT < 9) {  // API 9及之后走onOverScrolled方法监听，
        if (getScrollY() == 0) {
            isScrolledToTop = true;
            isScrolledToBottom = false;
        } else if (getScrollY() + getHeight() - getPaddingTop() - getPaddingBottom() == getChildAt(0).getHeight()) {
            isScrolledToBottom = true;
            isScrolledToTop = false;
        } else {
            isScrolledToTop = false;
            isScrolledToBottom = false;
        }
        notifyScrollChangedListeners();
//        }

    }


    //判断是否滑动到底部或顶部
    private void notifyScrollChangedListeners() {
        if (isScrolledToTop) {
            if (mSmartScrollChangedListener != null) {
                mSmartScrollChangedListener.onScrolledToTop();
            }
        } else if (isScrolledToBottom) {
            mHandler.removeMessages(MSG_SCROLL);
            if (!scrollLoop) {
                scrollAble = false;
            }
            if (scrollLoop) {
                mHandler.sendEmptyMessageDelayed(MSG_SCROLL_Loop, fistTimeScroll);
            }
            if (mSmartScrollChangedListener != null) {
                mSmartScrollChangedListener.onScrolledToBottom();
            }
        }
    }


    //handler
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_SCROLL:
                    if (scrollAble && autoToScroll) {
                        scrollTo(0, paddingTop);
                        paddingTop += 1;
                        mHandler.removeMessages(MSG_SCROLL);
                        mHandler.sendEmptyMessageDelayed(MSG_SCROLL, scrollRate);
                    }
                    break;
                case MSG_SCROLL_Loop:
                    paddingTop = 0;
                    autoToScroll = true;
                    mHandler.sendEmptyMessageDelayed(MSG_SCROLL, fistTimeScroll);

            }

        }
    };


    //获取子View和ScrollView的高度比较，决定是否能够滑动View
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        View childAt = getChildAt(0);
        int childMeasuredHeight = childAt.getMeasuredHeight(); //获取子控件的高度
        int measuredHeight = getMeasuredHeight();//获取ScrollView的高度
//        Log.e("onMeasure", "childMeasuredHeight:" + childMeasuredHeight + "  ,measuredHeight:" + measuredHeight);
        if (childMeasuredHeight > measuredHeight) {  //如果子控件的高度大于父控件才需要滚动
            scrollAble = true;
            paddingTop = 0;
            mHandler.sendEmptyMessageDelayed(MSG_SCROLL, fistTimeScroll);

        } else {
            scrollAble = false;
        }
    }

    //设置是否自动滚动
    public void setAutoToScroll(boolean autoToScroll) {
        this.autoToScroll = autoToScroll;
    }

    //设置第一次开始滚动的时间
    public void setFistTimeScroll(int fistTimeScroll) {
        this.fistTimeScroll = fistTimeScroll;
        mHandler.removeMessages(MSG_SCROLL);
        mHandler.sendEmptyMessageDelayed(MSG_SCROLL, fistTimeScroll);
    }

    //设置滚动的速率，多少毫秒滚动一个像素点
    public void setScrollRate(int scrollRate) {
        this.scrollRate = scrollRate;
    }

    //设置是否循环滚动
    public void setScrollLoop(boolean scrollLoop) {
        this.scrollLoop = scrollLoop;
    }

}