package com.gratus.meditationtrakcer.utils;

import android.content.Context;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class CenterZoomLayoutManager extends LinearLayoutManager {

    private final float mShrinkAmount = 0.6f; // Keep at 50% shrink
    private final float mShrinkDistance = 0.9f; // REDUCE this (was 0.9f) to make scaling happen faster/closer to center

    public CenterZoomLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int scrolled = super.scrollVerticallyBy(dy, recycler, state);
        if (getOrientation() == VERTICAL) scaleChildren();
        return scrolled;
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int scrolled = super.scrollHorizontallyBy(dx, recycler, state);
        if (getOrientation() == HORIZONTAL) scaleChildren();
        return scrolled;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        super.onLayoutChildren(recycler, state);
        scaleChildren();
    }

    private void scaleChildren() {
        float midpoint = (getOrientation() == HORIZONTAL) ? getWidth() / 2.f : getHeight() / 2.f;
        // The distance from center where shrinking begins
        float d1 = mShrinkDistance * midpoint;

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            float childMidpoint = (getOrientation() == HORIZONTAL)
                    ? (getDecoratedRight(child) + getDecoratedLeft(child)) / 2.f
                    : (getDecoratedBottom(child) + getDecoratedTop(child)) / 2.f;

            float d = Math.min(d1, Math.abs(midpoint - childMidpoint));

            // Calculate Scale
            float scale = 1.f - mShrinkAmount * d / d1;

            // KEY CHANGE: More dramatic Alpha fade
            // At center (d=0), alpha is 1.0. At edge (d=d1), alpha becomes 0.2.
            float alpha = 1.f - 0.8f * d / d1;

            child.setScaleX(scale);
            child.setScaleY(scale);
            child.setAlpha(alpha);
        }
    }
}