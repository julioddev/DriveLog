package com.example.drivelog;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;

public class SettingsParentFragment extends Fragment {

    private ListenerRegistration remoteMenuListener;

    public static SettingsParentFragment newInstance(int startTab) {
        SettingsParentFragment fragment = new SettingsParentFragment();
        Bundle args = new Bundle();
        args.putInt("start_tab", startTab);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof androidx.appcompat.app.AppCompatActivity) {
            androidx.appcompat.app.ActionBar actionBar = ((androidx.appcompat.app.AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) actionBar.hide();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof androidx.appcompat.app.AppCompatActivity) {
            androidx.appcompat.app.ActionBar actionBar = ((androidx.appcompat.app.AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) actionBar.show();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings_parent, container, false);

        TabLayout tabLayout = view.findViewById(R.id.tabLayoutSettings);
        androidx.viewpager2.widget.ViewPager2 viewPager = view.findViewById(R.id.viewPagerSettings);
        androidx.appcompat.widget.Toolbar toolbar = view.findViewById(R.id.toolbarSettings);

        // 🔥 Ajuste de altura e padding para não ficar atrás da barra de notificações e não cortar a seta
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            androidx.core.graphics.Insets sb = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            int actionBarSize = 0;
            android.util.TypedValue tv = new android.util.TypedValue();
            if (requireContext().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
                actionBarSize = android.util.TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
            }
            v.getLayoutParams().height = sb.top + actionBarSize;
            v.setPadding(0, sb.top, 0, 0);
            return insets;
        });

        toolbar.setNavigationIcon(R.drawable.ic_back_white);
        toolbar.setNavigationOnClickListener(v -> {
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        });

        SettingsPagerAdapter adapter = new SettingsPagerAdapter(this);
        viewPager.setUserInputEnabled(false);
        viewPager.setAdapter(adapter);

        // Listener Remoto para Sub-abas de Ajustes
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            android.content.SharedPreferences prefs = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
            int subType = prefs.getInt("sub_type", 0);
            
            remoteMenuListener = FirebaseHelper.listenRemoteMenus(subType, allowedIds -> {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        adapter.updateTabs(allowedIds);
                        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
                            tab.setText(adapter.getTabTitle(position));
                        }).attach();
                        
                        // Seleção inicial se houver
                        if (getArguments() != null && getArguments().containsKey("start_tab")) {
                            int startIdx = getArguments().getInt("start_tab");
                            int dynamicIdx = adapter.getDynamicIndexForFixed(startIdx);
                            viewPager.post(() -> viewPager.setCurrentItem(dynamicIdx, false));
                        }
                    });
                }
            });
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (remoteMenuListener != null) remoteMenuListener.remove();
    }

    private static class SettingsPagerAdapter extends FragmentStateAdapter {
        private final List<TabInfo> activeTabs = new ArrayList<>();
        private final List<TabInfo> allTabs = new ArrayList<>();

        SettingsPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
            allTabs.add(new TabInfo("settings_general", "Geral", 0));
            allTabs.add(new TabInfo("settings_map", "Mapa", 1));
            allTabs.add(new TabInfo("settings_tracking", "Rastreamento", 2));
            allTabs.add(new TabInfo("settings_platforms", "Plataformas", 3));
            allTabs.add(new TabInfo("settings_features", "Recursos", 4));
            allTabs.add(new TabInfo("settings_dev", "DEV", 5));
            allTabs.add(new TabInfo("settings_menu", "MENU DEV", 6));
            allTabs.add(new TabInfo("settings_emails", "EMAILS DEV", 7));
            allTabs.add(new TabInfo("settings_users", "DEV USERS", 8));
            activeTabs.addAll(allTabs); // Padrão
        }

        void updateTabs(List<String> allowedIds) {
            activeTabs.clear();
            
            // Garantir que abas básicas sempre apareçam se o allowedIds vier vazio ou incompleto
            // Abas de 0 a 4 são as básicas (Geral, Mapa, Rastreamento, Plataformas, Recursos)
            for (TabInfo tab : allTabs) {
                if (tab.fixedIndex <= 4) {
                    activeTabs.add(tab);
                } else if (allowedIds != null && allowedIds.contains(tab.id)) {
                    if (!activeTabs.contains(tab)) activeTabs.add(tab);
                }
            }
            notifyDataSetChanged();
        }

        String getTabTitle(int position) {
            if (position < 0 || position >= activeTabs.size()) return "";
            return activeTabs.get(position).title;
        }

        int getDynamicIndexForFixed(int fixedIdx) {
            for (int i = 0; i < activeTabs.size(); i++) {
                if (activeTabs.get(i).fixedIndex == fixedIdx) return i;
            }
            return 0;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            int fixedIdx = activeTabs.get(position).fixedIndex;
            switch (fixedIdx) {
                case 0: return new SettingsFragment();
                case 1: return new MapSettingsFragment();
                case 2: return new TrackingSettingsFragment();
                case 3: return new PlatformSettingsFragment();
                case 4: return new FeaturesSettingsFragment();
                case 5: return new DevDetectionSettingsFragment(); // O que era antes o "DEV" (Scanner/Ajustes)
                case 6: return new DevMenuControlFragment();     // O novo "DEV MENU"
                case 7: return new DevEmailControlFragment();    // O novo "DEV EMAIL"
                case 8: return new DevUserControlFragment();     // O novo "DEV USERS"
                default: return new SettingsFragment();
            }
        }

        @Override
        public int getItemCount() { return activeTabs.size(); }
        
        @Override
        public long getItemId(int position) { return activeTabs.get(position).id.hashCode(); }
        
        @Override
        public boolean containsItem(long itemId) {
            for (TabInfo t : activeTabs) if (t.id.hashCode() == itemId) return true;
            return false;
        }

        private static class TabInfo {
            String id, title;
            int fixedIndex;
            TabInfo(String i, String t, int fi) { id = i; title = t; fixedIndex = fi; }
        }
    }
}
