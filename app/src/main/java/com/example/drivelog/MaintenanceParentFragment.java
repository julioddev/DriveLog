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

public class MaintenanceParentFragment extends Fragment {

    private ViewPager2 viewPager;
    private MaintenanceRegisterFragment registerFragment = new MaintenanceRegisterFragment();
    private MaintenanceHistoryFragment historyFragment = new MaintenanceHistoryFragment();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_maintenance_parent, container, false);

        TabLayout tabLayout = view.findViewById(R.id.tabLayoutMaintenance);
        viewPager = view.findViewById(R.id.viewPagerMaintenance);

        // 🔥 Correção: Ajusta o recuo superior para não ficar embaixo da barra de notificações
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return position == 0 ? registerFragment : historyFragment;
            }

            @Override
            public int getItemCount() {
                return 2;
            }
        });

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "Registro" : "Histórico");
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

    public void switchToRegisterAndEdit(Maintenance maintenance) {
        viewPager.setCurrentItem(0);
        registerFragment.startEdit(maintenance);
    }
}