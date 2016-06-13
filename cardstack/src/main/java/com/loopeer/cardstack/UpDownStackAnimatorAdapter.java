package com.loopeer.cardstack;

import android.animation.ObjectAnimator;
import android.view.View;
import android.view.ViewGroup;

public class UpDownStackAnimatorAdapter extends AnimatorAdapter {

    public UpDownStackAnimatorAdapter(CardStackView cardStackView) {
        super(cardStackView);
    }

    protected void itemExpandAnimatorSet(final ViewHolder viewHolder, int position) {
        final View itemView = viewHolder.itemView;
        itemView.clearAnimation();
        ObjectAnimator oa = ObjectAnimator.ofFloat(itemView, View.Y, itemView.getY(), mCardStackView.getChildAt(0).getY());
        mSet.play(oa);
        int collapseShowItemCount = 0;
        for (int i = 0; i < mCardStackView.getChildCount(); i++) {
            int childTop;
            if (i == mCardStackView.getSelectPosition()) continue;
            final View child = mCardStackView.getChildAt(i);
            child.clearAnimation();
            if (i > mCardStackView.getSelectPosition() && collapseShowItemCount < 3) {
                childTop = mCardStackView.getShowHeight() - getCollapseStartTop(collapseShowItemCount);
                ObjectAnimator oAnim = ObjectAnimator.ofFloat(child, View.Y, child.getY(), childTop);
                mSet.play(oAnim);
                collapseShowItemCount++;
            } else if (i < mCardStackView.getSelectPosition()) {
                ObjectAnimator oAnim = ObjectAnimator.ofFloat(child, View.Y, child.getY(), mCardStackView.getChildAt(0).getY());
                mSet.play(oAnim);
            } else {
                ObjectAnimator oAnim = ObjectAnimator.ofFloat(child, View.Y, child.getY(), mCardStackView.getShowHeight());
                mSet.play(oAnim);
            }
        }
    }

    @Override
    protected void itemCollapseAnimatorSet(ViewHolder viewHolder) {
        int childTop = mCardStackView.getPaddingTop();
        for (int i = 0; i < mCardStackView.getChildCount(); i++) {
            View child = mCardStackView.getChildAt(i);
            child.clearAnimation();
            final ViewGroup.MarginLayoutParams lp =
                    (ViewGroup.MarginLayoutParams) child.getLayoutParams();
            childTop += lp.topMargin;
            if (i != 0) {
                childTop -= mCardStackView.getOverlapeGaps() * 2;
            }
            ObjectAnimator oAnim = ObjectAnimator.ofFloat(child, View.Y, child.getY(),
                    childTop - mCardStackView.getViewScrollY() < mCardStackView.getChildAt(0).getY()
                            ? mCardStackView.getChildAt(0).getY() : childTop - mCardStackView.getViewScrollY());
            mSet.play(oAnim);
            childTop += mCardStackView.getCardNormalHeight();
        }
    }

}
