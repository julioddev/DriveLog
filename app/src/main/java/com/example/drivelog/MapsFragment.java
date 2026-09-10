package com.example.drivelog;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.firebase.firestore.ListenerRegistration;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class MapsFragment extends Fragment {

    private MapView map;
    private IMapController mapController;
    private MyLocationNewOverlay locationOverlay;
    private Polyline polyline;
    private FloatingActionButton fabTracking, fabStop, fabDownload, fabCenter, fabHome, fabLoadingPoints, fabDeliveryApp;
    private TextView textStatus;
    private View cardRestWarning, cardTimeline;
    private SeekBar seekBarTimeline;
    private TextView textTimelineTime, btnS1, btnS2, btnS4, btnS8;
    private View btnExitHistory;
    private ImageView btnTimelinePlayPause;

    private Marker userDirectionMarker;
    private android.hardware.SensorManager sensorManager;
    private android.hardware.Sensor rotationVectorSensor;
    private float currentAzimuth = 0;
    private long lastGpsMoveTime = 0;
    private static final long GPS_COOLDOWN_MS = 3000;
    private GeoPoint currentLocation = new GeoPoint(0.0, 0.0);

    private final android.hardware.SensorEventListener compassListener = new android.hardware.SensorEventListener() {
        @Override
        public void onSensorChanged(android.hardware.SensorEvent event) {
            if (event.sensor.getType() == android.hardware.Sensor.TYPE_ROTATION_VECTOR) {
                float[] rotationMatrix = new float[9];
                android.hardware.SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
                
                // --- Lógica Estável: Remapeamento baseado na rotação da tela ---
                int worldX = android.hardware.SensorManager.AXIS_X;
                int worldY = android.hardware.SensorManager.AXIS_Y;

                if (getActivity() != null) {
                    int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
                    if (rotation == android.view.Surface.ROTATION_90) {
                        worldX = android.hardware.SensorManager.AXIS_Y;
                        worldY = android.hardware.SensorManager.AXIS_MINUS_X;
                    } else if (rotation == android.view.Surface.ROTATION_180) {
                        worldX = android.hardware.SensorManager.AXIS_MINUS_X;
                        worldY = android.hardware.SensorManager.AXIS_MINUS_Y;
                    } else if (rotation == android.view.Surface.ROTATION_270) {
                        worldX = android.hardware.SensorManager.AXIS_MINUS_Y;
                        worldY = android.hardware.SensorManager.AXIS_X;
                    }
                }

                float[] remappedMatrix = new float[9];
                android.hardware.SensorManager.remapCoordinateSystem(rotationMatrix, worldX, worldY, remappedMatrix);
                
                float[] orientation = new float[3];
                android.hardware.SensorManager.getOrientation(remappedMatrix, orientation);
                
                float azimuthDegrees = (float) Math.toDegrees(orientation[0]);
                if (azimuthDegrees < 0) azimuthDegrees += 360;

                // 🔥 NOVO: Calibração separada para o MapsFragment (sempre usa modo Fixo)
                float offset = sharedPreferences.getFloat("compass_offset_fixed", 0f);
                boolean inverted = sharedPreferences.getBoolean("compass_inverted_fixed", false);
                if (inverted) azimuthDegrees = (360 - azimuthDegrees) % 360;
                azimuthDegrees = (azimuthDegrees + offset + 360) % 360;

                float alpha = 0.2f;
                float diff = azimuthDegrees - currentAzimuth;
                if (diff > 180) diff -= 360; else if (diff < -180) diff += 360;
                currentAzimuth = currentAzimuth + alpha * diff;

                if (System.currentTimeMillis() - lastGpsMoveTime > GPS_COOLDOWN_MS) {
                    if (userDirectionMarker != null) {
                        boolean hideBoneco = sharedPreferences.getBoolean("hide_marker_when_stationary", false);
                        userDirectionMarker.setVisible(true);

                        // Se estivermos parados e a opção de ocultar boneco estiver ativa, garantimos que o ícone seja a seta
                        int targetRes = hideBoneco ? R.drawable.ic_original_arrow : R.drawable.ic_user_stationary;
                        
                        Object lastIconRes = userDirectionMarker.getRelatedObject();
                        if (!(lastIconRes instanceof Integer) || (Integer) lastIconRes != targetRes) {
                            Bitmap bmp = drawableToBitmap(targetRes, false);
                            userDirectionMarker.setIcon(new BitmapDrawable(getResources(), bmp));
                            userDirectionMarker.setRelatedObject(targetRes);
                        }

                        // No MapsFragment, como o mapa é sempre norte fixo, o boneco gira com a bússola
                        userDirectionMarker.setRotation(currentAzimuth);
                        if (map != null) map.invalidate();
                    }
                }
            }
        }
        @Override public void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) {}
    };

    private SharedPreferences sharedPreferences;
    private OnlineTileSourceBase currentMapSource;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean isFollowingUser = true;
    private List<LoadingPoint> loadingPoints = new ArrayList<>();
    private Map<Integer, Marker> loadingMarkers = new HashMap<>();
    private ListenerRegistration comboioListener;
    private Map<String, Marker> friendMarkers = new HashMap<>();

    private final SharedPreferences.OnSharedPreferenceChangeListener prefListener = (prefs, key) -> {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            if ("user_map_icon".equals(key)) setupLocationOverlay();
            else if ("map_tile_style".equals(key)) applyMapStyle();
            else if ("show_fab_km_tracking".equals(key)) refreshRemoteVisibility();
        });
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_maps, container, false);

        map = view.findViewById(R.id.map);
        textStatus = view.findViewById(R.id.textTrackingStatus);
        fabTracking = view.findViewById(R.id.fabTracking);
        fabStop = view.findViewById(R.id.fabStop);
        fabDownload = view.findViewById(R.id.fabDownload);
        fabCenter = view.findViewById(R.id.fabCenter);
        fabHome = view.findViewById(R.id.fabHome);
        fabLoadingPoints = view.findViewById(R.id.fabLoadingPoints);
        fabDeliveryApp = view.findViewById(R.id.fabDeliveryApp);
        cardRestWarning = view.findViewById(R.id.cardRestWarning);

        cardTimeline = view.findViewById(R.id.cardTimeline);
        seekBarTimeline = view.findViewById(R.id.seekBarTimeline);
        textTimelineTime = view.findViewById(R.id.textTimelineTime);
        btnExitHistory = view.findViewById(R.id.btnExitHistory);
        btnTimelinePlayPause = view.findViewById(R.id.btnTimelinePlayPause);
        btnS1 = view.findViewById(R.id.btnSpeed1x);
        btnS2 = view.findViewById(R.id.btnSpeed2x);
        btnS4 = view.findViewById(R.id.btnSpeed4x);
        btnS8 = view.findViewById(R.id.btnSpeed8x);

        sharedPreferences = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        sensorManager = (android.hardware.SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            rotationVectorSensor = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ROTATION_VECTOR);
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        applyMapStyle();
        map.setMultiTouchControls(true);
        mapController = map.getController();
        
        // 🔥 RESTAURAR ÚLTIMA POSIÇÃO
        float lastLat = sharedPreferences.getFloat("last_map_lat", 0f);
        float lastLon = sharedPreferences.getFloat("last_map_lon", 0f);
        float lastZoom = sharedPreferences.getFloat("last_map_zoom", 17.0f);
        if (lastLat != 0 && lastLon != 0) {
            mapController.setZoom((double) lastZoom);
            mapController.setCenter(new GeoPoint(lastLat, lastLon));
        } else {
            mapController.setZoom(17.0);
        }

        map.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == android.view.MotionEvent.ACTION_DOWN || action == android.view.MotionEvent.ACTION_POINTER_DOWN) {
                if (isFollowingUser) {
                    isFollowingUser = false;
                }
            }
            return false;
        });

        setupPolyline();
        setupLocationOverlay();

        fabTracking.setOnClickListener(v -> showKmTrackingPopup());
        fabStop.setOnClickListener(v -> handleStop());
        fabDownload.setOnClickListener(v -> promptDownloadArea());
        fabCenter.setOnClickListener(v -> centerOnCurrentLocation());
        fabHome.setOnClickListener(v -> startHomeSelection());
        fabLoadingPoints.setOnClickListener(v -> showLoadingPointsDialog());
        fabDeliveryApp.setOnClickListener(v -> launchDeliveryApp());
        btnExitHistory.setOnClickListener(v -> exitHistoryMode());

        TrackingService.pathPoints.observe(getViewLifecycleOwner(), points -> {
            if (polyline != null && points != null) {
                List<GeoPoint> geoPoints = new ArrayList<>();
                for (RoutePoint p : points) geoPoints.add(new GeoPoint(p.latitude, p.longitude));
                polyline.setPoints(geoPoints);
                if (!geoPoints.isEmpty()) {
                    currentLocation = geoPoints.get(geoPoints.size() - 1);
                    if (userDirectionMarker != null) userDirectionMarker.setPosition(currentLocation);
                    if (isFollowingUser) mapController.animateTo(currentLocation);
                }
                map.invalidate();
            }
        });

        loadLoadingPoints();
        return view;
    }

    private void applyMapStyle() {
        if (map == null) return;
        int style = sharedPreferences.getInt("map_tile_style", 0);
        switch (style) {
            case 1: currentMapSource = new XYTileSource("Esri_Satellite", 0, 18, 256, ".jpg", new String[] {"https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"}); break;
            case 4: currentMapSource = new XYTileSource("Google_Hybrid", 0, 19, 256, ".png", new String[] {"https://mt1.google.com/vt/lyrs=y&x="}) {
                @Override public String getTileURLString(long pMapTileIndex) { return getBaseUrl() + MapTileIndex.getX(pMapTileIndex) + "&y=" + MapTileIndex.getY(pMapTileIndex) + "&z=" + MapTileIndex.getZoom(pMapTileIndex); }
            }; break;
            default: currentMapSource = new XYTileSource("OSM", 0, 19, 256, ".png", new String[] {"https://a.tile.openstreetmap.org/"}); break;
        }
        map.setTileSource(currentMapSource);
    }

    private void setupPolyline() {
        if (polyline != null) map.getOverlays().remove(polyline);
        String colorHex = sharedPreferences.getString("tracking_route_line_color", "#2196F3");
        polyline = new Polyline(map);
        polyline.getOutlinePaint().setColor(Color.parseColor(colorHex));
        polyline.getOutlinePaint().setStrokeWidth(12f);
        map.getOverlays().add(polyline);
    }

    private void setupLocationOverlay() {
        if (map == null || getContext() == null) return;
        
        // 🔥 RESTAURAÇÃO TOTAL VERSÃO 1.3 (Ícones Extraídos do APK USB)
        if (locationOverlay != null) map.getOverlays().remove(locationOverlay);
        if (userDirectionMarker != null) map.getOverlays().remove(userDirectionMarker);

        GpsMyLocationProvider provider = new GpsMyLocationProvider(requireContext());
        locationOverlay = new MyLocationNewOverlay(provider, map) {
            @Override
            public void onLocationChanged(android.location.Location location, org.osmdroid.views.overlay.mylocation.IMyLocationProvider source) {
                super.onLocationChanged(location, source);
                if (getActivity() != null && location != null) {
                    getActivity().runOnUiThread(() -> {
                        currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                        if (userDirectionMarker != null) {
                            userDirectionMarker.setPosition(currentLocation);
                            
                            float speed = location.getSpeed();
                            boolean hideBoneco = sharedPreferences.getBoolean("hide_marker_when_stationary", false);
                            userDirectionMarker.setVisible(true);

                            int targetRes;
                            if (speed > 1.0f) {
                                targetRes = R.drawable.ic_user_moving;
                            } else {
                                // 🔥 Se a opção de ocultar o boneco estiver ativa, mostra a seta padrão
                                targetRes = hideBoneco ? R.drawable.ic_original_arrow : R.drawable.ic_user_stationary;
                            }
                            
                            if (location.hasBearing() && speed > 1.0f) {
                                lastGpsMoveTime = System.currentTimeMillis();
                                userDirectionMarker.setRotation(location.getBearing());
                            }

                            Object lastIconRes = userDirectionMarker.getRelatedObject();
                            if (!(lastIconRes instanceof Integer) || (Integer) lastIconRes != targetRes) {
                                Bitmap bmp = drawableToBitmap(targetRes, false);
                                userDirectionMarker.setIcon(new BitmapDrawable(getResources(), bmp));
                                userDirectionMarker.setRelatedObject(targetRes);
                            }
                        }
                    });
                }
            }
        };

        locationOverlay.setPersonIcon(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888));
        locationOverlay.setDirectionIcon(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888));
        locationOverlay.enableMyLocation();
        locationOverlay.setDrawAccuracyEnabled(false);
        
        userDirectionMarker = new Marker(map);
        userDirectionMarker.setInfoWindow(null);
        userDirectionMarker.setRelatedObject("USER_DIR");
        userDirectionMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        userDirectionMarker.setFlat(false);
        
        map.getOverlays().add(locationOverlay);
        map.getOverlays().add(userDirectionMarker);
        
        RotationGestureOverlay rotationGestureOverlay = new RotationGestureOverlay(map);
        rotationGestureOverlay.setEnabled(true);
        map.getOverlays().add(rotationGestureOverlay);
    }

    private Bitmap drawableToBitmap(int resId, boolean rotate) {
        Drawable d = ContextCompat.getDrawable(requireContext(), resId);
        if (d == null) return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        int size = (int) (38 * getResources().getDisplayMetrics().density);
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        if (rotate) { c.save(); c.rotate(-90, size/2f, size/2f); }
        d.setBounds(0, 0, size, size);
        d.draw(c);
        if (rotate) c.restore();
        return b;
    }

    public void refreshRemoteVisibility() {
        if (getActivity() instanceof MainActivity && fabTracking != null) {
            boolean visible = ((MainActivity) getActivity()).isMenuVisible("km");
            boolean prefVisible = sharedPreferences.getBoolean("show_fab_km_tracking", true);
            fabTracking.setVisibility((visible && prefVisible) ? View.VISIBLE : View.GONE);
            if (fabLoadingPoints != null) fabLoadingPoints.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void updateTrackingUI() {
        boolean isTracking = Boolean.TRUE.equals(TrackingService.isTracking.getValue());
        if (fabStop != null) fabStop.setVisibility(isTracking ? View.VISIBLE : View.GONE);
        if (textStatus != null) {
            int mode = sharedPreferences.getInt("tracking_mode_v2", 0);
            if (isTracking) {
                textStatus.setText("Rastreamento Ativo");
            } else if (mode == 2) {
                float homeLat = sharedPreferences.getFloat("home_lat", 0);
                float homeLon = sharedPreferences.getFloat("home_lon", 0);
                Float distHome = TrackingService.distanceToHome.getValue();
                int triggerRadius = sharedPreferences.getInt("home_trigger_radius", 100);

                if (homeLat == 0 || homeLon == 0) {
                    textStatus.setText("Defina sua Casa no Mapa");
                } else {
                    float currentDist = (distHome != null && distHome >= 0) ? distHome : -1f;
                    if (currentDist < 0 && locationOverlay != null && locationOverlay.getMyLocation() != null) {
                        GeoPoint myLoc = locationOverlay.getMyLocation();
                        float[] res = new float[1];
                        android.location.Location.distanceBetween(myLoc.getLatitude(), myLoc.getLongitude(), homeLat, homeLon, res);
                        currentDist = res[0];
                    }

                    if (currentDist >= 0) {
                        if (currentDist <= triggerRadius) {
                            textStatus.setText(String.format(Locale.getDefault(), "Dentro do raio da casa (%.0fm / %dm)", currentDist, triggerRadius));
                        } else {
                            textStatus.setText(String.format(Locale.getDefault(), "Fora do raio da casa (%.0fm / %dm)", currentDist, triggerRadius));
                        }
                    } else {
                        textStatus.setText("Buscando sinal GPS...");
                    }
                }
            } else if (mode == 1) {
                textStatus.setText("Aguardando Horário");
            } else {
                textStatus.setText("Rastreamento Inativo");
            }
        }
    }

    private void centerOnCurrentLocation() {
        isFollowingUser = true;
        if (locationOverlay != null && locationOverlay.getMyLocation() != null) {
            mapController.animateTo(locationOverlay.getMyLocation());
        }
    }

    private void showKmTrackingPopup() {
        boolean tracking = Boolean.TRUE.equals(TrackingService.isTracking.getValue());
        boolean paused = Boolean.TRUE.equals(TrackingService.isPaused.getValue());
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View customView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_km_tracking_mini, null);
        builder.setView(customView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        MaterialButton btnPlayPause = customView.findViewById(R.id.btnPlayPauseTracking);
        MaterialButton btnStop = customView.findViewById(R.id.btnStopTracking);
        btnPlayPause.setText(tracking ? (paused ? "Retomar" : "Pausar") : "Iniciar");
        btnStop.setVisibility(tracking ? View.VISIBLE : View.GONE);

        btnPlayPause.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), TrackingService.class);
            intent.setAction(tracking ? (paused ? "START" : "PAUSE") : "START");
            requireContext().startService(intent);
            dialog.dismiss();
        });

        btnStop.setOnClickListener(v1 -> {
            int currentMode = sharedPreferences.getInt("tracking_mode_v2", 0);
            if (currentMode != 0) {
                sharedPreferences.edit()
                        .putInt("tracking_mode_v2", 0)
                        .putBoolean("tracking_auto", false)
                        .putBoolean("home_tracking_enabled", false)
                        .apply();
                TrackingHelper.updateAutoTracking(requireContext());
                Toast.makeText(getContext(), "Trajeto salvo e Modo Automático desativado.", Toast.LENGTH_SHORT).show();
            }
            Intent intent = new Intent(getContext(), TrackingService.class);
            intent.setAction("STOP");
            requireContext().startService(intent);
            dialog.dismiss();
        });

        customView.findViewById(R.id.btnManageHome).setOnClickListener(v -> { dialog.dismiss(); startHomeSelection(); });
        customView.findViewById(R.id.btnManageLoadingPoints).setOnClickListener(v -> { dialog.dismiss(); showLoadingPointsDialog(); });
        customView.findViewById(R.id.btnTrackingHistory).setOnClickListener(v -> { dialog.dismiss(); ((MainActivity)getActivity()).openTrackingHistory(); });
        dialog.show();
    }

    private void loadLoadingPoints() {
        new Thread(() -> {
            loadingPoints = AppDatabase.getInstance(requireContext()).appDao().getAllLoadingPoints();
            if (getActivity() != null) getActivity().runOnUiThread(this::showLoadingMarkers);
        }).start();
    }

    private void showLoadingMarkers() {
        for (Marker m : loadingMarkers.values()) map.getOverlays().remove(m);
        loadingMarkers.clear();
        for (LoadingPoint lp : loadingPoints) {
            Marker m = new Marker(map);
            m.setPosition(new GeoPoint(lp.latitude, lp.longitude));
            m.setTitle(lp.name);
            m.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_money));
            map.getOverlays().add(m);
            loadingMarkers.put(lp.id, m);
        }
        map.invalidate();
    }

    private void startHomeSelection() { Toast.makeText(getContext(), "Toque no mapa para definir CASA", Toast.LENGTH_SHORT).show(); }
    private void showLoadingPointsDialog() { Toast.makeText(getContext(), "Gestão de Pontos", Toast.LENGTH_SHORT).show(); }
    private void launchDeliveryApp() {
        String pkg = sharedPreferences.getString("delivery_app_package", "");
        if (!pkg.isEmpty()) {
            Intent intent = requireContext().getPackageManager().getLaunchIntentForPackage(pkg);
            if (intent != null) startActivity(intent);
        }
    }
    private void exitHistoryMode() { cardTimeline.setVisibility(View.GONE); }

    private boolean isRestIntervalNow() {
        boolean restEnabled = sharedPreferences.getBoolean("rest_interval_enabled", false);
        if (!restEnabled) return false;
        String start = sharedPreferences.getString("rest_start_time", "12:00");
        String end = sharedPreferences.getString("rest_end_time", "13:00");
        try {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            int now = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE);
            String[] s = start.split(":"); String[] e = end.split(":");
            int st = Integer.parseInt(s[0]) * 60 + Integer.parseInt(s[1]);
            int et = Integer.parseInt(e[0]) * 60 + Integer.parseInt(e[1]);
            return now >= st && now <= et;
        } catch (Exception e) { return false; }
    }

    private void handleStop() {
        Intent intent = new Intent(getContext(), TrackingService.class);
        intent.setAction("STOP");
        requireContext().startService(intent);
    }

    private void promptDownloadArea() { Toast.makeText(getContext(), "Download Offline", Toast.LENGTH_SHORT).show(); }

    private void startComboioListener() {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            comboioListener = FirebaseHelper.listenFriendsLocations(user.getEmail(), locs -> {
                if (getActivity() != null) getActivity().runOnUiThread(() -> updateFriendMarkers(locs));
            });
        }
    }

    private void updateFriendMarkers(List<FirebaseHelper.FriendLocation> locs) {
        for (Marker m : friendMarkers.values()) map.getOverlays().remove(m);
        friendMarkers.clear();
        for (FirebaseHelper.FriendLocation fl : locs) {
            Marker m = new Marker(map);
            m.setPosition(new GeoPoint(fl.lat, fl.lon));
            m.setTitle(fl.name);
            map.getOverlays().add(m);
            friendMarkers.put(fl.email, m);
        }
        map.invalidate();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sensorManager != null && rotationVectorSensor != null) {
            sensorManager.registerListener(compassListener, rotationVectorSensor, android.hardware.SensorManager.SENSOR_DELAY_UI);
        }
        if (map != null) map.onResume();
        sharedPreferences.registerOnSharedPreferenceChangeListener(prefListener);
        updateTrackingUI();
        startComboioListener();
    }

    @Override
    public void onPause() {
        super.onPause();
        
        // 🔥 SALVAR ESTADO DO MAPA
        if (map != null) {
            sharedPreferences.edit()
                .putFloat("last_map_lat", (float) map.getMapCenter().getLatitude())
                .putFloat("last_map_lon", (float) map.getMapCenter().getLongitude())
                .putFloat("last_map_zoom", (float) map.getZoomLevelDouble())
                .apply();
        }

        if (sensorManager != null) sensorManager.unregisterListener(compassListener);
        if (map != null) map.onPause();
        if (comboioListener != null) comboioListener.remove();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(prefListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (map != null) map.onDetach();
        map = null;
    }
}
