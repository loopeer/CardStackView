package com.loopeer.cardstack;

import android.content.Context;
import android.database.Observable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.OverScroller;

import java.util.ArrayList;
import java.util.List;

public class CardStackView extends ViewGroup implements ScrollDelegate {

    static final int ANIMATED_SCROLL_GAP = 250;
    static final float MAX_SCROLL_FACTOR = 0.5f;
    private static final int INVALID_POINTER = -1;

    private static final String TAG = "CardStackView";

    static final int DEFUAL_SELECT_POSITION = -1;

    private int mTotalLength;
    private int mOverlapeGaps;
    private int mOverlapeGapsCollapse;
    private int mCardNormalHeight;
    private final int EXPAND_TYPE = 0;
    private final int COLLAPSE_TYPE = 1;
    private StackAdapter mStackAdapter;
    private final ViewDataObserver mObserver = new ViewDataObserver();
    private int mSelectPosition = DEFUAL_SELECT_POSITION;
    private int mNormalChildHeight = Integer.MAX_VALUE;
    private int mShowHeight;
    private List<ViewHolder> mViewHolders;

    private AnimatorAdapter mAnimatorAdapter;

    private OverScroller mScroller;
    private int mLastMotionY;
    private boolean mIsBeingDragged = false;
    private VelocityTracker mVelocityTracker;
    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;
    private int mActivePointerId = INVALID_POINTER;
    private final int[] mScrollOffset = new int[2];
    private int mNestedYOffset;
    private int mOverscrollDistance;
    private int mOverflingDistance;
    private boolean mScrollEnable = true;

    private ScrollDelegate mScrollDelegate;

    public CardStackView(Context context) {
        this(context, null);
    }

