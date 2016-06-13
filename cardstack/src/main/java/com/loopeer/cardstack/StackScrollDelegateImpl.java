package com.loopeer.cardstack;

import android.view.View;

public class StackScrollDelegateImpl implements ScrollDelegate{

    private CardStackView mCardStackView;
    private int mScrollY;
    private int mScrollX;

    public StackScrollDelegateImpl(CardStackView cardStackView) {
        mCardStackView = cardStackView;
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

    @Override
    public void scrollViewTo(int x, int y) {
        mScrollY = y;
        mScrollX = x;
        updateChildPos();
    }

    @Override
    public int getViewScrollY() {
        return mScrollY;
    }

    @Override
    public int getViewScrollX() {
        return mScrollX;
    }
}
