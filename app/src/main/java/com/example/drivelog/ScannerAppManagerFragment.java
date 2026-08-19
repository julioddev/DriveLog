package com.example.drivelog;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ScannerAppManagerFragment extends Fragment {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scanner_app_management, container, false);
        tabLayout = view.findViewById(R.id.tabLayoutScannerApps);
        viewPager = view.findViewById(R.id.viewPagerScannerApps);

        viewPager.setAdapter(new ScannerAppPagerAdapter(this));
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) tab.setText(R.string.tab_detected);
            else if (position == 1) tab.setText(R.string.tab_whitelist);
            else tab.setText(R.string.tab_blacklist);
        }).attach();

        return view;
    }

    private static class ScannerAppPagerAdapter extends FragmentStateAdapter {
        ScannerAppPagerAdapter(Fragment f) { super(f); }
        @NonNull @Override public Fragment createFragment(int position) {
            return ScannerAppListFragment.newInstance(position);
        }
        @Override public int getItemCount() { return 3; }
    }

    public static class ScannerAppListFragment extends Fragment {
        private int type; // 0: Detected, 1: Whitelist, 2: Blacklist
        private RecyclerView recyclerView;
        private AppAdapter adapter;
        private SharedPreferences prefs;

        public static ScannerAppListFragment newInstance(int type) {
            ScannerAppListFragment f = new ScannerAppListFragment();
            Bundle b = new Bundle(); b.putInt("type", type);
            f.setArguments(b);
            return f;
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.layout_scanner_app_list, container, false);
            // 🔥 Garante fundo opaco para cada lista interna
            if (getContext() != null) {
                android.util.TypedValue tv = new android.util.TypedValue();
                if (getContext().getTheme().resolveAttribute(android.R.attr.windowBackground, tv, true)) {
                    v.setBackgroundColor(tv.data);
                } else {
                    v.setBackgroundResource(android.R.color.background_light);
                }
            }
            
            type = getArguments() != null ? getArguments().getInt("type") : 0;
            recyclerView = v.findViewById(R.id.recyclerScannerApps);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            prefs = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
            
            loadApps();
            return v;
        }

        private void loadApps() {
            if (getContext() == null || prefs == null) return;
            
            Set<String> detected = prefs.getStringSet("scanner_detected_apps", new HashSet<>());
            Set<String> whitelist = prefs.getStringSet("scanner_whitelist_apps", new HashSet<>());
            Set<String> blacklist = prefs.getStringSet("scanner_blacklist_apps", new HashSet<>());

            List<String> list = new ArrayList<>();
            if (type == 0) {
                // Detected: apps in 'detected' that are NOT in white or black
                for (String pkg : detected) {
                    if (!whitelist.contains(pkg) && !blacklist.contains(pkg)) list.add(pkg);
                }
            } else if (type == 1) {
                list.addAll(whitelist);
            } else {
                list.addAll(blacklist);
            }

            adapter = new AppAdapter(list, type, pkg -> {
                moveTo(pkg);
                loadApps();
            });
            recyclerView.setAdapter(adapter);
        }

        private void moveTo(String pkg) {
            if (getContext() == null || prefs == null) return;
            
            Set<String> whitelist = new HashSet<>(prefs.getStringSet("scanner_whitelist_apps", new HashSet<>()));
            Set<String> blacklist = new HashSet<>(prefs.getStringSet("scanner_blacklist_apps", new HashSet<>()));

            if (type == 1 || type == 2) {
                // Remove from lists (return to detected)
                whitelist.remove(pkg);
                blacklist.remove(pkg);
            }
            
            prefs.edit()
                .putStringSet("scanner_whitelist_apps", whitelist)
                .putStringSet("scanner_blacklist_apps", blacklist)
                .apply();
            
            Toast.makeText(getContext(), R.string.list_updated, Toast.LENGTH_SHORT).show();
        }

        private class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {
            private List<String> items;
            private int listType;
            private java.util.function.Consumer<String> onAction;

            AppAdapter(List<String> items, int type, java.util.function.Consumer<String> onAction) {
                this.items = items;
                this.listType = type;
                this.onAction = onAction;
            }

            @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
                return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_scanner_app, p, false));
            }

            @Override
            public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
                String pkg = items.get(pos);
                h.pkgName.setText(pkg);
                
                PackageManager pm = requireContext().getPackageManager();
                try {
                    ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                    h.appName.setText(pm.getApplicationLabel(ai));
                } catch (Exception e) {
                    h.appName.setText(R.string.app_unknown);
                }

                if (listType == 0) { // Detected
                    h.btnWhite.setVisibility(View.VISIBLE);
                    h.btnBlack.setVisibility(View.VISIBLE);
                    h.btnWhite.setOnClickListener(v -> {
                        Set<String> set = new HashSet<>(prefs.getStringSet("scanner_whitelist_apps", new HashSet<>()));
                        set.add(pkg);
                        prefs.edit().putStringSet("scanner_whitelist_apps", set).apply();
                        onAction.accept(pkg);
                    });
                    h.btnBlack.setOnClickListener(v -> {
                        Set<String> set = new HashSet<>(prefs.getStringSet("scanner_blacklist_apps", new HashSet<>()));
                        set.add(pkg);
                        prefs.edit().putStringSet("scanner_blacklist_apps", set).apply();
                        onAction.accept(pkg);
                    });
                } else if (listType == 1) { // Whitelist
                    h.btnBlack.setVisibility(View.VISIBLE);
                    h.btnBlack.setText(R.string.btn_remove);
                    h.btnBlack.setOnClickListener(v -> onAction.accept(pkg));
                    h.btnWhite.setVisibility(View.GONE);
                } else { // Blacklist
                    h.btnWhite.setVisibility(View.VISIBLE);
                    h.btnWhite.setText(R.string.btn_remove);
                    h.btnWhite.setOnClickListener(v -> onAction.accept(pkg));
                    h.btnBlack.setVisibility(View.GONE);
                }
            }

            @Override public int getItemCount() { return items.size(); }

            class ViewHolder extends RecyclerView.ViewHolder {
                TextView appName, pkgName;
                MaterialButton btnWhite, btnBlack;
                ViewHolder(View v) {
                    super(v);
                    appName = v.findViewById(R.id.textAppName);
                    pkgName = v.findViewById(R.id.textPackageName);
                    btnWhite = v.findViewById(R.id.btnMoveToWhitelist);
                    btnBlack = v.findViewById(R.id.btnMoveToBlacklist);
                }
            }
        }
    }
}
