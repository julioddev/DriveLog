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
            return new MapsFragment();
        } else if (position == 1) {
            return new TrackHistoryFragment();
        } else if (position == 2) {
            return new KmRegisterFragment();
        } else {
            return new KmHistoryFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
