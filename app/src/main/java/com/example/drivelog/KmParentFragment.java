package com.example.drivelog;

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

public class KmParentFragment extends Fragment {

    private ViewPager2 viewPager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_km_parent, container, false);

        TabLayout tabLayout = view.findViewById(R.id.tabLayoutKm);
        viewPager = view.findViewById(R.id.viewPagerKm);

        // 🔥 Correção: Ajusta o recuo superior para não ficar embaixo da barra de notificações
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        KmPagerAdapter adapter = new KmPagerAdapter(this);
        viewPager.setAdapter(adapter);
        // Desativa swipe nas sub-abas de KM e mantém cache total
        viewPager.setUserInputEnabled(false);
        viewPager.setOffscreenPageLimit(3);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Rastreamento");
            } else if (position == 1) {
                tab.setText("Gravações/Rotas");
            } else if (position == 2) {
                tab.setText("Registrar");
            } else {
                tab.setText("Histórico");
            }
        }).attach();

        View btnBack = view.findViewById(R.id.btnBackToMap);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).returnToMainMap();
                }
            });
        }

        return view;
    }

    public void switchToRegisterAndEdit(DailyKm dailyKm) {
        viewPager.setCurrentItem(2); // KmRegisterFragment is at index 2
        for (Fragment fragment : getChildFragmentManager().getFragments()) {
            if (fragment instanceof KmRegisterFragment) {
                ((KmRegisterFragment) fragment).startEdit(dailyKm);
                break;
            }
        }
    }

    public void switchToTracking() {
        if (viewPager != null) {
            viewPager.setCurrentItem(0, false);
        }
    }

    public void switchToHistory() {
        if (viewPager != null) {
            viewPager.setCurrentItem(1, true);
        }
    }
}