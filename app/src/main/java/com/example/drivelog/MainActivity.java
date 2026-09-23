package com.example.drivelog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.LoadAdError;

import org.osmdroid.config.Configuration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
    private boolean isInternalSelection = false;
    private SharedPreferences sharedPreferences;
    private Menu topMenu;
    private int currentTopSelection = -1;
    private int requestedRouteKmId = -1;
    private double requestedMapLat = -1, requestedMapLon = -1;
    private boolean isSplashFinalizing = false;
    private int splashPhraseIndex = 0;
    private MaterialButton btnDrawerSeeAllRoutes;
    private final String[] splashPhrases = {
            "Preparando seus ganhos...",
            "Calculando rotas...",
            "Sincronizando registros...",
            "Quase pronto!",
            "Tudo certo para começar!"
    };

    private final SharedPreferences.OnSharedPreferenceChangeListener prefListener = (prefs, key) -> {
        if (key.startsWith("tab_") || key.equals("maps_enabled")) {
            runOnUiThread(this::refreshTabs);
        } else if ("sub_type".equals(key)) {
            runOnUiThread(() -> {
                setupAds();
                refreshTabs();
            });
        }
        
        // 🔥 Sincronização Automática com a Nuvem ao alterar qualquer configuração
        if (key != null && !key.endsWith("_x") && !key.endsWith("_y") && !key.contains("interaction") && !key.contains("last_update")) {
            CloudSyncHelper.syncNow(this, "Ajuste alterado");
        }
    };

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private Toolbar topAppBar;
    private ListenerRegistration friendBadgeListener, devAlertListener, remoteMenuListener;
    private TextView textDrawerFriendsBadge;
    private List<String> currentRemoteMenus = null;
    private int currentThemeIndex = -1;

    private final Handler interactionHandler = new Handler(Looper.getMainLooper());
    private final Runnable interactionRunnable = new Runnable() {
        @Override
        public void run() {
            if (sharedPreferences != null) {
                sharedPreferences.edit().putLong("last_drivelog_interaction", System.currentTimeMillis()).apply();
            }
            interactionHandler.postDelayed(this, 60000); // Atualiza a cada 1 minuto enquanto aberto
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        // Marca que o usuário está ativo no app
        sharedPreferences.edit().putLong("last_drivelog_interaction", System.currentTimeMillis()).apply();
        interactionHandler.post(interactionRunnable);

        // Remove o ícone flutuante se ele estiver ativo ao voltar para o app
        stopService(new Intent(this, FloatingIconService.class));
        
        // Garante que o monitoramento automático esteja ativo se configurado
        TrackingHelper.updateAutoTracking(this);

        startCommunityNotificationsListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Atualiza o timestamp ao sair ou pausar o app
        sharedPreferences.edit().putLong("last_drivelog_interaction", System.currentTimeMillis()).apply();
        interactionHandler.removeCallbacks(interactionRunnable);
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        // Marca o momento da saída para iniciar a contagem de inatividade (suspender CPF automático)
        sharedPreferences.edit().putLong("last_drivelog_interaction", System.currentTimeMillis()).apply();

        // 🔥 Quando o app é minimizado (Home/Recents)
        if (sharedPreferences != null && sharedPreferences.getBoolean("floating_icon_enabled", true)) {
            // Só iniciamos se tivermos a permissão de sobreposição
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    Intent serviceIntent = new Intent(this, FloatingIconService.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent);
                    } else {
                        startService(serviceIntent);
                    }
                }
            } else {
                startService(new Intent(this, FloatingIconService.class));
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        
        // 🔥 FORÇA MODO MAPA COMO PADRÃO ÚNICO
        sharedPreferences.edit().putInt("app_mode", 1).apply();
        
        // 🔥 PADRÃO: Azul Oceano (1) ao instalar pela primeira vez
        applyAppTheme(sharedPreferences.getInt("app_theme", 1));
        
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);


        if (!isUserAuthenticated()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String uniqueId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        Configuration.getInstance().load(this, sharedPreferences);
        Configuration.getInstance().setUserAgentValue("DriveLogApp_v1527_" + uniqueId);
        
        // 🔥 MELHORIA DE PERFORMANCE NO MAPA
        // Aumenta o cache para 1GB (em vez do padrão menor) e aumenta o paralelismo
        Configuration.getInstance().setTileFileSystemCacheMaxBytes(1024L * 1024L * 1024L);
        Configuration.getInstance().setTileDownloadThreads((short) 12); // Mais threads para baixar simultaneamente
        Configuration.getInstance().setCacheMapTileCount((short) 60); // Mais tiles em memória RAM
        
        // Configurações de trim do cache (limpa quando chegar perto do limite)
        Configuration.getInstance().setTileFileSystemCacheTrimBytes(800L * 1024L * 1024L);

        // 🔥 NOVO: Melhora a velocidade de resposta do disco
        Configuration.getInstance().setTileDownloadMaxQueueSize((short) 100);
        
        File tileCache = new File(getCacheDir(), "osmdroid_tiles_v142");
        if (!tileCache.exists()) tileCache.mkdirs();
        Configuration.getInstance().setOsmdroidTileCache(tileCache);

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

        viewPager = findViewById(R.id.viewPager);
        viewPager.setOffscreenPageLimit(4);
        viewPager.setUserInputEnabled(false); // 🔥 Desabilita o deslize lateral para não travar o mapa
        
        View mainRoot = findViewById(R.id.main);
        View appBarLayout = findViewById(R.id.appBarLayout);
        bottomNav = findViewById(R.id.bottomNavigation);
        View adContainer = findViewById(R.id.adViewContainer);

        // Setamos o adapter uma única vez no início
        viewPager.setAdapter(new ViewPagerAdapter(this));
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateToolbarTitle(position);
                syncBottomNav(position);
                updateKeepScreenOn(position);
            }
        });

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

        // 🔥 Verificação Automática de Atualização (Silenciosa se não houver)
        if (sharedPreferences.getBoolean("auto_check_updates", true)) {
            UpdateHelper.checkForUpdates(this, false, null);
        }

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            refreshTabs();
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                updateToolbarTitle(viewPager.getCurrentItem());
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && (drawerLayout.isDrawerOpen(GravityCompat.START) ||
                    drawerLayout.isDrawerOpen(GravityCompat.END))) {
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
            openFragmentInSettings(new EarningsParentFragment(), "Ganhos");
        });

        View btnKm = findViewById(R.id.btnLeftKm);
        if (btnKm != null) btnKm.setOnClickListener(v -> {
            if (drawerLayout != null) drawerLayout.closeDrawers();
            openFragmentInSettings(new KmParentFragment(), "KM Diário");
        });

        View btnFuel = findViewById(R.id.btnLeftFuel);
        if (btnFuel != null) btnFuel.setOnClickListener(v -> {
            if (drawerLayout != null) drawerLayout.closeDrawers();
            openFragmentInSettings(new FuelParentFragment(), "Abastecimentos");
        });

        View btnMaint = findViewById(R.id.btnLeftMaint);
        if (btnMaint != null) btnMaint.setOnClickListener(v -> {
            if (drawerLayout != null) drawerLayout.closeDrawers();
            openFragmentInSettings(new MaintenanceParentFragment(), "Manutenção");
        });

        View btnReports = findViewById(R.id.btnLeftReports);
        if (btnReports != null) btnReports.setOnClickListener(v -> {
            if (drawerLayout != null) drawerLayout.closeDrawers();
            openFragmentInSettings(new ReportsFragment(), "Relatórios");
        });

        View btnNotif = findViewById(R.id.btnLeftNotifications);
        if (btnNotif != null) btnNotif.setOnClickListener(v -> {
            if (drawerLayout != null) drawerLayout.closeDrawers();
            showCommunityNotificationsDialog();
        });
    }

    private void setupAds() {
        new Thread(() -> {
            MobileAds.initialize(this, initializationStatus -> {});
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
                        AdView adView = new AdView(this);
                        adView.setAdUnitId("ca-app-pub-3940256099942544/6300978111"); 
                        adView.setAdSize(AdSize.BANNER);
                        ((ViewGroup) adContainer).removeAllViews();
                        ((ViewGroup) adContainer).addView(adView);
                        AdRequest adRequest = new AdRequest.Builder().build();
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
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", adRequest, new InterstitialAdLoadCallback() {
            @Override public void onAdLoaded(@NonNull InterstitialAd interstitialAd) { mInterstitialAd = interstitialAd; }
            @Override public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) { mInterstitialAd = null; }
        });
    }

    public void showInterstitialThenAction(Runnable action) {
        if (mInterstitialAd != null) {
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override public void onAdDismissedFullScreenContent() { action.run(); loadInterstitialAd(); }
                @Override public void onAdFailedToShowFullScreenContent(AdError adError) { action.run(); }
            });
            mInterstitialAd.show(this);
        } else {
            action.run();
        }
    }

    private void startRemoteMenuListener() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        // 🔥 Sincroniza permissões e data de instalação do Firebase para o local
        long localInstallDate = sharedPreferences.getLong("install_date", System.currentTimeMillis());
        int localSub = sharedPreferences.getInt("sub_type", 0);
        FirebaseHelper.syncUserMetadata(user.getEmail(), localInstallDate, localSub, new FirebaseHelper.UserMetadataCallback() {
            @Override
            public void onSuccess(long cloudDate, int cloudSub) {
                if (localInstallDate != cloudDate || localSub != cloudSub) {
                    sharedPreferences.edit()
                            .putLong("install_date", cloudDate)
                            .putInt("sub_type", cloudSub)
                            .apply();
                    
                    if (localSub == 0 && cloudSub == 1) {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Sua conta foi atualizada para PREMIUM! Aproveite.", Toast.LENGTH_LONG).show());
                    }
                }
                
                // 🔥 Inicia a escuta dos menus remotos baseada no nível de assinatura ATUALIZADO
                startMenuListener(cloudSub);
            }
            @Override public void onError(String msg) {
                // Fallback para o nível local se falhar a sincronização inicial
                startMenuListener(localSub);
            }
        });
    }

    private void startMenuListener(int subType) {
        if (remoteMenuListener != null) remoteMenuListener.remove();
        
        remoteMenuListener = FirebaseHelper.listenRemoteMenus(subType, allowedIds -> {
            this.currentRemoteMenus = allowedIds;
            runOnUiThread(() -> {
                if (viewPager.getAdapter() instanceof ViewPagerAdapter) {
                    ((ViewPagerAdapter) viewPager.getAdapter()).setRemoteAllowedIds(allowedIds);
                }
                refreshTabs();
                invalidateOptionsMenu();
            });
        });
    }

    private void refreshTabs() {
        if (sharedPreferences == null || bottomNav == null) return;
        
        // 🔥 MODO MAPA É O ÚNICO E PADRÃO
        boolean useRemote = currentRemoteMenus != null;
        
        // Esconde menu inferior e barra superior para foco total no mapa
        bottomNav.setVisibility(View.GONE);
        View appBar = findViewById(R.id.appBarLayout);
        if (appBar != null) appBar.setVisibility(View.GONE);

        // Visibilidade das opções no Menu Lateral (Drawer)
        boolean hasOtherTabs;
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
        
        // ... (continua lógica do drawer)
        View btnMaint = findViewById(R.id.btnLeftMaint);
        if (btnMaint != null) btnMaint.setVisibility((!useRemote || currentRemoteMenus.contains("maintenance")) ? View.VISIBLE : View.GONE);
        
        View btnReportsLeft = findViewById(R.id.btnLeftReports);
        if (btnReportsLeft != null) btnReportsLeft.setVisibility((!useRemote || currentRemoteMenus.contains("reports")) ? View.VISIBLE : View.GONE);

        View containerNotif = findViewById(R.id.layoutLeftNotificationsContainer);
        if (containerNotif != null) containerNotif.setVisibility((!useRemote || currentRemoteMenus.contains("community_notifications")) ? View.VISIBLE : View.GONE);

        int backStackCount = getSupportFragmentManager().getBackStackEntryCount();
        boolean isShowingSettings = backStackCount > 0;
        
        if (settingsContainer != null) settingsContainer.setVisibility(isShowingSettings ? View.VISIBLE : View.GONE);
        if (viewPager != null) viewPager.setVisibility(isShowingSettings ? View.GONE : View.VISIBLE);

        View mainRoot = findViewById(R.id.main);
        if (mainRoot != null) ViewCompat.requestApplyInsets(mainRoot);

        updateNavigationIcon();

        View btnFriends = findViewById(R.id.btnDrawerFriends);
        if (btnFriends != null) {
            boolean remoteVisible = (!useRemote || currentRemoteMenus.contains("friends"));
            btnFriends.setVisibility(remoteVisible ? View.VISIBLE : View.GONE);
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
            topAppBar.setNavigationIcon(R.drawable.ic_menu_white);
        }
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
                    if (sharedPreferences.getBoolean("auto_check_updates", true)) {
                        UpdateHelper.checkForUpdates(this, false, null);
                    }
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
            else if (drawerLayout != null) drawerLayout.openDrawer(GravityCompat.START);
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
        if (id == R.id.nav_maps) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
            if (f instanceof RouteFragment) {
                ((RouteFragment) f).refreshBadges();
                ((RouteFragment) f).refreshRemoteVisibility();
            }
            if (f instanceof MapsFragment) {
                ((MapsFragment) f).refreshRemoteVisibility();
            }
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
        TextInputEditText edit = dialogView.findViewById(R.id.editRouteName);
        edit.setText(header.name);
        edit.selectAll();
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();
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
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnModernNegative);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btnModernPositive);
        title.setText("Excluir Rota");
        message.setText("Deseja apagar permanentemente a rota:\n" + header.name + "?");
        btnConfirm.setText("EXCLUIR");
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            new Thread(() -> { AppDatabase.getInstance(this).appDao().deleteRouteHeader(header); CloudSyncHelper.syncNow(this, "Rota Excluída"); }).start();
        });
        dialog.show();
    }

    private ListenerRegistration communityNotifListener;
    private List<Map<String, Object>> drawerCommunityNotifications = new ArrayList<>();

    private void startCommunityNotificationsListener() {
        if (communityNotifListener != null) communityNotifListener.remove();
        String currentUserId = sharedPreferences.getString("current_user_id", "anon");
        View badgeDot = findViewById(R.id.badgeDrawerNotificationDot);

        communityNotifListener = FirebaseHelper.listenCommunityNotifications(currentUserId, (list, unreadCount) -> {
            runOnUiThread(() -> {
                drawerCommunityNotifications = list != null ? list : new ArrayList<>();
                if (badgeDot != null) {
                    badgeDot.setVisibility(unreadCount > 0 ? View.VISIBLE : View.GONE);
                }
            });
        });
    }

    private void showCommunityNotificationsDialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_community_notifications, null);
        RecyclerView recycler = v.findViewById(R.id.recyclerCommunityNotifications);
        TextView textNoNotifs = v.findViewById(R.id.textNoCommunityNotifications);
        MaterialButton btnClear = v.findViewById(R.id.btnClearNotifications);
        MaterialButton btnClose = v.findViewById(R.id.btnCloseNotificationsDialog);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(v)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        String currentUserId = sharedPreferences.getString("current_user_id", "anon");

        List<String> unreadIds = new ArrayList<>();
        if (drawerCommunityNotifications != null) {
            for (Map<String, Object> item : drawerCommunityNotifications) {
                Boolean isRead = (Boolean) item.get("isRead");
                String docId = (String) item.get("docId");
                if ((Boolean.FALSE.equals(isRead) || isRead == null) && docId != null) {
                    unreadIds.add(docId);
                }
            }
        }
        if (!unreadIds.isEmpty()) {
            FirebaseHelper.markCommunityNotificationsAsRead(unreadIds);
        }
        View badgeDot = findViewById(R.id.badgeDrawerNotificationDot);
        if (badgeDot != null) badgeDot.setVisibility(View.GONE);

        if (recycler != null) {
            recycler.setLayoutManager(new LinearLayoutManager(this));
            CommunityNotificationsAdapter adapter = new CommunityNotificationsAdapter(drawerCommunityNotifications, notifMap -> {
                dialog.dismiss();
                Double latObj = (Double) notifMap.get("latitude");
                Double lonObj = (Double) notifMap.get("longitude");
                String qName = (String) notifMap.get("quadraName");
                String qNeigh = (String) notifMap.get("quadraNeighborhood");
                String qType = (String) notifMap.get("type");

                if (latObj != null && lonObj != null && latObj != 0 && lonObj != 0) {
                    double lat = latObj;
                    double lon = lonObj;

                    if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    }
                    int pos = ((ViewPagerAdapter) viewPager.getAdapter()).getPositionForId(R.id.nav_maps);
                    viewPager.setCurrentItem(pos, true);

                    focusQuadraOnRouteFragment(qName, qNeigh, lat, lon, qType);
                }
            });
            recycler.setAdapter(adapter);
            if (textNoNotifs != null) {
                textNoNotifs.setVisibility(drawerCommunityNotifications == null || drawerCommunityNotifications.isEmpty() ? View.VISIBLE : View.GONE);
            }
        }

        if (btnClear != null) {
            btnClear.setOnClickListener(view -> {
                FirebaseHelper.clearCommunityNotifications(currentUserId);
                drawerCommunityNotifications.clear();
                if (recycler != null && recycler.getAdapter() != null) {
                    recycler.getAdapter().notifyDataSetChanged();
                }
                if (textNoNotifs != null) textNoNotifs.setVisibility(View.VISIBLE);
            });
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(view -> dialog.dismiss());
        }

        dialog.show();
    }

    private void focusQuadraOnRouteFragment(String qName, String qNeigh, double lat, double lon, String qType) {
        List<Fragment> frags = getSupportFragmentManager().getFragments();
        for (Fragment f : frags) {
            if (f instanceof MapParentFragment) {
                List<Fragment> childFrags = f.getChildFragmentManager().getFragments();
                for (Fragment cf : childFrags) {
                    if (cf instanceof RouteFragment) {
                        ((RouteFragment) cf).focusQuadraFromNotification(qName, qNeigh, lat, lon, qType);
                        return;
                    }
                }
            } else if (f instanceof RouteFragment) {
                ((RouteFragment) f).focusQuadraFromNotification(qName, qNeigh, lat, lon, qType);
                return;
            }
        }
    }

    private static class CommunityNotificationsAdapter extends RecyclerView.Adapter<CommunityNotificationsAdapter.ViewHolder> {
        private final List<Map<String, Object>> items;
        private final OnNotificationClickListener clickListener;

        public interface OnNotificationClickListener {
            void onClick(Map<String, Object> item);
        }

        public CommunityNotificationsAdapter(List<Map<String, Object>> items, OnNotificationClickListener clickListener) {
            this.items = items != null ? items : new ArrayList<>();
            this.clickListener = clickListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_community_notification, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> item = items.get(position);
            String action = (String) item.get("actionType");
            String senderName = (String) item.get("senderName");
            String qName = (String) item.get("quadraName");
            String comment = (String) item.get("commentText");
            String qType = (String) item.get("type");
            Boolean isRead = (Boolean) item.get("isRead");
            Timestamp ts = (Timestamp) item.get("timestamp");

            boolean isBloco = "BLOCO".equalsIgnoreCase(qType);
            String typeLabel = isBloco ? "seu bloco" : "sua quadra";

            String icon = "🔔";
            String msg = (senderName != null ? senderName : "Alguém") + " interagiu com " + typeLabel + " " + (qName != null ? qName : "");

            if ("LIKE".equals(action)) {
                icon = "👍";
                msg = (senderName != null ? senderName : "Alguém") + " curtiu " + typeLabel + " " + (qName != null ? qName : "");
            } else if ("DISLIKE".equals(action)) {
                icon = "👎";
                msg = (senderName != null ? senderName : "Alguém") + " deu deslike em " + typeLabel + " " + (qName != null ? qName : "");
            } else if ("COMMENT".equals(action)) {
                icon = "💬";
                msg = (senderName != null ? senderName : "Alguém") + " comentou em " + typeLabel + " " + (qName != null ? qName : "");
            }

            holder.textIcon.setText(icon);
            holder.textMessage.setText(msg);

            if (comment != null && !comment.trim().isEmpty() && "COMMENT".equals(action)) {
                holder.textDetail.setText("\"" + comment.trim() + "\"");
                holder.textDetail.setVisibility(View.VISIBLE);
            } else {
                holder.textDetail.setVisibility(View.GONE);
            }

            String timeStr = "Agora há pouco";
            if (ts != null) {
                long diffMs = System.currentTimeMillis() - ts.toDate().getTime();
                long mins = diffMs / (1000 * 60);
                long hours = mins / 60;
                long days = hours / 24;
                if (days > 0) timeStr = "Há " + days + (days == 1 ? " dia" : " dias");
                else if (hours > 0) timeStr = "Há " + hours + (hours == 1 ? " hora" : " horas");
                else if (mins > 0) timeStr = "Há " + mins + (mins == 1 ? " minuto" : " minutos");
            }
            holder.textTime.setText(timeStr);

            holder.viewDot.setVisibility(Boolean.FALSE.equals(isRead) || isRead == null ? View.VISIBLE : View.GONE);

            holder.itemView.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onClick(item);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textIcon, textMessage, textDetail, textTime;
            View viewDot;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                textIcon = itemView.findViewById(R.id.textNotificationIcon);
                textMessage = itemView.findViewById(R.id.textNotificationMessage);
                textDetail = itemView.findViewById(R.id.textNotificationDetail);
                textTime = itemView.findViewById(R.id.textNotificationTime);
                viewDot = itemView.findViewById(R.id.viewUnreadDot);
            }
        }
    }

    private static class DrawerRouteAdapter extends RecyclerView.Adapter<DrawerRouteAdapter.ViewHolder> {
        private final List<RouteHeader> routes; private final OnRouteActionListener listener;
        private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault());
        interface OnRouteActionListener { void onClick(RouteHeader header); void onEdit(RouteHeader header); void onDelete(RouteHeader header); }
        DrawerRouteAdapter(List<RouteHeader> routes, OnRouteActionListener listener) { this.routes = routes; this.listener = listener; }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) { return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_drawer_route, parent, false)); }
        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            RouteHeader r = routes.get(position); holder.textName.setText(r.name); holder.textDate.setText(sdf.format(new Date(r.date)));
            holder.itemView.setOnClickListener(v -> listener.onClick(r)); holder.btnEdit.setOnClickListener(v -> listener.onEdit(r)); holder.btnDelete.setOnClickListener(v -> listener.onDelete(r));
        }
        @Override public int getItemCount() { return routes.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder { TextView textName, textDate; ImageButton btnEdit, btnDelete; ViewHolder(View v) { super(v); textName = v.findViewById(R.id.textDrawerRouteName); textDate = v.findViewById(R.id.textDrawerRouteDate); btnEdit = v.findViewById(R.id.btnEditRoute); btnDelete = v.findViewById(R.id.btnDeleteRoute); } }
    }

    private boolean isUserAuthenticated() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return false;
        // Só consideramos autenticado se o setup inicial do splash/backup foi concluído
        return sharedPreferences.getBoolean("first_setup_splash_done", false);
    }

    public void applyAppTheme(int themeIndex) {
        if (currentThemeIndex == themeIndex) return;
        
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
        int oldIndex = currentThemeIndex;
        currentThemeIndex = themeIndex;
        
        // Se a atividade já foi criada (oldIndex != -1), recria para aplicar as cores imediatamente
        if (oldIndex != -1) {
            recreate();
        }
    }

    public boolean isSystemUIVisible() { return isSystemUIVisible; }

    public void setSystemUIVisible(boolean visible) {
        if (this.isSystemUIVisible == visible) return;
        this.isSystemUIVisible = visible;
        refreshTabs();
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
            if (viewPager.getCurrentItem() == pos) {
                // Notifica os fragmentos diretamente se já estiver na aba de mapas
                notifyRouteFragmentsOfRecording(getSupportFragmentManager(), routeId);
            } else {
                viewPager.setCurrentItem(pos, true);
            }
        }
    }

    public void showQuadraOnMap(double lat, double lon, String quadraName) {
        if (viewPager != null && viewPager.getAdapter() instanceof ViewPagerAdapter) {
            int pos = ((ViewPagerAdapter) viewPager.getAdapter()).getPositionForId(R.id.nav_maps);
            if (viewPager.getCurrentItem() != pos) {
                viewPager.setCurrentItem(pos, true);
            }
        }
        notifyRouteFragmentsOfQuadra(getSupportFragmentManager(), lat, lon, quadraName);
    }

    private void notifyRouteFragmentsOfQuadra(FragmentManager fm, double lat, double lon, String quadraName) {
        if (fm == null) return;
        for (Fragment f : fm.getFragments()) {
            if (f instanceof RouteFragment) {
                ((RouteFragment) f).focusOnQuadra(lat, lon, quadraName);
            }
            if (f != null && f.getChildFragmentManager() != null) {
                notifyRouteFragmentsOfQuadra(f.getChildFragmentManager(), lat, lon, quadraName);
            }
        }
    }

    private void notifyRouteFragmentsOfRecording(FragmentManager fm, int routeId) {
        if (fm == null) return;
        for (Fragment f : fm.getFragments()) {
            if (f instanceof RouteFragment) {
                ((RouteFragment) f).handleRequestedRecording(routeId);
            }
            if (f != null && f.getChildFragmentManager() != null) {
                notifyRouteFragmentsOfRecording(f.getChildFragmentManager(), routeId);
            }
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
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    public void openTrackingHistory() {
        KmParentFragment fragment = new KmParentFragment();
        openFragmentInSettings(fragment, "KM Diário");
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            fragment.switchToHistory();
        }, 200);
    }

    public void openManualKmHistory() {
        KmParentFragment fragment = new KmParentFragment();
        openFragmentInSettings(fragment, "KM Diário");
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            fragment.switchToManualHistory();
        }, 200);
    }
}
