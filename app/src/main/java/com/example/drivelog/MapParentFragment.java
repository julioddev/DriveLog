package com.example.drivelog;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MapParentFragment extends Fragment {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private TabLayoutMediator mediator;
    private SharedPreferences sharedPreferences;

    private final SharedPreferences.OnSharedPreferenceChangeListener prefListener = (prefs, key) -> {
        if ("app_mode".equals(key)) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(this::refreshModeUI);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map_parent, container, false);

        tabLayout = view.findViewById(R.id.tabLayoutMap);
        viewPager = view.findViewById(R.id.viewPagerMap);

        // 🔥 Correção: Ajusta o recuo superior do TabLayout quando os menus do app estão ocultos
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            
            boolean isImmersiveActive = false;
            if (getActivity() instanceof MainActivity) {
                isImmersiveActive = !((MainActivity) getActivity()).isSystemUIVisible();
            }

            // Se estiver no modo imersivo (Toolbar oculta), o TabLayout sobe para o topo.
            // Precisamos adicionar o padding da barra de notificações para não cobrir os títulos.
            if (tabLayout != null) {
                int topPadding = isImmersiveActive ? systemBars.top : 0;
                tabLayout.setPadding(tabLayout.getPaddingLeft(), topPadding, tabLayout.getPaddingRight(), tabLayout.getPaddingBottom());
            }

            return insets;
        });

        sharedPreferences = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        sharedPreferences.registerOnSharedPreferenceChangeListener(prefListener);

        refreshModeUI();

        return view;
    }

    private void refreshModeUI() {
        if (viewPager == null || tabLayout == null || getContext() == null) return;

        SharedPreferences prefs = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        int subType = prefs.getInt("sub_type", 2); 
        int appMode = prefs.getInt("app_mode", 0); 
        if (subType == 0) {
            long installDate = prefs.getLong("install_date", System.currentTimeMillis());
            if (System.currentTimeMillis() - installDate > (7L * 24 * 60 * 60 * 1000)) appMode = 1;
        }
        boolean isMapsOnly = appMode == 1;
        boolean showDevTab = prefs.getBoolean("tab_route_detection_enabled", true);

        MapPagerAdapter adapter = new MapPagerAdapter(this, isMapsOnly, showDevTab);
        viewPager.setAdapter(adapter);
        viewPager.setUserInputEnabled(false);
        viewPager.setOffscreenPageLimit(2);
        
        tabLayout.setVisibility(isMapsOnly ? View.GONE : View.VISIBLE);

        if (mediator != null) mediator.detach();

        if (!isMapsOnly) {
            mediator = new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
                if (position == 0) tab.setText("Mapa");
                else if (position == 1) tab.setText("Rotas Criadas");
                else if (position == 2) tab.setText("Endereços Corrigidos");
                else tab.setText("Detecção de Rotas");
            });
            mediator.attach();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (sharedPreferences != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(prefListener);
        }
    }

    public void switchToMap() {
        if (viewPager != null) {
            viewPager.setCurrentItem(0, true);
        }
    }
}
