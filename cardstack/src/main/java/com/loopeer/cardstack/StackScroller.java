package com.loopeer.cardstack;

import android.view.View;

public class StackScroller {

    private CardStackView mCardStackView;
    private int mScrollY;

    public StackScroller(CardStackView cardStackView) {
        mCardStackView = cardStackView;
    }

    public void scrollTo(int x, int y) {
        mScrollY = y;
        updateChildPos();
    }

    private void updateChildPos() {
        for (int i = 0; i < mCardStackView.getChildCount(); i++) {
            View view = mCardStackView.getChildAt(i);
            view.postInvalidate();
            if (view.getTop() - mScrollY < mCardStackView.getChildAt(0).getY()) {
                view.setTranslationY(mCardStackView.getChildAt(0).getY() - view.getTop());
            } else if (view.getTop() - mScrollY > view.getTop()) {
                view.setTranslationY(0);
            } else {
                view.setTranslationY(-mScrollY);
            }
        }
    }

    public int getScrollY() {
        return mScrollY;
    }
}
