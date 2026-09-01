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

public class MapParentFragment extends Fragment {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map_parent, container, false);

        tabLayout = view.findViewById(R.id.tabLayoutMap);
        viewPager = view.findViewById(R.id.viewPagerMap);

        // 🔥 Ajusta recuo superior do TabLayout quando os menus estão ocultos
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            
            boolean isImmersiveActive = false;
            if (getActivity() instanceof MainActivity) {
                isImmersiveActive = !((MainActivity) getActivity()).isSystemUIVisible();
            }

            if (tabLayout != null) {
                int topPadding = isImmersiveActive ? systemBars.top : 0;
                tabLayout.setPadding(tabLayout.getPaddingLeft(), topPadding, tabLayout.getPaddingRight(), tabLayout.getPaddingBottom());
            }

            return insets;
        });

        refreshModeUI();

        return view;
    }

    private void refreshModeUI() {
        if (viewPager == null || tabLayout == null || getContext() == null) return;

        MapPagerAdapter adapter = new MapPagerAdapter(this);
        viewPager.setAdapter(adapter);
        viewPager.setUserInputEnabled(false);
        
        tabLayout.setVisibility(View.GONE);
    }

    public void switchToMap() {
        if (viewPager != null) {
            viewPager.setCurrentItem(0, true);
        }
    }
}
