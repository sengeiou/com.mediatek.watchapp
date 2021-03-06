package com.mediatek.watchapp;

import android.animation.LayoutTransition;
import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.ScrollView;

public class NotificationScrollView extends ScrollView implements com.mediatek.watchapp.SwipeHelper.Callback {
    private Callback mCallback;
    private GestureDetector mGestureDetector;
    protected int mLastScrollPosition;
    private FrameLayout mLayout;
    private SwipeHelper mSwipeHelper;

    /* renamed from: com.mediatek.watchapp.NotificationScrollView$1 */
    class C01401 implements Runnable {
        C01401() {
        }

        public void run() {
            LayoutTransition transition = NotificationScrollView.this.mLayout.getLayoutTransition();
            if (transition == null || !transition.isRunning()) {
                NotificationScrollView.this.scrollTo(0, NotificationScrollView.this.mLastScrollPosition);
            }
        }
    }

    public interface Callback {
        void handleSwipe(View view);
    }

    class YScrollDetector extends SimpleOnGestureListener {
        YScrollDetector() {
        }

        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return Math.abs(distanceY) > Math.abs(distanceX);
        }
    }

    public NotificationScrollView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
        this.mSwipeHelper = new SwipeHelper(0, this, getResources().getDisplayMetrics().density, (float) ViewConfiguration.get(context).getScaledPagingTouchSlop());
        this.mGestureDetector = new GestureDetector(context, new YScrollDetector());
    }

    public void removeViewInLayout(View view) {
        dismissChild(view);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (this.mSwipeHelper.onInterceptTouchEvent(ev) || super.onInterceptTouchEvent(ev)) {
            return this.mGestureDetector.onTouchEvent(ev);
        }
        return false;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        return !this.mSwipeHelper.onTouchEvent(ev) ? super.onTouchEvent(ev) : true;
    }

    public boolean canChildBeDismissed(View v) {
        return true;
    }

    public void dismissChild(View v) {
        this.mSwipeHelper.dismissChild(v, 0.0f);
    }

    public void onChildDismissed(View v) {
        View contentView = getChildContentView(v);
        contentView.setAlpha(1.0f);
        contentView.setTranslationX(0.0f);
        if (this.mCallback != null) {
            this.mCallback.handleSwipe(v);
        }
    }

    public void onBeginDrag(View v) {
        requestDisallowInterceptTouchEvent(true);
    }

    public void onDragCancelled(View v) {
    }

    public View getChildAtPosition(MotionEvent ev) {
        return this;
    }

    public View getChildContentView(View v) {
        return v.findViewById(R.id.card_scroll_content);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        setScrollbarFadingEnabled(true);
        this.mLayout = (FrameLayout) findViewById(R.id.card_scroll_content);
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.mSwipeHelper.setDensityScale(getResources().getDisplayMetrics().density);
        this.mSwipeHelper.setPagingTouchSlop((float) ViewConfiguration.get(getContext()).getScaledPagingTouchSlop());
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        LayoutTransition transition = this.mLayout.getLayoutTransition();
        if (transition == null || !transition.isRunning()) {
            this.mLastScrollPosition = 0;
            post(new C01401());
        }
    }

    public void setLayoutTransition(LayoutTransition transition) {
        this.mLayout.setLayoutTransition(transition);
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }
}
