package com.example.drivelog;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.LoadAdError;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNav;
    private DrawerLayout drawerLayout;
    private RecyclerView recyclerDrawerRoutes, recyclerDrawerAllRoutes;
    private View layoutDrawerRoutes, layoutDrawerAllRoutes;
    private View settingsContainer, layoutSplash;
    private TextView textSplashStatus, textSplashPercent;
    private LinearProgressIndicator progressSplashLinear;
    private CircularProgressIndicator progressSplashCircle;
    private InterstitialAd mInterstitialAd;
    private boolean hasEarningsToday = false;
    private boolean isSystemUIVisible = true;
    private int kmStateToday = 0; 
    private int earningsStateToday = 0; 
    private boolean isInternalSelection = false;
    private SharedPreferences sharedPreferences;
    private Menu topMenu;
    private int currentTopSelection = -1;
    private int requestedRouteKmId = -1;
    private double requestedMapLat = -1, requestedMapLon = -1;
    private boolean isSplashFinalizing = false;
    private int splashPhraseIndex = 0;
    private com.google.android.material.button.MaterialButton btnDrawerSeeAllRoutes;
    private final String[] splashPhrases = {
            "Preparando seus ganhos...",
            "Calculando rotas...",
            "Sincronizando registros...",
            "Quase pronto!",
            "Tudo certo para começar!"
    };

    private final SharedPreferences.OnSharedPreferenceChangeListener prefListener = (prefs, key) -> {
        if ("app_mode".equals(key)) {
            runOnUiThread(() -> {
                updateNavigationIcon();
                refreshTabs();
                if (viewPager != null && viewPager.getAdapter() instanceof ViewPagerAdapter) {
                    ((ViewPagerAdapter) viewPager.getAdapter()).refreshEnabledTabs(prefs);
                }
            });
        } else if (key.startsWith("tab_") || key.equals("maps_enabled")) {
            runOnUiThread(this::refreshTabs);
        } else if ("sub_type".equals(key)) {
            runOnUiThread(this::setupAds);
        }
    };

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent event) {
        if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof android.widget.EditText) {
                android.graphics.Rect outRect = new android.graphics.Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private Toolbar topAppBar;
    private ListenerRegistration friendBadgeListener, devAlertListener, remoteMenuListener;
    private TextView textDrawerFriendsBadge;
    private View badgeLeftKm, badgeLeftEarnings;
    private List<String> currentRemoteMenus = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        applyAppTheme(sharedPreferences.getInt("app_theme", 0));
        
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        if (!isUserAuthenticated()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String uniqueId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        org.osmdroid.config.Configuration.getInstance().load(this, sharedPreferences);
        org.osmdroid.config.Configuration.getInstance().setUserAgentValue("DriveLogApp_v142_" + uniqueId);
        
        File tileCache = new File(getCacheDir(), "osmdroid_tiles_v142");
        if (!tileCache.exists()) tileCache.mkdirs();
        org.osmdroid.config.Configuration.getInstance().setOsmdroidTileCache(tileCache);

        if (!SecurityHelper.isAppSafe(this) && !BuildConfig.DEBUG) {
            Toast.makeText(this, "Esta cópia do DriveLog não é autêntica e será encerrada.", Toast.LENGTH_LONG).show();
            finishAffinity();
            return;
        }

        sharedPreferences.registerOnSharedPreferenceChangeListener(prefListener);
        setContentView(R.layout.activity_main);
        
        topAppBar = findViewById(R.id.topAppBar);
        topAppBar.setTitle("DriveLog");
        setSupportActionBar(topAppBar);
        
        settingsContainer = findViewById(R.id.settings_container);
        layoutSplash = findViewById(R.id.layoutMainSplash);
        textSplashStatus = findViewById(R.id.textSplashStatus);
        textSplashPercent = findViewById(R.id.textSplashPercent);
        progressSplashLinear = findViewById(R.id.progressSplashLinear);
        progressSplashCircle = findViewById(R.id.progressSplashCircle);

        textDrawerFriendsBadge = findViewById(R.id.textDrawerFriendsBadge);
        badgeLeftKm = findViewById(R.id.badgeLeftKm);
        badgeLeftEarnings = findViewById(R.id.badgeLeftEarnings);

        viewPager = findViewById(R.id.viewPager);
        viewPager.setOffscreenPageLimit(4);
        viewPager.setUserInputEnabled(false); // 🔥 Desabilita o deslize lateral para não travar o mapa
        
        View mainRoot = findViewById(R.id.main);
        View appBarLayout = findViewById(R.id.appBarLayout);
        bottomNav = findViewById(R.id.bottomNavigation);
        View adContainer = findViewById(R.id.adViewContainer);

        // Setamos o adapter uma única vez no início
        viewPager.setAdapter(new ViewPagerAdapter(this));

        ViewCompat.setOnApplyWindowInsetsListener(mainRoot, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            
            // Padding Superior (Status Bar) aplicado apenas ao AppBarLayout
            if (appBarLayout != null) {
                appBarLayout.setPadding(0, systemBars.top, 0, 0);
            }

            // Padding Inferior (Navigation Bar)
            if (adContainer != null && adContainer.getVisibility() == View.VISIBLE) {
                adContainer.setPadding(0, 0, 0, systemBars.bottom);
                if (bottomNav != null) bottomNav.setPadding(0, 0, 0, 0);
            } else if (bottomNav != null && bottomNav.getVisibility() == View.VISIBLE) {
                bottomNav.setPadding(0, 0, 0, systemBars.bottom);
            }
            
            // Dispara insets para o ViewPager para que os fragments possam reagir
            if (viewPager != null) {
                ViewCompat.dispatchApplyWindowInsets(viewPager, insets);
            }

            // 🔥 Ajuste de padding para o conteúdo do Drawer (Menu Lateral)
            View drawerContent = findViewById(R.id.layoutDrawerRoutes);
            View drawerAllContent = findViewById(R.id.layoutDrawerAllRoutes);
            if (drawerContent != null) drawerContent.setPadding(0, systemBars.top, 0, 0);
            if (drawerAllContent != null) drawerAllContent.setPadding(0, systemBars.top, 0, 0);
            
            return insets;
        });

        drawerLayout = findViewById(R.id.drawerLayout);
        if (drawerLayout != null) {
            drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
                @Override
                public void onDrawerClosed(View drawerView) {
                    super.onDrawerClosed(drawerView);
                    resetDrawerState();
                }
            });
        }
        layoutDrawerRoutes = findViewById(R.id.layoutDrawerRoutes);
        layoutDrawerAllRoutes = findViewById(R.id.layoutDrawerAllRoutes);
        recyclerDrawerRoutes = findViewById(R.id.recyclerDrawerRoutes);
        recyclerDrawerAllRoutes = findViewById(R.id.recyclerDrawerAllRoutes);
        btnDrawerSeeAllRoutes = findViewById(R.id.btnDrawerSeeAllRoutes);

        if (btnDrawerSeeAllRoutes != null) {
            btnDrawerSeeAllRoutes.setOnClickListener(v -> {
                if (layoutDrawerRoutes != null && layoutDrawerAllRoutes != null) {
                    layoutDrawerRoutes.setVisibility(View.GONE);
                    layoutDrawerAllRoutes.setVisibility(View.VISIBLE);
                    setupDrawerAllRoutes();
                }
            });
        }

        View btnBackFromAllRoutes = findViewById(R.id.btnBackFromAllRoutes);
        if (btnBackFromAllRoutes != null) {
            btnBackFromAllRoutes.setOnClickListener(v -> {
                if (layoutDrawerRoutes != null && layoutDrawerAllRoutes != null) {
                    layoutDrawerRoutes.setVisibility(View.VISIBLE);
                    layoutDrawerAllRoutes.setVisibility(View.GONE);
                }
            });
        }

        View btnDrawerFixados = findViewById(R.id.btnDrawerFixados);
        if (btnDrawerFixados != null) {
            btnDrawerFixados.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.closeDrawers();
                openFragmentInSettings(new CorrectedAddressesParentFragment(), "Endereços Corrigidos");
            });
        }

        View btnNewRoute = findViewById(R.id.btnDrawerNewRoute);
        if (btnNewRoute != null) {
            btnNewRoute.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.closeDrawers();
                if (viewPager != null) viewPager.setCurrentItem(0, false);
                boolean found = findAndCallNewRoute(getSupportFragmentManager());
                if (!found) {
                    Intent intent = new Intent("com.example.entregas.ACTION_NEW_ROUTE");
                    intent.setPackage(getPackageName());
                    sendBroadcast(intent);
                }
            });
        }

        View btnSettings = findViewById(R.id.btnDrawerSettings);
        if (btnSettings != null) {
            btnSettings.setVisibility(View.VISIBLE);
            btnSettings.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.closeDrawers();
                openFragmentInSettings(new SettingsParentFragment(), "Ajustes");
            });
        }

        setupQuickAccessDrawer();
        setupAds();
        startRemoteMenuListener();
        setupDrawerRoutes();
        refreshTabs();

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            refreshTabs();
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                updateToolbarTitle(viewPager.getCurrentItem());
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && (drawerLayout.isDrawerOpen(androidx.core.view.GravityCompat.START) || 
                    drawerLayout.isDrawerOpen(androidx.core.view.GravityCompat.END))) {
                    drawerLayout.closeDrawers();
                    return;
                }
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                    return;
                }
                int mapPos = ((ViewPagerAdapter) viewPager.getAdapter()).getPositionForId(R.id.nav_maps);
                if (viewPager.getCurrentItem() != mapPos) {
                    viewPager.setCurrentItem(mapPos, true);
                    return;
                }
                setEnabled(false);
                MainActivity.super.onBackPressed();
                setEnabled(true);
            }
        });
    }

    private void setupQuickAccessDrawer() {
        View btnMap = findViewById(R.id.btnLeftMap);
        if (btnMap != null) btnMap.setOnClickListener(v -> {
            if (drawerLayout != null) drawerLayout.closeDrawers();
            int pos = ((ViewPagerAdapter) viewPager.getAdapter()).getPositionForId(R.id.nav_maps);
            viewPager.setCurrentItem(pos, true);
        });

        View btnEarnings = findViewById(R.id.btnLeftEarnings);
        if (btnEarnings != null) btnEarnings.setOnClickListener(v -> {
            if (drawerLayout != null) drawerLayout.closeDrawers();
            int pos = ((ViewPagerAdapter) viewPager.getAdapter()).getPositionForId(R.id.nav_earnings);
            viewPager.setCurrentItem(pos, true);
        });

        View btnKm = findViewById(R.id.btnLeftKm);
        if (btnKm != null) btnKm.setOnClickListener(v -> {
            if (drawerLayout != null) drawerLayout.closeDrawers();
            int pos = ((ViewPagerAdapter) viewPager.getAdapter()).getPositionForId(R.id.nav_km);
            viewPager.setCurrentItem(pos, true);
        });

        View btnFuel = findViewById(R.id.btnLeftFuel);
        if (btnFuel != null) btnFuel.setOnClickListener(v -> {
            if (drawerLayout != null) drawerLayout.closeDrawers();
            int pos = ((ViewPagerAdapter) viewPager.getAdapter()).getPositionForId(R.id.nav_fuel);
            viewPager.setCurrentItem(pos, true);
        });

        View btnMaint = findViewById(R.id.btnLeftMaint);
        if (btnMaint != null) btnMaint.setOnClickListener(v -> {
            if (drawerLayout != null) drawerLayout.closeDrawers();
            int pos = ((ViewPagerAdapter) viewPager.getAdapter()).getPositionForId(R.id.nav_maintenance);
            viewPager.setCurrentItem(pos, true);
        });

        View btnReports = findViewById(R.id.btnLeftReports);
        if (btnReports != null) btnReports.setOnClickListener(v -> {
            if (drawerLayout != null) drawerLayout.closeDrawers();
            openFragmentInSettings(new ReportsFragment(), "Relatórios");
        });
    }

    private void setupAds() {
        new Thread(() -> {
            com.google.android.gms.ads.MobileAds.initialize(this, initializationStatus -> {});
            runOnUiThread(() -> {
                int subType = sharedPreferences.getInt("sub_type", 0);
                View adContainer = findViewById(R.id.adViewContainer);
                boolean showAds = false;
                if (subType == 0) {
                    long installDate = sharedPreferences.getLong("install_date", System.currentTimeMillis());
                    if (System.currentTimeMillis() - installDate > (7L * 24 * 60 * 60 * 1000)) {
                        showAds = true;
                    }
                }
                if (showAds) {
                    loadInterstitialAd();
                    if (adContainer != null) {
                        adContainer.setVisibility(View.VISIBLE);
                        if (adContainer.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                            ((ViewGroup.MarginLayoutParams) adContainer.getLayoutParams()).topMargin = (int) (-20 * getResources().getDisplayMetrics().density);
                        }
                        com.google.android.gms.ads.AdView adView = new com.google.android.gms.ads.AdView(this);
                        adView.setAdUnitId("ca-app-pub-3940256099942544/6300978111"); 
                        adView.setAdSize(com.google.android.gms.ads.AdSize.BANNER);
                        ((ViewGroup) adContainer).removeAllViews();
                        ((ViewGroup) adContainer).addView(adView);
                        com.google.android.gms.ads.AdRequest adRequest = new com.google.android.gms.ads.AdRequest.Builder().build();
                        adView.loadAd(adRequest);
                    }
                } else {
                    if (adContainer != null) adContainer.setVisibility(View.GONE);
                }
                refreshTabs();
            });
        }).start();
    }

    private void loadInterstitialAd() {
        com.google.android.gms.ads.AdRequest adRequest = new com.google.android.gms.ads.AdRequest.Builder().build();
        InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", adRequest, new InterstitialAdLoadCallback() {
            @Override public void onAdLoaded(@NonNull InterstitialAd interstitialAd) { mInterstitialAd = interstitialAd; }
            @Override public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) { mInterstitialAd = null; }
        });
    }

    public void showInterstitialThenAction(Runnable action) {
        if (mInterstitialAd != null) {
            mInterstitialAd.setFullScreenContentCallback(new com.google.android.gms.ads.FullScreenContentCallback() {
                @Override public void onAdDismissedFullScreenContent() { action.run(); loadInterstitialAd(); }
                @Override public void onAdFailedToShowFullScreenContent(com.google.android.gms.ads.AdError adError) { action.run(); }
            });
            mInterstitialAd.show(this);
        } else {
            action.run();
        }
    }

    private void startRemoteMenuListener() {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;
        FirebaseHelper.checkDeveloperAccess(user.getEmail(), isDev -> {
            remoteMenuListener = FirebaseHelper.listenRemoteMenus(isDev, allowedIds -> {
                this.currentRemoteMenus = allowedIds;
                runOnUiThread(() -> {
                    if (viewPager.getAdapter() instanceof ViewPagerAdapter) {
                        ((ViewPagerAdapter) viewPager.getAdapter()).setRemoteAllowedIds(allowedIds);
                    }
                    refreshTabs();
                    invalidateOptionsMenu();
                });
            });
        });
    }

    private void refreshTabs() {
        if (sharedPreferences == null || bottomNav == null) return;
        int subType = sharedPreferences.getInt("sub_type", 0); 
        int appMode = sharedPreferences.getInt("app_mode", 0); 
        if (subType == 0) {
            long installDate = sharedPreferences.getLong("install_date", System.currentTimeMillis());
            if (System.currentTimeMillis() - installDate > (7L * 24 * 60 * 60 * 1000)) appMode = 1;
        }
        boolean mapsOnly = appMode == 1;
        boolean useRemote = currentRemoteMenus != null;
        
        Menu bnMenu = bottomNav.getMenu();
        if (bnMenu != null) {
            MenuItem itemMaps = bnMenu.findItem(R.id.nav_maps);
            if (itemMaps != null) itemMaps.setVisible(getBoolSafe("maps_enabled", true) && (!useRemote || currentRemoteMenus.contains("maps")));
            
            MenuItem itemEarnings = bnMenu.findItem(R.id.nav_earnings);
            if (itemEarnings != null) itemEarnings.setVisible(getBoolSafe("tab_earnings_enabled", true) && (!useRemote || currentRemoteMenus.contains("earnings")));
            
            MenuItem itemKm = bnMenu.findItem(R.id.nav_km);
            if (itemKm != null) itemKm.setVisible(getBoolSafe("tab_km_enabled", true) && (!useRemote || currentRemoteMenus.contains("km")));
            
            MenuItem itemFuel = bnMenu.findItem(R.id.nav_fuel);
            if (itemFuel != null) itemFuel.setVisible(getBoolSafe("tab_fuel_enabled", true) && (!useRemote || currentRemoteMenus.contains("fuel")));
            
            MenuItem itemMaint = bnMenu.findItem(R.id.nav_maintenance);
            if (itemMaint != null) itemMaint.setVisible(getBoolSafe("tab_maintenance_enabled", true) && (!useRemote || currentRemoteMenus.contains("maintenance")));
        }

        boolean hasOtherTabs = false;
        if (!useRemote) {
            hasOtherTabs = getBoolSafe("tab_earnings_enabled", true) || getBoolSafe("tab_km_enabled", true) || getBoolSafe("tab_fuel_enabled", true) || getBoolSafe("tab_maintenance_enabled", true);
        } else {
            hasOtherTabs = currentRemoteMenus.contains("earnings") || currentRemoteMenus.contains("km") || currentRemoteMenus.contains("fuel") || currentRemoteMenus.contains("maintenance") || currentRemoteMenus.contains("reports");
        }

        View layoutQuickAccess = findViewById(R.id.layoutDrawerQuickAccess);
        if (layoutQuickAccess != null) layoutQuickAccess.setVisibility(hasOtherTabs ? View.VISIBLE : View.GONE);

        View btnMap = findViewById(R.id.btnLeftMap);
        if (btnMap != null) btnMap.setVisibility((!useRemote || currentRemoteMenus.contains("maps")) ? View.VISIBLE : View.GONE);
        
        View btnEarn = findViewById(R.id.btnLeftEarnings);
        if (btnEarn != null) {
            boolean visible = (!useRemote || currentRemoteMenus.contains("earnings"));
            btnEarn.setVisibility(visible ? View.VISIBLE : View.GONE);
            if (btnEarn.getParent() instanceof View) ((View) btnEarn.getParent()).setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        
        View btnKm = findViewById(R.id.btnLeftKm);
        if (btnKm != null) {
            boolean visible = (!useRemote || currentRemoteMenus.contains("km"));
            btnKm.setVisibility(visible ? View.VISIBLE : View.GONE);
            if (btnKm.getParent() instanceof View) ((View) btnKm.getParent()).setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        
        View btnFuel = findViewById(R.id.btnLeftFuel);
        if (btnFuel != null) btnFuel.setVisibility((!useRemote || currentRemoteMenus.contains("fuel")) ? View.VISIBLE : View.GONE);
        
        View btnMaint = findViewById(R.id.btnLeftMaint);
        if (btnMaint != null) btnMaint.setVisibility((!useRemote || currentRemoteMenus.contains("maintenance")) ? View.VISIBLE : View.GONE);
        
        View btnReportsLeft = findViewById(R.id.btnLeftReports);
        if (btnReportsLeft != null) btnReportsLeft.setVisibility((!useRemote || currentRemoteMenus.contains("reports")) ? View.VISIBLE : View.GONE);

        int backStackCount = getSupportFragmentManager().getBackStackEntryCount();
        boolean isShowingSettings = backStackCount > 0;
        
        if (settingsContainer != null) settingsContainer.setVisibility(isShowingSettings ? View.VISIBLE : View.GONE);
        if (viewPager != null) viewPager.setVisibility(isShowingSettings ? View.GONE : View.VISIBLE);

        boolean shouldShowUI = !mapsOnly && isSystemUIVisible && !isShowingSettings;
        
        bottomNav.setVisibility(shouldShowUI ? View.VISIBLE : View.GONE);
        View appBar = findViewById(R.id.appBarLayout);
        if (appBar != null) appBar.setVisibility(shouldShowUI ? View.VISIBLE : View.GONE);
        
        View mainRoot = findViewById(R.id.main);
        if (mainRoot != null) ViewCompat.requestApplyInsets(mainRoot);

        updateNavigationIcon();

        View btnFriends = findViewById(R.id.btnDrawerFriends);
        if (btnFriends != null) {
            boolean remoteVisible = (!useRemote || currentRemoteMenus.contains("friends"));
            btnFriends.setVisibility((mapsOnly && remoteVisible) ? View.VISIBLE : View.GONE);
        }

        View btnSettings = findViewById(R.id.btnDrawerSettings);
        if (btnSettings != null) {
            btnSettings.setVisibility((!useRemote || currentRemoteMenus.contains("settings")) ? View.VISIBLE : View.GONE);
        }

        View btnFixados = findViewById(R.id.btnDrawerFixados);
        if (btnFixados != null) {
            boolean visible = (!useRemote || currentRemoteMenus.contains("corrected_addresses"));
            btnFixados.setVisibility(visible ? View.VISIBLE : View.GONE);
        }

        notifyFragmentsRemoteMenuChanged(getSupportFragmentManager());
    }

    private void updateNavigationIcon() {
        if (topAppBar == null) return;
        int backStackCount = getSupportFragmentManager().getBackStackEntryCount();
        if (backStackCount > 0) {
            topAppBar.setNavigationIcon(R.drawable.ic_back_white);
        } else {
            boolean useRemote = currentRemoteMenus != null;
            boolean earningsVisible = getBoolSafe("tab_earnings_enabled", true) && (!useRemote || currentRemoteMenus.contains("earnings"));
            boolean kmVisible = getBoolSafe("tab_km_enabled", true) && (!useRemote || currentRemoteMenus.contains("km"));
            boolean shouldShowBadge = (kmVisible && kmStateToday < 2) || (earningsVisible && earningsStateToday < 2);
            if (shouldShowBadge) topAppBar.setNavigationIcon(getBadgedHamburgerIcon(kmVisible, earningsVisible));
            else topAppBar.setNavigationIcon(R.drawable.ic_menu_white);
        }
    }

    private android.graphics.drawable.Drawable getBadgedHamburgerIcon(boolean kmVisible, boolean earningsVisible) {
        android.graphics.drawable.Drawable base = ContextCompat.getDrawable(this, R.drawable.ic_menu_white);
        if (base == null) return null;
        int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        base.setBounds(0, 0, size, size);
        base.draw(canvas);
        android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        boolean hasNone = (kmVisible && kmStateToday == 0) || (earningsVisible && earningsStateToday == 0);
        if (hasNone) paint.setColor(Color.RED); else paint.setColor(Color.BLACK);
        float radius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
        canvas.drawCircle(size - radius, radius, radius, paint);
        return new android.graphics.drawable.BitmapDrawable(getResources(), bitmap);
    }

    private void finalizeSplash(long delay) {
        if (isSplashFinalizing) return;
        isSplashFinalizing = true;
        if (progressSplashLinear != null) { progressSplashLinear.setIndeterminate(false); progressSplashLinear.setProgress(100); }
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (layoutSplash != null) {
                layoutSplash.animate().alpha(0f).setDuration(200).withEndAction(() -> {
                    layoutSplash.setVisibility(View.GONE);
                    if (viewPager != null && viewPager.getAdapter() != null) {
                        updateKeepScreenOn(viewPager.getCurrentItem());
                    }
                    
                    // 🔥 Verificação Automática de Atualização (Silenciosa se não houver)
                    UpdateHelper.checkForUpdates(this, false, null);
                }).start();
            }
        }, delay);
    }

    private boolean getBoolSafe(String key, boolean def) {
        try { return sharedPreferences.getBoolean(key, def); } catch (Exception e) { return def; }
    }

    public void openFragmentInSettings(Fragment fragment, String title) {
        if (settingsContainer == null) return;
        viewPager.setVisibility(View.GONE);
        settingsContainer.setVisibility(View.VISIBLE);
        getSupportFragmentManager().beginTransaction().replace(R.id.settings_container, fragment).addToBackStack(title).commit();
        refreshTabs(); // 🔥 Garante que a Toolbar principal suma
        updateToolbarTitle(-1);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { 
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) getSupportFragmentManager().popBackStack();
            else if (drawerLayout != null) drawerLayout.openDrawer(androidx.core.view.GravityCompat.START);
            return true;
        }
        int itemId = item.getItemId();
        if (itemId == R.id.action_reports) { openFragmentInSettings(new ReportsFragment(), "Relatórios"); currentTopSelection = itemId; }
        else if (itemId == R.id.action_friends) { openFragmentInSettings(new FriendsFragment(), "Amigos"); currentTopSelection = itemId; }
        else if (itemId == R.id.action_settings) { openFragmentInSettings(new SettingsParentFragment(), "Ajustes"); currentTopSelection = itemId; }
        updateTopMenuVisuals();
        return super.onOptionsItemSelected(item);
    }

    private void updateTopMenuVisuals() {
        if (topMenu == null) return;
        MenuItem friends = topMenu.findItem(R.id.action_friends);
        MenuItem reports = topMenu.findItem(R.id.action_reports);
        MenuItem settings = topMenu.findItem(R.id.action_settings);
        if (friends != null) friends.getIcon().setAlpha(currentTopSelection == R.id.action_friends ? 255 : 128);
        if (reports != null) reports.getIcon().setAlpha(currentTopSelection == R.id.action_reports ? 255 : 128);
        if (settings != null) settings.getIcon().setAlpha(currentTopSelection == R.id.action_settings ? 255 : 128);
    }

    private void updateToolbarTitle(int pos) {
        String title = "DriveLog";
        if (pos == -1) {
            Fragment f = getSupportFragmentManager().findFragmentById(R.id.settings_container);
            if (f instanceof SettingsFragment) title = "Configurações";
            else if (f instanceof ReportsFragment) title = "Relatórios e Estatísticas";
            else if (f instanceof FriendsFragment) title = "Amigos e Parceiros";
            else if (f instanceof CorrectedAddressesParentFragment) title = "Endereços Corrigidos";
        } else if (viewPager.getAdapter() != null) {
            int id = ((ViewPagerAdapter) viewPager.getAdapter()).getIdForPosition(pos);
            if (id == R.id.nav_maps) title = "Navegação e Rotas";
            else if (id == R.id.nav_earnings) title = "Meus Ganhos";
            else if (id == R.id.nav_km) title = "Controle de KM";
            else if (id == R.id.nav_fuel) title = "Abastecimentos";
            else if (id == R.id.nav_maintenance) title = "Manutenção";
        }
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(title);
        updateNavigationIcon();
    }

    private void updateKeepScreenOn(int pos) {
        if (viewPager.getAdapter() == null) return;
        int id = ((ViewPagerAdapter) viewPager.getAdapter()).getIdForPosition(pos);
        if (id == R.id.nav_maps) getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void syncBottomNav(int pos) {
        if (viewPager.getAdapter() == null) return;
        int id = ((ViewPagerAdapter) viewPager.getAdapter()).getIdForPosition(pos);
        isInternalSelection = true;
        bottomNav.setSelectedItemId(id);
        isInternalSelection = false;
    }

    private void updateIconColors() {
        // Dummy placeholder
    }

    public boolean isMenuVisible(String id) {
        return currentRemoteMenus == null || currentRemoteMenus.contains(id);
    }

    private void resetDrawerState() {
        if (layoutDrawerRoutes != null) layoutDrawerRoutes.setVisibility(View.VISIBLE);
        if (layoutDrawerAllRoutes != null) layoutDrawerAllRoutes.setVisibility(View.GONE);
    }

    private void notifyFragmentsRemoteMenuChanged(FragmentManager fm) {
        if (fm == null) return;
        for (Fragment f : fm.getFragments()) {
            if (f instanceof RouteFragment) ((RouteFragment) f).refreshBadges();
            if (f instanceof SettingsFragment) ((SettingsFragment) f).refreshVisibility();
            if (f != null && f.getChildFragmentManager() != null) notifyFragmentsRemoteMenuChanged(f.getChildFragmentManager());
        }
    }

    private boolean findAndCallNewRoute(FragmentManager fm) {
        if (fm == null) return false;
        for (Fragment f : fm.getFragments()) {
            if (f instanceof RouteFragment) { ((RouteFragment) f).promptNewRoute(); return true; }
            if (f != null && f.getChildFragmentManager().getFragments().size() > 0) if (findAndCallNewRoute(f.getChildFragmentManager())) return true;
        }
        return false;
    }

    private void setupDrawerRoutes() {
        if (recyclerDrawerRoutes == null) return;
        recyclerDrawerRoutes.setLayoutManager(new LinearLayoutManager(this));
        AppDatabase.getInstance(this).appDao().getAllRoutesLive().observe(this, list -> {
            List<RouteHeader> displayList = list;
            if (list != null && list.size() > 3) {
                displayList = list.subList(0, 3);
                if (btnDrawerSeeAllRoutes != null) btnDrawerSeeAllRoutes.setVisibility(View.VISIBLE);
            } else {
                if (btnDrawerSeeAllRoutes != null) btnDrawerSeeAllRoutes.setVisibility(View.GONE);
            }
            DrawerRouteAdapter adapter = new DrawerRouteAdapter(displayList, new DrawerRouteAdapter.OnRouteActionListener() {
                @Override public void onClick(RouteHeader header) { switchRoute(header.id); if (drawerLayout != null) drawerLayout.closeDrawers(); }
                @Override public void onEdit(RouteHeader header) { promptEditRouteName(header); }
                @Override public void onDelete(RouteHeader header) { confirmDeleteRoute(header); }
            });
            recyclerDrawerRoutes.setAdapter(adapter);
        });
    }

    private void setupDrawerAllRoutes() {
        if (recyclerDrawerAllRoutes == null) return;
        recyclerDrawerAllRoutes.setLayoutManager(new LinearLayoutManager(this));
        AppDatabase.getInstance(this).appDao().getAllRoutesLive().observe(this, list -> {
            DrawerRouteAdapter adapter = new DrawerRouteAdapter(list, new DrawerRouteAdapter.OnRouteActionListener() {
                @Override public void onClick(RouteHeader header) { switchRoute(header.id); if (drawerLayout != null) drawerLayout.closeDrawers(); }
                @Override public void onEdit(RouteHeader header) { promptEditRouteName(header); }
                @Override public void onDelete(RouteHeader header) { confirmDeleteRoute(header); }
            });
            recyclerDrawerAllRoutes.setAdapter(adapter);
        });
    }

    private void switchRoute(int id) { sharedPreferences.edit().putInt("last_opened_route_id", id).apply(); }
    
    private void promptEditRouteName(RouteHeader header) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_new_route_name, null);
        com.google.android.material.textfield.TextInputEditText edit = dialogView.findViewById(R.id.editRouteName);
        edit.setText(header.name);
        edit.selectAll();
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this).setView(dialogView).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialogView.findViewById(R.id.btnCancelNewRoute).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnNextNewRoute).setOnClickListener(v -> {
            String newN = edit.getText().toString().trim();
            if (!newN.isEmpty()) { new Thread(() -> { header.name = newN; AppDatabase.getInstance(this).appDao().updateRouteHeader(header); CloudSyncHelper.syncNow(this); }).start(); }
            dialog.dismiss();
        });
        dialog.show();
    }
    
    private void confirmDeleteRoute(RouteHeader header) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_modern_confirm, null);
        TextView title = dialogView.findViewById(R.id.textModernTitle);
        TextView message = dialogView.findViewById(R.id.textModernMessage);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnModernNegative);
        com.google.android.material.button.MaterialButton btnConfirm = dialogView.findViewById(R.id.btnModernPositive);
        title.setText("Excluir Rota");
        message.setText("Deseja apagar permanentemente a rota:\n" + header.name + "?");
        btnConfirm.setText("EXCLUIR");
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this).setView(dialogView).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            new Thread(() -> { AppDatabase.getInstance(this).appDao().deleteRouteHeader(header); CloudSyncHelper.syncNow(this); }).start();
        });
        dialog.show();
    }

    private static class DrawerRouteAdapter extends RecyclerView.Adapter<DrawerRouteAdapter.ViewHolder> {
        private final List<RouteHeader> routes; private final OnRouteActionListener listener;
        private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy HH:mm", java.util.Locale.getDefault());
        interface OnRouteActionListener { void onClick(RouteHeader header); void onEdit(RouteHeader header); void onDelete(RouteHeader header); }
        DrawerRouteAdapter(List<RouteHeader> routes, OnRouteActionListener listener) { this.routes = routes; this.listener = listener; }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) { return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_drawer_route, parent, false)); }
        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            RouteHeader r = routes.get(position); holder.textName.setText(r.name); holder.textDate.setText(sdf.format(new java.util.Date(r.date)));
            holder.itemView.setOnClickListener(v -> listener.onClick(r)); holder.btnEdit.setOnClickListener(v -> listener.onEdit(r)); holder.btnDelete.setOnClickListener(v -> listener.onDelete(r));
        }
        @Override public int getItemCount() { return routes.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder { TextView textName, textDate; ImageButton btnEdit, btnDelete; ViewHolder(View v) { super(v); textName = v.findViewById(R.id.textDrawerRouteName); textDate = v.findViewById(R.id.textDrawerRouteDate); btnEdit = v.findViewById(R.id.btnEditRoute); btnDelete = v.findViewById(R.id.btnDeleteRoute); } }
    }

    private boolean isUserAuthenticated() {
        com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return false;
        // Só consideramos autenticado se o setup inicial do splash/backup foi concluído
        return sharedPreferences.getBoolean("first_setup_splash_done", false);
    }

    public void applyAppTheme(int themeIndex) {
        int themeResId;
        switch (themeIndex) {
            case 1: themeResId = R.style.Theme_Entregas_Ocean; break;
            case 2: themeResId = R.style.Theme_Entregas_Forest; break;
            case 3: themeResId = R.style.Theme_Entregas_Purple; break;
            case 4: themeResId = R.style.Theme_Entregas_Orange; break;
            case 5: themeResId = R.style.Theme_Entregas_DeepDark; break;
            default: themeResId = R.style.Theme_Entregas; break;
        }
        setTheme(themeResId);
    }

    public boolean isSystemUIVisible() { return isSystemUIVisible; }

    public void setSystemUIVisible(boolean visible) {
        if (this.isSystemUIVisible == visible) return;
        this.isSystemUIVisible = visible;
        refreshTabs();
    }

    public void setKmStateToday(int state) {
        this.kmStateToday = state;
        runOnUiThread(this::updateNavigationIcon);
    }

    public void setEarningsStateToday(int state) {
        this.earningsStateToday = state;
        runOnUiThread(this::updateNavigationIcon);
    }

    public void returnToMainMap() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        if (settingsContainer != null) settingsContainer.setVisibility(View.GONE);
        if (viewPager != null) {
            viewPager.setVisibility(View.VISIBLE);
            if (viewPager.getAdapter() != null) {
                int pos = ((ViewPagerAdapter) viewPager.getAdapter()).getPositionForId(R.id.nav_maps);
                viewPager.setCurrentItem(pos, true);
                updateToolbarTitle(pos);
            }
        }
        refreshTabs();
    }

    public void returnToMainMap(double lat, double lon) {
        this.requestedMapLat = lat;
        this.requestedMapLon = lon;
        returnToMainMap();
    }

    public double[] consumeRequestedLocation() {
        if (requestedMapLat == -1) return null;
        double[] loc = new double[]{requestedMapLat, requestedMapLon};
        requestedMapLat = -1;
        requestedMapLon = -1;
        return loc;
    }

    public void openGeneralSettings() {
        openFragmentInSettings(new SettingsFragment(), "Configurações");
    }

    public void showRouteOnMap(int routeId) {
        this.requestedRouteKmId = routeId;
        if (viewPager != null && viewPager.getAdapter() instanceof ViewPagerAdapter) {
            int pos = ((ViewPagerAdapter) viewPager.getAdapter()).getPositionForId(R.id.nav_maps);
            viewPager.setCurrentItem(pos, true);
        }
    }

    public int consumeRequestedRouteKmId() {
        int id = requestedRouteKmId;
        requestedRouteKmId = -1;
        return id;
    }

    public void openRoutesDrawer() {
        if (drawerLayout != null) {
            // 🔥 Usamos explicitamente START para respeitar o layout e evitar erros de gravidade
            drawerLayout.openDrawer(androidx.core.view.GravityCompat.START);
        }
    }
}
