package com.example.drivelog;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MapPagerAdapter extends FragmentStateAdapter {

    private final boolean isMapsOnly;
    private final boolean showDevTab;

    public MapPagerAdapter(@NonNull Fragment fragment, boolean isMapsOnly, boolean showDevTab) {
        super(fragment);
        this.isMapsOnly = isMapsOnly;
        this.showDevTab = showDevTab;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (isMapsOnly) {
            return new RouteFragment();
        } else {
            if (position == 0) return new RouteFragment();
            if (position == 1) return new RouteHistoryFragment();
            if (position == 2) return new CorrectedAddressesParentFragment();
            return new DevRouteContainerFragment();
        }
    }

    @Override
    public int getItemCount() {
        if (isMapsOnly) return 1;
        return showDevTab ? 4 : 3;
    }
}
