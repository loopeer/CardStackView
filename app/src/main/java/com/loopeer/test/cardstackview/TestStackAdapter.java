package com.loopeer.test.cardstackview;

import android.content.Context;
import android.graphics.PorterDuff;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;

import com.loopeer.cardstack.StackAdapter;
import com.loopeer.cardstack.ViewHolder;

public class TestStackAdapter extends StackAdapter<Integer> {

    public TestStackAdapter(Context context) {
        super(context);
    }

    @Override
    public void bindView(Integer data, int position, ViewHolder holder) {
        ColorItemViewHolder h = (ColorItemViewHolder) holder;
        h.onBind(data);
    }

    @Override
    protected ViewHolder onCreateView(ViewGroup parent, int viewType) {
        View view = getLayoutInflater().inflate(R.layout.list_card_item, parent, false);
        return new ColorItemViewHolder(view);
    }

    static class ColorItemViewHolder extends ViewHolder {
        View mLayout;
        View mContainerContent;

        public ColorItemViewHolder(View view) {
            super(view);
            mLayout = view.findViewById(R.id.frame_list_card_item);
            mContainerContent = view.findViewById(R.id.container_list_content);
        }

        @Override
        public void onItemExpand(boolean b) {
            mContainerContent.setVisibility(b ? View.VISIBLE : View.GONE);
        }

        @Override
        public View getContentView() {
            return mContainerContent;
        }

        public void onBind(Integer integer) {
            mLayout.getBackground().setColorFilter(ContextCompat.getColor(getContext(), integer), PorterDuff.Mode.SRC_IN);
        }

    }

}
