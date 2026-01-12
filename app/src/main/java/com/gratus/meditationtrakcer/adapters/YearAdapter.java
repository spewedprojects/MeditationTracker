package com.gratus.meditationtrakcer.adapters;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.gratus.meditationtrakcer.R;
import java.util.List;

public class YearAdapter extends RecyclerView.Adapter<YearAdapter.YearVH> {

    private final List<Integer> years;
    // We remove the click listener from constructor because selection happens on Scroll/Snap

    public YearAdapter(List<Integer> years) {
        this.years = years;
    }

    @NonNull
    @Override
    public YearVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TextView tv = new TextView(parent.getContext());
        tv.setTextColor(ContextCompat.getColor(parent.getContext(), R.color.inverseprimary)); // Use ?attr/colorOnSurface if feasible
        tv.setAlpha(1.0f);
        tv.setGravity(Gravity.CENTER);

        // Font setup
        if (!parent.isInEditMode()) {
            Typeface tf = ResourcesCompat.getFont(parent.getContext(), R.font.atkinsonhyperlegiblenext_medium);
            tv.setTypeface(tf);
        }

        // Determine orientation
        RecyclerView rv = (RecyclerView) parent;
        boolean isHorizontal = rv.getLayoutManager().canScrollHorizontally();

        // 1. Get dimensions, but provide a SAFE FALLBACK if 0
        int parentWidth = parent.getWidth();
        int parentHeight = parent.getHeight();

        // FALLBACK: If parent is too small (imploded), use the screen width/height to reset it
        int minWidth = parent.getResources().getDisplayMetrics().widthPixels / 2; // Arbitrary safety floor
        if (isHorizontal && parentWidth < minWidth) {
            parentWidth = parent.getResources().getDisplayMetrics().widthPixels;
        }

        // Same for vertical height
        int minHeight = 200; // px
        if (!isHorizontal && parentHeight < minHeight) {
            // Use the fixed card height you defined in XML (240dp converted to approx px)
            parentHeight = (int) (240 * parent.getResources().getDisplayMetrics().density);
        }

        // 2. Calculate Item Size (1/3 of the parent)
        int itemSize;
        if (isHorizontal) {
            itemSize = parentWidth / 3;
            // Horizontal: Width is dynamic, Height is fixed/wrap
            tv.setLayoutParams(new ViewGroup.LayoutParams(itemSize, ViewGroup.LayoutParams.MATCH_PARENT));
            tv.setPadding(0, 0, 0, 0);
            tv.setTextSize(33);
        } else {
            itemSize = parentHeight / 3;
            // Vertical: Width is match parent, Height is dynamic
            tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, itemSize));
            tv.setTextSize(50);
        }

        return new YearVH(tv);
    }

    @Override
    public void onBindViewHolder(@NonNull YearVH holder, int position) {
        holder.textView.setText(String.valueOf(years.get(position)));
    }

    @Override
    public int getItemCount() {
        return years.size();
    }

    // Helper to get year by position for the SnapHelper
    public int getYearAt(int position) {
        if (position >= 0 && position < years.size()) {
            return years.get(position);
        }
        return -1;
    }

    static class YearVH extends RecyclerView.ViewHolder {
        TextView textView;
        YearVH(@NonNull View itemView) {
            super(itemView);
            textView = (TextView) itemView;
        }
    }
}