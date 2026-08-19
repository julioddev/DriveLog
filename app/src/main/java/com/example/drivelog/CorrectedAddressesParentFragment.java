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

public class CorrectedAddressesParentFragment extends Fragment {

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
        View view = inflater.inflate(R.layout.fragment_corrected_addresses_parent, container, false);

        androidx.appcompat.widget.Toolbar toolbar = view.findViewById(R.id.toolbarCorrected);
        if (toolbar != null) {
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
        }

        TabLayout tabLayout = view.findViewById(R.id.tabLayoutCorrected);
        ViewPager2 viewPager = view.findViewById(R.id.viewPagerCorrected);

        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull @Override public Fragment createFragment(int position) {
                if (position == 0) return new CorrectedAddressesFragment();
                return new CommunityAddressesFragment();
            }
            @Override public int getItemCount() { return 2; }
        });

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "Meus Endereços" : "Comunidade");
        }).attach();

        return view;
    }
}
