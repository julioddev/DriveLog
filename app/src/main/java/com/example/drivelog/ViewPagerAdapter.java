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

    private boolean earningsEnabled, kmEnabled, fuelEnabled, maintenanceEnabled, mapsEnabled;
    private final List<Integer> activeIds = new ArrayList<>();
    private List<String> remoteAllowedIds = new ArrayList<>();

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        refreshEnabledTabs(fragmentActivity.getSharedPreferences("AppConfig", Context.MODE_PRIVATE));
    }

    public void setRemoteAllowedIds(List<String> remoteIds) {
        this.remoteAllowedIds = remoteIds;
        refreshEnabledTabs(null); // Re-refreshes with combined logic
    }

    public void refreshEnabledTabs(SharedPreferences prefs) {
        if (prefs != null) {
            this.mapsEnabled = getBoolSafe(prefs, "maps_enabled", true);
            this.earningsEnabled = getBoolSafe(prefs, "tab_earnings_enabled", true);
            this.kmEnabled = getBoolSafe(prefs, "tab_km_enabled", true);
            this.fuelEnabled = getBoolSafe(prefs, "tab_fuel_enabled", true);
            this.maintenanceEnabled = getBoolSafe(prefs, "tab_maintenance_enabled", true);
        }
        
        activeIds.clear();
        boolean useRemote = remoteAllowedIds != null && !remoteAllowedIds.isEmpty();

        if (mapsEnabled && (!useRemote || remoteAllowedIds.contains("maps"))) activeIds.add(R.id.nav_maps);
        if (earningsEnabled && (!useRemote || remoteAllowedIds.contains("earnings"))) activeIds.add(R.id.nav_earnings);
        if (kmEnabled && (!useRemote || remoteAllowedIds.contains("km"))) activeIds.add(R.id.nav_km);
        if (fuelEnabled && (!useRemote || remoteAllowedIds.contains("fuel"))) activeIds.add(R.id.nav_fuel);
        if (maintenanceEnabled && (!useRemote || remoteAllowedIds.contains("maintenance"))) activeIds.add(R.id.nav_maintenance);
        
        notifyDataSetChanged();
    }

    private boolean getBoolSafe(SharedPreferences prefs, String key, boolean def) {
        try {
            return prefs.getBoolean(key, def);
        } catch (Exception e) {
            Object val = prefs.getAll().get(key);
            if (val != null) {
                String s = String.valueOf(val);
                return s.equalsIgnoreCase("true") || s.equals("1") || s.equalsIgnoreCase("on");
            }
            return def;
        }
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        int id = activeIds.get(position);
        if (id == R.id.nav_maps) return new MapParentFragment();
        if (id == R.id.nav_earnings) return new EarningsParentFragment();
        if (id == R.id.nav_km) return new KmParentFragment();
        if (id == R.id.nav_fuel) return new FuelParentFragment();
        if (id == R.id.nav_maintenance) return new MaintenanceParentFragment();
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
        int pos = activeIds.indexOf(itemId);
        return pos != -1 ? pos : 0;
    }

    public int getIdForPosition(int position) {
        if (position >= 0 && position < activeIds.size()) {
            return activeIds.get(position);
        }
        return R.id.nav_maps;
    }
}
