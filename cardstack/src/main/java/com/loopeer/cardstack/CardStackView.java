package com.loopeer.cardstack;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.database.Observable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.util.ArrayList;
import java.util.List;

public class CardStackView extends ViewGroup {

    private static final String TAG = "CardStackView";

    private static final int DEFUAL_SELECT_POSITION = -1;
    private static final int ANIMATION_DURATION = 300;

    private int mTotalLength;
    private int mOverlapeGaps;
    private int mCardNormalHeight;
    private final int EXPAND_TYPE = 0;
    private final int COLLAPSE_TYPE = 1;
    private StackAdapter mStackAdapter;
    private final ViewDataObserver mObserver = new ViewDataObserver();
    private int mSelectPosition = DEFUAL_SELECT_POSITION;
    private int mNormalChildHeight = Integer.MAX_VALUE;
    private int mShowHeight;
    private AnimatorSet mSet;
    private EnableScrollView mParentScrollView;
    private List<ViewHolder> mViewHolders;

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
        mCardNormalHeight = dp2px(160);
    }

    private int dp2px(int value) {
        final float scale = getContext().getResources().getDisplayMetrics().density;
        return (int) (value * scale + 0.5f);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mParentScrollView = (EnableScrollView) getParent();
        checkContentHeightByParent();
        measureExpand(widthMeasureSpec, heightMeasureSpec);
    }

    private void checkContentHeightByParent() {
        mShowHeight = mParentScrollView.getMeasuredHeight() - mParentScrollView.getPaddingTop() - mParentScrollView.getPaddingBottom();
    }

    private void measureExpand(int widthMeasureSpec, int heightMeasureSpec) {
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
        layoutExpand();
    }

    private void layoutExpand() {
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

    public void setAdapter(StackAdapter stackAdapter) {
        mStackAdapter = stackAdapter;
        mStackAdapter.registerObserver(mObserver);
        refreshView();
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

    private ViewHolder getViewHolder(int i) {
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
        if (mSelectPosition == position) {
            if (mSet != null && mSet.isRunning()) return;
            initAnimatorSet();
            viewHolder.getContentView().setVisibility(INVISIBLE);
            int childTop = getPaddingTop();
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                child.clearAnimation();
                final MarginLayoutParams lp =
                        (MarginLayoutParams) child.getLayoutParams();
                childTop += lp.topMargin;
                if (i != 0) {
                    childTop -= mOverlapeGaps * 2;
                    ObjectAnimator oAnim = ObjectAnimator.ofFloat(child, View.Y, child.getY(), childTop);
                    mSet.play(oAnim);
                } else {
                    ObjectAnimator oAnim = ObjectAnimator.ofFloat(child, View.Y, child.getY(), childTop);
                    mSet.play(oAnim);
                }
                childTop += mNormalChildHeight;
            }
            mSet.addListener(new AnimatorListenerAdapter() {

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    mSelectPosition = DEFUAL_SELECT_POSITION;
                    viewHolder.getContentView().setVisibility(GONE);
                    mParentScrollView.setScrollingEnabled(true);
                }
            });
            mSet.start();
        } else {
            final View itemView = viewHolder.itemView;
            if (mSet != null && mSet.isRunning()) return;
            initAnimatorSet();
            final int preSelectPosition = mSelectPosition;
            final ViewHolder preSelectViewHolder = getViewHolder(preSelectPosition);
            if (preSelectViewHolder != null) {
                preSelectViewHolder.getContentView().setVisibility(INVISIBLE);
            }
            mSelectPosition = position;
            itemView.clearAnimation();
            ObjectAnimator oa = ObjectAnimator.ofFloat(itemView, View.Y, itemView.getY(), mParentScrollView.getScrollY() + getPaddingTop());
            mSet.play(oa);
            int collapseShowItemCount = 0;
            for (int i = 0; i < getChildCount(); i++) {
                int childTop;
                if (i == mSelectPosition) continue;
                final View child = getChildAt(i);
                child.clearAnimation();
                if (collapseShowItemCount < 3) {
                    childTop = mShowHeight - (mOverlapeGaps * 3 - collapseShowItemCount * mOverlapeGaps) + mParentScrollView.getScrollY();
                    ObjectAnimator oAnim = ObjectAnimator.ofFloat(child, View.Y, child.getY() + mParentScrollView.getScrollY(), childTop);
                    mSet.play(oAnim);
                    collapseShowItemCount++;
                } else {
                    ObjectAnimator oAnim = ObjectAnimator.ofFloat(child, View.Y, child.getY() + mParentScrollView.getScrollY(), mShowHeight + mParentScrollView.getScrollY());
                    mSet.play(oAnim);
                }
            }
            mSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    if (preSelectViewHolder != null) {
                        preSelectViewHolder.getContentView().setVisibility(GONE);
                    }
                    viewHolder.onItemExpand(true);
                    mParentScrollView.setScrollingEnabled(false);
                }

            });
            mSet.start();
        }
        if (getChildCount() == 1)
            mSet.end();
    }

    private void initAnimatorSet() {
        mSet = new AnimatorSet();
        mSet.setInterpolator(new AccelerateDecelerateInterpolator());
        mSet.setDuration(ANIMATION_DURATION);
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

}
