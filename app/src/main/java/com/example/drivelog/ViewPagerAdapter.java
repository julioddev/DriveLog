package com.example.drivelog;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;
import java.util.List;

public class ViewPagerAdapter extends FragmentStateAdapter {

    private final List<Integer> activeIds = new ArrayList<>();

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        refreshEnabledTabs(null);
    }

    public void setRemoteAllowedIds(List<String> remoteIds) {
        // Ignora remotos na raiz, apenas o mapa é mostrado
    }

    public void refreshEnabledTabs(SharedPreferences prefs) {
        // 🔥 MODO MAPA É O ÚNICO NA RAIZ
        activeIds.clear();
        activeIds.add(R.id.nav_maps);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return new MapParentFragment();
    }

    @Override
    public int getItemCount() {
        return activeIds.size();
    }

    @Override
    public long getItemId(int position) {
        return activeIds.get(position);
    }

    @Override
    public boolean containsItem(long itemId) {
        return activeIds.contains((int) itemId);
    }

    public int getPositionForId(int itemId) {
        return 0;
    }

    public int getIdForPosition(int position) {
        return R.id.nav_maps;
    }
}
