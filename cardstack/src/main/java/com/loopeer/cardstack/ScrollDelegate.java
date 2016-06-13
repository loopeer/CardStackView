package com.loopeer.cardstack;

public interface ScrollDelegate {

    void scrollViewTo(int x, int y);
    int getViewScrollY();
    int getViewScrollX();

}
