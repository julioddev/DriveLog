package com.example.drivelog;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FriendsFragment extends Fragment {

    private ListenerRegistration badgeListener;
    private TabLayout tabLayout;

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
        View view = inflater.inflate(R.layout.fragment_friends_parent, container, false);

        androidx.appcompat.widget.Toolbar toolbar = view.findViewById(R.id.toolbarFriends);
        toolbar.setNavigationOnClickListener(v -> {
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        });

        tabLayout = view.findViewById(R.id.tabLayoutFriends);
        androidx.viewpager2.widget.ViewPager2 viewPager = view.findViewById(R.id.viewPagerFriends);

        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                if (position == 0) return new FriendsListFragment();
                if (position == 1) return new FriendRequestsFragment();
                return new MyProfileFragment();
            }

            @Override
            public int getItemCount() { return 3; }
        });

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) tab.setText("Meus Amigos");
            else if (position == 1) tab.setText("Notificações");
            else tab.setText("Meu Perfil");
        }).attach();

        // 🔥 Listener para o Badge da Aba Notificações
        startTabBadgeListener();

        // Remove o badge quando entrar na aba de Notificações
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 1) {
                    com.google.android.material.badge.BadgeDrawable badge = tab.getBadge();
                    if (badge != null) badge.setVisible(false);
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {
                if (tab.getPosition() == 1) {
                    com.google.android.material.badge.BadgeDrawable badge = tab.getBadge();
                    if (badge != null) badge.setVisible(false);
                }
            }
        });

        return view;
    }

    private void startTabBadgeListener() {
        com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        String myEmail = user.getEmail().toLowerCase();
        
        // 🔥 Busca o status de DEV uma vez antes de iniciar o listener do Badge
        FirebaseHelper.checkDeveloperAccess(myEmail, isDev -> {
            if (!isAdded()) return;
            
            badgeListener = FirebaseHelper.listenAllNotifications(myEmail, new FirebaseHelper.NotificationCallback() {
                @Override
                public void onResult(List<Map<String, Object>> notifications) {
                    // Se não for DEV, remove as notificações de compartilhamento da contagem do badge
                    List<Map<String, Object>> filtered = new ArrayList<>();
                    for (Map<String, Object> n : notifications) {
                        if (isDev || "friend_request".equals(n.get("type"))) {
                            filtered.add(n);
                        }
                    }
                    updateTabBadge(filtered.size());
                }

                @Override
                public void onError(String msg) { }
            });
        });
    }

    private void updateTabBadge(int count) {
        if (getActivity() == null || tabLayout == null) return;
        getActivity().runOnUiThread(() -> {
            TabLayout.Tab notificationsTab = tabLayout.getTabAt(1);
            if (notificationsTab != null) {
                if (count > 0) {
                    // Só mostra se NÃO estivermos na aba de notificações no momento
                    boolean isCurrentlyOnNotifications = tabLayout.getSelectedTabPosition() == 1;
                    com.google.android.material.badge.BadgeDrawable badge = notificationsTab.getOrCreateBadge();
                    badge.setNumber(count);
                    badge.setBackgroundColor(android.graphics.Color.RED);
                    badge.setBadgeTextColor(android.graphics.Color.WHITE);
                    badge.setVisible(!isCurrentlyOnNotifications);
                } else {
                    notificationsTab.removeBadge();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (badgeListener != null) badgeListener.remove();
    }
}
