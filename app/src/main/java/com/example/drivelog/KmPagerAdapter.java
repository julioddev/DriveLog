package com.example.drivelog;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class KmPagerAdapter extends FragmentStateAdapter {

    public KmPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new TrackHistoryFragment();
        } else {
            return new KmHistoryFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
