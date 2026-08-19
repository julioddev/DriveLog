package com.example.drivelog;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class EarningsPagerAdapter extends FragmentStateAdapter {

    public EarningsPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new EarningsRegisterFragment();
        } else {
            return new EarningsHistoryFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}