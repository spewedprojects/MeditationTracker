package com.gratus.meditationtrakcer.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.gratus.meditationtrakcer.SummaryFragment;

public class SummaryPagerAdapter extends FragmentStateAdapter {

    public SummaryPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Use modulo to map the huge position back to 0, 1, or 2
        // 0 = Week, 1 = Month, 2 = Year
        // The Modulo operator (%) is the magic here.
        // Even if position is 5,000,000...
        // 5,000,000 % 3 = 2.
        // It simply loads the "Year" fragment.
        return SummaryFragment.newInstance(position % 3);
    }

    @Override
    public int getItemCount() {
        // Return a massive number to simulate infinite scrolling
        return Integer.MAX_VALUE;
    }
}