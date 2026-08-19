package com.example.drivelog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class DevRouteContainerFragment extends Fragment {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dev_route_container, container, false);
        tabLayout = view.findViewById(R.id.tabLayoutDev);
        viewPager = view.findViewById(R.id.viewPagerDev);

        viewPager.setAdapter(new DevPagerAdapter(this));
        
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText("Scanner"); break;
                case 1: tab.setText("Ajustes"); break;
                case 2: tab.setText("EMAILS DEV"); break;
            }
        }).attach();

        return view;
    }

    private static class DevPagerAdapter extends FragmentStateAdapter {
        DevPagerAdapter(Fragment f) { super(f); }
        @NonNull @Override public Fragment createFragment(int position) {
            switch (position) {
                case 0: return new DevRouteDetectionFragment();
                case 1: return new DevDetectionSettingsFragment();
                default: return new DevEmailControlFragment();
            }
        }
        @Override public int getItemCount() { return 3; }
    }
}
