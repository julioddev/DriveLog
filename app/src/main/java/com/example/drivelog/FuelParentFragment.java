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

public class FuelParentFragment extends Fragment {

    private ViewPager2 viewPager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fuel_parent, container, false);

        TabLayout tabLayout = view.findViewById(R.id.tabLayoutFuel);
        viewPager = view.findViewById(R.id.viewPagerFuel);

        // 🔥 Correção: Ajusta o recuo superior para não ficar embaixo da barra de notificações
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        FuelPagerAdapter adapter = new FuelPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Registrar");
            } else if (position == 1) {
                tab.setText("Histórico");
            } else {
                tab.setText("Calculadora");
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

    public void switchToRegisterAndEdit(Fuel fuel) {
        viewPager.setCurrentItem(0);
        // Find the register fragment and start edit
        // Since fragments are managed by ViewPager, we can use the tag if we know it or pass data
        // For simplicity with FragmentStateAdapter, we can use a SharedViewModel or just find it.
        // Let's try finding the fragment in child manager.
        for (Fragment fragment : getChildFragmentManager().getFragments()) {
            if (fragment instanceof FuelRegisterFragment) {
                ((FuelRegisterFragment) fragment).startEdit(fuel);
                break;
            }
        }
    }
}