    public CardStackView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CardStackView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mViewHolders = new ArrayList<>();
        mOverlapeGaps = dp2px(20);
        mOverlapeGapsCollapse = dp2px(20);
        mCardNormalHeight = dp2px(160);
        initScroller();
        setAnimatorAdapter(new UpDownStackAnimatorAdapter(this));
    }

    private void initScroller() {
        mScroller = new OverScroller(getContext());
        setFocusable(true);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mOverscrollDistance = configuration.getScaledOverscrollDistance();
        mOverflingDistance = configuration.getScaledOverflingDistance();
    }

    private int dp2px(int value) {
        final float scale = getContext().getResources().getDisplayMetrics().density;
        return (int) (value * scale + 0.5f);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        checkContentHeightByParent();
        measureChild(widthMeasureSpec, heightMeasureSpec);
    }

    private void checkContentHeightByParent() {
        View parentView = (View) getParent();
        mShowHeight = parentView.getMeasuredHeight() - parentView.getPaddingTop() - parentView.getPaddingBottom();
    }

    private void measureChild(int widthMeasureSpec, int heightMeasureSpec) {
        int maxWidth = 0;
        mTotalLength = 0;
        mTotalLength += getPaddingTop() + getPaddingBottom();
        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
            final int totalLength = mTotalLength;
            final int childHeight = mCardNormalHeight;
            final MarginLayoutParams lp =
                    (MarginLayoutParams) child.getLayoutParams();
            mTotalLength = Math.max(totalLength, totalLength + childHeight + lp.topMargin +
                    lp.bottomMargin);
            mTotalLength -= mOverlapeGaps * 2;
            final int margin = lp.leftMargin + lp.rightMargin;
            final int measuredWidth = child.getMeasuredWidth() + margin;
            maxWidth = Math.max(maxWidth, measuredWidth);
            mNormalChildHeight = Math.min(childHeight, mNormalChildHeight);
        }

        mTotalLength += mOverlapeGaps * 2;
        int heightSize = mTotalLength;
        heightSize = Math.max(heightSize, mShowHeight);
        int heightSizeAndState = resolveSizeAndState(heightSize, heightMeasureSpec, 0);
        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, 0),
                heightSizeAndState);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        layoutChild();
    }

    private void layoutChild() {
        int childTop = getPaddingTop();
        int childLeft = getPaddingLeft();

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            final int childWidth = child.getMeasuredWidth();
            final int childHeight = child.getMeasuredHeight();

            final MarginLayoutParams lp =
                    (MarginLayoutParams) child.getLayoutParams();
            childTop += lp.topMargin;
            if (i != 0) {
                childTop -= mOverlapeGaps * 2;
                child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
            } else {
                child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
            }
            childTop += mNormalChildHeight;
        }
    }

    public void updateSelectPosition(final int selectPosition) {
        post(new Runnable() {
            @Override
            public void run() {
                doCardClickAnimation(mViewHolders.get(selectPosition), selectPosition);
            }
        });
    }

    public void clearSelectPosition() {
        updateSelectPosition(mSelectPosition);
    }

    public void setAdapter(StackAdapter stackAdapter) {
        mStackAdapter = stackAdapter;
        mStackAdapter.registerObserver(mObserver);
        refreshView();
    }

    public void setAnimatorAdapter(AnimatorAdapter animatorAdapter) {
        mAnimatorAdapter = animatorAdapter;
        if (mAnimatorAdapter instanceof UpDownStackAnimatorAdapter) {
            mScrollDelegate = new StackScrollDelegateImpl(this);
        } else {
            mScrollDelegate = this;
        }
    }

    private void refreshView() {
        removeAllViews();
        mViewHolders.clear();
        for (int i = 0; i < mStackAdapter.getItemCount(); i++) {
            ViewHolder holder = getViewHolder(i);
            holder.position = i;
            holder.onItemExpand(i == mSelectPosition);
            addView(holder.itemView);
            setClickAnimator(holder, i);
            mStackAdapter.bindViewHolder(holder, i);
        }
        requestLayout();
    }

    ViewHolder getViewHolder(int i) {
        if (i == DEFUAL_SELECT_POSITION) return null;
        ViewHolder viewHolder;
        if (mViewHolders.size() <= i) {
            viewHolder = mStackAdapter.createView(this, mStackAdapter.getItemViewType(i));
            mViewHolders.add(viewHolder);
        } else {
            viewHolder = mViewHolders.get(i);
        }
        return viewHolder;
    }

    private void setClickAnimator(final ViewHolder holder, final int position) {
        holder.itemView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                doCardClickAnimation(holder, position);
            }
        });
    }

    private void doCardClickAnimation(final ViewHolder viewHolder, int position) {
        checkContentHeightByParent();
        mAnimatorAdapter.itemClick(viewHolder, position);
    }

    private void initOrResetVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        if ((action == MotionEvent.ACTION_MOVE) && (mIsBeingDragged)) {
            return true;
        }
        if (getViewScrollY() == 0 && !canScrollVertically(1)) {
            return false;
        }

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_MOVE: {
                final int activePointerId = mActivePointerId;
                if (activePointerId == INVALID_POINTER) {
                    break;
                }

                final int pointerIndex = ev.findPointerIndex(activePointerId);
                if (pointerIndex == -1) {
                    Log.e(TAG, "Invalid pointerId=" + activePointerId
                            + " in onInterceptTouchEvent");
                    break;
                }

                final int y = (int) ev.getY(pointerIndex);
                final int yDiff = Math.abs(y - mLastMotionY);
                if (yDiff > mTouchSlop) {
                    mIsBeingDragged = true;
                    mLastMotionY = y;
                    initVelocityTrackerIfNotExists();
                    mVelocityTracker.addMovement(ev);
                    mNestedYOffset = 0;
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }
                break;
            }

            case MotionEvent.ACTION_DOWN: {
                final int y = (int) ev.getY();
                mLastMotionY = y;
                mActivePointerId = ev.getPointerId(0);
                initOrResetVelocityTracker();
                mVelocityTracker.addMovement(ev);
                mIsBeingDragged = !mScroller.isFinished();
                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                recycleVelocityTracker();
                if (mScroller.springBack(getViewScrollX(), getViewScrollY(), 0, 0, 0, getScrollRange())) {
                    postInvalidate();
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
        }
        return mIsBeingDragged;
    }

    @Override
    public int getViewScrollX() {
        return getScrollX();
    }

    @Override
    public void scrollViewTo(int x, int y) {
        scrollTo(x, y);
    }

    @Override
    public int getViewScrollY() {
        return getScrollY();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!mScrollEnable) return true;

        initVelocityTrackerIfNotExists();

        MotionEvent vtev = MotionEvent.obtain(ev);

        final int actionMasked = ev.getActionMasked();

        if (actionMasked == MotionEvent.ACTION_DOWN) {
            mNestedYOffset = 0;
        }
        vtev.offsetLocation(0, mNestedYOffset);

        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN: {
                if (getChildCount() == 0) {
                    return false;
                }
                if ((mIsBeingDragged = !mScroller.isFinished())) {
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }

                // Remember where the motion event started
                mLastMotionY = (int) ev.getY();
                mActivePointerId = ev.getPointerId(0);
                break;
            }
            case MotionEvent.ACTION_MOVE:
                final int activePointerIndex = ev.findPointerIndex(mActivePointerId);
                if (activePointerIndex == -1) {
                    Log.e(TAG, "Invalid pointerId=" + mActivePointerId + " in onTouchEvent");
                    break;
                }

                final int y = (int) ev.getY(activePointerIndex);
                int deltaY = mLastMotionY - y;
                if (!mIsBeingDragged && Math.abs(deltaY) > mTouchSlop) {
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                    mIsBeingDragged = true;
                    if (deltaY > 0) {
                        deltaY -= mTouchSlop;
                    } else {
                        deltaY += mTouchSlop;
                    }
                }
                if (mIsBeingDragged) {
                    // Scroll to follow the motion event
                    mLastMotionY = y - mScrollOffset[1];

                    final int range = getScrollRange();

                    // Calling overScrollBy will call onOverScrolled, which
                    // calls onScrollChanged if applicable.
                    if (mScrollDelegate instanceof StackScrollDelegateImpl) {
                        mScrollDelegate.scrollViewTo(0, deltaY + mScrollDelegate.getViewScrollY());
                    } else {
                        if (overScrollBy(0, deltaY, 0, getViewScrollY(),
                                0, range, 0, mOverscrollDistance, true)) {
                            // Break our velocity if we hit a scroll barrier.
                            mVelocityTracker.clear();
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mIsBeingDragged) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int initialVelocity = (int) velocityTracker.getYVelocity(mActivePointerId);
                    if (getChildCount() > 0) {
                        if ((Math.abs(initialVelocity) > mMinimumVelocity)) {
                            fling(-initialVelocity);
                        } else {
                            if (mScroller.springBack(getViewScrollX(), mScrollDelegate.getViewScrollY(), 0, 0, 0,
                                    getScrollRange())) {
                                postInvalidate();
                            }
                        }
                        mActivePointerId = INVALID_POINTER;
                    }
                }
                endDrag();
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mIsBeingDragged && getChildCount() > 0) {
                    if (mScroller.springBack(getViewScrollX(), mScrollDelegate.getViewScrollY(), 0, 0, 0, getScrollRange())) {
                        postInvalidate();
                    }
                    mActivePointerId = INVALID_POINTER;
                }
                endDrag();
                break;
            case MotionEvent.ACTION_POINTER_DOWN: {
                final int index = ev.getActionIndex();
                mLastMotionY = (int) ev.getY(index);
                mActivePointerId = ev.getPointerId(index);
                break;
            }
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                mLastMotionY = (int) ev.getY(ev.findPointerIndex(mActivePointerId));
                break;
        }

        if (mVelocityTracker != null) {
            mVelocityTracker.addMovement(vtev);
        }
        vtev.recycle();
        return true;
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >>
                MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mLastMotionY = (int) ev.getY(newPointerIndex);
            mActivePointerId = ev.getPointerId(newPointerIndex);
            if (mVelocityTracker != null) {
                mVelocityTracker.clear();
            }
        }
    }

    private int getScrollRange() {
        int scrollRange = 0;
        if (getChildCount() > 0) {
            scrollRange = Math.max(0,
                    mTotalLength - mShowHeight);
        }
        return scrollRange;
    }

    @Override
    protected int computeVerticalScrollRange() {
        final int count = getChildCount();
        final int contentHeight = mShowHeight;
        if (count == 0) {
            return contentHeight;
        }

        int scrollRange = mTotalLength;
        final int scrollY = mScrollDelegate.getViewScrollY();
        final int overscrollBottom = Math.max(0, scrollRange - contentHeight);
        if (scrollY < 0) {
            scrollRange -= scrollY;
        } else if (scrollY > overscrollBottom) {
            scrollRange += scrollY - overscrollBottom;
        }

        return scrollRange;
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY,
                                  boolean clampedX, boolean clampedY) {
        if (!mScroller.isFinished()) {
            final int oldX = mScrollDelegate.getViewScrollX();
            final int oldY = mScrollDelegate.getViewScrollY();
            setScrollX(scrollX);
            setScrollY(scrollY);
            onScrollChanged(mScrollDelegate.getViewScrollX(), getViewScrollY(), oldX, oldY);
            if (clampedY) {
                mScroller.springBack(mScrollDelegate.getViewScrollX(), mScrollDelegate.getViewScrollY(), 0, 0, 0, getScrollRange());
            }
        } else {
            super.scrollTo(scrollX, scrollY);
        }
    }

    @Override
    protected int computeVerticalScrollOffset() {
        return Math.max(0, super.computeVerticalScrollOffset());
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            /*if (mStackScroller != null) {
                mStackScroller.scrollTo(0, mScroller.getCurrY());
                postInvalidate();
            } else {
                scrollTo(0, mScroller.getCurrY());
                postInvalidate();
            }*/
            mScrollDelegate.scrollViewTo(0, mScroller.getCurrY());
            postInvalidate();
        }
    }

    public void fling(int velocityY) {
        if (getChildCount() > 0) {
            int height = mShowHeight;
            int bottom = mTotalLength;
            mScroller.fling(mScrollDelegate.getViewScrollX(), mScrollDelegate.getViewScrollY(), 0, velocityY, 0, 0, 0,
                    Math.max(0, bottom - height), 0, 0);
            postInvalidate();
        }
    }

    @Override
    public void scrollTo(int x, int y) {
        if (getChildCount() > 0) {
            x = clamp(x, getWidth() - getPaddingRight() - getPaddingLeft(), getWidth());
            y = clamp(y, mShowHeight, mTotalLength);
            if (x != mScrollDelegate.getViewScrollX() || y != mScrollDelegate.getViewScrollY()) {
                super.scrollTo(x, y);
            }
        }
    }

    private void endDrag() {
        mIsBeingDragged = false;
        recycleVelocityTracker();
    }

    private static int clamp(int n, int my, int child) {
        if (my >= child || n < 0) {
            return 0;
        }
        if ((my + n) > child) {
            return child - my;
        }
        return n;
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    public static class LayoutParams extends MarginLayoutParams {

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    public static abstract class Adapter<VH extends ViewHolder> {
        private final AdapterDataObservable mObservable = new AdapterDataObservable();

        VH createView(ViewGroup parent, int viewType) {
            VH holder = onCreateView(parent, viewType);
            return holder;
        }

        protected abstract VH onCreateView(ViewGroup parent, int viewType);

        public void bindViewHolder(VH holder, int position) {
            onBindViewHolder(holder, position);
        }

        protected abstract void onBindViewHolder(VH holder, int position);

        public abstract int getItemCount();

        public int getItemViewType(int position) {
            return 0;
        }

        public final void notifyDataSetChanged() {
            mObservable.notifyChanged();
        }

        public void registerObserver(AdapterDataObserver observer) {
            mObservable.registerObserver(observer);
        }
    }

    public static class AdapterDataObservable extends Observable<AdapterDataObserver> {
        public boolean hasObservers() {
            return !mObservers.isEmpty();
        }

        public void notifyChanged() {
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onChanged();
            }
        }
    }

    public static abstract class AdapterDataObserver {
        public void onChanged() {
        }
    }

    private class ViewDataObserver extends AdapterDataObserver {
        @Override
        public void onChanged() {
            refreshView();
        }
    }

    public int getSelectPosition() {
        return mSelectPosition;
    }

    public void setSelectPosition(int selectPosition) {
        mSelectPosition = selectPosition;
    }

    public int getOverlapeGaps() {
        return mOverlapeGaps;
    }

    public int getOverlapeGapsCollapse() {
        return mOverlapeGapsCollapse;
    }

    public boolean isScrollEnable() {
        return mScrollEnable;
    }

    public void setScrollEnable(boolean scrollEnable) {
        mScrollEnable = scrollEnable;
    }

    public int getCardNormalHeight() {
        return mCardNormalHeight;
    }

    public int getShowHeight() {
        return mShowHeight;
    }

    public ScrollDelegate getScrollDelegate() {
        return mScrollDelegate;
    }
}
