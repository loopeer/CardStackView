package com.loopeer.cardstack;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

public abstract class AnimatorAdapter {
    private static final int ANIMATION_DURATION = 400;

    protected CardStackView mCardStackView;
    protected AnimatorSet mSet;

    public AnimatorAdapter(CardStackView cardStackView) {
        mCardStackView = cardStackView;
    }

    protected void initAnimatorSet() {
        mSet = new AnimatorSet();
        mSet.setInterpolator(new AccelerateDecelerateInterpolator());
        mSet.setDuration(ANIMATION_DURATION);
    }

    public void itemClick(final ViewHolder viewHolder, int position) {
        if (mSet != null && mSet.isRunning()) return;
        initAnimatorSet();
        if (mCardStackView.getSelectPosition() == position) {
            onItemCollapse(viewHolder);
        } else {
            onItemExpand(viewHolder, position);
        }
        if (mCardStackView.getChildCount() == 1)
            mSet.end();
    }

    protected abstract void itemExpandAnimatorSet(ViewHolder viewHolder, int position);

    protected abstract void itemCollapseAnimatorSet(ViewHolder viewHolder);

    private void onItemExpand(final ViewHolder viewHolder, int position) {
        final int preSelectPosition = mCardStackView.getSelectPosition();
        final ViewHolder preSelectViewHolder = mCardStackView.getViewHolder(preSelectPosition);
        if (preSelectViewHolder != null) {
            preSelectViewHolder.getContentView().setVisibility(View.INVISIBLE);
        }
        mCardStackView.setSelectPosition(position);
        itemExpandAnimatorSet(viewHolder, position);
        mSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                mCardStackView.setScrollEnable(false);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (preSelectViewHolder != null) {
                    preSelectViewHolder.getContentView().setVisibility(View.GONE);
                }
                viewHolder.onItemExpand(true);
            }

        });
        mSet.start();
    }

    private void onItemCollapse(final ViewHolder viewHolder){
        viewHolder.getContentView().setVisibility(View.INVISIBLE);
        itemCollapseAnimatorSet(viewHolder);
        mSet.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                mCardStackView.setScrollEnable(true);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mCardStackView.setSelectPosition(CardStackView.DEFUAL_SELECT_POSITION);
                viewHolder.getContentView().setVisibility(View.GONE);
            }
        });
        mSet.start();
    }

    protected int getCollapseStartTop(int collapseShowItemCount) {
        return mCardStackView.getOverlapeGapsCollapse() * (3 - collapseShowItemCount - (3 - (mCardStackView.getChildCount() - mCardStackView.getSelectPosition() > 3 ? 3 : mCardStackView.getChildCount() - mCardStackView.getSelectPosition() - 1)));
    }
}
