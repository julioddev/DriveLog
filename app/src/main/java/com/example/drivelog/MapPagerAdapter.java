package com.example.drivelog;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MapPagerAdapter extends FragmentStateAdapter {

    public MapPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // 🔥 MODO MAPA É O ÚNICO: RouteFragment é a tela principal
        return new RouteFragment();
    }

    @Override
    public int getItemCount() {
        return 1;
    }
}
