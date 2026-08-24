package com.example.drivelog;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.cachemanager.CacheManager;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class MapsFragment extends Fragment {

    private MapView map = null;
    private IMapController mapController;
    private MyLocationNewOverlay locationOverlay;
    private Polyline polyline;
    private MapEventsOverlay homeSelectionOverlay;
    private Marker homeMarker;
    private Polygon homeRadiusOverlay;

    // Loading Points
    private List<LoadingPoint> loadingPoints = new ArrayList<>();
    private List<Marker> loadingMarkers = new ArrayList<>();
    private MapEventsOverlay loadingSelectionOverlay;
    
    private FusedLocationProviderClient fusedLocationClient;
    private FloatingActionButton fabTracking, fabStop, fabDownload, fabCenter, fabHome, fabLoadingPoints, fabDeliveryApp;
    private TextView textStatus;
    private View cardRestWarning;
    private View btnDisableRestInMaps;
    private boolean isFollowingUser = true;

    // Modo Comboio
    private com.google.firebase.firestore.ListenerRegistration comboioListener;
    private final Map<String, Marker> friendMarkers = new HashMap<>();

    // Timeline UI
    private View cardTimeline;
    private SeekBar seekBarTimeline;
    private TextView textTimelineTime;
    private TextView btnS1, btnS2, btnS4, btnS8;
    private View btnExitHistory; // Ensure this is View, not ImageView
    private ImageView btnTimelinePlayPause;
    private Marker timelineMarker;
    private List<RoutePoint> currentHistoricalPoints = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    // Playback logic
    private boolean isTimelinePlaying = false;
    private int timelineSpeedMultiplier = 1;
    private final Handler playbackHandler = new Handler(Looper.getMainLooper());
    private final Runnable playbackRunnable = new Runnable() {
        @Override
        public void run() {
            if (isTimelinePlaying && seekBarTimeline != null) {
                int currentProgress = seekBarTimeline.getProgress();
                if (currentProgress < seekBarTimeline.getMax()) {
                    int nextProgress = currentProgress + 1;
                    seekBarTimeline.setProgress(nextProgress);
                    updateTimelineMarker(nextProgress);
                    
                    long delay = 200 / timelineSpeedMultiplier;
                    playbackHandler.postDelayed(this, delay);
                } else {
                    pauseTimelinePlayback();
                }
            }
        }
    };

    private SharedPreferences sharedPreferences;
    private OnlineTileSourceBase currentMapSource;
    private final SharedPreferences.OnSharedPreferenceChangeListener prefListener = (prefs, key) -> {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            if ("tracking_mode_v2".equals(key)) {
                updateTrackingUI();
            } else if ("user_map_icon".equals(key)) {
                setupLocationOverlay();
            } else if ("map_tile_style".equals(key)) {
                applyMapStyle();
            } else if ("show_fab_km_tracking".equals(key)) {
                refreshRemoteVisibility();
            }
        });
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

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (Boolean.TRUE.equals(result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false))) {
                    setupLocationOverlay();
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = requireContext();
        sharedPreferences = ctx.getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        
        // Removido Configuration.load() para não sobrescrever o User-Agent global da MainActivity

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_maps, container, false);

        map = view.findViewById(R.id.map);
        sharedPreferences = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        applyMapStyle();

        map.setMultiTouchControls(true);

        mapController = map.getController();
        mapController.setZoom(17.0);

        fabTracking = view.findViewById(R.id.fabTracking);
        fabStop = view.findViewById(R.id.fabStop);
        fabDownload = view.findViewById(R.id.fabDownload);
        fabCenter = view.findViewById(R.id.fabCenter);
        fabHome = view.findViewById(R.id.fabHome);
        fabHome.setImageResource(R.drawable.ic_home);
        fabLoadingPoints = view.findViewById(R.id.fabLoadingPoints);
        fabDeliveryApp = view.findViewById(R.id.fabDeliveryApp);
        textStatus = view.findViewById(R.id.textTrackingStatus);
        cardRestWarning = view.findViewById(R.id.cardRestWarning);
        btnDisableRestInMaps = view.findViewById(R.id.btnDisableRestInMaps);

        if (btnDisableRestInMaps != null) {
            btnDisableRestInMaps.setOnClickListener(v -> disableRestAndGoToSettings());
        }

        cardTimeline = view.findViewById(R.id.cardTimeline);
        seekBarTimeline = view.findViewById(R.id.seekBarTimeline);
        textTimelineTime = view.findViewById(R.id.textTimelineTime);
        btnExitHistory = view.findViewById(R.id.btnExitHistory);
        btnTimelinePlayPause = view.findViewById(R.id.btnTimelinePlayPause);
        
        btnS1 = view.findViewById(R.id.btnSpeed1x);
        btnS2 = view.findViewById(R.id.btnSpeed2x);
        btnS4 = view.findViewById(R.id.btnSpeed4x);
        btnS8 = view.findViewById(R.id.btnSpeed8x);

        setupPolyline();
        
        if (checkPermissions()) {
            setupLocationOverlay();
        } else {
            requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }

        // Observe tracking state, pause state, points and distance from Service
        TrackingService.isTracking.observe(getViewLifecycleOwner(), tracking -> updateTrackingUI());
        TrackingService.isPaused.observe(getViewLifecycleOwner(), paused -> updateTrackingUI());
        TrackingService.currentDistance.observe(getViewLifecycleOwner(), dist -> updateTrackingUI());
        
        TrackingService.pathPoints.observe(getViewLifecycleOwner(), points -> {
            if (points == null || points.isEmpty() || map == null) return;
            
            // Garante que a polyline exista e esteja no mapa
            if (polyline == null) setupPolyline();
            
            if (Boolean.TRUE.equals(TrackingService.isTracking.getValue())) {
                ArrayList<GeoPoint> geoPoints = new ArrayList<>();
                for (RoutePoint rp : points) {
                    if (rp != null) geoPoints.add(new GeoPoint(rp.latitude, rp.longitude));
                }
                
                try {
                    polyline.setPoints(geoPoints);
                    currentHistoricalPoints = new ArrayList<>(points);
                    detectAndMarkStops(geoPoints, true);
                    
                    // Lógica de Auto-Siga
                    if (!geoPoints.isEmpty() && isFollowingUser) {
                        mapController.animateTo(geoPoints.get(geoPoints.size() - 1));
                    }
                    
                    map.invalidate();
                } catch (Exception e) {
                    // Silencia erros de renderização concorrente
                }
            }
        });

        fabTracking.setOnClickListener(v -> showKmTrackingPopup());
        fabStop.setOnClickListener(v -> handleStop());
        fabDownload.setOnClickListener(v -> promptDownloadArea());
        fabCenter.setOnClickListener(v -> centerOnCurrentLocation());
        fabHome.setOnClickListener(v -> startHomeSelection());
        fabLoadingPoints.setOnClickListener(v -> showLoadingPointsDialog());
        fabDeliveryApp.setOnClickListener(v -> launchDeliveryApp());
        btnExitHistory.setOnClickListener(v -> exitHistoryMode());

        btnTimelinePlayPause.setOnClickListener(v -> toggleTimelinePlayback());
        
        btnS1.setOnClickListener(v -> setTimelineSpeed(1));
        btnS2.setOnClickListener(v -> setTimelineSpeed(2));
        btnS4.setOnClickListener(v -> setTimelineSpeed(4));
        btnS8.setOnClickListener(v -> setTimelineSpeed(8));

        setupTimelineListener();

        checkRestInterval();

        // Listener para detectar quando o usuário move o mapa manualmente e desativar o auto-siga
        map.addMapListener(new org.osmdroid.events.MapListener() {
            @Override
            public boolean onScroll(org.osmdroid.events.ScrollEvent event) {
                if (event.getX() != 0 || event.getY() != 0) {
                    isFollowingUser = false;
                }
                return false;
            }
            @Override public boolean onZoom(org.osmdroid.events.ZoomEvent event) { return false; }
        });

        return view;
    }

    private void setupPolyline() {
        if (map == null) return;
        
        // Remove polylines anteriores para evitar duplicação ou conflitos
        map.getOverlays().remove(polyline);

        SharedPreferences prefs = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        String colorHex = prefs.getString("tracking_route_line_color", "#2196F3");

        polyline = new Polyline(map); 
        polyline.getOutlinePaint().setColor(Color.parseColor(colorHex));
        polyline.getOutlinePaint().setStrokeWidth(12f);
        map.getOverlays().add(polyline);
    }

    private void setupLocationOverlay() {
        if (map == null || getContext() == null) return;
        if (locationOverlay != null) map.getOverlays().remove(locationOverlay);
        
        locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), map);
        
        // Customização do Ícone do Usuário
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        String iconType = sharedPreferences.getString("user_map_icon", "arrow");
        if (!iconType.equals("arrow")) {
            int resId = R.drawable.ic_car_marker; // Default car
            if (iconType.equals("moto")) resId = R.drawable.ic_play; 
            else if (iconType.equals("truck")) resId = R.drawable.ic_package;

            try {
                Bitmap bmp = drawableToBitmap(resId, iconType.equals("moto"));
                locationOverlay.setPersonIcon(bmp);
                locationOverlay.setDirectionIcon(bmp);
                locationOverlay.setPersonHotspot(bmp.getWidth() / 2f, bmp.getHeight() / 2f);
            } catch (Exception e) { e.printStackTrace(); }
        }

        // 🔥 BLOQUEIO DE BATERIA: Só ativa se não estiver no horário de descanso
        if (!isRestIntervalNow()) {
            locationOverlay.enableMyLocation();
        }
        
        map.getOverlays().add(locationOverlay);

        RotationGestureOverlay rotationGestureOverlay = new RotationGestureOverlay(map);
        rotationGestureOverlay.setEnabled(true);
        map.getOverlays().add(rotationGestureOverlay);
    }

    private Bitmap drawableToBitmap(int resId, boolean rotate) {
        Drawable d = ContextCompat.getDrawable(requireContext(), resId);
        int size = (int) (38 * getResources().getDisplayMetrics().density);
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        if (rotate) {
            c.save();
            c.rotate(-90, size/2f, size/2f);
        }
        d.setBounds(0, 0, size, size);
        d.setTint(ContextCompat.getColor(requireContext(), R.color.teal_700));
        d.draw(c);
        if (rotate) c.restore();
        return b;
    }

    public void refreshRemoteVisibility() {
        if (getActivity() instanceof MainActivity && fabTracking != null) {
            boolean visible = ((MainActivity) getActivity()).isMenuVisible("km");
            boolean prefVisible = sharedPreferences.getBoolean("show_fab_km_tracking", true);
            
            fabTracking.setVisibility((visible && prefVisible) ? View.VISIBLE : View.GONE);
            if (fabStop != null) {
                boolean isTracking = Boolean.TRUE.equals(TrackingService.isTracking.getValue());
                fabStop.setVisibility((visible && prefVisible && isTracking) ? View.VISIBLE : View.GONE);
            }
            if (fabLoadingPoints != null) fabLoadingPoints.setVisibility(visible ? View.VISIBLE : View.GONE);
            showLoadingMarkers(); // Re-avalia se deve mostrar marcadores
        }
    }

    private void centerOnCurrentLocation() {
        isFollowingUser = true;
        if (locationOverlay != null && locationOverlay.getMyLocation() != null) {
            mapController.animateTo(locationOverlay.getMyLocation());
            return;
        }
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    mapController.animateTo(new GeoPoint(location.getLatitude(), location.getLongitude()));
                }
            });
        } catch (SecurityException ignored) {}
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
                
                TrackingHelper.updateAutoTracking(requireContext());
                Toast.makeText(getContext(), isChecked ? "Modo Automático Ativado" : "Modo Manual Ativado", Toast.LENGTH_SHORT).show();
                
                if (!tracking) {
                    btnPlayPause.setVisibility(isChecked ? View.GONE : View.VISIBLE);
                }
                updateTrackingUI(); 
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

        if (!tracking && currentMode != 0) {
            btnPlayPause.setVisibility(View.GONE);
        } else {
            btnPlayPause.setVisibility(View.VISIBLE);
        }

        btnPlayPause.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), TrackingService.class);
            if (!tracking) {
                if (checkPermissions()) {
                    exitHistoryMode();
                    isFollowingUser = true;
                    intent.setAction("START");
                    startService(intent);
                } else {
                    requestPermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
                }
            } else if (!paused) {
                intent.setAction("PAUSE");
                startService(intent);
            } else {
                intent.setAction("START");
                startService(intent);
            }
            dialog.dismiss();
        });

        btnStop.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Finalizar Rastreamento")
                    .setMessage("Deseja parar e salvar este trajeto?")
                    .setPositiveButton("Parar e Salvar", (d, which) -> {
                        Intent intent = new Intent(getContext(), TrackingService.class);
                        intent.setAction("STOP");
                        startService(intent);
                        dialog.dismiss();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        dialog.show();
    }

    private void handleStop() {
        new AlertDialog.Builder(getContext())
                .setTitle("Finalizar Rastreamento")
                .setMessage("Deseja parar e salvar este trajeto?")
                .setPositiveButton("Parar e Salvar", (dialog, which) -> {
                    Intent intent = new Intent(getContext(), TrackingService.class);
                    intent.setAction("STOP");
                    requireContext().startService(intent);
                })
                .setNegativeButton("Continuar", null)
                .show();
    }

    private void startService(Intent intent) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent);
        } else {
            requireContext().startService(intent);
        }
    }

    private void updateTrackingUI() {
        if (getContext() == null) return;

        Context context = getContext();
        SharedPreferences prefs = context.getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        int trackingMode = 0;
        try {
            trackingMode = prefs.getInt("tracking_mode_v2", 0); // 0: Manual, 1: Tempo, 2: Distância
        } catch (Exception e) {
            Object val = prefs.getAll().get("tracking_mode_v2");
            if (val != null) {
                try {
                    String s = String.valueOf(val);
                    if (s.equalsIgnoreCase("true")) trackingMode = 1;
                    else if (s.equalsIgnoreCase("false")) trackingMode = 0;
                    else trackingMode = (int) Double.parseDouble(s);
                } catch (Exception ignored) {}
            }
            prefs.edit().putInt("tracking_mode_v2", trackingMode).apply();
        }
        
        boolean tracking = Boolean.TRUE.equals(TrackingService.isTracking.getValue());
        boolean paused = Boolean.TRUE.equals(TrackingService.isPaused.getValue());
        Double dist = TrackingService.currentDistance.getValue();
        Double cost = TrackingService.estimatedCost.getValue();
        if (dist == null) dist = 0.0;
        if (cost == null) cost = 0.0;

        if (fabTracking == null || fabStop == null || textStatus == null) return;

        // O botão de play/pause (fabTracking) só aparece no modo Manual (0)
        // No modo Tempo (1) ou Distância (2), o início é automático
        fabTracking.setVisibility(trackingMode == 0 ? View.VISIBLE : View.GONE);

        if (!tracking) {
            fabTracking.setImageResource(R.drawable.ic_play);
            fabTracking.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.GREEN));
            fabStop.setVisibility(View.GONE);
            textStatus.setText("Rastreamento Desativado");
        } else {
            // No modo automático, o botão de STOP ainda pode ser útil para encerrar o dia forçadamente
            fabStop.setVisibility(View.VISIBLE);

            String distText = String.format(Locale.getDefault(), "%.2f KM", dist);
            String costText = String.format(Locale.getDefault(), "R$ %.2f", cost);
            String info = distText + " | " + costText;
            
            if (paused) {
                fabTracking.setImageResource(R.drawable.ic_play);
                fabTracking.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.YELLOW));
                textStatus.setText("Pausado: " + info);
            } else {
                fabTracking.setImageResource(R.drawable.ic_pause);
                fabTracking.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.RED));
                textStatus.setText("Gravando: " + info);
            }
        }
        
        checkRestInterval();
    }

    private boolean isRestIntervalNow() {
        if (getContext() == null) return false;
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        boolean restEnabled = prefs.getBoolean("rest_interval_enabled", false);
        if (!restEnabled) return false;

        String start = prefs.getString("rest_start_time", "12:00");
        String end = prefs.getString("rest_end_time", "13:00");
        return isCurrentTimeInInterval(start, end);
    }

    private void checkRestInterval() {
        if (isRestIntervalNow()) {
            if (cardRestWarning != null) cardRestWarning.setVisibility(View.VISIBLE);
            if (locationOverlay != null) {
                // 🔥 DESLIGAMENTO TOTAL DO SENSOR PARA ECONOMIA EXTREMA
                locationOverlay.disableMyLocation();
            }
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

    private boolean isCurrentTimeInInterval(String start, String end) {
        try {
            String[] sParts = start.split(":");
            String[] eParts = end.split(":");
            int sHour = Integer.parseInt(sParts[0]), sMin = Integer.parseInt(sParts[1]);
            int eHour = Integer.parseInt(eParts[0]), eMin = Integer.parseInt(eParts[1]);
            java.util.Calendar now = java.util.Calendar.getInstance();
            int nowHour = now.get(java.util.Calendar.HOUR_OF_DAY);
            int nowMin = now.get(java.util.Calendar.MINUTE);
            int startTotal = sHour * 60 + sMin;
            int endTotal = eHour * 60 + eMin;
            int nowTotal = nowHour * 60 + nowMin;
            if (startTotal < endTotal) return nowTotal >= startTotal && nowTotal < endTotal;
            else return nowTotal >= startTotal || nowTotal < endTotal;
        } catch (Exception e) { return false; }
    }

    private void disableRestAndGoToSettings() {
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("rest_interval_enabled", false).apply();
        checkRestInterval();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openGeneralSettings();
        }
    }

    private boolean checkPermissions() {
        return getContext() != null && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void promptDownloadArea() {
        if (map == null) return;
        BoundingBox box = map.getBoundingBox();
        int zoomMin = (int) map.getZoomLevelDouble();
        int zoomMax = Math.min(zoomMin + 1, 18); 
        new AlertDialog.Builder(getContext())
                .setTitle("Baixar Mapa Offline")
                .setMessage("Baixar área visível?")
                .setPositiveButton("Baixar", (d, w) -> startDownload(box, zoomMin, zoomMax))
                .setNegativeButton("Não", null).show();
    }

    private void startDownload(BoundingBox box, int zoomMin, int zoomMax) {
        try {
            CacheManager cm = new CacheManager(map);
            cm.downloadAreaAsync(getContext(), box, zoomMin, zoomMax, new CacheManager.CacheManagerCallback() {
                @Override public void onTaskComplete() {
                    if (getActivity() != null) getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Mapa pronto!", Toast.LENGTH_SHORT).show());
                }
                @Override public void onTaskFailed(int errors) {
                    if (getActivity() != null) getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Download finalizado.", Toast.LENGTH_SHORT).show());
                }
                @Override public void updateProgress(int p, int c, int min, int max) {}
                @Override public void downloadStarted() {
                    if (getActivity() != null) getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Baixando...", Toast.LENGTH_SHORT).show());
                }
                @Override public void setPossibleTilesInArea(int total) {}
            });
        } catch (Exception e) {
            Toast.makeText(getContext(), "Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    private void updateDeliveryAppFab() {
        if (fabDeliveryApp == null) return;
        fabDeliveryApp.setVisibility(View.GONE);
    }

    private void launchDeliveryApp() {
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
        }

        String pkg = prefs.getString("delivery_app_package", "").trim();
        
        if (!pkg.isEmpty()) {
            // Inicia o ícone flutuante
            Intent serviceIntent = new Intent(requireContext(), FloatingIconService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireContext().startForegroundService(serviceIntent);
            } else {
                requireContext().startService(serviceIntent);
            }
            openApp(pkg);
        } else {
            // 🔥 NOVO: Popup explicativo moderno se o app não estiver definido
            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_delivery_app_shortcut, null);
            android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            dialogView.findViewById(R.id.btnShortcutCancel).setOnClickListener(v -> dialog.dismiss());
            dialogView.findViewById(R.id.btnShortcutConfigure).setOnClickListener(v -> {
                dialog.dismiss();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openFragmentInSettings(
                            SettingsParentFragment.newInstance(1), // Aba Mapa
                            "Ajustes do Mapa"
                    );
                }
            });

            dialog.show();
        }
    }

    private void openApp(String pkg) {
        try {
            PackageManager pm = requireContext().getPackageManager();
            Intent intent = pm.getLaunchIntentForPackage(pkg);
            
            if (intent != null) {
                startActivity(intent);
                return;
            }
            
            // Busca profunda alternativa
            intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setPackage(pkg);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            android.content.pm.ResolveInfo resolveInfo = pm.queryIntentActivities(intent, 0).stream().findFirst().orElse(null);
            if (resolveInfo != null) {
                intent.setClassName(pkg, resolveInfo.activityInfo.name);
                startActivity(intent);
                return;
            }

            Toast.makeText(getContext(), "App não encontrado ou não pode ser aberto", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Erro ao abrir app", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadSavedRoute(int kmId) {
        new Thread(() -> {
            java.util.List<RoutePoint> savedPoints = AppDatabase.getInstance(getContext()).appDao().getRoutePointsForKm(kmId);
            DailyKm km = AppDatabase.getInstance(getContext()).appDao().getAllDailyKm().stream().filter(k -> k.id == kmId).findFirst().orElse(null);

            if (savedPoints != null && !savedPoints.isEmpty()) {
                currentHistoricalPoints = savedPoints;
                java.util.List<GeoPoint> geoPoints = new java.util.ArrayList<>();
                for (RoutePoint rp : savedPoints) {
                    geoPoints.add(new GeoPoint(rp.latitude, rp.longitude));
                }
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Intent intent = new Intent(getContext(), TrackingService.class);
                        intent.setAction("STOP");
                        requireContext().startService(intent);
                        
                        TrackingService.pathPoints.postValue(new java.util.ArrayList<>(savedPoints));
                        
                        if (polyline != null) {
                            polyline.setPoints(new java.util.ArrayList<>(geoPoints));
                        }

                        map.getOverlays().removeIf(overlay -> overlay instanceof Marker);
                        showHomeMarker();
                        showLoadingMarkers();

                        detectAndMarkStops(geoPoints, true);

                        Marker startMarker = new Marker(map);
                        startMarker.setPosition(geoPoints.get(0));
                        startMarker.setTitle("Início do Trajeto");
                        startMarker.setIcon(ContextCompat.getDrawable(getContext(), android.R.drawable.ic_menu_mylocation));
                        startMarker.getIcon().setTint(Color.GREEN);
                        map.getOverlays().add(startMarker);

                        Marker endMarker = new Marker(map);
                        endMarker.setPosition(geoPoints.get(geoPoints.size() - 1));
                        endMarker.setTitle("Fim do Trajeto");
                        endMarker.setIcon(ContextCompat.getDrawable(getContext(), android.R.drawable.ic_menu_recent_history));
                        endMarker.getIcon().setTint(Color.RED);
                        map.getOverlays().add(endMarker);
                        
                        timelineMarker = new Marker(map);
                        timelineMarker.setTitle("Posição no Horário");
                        timelineMarker.setIcon(ContextCompat.getDrawable(getContext(), R.drawable.ic_tire));
                        timelineMarker.getIcon().setTint(Color.parseColor("#FF9800"));
                        timelineMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        map.getOverlays().add(timelineMarker);

                        // Oculta UI normal
                        if (fabTracking != null) fabTracking.setVisibility(View.GONE);
                        if (fabStop != null) fabStop.setVisibility(View.GONE);
                        if (fabDownload != null) fabDownload.setVisibility(View.GONE);
                        if (fabCenter != null) fabCenter.setVisibility(View.GONE);
                        if (fabHome != null) fabHome.setVisibility(View.GONE);
                        if (fabLoadingPoints != null) fabLoadingPoints.setVisibility(View.GONE);
                        if (fabDeliveryApp != null) fabDeliveryApp.setVisibility(View.GONE);
                        if (textStatus != null) textStatus.setVisibility(View.GONE);

                        cardTimeline.setVisibility(View.VISIBLE);
                        seekBarTimeline.setMax(geoPoints.size() - 1);
                        seekBarTimeline.setProgress(0);
                        updateTimelineMarker(0);
                        
                        pauseTimelinePlayback(); 
                        setTimelineSpeed(1);

                        map.invalidate();
                        
                        BoundingBox bbox = BoundingBox.fromGeoPoints(geoPoints);
                        map.zoomToBoundingBox(bbox, true, 100);
                        
                        if (km != null && km.gpsDistance > 0) {
                            Toast.makeText(getContext(), String.format(Locale.getDefault(), "Rota: %.1f KM (via Maps)", km.gpsDistance), Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getContext(), "Rota histórica carregada!", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } else {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        cardTimeline.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Nenhuma rota salva para este registro.", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
    }

    private void detectAndMarkStops(List<GeoPoint> points, boolean useHistoricalTimestamps) {
        if (points.size() < 2 || currentHistoricalPoints.isEmpty()) return;

        SharedPreferences prefs = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        
        // Novos parâmetros configuráveis
        int shortTimeMaxMs = prefs.getInt("tracking_short_stop_time", 60) * 1000;
        int shortRadius = prefs.getInt("tracking_short_stop_radius", 20);
        
        int mediumTimeMaxMs = prefs.getInt("tracking_medium_stop_time", 240) * 1000;
        int mediumRadius = prefs.getInt("tracking_medium_stop_radius", 40);
        
        int longRadius = prefs.getInt("tracking_long_stop_radius", 80);
        
        String colorShort = prefs.getString("tracking_color_short", "#4CAF50");
        String colorMedium = prefs.getString("tracking_color_medium", "#FBC02D");
        String colorLong = prefs.getString("tracking_color_long", "#F44336");

        // Limpa marcas de parada anteriores
        map.getOverlays().removeIf(overlay -> {
            if (overlay instanceof Marker) {
                Marker m = (Marker) overlay;
                return m.getTitle() != null && m.getTitle().startsWith("Parada:");
            }
            return false;
        });

        int i = 0;
        while (i < points.size()) {
            int j = i + 1;
            long startTime = currentHistoricalPoints.get(i).timestamp;
            
            // Determina o raio atual baseado no tempo (padrão inicial curto)
            int currentRadius = shortRadius; 

            while (j < points.size()) {
                double dist = points.get(i).distanceToAsDouble(points.get(j));
                
                // Cálculo dinâmico do raio baseado na duração acumulada
                long durationSoFar = currentHistoricalPoints.get(j).timestamp - startTime;
                if (durationSoFar >= mediumTimeMaxMs) currentRadius = longRadius;
                else if (durationSoFar >= shortTimeMaxMs) currentRadius = mediumRadius;
                else currentRadius = shortRadius;

                if (dist > currentRadius) break; 
                j++;
            }

            long durationMs = currentHistoricalPoints.get(Math.min(j - 1, points.size() - 1)).timestamp - startTime;
            
            // Tempo mínimo configurável para evitar ruído de GPS
            int minStopDurationMs = prefs.getInt("min_stop_duration_seconds", 15) * 1000;
            if (durationMs >= minStopDurationMs) {
                int color;
                String timeStr = formatDuration(durationMs);
                
                if (durationMs >= mediumTimeMaxMs) { 
                    color = Color.parseColor(colorLong);
                } else if (durationMs >= shortTimeMaxMs) { 
                    color = Color.parseColor(colorMedium);
                } else { 
                    color = Color.parseColor(colorShort);
                }
                
                addStopMarker(points.get(i), "Parada: " + timeStr + " (Início: " + timeFormat.format(new java.util.Date(startTime)) + ")", color);
                i = j;
            } else {
                i++;
            }
        }
    }

    private String formatDuration(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }

    private void addStopMarker(GeoPoint point, String title, int color) {
        Marker stopMarker = new Marker(map);
        stopMarker.setPosition(point);
        stopMarker.setTitle(title);
        stopMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        
        android.graphics.drawable.ShapeDrawable dot = new android.graphics.drawable.ShapeDrawable(new android.graphics.drawable.shapes.OvalShape());
        dot.setIntrinsicWidth(20);
        dot.setIntrinsicHeight(20);
        dot.getPaint().setColor(color);
        // Adiciona borda preta para melhor visibilidade em qualquer mapa
        dot.getPaint().setStyle(android.graphics.Paint.Style.FILL_AND_STROKE);
        stopMarker.setIcon(dot);
        
        map.getOverlays().add(stopMarker);
    }

    private void exitHistoryMode() {
        pauseTimelinePlayback();
        if (cardTimeline != null) cardTimeline.setVisibility(View.GONE);
        currentHistoricalPoints.clear();
        timelineMarker = null;
        
        // Restaura UI normal
        refreshRemoteVisibility(); // Cuida dos botões de rastreio
        if (fabDownload != null) fabDownload.setVisibility(View.VISIBLE);
        if (fabCenter != null) fabCenter.setVisibility(View.VISIBLE);
        if (fabHome != null) fabHome.setVisibility(View.VISIBLE);
        if (fabLoadingPoints != null) fabLoadingPoints.setVisibility(View.VISIBLE);
        if (textStatus != null) textStatus.setVisibility(View.VISIBLE);
        updateDeliveryAppFab();

        if (map != null) {
            map.getOverlays().removeIf(overlay -> overlay instanceof Marker || overlay instanceof Polyline);
            showHomeMarker();
            showLoadingMarkers();
            map.invalidate();
            centerOnCurrentLocation();
        }
    }

    private void setupTimelineListener() {
        seekBarTimeline.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    updateTimelineMarker(progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {
                pauseTimelinePlayback();
            }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void toggleTimelinePlayback() {
        if (isTimelinePlaying) {
            pauseTimelinePlayback();
        } else {
            startTimelinePlayback();
        }
    }

    private void startTimelinePlayback() {
        if (seekBarTimeline.getProgress() >= seekBarTimeline.getMax()) {
            seekBarTimeline.setProgress(0);
        }
        isTimelinePlaying = true;
        btnTimelinePlayPause.setImageResource(R.drawable.ic_pause);
        playbackHandler.post(playbackRunnable);
    }

    private void pauseTimelinePlayback() {
        isTimelinePlaying = false;
        if (btnTimelinePlayPause != null) {
            btnTimelinePlayPause.setImageResource(R.drawable.ic_play);
        }
        playbackHandler.removeCallbacks(playbackRunnable);
    }

    private void setTimelineSpeed(int multiplier) {
        timelineSpeedMultiplier = multiplier;
        
        if (getContext() == null) return;
        
        TypedValue typedValue = new TypedValue();
        getContext().getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
        int colorPrimary = typedValue.data;
        int colorGray = Color.GRAY;

        btnS1.setTextColor(multiplier == 1 ? colorPrimary : colorGray);
        btnS2.setTextColor(multiplier == 2 ? colorPrimary : colorGray);
        btnS4.setTextColor(multiplier == 4 ? colorPrimary : colorGray);
        btnS8.setTextColor(multiplier == 8 ? colorPrimary : colorGray);
    }

    private void startHomeSelection() {
        if (homeSelectionOverlay != null) {
            cancelHomeSelection();
            return;
        }

        SharedPreferences prefs = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        float lat = prefs.getFloat("home_lat", 0);
        float lon = prefs.getFloat("home_lon", 0);

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
                        CloudSyncHelper.syncNow(requireContext());
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
        fabHome.setImageResource(R.drawable.ic_close);
        fabHome.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.RED));

        homeSelectionOverlay = new MapEventsOverlay(new MapEventsReceiver() {
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
        fabHome.setImageResource(R.drawable.ic_home);
        fabHome.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
        map.invalidate();
    }

    private void confirmHomeLocation(GeoPoint p) {
        new AlertDialog.Builder(getContext())
                .setTitle("Definir Casa")
                .setMessage("Deseja definir este local como sua casa para início automático do rastreamento?")
                .setPositiveButton("Sim", (dialog, which) -> {
                    saveHomeLocation(p);
                    cancelHomeSelection();
                })
                .setNegativeButton("Não", (dialog, which) -> cancelHomeSelection())
                .show();
    }

    private void saveHomeLocation(GeoPoint p) {
        SharedPreferences prefs = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        prefs.edit()
                .putFloat("home_lat", (float) p.getLatitude())
                .putFloat("home_lon", (float) p.getLongitude())
                .putBoolean("home_tracking_enabled", true)
                .apply();
        
        showHomeMarker();
        Toast.makeText(getContext(), "Casa definida! Rastreamento iniciará ao sair daqui.", Toast.LENGTH_LONG).show();

        // Trigger auto cloud sync
        CloudSyncHelper.syncNow(requireContext());
        TrackingHelper.updateAutoTracking(requireContext());
    }

    private void showHomeMarker() {
        SharedPreferences prefs = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        float lat = prefs.getFloat("home_lat", 0);
        float lon = prefs.getFloat("home_lon", 0);

        if (lat != 0 && lon != 0) {
            GeoPoint homePoint = new GeoPoint(lat, lon);

            // Marker
            if (homeMarker == null) {
                homeMarker = new Marker(map);
                homeMarker.setTitle("Minha Casa");
                homeMarker.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_home));
                homeMarker.getIcon().setTint(Color.parseColor("#4CAF50"));
                homeMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            }
            homeMarker.setPosition(homePoint);
            if (!map.getOverlays().contains(homeMarker)) {
                map.getOverlays().add(homeMarker);
            }

            // Radius visualization
            if (homeRadiusOverlay != null) {
                map.getOverlays().remove(homeRadiusOverlay);
            }

            int triggerRadius = 100;
            try {
                triggerRadius = prefs.getInt("home_trigger_radius", 100);
            } catch (ClassCastException e) {
                Object val = prefs.getAll().get("home_trigger_radius");
                if (val != null) {
                    try { triggerRadius = Integer.parseInt(String.valueOf(val)); } catch (Exception ignored) {}
                }
                prefs.edit().putInt("home_trigger_radius", triggerRadius).apply();
            }
            
            homeRadiusOverlay = new Polygon(map);
            homeRadiusOverlay.setPoints(Polygon.pointsAsCircle(homePoint, triggerRadius));
            homeRadiusOverlay.getFillPaint().setColor(Color.parseColor("#334CAF50")); // Verde semi-transparente
            homeRadiusOverlay.getOutlinePaint().setColor(Color.parseColor("#4CAF50"));
            homeRadiusOverlay.getOutlinePaint().setStrokeWidth(2f);
            
            // Adiciona o raio antes do marcador para que o marcador fique por cima
            map.getOverlays().add(0, homeRadiusOverlay);

            map.invalidate();
        }
    }

    private void loadLoadingPoints() {
        loadingPoints = AppDatabase.getInstance(getContext()).appDao().getAllLoadingPoints();
    }

    private void saveLoadingPoints() {
        showLoadingMarkers();
    }

    private void showLoadingMarkers() {
        if (map == null || getContext() == null) return;
        
        // 🔥 REGRA REMOTA: Verifica se os controles de carregamento/rastreamento estão visíveis
        if (getActivity() instanceof MainActivity && !((MainActivity) getActivity()).isMenuVisible("km")) {
            for (Marker m : loadingMarkers) map.getOverlays().remove(m);
            loadingMarkers.clear();
            map.getOverlays().removeIf(overlay -> overlay instanceof Polygon && ((Polygon)overlay).getTitle() != null && ((Polygon)overlay).getTitle().startsWith("LoadingRadius:"));
            map.invalidate();
            return;
        }

        loadLoadingPoints();
        for (Marker m : loadingMarkers) {
            map.getOverlays().remove(m);
        }
        loadingMarkers.clear();

        // Limpa raios de carregamento antigos
        map.getOverlays().removeIf(overlay -> {
            if (overlay instanceof Polygon) {
                Polygon p = (Polygon) overlay;
                return p.getTitle() != null && p.getTitle().startsWith("Radius:");
            }
            return false;
        });

        SharedPreferences prefs = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        int loadingRadius = 100;
        try {
            loadingRadius = prefs.getInt("loading_base_radius", 100);
        } catch (Exception e) {
            Object val = prefs.getAll().get("loading_base_radius");
            if (val != null) {
                try { loadingRadius = Integer.parseInt(String.valueOf(val)); } catch (Exception ignored) {}
            }
            prefs.edit().putInt("loading_base_radius", loadingRadius).apply();
        }

        for (LoadingPoint lp : loadingPoints) {
            GeoPoint point = new GeoPoint(lp.latitude, lp.longitude);
            
            // Desenha raio configurável para carregamento
            Polygon circle = new Polygon(map);
            circle.setPoints(Polygon.pointsAsCircle(point, loadingRadius));
            circle.getFillPaint().setColor(Color.parseColor("#33FF9800")); // Laranja semi-transparente
            circle.getOutlinePaint().setColor(Color.parseColor("#FF9800"));
            circle.getOutlinePaint().setStrokeWidth(2f);
            circle.setTitle("Radius:" + lp.id);
            map.getOverlays().add(0, circle);

            Marker m = new Marker(map);
            m.setPosition(point);
            m.setTitle(lp.name + " (" + (lp.platformName != null ? lp.platformName : "Sem Plataforma") + ")");
            m.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_money));
            m.getIcon().setTint(Color.parseColor("#FF9800"));
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
                            AppDatabase.getInstance(getContext()).appDao().deleteLoadingPoint(lp);
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    saveLoadingPoints();
                                    showLoadingMarkers();
                                    Toast.makeText(getContext(), "Ponto removido", Toast.LENGTH_SHORT).show();
                                    CloudSyncHelper.syncNow(requireContext());
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
        fabLoadingPoints.setImageResource(R.drawable.ic_close);
        fabLoadingPoints.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.RED));

        loadingSelectionOverlay = new MapEventsOverlay(new MapEventsReceiver() {
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
        fabLoadingPoints.setImageResource(R.drawable.ic_money);
        fabLoadingPoints.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
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
                AppDatabase.getInstance(getContext()).appDao().updateLoadingPoint(lp);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        saveLoadingPoints();
                        showLoadingMarkers();
                        Toast.makeText(getContext(), "Localização atualizada", Toast.LENGTH_SHORT).show();
                        CloudSyncHelper.syncNow(requireContext());
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
        List<Platform> platforms = AppDatabase.getInstance(getContext()).appDao().getAllPlatforms();
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
                            AppDatabase.getInstance(getContext()).appDao().insertLoadingPoint(lp);
                        } else {
                            LoadingPoint lp = loadingPoints.get(index);
                            lp.name = name;
                            lp.platformName = platformName;
                            AppDatabase.getInstance(getContext()).appDao().updateLoadingPoint(lp);
                        }
                        
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                saveLoadingPoints();
                                showLoadingMarkers();
                                Toast.makeText(getContext(), "Ponto salvo com sucesso!", Toast.LENGTH_SHORT).show();
                                CloudSyncHelper.syncNow(requireContext());
                            });
                        }
                    }).start();
                })
                .setNegativeButton("Cancelar", null)
                .create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog_rounded);
        dialog.show();
    }

    private void updateTimelineMarker(int index) {
        if (currentHistoricalPoints == null || index >= currentHistoricalPoints.size() || timelineMarker == null) return;
        
        RoutePoint point = currentHistoricalPoints.get(index);
        GeoPoint geoPoint = new GeoPoint(point.latitude, point.longitude);
        
        timelineMarker.setPosition(geoPoint);
        textTimelineTime.setText(timeFormat.format(new java.util.Date(point.timestamp)));
        
        mapController.animateTo(geoPoint);
        map.invalidate();
    }

    private void startComboioListener() {
        if (getActivity() == null) return;
        
        SharedPreferences prefs = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        // 🔥 BLOQUEIO TOTAL: Modo Comboio (visualização) apenas para DEVs
        FirebaseHelper.checkDeveloperAccess(user.getEmail(), isDev -> {
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (!isDev) {
                    if (comboioListener != null) comboioListener.remove();
                    return;
                }

                // Se for DEV, segue a lógica de visualização configurada
                int visibilityMode = prefs.getInt("comboio_visibility_mode", 2);
                if (visibilityMode == 0) {
                    if (comboioListener != null) comboioListener.remove();
                    return;
                }

                if (comboioListener != null) comboioListener.remove();

                comboioListener = FirebaseHelper.listenFriendsLocations(user.getEmail(), locations -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> updateFriendMarkers(locations));
                    }
                });
            });
        });
    }

    private void updateFriendMarkers(List<FirebaseHelper.FriendLocation> locations) {
        if (map == null || getContext() == null) return;
        
        List<String> activeEmails = new ArrayList<>();
        int iconSize = (int) (40 * getResources().getDisplayMetrics().density); // Tamanho similar à seta do GPS

        for (FirebaseHelper.FriendLocation fl : locations) {
            activeEmails.add(fl.email);
            Marker m = friendMarkers.get(fl.email);
            if (m == null) {
                m = new Marker(map);
                m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                map.getOverlays().add(m);
                friendMarkers.put(fl.email, m);
            }
            
            m.setPosition(new GeoPoint(fl.lat, fl.lon));
            m.setTitle(fl.name + " (@" + fl.username + ")");
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new java.util.Date(fl.timestamp));
            m.setSnippet("Visto as " + time);

            // 🔥 Carrega a foto como ícone circular
            if (fl.avatar != null && !fl.avatar.isEmpty()) {
                try {
                    byte[] decoded = android.util.Base64.decode(fl.avatar, android.util.Base64.DEFAULT);
                    Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                    if (bitmap != null) {
                        m.setIcon(new BitmapDrawable(getResources(), getCircularBitmapWithBorder(bitmap, iconSize)));
                    }
                } catch (Exception e) {
                    setDefaultFriendIcon(m);
                }
            } else {
                setDefaultFriendIcon(m);
            }
        }
        
        // Remove offline
        java.util.Iterator<Map.Entry<String, Marker>> it = friendMarkers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Marker> entry = it.next();
            if (!activeEmails.contains(entry.getKey())) {
                map.getOverlays().remove(entry.getValue());
                it.remove();
            }
        }
        map.invalidate();
    }

    private void setDefaultFriendIcon(Marker m) {
        Drawable d = ContextCompat.getDrawable(requireContext(), R.drawable.ic_car_marker);
        if (d != null) {
            d.setTint(Color.parseColor("#4CAF50"));
            m.setIcon(d);
        }
    }

    private Bitmap getCircularBitmapWithBorder(Bitmap bitmap, int size) {
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        
        float radius = size / 2f;
        
        // Desenha borda branca/azul para destaque
        paint.setColor(Color.WHITE);
        canvas.drawCircle(radius, radius, radius, paint);
        paint.setColor(Color.parseColor("#2196F3"));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        canvas.drawCircle(radius, radius, radius - 2, paint);
        
        // Recorta a foto em círculo
        paint.setStyle(Paint.Style.FILL);
        android.graphics.BitmapShader shader = new android.graphics.BitmapShader(
                Bitmap.createScaledBitmap(bitmap, size, size, false),
                android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP);
        paint.setShader(shader);
        canvas.drawCircle(radius, radius, radius - 6, paint);
        
        return output;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sharedPreferences != null) {
            sharedPreferences.registerOnSharedPreferenceChangeListener(prefListener);
        }
        if (map != null) {
            map.onResume();
            
            showHomeMarker();
            showLoadingMarkers();
            refreshRemoteVisibility();
            updateDeliveryAppFab();
            checkRestInterval();
            startComboioListener(); // 🔥 MODO COMBOIO
            
            if (getActivity() instanceof MainActivity) {
                int routeId = ((MainActivity) getActivity()).consumeRequestedRouteKmId();
                if (routeId != -1) {
                    loadSavedRoute(routeId);
                    return; 
                }
            }
            // Força a centralização na localização atual ao abrir o rastreamento
            new Handler(Looper.getMainLooper()).postDelayed(this::centerOnCurrentLocation, 500);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sharedPreferences != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(prefListener);
        }
        if (comboioListener != null) comboioListener.remove();
        pauseTimelinePlayback();
        if (locationOverlay != null) locationOverlay.disableMyLocation();
        if (map != null) map.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        pauseTimelinePlayback();
        if (map != null) map.onDetach();
        map = null;
    }
}
