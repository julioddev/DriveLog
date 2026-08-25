package com.example.drivelog;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.PopupMenu;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import nl.dionsegijn.konfetti.core.Party;
import nl.dionsegijn.konfetti.core.PartyFactory;
import nl.dionsegijn.konfetti.core.Position;
import nl.dionsegijn.konfetti.core.emitter.Emitter;
import nl.dionsegijn.konfetti.core.emitter.EmitterConfig;
import nl.dionsegijn.konfetti.core.models.Shape;
import nl.dionsegijn.konfetti.core.models.Size;
import nl.dionsegijn.konfetti.xml.KonfettiView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RouteFragment extends Fragment {

    private MapView map = null;
    private IMapController mapController;
    private EditText editSearch, editSearchStops;
    private View cardSearch, layoutSearchBalloonOuter, layoutSearchBalloonInner, layoutSearchContainer;
    private ImageButton btnToggleSearch, btnSearch, btnAddStopManual, btnRouteMenu, btnToggleSearchStops;
    private View layoutOpenDrawerInside;
    private FloatingActionButton fabAddStop, fabNewRoute, fabCenterMap, fabDeliveryApp, fabMapOrientation, fabReportHazard, fabKmTracking;
    private boolean isMapFollowingHeading = false;
    private android.hardware.SensorManager sensorManager;
    private android.hardware.Sensor rotationVectorSensor;
    private float currentAzimuth = 0;
    private final android.hardware.SensorEventListener compassListener = new android.hardware.SensorEventListener() {
        @Override
        public void onSensorChanged(android.hardware.SensorEvent event) {
            if (event.sensor.getType() == android.hardware.Sensor.TYPE_ROTATION_VECTOR) {
                float[] rotationMatrix = new float[9];
                float[] outR = new float[9];
                android.hardware.SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
                
                // 🔥 Remapeia o sistema de coordenadas para compensar a inclinação do dispositivo (celular de pé no suporte)
                android.hardware.SensorManager.remapCoordinateSystem(rotationMatrix, 
                        android.hardware.SensorManager.AXIS_X, 
                        android.hardware.SensorManager.AXIS_Z, 
                        outR);

                float[] orientation = new float[3];
                android.hardware.SensorManager.getOrientation(outR, orientation);
                float azimuthDegrees = (float) Math.toDegrees(orientation[0]);
                if (azimuthDegrees < 0) azimuthDegrees += 360;

                // Suavização (filtro passa-baixa) mais robusta para eliminar tremidinha
                float alpha = 0.05f;
                float diff = azimuthDegrees - currentAzimuth;
                if (diff > 180) diff -= 360;
                else if (diff < -180) diff += 360;
                
                // 🔥 Filtro de Limiar: Ignora variações minúsculas (ruído)
                if (Math.abs(diff) < 1.0f) return;

                currentAzimuth = currentAzimuth + alpha * diff;
                if (currentAzimuth < 0) currentAzimuth += 360;
                if (currentAzimuth >= 360) currentAzimuth -= 360;

                if (isMapFollowingHeading && isMapFocusedOnUser && map != null) {
                    map.setMapOrientation(-currentAzimuth);
                    if (userDirectionMarker != null) userDirectionMarker.setRotation(0); 
                } else if (userDirectionMarker != null) {
                    userDirectionMarker.setRotation(currentAzimuth);
                    // 🔥 Importante: Força a atualização do mapa para o marcador girar mesmo com o mapa fixo
                    if (map != null) map.invalidate();
                }
            }
        }
        @Override public void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) {}
    };
    private RecyclerView recyclerSuggestions;
    private SuggestionsAdapter suggestionsAdapter;
    private ViewPager2 viewPagerStops;
    private StopsCardAdapter stopsCardAdapter;
    private MaterialCardView cardNavigationMode;
    private ImageView imageNavManeuver;
    private TextView textNavDistance, textNavInstruction, textNavTotalTime;
    private RecyclerView recyclerAllStops;
    private StopsListAdapter stopsListAdapter;
    private TextView textSuccessCount, textFailedCount, textPendingCount, textSuccessPackageCount, textPendingPackageCount;
    private TextView textWeatherTemp, textWeatherCity, textRouteTotalTime, textSheetHeader;
    private View cardWeatherSummary, cardRouteTotalTime, cardSuccessSummary, cardFailedSummary, cardPendingSummary, layoutStatsGroup, btnToggleStatsSummary;
    private ImageView imageWeatherIcon, imageHomeWarning, imageToggleStatsSummary;
    private boolean isStopDeleteDialogShowing = false;
    private final Handler autoHideHandler = new Handler(Looper.getMainLooper());
    private Runnable autoHideRunnable;
    private List<LoadingPoint> loadingPoints = new ArrayList<>();
    private final List<Marker> loadingMarkers = new ArrayList<>();
    private org.osmdroid.views.overlay.MapEventsOverlay loadingSelectionOverlay;
    private String lastCityName = "";
    private GeoPoint lastWeatherLocation = null;
    private long lastWeatherUpdate = 0;
    private List<DayWeather> lastWeekWeather = new ArrayList<>();

    private static class DayWeather {
        String label;
        List<HourlyWeather> hourly;
        int dominantCode;
        DayWeather(String l, List<HourlyWeather> h, int dc) { label = l; hourly = h; dominantCode = dc; }
    }

    private static class HourlyWeather {
        String time;
        double temp;
        int code;
        HourlyWeather(String t, double te, int c) { time = t; temp = te; code = c; }
    }
    private View bottomSheet, layoutSheetHeader;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private MaterialButton btnEditList, btnCreateGroup, btnUnifyManual, btnStartLassoDraw, btnUndoLasso, btnExitLasso;
    private com.google.android.material.materialswitch.MaterialSwitch switchTraceLine;
    private TextView textSwitchDistance;
    private boolean isEditMode = false;
    private boolean isUnifyMode = false;
    private boolean isLassoMode = false;
    private boolean isLassoDrawingEnabled = false;
    private int lassoGroupCounter = 0;
    private ItemTouchHelper itemTouchHelper;
    private MyLocationNewOverlay locationOverlay;
    private Marker homeMarker, userDirectionMarker;
    private org.osmdroid.views.overlay.Polygon homeRadiusOverlay;
    private Polyline selectionTracePolyline;
    private org.osmdroid.views.overlay.MapEventsOverlay currentFixOverlay, homeSelectionOverlay;
    private LassoOverlay lassoOverlay;
    private View cardFixMode, cardLassoMode, layoutSideFabs;
    private View layoutSummary, layoutLeftSummary, layoutSwitchContainer;
    private View cardToggleSystemUI;
    private ImageView imageToggleArrow;
    private View cardRestWarning;
    private View btnDisableRestInRoute;
    private List<NavInstruction> lastRouteInstructions = new ArrayList<>();
    
    private TextToSpeech tts;
    private KonfettiView konfettiView;

    private RouteStop currentlySelectedStop = null;
    private long lastTraceUpdate = 0;
    private GeoPoint lastTraceLocation = null;

    private com.google.firebase.firestore.ListenerRegistration comboioListener;
    private com.google.firebase.firestore.ListenerRegistration hazardListener;
    private final Map<String, Marker> friendMarkers = new HashMap<>();
    private final Map<String, Marker> hazardMarkers = new HashMap<>();

    private FusedLocationProviderClient fusedLocationClient;
    private GeoPoint currentLocation = new GeoPoint(-23.5505, -46.6333); 
    private GeoPoint lastSearchedPoint = null;
    private String lastSearchedAddress = "";
    private RouteHeader currentRouteHeader = null;
    
    // --- Linha do Tempo / Histórico GPS ---
    private View cardTimeline;
    private SeekBar seekBarTimeline;
    private TextView textTimelineTime;
    private TextView btnS1, btnS2, btnS4, btnS8;
    private View btnExitHistory;
    private ImageView btnTimelinePlayPause;
    private Marker timelineMarker;
    private List<RoutePoint> historicalPoints = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault());
    private boolean isTimelinePlaying = false;
    private int timelineSpeedMultiplier = 1;
    private final Handler playbackHandler = new Handler(Looper.getMainLooper());
    private final Runnable playbackRunnable = new Runnable() {
        @Override public void run() {
            if (isTimelinePlaying && seekBarTimeline != null) {
                int currentProgress = seekBarTimeline.getProgress();
                if (currentProgress < seekBarTimeline.getMax()) {
                    int nextProgress = currentProgress + 1;
                    seekBarTimeline.setProgress(nextProgress);
                    updateTimelineMarker(nextProgress);
                    long delay = 200 / timelineSpeedMultiplier;
                    playbackHandler.postDelayed(this, delay);
                } else pauseTimelinePlayback();
            }
        }
    };
    
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAdded() && currentRouteHeader != null && currentRouteHeader.startTime > 0) {
                long elapsed;
                if (currentRouteHeader.endTime > 0) {
                    elapsed = currentRouteHeader.endTime - currentRouteHeader.startTime - currentRouteHeader.totalPausedMs;
                } else {
                    long now = System.currentTimeMillis();
                    long currentPausedMs = currentRouteHeader.totalPausedMs + 
                        (currentRouteHeader.lastPauseStartTime > 0 ? (now - currentRouteHeader.lastPauseStartTime) : 0);
                    elapsed = now - currentRouteHeader.startTime - currentPausedMs;
                }
                
                long s = elapsed / 1000;
                long m = s / 60;
                long h_val = m / 60;
                String time = String.format(Locale.getDefault(), "%02d:%02d:%02d", h_val, m % 60, s % 60);

                boolean timerOnCardsOnly = sharedPreferences.getBoolean("timer_on_cards_only", false);

                if (textRouteTotalTime != null) textRouteTotalTime.setText(time);
                
                boolean homeDefined = sharedPreferences.getFloat("home_lat", 0) != 0;
                if (imageHomeWarning != null) {
                    imageHomeWarning.setVisibility(homeDefined ? View.GONE : View.VISIBLE);
                }

                if (cardRouteTotalTime != null) {
                    if (timerOnCardsOnly) {
                        // Se estiver no modo "Apenas nos Cards", só mostra o balão se a rota estiver finalizada
                        cardRouteTotalTime.setVisibility(currentRouteHeader.endTime > 0 ? View.VISIBLE : View.GONE);
                    } else {
                        // Caso contrário, mostra sempre que estiver rodando
                        cardRouteTotalTime.setVisibility(View.VISIBLE);
                    }
                }

                // 🔥 Notifica os cards para atualizar o tempo individual
                if (viewPagerStops != null && stopsCardAdapter != null) {
                    stopsCardAdapter.notifyItemRangeChanged(0, currentStops.size(), "TIMER_UPDATE");
                }
            } else {
                if (cardRouteTotalTime != null) cardRouteTotalTime.setVisibility(View.GONE);
            }
            timerHandler.postDelayed(this, 1000);
        }
    };
    private boolean isSelectingSuggestion = false;
    private List<RouteStop> currentStops = new ArrayList<>();
    private int currentRouteId = -1;
    private int pendingRestoreIndex = -1;
    private boolean isPositionRestored = false;
    private androidx.lifecycle.LiveData<List<RouteStop>> currentStopsLive;
    private androidx.lifecycle.LiveData<List<RouteGroup>> currentGroupsLive;
    private androidx.lifecycle.LiveData<RouteHeader> currentHeaderLive;

    private static final String PREF_LAST_ROUTE = "last_opened_route_id";
    private static final String PREF_LAST_STOP_PREFIX = "last_stop_index_";
    private static final String STATE_CURRENT_STOP_INDEX = "current_stop_index";

    private boolean isMapFocusedOnUser = true;
    private boolean shouldFocusOnFirstStop = false;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private android.net.ConnectivityManager.NetworkCallback networkCallback;

    private ImageButton btnOpenDrawer;
    private SharedPreferences sharedPreferences;
    private OnlineTileSourceBase currentMapSource;
    private final SharedPreferences.OnSharedPreferenceChangeListener prefListener = (prefs, key) -> {
        if (PREF_LAST_ROUTE.equals(key)) {
            int lastId = prefs.getInt(PREF_LAST_ROUTE, -1);
            if (lastId != -1 && lastId != currentRouteId) loadLastRoute();
        } else if ("app_mode".equals(key) || "delivery_app_package".equals(key) || "side_fabs_alignment".equals(key) || "show_bottom_sheet_stops".equals(key) || "show_fab_km_tracking".equals(key)) {
            Activity activity = getActivity(); if (activity != null) activity.runOnUiThread(this::updateAppModeUI);
        } else if ("route_line_opacity".equals(key)) {
            if (currentlySelectedStop != null) updateSelectionTrace(currentlySelectedStop);
        } else if ("user_map_icon".equals(key)) {
            Activity activity = getActivity(); if (activity != null) activity.runOnUiThread(this::setupLocationOverlay);
        } else if ("map_tile_style".equals(key)) {
            Activity activity = getActivity(); if (activity != null) activity.runOnUiThread(this::applyMapStyle);
        }
    };

    private void applyMapStyle() {
        if (map == null || getContext() == null) return;
        int style = sharedPreferences.getInt("map_tile_style", 0);
        
        switch (style) {
            case 1: // Satélite (Esri) - Requer inversão de X/Y
                currentMapSource = new XYTileSource("Esri_Satellite_v142", 0, 18, 256, ".jpg", 
                    new String[] { "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/" }) {
                    @Override
                    public String getTileURLString(long pMapTileIndex) {
                        return getBaseUrl() + MapTileIndex.getZoom(pMapTileIndex) + "/" + MapTileIndex.getY(pMapTileIndex) + "/" + MapTileIndex.getX(pMapTileIndex) + mImageFilenameEnding;
                    }
                };
                break;
            case 2: // Vias Urbanas (CartoDB)
                currentMapSource = new XYTileSource("CartoDB_Voyager_v142", 0, 20, 256, ".png", 
                    new String[] { "https://a.basemaps.cartocdn.com/rastertiles/voyager/", "https://b.basemaps.cartocdn.com/rastertiles/voyager/", "https://c.basemaps.cartocdn.com/rastertiles/voyager/" });
                break;
            case 3: // OpenTopoMap
                currentMapSource = new XYTileSource("OpenTopo_v142", 0, 17, 256, ".png", 
                    new String[] { "https://a.tile.opentopomap.org/", "https://b.tile.opentopomap.org/", "https://c.tile.opentopomap.org/" });
                break;
            case 4: // Satélite Híbrido (Google)
                currentMapSource = new XYTileSource("Google_Hybrid_v142", 0, 19, 256, ".png", 
                    new String[] { "https://mt0.google.com/vt/lyrs=y&x=", "https://mt1.google.com/vt/lyrs=y&x=", "https://mt2.google.com/vt/lyrs=y&x=", "https://mt3.google.com/vt/lyrs=y&x=" }) {
                    @Override
                    public String getTileURLString(long pMapTileIndex) {
                        return getBaseUrl() + MapTileIndex.getX(pMapTileIndex) + "&y=" + MapTileIndex.getY(pMapTileIndex) + "&z=" + MapTileIndex.getZoom(pMapTileIndex);
                    }
                };
                break;
            default: // Padrão (OSM)
                currentMapSource = new XYTileSource("DriveLog_Map_v142", 0, 19, 256, ".png",
                    new String[] { "https://a.tile.openstreetmap.org/", "https://b.tile.openstreetmap.org/", "https://c.tile.openstreetmap.org/" });
                break;
        }
        
        map.setTileSource(currentMapSource);
        map.invalidate();
    }

    private void updateAppModeUI() { updateAppModeUI(getView()); }

    private void updateAppModeUI(View root) {
        if (getContext() == null || root == null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        int subType = prefs.getInt("sub_type", 2);
        int appMode = (subType == 0 && (System.currentTimeMillis() - prefs.getLong("install_date", System.currentTimeMillis()) > (7L * 24 * 60 * 60 * 1000)))
                ? 1 : prefs.getInt("app_mode", 0);
        boolean isMapsOnly = appMode == 1;
        
        if (btnOpenDrawer != null) {
            btnOpenDrawer.setVisibility(isMapsOnly ? View.VISIBLE : View.GONE);
        }
        
        if (layoutOpenDrawerInside != null) {
            layoutOpenDrawerInside.setVisibility(isMapsOnly ? View.VISIBLE : View.GONE);
        }
        
        if (fabNewRoute != null) {
            fabNewRoute.setVisibility(isMapsOnly ? View.GONE : View.VISIBLE);
        }
        
        updateDeliveryAppFab();

        View searchContainer = root.findViewById(R.id.layoutSearchContainer);
        View bs = root.findViewById(R.id.bottomSheetStops);
        View vp = root.findViewById(R.id.viewPagerStops);

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            
            // Verificamos se há propaganda (mesma lógica da MainActivity)
            int adHeight = 0;
            if (subType == 0) {
                long installDate = prefs.getLong("install_date", System.currentTimeMillis());
                if (System.currentTimeMillis() - installDate > (7L * 24 * 60 * 60 * 1000)) {
                    adHeight = (int) (55 * getResources().getDisplayMetrics().density);
                }
            }

            // A MainActivity já aplica recuos dependendo do modo.
            // Precisamos compensar apenas o que ela NÃO faz no container do ViewPager.
            
            if (searchContainer != null) {
                boolean isAppBarHidden = isMapsOnly;
                if (getActivity() instanceof MainActivity) {
                    isAppBarHidden = !((MainActivity) getActivity()).isSystemUIVisible() || isMapsOnly;
                }
                
                // Reduzido para 10dp conforme solicitado
                int marginDp = (int) (10 * getResources().getDisplayMetrics().density);
                int topPadding = (isAppBarHidden ? systemBars.top : 0) + marginDp;
                searchContainer.setPadding(searchContainer.getPaddingLeft(), topPadding, searchContainer.getPaddingRight(), searchContainer.getPaddingBottom());
            }

            // O ViewPager na MainActivity é empurrado pela AdView (via margin) ou pelo BottomNav.
            // O fragmento "vaza" para trás dos botões no modo Mapa SEM propaganda OU no modo Imersivo.
            int bottomOffset = 0;
            boolean isImmersiveActive = false;
            if (getActivity() instanceof MainActivity) {
                isImmersiveActive = !((MainActivity) getActivity()).isSystemUIVisible();
            }

            if ((isMapsOnly || isImmersiveActive) && adHeight == 0) {
                bottomOffset = systemBars.bottom;
            }

            if (bs != null) {
                BottomSheetBehavior<View> b = BottomSheetBehavior.from(bs);
                
                // Espaçador dinâmico
                View spacer = root.findViewById(R.id.viewBottomSpacer);
                if (spacer != null) {
                    ViewGroup.LayoutParams lp = spacer.getLayoutParams();
                    lp.height = bottomOffset;
                    spacer.setLayoutParams(lp);
                }

                // Ajustamos o peekHeight para garantir visibilidade total do card
                int bp = (int) (200 * getResources().getDisplayMetrics().density) + bottomOffset;
                b.setPeekHeight(bp, true);
                
                root.setTag(R.id.layoutSideFabs, bottomOffset);
            }
            
            if (vp != null) {
                // Removemos o padding do ViewPager para não duplicar o espaço do spacer
                vp.setPadding(vp.getPaddingLeft(), vp.getPaddingTop(), vp.getPaddingRight(), 0);
            }
            
            if (layoutSideFabs != null) {
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams lp = (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) layoutSideFabs.getLayoutParams();
                
                // 🔥 Lógica de Alinhamento dos Botões Flutuantes (Preferência do usuário ou Auto)
                String alignment = sharedPreferences.getString("side_fabs_alignment", "auto");
                boolean shouldBeOnRight;
                
                if ("left".equals(alignment)) {
                    shouldBeOnRight = false;
                } else if ("right".equals(alignment)) {
                    shouldBeOnRight = true;
                } else {
                    // Modo Auto: Direita no modo Mapa, Esquerda no modo Completo
                    shouldBeOnRight = isMapsOnly;
                }

                if (shouldBeOnRight) {
                    lp.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET;
                    lp.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
                    lp.setMarginEnd((int) (16 * getResources().getDisplayMetrics().density));
                    lp.setMarginStart(0);
                } else {
                    lp.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET;
                    lp.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
                    lp.setMarginStart((int) (16 * getResources().getDisplayMetrics().density));
                    lp.setMarginEnd(0);
                }
                layoutSideFabs.setLayoutParams(lp);
            }
            
            if (cardToggleSystemUI != null) {
                cardToggleSystemUI.setVisibility(isMapsOnly ? View.GONE : View.VISIBLE);
                
                if (getActivity() instanceof MainActivity) {
                    MainActivity activity = (MainActivity) getActivity();
                    boolean isVisible = activity.isSystemUIVisible();
                    if (imageToggleArrow != null) {
                        imageToggleArrow.setImageResource(isVisible ? R.drawable.ic_arrow_down : R.drawable.ic_arrow_up);
                    }
                    
                    // Se estiver no modo imersivo (menus ocultos), o botão também precisa subir para desviar da barra do sistema
                    ViewGroup.MarginLayoutParams lp2 = (ViewGroup.MarginLayoutParams) cardToggleSystemUI.getLayoutParams();
                    lp2.bottomMargin = isVisible ? 0 : bottomOffset;
                    cardToggleSystemUI.setLayoutParams(lp2);
                }
            }

            v.post(this::updateFabsPosition);
            v.post(this::updateFloatingButtonsVisibility);
            return insets;
        });
        androidx.core.view.ViewCompat.requestApplyInsets(root);
    }

    private void closeSearchUI() {
        if (editSearch == null || cardSearch == null || getContext() == null) return;
        if (editSearch.getVisibility() == View.GONE) return;

        ViewGroup.LayoutParams lp = cardSearch.getLayoutParams();
        if (!(lp instanceof RelativeLayout.LayoutParams)) return;
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) lp;
        params.width = RelativeLayout.LayoutParams.WRAP_CONTENT;
        params.removeRule(RelativeLayout.ALIGN_PARENT_START);
        params.addRule(RelativeLayout.START_OF, R.id.btnToggleStatsSummary);
        params.removeRule(RelativeLayout.END_OF);
        cardSearch.setLayoutParams(params);
        
        if (layoutSearchBalloonOuter != null) layoutSearchBalloonOuter.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
        if (layoutSearchBalloonInner != null) layoutSearchBalloonInner.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;

        editSearch.setVisibility(View.GONE);
        btnSearch.setVisibility(View.GONE);
        if (btnToggleSearch != null) btnToggleSearch.setImageResource(android.R.drawable.ic_menu_search);
        editSearch.setText("");
        editSearch.clearFocus();
        
        if (recyclerSuggestions != null) recyclerSuggestions.setVisibility(View.GONE);

        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editSearch.getWindowToken(), 0);

        // Restaura botão do drawer se necessário
        updateAppModeUI();
    }

    private void updateFabsPosition() {
        if (layoutSideFabs == null || getContext() == null || getView() == null) return;
        
        // 🔥 Lógica: O card de paradas está visível apenas se houver paradas E a preferência estiver ativa
        boolean showStopsCard = sharedPreferences.getBoolean("show_bottom_sheet_stops", true);
        boolean hasStops = !currentStops.isEmpty();
        boolean isCardActive = hasStops && showStopsCard;
        
        int systemBottom = 0;
        Object tag = getView().getTag(R.id.layoutSideFabs);
        if (tag instanceof Integer) systemBottom = (Integer) tag;

        int mb;
        if (isCardActive) {
            // Se o card de paradas estiver ativo, os botões ficam ACIMA dele (margem maior)
            mb = (int) (220 * getResources().getDisplayMetrics().density) + systemBottom;
        } else {
            // Se NÃO estiver ativo, eles descem para perto da parte inferior da tela
            // No modo Mapa (app_mode 1), sistemaBottom cuida da barra de navegação. 
            // No modo Completo, eles ficam logo acima do menu inferior.
            mb = (int) (32 * getResources().getDisplayMetrics().density) + systemBottom;
        }

        ViewGroup.LayoutParams lp = layoutSideFabs.getLayoutParams();
        if (lp instanceof androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) {
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams p = (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) lp;
            p.bottomMargin = mb; 
            layoutSideFabs.setLayoutParams(p);
        }
    }

    private void promptHideFab(String prefKey, String label) {
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Ocultar Botão")
                .setMessage("Deseja ocultar o botão '" + label + "'?\n\nVocê pode ativá-lo novamente nas configurações de 'Visibilidade de Botões'.")
                .setPositiveButton("Ocultar", (d, which) -> {
                    sharedPreferences.edit().putBoolean(prefKey, false).apply();
                    updateFloatingButtonsVisibility();
                    Toast.makeText(getContext(), label + " ocultado", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog_rounded);
        dialog.show();
    }

    private void updateFloatingButtonsVisibility() {
        if (getContext() == null) return;
        
        // --- Cálculo do estado da rota ---
        int pendCount = 0;
        for (RouteStop s : currentStops) if (s.deliveryStatus == 0) pendCount++;
        boolean hasStops = !currentStops.isEmpty();
        boolean isRouteFinished = hasStops && pendCount == 0;
        boolean isRouteActive = hasStops && !isRouteFinished;

        boolean showDelivery = sharedPreferences.getBoolean("show_fab_delivery_app", true);
        boolean showReport = sharedPreferences.getBoolean("show_fab_report_hazard", true);
        boolean showCenter = sharedPreferences.getBoolean("show_fab_center_map", true);
        boolean showNorth = sharedPreferences.getBoolean("show_fab_orientation", true);
        boolean showKm = sharedPreferences.getBoolean("show_fab_km_tracking", true);
        boolean showStopsCard = sharedPreferences.getBoolean("show_bottom_sheet_stops", true);

        if (fabDeliveryApp != null) fabDeliveryApp.setVisibility(showDelivery ? View.VISIBLE : View.GONE);
        if (fabReportHazard != null) fabReportHazard.setVisibility(showReport ? View.VISIBLE : View.GONE);
        
        // 🔥 REGRA: O botão de alternar foco só aparece se a rota estiver ativa (não vazia e não finalizada)
        if (fabCenterMap != null) fabCenterMap.setVisibility((showCenter && isRouteActive) ? View.VISIBLE : View.GONE);
        
        if (fabMapOrientation != null) fabMapOrientation.setVisibility(showNorth ? View.VISIBLE : View.GONE);

        if (fabKmTracking != null) {
            boolean remoteVisible = getActivity() instanceof MainActivity && ((MainActivity) getActivity()).isMenuVisible("km");
            fabKmTracking.setVisibility((remoteVisible && showKm) ? View.VISIBLE : View.GONE);
        }
        
        if (fabNewRoute != null) {
            fabNewRoute.setVisibility((!hasStops || isRouteFinished) ? View.VISIBLE : View.GONE);
        }

        if (bottomSheet != null) {
            bottomSheet.setVisibility((!hasStops || !showStopsCard) ? View.GONE : View.VISIBLE);
        }
    }

    private void animateNumber(TextView textView, int targetValue) {
        if (textView == null) return;
        String currentText = textView.getText().toString();
        int initialValue = 0;
        try { initialValue = Integer.parseInt(currentText); } catch (Exception ignored) {}
        
        if (initialValue == targetValue) return;

        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofInt(initialValue, targetValue);
        animator.setDuration(800);
        animator.addUpdateListener(animation -> textView.setText(animation.getAnimatedValue().toString()));
        animator.start();
    }

    private final ActivityResultLauncher<Intent> importXlsxLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) processXlsxImport(result.getData().getData());
    });

    private final android.content.BroadcastReceiver newRouteReceiver = new android.content.BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { if ("com.example.entregas.ACTION_NEW_ROUTE".equals(intent.getAction())) promptNewRoute(); }
    };

    public void refreshBadges() {
        // Marcadores de pendência desativados
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        android.util.Log.d("DriveLog", "Iniciando onCreateView - " + System.currentTimeMillis());
        View view = inflater.inflate(R.layout.fragment_route, container, false);
        // view.setBackgroundColor(Color.RED); // TESTE RADICAL: SE O FUNDO FICAR VERMELHO, O CÓDIGO NOVO ESTÁ RODANDO
        sharedPreferences = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        sensorManager = (android.hardware.SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            rotationVectorSensor = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ROTATION_VECTOR);
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        
        // Removido Configuration.load() para não sobrescrever o User-Agent global da MainActivity
        
        tts = new TextToSpeech(requireContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("pt", "BR"));
                
                // Aplicar voz personalizada se existir
                String voiceName = sharedPreferences.getString("voice_name", null);
                if (voiceName != null) {
                    try {
                        for (Voice v : tts.getVoices()) {
                            if (v.getName().equals(voiceName)) {
                                tts.setVoice(v);
                                break;
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        });

        map = view.findViewById(R.id.mapRoute); 
        applyMapStyle();
        map.setMultiTouchControls(true);
        mapController = map.getController(); mapController.setZoom(15.0);
        sharedPreferences.registerOnSharedPreferenceChangeListener(prefListener);
        setupLocationOverlay();
        
        // Overlay para fechar busca ao clicar no mapa
        map.getOverlays().add(new org.osmdroid.views.overlay.MapEventsOverlay(new org.osmdroid.events.MapEventsReceiver() {
            @Override public boolean singleTapConfirmedHelper(org.osmdroid.util.GeoPoint p) {
                closeSearchUI();
                return false; 
            }
            @Override public boolean longPressHelper(org.osmdroid.util.GeoPoint p) { return false; }
        }));

        map.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_MOVE) {
                if (isMapFocusedOnUser) {
                    isMapFocusedOnUser = false;
                    isMapFollowingHeading = false;
                    map.setMapOrientation(0);
                    updateCenterFabIcon();
                    updateOrientationFabIcon();
                }
            }
            return v.performClick();
        });

        cardSearch = view.findViewById(R.id.cardSearch);
        layoutSearchContainer = view.findViewById(R.id.layoutSearchContainer);
        layoutSummary = view.findViewById(R.id.layoutSummary);
        layoutLeftSummary = view.findViewById(R.id.layoutLeftSummary);
        layoutSwitchContainer = view.findViewById(R.id.layoutSwitchContainer);
        layoutSearchBalloonOuter = view.findViewById(R.id.layoutSearchBalloonOuter);
        layoutSearchBalloonInner = view.findViewById(R.id.layoutSearchBalloonInner);
        btnToggleSearch = view.findViewById(R.id.btnToggleSearch);
        editSearch = view.findViewById(R.id.editSearchAddress); btnSearch = view.findViewById(R.id.btnSearchAddress);
        
        btnToggleSearch.setOnClickListener(v -> {
            ViewGroup.LayoutParams lp = cardSearch.getLayoutParams();
            if (!(lp instanceof RelativeLayout.LayoutParams)) return;
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) lp;
            
            if (editSearch.getVisibility() == View.GONE) {
                params.width = RelativeLayout.LayoutParams.MATCH_PARENT;
                params.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE);
                params.addRule(RelativeLayout.START_OF, R.id.btnToggleStatsSummary);
                params.removeRule(RelativeLayout.END_OF);
                cardSearch.setLayoutParams(params);
                
                if (layoutSearchBalloonOuter != null) layoutSearchBalloonOuter.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
                if (layoutSearchBalloonInner != null) layoutSearchBalloonInner.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
                
                editSearch.setVisibility(View.VISIBLE);
                btnSearch.setVisibility(View.VISIBLE);
                btnToggleSearch.setImageResource(R.drawable.ic_close);
                editSearch.requestFocus();
                
                // Oculta botão do drawer quando busca abre
                if (layoutOpenDrawerInside != null) layoutOpenDrawerInside.setVisibility(View.GONE);

                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(editSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            } else {
                closeSearchUI();
            }
        });

        btnAddStopManual = view.findViewById(R.id.btnAddStopManual); 
        btnOpenDrawer = view.findViewById(R.id.btnOpenRoutesDrawerInside);
        layoutOpenDrawerInside = view.findViewById(R.id.layoutOpenDrawerInside);

        btnRouteMenu = view.findViewById(R.id.btnRouteMenu); layoutSideFabs = view.findViewById(R.id.layoutSideFabs);
        updateAppModeUI(view);
        if (btnOpenDrawer != null) {
            btnOpenDrawer.setOnClickListener(v -> {
                if (v.getVisibility() == View.VISIBLE && getActivity() instanceof MainActivity) {
                    // Pequena animação de escala ao clicar para ser mais "suave"
                    v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction(() -> {
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                        ((MainActivity) getActivity()).openRoutesDrawer();
                    }).start();
                }
            });
        }
        fabNewRoute = view.findViewById(R.id.fabNewRoute); fabAddStop = view.findViewById(R.id.fabAddStop);
        fabCenterMap = view.findViewById(R.id.fabCenterMap); fabDeliveryApp = view.findViewById(R.id.fabDeliveryApp);
        fabReportHazard = view.findViewById(R.id.fabReportHazard);
        fabKmTracking = view.findViewById(R.id.fabKmTracking);

        if (fabNewRoute != null) { fabNewRoute.setOnClickListener(v -> promptNewRoute()); if (sharedPreferences.getInt("app_mode", 0) == 1) fabNewRoute.setVisibility(View.GONE); }
        if (fabAddStop != null) fabAddStop.setOnClickListener(v -> confirmAddStop());
        if (fabCenterMap != null) fabCenterMap.setOnClickListener(v -> toggleMapFocus());
        fabMapOrientation = view.findViewById(R.id.fabMapOrientation);
        if (fabMapOrientation != null) {
            fabMapOrientation.setOnClickListener(v -> toggleMapOrientation());
            fabMapOrientation.setOnLongClickListener(v -> { promptHideFab("show_fab_orientation", "Modo Norte"); return true; });
        }
        if (fabDeliveryApp != null) {
            fabDeliveryApp.setOnClickListener(v -> launchDeliveryApp());
            fabDeliveryApp.setOnLongClickListener(v -> { promptHideFab("show_fab_delivery_app", "App de Entrega"); return true; });
        }
        if (fabReportHazard != null) {
            fabReportHazard.setOnClickListener(v -> promptReportHazard());
            fabReportHazard.setOnLongClickListener(v -> { promptHideFab("show_fab_report_hazard", "Reportar Alerta"); return true; });
        }
        if (fabKmTracking != null) {
            fabKmTracking.setOnClickListener(v -> showKmTrackingPopup());
            fabKmTracking.setOnLongClickListener(v -> { promptHideFab("show_fab_km_tracking", "Atalho Rastreamento"); return true; });
            observeTrackingStatus();
        }

        // --- Inicialização da Linha do Tempo ---
        cardTimeline = view.findViewById(R.id.cardTimelineRoute);
        seekBarTimeline = view.findViewById(R.id.seekBarTimelineRoute);
        textTimelineTime = view.findViewById(R.id.textTimelineTimeRoute);
        btnExitHistory = view.findViewById(R.id.btnExitHistoryRoute);
        btnTimelinePlayPause = view.findViewById(R.id.btnTimelinePlayPauseRoute);
        btnS1 = view.findViewById(R.id.btnSpeed1xRoute);
        btnS2 = view.findViewById(R.id.btnSpeed2xRoute);
        btnS4 = view.findViewById(R.id.btnSpeed4xRoute);
        btnS8 = view.findViewById(R.id.btnSpeed8xRoute);

        if (btnExitHistory != null) btnExitHistory.setOnClickListener(v -> exitHistoryMode());
        if (btnTimelinePlayPause != null) btnTimelinePlayPause.setOnClickListener(v -> toggleTimelinePlayback());
        if (btnS1 != null) btnS1.setOnClickListener(v -> setTimelineSpeed(1));
        if (btnS2 != null) btnS2.setOnClickListener(v -> setTimelineSpeed(2));
        if (btnS4 != null) btnS4.setOnClickListener(v -> setTimelineSpeed(4));
        if (btnS8 != null) btnS8.setOnClickListener(v -> setTimelineSpeed(8));
        setupTimelineListener();

        if (btnSearch != null) btnSearch.setOnClickListener(v -> searchAddress(editSearch.getText().toString()));
        if (btnAddStopManual != null) btnAddStopManual.setOnClickListener(v -> promptManualStop());
        if (btnRouteMenu != null) {
            btnRouteMenu.setOnClickListener(v -> showRouteOptionsMenu(v));
        }
        
        cardToggleSystemUI = view.findViewById(R.id.cardToggleSystemUI);
        imageToggleArrow = view.findViewById(R.id.imageToggleArrow);
        if (cardToggleSystemUI != null) {
            cardToggleSystemUI.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    MainActivity activity = (MainActivity) getActivity();
                    boolean newState = !activity.isSystemUIVisible();
                    activity.setSystemUIVisible(newState);
                    
                    if (imageToggleArrow != null) {
                        imageToggleArrow.setImageResource(newState ? R.drawable.ic_arrow_down : R.drawable.ic_arrow_up);
                    }
                    
                    Toast.makeText(getContext(), newState ? "Menus visíveis" : "Modo Mapa Imersivo", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // Aplica a posição inicial dos FABs baseada no modo
        updateAppModeUI(view);
        
        recyclerSuggestions = view.findViewById(R.id.recyclerSuggestions); viewPagerStops = view.findViewById(R.id.viewPagerStops);
        recyclerAllStops = view.findViewById(R.id.recyclerAllStops); textSuccessCount = view.findViewById(R.id.textSuccessCount);
        textSuccessPackageCount = view.findViewById(R.id.textSuccessPackageCount); textFailedCount = view.findViewById(R.id.textFailedCount);
        textPendingCount = view.findViewById(R.id.textPendingCount); textPendingPackageCount = view.findViewById(R.id.textPendingPackageCount);
        textWeatherTemp = view.findViewById(R.id.textWeatherTemp); 
        textWeatherCity = view.findViewById(R.id.textWeatherCity);
        imageWeatherIcon = view.findViewById(R.id.imageWeatherIcon);
        cardWeatherSummary = view.findViewById(R.id.cardWeatherSummary);
        cardRouteTotalTime = view.findViewById(R.id.cardRouteTotalTime);
        textRouteTotalTime = view.findViewById(R.id.textRouteTotalTime);
        textSheetHeader = view.findViewById(R.id.textSheetHeader);
        imageHomeWarning = view.findViewById(R.id.imageHomeWarning);
        if (cardRouteTotalTime != null) {
            cardRouteTotalTime.setOnClickListener(v -> {
                PopupMenu p = new PopupMenu(requireContext(), v);
                
                boolean homeDefined = sharedPreferences.getFloat("home_lat", 0) != 0;
                if (!homeDefined) {
                    p.getMenu().add("Definir Endereço de Casa");
                }
                
                if (currentRouteHeader != null && currentRouteHeader.startTime > 0) {
                    p.getMenu().add("Estatísticas da Rota");
                }

                p.setOnMenuItemClickListener(item -> {
                    if ("Definir Endereço de Casa".equals(item.getTitle())) {
                        startHomeSelection();
                    } else if (item.getTitle().equals("Estatísticas da Rota")) {
                        showRouteStatsPopup();
                    }
                    return true;
                });
                p.show();
            });
        }
        if (cardWeatherSummary != null) {
            cardWeatherSummary.setOnClickListener(v -> {
                if (lastWeekWeather.isEmpty()) {
                    fetchWeather();
                    Toast.makeText(getContext(), "Carregando previsão...", Toast.LENGTH_SHORT).show();
                } else {
                    showWeatherHourlyPopup();
                }
            });
        }
        
        fetchWeather();
        bottomSheet = view.findViewById(R.id.bottomSheetStops); layoutSheetHeader = view.findViewById(R.id.layoutSheetHeader);
        btnEditList = view.findViewById(R.id.btnEditList); btnCreateGroup = view.findViewById(R.id.btnCreateGroup);
        btnUnifyManual = view.findViewById(R.id.btnUnifyManual);
        View btnCollapseSheet = view.findViewById(R.id.btnCollapseSheet);
        if (btnCollapseSheet != null) {
            btnCollapseSheet.setOnClickListener(v -> {
                if (bottomSheetBehavior != null) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            });
        }
        btnStartLassoDraw = view.findViewById(R.id.btnStartLassoDraw); btnUndoLasso = view.findViewById(R.id.btnUndoLasso);
        btnExitLasso = view.findViewById(R.id.btnExitLasso); cardFixMode = view.findViewById(R.id.cardFixMode);
        cardLassoMode = view.findViewById(R.id.cardLassoMode); cardRestWarning = view.findViewById(R.id.cardRestWarningRoute);
        cardNavigationMode = view.findViewById(R.id.cardNavigationMode);

        editSearchStops = view.findViewById(R.id.editSearchStops);
        btnToggleSearchStops = view.findViewById(R.id.btnToggleSearchStops);
        if (btnToggleSearchStops != null) {
            btnToggleSearchStops.setOnClickListener(v -> {
                if (editSearchStops.getVisibility() == View.GONE) {
                    editSearchStops.setVisibility(View.VISIBLE);
                    textSheetHeader.setVisibility(View.GONE);
                    btnToggleSearchStops.setImageResource(R.drawable.ic_close);
                    editSearchStops.requestFocus();
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(editSearchStops, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                } else {
                    editSearchStops.setVisibility(View.GONE);
                    textSheetHeader.setVisibility(View.VISIBLE);
                    btnToggleSearchStops.setImageResource(android.R.drawable.ic_menu_search);
                    editSearchStops.setText("");
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(editSearchStops.getWindowToken(), 0);
                }
            });
        }
        if (editSearchStops != null) {
            editSearchStops.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (stopsListAdapter != null) stopsListAdapter.filter(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
        if (cardNavigationMode != null) cardNavigationMode.setOnClickListener(v -> showRouteInstructionsPopup());
        konfettiView = view.findViewById(R.id.konfettiView);
        if (konfettiView != null) {
            android.util.Log.d("DriveLog", "KonfettiView encontrado com sucesso!");
        } else {
            android.util.Log.e("DriveLog", "KonfettiView NÃO encontrado no layout!");
        }
        imageNavManeuver = view.findViewById(R.id.imageNavManeuver);
        textNavDistance = view.findViewById(R.id.textNavDistance);
        textNavTotalTime = view.findViewById(R.id.textNavTotalTime);
        textNavInstruction = view.findViewById(R.id.textNavInstruction);
        btnDisableRestInRoute = view.findViewById(R.id.btnDisableRestInRoute);
        switchTraceLine = view.findViewById(R.id.switchTraceLine);
        textSwitchDistance = view.findViewById(R.id.textSwitchDistance);
        View layoutFakeThumb = view.findViewById(R.id.layoutFakeThumb);

        if (switchTraceLine != null) {
            boolean showTrace = sharedPreferences.getBoolean("show_trace_line", true);
            switchTraceLine.setChecked(showTrace);
            if (textSwitchDistance != null) textSwitchDistance.setVisibility(showTrace ? View.VISIBLE : View.GONE);
            if (layoutFakeThumb != null) layoutFakeThumb.setActivated(showTrace);
            
            if (layoutFakeThumb != null) {
                layoutFakeThumb.setOnClickListener(v -> switchTraceLine.toggle());
            }

            switchTraceLine.setOnCheckedChangeListener((v, isChecked) -> {
                if (isChecked && !NetworkHelper.isNetworkAvailable(getContext())) {
                    v.setChecked(false);
                    showNoConnectionPopup();
                    return;
                }
                sharedPreferences.edit().putBoolean("show_trace_line", isChecked).apply();
                animateNavigationCard(isChecked);
                
                if (textSwitchDistance != null) {
                    if (isChecked) {
                        textSwitchDistance.setVisibility(View.VISIBLE);
                        // Começa totalmente escondido no CENTRO do ícone (translação proporcional ao novo tamanho)
                        textSwitchDistance.setTranslationX(19f * getResources().getDisplayMetrics().density); 
                        textSwitchDistance.setAlpha(0f);
                        textSwitchDistance.animate()
                                .translationX(-5f * getResources().getDisplayMetrics().density) // Projeta para a esquerda
                                .alpha(1f)
                                .setDuration(300)
                                .setInterpolator(new android.transition.Explode().getInterpolator())
                                .start();
                    } else {
                        textSwitchDistance.animate()
                                .translationX(19f * getResources().getDisplayMetrics().density) // Volta para o centro exato
                                .alpha(1f)
                                .setDuration(250)
                                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                                .withEndAction(() -> {
                                    textSwitchDistance.setVisibility(View.INVISIBLE);
                                    textSwitchDistance.setAlpha(0f);
                                })
                                .start();
                    }
                }

                if (layoutFakeThumb != null) layoutFakeThumb.setActivated(isChecked);

                if (!isChecked && selectionTracePolyline != null) {
                    map.getOverlays().remove(selectionTracePolyline);
                    map.invalidate();
                } else if (isChecked && currentlySelectedStop != null) {
                    updateSelectionTrace(currentlySelectedStop);
                }
            });

            // Popup explicativo ao pressionar
            switchTraceLine.setOnLongClickListener(v -> {
                showTraceInfoPopup();
                return true;
            });
        }

        if (btnDisableRestInRoute != null) btnDisableRestInRoute.setOnClickListener(v -> disableRestAndGoToSettings());
        if (bottomSheet != null) bottomSheet.setVisibility(View.GONE);
        if (btnEditList != null) btnEditList.setOnClickListener(v -> toggleEditMode());
        if (btnCreateGroup != null) btnCreateGroup.setOnClickListener(v -> promptCreateGroup());
        if (btnUnifyManual != null) btnUnifyManual.setOnClickListener(v -> toggleUnifyMode());
        if (btnStartLassoDraw != null) btnStartLassoDraw.setOnClickListener(v -> startLassoDrawing());
        if (btnUndoLasso != null) btnUndoLasso.setOnClickListener(v -> undoLastLasso());
        if (btnExitLasso != null) btnExitLasso.setOnClickListener(v -> exitLassoMode());

        cardSuccessSummary = view.findViewById(R.id.cardSuccessSummary);
        cardFailedSummary = view.findViewById(R.id.cardFailedSummary);
        cardPendingSummary = view.findViewById(R.id.cardPendingSummary);
        if (cardSuccessSummary != null) cardSuccessSummary.setOnClickListener(v -> showStatsPopup(1));
        if (cardFailedSummary != null) cardFailedSummary.setOnClickListener(v -> showStatsPopup(2));
        if (cardPendingSummary != null) cardPendingSummary.setOnClickListener(v -> showStatsPopup(0));

        layoutStatsGroup = view.findViewById(R.id.layoutStatsGroup);
        btnToggleStatsSummary = view.findViewById(R.id.btnToggleStatsSummary);
        imageToggleStatsSummary = view.findViewById(R.id.imageToggleStatsSummary);

        if (btnToggleStatsSummary != null) {
            btnToggleStatsSummary.setOnClickListener(v -> toggleStatsSummary());
            
            // Estado inicial do SharedPreferences
            boolean isExpanded = sharedPreferences.getBoolean("stats_summary_expanded", true);
            if (!isExpanded) {
                if (layoutStatsGroup != null) layoutStatsGroup.setVisibility(View.GONE);
                if (imageToggleStatsSummary != null) imageToggleStatsSummary.setImageResource(R.drawable.ic_arrow_down);
            }
        }

        if (bottomSheet != null) {
            bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
            bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                @Override public void onStateChanged(@NonNull View bs, int ns) {
                    if (ns == BottomSheetBehavior.STATE_EXPANDED) { 
                        viewPagerStops.setVisibility(View.GONE); 
                        recyclerAllStops.setVisibility(View.VISIBLE); 
                        layoutSheetHeader.setVisibility(View.VISIBLE); 
                        bs.setBackgroundColor(Color.WHITE);

                        // Sincroniza a lista com a parada selecionada no mapa (coloca no topo)
                        int current = viewPagerStops.getCurrentItem();
                        if (current >= 0 && current < currentStops.size()) {
                            if (recyclerAllStops.getLayoutManager() instanceof LinearLayoutManager) {
                                ((LinearLayoutManager) recyclerAllStops.getLayoutManager()).scrollToPositionWithOffset(current, 0);
                            } else {
                                recyclerAllStops.scrollToPosition(current);
                            }
                        }
                    }
                    else if (ns == BottomSheetBehavior.STATE_COLLAPSED) { 
                        viewPagerStops.setVisibility(View.VISIBLE); 
                        recyclerAllStops.setVisibility(View.GONE); 
                        layoutSheetHeader.setVisibility(View.GONE); 
                        if (isEditMode) toggleEditMode(); 
                        if (isUnifyMode) toggleUnifyMode();
                        bs.setBackgroundColor(Color.TRANSPARENT);
                    }
                    else if (ns == BottomSheetBehavior.STATE_DRAGGING || ns == BottomSheetBehavior.STATE_SETTLING) {
                        bs.setBackgroundColor(Color.WHITE);
                    }
                }
                @Override public void onSlide(@NonNull View bs, float so) {
                    viewPagerStops.setAlpha(Math.max(0, 1.0f - so * 1.5f)); 
                    recyclerAllStops.setAlpha(Math.max(0, (so - 0.2f) * 1.5f)); 
                    layoutSheetHeader.setAlpha(Math.max(0, (so - 0.2f) * 1.5f));
                    if (so > 0.1) { 
                        recyclerAllStops.setVisibility(View.VISIBLE); 
                        layoutSheetHeader.setVisibility(View.VISIBLE); 
                        bs.setBackgroundColor(Color.WHITE);
                    } else if (so <= 0) {
                        bs.setBackgroundColor(Color.TRANSPARENT);
                    }
                }
            });
        }
        recyclerSuggestions.setLayoutManager(new LinearLayoutManager(getContext()));
        suggestionsAdapter = new SuggestionsAdapter(new ArrayList<>(), this::onSuggestionClicked);
        recyclerSuggestions.setAdapter(suggestionsAdapter);
        stopsCardAdapter = new StopsCardAdapter(this, new ArrayList<>(), this::onStopAction);
        viewPagerStops.setAdapter(stopsCardAdapter);
        stopsListAdapter = new StopsListAdapter(new ArrayList<>(), stop -> { 
            if (isUnifyMode) {
                btnUnifyManual.setText("Confirmar (" + stopsListAdapter.getSelectedStops().size() + ")");
            } else if (isEditMode) {
                promptAssignGroup(stop);
            } else {
                int i = currentStops.indexOf(stop);
                if (i != -1) {
                    viewPagerStops.setCurrentItem(i, false);
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }
        }, stop -> deleteStopDialog(stop));
        recyclerAllStops.setLayoutManager(new LinearLayoutManager(getContext())); recyclerAllStops.setAdapter(stopsListAdapter);
        setupDragAndDrop();
        viewPagerStops.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int p) { if (p >= 0 && p < currentStops.size()) { RouteStop s = currentStops.get(p); currentlySelectedStop = s; if (mapController != null && s.latitude != 0) { mapController.animateTo(new GeoPoint(s.latitude, s.longitude)); updateSelectionTrace(s); } saveCurrentStopIndex(p); updateMarkerIcons(); } }
        });
        centerOnCurrentLocation();
        editSearch.setOnEditorActionListener((v, aid, ev) -> { if (aid == EditorInfo.IME_ACTION_SEARCH) { searchAddress(editSearch.getText().toString()); return true; } return false; });
        if (editSearch != null) {
            editSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) { if (isSelectingSuggestion) return; if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable); if (s.length() > 3) { searchRunnable = () -> fetchSuggestions(s.toString()); searchHandler.postDelayed(searchRunnable, 600); } else recyclerSuggestions.setVisibility(View.GONE); }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
        if (savedInstanceState != null) pendingRestoreIndex = savedInstanceState.getInt(STATE_CURRENT_STOP_INDEX, -1);
        
        setupNetworkListener();
        animationHandler.post(markerAnimationRunnable);
        
        loadLastRoute(); checkRestInterval(); return view;
    }

    private void setupNetworkListener() {
        if (getContext() == null) return;
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;
        
        networkCallback = new android.net.ConnectivityManager.NetworkCallback() {
            @Override
            public void onLost(@NonNull android.net.Network network) {
                Activity activity = getActivity();
                if (activity != null) activity.runOnUiThread(() -> {
                        if (switchTraceLine != null && switchTraceLine.isChecked()) {
                            switchTraceLine.setChecked(false);
                            Toast.makeText(getContext(), "Conexão perdida. Trajeto desativado.", Toast.LENGTH_SHORT).show();
                        }
                    });
            }
        };
        cm.registerDefaultNetworkCallback(networkCallback);
    }

    private boolean isRestIntervalNow() {
        if (getContext() == null) return false;
        SharedPreferences p = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        if (!p.getBoolean("rest_interval_enabled", false)) return false;
        String s = p.getString("rest_start_time", "12:00"), e = p.getString("rest_end_time", "13:00");
        return isCurrentTimeInInterval(s, e);
    }

    private void checkRestInterval() {
        if (isRestIntervalNow()) { 
            if (cardRestWarning != null) cardRestWarning.setVisibility(View.VISIBLE); 
            if (locationOverlay != null) locationOverlay.disableMyLocation(); 
            return; 
        }
        if (cardRestWarning != null) cardRestWarning.setVisibility(View.GONE); 
        if (locationOverlay != null && !locationOverlay.isMyLocationEnabled()) {
            try {
                if (locationOverlay.getMyLocationProvider() == null) {
                    setupLocationOverlay();
                } else {
                    locationOverlay.enableMyLocation();
                }
            } catch (Exception e) {
                setupLocationOverlay();
            }
        }
    }

    private boolean isCurrentTimeInInterval(String s, String e) {
        try {
            String[] sp = s.split(":"), ep = e.split(":");
            int st = Integer.parseInt(sp[0]) * 60 + Integer.parseInt(sp[1]), et = Integer.parseInt(ep[0]) * 60 + Integer.parseInt(ep[1]);
            java.util.Calendar n = java.util.Calendar.getInstance(); int nt = n.get(java.util.Calendar.HOUR_OF_DAY) * 60 + n.get(java.util.Calendar.MINUTE);
            return (st < et) ? (nt >= st && nt < et) : (nt >= st || nt < et);
        } catch (Exception ex) { return false; }
    }

    private void disableRestAndGoToSettings() { requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE).edit().putBoolean("rest_interval_enabled", false).apply(); checkRestInterval(); if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).openGeneralSettings(); }

    private void setupLocationOverlay() {
        if (map == null || getContext() == null) return;
        
        // Limpeza inicial
        map.getOverlays().removeIf(o -> o instanceof MyLocationNewOverlay || o instanceof RotationGestureOverlay || (o instanceof Marker && "USER_DIR".equals(((Marker)o).getRelatedObject())));
        
        GpsMyLocationProvider provider = new GpsMyLocationProvider(requireContext());
        locationOverlay = new MyLocationNewOverlay(provider, map) {
            @Override public void onLocationChanged(android.location.Location location, org.osmdroid.views.overlay.mylocation.IMyLocationProvider source) {
                super.onLocationChanged(location, source);
                Activity activity = getActivity();
                if (location != null && activity != null) {
                    activity.runOnUiThread(() -> {
                        currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                        
                        if (userDirectionMarker != null) {
                            userDirectionMarker.setPosition(currentLocation);
                        }

                        // 🔥 Seguir localização se o foco estiver ativo
                        if (isMapFocusedOnUser && mapController != null) {
                            mapController.animateTo(currentLocation);
                        }

                        // 🔥 Atualiza clima se moveu mais de 5km ou passou 30 minutos
                        long now = System.currentTimeMillis();
                        double distClima = (lastWeatherLocation != null) ? currentLocation.distanceToAsDouble(lastWeatherLocation) : 99999;
                        if (distClima > 5000 || (now - lastWeatherUpdate > 1800000)) {
                            fetchWeather();
                        }

                        // 🔥 Pausa/Retomada automática do timer baseada no endereço de casa
                        checkHomeAutoPause(currentLocation);

                        if (currentlySelectedStop != null) {
                            double d = (lastTraceLocation != null) ? currentLocation.distanceToAsDouble(lastTraceLocation) : 999;
                            // Só atualiza se moveu mais de 30 metros OU se passou 1 minuto e moveu pelo menos 10 metros (evita jitter parado)
                            if (d > 30 || (now - lastTraceUpdate > 60000 && d > 10)) { 
                                lastTraceUpdate = now; 
                                lastTraceLocation = currentLocation; 
                                updateSelectionTrace(currentlySelectedStop); 
                            }
                        }
                    });
                }
            }
        };

        // Customização do Ícone do Usuário
        String iconType = sharedPreferences.getString("user_map_icon", "arrow");
        int resId = R.drawable.ic_car_marker; // Default car
        int tintColor = ContextCompat.getColor(requireContext(), R.color.teal_700);
        
        if (iconType.equals("moto")) resId = R.drawable.ic_play; 
        else if (iconType.equals("truck")) resId = R.drawable.ic_package;
        else if (iconType.equals("arrow")) {
            // 🔥 NOVO: Ícone 3D Branco Profissional
            resId = R.drawable.ic_nav_3d; 
            tintColor = 0; // Sinaliza para não aplicar cor sólida
        }

        try {
            Bitmap bmp = drawableToBitmap(resId, iconType.equals("moto"), tintColor);
            
            // Usamos um marcador separado para a direção para garantir norte fixo de navegação
            userDirectionMarker = new Marker(map);
            userDirectionMarker.setInfoWindow(null);
            userDirectionMarker.setRelatedObject("USER_DIR");
            userDirectionMarker.setPosition(currentLocation);
            userDirectionMarker.setIcon(new BitmapDrawable(getResources(), bmp));
            userDirectionMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            userDirectionMarker.setFlat(false); // 🔥 Alinhado à tela, não ao mapa
            userDirectionMarker.setRotation(isMapFollowingHeading ? 0 : currentAzimuth);
            
            // Oculta os ícones padrões do overlay para usar o nosso marcador customizado
            locationOverlay.setPersonIcon(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888));
            locationOverlay.setDirectionIcon(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888));
            locationOverlay.setDrawAccuracyEnabled(false);
        } catch (Exception e) { 
            e.printStackTrace(); 
            locationOverlay.setPersonIcon(null); 
            locationOverlay.setDirectionIcon(null);
        }

        // 🔥 Posicionamento imediato baseado na última localização conhecida
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(l -> {
                if (l != null && userDirectionMarker != null) {
                    currentLocation = new GeoPoint(l.getLatitude(), l.getLongitude());
                    userDirectionMarker.setPosition(currentLocation);
                    if (isMapFocusedOnUser && mapController != null) {
                        mapController.animateTo(currentLocation);
                    }
                    map.invalidate();
                }
            });
        }

        if (!isRestIntervalNow()) {
            locationOverlay.enableMyLocation();
        }
        
        map.getOverlays().add(locationOverlay);
        if (userDirectionMarker != null) map.getOverlays().add(userDirectionMarker);
        
        RotationGestureOverlay ro = new RotationGestureOverlay(map); ro.setEnabled(true); map.getOverlays().add(ro);
    }

    private Bitmap drawableToBitmap(int resId, boolean rotate, int tintColor) {
        Drawable d = ContextCompat.getDrawable(requireContext(), resId);
        int size = (int) (38 * getResources().getDisplayMetrics().density);
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        if (rotate) {
            c.save();
            c.rotate(-90, size/2f, size/2f);
        }
        d.setBounds(0, 0, size, size);
        if (tintColor != 0) d.setTint(tintColor);
        d.draw(c);
        if (rotate) c.restore();
        return b;
    }

    private void loadLastRoute() {
        if (getContext() == null) return;
        Context ctx = requireContext(); int lid = ctx.getSharedPreferences("AppConfig", Context.MODE_PRIVATE).getInt(PREF_LAST_ROUTE, -1);
        new Thread(() -> {
            AppDao dao = AppDatabase.getInstance(ctx).appDao(); 
            RouteHeader h = (lid != -1) ? dao.getRouteById(lid) : null;
            
            // 🔥 Se não encontrou a última rota e não temos nenhuma selecionada, pega a primeira disponível
            if (h == null && currentRouteId == -1) { 
                List<RouteHeader> all = dao.getAllRoutes(); 
                if (!all.isEmpty()) h = all.get(0); 
            }
            
            if (h != null && h.id != currentRouteId) { 
                final int fid = h.id; 
                final String fn = h.name; 
                Activity activity = getActivity(); if (activity != null) activity.runOnUiThread(() -> switchRoute(fid, fn)); 
            }
        }).start();
    }

    private void saveCurrentStopIndex(int i) { 
        if (currentRouteId != -1 && getContext() != null) {
            requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE).edit().putInt(PREF_LAST_STOP_PREFIX + currentRouteId, i).apply();
            
            // Falar os detalhes da parada ao mudar
            boolean isTtsEnabled = sharedPreferences.getBoolean("voice_commands_enabled", false);
            if (isTtsEnabled && tts != null && i >= 0 && i < currentStops.size()) {
                RouteStop s = currentStops.get(i);
                StringBuilder sb = new StringBuilder();
                sb.append("Próxima parada número ").append(i + 1).append(". ");
                sb.append(s.address).append(". ");
                
                if (s.packageCount > 1) {
                    sb.append(s.packageCount).append(" pacotes. ");
                } else {
                    sb.append("Um pacote. ");
                }

                String seqs = (s.allSequences != null && !s.allSequences.isEmpty()) ? s.allSequences : String.valueOf(s.sequence);
                if (seqs != null && !seqs.isEmpty()) {
                    sb.append("Identificação: ");
                    String[] parts = seqs.split(",\\s*");
                    for (int j = 0; j < parts.length; j++) {
                        String p = parts[j].trim();
                        if (p.equals("-")) {
                            sb.append("um pacote sem identificação");
                        } else {
                            sb.append(p);
                        }
                        if (j < parts.length - 1) sb.append(", ");
                    }
                    sb.append(".");
                }

                tts.speak(sb.toString(), TextToSpeech.QUEUE_FLUSH, null, "stop_announcement");
            }
        }
    }

    private void switchRoute(int rid, String n) {
        if (getContext() == null) return;
        this.currentRouteId = rid;
        
        // 🔥 Limpeza total do estado da rota anterior
        currentlySelectedStop = null;
        if (selectionTracePolyline != null && map != null) {
            map.getOverlays().remove(selectionTracePolyline);
            selectionTracePolyline = null;
        }
        if (cardNavigationMode != null) cardNavigationMode.setVisibility(View.GONE);

        // Só resetamos o índice se NÃO houver uma restauração de estado pendente (ex: vindo de salvar instância)
        if (pendingRestoreIndex == -1) {
             // Tenta buscar do SharedPreferences se não houver no bundle
             pendingRestoreIndex = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
                     .getInt(PREF_LAST_STOP_PREFIX + rid, -1);
        }
        
        requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE).edit().putInt(PREF_LAST_ROUTE, rid).apply();
        currentStops.clear(); if (stopsCardAdapter != null) stopsCardAdapter.setStops(new ArrayList<>()); if (stopsListAdapter != null) stopsListAdapter.setStops(new ArrayList<>());
        if (map != null) { 
            // Limpa todos os marcadores da rota anterior
            map.getOverlays().removeIf(o -> o instanceof Marker && !"HOME".equals(((Marker)o).getRelatedObject())); 
            showHomeMarker();
            map.invalidate(); 
        }
        
        // Reseta visualmente as estatísticas para não mostrar dados da rota anterior enquanto carrega
        if (textSuccessCount != null) textSuccessCount.setText("0");
        if (textSuccessPackageCount != null) textSuccessPackageCount.setText("0");
        if (textFailedCount != null) textFailedCount.setText("0");
        if (textPendingCount != null) textPendingCount.setText("0");
        if (textPendingPackageCount != null) textPendingPackageCount.setText("0");
        if (cardFailedSummary != null) cardFailedSummary.setVisibility(View.GONE);
        
        this.currentRouteHeader = null; // 🔥 Reseta o header para o timer não mostrar dados da rota anterior
        loadStopsForCurrentRoute(); if (bottomSheetBehavior != null) bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    public void promptNewRoute() {
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_new_route_name, null);
        EditText e = v.findViewById(R.id.editRouteName); String dstr = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new java.util.Date());
        e.setText(dstr); e.selectAll(); AlertDialog d = new AlertDialog.Builder(requireContext()).setView(v).create();
        if (d.getWindow() != null) d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        v.findViewById(R.id.btnCancelNewRoute).setOnClickListener(v2 -> d.dismiss());
        v.findViewById(R.id.btnNextNewRoute).setOnClickListener(v2 -> { String name = e.getText().toString().trim(); if (name.isEmpty()) name = "Rota " + dstr; d.dismiss(); showRouteCreationOptions(name); });
        d.show();
    }

    private void showRouteCreationOptions(String name) {
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_route_creation_options, null); AlertDialog d = new AlertDialog.Builder(requireContext()).setView(v).create();
        if (d.getWindow() != null) d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        v.findViewById(R.id.btnCancelRouteOptions).setOnClickListener(v2 -> d.dismiss());
        v.findViewById(R.id.cardNewEmptyRoute).setOnClickListener(v2 -> { 
            d.dismiss(); 
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showInterstitialThenAction(() -> createNewRoute(name, false));
            } else {
                createNewRoute(name, false);
            }
        });
        v.findViewById(R.id.cardImportXlsx).setOnClickListener(v2 -> { 
            d.dismiss(); 
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showInterstitialThenAction(() -> createNewRoute(name, true));
            } else {
                createNewRoute(name, true);
            }
        });
        d.show();
    }

    private void createNewRoute(String name, boolean imp) { 
        new Thread(() -> { 
            RouteHeader h = new RouteHeader(name); 
            long id = AppDatabase.getInstance(requireContext()).appDao().insertRouteHeader(h); 
            Activity activity = getActivity(); 
            if (activity != null) activity.runOnUiThread(() -> { 
                switchRoute((int) id, name); 
                
                // 🔥 Ativa automaticamente o card de paradas ao criar uma nova rota
                sharedPreferences.edit().putBoolean("show_bottom_sheet_stops", true).apply();
                
                if (imp) startXlsxImport(); 
                CloudSyncHelper.syncNow(requireContext(), "Nova Rota"); 
            }); 
        }).start(); 
    }

    private void toggleEditMode() { isEditMode = !isEditMode; btnEditList.setText(isEditMode ? "Concluir" : "Editar Lista"); stopsListAdapter.setEditMode(isEditMode); if (isEditMode) itemTouchHelper.attachToRecyclerView(recyclerAllStops); else { itemTouchHelper.attachToRecyclerView(null); saveStopsOrder(); } }

    private void toggleUnifyMode() {
        isUnifyMode = !isUnifyMode;
        if (isUnifyMode) {
            btnUnifyManual.setText("Confirmar (0)");
            btnUnifyManual.setIconResource(android.R.drawable.checkbox_on_background);
            stopsListAdapter.setUnifyMode(true);
            btnEditList.setEnabled(false);
            btnCreateGroup.setEnabled(false);
        } else {
            java.util.Set<RouteStop> selected = stopsListAdapter.getSelectedStops();
            if (selected.size() > 1) {
                confirmUnification(new ArrayList<>(selected));
            } else {
                exitUnifyMode();
            }
        }
    }

    private void exitUnifyMode() {
        isUnifyMode = false;
        btnUnifyManual.setText("Unificar");
        btnUnifyManual.setIconResource(android.R.drawable.ic_menu_share);
        stopsListAdapter.setUnifyMode(false);
        btnEditList.setEnabled(true);
        btnCreateGroup.setEnabled(true);
    }

    private void confirmUnification(List<RouteStop> selected) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Unificar Paradas")
            .setMessage("Deseja unificar estas " + selected.size() + " paradas em uma só?")
            .setPositiveButton("Sim", (d, w) -> processUnification(selected))
            .setNegativeButton("Não", (d, w) -> exitUnifyMode())
            .show();
    }

    private void processUnification(List<RouteStop> selected) {
        new Thread(() -> {
            // Ordenar por ordem original para manter o "mestre" como a primeira da sequência
            selected.sort((a, b) -> Integer.compare(a.sortOrder, b.sortOrder));
            
            RouteStop master = selected.get(0);
            StringBuilder combinedAddresses = new StringBuilder(master.allAddresses != null ? master.allAddresses : master.address);
            StringBuilder combinedSequences = new StringBuilder(master.allSequences != null ? master.allSequences : String.valueOf(master.sequence));
            int totalPackages = master.packageCount;
            java.util.Set<String> uniqueBuyers = new java.util.HashSet<>();
            if (master.allAddresses != null) {
                for (String addr : master.allAddresses.split("\n")) uniqueBuyers.add(addr.trim());
            } else {
                uniqueBuyers.add(master.address.trim());
            }

            for (int i = 1; i < selected.size(); i++) {
                RouteStop other = selected.get(i);
                combinedAddresses.append("\n").append(other.allAddresses != null ? other.allAddresses : other.address);
                combinedSequences.append(", ").append(other.allSequences != null ? other.allSequences : other.sequence);
                totalPackages += other.packageCount;
                if (other.allAddresses != null) {
                    for (String addr : other.allAddresses.split("\n")) uniqueBuyers.add(addr.trim());
                } else {
                    uniqueBuyers.add(other.address.trim());
                }
            }

            master.allAddresses = combinedAddresses.toString();
            master.allSequences = combinedSequences.toString();
            master.packageCount = totalPackages;
            master.buyerCount = uniqueBuyers.size();

            AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
            dao.updateRouteStop(master);
            
            // Remover as outras
            for (int i = 1; i < selected.size(); i++) {
                dao.deleteRouteStop(selected.get(i));
            }

            // Renumerar
            List<RouteStop> all = dao.getStopsForRoute(currentRouteId);
            for (int i = 0; i < all.size(); i++) {
                all.get(i).sortOrder = i;
                all.get(i).stopNumber = i + 1; // Sincroniza número da parada com a nova ordem
            }
            dao.updateRouteStops(all);

            Activity activity = getActivity();
            if (activity != null) activity.runOnUiThread(() -> {
                    exitUnifyMode();
                    Toast.makeText(getContext(), "Paradas unificadas com sucesso!", Toast.LENGTH_SHORT).show();
                    CloudSyncHelper.syncNow(requireContext(), "Atividade na Rota");
                });
        }).start();
    }


    private void setupDragAndDrop() {
        itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override 
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder t) { 
                int from = vh.getBindingAdapterPosition();
                int to = t.getBindingAdapterPosition();
                
                // Swap no cache local do fragment
                java.util.Collections.swap(currentStops, from, to); 
                
                // Swap interno no adapter (para manter integridade visual e de dados)
                stopsListAdapter.swap(from, to); 
                
                // Atualiza o ViewPager se estiver ativo
                if (stopsCardAdapter != null) stopsCardAdapter.setStops(currentStops); 
                return true; 
            }
            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int d) {}
        });
    }

    private void saveStopsOrder() { new Thread(() -> { AppDao dao = AppDatabase.getInstance(requireContext()).appDao(); for (int i = 0; i < currentStops.size(); i++) { currentStops.get(i).sortOrder = i; currentStops.get(i).stopNumber = i + 1; } dao.updateRouteStops(currentStops); Activity activity = getActivity(); if (activity != null) activity.runOnUiThread(() -> CloudSyncHelper.syncNow(requireContext(), "Ordem Paradas")); }).start(); }

    private void promptCreateGroup() { EditText i = new EditText(getContext()); i.setHint("Nome"); new AlertDialog.Builder(getContext()).setTitle("Grupo").setView(i).setPositiveButton("Ok", (d, w) -> { String n = i.getText().toString().trim(); if (!n.isEmpty()) showVisualColorPicker(n); }).show(); }

    private void showVisualColorPicker(String name) { View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_color_picker, null); SeekBar s = v.findViewById(R.id.seekHue); new AlertDialog.Builder(requireContext()).setTitle("Cor").setView(v).setPositiveButton("Ok", (d, w) -> { saveGroup(name, String.format("#%06X", (0xFFFFFF & Color.HSVToColor(new float[]{s.getProgress(), 1f, 1f})))); }).show(); }

    private void saveGroup(String n, String c) { new Thread(() -> { AppDatabase.getInstance(requireContext()).appDao().insertRouteGroup(new RouteGroup(n, c, currentRouteId)); CloudSyncHelper.syncNow(requireContext(), "Atividade na Rota"); }).start(); }

    private void promptAssignGroup(RouteStop s) { new Thread(() -> { List<RouteGroup> gs = AppDatabase.getInstance(requireContext()).appDao().getGroupsForRoute(currentRouteId); Activity activity = getActivity(); if (activity != null) activity.runOnUiThread(() -> { String[] ns = new String[gs.size()+1]; ns[0] = "Nenhum"; for (int i=0; i<gs.size(); i++) ns[i+1] = gs.get(i).name; new AlertDialog.Builder(getContext()).setItems(ns, (d, w) -> { new Thread(() -> { s.groupId = (w == 0) ? null : gs.get(w-1).id; AppDatabase.getInstance(requireContext()).appDao().updateRouteStop(s); }).start(); }).show(); }); }).start(); }

    private void toggleMapFocus() { if (isMapFocusedOnUser) centerOnActiveStop(); else centerOnCurrentLocation(); }

    private void shareCurrentRouteWithDevs() { new Thread(() -> { AppDao dao = AppDatabase.getInstance(requireContext()).appDao(); RouteHeader h = dao.getRouteById(currentRouteId); List<RouteStop> ss = dao.getStopsForRoute(currentRouteId); com.google.firebase.auth.FirebaseUser u = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser(); if (u != null && h != null && !ss.isEmpty()) FirebaseHelper.shareRouteWithDevelopers(u.getEmail(), u.getDisplayName(), h, ss, new FirebaseHelper.GlobalUploadCallback() { @Override public void onSuccess() { Activity activity = getActivity(); if (activity != null) activity.runOnUiThread(() -> Toast.makeText(getContext(), "Sucesso!", Toast.LENGTH_SHORT).show()); } @Override public void onFailure(String m) {} }); }).start(); }

    private void showSharedDeveloperRoutes() { FirebaseHelper.fetchSharedDeveloperRoutes(new FirebaseHelper.SharedRoutesCallback() { @Override public void onResult(List<Map<String, Object>> rs) { Activity activity = getActivity(); if (activity == null) return; activity.runOnUiThread(() -> { String[] ns = new String[rs.size()]; for(int i=0; i<rs.size(); i++) ns[i] = (String) rs.get(i).get("name"); new AlertDialog.Builder(requireContext()).setTitle("Dev").setItems(ns, (d, w) -> importSharedDevRoute(rs.get(w))).show(); }); } @Override public void onError(String m) {} }); }

    private void importSharedDevRoute(Map<String, Object> d) { 
        new Thread(() -> { 
            try { 
                String n = (String) d.get("name"); 
                List<Map<String, Object>> sd = (List<Map<String, Object>>) d.get("stops"); 
                AppDao dao = AppDatabase.getInstance(requireContext()).appDao(); 
                long nid = dao.insertRouteHeader(new RouteHeader("DEV: " + n)); 
                List<RouteStop> ns = new ArrayList<>(); 
                for (int i = 0; i < sd.size(); i++) { 
                    Map<String, Object> s = sd.get(i);
                    RouteStop st = new RouteStop((String)s.get("address"), (Double)s.get("lat"), (Double)s.get("lon")); 
                    st.routeId = (int)nid; 
                    st.sortOrder = i; // Nova rota sempre começa do 0
                    st.stopNumber = i + 1;
                    ns.add(st); 
                } 
                dao.insertRouteStops(ns); 
                Activity activity = getActivity(); 
                if (activity != null) activity.runOnUiThread(() -> { 
                    sharedPreferences.edit().putBoolean("show_bottom_sheet_stops", true).apply();
                    switchRoute((int)nid, "DEV: " + n); 
                }); 
            } catch(Exception ignored){} 
        }).start(); 
    }

    private void showStatsPopup(int type) {
        if (getContext() == null) return;
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_stats_info, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(v).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        ImageView icon = v.findViewById(R.id.imageStatsIcon);
        TextView title = v.findViewById(R.id.textStatsTitle);
        TextView textStops = v.findViewById(R.id.textStatsStopsCount);
        TextView textPackages = v.findViewById(R.id.textStatsPackagesCount);
        TextView textStopsList = v.findViewById(R.id.textStatsStopsList);
        View layoutPackages = v.findViewById(R.id.layoutStatsPackages);

        int okStops = 0, okPkgs = 0, errStops = 0, pendStops = 0, pendPkgs = 0;
        StringBuilder listBuilder = new StringBuilder();
        
        for (int i = 0; i < currentStops.size(); i++) {
            RouteStop s = currentStops.get(i);
            boolean match = false;
            if (type == 1 && s.deliveryStatus == 1) { okStops++; okPkgs += s.packageCount; match = true; }
            else if (type == 2 && s.deliveryStatus == 2) { errStops++; match = true; }
            else if (type == 0 && s.deliveryStatus == 0) { pendStops++; pendPkgs += s.packageCount; match = true; }

            if (match) {
                String seq = (s.allSequences != null && !s.allSequences.isEmpty()) ? s.allSequences : String.valueOf(s.sequence);
                listBuilder.append("<b>#").append(s.stopNumber).append("</b> - ")
                        .append(s.address).append("<br/>")
                        .append("<font color='#666666'>Sequência: ").append(seq).append("</font><br/><br/>");
            }
        }

        if (type == 1) { // Sucesso
            icon.setImageResource(android.R.drawable.checkbox_on_background);
            icon.setColorFilter(Color.parseColor("#388E3C"));
            title.setText("Entregas Realizadas");
            textStops.setText(String.valueOf(okStops));
            textPackages.setText(String.valueOf(okPkgs));
        } else if (type == 2) { // Erro
            icon.setImageResource(android.R.drawable.ic_delete);
            icon.setColorFilter(Color.parseColor("#D32F2F"));
            title.setText("Paradas com Erro");
            textStops.setText(String.valueOf(errStops));
            layoutPackages.setVisibility(View.GONE);
        } else { // Pendente
            icon.setImageResource(android.R.drawable.ic_menu_myplaces);
            icon.setColorFilter(Color.parseColor("#1976D2"));
            title.setText("Paradas Pendentes");
            textStops.setText(String.valueOf(pendStops));
            textPackages.setText(String.valueOf(pendPkgs));
        }

        if (listBuilder.length() > 0) {
            textStopsList.setText(android.text.Html.fromHtml(listBuilder.toString(), android.text.Html.FROM_HTML_MODE_COMPACT));
        } else {
            textStopsList.setText("Nenhuma parada nesta categoria.");
        }

        v.findViewById(R.id.btnStatsClose).setOnClickListener(v2 -> dialog.dismiss());
        dialog.show();
    }

    private void showTraceInfoPopup() {
        if (getContext() == null) return;
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_trace_info, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(v).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        v.findViewById(R.id.btnTraceInfoClose).setOnClickListener(v2 -> dialog.dismiss());
        dialog.show();
    }

    private void centerOnActiveStop() { if (currentStops.isEmpty()) { centerOnCurrentLocation(); return; } int p = viewPagerStops.getCurrentItem(); if (p>=0 && p<currentStops.size()) { RouteStop s = currentStops.get(p); if (mapController!=null && s.latitude!=0) { mapController.animateTo(new GeoPoint(s.latitude, s.longitude)); } else centerOnCurrentLocation(); isMapFocusedOnUser = false; updateCenterFabIcon(); } else centerOnCurrentLocation(); }

    private void centerOnCurrentLocation() { if (getContext()!=null && ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) fusedLocationClient.getLastLocation().addOnSuccessListener(l -> { if (l!=null && mapController!=null) { currentLocation = new GeoPoint(l.getLatitude(), l.getLongitude()); mapController.animateTo(currentLocation); isMapFocusedOnUser = true; updateCenterFabIcon(); } }); }

    private void startHomeSelection() {
        if (homeSelectionOverlay != null) {
            cancelHomeSelection();
            return;
        }

        float lat = sharedPreferences.getFloat("home_lat", 0);
        float lon = sharedPreferences.getFloat("home_lon", 0);

        if (lat != 0 && lon != 0) {
            AlertDialog dialog = new AlertDialog.Builder(getContext())
                    .setTitle("Casa já definida")
                    .setMessage("O local da sua casa já está salvo. Deseja alterar ou remover o endereço atual?")
                    .setPositiveButton("Alterar Novo Local", (d, which) -> enterHomeSelectionMode())
                    .setNeutralButton("Excluir Endereço", (d, which) -> {
                        sharedPreferences.edit().remove("home_lat").remove("home_lon").apply();
                        if (homeMarker != null) map.getOverlays().remove(homeMarker);
                        if (homeRadiusOverlay != null) map.getOverlays().remove(homeRadiusOverlay);
                        homeMarker = null; homeRadiusOverlay = null;
                        map.invalidate();
                        Toast.makeText(getContext(), "Endereço de casa removido", Toast.LENGTH_SHORT).show();
                        CloudSyncHelper.syncNow(requireContext(), "Atividade na Rota");
                        TrackingHelper.updateAutoTracking(requireContext());
                    })
                    .setNegativeButton("Manter Atual", null)
                    .create();
            if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog_rounded);
            dialog.show();
        } else {
            enterHomeSelectionMode();
        }
    }

    private void enterHomeSelectionMode() {
        Toast.makeText(getContext(), "Toque no mapa para definir sua CASA", Toast.LENGTH_LONG).show();
        
        homeSelectionOverlay = new org.osmdroid.views.overlay.MapEventsOverlay(new org.osmdroid.events.MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                confirmHomeLocation(p);
                return true;
            }
            @Override public boolean longPressHelper(GeoPoint p) { return false; }
        });
        map.getOverlays().add(homeSelectionOverlay);
    }

    private void cancelHomeSelection() {
        if (homeSelectionOverlay != null) {
            map.getOverlays().remove(homeSelectionOverlay);
            homeSelectionOverlay = null;
        }
        map.invalidate();
    }

    private void confirmHomeLocation(GeoPoint p) {
        new AlertDialog.Builder(getContext())
                .setTitle("Definir Casa")
                .setMessage("Deseja definir este local como sua casa para o início automático do rastreamento?")
                .setPositiveButton("Sim", (dialog, which) -> {
                    saveHomeLocation(p);
                    cancelHomeSelection();
                })
                .setNegativeButton("Não", (dialog, which) -> cancelHomeSelection())
                .show();
    }

    private void saveHomeLocation(GeoPoint p) {
        sharedPreferences.edit()
                .putFloat("home_lat", (float) p.getLatitude())
                .putFloat("home_lon", (float) p.getLongitude())
                .putBoolean("home_tracking_enabled", true)
                .apply();
        
        showHomeMarker();
        Toast.makeText(getContext(), "Casa definida! Rastreamento iniciará ao sair daqui.", Toast.LENGTH_LONG).show();
        CloudSyncHelper.syncNow(requireContext(), "Casa Definida");
        TrackingHelper.updateAutoTracking(requireContext());
    }

    private void showHomeMarker() {
        if (map == null || getContext() == null) return;
        
        float lat = sharedPreferences.getFloat("home_lat", 0);
        float lon = sharedPreferences.getFloat("home_lon", 0);

        if (lat != 0 && lon != 0) {
            GeoPoint homePoint = new GeoPoint(lat, lon);

            if (homeMarker == null) {
                homeMarker = new Marker(map);
                homeMarker.setTitle("Minha Casa");
                homeMarker.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_home));
                homeMarker.getIcon().setTint(Color.parseColor("#4CAF50"));
                homeMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                homeMarker.setRelatedObject("HOME");
            }
            homeMarker.setPosition(homePoint);
            if (!map.getOverlays().contains(homeMarker)) {
                map.getOverlays().add(homeMarker);
            }

            if (homeRadiusOverlay != null) {
                map.getOverlays().remove(homeRadiusOverlay);
            }

            int triggerRadius = sharedPreferences.getInt("home_trigger_radius", 100);
            homeRadiusOverlay = new org.osmdroid.views.overlay.Polygon(map);
            homeRadiusOverlay.setPoints(org.osmdroid.views.overlay.Polygon.pointsAsCircle(homePoint, triggerRadius));
            homeRadiusOverlay.getFillPaint().setColor(Color.parseColor("#334CAF50"));
            homeRadiusOverlay.getOutlinePaint().setColor(Color.parseColor("#4CAF50"));
            homeRadiusOverlay.getOutlinePaint().setStrokeWidth(2f);
            
            map.getOverlays().add(0, homeRadiusOverlay);
            map.invalidate();
        }
    }

    private void loadLoadingPoints() {
        loadingPoints = AppDatabase.getInstance(requireContext()).appDao().getAllLoadingPoints();
    }

    private void showLoadingMarkers() {
        if (map == null || getContext() == null) return;
        
        // 🔥 REGRA REMOTA: Verifica se os controles de carregamento/rastreamento estão visíveis
        if (getActivity() instanceof MainActivity && !((MainActivity) getActivity()).isMenuVisible("km")) {
            for (Marker m : loadingMarkers) map.getOverlays().remove(m);
            loadingMarkers.clear();
            map.getOverlays().removeIf(overlay -> overlay instanceof org.osmdroid.views.overlay.Polygon && ((org.osmdroid.views.overlay.Polygon)overlay).getTitle() != null && ((org.osmdroid.views.overlay.Polygon)overlay).getTitle().startsWith("LoadingRadius:"));
            map.invalidate();
            return;
        }

        loadLoadingPoints();
        
        for (Marker m : loadingMarkers) {
            map.getOverlays().remove(m);
        }
        loadingMarkers.clear();

        map.getOverlays().removeIf(overlay -> {
            if (overlay instanceof org.osmdroid.views.overlay.Polygon) {
                org.osmdroid.views.overlay.Polygon p = (org.osmdroid.views.overlay.Polygon) overlay;
                return p.getTitle() != null && p.getTitle().startsWith("LoadingRadius:");
            }
            return false;
        });

        int loadingRadius = sharedPreferences.getInt("loading_base_radius", 100);

        for (LoadingPoint lp : loadingPoints) {
            GeoPoint point = new GeoPoint(lp.latitude, lp.longitude);
            
            org.osmdroid.views.overlay.Polygon circle = new org.osmdroid.views.overlay.Polygon(map);
            circle.setPoints(org.osmdroid.views.overlay.Polygon.pointsAsCircle(point, loadingRadius));
            circle.getFillPaint().setColor(Color.parseColor("#33FF9800")); // Laranja semi-transparente
            circle.getOutlinePaint().setColor(Color.parseColor("#FF9800"));
            circle.getOutlinePaint().setStrokeWidth(2f);
            circle.setTitle("LoadingRadius:" + lp.id);
            map.getOverlays().add(0, circle);

            Marker m = new Marker(map);
            m.setPosition(point);
            m.setTitle(lp.name + (lp.platformName != null ? " (" + lp.platformName + ")" : ""));
            m.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_money));
            if (m.getIcon() != null) m.getIcon().setTint(Color.parseColor("#FF9800"));
            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            map.getOverlays().add(m);
            loadingMarkers.add(m);
        }
        map.invalidate();
    }

    private void showLoadingPointsDialog() {
        if (loadingSelectionOverlay != null) {
            cancelLoadingSelection();
            return;
        }
        loadLoadingPoints();
        String[] names = new String[loadingPoints.size() + (loadingPoints.size() < 5 ? 1 : 0)];
        for (int i = 0; i < loadingPoints.size(); i++) {
            names[i] = loadingPoints.get(i).name;
        }
        if (loadingPoints.size() < 5) {
            names[loadingPoints.size()] = "+ Adicionar novo ponto";
        }

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle("Pontos de Carregamento")
                .setItems(names, (d, which) -> {
                    if (which == loadingPoints.size()) {
                        enterLoadingSelectionMode(-1);
                    } else {
                        showEditLoadingPointDialog(which);
                    }
                })
                .setNegativeButton("Fechar", null)
                .create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog_rounded);
        dialog.show();
    }

    private void showEditLoadingPointDialog(int index) {
        LoadingPoint lp = loadingPoints.get(index);
        String[] options = {"Editar Nome", "Editar Localização", "Excluir"};
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(lp.name)
                .setItems(options, (d, which) -> {
                    if (which == 0) {
                        promptForLoadingPointName(lp.latitude, lp.longitude, index);
                    } else if (which == 1) {
                        enterLoadingSelectionMode(index);
                    } else if (which == 2) {
                        new Thread(() -> {
                            AppDatabase.getInstance(requireContext()).appDao().deleteLoadingPoint(lp);
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    showLoadingMarkers();
                                    Toast.makeText(getContext(), "Ponto removido", Toast.LENGTH_SHORT).show();
                                    CloudSyncHelper.syncNow(requireContext(), "Ponto Removido");
                                });
                            }
                        }).start();
                    }
                })
                .create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog_rounded);
        dialog.show();
    }

    private void enterLoadingSelectionMode(int index) {
        if (loadingSelectionOverlay != null) {
            cancelLoadingSelection();
        }
        Toast.makeText(getContext(), "Toque no mapa para definir o ponto de CARREGAMENTO", Toast.LENGTH_LONG).show();
        
        if (cardFixMode != null) {
            cardFixMode.setVisibility(View.VISIBLE);
        }

        loadingSelectionOverlay = new org.osmdroid.views.overlay.MapEventsOverlay(new org.osmdroid.events.MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                confirmLoadingLocation(p, index);
                return true;
            }
            @Override public boolean longPressHelper(GeoPoint p) { return false; }
        });
        map.getOverlays().add(loadingSelectionOverlay);
    }

    private void cancelLoadingSelection() {
        if (loadingSelectionOverlay != null) {
            map.getOverlays().remove(loadingSelectionOverlay);
            loadingSelectionOverlay = null;
        }
        if (cardFixMode != null) cardFixMode.setVisibility(View.GONE);
        map.invalidate();
    }

    private void confirmLoadingLocation(GeoPoint p, int index) {
        cancelLoadingSelection();
        
        if (index == -1) {
            promptForLoadingPointName(p.getLatitude(), p.getLongitude(), -1);
        } else {
            LoadingPoint lp = loadingPoints.get(index);
            lp.latitude = p.getLatitude();
            lp.longitude = p.getLongitude();
            new Thread(() -> {
                AppDatabase.getInstance(requireContext()).appDao().updateLoadingPoint(lp);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoadingMarkers();
                        Toast.makeText(getContext(), "Localização atualizada", Toast.LENGTH_SHORT).show();
                        CloudSyncHelper.syncNow(requireContext(), "Atividade na Rota");
                    });
                }
            }).start();
        }
    }

    private void promptForLoadingPointName(double lat, double lon, int index) {
        android.widget.EditText input = new android.widget.EditText(getContext());
        if (index != -1) input.setText(loadingPoints.get(index).name);
        else input.setHint("Nome da Empresa");

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(index == -1 ? "Nome do Ponto" : "Editar Nome")
                .setView(input)
                .setPositiveButton("Próximo", (d, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) name = "Carregamento " + (loadingPoints.size() + 1);
                    promptForPlatformSelection(lat, lon, index, name);
                })
                .setNegativeButton("Cancelar", null)
                .create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog_rounded);
        dialog.show();
    }

    private void promptForPlatformSelection(double lat, double lon, int index, String name) {
        AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
        List<Platform> platforms = dao.getAllPlatforms();
        if (platforms.isEmpty()) {
            Toast.makeText(getContext(), "Nenhuma plataforma cadastrada. Configure em Ajustes primeiro.", Toast.LENGTH_LONG).show();
            return;
        }

        String[] platformNames = new String[platforms.size()];
        for (int i = 0; i < platforms.size(); i++) {
            platformNames[i] = platforms.get(i).name;
        }

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle("Selecione a Plataforma")
                .setItems(platformNames, (d, which) -> {
                    String platformName = platforms.get(which).name;
                    new Thread(() -> {
                        if (index == -1) {
                            LoadingPoint lp = new LoadingPoint(name, lat, lon, platformName);
                            dao.insertLoadingPoint(lp);
                        } else {
                            LoadingPoint lp = loadingPoints.get(index);
                            lp.name = name;
                            lp.platformName = platformName;
                            dao.updateLoadingPoint(lp);
                        }
                        
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                showLoadingMarkers();
                                Toast.makeText(getContext(), "Ponto salvo com sucesso!", Toast.LENGTH_SHORT).show();
                                CloudSyncHelper.syncNow(requireContext(), "Ponto Carregamento");
                            });
                        }
                    }).start();
                })
                .setNegativeButton("Cancelar", null)
                .create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog_rounded);
        dialog.show();
    }

    private void centerOnHome() {
        if (map == null || getContext() == null) return;
        float lat = sharedPreferences.getFloat("home_lat", 0);
        float lon = sharedPreferences.getFloat("home_lon", 0);
        if (lat != 0 && lon != 0) {
            mapController.animateTo(new GeoPoint(lat, lon));
            Toast.makeText(getContext(), "Rota Finalizada! Voltando para casa...", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkHomeAutoPause(GeoPoint loc) {
        if (currentRouteHeader == null || currentRouteHeader.startTime == 0 || currentRouteHeader.endTime > 0) return;
        
        float homeLat = sharedPreferences.getFloat("home_lat", 0);
        float homeLon = sharedPreferences.getFloat("home_lon", 0);
        if (homeLat == 0) return;
        
        GeoPoint home = new GeoPoint(homeLat, homeLon);
        double dist = loc.distanceToAsDouble(home);
        int radius = sharedPreferences.getInt("home_trigger_radius", 100);
        
        long now = System.currentTimeMillis();
        boolean updated = false;
        
        if (dist < radius) {
            if (currentRouteHeader.lastPauseStartTime == 0) {
                currentRouteHeader.lastPauseStartTime = now;
                updated = true;
                android.util.Log.d("DriveLog", "[Timer] Pausado automaticamente (Chegou em casa)");
            }
        } else {
            if (currentRouteHeader.lastPauseStartTime > 0) {
                long pauseDuration = now - currentRouteHeader.lastPauseStartTime;
                currentRouteHeader.totalPausedMs += pauseDuration;
                currentRouteHeader.lastPauseStartTime = 0;
                updated = true;
                android.util.Log.d("DriveLog", "[Timer] Retomado automaticamente (Saiu de casa)");
            }
        }
        
        if (updated) {
            final RouteHeader h = currentRouteHeader;
            new Thread(() -> AppDatabase.getInstance(requireContext()).appDao().updateRouteHeader(h)).start();
        }
    }

    private void toggleMapOrientation() {
        if (!isMapFocusedOnUser) {
            // Se não estiver focado no usuário, foca primeiro
            isMapFocusedOnUser = true;
            centerOnCurrentLocation();
        }
        isMapFollowingHeading = !isMapFollowingHeading;
        if (!isMapFollowingHeading) {
            map.setMapOrientation(0); // Reseta para o Norte
        } else {
            // Ao ativar, já pega a orientação atual do sensor se disponível
            map.setMapOrientation(-currentAzimuth);
        }
        if (userDirectionMarker != null) {
            userDirectionMarker.setRotation(isMapFollowingHeading ? 0 : currentAzimuth);
        }
        updateOrientationFabIcon();
        Toast.makeText(getContext(), isMapFollowingHeading ? "Seguindo direção" : "Norte fixo", Toast.LENGTH_SHORT).show();
    }

    private void updateOrientationFabIcon() {
        if (fabMapOrientation != null) {
            fabMapOrientation.setImageResource(isMapFollowingHeading ? android.R.drawable.ic_menu_compass : android.R.drawable.ic_menu_directions);
            fabMapOrientation.setSupportImageTintList(android.content.res.ColorStateList.valueOf(
                isMapFollowingHeading ? Color.parseColor("#F44336") : ContextCompat.getColor(requireContext(), R.color.teal_700)
            ));
        }
    }

    private void updateCenterFabIcon() { if (fabCenterMap!=null) fabCenterMap.setImageResource(isMapFocusedOnUser ? android.R.drawable.ic_menu_myplaces : R.drawable.ic_my_location); }

    private void fetchSuggestions(String q) { new Thread(() -> { try { 
        String uniqueId = android.provider.Settings.Secure.getString(requireContext().getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        String userAgent = "DriveLogApp_v141_" + uniqueId;
        String u = String.format(Locale.US, "https://nominatim.openstreetmap.org/search?q=%s&format=json&limit=5", URLEncoder.encode(q, java.nio.charset.StandardCharsets.UTF_8.name())); 
        HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection(); 
        c.setRequestProperty("User-Agent", userAgent); 
        BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream())); StringBuilder res = new StringBuilder(); String l; while((l=r.readLine())!=null) res.append(l); JSONArray a = new JSONArray(res.toString()); List<Suggestion> sl = new ArrayList<>(); for(int i=0; i<a.length(); i++) { JSONObject o = a.getJSONObject(i); sl.add(new Suggestion(o.getString("display_name"), o.getDouble("lat"), o.getDouble("lon"))); } Activity activity = getActivity(); if (activity != null) activity.runOnUiThread(() -> { suggestionsAdapter.setSuggestions(sl); recyclerSuggestions.setVisibility(sl.isEmpty() ? View.GONE : View.VISIBLE); }); } catch(Exception ignored){} }).start(); }

    private void onSuggestionClicked(Suggestion s) { isSelectingSuggestion = true; recyclerSuggestions.setVisibility(View.GONE); editSearch.setText(s.displayName); lastSearchedPoint = new GeoPoint(s.lat, s.lon); lastSearchedAddress = s.displayName; if (mapController!=null) { mapController.animateTo(lastSearchedPoint); mapController.setZoom(18.0); } showTempMarker(lastSearchedPoint, lastSearchedAddress); new Handler(Looper.getMainLooper()).postDelayed(() -> { if (isAdded()) { confirmAddStop(); isSelectingSuggestion = false; } }, 600); }

    private void searchAddress(String q) { if (!q.isEmpty()) fetchSuggestions(q); }

    private void promptManualStop() {
        if (currentRouteId == -1) {
            Toast.makeText(getContext(), "Selecione ou crie uma rota primeiro!", Toast.LENGTH_SHORT).show();
            promptNewRoute();
            return;
        }
        View v = getLayoutInflater().inflate(R.layout.dialog_add_stop_manual, null);
        EditText ea = v.findViewById(R.id.editStopAddress);
        EditText en = v.findViewById(R.id.editStopNeighborhood);
        
        // Se houver algo no campo de busca que não seja um endereço completo selecionado, preenchemos
        String typed = (editSearch != null) ? editSearch.getText().toString().trim() : "";
        if (!typed.isEmpty() && !isSelectingSuggestion) {
            ea.setText(typed);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Adicionar Parada")
                .setView(v)
                .setPositiveButton("Buscar Endereço", null) // Sobrescrevemos o clique para validar
                .setNeutralButton("Localização Atual", (d, w) -> {
                    String a = ea.getText().toString().trim();
                    if (a.isEmpty()) a = "Minha Localização";
                    String neighborhood = (en != null) ? en.getText().toString().trim() : "";
                    saveStopToDb(a, neighborhood, currentLocation.getLatitude(), currentLocation.getLongitude());
                })
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(dInterface -> {
            Button b = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            b.setOnClickListener(view -> {
                String a = ea.getText().toString().trim();
                String neighborhood = (en != null) ? en.getText().toString().trim() : "";
                if (a.isEmpty()) {
                    Toast.makeText(getContext(), "Digite o endereço para buscar", Toast.LENGTH_SHORT).show();
                    return;
                }
                geocodeAndSaveStop(a, neighborhood);
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    private void geocodeAndSaveStop(String address, String neighborhood) {
        new Thread(() -> {
            try {
                String uniqueId = android.provider.Settings.Secure.getString(requireContext().getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                String userAgent = "DriveLogApp_v141_" + uniqueId;
                
                String query = address + (neighborhood.isEmpty() ? "" : ", " + neighborhood);
                String u = String.format(Locale.US, "https://nominatim.openstreetmap.org/search?q=%s&format=json&limit=1", URLEncoder.encode(query, "UTF-8"));
                HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
                c.setRequestProperty("User-Agent", userAgent);
                BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()));
                StringBuilder res = new StringBuilder();
                String l; while((l=r.readLine())!=null) res.append(l);
                JSONArray a = new JSONArray(res.toString());
                
                if (a.length() > 0) {
                    JSONObject o = a.getJSONObject(0);
                    saveStopToDb(address, neighborhood, o.getDouble("lat"), o.getDouble("lon"));
                } else {
                    Activity activity = getActivity();
                    if (activity != null) activity.runOnUiThread(() -> {
                            new AlertDialog.Builder(requireContext())
                                .setTitle("Endereço não localizado")
                                .setMessage("Não encontramos as coordenadas para: " + address + ".\n\nDeseja adicionar na sua localização atual mesmo assim?")
                                .setPositiveButton("Sim, Usar GPS", (d, w) -> saveStopToDb(address, neighborhood, currentLocation.getLatitude(), currentLocation.getLongitude()))
                                .setNegativeButton("Não, Cancelar", null)
                                .show();
                        });
                }
            } catch (Exception e) {
                Activity activity = getActivity();
                    if (activity != null) activity.runOnUiThread(() -> Toast.makeText(getContext(), "Erro na busca: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void startHazardListener() {
        if (hazardListener != null) hazardListener.remove();
        hazardListener = FirebaseHelper.listenHazards(hazards -> {
            Activity activity = getActivity(); if (activity != null) activity.runOnUiThread(() -> updateHazardMarkers(hazards));
        });
    }

    private void updateHazardMarkers(List<FirebaseHelper.HazardReport> list) {
        if (map == null || getContext() == null || !isAdded()) return;
        List<String> activeIds = new ArrayList<>();
        for (FirebaseHelper.HazardReport h : list) {
            activeIds.add(h.id);
            Marker m = hazardMarkers.get(h.id);
            if (m == null) {
                m = new Marker(map);
                m.setInfoWindow(null); // Evita NPE e limpa o mapa
                m.setRelatedObject("HAZARD");
                m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                map.getOverlays().add(m);
                hazardMarkers.put(h.id, m);
            }
            m.setPosition(new GeoPoint(h.lat, h.lon));
            m.setTitle(h.type + ": " + h.description);
            m.setSnippet("Votos: " + h.likes + " 👍 | " + h.dislikes + " 👎"); 
            
            m.setOnMarkerClickListener((marker, mapView) -> {
                showHazardDetailsDialog(h);
                return true;
            });

            int iconRes = R.drawable.ic_reports;
            int color = Color.RED;
            if ("Trânsito".equals(h.type)) color = Color.parseColor("#FF9800");
            else if ("Obra".equals(h.type)) color = Color.YELLOW;
            
            // Gerar ícone triangular customizado
            m.setIcon(new BitmapDrawable(getResources(), generateTriangleMarkerBitmap(iconRes, color)));
            
            // TTS Alerta ao se aproximar
            boolean isTtsEnabled = sharedPreferences.getBoolean("voice_commands_enabled", false);
            if (isTtsEnabled && currentLocation != null && currentLocation.distanceToAsDouble(m.getPosition()) < 300) {
                String key = "tts_alert_" + h.id;
                if (!sharedPreferences.contains(key)) {
                    sharedPreferences.edit().putBoolean(key, true).apply();
                    tts.speak("Alerta de " + h.type + " à frente.", TextToSpeech.QUEUE_ADD, null, h.id);
                }
            }
        }
        hazardMarkers.entrySet().removeIf(entry -> {
            if (!activeIds.contains(entry.getKey())) {
                map.getOverlays().remove(entry.getValue());
                return true;
            }
            return false;
        });
        map.invalidate();
    }

    private Bitmap generateTriangleMarkerBitmap(int iconRes, int color) {
        int size = 90;
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Desenhar Triângulo de Fundo
        p.setColor(color);
        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(size / 2f, 0); // Topo
        path.lineTo(size, size);    // Inferior Direito
        path.lineTo(0, size);       // Inferior Esquerdo
        path.close();
        c.drawPath(path, p);

        // Borda Branca
        p.setStyle(Paint.Style.STROKE);
        p.setColor(Color.WHITE);
        p.setStrokeWidth(6f);
        c.drawPath(path, p);

        // Desenhar o Ícone dentro do Triângulo
        Drawable d = ContextCompat.getDrawable(requireContext(), iconRes);
        if (d != null) {
            d.setTint(Color.WHITE);
            if (color == Color.YELLOW) d.setTint(Color.BLACK); // Melhor contraste
            int iconSize = size / 2;
            int left = (size - iconSize) / 2;
            int top = (int) (size * 0.4f); // Um pouco mais abaixo do topo para centralizar visualmente no triângulo
            d.setBounds(left, top, left + iconSize, top + iconSize);
            d.draw(c);
        }

        return b;
    }

    private void promptReportHazard() {
        View vLoc = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_modern_confirm, null);
        TextView titleLoc = vLoc.findViewById(R.id.textModernTitle);
        TextView messageLoc = vLoc.findViewById(R.id.textModernMessage);
        MaterialButton btnCurrent = vLoc.findViewById(R.id.btnModernPositive);
        MaterialButton btnMap = vLoc.findViewById(R.id.btnModernNegative);

        titleLoc.setText("Local do Alerta");
        messageLoc.setText("Onde ocorreu o problema?");
        btnCurrent.setText("LOCAL ATUAL");
        btnMap.setText("ESCOLHER NO MAPA");

        AlertDialog locDialog = new AlertDialog.Builder(requireContext()).setView(vLoc).create();
        if (locDialog.getWindow() != null) locDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        btnCurrent.setOnClickListener(v -> {
            locDialog.dismiss();
            showHazardTypeSelection(currentLocation);
        });

        btnMap.setOnClickListener(v -> {
            locDialog.dismiss();
            cardFixMode.setVisibility(View.VISIBLE);
            TextView textFix = cardFixMode.findViewById(R.id.textModernTitle); // Reutilizando cardFixMode se possível ou adaptando
            if (textFix == null) {
                // Tenta achar pelo texto se o ID for diferente no cardFixMode
                View tv = cardFixMode.findViewWithTag("fix_text"); 
                if (tv instanceof TextView) ((TextView) tv).setText("Toque no local do alerta");
            }
            Toast.makeText(getContext(), "Toque no mapa onde está o perigo", Toast.LENGTH_LONG).show();
            
            org.osmdroid.views.overlay.MapEventsOverlay pickOverlay = new org.osmdroid.views.overlay.MapEventsOverlay(new org.osmdroid.events.MapEventsReceiver() {
                @Override public boolean singleTapConfirmedHelper(GeoPoint p) {
                    Activity activity = getActivity();
                    if (activity != null) activity.runOnUiThread(() -> {
                            cardFixMode.setVisibility(View.GONE);
                            map.getOverlays().removeIf(o -> o instanceof org.osmdroid.views.overlay.MapEventsOverlay && !(o == currentFixOverlay)); 
                            showHazardTypeSelection(p);
                        });
                    return true;
                }
                @Override public boolean longPressHelper(GeoPoint p) { return false; }
            });
            map.getOverlays().add(pickOverlay);
        });

        locDialog.show();
    }

    private void showHazardTypeSelection(GeoPoint loc) {
        String[] types = {"Assalto", "Trânsito", "Obra", "Acidente", "Polícia"};
        
        // Popup 1: Escolha do Tipo
        View vType = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_modern_confirm, null);
        TextView titleType = vType.findViewById(R.id.textModernTitle);
        TextView messageType = vType.findViewById(R.id.textModernMessage);
        MaterialButton btnCancel = vType.findViewById(R.id.btnModernNegative);
        MaterialButton btnOk = vType.findViewById(R.id.btnModernPositive);
        
        titleType.setText("Reportar Alerta");
        messageType.setText("Selecione o tipo de perigo no local selecionado.");
        btnOk.setVisibility(View.GONE); 
        
        LinearLayout container = (LinearLayout) messageType.getParent();
        android.widget.ListView listView = new android.widget.ListView(requireContext());
        listView.setDivider(null);
        listView.setAdapter(new android.widget.ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, types));
        container.addView(listView, container.indexOfChild(messageType) + 1);
        
        AlertDialog typeDialog = new AlertDialog.Builder(requireContext()).setView(vType).create();
        if (typeDialog.getWindow() != null) typeDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        listView.setOnItemClickListener((parent, view, position, id) -> {
            typeDialog.dismiss();
            showReportDetailsDialog(types[position], loc);
        });
        
        btnCancel.setOnClickListener(v -> typeDialog.dismiss());
        typeDialog.show();
    }

    private void showReportDetailsDialog(String type, GeoPoint loc) {
        View vDetails = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_modern_confirm, null);
        TextView title = vDetails.findViewById(R.id.textModernTitle);
        TextView message = vDetails.findViewById(R.id.textModernMessage);
        MaterialButton btnCancel = vDetails.findViewById(R.id.btnModernNegative);
        MaterialButton btnReport = vDetails.findViewById(R.id.btnModernPositive);
        
        title.setText(type);
        message.setText("Adicione detalhes e o tempo que este alerta deve ficar ativo.");
        btnReport.setText("REPORTAR");
        
        LinearLayout container = (LinearLayout) message.getParent();
        
        EditText input = new EditText(requireContext());
        input.setHint("Detalhes (opcional)");
        input.setBackgroundResource(android.R.drawable.edit_text); 
        
        TextView textDuration = new TextView(requireContext());
        textDuration.setText("Duração: 10 minutos");
        textDuration.setPadding(0, 30, 0, 0);
        textDuration.setTextColor(Color.BLACK);

        SeekBar seekDuration = new SeekBar(requireContext());
        seekDuration.setMax(50); 
        seekDuration.setProgress(0); 

        container.addView(input, container.indexOfChild(message) + 1);
        container.addView(textDuration);
        container.addView(seekDuration);

        final int[] selectedMinutes = {10};
        seekDuration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedMinutes[0] = progress + 10;
                textDuration.setText("Duração: " + selectedMinutes[0] + " minutos");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        AlertDialog detailsDialog = new AlertDialog.Builder(requireContext()).setView(vDetails).create();
        if (detailsDialog.getWindow() != null) detailsDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        btnCancel.setOnClickListener(v -> detailsDialog.dismiss());
        btnReport.setOnClickListener(v -> {
            String desc = input.getText().toString();
            String uid = sharedPreferences.getString("current_user_id", "anon");
            FirebaseHelper.HazardReport h = new FirebaseHelper.HazardReport(type, desc, loc.getLatitude(), loc.getLongitude(), uid, selectedMinutes[0]);
            FirebaseHelper.reportHazard(h, new FirebaseHelper.GlobalUploadCallback() {
                @Override public void onSuccess() { Toast.makeText(getContext(), "Alerta enviado!", Toast.LENGTH_SHORT).show(); }
                @Override public void onFailure(String m) { Toast.makeText(getContext(), "Erro: " + m, Toast.LENGTH_SHORT).show(); }
            });
            detailsDialog.dismiss();
        });
        
        detailsDialog.show();
    }

    private void showHazardDetailsDialog(FirebaseHelper.HazardReport h) {
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_modern_confirm, null);
        TextView title = v.findViewById(R.id.textModernTitle);
        TextView message = v.findViewById(R.id.textModernMessage);
        MaterialButton btnOk = v.findViewById(R.id.btnModernPositive);
        MaterialButton btnDelete = v.findViewById(R.id.btnModernNegative);

        title.setText(h.type);
        
        StringBuilder sb = new StringBuilder();
        if (h.description != null && !h.description.isEmpty()) {
            sb.append(h.description).append("\n\n");
        }
        sb.append("Votos: ").append(h.likes).append(" 👍 | ").append(h.dislikes).append(" 👎");
        message.setText(sb.toString());

        btnOk.setText("VOTAR");
        btnDelete.setText("EXCLUIR");
        
        String myUid = sharedPreferences.getString("current_user_id", "anon");
        boolean isCreator = h.creatorId != null && h.creatorId.equals(myUid);
        btnDelete.setVisibility(isCreator ? View.VISIBLE : View.GONE);

        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(v).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        btnOk.setOnClickListener(v2 -> {
            dialog.dismiss();
            showHazardFeedbackDialog(h);
        });
        btnDelete.setOnClickListener(v2 -> {
            new AlertDialog.Builder(requireContext())
                .setTitle("Confirmar")
                .setMessage("Deseja realmente excluir este alerta?")
                .setPositiveButton("Sim", (d, w) -> {
                    FirebaseHelper.deleteHazard(h.id);
                    dialog.dismiss();
                    Toast.makeText(getContext(), "Alerta removido", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Não", null)
                .show();
        });

        dialog.show();
    }

    private void showHazardFeedbackDialog(FirebaseHelper.HazardReport h) {
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_community_feedback, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(v).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView title = v.findViewById(R.id.textFeedbackTitle);
        if (title != null) title.setText(h.type);

        v.findViewById(R.id.btnFeedbackLike).setOnClickListener(v2 -> {
            FirebaseHelper.addHazardFeedback(h.id, true);
            Toast.makeText(getContext(), "Voto registrado!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        v.findViewById(R.id.btnFeedbackDislike).setOnClickListener(v2 -> {
            FirebaseHelper.addHazardFeedback(h.id, false);
            Toast.makeText(getContext(), "Voto registrado!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        View btnComments = v.findViewById(R.id.btnFeedbackComments);
        if (btnComments != null) btnComments.setVisibility(View.GONE);

        v.findViewById(R.id.btnFeedbackClose).setOnClickListener(v2 -> dialog.dismiss());
        dialog.show();
    }

    private void showRouteOptionsMenu(View anchor) {
        android.util.Log.d("DriveLog", "showRouteOptionsMenu chamado!");
        if (currentRouteId == -1) return;
        PopupMenu p = new PopupMenu(requireContext(), anchor);
        
        boolean showWeather = sharedPreferences.getBoolean("show_weather_balloon", true);
        p.getMenu().add(0, 105, 1, "Exibir Clima no Mapa").setCheckable(true).setChecked(showWeather);
        
        boolean autoNearest = sharedPreferences.getBoolean("advance_to_nearest", false);
        p.getMenu().add(0, 103, 2, "Avançar para a mais próxima").setCheckable(true).setChecked(autoNearest);
        
        boolean timerOnCards = sharedPreferences.getBoolean("timer_on_cards_only", false);
        p.getMenu().add(0, 104, 3, "Tempo apenas nos Cards").setCheckable(true).setChecked(timerOnCards);
        
        p.getMenu().add("Otimizar Rota");
        p.getMenu().add("Otimizar Rota 2.0");
        p.getMenu().add("Desenhar Rota (Laço)");
        p.getMenu().add("Inverter Ordem");
        p.getMenu().add("Limpar Rota");
        p.getMenu().add("Importar Planilha");
        
        // --- Submenu de Alinhamento de Botões ---
        android.view.SubMenu sub = p.getMenu().addSubMenu("Lado dos Botões Flutuantes");
        String currentAlign = sharedPreferences.getString("side_fabs_alignment", "auto");
        sub.add(2, 200, 0, "Automático (Padrão)").setCheckable(true).setChecked("auto".equals(currentAlign));
        sub.add(2, 201, 1, "Esquerdo").setCheckable(true).setChecked("left".equals(currentAlign));
        sub.add(2, 202, 2, "Direito").setCheckable(true).setChecked("right".equals(currentAlign));
        
        // --- NOVO: Submenu de Visibilidade dos Botões ---
        android.view.SubMenu subVis = p.getMenu().addSubMenu("Visibilidade dos Botões");
        subVis.add(3, 301, 0, "Botão Atalho App").setCheckable(true).setChecked(sharedPreferences.getBoolean("show_fab_delivery_app", true));
        subVis.add(3, 302, 1, "Botão Reportar").setCheckable(true).setChecked(sharedPreferences.getBoolean("show_fab_report_hazard", true));
        subVis.add(3, 303, 2, "Botão Localização").setCheckable(true).setChecked(sharedPreferences.getBoolean("show_fab_center_map", true));
        subVis.add(3, 304, 3, "Botão Norte").setCheckable(true).setChecked(sharedPreferences.getBoolean("show_fab_orientation", true));
        
        if (getActivity() instanceof MainActivity && ((MainActivity) getActivity()).isMenuVisible("km")) {
            subVis.add(3, 306, 4, "Botão Rastreio KM").setCheckable(true).setChecked(sharedPreferences.getBoolean("show_fab_km_tracking", true));
        }
        
        subVis.add(3, 305, 5, "Card de Paradas").setCheckable(true).setChecked(sharedPreferences.getBoolean("show_bottom_sheet_stops", true));

        // Opção "Ocultar Entregas" movida para baixo do alinhamento de botões
        boolean hideDelivered = sharedPreferences.getBoolean("hide_delivered_stops", false);
        p.getMenu().add(0, 102, 8, "Ocultar Entregas no Mapa").setCheckable(true).setChecked(hideDelivered);
        
        com.google.firebase.auth.FirebaseUser u = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (u != null && u.getEmail() != null) {
            FirebaseHelper.checkDeveloperAccess(u.getEmail(), isDev -> {
                Activity activity = getActivity();
                if (isDev && activity != null) {
                    activity.runOnUiThread(() -> {
                        SharedPreferences pr = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
                        p.getMenu().add(1, 101, 9, "🔄 Dev: Auto-Compartilhar").setCheckable(true).setChecked(pr.getBoolean("dev_auto_share_routes", false));
                        p.getMenu().add(1, 99, 10, "🚀 Dev: Compartilhar Rota");
                        p.getMenu().add(1, 100, 11, "📥 Dev: Baixar Rotas");
                    });
                }
            });
        }
        
        p.setOnMenuItemClickListener(item -> {
            android.util.Log.d("DriveLog", "Menu clicado: " + item.getTitle());
            if (item.getItemId() == 99) {
                shareCurrentRouteWithDevs();
            } else if (item.getItemId() == 100) {
                showSharedDeveloperRoutes();
            } else if (item.getItemId() == 101) {
                boolean n = !item.isChecked();
                item.setChecked(n);
                sharedPreferences.edit().putBoolean("dev_auto_share_routes", n).apply();
            } else if (item.getItemId() == 102) {
                boolean n = !item.isChecked();
                item.setChecked(n);
                sharedPreferences.edit().putBoolean("hide_delivered_stops", n).apply();
                refreshMarkers(); // Atualiza o mapa imediatamente
            } else if (item.getItemId() == 105) {
                boolean n = !item.isChecked();
                item.setChecked(n);
                sharedPreferences.edit().putBoolean("show_weather_balloon", n).apply();
                if (cardWeatherSummary != null) cardWeatherSummary.setVisibility(n ? View.VISIBLE : View.GONE);
            } else if (item.getItemId() == 103) {
                boolean n = !item.isChecked();
                item.setChecked(n);
                sharedPreferences.edit().putBoolean("advance_to_nearest", n).apply();
            } else if (item.getItemId() == 104) {
                boolean n = !item.isChecked();
                item.setChecked(n);
                sharedPreferences.edit().putBoolean("timer_on_cards_only", n).apply();
                // Força atualização imediata da visibilidade do balão
                if (cardRouteTotalTime != null) {
                    if (n) {
                        // Se ativou "apenas nos cards", só mostra se já estiver finalizada
                        cardRouteTotalTime.setVisibility(currentRouteHeader != null && currentRouteHeader.endTime > 0 ? View.VISIBLE : View.GONE);
                    } else {
                        // Se desativou, mostra se já tiver começado
                        cardRouteTotalTime.setVisibility(currentRouteHeader != null && currentRouteHeader.startTime > 0 ? View.VISIBLE : View.GONE);
                    }
                }
                if (stopsCardAdapter != null) stopsCardAdapter.notifyItemRangeChanged(0, currentStops.size(), "TIMER_UPDATE");
            } else if (item.getItemId() == 200) {
                sharedPreferences.edit().putString("side_fabs_alignment", "auto").apply();
                updateAppModeUI();
            } else if (item.getItemId() == 201) {
                sharedPreferences.edit().putString("side_fabs_alignment", "left").apply();
                updateAppModeUI();
            } else if (item.getItemId() == 202) {
                sharedPreferences.edit().putString("side_fabs_alignment", "right").apply();
                updateAppModeUI();
            } else if (item.getItemId() == 301) {
                boolean n = !item.isChecked();
                item.setChecked(n);
                sharedPreferences.edit().putBoolean("show_fab_delivery_app", n).apply();
                updateFloatingButtonsVisibility();
            } else if (item.getItemId() == 302) {
                boolean n = !item.isChecked();
                item.setChecked(n);
                sharedPreferences.edit().putBoolean("show_fab_report_hazard", n).apply();
                updateFloatingButtonsVisibility();
            } else if (item.getItemId() == 303) {
                boolean n = !item.isChecked();
                item.setChecked(n);
                sharedPreferences.edit().putBoolean("show_fab_center_map", n).apply();
                updateFloatingButtonsVisibility();
            } else if (item.getItemId() == 304) {
                boolean n = !item.isChecked();
                item.setChecked(n);
                sharedPreferences.edit().putBoolean("show_fab_orientation", n).apply();
                updateFloatingButtonsVisibility();
            } else if (item.getItemId() == 305) {
                boolean n = !item.isChecked();
                item.setChecked(n);
                sharedPreferences.edit().putBoolean("show_bottom_sheet_stops", n).apply();
                updateFloatingButtonsVisibility();
            } else if (item.getItemId() == 306) {
                boolean n = !item.isChecked();
                item.setChecked(n);
                sharedPreferences.edit().putBoolean("show_fab_km_tracking", n).apply();
                updateFloatingButtonsVisibility();
            } else if ("Otimizar Rota".equals(item.getTitle())) {
                optimizeRoute();
            } else if ("Otimizar Rota 2.0".equals(item.getTitle())) {
                optimizeRouteV2();
            } else if ("Desenhar Rota (Laço)".equals(item.getTitle())) {
                enterLassoMode();
            } else if ("Inverter Ordem".equals(item.getTitle())) {
                reverseRouteOrder();
            } else if ("Limpar Rota".equals(item.getTitle())) {
                promptClearRoute();
            } else if ("Importar Planilha".equals(item.getTitle())) {
                startXlsxImport();
            }
            return true;
        });
        p.show();
    }

    private void enterLassoMode() {
        isLassoMode = true;
        isLassoDrawingEnabled = false;
        if (cardLassoMode != null) cardLassoMode.setVisibility(View.VISIBLE);
        if (lassoOverlay == null) {
            lassoOverlay = new LassoOverlay();
            map.getOverlays().add(lassoOverlay);
        }
        lassoOverlay.clear();
        lassoGroupCounter = 0;
        updateLassoButtonText();
        map.invalidate();
        Toast.makeText(getContext(), "Modo Laço: Clique no botão para começar", Toast.LENGTH_SHORT).show();
    }

    private void updateLassoButtonText() {
        if (btnStartLassoDraw != null) {
            String text = "Desenhar o " + (lassoGroupCounter + 1) + "º grupo";
            btnStartLassoDraw.setText(text);
        }
    }

    private void promptClearRoute() {
        if (getContext() == null) return;
        
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_modern_confirm, null);
        TextView title = dialogView.findViewById(R.id.textModernTitle);
        TextView message = dialogView.findViewById(R.id.textModernMessage);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnModernNegative);
        com.google.android.material.button.MaterialButton btnConfirm = dialogView.findViewById(R.id.btnModernPositive);

        title.setText("Limpar Rota");
        message.setText("Deseja remover todas as paradas desta rota?");
        btnConfirm.setText("LIMPAR");

        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(dialogView).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            new Thread(() -> {
                AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
                dao.clearRouteStopsByRoute(currentRouteId);
                if (isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Rota limpa!", Toast.LENGTH_SHORT).show();
                        CloudSyncHelper.syncNow(requireContext(), "Atividade na Rota");
                    });
                }
            }).start();
        });
        dialog.show();
    }

    private void startLassoDrawing() { 
        isLassoDrawingEnabled = true; 
        map.setMultiTouchControls(false); 
        Toast.makeText(getContext(), "Desenhe no mapa a área do " + (lassoGroupCounter + 1) + "º grupo", Toast.LENGTH_SHORT).show();
    }
    private void undoLastLasso() { if (lassoOverlay!=null) { lassoOverlay.undoLastPath(); map.invalidate(); } }
    private void exitLassoMode() { 
        isLassoMode = false; 
        cardLassoMode.setVisibility(View.GONE); 
        map.setMultiTouchControls(true); 
        if (lassoOverlay != null) {
            lassoOverlay.clear(); // Limpa os desenhos ao sair
        }
        map.invalidate();
    }

    private class LassoOverlay extends org.osmdroid.views.overlay.Overlay {
        private List<List<GeoPoint>> allPaths = new ArrayList<>();
        private List<GeoPoint> currentPath = new ArrayList<>();
        private Paint paint = new Paint();
        private boolean isDrawing = false;

        LassoOverlay() {
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5f);
            paint.setAntiAlias(true);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
        }

        void clear() {
            allPaths.clear();
            currentPath.clear();
            isDrawing = false;
        }

        void undoLastPath() {
            if (!allPaths.isEmpty()) allPaths.remove(allPaths.size() - 1);
        }

        @Override
        public void draw(Canvas canvas, org.osmdroid.views.Projection projection) {
            android.graphics.Path path = new android.graphics.Path();
            // Desenha caminhos finalizados
            for (List<GeoPoint> pts : allPaths) {
                if (pts.size() < 2) continue;
                path.reset();
                android.graphics.Point p0 = projection.toPixels(pts.get(0), null);
                path.moveTo(p0.x, p0.y);
                for (int i = 1; i < pts.size(); i++) {
                    android.graphics.Point p = projection.toPixels(pts.get(i), null);
                    path.lineTo(p.x, p.y);
                }
                canvas.drawPath(path, paint);
            }
            // Desenha caminho atual
            if (currentPath.size() >= 2) {
                path.reset();
                android.graphics.Point p0 = projection.toPixels(currentPath.get(0), null);
                path.moveTo(p0.x, p0.y);
                for (int i = 1; i < currentPath.size(); i++) {
                    android.graphics.Point p = projection.toPixels(currentPath.get(i), null);
                    path.lineTo(p.x, p.y);
                }
                canvas.drawPath(path, paint);
            }
        }

        @Override
        public boolean onTouchEvent(android.view.MotionEvent event, MapView mapView) {
            if (!isLassoDrawingEnabled) return false;

            GeoPoint gp = (GeoPoint) mapView.getProjection().fromPixels((int) event.getX(), (int) event.getY());

            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    isDrawing = true;
                    currentPath.clear();
                    currentPath.add(gp);
                    mapView.invalidate();
                    return true;

                case android.view.MotionEvent.ACTION_MOVE:
                    if (isDrawing) {
                        currentPath.add(gp);
                        mapView.invalidate();
                        return true;
                    }
                    break;

                case android.view.MotionEvent.ACTION_UP:
                    if (isDrawing) {
                        isDrawing = false;
                        if (currentPath.size() > 5) {
                            List<GeoPoint> finishedPath = new ArrayList<>(currentPath);
                            allPaths.add(finishedPath);
                            isLassoDrawingEnabled = false; // Pausa o desenho para confirmar
                            map.setMultiTouchControls(true); // Reativa movimento do mapa durante a confirmação
                            
                            View cv = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_modern_confirm, null);
                            TextView tt = cv.findViewById(R.id.textModernTitle);
                            TextView tm = cv.findViewById(R.id.textModernMessage);
                            MaterialButton bn = cv.findViewById(R.id.btnModernNegative);
                            MaterialButton bp = cv.findViewById(R.id.btnModernPositive);

                            tt.setText("Confirmar Grupo");
                            tm.setText("Deseja criar este grupo com as paradas selecionadas?");
                            bn.setText("DESFAZER");
                            bp.setText("CONFIRMAR");

                            AlertDialog confirmDialog = new AlertDialog.Builder(requireContext()).setView(cv).setCancelable(false).create();
                            if (confirmDialog.getWindow() != null) confirmDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                            
                            bp.setOnClickListener(v2 -> {
                                confirmDialog.dismiss();
                                processGroupSelection(finishedPath);
                                promptNextGroup();
                            });

                            bn.setOnClickListener(v2 -> {
                                confirmDialog.dismiss();
                                allPaths.remove(finishedPath);
                                isLassoDrawingEnabled = true;
                                map.setMultiTouchControls(false);
                                map.invalidate();
                            });

                            confirmDialog.show();
                        }
                        currentPath.clear();
                        mapView.invalidate();
                        return true;
                    }
                    break;
            }
            return false;
        }

        private void promptNextGroup() {
            View cv = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_modern_confirm, null);
            TextView tt = cv.findViewById(R.id.textModernTitle);
            TextView tm = cv.findViewById(R.id.textModernMessage);
            MaterialButton bn = cv.findViewById(R.id.btnModernNegative);
            MaterialButton bp = cv.findViewById(R.id.btnModernPositive);

            tt.setText("Próximo Grupo");
            tm.setText("Deseja desenhar o próximo grupo agora?");
            bn.setText("NÃO, FINALIZAR");
            bp.setText("SIM, CONTINUAR");

            AlertDialog nextDialog = new AlertDialog.Builder(requireContext()).setView(cv).setCancelable(false).create();
            if (nextDialog.getWindow() != null) nextDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

            bp.setOnClickListener(v -> {
                nextDialog.dismiss();
                lassoGroupCounter++;
                updateLassoButtonText();
                Toast.makeText(getContext(), "Desenhe a próxima área no mapa", Toast.LENGTH_SHORT).show();
                // O usuário deve clicar no botão de desenho para travar o mapa novamente
                // se quisermos manter a lógica de "Mapa Livre" entre os passos.
            });

            bn.setOnClickListener(v -> {
                nextDialog.dismiss();
                exitLassoMode();
            });

            nextDialog.show();
        }

        private void processGroupSelection(List<GeoPoint> poly) {
            // Algoritmo de ponto em polígono para agrupar
            new Thread(() -> {
                AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
                int nextNum = lassoGroupCounter + 1;
                String groupName = "Laço " + nextNum;
                String color = String.format("#%06X", (0xFFFFFF & Color.HSVToColor(new float[]{(nextNum * 77) % 360, 0.8f, 0.9f})));
                
                long groupId = dao.insertRouteGroup(new RouteGroup(groupName, color, currentRouteId));
                List<RouteStop> toUpdate = new ArrayList<>();
                
                for (RouteStop s : currentStops) {
                    if (isPointInPolygon(new GeoPoint(s.latitude, s.longitude), poly)) {
                        s.groupId = (int) groupId;
                        toUpdate.add(s);
                    }
                }
                
                if (!toUpdate.isEmpty()) {
                    dao.updateRouteStops(toUpdate);
                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.runOnUiThread(() -> {
                            Toast.makeText(getContext(), groupName + " criado com " + toUpdate.size() + " paradas!", Toast.LENGTH_SHORT).show();
                            CloudSyncHelper.syncNow(requireContext(), "Atividade na Rota");
                        });
                    }
                }
            }).start();
        }

        private boolean isPointInPolygon(GeoPoint p, List<GeoPoint> poly) {
            boolean result = false;
            for (int i = 0, j = poly.size() - 1; i < poly.size(); j = i++) {
                if ((poly.get(i).getLatitude() > p.getLatitude()) != (poly.get(j).getLatitude() > p.getLatitude()) &&
                        (p.getLongitude() < (poly.get(j).getLongitude() - poly.get(i).getLongitude()) * (p.getLatitude() - poly.get(i).getLatitude()) / (poly.get(j).getLatitude() - poly.get(i).getLatitude()) + poly.get(i).getLongitude())) {
                    result = !result;
                }
            }
            return result;
        }
    }

    private void fetchWeather() {
        if (currentLocation == null) {
            new Handler(Looper.getMainLooper()).postDelayed(this::fetchWeather, 5000);
            return;
        }

        final double lat = currentLocation.getLatitude();
        final double lon = currentLocation.getLongitude();
        lastWeatherLocation = currentLocation;
        lastWeatherUpdate = System.currentTimeMillis();

        new Thread(() -> {
            try {
                // 1. Buscar Clima (Open-Meteo) - Agora incluindo HOURLY
                String urlStr = String.format(Locale.US, "https://api.open-meteo.com/v1/forecast?latitude=%.6f&longitude=%.6f&current_weather=true&hourly=temperature_2m,weathercode", lat, lon);
                java.net.URL url = new java.net.URL(urlStr);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                org.json.JSONObject json = new org.json.JSONObject(sb.toString());
                
                // Dados Atuais
                org.json.JSONObject current = json.getJSONObject("current_weather");
                final double temp = current.getDouble("temperature");
                final int code = current.getInt("weathercode");

                // Dados Por Hora (Próximas 24 horas)
                org.json.JSONObject hourly = json.getJSONObject("hourly");
                org.json.JSONArray times = hourly.getJSONArray("time");
                org.json.JSONArray temps = hourly.getJSONArray("temperature_2m");
                org.json.JSONArray codes = hourly.getJSONArray("weathercode");

                List<DayWeather> weekList = new ArrayList<>();
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US);
                SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", new Locale("pt", "BR"));

                for (int d = 0; d < 7; d++) {
                    List<HourlyWeather> hourlyList = new ArrayList<>();
                    String dayLabel = "";
                    Map<Integer, Integer> codeCounts = new HashMap<>();
                    
                    for (int h = 0; h < 24; h++) {
                        int idx = d * 24 + h;
                        if (idx >= times.length()) break;
                        
                        String fullTime = times.getString(idx);
                        String timeOnly = fullTime.split("T")[1];
                        int hCode = codes.getInt(idx);
                        hourlyList.add(new HourlyWeather(timeOnly, temps.getDouble(idx), hCode));
                        
                        // Contabiliza códigos entre 08h e 18h para o ícone do dia
                        if (h >= 8 && h <= 18) {
                            codeCounts.put(hCode, codeCounts.getOrDefault(hCode, 0) + 1);
                        }

                        if (h == 0) {
                            if (d == 0) dayLabel = "Hoje";
                            else if (d == 1) dayLabel = "Amanhã";
                            else {
                                try {
                                    java.util.Date date = inputFormat.parse(fullTime);
                                    dayLabel = dayFormat.format(date).toUpperCase();
                                } catch (Exception e) { dayLabel = "Dia " + (d+1); }
                            }
                        }
                    }

                    // Define o código dominante (Prioriza chuva/tempestade se houver)
                    int dominant = 0;
                    if (!codeCounts.isEmpty()) {
                        int maxFreq = -1;
                        for (int c : codeCounts.keySet()) {
                            int freq = codeCounts.get(c);
                            // Se tiver chuva/raio pelo menos 2 horas, prioriza mostrar
                            if (c >= 51 && freq >= 2) { dominant = c; break; }
                            if (freq > maxFreq) { maxFreq = freq; dominant = c; }
                        }
                    }

                    if (!hourlyList.isEmpty()) weekList.add(new DayWeather(dayLabel, hourlyList, dominant));
                    if (weekList.size() >= 7) break;
                }
                lastWeekWeather = weekList;

                // 2. Buscar Nome da Cidade (Nominatim Reverse Geocoding)
                String cityName = "";
                try {
                    String uniqueId = android.provider.Settings.Secure.getString(requireContext().getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                    String userAgent = "DriveLogApp_v142_" + uniqueId;
                    String geoUrl = String.format(Locale.US, "https://nominatim.openstreetmap.org/reverse?lat=%.6f&lon=%.6f&format=json&zoom=10", lat, lon);
                    java.net.URL urlGeo = new java.net.URL(geoUrl);
                    java.net.HttpURLConnection connGeo = (java.net.HttpURLConnection) urlGeo.openConnection();
                    connGeo.setRequestProperty("User-Agent", userAgent);
                    java.io.BufferedReader readerGeo = new java.io.BufferedReader(new java.io.InputStreamReader(connGeo.getInputStream()));
                    StringBuilder sbGeo = new StringBuilder();
                    while ((line = readerGeo.readLine()) != null) sbGeo.append(line);
                    readerGeo.close();
                    
                    org.json.JSONObject jsonGeo = new org.json.JSONObject(sbGeo.toString());
                    if (jsonGeo.has("address")) {
                        org.json.JSONObject addr = jsonGeo.getJSONObject("address");
                        if (addr.has("city")) cityName = addr.getString("city");
                        else if (addr.has("town")) cityName = addr.getString("town");
                        else if (addr.has("village")) cityName = addr.getString("village");
                        else if (addr.has("suburb")) cityName = addr.getString("suburb");
                        else if (addr.has("neighbourhood")) cityName = addr.getString("neighbourhood");
                        else if (addr.has("municipality")) cityName = addr.getString("municipality");
                    }
                } catch (Exception ignored) {}

                final String finalCity = cityName;
                lastCityName = finalCity;
                Activity activity = getActivity();
                if (activity != null) activity.runOnUiThread(() -> updateWeatherUI(temp, code, finalCity));
            } catch (Exception e) {
                android.util.Log.e("DriveLog", "Erro ao buscar clima: " + e.getMessage());
            }
        }).start();
    }

    private int getWeatherIconRes(int code) {
        if (code == 0) return R.drawable.ic_weather_sun;
        if (code >= 1 && code <= 3) return R.drawable.ic_weather_cloud;
        if (code >= 51 && code <= 67) return R.drawable.ic_weather_rain;
        if (code >= 95) return R.drawable.ic_weather_thunder;
        return R.drawable.ic_weather_sun;
    }

    private void updateWeatherUI(double temp, int code, String city) {
        if (textWeatherTemp != null) {
            textWeatherTemp.setText(String.format(Locale.getDefault(), "%.1f°C", temp));
        }
        if (textWeatherCity != null) {
            if (city != null && !city.isEmpty()) {
                textWeatherCity.setText(city);
                textWeatherCity.setVisibility(View.VISIBLE);
            } else {
                textWeatherCity.setVisibility(View.GONE);
            }
        }
        if (imageWeatherIcon != null) {
            imageWeatherIcon.setImageResource(getWeatherIconRes(code));
        }
        boolean showWeather = sharedPreferences.getBoolean("show_weather_balloon", true);
        if (cardWeatherSummary != null) cardWeatherSummary.setVisibility(showWeather ? View.VISIBLE : View.GONE);
    }

    private void showWeatherHourlyPopup() {
        if (getContext() == null || lastWeekWeather.isEmpty()) return;
        
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_weather_hourly, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(v).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView textCity = v.findViewById(R.id.textWeatherPopupCity);
        TextView textTitle = v.findViewById(R.id.textWeatherPopupTitle);
        
        if (lastCityName != null && !lastCityName.isEmpty()) {
            textCity.setText("- " + lastCityName);
            textCity.setVisibility(View.VISIBLE);
        } else {
            textCity.setVisibility(View.GONE);
        }

        com.google.android.material.tabs.TabLayout tabLayout = v.findViewById(R.id.tabWeatherDays);
        RecyclerView rv = v.findViewById(R.id.recyclerWeatherHourly);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        
        HourlyWeatherAdapter adapter = new HourlyWeatherAdapter(lastWeekWeather.get(0).hourly);
        rv.setAdapter(adapter);

        // Preencher abas
        for (DayWeather day : lastWeekWeather) {
            tabLayout.addTab(tabLayout.newTab()
                    .setText(day.label)
                    .setIcon(getWeatherIconRes(day.dominantCode)));
        }

        if (textTitle != null) textTitle.setText("Previsão " + lastWeekWeather.get(0).label);

        tabLayout.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                int pos = tab.getPosition();
                if (pos >= 0 && pos < lastWeekWeather.size()) {
                    adapter.setList(lastWeekWeather.get(pos).hourly);
                    if (textTitle != null) {
                        textTitle.setText("Previsão " + lastWeekWeather.get(pos).label);
                    }
                }
            }
            @Override public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
            @Override public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
        });

        v.findViewById(R.id.btnWeatherPopupClose).setOnClickListener(v2 -> dialog.dismiss());
        dialog.show();
    }

    private void reverseRouteOrder() { 
        new Thread(() -> { 
            AppDao dao = AppDatabase.getInstance(requireContext()).appDao(); 
            List<RouteStop> s = dao.getStopsForRoute(currentRouteId); 
            java.util.Collections.reverse(s); 
            for (int i=0; i<s.size(); i++) { 
                s.get(i).sortOrder = i; 
                s.get(i).stopNumber = i + 1; 
            } 
            dao.updateRouteStops(s); 
            Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(() -> CloudSyncHelper.syncNow(requireContext())); 
            }
        }).start(); 
    }

    private void showTempMarker(GeoPoint p, String t) { 
        if (map == null || !isAdded()) return; 
        map.getOverlays().removeIf(o -> o instanceof Marker && "TEMP".equals(((Marker)o).getRelatedObject())); 
        Marker m = new Marker(map); 
        m.setInfoWindow(null); // Evita NPE e limpa a tela
        m.setRelatedObject("TEMP"); 
        m.setPosition(p); 
        m.setTitle(t); 
        map.getOverlays().add(m); 
        map.invalidate(); 
    }

    private void confirmAddStop() {
        if (lastSearchedPoint == null) {
            Toast.makeText(getContext(), "Pesquise um endereço primeiro ou use a adição manual (+)", Toast.LENGTH_SHORT).show();
            return;
        }
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_modern_confirm, null);
        TextView title = dialogView.findViewById(R.id.textModernTitle);
        TextView message = dialogView.findViewById(R.id.textModernMessage);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnModernNegative);
        com.google.android.material.button.MaterialButton btnConfirm = dialogView.findViewById(R.id.btnModernPositive);

        title.setText("Confirmar Parada");
        message.setText(lastSearchedAddress);
        btnConfirm.setText("ADICIONAR");

        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(dialogView).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            saveStopToDb(lastSearchedAddress, "", lastSearchedPoint.getLatitude(), lastSearchedPoint.getLongitude());
        });
        dialog.show();
    }

    private void saveStopToDb(String a, double la, double lo) {
        saveStopToDb(a, "", la, lo);
    }

    private void saveStopToDb(String address, String neighborhood, double lat, double lon) {
        if (getContext() == null || currentRouteId == -1) return;
        final int routeId = currentRouteId;
        new Thread(() -> {
            AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
            RouteStop s = new RouteStop(address, lat, lon);
            s.routeId = routeId;
            s.neighborhood = neighborhood;
            List<RouteStop> existing = dao.getStopsForRoute(routeId);
            s.sortOrder = existing.size();
            s.stopNumber = existing.size() + 1;
            dao.insertRouteStop(s);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (editSearch != null) editSearch.setText("");
                    
                    // 🔥 Ativa automaticamente o card de paradas ao adicionar uma parada manualmente
                    sharedPreferences.edit().putBoolean("show_bottom_sheet_stops", true).apply();

                    Toast.makeText(getContext(), "Parada adicionada!", Toast.LENGTH_SHORT).show();
                    CloudSyncHelper.syncNow(requireContext(), "Atividade na Rota");
                });
            }
        }).start();
    }

    private void loadStopsForCurrentRoute() { 
        if (currentRouteId == -1 || getContext() == null) return; 
        
        AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
        
        // 🔥 Evita re-atachar o mesmo observer se a rota não mudou
        // Isso previne flickering e bugs de celebração ao atualizar paradas
        if (currentStopsLive != null) {
            currentStopsLive.removeObservers(getViewLifecycleOwner());
        }
        
        isPositionRestored = false;
        
        // Monitora paradas
        currentStopsLive = dao.getStopsForRouteLive(currentRouteId);
        currentStopsLive.observe(getViewLifecycleOwner(), stops -> { 
            if (stops == null) return;
            currentStops = new ArrayList<>(stops); 
            refreshMarkers(); 
            
            // Atualiza Estatísticas
            int okStops = 0, okPackages = 0;
            int errStops = 0;
            int pendStops = 0, pendPackages = 0;

            for (RouteStop s : stops) {
                if (s.deliveryStatus == 1) { // Sucesso
                    okStops++;
                    okPackages += s.packageCount;
                } else if (s.deliveryStatus == 2) { // Falha
                    errStops++;
                } else { // Pendente
                    pendStops++;
                    pendPackages += s.packageCount;
                }
            }

            animateNumber(textSuccessCount, okStops);
            animateNumber(textSuccessPackageCount, okPackages);
            animateNumber(textFailedCount, errStops);
            animateNumber(textPendingCount, pendStops);
            animateNumber(textPendingPackageCount, pendPackages);

            if (cardFailedSummary != null && autoHideRunnable == null) {
                cardFailedSummary.setVisibility(errStops > 0 ? View.VISIBLE : View.GONE);
            }

            // Efeito de celebração se a rota foi concluída (nenhuma pendente)
            String celebrationKey = "last_finished_route_" + currentRouteId;
            boolean alreadyCelebrated = sharedPreferences.getBoolean(celebrationKey, false);
            
            if (!stops.isEmpty() && pendStops == 0) {
                if (!alreadyCelebrated) {
                    android.util.Log.d("DriveLog", "Rota concluída! Disparando celebração para rota: " + currentRouteId);
                    Toast.makeText(getContext(), "🎉 ROTA CONCLUÍDA!", Toast.LENGTH_LONG).show();
                    triggerCelebration();
                }
            } else if (!stops.isEmpty() && pendStops > 0 && alreadyCelebrated) {
                // Se voltou a ter paradas pendentes, permite celebrar de novo quando terminar
                sharedPreferences.edit().remove(celebrationKey).apply();
                android.util.Log.d("DriveLog", "Resetando flag de celebração pois há paradas pendentes na rota: " + currentRouteId);
            }

            boolean showStopsCard = sharedPreferences.getBoolean("show_bottom_sheet_stops", true);
            if (stops.size() > 0 && pendStops == 0 && showStopsCard) {
                // Auto-oculta o card ao finalizar, mas permite reativar manualmente no menu
                sharedPreferences.edit().putBoolean("show_bottom_sheet_stops", false).apply();
                showStopsCard = false;
            }

            if (stopsCardAdapter != null) { 
                stopsCardAdapter.setStops(stops); 
                bottomSheet.setVisibility((stops.isEmpty() || !showStopsCard) ? View.GONE : View.VISIBLE); 
            } 
            if (stopsListAdapter != null) stopsListAdapter.setStops(stops); 
            
            updateFloatingButtonsVisibility(); // 🔥 Força atualização dos FABs (incluindo o Nova Rota) ao mudar paradas
            updateFabsPosition(); 

            // 🔥 RESTAURAÇÃO DA POSIÇÃO
            if (!isPositionRestored && !stops.isEmpty() && viewPagerStops != null) {
                if (pendingRestoreIndex >= 0 && pendingRestoreIndex < stops.size()) {
                    final int indexToRestore = pendingRestoreIndex;
                    viewPagerStops.post(() -> {
                        viewPagerStops.setCurrentItem(indexToRestore, false);
                        isPositionRestored = true;
                        pendingRestoreIndex = -1;
                    });
                } else {
                    isPositionRestored = true;
                }
            }
        });

        // Monitora o Header para o timer
        if (currentHeaderLive != null) {
            currentHeaderLive.removeObservers(getViewLifecycleOwner());
        }
        currentHeaderLive = dao.getRouteByIdLive(currentRouteId);
        currentHeaderLive.observe(getViewLifecycleOwner(), header -> {
            this.currentRouteHeader = header;
            if (stopsCardAdapter != null) stopsCardAdapter.setRouteHeader(header);
        });

        // 🔥 Monitora grupos para cores
        if (currentGroupsLive != null) {
            currentGroupsLive.removeObservers(getViewLifecycleOwner());
        }

        currentGroupsLive = dao.getGroupsForRouteLive(currentRouteId);
        currentGroupsLive.observe(getViewLifecycleOwner(), groups -> {
            if (stopsListAdapter != null) stopsListAdapter.updateGroupColors(groups);
            refreshMarkers(); // Repinta marcadores se as cores dos grupos mudarem
        });
    }

    private void refreshMarkers() { 
        if (map == null || currentStops == null) return; 
        
        final List<RouteStop> stopsSnapshot = new ArrayList<>(currentStops);
        final int selectedIndex = (viewPagerStops != null) ? viewPagerStops.getCurrentItem() : 0;
        final int targetRouteId = currentRouteId; 
        final boolean hideDelivered = sharedPreferences.getBoolean("hide_delivered_stops", false);

        new Thread(() -> {
            if (getContext() == null) return;
            AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
            List<RouteGroup> groups = dao.getGroupsForRoute(targetRouteId);
            Map<Integer, String> colorMap = new HashMap<>();
            for (RouteGroup g : groups) colorMap.put(g.id, g.color);

            List<Bitmap> bitmaps = new ArrayList<>();
            for (int i = 0; i < stopsSnapshot.size(); i++) {
                RouteStop s = stopsSnapshot.get(i);
                
                // Se a opção estiver ativa e a parada for status 1 (Sucesso/Entregue), não gera o bitmap
                if (hideDelivered && s.deliveryStatus == 1) {
                    bitmaps.add(null);
                } else {
                    String gColor = (s.groupId != null) ? colorMap.get(s.groupId) : null;
                    bitmaps.add(generateMarkerBitmap(i+1, s.deliveryStatus, s.packageCount > 1, (i == selectedIndex), gColor));
                }
            }

            Activity activity = getActivity();
            if (activity != null) activity.runOnUiThread(() -> {
                // 🔥 Verificações de segurança para evitar NPE se o fragmento foi fechado
                if (map == null || currentRouteId != targetRouteId || !isAdded()) return;
                
                List<org.osmdroid.views.overlay.Overlay> toAdd = new ArrayList<>();
                for (int i = 0; i < stopsSnapshot.size(); i++) { 
                    Bitmap markerBmp = bitmaps.get(i);
                    if (markerBmp == null) continue; // Pula paradas ocultas

                    RouteStop s = stopsSnapshot.get(i); 
                    
                    if (s.latitude == 0 && s.longitude == 0 || map == null) continue;

                    Marker m = new Marker(map) {
                        private final Handler longClickHandler = new Handler(Looper.getMainLooper());
                        private Runnable longClickRunnable;
                        private boolean isLongClickTriggered = false;

                        @Override
                        public boolean onTouchEvent(android.view.MotionEvent event, MapView mapView) {
                            if (hitTest(event, mapView)) {
                                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                                    isLongClickTriggered = false;
                                    longClickRunnable = () -> {
                                        isLongClickTriggered = true;
                                        if (isAdded()) deleteStopDialog(s);
                                    };
                                    longClickHandler.postDelayed(longClickRunnable, 800);
                                } else if (event.getAction() == android.view.MotionEvent.ACTION_UP || event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                                    longClickHandler.removeCallbacks(longClickRunnable);
                                    if (isLongClickTriggered) return true;
                                }
                            } else {
                                longClickHandler.removeCallbacks(longClickRunnable);
                            }
                            return super.onTouchEvent(event, mapView);
                        }
                    }; 
                    m.setInfoWindow(null); 
                    m.setRelatedObject("STOP_INDEX_" + i); // 🔥 Identificador único baseado no índice, não na coordenada
                    m.setPosition(new GeoPoint(s.latitude, s.longitude)); 
                    m.setIcon(new BitmapDrawable(getResources(), bitmaps.get(i)));
                    
                    final int index = i;
                    m.setOnMarkerClickListener((marker, mapView) -> {
                        if (viewPagerStops != null) viewPagerStops.setCurrentItem(index, true);
                        return true;
                    });
                    toAdd.add(m);
                } 

                if (map == null) return;

                // Otimização: Coleta os overlays que NÃO são paradas para reinserir
                List<org.osmdroid.views.overlay.Overlay> currentOverlays = map.getOverlays();
                List<org.osmdroid.views.overlay.Overlay> nonStopOverlays = new ArrayList<>();
                for (org.osmdroid.views.overlay.Overlay o : currentOverlays) {
                    Object tag = (o instanceof Marker) ? ((Marker) o).getRelatedObject() : null;
                    if (!(tag instanceof String && ((String) tag).startsWith("STOP_INDEX_"))) {
                        nonStopOverlays.add(o);
                    }
                }

                // Limpa e reconstrói a lista de overlays de uma vez (evita removeIf lento)
                currentOverlays.clear();
                currentOverlays.addAll(nonStopOverlays);

                int userIndex = -1;
                for (int i = 0; i < currentOverlays.size(); i++) {
                    if (currentOverlays.get(i) instanceof MyLocationNewOverlay) {
                        userIndex = i;
                        break;
                    }
                }

                for (int i = 0; i < toAdd.size(); i++) {
                    if (i == selectedIndex) {
                        currentOverlays.add(toAdd.get(i));
                    } else {
                        if (userIndex != -1 && userIndex < currentOverlays.size()) {
                            currentOverlays.add(userIndex, toAdd.get(i));
                            userIndex++;
                        } else {
                            currentOverlays.add(toAdd.get(i));
                        }
                    }
                }
                map.invalidate();
            });
        }).start();
    }

    private int markerRotationAngle = 0;
    private final Handler animationHandler = new Handler(Looper.getMainLooper());
    private final Runnable markerAnimationRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAdded() && map != null && !isRestIntervalNow()) {
                // Rotação baseada no tempo para ser constante e um pouco mais lenta
                markerRotationAngle = (int) ((System.currentTimeMillis() / 10) % 360);
                updateSelectedMarkerIcon();
                animationHandler.postDelayed(this, 150); // Aumentado para 150ms para evitar ANR
            } else {
                animationHandler.postDelayed(this, 1000); // Se parado ou em descanso, checa menos
            }
        }
    };

    private void updateSelectedMarkerIcon() {
        if (map == null || viewPagerStops == null || currentStops.isEmpty() || !isAdded()) return;
        
        // 🔥 Economia: Se o mapa não está visível ou o BottomSheet está expandido (escondendo o mapa), não anima
        if (bottomSheetBehavior != null && bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) return;

        int sel = viewPagerStops.getCurrentItem();
        if (sel < 0 || sel >= currentStops.size()) return;
        
        String targetTag = "STOP_INDEX_" + sel;
        
        // Otimização: Apenas um loop simples para achar o marcador certo
        List<org.osmdroid.views.overlay.Overlay> overlays = map.getOverlays();
        for (int i = overlays.size() - 1; i >= 0; i--) {
            org.osmdroid.views.overlay.Overlay o = overlays.get(i);
            if (o instanceof Marker) {
                Marker m = (Marker) o;
                if (targetTag.equals(m.getRelatedObject())) {
                    RouteStop s = currentStops.get(sel);
                    String gColor = (s.groupId != null && stopsListAdapter != null) ? stopsListAdapter.groupColors.get(s.groupId) : null;
                    
                    // Gera o bitmap apenas para o marcador selecionado
                    Bitmap animatedBitmap = generateMarkerBitmap(sel + 1, s.deliveryStatus, s.packageCount > 1, true, gColor);
                    m.setIcon(new BitmapDrawable(getResources(), animatedBitmap));
                    map.invalidate();
                    return;
                }
            }
        }
    }

    private Bitmap generateMarkerBitmap(int n, int status, boolean multi, boolean selected, String gColor) {
        int size = selected ? 110 : 80;
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b); 
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        
        int stopColor = Color.parseColor("#2196F3");
        int alpha = 255;
        if (status == 1) { stopColor = Color.parseColor("#4CAF50"); alpha = 130; } 
        else if (status == 2) { stopColor = Color.parseColor("#F44336"); }
        else if (multi) { stopColor = Color.parseColor("#FF9800"); }
        else if (gColor != null) { try { stopColor = Color.parseColor(gColor); } catch (Exception ignored) {} }

        float center = size / 2f;
        float radius = size / 2.6f;

        // 1. Círculo Central Preenchido
        p.setColor(stopColor);
        p.setAlpha(alpha);
        p.setStyle(Paint.Style.FILL);
        c.drawCircle(center, center, radius, p);

        // 2. Desenhar Borda
        if (selected) {
            // Efeito de Carregamento (Spinner)
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(8f);
            
            // SweepGradient: Começa com a cor da parada e tem um arco branco
            // Usamos uma transição nítida para parecer uma "peça" girando
            int[] colors = {stopColor, stopColor, Color.WHITE, stopColor, stopColor};
            float[] positions = {0.0f, 0.30f, 0.5f, 0.70f, 1.0f};
            
            android.graphics.SweepGradient gradient = new android.graphics.SweepGradient(center, center, colors, positions);
            
            android.graphics.Matrix matrix = new android.graphics.Matrix();
            matrix.postRotate(markerRotationAngle, center, center);
            gradient.setLocalMatrix(matrix);
            
            p.setShader(gradient);
            c.drawCircle(center, center, radius, p);
            p.setShader(null);
            
            // Borda externa fina para acabamento
            p.setStrokeWidth(1f);
            p.setColor(Color.WHITE);
            c.drawCircle(center, center, radius + 4f, p);
        } else {
            // Borda Simples Branca
            p.setColor(Color.WHITE);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(4f);
            p.setAlpha(alpha);
            c.drawCircle(center, center, radius, p);
        }

        // 3. Texto (Número)
        p.setStyle(Paint.Style.FILL);
        p.setTextSize(selected ? 44 : 32);
        p.setTextAlign(Paint.Align.CENTER);
        p.setAlpha(alpha);
        if (stopColor == Color.parseColor("#FF9800")) p.setColor(Color.BLACK); else p.setColor(Color.WHITE);
        
        // Ajuste vertical do texto
        Paint.FontMetrics fm = p.getFontMetrics();
        float textY = center - (fm.ascent + fm.descent) / 2;
        c.drawText(String.valueOf(n), center, textY, p);

        return b;
    }

    private Drawable createNumberedMarkerIcon(int n, int status, boolean multi, boolean selected, String gColor) {
        return new BitmapDrawable(getResources(), generateMarkerBitmap(n, status, multi, selected, gColor));
    }

    private void updateSelectionTrace(RouteStop stop) {
        if (map == null || currentLocation == null || stop.latitude == 0) return;
        
        if (switchTraceLine != null && !switchTraceLine.isChecked()) {
            if (selectionTracePolyline != null) {
                map.getOverlays().remove(selectionTracePolyline);
                selectionTracePolyline = null;
            }
            if (cardNavigationMode != null) cardNavigationMode.setVisibility(View.GONE);
            map.invalidate();
            return;
        }

        new Thread(() -> {
            try {
                String uniqueId = android.provider.Settings.Secure.getString(requireContext().getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                String userAgent = "DriveLogApp_v141_" + uniqueId;
                
                String u = String.format(Locale.US, "https://router.project-osrm.org/route/v1/driving/%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson&steps=true", currentLocation.getLongitude(), currentLocation.getLatitude(), stop.longitude, stop.latitude);
                HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection(); 
                c.setRequestProperty("User-Agent", userAgent);
                if (c.getResponseCode() == 200) {
                    BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream())); StringBuilder res = new StringBuilder(); String l; while((l=r.readLine())!=null) res.append(l);
                    JSONObject json = new JSONObject(res.toString());
                    JSONArray routes = json.getJSONArray("routes");
                    if (routes.length() > 0) {
                        JSONObject route = routes.getJSONObject(0);
                        double distanceMeters = route.getDouble("distance");
                        double durationSeconds = route.optDouble("duration", 0);
                        
                        // Extração de Manobras (Próximo Passo)
                        String instruction = "";
                        double nextStepDist = 0;
                        String maneuverType = "";
                        String modifier = "";
                        List<NavInstruction> allInstructions = new ArrayList<>();
                        
                        if (route.has("legs")) {
                            JSONArray legs = route.getJSONArray("legs");
                            if (legs.length() > 0) {
                                JSONArray steps = legs.getJSONObject(0).getJSONArray("steps");
                                for (int s = 0; s < steps.length(); s++) {
                                    JSONObject step = steps.getJSONObject(s);
                                    double sDist = step.getDouble("distance");
                                    JSONObject man = step.getJSONObject("maneuver");
                                    String mType = man.optString("type", "");
                                    String mMod = man.optString("modifier", "");
                                    String mName = step.optString("name", "");
                                    
                                    String inst = parseInstruction(mType, mMod, mName);
                                    allInstructions.add(new NavInstruction(inst, sDist, mType, mMod));
                                    
                                    if (s == 0 || (s == 1 && nextStepDist < 10)) {
                                        instruction = inst;
                                        nextStepDist = sDist;
                                        maneuverType = mType;
                                        modifier = mMod;
                                    }
                                }
                            }
                        }

                        final String finalInstruction = instruction;
                        final double finalNextDist = nextStepDist;
                        final String finalManeuver = maneuverType;
                        final String finalModifier = modifier;
                        final List<NavInstruction> finalAllInstructions = allInstructions;
                        final double finalDuration = durationSeconds;

                        JSONArray co = route.getJSONObject("geometry").getJSONArray("coordinates");
                        List<GeoPoint> pts = new ArrayList<>(); for(int i=0; i<co.length(); i++) pts.add(new GeoPoint(co.getJSONArray(i).getDouble(1), co.getJSONArray(i).getDouble(0)));
                        Activity activity = getActivity();
                        if (activity != null) activity.runOnUiThread(() -> {
                                if (map == null || !isAdded()) return;

                                lastRouteInstructions = finalAllInstructions;
                            // Criamos a nova linha antes de remover a antiga para evitar o "pisca"
                            // Usamos o construtor sem argumentos para evitar NPE interno em alguns dispositivos/versões do osmdroid
                            // onde map.getRepository() pode falhar se o mapa estiver em estado inconsistente.
                            Polyline newPolyline = new Polyline();
                            
                            int opacityPercent = sharedPreferences.getInt("route_line_opacity", 95);
                            int alpha = (int) (opacityPercent * 255 / 100f);
                            String alphaHex = String.format("%02X", alpha);
                            newPolyline.getOutlinePaint().setColor(Color.parseColor("#" + alphaHex + "2196F3"));

                            newPolyline.getOutlinePaint().setStrokeWidth(12f); 
                            newPolyline.setPoints(pts);
                            newPolyline.setInfoWindow(null);
                            newPolyline.setOnClickListener((polyline, mapView, eventPos) -> true);
                            
                            if (selectionTracePolyline != null) map.getOverlays().remove(selectionTracePolyline);
                            selectionTracePolyline = newPolyline;
                            
                            // Adiciona no índice 0 para ficar por BAIXO dos marcadores
                            map.getOverlays().add(0, selectionTracePolyline);
                            map.invalidate();
                            
                            if (switchTraceLine != null) {
                                String distStr;
                                if (distanceMeters < 1000) distStr = String.format(Locale.getDefault(), "%.0fm", distanceMeters);
                                else distStr = String.format(Locale.getDefault(), "%.1fkm", distanceMeters / 1000.0);
                                
                                if (textSwitchDistance != null) {
                                    textSwitchDistance.setText(distStr);
                                }
                                
                                // Modo Navegação (Balão Superior)
                                if (cardNavigationMode != null) {
                                    animateNavigationCard(true);
                                    if (textNavDistance != null) {
                                        if (finalNextDist < 1000) textNavDistance.setText(String.format(Locale.getDefault(), "%.0fm", finalNextDist));
                                        else textNavDistance.setText(String.format(Locale.getDefault(), "%.1fkm", finalNextDist / 1000.0));
                                    }

                                    if (textNavTotalTime != null) {
                                        if (finalDuration > 0) {
                                            int mins = (int) Math.round(finalDuration / 60.0);
                                            if (mins < 1) mins = 1;
                                            String timeStr;
                                            if (mins >= 60) {
                                                int h = mins / 60;
                                                int m = mins % 60;
                                                timeStr = String.format(Locale.getDefault(), "(%dh %dmin)", h, m);
                                            } else {
                                                timeStr = String.format(Locale.getDefault(), "(%d min)", mins);
                                            }
                                            textNavTotalTime.setText(timeStr);
                                            textNavTotalTime.setVisibility(View.VISIBLE);
                                        } else {
                                            textNavTotalTime.setVisibility(View.GONE);
                                        }
                                    }

                                    if (textNavInstruction != null) textNavInstruction.setText(finalInstruction);
                                    if (imageNavManeuver != null) imageNavManeuver.setImageResource(getManeuverIcon(finalManeuver, finalModifier));
                                }
                                
                                // Limpamos qualquer texto interno que possa estar interferindo
                                switchTraceLine.setTextOn("");
                                switchTraceLine.setTextOff("");
                            }
                        });
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private void animateNavigationCard(boolean show) {
        if (cardNavigationMode == null) return;

        if (show) {
            // Evita reiniciar a animação se já estiver visível
            if (cardNavigationMode.getVisibility() == View.VISIBLE && cardNavigationMode.getAlpha() > 0.5f) return;

            cardNavigationMode.setAlpha(0f);
            cardNavigationMode.setTranslationY(-30f * getResources().getDisplayMetrics().density);
            cardNavigationMode.setScaleX(0.85f);
            cardNavigationMode.setScaleY(0.85f);
            cardNavigationMode.setVisibility(View.VISIBLE);

            cardNavigationMode.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(500)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                    .start();
        } else {
            if (cardNavigationMode.getVisibility() != View.VISIBLE) return;

            cardNavigationMode.animate()
                    .alpha(0f)
                    .translationY(-30f * getResources().getDisplayMetrics().density)
                    .scaleX(0.85f)
                    .scaleY(0.85f)
                    .setDuration(400)
                    .setInterpolator(new android.view.animation.AnticipateInterpolator())
                    .withEndAction(() -> cardNavigationMode.setVisibility(View.GONE))
                    .start();
        }
    }

    private String parseInstruction(String type, String modifier, String name) {
        String base = "";
        if (type.equals("turn") || type.equals("on_ramp") || type.equals("off_ramp")) {
            if (modifier.contains("right")) base = "Dobre à direita";
            else if (modifier.contains("left")) base = "Dobre à esquerda";
            else if (modifier.contains("slight right")) base = "Mantenha à direita";
            else if (modifier.contains("slight left")) base = "Mantenha à esquerda";
            else base = "Siga em frente";
        } else if (type.equals("new name")) base = "Siga para";
        else if (type.equals("depart")) base = "Siga em direção a";
        else if (type.equals("arrive")) base = "Você chegou ao destino";
        else if (type.equals("roundabout") || type.equals("rotary")) base = "Na rotatória, saia";
        else if (type.equals("uturn")) base = "Faça o retorno";
        else if (type.equals("merge")) base = "Acesse a via";
        else base = "Siga em frente";
        
        if (name != null && !name.isEmpty() && !name.equals("null") && !name.equals("unnamed")) return base + " na " + name;
        if (base.equals("Siga em direção a")) return "Siga em frente";
        return base;
    }

    private int getManeuverIcon(String type, String modifier) {
        if (type.equals("uturn")) return R.drawable.ic_nav_uturn;
        if (type.equals("roundabout") || type.equals("rotary")) return R.drawable.ic_nav_roundabout;
        if (modifier.contains("right")) return R.drawable.ic_nav_turn_right;
        if (modifier.contains("left")) return R.drawable.ic_nav_turn_left;
        if (type.equals("arrive")) return android.R.drawable.ic_menu_myplaces;
        return R.drawable.ic_nav_straight;
    }

    private void showRouteInstructionsPopup() {
        if (getContext() == null || lastRouteInstructions.isEmpty()) return;
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_route_instructions, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(v).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        RecyclerView rv = v.findViewById(R.id.recyclerRouteInstructions);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(new RouteInstructionsAdapter(lastRouteInstructions));

        v.findViewById(R.id.btnRouteInstructionsClose).setOnClickListener(v2 -> dialog.dismiss());
        dialog.show();
    }

    @Override public void onResume() {         super.onResume(); 
        if (getView() != null) androidx.core.view.ViewCompat.requestApplyInsets(getView());
        timerHandler.post(timerRunnable);
        animationHandler.post(markerAnimationRunnable);
        
        // Registrar receiver para nova rota
        if (getContext() != null) {
            android.content.IntentFilter filter = new android.content.IntentFilter("com.example.entregas.ACTION_NEW_ROUTE");
            ContextCompat.registerReceiver(requireContext(), newRouteReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        }

        if (sensorManager != null && rotationVectorSensor != null) {
            sensorManager.registerListener(compassListener, rotationVectorSensor, android.hardware.SensorManager.SENSOR_DELAY_UI);
        }
        if (map != null) { 
            map.onResume(); 
            showHomeMarker();
            showLoadingMarkers();
            refreshRemoteVisibility();
            if (locationOverlay != null) {
                // Previne crash caso o provider tenha sido perdido por algum motivo interno do osmdroid
                if (locationOverlay.getMyLocationProvider() == null) {
                    setupLocationOverlay();
                } else {
                    try {
                        locationOverlay.enableMyLocation(); 
                    } catch (Exception e) {
                        setupLocationOverlay();
                    }
                }
            }
        } 
        checkRestInterval(); 
        startComboioListener(); 
        startHazardListener();
        updateDeliveryAppFab();

        // Verifica se há foco pendente (ex: vindo de notificação de amigo)
        if (getContext() != null) {
            SharedPreferences prefs = requireContext().getSharedPreferences("AppConfig", android.content.Context.MODE_PRIVATE);
            String plat = prefs.getString("pending_map_focus_lat", "");
            String plon = prefs.getString("pending_map_focus_lon", "");
            if (!plat.isEmpty() && !plon.isEmpty()) {
                try {
                    double lat = Double.parseDouble(plat);
                    double lon = Double.parseDouble(plon);
                    if (mapController != null) {
                        mapController.setZoom(17.0);
                        mapController.animateTo(new GeoPoint(lat, lon));
                        isMapFocusedOnUser = false;
                        updateCenterFabIcon();
                    }
                    // Limpa para não focar novamente ao rotacionar
                    prefs.edit().remove("pending_map_focus_lat").remove("pending_map_focus_lon").apply();
                } catch (Exception ignored) {}
            }
        }

        // Verifica se há uma gravação GPS solicitada para exibição (vinda da aba Gravações)
        if (getActivity() instanceof MainActivity) {
            int recordingId = ((MainActivity) getActivity()).consumeRequestedRouteKmId();
            if (recordingId != -1) {
                loadSavedRoute(recordingId);
            }
        }
    }

    public void handleRequestedRecording(int kmId) {
        if (isAdded()) {
            loadSavedRoute(kmId);
        }
    }

    public void refreshRemoteVisibility() {
        updateFloatingButtonsVisibility();
        showLoadingMarkers(); // Re-avalia se deve mostrar marcadores
    }
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (viewPagerStops != null) {
            outState.putInt(STATE_CURRENT_STOP_INDEX, viewPagerStops.getCurrentItem());
        }
    }

    @Override public void onPause() { 
        super.onPause(); 
        timerHandler.removeCallbacks(timerRunnable);
        animationHandler.removeCallbacks(markerAnimationRunnable);
        if (sensorManager != null) sensorManager.unregisterListener(compassListener);
        if (networkCallback != null && getContext() != null) {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) cm.unregisterNetworkCallback(networkCallback);
        }
        
        // Desregistrar receiver
        try {
            if (getContext() != null) requireContext().unregisterReceiver(newRouteReceiver);
        } catch (Exception ignored) {}

        if (comboioListener != null) { comboioListener.remove(); comboioListener = null; } 
        if (hazardListener != null) { hazardListener.remove(); hazardListener = null; }
        if (locationOverlay != null) locationOverlay.disableMyLocation(); if (map != null) map.onPause();
    }

    private void startComboioListener() {
        com.google.firebase.auth.FirebaseUser u = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser(); if (u == null || u.getEmail() == null) return;
        FirebaseHelper.checkDeveloperAccess(u.getEmail(), isDev -> {
            if (!isDev) { if (comboioListener != null) comboioListener.remove(); return; }
            Activity activity = getActivity();
            if (activity == null) return;
            activity.runOnUiThread(() -> {
                if (comboioListener != null) comboioListener.remove();
                comboioListener = FirebaseHelper.listenFriendsLocations(u.getEmail(), locs -> { Activity activity2 = getActivity(); if (activity2 != null) activity2.runOnUiThread(() -> updateFriendMarkers(locs)); });
            });
        });
    }

    private void updateFriendMarkers(List<FirebaseHelper.FriendLocation> locations) {
        if (map == null || getContext() == null || !isAdded()) return;
        List<String> active = new ArrayList<>(); int size = (int) (40 * getResources().getDisplayMetrics().density);
        for (FirebaseHelper.FriendLocation fl : locations) {
            active.add(fl.email); Marker m = friendMarkers.get(fl.email);
            if (m == null) { 
                m = new Marker(map); 
                m.setInfoWindow(null); // Desativa o balão de texto e evita NPE
                m.setRelatedObject("FRIEND");
                m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER); 
                map.getOverlays().add(m); 
                friendMarkers.put(fl.email, m); 
            }
            m.setPosition(new GeoPoint(fl.lat, fl.lon));
            m.setTitle(fl.name != null ? fl.name : fl.username);
            if (fl.avatar != null && !fl.avatar.isEmpty()) {
                try { byte[] b = android.util.Base64.decode(fl.avatar, android.util.Base64.DEFAULT); Bitmap bmp = BitmapFactory.decodeByteArray(b, 0, b.length); if (bmp != null) m.setIcon(new BitmapDrawable(getResources(), getCircularBitmapWithBorder(bmp, size))); } catch (Exception e) { setDefaultFriendIcon(m); }
            } else setDefaultFriendIcon(m);
        }
        java.util.Iterator<Map.Entry<String, Marker>> it = friendMarkers.entrySet().iterator();
        while (it.hasNext()) { Map.Entry<String, Marker> e = it.next(); if (!active.contains(e.getKey())) { map.getOverlays().remove(e.getValue()); it.remove(); } }
        map.invalidate();
    }

    private void setDefaultFriendIcon(Marker m) { Drawable d = ContextCompat.getDrawable(requireContext(), R.drawable.ic_car_marker); if (d != null) { d.setTint(Color.parseColor("#4CAF50")); m.setIcon(d); } }
    private Bitmap getCircularBitmapWithBorder(Bitmap bmp, int size) {
        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888); Canvas c = new Canvas(out); Paint p = new Paint(); p.setAntiAlias(true); float r = size / 2f;
        p.setColor(Color.WHITE); c.drawCircle(r, r, r, p); p.setColor(Color.parseColor("#2196F3")); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(4f); c.drawCircle(r, r, r - 2, p);
        p.setStyle(Paint.Style.FILL); p.setShader(new android.graphics.BitmapShader(Bitmap.createScaledBitmap(bmp, size, size, false), android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP)); c.drawCircle(r, r, r - 6, p);
        return out;
    }

    @Override public void onDestroyView() { 
        super.onDestroyView(); 
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (comboioListener != null) comboioListener.remove(); 
        if (hazardListener != null) hazardListener.remove();
        if (getContext() != null) requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(prefListener); 
        if (map != null) map.onDetach(); map = null; 
    }

    private void updateDeliveryAppFab() {
        if (fabDeliveryApp == null || getContext() == null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        String pkg = prefs.getString("delivery_app_package", "").trim();
        
        // Visibilidade agora controlada por updateFloatingButtonsVisibility
        // fabDeliveryApp.setVisibility(View.VISIBLE);

        if (pkg.isEmpty()) {
            fabDeliveryApp.setImageResource(R.drawable.ic_map);
            fabDeliveryApp.setClipToOutline(false);
            fabDeliveryApp.setSupportBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
        } else {
            try {
                PackageManager pm = requireContext().getPackageManager();
                ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                Drawable icon = info.loadIcon(pm);
                fabDeliveryApp.setImageDrawable(icon);
                fabDeliveryApp.setClipToOutline(true);
                fabDeliveryApp.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);
                fabDeliveryApp.setSupportBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
            } catch (Exception e) {
                fabDeliveryApp.setImageResource(R.drawable.ic_map);
                fabDeliveryApp.setClipToOutline(false);
            }
        }
    }
    private void launchDeliveryApp() {
        if (getContext() == null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        
        // 🔥 Lógica do CPF Automático
        if (prefs.getBoolean("auto_copy_fake_cpf", false)) {
            CpfHelper.generateAndCopyCpf(requireContext());
            
            // 💓 Feedback Visual: Pulsar o botão quando o CPF for copiado
            if (fabDeliveryApp != null) {
                fabDeliveryApp.animate()
                        .scaleX(1.3f)
                        .scaleY(1.3f)
                        .setDuration(150)
                        .withEndAction(() -> fabDeliveryApp.animate().scaleX(1f).scaleY(1f).setDuration(150).start())
                        .start();
            }

            // 🔥 Inicia ou Reinicia o timer de CPF automático no TrackingService
            if (prefs.getBoolean("cpf_interval_enabled", false)) {
                Intent intent = new Intent(getContext(), TrackingService.class);
                intent.setAction("RESET_CPF_TIMER");
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    requireContext().startForegroundService(intent);
                } else {
                    requireContext().startService(intent);
                }
            }
        }

        String pkg = prefs.getString("delivery_app_package", "").trim();
        if (!pkg.isEmpty()) {
            PackageManager pm = requireContext().getPackageManager();
            Intent intent = pm.getLaunchIntentForPackage(pkg);
            if (intent != null) { try { startActivity(intent); return; } catch (Exception ignored) {} }
            try {
                intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setPackage(pkg);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                android.content.pm.ResolveInfo resolveInfo = pm.queryIntentActivities(intent, 0).stream().findFirst().orElse(null);
                if (resolveInfo != null) { intent.setClassName(pkg, resolveInfo.activityInfo.name); startActivity(intent); return; }
            } catch (Exception ignored) {}
            Toast.makeText(getContext(), "App não encontrado", Toast.LENGTH_SHORT).show();
        } else {
            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_delivery_app_shortcut, null);
            AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(dialogView).create();
            if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialogView.findViewById(R.id.btnShortcutCancel).setOnClickListener(v -> dialog.dismiss());
            dialogView.findViewById(R.id.btnShortcutConfigure).setOnClickListener(v -> {
                dialog.dismiss();
                if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).openFragmentInSettings(SettingsParentFragment.newInstance(1), "Ajustes do Mapa");
            });
            dialog.show();
        }
    }
    private void startXlsxImport() {
        if (currentRouteId == -1) { Toast.makeText(getContext(), "Crie uma rota primeiro!", Toast.LENGTH_SHORT).show(); promptNewRoute(); return; }
        shouldFocusOnFirstStop = true;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        importXlsxLauncher.launch(intent);
    }
    private double getNumericCellValue(Cell cell) {
        if (cell == null) return 0.0;
        try {
            if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                return cell.getNumericCellValue();
            } else if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                return parseSafeDouble(cell.getStringCellValue());
            }
        } catch (Exception ignored) {}
        return 0.0;
    }

    private void processXlsxImport(Uri uri) {
        if (uri == null || getContext() == null) return;

        // Popup de progresso com animação
        View dv = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_optimization_progress, null);
        com.google.android.material.progressindicator.CircularProgressIndicator progress = dv.findViewById(R.id.progressOptimization);
        TextView textStatus = dv.findViewById(R.id.textOptimizationStatus);
        TextView textPercent = dv.findViewById(R.id.textOptimizationPercent);
        
        AlertDialog importDialog = new AlertDialog.Builder(requireContext()).setView(dv).setCancelable(false).create();
        if (importDialog.getWindow() != null) importDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        importDialog.show();

        final int targetRouteId = currentRouteId; // 🔥 Captura o ID da rota para a qual estamos importando

        new Thread(() -> {
            try (InputStream inputStream = getContext().getContentResolver().openInputStream(uri)) {
                Workbook workbook = new XSSFWorkbook(inputStream);
                Sheet sheet = workbook.getSheetAt(0);
                
                String[] phrases = {
                    "Lendo planilha XLSX...",
                    "Identificando endereços e sequências...",
                    "Unificando pacotes por parada...",
                    "Aplicando correções inteligentes...",
                    "Organizando grupos por cores...",
                    "Finalizando importação segura..."
                };

                for (int i = 0; i < phrases.length; i++) {
                    final String msg = phrases[i];
                    final int p = (i + 1) * (100 / phrases.length);
                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.runOnUiThread(() -> {
                            textStatus.setText(msg);
                            progress.setProgress(p);
                            textPercent.setText(p + "%");
                        });
                    }
                    Thread.sleep(800); 
                }

                Map<String, RouteStop> stopsMap = new LinkedHashMap<>();
                AppDao dao = AppDatabase.getInstance(getContext()).appDao();
                
                // --- Recriação da Lógica de Grupos Automáticos ---
                SharedPreferences ms = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
                long g1Id = dao.insertRouteGroup(new RouteGroup("Grupo 1", ms.getString("color_group_1", "#2196F3"), targetRouteId));
                long g2Id = dao.insertRouteGroup(new RouteGroup("Grupo 2", ms.getString("color_group_2", "#9C27B0"), targetRouteId));
                long g3Id = dao.insertRouteGroup(new RouteGroup("Grupo 3", ms.getString("color_group_3", "#FBC02D"), targetRouteId));
                long g4Id = dao.insertRouteGroup(new RouteGroup("Grupo 4", ms.getString("color_group_4", "#795548"), targetRouteId));

                // 🔥 Correção de Integridade: Começa do fim da lista atual se já houver paradas
                List<RouteStop> existingStops = dao.getStopsForRoute(targetRouteId);
                int currentSortOrder = existingStops.size();
                
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i); if (row == null) continue;
                    try {
                        String rawAddr = getCellValue(row.getCell(4)).trim(); if (rawAddr.isEmpty()) continue;
                        String normAddr = Normalizer.normalize(rawAddr.toUpperCase(), Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "").replaceAll("[.,\\-]", " ").replaceAll("\\s+", " ").trim();
                        String baseAddress = rawAddr; String unificationKey = normAddr;
                        if (rawAddr.contains(",")) {
                            String[] parts = rawAddr.split(","); String street = parts[0].trim(); String numberPart = parts[1].trim().split(" ")[0];
                            baseAddress = street + ", " + numberPart;
                            String specificInfo = "";
                            java.util.regex.Matcher mQ = java.util.regex.Pattern.compile("(?i)(QUADRA|QD\\.?|QU?AD\\.?|Q\\.?|QDR\\.?) ?(\\d+[A-Z]?)").matcher(rawAddr);
                            java.util.regex.Matcher mB = java.util.regex.Pattern.compile("(?i)(BLOCO|BL\\.?|B\\.?|BLO?C\\.?) ?(\\d+[A-Z]?)").matcher(rawAddr);
                            if (mQ.find()) specificInfo += " QD " + mQ.group(2).toUpperCase();
                            if (mB.find()) specificInfo += " BL " + mB.group(2).toUpperCase();
                            unificationKey = Normalizer.normalize((street + " " + numberPart + specificInfo).toUpperCase(), Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "").replaceAll("[.,\\-]", " ").replaceAll("\\s+", " ").trim();
                            if (!specificInfo.isEmpty()) baseAddress += " -" + specificInfo;
                        }
                        String sequenceStr = getCellValue(row.getCell(1));
                        int seq = parseSafeInt(sequenceStr);
                        
                        // Atribuição de Grupo baseada na sequência SPX
                        Integer targetGroupId = null;
                        if (seq >= 1 && seq <= 15) targetGroupId = (int) g1Id;
                        else if (seq >= 16 && seq <= 30) targetGroupId = (int) g2Id;
                        else if (seq >= 31 && seq <= 45) targetGroupId = (int) g3Id;
                        else if (seq >= 46) targetGroupId = (int) g4Id;

                        // PEGA O NUMERO REAL DO EXCEL (sem converter pra string primeiro)
                        double lat = getNumericCellValue(row.getCell(8)); 
                        double lon = getNumericCellValue(row.getCell(9));
                        
                        // Verifica se existe uma correção manual para este endereço específico no banco local
                        CorrectedAddress corrected = dao.getCorrectedAddress(baseAddress); 
                        if (corrected != null) {
                            lat = corrected.latitude;
                            lon = corrected.longitude;
                        }

                        if (stopsMap.containsKey(unificationKey)) {
                            RouteStop existing = stopsMap.get(unificationKey); existing.packageCount++;
                            if (existing.allSequences == null) existing.allSequences = String.valueOf(existing.sequence);
                            existing.allSequences += ", " + sequenceStr;
                            existing.allAddresses = (existing.allAddresses != null ? existing.allAddresses : "") + "\n" + rawAddr;
                            java.util.Set<String> uniqueBuyers = new java.util.HashSet<>(java.util.Arrays.asList(existing.allAddresses.split("\n")));
                            existing.buyerCount = uniqueBuyers.size();
                        } else {
                            RouteStop stop = new RouteStop(); stop.routeId = targetRouteId; stop.atId = getCellValue(row.getCell(0));
                            stop.sequence = seq; stop.allSequences = sequenceStr; stop.allAddresses = rawAddr; stop.buyerCount = 1;
                            stop.sortOrder = currentSortOrder++; stop.stopNumber = parseSafeInt(getCellValue(row.getCell(2))); stop.spxTn = getCellValue(row.getCell(3));
                            stop.address = baseAddress; stop.neighborhood = getCellValue(row.getCell(5)); stop.city = getCellValue(row.getCell(6)); stop.zipcode = getCellValue(row.getCell(7));
                            stop.latitude = lat; stop.longitude = lon;
                            stop.originalLatitude = getNumericCellValue(row.getCell(8));
                            stop.originalLongitude = getNumericCellValue(row.getCell(9));
                            stop.packageCount = 1; stop.createdAt = System.currentTimeMillis();
                            stop.groupId = targetGroupId;
                            
                            // 🔥 Correção: Aceita a parada mesmo que a lat/lon do Excel seja 0, 
                            // desde que tenha um endereço (ela aparecerá no centro do mapa ou será corrigida depois)
                            stopsMap.put(unificationKey, stop);
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }
                if (!stopsMap.isEmpty()) {
                    List<RouteStop> finalStops = new ArrayList<>(stopsMap.values());
                    
                    // 🔥 Ordenação e Renumeração: Garante que os números das paradas sejam únicos e sequenciais
                    // respeitando a ordem de aparição no Excel (sortOrder)
                    Collections.sort(finalStops, (a, b) -> Integer.compare(a.sortOrder, b.sortOrder));
                    for (int i = 0; i < finalStops.size(); i++) {
                        finalStops.get(i).sortOrder = i;
                        finalStops.get(i).stopNumber = i + 1;
                    }
                    
                    dao.insertRouteStops(finalStops);
                    
                    // 🔥 Sincroniza TUDO após a inserção para garantir que não haja números duplicados
                    // se o usuário importou em uma rota que já tinha paradas
                    List<RouteStop> all = dao.getStopsForRoute(targetRouteId);
                    for (int i = 0; i < all.size(); i++) {
                        all.get(i).sortOrder = i;
                        all.get(i).stopNumber = i + 1;
                    }
                    dao.updateRouteStops(all);

                    if (getActivity() != null) getActivity().runOnUiThread(() -> { 
                        importDialog.dismiss();
                        
                        // 🔥 Ativa automaticamente o card de paradas ao importar com sucesso
                        sharedPreferences.edit().putBoolean("show_bottom_sheet_stops", true).apply();

                        Toast.makeText(getContext(), "Importado com sucesso!", Toast.LENGTH_SHORT).show(); 
                        CloudSyncHelper.syncNow(requireContext(), "Atividade na Rota"); 
                    });
                } else {
                    getActivity().runOnUiThread(importDialog::dismiss);
                }
                workbook.close();
            } catch (Exception e) { 
                e.printStackTrace(); 
                getActivity().runOnUiThread(importDialog::dismiss);
            }
        }).start();
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue();
                case NUMERIC:
                    double val = cell.getNumericCellValue();
                    // Se o valor for inteiro (ex: IDs, Sequências), remove o .0
                    if (val == Math.floor(val)) return String.valueOf((long) val);
                    // Caso contrário (ex: Coordenadas), mantém os decimais
                    return String.valueOf(val);
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                default:
                    return "";
            }
        } catch (Exception e) {
            return "";
        }
    }
    private int parseSafeInt(String val) { try { if (val == null || val.isEmpty()) return 0; return Integer.parseInt(val.split("\\.")[0].trim()); } catch (Exception e) { return 0; } }
    private double parseSafeDouble(String val) { 
        try { 
            if (val == null || val.isEmpty()) return 0.0; 
            // 🔥 Suporte para coordenadas brasileiras (vírgula como decimal)
            String cleanVal = val.replace(",", ".").replaceAll("[^0-9.\\-]", "");
            return Double.parseDouble(cleanVal); 
        } catch (Exception e) { 
            return 0.0; 
        } 
    }
    private void observeTrackingStatus() {
        if (getViewLifecycleOwner() == null) return;
        TrackingService.isTracking.observe(getViewLifecycleOwner(), tracking -> updateKmTrackingUI());
        TrackingService.isPaused.observe(getViewLifecycleOwner(), paused -> updateKmTrackingUI());
    }

    private void updateKmTrackingUI() {
        if (fabKmTracking == null) return;
        boolean tracking = Boolean.TRUE.equals(TrackingService.isTracking.getValue());
        boolean paused = Boolean.TRUE.equals(TrackingService.isPaused.getValue());

        fabKmTracking.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));

        if (!tracking) {
            fabKmTracking.setImageTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#2196F3"))); // Azul (Inativo)
        } else {
            if (paused) {
                fabKmTracking.setImageTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFC107"))); // Amarelo (Pausado)
            } else {
                fabKmTracking.setImageTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336"))); // Vermelho (Ativo)
            }
        }
    }

    private void showKmTrackingPopup() {
        boolean tracking = Boolean.TRUE.equals(TrackingService.isTracking.getValue());
        boolean paused = Boolean.TRUE.equals(TrackingService.isPaused.getValue());

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View customView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_km_tracking_mini, null);
        builder.setView(customView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView textKm = customView.findViewById(R.id.textKmValue);
        TextView textStatus = customView.findViewById(R.id.textTrackingStatus);
        MaterialButton btnPlayPause = customView.findViewById(R.id.btnPlayPauseTracking);
        MaterialButton btnStop = customView.findViewById(R.id.btnStopTracking);
        com.google.android.material.materialswitch.MaterialSwitch switchAuto = customView.findViewById(R.id.switchAutoTracking);
        MaterialButton btnManageHome = customView.findViewById(R.id.btnManageHome);
        MaterialButton btnLoadingPoints = customView.findViewById(R.id.btnManageLoadingPoints);
        MaterialButton btnTrackingHistory = customView.findViewById(R.id.btnTrackingHistory);
        MaterialButton btnManualKmRegister = customView.findViewById(R.id.btnManualKmRegister);
        MaterialButton btnManualKmHistory = customView.findViewById(R.id.btnManualKmHistory);

        // --- Elementos de Registro Manual ---
        View layoutMain = customView.findViewById(R.id.layoutTrackingMain);
        View layoutManual = customView.findViewById(R.id.layoutManualRegister);
        EditText editManualDate = customView.findViewById(R.id.editManualKmDate);
        EditText editManualStart = customView.findViewById(R.id.editManualStartKm);
        EditText editManualEnd = customView.findViewById(R.id.editManualEndKm);
        MaterialButton btnSaveManual = customView.findViewById(R.id.btnSaveManualKm);
        ImageButton btnBack = customView.findViewById(R.id.btnBackToTracking);

        java.util.Calendar manualCalendar = java.util.Calendar.getInstance();
        SimpleDateFormat manualSdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        if (editManualDate != null) {
            editManualDate.setText(manualSdf.format(manualCalendar.getTime()));
            editManualDate.setOnClickListener(v -> {
                new android.app.DatePickerDialog(requireContext(), (view, year, month, day) -> {
                    manualCalendar.set(year, month, day);
                    editManualDate.setText(manualSdf.format(manualCalendar.getTime()));
                }, manualCalendar.get(java.util.Calendar.YEAR), manualCalendar.get(java.util.Calendar.MONTH), manualCalendar.get(java.util.Calendar.DAY_OF_MONTH)).show();
            });
        }

        if (btnManualKmRegister != null) {
            AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
            DailyKm pending = dao.getLastPendingDailyKm();
            if (pending != null) {
                btnManualKmRegister.setText("Finalizar KM Manual");
                btnManualKmRegister.setIconResource(android.R.drawable.ic_menu_save);
            }

            btnManualKmRegister.setOnClickListener(v -> {
                layoutMain.setVisibility(View.GONE);
                layoutManual.setVisibility(View.VISIBLE);
                
                if (pending != null) {
                    manualCalendar.setTimeInMillis(pending.date);
                    if (editManualDate != null) editManualDate.setText(manualSdf.format(manualCalendar.getTime()));
                    if (editManualStart != null) {
                        editManualStart.setText(String.valueOf(pending.kmStart));
                        editManualStart.setEnabled(false); // Evita mudar o inicial ao finalizar
                    }
                    if (btnSaveManual != null) btnSaveManual.setText("Finalizar e Calcular");
                    if (editManualEnd != null) {
                        editManualEnd.setText("");
                        editManualEnd.requestFocus();
                    }
                } else {
                    if (btnSaveManual != null) btnSaveManual.setText("Salvar Registro");
                    if (editManualStart != null) {
                        editManualStart.setText("");
                        editManualStart.setEnabled(true);
                    }
                    if (editManualEnd != null) editManualEnd.setText("");
                }
            });
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                layoutManual.setVisibility(View.GONE);
                layoutMain.setVisibility(View.VISIBLE);
            });
        }

        if (btnSaveManual != null) {
            btnSaveManual.setOnClickListener(v -> {
                String startStr = editManualStart.getText().toString().trim();
                if (startStr.isEmpty()) {
                    Toast.makeText(getContext(), "Informe o KM inicial", Toast.LENGTH_SHORT).show();
                    return;
                }

                double kmStart = parseSafeDouble(startStr);
                String endStr = editManualEnd.getText().toString().trim();
                double kmEnd = endStr.isEmpty() ? 0 : parseSafeDouble(endStr);

                if (kmEnd > 0 && kmEnd < kmStart) {
                    Toast.makeText(getContext(), "KM final menor que inicial", Toast.LENGTH_SHORT).show();
                    return;
                }

                new Thread(() -> {
                    AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
                    DailyKm pending = dao.getLastPendingDailyKm();
                    DailyKm km = (pending != null) ? pending : new DailyKm();
                    
                    km.date = manualCalendar.getTimeInMillis();
                    km.kmStart = kmStart;
                    km.kmEnd = kmEnd;
                    km.isCompleted = kmEnd > 0;
                    if (km.isCompleted) {
                        km.totalKm = kmEnd - kmStart;
                        Fuel lastFuel = dao.getLastCompletedFuel();
                        if (lastFuel != null && lastFuel.liters > 0 && lastFuel.kmDriven > 0) {
                            double cons = lastFuel.kmDriven / lastFuel.liters;
                            km.consumptionUsed = cons;
                            km.estimatedFuelCost = (km.totalKm / cons) * lastFuel.pricePerLiter;
                        }
                    }
                    
                    if (pending != null) dao.updateDailyKm(km);
                    else dao.insertDailyKm(km);
                    
                    Activity act = getActivity();
                    if (act != null) act.runOnUiThread(() -> {
                        Toast.makeText(getContext(), km.isCompleted ? "KM Finalizado!" : "KM Inicial salvo!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        CloudSyncHelper.syncNow(requireContext(), "KM Manual Salvo");
                    });
                }).start();
            });
        }

        boolean homeDefined = sharedPreferences.getFloat("home_lat", 0) != 0;
        if (btnManageHome != null) {
            btnManageHome.setText(homeDefined ? "Editar Endereço da Residência" : "Definir Endereço de Casa");
            btnManageHome.setOnClickListener(v -> {
                dialog.dismiss();
                startHomeSelection();
            });
        }

        if (btnLoadingPoints != null) {
            btnLoadingPoints.setOnClickListener(v -> {
                dialog.dismiss();
                showLoadingPointsDialog();
            });
        }

        if (btnTrackingHistory != null) {
            btnTrackingHistory.setOnClickListener(v -> {
                dialog.dismiss();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openTrackingHistory();
                }
            });
        }

        if (btnManualKmHistory != null) {
            btnManualKmHistory.setOnClickListener(v -> {
                dialog.dismiss();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openManualKmHistory();
                }
            });
        }

        // Configuração do Switch de Rastreamento Automático
        int currentMode = sharedPreferences.getInt("tracking_mode_v2", 0);
        int lastAuto = sharedPreferences.getInt("last_auto_mode_v2", 2); // Default to Mode 2 (Distance) if never set
        
        if (switchAuto != null) {
            switchAuto.setChecked(currentMode != 0);
            switchAuto.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int newMode = isChecked ? lastAuto : 0;
                sharedPreferences.edit()
                        .putInt("tracking_mode_v2", newMode)
                        .putBoolean("tracking_auto", newMode == 1)
                        .putBoolean("home_tracking_enabled", newMode == 2)
                        .apply();
                
                // Notifica o TrackingHelper se necessário
                TrackingHelper.updateAutoTracking(requireContext());
                
                Toast.makeText(getContext(), isChecked ? "Modo Automático Ativado" : "Modo Manual Ativado", Toast.LENGTH_SHORT).show();
                
                // Atualiza a visibilidade do botão de play/pause no próprio diálogo
                if (!tracking) {
                    btnPlayPause.setVisibility(isChecked ? View.GONE : View.VISIBLE);
                }
                
                updateKmTrackingUI(); 
            });
        }

        // Atualiza km em tempo real no popup
        TrackingService.currentDistance.observe(getViewLifecycleOwner(), dist -> {
            if (textKm != null) textKm.setText(String.format(Locale.getDefault(), "%.2f KM", dist != null ? dist : 0.0));
        });

        if (!tracking) {
            textStatus.setText("Rastreamento Inativo");
            btnPlayPause.setText("Iniciar");
            btnPlayPause.setIconResource(R.drawable.ic_play);
            btnStop.setVisibility(View.GONE);
        } else {
            textStatus.setText(paused ? "Pausado" : "Rastreando...");
            btnPlayPause.setText(paused ? "Retomar" : "Pausar");
            btnPlayPause.setIconResource(paused ? R.drawable.ic_play : R.drawable.ic_pause);
            btnStop.setVisibility(View.VISIBLE);
        }

        // Oculta botão de play/pause se o modo for automático e não estiver rastreando ainda
        // (No modo automático o início é por gatilho de tempo ou localização)
        if (!tracking && currentMode != 0) {
            btnPlayPause.setVisibility(View.GONE);
        } else {
            btnPlayPause.setVisibility(View.VISIBLE);
        }

        btnPlayPause.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), TrackingService.class);
            if (!tracking) {
                intent.setAction("START");
            } else if (!paused) {
                intent.setAction("PAUSE");
            } else {
                intent.setAction("START");
            }
            startTrackingService(intent);
            dialog.dismiss();
        });

        btnStop.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Finalizar Rastreamento")
                    .setMessage("Deseja parar e salvar este trajeto?")
                    .setPositiveButton("Parar e Salvar", (d, which) -> {
                        Intent intent = new Intent(getContext(), TrackingService.class);
                        intent.setAction("STOP");
                        requireContext().startService(intent);
                        dialog.dismiss();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        dialog.show();
    }

    private void startTrackingService(Intent intent) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent);
        } else {
            requireContext().startService(intent);
        }
    }

    private void onStopAction(RouteStop stop, int action) {
        if (action == 1) { 
            stop.deliveryStatus = 1; 
            stop.deliveryTimestamp = System.currentTimeMillis(); // 🔥 Grava horário da entrega
            updateStopInDb(stop); 
            flashStatsSummary(cardSuccessSummary);
            
            // 🔥 Lógica de Tempo Total
            if (currentRouteHeader != null) {
                new Thread(() -> {
                    AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
                    RouteHeader header = dao.getRouteById(currentRouteId);
                    if (header != null) {
                        boolean updated = false;
                        if (header.startTime == 0) {
                            header.startTime = System.currentTimeMillis();
                            updated = true;
                        }
                        
                        // Verifica se é a última parada
                        List<RouteStop> all = dao.getStopsForRoute(currentRouteId);
                        boolean allDone = true;
                        for (RouteStop rs : all) {
                            if (rs.id == stop.id) continue; // A atual já marcamos acima no DB via updateStopInDb mas o thread pode ser rápido
                            if (rs.deliveryStatus == 0) { allDone = false; break; }
                        }
                        
                        if (allDone && header.endTime == 0) {
                            header.endTime = System.currentTimeMillis();
                            updated = true;
                            
                            // 🔥 Se "Ocultar Entregas" estiver ativo, volta para casa automaticamente ao finalizar a última
                            if (sharedPreferences.getBoolean("hide_delivered_stops", false)) {
                                Activity activity = getActivity();
                                if (activity != null) activity.runOnUiThread(() -> centerOnHome());
                            }
                        }
                        
                        if (updated) dao.updateRouteHeader(header);
                    }
                }).start();
            }

            advanceToNextStop(); 
        }
        else if (action == 2) { 
            stop.deliveryStatus = 2; 
            updateStopInDb(stop); 
            flashStatsSummary(cardFailedSummary);

            // 🔥 Lógica de Tempo Total para falha também
            if (currentRouteHeader != null) {
                new Thread(() -> {
                    AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
                    RouteHeader header = dao.getRouteById(currentRouteId);
                    if (header != null) {
                        List<RouteStop> all = dao.getStopsForRoute(currentRouteId);
                        boolean allDone = true;
                        for (RouteStop rs : all) {
                            if (rs.id == stop.id) continue;
                            if (rs.deliveryStatus == 0) { allDone = false; break; }
                        }
                        
                        if (allDone && header.endTime == 0) {
                            header.endTime = System.currentTimeMillis();
                            dao.updateRouteHeader(header);
                        }
                    }
                }).start();
            }

            advanceToNextStop(); 
        }
        else if (action == 3) navigateToStop(stop);
        else if (action == 4) deleteStopDialog(stop);
        else if (action == 5) promptCorrectLocation(stop);
        else if (action == 6) { 
            stop.deliveryStatus = 0; 
            stop.deliveryTimestamp = 0; // 🔥 Reseta horário
            updateStopInDb(stop); 
            flashStatsSummary(cardPendingSummary);
            
            // 🔥 Reset de Tempo se necessário
            if (currentRouteHeader != null) {
                new Thread(() -> {
                    AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
                    RouteHeader header = dao.getRouteById(currentRouteId);
                    if (header != null) {
                        List<RouteStop> all = dao.getStopsForRoute(currentRouteId);
                        boolean anyDelivered = false;
                        for (RouteStop rs : all) {
                            if (rs.id == stop.id) continue;
                            if (rs.deliveryStatus == 1) { anyDelivered = true; break; }
                        }
                        
                        boolean updated = false;
                        if (!anyDelivered) {
                            header.startTime = 0;
                            header.endTime = 0;
                            updated = true;
                        } else {
                            // Se resetou uma, ela não pode mais ser a "última entregue" que parou o cronômetro
                            if (header.endTime > 0) {
                                header.endTime = 0;
                                updated = true;
                            }
                        }
                        
                        if (updated) dao.updateRouteHeader(header);
                    }
                }).start();
            }

            String celebrationKey = "last_finished_route_" + currentRouteId;
            if (sharedPreferences.contains(celebrationKey)) {
                sharedPreferences.edit().remove(celebrationKey).apply();
                android.util.Log.d("DriveLog", "Resetando flag de celebração via onStopAction (Reset)");
            }
        }
        else if (action == 7) showStopDetails(stop);
        else if (action == 8) showGlobalFeedbackDialog(stop.address);
    }

    private void triggerCelebration() {
        triggerCelebration(false);
    }

    private void triggerCelebration(boolean force) {
        if (konfettiView == null) return;
        
        // Evita disparar repetidamente se já estiver comemorando
        String lastFinishedRouteKey = "last_finished_route_" + currentRouteId;
        if (!force && sharedPreferences.getBoolean(lastFinishedRouteKey, false)) {
            android.util.Log.d("DriveLog", "Celebração ignorada: já comemorou esta rota: " + currentRouteId);
            return;
        }
        
        // Marca IMEDIATAMENTE como celebrado para evitar disparos múltiplos por mudanças rápidas no DB
        sharedPreferences.edit().putBoolean(lastFinishedRouteKey, true).apply();
        
        android.util.Log.d("DriveLog", "Disparando Celebração Explosiva!");
        konfettiView.setVisibility(View.VISIBLE);
        konfettiView.bringToFront();

        EmitterConfig emitterConfig = new Emitter(5, java.util.concurrent.TimeUnit.SECONDS).perSecond(30);
        Party party = new PartyFactory(emitterConfig)
                .angle(270)
                .spread(90)
                .setSpeedBetween(1f, 5f)
                .position(new Position.Relative(0.5, 1.0)) // Do fundo ao centro
                .sizes(new Size(12, 5f, 0.2f))
                .colors(java.util.Arrays.asList(0xffffd700, 0xff32cd32, 0xff1e90ff, 0xffff4500, 0xffba55d3))
                .shapes(Shape.Square.INSTANCE, Shape.Circle.INSTANCE)
                .timeToLive(3000L)
                .build();
        
        konfettiView.start(party);
        
        // Adiciona uma segunda explosão lateral para garantir visibilidade
        konfettiView.start(new PartyFactory(new Emitter(2, java.util.concurrent.TimeUnit.SECONDS).perSecond(20))
                .angle(0) // Direita
                .spread(60)
                .position(new Position.Relative(0.0, 0.5))
                .build());
        
        konfettiView.start(new PartyFactory(new Emitter(2, java.util.concurrent.TimeUnit.SECONDS).perSecond(20))
                .angle(180) // Esquerda
                .spread(60)
                .position(new Position.Relative(1.0, 0.5))
                .build());
        
        if (tts != null && sharedPreferences.getBoolean("voice_commands_enabled", false)) {
            tts.speak("Parabéns! Você concluiu todas as entregas desta rota.", TextToSpeech.QUEUE_ADD, null, "celebration");
        }
    }

    private void updateStopInDb(RouteStop stop) {
        new Thread(() -> {
            AppDatabase.getInstance(requireContext()).appDao().updateRouteStop(stop);
            Activity activity = getActivity();
            if (activity != null) activity.runOnUiThread(() -> CloudSyncHelper.syncNow(requireContext(), "Status Parada"));
        }).start();
    }

    private void advanceToNextStop() {
        if (viewPagerStops == null || currentStops == null || currentStops.isEmpty()) return;

        boolean autoNearest = sharedPreferences.getBoolean("advance_to_nearest", false);
        
        if (autoNearest && currentLocation != null) {
            // Lógica para encontrar a parada pendente mais próxima via RUA (OSRM)
            new Thread(() -> {
                try {
                    List<RouteStop> pending = new ArrayList<>();
                    for (RouteStop s : currentStops) {
                        if (s.deliveryStatus == 0) pending.add(s);
                    }
                    
                    if (pending.isEmpty()) return;

                    // Se houver apenas uma parada pendente, pula lógica OSRM
                    if (pending.size() == 1) {
                        int finalIdx = currentStops.indexOf(pending.get(0));
                        Activity activity = getActivity();
                        if (activity != null) activity.runOnUiThread(() -> viewPagerStops.setCurrentItem(finalIdx, true));
                        return;
                    }

                    // Limita a 50 coordenadas para o servidor OSRM público
                    List<RouteStop> targetStops = pending.size() > 50 ? pending.subList(0, 50) : pending;

                    StringBuilder coords = new StringBuilder();
                    coords.append(String.format(Locale.US, "%.6f,%.6f", currentLocation.getLongitude(), currentLocation.getLatitude()));
                    for (RouteStop s : targetStops) {
                        coords.append(String.format(Locale.US, ";%.6f,%.6f", s.longitude, s.latitude));
                    }

                    String url = "https://router.project-osrm.org/table/v1/driving/" + coords.toString() + "?sources=0&annotations=distance";
                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                    String uniqueId = android.provider.Settings.Secure.getString(requireContext().getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                    conn.setRequestProperty("User-Agent", "DriveLogApp_v142_" + uniqueId);
                    
                    if (conn.getResponseCode() == 200) {
                        BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder res = new StringBuilder(); String line;
                        while ((line = r.readLine()) != null) res.append(line);
                        JSONObject json = new JSONObject(res.toString());
                        JSONArray distances = json.getJSONArray("distances").getJSONArray(0);
                        
                        int bestIdxInTargets = -1;
                        double minDist = Double.MAX_VALUE;
                        for (int i = 1; i < distances.length(); i++) {
                            double d = distances.getDouble(i);
                            if (d < minDist) { minDist = d; bestIdxInTargets = i - 1; }
                        }
                        
                        if (bestIdxInTargets != -1) {
                            RouteStop nearest = targetStops.get(bestIdxInTargets);
                            int finalIdx = currentStops.indexOf(nearest);
                            Activity activity = getActivity();
                            if (activity != null) activity.runOnUiThread(() -> viewPagerStops.setCurrentItem(finalIdx, true));
                            return;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Fallback para distância linear se der erro ou sem internet
                int nearestIdx = -1;
                double minDist = Double.MAX_VALUE;
                for (int i = 0; i < currentStops.size(); i++) {
                    RouteStop s = currentStops.get(i);
                    if (s.deliveryStatus == 0) {
                        double d = currentLocation.distanceToAsDouble(new GeoPoint(s.latitude, s.longitude));
                        if (d < minDist) { minDist = d; nearestIdx = i; }
                    }
                }
                if (nearestIdx != -1) {
                    final int finalIdx = nearestIdx;
                    Activity activity = getActivity();
                    if (activity != null) activity.runOnUiThread(() -> viewPagerStops.setCurrentItem(finalIdx, true));
                }
            }).start();
            return;
        }

        // Comportamento Padrão: Próxima na ordem numérica
        int current = viewPagerStops.getCurrentItem();
        for (int i = current + 1; i < currentStops.size(); i++) {
            if (currentStops.get(i).deliveryStatus == 0) {
                viewPagerStops.setCurrentItem(i, true);
                return;
            }
        }
        
        // Se não houver próxima após a atual, tenta desde o começo (ex: pulou paradas)
        for (int i = 0; i < current; i++) {
            if (currentStops.get(i).deliveryStatus == 0) {
                viewPagerStops.setCurrentItem(i, true);
                return;
            }
        }
    }

    private void showStopDetails(RouteStop stop) {
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_stop_details, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(v).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView textAddress = v.findViewById(R.id.textStopAddress);
        TextView textNeighborhood = v.findViewById(R.id.textStopNeighborhood);
        TextView textPackageCount = v.findViewById(R.id.textPackageCount);
        TextView textBuyerCount = v.findViewById(R.id.textBuyerCount);
        TextView textBuyerList = v.findViewById(R.id.textBuyerList);
        TextView textStopNumber = v.findViewById(R.id.textStopNumber);
        TextView textSequences = v.findViewById(R.id.textSequences);
        TextView textCorrectionStatus = v.findViewById(R.id.textCorrectionStatus);
        MaterialButton btnShareLocalFix = v.findViewById(R.id.btnShareLocalFix);
        MaterialButton btnDeleteLocalFix = v.findViewById(R.id.btnDeleteLocalFix);
        MaterialButton btnDeleteDownloadedFix = v.findViewById(R.id.btnDeleteDownloadedFix);
        MaterialButton btnDevForceApplyFix = v.findViewById(R.id.btnDevForceApplyFix);
        MaterialButton btnSeparatePurchases = v.findViewById(R.id.btnSeparatePurchases);
        
        // --- Campos de Notas ---
        View layoutNote = v.findViewById(R.id.layoutNoteInput);
        View btnShowAddNote = v.findViewById(R.id.btnShowAddNote);
        EditText editNotes = v.findViewById(R.id.editStopNotes);
        android.widget.Spinner spinnerTarget = v.findViewById(R.id.spinnerNoteTarget);
        com.google.android.material.materialswitch.MaterialSwitch switchPublic = v.findViewById(R.id.switchNotePublic);

        textAddress.setText(stop.address);
        textNeighborhood.setText(stop.neighborhood != null && !stop.neighborhood.isEmpty() ? stop.neighborhood : "Bairro não informado");
        textPackageCount.setText(stop.packageCount + (stop.packageCount > 1 ? " Pacotes" : " Pacote"));
        textBuyerCount.setText(stop.buyerCount + (stop.buyerCount > 1 ? " Compradores" : " Comprador"));
        textStopNumber.setText("Parada #" + stop.stopNumber);
        textSequences.setText("Sequências: " + (stop.allSequences != null ? stop.allSequences : stop.sequence));

        // --- Status Inicial de Correção ---
        textCorrectionStatus.setVisibility(View.GONE);

        // --- Seção de Correção Global ---
        View cardGlobal = v.findViewById(R.id.cardGlobalDetails);
        TextView textGlobalLikes = v.findViewById(R.id.textGlobalLikes);
        TextView textGlobalNoteDetails = v.findViewById(R.id.textGlobalNoteDetails);
        MaterialButton btnDownload = v.findViewById(R.id.btnDownloadGlobalFix);

        FirebaseHelper.searchGlobal(stop.address, new FirebaseHelper.GlobalCorrectionCallback() {
            @Override
            public void onResult(double lat, double lon, int likes, int dislikes, String creatorId, String note, int comments, String creatorName, long date) {
                Activity activity = getActivity();
                if (activity != null) activity.runOnUiThread(() -> {
                    // Se a correção global for do próprio usuário OU já estiver baixada localmente, não mostra o card de download
                    AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
                    new Thread(() -> {
                        CorrectedAddress localFix = dao.getCorrectedAddress(stop.address);
                        Activity activity2 = getActivity();
                        if (activity2 != null) activity2.runOnUiThread(() -> {
                            String currentUserId = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE).getString("current_user_id", "anon");
                            boolean isMine = creatorId != null && creatorId.equals(currentUserId);
                            boolean isAlreadyDownloaded = localFix != null && !isMine;

                            // Mostra sempre o feedback se existir na comunidade
                            cardGlobal.setVisibility(View.VISIBLE);
                            textGlobalLikes.setText(likes + " 👍 | " + dislikes + " 👎");
                            textGlobalLikes.setOnClickListener(v3 -> showGlobalFeedbackDialog(stop.address));

                            if (isMine || isAlreadyDownloaded) {
                                btnDownload.setVisibility(View.GONE);
                            } else {
                                btnDownload.setVisibility(View.VISIBLE);
                                // Se chegou aqui, tem uma correção global disponível que não é a atual
                                textCorrectionStatus.setVisibility(View.VISIBLE);
                                textCorrectionStatus.setText("Correção disponível");
                                textCorrectionStatus.setTextColor(Color.parseColor("#F44336")); // Vermelho
                            }

                            if (note != null && !note.isEmpty()) {
                                textGlobalNoteDetails.setVisibility(View.VISIBLE);
                                textGlobalNoteDetails.setText("Obs: " + note);
                            } else {
                                textGlobalNoteDetails.setVisibility(View.GONE);
                            }
                            
                            btnDownload.setOnClickListener(v3 -> {
                                new Thread(() -> {
                                    CorrectedAddress ca = dao.getCorrectedAddress(stop.address);
                                    if (ca == null) ca = new CorrectedAddress(stop.address, stop.neighborhood, lat, lon);
                                    else { ca.latitude = lat; ca.longitude = lon; }
                                    ca.notes = note; 
                                    ca.creatorId = (creatorId != null) ? creatorId : "community_anon"; // Marca como baixada
                                    ca.updatedAt = System.currentTimeMillis();
                                    dao.insertCorrectedAddress(ca);
                                    
                                    // Atualiza a parada atual com a nova coordenada
                                    stop.latitude = lat; stop.longitude = lon;
                                    dao.updateRouteStop(stop);
                                    
                                    Activity activity3 = getActivity();
                                    if (activity3 != null) activity3.runOnUiThread(() -> {
                                        Toast.makeText(getContext(), "Correção baixada e aplicada!", Toast.LENGTH_SHORT).show();
                                        cardGlobal.setVisibility(View.GONE); // Esconde o card após baixar
                                        textCorrectionStatus.setText("Correção baixada");
                                        textCorrectionStatus.setTextColor(Color.parseColor("#FF9800")); // Laranja
                                    });
                                }).start();
                            });
                        });
                    }).start();
                });
            }
            @Override public void onError(String msg) {}
        });

        if (stop.allAddresses != null && !stop.allAddresses.isEmpty()) {
            textBuyerList.setText(stop.allAddresses);
            textBuyerList.setVisibility(View.VISIBLE);
        } else {
            textBuyerList.setVisibility(View.GONE);
        }

        new Thread(() -> {
            AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
            CorrectedAddress corrected = dao.getCorrectedAddress(stop.address);
            getActivity().runOnUiThread(() -> {
                if (corrected != null && corrected.notes != null && !corrected.notes.isEmpty()) {
                    editNotes.setText(corrected.notes);
                    switchPublic.setChecked(corrected.isNotePublic);
                    layoutNote.setVisibility(View.VISIBLE);
                    ((MaterialButton) btnShowAddNote).setText("EDITAR OBSERVAÇÃO");
                }

                // Lógica do botão de compartilhar correção local
                if (corrected != null) {
                    textCorrectionStatus.setVisibility(View.VISIBLE);
                    String currentUserId = sharedPreferences.getString("current_user_id", "anon");
                    boolean isMine = (corrected.creatorId == null || (currentUserId != null && !currentUserId.equals("anon") && currentUserId.equals(corrected.creatorId)));
                    
                    if (isMine) {
                        textCorrectionStatus.setText(corrected.creatorId == null ? "Correção local" : "Minha correção");
                        textCorrectionStatus.setTextColor(Color.parseColor("#4CAF50")); // Verde
                    } else {
                        textCorrectionStatus.setText("Correção baixada");
                        textCorrectionStatus.setTextColor(Color.parseColor("#FF9800")); // Laranja
                    }

                    if (corrected.creatorId == null) {
                        btnShareLocalFix.setVisibility(View.VISIBLE);
                        btnShareLocalFix.setOnClickListener(v2 -> {
                            String uName = sharedPreferences.getString("profile_name", "Entregador");
                            
                            btnShareLocalFix.setEnabled(false);
                            btnShareLocalFix.setText("ENVIANDO...");

                            FirebaseHelper.uploadCorrection(currentUserId, uName, corrected, new FirebaseHelper.GlobalUploadCallback() {
                                @Override public void onSuccess() {
                                    new Thread(() -> {
                                        corrected.creatorId = currentUserId;
                                        dao.updateCorrectedAddress(corrected);
                                        Activity activity2 = getActivity();
                                        if (activity2 != null) activity2.runOnUiThread(() -> {
                                            btnShareLocalFix.setVisibility(View.GONE);
                                            textCorrectionStatus.setText("Minha correção");
                                            Toast.makeText(getContext(), "Compartilhado com sucesso!", Toast.LENGTH_SHORT).show();
                                        });
                                    }).start();
                                }
                                @Override public void onFailure(String msg) {
                                    if (getActivity() != null) getActivity().runOnUiThread(() -> {
                                        btnShareLocalFix.setEnabled(true);
                                        btnShareLocalFix.setText("ENVIAR PARA A COMUNIDADE");
                                        Toast.makeText(getContext(), "Erro ao enviar: " + msg, Toast.LENGTH_SHORT).show();
                                    });
                                }
                            });
                        });
                    } else {
                        btnShareLocalFix.setVisibility(View.GONE);
                    }
                } else {
                    btnShareLocalFix.setVisibility(View.GONE);
                }

                // Lógica do botão de excluir correção local (Apenas se for MINHA)
                String currentUserId = sharedPreferences.getString("current_user_id", "anon");
                boolean isMine = corrected != null && (corrected.creatorId == null || (currentUserId != null && !currentUserId.equals("anon") && currentUserId.equals(corrected.creatorId)));
                boolean isDownloaded = corrected != null && !isMine;

                if (isMine) {
                    btnDeleteLocalFix.setVisibility(View.VISIBLE);
                    btnDeleteDownloadedFix.setVisibility(View.GONE);
                    btnDeleteLocalFix.setOnClickListener(v2 -> {
                        new AlertDialog.Builder(requireContext())
                            .setTitle("Remover Minha Correção")
                            .setMessage("Deseja apagar esta correção e voltar para a localização original do Excel/Comunidade?")
                            .setPositiveButton("Sim, Remover", (dialogInterface, i) -> {
                                new Thread(() -> {
                                    dao.deleteCorrectedAddress(corrected);
                                    FirebaseHelper.searchGlobal(stop.address, new FirebaseHelper.GlobalCorrectionCallback() {
                                        @Override
                                        public void onResult(double lat, double lon, int likes, int dislikes, String creatorId, String publicNote, int commentCount, String creatorName, long updateDate) {
                                            new Thread(() -> {
                                                stop.latitude = lat;
                                                stop.longitude = lon;
                                                dao.updateRouteStop(stop);
                                                Activity activity2 = getActivity();
                                                if (activity2 != null) activity2.runOnUiThread(() -> {
                                                    dialog.dismiss();
                                                    Toast.makeText(getContext(), "Correção removida!", Toast.LENGTH_SHORT).show();
                                                });
                                            }).start();
                                        }
                                        @Override
                                        public void onError(String msg) {
                                            getActivity().runOnUiThread(() -> {
                                                dialog.dismiss();
                                                Toast.makeText(getContext(), "Correção removida localmente.", Toast.LENGTH_SHORT).show();
                                            });
                                        }
                                    });
                                }).start();
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();
                    });
                } else if (isDownloaded) {
                    btnDeleteLocalFix.setVisibility(View.GONE);
                    btnDeleteDownloadedFix.setVisibility(View.VISIBLE);
                    btnDeleteDownloadedFix.setOnClickListener(v2 -> {
                        new AlertDialog.Builder(requireContext())
                            .setTitle("Excluir Correção Baixada")
                            .setMessage("Deseja apagar esta correção baixada da comunidade e voltar para a localização original?")
                            .setPositiveButton("Sim, Excluir", (dialogInterface, i) -> {
                                new Thread(() -> {
                                    dao.deleteCorrectedAddress(corrected);
                                    
                                    // Restaura para a localização original (do Excel ou valor padrão se não houver original)
                                    if (stop.originalLatitude != 0) {
                                        stop.latitude = stop.originalLatitude;
                                        stop.longitude = stop.originalLongitude;
                                    } else {
                                        // Fallback se não tiver original: tenta zerar ou manter? 
                                        // Idealmente restauramos para o que estava na planilha.
                                    }
                                    dao.updateRouteStop(stop);

                                    Activity activity2 = getActivity();
                                    if (activity2 != null) activity2.runOnUiThread(() -> {
                                        dialog.dismiss();
                                        Toast.makeText(getContext(), "Correção baixada excluída!", Toast.LENGTH_SHORT).show();
                                    });
                                }).start();
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();
                    });
                } else {
                    btnDeleteLocalFix.setVisibility(View.GONE);
                    btnDeleteDownloadedFix.setVisibility(View.GONE);
                }

                // Lógica DEV: Forçar aplicação da correção na parada atual
                if (corrected != null) {
                    com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null && user.getEmail() != null) {
                        FirebaseHelper.checkDeveloperAccess(user.getEmail(), isDev -> {
                            if (isDev && getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    btnDevForceApplyFix.setVisibility(View.VISIBLE);
                                    btnDevForceApplyFix.setOnClickListener(v2 -> {
                                        new Thread(() -> {
                                            stop.latitude = corrected.latitude;
                                            stop.longitude = corrected.longitude;
                                            dao.updateRouteStop(stop);
                                            Activity activity2 = getActivity();
                                            if (activity2 != null) activity2.runOnUiThread(() -> {
                                                Toast.makeText(getContext(), "DEV: Posição forçada com sucesso!", Toast.LENGTH_SHORT).show();
                                            });
                                        }).start();
                                    });
                                });
                            }
                        });
                    }
                } else {
                    btnDevForceApplyFix.setVisibility(View.GONE);
                }

                // --- Lógica de Separar Compras ---
                if (stop.packageCount > 1) {
                    btnSeparatePurchases.setVisibility(View.VISIBLE);
                    btnSeparatePurchases.setOnClickListener(v2 -> {
                        dialog.dismiss();
                        showPurchaseSeparationDialog(stop);
                    });
                } else {
                    btnSeparatePurchases.setVisibility(View.GONE);
                }
            });
        }).start();

        // Configurar Spinner de Destinatários
        List<String> recipients = new ArrayList<>();
        if (stop.allAddresses != null) {
            recipients.add("Todos neste endereço");
            for (String line : stop.allAddresses.split("\n")) if (!line.trim().isEmpty()) recipients.add(line.trim());
        } else {
            recipients.add(stop.address);
        }
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, recipients);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTarget.setAdapter(adapter);

        btnShowAddNote.setOnClickListener(v2 -> {
            if (layoutNote.getVisibility() == View.GONE) {
                layoutNote.setVisibility(View.VISIBLE);
                ((MaterialButton) btnShowAddNote).setText("RECOLHER NOTA");
            } else {
                layoutNote.setVisibility(View.GONE);
                ((MaterialButton) btnShowAddNote).setText("EDITAR OBSERVAÇÃO");
            }
        });

        v.findViewById(R.id.btnCloseDetails).setOnClickListener(v2 -> {
            String noteText = editNotes.getText().toString().trim();
            boolean isPublic = switchPublic.isChecked();
            
            // Salvar Nota se houver alteração
            new Thread(() -> {
                AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
                CorrectedAddress corrected = dao.getCorrectedAddress(stop.address);
                
                // Só salva se o texto da nota mudou ou se já existia uma correção
                boolean hasNote = !noteText.isEmpty();
                if (corrected != null || hasNote) {
                    if (corrected == null) {
                        corrected = new CorrectedAddress(stop.address, stop.neighborhood, stop.latitude, stop.longitude);
                    }
                    
                    // Só atualiza se houver mudança real para evitar "sujar" o banco e marcar como "minha local" sem necessidade
                    if (!noteText.equals(corrected.notes) || isPublic != corrected.isNotePublic) {
                        corrected.notes = noteText;
                        corrected.isNotePublic = isPublic;
                        corrected.updatedAt = System.currentTimeMillis();
                        dao.insertCorrectedAddress(corrected); // Insert or Update

                        if (isPublic && !noteText.isEmpty()) {
                            com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                            String uName = (user != null) ? user.getDisplayName() : "Entregador";
                            String uId = (user != null) ? user.getUid() : "anon";
                            FirebaseHelper.addFeedback(stop.address, true, noteText, uName, uId);
                        }
                    }
                }
            }).start();
            
            dialog.dismiss();
        });
        dialog.show();
    }

    private void showPurchaseSeparationDialog(RouteStop stop) {
        if (stop.allAddresses == null || stop.allAddresses.isEmpty()) return;
        
        String[] packages = stop.allAddresses.split("\n");
        String[] sequences = (stop.allSequences != null) ? stop.allSequences.split(", ") : new String[]{String.valueOf(stop.sequence)};
        
        String[] items = new String[packages.length];
        for (int i = 0; i < packages.length; i++) {
            String seq = (i < sequences.length) ? sequences[i] : "?";
            items[i] = "Seq " + seq + ": " + packages[i];
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Escolha o pacote para mover")
                .setItems(items, (d, which) -> {
                    String selectedPackage = packages[which];
                    String selectedSequence = (which < sequences.length) ? sequences[which] : String.valueOf(stop.sequence);
                    showMoveDestinationDialog(stop, selectedPackage, selectedSequence, which);
                })
                .setNegativeButton("Cancelar", null)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog_rounded);
        }
        dialog.show();
    }

    private void showMoveDestinationDialog(RouteStop sourceStop, String packageToMove, String sequenceToMove, int indexInSource) {
        AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
        new Thread(() -> {
            List<RouteStop> allStops = dao.getStopsForRoute(currentRouteId);
            Activity activity = getActivity();
            if (activity != null) activity.runOnUiThread(() -> {
                    List<RouteStop> otherStops = new ArrayList<>();
                for (RouteStop s : allStops) {
                    if (s.id != sourceStop.id) otherStops.add(s);
                }

                String[] options = new String[otherStops.size() + 1];
                options[0] = "+ Criar Nova Parada";
                for (int i = 0; i < otherStops.size(); i++) {
                    options[i+1] = "Parada #" + otherStops.get(i).stopNumber + ": " + otherStops.get(i).address;
                }

                AlertDialog dialog = new AlertDialog.Builder(requireContext())
                        .setTitle("Mover para qual parada?")
                        .setItems(options, (d, which) -> {
                            if (which == 0) {
                                // Criar nova parada baseada nesta
                                processMoveToNewStop(sourceStop, packageToMove, sequenceToMove, indexInSource);
                            } else {
                                // Mover para parada existente
                                processMoveToExistingStop(sourceStop, otherStops.get(which - 1), packageToMove, sequenceToMove, indexInSource);
                            }
                        })
                        .setNegativeButton("Voltar", (d, w) -> showPurchaseSeparationDialog(sourceStop))
                        .create();

                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog_rounded);
                }
                dialog.show();
            });
        }).start();
    }

    private void processMoveToNewStop(RouteStop source, String pkg, String seq, int index) {
        new Thread(() -> {
            AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
            
            // 1. Cria a nova parada
            RouteStop target = new RouteStop();
            target.routeId = source.routeId;
            target.address = source.address; // Mantém o mesmo endereço base
            target.neighborhood = source.neighborhood;
            target.city = source.city;
            target.zipcode = source.zipcode;
            target.latitude = source.latitude;
            target.longitude = source.longitude;
            target.originalLatitude = source.originalLatitude;
            target.originalLongitude = source.originalLongitude;
            target.sequence = parseSafeInt(seq);
            target.allSequences = seq;
            target.allAddresses = pkg;
            target.packageCount = 1;
            target.buyerCount = 1;
            target.deliveryStatus = 0; // Volta para pendente
            target.createdAt = System.currentTimeMillis();
            target.sortOrder = source.sortOrder + 1; // Coloca logo após
            target.stopNumber = dao.getNextStopNumber(currentRouteId);
            target.groupId = source.groupId;
            
            dao.insertRouteStop(target);

            // 2. Remove da origem
            updateSourceAfterMove(source, index);
            
            // 3. Renumerar tudo para garantir integridade
            List<RouteStop> all = dao.getStopsForRoute(currentRouteId);
            for (int i = 0; i < all.size(); i++) {
                all.get(i).sortOrder = i;
                all.get(i).stopNumber = i + 1;
            }
            dao.updateRouteStops(all);
            
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "Pacote movido para nova parada!", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void processMoveToExistingStop(RouteStop source, RouteStop target, String pkg, String seq, int index) {
        new Thread(() -> {
            AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
            
            // 1. Atualiza o alvo
            target.packageCount++;
            target.allAddresses = (target.allAddresses != null && !target.allAddresses.isEmpty()) ? target.allAddresses + "\n" + pkg : pkg;
            target.allSequences = (target.allSequences != null && !target.allSequences.isEmpty()) ? target.allSequences + ", " + seq : seq;
            
            // Recalcula compradores únicos no alvo
            java.util.Set<String> targetBuyers = new java.util.HashSet<>(java.util.Arrays.asList(target.allAddresses.split("\n")));
            target.buyerCount = targetBuyers.size();
            
            dao.updateRouteStop(target);

            // 2. Remove da origem
            updateSourceAfterMove(source, index);
            
            // 3. Renumerar tudo para garantir integridade
            List<RouteStop> all = dao.getStopsForRoute(currentRouteId);
            for (int i = 0; i < all.size(); i++) {
                all.get(i).sortOrder = i;
                all.get(i).stopNumber = i + 1;
            }
            dao.updateRouteStops(all);
            
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "Pacote movido com sucesso!", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void updateSourceAfterMove(RouteStop source, int indexToRemove) {
        AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
        
        String[] packages = source.allAddresses.split("\n");
        String[] sequences = (source.allSequences != null) ? source.allSequences.split(", ") : new String[]{String.valueOf(source.sequence)};
        
        List<String> newPackagesList = new ArrayList<>();
        List<String> newSequencesList = new ArrayList<>();
        
        for (int i = 0; i < packages.length; i++) {
            if (i != indexToRemove) {
                newPackagesList.add(packages[i]);
                if (i < sequences.length) newSequencesList.add(sequences[i]);
            }
        }

        if (newPackagesList.isEmpty()) {
            dao.deleteRouteStop(source);
        } else {
            source.packageCount = newPackagesList.size();
            source.allAddresses = String.join("\n", newPackagesList);
            source.allSequences = String.join(", ", newSequencesList);
            if (!newSequencesList.isEmpty()) source.sequence = parseSafeInt(newSequencesList.get(0));
            
            // Recalcula compradores únicos na origem
            java.util.Set<String> sourceBuyers = new java.util.HashSet<>(newPackagesList);
            source.buyerCount = sourceBuyers.size();
            
            dao.updateRouteStop(source);
        }
    }

    private void deleteStopDialog(RouteStop stop) {
        if (getContext() == null || isStopDeleteDialogShowing) return;
        isStopDeleteDialogShowing = true;
        
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_modern_confirm, null);
        TextView title = dialogView.findViewById(R.id.textModernTitle);
        TextView message = dialogView.findViewById(R.id.textModernMessage);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnModernNegative);
        com.google.android.material.button.MaterialButton btnConfirm = dialogView.findViewById(R.id.btnModernPositive);

        title.setText("Excluir Parada");
        message.setText("Deseja remover esta parada da sua rota?");
        btnConfirm.setText("REMOVER");

        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(dialogView).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dialog.setOnDismissListener(d -> isStopDeleteDialogShowing = false);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            new Thread(() -> { 
                AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
                dao.deleteRouteStop(stop); 
                
                // Renumerar para remover buracos e atualizar stopNumber
                List<RouteStop> all = dao.getStopsForRoute(currentRouteId);
                for (int i = 0; i < all.size(); i++) {
                    all.get(i).sortOrder = i;
                    all.get(i).stopNumber = i + 1;
                }
                dao.updateRouteStops(all);
                
                if (isAdded()) {
                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.runOnUiThread(() -> {
                            CloudSyncHelper.syncNow(requireContext(), "Atividade na Rota");
                        });
                    }
                }
            }).start();
        });
        dialog.show();
    }

    private void showGlobalFeedbackDialog(String address) {
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_community_feedback, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(v).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        String currentUserId = sharedPreferences.getString("current_user_id", "anon");
        String userName = sharedPreferences.getString("profile_name", "Entregador");

        v.findViewById(R.id.btnFeedbackLike).setOnClickListener(v2 -> {
            FirebaseHelper.addFeedback(address, true, null, userName, currentUserId);
            Toast.makeText(getContext(), "Valeu!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        v.findViewById(R.id.btnFeedbackDislike).setOnClickListener(v2 -> {
            FirebaseHelper.addFeedback(address, false, null, userName, currentUserId);
            Toast.makeText(getContext(), "Feedback enviado!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        v.findViewById(R.id.btnFeedbackComments).setOnClickListener(v2 -> {
            dialog.dismiss();
            promptFeedbackComment(address);
        });

        v.findViewById(R.id.btnFeedbackClose).setOnClickListener(v2 -> dialog.dismiss());
        
        dialog.show();
    }

    private void promptFeedbackComment(String address) {
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_community_comments, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(v).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        android.widget.LinearLayout layoutCommentsList = v.findViewById(R.id.layoutCommentsList);
        EditText editInput = v.findViewById(R.id.editCommentInput);
        String currentUserId = sharedPreferences.getString("current_user_id", "anon");
        String userName = sharedPreferences.getString("profile_name", "Entregador");

        FirebaseHelper.fetchComments(address, new FirebaseHelper.CommentsFetchCallback() {
            @Override public void onSuccess(List<FirebaseHelper.CommentsFetchCallback.CommentModel> list) {
                Activity activity = getActivity();
                if (activity != null) activity.runOnUiThread(() -> {
                    layoutCommentsList.removeAllViews();
                    if (list.isEmpty()) {
                        TextView tv = new TextView(getContext());
                        tv.setText("Nenhum comentário ainda.");
                        tv.setPadding(10, 20, 10, 20);
                        tv.setGravity(android.view.Gravity.CENTER);
                        layoutCommentsList.addView(tv);
                    } else {
                        for (FirebaseHelper.CommentsFetchCallback.CommentModel c : list) {
                            TextView tv = new TextView(getContext());
                            tv.setText(c.user + ": " + c.text);
                            tv.setTextSize(13);
                            tv.setPadding(0, 8, 0, 8);
                            tv.setTextColor(Color.parseColor("#333333"));
                            layoutCommentsList.addView(tv);
                        }
                    }
                });
            }
            @Override public void onError(String msg) {}
        });

        v.findViewById(R.id.btnSendComment).setOnClickListener(v2 -> {
            String text = editInput.getText().toString().trim();
            if (!text.isEmpty()) {
                FirebaseHelper.addFeedback(address, null, text, userName, currentUserId);
                Toast.makeText(getContext(), "Comentário enviado!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        v.findViewById(R.id.btnCancelComments).setOnClickListener(v2 -> dialog.dismiss());

        dialog.show();
    }

    private void promptCorrectLocation(RouteStop stop) {
        cardFixMode.setVisibility(View.VISIBLE);
        Toast.makeText(getContext(), "Toque no local correto", Toast.LENGTH_LONG).show();
        currentFixOverlay = new org.osmdroid.views.overlay.MapEventsOverlay(new org.osmdroid.events.MapEventsReceiver() {
            @Override public boolean singleTapConfirmedHelper(GeoPoint p) {
                new Thread(() -> {
                    AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
                    // Garante que usamos EXCLUSIVAMENTE o ponto tocado no mapa (p)
                    stop.latitude = p.getLatitude(); 
                    stop.longitude = p.getLongitude();
                    dao.updateRouteStop(stop);
                    
                    // 🔥 Salva na lista global de "Meus Endereços"
                    CorrectedAddress ca = dao.getCorrectedAddress(stop.address);
                    String currentUserId = sharedPreferences.getString("current_user_id", "anon");
                    String uName = sharedPreferences.getString("profile_name", "Entregador");

                    if (ca == null) {
                        ca = new CorrectedAddress(stop.address, stop.neighborhood, stop.latitude, stop.longitude);
                    } else {
                        ca.latitude = stop.latitude;
                        ca.longitude = stop.longitude;
                    }
                    ca.creatorId = null; // Inicialmente local (não enviado)
                    ca.updatedAt = System.currentTimeMillis();
                    long newId = dao.insertCorrectedAddress(ca);
                    ca.id = (int) newId;
                    final CorrectedAddress finalCa = ca;

                    // 🔥 Compartilhamento Automático se ativado
                    // Somente para paradas vindas de planilha (que possuem atId)
                    boolean isManualStop = stop.atId == null || stop.atId.isEmpty();
                    if (!isManualStop && sharedPreferences.getBoolean("auto_share_corrections", true)) {
                        FirebaseHelper.uploadCorrection(currentUserId, uName, ca, new FirebaseHelper.GlobalUploadCallback() {
                            @Override public void onSuccess() {
                                new Thread(() -> {
                                    finalCa.creatorId = currentUserId;
                                    dao.updateCorrectedAddress(finalCa);
                                }).start();
                            }
                            @Override public void onFailure(String msg) {}
                        });
                    }

                    Activity activity = getActivity();
                    if (activity != null) activity.runOnUiThread(() -> { 
                            cardFixMode.setVisibility(View.GONE); 
                            map.getOverlays().remove(currentFixOverlay); 
                            map.invalidate(); 
                        });
                }).start();
                return true;
            }
            @Override public boolean longPressHelper(GeoPoint p) { return false; }
        });
        map.getOverlays().add(currentFixOverlay);
    }
    private void showNoConnectionPopup() {
        if (getContext() == null) return;
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_modern_confirm, null);
        TextView tt = v.findViewById(R.id.textModernTitle);
        TextView tm = v.findViewById(R.id.textModernMessage);
        MaterialButton bn = v.findViewById(R.id.btnModernNegative);
        MaterialButton bp = v.findViewById(R.id.btnModernPositive);

        tt.setText("Sem Conexão");
        tm.setText("Não é possível ativar o trajeto sem conexão com a internet.");
        bn.setVisibility(View.GONE);
        bp.setText("ENTENDI");

        AlertDialog d = new AlertDialog.Builder(requireContext()).setView(v).create();
        if (d.getWindow() != null) d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        bp.setOnClickListener(v2 -> d.dismiss());
        d.show();
    }

    private void toggleStatsSummary() {
        if (layoutStatsGroup == null) return;
        boolean isVisible = layoutStatsGroup.getVisibility() == View.VISIBLE;
        
        if (isVisible) {
            // Recolher
            layoutStatsGroup.animate()
                    .alpha(0f)
                    .translationY(-20f)
                    .setDuration(250)
                    .withEndAction(() -> {
                        layoutStatsGroup.setVisibility(View.GONE);
                        if (imageToggleStatsSummary != null) {
                            imageToggleStatsSummary.setImageResource(R.drawable.ic_arrow_down);
                        }
                    })
                    .start();
            sharedPreferences.edit().putBoolean("stats_summary_expanded", false).apply();
        } else {
            // Expandir
            if (cardSuccessSummary != null) cardSuccessSummary.setVisibility(View.VISIBLE);
            if (cardPendingSummary != null) cardPendingSummary.setVisibility(View.VISIBLE);
            // cardFailedSummary será atualizado pelo observer ou podemos deixar invisível se 0
            
            layoutStatsGroup.setVisibility(View.VISIBLE);
            layoutStatsGroup.setAlpha(0f);
            layoutStatsGroup.setTranslationY(-20f);
            layoutStatsGroup.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(250)
                    .start();
            if (imageToggleStatsSummary != null) {
                imageToggleStatsSummary.setImageResource(R.drawable.ic_arrow_up);
            }
            sharedPreferences.edit().putBoolean("stats_summary_expanded", true).apply();
        }
    }

    private void flashStatsSummary(View targetCard) {
        if (layoutStatsGroup == null || targetCard == null) return;
        
        // Se o painel estiver configurado para ficar permanentemente visível, não fazemos o flash
        boolean isExpandedPermanently = sharedPreferences.getBoolean("stats_summary_expanded", true);
        if (isExpandedPermanently && layoutStatsGroup.getVisibility() == View.VISIBLE && autoHideRunnable == null) {
            return;
        }

        // Esconde os outros cards para mostrar apenas o referente à ação
        if (cardSuccessSummary != null) cardSuccessSummary.setVisibility(targetCard == cardSuccessSummary ? View.VISIBLE : View.GONE);
        if (cardFailedSummary != null) cardFailedSummary.setVisibility(targetCard == cardFailedSummary ? View.VISIBLE : View.GONE);
        if (cardPendingSummary != null) cardPendingSummary.setVisibility(targetCard == cardPendingSummary ? View.VISIBLE : View.GONE);

        // Se o grupo está oculto, mostra com animação
        if (layoutStatsGroup.getVisibility() != View.VISIBLE) {
            layoutStatsGroup.setVisibility(View.VISIBLE);
            layoutStatsGroup.setAlpha(0f);
            layoutStatsGroup.setTranslationY(-20f);
            layoutStatsGroup.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(250)
                    .start();
        }

        if (autoHideRunnable != null) autoHideHandler.removeCallbacks(autoHideRunnable);
        autoHideRunnable = () -> {
            if (isAdded() && layoutStatsGroup != null) {
                layoutStatsGroup.animate()
                        .alpha(0f)
                        .translationY(-20f)
                        .setDuration(250)
                        .withEndAction(() -> {
                            layoutStatsGroup.setVisibility(View.GONE);
                            // Restaura visibilidade padrão para quando o usuário expandir manualmente
                            if (cardSuccessSummary != null) cardSuccessSummary.setVisibility(View.VISIBLE);
                            if (cardPendingSummary != null) cardPendingSummary.setVisibility(View.VISIBLE);
                            // cardFailedSummary será resolvido pelo observer no próximo ciclo ou mantido GONE
                            autoHideRunnable = null;
                        })
                        .start();
            }
        };
        autoHideHandler.postDelayed(autoHideRunnable, 3000);
    }

    private void showRouteStatsPopup() {
        if (getContext() == null || currentRouteHeader == null) return;
        
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_route_stats, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(v).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView textTitle = v.findViewById(R.id.textStatsTitle);
        TextView textTotal = v.findViewById(R.id.textStatsTotalDuration);
        TextView textAvg = v.findViewById(R.id.textStatsAvgPerHour);
        TextView textAvgStops = v.findViewById(R.id.textStatsAvgStopsPerHour);
        RecyclerView rv = v.findViewById(R.id.recyclerStopStats);
        
        textTitle.setText("Estatísticas: " + currentRouteHeader.name);
        
        long totalElapsed;
        if (currentRouteHeader.endTime > 0) {
            totalElapsed = currentRouteHeader.endTime - currentRouteHeader.startTime - currentRouteHeader.totalPausedMs;
        } else {
            long now = System.currentTimeMillis();
            long currentPausedMs = currentRouteHeader.totalPausedMs + 
                (currentRouteHeader.lastPauseStartTime > 0 ? (now - currentRouteHeader.lastPauseStartTime) : 0);
            totalElapsed = now - currentRouteHeader.startTime - currentPausedMs;
        }
        
        long s = totalElapsed / 1000;
        long m = s / 60;
        long h = m / 60;
        textTotal.setText(String.format(Locale.getDefault(), "Tempo Total (Ativo): %02d:%02d:%02d", h, m % 60, s % 60));

        // Calcular tempos por parada
        List<RouteStop> delivered = new ArrayList<>();
        int totalPackages = 0;
        for (RouteStop st : currentStops) {
            if (st.deliveryStatus == 1 && st.deliveryTimestamp > 0) {
                delivered.add(st);
                totalPackages += st.packageCount;
            }
        }
        delivered.sort((a, b) -> Long.compare(a.deliveryTimestamp, b.deliveryTimestamp));

        // Calcular Médias por Hora
        if (totalElapsed > 0 && !delivered.isEmpty()) {
            double hours = totalElapsed / (1000.0 * 60 * 60);
            
            // Média de Pacotes
            double avgPkgs = totalPackages / hours;
            textAvg.setText(String.format(Locale.getDefault(), "Média: %.1f pacotes/hora", avgPkgs));
            textAvg.setVisibility(View.VISIBLE);
            
            // Média de Paradas
            double avgStops = delivered.size() / hours;
            textAvgStops.setText(String.format(Locale.getDefault(), "Média: %.1f paradas/hora", avgStops));
            textAvgStops.setVisibility(View.VISIBLE);
        } else {
            textAvg.setVisibility(View.GONE);
            textAvgStops.setVisibility(View.GONE);
        }

        List<String> statsList = new ArrayList<>();
        long lastTime = currentRouteHeader.startTime;
        
        for (RouteStop st : delivered) {
            long diff = st.deliveryTimestamp - lastTime;
            long ds = diff / 1000;
            long dm = ds / 60;
            String timeStr = String.format(Locale.getDefault(), "%02d:%02d", dm, ds % 60);
            
            statsList.add("<b>#" + st.stopNumber + "</b> - " + st.address + "<br/>" +
                         "<font color='#666666'>Duração: " + timeStr + "</font>");
            lastTime = st.deliveryTimestamp;
        }

        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
                TextView tv = new TextView(p.getContext());
                tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                tv.setPadding(0, 16, 0, 16);
                tv.setTextSize(13);
                return new RecyclerView.ViewHolder(tv) {};
            }
            @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
                ((TextView)h.itemView).setText(android.text.Html.fromHtml(statsList.get(pos), android.text.Html.FROM_HTML_MODE_COMPACT));
            }
            @Override public int getItemCount() { return statsList.size(); }
        });

        v.findViewById(R.id.btnStatsClose).setOnClickListener(v2 -> dialog.dismiss());
        dialog.show();
    }

    private void navigateToStop(RouteStop stop) {
        try {
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + stop.latitude + "," + stop.longitude);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            startActivity(mapIntent);
        } catch (Exception e) { Toast.makeText(getContext(), "Nenhum app de mapas encontrado", Toast.LENGTH_SHORT).show(); }
    }
    private void optimizeRoute() {
        if (currentRouteId == -1 || getContext() == null) return;
        
        View dv = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_optimization_progress, null);
        com.google.android.material.progressindicator.CircularProgressIndicator progress = dv.findViewById(R.id.progressOptimization);
        TextView textStatus = dv.findViewById(R.id.textOptimizationStatus);
        TextView textPercent = dv.findViewById(R.id.textOptimizationPercent);
        
        // Fundo transparente para o diálogo, pois o layout agora usa um CardView com fundo branco e bordas arredondadas
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(dv).setCancelable(false).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();

        new Thread(() -> {
            try {
                AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
                List<RouteStop> all = dao.getStopsForRoute(currentRouteId);
                if (all.isEmpty()) { Activity activity = getActivity(); if (activity != null) activity.runOnUiThread(dialog::dismiss); return; }
                
                String[] phrases = {
                    "Iniciando Inteligência Artificial...",
                    "Analisando malha viária da região...",
                    "Calculando rotas alternativas...",
                    "Evitando tráfego intenso...",
                    "Otimizando consumo de combustível...",
                    "Processando melhor sequência...",
                    "Finalizando trajeto inteligente..."
                };

                for (int i = 0; i < phrases.length; i++) {
                    final String msg = phrases[i];
                    final int p = (i + 1) * (100 / phrases.length);
                    Activity activity = getActivity();
                    if (activity != null) activity.runOnUiThread(() -> {
                            textStatus.setText(msg);
                            progress.setProgress(p);
                            textPercent.setText(p + "%");
                        });
                    // Deixando mais lento para o app "analisar" conforme pedido
                    Thread.sleep(1200); 
                }

                // Cálculo real
                List<RouteStop> unvisited = new ArrayList<>(all);
                List<RouteStop> optimized = new ArrayList<>();
                GeoPoint current = currentLocation;
                
                while (!unvisited.isEmpty()) {
                    RouteStop nearest = null; double minDist = Double.MAX_VALUE;
                    for (RouteStop s : unvisited) {
                        double d = current.distanceToAsDouble(new GeoPoint(s.latitude, s.longitude));
                        if (d < minDist) { minDist = d; nearest = s; }
                    }
                    optimized.add(nearest); unvisited.remove(nearest);
                    current = new GeoPoint(nearest.latitude, nearest.longitude);
                }
                
                for (int i=0; i<optimized.size(); i++) {
                    optimized.get(i).sortOrder = i;
                    optimized.get(i).stopNumber = i + 1; // Sincroniza número da parada com a nova ordem
                }
                dao.updateRouteStops(optimized);
                
                Activity activity = getActivity();
                if (activity != null) activity.runOnUiThread(() -> {
                        dialog.dismiss();
                        Toast.makeText(getContext(), "Rota otimizada com sucesso!", Toast.LENGTH_SHORT).show();
                    });
            } catch (Exception e) { 
                Activity activity2 = getActivity();
                if (activity2 != null) activity2.runOnUiThread(dialog::dismiss);
            }
        }).start();
    }

    private void optimizeRouteV2() {
        if (currentRouteId == -1 || getContext() == null) return;
        
        View dv = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_optimization_progress, null);
        com.google.android.material.progressindicator.CircularProgressIndicator progress = dv.findViewById(R.id.progressOptimization);
        TextView textStatus = dv.findViewById(R.id.textOptimizationStatus);
        TextView textPercent = dv.findViewById(R.id.textOptimizationPercent);
        
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(dv).setCancelable(false).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();

        new Thread(() -> {
            try {
                AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
                List<RouteStop> all = dao.getStopsForRoute(currentRouteId);
                if (all.isEmpty()) { Activity activity = getActivity(); if (activity != null) activity.runOnUiThread(dialog::dismiss); return; }

                String[] phrases = {
                    "Mapeando endereços para rede viária...",
                    "Analisando caminhos reais entre cada parada...",
                    "Calculando matriz de distância e tempo (OSRM 2.0)...",
                    "Processando algoritmos de vizinho mais próximo viário...",
                    "Resolvendo Problema do Caixeiro Viajante inteligente...",
                    "Eliminando voltas desnecessárias e cruzamentos...",
                    "Organizando sequência lógica por ruas e avenidas...",
                    "Finalizando otimização de alta precisão..."
                };

                for (int i = 0; i < phrases.length; i++) {
                    final String msg = phrases[i];
                    final int p = (i + 1) * (100 / phrases.length);
                    getActivity().runOnUiThread(() -> {
                        textStatus.setText(msg); progress.setProgress(p); textPercent.setText(p + "%");
                    });
                    Thread.sleep(1500); 
                }

                // Lógica Inteligente: Usar OSRM Table API para pegar distâncias reais entre paradas
                // Limitamos a 50 paradas por vez para o servidor público do OSRM
                List<RouteStop> source = new ArrayList<>(all);
                List<RouteStop> optimized = new ArrayList<>();
                GeoPoint current = currentLocation;

                // Para cada passo, buscamos a parada que tem a menor distância REAL de rua
                while (!source.isEmpty()) {
                    StringBuilder coords = new StringBuilder();
                    coords.append(String.format(Locale.US, "%.6f,%.6f", current.getLongitude(), current.getLatitude()));
                    for (RouteStop s : source) {
                        coords.append(String.format(Locale.US, ";%.6f,%.6f", s.longitude, s.latitude));
                    }

                    String url = "https://router.project-osrm.org/table/v1/driving/" + coords.toString() + "?sources=0&annotations=distance";
                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                    
                    String uniqueId = android.provider.Settings.Secure.getString(requireContext().getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                    String userAgent = "DriveLogApp_v141_" + uniqueId;
                    conn.setRequestProperty("User-Agent", userAgent);
                    
                    if (conn.getResponseCode() == 200) {
                        BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder res = new StringBuilder(); String line;
                        while ((line = r.readLine()) != null) res.append(line);
                        
                        JSONObject json = new JSONObject(res.toString());
                        JSONArray distances = json.getJSONArray("distances").getJSONArray(0);
                        
                        int bestIdx = -1;
                        double minDist = Double.MAX_VALUE;
                        // O índice 0 na resposta é a distância de 'current' para 'current' (sempre 0)
                        // Os índices 1 em diante são as distâncias para as paradas na ordem em que enviamos
                        for (int i = 1; i < distances.length(); i++) {
                            double d = distances.getDouble(i);
                            if (d < minDist) { minDist = d; bestIdx = i - 1; }
                        }
                        
                        if (bestIdx != -1) {
                            RouteStop next = source.get(bestIdx);
                            optimized.add(next);
                            source.remove(bestIdx);
                            current = new GeoPoint(next.latitude, next.longitude);
                        } else break;
                    } else {
                        // Fallback para distância linear se a API falhar
                        RouteStop nearest = null; double minDist = Double.MAX_VALUE;
                        for (RouteStop s : source) {
                            double d = current.distanceToAsDouble(new GeoPoint(s.latitude, s.longitude));
                            if (d < minDist) { minDist = d; nearest = s; }
                        }
                        optimized.add(nearest); source.remove(nearest);
                        current = new GeoPoint(nearest.latitude, nearest.longitude);
                    }
                }

                for (int i = 0; i < optimized.size(); i++) {
                    optimized.get(i).sortOrder = i;
                    optimized.get(i).stopNumber = i + 1; // Sincroniza número da parada com a nova ordem
                }
                dao.updateRouteStops(optimized);

                Activity activity = getActivity();
                if (activity != null) activity.runOnUiThread(() -> {
                    dialog.dismiss();
                    Toast.makeText(getContext(), "Otimização 2.0 concluída!", Toast.LENGTH_SHORT).show();
                    CloudSyncHelper.syncNow(requireContext(), "Atividade na Rota");
                });
            } catch (Exception e) {
                Activity activity = getActivity();
                if (activity != null) activity.runOnUiThread(() -> {
                    dialog.dismiss();
                    Toast.makeText(getContext(), "Erro na otimização 2.0", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    private void updateMarkerIcons() {
        if (map == null || currentStops == null) return;
        int sel = viewPagerStops.getCurrentItem();
        
        new Thread(() -> {
            AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
            List<RouteGroup> groups = dao.getGroupsForRoute(currentRouteId);
            Map<Integer, String> colorMap = new HashMap<>();
            for (RouteGroup g : groups) colorMap.put(g.id, g.color);

            Activity activity = getActivity();
            if (activity != null) activity.runOnUiThread(() -> {
                // 🔥 Reorganiza a ordem das paradas para garantir que o usuário fique por cima, 
                // EXCETO da parada selecionada.
                
                org.osmdroid.views.overlay.Overlay selectedMarker = null;
                List<org.osmdroid.views.overlay.Overlay> otherMarkers = new ArrayList<>();
                
                // Primeiro removemos todos os marcadores de paradas para reinserir na ordem correta
                List<org.osmdroid.views.overlay.Overlay> stopsToRemove = new ArrayList<>();
                for (org.osmdroid.views.overlay.Overlay o : map.getOverlays()) {
                    if (o instanceof Marker) {
                        Marker m = (Marker) o;
                        Object tag = m.getRelatedObject();
                        if (tag instanceof String && ((String) tag).startsWith("STOP_INDEX_")) {
                            try {
                                int i = Integer.parseInt(((String) tag).replace("STOP_INDEX_", ""));
                                if (i >= 0 && i < currentStops.size()) {
                                    RouteStop s = currentStops.get(i);
                                    String gColor = (s.groupId != null) ? colorMap.get(s.groupId) : null;
                                    m.setIcon(createNumberedMarkerIcon(i+1, s.deliveryStatus, s.packageCount>1, (i==sel), gColor));
                                    m.setInfoWindow(null);
                                    if (i == sel) selectedMarker = m;
                                    else otherMarkers.add(m);
                                }
                            } catch (Exception ignored) {}
                            stopsToRemove.add(o);
                        }
                    }
                }

                map.getOverlays().removeAll(stopsToRemove);

                // Agora reinserimos seguindo a hierarquia
                // 1. Paradas normais (Fundo)
                map.getOverlays().addAll(0, otherMarkers);
                
                // 2. Localização do Usuário (Meio) - Já deve estar na lista, mas garantimos que as paradas fiquem abaixo
                // O locationOverlay geralmente está no final ou após o polyline.
                
                // 3. Parada Selecionada (Topo)
                if (selectedMarker != null) {
                    map.getOverlays().add(selectedMarker);
                }
                
                // 4. Localização do Usuário (Topo Absoluto para não sumir)
                if (userDirectionMarker != null) {
                    map.getOverlays().remove(userDirectionMarker);
                    map.getOverlays().add(userDirectionMarker);
                }

                map.invalidate();
            });
        }).start();
    }

    private static class RouteMarker extends Marker {
        private final RouteStop stop; public RouteMarker(MapView mv, RouteStop s) { super(mv); this.stop = s; }
    }
    
    private static class NavInstruction {
        String text; double distance; String type; String modifier;
        NavInstruction(String t, double d, String ty, String m) { text = t; distance = d; type = ty; modifier = m; }
    }

    private class RouteInstructionsAdapter extends RecyclerView.Adapter<RouteInstructionsAdapter.ViewHolder> {
        private final List<NavInstruction> list;
        RouteInstructionsAdapter(List<NavInstruction> l) { this.list = l; }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) { return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_route_instruction, p, false)); }
        @Override public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
            NavInstruction item = list.get(pos);
            h.text.setText(item.text);
            if (item.distance < 1000) h.dist.setText(String.format(Locale.getDefault(), "%.0fm", item.distance));
            else h.dist.setText(String.format(Locale.getDefault(), "%.1fkm", item.distance / 1000.0));
            h.icon.setImageResource(getManeuverIcon(item.type, item.modifier));
        }
        @Override public int getItemCount() { return list.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text, dist; ImageView icon;
            ViewHolder(View v) { super(v); text = v.findViewById(R.id.textInstructionText); dist = v.findViewById(R.id.textInstructionDistance); icon = v.findViewById(R.id.imageInstructionIcon); }
        }
    }

    private static class HourlyWeatherAdapter extends RecyclerView.Adapter<HourlyWeatherAdapter.ViewHolder> {
        private final List<HourlyWeather> list;
        HourlyWeatherAdapter(List<HourlyWeather> l) { this.list = new ArrayList<>(l); }
        void setList(List<HourlyWeather> l) { list.clear(); list.addAll(l); notifyDataSetChanged(); }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) { return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_weather_hourly, p, false)); }
        @Override public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
            HourlyWeather item = list.get(pos);
            h.time.setText(item.time);
            h.temp.setText(String.format(Locale.getDefault(), "%.1f°C", item.temp));
            
            int iconRes = R.drawable.ic_weather_sun;
            if (item.code == 0) iconRes = R.drawable.ic_weather_sun;
            else if (item.code >= 1 && item.code <= 3) iconRes = R.drawable.ic_weather_cloud;
            else if (item.code >= 51 && item.code <= 67) iconRes = R.drawable.ic_weather_rain;
            else if (item.code >= 95) iconRes = R.drawable.ic_weather_thunder;
            
            h.icon.setImageResource(iconRes);
        }
        @Override public int getItemCount() { return list.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView time, temp; ImageView icon;
            ViewHolder(View v) { super(v); time = v.findViewById(R.id.textHourlyTime); temp = v.findViewById(R.id.textHourlyTemp); icon = v.findViewById(R.id.imageHourlyIcon); }
        }
    }

    private static class Suggestion { String displayName; double lat, lon; Suggestion(String d, double la, double lo) { this.displayName = d; this.lat = la; this.lon = lo; } }
    private static class SuggestionsAdapter extends RecyclerView.Adapter<SuggestionsAdapter.ViewHolder> {
        private final List<Suggestion> list; private final OnItemClickListener listener; interface OnItemClickListener { void onItemClick(Suggestion s); }
        SuggestionsAdapter(List<Suggestion> l, OnItemClickListener li) { this.list = l; this.listener = li; }
        void setSuggestions(List<Suggestion> s) { list.clear(); list.addAll(s); notifyDataSetChanged(); }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) { return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_suggestion, p, false)); }
        @Override public void onBindViewHolder(@NonNull ViewHolder h, int pos) { Suggestion s = list.get(pos); h.text.setText(s.displayName); h.itemView.setOnClickListener(v -> listener.onItemClick(s)); }
        @Override public int getItemCount() { return list.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder { TextView text; ViewHolder(View v) { super(v); text = v.findViewById(R.id.textSuggestion); } }
    }
    private static class StopsCardAdapter extends RecyclerView.Adapter<StopsCardAdapter.ViewHolder> {
        private final RouteFragment fragment;
        private final List<RouteStop> list; private final OnStopActionListener listener; interface OnStopActionListener { void onAction(RouteStop s, int a); }
        private RouteHeader routeHeader = null;

        StopsCardAdapter(RouteFragment fragment, List<RouteStop> l, OnStopActionListener li) { this.fragment = fragment; this.list = l; this.listener = li; }
        void setStops(List<RouteStop> s) { list.clear(); list.addAll(s); notifyDataSetChanged(); }
        void setRouteHeader(RouteHeader header) { this.routeHeader = header; notifyDataSetChanged(); }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) { return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_route_stop_card, p, false)); }

        @Override public void onBindViewHolder(@NonNull ViewHolder h, int position, @NonNull List<Object> payloads) {
            if (!payloads.isEmpty() && payloads.contains("TIMER_UPDATE")) {
                updateTimer(h);
            } else {
                super.onBindViewHolder(h, position, payloads);
            }
        }

        private void updateTimer(ViewHolder h) {
            if (routeHeader != null && routeHeader.startTime > 0) {
                long elapsed;
                if (routeHeader.endTime > 0) {
                    elapsed = routeHeader.endTime - routeHeader.startTime;
                } else {
                    elapsed = System.currentTimeMillis() - routeHeader.startTime;
                }
                
                long s = elapsed / 1000;
                long m = s / 60;
                long h_val = m / 60;
                String time = String.format(Locale.getDefault(), "%02d:%02d:%02d", h_val, m % 60, s % 60);
                h.textStopTimer.setText(time);
                
                boolean timerOnCardsOnly = fragment.sharedPreferences.getBoolean("timer_on_cards_only", false);
                h.textStopTimer.setVisibility(timerOnCardsOnly ? View.VISIBLE : View.GONE);
            } else {
                h.textStopTimer.setVisibility(View.GONE);
            }
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder h, int position) { 
            int pos = h.getBindingAdapterPosition();
            RouteStop s = list.get(pos); 
            h.textNumber.setText(String.valueOf(pos + 1)); 
            updateTimer(h);
            h.textAddress.setText(s.address); 

            if (s.buyerCount == 1 && s.allAddresses != null && !s.allAddresses.isEmpty()) {
                // Pega apenas a primeira linha do endereço original para não repetir se houver + de 1 pacote
                String firstAddr = s.allAddresses.split("\n")[0];
                h.textRawAddress.setText(firstAddr);
                h.textRawAddress.setVisibility(View.VISIBLE);
            } else {
                h.textRawAddress.setVisibility(View.GONE);
            }

            h.textNeighborhood.setText(s.neighborhood);
            
            // --- Status Colorido ---
            String status = "Pendente";
            int statusColor = Color.parseColor("#FF9800"); // Laranja para pendente
            if (s.deliveryStatus == 1) {
                status = "Entregue";
                statusColor = Color.parseColor("#4CAF50"); // Verde
            } else if (s.deliveryStatus == 2) {
                status = "Falha";
                statusColor = Color.parseColor("#F44336"); // Vermelho
            }
            h.textStatus.setText(status);
            h.textStatus.setTextColor(statusColor);
            
            h.textPackageCount.setText(s.packageCount > 1 ? s.packageCount + " Pacotes" : "1 Pacote");

            // --- Novas Métricas: Compradores e Sequências ---
            String seqInfo = (s.allSequences != null && !s.allSequences.isEmpty()) ? "Seq: " + s.allSequences : "Seq: " + s.sequence;
            h.textDownloadedStatus.setVisibility(View.VISIBLE); // Reutilizando campo ou garantindo visibilidade
            
            // Note: I will use a dedicated TextView if available, otherwise appending to secondary info
            h.textPackageCount.setText(h.textPackageCount.getText() + " | " + s.buyerCount + " Comp. | " + seqInfo);

            // --- Verificação de Correção Local e Global ---
            new Thread(() -> {
                AppDao dao = AppDatabase.getInstance(h.itemView.getContext()).appDao();
                CorrectedAddress localFix = dao.getCorrectedAddress(s.address);
                String currentUserId = fragment.requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE).getString("current_user_id", "anon");

                Activity activity = fragment.getActivity();
                if (activity != null) activity.runOnUiThread(() -> {
                    if (localFix != null) {
                        h.textDownloadedStatus.setVisibility(View.VISIBLE);
                        boolean isMine = localFix.creatorId == null || (!currentUserId.equals("anon") && localFix.creatorId.equals(currentUserId));
                        if (isMine) {
                            h.textDownloadedStatus.setText(localFix.creatorId == null ? "Correção local" : "Minha correção");
                            h.textDownloadedStatus.setTextColor(Color.parseColor("#4CAF50")); // Verde
                        } else {
                            h.textDownloadedStatus.setText("Correção baixada");
                            h.textDownloadedStatus.setTextColor(Color.parseColor("#FF9800")); // Laranja
                        }
                    } else {
                        // Se não tem local, vamos resetar e esperar a busca global
                        h.textDownloadedStatus.setVisibility(View.GONE);
                    }
                });

                // Feedback Global da Comunidade (Sempre busca se houver algo na nuvem)
                FirebaseHelper.searchGlobal(s.address, new FirebaseHelper.GlobalCorrectionCallback() {
                    @Override
                    public void onResult(double lat, double lon, int likes, int dislikes, String creatorId, String note, int comments, String creatorName, long date) {
                        if (h.getBindingAdapterPosition() == pos) {
                            Activity activity = fragment.getActivity();
                            if (activity != null) activity.runOnUiThread(() -> {
                                h.layoutGlobalFeedback.setVisibility(View.VISIBLE);
                                h.textGlobalStats.setText(likes + " 👍 | " + dislikes + " 👎");

                                if (note != null && !note.isEmpty()) {
                                    h.textGlobalNote.setVisibility(View.VISIBLE);
                                    h.textGlobalNote.setText("Obs: " + note);
                                } else {
                                    h.textGlobalNote.setVisibility(View.GONE);
                                }
                                
                                // Se não tem correção local, mostra que há uma disponível para baixar
                                if (localFix == null) {
                                    h.textDownloadedStatus.setVisibility(View.VISIBLE);
                                    h.textDownloadedStatus.setText("Correção disponível");
                                    h.textDownloadedStatus.setTextColor(Color.parseColor("#F44336")); // Vermelho
                                }
                            });
                        }
                    }
                    @Override public void onError(String msg) {
                        if (h.getBindingAdapterPosition() == pos) {
                            Activity activity = fragment.getActivity();
                            if (activity != null) activity.runOnUiThread(() -> h.layoutGlobalFeedback.setVisibility(View.GONE));
                        }
                    }
                });
            }).start();

            h.btnSuccess.setOnClickListener(v -> listener.onAction(s, 1)); 
            h.btnFailed.setOnClickListener(v -> listener.onAction(s, 2)); 
            h.btnNavigate.setOnClickListener(v -> listener.onAction(s, 3)); 
            h.btnFixLocation.setOnClickListener(v -> listener.onAction(s, 5)); 
            
            // 🔥 Visibilidade dinâmica dos botões baseada no status
            if (s.deliveryStatus == 1) { // Entregue
                h.btnSuccess.setVisibility(View.GONE);
                h.btnFailed.setVisibility(View.VISIBLE);
                h.btnReset.setVisibility(View.VISIBLE);
            } else if (s.deliveryStatus == 2) { // Erro
                h.btnSuccess.setVisibility(View.VISIBLE);
                h.btnFailed.setVisibility(View.GONE);
                h.btnReset.setVisibility(View.VISIBLE);
            } else { // Pendente (0)
                h.btnSuccess.setVisibility(View.VISIBLE);
                h.btnFailed.setVisibility(View.VISIBLE);
                h.btnReset.setVisibility(View.GONE);
            }
            h.btnReset.setOnClickListener(v -> listener.onAction(s, 6)); 

            h.layoutAddress.setOnClickListener(v -> listener.onAction(s, 7)); 
            h.layoutGlobalFeedback.setOnClickListener(v -> listener.onAction(s, 8)); 

            h.itemView.setOnLongClickListener(v -> {
                listener.onAction(s, 4); // 4 é o código para excluir
                return true;
            });
        }
        @Override public int getItemCount() { return list.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder { TextView textNumber, textStopTimer, textAddress, textRawAddress, textNeighborhood, textStatus, textPackageCount, textGlobalStats, textGlobalNote, textDownloadedStatus; View btnSuccess, btnFailed, btnNavigate, btnFixLocation, btnReset, layoutAddress, layoutGlobalFeedback; MaterialCardView card; ViewHolder(View v) { super(v); card = v.findViewById(R.id.cardStop); textNumber = v.findViewById(R.id.textStopNumber); textStopTimer = v.findViewById(R.id.textStopTimer); textAddress = v.findViewById(R.id.textStopAddress); textRawAddress = v.findViewById(R.id.textRawAddress); textNeighborhood = v.findViewById(R.id.textStopNeighborhood); textStatus = v.findViewById(R.id.textStopStatus); textPackageCount = v.findViewById(R.id.textPackageCount); textGlobalStats = v.findViewById(R.id.textGlobalStats); textGlobalNote = v.findViewById(R.id.textGlobalNote); textDownloadedStatus = v.findViewById(R.id.textDownloadedStatus); btnSuccess = v.findViewById(R.id.btnSuccess); btnFailed = v.findViewById(R.id.btnFailed); btnNavigate = v.findViewById(R.id.btnNavigate); btnFixLocation = v.findViewById(R.id.btnFixLocation); btnReset = v.findViewById(R.id.btnReset); layoutAddress = v.findViewById(R.id.layoutStopText); layoutGlobalFeedback = v.findViewById(R.id.layoutGlobalFeedback); } }
    }
    private static class StopsListAdapter extends RecyclerView.Adapter<StopsListAdapter.ViewHolder> {
        private List<RouteStop> fullList = new ArrayList<>();
        private List<RouteStop> filteredList = new ArrayList<>();
        private final OnItemClickListener click; 
        private final OnItemLongClickListener longClick; 
        private final Map<Integer, String> groupColors = new HashMap<>();
        private boolean isUnifyMode = false;
        private final java.util.Set<RouteStop> selectedStops = new java.util.HashSet<>();

        interface OnItemClickListener { void onItemClick(RouteStop s); } 
        interface OnItemLongClickListener { void onItemLongClick(RouteStop s); }
        
        StopsListAdapter(List<RouteStop> l, OnItemClickListener c, OnItemLongClickListener lc) { 
            this.fullList = l; 
            this.filteredList = new ArrayList<>(l);
            this.click = c; 
            this.longClick = lc; 
        }
        
        void setUnifyMode(boolean e) { 
            this.isUnifyMode = e; 
            if (!e) selectedStops.clear(); 
            notifyDataSetChanged(); 
        }
        
        java.util.Set<RouteStop> getSelectedStops() { return selectedStops; }

        void setStops(List<RouteStop> s) { 
            this.fullList = s; 
            this.filteredList = new ArrayList<>(s); 
            notifyDataSetChanged(); 
        }

        void swap(int from, int to) {
            if (from < filteredList.size() && to < filteredList.size()) {
                java.util.Collections.swap(filteredList, from, to);
                notifyItemMoved(from, to);
            }
        }

        void filter(String query) {
            filteredList.clear();
            if (query.isEmpty()) {
                filteredList.addAll(fullList);
            } else {
                String q = query.toLowerCase().trim();
                for (RouteStop s : fullList) {
                    String addr = (s.address != null ? s.address : "").toLowerCase();
                    String num = String.valueOf(s.stopNumber);
                    if (addr.contains(q) || num.equals(q)) {
                        filteredList.add(s);
                    }
                }
            }
            notifyDataSetChanged();
        }
        
        void updateGroupColors(List<RouteGroup> groups) {
            groupColors.clear();
            for (RouteGroup g : groups) groupColors.put(g.id, g.color);
            notifyDataSetChanged();
        }

        void setEditMode(boolean e) { }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) { return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_route_stop_list, p, false)); }
        @Override public void onBindViewHolder(@NonNull ViewHolder h, int pos) { 
            RouteStop s = filteredList.get(pos); 
            h.textNumber.setText(String.valueOf(s.stopNumber));
            h.textAddress.setText(s.address); 

            if (s.buyerCount == 1 && s.allAddresses != null && !s.allAddresses.isEmpty()) {
                // Pega apenas a primeira linha do endereço original para não repetir se houver + de 1 pacote
                String firstAddr = s.allAddresses.split("\n")[0];
                h.textRawAddress.setText(firstAddr);
                h.textRawAddress.setVisibility(View.VISIBLE);
            } else {
                h.textRawAddress.setVisibility(View.GONE);
            }

            // 🔥 Reset inicial e Tag para evitar bug de reciclagem e "piscadeira"
            h.textDownloadedStatus.setVisibility(View.GONE);
            h.textDownloadedStatus.setTag(s.address);
            final String boundAddress = s.address;

            // Verificação de Correção para a Lista
            new Thread(() -> {
                AppDao dao = AppDatabase.getInstance(h.itemView.getContext()).appDao();
                CorrectedAddress localFix = dao.getCorrectedAddress(boundAddress);
                
                h.itemView.post(() -> {
                    // Se o ViewHolder já foi reciclado para outro endereço, ignora
                    if (!boundAddress.equals(h.textDownloadedStatus.getTag())) return;

                    if (localFix != null) {
                        h.textDownloadedStatus.setVisibility(View.VISIBLE);
                        String currentUserId = h.itemView.getContext().getSharedPreferences("AppConfig", android.content.Context.MODE_PRIVATE).getString("current_user_id", "anon");
                        
                        boolean isMine = localFix.creatorId == null || (!currentUserId.equals("anon") && localFix.creatorId.equals(currentUserId));
                        if (isMine) {
                            h.textDownloadedStatus.setText(localFix.creatorId == null ? "Correção local" : "Minha correção");
                            h.textDownloadedStatus.setTextColor(Color.parseColor("#4CAF50")); // Verde
                        } else {
                            h.textDownloadedStatus.setText("Correção baixada");
                            h.textDownloadedStatus.setTextColor(Color.parseColor("#FF9800")); // Laranja
                        }
                    } else {
                        h.textDownloadedStatus.setVisibility(View.GONE);
                    }

                    // Verifica global SEMPRE para mostrar os likes/deslikes se existirem
                    FirebaseHelper.searchGlobal(boundAddress, new FirebaseHelper.GlobalCorrectionCallback() {
                        @Override public void onResult(double lat, double lon, int likes, int dislikes, String creatorId, String note, int comments, String creatorName, long date) {
                            h.itemView.post(() -> {
                                if (boundAddress.equals(h.textDownloadedStatus.getTag())) {
                                    if (localFix == null) {
                                        h.textDownloadedStatus.setVisibility(View.VISIBLE);
                                        h.textDownloadedStatus.setText("Correção disponível");
                                        h.textDownloadedStatus.setTextColor(Color.parseColor("#F44336")); // Vermelho
                                    }
                                    
                                    h.layoutGlobalFeedback.setVisibility(View.VISIBLE);
                                    h.textGlobalStats.setText(likes + " 👍 | " + dislikes + " 👎");
                                }
                            });
                        }
                        @Override public void onError(String msg) {
                            h.itemView.post(() -> {
                                if (boundAddress.equals(h.textDownloadedStatus.getTag())) {
                                    h.layoutGlobalFeedback.setVisibility(View.GONE);
                                }
                            });
                        }
                    });
                });
            }).start();
            
            // Visual de Unificação
            h.checkBox.setVisibility(isUnifyMode ? View.VISIBLE : View.GONE);
            h.checkBox.setChecked(selectedStops.contains(s));

            // Visual do Grupo
            if (s.groupId != null && groupColors.containsKey(s.groupId)) {
                int color = Color.parseColor(groupColors.get(s.groupId));
                h.divider.setVisibility(View.VISIBLE);
                h.divider.setBackgroundColor(color);
                h.card.setStrokeColor(color);
                h.card.setStrokeWidth(4);
            } else {
                h.divider.setVisibility(View.GONE);
                h.card.setStrokeWidth(0);
            }

            h.itemView.setOnClickListener(v -> {
                if (isUnifyMode) {
                    if (selectedStops.contains(s)) selectedStops.remove(s);
                    else selectedStops.add(s);
                    notifyItemChanged(h.getBindingAdapterPosition());
                    if (click != null) click.onItemClick(s);
                } else {
                    click.onItemClick(s);
                }
            }); 
            h.itemView.setOnLongClickListener(v -> { longClick.onItemLongClick(s); return true; }); 
        }
        @Override public int getItemCount() { return filteredList.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder { 
            TextView textNumber, textAddress, textRawAddress, textNeighborhood, textStatus, textDownloadedStatus, textGlobalStats; 
            ImageView imageStatus; 
            android.widget.CheckBox checkBox;
            MaterialCardView card; 
            View divider, layoutGlobalFeedback; 
            ViewHolder(View v) { 
                super(v); 
                card = v.findViewById(R.id.cardListStop); 
                divider = v.findViewById(R.id.viewGroupDivider); 
                textNumber = v.findViewById(R.id.textListNumber); 
                textAddress = v.findViewById(R.id.textListAddress); 
                textRawAddress = v.findViewById(R.id.textListRawAddress);
                textNeighborhood = v.findViewById(R.id.textListNeighborhood); 
                textStatus = v.findViewById(R.id.textListStatus); 
                imageStatus = v.findViewById(R.id.imageListStatus); 
                textDownloadedStatus = v.findViewById(R.id.textListDownloadedStatus); 
                checkBox = v.findViewById(R.id.checkListUnify);
                layoutGlobalFeedback = v.findViewById(R.id.layoutListGlobalFeedback);
                textGlobalStats = v.findViewById(R.id.textListGlobalStats);
            } 
        }
    }

    private void loadSavedRoute(int kmId) {
        new Thread(() -> {
            if (getContext() == null) return;
            List<RoutePoint> savedPoints = AppDatabase.getInstance(requireContext()).appDao().getRoutePointsForKm(kmId);
            DailyKm km = AppDatabase.getInstance(requireContext()).appDao().getAllDailyKm().stream().filter(k -> k.id == kmId).findFirst().orElse(null);

            if (savedPoints != null && !savedPoints.isEmpty()) {
                historicalPoints = savedPoints;
                List<GeoPoint> geoPoints = new ArrayList<>();
                for (RoutePoint rp : savedPoints) geoPoints.add(new GeoPoint(rp.latitude, rp.longitude));
                
                Activity activity = getActivity();
                if (activity != null) activity.runOnUiThread(() -> {
                    // Oculta UI normal de rotas para foco total no trajeto
                    if (layoutSideFabs != null) layoutSideFabs.setVisibility(View.GONE);
                    if (bottomSheet != null) {
                        bottomSheet.setVisibility(View.GONE);
                        if (bottomSheetBehavior != null) bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    }
                    if (layoutSearchContainer != null) layoutSearchContainer.setVisibility(View.GONE);
                    if (layoutSummary != null) layoutSummary.setVisibility(View.GONE);
                    if (layoutLeftSummary != null) layoutLeftSummary.setVisibility(View.GONE);
                    if (layoutSwitchContainer != null) layoutSwitchContainer.setVisibility(View.GONE);
                    if (cardRouteTotalTime != null) cardRouteTotalTime.setVisibility(View.GONE);
                    if (cardWeatherSummary != null) cardWeatherSummary.setVisibility(View.GONE);
                    if (cardToggleSystemUI != null) cardToggleSystemUI.setVisibility(View.GONE);
                    
                    // Limpa mapa e desenha trajeto
                    map.getOverlays().removeIf(o -> o instanceof Marker || o instanceof Polyline);
                    showHomeMarker();
                    showLoadingMarkers();

                    Polyline historyPoly = new Polyline(map);
                    historyPoly.setPoints(geoPoints);
                    historyPoly.getOutlinePaint().setColor(Color.parseColor("#2196F3"));
                    historyPoly.getOutlinePaint().setStrokeWidth(12f);
                    map.getOverlays().add(historyPoly);

                    detectAndMarkHistoricalStops(geoPoints);

                    // Marcadores de Início e Fim
                    Marker startMarker = new Marker(map);
                    startMarker.setPosition(geoPoints.get(0));
                    startMarker.setTitle("Início do Trajeto");
                    startMarker.setIcon(ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_mylocation));
                    if (startMarker.getIcon() != null) startMarker.getIcon().setTint(Color.GREEN);
                    map.getOverlays().add(startMarker);

                    Marker endMarker = new Marker(map);
                    endMarker.setPosition(geoPoints.get(geoPoints.size() - 1));
                    endMarker.setTitle("Fim do Trajeto");
                    endMarker.setIcon(ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_recent_history));
                    if (endMarker.getIcon() != null) endMarker.getIcon().setTint(Color.RED);
                    map.getOverlays().add(endMarker);
                    
                    timelineMarker = new Marker(map);
                    timelineMarker.setTitle("Posição no Horário");
                    timelineMarker.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_car_marker));
                    if (timelineMarker.getIcon() != null) timelineMarker.getIcon().setTint(Color.parseColor("#FF9800"));
                    timelineMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                    map.getOverlays().add(timelineMarker);

                    // Ativa Linha do Tempo e garante visibilidade
                    if (cardTimeline != null) {
                        cardTimeline.setVisibility(View.VISIBLE);
                        cardTimeline.setAlpha(1.0f);
                        cardTimeline.bringToFront();
                    }
                    
                    if (seekBarTimeline != null) {
                        seekBarTimeline.setMax(geoPoints.size() - 1);
                        seekBarTimeline.setProgress(0);
                    }
                    updateTimelineMarker(0);
                    
                    pauseTimelinePlayback(); 
                    setTimelineSpeed(1);

                    map.invalidate();
                    
                    // Ajusta Zoom com margem de segurança generosa
                    if (geoPoints.size() > 1) {
                        try {
                            BoundingBox bbox = BoundingBox.fromGeoPoints(geoPoints);
                            map.zoomToBoundingBox(bbox, true, 250);
                            
                            // Garante que o zoom não seja EXCESSIVO (muito perto) após a animação
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                if (isAdded() && map != null && map.getZoomLevelDouble() > 17.5) {
                                    map.getController().setZoom(16.5);
                                }
                            }, 1000);
                        } catch (Exception e) {
                            map.getController().setZoom(15.0);
                            map.getController().animateTo(geoPoints.get(0));
                        }
                    } else {
                        map.getController().setZoom(16.0);
                        map.getController().animateTo(geoPoints.get(0));
                    }
                    
                    Toast.makeText(getContext(), "Gravação Histórica Carregada", Toast.LENGTH_SHORT).show();
                });
            } else {
                Activity activity = getActivity();
                if (activity != null) activity.runOnUiThread(() -> Toast.makeText(getContext(), "Nenhum ponto de GPS nesta gravação.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void detectAndMarkHistoricalStops(List<GeoPoint> points) {
        if (points.size() < 2 || historicalPoints.isEmpty()) return;
        SharedPreferences prefs = sharedPreferences;
        int shortTimeMaxMs = prefs.getInt("tracking_short_stop_time", 60) * 1000;
        int shortRadius = prefs.getInt("tracking_short_stop_radius", 20);
        int mediumTimeMaxMs = prefs.getInt("tracking_medium_stop_time", 240) * 1000;
        int mediumRadius = prefs.getInt("tracking_medium_stop_radius", 40);
        int longRadius = prefs.getInt("tracking_long_stop_radius", 80);
        String colorShort = prefs.getString("tracking_color_short", "#4CAF50");
        String colorMedium = prefs.getString("tracking_color_medium", "#FBC02D");
        String colorLong = prefs.getString("tracking_color_long", "#F44336");

        int i = 0;
        while (i < points.size()) {
            int j = i + 1;
            long startTime = historicalPoints.get(i).timestamp;
            int currentRadius = shortRadius; 
            while (j < points.size()) {
                double dist = points.get(i).distanceToAsDouble(points.get(j));
                long durationSoFar = historicalPoints.get(j).timestamp - startTime;
                if (durationSoFar >= mediumTimeMaxMs) currentRadius = longRadius;
                else if (durationSoFar >= shortTimeMaxMs) currentRadius = mediumRadius;
                else currentRadius = shortRadius;
                if (dist > currentRadius) break; 
                j++;
            }
            long durationMs = historicalPoints.get(Math.min(j - 1, points.size() - 1)).timestamp - startTime;
            if (durationMs >= prefs.getInt("min_stop_duration_seconds", 15) * 1000) {
                int color;
                if (durationMs >= mediumTimeMaxMs) color = Color.parseColor(colorLong);
                else if (durationMs >= shortTimeMaxMs) color = Color.parseColor(colorMedium);
                else color = Color.parseColor(colorShort);
                addHistoricalStopMarker(points.get(i), "Parada: " + formatDuration(durationMs), color);
                i = j;
            } else i++;
        }
    }

    private void addHistoricalStopMarker(GeoPoint point, String title, int color) {
        Marker m = new Marker(map); m.setPosition(point); m.setTitle(title); m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        android.graphics.drawable.ShapeDrawable dot = new android.graphics.drawable.ShapeDrawable(new android.graphics.drawable.shapes.OvalShape());
        dot.setIntrinsicWidth(20); dot.setIntrinsicHeight(20); dot.getPaint().setColor(color); dot.getPaint().setStyle(Paint.Style.FILL_AND_STROKE);
        m.setIcon(dot); map.getOverlays().add(m);
    }

    private String formatDuration(long ms) {
        long s = ms / 1000; long m = s / 60; s %= 60;
        return m > 0 ? m + "m " + s + "s" : s + "s";
    }

    private void exitHistoryMode() {
        pauseTimelinePlayback();
        if (cardTimeline != null) cardTimeline.setVisibility(View.GONE);
        historicalPoints.clear();
        timelineMarker = null;
        
        // Limpa visualização da gravação do mapa
        if (map != null) {
            map.getOverlays().removeIf(o -> o instanceof Marker || o instanceof Polyline);
        }

        // Restaura UI normal
        if (layoutSideFabs != null) layoutSideFabs.setVisibility(View.VISIBLE);
        if (layoutSearchContainer != null) layoutSearchContainer.setVisibility(View.VISIBLE);
        if (layoutSummary != null) layoutSummary.setVisibility(View.VISIBLE);
        if (layoutLeftSummary != null) layoutLeftSummary.setVisibility(View.VISIBLE);
        if (layoutSwitchContainer != null) layoutSwitchContainer.setVisibility(View.VISIBLE);
        if (cardToggleSystemUI != null && sharedPreferences.getInt("app_mode", 0) != 1) {
            cardToggleSystemUI.setVisibility(View.VISIBLE);
        }
        
        updateAppModeUI();
        updateFloatingButtonsVisibility();
        
        // Recarrega paradas da rota atual e elementos do mapa
        showHomeMarker();
        showLoadingMarkers();
        if (currentRouteId != -1) loadLastRoute();
        else refreshMarkers();
        
        centerOnCurrentLocation();
    }

    private void setupTimelineListener() {
        if (seekBarTimeline == null) return;
        seekBarTimeline.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { if (fromUser) updateTimelineMarker(progress); }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { pauseTimelinePlayback(); }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void toggleTimelinePlayback() { if (isTimelinePlaying) pauseTimelinePlayback(); else startTimelinePlayback(); }
    private void startTimelinePlayback() {
        if (seekBarTimeline.getProgress() >= seekBarTimeline.getMax()) seekBarTimeline.setProgress(0);
        isTimelinePlaying = true;
        if (btnTimelinePlayPause != null) btnTimelinePlayPause.setImageResource(R.drawable.ic_pause);
        playbackHandler.post(playbackRunnable);
    }
    private void pauseTimelinePlayback() {
        isTimelinePlaying = false;
        if (btnTimelinePlayPause != null) btnTimelinePlayPause.setImageResource(R.drawable.ic_play);
        playbackHandler.removeCallbacks(playbackRunnable);
    }
    private void setTimelineSpeed(int multiplier) {
        timelineSpeedMultiplier = multiplier;
        if (getContext() == null) return;
        TypedValue tv = new TypedValue();
        requireContext().getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, tv, true);
        int cp = tv.data; int cg = Color.GRAY;
        if (btnS1 != null) btnS1.setTextColor(multiplier == 1 ? cp : cg);
        if (btnS2 != null) btnS2.setTextColor(multiplier == 2 ? cp : cg);
        if (btnS4 != null) btnS4.setTextColor(multiplier == 4 ? cp : cg);
        if (btnS8 != null) btnS8.setTextColor(multiplier == 8 ? cp : cg);
    }

    private void updateTimelineMarker(int index) {
        if (historicalPoints == null || index >= historicalPoints.size() || timelineMarker == null) return;
        RoutePoint p = historicalPoints.get(index);
        GeoPoint gp = new GeoPoint(p.latitude, p.longitude);
        timelineMarker.setPosition(gp);
        if (textTimelineTime != null) textTimelineTime.setText(timeFormat.format(new java.util.Date(p.timestamp)));
        mapController.animateTo(gp);
        map.invalidate();
    }
}
