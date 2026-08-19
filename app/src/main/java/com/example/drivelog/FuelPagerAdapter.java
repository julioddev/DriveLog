package com.example.drivelog;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class FuelPagerAdapter extends FragmentStateAdapter {

    public FuelPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new FuelRegisterFragment();
        } else if (position == 1) {
            return new FuelHistoryFragment();
        } else {
            return new FuelCalculatorFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}