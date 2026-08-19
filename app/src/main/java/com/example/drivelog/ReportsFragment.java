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

public class ReportsFragment extends Fragment {

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
        View view = inflater.inflate(R.layout.fragment_reports_parent, container, false);

        androidx.appcompat.widget.Toolbar toolbar = view.findViewById(R.id.toolbarReports);
        toolbar.setNavigationOnClickListener(v -> {
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        });

        TabLayout tabLayout = view.findViewById(R.id.tabLayoutReports);
        ViewPager2 viewPager = view.findViewById(R.id.viewPagerReports);

        ReportsPagerAdapter adapter = new ReportsPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText("Diário"); break;
                case 1: tab.setText("Semanal"); break;
                case 2: tab.setText("Mensal"); break;
                case 3: tab.setText("Anual"); break;
                case 4: tab.setText("Médias"); break;
            }
        }).attach();

        // Fix conflict with parent ViewPager2 (Main tabs)
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // Can be used to update specific data if needed
            }
        });

        return view;
    }
}