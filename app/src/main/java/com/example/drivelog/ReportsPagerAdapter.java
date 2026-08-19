package com.example.drivelog;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ReportsPagerAdapter extends FragmentStateAdapter {

    public ReportsPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new DailyReportFragment();
            case 1: return new WeeklyReportFragment();
            case 2: return new MonthlyReportFragment();
            case 3: return new AnnualReportFragment();
            default: return new AveragesFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 5;
    }
}