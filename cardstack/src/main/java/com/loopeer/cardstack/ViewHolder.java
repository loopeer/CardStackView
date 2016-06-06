package com.loopeer.cardstack;

import android.content.Context;
import android.view.View;

public abstract class ViewHolder {

    View itemView;
    int position;

    public ViewHolder(View view) {
        itemView = view;
    }

    public Context getContext() {
        return itemView.getContext();
    }

    public abstract void onItemExpand(boolean b);

    public abstract View getContentView();
}
