package com.example.drivelog;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.TreeSet;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.transition.Explode;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnticipateInterpolator;
import android.view.Gravity;
import android.view.animation.OvershootInterpolator;
import android.widget.ScrollView;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.PopupMenu;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;
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
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.events.MapEventsReceiver;
import androidx.activity.OnBackPressedCallback;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RouteFragment extends Fragment {

    private MapView map = null;
    private IMapController mapController;
    private EditText editSearch, editSearchStops;
    private View cardSearch, layoutSearchBalloonOuter, layoutSearchBalloonInner, layoutSearchContainer;
    private ImageButton btnToggleSearch, btnSearch, btnAddStopManual, btnPackageOrganization, btnRouteMenu, btnToggleSearchStops, btnNotifications, btnNavMapIcon;
    private View layoutNotificationsContainer, badgeNotificationDot, layoutNavMapIconContainer;
    private View layoutOpenDrawerInside;
    private View fabCompass;
    private ImageView imageCompassInner;
    private FloatingActionButton fabAddStop, fabNewRoute, fabCenterMap, fabDeliveryApp, fabMapOrientation, fabReportHazard, fabKmTracking;
    private boolean isMapFollowingHeading = false;
    private SensorManager sensorManager;
    private Sensor rotationVectorSensor;
    private float currentAzimuth = 0;
    private long lastGpsMoveTime = 0;
    private static final long GPS_COOLDOWN_MS = 3000;

    private final SensorEventListener compassListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                float[] rotationMatrix = new float[9];
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
                
                // --- Lógica Estável: Remapeamento baseado na rotação da tela ---
                int worldX = SensorManager.AXIS_X;
                int worldY = SensorManager.AXIS_Y;

                if (getActivity() != null) {
                    int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
                    if (rotation == Surface.ROTATION_90) {
                        worldX = SensorManager.AXIS_Y;
                        worldY = SensorManager.AXIS_MINUS_X;
                    } else if (rotation == Surface.ROTATION_180) {
                        worldX = SensorManager.AXIS_MINUS_X;
                        worldY = SensorManager.AXIS_MINUS_Y;
                    } else if (rotation == Surface.ROTATION_270) {
                        worldX = SensorManager.AXIS_MINUS_Y;
                        worldY = SensorManager.AXIS_X;
                    }
                }

                float[] remappedMatrix = new float[9];
                SensorManager.remapCoordinateSystem(rotationMatrix, worldX, worldY, remappedMatrix);
                
                float[] orientation = new float[3];
                SensorManager.getOrientation(remappedMatrix, orientation);
                
                // O azimute (direção) é o primeiro valor do array de orientação
                float azimuthDegrees = (float) Math.toDegrees(orientation[0]);
                if (azimuthDegrees < 0) azimuthDegrees += 360;

                // 🔥 NOVO: Calibração TOTALMENTE separada por modo
                String offsetKey = isMapFollowingHeading ? "compass_offset_follow" : "compass_offset_fixed";
                String invertKey = isMapFollowingHeading ? "compass_inverted_follow" : "compass_inverted_fixed";
                
                float offset = sharedPreferences.getFloat(offsetKey, 0f);
                boolean inverted = sharedPreferences.getBoolean(invertKey, false);

                if (inverted) azimuthDegrees = (360 - azimuthDegrees) % 360;
                azimuthDegrees = (azimuthDegrees + offset + 360) % 360;

                float alpha = 0.2f; 
                float diff = azimuthDegrees - currentAzimuth;
                if (diff > 180) diff -= 360; else if (diff < -180) diff += 360;
                currentAzimuth = currentAzimuth + alpha * diff;

                // Atualiza apenas o mostrador interno da bússola (o fundo do botão fica parado)
                if (imageCompassInner != null && fabCompass != null && fabCompass.getVisibility() == View.VISIBLE) {
                    imageCompassInner.setRotation(-currentAzimuth);
                }

                // 🔥 Só atualiza pela bússola se o GPS não estiver mandando rumo (parado)
                if (System.currentTimeMillis() - lastGpsMoveTime > GPS_COOLDOWN_MS) {
                    if (userDirectionMarker != null && map != null) {
                        boolean hideBoneco = sharedPreferences.getBoolean("hide_marker_when_stationary", false);
                        userDirectionMarker.setVisible(true);

                        // Se estivermos parados e a opção de ocultar boneco estiver ativa, mostramos a seta
                        int targetRes = hideBoneco ? R.drawable.ic_original_arrow : R.drawable.ic_user_stationary;
                        
                        Object lastIconRes = userDirectionMarker.getRelatedObject();
                        if (!(lastIconRes instanceof Integer) || (Integer) lastIconRes != targetRes) {
                            Bitmap bmp = drawableToBitmap(targetRes, false, 0);
                            userDirectionMarker.setIcon(new BitmapDrawable(getResources(), bmp));
                            userDirectionMarker.setRelatedObject(targetRes);
                        }

                        if (isMapFollowingHeading) {
                            // Modo Seguir Direção: Mapa gira e Boneco fica travado para cima (usando o offset do modo)
                            if (isMapFocusedOnUser) {
                                map.setMapOrientation(-currentAzimuth);
                            }
                            // O boneco deve apontar para o "topo", que é corrigido pelo offset de calibração deste modo
                            userDirectionMarker.setRotation(offset); 
                        } else {
                            // Modo Norte Fixo: Mapa parado e Boneco gira com a bússola
                            userDirectionMarker.setRotation(currentAzimuth);
                        }
                        map.invalidate();
                    }
                }
            }
        }
        @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };
    private RecyclerView recyclerSuggestions;
    private SuggestionsAdapter suggestionsAdapter;
    private ViewPager2 viewPagerStops;
    private StopsCardAdapter stopsCardAdapter;
    private View cardNavigationMode;
    private BorderProgressDrawable borderProgressDrawable;
    private double initialStepDistance = 200.0;
    private GeoPoint nextStepLocation = null;
    private float currentStepMaxProgressPct = 0f;
    private String currentStepInstructionKey = "";
    private ImageView imageNavManeuver;
    private TextView textNavDistance, textNavInstruction, textNavTotalTime;
    private RecyclerView recyclerAllStops;
    private StopsListAdapter stopsListAdapter;
    private TextView textSuccessCount, textFailedCount, textPendingCount, textSuccessPackageCount, textPendingPackageCount;
    private TextView textWeatherTemp, textWeatherCity, textRouteTotalTime, textRouteCorrections, textSheetHeader, textPendingManualListTitle, textPendingManualListSubtitle;
    private View cardWeatherSummary, cardRouteTotalTime, cardRouteCorrections, cardSuccessSummary, cardFailedSummary, cardPendingSummary, layoutStatsGroup, btnToggleStatsSummary;
    private MaterialCardView cardPendingOptimization, cardPendingManualList, cardUndoCorrection, cardSearchNavDistance;
    private View cardRestoreStopsSheet;
    private MaterialButton btnFinishOptimizationNow, btnContinueManualListEditing, btnCancelAndDeleteManualRoute, btnCancelAndDeleteOptimizationRoute, btnUndoCorrectionAction;
    private TextView textUndoCorrectionMessage, textSearchNavDistance;
    private final Handler undoHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingUndoRunnable;
    private ImageView imageWeatherIcon, imageHomeWarning, imageToggleStatsSummary;
    private boolean isStopDeleteDialogShowing = false;
    private boolean isSubMenuOpen = false;
    private boolean isMenuItemClicked = false;
    private final Handler autoHideHandler = new Handler(Looper.getMainLooper());
    private Runnable autoHideRunnable;
    private final Handler menuAnimHandler = new Handler(Looper.getMainLooper());
    private Runnable menuAnimRunnable;
    private List<LoadingPoint> loadingPoints = new ArrayList<>();
    private final List<Marker> loadingMarkers = new ArrayList<>();
    private MapEventsOverlay loadingSelectionOverlay;
    private String lastCityName = "";
    private GeoPoint lastWeatherLocation = null;
    private long lastWeatherUpdate = 0;
    private List<DayWeather> lastWeekWeather = new ArrayList<>();

    public static class BorderProgressDrawable extends Drawable {
        private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path borderPath = new Path();
        private final Path segmentPath = new Path();
        private final PathMeasure pathMeasure = new PathMeasure();
        private final RectF rectF = new RectF();
        private float cornerRadius;
        private float strokeWidth;
        private float progressPercent = 100f;

        public BorderProgressDrawable(float strokeWidthDp, float cornerRadiusDp, int trackColor, int progressColor, Context context) {
            float density = context.getResources().getDisplayMetrics().density;
            this.strokeWidth = strokeWidthDp * density;
            this.cornerRadius = cornerRadiusDp * density;

            trackPaint.setStyle(Paint.Style.STROKE);
            trackPaint.setStrokeWidth(this.strokeWidth);
            trackPaint.setColor(trackColor);

            progressPaint.setStyle(Paint.Style.STROKE);
            progressPaint.setStrokeWidth(this.strokeWidth);
            progressPaint.setColor(progressColor);
            progressPaint.setStrokeCap(Paint.Cap.ROUND);
        }

        private ValueAnimator progressAnimator;

        public float getProgress() {
            return this.progressPercent;
        }

        public void setProgress(float percent) {
            setProgress(percent, true);
        }

        public void setProgress(float percent, boolean animate) {
            final float target = Math.max(0f, Math.min(100f, percent));

            if (!animate) {
                if (progressAnimator != null && progressAnimator.isRunning()) {
                    progressAnimator.cancel();
                }
                this.progressPercent = target;
                invalidateSelf();
                return;
            }

            if (Math.abs(this.progressPercent - target) < 0.1f) {
                return;
            }

            if (progressAnimator != null && progressAnimator.isRunning()) {
                progressAnimator.cancel();
            }

            progressAnimator = ValueAnimator.ofFloat(this.progressPercent, target);
            progressAnimator.setDuration(450);
            progressAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            progressAnimator.addUpdateListener(animation -> {
                this.progressPercent = (float) animation.getAnimatedValue();
                invalidateSelf();
            });
            progressAnimator.start();
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            super.onBoundsChange(bounds);
            float inset = strokeWidth / 2f;
            rectF.set(bounds.left + inset, bounds.top + inset, bounds.right - inset, bounds.bottom - inset);

            borderPath.reset();
            borderPath.addRoundRect(rectF, cornerRadius, cornerRadius, Path.Direction.CW);
            pathMeasure.setPath(borderPath, false);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            Rect b = getBounds();
            if (b.width() > 0 && b.height() > 0) {
                if (rectF.width() != (b.width() - strokeWidth) || rectF.height() != (b.height() - strokeWidth)) {
                    float inset = strokeWidth / 2f;
                    rectF.set(b.left + inset, b.top + inset, b.right - inset, b.bottom - inset);
                    borderPath.reset();
                    borderPath.addRoundRect(rectF, cornerRadius, cornerRadius, Path.Direction.CW);
                    pathMeasure.setPath(borderPath, false);
                }
            }

            canvas.drawPath(borderPath, trackPaint);

            float totalLength = pathMeasure.getLength();
            if (totalLength > 0 && progressPercent > 0) {
                float drawLength = (progressPercent / 100f) * totalLength;
                segmentPath.reset();
                pathMeasure.getSegment(0, drawLength, segmentPath, true);
                segmentPath.rLineTo(0, 0);
                canvas.drawPath(segmentPath, progressPaint);
            }
        }

        @Override
        public void setAlpha(int alpha) {
            trackPaint.setAlpha(alpha);
            progressPaint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            trackPaint.setColorFilter(colorFilter);
            progressPaint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

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
    private MaterialSwitch switchTraceLine;
    private TextView textSwitchDistance;
    private boolean isEditMode = false;
    private boolean isUnifyMode = false;
    private boolean isLassoMode = false;
    private boolean isLassoDrawingEnabled = false;
    private int lassoGroupCounter = 0;
    private ItemTouchHelper itemTouchHelper;
    private MyLocationNewOverlay locationOverlay;
    private Marker homeMarker, userDirectionMarker;
    private Polygon homeRadiusOverlay;
    private Polyline selectionTracePolyline;
    private MapEventsOverlay currentFixOverlay, homeSelectionOverlay, searchMapEventsOverlay;
    private LassoOverlay lassoOverlay;
    private View cardFixMode, cardLassoMode, layoutSideFabs;
    private View layoutSummary, layoutLeftSummary, layoutSwitchContainer;
    private View cardToggleSystemUI;
    private ImageView imageToggleArrow;
    private View cardRestWarning;
    private View btnDisableRestInRoute;
    private List<NavInstruction> lastRouteInstructions = new ArrayList<>();
    
    private ImageButton btnMuteVoiceNav;
    private TextToSpeech tts;
    private String lastSpokenInstruction = "";
    private long lastSpokenTime = 0;
    private int lastAnnouncedStopId = -1;
    private boolean is100mAnnounced = false;
    private boolean isArrivedAnnounced = false;
    private KonfettiView konfettiView;

    private boolean isVoiceNavigationEnabled() {
        if (sharedPreferences == null) return true;
        boolean isMuted = sharedPreferences.getBoolean("voice_navigation_muted", false);
        boolean isEnabled = sharedPreferences.getBoolean("voice_navigation_enabled", true)
                && sharedPreferences.getBoolean("voice_commands_enabled", true);
        return !isMuted && isEnabled;
    }

    private void setVoiceNavigationEnabled(boolean enabled, boolean showToast) {
        if (sharedPreferences == null) return;

        boolean isMuted = !enabled;
        sharedPreferences.edit()
                .putBoolean("voice_navigation_muted", isMuted)
                .putBoolean("voice_navigation_enabled", enabled)
                .putBoolean("voice_commands_enabled", enabled)
                .apply();

        updateMuteButtonIcon();

        if (enabled) {
            if (showToast) Toast.makeText(getContext(), "🔊 Voz na Navegação Ativada", Toast.LENGTH_SHORT).show();
            speakVoiceNavigation("Voz da navegação ativada", true);
        } else {
            if (tts != null) tts.stop();
            if (showToast) Toast.makeText(getContext(), "🔇 Voz na Navegação Silenciada", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateMuteButtonIcon() {
        if (btnMuteVoiceNav == null) return;
        boolean enabled = isVoiceNavigationEnabled();
        if (enabled) {
            btnMuteVoiceNav.setImageResource(android.R.drawable.ic_lock_silent_mode_off);
        } else {
            btnMuteVoiceNav.setImageResource(android.R.drawable.ic_lock_silent_mode);
        }
    }

    private void speakVoiceNavigation(String text, boolean force) {
        if (text == null || text.trim().isEmpty() || tts == null) return;
        if (!isVoiceNavigationEnabled()) return;

        long now = System.currentTimeMillis();
        String cleaned = text.trim();
        if (!force && cleaned.equalsIgnoreCase(lastSpokenInstruction) && (now - lastSpokenTime < 8000)) {
            return;
        }

        lastSpokenInstruction = cleaned;
        lastSpokenTime = now;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(cleaned, TextToSpeech.QUEUE_FLUSH, null, "nav_voice_" + now);
        } else {
            tts.speak(cleaned, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    private String lastManeuverKey = "";
    private int lastDistanceStage = -1;

    private void announceNavigationTurn(String instruction, double nextDistMeters) {
        if (instruction == null || instruction.trim().isEmpty()) return;

        String distPhrase;
        int distanceStage;

        if (nextDistMeters > 300) {
            distanceStage = 0;
            if (nextDistMeters >= 1000) {
                distPhrase = String.format(Locale.getDefault(), "Em %.1f quilômetros, ", nextDistMeters / 1000.0);
            } else {
                distPhrase = "Em " + (int) (Math.round(nextDistMeters / 50.0) * 50) + " metros, ";
            }
        } else if (nextDistMeters > 60) {
            distanceStage = 1;
            distPhrase = "Em " + (int) (Math.round(nextDistMeters / 10.0) * 10) + " metros, ";
        } else {
            distanceStage = 2;
            distPhrase = "Agora, ";
        }

        String maneuverKey = instruction.trim().toLowerCase();
        String fullVoiceText = distPhrase + instruction.trim();

        if (!maneuverKey.equalsIgnoreCase(lastManeuverKey) || distanceStage > lastDistanceStage) {
            lastManeuverKey = maneuverKey;
            lastDistanceStage = distanceStage;
            speakVoiceNavigation(fullVoiceText, true);
        }
    }

    private RouteStop currentlySelectedStop = null;
    private long lastTraceUpdate = 0;
    private GeoPoint lastTraceLocation = null;
    private List<GeoPoint> fullTracePoints = new ArrayList<>();

    private ListenerRegistration comboioListener;
    private ListenerRegistration hazardListener;
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
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
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
            try {
                if (isAdded() && currentRouteHeader != null) {
                    boolean hasStops = currentStops != null && !currentStops.isEmpty();
                    boolean timerOnCardsOnly = sharedPreferences != null && sharedPreferences.getBoolean("timer_on_cards_only", false);

                    if (currentRouteHeader.startTime > 0) {
                        long elapsed;
                        if (currentRouteHeader.endTime > 0) {
                            elapsed = currentRouteHeader.endTime - currentRouteHeader.startTime - currentRouteHeader.totalPausedMs;
                        } else {
                            long now = System.currentTimeMillis();
                            long currentPausedMs = currentRouteHeader.totalPausedMs + 
                                (currentRouteHeader.lastPauseStartTime > 0 ? (now - currentRouteHeader.lastPauseStartTime) : 0);
                            elapsed = now - currentRouteHeader.startTime - currentPausedMs;
                        }
                        if (elapsed < 0) elapsed = 0;
                        
                        long s = elapsed / 1000;
                        long m = s / 60;
                        long h_val = m / 60;
                        String time = String.format(Locale.getDefault(), "%02d:%02d:%02d", h_val, m % 60, s % 60);
                        if (currentRouteHeader.lastPauseStartTime > 0 && currentRouteHeader.endTime == 0) {
                            time += " ⏸️";
                        }

                        if (textRouteTotalTime != null) textRouteTotalTime.setText(time);
                        
                        boolean homeDefined = sharedPreferences != null && sharedPreferences.getFloat("home_lat", 0) != 0;
                        if (imageHomeWarning != null) {
                            imageHomeWarning.setVisibility(homeDefined ? View.GONE : View.VISIBLE);
                        }

                        boolean isOptPending = sharedPreferences != null && sharedPreferences.getBoolean("route_optimization_pending_" + currentRouteId, false);

                        if (cardRouteTotalTime != null) {
                            if (isOptPending) {
                                cardRouteTotalTime.setVisibility(View.GONE);
                            } else if (timerOnCardsOnly) {
                                cardRouteTotalTime.setVisibility(currentRouteHeader.endTime > 0 ? View.VISIBLE : View.GONE);
                            } else {
                                cardRouteTotalTime.setVisibility(hasStops ? View.VISIBLE : View.GONE);
                            }
                        }

                        // Notifica os cards para atualizar o tempo individual
                        if (viewPagerStops != null && stopsCardAdapter != null && currentStops != null && !currentStops.isEmpty()) {
                            stopsCardAdapter.notifyItemRangeChanged(0, currentStops.size(), "TIMER_UPDATE");
                        }
                    } else {
                        // Rota carregada, mas ainda não iniciada (startTime == 0)
                        boolean isOptPending = sharedPreferences != null && sharedPreferences.getBoolean("route_optimization_pending_" + currentRouteId, false);
                        if (cardRouteTotalTime != null && textRouteTotalTime != null) {
                            if (hasStops && !timerOnCardsOnly && !isOptPending) {
                                textRouteTotalTime.setText("Iniciar Rota");
                                if (imageHomeWarning != null) imageHomeWarning.setVisibility(View.GONE);
                                cardRouteTotalTime.setVisibility(View.VISIBLE);
                            } else {
                                cardRouteTotalTime.setVisibility(View.GONE);
                            }
                        }
                    }
                } else {
                    if (cardRouteTotalTime != null) cardRouteTotalTime.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                Log.e("DriveLog", "Erro no timerRunnable: " + e.getMessage(), e);
            } finally {
                timerHandler.postDelayed(this, 1000);
            }
        }
    };
    private boolean isSelectingSuggestion = false;
    private List<RouteStop> currentStops = new ArrayList<>();
    private int currentRouteId = -1;
    private int pendingRestoreIndex = -1;
    private boolean isPositionRestored = false;
    private boolean hasLeftHomeForRoute = false;
    private LiveData<List<RouteStop>> currentStopsLive;
    private LiveData<List<RouteGroup>> currentGroupsLive;
    private LiveData<RouteHeader> currentHeaderLive;

    private static final String PREF_LAST_ROUTE = "last_opened_route_id";
    private static final String PREF_LAST_STOP_PREFIX = "last_stop_index_";
    private static final String STATE_CURRENT_STOP_INDEX = "current_stop_index";

    private boolean isMapFocusedOnUser = true;
    private boolean shouldFocusOnFirstStop = false;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private ConnectivityManager.NetworkCallback networkCallback;

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
        } else if ("bloco_icon_size".equals(key) || "quadra_icon_size".equals(key)) {
            Activity activity = getActivity(); if (activity != null) activity.runOnUiThread(this::refreshQuadraMarkers);
        } else if ("stop_icon_size".equals(key)) {
            Activity activity = getActivity(); if (activity != null) activity.runOnUiThread(this::refreshMarkers);
        } else if ("home_trigger_radius".equals(key) || "home_arrival_radius".equals(key) || "home_lat".equals(key) || "home_lon".equals(key)) {
            Activity activity = getActivity(); if (activity != null) activity.runOnUiThread(this::showHomeMarker);
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

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            
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
                ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) layoutSideFabs.getLayoutParams();
                
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
                    lp.startToStart = ConstraintLayout.LayoutParams.UNSET;
                    lp.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
                    lp.setMarginEnd((int) (16 * getResources().getDisplayMetrics().density));
                    lp.setMarginStart(0);
                } else {
                    lp.endToEnd = ConstraintLayout.LayoutParams.UNSET;
                    lp.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
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
        ViewCompat.requestApplyInsets(root);
    }

    private void closeSearchUI() {
        if (editSearch == null || cardSearch == null || getContext() == null) return;
        if (editSearch.getVisibility() == View.GONE) return;

        editSearch.animate().alpha(0f).setDuration(200).withEndAction(() -> {
            editSearch.setVisibility(View.GONE);
            editSearch.setText("");

            ViewGroup.LayoutParams lp = cardSearch.getLayoutParams();
            if (lp instanceof RelativeLayout.LayoutParams) {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) lp;
                params.width = RelativeLayout.LayoutParams.WRAP_CONTENT;
                params.removeRule(RelativeLayout.ALIGN_PARENT_START);
                params.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE);
                cardSearch.setLayoutParams(params);
            }

            if (layoutSearchBalloonOuter != null) layoutSearchBalloonOuter.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
            if (layoutSearchBalloonInner != null) layoutSearchBalloonInner.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;

            if (btnToggleSearch != null) {
                btnToggleSearch.setVisibility(View.VISIBLE);
                btnToggleSearch.setImageResource(android.R.drawable.ic_menu_search);
                btnToggleSearch.setTranslationX(0f);
                btnToggleSearch.setAlpha(0f);
                btnToggleSearch.animate().alpha(1f).setDuration(200).start();
            }

            if (btnAddStopManual != null) {
                btnAddStopManual.setVisibility(View.VISIBLE);
                btnAddStopManual.setAlpha(0f);
                btnAddStopManual.animate().alpha(1f).setDuration(200).start();
            }

            if (btnPackageOrganization != null) {
                btnPackageOrganization.setVisibility(View.VISIBLE);
                btnPackageOrganization.setAlpha(0f);
                btnPackageOrganization.animate().alpha(1f).setDuration(200).start();
            }

            if (btnRouteMenu != null) {
                btnRouteMenu.setVisibility(View.VISIBLE);
                btnRouteMenu.setAlpha(0f);
                btnRouteMenu.animate().alpha(1f).setDuration(200).start();
            }

            if (layoutNotificationsContainer != null) {
                boolean showNotifs = sharedPreferences.getBoolean("show_fab_notifications", true);
                layoutNotificationsContainer.setVisibility(showNotifs ? View.VISIBLE : View.GONE);
                layoutNotificationsContainer.setAlpha(0f);
                layoutNotificationsContainer.animate().alpha(1f).setDuration(200).start();
            }

            if (layoutNavMapIconContainer != null) {
                boolean isSearchBalloonStyle = "search_balloon".equals(sharedPreferences.getString("nav_button_style", "original"));
                boolean hasStops = currentStops != null && !currentStops.isEmpty();
                boolean isOptPending = sharedPreferences.getBoolean("route_optimization_pending_" + currentRouteId, false);
                boolean isManualPending = sharedPreferences.getBoolean("route_manual_list_pending_" + currentRouteId, false);
                
                if (isSearchBalloonStyle && hasStops && !isOptPending && !isManualPending) {
                    layoutNavMapIconContainer.setVisibility(View.VISIBLE);
                    layoutNavMapIconContainer.setAlpha(0f);
                    layoutNavMapIconContainer.animate().alpha(1f).setDuration(200).start();
                } else {
                    layoutNavMapIconContainer.setVisibility(View.GONE);
                }
            }

            updateNavigationButtonStyle();
        }).start();

        editSearch.clearFocus();
        if (recyclerSuggestions != null) recyclerSuggestions.setVisibility(View.GONE);

        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editSearch.getWindowToken(), 0);

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
            mb = (int) (246 * getResources().getDisplayMetrics().density) + systemBottom;
        } else {
            // Se NÃO estiver ativo, eles descem para perto da parte inferior da tela
            // No modo Mapa (app_mode 1), sistemaBottom cuida da barra de navegação. 
            // No modo Completo, eles ficam logo acima do menu inferior.
            mb = (int) (32 * getResources().getDisplayMetrics().density) + systemBottom;
        }

        ViewGroup.LayoutParams lp = layoutSideFabs.getLayoutParams();
        if (lp instanceof ConstraintLayout.LayoutParams) {
            ConstraintLayout.LayoutParams p = (ConstraintLayout.LayoutParams) lp;
            p.bottomMargin = mb; 
            layoutSideFabs.setLayoutParams(p);
        }

        if (cardRouteTotalTime != null) {
            ViewGroup.LayoutParams lpTime = cardRouteTotalTime.getLayoutParams();
            if (lpTime instanceof ConstraintLayout.LayoutParams) {
                ConstraintLayout.LayoutParams pTime = (ConstraintLayout.LayoutParams) lpTime;
                int stopsCardTopEdge = isCardActive ? (int) (201 * getResources().getDisplayMetrics().density) + systemBottom : ((int) (16 * getResources().getDisplayMetrics().density) + systemBottom);
                pTime.bottomMargin = stopsCardTopEdge;
                cardRouteTotalTime.setLayoutParams(pTime);
            }
        }

        if (cardRestoreStopsSheet != null) {
            ViewGroup.LayoutParams lpRestore = cardRestoreStopsSheet.getLayoutParams();
            if (lpRestore instanceof ConstraintLayout.LayoutParams) {
                ConstraintLayout.LayoutParams pRestore = (ConstraintLayout.LayoutParams) lpRestore;
                int restoreMargin = (int) (24 * getResources().getDisplayMetrics().density) + systemBottom;
                pRestore.bottomMargin = restoreMargin;
                cardRestoreStopsSheet.setLayoutParams(pRestore);
            }
        }
    }

    private void showCompassCalibrationDialog() {
        final String modeName = isMapFollowingHeading ? "Seguir Direção" : "Norte Fixo";
        final String offsetKey = isMapFollowingHeading ? "compass_offset_follow" : "compass_offset_fixed";
        final String invertKey = isMapFollowingHeading ? "compass_inverted_follow" : "compass_inverted_fixed";

        float currentOffset = sharedPreferences.getFloat(offsetKey, 0f);
        boolean currentInverted = sharedPreferences.getBoolean(invertKey, false);
        String invertStatus = currentInverted ? "Ligado" : "Desligado";

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        String[] options = {"Girar +90°", "Girar -90°", "Inverter Sentido do Giro", "Ocultar este Botão", "Resetar Padrão"};
        
        builder.setTitle(modeName + " (Ajuste: " + (int)currentOffset + "° | Inverter: " + invertStatus + ")")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) sharedPreferences.edit().putFloat(offsetKey, (currentOffset + 90) % 360).apply();
                    else if (which == 1) sharedPreferences.edit().putFloat(offsetKey, (currentOffset - 90 + 360) % 360).apply();
                    else if (which == 2) sharedPreferences.edit().putBoolean(invertKey, !currentInverted).apply();
                    else if (which == 3) promptHideFab("show_fab_compass", "Bússola");
                    else if (which == 4) sharedPreferences.edit().putFloat(offsetKey, 0f).putBoolean(invertKey, false).apply();
                    
                    if (which != 3) Toast.makeText(getContext(), "Calibração aplicada para " + modeName, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Fechar", null);
        
        AlertDialog d = builder.create();
        if (d.getWindow() != null) d.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog_rounded);
        d.show();
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

    private void updateNavigationButtonStyle() {
        if (getContext() == null) return;
        String navStyle = sharedPreferences.getString("nav_button_style", "original");
        boolean isSearchBalloonStyle = "search_balloon".equals(navStyle);
        boolean hasStops = currentStops != null && !currentStops.isEmpty();

        boolean isOptPending = sharedPreferences.getBoolean("route_optimization_pending_" + currentRouteId, false);
        boolean isManualPending = sharedPreferences.getBoolean("route_manual_list_pending_" + currentRouteId, false);

        if (isOptPending || isManualPending || !hasStops) {
            if (layoutSwitchContainer != null) layoutSwitchContainer.setVisibility(View.GONE);
            if (layoutNavMapIconContainer != null) layoutNavMapIconContainer.setVisibility(View.GONE);
            return;
        }

        if (isSearchBalloonStyle) {
            if (layoutSwitchContainer != null) layoutSwitchContainer.setVisibility(View.GONE);
            if (layoutNavMapIconContainer != null) layoutNavMapIconContainer.setVisibility(View.VISIBLE);
        } else {
            if (layoutSwitchContainer != null) layoutSwitchContainer.setVisibility(View.VISIBLE);
            if (layoutNavMapIconContainer != null) layoutNavMapIconContainer.setVisibility(View.GONE);
        }

        updateNavMapIconState();
    }

    private void updateNavMapIconState() {
        if (btnNavMapIcon == null) return;
        boolean isTraceActive = switchTraceLine != null && switchTraceLine.isChecked();
        String navStyle = sharedPreferences != null ? sharedPreferences.getString("nav_button_style", "original") : "original";
        boolean isSearchBalloonStyle = "search_balloon".equals(navStyle);

        TypedValue typedValue = new TypedValue();
        int primaryColor = Color.parseColor("#0077B6");
        if (getContext() != null) {
            requireContext().getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
            primaryColor = typedValue.data;
        }

        btnNavMapIcon.setColorFilter(primaryColor);

        if (isTraceActive) {
            btnNavMapIcon.setAlpha(1.0f);

            if (isSearchBalloonStyle && cardSearchNavDistance != null && editSearch != null && editSearch.getVisibility() != View.VISIBLE) {
                if (cardSearchNavDistance.getVisibility() != View.VISIBLE) {
                    cardSearchNavDistance.setVisibility(View.VISIBLE);
                    cardSearchNavDistance.setTranslationY(-20f);
                    cardSearchNavDistance.setAlpha(0f);
                    cardSearchNavDistance.animate().translationY(0f).alpha(1f).setDuration(250).start();
                }
            } else if (cardSearchNavDistance != null) {
                cardSearchNavDistance.setVisibility(View.GONE);
            }
        } else {
            btnNavMapIcon.setAlpha(0.5f);

            if (cardSearchNavDistance != null && cardSearchNavDistance.getVisibility() == View.VISIBLE) {
                cardSearchNavDistance.animate().translationY(-20f).alpha(0f).setDuration(200).withEndAction(() -> {
                    cardSearchNavDistance.setVisibility(View.GONE);
                }).start();
            }
        }
    }

    private void updateFloatingButtonsVisibility() {
        if (getContext() == null) return;

        updateNavigationButtonStyle();

        boolean isOptPending = sharedPreferences.getBoolean("route_optimization_pending_" + currentRouteId, false);
        boolean isManualPending = sharedPreferences.getBoolean("route_manual_list_pending_" + currentRouteId, false);

        if (isOptPending || isManualPending) {
            if (fabDeliveryApp != null) fabDeliveryApp.setVisibility(View.GONE);
            if (fabCompass != null) fabCompass.setVisibility(View.GONE);
            if (fabReportHazard != null) fabReportHazard.setVisibility(View.GONE);
            if (fabCenterMap != null) fabCenterMap.setVisibility(View.GONE);
            if (fabMapOrientation != null) fabMapOrientation.setVisibility(View.GONE);
            if (fabKmTracking != null) fabKmTracking.setVisibility(View.GONE);
            if (layoutSwitchContainer != null) layoutSwitchContainer.setVisibility(View.GONE);
            if (cardRouteTotalTime != null) cardRouteTotalTime.setVisibility(View.GONE);
            if (bottomSheet != null) bottomSheet.setVisibility(View.GONE);
            if (cardNavigationMode != null) cardNavigationMode.setVisibility(View.GONE);
            if (layoutStatsGroup != null) layoutStatsGroup.setVisibility(View.GONE);
            if (cardSuccessSummary != null) cardSuccessSummary.setVisibility(View.GONE);
            if (cardPendingSummary != null) cardPendingSummary.setVisibility(View.GONE);
            if (cardFailedSummary != null) cardFailedSummary.setVisibility(View.GONE);
            if (btnToggleStatsSummary != null) btnToggleStatsSummary.setVisibility(View.INVISIBLE);
            if (cardRouteCorrections != null) cardRouteCorrections.setVisibility(View.GONE);

            if (cardPendingManualList != null) {
                cardPendingManualList.setVisibility(isManualPending ? View.VISIBLE : View.GONE);
                if (isManualPending) updatePendingManualListUI();
            }
            if (cardPendingOptimization != null) {
                cardPendingOptimization.setVisibility(isOptPending && !isManualPending ? View.VISIBLE : View.GONE);
            }
            if (cardRestoreStopsSheet != null) cardRestoreStopsSheet.setVisibility(View.GONE);
            return;
        } else {
            if (cardPendingManualList != null) cardPendingManualList.setVisibility(View.GONE);
            if (cardPendingOptimization != null) cardPendingOptimization.setVisibility(View.GONE);
        }

        // --- Cálculo do estado da rota ---
        int pendCount = 0;
        for (RouteStop s : currentStops) if (s.deliveryStatus == 0) pendCount++;
        boolean hasStops = !currentStops.isEmpty();
        boolean isRouteFinished = hasStops && pendCount == 0;
        boolean isRouteActive = hasStops && !isRouteFinished;

        boolean showDelivery = sharedPreferences.getBoolean("show_fab_delivery_app", true);
        boolean showCompass = sharedPreferences.getBoolean("show_fab_compass", true);
        boolean showReport = sharedPreferences.getBoolean("show_fab_report_hazard", true);
        boolean showCenter = sharedPreferences.getBoolean("show_fab_center_map", true);
        boolean showNorth = sharedPreferences.getBoolean("show_fab_orientation", true);
        boolean showKm = sharedPreferences.getBoolean("show_fab_km_tracking", true);
        boolean showStopsCard = sharedPreferences.getBoolean("show_bottom_sheet_stops", true);

        if (fabDeliveryApp != null) fabDeliveryApp.setVisibility(showDelivery ? View.VISIBLE : View.GONE);
        if (fabCompass != null) fabCompass.setVisibility(showCompass ? View.VISIBLE : View.GONE);
        if (fabReportHazard != null) fabReportHazard.setVisibility(showReport ? View.VISIBLE : View.GONE);
        if (fabCenterMap != null) fabCenterMap.setVisibility((showCenter && isRouteActive) ? View.VISIBLE : View.GONE);
        if (fabMapOrientation != null) fabMapOrientation.setVisibility(showNorth ? View.VISIBLE : View.GONE);

        if (fabKmTracking != null) {
            boolean remoteVisible = getActivity() instanceof MainActivity && ((MainActivity) getActivity()).isMenuVisible("km");
            fabKmTracking.setVisibility((remoteVisible && showKm) ? View.VISIBLE : View.GONE);
        }

        boolean showNotifs = sharedPreferences.getBoolean("show_fab_notifications", true);
        if (layoutNotificationsContainer != null) {
            layoutNotificationsContainer.setVisibility(showNotifs ? View.VISIBLE : View.GONE);
        }
        
        if (fabNewRoute != null) {
            fabNewRoute.setVisibility((!hasStops || isRouteFinished) ? View.VISIBLE : View.GONE);
        }

        updateNavigationButtonStyle();

        if (bottomSheet != null) {
            bottomSheet.setVisibility((!hasStops || !showStopsCard) ? View.GONE : View.VISIBLE);
        }

        if (cardRestoreStopsSheet != null) {
            boolean shouldShowRestoreButton = hasStops && !showStopsCard;
            cardRestoreStopsSheet.setVisibility(shouldShowRestoreButton ? View.VISIBLE : View.GONE);
        }

        if (hasStops && autoHideRunnable == null) {
            boolean isStatsExpanded = sharedPreferences.getBoolean("stats_summary_expanded", true);
            if (layoutStatsGroup != null) layoutStatsGroup.setVisibility(isStatsExpanded ? View.VISIBLE : View.GONE);
            if (cardSuccessSummary != null) cardSuccessSummary.setVisibility(isStatsExpanded ? View.VISIBLE : View.GONE);
            if (cardPendingSummary != null) cardPendingSummary.setVisibility(isStatsExpanded ? View.VISIBLE : View.GONE);
            if (cardFailedSummary != null) {
                int errStops = 0;
                if (currentStops != null) {
                    for (RouteStop s : currentStops) if (s.deliveryStatus == 2) errStops++;
                }
                cardFailedSummary.setVisibility((isStatsExpanded && errStops > 0) ? View.VISIBLE : View.GONE);
            }
        }
    }

    private void animateNumber(TextView textView, int targetValue) {
        if (textView == null) return;
        String currentText = textView.getText().toString();
        int initialValue = 0;
        try { initialValue = Integer.parseInt(currentText); } catch (Exception ignored) {}
        
        if (initialValue == targetValue) return;

        ValueAnimator animator = ValueAnimator.ofInt(initialValue, targetValue);
        animator.setDuration(800);
        animator.addUpdateListener(animation -> textView.setText(animation.getAnimatedValue().toString()));
        animator.start();
    }

    private final ActivityResultLauncher<Intent> importXlsxLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) processXlsxImport(result.getData().getData());
    });

    private EditText activeAddressFieldForOcr = null;
    private EditText activeSequenceFieldForOcr = null;

    private final ActivityResultLauncher<Intent> ocrPhotoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) processOcrImageUri(uri);
                }
            }
    );

    private final ActivityResultLauncher<Intent> ocrCameraCaptureLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null && extras.get("data") instanceof Bitmap) {
                        Bitmap bitmap = (Bitmap) extras.get("data");
                        processOcrBitmap(bitmap);
                    }
                }
            }
    );

    private void promptOcrSource(EditText targetAddressField, EditText targetSequenceField) {
        this.activeAddressFieldForOcr = targetAddressField;
        this.activeSequenceFieldForOcr = targetSequenceField;

        String[] options = {"📷 Tirar Foto com Câmera", "🖼️ Escolher Imagem da Galeria"};
        new AlertDialog.Builder(requireContext())
                .setTitle("📷 Identificar Endereço por Foto")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        ocrCameraCaptureLauncher.launch(cameraIntent);
                    } else {
                        Intent galleryIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        galleryIntent.addCategory(Intent.CATEGORY_OPENABLE);
                        galleryIntent.setType("image/*");
                        ocrPhotoPickerLauncher.launch(galleryIntent);
                    }
                })
                .show();
    }

    private void processOcrImageUri(Uri imageUri) {
        try {
            InputImage image = InputImage.fromFilePath(requireContext(), imageUri);
            runMlKitTextRecognition(image);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Erro ao carregar imagem: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void processOcrBitmap(Bitmap bitmap) {
        try {
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            runMlKitTextRecognition(image);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Erro ao processar foto: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void runMlKitTextRecognition(InputImage image) {
        if (getContext() == null) return;

        Toast.makeText(getContext(), "🔍 Lendo texto da foto com IA...", Toast.LENGTH_SHORT).show();

        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    List<String> detectedLines = new ArrayList<>();
                    String textAll = visionText.getText();

                    if (textAll == null || textAll.trim().isEmpty()) {
                        Toast.makeText(getContext(), "Nenhum texto identificado na foto.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (Text.TextBlock block : visionText.getTextBlocks()) {
                        for (Text.Line line : block.getLines()) {
                            String lText = line.getText().trim();
                            if (lText.length() > 3) {
                                detectedLines.add(lText);
                            }
                        }
                    }

                    if (detectedLines.isEmpty()) {
                        Toast.makeText(getContext(), "Nenhum endereço reconhecido.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<String> addressCandidates = new ArrayList<>();
                    String detectedSeq = "";

                    for (String line : detectedLines) {
                        String lower = line.toLowerCase();
                        if (lower.contains("rua") || lower.contains("r.") || lower.contains("av") || lower.contains("avenida")
                                || lower.contains("alameda") || lower.contains("servid") || lower.contains("travessa")
                                || lower.contains("rodovia") || lower.contains("quadra") || lower.contains("qd")
                                || lower.contains("bloco") || lower.contains("bl") || lower.contains("bairro")
                                || line.matches(".*\\d{1,5}.*")) {
                            addressCandidates.add(line);
                        }

                        if (detectedSeq.isEmpty() && (lower.contains("spx") || lower.contains("seq") || line.matches("^\\d{1,3}$"))) {
                            detectedSeq = line.replaceAll("[^0-9A-Za-z-]", "").trim();
                        }
                    }

                    final String finalSeq = detectedSeq;

                    if (addressCandidates.isEmpty()) {
                        addressCandidates = detectedLines;
                    }

                    if (addressCandidates.size() == 1) {
                        applyOcrResultToFields(addressCandidates.get(0), finalSeq);
                    } else {
                        String[] candidateArray = addressCandidates.toArray(new String[0]);
                        new AlertDialog.Builder(requireContext())
                                .setTitle("📷 Selecione o Endereço Lido")
                                .setItems(candidateArray, (dialog, which) -> {
                                    applyOcrResultToFields(candidateArray[which], finalSeq);
                                })
                                .setNegativeButton("CANCELAR", null)
                                .show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Erro ao reconhecer texto: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void applyOcrResultToFields(String addressText, String sequenceText) {
        if (activeAddressFieldForOcr != null) {
            activeAddressFieldForOcr.setText(addressText);
        }
        if (activeSequenceFieldForOcr != null && sequenceText != null && !sequenceText.isEmpty()) {
            activeSequenceFieldForOcr.setText(sequenceText);
        }
        Toast.makeText(getContext(), "✨ Endereço preenchido pela foto!", Toast.LENGTH_SHORT).show();
    }

    private final BroadcastReceiver newRouteReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { if ("com.example.entregas.ACTION_NEW_ROUTE".equals(intent.getAction())) promptNewRoute(); }
    };

    public void refreshBadges() {
        // Marcadores de pendência desativados
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d("DriveLog", "Iniciando onCreateView - " + System.currentTimeMillis());
        View view = inflater.inflate(R.layout.fragment_route, container, false);
        // view.setBackgroundColor(Color.RED); // TESTE RADICAL: SE O FUNDO FICAR VERMELHO, O CÓDIGO NOVO ESTÁ RODANDO
        sharedPreferences = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
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
        map.setBuiltInZoomControls(false);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        mapController = map.getController(); 
        
        // 🔥 RESTAURAR ÚLTIMA POSIÇÃO PARA EVITAR CARREGAMENTO DO ZERO
        float lastLat = sharedPreferences.getFloat("last_map_lat", 0f);
        float lastLon = sharedPreferences.getFloat("last_map_lon", 0f);
        float lastZoom = sharedPreferences.getFloat("last_map_zoom", 15.0f);
        if (lastLat != 0 && lastLon != 0) {
            mapController.setZoom((double) lastZoom);
            mapController.setCenter(new GeoPoint(lastLat, lastLon));
        } else {
            mapController.setZoom(15.0);
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(prefListener);
        setupLocationOverlay();
        refreshQuadraMarkers();

        map.addMapListener(new MapListener() {
            @Override
            public boolean onZoom(ZoomEvent event) {
                if (map != null) {
                    map.post(() -> updateQuadraMarkerSizes());
                }
                return false;
            }

            @Override
            public boolean onScroll(ScrollEvent event) {
                return false;
            }
        });
        
        // Overlay para fechar busca ao clicar no mapa
        searchMapEventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
            @Override public boolean singleTapConfirmedHelper(GeoPoint p) {
                closeSearchUI();
                return false; 
            }
            @Override public boolean longPressHelper(GeoPoint p) { return false; }
        });
        map.getOverlays().add(searchMapEventsOverlay);

        map.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                if (isMapFocusedOnUser || isMapFollowingHeading) {
                    isMapFocusedOnUser = false;
                    isMapFollowingHeading = false;
                    // 🔥 REMOVIDO: map.setMapOrientation(0); 
                    // Não resetamos para o Norte no toque, apenas paramos de seguir automaticamente
                    // para permitir que o usuário arraste ou gire o mapa livremente.
                    updateCenterFabIcon();
                    updateOrientationFabIcon();
                }
            }
            return false; // Retorna false para que o MapView processe os gestos (scroll, zoom, rotação)
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
                final float startX = btnToggleSearch.getX();

                params.width = RelativeLayout.LayoutParams.MATCH_PARENT;
                params.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE);
                params.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE);
                cardSearch.setLayoutParams(params);
                
                if (layoutSearchBalloonOuter != null) layoutSearchBalloonOuter.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
                if (layoutSearchBalloonInner != null) layoutSearchBalloonInner.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
                
                if (btnAddStopManual != null) btnAddStopManual.animate().alpha(0f).setDuration(150).withEndAction(() -> btnAddStopManual.setVisibility(View.GONE)).start();
                if (btnPackageOrganization != null) btnPackageOrganization.animate().alpha(0f).setDuration(150).withEndAction(() -> btnPackageOrganization.setVisibility(View.GONE)).start();
                if (btnRouteMenu != null) btnRouteMenu.animate().alpha(0f).setDuration(150).withEndAction(() -> btnRouteMenu.setVisibility(View.GONE)).start();
                if (layoutNotificationsContainer != null) layoutNotificationsContainer.animate().alpha(0f).setDuration(150).withEndAction(() -> layoutNotificationsContainer.setVisibility(View.GONE)).start();
                if (layoutNavMapIconContainer != null) layoutNavMapIconContainer.animate().alpha(0f).setDuration(150).withEndAction(() -> layoutNavMapIconContainer.setVisibility(View.GONE)).start();
                if (layoutOpenDrawerInside != null) layoutOpenDrawerInside.setVisibility(View.GONE);

                editSearch.setVisibility(View.VISIBLE);
                editSearch.setAlpha(0f);
                editSearch.animate().alpha(1f).setDuration(280).start();

                cardSearch.post(() -> {
                    float newLayoutX = btnToggleSearch.getX();
                    float initialOffsetX = startX - newLayoutX;
                    btnToggleSearch.setTranslationX(initialOffsetX);
                    btnToggleSearch.animate()
                            .translationX(0f)
                            .setDuration(300)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .start();
                });

                editSearch.requestFocus();

                InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(editSearch, InputMethodManager.SHOW_IMPLICIT);
            } else {
                String query = editSearch.getText().toString().trim();
                if (query.isEmpty()) {
                    closeSearchUI();
                } else {
                    searchAddress(query);
                }
            }
        });

        btnAddStopManual = view.findViewById(R.id.btnAddStopManual); 
        btnOpenDrawer = view.findViewById(R.id.btnOpenRoutesDrawerInside);
        layoutOpenDrawerInside = view.findViewById(R.id.layoutOpenDrawerInside);

        if (btnOpenDrawer instanceof ImageView) {
            Drawable d = ((ImageView) btnOpenDrawer).getDrawable();
            if (d instanceof AnimatedVectorDrawable) {
                AnimatedVectorDrawable avd = (AnimatedVectorDrawable) d;
                menuAnimRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (isAdded()) {
                            avd.start();
                            menuAnimHandler.postDelayed(this, 5000);
                        }
                    }
                };
            }
        }

        cardUndoCorrection = view.findViewById(R.id.cardUndoCorrection);
        textUndoCorrectionMessage = view.findViewById(R.id.textUndoCorrectionMessage);
        btnUndoCorrectionAction = view.findViewById(R.id.btnUndoCorrectionAction);

        layoutNotificationsContainer = view.findViewById(R.id.layoutNotificationsContainer);
        btnNotifications = view.findViewById(R.id.btnNotifications);
        badgeNotificationDot = view.findViewById(R.id.badgeNotificationDot);

        layoutNavMapIconContainer = view.findViewById(R.id.layoutNavMapIconContainer);
        btnNavMapIcon = view.findViewById(R.id.btnNavMapIcon);
        cardSearchNavDistance = view.findViewById(R.id.cardSearchNavDistance);
        textSearchNavDistance = view.findViewById(R.id.textSearchNavDistance);

        if (btnNavMapIcon != null) {
            btnNavMapIcon.setOnClickListener(v -> {
                if (switchTraceLine != null) {
                    boolean newState = !switchTraceLine.isChecked();
                    switchTraceLine.setChecked(newState);
                    updateNavMapIconState();
                    if (newState) {
                        Toast.makeText(getContext(), "🗺️ Linha de navegação ativada", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "🗺️ Linha de navegação desativada", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).showCommunityNotificationsDialog();
                }
            });
        }

        cardRestoreStopsSheet = view.findViewById(R.id.cardRestoreStopsSheet);
        if (cardRestoreStopsSheet != null) {
            cardRestoreStopsSheet.setOnClickListener(v -> {
                sharedPreferences.edit().putBoolean("show_bottom_sheet_stops", true).apply();
                updateFloatingButtonsVisibility();
                updateFabsPosition();
            });
        }

        fabCompass = view.findViewById(R.id.fabCompass);
        imageCompassInner = view.findViewById(R.id.imageCompassInner);
        
        if (imageCompassInner != null) {
            imageCompassInner.setImageDrawable(new BitmapDrawable(getResources(), generateCompassBitmap()));
        }

        if (fabCompass != null) {
            fabCompass.setOnClickListener(v -> {
                // Clique rápido reseta a orientação do mapa para o Norte se estiver perdido
                if (map != null) {
                    map.setMapOrientation(0);
                    isMapFollowingHeading = false;
                    updateOrientationFabIcon();
                    Toast.makeText(getContext(), "Mapa orientado ao Norte", Toast.LENGTH_SHORT).show();
                }
            });
            fabCompass.setOnLongClickListener(v -> {
                showCompassCalibrationDialog();
                return true;
            });
        }
        
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
        if (fabAddStop != null) fabAddStop.setOnClickListener(v -> showAddOptionDialog());
        if (fabCenterMap != null) fabCenterMap.setOnClickListener(v -> toggleMapFocus());
        fabMapOrientation = view.findViewById(R.id.fabMapOrientation);
        if (fabMapOrientation != null) {
            fabMapOrientation.setOnClickListener(v -> toggleMapOrientation());
            fabMapOrientation.setOnLongClickListener(v -> {
                promptHideFab("show_fab_orientation", "Modo Norte");
                return true;
            });
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

        // --- Balão de Otimização Pendente ---
        cardPendingOptimization = view.findViewById(R.id.cardPendingOptimization);
        btnFinishOptimizationNow = view.findViewById(R.id.btnFinishOptimizationNow);
        if (btnFinishOptimizationNow != null) {
            btnFinishOptimizationNow.setOnClickListener(v -> promptFinishOptimizationNow());
        }
        btnCancelAndDeleteOptimizationRoute = view.findViewById(R.id.btnCancelAndDeleteOptimizationRoute);
        if (btnCancelAndDeleteOptimizationRoute != null) {
            btnCancelAndDeleteOptimizationRoute.setOnClickListener(v -> promptCancelAndDeleteManualRoute());
        }

        // --- Balão de Lista em Edição ---
        cardPendingManualList = view.findViewById(R.id.cardPendingManualList);
        textPendingManualListTitle = view.findViewById(R.id.textPendingManualListTitle);
        textPendingManualListSubtitle = view.findViewById(R.id.textPendingManualListSubtitle);
        btnContinueManualListEditing = view.findViewById(R.id.btnContinueManualListEditing);
        if (btnContinueManualListEditing != null) {
            btnContinueManualListEditing.setOnClickListener(v -> {
                if (currentRouteId != -1) {
                    showManualListCreationDialog(currentRouteId, (currentRouteHeader != null && currentRouteHeader.name != null) ? currentRouteHeader.name : "Minha Rota");
                }
            });
        }
        btnCancelAndDeleteManualRoute = view.findViewById(R.id.btnCancelAndDeleteManualRoute);
        if (btnCancelAndDeleteManualRoute != null) {
            btnCancelAndDeleteManualRoute.setOnClickListener(v -> promptCancelAndDeleteManualRoute());
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
        if (btnAddStopManual != null) btnAddStopManual.setOnClickListener(v -> showAddOptionDialog());
        btnPackageOrganization = view.findViewById(R.id.btnPackageOrganization);
        if (btnPackageOrganization != null) btnPackageOrganization.setOnClickListener(v -> showPackageOrganizationDialog());
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
        cardRouteCorrections = view.findViewById(R.id.cardRouteCorrections);
        textRouteCorrections = view.findViewById(R.id.textRouteCorrections);
        if (cardRouteCorrections != null) {
            cardRouteCorrections.setOnClickListener(v -> showRouteCorrectionsDialog());
        }
        
        cleanupInvalidLocalFixes(getContext());

        AppDatabase.getInstance(requireContext()).appDao().getAllCorrectedAddressesLive()
            .observe(getViewLifecycleOwner(), caList -> updateRouteCorrectionsCount());

        textSheetHeader = view.findViewById(R.id.textSheetHeader);
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

        btnMuteVoiceNav = view.findViewById(R.id.btnMuteVoiceNav);
        updateMuteButtonIcon();
        if (btnMuteVoiceNav != null) {
            btnMuteVoiceNav.setOnClickListener(v -> {
                boolean currentState = isVoiceNavigationEnabled();
                setVoiceNavigationEnabled(!currentState, true);
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
        View btnCancelFix = view.findViewById(R.id.btnCancelFix);
        if (btnCancelFix != null) {
            btnCancelFix.setOnClickListener(v -> exitFixMode());
        }
        cardLassoMode = view.findViewById(R.id.cardLassoMode); cardRestWarning = view.findViewById(R.id.cardRestWarningRoute);
        cardNavigationMode = view.findViewById(R.id.cardNavigationMode);
        borderProgressDrawable = new BorderProgressDrawable(3.5f, 12f, Color.parseColor("#CCCCCC"), Color.parseColor("#2196F3"), requireContext());
        if (cardNavigationMode != null) {
            cardNavigationMode.setForeground(borderProgressDrawable);
        }

        editSearchStops = view.findViewById(R.id.editSearchStops);
        btnToggleSearchStops = view.findViewById(R.id.btnToggleSearchStops);
        if (btnToggleSearchStops != null) {
            btnToggleSearchStops.setOnClickListener(v -> {
                if (editSearchStops.getVisibility() == View.GONE) {
                    editSearchStops.setVisibility(View.VISIBLE);
                    textSheetHeader.setVisibility(View.GONE);
                    btnToggleSearchStops.setImageResource(R.drawable.ic_close);
                    editSearchStops.requestFocus();
                    InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(editSearchStops, InputMethodManager.SHOW_IMPLICIT);
                } else {
                    editSearchStops.setVisibility(View.GONE);
                    textSheetHeader.setVisibility(View.VISIBLE);
                    btnToggleSearchStops.setImageResource(android.R.drawable.ic_menu_search);
                    editSearchStops.setText("");
                    InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
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
            Log.d("DriveLog", "KonfettiView encontrado com sucesso!");
        } else {
            Log.e("DriveLog", "KonfettiView NÃO encontrado no layout!");
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
                                .setInterpolator(new Explode().getInterpolator())
                                .start();
                    } else {
                        textSwitchDistance.animate()
                                .translationX(19f * getResources().getDisplayMetrics().density) // Volta para o centro exato
                                .alpha(1f)
                                .setDuration(250)
                                .setInterpolator(new AccelerateInterpolator())
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

        if (bottomSheet != null) {
            bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

            requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    if (bottomSheetBehavior != null && bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    } else {
                        setEnabled(false);
                        requireActivity().getOnBackPressedDispatcher().onBackPressed();
                        setEnabled(true);
                    }
                }
            });

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
        stopsListAdapter = new StopsListAdapter(this, new ArrayList<>(), stop -> { 
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
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;
        
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onLost(@NonNull Network network) {
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
            Calendar n = Calendar.getInstance(); int nt = n.get(Calendar.HOUR_OF_DAY) * 60 + n.get(Calendar.MINUTE);
            return (st < et) ? (nt >= st && nt < et) : (nt >= st || nt < et);
        } catch (Exception ex) { return false; }
    }

    private void disableRestAndGoToSettings() { requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE).edit().putBoolean("rest_interval_enabled", false).apply(); checkRestInterval(); if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).openGeneralSettings(); }

    private void setupLocationOverlay() {
        if (map == null || getContext() == null) return;
        
        map.getOverlays().removeIf(o -> o instanceof MyLocationNewOverlay || o instanceof RotationGestureOverlay || (o instanceof Marker && "USER_DIR".equals(((Marker)o).getRelatedObject())));
        
        GpsMyLocationProvider provider = new GpsMyLocationProvider(requireContext());
        locationOverlay = new MyLocationNewOverlay(provider, map) {
            @Override
            public void onLocationChanged(Location location, IMyLocationProvider source) {
                super.onLocationChanged(location, source);
                Activity activity = getActivity();
                if (location != null && activity != null) {
                    activity.runOnUiThread(() -> {
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
                                String offsetKey = isMapFollowingHeading ? "compass_offset_follow" : "compass_offset_fixed";
                                float offset = sharedPreferences.getFloat(offsetKey, 0f);

                                if (isMapFollowingHeading) {
                                    // No modo Seguir Direção, o mapa gira e o boneco fica para cima (aplicando offset)
                                    userDirectionMarker.setRotation(offset);
                                    if (isMapFocusedOnUser && map != null) {
                                        map.setMapOrientation(-location.getBearing());
                                    }
                                } else {
                                    // No modo Norte Fixo, o boneco/seta gira com o rumo do GPS,
                                    // mas NÃO resetamos a rotação do mapa a cada ponto do GPS!
                                    // Isso permite que o motorista gire o mapa livremente sem que o mapa "flique" de volta pro Norte.
                                    userDirectionMarker.setRotation(location.getBearing());
                                }
                            }

                            Object lastIconRes = userDirectionMarker.getRelatedObject();
                            if (!(lastIconRes instanceof Integer) || (Integer) lastIconRes != targetRes) {
                                Bitmap bmp = drawableToBitmap(targetRes, false, 0);
                                userDirectionMarker.setIcon(new BitmapDrawable(getResources(), bmp));
                                userDirectionMarker.setRelatedObject(targetRes); 
                            }
                        }

                        if (isMapFocusedOnUser && mapController != null) {
                            mapController.animateTo(currentLocation);
                        }
                        checkHomeAutoPause(currentLocation);

                        // 🔥 Atualiza a linha azul de navegação conforme o motorista anda
                        if (currentlySelectedStop != null && switchTraceLine != null && switchTraceLine.isChecked()) {
                            updateNavigationLineProgress();
                        }

                        // 🔥 Atualiza o clima automaticamente ao mudar de cidade/posição (> 5km) ou após 30 min
                        boolean shouldUpdateWeather = false;
                        if (lastWeatherLocation == null) {
                            shouldUpdateWeather = true;
                        } else if (currentLocation.distanceToAsDouble(lastWeatherLocation) > 5000) {
                            shouldUpdateWeather = true;
                        } else if (System.currentTimeMillis() - lastWeatherUpdate > 1800000) {
                            shouldUpdateWeather = true;
                        }
                        if (shouldUpdateWeather) {
                            fetchWeather();
                        }
                    });
                }
            }
        };

        locationOverlay.setPersonIcon(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888));
        locationOverlay.setDirectionIcon(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888));
        locationOverlay.enableMyLocation();

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

    private Bitmap drawableToBitmap(int resId, boolean rotate, int tintColor) {
        Drawable d = ContextCompat.getDrawable(requireContext(), resId);
        int size = (int) (38 * getResources().getDisplayMetrics().density);
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        
        // 🔥 Removido o rotate fixo de -90 que entortava a seta original
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
        this.hasLeftHomeForRoute = false;
        
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
        EditText e = v.findViewById(R.id.editRouteName); String dstr = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
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
        v.findViewById(R.id.cardManualListCreation).setOnClickListener(v2 -> { 
            d.dismiss(); 
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showInterstitialThenAction(() -> createNewRouteWithManualList(name));
            } else {
                createNewRouteWithManualList(name);
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

    private void createNewRouteWithManualList(String name) {
        new Thread(() -> {
            RouteHeader h = new RouteHeader(name);
            long id = AppDatabase.getInstance(requireContext()).appDao().insertRouteHeader(h);
            Activity activity = getActivity();
            if (activity != null) activity.runOnUiThread(() -> {
                switchRoute((int) id, name);
                sharedPreferences.edit().putBoolean("route_manual_list_pending_" + id, true).apply();
                sharedPreferences.edit().putBoolean("show_bottom_sheet_stops", true).apply();
                showManualListCreationDialog((int) id, name);
                CloudSyncHelper.syncNow(requireContext(), "Nova Rota Manual");
            });
        }).start();
    }

    private void showManualListCreationDialog(int routeId, String routeName) {
        if (getContext() == null) return;

        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_manual_list_creation, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(v).setCancelable(false).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView textTitle = v.findViewById(R.id.textManualListTitle);
        if (textTitle != null) textTitle.setText("📝 Criar Lista de Paradas - " + routeName);

        EditText editAddress = v.findViewById(R.id.editManualAddress);
        EditText editCity = v.findViewById(R.id.editManualCity);
        EditText editSequence = v.findViewById(R.id.editManualSequence);

        if (editCity != null) {
            String detectedCity = getCurrentCityAndState();
            if (!detectedCity.isEmpty()) {
                editCity.setText(detectedCity);
            }
        }

        MaterialButton btnScanPhoto = v.findViewById(R.id.btnScanAddressFromPhoto);
        if (btnScanPhoto != null) {
            btnScanPhoto.setOnClickListener(v2 -> promptOcrSource(editAddress, editSequence));
        }

        MaterialButton btnAddStop = v.findViewById(R.id.btnAddManualStopToList);
        TextView textSavedHeader = v.findViewById(R.id.textSavedStopsHeader);
        RecyclerView recyclerPreview = v.findViewById(R.id.recyclerManualStopsPreview);
        MaterialButton btnCancel = v.findViewById(R.id.btnCancelManualListCreation);
        MaterialButton btnFinish = v.findViewById(R.id.btnFinishManualListCreation);

        List<RouteStop> savedStops = new ArrayList<>();
        Set<Integer> emptySelected = new HashSet<>();
        final ImportPreviewAdapter[] manualAdapterHolder = new ImportPreviewAdapter[1];

        ImportPreviewAdapter manualAdapter = new ImportPreviewAdapter(savedStops, emptySelected, new ImportPreviewAdapter.OnPreviewInteractionListener() {
            @Override public void onSelectionChanged() {}
            @Override
            public void onDelete(int position) {
                if (position >= 0 && position < savedStops.size()) {
                    RouteStop toRemove = savedStops.remove(position);
                    new Thread(() -> {
                        AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
                        dao.deleteRouteStop(toRemove);
                        List<RouteStop> current = dao.getStopsForRoute(routeId);
                        for (int i = 0; i < current.size(); i++) {
                            current.get(i).sortOrder = i;
                            current.get(i).stopNumber = i + 1;
                        }
                        dao.updateRouteStops(current);
                    }).start();

                    if (manualAdapterHolder[0] != null) manualAdapterHolder[0].notifyDataSetChanged();
                    int count = savedStops.size();
                    if (textTitle != null) textTitle.setText("📝 Criar Lista (" + count + " Paradas) - " + routeName);
                    if (textSavedHeader != null) textSavedHeader.setText("📋 Paradas Adicionadas (" + count + "):");
                    if (btnFinish != null) btnFinish.setText("Finalizar Lista e Iniciar Rota");
                }
            }
            @Override public void onStartDrag(RecyclerView.ViewHolder viewHolder) {}
        });
        manualAdapterHolder[0] = manualAdapter;

        if (recyclerPreview != null) {
            recyclerPreview.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerPreview.setAdapter(manualAdapter);
        }

        Runnable reloadSavedStops = () -> new Thread(() -> {
            AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
            List<RouteStop> stopsFromDb = dao.getStopsForRoute(routeId);
            Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(() -> {
                    savedStops.clear();
                    savedStops.addAll(stopsFromDb);
                    int count = savedStops.size();
                    if (manualAdapterHolder[0] != null) manualAdapterHolder[0].notifyDataSetChanged();
                    if (textTitle != null) {
                        textTitle.setText("📝 Criar Lista (" + count + " Paradas) - " + routeName);
                    }
                    if (textSavedHeader != null) {
                        textSavedHeader.setText("📋 Paradas Adicionadas (" + count + "):");
                    }
                    if (btnFinish != null) {
                        btnFinish.setText("Finalizar Lista e Iniciar Rota");
                    }
                });
            }
        }).start();

        reloadSavedStops.run();

        View.OnClickListener saveActionListener = v2 -> {
            String rawAddr = editAddress != null ? editAddress.getText().toString().trim() : "";
            String cityText = editCity != null ? editCity.getText().toString().trim() : "";
            String seqStr = editSequence != null ? editSequence.getText().toString().trim() : "";

            if (rawAddr.isEmpty()) {
                Toast.makeText(getContext(), "Digite o endereço do pacote.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (editAddress != null) editAddress.setText("");
            if (editSequence != null) editSequence.setText("");
            if (editAddress != null) editAddress.requestFocus();

            geocodeManualStopAndSave(routeId, rawAddr, seqStr, cityText, () -> {
                Toast.makeText(getContext(), "Parada salva no mapa!", Toast.LENGTH_SHORT).show();
                reloadSavedStops.run();
            });
        };

        if (btnAddStop != null) {
            btnAddStop.setOnClickListener(saveActionListener);
        }

        if (editSequence != null) {
            editSequence.setOnEditorActionListener((tv, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                    saveActionListener.onClick(tv);
                    return true;
                }
                return false;
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v2 -> {
                dialog.dismiss();
                loadStopsForCurrentRoute();
            });
        }

        if (btnFinish != null) {
            btnFinish.setOnClickListener(v2 -> {
                dialog.dismiss();
                sharedPreferences.edit().remove("route_manual_list_pending_" + routeId).apply();
                if (!savedStops.isEmpty()) {
                    sharedPreferences.edit().putBoolean("route_optimization_pending_" + routeId, true).apply();
                    showImportPreviewDialog(routeId, new ArrayList<>(savedStops));
                } else {
                    loadStopsForCurrentRoute();
                }
            });
        }

        dialog.show();
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
            Set<RouteStop> selected = stopsListAdapter.getSelectedStops();
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
            Set<String> uniqueBuyers = new HashSet<>();
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
                Collections.swap(currentStops, from, to);
                
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

    private void shareCurrentRouteWithDevs() { new Thread(() -> { AppDao dao = AppDatabase.getInstance(requireContext()).appDao(); RouteHeader h = dao.getRouteById(currentRouteId); List<RouteStop> ss = dao.getStopsForRoute(currentRouteId); FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser(); if (u != null && h != null && !ss.isEmpty()) FirebaseHelper.shareRouteWithDevelopers(u.getEmail(), u.getDisplayName(), h, ss, new FirebaseHelper.GlobalUploadCallback() { @Override public void onSuccess() { Activity activity = getActivity(); if (activity != null) activity.runOnUiThread(() -> Toast.makeText(getContext(), "Sucesso!", Toast.LENGTH_SHORT).show()); } @Override public void onFailure(String m) {} }); }).start(); }

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
        RecyclerView recycler = v.findViewById(R.id.recyclerStatsStopsList);
        TextView textEmpty = v.findViewById(R.id.textStatsStopsListEmpty);
        View layoutPackages = v.findViewById(R.id.layoutStatsPackages);

        int okStops = 0, okPkgs = 0, errStops = 0, pendStops = 0, pendPkgs = 0;
        List<RouteStop> matchedStops = new ArrayList<>();
        
        for (int i = 0; i < currentStops.size(); i++) {
            RouteStop s = currentStops.get(i);
            boolean match = false;
            if (type == 1 && s.deliveryStatus == 1) { okStops++; okPkgs += s.packageCount; match = true; }
            else if (type == 2 && s.deliveryStatus == 2) { errStops++; match = true; }
            else if (type == 0 && s.deliveryStatus == 0) { pendStops++; pendPkgs += s.packageCount; match = true; }

            if (match) {
                matchedStops.add(s);
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

        if (!matchedStops.isEmpty()) {
            if (recycler != null) {
                recycler.setVisibility(View.VISIBLE);
                recycler.setLayoutManager(new LinearLayoutManager(getContext()));
                recycler.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
                        View itemView = LayoutInflater.from(p.getContext()).inflate(R.layout.item_dialog_stop_info, p, false);
                        return new RecyclerView.ViewHolder(itemView) {};
                    }

                    @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
                        RouteStop s = matchedStops.get(pos);
                        TextView tNum = h.itemView.findViewById(R.id.textDialogStopNumber);
                        TextView tAddr = h.itemView.findViewById(R.id.textDialogStopAddress);
                        TextView tSeq = h.itemView.findViewById(R.id.textDialogStopSeq);
                        TextView btnLoc = h.itemView.findViewById(R.id.btnDialogVehicleLocation);

                        if (tNum != null) tNum.setText(String.valueOf(s.stopNumber));
                        if (tAddr != null) tAddr.setText(s.address);
                        if (tSeq != null) tSeq.setText(getFormattedSeqInfo(s));

                        if (btnLoc != null) {
                            if (s.vehicleLocation != null && !s.vehicleLocation.trim().isEmpty()) {
                                btnLoc.setText("📍 " + s.vehicleLocation + " ▾");
                            } else {
                                btnLoc.setText("📍 Local ▾");
                            }
                            btnLoc.setOnClickListener(btnV -> {
                                showVehicleLocationPicker(s, btnV);
                                if (tSeq != null) tSeq.setText(getFormattedSeqInfo(s));
                            });
                        }
                    }

                    @Override public int getItemCount() { return matchedStops.size(); }
                });
            }
            if (textEmpty != null) textEmpty.setVisibility(View.GONE);
        } else {
            if (recycler != null) recycler.setVisibility(View.GONE);
            if (textEmpty != null) textEmpty.setVisibility(View.VISIBLE);
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

    private void startRouteTimerManually() {
        if (getContext() == null) return;
        final int targetId = (currentRouteHeader != null && currentRouteHeader.id > 0) ? currentRouteHeader.id : currentRouteId;
        if (targetId <= 0) return;

        long now = System.currentTimeMillis();
        hasLeftHomeForRoute = false;

        if (currentRouteHeader != null) {
            currentRouteHeader.startTime = now;
            currentRouteHeader.totalPausedMs = 0;
            currentRouteHeader.lastPauseStartTime = 0;
            currentRouteHeader.endTime = 0;
            currentRouteHeader.isCompleted = false;
        }

        Toast.makeText(getContext(), "🚀 Rota iniciada!", Toast.LENGTH_SHORT).show();
        timerHandler.removeCallbacks(timerRunnable);
        timerHandler.post(timerRunnable);

        new Thread(() -> {
            AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
            RouteHeader header = dao.getRouteById(targetId);
            if (header != null) {
                header.startTime = now;
                header.totalPausedMs = 0;
                header.lastPauseStartTime = 0;
                header.endTime = 0;
                header.isCompleted = false;
                dao.updateRouteHeader(header);
            }
        }).start();
    }

    private void promptResetRoute() {
        if (getContext() == null || currentRouteId == -1) return;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_modern_confirm, null);
        TextView title = dialogView.findViewById(R.id.textModernTitle);
        TextView message = dialogView.findViewById(R.id.textModernMessage);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnModernNegative);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btnModernPositive);

        if (title != null) title.setText("Reiniciar Rota");
        if (message != null) message.setText("Deseja zerar todo o tempo e reiniciar todas as paradas desta rota para o estado pendente?");
        if (btnConfirm != null) btnConfirm.setText("REINICIAR");

        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(dialogView).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                dialog.dismiss();
                resetEntireRoute();
            });
        }

        dialog.show();
    }

    private void resetEntireRoute() {
        if (getContext() == null || currentRouteId == -1) return;
        hasLeftHomeForRoute = false;

        new Thread(() -> {
            AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
            RouteHeader header = dao.getRouteById(currentRouteId);
            if (header != null) {
                header.startTime = 0;
                header.endTime = 0;
                header.totalPausedMs = 0;
                header.lastPauseStartTime = 0;
                header.isCompleted = false;
                dao.updateRouteHeader(header);
            }

            if (currentRouteHeader != null) {
                currentRouteHeader.startTime = 0;
                currentRouteHeader.endTime = 0;
                currentRouteHeader.totalPausedMs = 0;
                currentRouteHeader.lastPauseStartTime = 0;
                currentRouteHeader.isCompleted = false;
            }

            // Reseta o status de todas as paradas da rota para 0 (Pendente)
            if (currentStops != null) {
                for (RouteStop s : currentStops) {
                    s.deliveryStatus = 0;
                    s.deliveryTimestamp = 0;
                    dao.updateRouteStop(s);
                }
            }

            String celebrationKey = "last_finished_route_" + currentRouteId;
            sharedPreferences.edit().remove(celebrationKey).apply();

            Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(() -> {
                    if (textRouteTotalTime != null) textRouteTotalTime.setText("Iniciar Rota");
                    if (stopsCardAdapter != null) stopsCardAdapter.notifyDataSetChanged();
                    if (stopsListAdapter != null) stopsListAdapter.notifyDataSetChanged();
                    refreshMarkers();
                    flashStatsSummary(cardPendingSummary);
                    CloudSyncHelper.syncNow(requireContext(), "Rota Reiniciada");
                    Toast.makeText(getContext(), "🔄 Rota reiniciada com sucesso!", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

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

            // Remocao garantida de qualquer overlay antigo do raio de casa
            map.getOverlays().removeIf(o -> o instanceof Polygon
                    && "HOME_RADIUS".equals(((Polygon) o).getTitle()));

            int triggerRadius = sharedPreferences.getInt("home_trigger_radius", 100);
            homeRadiusOverlay = new Polygon(map);
            homeRadiusOverlay.setTitle("HOME_RADIUS");
            homeRadiusOverlay.setPoints(Polygon.pointsAsCircle(homePoint, triggerRadius));
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
            map.getOverlays().removeIf(overlay -> overlay instanceof Polygon && ((Polygon)overlay).getTitle() != null && ((Polygon)overlay).getTitle().startsWith("LoadingRadius:"));
            map.invalidate();
            return;
        }

        loadLoadingPoints();
        
        for (Marker m : loadingMarkers) {
            map.getOverlays().remove(m);
        }
        loadingMarkers.clear();

        map.getOverlays().removeIf(overlay -> {
            if (overlay instanceof Polygon) {
                Polygon p = (Polygon) overlay;
                return p.getTitle() != null && p.getTitle().startsWith("LoadingRadius:");
            }
            return false;
        });

        int loadingRadius = sharedPreferences.getInt("loading_base_radius", 100);

        for (LoadingPoint lp : loadingPoints) {
            GeoPoint point = new GeoPoint(lp.latitude, lp.longitude);
            
            Polygon circle = new Polygon(map);
            circle.setPoints(Polygon.pointsAsCircle(point, loadingRadius));
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
        EditText input = new EditText(getContext());
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
        if (loc == null || currentRouteHeader == null || currentRouteHeader.startTime == 0 || currentRouteHeader.endTime > 0) return;
        
        // Só ativa a pausa automática por estar em casa se pelo menos 1 parada da rota tiver sido entregue
        boolean hasDeliveredStops = false;
        if (currentStops != null) {
            for (RouteStop s : currentStops) {
                if (s.deliveryStatus == 1) {
                    hasDeliveredStops = true;
                    break;
                }
            }
        }
        if (!hasDeliveredStops) return;

        float homeLat = sharedPreferences.getFloat("home_lat", 0);
        float homeLon = sharedPreferences.getFloat("home_lon", 0);
        if (homeLat == 0) return;
        
        GeoPoint home = new GeoPoint(homeLat, homeLon);
        double dist = loc.distanceToAsDouble(home);
        int radius = sharedPreferences.getInt("home_trigger_radius", 100);

        // Se o motorista se afastar de casa durante a rota, marca que ele já saiu de casa
        if (dist >= radius) {
            hasLeftHomeForRoute = true;
        }

        // A pausa automática por estar em casa SÓ deve acontecer se o motorista JÁ TIVER SAÍDO de casa pelo menos uma vez durante a rota.
        // Isso evita que o cronômetro congele se o motorista iniciar a rota ou testar entregas estando em casa.
        if (!hasLeftHomeForRoute) {
            return;
        }
        
        long now = System.currentTimeMillis();
        boolean updated = false;
        
        if (dist < radius) {
            if (currentRouteHeader.lastPauseStartTime == 0) {
                currentRouteHeader.lastPauseStartTime = now;
                updated = true;
                Log.d("DriveLog", "[Timer] Pausado automaticamente (Chegou em casa)");
            }
        } else {
            if (currentRouteHeader.lastPauseStartTime > 0) {
                long pauseDuration = now - currentRouteHeader.lastPauseStartTime;
                currentRouteHeader.totalPausedMs += pauseDuration;
                currentRouteHeader.lastPauseStartTime = 0;
                updated = true;
                Log.d("DriveLog", "[Timer] Retomado automaticamente (Saiu de casa)");
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
        
        // 🔥 NOVO: Ao trocar de modo, forçamos o azimute a ler os novos valores de calibração imediatamente
        // para evitar o efeito "elástico" (smooth rotation) lento.
        currentAzimuth = -1; // Flag para forçar atualização no próximo sensor event ou aqui
        
        if (!isMapFollowingHeading) {
            map.setMapOrientation(0); // Reseta para o Norte
        } else {
            // Ao ativar, já pega a orientação atual do sensor se disponível
            map.setMapOrientation(-currentAzimuth);
        }
        updateOrientationFabIcon();
        Toast.makeText(getContext(), isMapFollowingHeading ? "Seguindo direção" : "Norte fixo", Toast.LENGTH_SHORT).show();
    }

    private void updateOrientationFabIcon() {
        if (fabMapOrientation != null) {
            fabMapOrientation.setImageResource(isMapFollowingHeading ? android.R.drawable.ic_menu_compass : android.R.drawable.ic_menu_directions);
            fabMapOrientation.setSupportImageTintList(ColorStateList.valueOf(
                isMapFollowingHeading ? Color.parseColor("#F44336") : ContextCompat.getColor(requireContext(), R.color.teal_700)
            ));
        }
    }

    private void updateCenterFabIcon() { if (fabCenterMap!=null) fabCenterMap.setImageResource(isMapFocusedOnUser ? android.R.drawable.ic_menu_myplaces : R.drawable.ic_my_location); }

    private void fetchSuggestions(String q) { new Thread(() -> { try { 
        String uniqueId = Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        String userAgent = "DriveLogApp_v1527_" + uniqueId;
        String u = String.format(Locale.US, "https://nominatim.openstreetmap.org/search?q=%s&format=json&limit=5", URLEncoder.encode(q, StandardCharsets.UTF_8.name()));
        HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection(); 
        c.setRequestProperty("User-Agent", userAgent); 
        BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream())); StringBuilder res = new StringBuilder(); String l; while((l=r.readLine())!=null) res.append(l); JSONArray a = new JSONArray(res.toString()); List<Suggestion> sl = new ArrayList<>(); for(int i=0; i<a.length(); i++) { JSONObject o = a.getJSONObject(i); sl.add(new Suggestion(o.getString("display_name"), o.getDouble("lat"), o.getDouble("lon"))); } Activity activity = getActivity(); if (activity != null) activity.runOnUiThread(() -> { suggestionsAdapter.setSuggestions(sl); recyclerSuggestions.setVisibility(sl.isEmpty() ? View.GONE : View.VISIBLE); }); } catch(Exception ignored){} }).start(); }

    private void onSuggestionClicked(Suggestion s) { isSelectingSuggestion = true; recyclerSuggestions.setVisibility(View.GONE); editSearch.setText(s.displayName); lastSearchedPoint = new GeoPoint(s.lat, s.lon); lastSearchedAddress = s.displayName; if (mapController!=null) { mapController.animateTo(lastSearchedPoint); mapController.setZoom(18.0); } showTempMarker(lastSearchedPoint, lastSearchedAddress); new Handler(Looper.getMainLooper()).postDelayed(() -> { if (isAdded()) { confirmAddStop(); isSelectingSuggestion = false; } }, 600); }

    private void showAddOptionDialog() {
        if (getContext() == null) return;
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_option_selection, null);
        
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        View cardAddStop = dialogView.findViewById(R.id.cardOptionAddStop);
        View cardAddQuadra = dialogView.findViewById(R.id.cardOptionAddQuadra);
        View cardAddBloco = dialogView.findViewById(R.id.cardOptionAddBloco);
        View btnCancel = dialogView.findViewById(R.id.btnCancelOptionAdd);

        if (cardAddStop != null) {
            cardAddStop.setOnClickListener(v -> {
                dialog.dismiss();
                promptManualStop();
            });
        }

        if (cardAddQuadra != null) {
            cardAddQuadra.setOnClickListener(v -> {
                dialog.dismiss();
                startQuadraSelection("QUADRA");
            });
        }

        if (cardAddBloco != null) {
            cardAddBloco.setOnClickListener(v -> {
                dialog.dismiss();
                startQuadraSelection("BLOCO");
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private MapEventsOverlay quadraSelectionOverlay;

    private void startQuadraSelection(String itemType) {
        String label = "BLOCO".equalsIgnoreCase(itemType) ? "Bloco" : "Quadra";
        Toast.makeText(requireContext(), "Toque no local do mapa onde fica o(a) " + label, Toast.LENGTH_LONG).show();

        if (quadraSelectionOverlay != null && map != null) {
            map.getOverlays().remove(quadraSelectionOverlay);
        }

        quadraSelectionOverlay = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                if (map != null && quadraSelectionOverlay != null) {
                    map.getOverlays().remove(quadraSelectionOverlay);
                    quadraSelectionOverlay = null;
                    map.invalidate();
                }
                promptAddQuadraDialog(p, itemType);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        });

        if (map != null) {
            map.getOverlays().add(quadraSelectionOverlay);
            map.invalidate();
        }
    }

    private String getUserDisplayName() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
            return user.getDisplayName().trim();
        }
        if (getContext() != null) {
            String prefName = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
                    .getString("user_display_name", "");
            if (!prefName.trim().isEmpty()) return prefName.trim();
        }
        if (user != null && user.getEmail() != null && !user.getEmail().isEmpty()) {
            return user.getEmail().split("@")[0];
        }
        return "Entregador";
    }

    private void promptAddQuadraDialog(GeoPoint p, String itemType) {
        if (p == null || getContext() == null) return;
        boolean isBloco = "BLOCO".equalsIgnoreCase(itemType);

        View v = getLayoutInflater().inflate(R.layout.dialog_add_quadra, null);
        TextView textDialogTitle = v.findViewById(R.id.textDialogAddQuadraTitle);
        EditText editName = v.findViewById(R.id.editDialogQuadraName);
        EditText editNeighborhood = v.findViewById(R.id.editDialogQuadraNeighborhood);
        EditText editCity = v.findViewById(R.id.editDialogQuadraCity);
        EditText editNotes = v.findViewById(R.id.editDialogQuadraNotes);

        MaterialButton btnSave = v.findViewById(R.id.btnSaveAddQuadra);
        MaterialButton btnCancel = v.findViewById(R.id.btnCancelAddQuadra);

        if (textDialogTitle != null) {
            textDialogTitle.setText(isBloco ? "🏢 Adicionar Bloco" : "🧱 Adicionar Quadra");
        }
        if (editName != null) {
            editName.setHint(isBloco ? "Nome do Bloco (Ex: Bloco B, Ed. Solar)" : "Nome da Quadra (Ex: 104 Norte, QD 12)");
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(v)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // 🔥 Auto-preenchimento do Bairro e Cidade via Geocoding em segundo plano
        if (editNeighborhood != null && editCity != null) {
            new Thread(() -> {
                try {
                    String uniqueId = Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID);
                    String userAgent = "DriveLogApp_v1527_" + uniqueId;
                    String urlStr = String.format(Locale.US, "https://nominatim.openstreetmap.org/reverse?lat=%.6f&lon=%.6f&format=json", p.getLatitude(), p.getLongitude());

                    HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
                    c.setRequestProperty("User-Agent", userAgent);
                    if (c.getResponseCode() == 200) {
                        BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
                        StringBuilder res = new StringBuilder();
                        String l;
                        while ((l = r.readLine()) != null) res.append(l);
                        JSONObject json = new JSONObject(res.toString());
                        if (json.has("address")) {
                            JSONObject addr = json.getJSONObject("address");
                            String neigh = addr.optString("suburb", addr.optString("neighbourhood", addr.optString("district", "")));
                            String city = addr.optString("city", addr.optString("town", addr.optString("municipality", "")));

                            Activity act = getActivity();
                            if (act != null) {
                                act.runOnUiThread(() -> {
                                    if (editNeighborhood != null && !neigh.isEmpty()) {
                                        editNeighborhood.setText(neigh);
                                    }
                                    if (editCity != null && !city.isEmpty()) {
                                        editCity.setText(city);
                                    }
                                });
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }).start();
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(view -> dialog.dismiss());
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(view -> {
                String name = editName != null ? editName.getText().toString().trim() : "";
                String neighborhood = editNeighborhood != null ? editNeighborhood.getText().toString().trim() : "";
                String city = editCity != null ? editCity.getText().toString().trim() : "";
                String notes = editNotes != null ? editNotes.getText().toString().trim() : "";

                if (name.isEmpty()) {
                    Toast.makeText(getContext(), isBloco ? "Digite o nome do Bloco" : "Digite o nome da Quadra", Toast.LENGTH_SHORT).show();
                    return;
                }

                dialog.dismiss();

                new Thread(() -> {
                    String currentUserId = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
                            .getString("current_user_id", "anon");
                    String currentUserName = getUserDisplayName();

                    CorrectedQuadra quadra = new CorrectedQuadra(name, neighborhood, city, p.getLatitude(), p.getLongitude());
                    quadra.notes = notes;
                    quadra.creatorId = currentUserId;
                    quadra.creatorName = currentUserName;
                    quadra.type = isBloco ? "BLOCO" : "QUADRA";

                    AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
                    dao.insertCorrectedQuadra(quadra);

                    FirebaseHelper.uploadQuadra(currentUserId, currentUserName, quadra, null);

                    Activity act = getActivity();
                    if (act != null) {
                        act.runOnUiThread(() -> {
                            String itemLabel = isBloco ? "🏢 Bloco '" : "🧱 Quadra '";
                            Toast.makeText(getContext(), itemLabel + name + "' cadastrado(a) com sucesso!", Toast.LENGTH_LONG).show();
                            refreshQuadraMarkers();
                        });
                    }
                }).start();
            });
        }

        dialog.show();
    }

    public void focusOnQuadra(double lat, double lon, String quadraName) {
        if (mapController != null && lat != 0 && lon != 0) {
            GeoPoint gp = new GeoPoint(lat, lon);
            mapController.animateTo(gp);
            mapController.setZoom(18.5);
            Toast.makeText(getContext(), "📍 Exibindo Quadra: " + quadraName, Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshQuadraMarkers() {
        if (map == null || !isAdded()) return;

        new Thread(() -> {
            AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
            List<CorrectedQuadra> localQuadras = dao.getAllCorrectedQuadras();

            FirebaseHelper.getAllGlobalQuadras(new FirebaseHelper.GlobalQuadrasCallback() {
                @Override
                public void onResult(List<CorrectedQuadra> globalQuadras) {
                    List<CorrectedQuadra> combined = new ArrayList<>(localQuadras);
                    if (globalQuadras != null) {
                        for (CorrectedQuadra gq : globalQuadras) {
                            boolean exists = false;
                            for (CorrectedQuadra lq : localQuadras) {
                                if (lq.name != null && lq.name.equalsIgnoreCase(gq.name) &&
                                    (lq.neighborhood == null || gq.neighborhood == null || lq.neighborhood.equalsIgnoreCase(gq.neighborhood))) {
                                    lq.likes = gq.likes;
                                    lq.dislikes = gq.dislikes;
                                    if (gq.docId != null) lq.docId = gq.docId;
                                    if (gq.notes != null && !gq.notes.isEmpty()) lq.notes = gq.notes;
                                    if (gq.creatorId != null) lq.creatorId = gq.creatorId;
                                    if (gq.creatorName != null) lq.creatorName = gq.creatorName;
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) combined.add(gq);
                        }
                    }

                    Activity act = getActivity();
                    if (act != null) {
                        act.runOnUiThread(() -> {
                            if (map == null || !isAdded()) return;

                            map.getOverlays().removeIf(o -> o instanceof Marker && ((Marker) o).getRelatedObject() instanceof CorrectedQuadra);

                            double zoom = map.getZoomLevelDouble();
                            int tier;
                            if (zoom < 14.0) {
                                tier = 0;
                            } else if (zoom < 16.0) {
                                tier = 1;
                            } else {
                                tier = 2;
                            }
                            currentQuadraZoomTier = tier;

                            for (CorrectedQuadra q : combined) {
                                if (q.latitude == 0 || q.longitude == 0) continue;

                                boolean isBloco = "BLOCO".equalsIgnoreCase(q.type);

                                Marker m = new Marker(map);
                                m.setRelatedObject(q);
                                m.setPosition(new GeoPoint(q.latitude, q.longitude));
                                String prefix = isBloco ? "🏢 " : "🧱 ";
                                m.setTitle(prefix + q.name + (q.neighborhood != null && !q.neighborhood.isEmpty() ? " (" + q.neighborhood + ")" : ""));

                                Bitmap bmp = generateQuadraMarkerBitmap(q.name, tier, q.likes, q.dislikes, isBloco);
                                m.setIcon(new BitmapDrawable(getResources(), bmp));

                                m.setOnMarkerClickListener((marker, mapView) -> {
                                    showQuadraDetailsDialog(q);
                                    return true;
                                });

                                map.getOverlays().add(m);
                            }
                            map.invalidate();
                        });
                    }
                }

                @Override
                public void onError(String message) {}
            });
        }).start();
    }

    private int currentQuadraZoomTier = -1;

    private void updateQuadraMarkerSizes() {
        if (map == null || !isAdded()) return;
        double zoom = map.getZoomLevelDouble();
        int tier;
        if (zoom < 14.0) {
            tier = 0; // Muito longe: pontinho discreto
        } else if (zoom < 16.0) {
            tier = 1; // Médio afastamento: badge compacto
        } else {
            tier = 2; // Zoom próximo: badge completo
        }

        if (tier != currentQuadraZoomTier) {
            currentQuadraZoomTier = tier;
            for (Overlay o : map.getOverlays()) {
                if (o instanceof Marker && ((Marker) o).getRelatedObject() instanceof CorrectedQuadra) {
                    Marker m = (Marker) o;
                    CorrectedQuadra q = (CorrectedQuadra) m.getRelatedObject();
                    boolean isBloco = "BLOCO".equalsIgnoreCase(q.type);
                    Bitmap bmp = generateQuadraMarkerBitmap(q.name, tier, q.likes, q.dislikes, isBloco);
                    m.setIcon(new BitmapDrawable(getResources(), bmp));
                }
            }
            map.invalidate();
        }
    }

    private void updateSingleQuadraMarkerIcon(CorrectedQuadra quadra) {
        if (map == null || quadra == null) return;
        int tier = currentQuadraZoomTier >= 0 ? currentQuadraZoomTier : 2;
        for (Overlay o : map.getOverlays()) {
            if (o instanceof Marker && ((Marker) o).getRelatedObject() instanceof CorrectedQuadra) {
                Marker m = (Marker) o;
                CorrectedQuadra q = (CorrectedQuadra) m.getRelatedObject();
                if (q.name != null && q.name.equalsIgnoreCase(quadra.name)) {
                    q.likes = quadra.likes;
                    q.dislikes = quadra.dislikes;
                    boolean isBloco = "BLOCO".equalsIgnoreCase(q.type);
                    Bitmap bmp = generateQuadraMarkerBitmap(q.name, tier, q.likes, q.dislikes, isBloco);
                    m.setIcon(new BitmapDrawable(getResources(), bmp));
                    break;
                }
            }
        }
        map.invalidate();
    }

    private Bitmap generateQuadraMarkerBitmap(String name, int tier, int likes, int dislikes, boolean isBloco) {
        // Ícone de Bloco deve ser menor que o da Quadra
        int baseScale = isBloco ? 12 : 0;
        int size = (tier == 0) ? (14 - (isBloco ? 2 : 0)) : (tier == 1 ? (28 - (isBloco ? 2 : 0)) : (56 - baseScale));

        if (getContext() != null) {
            String scaleKey = isBloco ? "bloco_icon_size" : "quadra_icon_size";
            int scalePercent = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
                    .getInt(scaleKey, 100);
            size = Math.round(size * (scalePercent / 100f));
        }

        if (size < 10) size = 10;

        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Define a cor com base na confiabilidade
        int mainColor;
        int totalVotes = likes + dislikes;
        if (totalVotes == 0) {
            mainColor = Color.parseColor("#F57C00"); // Laranja/Âmbar (Em avaliação)
        } else {
            double ratio = (double) likes / totalVotes;
            if (ratio >= 0.75) {
                mainColor = Color.parseColor("#388E3C"); // Verde (Alta confiabilidade)
            } else if (ratio >= 0.40) {
                mainColor = Color.parseColor("#E65100"); // Laranja (Média confiabilidade)
            } else {
                mainColor = Color.parseColor("#D32F2F"); // Vermelho (Baixa confiabilidade)
            }
        }

        p.setColor(mainColor);
        p.setStyle(Paint.Style.FILL);

        float cx = size / 2f;
        float cy = size / 2f;

        if (tier == 0) {
            // Apenas uma forma discreta com borda branca
            if (isBloco) {
                Path hex = createHexagonPath(cx, cy, (size / 2f) - 1f);
                c.drawPath(hex, p);
                p.setColor(Color.WHITE);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(1.2f);
                c.drawPath(hex, p);
            } else {
                c.drawCircle(cx, cy, (size / 2f) - 1f, p);
                p.setColor(Color.WHITE);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(1.2f);
                c.drawCircle(cx, cy, (size / 2f) - 1f, p);
            }
        } else if (tier == 1) {
            // Hexágono ou quadrado compacto arredondado com mini ponto central
            if (isBloco) {
                Path hex = createHexagonPath(cx, cy, (size / 2f) - 1.5f);
                c.drawPath(hex, p);
                p.setColor(Color.WHITE);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(1.5f);
                c.drawPath(hex, p);
            } else {
                float rx = 5f;
                c.drawRoundRect(new RectF(1.5f, 1.5f, size - 1.5f, size - 1.5f), rx, rx, p);
                p.setColor(Color.WHITE);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(1.5f);
                c.drawRoundRect(new RectF(1.5f, 1.5f, size - 1.5f, size - 1.5f), rx, rx, p);
            }

            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.WHITE);
            c.drawCircle(cx, cy, 2.5f, p);
        } else {
            // Zoom detalhado: Hexágono para Bloco ou Rounded Rect para Quadra
            if (isBloco) {
                Path hex = createHexagonPath(cx, cy, (size / 2f) - 2f);
                c.drawPath(hex, p);
                p.setColor(Color.WHITE);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(2f);
                c.drawPath(hex, p);
            } else {
                float rx = 10f;
                c.drawRoundRect(new RectF(2, 2, size - 2, size - 2), rx, rx, p);
                p.setColor(Color.WHITE);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(2f);
                c.drawRoundRect(new RectF(2, 2, size - 2, size - 2), rx, rx, p);
            }

            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.WHITE);
            p.setTextSize(isBloco ? 15f : 18f);
            p.setTextAlign(Paint.Align.CENTER);
            String text = (name != null && name.length() > 5) ? name.substring(0, 5) : (name != null ? name : "B");
            Paint.FontMetrics fm = p.getFontMetrics();
            float textY = cy - (fm.ascent + fm.descent) / 2f;
            c.drawText(text, cx, textY, p);
        }

        return b;
    }

    private Path createHexagonPath(float cx, float cy, float r) {
        Path path = new Path();
        for (int i = 0; i < 6; i++) {
            double angleRad = Math.toRadians(30 + i * 60);
            float x = (float) (cx + r * Math.cos(angleRad));
            float y = (float) (cy + r * Math.sin(angleRad));
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();
        return path;
    }

    public void focusQuadraFromNotification(String name, String neighborhood, double lat, double lon, String type) {
        if (mapController != null && lat != 0 && lon != 0) {
            mapController.setZoom(17.5);
            mapController.animateTo(new GeoPoint(lat, lon));
            isMapFocusedOnUser = false;
            updateCenterFabIcon();
        }
        CorrectedQuadra q = new CorrectedQuadra(name, neighborhood, "", lat, lon);
        q.type = type;
        showQuadraDetailsDialog(q);
    }

    private void showQuadraDetailsDialog(CorrectedQuadra q) {
        if (q == null || getContext() == null) return;

        View v = getLayoutInflater().inflate(R.layout.dialog_quadra_details, null);
        TextView textTitle = v.findViewById(R.id.textQuadraTitle);
        TextView textLocation = v.findViewById(R.id.textQuadraLocation);
        TextView textNotes = v.findViewById(R.id.textQuadraNotes);
        TextView textCreatorDetail = v.findViewById(R.id.textQuadraCreatorDetail);

        MaterialButton btnLike = v.findViewById(R.id.btnQuadraLike);
        MaterialButton btnDislike = v.findViewById(R.id.btnQuadraDislike);
        MaterialButton btnDelete = v.findViewById(R.id.btnDeleteQuadraDialog);

        RecyclerView recyclerComments = v.findViewById(R.id.recyclerQuadraComments);
        TextView textNoComments = v.findViewById(R.id.textNoQuadraComments);
        EditText editCommentInput = v.findViewById(R.id.editQuadraCommentInput);
        ImageButton btnSendComment = v.findViewById(R.id.btnSendQuadraComment);

        boolean isBloco = "BLOCO".equalsIgnoreCase(q.type);
        if (textTitle != null) textTitle.setText((isBloco ? "🏢 " : "🧱 ") + (q.name != null ? q.name : (isBloco ? "Bloco" : "Quadra")));

        StringBuilder locBuilder = new StringBuilder();
        if (q.neighborhood != null && !q.neighborhood.isEmpty()) locBuilder.append(q.neighborhood);
        if (q.city != null && !q.city.isEmpty()) {
            if (locBuilder.length() > 0) locBuilder.append(", ");
            locBuilder.append(q.city);
        }
        if (textLocation != null) textLocation.setText(locBuilder.length() > 0 ? locBuilder.toString() : "Localização cadastrada");

        if (textNotes != null) {
            if (q.notes != null && !q.notes.isEmpty()) {
                textNotes.setText("Observação: " + q.notes);
                textNotes.setVisibility(View.VISIBLE);
            } else {
                textNotes.setVisibility(View.GONE);
            }
        }

        String creatorName = (q.creatorName != null && !q.creatorName.isEmpty()) ? q.creatorName : "Comunidade";
        if (textCreatorDetail != null) {
            textCreatorDetail.setText("Adicionado por: " + creatorName);
        }

        String currentUserId = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
                .getString("current_user_id", "anon");
        String currentUserName = getUserDisplayName();

        boolean isMine = (q.creatorId != null && q.creatorId.equals(currentUserId));
        if (btnDelete != null) {
            btnDelete.setText(isBloco ? "🗑️ Excluir este Bloco" : "🗑️ Excluir esta Quadra");
            btnDelete.setVisibility(isMine ? View.VISIBLE : View.GONE);
        }

        final int[] likesCount = {q.likes};
        final int[] dislikesCount = {q.dislikes};
        final Boolean[] userVote = {null};
        final boolean[] hasUserInteracted = {false};

        TextView textReliability = v.findViewById(R.id.textQuadraReliability);
        Runnable updateReliabilityBadge = () -> {
            if (textReliability == null) return;
            int likes = likesCount[0];
            int dislikes = dislikesCount[0];
            int totalVotes = likes + dislikes;

            if (totalVotes == 0) {
                textReliability.setText("⏳ Em avaliação");
                textReliability.setTextColor(Color.parseColor("#F57C00"));
                textReliability.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFF8E1")));
            } else {
                double ratio = (double) likes / totalVotes;
                if (ratio >= 0.75) {
                    textReliability.setText("🛡️ Alta Confiabilidade (" + (int)(ratio * 100) + "%)");
                    textReliability.setTextColor(Color.parseColor("#2E7D32"));
                    textReliability.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E8F5E9")));
                } else if (ratio >= 0.40) {
                    textReliability.setText("⚠️ Média Confiabilidade (" + (int)(ratio * 100) + "%)");
                    textReliability.setTextColor(Color.parseColor("#E65100"));
                    textReliability.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFF3E0")));
                } else {
                    textReliability.setText("❌ Baixa Confiabilidade (" + (int)(ratio * 100) + "%)");
                    textReliability.setTextColor(Color.parseColor("#C62828"));
                    textReliability.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFEBEE")));
                }
            }
        };

        if (btnLike != null) btnLike.setText("👍 " + likesCount[0]);
        if (btnDislike != null) btnDislike.setText("👎 " + dislikesCount[0]);
        updateReliabilityBadge.run();

        int defaultBgColor = Color.parseColor("#E0E0E0");
        int activeLikeColor = Color.parseColor("#4CAF50");
        int activeDislikeColor = Color.parseColor("#F44336");

        // Busca a contagem atualizada do Firebase para garantir precisão
        FirebaseHelper.getQuadraDetails(q.name, q.neighborhood, (freshLikes, freshDislikes) -> {
            if (isAdded() && !hasUserInteracted[0]) {
                q.likes = freshLikes;
                q.dislikes = freshDislikes;
                likesCount[0] = freshLikes;
                dislikesCount[0] = freshDislikes;
                if (btnLike != null) btnLike.setText("👍 " + likesCount[0]);
                if (btnDislike != null) btnDislike.setText("👎 " + dislikesCount[0]);
                updateReliabilityBadge.run();
                updateSingleQuadraMarkerIcon(q);
            }
        });

        // Busca o voto prévio do usuário
        FirebaseHelper.getUserQuadraVote(q.name, q.neighborhood, currentUserId, isLike -> {
            if (isAdded() && !hasUserInteracted[0]) {
                userVote[0] = isLike;
                if (Boolean.TRUE.equals(isLike) && btnLike != null) {
                    btnLike.setBackgroundTintList(ColorStateList.valueOf(activeLikeColor));
                    btnLike.setTextColor(Color.WHITE);
                } else if (Boolean.FALSE.equals(isLike) && btnDislike != null) {
                    btnDislike.setBackgroundTintList(ColorStateList.valueOf(activeDislikeColor));
                    btnDislike.setTextColor(Color.WHITE);
                }
            }
        });

        // Configura Lógica de Voto Único / Alternância de Like
        if (btnLike != null) {
            btnLike.setOnClickListener(view -> {
                hasUserInteracted[0] = true;
                if (Boolean.TRUE.equals(userVote[0])) {
                    userVote[0] = null;
                    likesCount[0] = Math.max(0, likesCount[0] - 1);
                    q.likes = likesCount[0];
                    q.dislikes = dislikesCount[0];
                    btnLike.setText("👍 " + likesCount[0]);
                    btnLike.setBackgroundTintList(ColorStateList.valueOf(defaultBgColor));
                    btnLike.setTextColor(Color.parseColor("#333333"));
                    FirebaseHelper.addQuadraFeedback(q.name, q.neighborhood, q.city, q.latitude, q.longitude, true, null, currentUserName, currentUserId);
                } else if (Boolean.FALSE.equals(userVote[0])) {
                    userVote[0] = true;
                    dislikesCount[0] = Math.max(0, dislikesCount[0] - 1);
                    likesCount[0]++;
                    q.likes = likesCount[0];
                    q.dislikes = dislikesCount[0];
                    btnLike.setText("👍 " + likesCount[0]);
                    btnDislike.setText("👎 " + dislikesCount[0]);
                    btnLike.setBackgroundTintList(ColorStateList.valueOf(activeLikeColor));
                    btnLike.setTextColor(Color.WHITE);
                    btnDislike.setBackgroundTintList(ColorStateList.valueOf(defaultBgColor));
                    btnDislike.setTextColor(Color.parseColor("#333333"));
                    FirebaseHelper.addQuadraFeedback(q.name, q.neighborhood, q.city, q.latitude, q.longitude, true, null, currentUserName, currentUserId);
                } else {
                    userVote[0] = true;
                    likesCount[0]++;
                    q.likes = likesCount[0];
                    q.dislikes = dislikesCount[0];
                    btnLike.setText("👍 " + likesCount[0]);
                    btnLike.setBackgroundTintList(ColorStateList.valueOf(activeLikeColor));
                    btnLike.setTextColor(Color.WHITE);
                    FirebaseHelper.addQuadraFeedback(q.name, q.neighborhood, q.city, q.latitude, q.longitude, true, null, currentUserName, currentUserId);
                }
                updateReliabilityBadge.run();
                updateSingleQuadraMarkerIcon(q);
            });
        }

        // Configura Lógica de Voto Único / Alternância de Dislike
        if (btnDislike != null) {
            btnDislike.setOnClickListener(view -> {
                hasUserInteracted[0] = true;
                if (Boolean.FALSE.equals(userVote[0])) {
                    userVote[0] = null;
                    dislikesCount[0] = Math.max(0, dislikesCount[0] - 1);
                    q.likes = likesCount[0];
                    q.dislikes = dislikesCount[0];
                    btnDislike.setText("👎 " + dislikesCount[0]);
                    btnDislike.setBackgroundTintList(ColorStateList.valueOf(defaultBgColor));
                    btnDislike.setTextColor(Color.parseColor("#333333"));
                    FirebaseHelper.addQuadraFeedback(q.name, q.neighborhood, q.city, q.latitude, q.longitude, false, null, currentUserName, currentUserId);
                } else if (Boolean.TRUE.equals(userVote[0])) {
                    userVote[0] = false;
                    likesCount[0] = Math.max(0, likesCount[0] - 1);
                    dislikesCount[0]++;
                    q.likes = likesCount[0];
                    q.dislikes = dislikesCount[0];
                    btnLike.setText("👍 " + likesCount[0]);
                    btnDislike.setText("👎 " + dislikesCount[0]);
                    btnDislike.setBackgroundTintList(ColorStateList.valueOf(activeDislikeColor));
                    btnDislike.setTextColor(Color.WHITE);
                    btnLike.setBackgroundTintList(ColorStateList.valueOf(defaultBgColor));
                    btnLike.setTextColor(Color.parseColor("#333333"));
                    FirebaseHelper.addQuadraFeedback(q.name, q.neighborhood, q.city, q.latitude, q.longitude, false, null, currentUserName, currentUserId);
                } else {
                    userVote[0] = false;
                    dislikesCount[0]++;
                    q.likes = likesCount[0];
                    q.dislikes = dislikesCount[0];
                    btnDislike.setText("👎 " + dislikesCount[0]);
                    btnDislike.setBackgroundTintList(ColorStateList.valueOf(activeDislikeColor));
                    btnDislike.setTextColor(Color.WHITE);
                    FirebaseHelper.addQuadraFeedback(q.name, q.neighborhood, q.city, q.latitude, q.longitude, false, null, currentUserName, currentUserId);
                }
                updateReliabilityBadge.run();
                updateSingleQuadraMarkerIcon(q);
            });
        }

        // --- Configuração da Lista de Comentários Inline ---
        if (recyclerComments != null) {
            recyclerComments.setLayoutManager(new LinearLayoutManager(getContext()));
        }

        List<Map<String, Object>> commentsList = new ArrayList<>();
        RecyclerView.Adapter<RecyclerView.ViewHolder> commentAdapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_quadra_comment, parent, false);
                return new RecyclerView.ViewHolder(view) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                Map<String, Object> item = commentsList.get(position);
                TextView txtUser = holder.itemView.findViewById(R.id.textCommentUser);
                TextView txtTime = holder.itemView.findViewById(R.id.textCommentTime);
                TextView txtBody = holder.itemView.findViewById(R.id.textCommentBody);
                ImageButton btnDelComment = holder.itemView.findViewById(R.id.btnDeleteComment);

                if (txtUser != null) txtUser.setText((String) item.getOrDefault("user", "Anônimo"));
                if (txtBody != null) txtBody.setText((String) item.getOrDefault("text", ""));
                
                if (txtTime != null) {
                    Object dateObj = item.get("date");
                    if (dateObj instanceof Timestamp) {
                        long ts = ((Timestamp) dateObj).toDate().getTime();
                        txtTime.setText(DateUtils.getRelativeTimeSpanString(ts, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
                    } else {
                        txtTime.setText("agora");
                    }
                }

                String commentDocId = (String) item.get("docId");
                String commentUserId = (String) item.get("userId");

                FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
                String myEmail = fUser != null ? fUser.getEmail() : "";
                String myUid = fUser != null ? fUser.getUid() : "";

                boolean isCommentMine = (commentUserId != null && (commentUserId.equals(currentUserId) || commentUserId.equals(myUid) || (myEmail != null && commentUserId.equalsIgnoreCase(myEmail))));

                if (btnDelComment != null) {
                    btnDelComment.setVisibility(isCommentMine ? View.VISIBLE : View.GONE);
                    btnDelComment.setOnClickListener(vDel -> {
                        showModernConfirmDialog("Excluir Comentário", "Deseja apagar o seu comentário?", "EXCLUIR", () -> {
                            FirebaseHelper.deleteQuadraComment(q.name, q.neighborhood, commentDocId, null);
                            Toast.makeText(getContext(), "Comentário excluído", Toast.LENGTH_SHORT).show();
                        });
                    });
                }
            }

            @Override
            public int getItemCount() {
                return commentsList.size();
            }
        };

        if (recyclerComments != null) {
            recyclerComments.setAdapter(commentAdapter);
        }

        // Escuta comentários em tempo real da Quadra
        ListenerRegistration commentsListener = FirebaseHelper.listenQuadraComments(q.name, q.neighborhood, list -> {
            if (isAdded()) {
                commentsList.clear();
                if (list != null) commentsList.addAll(list);
                commentAdapter.notifyDataSetChanged();

                if (textNoComments != null) {
                    textNoComments.setVisibility(commentsList.isEmpty() ? View.VISIBLE : View.GONE);
                }
                if (recyclerComments != null) {
                    recyclerComments.setVisibility(commentsList.isEmpty() ? View.GONE : View.VISIBLE);
                    if (!commentsList.isEmpty()) {
                        recyclerComments.scrollToPosition(commentsList.size() - 1);
                    }
                }
            }
        });

        // Envio Direto de Comentários no Mesmo Popup
        if (btnSendComment != null) {
            btnSendComment.setOnClickListener(view -> {
                String commentText = editCommentInput != null ? editCommentInput.getText().toString().trim() : "";
                if (commentText.isEmpty()) {
                    Toast.makeText(getContext(), "Escreva um comentário antes de enviar", Toast.LENGTH_SHORT).show();
                    return;
                }

                String myGoogleName = getUserDisplayName();
                FirebaseHelper.addQuadraFeedback(q.name, q.neighborhood, q.city, q.latitude, q.longitude, null, commentText, myGoogleName, currentUserId);
                if (editCommentInput != null) editCommentInput.setText("");
                Toast.makeText(getContext(), "Comentário enviado!", Toast.LENGTH_SHORT).show();
            });
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(v)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.setOnDismissListener(dialogInterface -> {
            if (commentsListener != null) commentsListener.remove();
        });

        if (btnDelete != null) {
            btnDelete.setOnClickListener(view -> {
                dialog.dismiss();
                boolean isBlocoItem = "BLOCO".equalsIgnoreCase(q.type);
                String itemTypeLabel = isBlocoItem ? "Bloco" : "Quadra";
                showModernConfirmDialog("Excluir " + itemTypeLabel, "Deseja realmente excluir o(a) " + itemTypeLabel + " " + q.name + "?", "EXCLUIR", () -> {
                    new Thread(() -> {
                        AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
                        dao.deleteCorrectedQuadraByNameAndNeighborhood(q.name, q.neighborhood);
                        
                        String docIdToDel = q.docId;
                        if (docIdToDel == null || docIdToDel.isEmpty()) {
                            String namePart = q.name != null ? q.name : "quadra";
                            String neighPart = q.neighborhood != null ? q.neighborhood : "";
                            // Usa a mesma lógica de sanitização do FirebaseHelper
                            docIdToDel = namePart.trim().toLowerCase().replace(" ", "_").replaceAll("[^a-z0-9_]", "") + "_" +
                                         neighPart.trim().toLowerCase().replace(" ", "_").replaceAll("[^a-z0-9_]", "");
                        }
                        FirebaseHelper.deleteGlobalQuadra(docIdToDel, null);

                        Activity act = getActivity();
                        if (act != null) {
                            act.runOnUiThread(() -> {
                                Toast.makeText(getContext(), itemTypeLabel + " excluído(a)", Toast.LENGTH_SHORT).show();
                                refreshQuadraMarkers();
                            });
                        }
                    }).start();
                });
            });
        }

        dialog.show();
    }

    private void showModernConfirmDialog(String title, String message, String positiveText, Runnable onConfirm) {
        if (getContext() == null) return;
        View view = getLayoutInflater().inflate(R.layout.dialog_modern_confirm, null);
        TextView txtTitle = view.findViewById(R.id.textModernTitle);
        TextView txtMessage = view.findViewById(R.id.textModernMessage);
        MaterialButton btnNegative = view.findViewById(R.id.btnModernNegative);
        MaterialButton btnPositive = view.findViewById(R.id.btnModernPositive);

        if (txtTitle != null) txtTitle.setText(title);
        if (txtMessage != null) txtMessage.setText(message);
        if (btnPositive != null) btnPositive.setText(positiveText);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        if (btnNegative != null) {
            btnNegative.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnPositive != null) {
            btnPositive.setOnClickListener(v -> {
                dialog.dismiss();
                if (onConfirm != null) onConfirm.run();
            });
        }

        dialog.show();
    }

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
            double[] coords = searchCoordinatesCityRestricted(address, neighborhood);
            saveStopToDb(address, neighborhood, coords[0], coords[1]);
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
        Path path = new Path();
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

    private Bitmap generateCompassBitmap() {
        int size = (int) (48 * getResources().getDisplayMetrics().density);
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        float center = size / 2f;
        float radius = size * 0.42f;

        // Fundo transparente do mostrador (o fundo branco vem do CardView)
        p.setColor(Color.TRANSPARENT);
        p.setStyle(Paint.Style.FILL);
        c.drawCircle(center, center, radius, p);

        // Borda externa
        p.setColor(Color.DKGRAY);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3f);
        c.drawCircle(center, center, radius, p);

        // Desenhar Letras Norte, Sul, Leste, Oeste
        p.setStyle(Paint.Style.FILL);
        p.setTextSize(size * 0.25f); // Aumentado o tamanho da fonte
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        
        Paint.FontMetrics fm = p.getFontMetrics();
        float baselineOffset = (fm.descent - fm.ascent) / 2 - fm.descent;

        // Norte (N) - Vermelho
        p.setColor(Color.RED);
        c.drawText("N", center, center - radius + (size * 0.22f), p);

        // Sul (S)
        p.setColor(Color.BLACK);
        c.drawText("S", center, center + radius - (size * 0.05f), p);

        // Leste (L)
        p.setColor(Color.BLACK);
        c.drawText("L", center + radius - (size * 0.12f), center + baselineOffset, p);

        // Oeste (O)
        p.setColor(Color.BLACK);
        c.drawText("O", center - radius + (size * 0.12f), center + baselineOffset, p);

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
            
            MapEventsOverlay pickOverlay = new MapEventsOverlay(new MapEventsReceiver() {
                @Override public boolean singleTapConfirmedHelper(GeoPoint p) {
                    Activity activity = getActivity();
                    if (activity != null) activity.runOnUiThread(() -> {
                            cardFixMode.setVisibility(View.GONE);
                            map.getOverlays().removeIf(o -> o instanceof MapEventsOverlay && !(o == currentFixOverlay));
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
        ListView listView = new ListView(requireContext());
        listView.setDivider(null);
        listView.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, types));
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

    private boolean isTemporaryNavCardShownForTutorial = false;

    public View getTutorialTargetView(int position) {
        if (getView() == null) return null;

        if (isTemporaryNavCardShownForTutorial && position != 4) {
            isTemporaryNavCardShownForTutorial = false;
            updateFloatingButtonsVisibility();
        }

        switch (position) {
            case 0:
                return (cardSearch != null && cardSearch.getVisibility() == View.VISIBLE) ? cardSearch : btnToggleSearch;
            case 1:
                return (btnOpenDrawer != null && btnOpenDrawer.getVisibility() == View.VISIBLE) ? btnOpenDrawer : layoutOpenDrawerInside;
            case 2:
                return (fabNewRoute != null && fabNewRoute.getVisibility() == View.VISIBLE) ? fabNewRoute : fabAddStop;
            case 3:
                return (btnPackageOrganization != null && btnPackageOrganization.getVisibility() == View.VISIBLE) ? btnPackageOrganization : btnRouteMenu;
            case 4:
                if (cardNavigationMode != null) {
                    if (cardNavigationMode.getVisibility() != View.VISIBLE) {
                        cardNavigationMode.setVisibility(View.VISIBLE);
                        cardNavigationMode.setAlpha(1.0f);
                        isTemporaryNavCardShownForTutorial = true;
                    }
                    return cardNavigationMode;
                }
                return layoutSwitchContainer;
            case 5:
                return (fabDeliveryApp != null && fabDeliveryApp.getVisibility() == View.VISIBLE) ? fabDeliveryApp : fabCompass;
            case 6:
                return (fabReportHazard != null && fabReportHazard.getVisibility() == View.VISIBLE) ? fabReportHazard : layoutNotificationsContainer;
            default:
                return cardSearch;
        }
    }

    public float getTutorialTargetRadiusDp(int position) {
        switch (position) {
            case 0: return 24f;
            case 1: return 24f;
            case 2: return 28f;
            case 3: return 20f;
            case 4: return 16f;
            case 5: return 28f;
            case 6: return 28f;
            default: return 16f;
        }
    }

    public void onTutorialDismissed() {
        if (isTemporaryNavCardShownForTutorial) {
            isTemporaryNavCardShownForTutorial = false;
            updateFloatingButtonsVisibility();
        }
    }

    public static class DialogOptionItem {
        String id;
        String title;
        boolean isCategory;
        boolean isCheckable;
        boolean isChecked;
        boolean isDropdown;
        String valueText;

        DialogOptionItem(String id, String title, boolean isCategory, boolean isCheckable, boolean isChecked) {
            this(id, title, isCategory, isCheckable, isChecked, false, null);
        }

        DialogOptionItem(String id, String title, boolean isCategory, boolean isCheckable, boolean isChecked, boolean isDropdown, String valueText) {
            this.id = id;
            this.title = title;
            this.isCategory = isCategory;
            this.isCheckable = isCheckable;
            this.isChecked = isChecked;
            this.isDropdown = isDropdown;
            this.valueText = valueText;
        }
    }

    private String currentPopupTopicId = null;

    private void showRouteOptionsMenu(View anchor) {
        showRouteOptionsMenuDialog();
    }

    private void showRouteOptionsMenuDialog() {
        if (currentRouteId == -1 || getContext() == null) return;

        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_route_options, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(v).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView textTitle = v.findViewById(R.id.textDialogMenuTitle);
        MaterialButton btnBack = v.findViewById(R.id.btnDialogBackMenu);
        View btnClose = v.findViewById(R.id.btnDialogCloseMenu);
        RecyclerView recycler = v.findViewById(R.id.recyclerDialogOptionsMenu);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        if (btnClose != null) btnClose.setOnClickListener(v2 -> dialog.dismiss());

        currentPopupTopicId = null;

        dialog.setOnKeyListener((dialogInterface, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                if (currentPopupTopicId != null) {
                    currentPopupTopicId = null;
                    refreshDialogMenuUI(v, dialog, btnBack, textTitle, recycler);
                    return true;
                }
            }
            return false;
        });

        if (btnBack != null) {
            btnBack.setOnClickListener(v2 -> {
                currentPopupTopicId = null;
                refreshDialogMenuUI(v, dialog, btnBack, textTitle, recycler);
            });
        }

        refreshDialogMenuUI(v, dialog, btnBack, textTitle, recycler);
        dialog.show();
    }

    private void refreshDialogMenuUI(View dialogView, AlertDialog dialog, MaterialButton btnBack, TextView textTitle, RecyclerView recycler) {
        if (getContext() == null) return;
        List<DialogOptionItem> items = new ArrayList<>();

        if (currentPopupTopicId == null) {
            if (btnBack != null) btnBack.setVisibility(View.GONE);
            if (textTitle != null) textTitle.setText("⚙️ Opções da Rota");

            items.add(new DialogOptionItem("topic_quick_access", "⚡ Acesso Rápido", true, false, false));
            items.add(new DialogOptionItem("topic_optimization", "🧠 Otimização e Ações", true, false, false));
            items.add(new DialogOptionItem("topic_navigation", "🔊 Navegação e Voz", true, false, false));
            items.add(new DialogOptionItem("topic_visibility", "👁️ Visibilidade no Mapa", true, false, false));
            items.add(new DialogOptionItem("topic_layout", "🔘 Botões e Layout", true, false, false));
            items.add(new DialogOptionItem("topic_help", "❓ Ajuda", true, false, false));
            items.add(new DialogOptionItem("quick_settings", "⚙️ Ajustes", false, false, false));

            FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
            if (u != null && u.getEmail() != null) {
                FirebaseHelper.checkDeveloperAccess(u.getEmail(), isDev -> {
                    Activity act = getActivity();
                    if (isDev && act != null) {
                        act.runOnUiThread(() -> {
                            items.add(new DialogOptionItem("topic_dev", "🛠️ Ferramentas DEV", true, false, false));
                            if (recycler.getAdapter() != null) recycler.getAdapter().notifyDataSetChanged();
                        });
                    }
                });
            }
        } else {
            if (btnBack != null) btnBack.setVisibility(View.VISIBLE);

            if ("topic_quick_access".equals(currentPopupTopicId)) {
                if (textTitle != null) textTitle.setText("⚡ Acesso Rápido");
                items.add(new DialogOptionItem("quick_map", "🗺️ Mapa", false, false, false));
                items.add(new DialogOptionItem("quick_earnings", "💰 Ganhos", false, false, false));
                items.add(new DialogOptionItem("quick_km", "🛣️ Rastreamento de KM", false, false, false));
                items.add(new DialogOptionItem("quick_fuel", "⛽ Abastecimento", false, false, false));
                items.add(new DialogOptionItem("quick_maint", "🛠️ Manutenção", false, false, false));
                items.add(new DialogOptionItem("quick_reports", "📊 Relatórios", false, false, false));
                
                boolean showHelp = sharedPreferences.getBoolean("show_fab_help", true);
                if (showHelp) {
                    items.add(new DialogOptionItem("quick_help", "❓ Ajuda e Tutoriais", false, false, false));
                }
                items.add(new DialogOptionItem("quick_settings", "⚙️ Ajustes", false, false, false));
            } else if ("topic_optimization".equals(currentPopupTopicId)) {
                if (textTitle != null) textTitle.setText("🧠 Otimização e Ações");
                items.add(new DialogOptionItem("opt_v3", "🧠 Otimizar Rota 3.0 (Combustível)", false, false, false));
                items.add(new DialogOptionItem("opt_v2", "⚡ Otimizar Rota 2.0 (Padrão OSRM)", false, false, false));
                items.add(new DialogOptionItem("opt_v1", "Otimizar Rota (Básica)", false, false, false));
                items.add(new DialogOptionItem("opt_sheet", "📄 Ordem Original (Planilha)", false, false, false));
                items.add(new DialogOptionItem("opt_invert", "🔄 Inverter Ordem", false, false, false));
                items.add(new DialogOptionItem("opt_lasso", "✏️ Desenhar Rota (Laço)", false, false, false));
                items.add(new DialogOptionItem("opt_import", "📥 Importar Planilha", false, false, false));
                items.add(new DialogOptionItem("opt_clear", "🗑️ Limpar Rota", false, false, false));
            } else if ("topic_navigation".equals(currentPopupTopicId)) {
                if (textTitle != null) textTitle.setText("🔊 Navegação e Voz");
                boolean voiceNav = isVoiceNavigationEnabled();
                boolean autoNearest = sharedPreferences.getBoolean("advance_to_nearest", false);
                boolean timerOnCards = sharedPreferences.getBoolean("timer_on_cards_only", false);

                items.add(new DialogOptionItem("nav_voice", "🔊 Voz na Navegação", false, true, voiceNav));
                items.add(new DialogOptionItem("nav_auto_nearest", "🎯 Avançar para a mais próxima", false, true, autoNearest));
                items.add(new DialogOptionItem("nav_timer_cards", "⏱️ Tempo apenas nos Cards", false, true, timerOnCards));
            } else if ("topic_visibility".equals(currentPopupTopicId)) {
                if (textTitle != null) textTitle.setText("👁️ Visibilidade no Mapa");
                boolean showWeather = sharedPreferences.getBoolean("show_weather_balloon", true);
                boolean isStatsExpanded = sharedPreferences.getBoolean("stats_summary_expanded", true);
                boolean hideDelivered = sharedPreferences.getBoolean("hide_delivered_stops", false);

                items.add(new DialogOptionItem("vis_weather", "☁️ Exibir Clima no Mapa", false, true, showWeather));
                items.add(new DialogOptionItem("vis_stats", "📊 Exibir Resumo de Entregas", false, true, isStatsExpanded));
                items.add(new DialogOptionItem("vis_hide_delivered", "🙈 Ocultar Entregas no Mapa", false, true, hideDelivered));
            } else if ("topic_help".equals(currentPopupTopicId)) {
                if (textTitle != null) textTitle.setText("❓ Ajuda");
                items.add(new DialogOptionItem("help_tutorial", "🎓 Ver Tutorial do App", false, false, false));
            } else if ("topic_layout".equals(currentPopupTopicId)) {
                if (textTitle != null) textTitle.setText("🔘 Botões e Layout");

                String currentStyle = sharedPreferences.getString("nav_button_style", "original");
                String styleLabel = "search_balloon".equals(currentStyle) ? "Ícone no Balão de Busca" : "Estilo Original";
                items.add(new DialogOptionItem("nav_style_picker", "Estilo de Botão de Navegação", false, false, false, true, styleLabel + " ▼"));

                String currentAlign = sharedPreferences.getString("side_fabs_alignment", "auto");
                String alignLabel = "Automático (Padrão)";
                if ("left".equals(currentAlign)) alignLabel = "Esquerdo";
                else if ("right".equals(currentAlign)) alignLabel = "Direito";

                items.add(new DialogOptionItem("align_picker", "Lado dos Botões Flutuantes", false, false, false, true, alignLabel + " ▼"));

                items.add(new DialogOptionItem("btn_app", "Botão Atalho App", false, true, sharedPreferences.getBoolean("show_fab_delivery_app", true)));
                items.add(new DialogOptionItem("btn_compass", "Botão Bússola", false, true, sharedPreferences.getBoolean("show_fab_compass", true)));
                items.add(new DialogOptionItem("btn_report", "Botão Reportar", false, true, sharedPreferences.getBoolean("show_fab_report_hazard", true)));
                items.add(new DialogOptionItem("btn_center", "Botão Localização", false, true, sharedPreferences.getBoolean("show_fab_center_map", true)));
                items.add(new DialogOptionItem("btn_north", "Botão Norte", false, true, sharedPreferences.getBoolean("show_fab_orientation", true)));
                items.add(new DialogOptionItem("btn_stops_sheet", "Card de Paradas", false, true, sharedPreferences.getBoolean("show_bottom_sheet_stops", true)));

                if (getActivity() instanceof MainActivity && ((MainActivity) getActivity()).isMenuVisible("km")) {
                    items.add(new DialogOptionItem("btn_km", "Botão Rastreio KM", false, true, sharedPreferences.getBoolean("show_fab_km_tracking", true)));
                }
                if (getActivity() instanceof MainActivity && ((MainActivity) getActivity()).isMenuVisible("community_notifications")) {
                    items.add(new DialogOptionItem("btn_notif", "Botão Notificações", false, true, sharedPreferences.getBoolean("show_fab_notifications", true)));
                }
            } else if ("topic_dev".equals(currentPopupTopicId)) {
                if (textTitle != null) textTitle.setText("🛠️ Ferramentas DEV");
                SharedPreferences pr = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
                items.add(new DialogOptionItem("dev_auto_share", "🔄 Dev: Auto-Compartilhar", false, true, pr.getBoolean("dev_auto_share_routes", false)));
                items.add(new DialogOptionItem("dev_share", "🚀 Dev: Compartilhar Rota", false, false, false));
                items.add(new DialogOptionItem("dev_download", "📥 Dev: Baixar Rotas", false, false, false));
                items.add(new DialogOptionItem("dev_show_help", "Mostrar Ícone de Ajuda no Acesso Rápido", false, true, sharedPreferences.getBoolean("show_fab_help", true)));
            }
        }

        DialogOptionsMenuAdapter adapter = new DialogOptionsMenuAdapter(items, item -> {
            if (item.isCategory) {
                currentPopupTopicId = item.id;
                refreshDialogMenuUI(dialogView, dialog, btnBack, textTitle, recycler);
            } else {
                handleDialogOptionAction(dialog, item, () -> refreshDialogMenuUI(dialogView, dialog, btnBack, textTitle, recycler));
            }
        });
        recycler.setAdapter(adapter);
    }

    private void handleDialogOptionAction(AlertDialog dialog, DialogOptionItem item, Runnable refreshUI) {
        if (item == null) return;

        if (item.isCheckable) {
            boolean newChecked = !item.isChecked;
            item.isChecked = newChecked;

            switch (item.id) {
                case "nav_voice":
                    setVoiceNavigationEnabled(newChecked, true);
                    break;
                case "nav_auto_nearest":
                    sharedPreferences.edit().putBoolean("advance_to_nearest", newChecked).apply();
                    break;
                case "nav_timer_cards":
                    sharedPreferences.edit().putBoolean("timer_on_cards_only", newChecked).apply();
                    if (cardRouteTotalTime != null) {
                        if (newChecked) {
                            cardRouteTotalTime.setVisibility(currentRouteHeader != null && currentRouteHeader.endTime > 0 ? View.VISIBLE : View.GONE);
                        } else {
                            cardRouteTotalTime.setVisibility(currentRouteHeader != null && currentRouteHeader.startTime > 0 ? View.VISIBLE : View.GONE);
                        }
                    }
                    if (stopsCardAdapter != null && currentStops != null && !currentStops.isEmpty()) {
                        stopsCardAdapter.notifyItemRangeChanged(0, currentStops.size(), "TIMER_UPDATE");
                    }
                    break;
                case "vis_weather":
                    sharedPreferences.edit().putBoolean("show_weather_balloon", newChecked).apply();
                    if (cardWeatherSummary != null) cardWeatherSummary.setVisibility(newChecked ? View.VISIBLE : View.GONE);
                    break;
                case "vis_stats":
                    sharedPreferences.edit().putBoolean("stats_summary_expanded", newChecked).apply();
                    updateFloatingButtonsVisibility();
                    break;
                case "vis_hide_delivered":
                    sharedPreferences.edit().putBoolean("hide_delivered_stops", newChecked).apply();
                    refreshMarkers();
                    break;
                case "btn_app":
                    sharedPreferences.edit().putBoolean("show_fab_delivery_app", newChecked).apply();
                    updateFloatingButtonsVisibility();
                    break;
                case "btn_compass":
                    sharedPreferences.edit().putBoolean("show_fab_compass", newChecked).apply();
                    updateFloatingButtonsVisibility();
                    break;
                case "dev_show_help":
                    sharedPreferences.edit().putBoolean("show_fab_help", newChecked).apply();
                    break;
                case "btn_report":
                    sharedPreferences.edit().putBoolean("show_fab_report_hazard", newChecked).apply();
                    updateFloatingButtonsVisibility();
                    break;
                case "btn_center":
                    sharedPreferences.edit().putBoolean("show_fab_center_map", newChecked).apply();
                    updateFloatingButtonsVisibility();
                    break;
                case "btn_north":
                    sharedPreferences.edit().putBoolean("show_fab_orientation", newChecked).apply();
                    updateFloatingButtonsVisibility();
                    break;
                case "btn_stops_sheet":
                    sharedPreferences.edit().putBoolean("show_bottom_sheet_stops", newChecked).apply();
                    updateFloatingButtonsVisibility();
                    break;
                case "btn_km":
                    sharedPreferences.edit().putBoolean("show_fab_km_tracking", newChecked).apply();
                    updateFloatingButtonsVisibility();
                    break;
                case "btn_notif":
                    sharedPreferences.edit().putBoolean("show_fab_notifications", newChecked).apply();
                    updateFloatingButtonsVisibility();
                    break;
                case "dev_auto_share":
                    sharedPreferences.edit().putBoolean("dev_auto_share_routes", newChecked).apply();
                    break;
            }
            if (refreshUI != null) refreshUI.run();
        } else {
            dialog.dismiss();
            switch (item.id) {
                case "quick_map":
                    centerOnCurrentLocation();
                    break;
                case "quick_earnings":
                    if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).openFragmentInSettings(new EarningsParentFragment(), "Ganhos");
                    break;
                case "quick_km":
                    if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).openFragmentInSettings(new KmParentFragment(), "KM Diário");
                    break;
                case "quick_fuel":
                    if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).openFragmentInSettings(new FuelParentFragment(), "Abastecimentos");
                    break;
                case "quick_maint":
                    if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).openFragmentInSettings(new MaintenanceParentFragment(), "Manutenção");
                    break;
                case "quick_reports":
                    if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).openFragmentInSettings(new ReportsFragment(), "Relatórios");
                    break;
                case "quick_settings":
                    if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).openFragmentInSettings(new SettingsParentFragment(), "Ajustes");
                    break;
                case "help_tutorial":
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).showAppTutorial();
                    }
                    break;
                case "opt_v3":
                    promptOptimizeRouteV3();
                    break;
                case "opt_v2":
                    optimizeRouteV2();
                    break;
                case "opt_v1":
                    optimizeRoute();
                    break;
                case "opt_sheet":
                    promptApplyOriginalOrder();
                    break;
                case "opt_invert":
                    reverseRouteOrder();
                    break;
                case "opt_lasso":
                    enterLassoMode();
                    break;
                case "opt_import":
                    startXlsxImport();
                    break;
                case "opt_clear":
                    promptClearRoute();
                    break;
                case "nav_style_picker":
                    String[] styleOptions = {"Estilo Original", "Ícone no Balão de Busca"};
                    String currStyle = sharedPreferences.getString("nav_button_style", "original");
                    int currSel = "search_balloon".equals(currStyle) ? 1 : 0;

                    AlertDialog styleDialog = new AlertDialog.Builder(requireContext())
                            .setTitle("Estilo de Botão de Navegação")
                            .setSingleChoiceItems(styleOptions, currSel, (d, which) -> {
                                d.dismiss();
                                String newStyle = (which == 1) ? "search_balloon" : "original";
                                sharedPreferences.edit().putString("nav_button_style", newStyle).apply();
                                updateNavigationButtonStyle();
                                if (refreshUI != null) refreshUI.run();
                            })
                            .setNegativeButton("Cancelar", null)
                            .create();

                    if (styleDialog.getWindow() != null) {
                        styleDialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog_rounded);
                    }
                    styleDialog.show();
                    break;
                case "align_picker":
                    String[] alignOptions = {"Automático (Padrão)", "Esquerdo", "Direito"};
                    String currentAlign = sharedPreferences.getString("side_fabs_alignment", "auto");
                    int currentSelection = 0;
                    if ("left".equals(currentAlign)) currentSelection = 1;
                    else if ("right".equals(currentAlign)) currentSelection = 2;

                    AlertDialog pickerDialog = new AlertDialog.Builder(requireContext())
                            .setTitle("Lado dos Botões Flutuantes")
                            .setSingleChoiceItems(alignOptions, currentSelection, (d, which) -> {
                                d.dismiss();
                                String newAlign = "auto";
                                if (which == 1) newAlign = "left";
                                else if (which == 2) newAlign = "right";

                                sharedPreferences.edit().putString("side_fabs_alignment", newAlign).apply();
                                updateAppModeUI();
                                if (refreshUI != null) refreshUI.run();
                            })
                            .setNegativeButton("Cancelar", null)
                            .create();

                    if (pickerDialog.getWindow() != null) {
                        pickerDialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog_rounded);
                    }
                    pickerDialog.show();
                    break;
                case "align_auto":
                    sharedPreferences.edit().putString("side_fabs_alignment", "auto").apply();
                    updateAppModeUI();
                    break;
                case "align_left":
                    sharedPreferences.edit().putString("side_fabs_alignment", "left").apply();
                    updateAppModeUI();
                    break;
                case "align_right":
                    sharedPreferences.edit().putString("side_fabs_alignment", "right").apply();
                    updateAppModeUI();
                    break;
                case "dev_share":
                    shareCurrentRouteWithDevs();
                    break;
                case "dev_download":
                    showSharedDeveloperRoutes();
                    break;
            }
        }
    }

    public static class DialogOptionsMenuAdapter extends RecyclerView.Adapter<DialogOptionsMenuAdapter.ViewHolder> {
        private final List<DialogOptionItem> items;
        private final OnOptionClickListener listener;

        public interface OnOptionClickListener {
            void onClick(DialogOptionItem item);
        }

        public DialogOptionsMenuAdapter(List<DialogOptionItem> items, OnOptionClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dialog_route_option, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DialogOptionItem item = items.get(position);
            holder.textTitle.setText(item.title);

            if (item.isCategory) {
                holder.imageArrow.setVisibility(View.VISIBLE);
                holder.switchCheck.setVisibility(View.GONE);
                holder.textValue.setVisibility(View.GONE);
            } else if (item.isDropdown) {
                holder.imageArrow.setVisibility(View.GONE);
                holder.switchCheck.setVisibility(View.GONE);
                holder.textValue.setVisibility(View.VISIBLE);
                holder.textValue.setText(item.valueText);
            } else if (item.isCheckable) {
                holder.imageArrow.setVisibility(View.GONE);
                holder.switchCheck.setVisibility(View.VISIBLE);
                holder.switchCheck.setChecked(item.isChecked);
                holder.textValue.setVisibility(View.GONE);
            } else {
                holder.imageArrow.setVisibility(View.GONE);
                holder.switchCheck.setVisibility(View.GONE);
                holder.textValue.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onClick(item);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textTitle, textValue;
            MaterialSwitch switchCheck;
            ImageView imageArrow;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                textTitle = itemView.findViewById(R.id.textOptionTitle);
                textValue = itemView.findViewById(R.id.textOptionValue);
                switchCheck = itemView.findViewById(R.id.switchOptionCheck);
                imageArrow = itemView.findViewById(R.id.imageOptionArrow);
            }
        }
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
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnModernNegative);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btnModernPositive);

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

    private class LassoOverlay extends Overlay {
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
        public void draw(Canvas canvas, Projection projection) {
            Path path = new Path();
            // Desenha caminhos finalizados
            for (List<GeoPoint> pts : allPaths) {
                if (pts.size() < 2) continue;
                path.reset();
                Point p0 = projection.toPixels(pts.get(0), null);
                path.moveTo(p0.x, p0.y);
                for (int i = 1; i < pts.size(); i++) {
                    Point p = projection.toPixels(pts.get(i), null);
                    path.lineTo(p.x, p.y);
                }
                canvas.drawPath(path, paint);
            }
            // Desenha caminho atual
            if (currentPath.size() >= 2) {
                path.reset();
                Point p0 = projection.toPixels(currentPath.get(0), null);
                path.moveTo(p0.x, p0.y);
                for (int i = 1; i < currentPath.size(); i++) {
                    Point p = projection.toPixels(currentPath.get(i), null);
                    path.lineTo(p.x, p.y);
                }
                canvas.drawPath(path, paint);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event, MapView mapView) {
            if (!isLassoDrawingEnabled) return false;

            GeoPoint gp = (GeoPoint) mapView.getProjection().fromPixels((int) event.getX(), (int) event.getY());

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isDrawing = true;
                    currentPath.clear();
                    currentPath.add(gp);
                    mapView.invalidate();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (isDrawing) {
                        currentPath.add(gp);
                        mapView.invalidate();
                        return true;
                    }
                    break;

                case MotionEvent.ACTION_UP:
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
        if (getContext() == null) return;

        // Verifica se a localização GPS atual é válida (não padrão e com coordenadas reais)
        boolean isGpsValid = (currentLocation != null 
                && Math.abs(currentLocation.getLatitude()) > 0.001 
                && Math.abs(currentLocation.getLongitude()) > 0.001
                && !(Math.abs(currentLocation.getLatitude() - (-23.5505)) < 0.01 && Math.abs(currentLocation.getLongitude() - (-46.6333)) < 0.01));

        double targetLat = 0;
        double targetLon = 0;

        if (isGpsValid) {
            targetLat = currentLocation.getLatitude();
            targetLon = currentLocation.getLongitude();
        } else if (currentStops != null && !currentStops.isEmpty()) {
            // Se o GPS ainda não fixou, usa a localização da primeira parada da rota como fallback temporário
            for (RouteStop s : currentStops) {
                if (Math.abs(s.latitude) > 0.001) {
                    targetLat = s.latitude;
                    targetLon = s.longitude;
                    break;
                }
            }
        }

        if (targetLat == 0 && targetLon == 0) {
            new Handler(Looper.getMainLooper()).postDelayed(this::fetchWeather, 3000);
            return;
        }

        final double lat = targetLat;
        final double lon = targetLon;
        lastWeatherLocation = new GeoPoint(lat, lon);
        lastWeatherUpdate = System.currentTimeMillis();

        new Thread(() -> {
            try {
                // 1. Buscar Clima (Open-Meteo)
                String urlStr = String.format(Locale.US, "https://api.open-meteo.com/v1/forecast?latitude=%.6f&longitude=%.6f&current_weather=true&hourly=temperature_2m,weathercode", lat, lon);
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject json = new JSONObject(sb.toString());
                
                // Dados Atuais
                JSONObject current = json.getJSONObject("current_weather");
                final double temp = current.getDouble("temperature");
                final int code = current.getInt("weathercode");

                // Dados Por Hora (Próximas 24 horas)
                JSONObject hourly = json.getJSONObject("hourly");
                JSONArray times = hourly.getJSONArray("time");
                JSONArray temps = hourly.getJSONArray("temperature_2m");
                JSONArray codes = hourly.getJSONArray("weathercode");

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
                                    Date date = inputFormat.parse(fullTime);
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
                            if (c >= 51 && freq >= 2) { dominant = c; break; }
                            if (freq > maxFreq) { maxFreq = freq; dominant = c; }
                        }
                    }

                    if (!hourlyList.isEmpty()) weekList.add(new DayWeather(dayLabel, hourlyList, dominant));
                    if (weekList.size() >= 7) break;
                }
                lastWeekWeather = weekList;

                // 2. Buscar Nome da Cidade (Geocoder nativo + Nominatim fallback)
                String cityName = "";
                if (getContext() != null) {
                    try {
                        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                        List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                        if (addresses != null && !addresses.isEmpty()) {
                            Address a = addresses.get(0);
                            if (a.getLocality() != null && !a.getLocality().isEmpty()) {
                                cityName = a.getLocality();
                            } else if (a.getSubAdminArea() != null && !a.getSubAdminArea().isEmpty()) {
                                cityName = a.getSubAdminArea();
                            }
                        }
                    } catch (Exception ignored) {}
                }

                if (cityName.isEmpty()) {
                    try {
                        String uniqueId = Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID);
                        String userAgent = "DriveLogApp_v1527_" + uniqueId;
                        String geoUrl = String.format(Locale.US, "https://nominatim.openstreetmap.org/reverse?lat=%.6f&lon=%.6f&format=json&zoom=10", lat, lon);
                        URL urlGeo = new URL(geoUrl);
                        HttpURLConnection connGeo = (HttpURLConnection) urlGeo.openConnection();
                        connGeo.setConnectTimeout(4000);
                        connGeo.setReadTimeout(4000);
                        connGeo.setRequestProperty("User-Agent", userAgent);
                        BufferedReader readerGeo = new BufferedReader(new InputStreamReader(connGeo.getInputStream()));
                        StringBuilder sbGeo = new StringBuilder();
                        while ((line = readerGeo.readLine()) != null) sbGeo.append(line);
                        readerGeo.close();

                        JSONObject jsonGeo = new JSONObject(sbGeo.toString());
                        if (jsonGeo.has("address")) {
                            JSONObject addr = jsonGeo.getJSONObject("address");
                            if (addr.has("city")) cityName = addr.getString("city");
                            else if (addr.has("town")) cityName = addr.getString("town");
                            else if (addr.has("municipality")) cityName = addr.getString("municipality");
                            else if (addr.has("village")) cityName = addr.getString("village");
                            else if (addr.has("county")) cityName = addr.getString("county");
                            else if (addr.has("suburb")) cityName = addr.getString("suburb");
                            else if (addr.has("neighbourhood")) cityName = addr.getString("neighbourhood");
                        }
                    } catch (Exception ignored) {}
                }

                final String finalCity = cityName;
                lastCityName = finalCity;
                Activity activity = getActivity();
                if (activity != null) activity.runOnUiThread(() -> updateWeatherUI(temp, code, finalCity));
            } catch (Exception e) {
                Log.e("DriveLog", "Erro ao buscar clima: " + e.getMessage());
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

        TabLayout tabLayout = v.findViewById(R.id.tabWeatherDays);
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

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                if (pos >= 0 && pos < lastWeekWeather.size()) {
                    adapter.setList(lastWeekWeather.get(pos).hourly);
                    if (textTitle != null) {
                        textTitle.setText("Previsão " + lastWeekWeather.get(pos).label);
                    }
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        v.findViewById(R.id.btnWeatherPopupClose).setOnClickListener(v2 -> dialog.dismiss());
        dialog.show();
    }

    private void reverseRouteOrder() { 
        new Thread(() -> { 
            AppDao dao = AppDatabase.getInstance(requireContext()).appDao(); 
            List<RouteStop> s = dao.getStopsForRoute(currentRouteId); 
            Collections.reverse(s);
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
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnModernNegative);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btnModernPositive);

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

    private String getCurrentCityAndState() {
        if (getContext() == null) return "";
        try {
            double lat = 0.0, lon = 0.0;
            if (currentLocation != null && Math.abs(currentLocation.getLatitude()) > 0.001) {
                lat = currentLocation.getLatitude();
                lon = currentLocation.getLongitude();
            } else if (currentStops != null && !currentStops.isEmpty()) {
                for (RouteStop s : currentStops) {
                    if (s.latitude != 0 && s.longitude != 0) {
                        lat = s.latitude;
                        lon = s.longitude;
                        break;
                    }
                }
            }

            if (lat == 0.0 || lon == 0.0) return "";

            Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address a = addresses.get(0);
                String city = a.getLocality() != null ? a.getLocality() : a.getSubAdminArea();
                String state = a.getAdminArea() != null ? a.getAdminArea() : "";
                if (city != null && !city.isEmpty()) {
                    if (!state.isEmpty()) return city + ", " + state;
                    return city;
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    private double[] searchCoordinatesCityRestricted(String rawAddr, String neighborhood) {
        return searchCoordinatesCityRestricted(rawAddr, neighborhood, "");
    }

    private double[] searchCoordinatesCityRestricted(String rawAddr, String neighborhood, String cityOverride) {
        double[] result = new double[]{0.0, 0.0};
        if (rawAddr == null || rawAddr.trim().isEmpty() || getContext() == null) return result;

        Context ctx = getContext();
        double baseLat = 0.0, baseLon = 0.0;
        if (currentLocation != null && Math.abs(currentLocation.getLatitude()) > 0.001) {
            baseLat = currentLocation.getLatitude();
            baseLon = currentLocation.getLongitude();
        } else if (currentStops != null && !currentStops.isEmpty()) {
            for (RouteStop s : currentStops) {
                if (s.latitude != 0 && s.longitude != 0) {
                    baseLat = s.latitude;
                    baseLon = s.longitude;
                    break;
                }
            }
        }

        // Tier 1: Local / Community Corrected Address
        AppDao dao = AppDatabase.getInstance(ctx.getApplicationContext()).appDao();
        CorrectedAddress ca = dao.getCorrectedAddress(rawAddr);
        if (ca != null && ca.latitude != 0 && ca.longitude != 0) {
            return new double[]{ca.latitude, ca.longitude};
        }

        // Detect current city and state unless cityOverride is specified
        String cityState = (cityOverride != null && !cityOverride.trim().isEmpty()) ? cityOverride.trim() : "";
        if (cityState.isEmpty() && baseLat != 0.0 && baseLon != 0.0) {
            try {
                Geocoder g = new Geocoder(ctx, Locale.getDefault());
                List<Address> addrs = g.getFromLocation(baseLat, baseLon, 1);
                if (addrs != null && !addrs.isEmpty()) {
                    Address a = addrs.get(0);
                    String city = a.getLocality() != null ? a.getLocality() : a.getSubAdminArea();
                    String state = a.getAdminArea() != null ? a.getAdminArea() : "";
                    if (city != null && !city.isEmpty()) {
                        cityState = !state.isEmpty() ? (city + ", " + state) : city;
                    }
                }
            } catch (Exception ignored) {}
        }

        String baseQuery = rawAddr.trim() + (neighborhood != null && !neighborhood.trim().isEmpty() ? ", " + neighborhood.trim() : "");
        String cityQuery = (!cityState.isEmpty() && !baseQuery.toLowerCase().contains(cityState.toLowerCase())) 
                ? (baseQuery + ", " + cityState) 
                : baseQuery;

        double minLat = baseLat != 0 ? baseLat - 0.45 : -90;
        double maxLat = baseLat != 0 ? baseLat + 0.45 : 90;
        double minLon = baseLon != 0 ? baseLon - 0.45 : -180;
        double maxLon = baseLon != 0 ? baseLon + 0.45 : 180;

        // Tier 2: Android Native Geocoder (City-Qualified & Bounded)
        try {
            Geocoder geocoder = new Geocoder(ctx, Locale.getDefault());
            List<Address> addresses = null;

            if (baseLat != 0 && baseLon != 0) {
                addresses = geocoder.getFromLocationName(cityQuery, 5, minLat, minLon, maxLat, maxLon);
                if (addresses == null || addresses.isEmpty()) {
                    addresses = geocoder.getFromLocationName(baseQuery, 5, minLat, minLon, maxLat, maxLon);
                }
            }
            if (addresses == null || addresses.isEmpty()) {
                addresses = geocoder.getFromLocationName(cityQuery, 5);
            }

            if (addresses != null && !addresses.isEmpty()) {
                Address best = addresses.get(0);
                if (baseLat != 0 && baseLon != 0) {
                    double bestDist = Double.MAX_VALUE;
                    for (Address a : addresses) {
                        float[] distResult = new float[1];
                        Location.distanceBetween(baseLat, baseLon, a.getLatitude(), a.getLongitude(), distResult);
                        if (distResult[0] < bestDist) {
                            bestDist = distResult[0];
                            best = a;
                        }
                    }
                }
                return new double[]{best.getLatitude(), best.getLongitude()};
            }
        } catch (Exception ignored) {}

        // Tier 3: OpenStreetMap Nominatim Search (With Viewbox Bounding)
        try {
            String uniqueId = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
            String userAgent = "DriveLogApp_v1527_" + uniqueId;
            
            String viewboxParam = (baseLat != 0 && baseLon != 0) 
                    ? String.format(Locale.US, "&viewbox=%.6f,%.6f,%.6f,%.6f&bounded=0", minLon, maxLat, maxLon, minLat) 
                    : "";

            String u = "https://nominatim.openstreetmap.org/search?q=" + URLEncoder.encode(cityQuery, "UTF-8") + "&format=json&limit=5" + viewboxParam;
            HttpURLConnection conn = (HttpURLConnection) new URL(u).openConnection();
            conn.setRequestProperty("User-Agent", userAgent);

            if (conn.getResponseCode() == 200) {
                BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder res = new StringBuilder(); String l;
                while ((l = r.readLine()) != null) res.append(l);

                JSONArray arr = new JSONArray(res.toString());
                if (arr.length() > 0) {
                    JSONObject bestObj = arr.getJSONObject(0);
                    if (baseLat != 0 && baseLon != 0) {
                        double bestDist = Double.MAX_VALUE;
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject o = arr.getJSONObject(i);
                            double oLat = o.getDouble("lat");
                            double oLon = o.getDouble("lon");
                            float[] distResult = new float[1];
                            Location.distanceBetween(baseLat, baseLon, oLat, oLon, distResult);
                            if (distResult[0] < bestDist) {
                                bestDist = distResult[0];
                                bestObj = o;
                            }
                        }
                    }
                    return new double[]{bestObj.getDouble("lat"), bestObj.getDouble("lon")};
                }
            }
        } catch (Exception ignored) {}

        // Tier 4: Fallback GPS
        if (baseLat != 0 && baseLon != 0) {
            return new double[]{baseLat, baseLon};
        }

        return result;
    }

    private void geocodeManualStopAndSave(int routeId, String rawAddr, String seqStr, String cityOverride, Runnable onComplete) {
        if (getContext() == null) return;
        final Context ctx = getContext();
        new Thread(() -> {
            AppDao dao = AppDatabase.getInstance(ctx.getApplicationContext()).appDao();
            List<RouteStop> current = dao.getStopsForRoute(routeId);
            int nextOrder = current.size();
            int seq = parseSafeInt(seqStr);

            double[] coords = searchCoordinatesCityRestricted(rawAddr, "", cityOverride);
            double lat = coords[0];
            double lon = coords[1];

            RouteStop newStop = new RouteStop(rawAddr, lat, lon);
            newStop.routeId = routeId;
            newStop.sequence = seq;
            newStop.allSequences = !seqStr.isEmpty() ? seqStr : String.valueOf(nextOrder + 1);
            newStop.packageCount = 1;
            newStop.sortOrder = nextOrder;
            newStop.stopNumber = nextOrder + 1;
            if (cityOverride != null && !cityOverride.trim().isEmpty()) {
                newStop.city = cityOverride.trim();
            }

            dao.insertRouteStop(newStop);

            Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(() -> {
                    if (onComplete != null) onComplete.run();
                });
            }
        }).start();
    }

    private void saveStopToDb(String address, String neighborhood, double lat, double lon) {
        if (getContext() == null || currentRouteId == -1) return;
        final int routeId = currentRouteId;
        final Context ctx = getContext();
        new Thread(() -> {
            AppDao dao = AppDatabase.getInstance(ctx.getApplicationContext()).appDao();

            double finalLat = lat;
            double finalLon = lon;

            if (finalLat == 0.0 || finalLon == 0.0) {
                double[] coords = searchCoordinatesCityRestricted(address, neighborhood);
                finalLat = coords[0];
                finalLon = coords[1];
            }

            RouteStop s = new RouteStop(address, finalLat, finalLon);
            s.routeId = routeId;
            s.neighborhood = neighborhood;
            List<RouteStop> existing = dao.getStopsForRoute(routeId);
            s.sortOrder = existing.size();
            s.stopNumber = existing.size() + 1;
            dao.insertRouteStop(s);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (editSearch != null) editSearch.setText("");
                    sharedPreferences.edit().putBoolean("show_bottom_sheet_stops", true).apply();
                    Toast.makeText(getContext(), "Parada adicionada!", Toast.LENGTH_SHORT).show();
                    CloudSyncHelper.syncNow(ctx, "Atividade na Rota");
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
            updateRouteCorrectionsCount();
            
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

            boolean isOptPending = sharedPreferences.getBoolean("route_optimization_pending_" + currentRouteId, false);
            boolean isManualPending = sharedPreferences.getBoolean("route_manual_list_pending_" + currentRouteId, false);
            boolean isStatsExpanded = sharedPreferences.getBoolean("stats_summary_expanded", true);

            if (isOptPending || isManualPending) {
                if (layoutStatsGroup != null) layoutStatsGroup.setVisibility(View.GONE);
                if (cardSuccessSummary != null) cardSuccessSummary.setVisibility(View.GONE);
                if (cardPendingSummary != null) cardPendingSummary.setVisibility(View.GONE);
                if (cardFailedSummary != null) cardFailedSummary.setVisibility(View.GONE);
                if (cardRouteCorrections != null) cardRouteCorrections.setVisibility(View.GONE);
            } else if (autoHideRunnable == null) {
                if (layoutStatsGroup != null) layoutStatsGroup.setVisibility(isStatsExpanded ? View.VISIBLE : View.GONE);
                if (cardSuccessSummary != null) cardSuccessSummary.setVisibility(isStatsExpanded ? View.VISIBLE : View.GONE);
                if (cardPendingSummary != null) cardPendingSummary.setVisibility(isStatsExpanded ? View.VISIBLE : View.GONE);
                if (cardFailedSummary != null) {
                    cardFailedSummary.setVisibility((isStatsExpanded && errStops > 0) ? View.VISIBLE : View.GONE);
                }
            }

            // Efeito de celebração se a rota foi concluída (nenhuma pendente)
            String celebrationKey = "last_finished_route_" + currentRouteId;
            boolean alreadyCelebrated = sharedPreferences.getBoolean(celebrationKey, false);
            
            if (!stops.isEmpty() && pendStops == 0) {
                if (!alreadyCelebrated) {
                    sharedPreferences.edit().putBoolean(celebrationKey, true).apply();
                    Log.d("DriveLog", "Rota concluída! Disparando celebração para rota: " + currentRouteId);
                    Toast.makeText(getContext(), "🎉 ROTA CONCLUÍDA!", Toast.LENGTH_LONG).show();
                    triggerCelebration();

                    // 🔥 SE A NAVEGAÇÃO ESTIVER ATIVA (SWITCH ATIVADO), ENCAMINHA PARA A CASA DO MOTORISTA
                    if (switchTraceLine != null && switchTraceLine.isChecked()) {
                        float homeLat = sharedPreferences.getFloat("home_lat", 0);
                        float homeLon = sharedPreferences.getFloat("home_lon", 0);

                        if (homeLat != 0 && homeLon != 0) {
                            RouteStop homeStop = new RouteStop("Minha Residência (Casa)", homeLat, homeLon);
                            homeStop.stopNumber = 0;
                            currentlySelectedStop = homeStop;
                            updateSelectionTrace(homeStop);
                            speakVoiceNavigation("Todas as entregas foram concluídas! Encaminhando você para sua residência.", true);
                            Toast.makeText(getContext(), "🏠 Rota concluída! Encaminhando para sua residência...", Toast.LENGTH_LONG).show();
                        } else {
                            speakVoiceNavigation("Parabéns! Todas as entregas foram concluídas. Defina seu endereço de Casa no menu para ser direcionado ao finalizar.", true);
                            Toast.makeText(getContext(), "🏠 Defina o endereço de Casa no menu para ser direcionado ao finalizar.", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            } else if (!stops.isEmpty() && pendStops > 0 && alreadyCelebrated) {
                // Se voltou a ter paradas pendentes, permite celebrar de novo quando terminar
                sharedPreferences.edit().remove(celebrationKey).apply();
                Log.d("DriveLog", "Resetando flag de celebração pois há paradas pendentes na rota: " + currentRouteId);
            }

            boolean showStopsCard = sharedPreferences.getBoolean("show_bottom_sheet_stops", true);
            if (stops.size() > 0 && pendStops == 0 && showStopsCard) {
                // Auto-oculta o card ao finalizar, mas permite reativar manualmente no menu
                sharedPreferences.edit().putBoolean("show_bottom_sheet_stops", false).apply();
                showStopsCard = false;
            }

            if (stopsCardAdapter != null) { 
                stopsCardAdapter.setStops(stops); 
                bottomSheet.setVisibility((stops.isEmpty() || !showStopsCard || isOptPending || isManualPending) ? View.GONE : View.VISIBLE); 
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
            if (header == null) return;
            if (this.currentRouteHeader != null && this.currentRouteHeader.startTime > 0 && header.startTime == 0) {
                header.startTime = this.currentRouteHeader.startTime;
                header.totalPausedMs = this.currentRouteHeader.totalPausedMs;
                header.lastPauseStartTime = this.currentRouteHeader.lastPauseStartTime;
            }
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

    private void updatePendingManualListUI() {
        if (currentRouteId == -1 || cardPendingManualList == null) return;
        int stopCount = currentStops != null ? currentStops.size() : 0;

        if (textPendingManualListTitle != null) {
            textPendingManualListTitle.setText("📝 Lista em Edição (" + stopCount + " Paradas)");
        }
        if (textPendingManualListSubtitle != null) {
            if (stopCount == 0) {
                textPendingManualListSubtitle.setText("Sua lista ainda está sem paradas. Clique abaixo para cadastrar pacotes.");
            } else if (stopCount == 1) {
                textPendingManualListSubtitle.setText("Você possui 1 parada salva em edição. Clique abaixo para continuar cadastrando.");
            } else {
                textPendingManualListSubtitle.setText("Você possui " + stopCount + " paradas salvas em edição. Clique abaixo para continuar cadastrando.");
            }
        }
        if (btnContinueManualListEditing != null) {
            btnContinueManualListEditing.setText("📝 CONTINUAR EDIÇÃO");
        }
    }

    private void promptCancelAndDeleteManualRoute() {
        if (currentRouteId == -1 || getContext() == null) return;
        final int routeToDeleteId = currentRouteId;

        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_modern_confirm, null);
        TextView title = v.findViewById(R.id.textModernTitle);
        TextView message = v.findViewById(R.id.textModernMessage);
        MaterialButton btnCancel = v.findViewById(R.id.btnModernNegative);
        MaterialButton btnConfirm = v.findViewById(R.id.btnModernPositive);

        if (title != null) title.setText("Excluir Rota em Edição");
        if (message != null) message.setText("Deseja realmente cancelar a edição e excluir esta rota?");
        if (btnCancel != null) btnCancel.setText("CANCELAR");
        if (btnConfirm != null) {
            btnConfirm.setText("EXCLUIR ROTA");
            btnConfirm.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F44336")));
        }

        AlertDialog confirmDialog = new AlertDialog.Builder(requireContext()).setView(v).create();
        if (confirmDialog.getWindow() != null) {
            confirmDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        if (btnCancel != null) btnCancel.setOnClickListener(v2 -> confirmDialog.dismiss());

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v2 -> {
                confirmDialog.dismiss();
                new Thread(() -> {
                    AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
                    
                    dao.clearRouteStopsByRoute(routeToDeleteId);
                    dao.deleteGroupsForRoute(routeToDeleteId);
                    
                    RouteHeader headerToDelete = dao.getRouteById(routeToDeleteId);
                    if (headerToDelete != null) {
                        dao.deleteRouteHeader(headerToDelete);
                    }

                    sharedPreferences.edit().remove("route_manual_list_pending_" + routeToDeleteId).apply();
                    sharedPreferences.edit().remove("route_optimization_pending_" + routeToDeleteId).apply();

                    List<RouteHeader> remainingRoutes = dao.getAllRoutes();
                    int targetPreviousRouteId = -1;
                    String targetPreviousRouteName = "Minha Rota";

                    if (remainingRoutes != null && !remainingRoutes.isEmpty()) {
                        RouteHeader prev = remainingRoutes.get(0);
                        targetPreviousRouteId = prev.id;
                        targetPreviousRouteName = prev.name;
                    }

                    final int prevId = targetPreviousRouteId;
                    final String prevName = targetPreviousRouteName;

                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.runOnUiThread(() -> {
                            if (prevId != -1) {
                                switchRoute(prevId, prevName);
                                Toast.makeText(getContext(), "Rota excluída. Retornando para: " + prevName, Toast.LENGTH_SHORT).show();
                            } else {
                                currentRouteId = -1;
                                if (cardPendingManualList != null) cardPendingManualList.setVisibility(View.GONE);
                                if (cardPendingOptimization != null) cardPendingOptimization.setVisibility(View.GONE);
                                updateFloatingButtonsVisibility();
                                Toast.makeText(getContext(), "Rota excluída com sucesso.", Toast.LENGTH_SHORT).show();
                            }
                            CloudSyncHelper.syncNow(requireContext(), "Exclusão de Rota");
                        });
                    }
                }).start();
            });
        }

        confirmDialog.show();
    }

    private void refreshMarkers() { 
        if (map == null || currentStops == null) return; 

        boolean isOptPending = sharedPreferences.getBoolean("route_optimization_pending_" + currentRouteId, false);
        boolean isManualPending = sharedPreferences.getBoolean("route_manual_list_pending_" + currentRouteId, false);

        if (isOptPending || isManualPending) {
            Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(() -> {
                    if (cardPendingManualList != null) {
                        cardPendingManualList.setVisibility(isManualPending ? View.VISIBLE : View.GONE);
                        if (isManualPending) updatePendingManualListUI();
                    }
                    if (cardPendingOptimization != null) cardPendingOptimization.setVisibility(isOptPending && !isManualPending ? View.VISIBLE : View.GONE);
                    if (layoutSwitchContainer != null) layoutSwitchContainer.setVisibility(View.GONE);
                    if (cardRouteTotalTime != null) cardRouteTotalTime.setVisibility(View.GONE);
                    if (bottomSheet != null) bottomSheet.setVisibility(View.GONE);
                    if (cardNavigationMode != null) cardNavigationMode.setVisibility(View.GONE);
                    if (fabDeliveryApp != null) fabDeliveryApp.setVisibility(View.GONE);
                    if (fabCompass != null) fabCompass.setVisibility(View.GONE);
                    if (fabReportHazard != null) fabReportHazard.setVisibility(View.GONE);
                    if (fabCenterMap != null) fabCenterMap.setVisibility(View.GONE);
                    if (fabMapOrientation != null) fabMapOrientation.setVisibility(View.GONE);
                    if (fabKmTracking != null) fabKmTracking.setVisibility(View.GONE);
                    if (layoutStatsGroup != null) layoutStatsGroup.setVisibility(View.GONE);
                    if (cardSuccessSummary != null) cardSuccessSummary.setVisibility(View.GONE);
                    if (cardPendingSummary != null) cardPendingSummary.setVisibility(View.GONE);
                    if (cardFailedSummary != null) cardFailedSummary.setVisibility(View.GONE);
                    if (btnToggleStatsSummary != null) btnToggleStatsSummary.setVisibility(View.INVISIBLE);
                    if (cardRouteCorrections != null) cardRouteCorrections.setVisibility(View.GONE);
                    if (map != null) {
                        map.getOverlays().clear();
                        map.invalidate();
                    }
                });
            }
            return;
        } else {
            Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(() -> {
                    if (cardPendingManualList != null) cardPendingManualList.setVisibility(View.GONE);
                    if (cardPendingOptimization != null) cardPendingOptimization.setVisibility(View.GONE);
                    updateFloatingButtonsVisibility();
                });
            }
        }
        
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

            // Mapeia grupos de paradas que estão no mesmo local/encostando (distância < 2.5 metros)
            Map<Integer, List<RouteStop>> overlapMap = new HashMap<>();
            for (int i = 0; i < stopsSnapshot.size(); i++) {
                RouteStop s = stopsSnapshot.get(i);
                if (s.latitude == 0 && s.longitude == 0) continue;

                List<RouteStop> sameLocList = new ArrayList<>();
                for (int j = 0; j < stopsSnapshot.size(); j++) {
                    RouteStop other = stopsSnapshot.get(j);
                    if (other.latitude == 0 && other.longitude == 0) continue;

                    float[] res = new float[1];
                    Location.distanceBetween(s.latitude, s.longitude, other.latitude, other.longitude, res);
                    if (res[0] < 2.5) { // Menos de 2.5m = ícones encostados/sobrepostos exatamente no mesmo ponto
                        sameLocList.add(other);
                    }
                }
                overlapMap.put(s.id, sameLocList);
            }

            List<Bitmap> bitmaps = new ArrayList<>();
            for (int i = 0; i < stopsSnapshot.size(); i++) {
                RouteStop s = stopsSnapshot.get(i);
                
                if (hideDelivered && s.deliveryStatus == 1) {
                    bitmaps.add(null);
                } else {
                    String gColor = (s.groupId != null) ? colorMap.get(s.groupId) : null;
                    List<RouteStop> sameLoc = overlapMap.get(s.id);
                    
                    boolean isGroupOverlapping = sameLoc != null && sameLoc.size() > 1;
                    int pendingCountAtLocation = 0;
                    if (sameLoc != null) {
                        for (RouteStop st : sameLoc) {
                            if (st.deliveryStatus == 0) { // 0 = Pendente
                                pendingCountAtLocation++;
                            }
                        }
                    }

                    // 🔥 A bolinha vermelha só desaparece quando TODAS as paradas daquele local forem entregues ou com erro (pendingCount == 0)
                    boolean hasOverlap = isGroupOverlapping && pendingCountAtLocation > 0;

                    bitmaps.add(generateMarkerBitmap(i+1, s.deliveryStatus, s.packageCount > 1, (i == selectedIndex), gColor, hasOverlap));
                }
            }

            Activity activity = getActivity();
            if (activity != null) activity.runOnUiThread(() -> {
                if (map == null || currentRouteId != targetRouteId || !isAdded()) return;
                
                List<Overlay> toAdd = new ArrayList<>();
                for (int i = 0; i < stopsSnapshot.size(); i++) { 
                    Bitmap markerBmp = bitmaps.get(i);
                    if (markerBmp == null) continue;

                    RouteStop s = stopsSnapshot.get(i); 
                    if (s.latitude == 0 && s.longitude == 0 || map == null) continue;

                    Marker m = new Marker(map) {
                        private final Handler longClickHandler = new Handler(Looper.getMainLooper());
                        private Runnable longClickRunnable;
                        private boolean isLongClickTriggered = false;

                        @Override
                        public boolean onTouchEvent(MotionEvent event, MapView mapView) {
                            if (hitTest(event, mapView)) {
                                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                                    isLongClickTriggered = false;
                                    longClickRunnable = () -> {
                                        isLongClickTriggered = true;
                                        if (isAdded()) deleteStopDialog(s);
                                    };
                                    longClickHandler.postDelayed(longClickRunnable, 800);
                                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
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
                    m.setRelatedObject("STOP_INDEX_" + i);

                    // 🔥 Micro-deslocamento radial em leque para desobstruir os marcadores sobrepostos
                    List<RouteStop> sameLoc = overlapMap.get(s.id);
                    double markerLat = s.latitude;
                    double markerLon = s.longitude;

                    if (sameLoc != null && sameLoc.size() > 1) {
                        int k = sameLoc.indexOf(s);
                        if (k != -1) {
                            double angle = (2.0 * Math.PI * k) / sameLoc.size();
                            double offsetDegrees = 0.000045; // ~4.5 metros de leque no mapa
                            markerLat += offsetDegrees * Math.sin(angle);
                            markerLon += (offsetDegrees * Math.cos(angle)) / Math.cos(Math.toRadians(s.latitude));
                        }
                    }

                    m.setPosition(new GeoPoint(markerLat, markerLon)); 
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
                List<Overlay> currentOverlays = map.getOverlays();
                List<Overlay> nonStopOverlays = new ArrayList<>();
                for (Overlay o : currentOverlays) {
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
        List<Overlay> overlays = map.getOverlays();
        for (int i = overlays.size() - 1; i >= 0; i--) {
            Overlay o = overlays.get(i);
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
        return generateMarkerBitmap(n, status, multi, selected, gColor, false);
    }

    private Bitmap generateMarkerBitmap(int n, int status, boolean multi, boolean selected, String gColor, boolean hasOverlap) {
        int baseSize = selected ? 110 : 80;
        float stopScaleMultiplier = 1.0f;
        if (getContext() != null) {
            int stopScalePercent = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
                    .getInt("stop_icon_size", 100);
            stopScaleMultiplier = stopScalePercent / 100f;
        }
        int size = Math.round(baseSize * stopScaleMultiplier);
        if (size < 20) size = 20;

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
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(8f * stopScaleMultiplier);
            
            int[] colors = {stopColor, stopColor, Color.WHITE, stopColor, stopColor};
            float[] positions = {0.0f, 0.30f, 0.5f, 0.70f, 1.0f};
            
            SweepGradient gradient = new SweepGradient(center, center, colors, positions);
            
            Matrix matrix = new Matrix();
            matrix.postRotate(markerRotationAngle, center, center);
            gradient.setLocalMatrix(matrix);
            
            p.setShader(gradient);
            c.drawCircle(center, center, radius, p);
            p.setShader(null);
            
            p.setStrokeWidth(1f * stopScaleMultiplier);
            p.setColor(Color.WHITE);
            c.drawCircle(center, center, radius + 4f * stopScaleMultiplier, p);
        } else {
            p.setColor(Color.WHITE);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(4f * stopScaleMultiplier);
            p.setAlpha(alpha);
            c.drawCircle(center, center, radius, p);
        }

        // 3. Texto (Número da Parada)
        p.setStyle(Paint.Style.FILL);
        p.setTextSize((selected ? 44f : 32f) * stopScaleMultiplier);
        p.setTextAlign(Paint.Align.CENTER);
        p.setAlpha(alpha);
        if (stopColor == Color.parseColor("#FF9800")) p.setColor(Color.BLACK); else p.setColor(Color.WHITE);
        
        Paint.FontMetrics fm = p.getFontMetrics();
        float textY = center - (fm.ascent + fm.descent) / 2;
        c.drawText(String.valueOf(n), center, textY, p);

        // 4. 🔥 Badge de Paradas Sobrepostas (Bolinha Vermelha no canto superior direito)
        if (hasOverlap) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.parseColor("#FF1744"));
            float badgeX = center + radius * 0.65f;
            float badgeY = center - radius * 0.65f;
            float badgeR = (selected ? 13f : 9f) * stopScaleMultiplier;
            c.drawCircle(badgeX, badgeY, badgeR, p);
            
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2f * stopScaleMultiplier);
            p.setColor(Color.WHITE);
            c.drawCircle(badgeX, badgeY, badgeR, p);
        }

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
                String uniqueId = Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID);
                String userAgent = "DriveLogApp_v1527_" + uniqueId;
                
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
                        GeoPoint stepLoc = null;
                        
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
                                }

                                if (steps.length() > 0) {
                                    int targetStepIdx = 0;
                                    double accumDist = steps.getJSONObject(0).optDouble("distance", 0);

                                    // Se houver próximo passo, a manobra a exibir é a do PRÓXIMO passo (ex: dobrar na próxima rua)
                                    if (steps.length() > 1) {
                                        targetStepIdx = 1;
                                        // Se o primeiro segmento for muito curto (<15m) e houver mais passos, avança para a manobra seguinte
                                        if (accumDist < 15 && steps.length() > 2) {
                                            accumDist += steps.getJSONObject(1).optDouble("distance", 0);
                                            targetStepIdx = 2;
                                        }
                                    }

                                    JSONObject targetStep = steps.getJSONObject(targetStepIdx);
                                    JSONObject targetMan = targetStep.getJSONObject("maneuver");

                                    if (targetMan.has("location")) {
                                        JSONArray locArr = targetMan.getJSONArray("location");
                                        if (locArr.length() >= 2) {
                                            stepLoc = new GeoPoint(locArr.getDouble(1), locArr.getDouble(0));
                                        }
                                    }

                                    nextStepDist = accumDist;
                                    maneuverType = targetMan.optString("type", "");
                                    modifier = targetMan.optString("modifier", "");
                                    String targetName = targetStep.optString("name", "");

                                    instruction = parseInstruction(maneuverType, modifier, targetName);
                                }
                            }
                        }

                        if (stepLoc == null && stop != null) {
                            stepLoc = new GeoPoint(stop.latitude, stop.longitude);
                        }

                        final String finalInstruction = instruction;
                        final double finalNextDist = nextStepDist;
                        final String finalManeuver = maneuverType;
                        final String finalModifier = modifier;
                        final List<NavInstruction> finalAllInstructions = allInstructions;
                        final double finalDuration = durationSeconds;
                        final GeoPoint finalNextStepLoc = stepLoc;

                        JSONArray co = route.getJSONObject("geometry").getJSONArray("coordinates");
                        List<GeoPoint> pts = new ArrayList<>(); for(int i=0; i<co.length(); i++) pts.add(new GeoPoint(co.getJSONArray(i).getDouble(1), co.getJSONArray(i).getDouble(0)));
                        Activity activity = getActivity();
                        if (activity != null) activity.runOnUiThread(() -> {
                                if (map == null || !isAdded()) return;

                                fullTracePoints = new ArrayList<>(pts);
                                lastTraceUpdate = System.currentTimeMillis();
                                lastRouteInstructions = finalAllInstructions;

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
                                if (textSearchNavDistance != null) {
                                    textSearchNavDistance.setText(distStr);
                                }
                                
                                // Modo Navegação (Balão Superior)
                                if (cardNavigationMode != null) {
                                    animateNavigationCard(true);
                                    nextStepLocation = finalNextStepLoc;
                                    double initialDistToNext = (nextStepLocation != null && currentLocation != null)
                                            ? currentLocation.distanceToAsDouble(nextStepLocation)
                                            : (finalNextDist > 0 ? finalNextDist : 200.0);

                                    initialStepDistance = Math.max(finalNextDist > 0 ? finalNextDist : 200.0, initialDistToNext);

                                    String newStepKey = finalInstruction + "_" + (int) initialStepDistance;
                                    if (!newStepKey.equals(currentStepInstructionKey)) {
                                        currentStepInstructionKey = newStepKey;
                                        currentStepMaxProgressPct = 0f;
                                    }

                                    double completedDist = Math.max(0, initialStepDistance - initialDistToNext);
                                    float rawPct = (float) Math.max(0, Math.min(100, (completedDist / initialStepDistance) * 100.0));
                                    currentStepMaxProgressPct = Math.max(currentStepMaxProgressPct, rawPct);

                                    if (borderProgressDrawable != null) {
                                        borderProgressDrawable.setProgress(currentStepMaxProgressPct);
                                    }
                                    cardNavigationMode.invalidate();

                                    if (textNavDistance != null) {
                                        if (initialDistToNext < 1000) textNavDistance.setText(String.format(Locale.getDefault(), "%.0fm", initialDistToNext));
                                        else textNavDistance.setText(String.format(Locale.getDefault(), "%.1fkm", initialDistToNext / 1000.0));
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
                                    if (finalInstruction != null && !finalInstruction.trim().isEmpty()) {
                                        announceNavigationTurn(finalInstruction, finalNextDist);
                                    }
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

    private void updateNavigationLineProgress() {
        if (map == null || currentLocation == null || currentlySelectedStop == null) return;
        if (selectionTracePolyline == null || fullTracePoints == null || fullTracePoints.isEmpty()) return;

        long now = System.currentTimeMillis();
        // Se passou mais de 15 segundos desde a última rota OSRM, recalcula do zero no servidor
        if (now - lastTraceUpdate > 15000) {
            lastTraceUpdate = now;
            updateSelectionTrace(currentlySelectedStop);
            return;
        }

        // Encontra o ponto no caminho mais próximo do motorista
        int closestIndex = -1;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < fullTracePoints.size(); i++) {
            double dist = currentLocation.distanceToAsDouble(fullTracePoints.get(i));
            if (dist < minDistance) {
                minDistance = dist;
                closestIndex = i;
            }
        }

        // Se o motorista desviou mais de 60 metros da rota, recalcula nova rota no OSRM
        if (minDistance > 60) {
            speakVoiceNavigation("Recalculando rota...", false);
            lastTraceUpdate = now;
            updateSelectionTrace(currentlySelectedStop);
            return;
        }

        if (closestIndex != -1 && closestIndex < fullTracePoints.size()) {
            List<GeoPoint> remainingPoints = new ArrayList<>();
            remainingPoints.add(currentLocation); // A linha azul começa na localização exata do veículo

            for (int i = closestIndex; i < fullTracePoints.size(); i++) {
                remainingPoints.add(fullTracePoints.get(i));
            }

            selectionTracePolyline.setPoints(remainingPoints);

            // Atualiza distância restante até a parada
            double remainingMeters = currentLocation.distanceToAsDouble(new GeoPoint(currentlySelectedStop.latitude, currentlySelectedStop.longitude));
            if (textSwitchDistance != null) {
                String distStr;
                if (remainingMeters < 1000) distStr = String.format(Locale.getDefault(), "%.0fm", remainingMeters);
                else distStr = String.format(Locale.getDefault(), "%.1fkm", remainingMeters / 1000.0);
                textSwitchDistance.setText(distStr);
                if (textSearchNavDistance != null) textSearchNavDistance.setText(distStr);
            }

            // Distância e progresso até o próximo passo/manobra da navegação
            double distToNextStep = (nextStepLocation != null)
                    ? currentLocation.distanceToAsDouble(nextStepLocation)
                    : remainingMeters;

            if (textNavDistance != null) {
                if (distToNextStep < 1000) textNavDistance.setText(String.format(Locale.getDefault(), "%.0fm", distToNextStep));
                else textNavDistance.setText(String.format(Locale.getDefault(), "%.1fkm", distToNextStep / 1000.0));
            }

            if (borderProgressDrawable != null && initialStepDistance > 0) {
                if (distToNextStep > initialStepDistance) {
                    initialStepDistance = distToNextStep;
                }
                double completedDist = Math.max(0, initialStepDistance - distToNextStep);
                float rawPct = (float) Math.max(0, Math.min(100, (completedDist / initialStepDistance) * 100.0));
                currentStepMaxProgressPct = Math.max(currentStepMaxProgressPct, rawPct);

                borderProgressDrawable.setProgress(currentStepMaxProgressPct);
                if (cardNavigationMode != null) {
                    cardNavigationMode.invalidate();
                }
            }

            if (currentlySelectedStop.id != lastAnnouncedStopId) {
                lastAnnouncedStopId = currentlySelectedStop.id;
                is100mAnnounced = false;
                isArrivedAnnounced = false;
            }

            if (remainingMeters <= 25.0 && !isArrivedAnnounced) {
                isArrivedAnnounced = true;
                is100mAnnounced = true;
                if (currentlySelectedStop.stopNumber == 0) {
                    speakVoiceNavigation("Você chegou em sua residência. Bom descanso!", true);
                    Toast.makeText(getContext(), "🏠 Você chegou em sua residência! Bom descanso!", Toast.LENGTH_LONG).show();
                } else {
                    String addr = currentlySelectedStop.address != null ? currentlySelectedStop.address : "";
                    speakVoiceNavigation("Você chegou na Parada #" + currentlySelectedStop.stopNumber + ": " + addr, true);
                }
            } else if (remainingMeters <= 100.0 && remainingMeters > 25.0 && !is100mAnnounced) {
                is100mAnnounced = true;
                if (currentlySelectedStop.stopNumber == 0) {
                    speakVoiceNavigation("Sua residência está a 100 metros à frente.", true);
                } else {
                    String addr = currentlySelectedStop.address != null ? currentlySelectedStop.address : "";
                    speakVoiceNavigation("Parada #" + currentlySelectedStop.stopNumber + " a 100 metros: " + addr, true);
                }
            }

            map.invalidate();
        }
    }

    private void animateNavigationCard(boolean show) {
        if (cardNavigationMode == null) return;

        boolean isOptPending = sharedPreferences != null && sharedPreferences.getBoolean("route_optimization_pending_" + currentRouteId, false);
        if (isOptPending) show = false;

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
                    .setInterpolator(new OvershootInterpolator(1.2f))
                    .start();
        } else {
            if (cardNavigationMode.getVisibility() != View.VISIBLE) return;

            cardNavigationMode.animate()
                    .alpha(0f)
                    .translationY(-30f * getResources().getDisplayMetrics().density)
                    .scaleX(0.85f)
                    .scaleY(0.85f)
                    .setDuration(400)
                    .setInterpolator(new AnticipateInterpolator())
                    .withEndAction(() -> cardNavigationMode.setVisibility(View.GONE))
                    .start();
        }
    }



    private String parseInstruction(String type, String modifier, String name) {
        boolean hasName = name != null && !name.trim().isEmpty() && !name.equalsIgnoreCase("null") && !name.equalsIgnoreCase("unnamed");
        String formattedName = hasName ? name.trim() : "";
        String mod = modifier != null ? modifier.toLowerCase(Locale.ROOT) : "";

        if ("arrive".equals(type)) {
            return "Você chegou ao destino";
        }

        // 1. Se houver indicação de curva/virada no modificador (direita ou esquerda), essa é a instrução principal!
        String turnBase = null;
        if (mod.contains("sharp right")) {
            turnBase = "Curva acentuada à direita";
        } else if (mod.contains("sharp left")) {
            turnBase = "Curva acentuada à esquerda";
        } else if (mod.contains("slight right")) {
            turnBase = "Mantenha à direita";
        } else if (mod.contains("slight left")) {
            turnBase = "Mantenha à esquerda";
        } else if (mod.contains("right")) {
            turnBase = "Dobre à direita";
        } else if (mod.contains("left")) {
            turnBase = "Dobre à esquerda";
        } else if (mod.contains("uturn") || "uturn".equals(type)) {
            turnBase = "Faça o retorno";
        }

        if (turnBase != null) {
            if (hasName) {
                return turnBase + " na " + formattedName;
            }
            return turnBase;
        }

        // 2. Se for uma continuação sem virada (linha reta, mudança de nome da via, rotatória, etc)
        if ("roundabout".equals(type) || "rotary".equals(type)) {
            if (hasName) return "Na rotatória, saia em direção a " + formattedName;
            return "Na rotatória, pegue a saída";
        }

        if ("merge".equals(type)) {
            if (hasName) return "Acesse a via " + formattedName;
            return "Acesse a via";
        }

        if ("end of road".equals(type)) {
            if (hasName) return "No fim da via, siga na " + formattedName;
            return "No fim da via";
        }

        if ("depart".equals(type)) {
            if (hasName) return "Siga na " + formattedName;
            return "Siga em frente";
        }

        if ("new name".equals(type)) {
            if (hasName) return "Siga pela " + formattedName;
            return "Siga em frente";
        }

        if (hasName) {
            return "Siga na " + formattedName;
        }

        return "Siga em frente";
    }

    private int getManeuverIcon(String type, String modifier) {
        String mod = modifier != null ? modifier.toLowerCase(Locale.ROOT) : "";
        if ("uturn".equals(type) || mod.contains("uturn")) return R.drawable.ic_nav_uturn;
        if ("roundabout".equals(type) || "rotary".equals(type)) return R.drawable.ic_nav_roundabout;
        if (mod.contains("right")) return R.drawable.ic_nav_turn_right;
        if (mod.contains("left")) return R.drawable.ic_nav_turn_left;
        if ("arrive".equals(type)) return android.R.drawable.ic_menu_myplaces;
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
        if (getView() != null) ViewCompat.requestApplyInsets(getView());
        timerHandler.post(timerRunnable);
        animationHandler.post(markerAnimationRunnable);
        if (menuAnimRunnable != null) menuAnimHandler.post(menuAnimRunnable);
        
        // Registrar receiver para nova rota
        if (getContext() != null) {
            IntentFilter filter = new IntentFilter("com.example.entregas.ACTION_NEW_ROUTE");
            ContextCompat.registerReceiver(requireContext(), newRouteReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        }

        if (sensorManager != null) {
            Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            Sensor magnet = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            if (rotationVectorSensor != null) sensorManager.registerListener(compassListener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI);
            if (accel != null) sensorManager.registerListener(compassListener, accel, SensorManager.SENSOR_DELAY_UI);
            if (magnet != null) sensorManager.registerListener(compassListener, magnet, SensorManager.SENSOR_DELAY_UI);
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
            SharedPreferences prefs = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
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
        if (menuAnimRunnable != null) menuAnimHandler.removeCallbacks(menuAnimRunnable);
        
        // 🔥 SALVAR ESTADO DO MAPA PARA CARREGAMENTO RÁPIDO NO PRÓXIMO INÍCIO
        if (map != null) {
            sharedPreferences.edit()
                .putFloat("last_map_lat", (float) map.getMapCenter().getLatitude())
                .putFloat("last_map_lon", (float) map.getMapCenter().getLongitude())
                .putFloat("last_map_zoom", (float) map.getZoomLevelDouble())
                .apply();
        }

        timerHandler.removeCallbacks(timerRunnable);
        animationHandler.removeCallbacks(markerAnimationRunnable);
        if (sensorManager != null) sensorManager.unregisterListener(compassListener);
        if (networkCallback != null && getContext() != null) {
            ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
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
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser(); if (u == null || u.getEmail() == null) return;
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
                try { byte[] b = Base64.decode(fl.avatar, Base64.DEFAULT); Bitmap bmp = BitmapFactory.decodeByteArray(b, 0, b.length); if (bmp != null) m.setIcon(new BitmapDrawable(getResources(), getCircularBitmapWithBorder(bmp, size))); } catch (Exception e) { setDefaultFriendIcon(m); }
            } else setDefaultFriendIcon(m);
        }
        Iterator<Map.Entry<String, Marker>> it = friendMarkers.entrySet().iterator();
        while (it.hasNext()) { Map.Entry<String, Marker> e = it.next(); if (!active.contains(e.getKey())) { map.getOverlays().remove(e.getValue()); it.remove(); } }
        map.invalidate();
    }

    private void setDefaultFriendIcon(Marker m) { Drawable d = ContextCompat.getDrawable(requireContext(), R.drawable.ic_car_marker); if (d != null) { d.setTint(Color.parseColor("#4CAF50")); m.setIcon(d); } }
    private Bitmap getCircularBitmapWithBorder(Bitmap bmp, int size) {
        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888); Canvas c = new Canvas(out); Paint p = new Paint(); p.setAntiAlias(true); float r = size / 2f;
        p.setColor(Color.WHITE); c.drawCircle(r, r, r, p); p.setColor(Color.parseColor("#2196F3")); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(4f); c.drawCircle(r, r, r - 2, p);
        p.setStyle(Paint.Style.FILL); p.setShader(new BitmapShader(Bitmap.createScaledBitmap(bmp, size, size, false), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)); c.drawCircle(r, r, r - 6, p);
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

        fabDeliveryApp.setElevation(0f);
        fabDeliveryApp.setCompatElevation(0f);
        fabDeliveryApp.setOutlineProvider(null);

        if (pkg.isEmpty()) {
            fabDeliveryApp.setImageResource(R.drawable.ic_map);
            fabDeliveryApp.setSupportBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
        } else {
            try {
                PackageManager pm = requireContext().getPackageManager();
                ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                Drawable icon = info.loadIcon(pm);
                fabDeliveryApp.setImageDrawable(icon);
                fabDeliveryApp.setSupportBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            } catch (Exception e) {
                fabDeliveryApp.setImageResource(R.drawable.ic_map);
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
                ResolveInfo resolveInfo = pm.queryIntentActivities(intent, 0).stream().findFirst().orElse(null);
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
            if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                return parseSafeDouble(cell.getStringCellValue());
            }
        } catch (Exception ignored) {}
        return 0.0;
    }

    private void processXlsxImport(Uri uri) {
        if (uri == null || getContext() == null) return;

        // Popup de progresso com animação
        View dv = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_optimization_progress, null);
        CircularProgressIndicator progress = dv.findViewById(R.id.progressOptimization);
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
                            Matcher mQ = Pattern.compile("(?i)(QUADRA|QD\\.?|QU?AD\\.?|Q\\.?|QDR\\.?) ?(\\d+[A-Z]?)").matcher(rawAddr);
                            Matcher mB = Pattern.compile("(?i)(BLOCO|BL\\.?|B\\.?|BLO?C\\.?) ?(\\d+[A-Z]?)").matcher(rawAddr);
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
                            Set<String> uniqueBuyers = new HashSet<>(Arrays.asList(existing.allAddresses.split("\n")));
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
                    
                    Collections.sort(finalStops, (a, b) -> Integer.compare(a.sortOrder, b.sortOrder));
                    for (int i = 0; i < finalStops.size(); i++) {
                        finalStops.get(i).sortOrder = i;
                        finalStops.get(i).stopNumber = i + 1;
                    }

                    // Grava as paradas importadas no banco de dados limpando a versão anterior da rota
                    dao.clearRouteStopsByRoute(targetRouteId);
                    for (RouteStop s : finalStops) s.id = 0;
                    dao.insertRouteStops(finalStops);

                    sharedPreferences.edit().putBoolean("route_optimization_pending_" + targetRouteId, true).apply();

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> { 
                            importDialog.dismiss();
                            showImportPreviewDialog(targetRouteId, finalStops);
                        });
                    }
                } else {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            importDialog.dismiss();
                            Toast.makeText(getContext(), "Nenhuma parada encontrada na planilha", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
                workbook.close();
            } catch (Exception e) { 
                e.printStackTrace(); 
                if (getActivity() != null) getActivity().runOnUiThread(importDialog::dismiss);
            }
        }).start();
    }

    private void showImportPreviewDialog(int targetRouteId, List<RouteStop> initialStops) {
        if (getContext() == null || initialStops == null || initialStops.isEmpty()) return;

        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_import_preview, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(v).setCancelable(false).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView textTitle = v.findViewById(R.id.textImportPreviewTitle);
        MaterialButton btnUnify = v.findViewById(R.id.btnUnifyPreviewSelected);
        MaterialButton btnSelectAll = v.findViewById(R.id.btnSelectAllPreview);
        RecyclerView recycler = v.findViewById(R.id.recyclerImportPreview);
        MaterialButton btnCancel = v.findViewById(R.id.btnCancelImportPreview);
        MaterialButton btnConfirm = v.findViewById(R.id.btnConfirmImportPreview);

        Spinner spinnerOptEngine = v.findViewById(R.id.spinnerOptEngine);
        Spinner spinnerStartMode = v.findViewById(R.id.spinnerStartMode);
        LinearLayout layoutStartPicker = v.findViewById(R.id.layoutStartStopPicker);
        Spinner spinnerStartStop = v.findViewById(R.id.spinnerStartStop);

        List<RouteStop> previewList = new ArrayList<>(initialStops);
        renumberPreviewList(previewList);

        if (textTitle != null) {
            textTitle.setText("📋 Prévia da Rota (" + previewList.size() + " Paradas)");
        }

        String[] engineOptions = {
            "🧠 Otimização 3.0 (Combustível - TSP 2-Opt)",
            "⚡ Otimização 2.0 (Padrão OSRM)",
            "📄 Ordem Original (Planilha Importada)"
        };
        ArrayAdapter<String> engineAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, engineOptions);
        if (spinnerOptEngine != null) {
            spinnerOptEngine.setAdapter(engineAdapter);
            spinnerOptEngine.setSelection(0); // 3.0 é o padrão!
        }

        String[] startModeOptions = {
            "📍 Localização Atual (GPS)",
            "🎯 A partir de uma Parada específica"
        };
        ArrayAdapter<String> startModeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, startModeOptions);
        if (spinnerStartMode != null) {
            spinnerStartMode.setAdapter(startModeAdapter);
            spinnerStartMode.setSelection(0); // GPS é o padrão!
            spinnerStartMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (layoutStartPicker != null) {
                        layoutStartPicker.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
                    }
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        List<String> stopTitles = new ArrayList<>();
        for (int i = 0; i < previewList.size(); i++) {
            RouteStop s = previewList.get(i);
            stopTitles.add("Parada #" + (i + 1) + ": " + (s.address != null ? s.address : ""));
        }
        ArrayAdapter<String> startStopAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, stopTitles);
        if (spinnerStartStop != null) spinnerStartStop.setAdapter(startStopAdapter);

        Set<Integer> selectedIndices = new HashSet<>();
        final ImportPreviewAdapter[] previewAdapterHolder = new ImportPreviewAdapter[1];
        final ItemTouchHelper[] previewTouchHelperHolder = new ItemTouchHelper[1];

        ItemTouchHelper previewTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int fromPos = viewHolder.getBindingAdapterPosition();
                int toPos = target.getBindingAdapterPosition();

                if (fromPos != RecyclerView.NO_POSITION && toPos != RecyclerView.NO_POSITION && fromPos != toPos) {
                    Collections.swap(previewList, fromPos, toPos);
                    renumberPreviewList(previewList);
                    selectedIndices.clear();
                    if (previewAdapterHolder[0] != null) {
                        previewAdapterHolder[0].notifyItemMoved(fromPos, toPos);
                        previewAdapterHolder[0].notifyItemRangeChanged(0, previewList.size());
                    }
                    if (textTitle != null) textTitle.setText("📋 Prévia da Rota (" + previewList.size() + " Paradas)");
                    if (btnUnify != null) btnUnify.setText("Unificar (0)");
                    if (btnSelectAll != null) btnSelectAll.setText("Marcar Todos");
                    return true;
                }
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                    viewHolder.itemView.setAlpha(0.8f);
                    viewHolder.itemView.setScaleX(1.02f);
                    viewHolder.itemView.setScaleY(1.02f);
                }
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                viewHolder.itemView.setAlpha(1.0f);
                viewHolder.itemView.setScaleX(1.0f);
                viewHolder.itemView.setScaleY(1.0f);
                renumberPreviewList(previewList);
                if (previewAdapterHolder[0] != null) {
                    previewAdapterHolder[0].notifyDataSetChanged();
                }
            }
        });
        previewTouchHelperHolder[0] = previewTouchHelper;

        ImportPreviewAdapter previewAdapter = new ImportPreviewAdapter(previewList, selectedIndices, new ImportPreviewAdapter.OnPreviewInteractionListener() {
            @Override
            public void onSelectionChanged() {
                if (btnUnify != null) btnUnify.setText("Unificar (" + selectedIndices.size() + ")");
                if (btnSelectAll != null) btnSelectAll.setText(selectedIndices.size() == previewList.size() ? "Desmarcar Todos" : "Marcar Todos");
            }

            @Override
            public void onDelete(int position) {
                if (position >= 0 && position < previewList.size()) {
                    previewList.remove(position);
                    selectedIndices.clear();
                    renumberPreviewList(previewList);
                    if (previewAdapterHolder[0] != null) previewAdapterHolder[0].notifyDataSetChanged();
                    if (textTitle != null) textTitle.setText("📋 Prévia da Rota (" + previewList.size() + " Paradas)");
                    if (btnUnify != null) btnUnify.setText("Unificar (0)");
                    if (btnSelectAll != null) btnSelectAll.setText("Marcar Todos");
                }
            }

            @Override
            public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
                if (previewTouchHelperHolder[0] != null) {
                    previewTouchHelperHolder[0].startDrag(viewHolder);
                }
            }
        });
        previewAdapterHolder[0] = previewAdapter;

        MaterialCardView cardGroupingSuggestions = v.findViewById(R.id.cardGroupingSuggestions);
        TextView textGroupingSuggestions = v.findViewById(R.id.textGroupingSuggestions);

        Runnable updateSuggestionsUI = () -> {
            if (cardGroupingSuggestions == null || textGroupingSuggestions == null) return;
            int suggestedClustersCount = calculateGroupingSuggestionsCount(previewList);
            if (suggestedClustersCount > 0) {
                cardGroupingSuggestions.setVisibility(View.VISIBLE);
                textGroupingSuggestions.setText("💡 " + suggestedClustersCount + " Sugestão(ões) de Agrupamento encontradas (mesmo local/condomínio). Clique para unificar!");
            } else {
                cardGroupingSuggestions.setVisibility(View.GONE);
            }
        };

        updateSuggestionsUI.run();

        if (cardGroupingSuggestions != null) {
            cardGroupingSuggestions.setOnClickListener(v2 -> {
                showGroupingSuggestionsReviewDialog(previewList, previewAdapterHolder[0], textTitle, btnUnify, stopTitles, startStopAdapter, updateSuggestionsUI);
            });
        }

        if (recycler != null) {
            recycler.setLayoutManager(new LinearLayoutManager(getContext()));
            recycler.setAdapter(previewAdapter);
            previewTouchHelper.attachToRecyclerView(recycler);
        }

        if (btnSelectAll != null) {
            btnSelectAll.setOnClickListener(v2 -> {
                if (selectedIndices.size() == previewList.size()) {
                    selectedIndices.clear();
                } else {
                    for (int i = 0; i < previewList.size(); i++) selectedIndices.add(i);
                }
                previewAdapter.notifyDataSetChanged();
                if (btnUnify != null) btnUnify.setText("Unificar (" + selectedIndices.size() + ")");
                btnSelectAll.setText(selectedIndices.size() == previewList.size() ? "Desmarcar Todos" : "Marcar Todos");
            });
        }

        if (btnUnify != null) {
            btnUnify.setOnClickListener(v2 -> {
                if (selectedIndices.size() < 2) {
                    Toast.makeText(getContext(), "Selecione pelo menos 2 paradas para unificar!", Toast.LENGTH_SHORT).show();
                    return;
                }
                List<Integer> sortedIndices = new ArrayList<>(selectedIndices);
                Collections.sort(sortedIndices);

                RouteStop master = previewList.get(sortedIndices.get(0));
                StringBuilder combinedAddresses = new StringBuilder(master.allAddresses != null ? master.allAddresses : master.address);
                StringBuilder combinedSequences = new StringBuilder(master.allSequences != null ? master.allSequences : (master.sequence > 0 ? String.valueOf(master.sequence) : ""));
                int totalPackages = master.packageCount;
                Set<String> uniqueBuyers = new HashSet<>();
                if (master.allAddresses != null) {
                    for (String addr : master.allAddresses.split("\n")) uniqueBuyers.add(addr.trim());
                } else {
                    uniqueBuyers.add(master.address.trim());
                }

                for (int i = 1; i < sortedIndices.size(); i++) {
                    RouteStop other = previewList.get(sortedIndices.get(i));
                    combinedAddresses.append("\n").append(other.allAddresses != null ? other.allAddresses : other.address);
                    String oSeq = other.allSequences != null ? other.allSequences : (other.sequence > 0 ? String.valueOf(other.sequence) : "");
                    if (!oSeq.isEmpty()) combinedSequences.append(", ").append(oSeq);
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

                for (int i = sortedIndices.size() - 1; i >= 1; i--) {
                    int removeIdx = sortedIndices.get(i);
                    previewList.remove(removeIdx);
                }

                selectedIndices.clear();
                renumberPreviewList(previewList);
                previewAdapter.notifyDataSetChanged();
                if (textTitle != null) textTitle.setText("📋 Prévia da Rota (" + previewList.size() + " Paradas)");
                btnUnify.setText("Unificar (0)");
                if (btnSelectAll != null) btnSelectAll.setText("Marcar Todos");
                Toast.makeText(getContext(), "Paradas unificadas com sucesso!", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnCancel != null) {
            btnCancel.setText("FECHAR");
            btnCancel.setOnClickListener(v2 -> {
                dialog.dismiss();
                saveImportedStopsToDatabase(targetRouteId, previewList);
            });
        }

        if (btnConfirm != null) {
            btnConfirm.setText("⚡ Otimizar e Iniciar Rota");
            btnConfirm.setOnClickListener(v2 -> {
                boolean isGpsMode = (spinnerStartMode == null || spinnerStartMode.getSelectedItemPosition() == 0);
                int chosenStartIdx = (spinnerStartStop != null && !isGpsMode) ? spinnerStartStop.getSelectedItemPosition() : 0;
                int selectedEngineIndex = (spinnerOptEngine != null) ? spinnerOptEngine.getSelectedItemPosition() : 0;

                Runnable saveAndDismiss = () -> {
                    dialog.dismiss();
                    sharedPreferences.edit().remove("route_optimization_pending_" + targetRouteId).apply();
                    saveImportedStopsToDatabase(targetRouteId, previewList);
                };

                if (selectedEngineIndex == 2) { // Ordem Original
                    applyOriginalOrder(previewList, isGpsMode, chosenStartIdx, saveAndDismiss);
                } else if (selectedEngineIndex == 1) { // 2.0
                    optimizePreviewListV2(previewList, isGpsMode, chosenStartIdx, saveAndDismiss);
                } else { // 3.0 (padrão)
                    optimizePreviewListV3(previewList, isGpsMode, chosenStartIdx, saveAndDismiss);
                }
            });
        }

        dialog.show();
    }

    private int calculateGroupingSuggestionsCount(List<RouteStop> list) {
        if (list == null || list.size() < 2) return 0;
        int clusters = 0;
        Set<Integer> processed = new HashSet<>();

        for (int i = 0; i < list.size(); i++) {
            if (processed.contains(i)) continue;
            RouteStop a = list.get(i);
            boolean foundPair = false;

            for (int j = i + 1; j < list.size(); j++) {
                if (processed.contains(j)) continue;
                RouteStop b = list.get(j);

                if (isSameLocationOrAddress(a, b)) {
                    processed.add(j);
                    foundPair = true;
                }
            }
            if (foundPair) {
                processed.add(i);
                clusters++;
            }
        }
        return clusters;
    }

    public static class GroupingSuggestion {
        public RouteStop master;
        public List<RouteStop> matchedStops = new ArrayList<>();
        public String reason;
        public boolean isChecked = true;
    }

    private List<GroupingSuggestion> findGroupingSuggestions(List<RouteStop> list) {
        List<GroupingSuggestion> suggestions = new ArrayList<>();
        if (list == null || list.size() < 2) return suggestions;

        Set<Integer> processed = new HashSet<>();

        for (int i = 0; i < list.size(); i++) {
            if (processed.contains(i)) continue;
            RouteStop master = list.get(i);
            GroupingSuggestion sugg = new GroupingSuggestion();
            sugg.master = master;

            double minDistanceFound = Double.MAX_VALUE;
            boolean sameAddressMatch = false;

            for (int j = i + 1; j < list.size(); j++) {
                if (processed.contains(j)) continue;
                RouteStop other = list.get(j);

                if (isSameLocationOrAddress(master, other)) {
                    sugg.matchedStops.add(other);
                    processed.add(j);

                    if (master.latitude != 0 && master.longitude != 0 && other.latitude != 0 && other.longitude != 0) {
                        float[] res = new float[1];
                        Location.distanceBetween(master.latitude, master.longitude, other.latitude, other.longitude, res);
                        if (res[0] < minDistanceFound) minDistanceFound = res[0];
                    }

                    if (master.address != null && other.address != null && master.address.equalsIgnoreCase(other.address)) {
                        sameAddressMatch = true;
                    }
                }
            }

            if (!sugg.matchedStops.isEmpty()) {
                processed.add(i);
                if (sameAddressMatch) {
                    sugg.reason = "📍 Mesmo Endereço / Condomínio";
                } else if (minDistanceFound < Double.MAX_VALUE) {
                    sugg.reason = String.format(Locale.getDefault(), "📏 Distância Próxima (%.0fm)", minDistanceFound);
                } else {
                    sugg.reason = "📍 Mesmo Local";
                }
                suggestions.add(sugg);
            }
        }

        return suggestions;
    }

    private void showGroupingSuggestionsReviewDialog(
            List<RouteStop> previewList,
            ImportPreviewAdapter adapter,
            TextView textTitle,
            MaterialButton btnUnify,
            List<String> stopTitles,
            ArrayAdapter<String> startStopAdapter,
            Runnable updateSuggestionsUI) {

        if (getContext() == null || previewList == null) return;

        List<GroupingSuggestion> suggestions = findGroupingSuggestions(previewList);
        if (suggestions.isEmpty()) {
            Toast.makeText(getContext(), "Nenhuma sugestão de agrupamento encontrada.", Toast.LENGTH_SHORT).show();
            return;
        }

        Context ctx = requireContext();
        LinearLayout mainLayout = new LinearLayout(ctx);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(32, 24, 32, 8);

        TextView textDialogTitle = new TextView(ctx);
        textDialogTitle.setText("💡 Sugestões de Agrupamento (" + suggestions.size() + ")");
        textDialogTitle.setTextSize(16);
        textDialogTitle.setTypeface(null, Typeface.BOLD);
        textDialogTitle.setTextColor(0xFF1976D2);
        mainLayout.addView(textDialogTitle);

        TextView textSub = new TextView(ctx);
        textSub.setText("Verifique os grupos encontrados e desmarque qualquer um que não seja viável antes de unificar.");
        textSub.setTextSize(12);
        textSub.setTextColor(0xFF666666);
        textSub.setPadding(0, 4, 0, 16);
        mainLayout.addView(textSub);

        ScrollView scrollView = new ScrollView(ctx);
        LinearLayout itemsLayout = new LinearLayout(ctx);
        itemsLayout.setOrientation(LinearLayout.VERTICAL);

        for (int i = 0; i < suggestions.size(); i++) {
            GroupingSuggestion sugg = suggestions.get(i);

            MaterialCardView card = new MaterialCardView(ctx);
            card.setRadius(12f);
            card.setCardElevation(2f);
            card.setStrokeWidth(1);
            card.setStrokeColor(0xFFBBDEFB);
            card.setCardBackgroundColor(0xFFF1F8E9);
            
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, 12);
            card.setLayoutParams(cardParams);

            LinearLayout container = new LinearLayout(ctx);
            container.setOrientation(LinearLayout.HORIZONTAL);
            container.setPadding(12, 12, 12, 12);
            container.setGravity(Gravity.CENTER_VERTICAL);

            CheckBox checkBox = new CheckBox(ctx);
            checkBox.setChecked(sugg.isChecked);
            checkBox.setOnCheckedChangeListener((cb, isChecked) -> sugg.isChecked = isChecked);
            container.addView(checkBox);

            LinearLayout infoLayout = new LinearLayout(ctx);
            infoLayout.setOrientation(LinearLayout.VERTICAL);
            infoLayout.setPadding(8, 0, 0, 0);

            TextView textReason = new TextView(ctx);
            textReason.setText(sugg.reason + " • Total: " + (sugg.master.packageCount + countPackages(sugg.matchedStops)) + " pacotes");
            textReason.setTextSize(11);
            textReason.setTypeface(null, Typeface.BOLD);
            textReason.setTextColor(0xFF2E7D32);
            infoLayout.addView(textReason);

            TextView textMaster = new TextView(ctx);
            textMaster.setText("📍 Parada Principal: " + (sugg.master.address != null ? sugg.master.address : ""));
            textMaster.setTextSize(12);
            textMaster.setTypeface(null, Typeface.BOLD);
            textMaster.setTextColor(0xFF333333);
            infoLayout.addView(textMaster);

            for (RouteStop m : sugg.matchedStops) {
                TextView textMatched = new TextView(ctx);
                textMatched.setText("   └ Unificar com: " + (m.address != null ? m.address : ""));
                textMatched.setTextSize(11);
                textMatched.setTextColor(0xFF555555);
                infoLayout.addView(textMatched);
            }

            container.addView(infoLayout);
            card.addView(container);

            card.setOnClickListener(v -> checkBox.toggle());

            itemsLayout.addView(card);
        }

        scrollView.addView(itemsLayout);
        mainLayout.addView(scrollView);

        new AlertDialog.Builder(ctx)
                .setView(mainLayout)
                .setPositiveButton("UNIFICAR SELECIONADOS", (dialog, which) -> {
                    int unifiedCount = applyApprovedGroupUnification(previewList, suggestions);
                    if (unifiedCount > 0) {
                        renumberPreviewList(previewList);
                        if (adapter != null) adapter.notifyDataSetChanged();
                        if (textTitle != null) textTitle.setText("📋 Prévia da Rota (" + previewList.size() + " Paradas)");
                        if (btnUnify != null) btnUnify.setText("Unificar (0)");

                        if (stopTitles != null && startStopAdapter != null) {
                            stopTitles.clear();
                            for (int i = 0; i < previewList.size(); i++) {
                                RouteStop s = previewList.get(i);
                                stopTitles.add("Parada #" + (i + 1) + ": " + (s.address != null ? s.address : ""));
                            }
                            startStopAdapter.notifyDataSetChanged();
                        }

                        if (updateSuggestionsUI != null) updateSuggestionsUI.run();
                        Toast.makeText(getContext(), "⚡ " + unifiedCount + " grupo(s) unificados com sucesso!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Nenhum grupo selecionado.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("CANCELAR", null)
                .show();
    }

    private int countPackages(List<RouteStop> stops) {
        if (stops == null) return 0;
        int total = 0;
        for (RouteStop s : stops) total += s.packageCount;
        return total;
    }

    private int applyApprovedGroupUnification(List<RouteStop> previewList, List<GroupingSuggestion> suggestions) {
        if (previewList == null || suggestions == null) return 0;
        int count = 0;

        for (GroupingSuggestion sugg : suggestions) {
            if (!sugg.isChecked || sugg.matchedStops.isEmpty()) continue;

            RouteStop master = sugg.master;
            StringBuilder combinedAddresses = new StringBuilder(master.allAddresses != null ? master.allAddresses : master.address);
            StringBuilder combinedSequences = new StringBuilder(master.allSequences != null ? master.allSequences : (master.sequence > 0 ? String.valueOf(master.sequence) : ""));
            int totalPackages = master.packageCount;
            Set<String> uniqueBuyers = new HashSet<>();
            if (master.allAddresses != null) {
                for (String addr : master.allAddresses.split("\n")) uniqueBuyers.add(addr.trim());
            } else {
                uniqueBuyers.add(master.address.trim());
            }

            for (RouteStop other : sugg.matchedStops) {
                combinedAddresses.append("\n").append(other.allAddresses != null ? other.allAddresses : other.address);
                String oSeq = other.allSequences != null ? other.allSequences : (other.sequence > 0 ? String.valueOf(other.sequence) : "");
                if (!oSeq.isEmpty()) combinedSequences.append(", ").append(oSeq);
                totalPackages += other.packageCount;
                if (other.allAddresses != null) {
                    for (String addr : other.allAddresses.split("\n")) uniqueBuyers.add(addr.trim());
                } else {
                    uniqueBuyers.add(other.address.trim());
                }
                previewList.remove(other);
            }

            master.allAddresses = combinedAddresses.toString();
            master.allSequences = combinedSequences.toString();
            master.packageCount = totalPackages;
            master.buyerCount = uniqueBuyers.size();

            count++;
        }

        return count;
    }

    private boolean isSameLocationOrAddress(RouteStop a, RouteStop b) {
        if (a == null || b == null) return false;
        
        if (a.latitude != 0 && a.longitude != 0 && b.latitude != 0 && b.longitude != 0) {
            float[] res = new float[1];
            Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, res);
            if (res[0] < 15.0) return true;
        }

        if (a.address != null && b.address != null) {
            String normA = Normalizer.normalize(a.address.toUpperCase(), Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "").replaceAll("[.,\\-]", " ").replaceAll("\\s+", " ").trim();
            String normB = Normalizer.normalize(b.address.toUpperCase(), Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "").replaceAll("[.,\\-]", " ").replaceAll("\\s+", " ").trim();
            if (!normA.isEmpty() && normA.equalsIgnoreCase(normB)) return true;
        }

        return false;
    }

    private int applySuggestedGroupUnification(List<RouteStop> list) {
        if (list == null || list.size() < 2) return 0;

        int count = 0;
        boolean foundAny = true;

        while (foundAny) {
            foundAny = false;
            for (int i = 0; i < list.size(); i++) {
                RouteStop master = list.get(i);
                List<Integer> matches = new ArrayList<>();

                for (int j = i + 1; j < list.size(); j++) {
                    RouteStop other = list.get(j);
                    if (isSameLocationOrAddress(master, other)) {
                        matches.add(j);
                    }
                }

                if (!matches.isEmpty()) {
                    foundAny = true;
                    count++;

                    StringBuilder combinedAddresses = new StringBuilder(master.allAddresses != null ? master.allAddresses : master.address);
                    StringBuilder combinedSequences = new StringBuilder(master.allSequences != null ? master.allSequences : (master.sequence > 0 ? String.valueOf(master.sequence) : ""));
                    int totalPackages = master.packageCount;
                    Set<String> uniqueBuyers = new HashSet<>();
                    if (master.allAddresses != null) {
                        for (String addr : master.allAddresses.split("\n")) uniqueBuyers.add(addr.trim());
                    } else {
                        uniqueBuyers.add(master.address.trim());
                    }

                    for (int idx : matches) {
                        RouteStop other = list.get(idx);
                        combinedAddresses.append("\n").append(other.allAddresses != null ? other.allAddresses : other.address);
                        String oSeq = other.allSequences != null ? other.allSequences : (other.sequence > 0 ? String.valueOf(other.sequence) : "");
                        if (!oSeq.isEmpty()) combinedSequences.append(", ").append(oSeq);
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

                    for (int k = matches.size() - 1; k >= 0; k--) {
                        list.remove((int) matches.get(k));
                    }
                    break;
                }
            }
        }
        return count;
    }

    private void promptFinishOptimizationNow() {
        if (currentRouteId == -1) {
            if (getContext() != null) Toast.makeText(getContext(), "Nenhuma rota ativa selecionada.", Toast.LENGTH_SHORT).show();
            return;
        }

        final Context ctx = getContext();
        if (ctx == null) return;

        new Thread(() -> {
            AppDao dao = AppDatabase.getInstance(ctx).appDao();
            List<RouteStop> stops = dao.getStopsForRoute(currentRouteId);
            if (stops == null || stops.isEmpty()) {
                Activity act = getActivity();
                if (act != null) {
                    act.runOnUiThread(() -> Toast.makeText(ctx, "Nenhuma parada encontrada nesta rota.", Toast.LENGTH_SHORT).show());
                }
                return;
            }

            Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(() -> showImportPreviewDialog(currentRouteId, stops));
            }
        }).start();
    }

    private void applyOriginalOrder(List<RouteStop> previewList, boolean isGpsMode, int chosenStartIdx, Runnable onComplete) {
        if (previewList == null || previewList.isEmpty()) return;

        RouteStop selectedFirst = (!isGpsMode && chosenStartIdx >= 0 && chosenStartIdx < previewList.size()) 
                ? previewList.get(chosenStartIdx) 
                : null;

        Collections.sort(previewList, (a, b) -> {
            if (a.sequence > 0 && b.sequence > 0) {
                return Integer.compare(a.sequence, b.sequence);
            }
            return Integer.compare(a.sortOrder, b.sortOrder);
        });

        if (selectedFirst != null) {
            int newPos = previewList.indexOf(selectedFirst);
            if (newPos > 0) {
                List<RouteStop> reordered = new ArrayList<>();
                for (int i = newPos; i < previewList.size(); i++) {
                    reordered.add(previewList.get(i));
                }
                for (int i = 0; i < newPos; i++) {
                    reordered.add(previewList.get(i));
                }
                previewList.clear();
                previewList.addAll(reordered);
            }
        }

        renumberPreviewList(previewList);

        if (getContext() != null) {
            Toast.makeText(getContext(), "📄 Ordem original da planilha aplicada!", Toast.LENGTH_SHORT).show();
        }

        if (onComplete != null) {
            onComplete.run();
        }
    }

    private void promptApplyOriginalOrder() {
        if (currentRouteId == -1 || getContext() == null) return;

        new Thread(() -> {
            AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
            List<RouteStop> stops = dao.getStopsForRoute(currentRouteId);
            if (stops == null || stops.isEmpty()) return;

            Activity activity = getActivity();
            if (activity == null) return;

            activity.runOnUiThread(() -> {
                applyOriginalOrder(stops, true, 0, () -> {
                    sharedPreferences.edit().remove("route_optimization_pending_" + currentRouteId).apply();
                    saveImportedStopsToDatabase(currentRouteId, stops);
                });
            });
        }).start();
    }

    private void promptOptimizeRouteV3() {
        if (currentRouteId == -1 || getContext() == null) return;

        new Thread(() -> {
            AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
            List<RouteStop> stops = dao.getStopsForRoute(currentRouteId);
            if (stops == null || stops.isEmpty()) {
                Activity act = getActivity();
                if (act != null) {
                    act.runOnUiThread(() -> Toast.makeText(requireContext(), "Nenhuma parada encontrada nesta rota.", Toast.LENGTH_SHORT).show());
                }
                return;
            }

            Activity activity = getActivity();
            if (activity == null) return;

            activity.runOnUiThread(() -> {
                View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_import_preview, null);
                
                View btnUnify = v.findViewById(R.id.btnUnifyPreviewSelected);
                View btnSelectAll = v.findViewById(R.id.btnSelectAllPreview);
                View recycler = v.findViewById(R.id.recyclerImportPreview);
                if (btnUnify != null) btnUnify.setVisibility(View.GONE);
                if (btnSelectAll != null) btnSelectAll.setVisibility(View.GONE);
                if (recycler != null) recycler.setVisibility(View.GONE);

                TextView textTitle = v.findViewById(R.id.textImportPreviewTitle);
                if (textTitle != null) textTitle.setText("🧠 Otimização 3.0 (Combustível)");

                Spinner spinnerOptEngine = v.findViewById(R.id.spinnerOptEngine);
                Spinner spinnerStartMode = v.findViewById(R.id.spinnerStartMode);
                LinearLayout layoutStartPicker = v.findViewById(R.id.layoutStartStopPicker);
                Spinner spinnerStartStop = v.findViewById(R.id.spinnerStartStop);

                String[] engineOptions = {
                    "🧠 Otimização 3.0 (Combustível - TSP 2-Opt)",
                    "⚡ Otimização 2.0 (Padrão OSRM)",
                    "📄 Ordem Original (Planilha Importada)"
                };
                ArrayAdapter<String> engineAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, engineOptions);
                if (spinnerOptEngine != null) {
                    spinnerOptEngine.setAdapter(engineAdapter);
                    spinnerOptEngine.setSelection(0);
                }

                String[] startModeOptions = {
                    "📍 Localização Atual (GPS)",
                    "🎯 A partir de uma Parada específica"
                };
                ArrayAdapter<String> startModeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, startModeOptions);
                if (spinnerStartMode != null) {
                    spinnerStartMode.setAdapter(startModeAdapter);
                    spinnerStartMode.setSelection(0);
                    spinnerStartMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            if (layoutStartPicker != null) {
                                layoutStartPicker.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
                            }
                        }
                        @Override public void onNothingSelected(AdapterView<?> parent) {}
                    });
                }

                List<String> stopTitles = new ArrayList<>();
                for (int i = 0; i < stops.size(); i++) {
                    RouteStop s = stops.get(i);
                    stopTitles.add("Parada #" + (i + 1) + ": " + (s.address != null ? s.address : ""));
                }
                ArrayAdapter<String> startStopAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, stopTitles);
                if (spinnerStartStop != null) spinnerStartStop.setAdapter(startStopAdapter);

                MaterialButton btnCancel = v.findViewById(R.id.btnCancelImportPreview);
                MaterialButton btnConfirm = v.findViewById(R.id.btnConfirmImportPreview);

                AlertDialog optDialog = new AlertDialog.Builder(requireContext()).setView(v).create();
                if (optDialog.getWindow() != null) optDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

                if (btnCancel != null) {
                    btnCancel.setText("CANCELAR");
                    btnCancel.setOnClickListener(v2 -> optDialog.dismiss());
                }

                if (btnConfirm != null) {
                    btnConfirm.setText("🧠 EXECUTAR OTIMIZAÇÃO 3.0");
                    btnConfirm.setOnClickListener(v2 -> {
                        optDialog.dismiss();
                        boolean isGpsMode = (spinnerStartMode == null || spinnerStartMode.getSelectedItemPosition() == 0);
                        int chosenStartIdx = (spinnerStartStop != null && !isGpsMode) ? spinnerStartStop.getSelectedItemPosition() : 0;
                        int selectedEngineIndex = (spinnerOptEngine != null) ? spinnerOptEngine.getSelectedItemPosition() : 0;

                        Runnable saveAndDismiss = () -> {
                            sharedPreferences.edit().remove("route_optimization_pending_" + currentRouteId).apply();
                            saveImportedStopsToDatabase(currentRouteId, stops);
                        };

                        if (selectedEngineIndex == 2) {
                            applyOriginalOrder(stops, isGpsMode, chosenStartIdx, saveAndDismiss);
                        } else if (selectedEngineIndex == 1) {
                            optimizePreviewListV2(stops, isGpsMode, chosenStartIdx, saveAndDismiss);
                        } else {
                            optimizePreviewListV3(stops, isGpsMode, chosenStartIdx, saveAndDismiss);
                        }
                    });
                }

                optDialog.show();
            });
        }).start();
    }

    private void optimizePreviewListV2(List<RouteStop> previewList, boolean isGpsMode, int chosenStartIdx, Runnable onComplete) {
        if (getContext() == null || previewList == null || previewList.isEmpty()) return;

        View dv = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_optimization_progress, null);
        CircularProgressIndicator progress = dv.findViewById(R.id.progressOptimization);
        TextView textStatus = dv.findViewById(R.id.textOptimizationStatus);
        TextView textPercent = dv.findViewById(R.id.textOptimizationPercent);

        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(dv).setCancelable(false).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();

        new Thread(() -> {
            try {
                String[] phrases = {
                    "Analisando paradas para otimização 2.0...",
                    "Mapeando com rota viária inteligente OSRM...",
                    "Calculando menor percurso e melhor sequência...",
                    "Eliminando voltas e otimizando paradas...",
                    "Finalizando sequência perfeita..."
                };

                for (int i = 0; i < phrases.length; i++) {
                    final String msg = phrases[i];
                    final int p = (i + 1) * (100 / phrases.length);
                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.runOnUiThread(() -> {
                            if (textStatus != null) textStatus.setText(msg);
                            if (progress != null) progress.setProgress(p);
                            if (textPercent != null) textPercent.setText(p + "%");
                        });
                    }
                    Thread.sleep(500);
                }

                List<RouteStop> source = new ArrayList<>(previewList);
                List<RouteStop> optimized = new ArrayList<>();

                GeoPoint current;
                if (isGpsMode) {
                    current = currentLocation != null ? currentLocation : (source.get(0).latitude != 0 ? new GeoPoint(source.get(0).latitude, source.get(0).longitude) : new GeoPoint(-10.0, -55.0));
                } else {
                    int startIdx = (chosenStartIdx >= 0 && chosenStartIdx < source.size()) ? chosenStartIdx : 0;
                    RouteStop firstStop = source.remove(startIdx);
                    optimized.add(firstStop);
                    current = new GeoPoint(firstStop.latitude, firstStop.longitude);
                }

                while (!source.isEmpty()) {
                    StringBuilder coords = new StringBuilder();
                    coords.append(String.format(Locale.US, "%.6f,%.6f", current.getLongitude(), current.getLatitude()));
                    for (RouteStop s : source) {
                        coords.append(String.format(Locale.US, ";%.6f,%.6f", s.longitude, s.latitude));
                    }

                    String url = "https://router.project-osrm.org/table/v1/driving/" + coords.toString() + "?sources=0&annotations=distance";
                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

                    String uniqueId = Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID);
                    String userAgent = "DriveLogApp_v1527_" + uniqueId;
                    conn.setRequestProperty("User-Agent", userAgent);

                    if (conn.getResponseCode() == 200) {
                        BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder res = new StringBuilder(); String line;
                        while ((line = r.readLine()) != null) res.append(line);

                        JSONObject json = new JSONObject(res.toString());
                        JSONArray distances = json.getJSONArray("distances").getJSONArray(0);

                        int bestIdx = -1;
                        double minDist = Double.MAX_VALUE;
                        for (int i = 1; i < distances.length(); i++) {
                            if (!distances.isNull(i)) {
                                double d = distances.getDouble(i);
                                if (d < minDist) { minDist = d; bestIdx = i - 1; }
                            }
                        }

                        if (bestIdx != -1) {
                            RouteStop next = source.get(bestIdx);
                            optimized.add(next);
                            source.remove(bestIdx);
                            current = new GeoPoint(next.latitude, next.longitude);
                        } else {
                            RouteStop nearest = source.get(0);
                            optimized.add(nearest);
                            source.remove(0);
                        }
                    } else {
                        RouteStop nearest = null; double minDist = Double.MAX_VALUE;
                        for (RouteStop s : source) {
                            double d = current.distanceToAsDouble(new GeoPoint(s.latitude, s.longitude));
                            if (d < minDist) { minDist = d; nearest = s; }
                        }
                        if (nearest != null) {
                            optimized.add(nearest); source.remove(nearest);
                            current = new GeoPoint(nearest.latitude, nearest.longitude);
                        } else break;
                    }
                }

                previewList.clear();
                previewList.addAll(optimized);

                Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        dialog.dismiss();
                        if (onComplete != null) onComplete.run();
                        Toast.makeText(getContext(), "Otimização 2.0 concluída!", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        dialog.dismiss();
                        Toast.makeText(getContext(), "Erro ao otimizar", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
    }

    private void optimizePreviewListV3(List<RouteStop> previewList, boolean isGpsMode, int chosenStartIdx, Runnable onComplete) {
        if (getContext() == null || previewList == null || previewList.isEmpty()) return;

        View dv = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_optimization_progress, null);
        CircularProgressIndicator progress = dv.findViewById(R.id.progressOptimization);
        TextView textStatus = dv.findViewById(R.id.textOptimizationStatus);
        TextView textPercent = dv.findViewById(R.id.textOptimizationPercent);

        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(dv).setCancelable(false).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();

        new Thread(() -> {
            try {
                String[] phrases = {
                    "🧠 Otimização 3.0: Mapeando coordenadas da rota...",
                    "🧠 Consultando matriz OSRM de distâncias viárias...",
                    "🧠 Analisando e testando 200 combinações de rotas com TSP 2-Opt...",
                    "🧠 Comparando quilometragem e selecionando a menor rota...",
                    "🧠 Finalizando sequência com máxima economia de combustível!"
                };

                for (int i = 0; i < phrases.length; i++) {
                    final String msg = phrases[i];
                    final int p = (i + 1) * (100 / phrases.length);
                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.runOnUiThread(() -> {
                            if (textStatus != null) textStatus.setText(msg);
                            if (progress != null) progress.setProgress(p);
                            if (textPercent != null) textPercent.setText(p + "%");
                        });
                    }
                    Thread.sleep(600);
                }

                List<RouteStop> source = new ArrayList<>(previewList);
                List<RouteStop> stopNodes = new ArrayList<>();
                GeoPoint startPoint;

                if (isGpsMode) {
                    startPoint = currentLocation != null ? currentLocation : (source.get(0).latitude != 0 ? new GeoPoint(source.get(0).latitude, source.get(0).longitude) : new GeoPoint(-10.0, -55.0));
                    stopNodes.addAll(source);
                } else {
                    int sIdx = (chosenStartIdx >= 0 && chosenStartIdx < source.size()) ? chosenStartIdx : 0;
                    RouteStop firstStop = source.remove(sIdx);
                    startPoint = new GeoPoint(firstStop.latitude, firstStop.longitude);
                    stopNodes.add(firstStop);
                    stopNodes.addAll(source);
                }

                int numStops = stopNodes.size();
                int totalPoints = isGpsMode ? numStops + 1 : numStops;

                StringBuilder coords = new StringBuilder();
                if (isGpsMode) {
                    coords.append(String.format(Locale.US, "%.6f,%.6f", startPoint.getLongitude(), startPoint.getLatitude()));
                    for (RouteStop s : stopNodes) {
                        coords.append(String.format(Locale.US, ";%.6f,%.6f", s.longitude, s.latitude));
                    }
                } else {
                    for (int i = 0; i < stopNodes.size(); i++) {
                        RouteStop s = stopNodes.get(i);
                        if (i > 0) coords.append(";");
                        coords.append(String.format(Locale.US, "%.6f,%.6f", s.longitude, s.latitude));
                    }
                }

                String url = "https://router.project-osrm.org/table/v1/driving/" + coords.toString() + "?annotations=distance";
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                String uniqueId = Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID);
                conn.setRequestProperty("User-Agent", "DriveLogApp_v30_" + uniqueId);

                double[][] distMatrix = new double[totalPoints][totalPoints];
                boolean matrixSuccess = false;

                if (conn.getResponseCode() == 200) {
                    BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder res = new StringBuilder(); String line;
                    while ((line = r.readLine()) != null) res.append(line);

                    JSONObject json = new JSONObject(res.toString());
                    JSONArray distancesArray = json.getJSONArray("distances");

                    for (int i = 0; i < totalPoints; i++) {
                        JSONArray row = distancesArray.getJSONArray(i);
                        for (int j = 0; j < totalPoints; j++) {
                            if (!row.isNull(j)) {
                                distMatrix[i][j] = row.getDouble(j);
                            } else {
                                distMatrix[i][j] = Double.MAX_VALUE;
                            }
                        }
                    }
                    matrixSuccess = true;
                }

                if (!matrixSuccess) {
                    List<GeoPoint> points = new ArrayList<>();
                    if (isGpsMode) points.add(startPoint);
                    for (RouteStop s : stopNodes) points.add(new GeoPoint(s.latitude, s.longitude));

                    for (int i = 0; i < totalPoints; i++) {
                        for (int j = 0; j < totalPoints; j++) {
                            distMatrix[i][j] = points.get(i).distanceToAsDouble(points.get(j));
                        }
                    }
                }

                // --- MULTI-TOUR TSP EVALUATION: EVALUATE 200 CANDIDATE ROUTES & SELECT MIN DISTANCE ---
                int numCandidates = 200;
                int[][] candidateTours = new int[numCandidates][totalPoints];
                double[] candidateDistances = new double[numCandidates];

                Random rng = new Random(42);

                for (int cIdx = 0; cIdx < numCandidates; cIdx++) {
                    int[] tour = new int[totalPoints];
                    tour[0] = 0; // Ponto de partida fixo (GPS ou Parada #1)
                    boolean[] visited = new boolean[totalPoints];
                    visited[0] = true;

                    for (int i = 0; i < totalPoints - 1; i++) {
                        int curr = tour[i];
                        
                        if (cIdx == 0) {
                            int bestNext = -1;
                            double minDist = Double.MAX_VALUE;
                            for (int j = 1; j < totalPoints; j++) {
                                if (!visited[j] && distMatrix[curr][j] < minDist) {
                                    minDist = distMatrix[curr][j];
                                    bestNext = j;
                                }
                            }
                            if (bestNext != -1) {
                                visited[bestNext] = true;
                                tour[i + 1] = bestNext;
                            }
                        } else {
                            List<Integer> unvisitedList = new ArrayList<>();
                            for (int j = 1; j < totalPoints; j++) {
                                if (!visited[j]) unvisitedList.add(j);
                            }

                            if (!unvisitedList.isEmpty()) {
                                unvisitedList.sort((a, b) -> Double.compare(distMatrix[curr][a], distMatrix[curr][b]));
                                int kBound = Math.min(3, unvisitedList.size());
                                int pickIdx = (rng.nextDouble() < 0.65) ? 0 : rng.nextInt(kBound);
                                int selectedNext = unvisitedList.get(pickIdx);
                                visited[selectedNext] = true;
                                tour[i + 1] = selectedNext;
                            }
                        }
                    }

                    boolean improved = true;
                    int maxOptIterations = 300;
                    int iter = 0;

                    while (improved && iter < maxOptIterations) {
                        improved = false;
                        iter++;

                        for (int i = 1; i < totalPoints - 1; i++) {
                            for (int j = i + 1; j < totalPoints; j++) {
                                double currentDist = distMatrix[tour[i - 1]][tour[i]];
                                for (int k = i; k < j; k++) {
                                    currentDist += distMatrix[tour[k]][tour[k + 1]];
                                }
                                if (j < totalPoints - 1) {
                                    currentDist += distMatrix[tour[j]][tour[j + 1]];
                                }

                                double newDist = distMatrix[tour[i - 1]][tour[j]];
                                for (int k = j; k > i; k--) {
                                    newDist += distMatrix[tour[k]][tour[k - 1]];
                                }
                                if (j < totalPoints - 1) {
                                    newDist += distMatrix[tour[i]][tour[j + 1]];
                                }

                                if (newDist < currentDist - 0.01) {
                                    int left = i, right = j;
                                    while (left < right) {
                                        int temp = tour[left];
                                        tour[left] = tour[right];
                                        tour[right] = temp;
                                        left++;
                                        right--;
                                    }
                                    improved = true;
                                }
                            }
                        }
                    }

                    double tourDistMeters = 0.0;
                    for (int i = 0; i < totalPoints - 1; i++) {
                        double d = distMatrix[tour[i]][tour[i + 1]];
                        if (d < Double.MAX_VALUE) {
                            tourDistMeters += d;
                        }
                    }
                    candidateTours[cIdx] = tour;
                    candidateDistances[cIdx] = tourDistMeters;
                }

                int bestCandidateIdx = 0;
                double minTotalMeters = Double.MAX_VALUE;
                for (int cIdx = 0; cIdx < numCandidates; cIdx++) {
                    if (candidateDistances[cIdx] < minTotalMeters) {
                        minTotalMeters = candidateDistances[cIdx];
                        bestCandidateIdx = cIdx;
                    }
                }

                int[] winningTour = candidateTours[bestCandidateIdx];
                final double finalTotalKm = minTotalMeters / 1000.0;
                final int finalStopsCount = stopNodes.size();

                List<RouteStop> finalOptimized = new ArrayList<>();
                if (!isGpsMode && !stopNodes.isEmpty()) {
                    finalOptimized.add(stopNodes.get(0)); // PARADA #1 GARANTIDA NO ÍNDICE 0
                    for (int i = 1; i < totalPoints; i++) {
                        int stopIdx = winningTour[i];
                        if (stopIdx > 0 && stopIdx < stopNodes.size()) {
                            finalOptimized.add(stopNodes.get(stopIdx));
                        }
                    }
                } else {
                    for (int i = 1; i < totalPoints; i++) {
                        int stopIdx = winningTour[i] - 1;
                        if (stopIdx >= 0 && stopIdx < stopNodes.size()) {
                            finalOptimized.add(stopNodes.get(stopIdx));
                        }
                    }
                }

                previewList.clear();
                previewList.addAll(finalOptimized);

                Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        dialog.dismiss();
                        if (onComplete != null) onComplete.run();
                        showOptimization30SummaryPopup(finalTotalKm, finalStopsCount);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        dialog.dismiss();
                        Toast.makeText(getContext(), "Erro ao executar Otimização 3.0", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
    }

    private void showOptimization30SummaryPopup(double totalKm, int totalStops) {
        if (getContext() == null) return;

        Context ctx = requireContext();
        View v = LayoutInflater.from(ctx).inflate(R.layout.dialog_modern_confirm, null);
        TextView title = v.findViewById(R.id.textModernTitle);
        TextView message = v.findViewById(R.id.textModernMessage);
        MaterialButton btnCancel = v.findViewById(R.id.btnModernNegative);
        MaterialButton btnConfirm = v.findViewById(R.id.btnModernPositive);

        if (title != null) title.setText("🧠 Otimização 3.0 Concluída!");
        if (message != null) {
            String msg = String.format(Locale.getDefault(),
                    "📍 Distância Total da Rota: %.1f km\n" +
                    "📦 Total de Paradas: %d paradas\n\n" +
                    "⛽ 200 combinações de trajetos foram analisadas pelo algoritmo TSP 2-Opt. A rota com a MENOR distância viária em KM foi selecionada!",
                    totalKm, totalStops);
            message.setText(msg);
        }

        if (btnCancel != null) btnCancel.setVisibility(View.GONE);
        if (btnConfirm != null) {
            btnConfirm.setText("🚀 INICIAR ROTA");
            btnConfirm.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2E7D32")));
        }

        AlertDialog dialog = new AlertDialog.Builder(ctx).setView(v).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        if (btnConfirm != null) btnConfirm.setOnClickListener(v2 -> dialog.dismiss());

        dialog.show();
    }

    private void renumberPreviewList(List<RouteStop> list) {
        if (list == null) return;
        for (int i = 0; i < list.size(); i++) {
            list.get(i).sortOrder = i;
            list.get(i).stopNumber = i + 1;
        }
    }

    private void saveImportedStopsToDatabase(int targetRouteId, List<RouteStop> finalStops) {
        if (getContext() == null || finalStops == null || finalStops.isEmpty()) return;
        final Context ctx = getContext();
        new Thread(() -> {
            AppDao dao = AppDatabase.getInstance(ctx).appDao();
            renumberPreviewList(finalStops);

            // Evita duplicação: limpa as paradas antigas da rota e reseta os IDs
            dao.clearRouteStopsByRoute(targetRouteId);
            for (RouteStop s : finalStops) {
                s.id = 0;
            }
            dao.insertRouteStops(finalStops);

            List<RouteStop> all = dao.getStopsForRoute(targetRouteId);
            for (int i = 0; i < all.size(); i++) {
                all.get(i).sortOrder = i;
                all.get(i).stopNumber = i + 1;
            }
            dao.updateRouteStops(all);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    sharedPreferences.edit().putBoolean("show_bottom_sheet_stops", true).apply();
                    loadStopsForCurrentRoute();
                    Toast.makeText(ctx, "Rota salva com sucesso!", Toast.LENGTH_SHORT).show();
                    CloudSyncHelper.syncNow(ctx, "Importação Concluída");
                });
            }
        }).start();
    }

    private static class ImportPreviewAdapter extends RecyclerView.Adapter<ImportPreviewAdapter.ViewHolder> {
        private final List<RouteStop> stops;
        private final Set<Integer> selectedIndices;
        private final OnPreviewInteractionListener listener;

        interface OnPreviewInteractionListener {
            void onSelectionChanged();
            void onDelete(int position);
            void onStartDrag(RecyclerView.ViewHolder viewHolder);
        }

        ImportPreviewAdapter(List<RouteStop> stops, Set<Integer> selectedIndices, OnPreviewInteractionListener listener) {
            this.stops = stops;
            this.selectedIndices = selectedIndices;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_import_preview_stop, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder h, int position) {
            RouteStop item = stops.get(position);
            h.textTitle.setText("Parada #" + item.stopNumber);
            h.textAddress.setText(item.address != null ? item.address : "");
            
            String neighborhoodCity = "";
            if (item.neighborhood != null && !item.neighborhood.isEmpty()) neighborhoodCity += item.neighborhood;
            if (item.city != null && !item.city.isEmpty()) {
                if (!neighborhoodCity.isEmpty()) neighborhoodCity += " - ";
                neighborhoodCity += item.city;
            }
            h.textNeighborhood.setText(neighborhoodCity);

            String seqText = (item.allSequences != null && !item.allSequences.isEmpty()) ? item.allSequences : (item.sequence > 0 ? String.valueOf(item.sequence) : "");
            h.textPackagesSeq.setText("📦 " + item.packageCount + (item.packageCount > 1 ? " pacotes" : " pacote") + (seqText.isEmpty() ? "" : " | Seq: " + seqText));

            h.chkSelect.setOnCheckedChangeListener(null);
            h.chkSelect.setChecked(selectedIndices.contains(position));
            h.chkSelect.setOnCheckedChangeListener((btn, isChecked) -> {
                int adapterPos = h.getBindingAdapterPosition();
                if (adapterPos != RecyclerView.NO_POSITION) {
                    if (isChecked) selectedIndices.add(adapterPos);
                    else selectedIndices.remove(adapterPos);
                    if (listener != null) listener.onSelectionChanged();
                }
            });

            // Toque/manter pressionado no ícone de reordenar ativa o arraste
            h.imgDragHandle.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (listener != null) listener.onStartDrag(h);
                }
                return false;
            });

            h.btnDelete.setOnClickListener(v -> {
                int adapterPos = h.getBindingAdapterPosition();
                if (adapterPos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onDelete(adapterPos);
                }
            });
        }

        @Override
        public int getItemCount() {
            return stops != null ? stops.size() : 0;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            CheckBox chkSelect;
            TextView textTitle, textAddress, textNeighborhood, textPackagesSeq;
            ImageView imgDragHandle;
            ImageButton btnDelete;

            ViewHolder(View v) {
                super(v);
                chkSelect = v.findViewById(R.id.chkPreviewSelect);
                textTitle = v.findViewById(R.id.textPreviewStopTitle);
                textAddress = v.findViewById(R.id.textPreviewAddress);
                textNeighborhood = v.findViewById(R.id.textPreviewNeighborhood);
                textPackagesSeq = v.findViewById(R.id.textPreviewPackagesSeq);
                imgDragHandle = v.findViewById(R.id.imgPreviewDragHandle);
                btnDelete = v.findViewById(R.id.btnPreviewDelete);
            }
        }
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

        fabKmTracking.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));

        if (!tracking) {
            fabKmTracking.setImageTintList(ColorStateList.valueOf(Color.parseColor("#2196F3"))); // Azul (Inativo)
        } else {
            if (paused) {
                fabKmTracking.setImageTintList(ColorStateList.valueOf(Color.parseColor("#FFC107"))); // Amarelo (Pausado)
            } else {
                fabKmTracking.setImageTintList(ColorStateList.valueOf(Color.parseColor("#F44336"))); // Vermelho (Ativo)
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
        MaterialSwitch switchAuto = customView.findViewById(R.id.switchAutoTracking);
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

        Calendar manualCalendar = Calendar.getInstance();
        SimpleDateFormat manualSdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        if (editManualDate != null) {
            editManualDate.setText(manualSdf.format(manualCalendar.getTime()));
            editManualDate.setOnClickListener(v -> {
                new DatePickerDialog(requireContext(), (view, year, month, day) -> {
                    manualCalendar.set(year, month, day);
                    editManualDate.setText(manualSdf.format(manualCalendar.getTime()));
                }, manualCalendar.get(Calendar.YEAR), manualCalendar.get(Calendar.MONTH), manualCalendar.get(Calendar.DAY_OF_MONTH)).show();
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

        TrackingService.distanceToHome.observe(getViewLifecycleOwner(), distHome -> {
            boolean isTrk = Boolean.TRUE.equals(TrackingService.isTracking.getValue());
            int mode = sharedPreferences.getInt("tracking_mode_v2", 0);
            if (!isTrk && mode == 2 && textStatus != null) {
                float homeLat = sharedPreferences.getFloat("home_lat", 0);
                float homeLon = sharedPreferences.getFloat("home_lon", 0);
                int triggerRadius = sharedPreferences.getInt("home_trigger_radius", 100);
                if (homeLat == 0 || homeLon == 0) {
                    textStatus.setText("Defina sua Casa no Mapa");
                } else if (distHome != null && distHome >= 0) {
                    if (distHome <= triggerRadius) {
                        textStatus.setText(String.format(Locale.getDefault(), "Dentro do raio da casa (%.0fm / %dm)", distHome, triggerRadius));
                    } else {
                        textStatus.setText(String.format(Locale.getDefault(), "Fora do raio da casa (%.0fm / %dm) - Iniciando...", distHome, triggerRadius));
                        // 🔥 Disparo Imediato: Se identificou fora do raio, forca o inicio imediato!
                        if (!TrackingService.isTrackingActive && !Boolean.TRUE.equals(TrackingService.isTracking.getValue())) {
                            Intent intent = new Intent(getContext(), TrackingService.class);
                            intent.setAction("START");
                            startTrackingService(intent);
                        }
                    }
                }
            }
        });

        // 🔥 Observação Dinâmica de Estado no Popup
        TrackingService.isTracking.observe(getViewLifecycleOwner(), isTrk -> {
            boolean isPsd = Boolean.TRUE.equals(TrackingService.isPaused.getValue());
            int mode = sharedPreferences.getInt("tracking_mode_v2", 0);

            if (isTrk) {
                if (textStatus != null) textStatus.setText(isPsd ? "Pausado" : "Rastreando...");
                if (btnPlayPause != null) {
                    btnPlayPause.setVisibility(View.VISIBLE);
                    btnPlayPause.setText(isPsd ? "Retomar" : "Pausar");
                    btnPlayPause.setIconResource(isPsd ? R.drawable.ic_play : R.drawable.ic_pause);
                }
                if (btnStop != null) btnStop.setVisibility(View.VISIBLE);
            } else {
                if (btnStop != null) btnStop.setVisibility(View.GONE);
                if (btnPlayPause != null) {
                    btnPlayPause.setVisibility(mode == 0 ? View.VISIBLE : View.GONE);
                    btnPlayPause.setText("Iniciar");
                    btnPlayPause.setIconResource(R.drawable.ic_play);
                }
            }
        });

        TrackingService.isPaused.observe(getViewLifecycleOwner(), isPsd -> {
            boolean isTrk = Boolean.TRUE.equals(TrackingService.isTracking.getValue());
            if (isTrk) {
                if (textStatus != null) textStatus.setText(isPsd ? "Pausado" : "Rastreando...");
                if (btnPlayPause != null) {
                    btnPlayPause.setText(isPsd ? "Retomar" : "Pausar");
                    btnPlayPause.setIconResource(isPsd ? R.drawable.ic_play : R.drawable.ic_pause);
                }
            }
        });

        // Estado inicial ao abrir o diálogo
        boolean isCurrentlyTracking = Boolean.TRUE.equals(TrackingService.isTracking.getValue());
        boolean isCurrentlyPaused = Boolean.TRUE.equals(TrackingService.isPaused.getValue());

        if (!isCurrentlyTracking) {
            if (currentMode == 2) {
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
                        Location.distanceBetween(myLoc.getLatitude(), myLoc.getLongitude(), homeLat, homeLon, res);
                        currentDist = res[0];
                    }

                    if (currentDist >= 0) {
                        if (currentDist <= triggerRadius) {
                            textStatus.setText(String.format(Locale.getDefault(), "Dentro do raio da casa (%.0fm / %dm)", currentDist, triggerRadius));
                        } else {
                            textStatus.setText(String.format(Locale.getDefault(), "Fora do raio da casa (%.0fm / %dm) - Iniciando...", currentDist, triggerRadius));
                            if (!TrackingService.isTrackingActive && !Boolean.TRUE.equals(TrackingService.isTracking.getValue())) {
                                Intent intent = new Intent(getContext(), TrackingService.class);
                                intent.setAction("START");
                                startTrackingService(intent);
                            }
                        }
                    } else {
                        textStatus.setText("Buscando sinal GPS...");
                    }
                }
            } else if (currentMode == 1) {
                textStatus.setText("Aguardando Horário");
            } else {
                textStatus.setText("Rastreamento Inativo");
            }
            btnPlayPause.setText("Iniciar");
            btnPlayPause.setIconResource(R.drawable.ic_play);
            btnStop.setVisibility(View.GONE);
            btnPlayPause.setVisibility(currentMode == 0 ? View.VISIBLE : View.GONE);
        } else {
            textStatus.setText(isCurrentlyPaused ? "Pausado" : "Rastreando...");
            btnPlayPause.setText(isCurrentlyPaused ? "Retomar" : "Pausar");
            btnPlayPause.setIconResource(isCurrentlyPaused ? R.drawable.ic_play : R.drawable.ic_pause);
            btnPlayPause.setVisibility(View.VISIBLE);
            btnStop.setVisibility(View.VISIBLE);
        }

        btnPlayPause.setOnClickListener(v -> {
            boolean activeTracking = Boolean.TRUE.equals(TrackingService.isTracking.getValue());
            boolean activePaused = Boolean.TRUE.equals(TrackingService.isPaused.getValue());
            Intent intent = new Intent(getContext(), TrackingService.class);
            if (!activeTracking) {
                intent.setAction("START");
            } else if (!activePaused) {
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
                        // 🔥 Se o modo automático estava ativo, desativa o modo automático ao forçar o botão Parar
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
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        dialog.show();
    }

    private void startTrackingService(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
                final int targetRouteId = (stop != null && stop.routeId > 0) ? stop.routeId : (currentRouteHeader != null && currentRouteHeader.id > 0 ? currentRouteHeader.id : currentRouteId);
                new Thread(() -> {
                    AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
                    RouteHeader header = dao.getRouteById(targetRouteId);
                    if (header != null) {
                        boolean updated = false;
                        if (header.startTime == 0) {
                            long startTimeToUse = (currentRouteHeader != null && currentRouteHeader.startTime > 0)
                                ? currentRouteHeader.startTime
                                : System.currentTimeMillis();
                            header.startTime = startTimeToUse;
                            header.totalPausedMs = 0;
                            header.lastPauseStartTime = 0;
                            updated = true;
                            if (currentRouteHeader != null) {
                                currentRouteHeader.startTime = startTimeToUse;
                                currentRouteHeader.totalPausedMs = 0;
                                currentRouteHeader.lastPauseStartTime = 0;
                            }
                        }
                        
                        // Verifica se é a última parada
                        List<RouteStop> all = dao.getStopsForRoute(targetRouteId);
                        boolean allDone = (all != null && !all.isEmpty());
                        if (all != null) {
                            for (RouteStop rs : all) {
                                if (rs.id == stop.id) continue; // A atual já marcamos acima no DB via updateStopInDb mas o thread pode ser rápido
                                if (rs.deliveryStatus == 0) { allDone = false; break; }
                            }
                        }
                        
                        if (allDone) {
                            if (header.endTime == 0) {
                                header.endTime = System.currentTimeMillis();
                                header.isCompleted = true;
                                updated = true;
                                if (currentRouteHeader != null) {
                                    currentRouteHeader.endTime = header.endTime;
                                    currentRouteHeader.isCompleted = true;
                                }
                            }
                        } else {
                            if (header.endTime > 0) {
                                header.endTime = 0;
                                header.isCompleted = false;
                                updated = true;
                                if (currentRouteHeader != null) {
                                    currentRouteHeader.endTime = 0;
                                    currentRouteHeader.isCompleted = false;
                                }
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
                final int targetRouteId = (stop != null && stop.routeId > 0) ? stop.routeId : (currentRouteHeader != null && currentRouteHeader.id > 0 ? currentRouteHeader.id : currentRouteId);
                new Thread(() -> {
                    AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
                    RouteHeader header = dao.getRouteById(targetRouteId);
                    if (header != null) {
                        boolean updated = false;
                        if (header.startTime == 0) {
                            long startTimeToUse = (currentRouteHeader != null && currentRouteHeader.startTime > 0)
                                ? currentRouteHeader.startTime
                                : System.currentTimeMillis();
                            header.startTime = startTimeToUse;
                            header.totalPausedMs = 0;
                            header.lastPauseStartTime = 0;
                            updated = true;
                            if (currentRouteHeader != null) {
                                currentRouteHeader.startTime = startTimeToUse;
                                currentRouteHeader.totalPausedMs = 0;
                                currentRouteHeader.lastPauseStartTime = 0;
                            }
                        }

                        List<RouteStop> all = dao.getStopsForRoute(targetRouteId);
                        boolean allDone = (all != null && !all.isEmpty());
                        if (all != null) {
                            for (RouteStop rs : all) {
                                if (rs.id == stop.id) continue;
                                if (rs.deliveryStatus == 0) { allDone = false; break; }
                            }
                        }
                        
                        if (allDone) {
                            if (header.endTime == 0) {
                                header.endTime = System.currentTimeMillis();
                                header.isCompleted = true;
                                updated = true;
                                if (currentRouteHeader != null) {
                                    currentRouteHeader.endTime = header.endTime;
                                    currentRouteHeader.isCompleted = true;
                                }
                            }
                        } else {
                            if (header.endTime > 0) {
                                header.endTime = 0;
                                header.isCompleted = false;
                                updated = true;
                                if (currentRouteHeader != null) {
                                    currentRouteHeader.endTime = 0;
                                    currentRouteHeader.isCompleted = false;
                                }
                            }
                        }

                        if (updated) dao.updateRouteHeader(header);
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
            
            // Se a rota estava finalizada, reabre a rota mantendo o startTime intacto
            if (currentRouteHeader != null) {
                final int targetRouteId = (stop != null && stop.routeId > 0) ? stop.routeId : (currentRouteHeader != null && currentRouteHeader.id > 0 ? currentRouteHeader.id : currentRouteId);
                new Thread(() -> {
                    AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
                    RouteHeader header = dao.getRouteById(targetRouteId);
                    if (header != null) {
                        boolean updated = false;
                        if (header.endTime > 0) {
                            header.endTime = 0;
                            header.isCompleted = false;
                            updated = true;
                            if (currentRouteHeader != null) {
                                currentRouteHeader.endTime = 0;
                                currentRouteHeader.isCompleted = false;
                            }
                        }
                        if (updated) dao.updateRouteHeader(header);
                    }
                }).start();
            }

            String celebrationKey = "last_finished_route_" + currentRouteId;
            if (sharedPreferences.contains(celebrationKey)) {
                sharedPreferences.edit().remove(celebrationKey).apply();
                Log.d("DriveLog", "Resetando flag de celebração via onStopAction (Reset)");
            }

            advanceToNextStop();
        }
        else if (action == 7) showStopDetails(stop);
        else if (action == 8) showGlobalFeedbackDialog(stop.address);

        // Atualiza os adaptadores da lista e os marcadores do mapa
        if (action == 1 || action == 2 || action == 6) {
            Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(() -> {
                    if (stopsCardAdapter != null) stopsCardAdapter.notifyDataSetChanged();
                    if (stopsListAdapter != null) stopsListAdapter.notifyDataSetChanged();
                    refreshMarkers();
                });
            }
        }
    }

    private void triggerCelebration() {
        triggerCelebration(false);
    }

    private void triggerCelebration(boolean force) {
        if (konfettiView == null) return;
        
        // Evita disparar repetidamente se já estiver comemorando
        String lastFinishedRouteKey = "last_finished_route_" + currentRouteId;
        if (!force && sharedPreferences.getBoolean(lastFinishedRouteKey, false)) {
            Log.d("DriveLog", "Celebração ignorada: já comemorou esta rota: " + currentRouteId);
            return;
        }
        
        // Marca IMEDIATAMENTE como celebrado para evitar disparos múltiplos por mudanças rápidas no DB
        sharedPreferences.edit().putBoolean(lastFinishedRouteKey, true).apply();
        
        Log.d("DriveLog", "Disparando Celebração Explosiva!");
        konfettiView.setVisibility(View.VISIBLE);
        konfettiView.bringToFront();

        EmitterConfig emitterConfig = new Emitter(5, TimeUnit.SECONDS).perSecond(30);
        Party party = new PartyFactory(emitterConfig)
                .angle(270)
                .spread(90)
                .setSpeedBetween(1f, 5f)
                .position(new Position.Relative(0.5, 1.0)) // Do fundo ao centro
                .sizes(new Size(12, 5f, 0.2f))
                .colors(Arrays.asList(0xffffd700, 0xff32cd32, 0xff1e90ff, 0xffff4500, 0xffba55d3))
                .shapes(Shape.Square.INSTANCE, Shape.Circle.INSTANCE)
                .timeToLive(3000L)
                .build();
        
        konfettiView.start(party);
        
        // Adiciona uma segunda explosão lateral para garantir visibilidade
        konfettiView.start(new PartyFactory(new Emitter(2, TimeUnit.SECONDS).perSecond(20))
                .angle(0) // Direita
                .spread(60)
                .position(new Position.Relative(0.0, 0.5))
                .build());
        
        konfettiView.start(new PartyFactory(new Emitter(2, TimeUnit.SECONDS).perSecond(20))
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

    private int findStopIndexInList(List<RouteStop> list, RouteStop target) {
        if (list == null || target == null) return -1;
        for (int i = 0; i < list.size(); i++) {
            RouteStop s = list.get(i);
            if (s != null) {
                if (target.id > 0 && s.id > 0 && target.id == s.id) {
                    return i;
                }
                if (target.stopNumber > 0 && s.stopNumber == target.stopNumber) {
                    return i;
                }
                if (target.address != null && s.address != null && target.address.equalsIgnoreCase(s.address)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void advanceToNextStop() {
        if (viewPagerStops == null || currentStops == null || currentStops.isEmpty()) return;

        boolean autoNearest = sharedPreferences.getBoolean("advance_to_nearest", false);
        int currentIdx = viewPagerStops.getCurrentItem();

        if (autoNearest) {
            new Thread(() -> {
                try {
                    List<RouteStop> pending = new ArrayList<>();
                    for (RouteStop s : currentStops) {
                        if (s != null && s.deliveryStatus == 0) {
                            pending.add(s);
                        }
                    }

                    if (pending.isEmpty()) return;

                    // 1. Determina o ponto de referência (GPS atual se válido, ou localização da parada ativa)
                    GeoPoint refPoint = null;
                    boolean isGpsValid = (currentLocation != null 
                            && Math.abs(currentLocation.getLatitude()) > 0.001 
                            && Math.abs(currentLocation.getLongitude()) > 0.001
                            && !(Math.abs(currentLocation.getLatitude() - (-23.5505)) < 0.01 && Math.abs(currentLocation.getLongitude() - (-46.6333)) < 0.01));

                    if (isGpsValid) {
                        refPoint = currentLocation;
                    } else if (currentIdx >= 0 && currentIdx < currentStops.size()) {
                        RouteStop activeStop = currentStops.get(currentIdx);
                        if (activeStop != null && Math.abs(activeStop.latitude) > 0.001) {
                            refPoint = new GeoPoint(activeStop.latitude, activeStop.longitude);
                        }
                    }

                    // Se não tiver ponto de referência ou se só houver 1 parada pendente
                    if (refPoint == null || pending.size() == 1) {
                        int finalIdx = findStopIndexInList(currentStops, pending.get(0));
                        if (finalIdx >= 0 && getActivity() != null) {
                            getActivity().runOnUiThread(() -> viewPagerStops.setCurrentItem(finalIdx, true));
                        }
                        return;
                    }

                    // Filtra apenas paradas pendentes com coordenadas válidas
                    List<RouteStop> validPending = new ArrayList<>();
                    for (RouteStop s : pending) {
                        if (Math.abs(s.latitude) > 0.001 && Math.abs(s.longitude) > 0.001) {
                            validPending.add(s);
                        }
                    }

                    if (validPending.isEmpty()) {
                        int finalIdx = findStopIndexInList(currentStops, pending.get(0));
                        if (finalIdx >= 0 && getActivity() != null) {
                            getActivity().runOnUiThread(() -> viewPagerStops.setCurrentItem(finalIdx, true));
                        }
                        return;
                    }

                    // Tenta calcular a distância via OSRM (RUA)
                    final GeoPoint finalRefPoint = refPoint;
                    RouteStop bestOsrmStop = null;
                    try {
                        List<RouteStop> targetStops = validPending.size() > 50 ? validPending.subList(0, 50) : validPending;
                        StringBuilder coords = new StringBuilder();
                        coords.append(String.format(Locale.US, "%.6f,%.6f", finalRefPoint.getLongitude(), finalRefPoint.getLatitude()));
                        for (RouteStop s : targetStops) {
                            coords.append(String.format(Locale.US, ";%.6f,%.6f", s.longitude, s.latitude));
                        }

                        String url = "https://router.project-osrm.org/table/v1/driving/" + coords.toString() + "?sources=0&annotations=distance";
                        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                        conn.setConnectTimeout(2000);
                        conn.setReadTimeout(2000);
                        String uniqueId = Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID);
                        conn.setRequestProperty("User-Agent", "DriveLogApp_v1527_" + uniqueId);

                        if (conn.getResponseCode() == 200) {
                            BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                            StringBuilder res = new StringBuilder(); String line;
                            while ((line = r.readLine()) != null) res.append(line);
                            JSONObject json = new JSONObject(res.toString());
                            JSONArray distances = json.getJSONArray("distances").getJSONArray(0);

                            int bestIdxInTargets = -1;
                            double minDist = Double.MAX_VALUE;
                            for (int i = 1; i < distances.length(); i++) {
                                if (!distances.isNull(i)) {
                                    double d = distances.getDouble(i);
                                    if (d < minDist) {
                                        minDist = d;
                                        bestIdxInTargets = i - 1;
                                    }
                                }
                            }

                            if (bestIdxInTargets >= 0 && bestIdxInTargets < targetStops.size()) {
                                bestOsrmStop = targetStops.get(bestIdxInTargets);
                            }
                        }
                    } catch (Exception ignored) {}

                    if (bestOsrmStop != null) {
                        int finalIdx = findStopIndexInList(currentStops, bestOsrmStop);
                        if (finalIdx >= 0 && getActivity() != null) {
                            getActivity().runOnUiThread(() -> viewPagerStops.setCurrentItem(finalIdx, true));
                            return;
                        }
                    }

                    // Fallback para distância direta (linear)
                    RouteStop bestLinearStop = null;
                    double minDist = Double.MAX_VALUE;
                    for (RouteStop s : validPending) {
                        double d = finalRefPoint.distanceToAsDouble(new GeoPoint(s.latitude, s.longitude));
                        if (d < minDist) {
                            minDist = d;
                            bestLinearStop = s;
                        }
                    }

                    if (bestLinearStop != null) {
                        int finalIdx = findStopIndexInList(currentStops, bestLinearStop);
                        if (finalIdx >= 0 && getActivity() != null) {
                            getActivity().runOnUiThread(() -> viewPagerStops.setCurrentItem(finalIdx, true));
                            return;
                        }
                    }

                    // Se tudo falhar, vai para a primeira pendente
                    int finalIdx = findStopIndexInList(currentStops, pending.get(0));
                    if (finalIdx >= 0 && getActivity() != null) {
                        getActivity().runOnUiThread(() -> viewPagerStops.setCurrentItem(finalIdx, true));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
            return;
        }

        // Comportamento Padrão: Próxima na ordem numérica
        for (int i = currentIdx + 1; i < currentStops.size(); i++) {
            if (currentStops.get(i) != null && currentStops.get(i).deliveryStatus == 0) {
                viewPagerStops.setCurrentItem(i, true);
                return;
            }
        }

        // Se não houver próxima após a atual, tenta desde o começo (ex: pulou paradas)
        for (int i = 0; i < currentIdx; i++) {
            if (currentStops.get(i) != null && currentStops.get(i).deliveryStatus == 0) {
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
        Spinner spinnerTarget = v.findViewById(R.id.spinnerNoteTarget);
        MaterialSwitch switchPublic = v.findViewById(R.id.switchNotePublic);

        textAddress.setText(stop.address);
        textNeighborhood.setText(stop.neighborhood != null && !stop.neighborhood.isEmpty() ? stop.neighborhood : "Bairro não informado");
        textPackageCount.setText(stop.packageCount + (stop.packageCount > 1 ? " Pacotes" : " Pacote"));
        textBuyerCount.setText(stop.buyerCount + (stop.buyerCount > 1 ? " Compradores" : " Comprador"));
        textStopNumber.setText("Parada #" + stop.stopNumber);
        textSequences.setText("Sequências: " + (stop.allSequences != null ? stop.allSequences : stop.sequence));

        // --- Status Inicial de Correção ---
        textCorrectionStatus.setVisibility(View.GONE);
        View cardGlobal = v.findViewById(R.id.cardGlobalDetails);
        MaterialButton btnDownload = v.findViewById(R.id.btnDownloadGlobalFix);

        // --- Carrega o Card de Avaliação / Correção da Comunidade com Botões Diretos ---
        loadGlobalCardForStopDetails(v, stop);

        if (stop.allAddresses != null && !stop.allAddresses.isEmpty()) {
            textBuyerList.setText(stop.allAddresses);
            textBuyerList.setVisibility(View.VISIBLE);
        } else {
            textBuyerList.setVisibility(View.GONE);
        }

        new Thread(() -> {
            AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
            CorrectedAddress corrected = dao.getCorrectedAddress(stop.address);
            
            Activity act = getActivity();
            if (act != null) act.runOnUiThread(() -> {
                if (corrected != null && corrected.latitude != 0.0 && corrected.longitude != 0.0 && Math.abs(corrected.latitude) > 0.001) {
                    boolean localApplied = Math.abs(stop.latitude - corrected.latitude) < 0.00001 
                                        && Math.abs(stop.longitude - corrected.longitude) < 0.00001;
                    if (!localApplied) {
                        cardGlobal.setVisibility(View.VISIBLE);
                        btnDownload.setVisibility(View.VISIBLE);
                        btnDownload.setEnabled(true);
                        btnDownload.setText("📥 APLICAR CORREÇÃO DESTE ENDEREÇO");
                        textCorrectionStatus.setVisibility(View.VISIBLE);
                        textCorrectionStatus.setText("Correção disponível");
                        textCorrectionStatus.setTextColor(Color.parseColor("#F44336"));

                        btnDownload.setOnClickListener(v3 -> {
                            if (corrected.latitude == 0.0 || corrected.longitude == 0.0 || Math.abs(corrected.latitude) < 0.001) {
                                Toast.makeText(getContext(), "Coordenadas locais inválidas.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            btnDownload.setEnabled(false);
                            btnDownload.setText("APLICANDO...");
                            new Thread(() -> {
                                stop.latitude = corrected.latitude;
                                stop.longitude = corrected.longitude;
                                dao.updateRouteStop(stop);

                                Activity activity3 = getActivity();
                                if (activity3 != null) activity3.runOnUiThread(() -> {
                                    Toast.makeText(getContext(), "Correção aplicada nesta parada!", Toast.LENGTH_SHORT).show();
                                    btnDownload.setVisibility(View.GONE);
                                    textCorrectionStatus.setText("Correção aplicada");
                                    textCorrectionStatus.setTextColor(Color.parseColor("#4CAF50"));
                                    refreshMarkers();
                                    updateRouteCorrectionsCount();
                                });
                            }).start();
                        });
                    }

                    if (corrected.notes != null && !corrected.notes.isEmpty()) {
                        editNotes.setText(corrected.notes);
                        switchPublic.setChecked(corrected.isNotePublic);
                        layoutNote.setVisibility(View.VISIBLE);
                        ((MaterialButton) btnShowAddNote).setText("EDITAR OBSERVAÇÃO");
                    }

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
                                        if (msg != null && msg.startsWith("ALREADY_CORRECTED_BY_OTHER")) {
                                            String[] parts = msg.split(":");
                                            String creatorName = (parts.length > 1 && !parts[1].isEmpty()) ? parts[1] : "outro entregador";
                                            promptSubstitutionRequestDialog(corrected, creatorName);
                                        } else {
                                            Toast.makeText(getContext(), "Erro ao enviar: " + msg, Toast.LENGTH_SHORT).show();
                                        }
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
                            .setMessage("Deseja apagar esta correção e voltar para a localização original da planilha?")
                            .setPositiveButton("Sim, Remover", (dialogInterface, i) -> {
                                new Thread(() -> {
                                    dao.deleteCorrectedAddress(corrected);
                                    
                                    // 🔥 RESTAURAÇÃO COMPLETA: Volta sempre para a coordenada original da planilha
                                    if (stop.originalLatitude != 0 && stop.originalLongitude != 0) {
                                        stop.latitude = stop.originalLatitude;
                                        stop.longitude = stop.originalLongitude;
                                    }
                                    dao.updateRouteStop(stop);

                                    Activity activity2 = getActivity();
                                    if (activity2 != null) activity2.runOnUiThread(() -> {
                                        dialog.dismiss();
                                        Toast.makeText(getContext(), "Correção removida! Localização original da planilha restaurada.", Toast.LENGTH_SHORT).show();
                                        refreshMarkers();
                                        updateRouteCorrectionsCount();
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
                            .setMessage("Deseja apagar esta correção baixada da comunidade e voltar para a localização original da planilha?")
                            .setPositiveButton("Sim, Excluir", (dialogInterface, i) -> {
                                new Thread(() -> {
                                    dao.deleteCorrectedAddress(corrected);
                                    
                                    if (stop.originalLatitude != 0 && stop.originalLongitude != 0) {
                                        stop.latitude = stop.originalLatitude;
                                        stop.longitude = stop.originalLongitude;
                                    }
                                    dao.updateRouteStop(stop);

                                    Activity activity2 = getActivity();
                                    if (activity2 != null) activity2.runOnUiThread(() -> {
                                        dialog.dismiss();
                                        Toast.makeText(getContext(), "Correção baixada excluída! Localização original restaurada.", Toast.LENGTH_SHORT).show();
                                        refreshMarkers();
                                        updateRouteCorrectionsCount();
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
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
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
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, recipients);
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
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
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

    private void loadGlobalCardForStopDetails(View dialogView, RouteStop stop) {
        if (dialogView == null || stop == null) return;
        View cardGlobal = dialogView.findViewById(R.id.cardGlobalDetails);
        TextView textGlobalLikes = dialogView.findViewById(R.id.textGlobalLikes);
        TextView textGlobalNoteDetails = dialogView.findViewById(R.id.textGlobalNoteDetails);
        MaterialButton btnDownload = dialogView.findViewById(R.id.btnDownloadGlobalFix);

        FirebaseHelper.searchGlobal(stop.address, new FirebaseHelper.GlobalCorrectionCallback() {
            @Override
            public void onResult(double lat, double lon, int likes, int dislikes, String creatorId, String note, int comments, String creatorName, long date, boolean hasCoordinateFix) {
                Activity activity = getActivity();
                if (activity != null) activity.runOnUiThread(() -> {
                    boolean isRealFix = hasCoordinateFix && lat != 0.0 && lon != 0.0 && Math.abs(lat) > 0.001;
                    boolean alreadyApplied = isRealFix && Math.abs(stop.latitude - lat) < 0.00001 && Math.abs(stop.longitude - lon) < 0.00001;

                    TextView textGlobalTitle = dialogView.findViewById(R.id.textGlobalTitle);
                    if (textGlobalTitle != null) {
                        textGlobalTitle.setText(isRealFix ? "Correção da Comunidade" : "Avaliação do Endereço");
                    }

                    if (cardGlobal != null) cardGlobal.setVisibility(View.VISIBLE);

                    final int[] likesCount = {likes};
                    final int[] dislikesCount = {dislikes};
                    final Boolean[] userVote = {null};
                    final boolean[] hasUserInteracted = {false};

                    int defaultBgColor = Color.parseColor("#E0E0E0");
                    int activeLikeColor = Color.parseColor("#4CAF50");
                    int activeDislikeColor = Color.parseColor("#F44336");

                    MaterialButton btnDirectLike = dialogView.findViewById(R.id.btnDirectLike);
                    MaterialButton btnDirectDislike = dialogView.findViewById(R.id.btnDirectDislike);
                    MaterialButton btnDirectComments = dialogView.findViewById(R.id.btnDirectComments);

                    TextView textReliabilityLabel = dialogView.findViewById(R.id.textGlobalReliabilityLabel);
                    View viewReliabilityBar = dialogView.findViewById(R.id.viewGlobalReliabilityBar);

                    Runnable updateReliabilityUI = () -> {
                        int totalVotes = likesCount[0] + dislikesCount[0];
                        int mainColor;
                        String reliabilityText;

                        if (totalVotes == 0) {
                            mainColor = Color.parseColor("#F57C00");
                            reliabilityText = "⏳ Em avaliação (Sem votos ainda)";
                        } else {
                            double ratio = (double) likesCount[0] / totalVotes;
                            int percent = (int) (ratio * 100);
                            if (ratio >= 0.75) {
                                mainColor = Color.parseColor("#388E3C");
                                reliabilityText = "🛡️ Alta Confiabilidade (" + percent + "%)";
                            } else if (ratio >= 0.40) {
                                mainColor = Color.parseColor("#E65100");
                                reliabilityText = "⚠️ Média Confiabilidade (" + percent + "%)";
                            } else {
                                mainColor = Color.parseColor("#D32F2F");
                                reliabilityText = "❌ Baixa Confiabilidade (" + percent + "%)";
                            }
                        }

                        if (textReliabilityLabel != null) {
                            textReliabilityLabel.setText(reliabilityText);
                            textReliabilityLabel.setTextColor(mainColor);
                        }
                        if (viewReliabilityBar != null) {
                            viewReliabilityBar.setBackgroundColor(mainColor);
                        }
                    };

                    Runnable updateButtonsUI = () -> {
                        if (btnDirectLike != null) {
                            btnDirectLike.setText("👍 " + likesCount[0]);
                            if (Boolean.TRUE.equals(userVote[0])) {
                                btnDirectLike.setBackgroundTintList(ColorStateList.valueOf(activeLikeColor));
                                btnDirectLike.setTextColor(Color.WHITE);
                            } else {
                                btnDirectLike.setBackgroundTintList(ColorStateList.valueOf(defaultBgColor));
                                btnDirectLike.setTextColor(Color.parseColor("#388E3C"));
                            }
                        }
                        if (btnDirectDislike != null) {
                            btnDirectDislike.setText("👎 " + dislikesCount[0]);
                            if (Boolean.FALSE.equals(userVote[0])) {
                                btnDirectDislike.setBackgroundTintList(ColorStateList.valueOf(activeDislikeColor));
                                btnDirectDislike.setTextColor(Color.WHITE);
                            } else {
                                btnDirectDislike.setBackgroundTintList(ColorStateList.valueOf(defaultBgColor));
                                btnDirectDislike.setTextColor(Color.parseColor("#D32F2F"));
                            }
                        }
                        if (textGlobalLikes != null) {
                            textGlobalLikes.setText(likesCount[0] + " 👍 | " + dislikesCount[0] + " 👎");
                        }
                        updateReliabilityUI.run();
                    };

                    updateButtonsUI.run();

                    if (textGlobalLikes != null) {
                        textGlobalLikes.setOnClickListener(v3 -> showGlobalFeedbackDialog(stop.address, () -> {
                            loadGlobalCardForStopDetails(dialogView, stop);
                        }));
                    }

                    String currentUserId = sharedPreferences.getString("current_user_id", "anon");
                    String userName = sharedPreferences.getString("profile_name", "Entregador");

                    // Busca o voto prévio do usuário para este endereço
                    FirebaseHelper.getUserAddressVote(stop.address, currentUserId, isLike -> {
                        Activity act = getActivity();
                        if (act != null) {
                            act.runOnUiThread(() -> {
                                if (!hasUserInteracted[0]) {
                                    userVote[0] = isLike;
                                    updateButtonsUI.run();
                                }
                            });
                        }
                    });

                    if (btnDirectLike != null) {
                        btnDirectLike.setOnClickListener(vClick -> {
                            hasUserInteracted[0] = true;
                            if (Boolean.TRUE.equals(userVote[0])) {
                                userVote[0] = null;
                                likesCount[0] = Math.max(0, likesCount[0] - 1);
                            } else if (Boolean.FALSE.equals(userVote[0])) {
                                userVote[0] = true;
                                dislikesCount[0] = Math.max(0, dislikesCount[0] - 1);
                                likesCount[0]++;
                            } else {
                                userVote[0] = true;
                                likesCount[0]++;
                            }
                            updateButtonsUI.run();

                            FirebaseHelper.addFeedback(stop.address, true, null, userName, currentUserId, () -> {
                                Activity act = getActivity();
                                if (act != null) {
                                    act.runOnUiThread(() -> {
                                        if (stopsCardAdapter != null) stopsCardAdapter.notifyDataSetChanged();
                                        if (stopsListAdapter != null) stopsListAdapter.notifyDataSetChanged();
                                    });
                                }
                            });
                            Toast.makeText(getContext(), "Valeu!", Toast.LENGTH_SHORT).show();
                        });
                    }

                    if (btnDirectDislike != null) {
                        btnDirectDislike.setOnClickListener(vClick -> {
                            hasUserInteracted[0] = true;
                            if (Boolean.FALSE.equals(userVote[0])) {
                                userVote[0] = null;
                                dislikesCount[0] = Math.max(0, dislikesCount[0] - 1);
                            } else if (Boolean.TRUE.equals(userVote[0])) {
                                userVote[0] = false;
                                likesCount[0] = Math.max(0, likesCount[0] - 1);
                                dislikesCount[0]++;
                            } else {
                                userVote[0] = false;
                                dislikesCount[0]++;
                            }
                            updateButtonsUI.run();

                            FirebaseHelper.addFeedback(stop.address, false, null, userName, currentUserId, () -> {
                                Activity act = getActivity();
                                if (act != null) {
                                    act.runOnUiThread(() -> {
                                        if (stopsCardAdapter != null) stopsCardAdapter.notifyDataSetChanged();
                                        if (stopsListAdapter != null) stopsListAdapter.notifyDataSetChanged();
                                    });
                                }
                            });
                            Toast.makeText(getContext(), "Feedback enviado!", Toast.LENGTH_SHORT).show();
                        });
                    }

                    if (btnDirectComments != null) {
                        btnDirectComments.setText("💬 " + comments);
                        btnDirectComments.setOnClickListener(vClick -> {
                            promptFeedbackComment(stop.address, () -> {
                                loadGlobalCardForStopDetails(dialogView, stop);
                                if (stopsCardAdapter != null) stopsCardAdapter.notifyDataSetChanged();
                                if (stopsListAdapter != null) stopsListAdapter.notifyDataSetChanged();
                            });
                        });
                    }

                    if (btnDownload != null) {
                        if (isRealFix && !alreadyApplied) {
                            btnDownload.setVisibility(View.VISIBLE);
                            btnDownload.setEnabled(true);
                            btnDownload.setText("📥 BAIXAR E APLICAR CORREÇÃO DA COMUNIDADE");
                        } else {
                            btnDownload.setVisibility(View.GONE);
                        }
                    }

                    if (textGlobalNoteDetails != null) {
                        if (note != null && !note.isEmpty()) {
                            textGlobalNoteDetails.setVisibility(View.VISIBLE);
                            textGlobalNoteDetails.setText("Obs: " + note);
                        } else {
                            textGlobalNoteDetails.setVisibility(View.GONE);
                        }
                    }
                });
            }
            @Override public void onError(String msg) {}
        });
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
            Set<String> targetBuyers = new HashSet<>(Arrays.asList(target.allAddresses.split("\n")));
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
            Set<String> sourceBuyers = new HashSet<>(newPackagesList);
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
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnModernNegative);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btnModernPositive);

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
        showGlobalFeedbackDialog(address, null);
    }

    private void showGlobalFeedbackDialog(String address, Runnable onVoteCast) {
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_community_feedback, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(v).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        String currentUserId = sharedPreferences.getString("current_user_id", "anon");
        String userName = sharedPreferences.getString("profile_name", "Entregador");

        Runnable refreshAction = () -> {
            Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(() -> {
                    if (stopsCardAdapter != null) stopsCardAdapter.notifyDataSetChanged();
                    if (stopsListAdapter != null) stopsListAdapter.notifyDataSetChanged();
                    if (onVoteCast != null) onVoteCast.run();
                });
            }
        };

        v.findViewById(R.id.btnFeedbackLike).setOnClickListener(v2 -> {
            FirebaseHelper.addFeedback(address, true, null, userName, currentUserId, refreshAction);
            Toast.makeText(getContext(), "Valeu!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        v.findViewById(R.id.btnFeedbackDislike).setOnClickListener(v2 -> {
            FirebaseHelper.addFeedback(address, false, null, userName, currentUserId, refreshAction);
            Toast.makeText(getContext(), "Feedback enviado!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        v.findViewById(R.id.btnFeedbackComments).setOnClickListener(v2 -> {
            dialog.dismiss();
            promptFeedbackComment(address, refreshAction);
        });

        v.findViewById(R.id.btnFeedbackClose).setOnClickListener(v2 -> dialog.dismiss());
        
        dialog.show();
    }

    private void promptFeedbackComment(String address, Runnable refreshAction) {
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_community_comments, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(v).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        LinearLayout layoutCommentsList = v.findViewById(R.id.layoutCommentsList);
        EditText editInput = v.findViewById(R.id.editCommentInput);
        String currentUserId = sharedPreferences.getString("current_user_id", "anon");
        String userName = sharedPreferences.getString("profile_name", "Entregador");

        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        String myEmail = fUser != null ? fUser.getEmail() : "";
        String myUid = fUser != null ? fUser.getUid() : "";

        FirebaseHelper.fetchComments(address, new FirebaseHelper.CommentsFetchCallback() {
            @Override public void onSuccess(List<CommentModel> list) {
                Activity activity = getActivity();
                if (activity != null) activity.runOnUiThread(() -> {
                    layoutCommentsList.removeAllViews();
                    if (list.isEmpty()) {
                        TextView tv = new TextView(getContext());
                        tv.setText("Nenhum comentário ainda.");
                        tv.setPadding(10, 20, 10, 20);
                        tv.setGravity(Gravity.CENTER);
                        layoutCommentsList.addView(tv);
                    } else {
                        for (CommentModel c : list) {
                            View commentView = LayoutInflater.from(requireContext()).inflate(R.layout.item_quadra_comment, layoutCommentsList, false);
                            TextView txtUser = commentView.findViewById(R.id.textCommentUser);
                            TextView txtTime = commentView.findViewById(R.id.textCommentTime);
                            TextView txtBody = commentView.findViewById(R.id.textCommentBody);
                            ImageButton btnDel = commentView.findViewById(R.id.btnDeleteComment);

                            if (txtUser != null) txtUser.setText(c.user != null ? c.user : "Anônimo");
                            if (txtBody != null) txtBody.setText(c.text != null ? c.text : "");
                            if (txtTime != null) {
                                if (c.date > 0) {
                                    txtTime.setText(DateUtils.getRelativeTimeSpanString(c.date, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
                                } else {
                                    txtTime.setText("agora");
                                }
                            }

                            boolean isCommentMine = (c.userId != null && (c.userId.equals(currentUserId) || c.userId.equals(myUid) || (myEmail != null && !myEmail.isEmpty() && c.userId.equalsIgnoreCase(myEmail))));

                            if (btnDel != null) {
                                btnDel.setVisibility(isCommentMine ? View.VISIBLE : View.GONE);
                                btnDel.setOnClickListener(vDel -> {
                                    showModernConfirmDialog("Excluir Comentário", "Deseja apagar o seu comentário?", "EXCLUIR", () -> {
                                        FirebaseHelper.deleteComment(address, c.id, new FirebaseHelper.GlobalUploadCallback() {
                                            @Override public void onSuccess() {
                                                Activity act = getActivity();
                                                if (act != null) {
                                                    act.runOnUiThread(() -> {
                                                        layoutCommentsList.removeView(commentView);
                                                        if (layoutCommentsList.getChildCount() == 0) {
                                                            TextView tvEmpty = new TextView(getContext());
                                                            tvEmpty.setText("Nenhum comentário ainda.");
                                                            tvEmpty.setPadding(10, 20, 10, 20);
                                                            tvEmpty.setGravity(Gravity.CENTER);
                                                            layoutCommentsList.addView(tvEmpty);
                                                        }
                                                        Toast.makeText(getContext(), "Comentário excluído!", Toast.LENGTH_SHORT).show();
                                                        if (refreshAction != null) refreshAction.run();
                                                    });
                                                }
                                            }
                                            @Override public void onFailure(String msg) {
                                                Activity act = getActivity();
                                                if (act != null) {
                                                    act.runOnUiThread(() -> Toast.makeText(getContext(), "Erro ao excluir: " + msg, Toast.LENGTH_SHORT).show());
                                                }
                                            }
                                        });
                                    });
                                });
                            }

                            layoutCommentsList.addView(commentView);
                        }
                    }
                });
            }
            @Override public void onError(String msg) {}
        });

        v.findViewById(R.id.btnSendComment).setOnClickListener(v2 -> {
            String text = editInput.getText().toString().trim();
            if (!text.isEmpty()) {
                FirebaseHelper.addFeedback(address, null, text, userName, currentUserId, refreshAction);
                Toast.makeText(getContext(), "Comentário enviado!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        v.findViewById(R.id.btnCancelComments).setOnClickListener(v2 -> dialog.dismiss());

        dialog.show();
    }

    private void exitFixMode() {
        Activity activity = getActivity();
        Runnable doExit = () -> {
            if (cardFixMode != null) {
                cardFixMode.setVisibility(View.GONE);
            }
            if (map != null) {
                if (currentFixOverlay != null) {
                    map.getOverlays().remove(currentFixOverlay);
                    currentFixOverlay = null;
                }
                if (homeSelectionOverlay != null) {
                    map.getOverlays().remove(homeSelectionOverlay);
                    homeSelectionOverlay = null;
                }
                if (loadingSelectionOverlay != null) {
                    map.getOverlays().remove(loadingSelectionOverlay);
                    loadingSelectionOverlay = null;
                }
                if (quadraSelectionOverlay != null) {
                    map.getOverlays().remove(quadraSelectionOverlay);
                    quadraSelectionOverlay = null;
                }
                map.getOverlays().removeIf(o -> o instanceof MapEventsOverlay && o != searchMapEventsOverlay);
                map.invalidate();
            }
        };
        if (activity != null) {
            activity.runOnUiThread(doExit);
        } else {
            doExit.run();
        }
    }

    private void promptSubstitutionRequestDialog(CorrectedAddress newAddr, String creatorName) {
        if (getContext() == null || newAddr == null) return;

        String currentUserId = sharedPreferences.getString("current_user_id", "anon");
        String userName = sharedPreferences.getString("profile_name", "Entregador");

        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_modern_confirm, null);
        TextView title = v.findViewById(R.id.textModernTitle);
        TextView message = v.findViewById(R.id.textModernMessage);
        MaterialButton btnCancel = v.findViewById(R.id.btnModernNegative);
        MaterialButton btnConfirm = v.findViewById(R.id.btnModernPositive);

        if (title != null) title.setText("Endereço Já Corrigido");
        if (message != null) {
            message.setText("O endereço \"" + newAddr.address + "\" já possui uma correção na comunidade enviada por " 
                    + (creatorName != null && !creatorName.isEmpty() ? creatorName : "outro entregador") 
                    + ".\n\nPara evitar divergências, você não pode sobrescrevê-lo diretamente. Deseja enviar uma solicitação de substituição para análise de um desenvolvedor?");
        }

        if (btnCancel != null) btnCancel.setText("CANCELAR");
        if (btnConfirm != null) btnConfirm.setText("SOLICITAR SUBSTITUIÇÃO");

        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(v).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        if (btnCancel != null) btnCancel.setOnClickListener(v2 -> dialog.dismiss());
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v2 -> {
                dialog.dismiss();
                promptSubstitutionReasonDialog(newAddr, currentUserId, userName);
            });
        }

        dialog.show();
    }

    private void promptSubstitutionReasonDialog(CorrectedAddress newAddr, String userId, String userName) {
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_community_comments, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(v).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        EditText editReason = v.findViewById(R.id.editCommentInput);
        View btnSend = v.findViewById(R.id.btnSendComment);
        View btnCancel = v.findViewById(R.id.btnCancelComments);

        if (editReason != null) {
            editReason.setHint("Explique o motivo da alteração (ex: portão correto fica na rua de trás)...");
        }

        if (btnSend != null) {
            btnSend.setOnClickListener(v2 -> {
                String reason = editReason != null ? editReason.getText().toString().trim() : "";
                if (reason.isEmpty()) {
                    reason = "Solicitação de substituição de localização";
                }
                FirebaseHelper.requestAddressSubstitution(newAddr, userId, userName, reason, new FirebaseHelper.GlobalUploadCallback() {
                    @Override
                    public void onSuccess() {
                        Activity act = getActivity();
                        if (act != null) {
                            act.runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Solicitação de substituição enviada para análise!", Toast.LENGTH_LONG).show();
                                dialog.dismiss();
                            });
                        }
                    }

                    @Override
                    public void onFailure(String msg) {
                        Activity act = getActivity();
                        if (act != null) {
                            act.runOnUiThread(() -> Toast.makeText(getContext(), "Erro ao solicitar: " + msg, Toast.LENGTH_SHORT).show());
                        }
                    }
                });
            });
        }

        if (btnCancel != null) btnCancel.setOnClickListener(v2 -> dialog.dismiss());
        dialog.show();
    }

    private void showUndoCorrectionSnackbar(RouteStop stop, double oldLat, double oldLon, CorrectedAddress oldCa, CorrectedAddress newCa) {
        if (cardUndoCorrection == null || getContext() == null) return;

        if (pendingUndoRunnable != null) {
            undoHandler.removeCallbacks(pendingUndoRunnable);
        }

        if (textUndoCorrectionMessage != null) {
            String addrName = (stop.address != null && !stop.address.isEmpty()) ? stop.address : "Endereço";
            textUndoCorrectionMessage.setText("📍 Localização de \"" + addrName + "\" corrigida!");
        }

        if (btnUndoCorrectionAction != null) {
            btnUndoCorrectionAction.setOnClickListener(v -> {
                if (pendingUndoRunnable != null) {
                    undoHandler.removeCallbacks(pendingUndoRunnable);
                    pendingUndoRunnable = null;
                }
                cardUndoCorrection.setVisibility(View.GONE);

                new Thread(() -> {
                    AppDao dao = AppDatabase.getInstance(requireContext()).appDao();

                    stop.latitude = oldLat;
                    stop.longitude = oldLon;
                    dao.updateRouteStop(stop);

                    if (oldCa == null) {
                        if (newCa != null) dao.deleteCorrectedAddress(newCa);
                    } else {
                        dao.updateCorrectedAddress(oldCa);
                    }

                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.runOnUiThread(() -> {
                            refreshMarkers();
                            Toast.makeText(getContext(), "↩️ Correção desfeita com sucesso!", Toast.LENGTH_SHORT).show();
                            CloudSyncHelper.syncNow(requireContext(), "Desfazer Correção");
                        });
                    }
                }).start();
            });
        }

        cardUndoCorrection.setVisibility(View.VISIBLE);

        pendingUndoRunnable = () -> {
            if (cardUndoCorrection != null) {
                cardUndoCorrection.setVisibility(View.GONE);
            }
            pendingUndoRunnable = null;

            // 🔥 O envio para a comunidade ocorre SOMENTE APÓS os 5 segundos expirarem sem clicar em Desfazer!
            boolean isManualStop = stop.atId == null || stop.atId.isEmpty();
            if (!isManualStop && sharedPreferences != null && sharedPreferences.getBoolean("auto_share_corrections", true)) {
                String currentUserId = sharedPreferences.getString("current_user_id", "anon");
                String uName = sharedPreferences.getString("profile_name", "Entregador");
                AppDao dao = AppDatabase.getInstance(requireContext()).appDao();

                FirebaseHelper.uploadCorrection(currentUserId, uName, newCa, new FirebaseHelper.GlobalUploadCallback() {
                    @Override public void onSuccess() {
                        new Thread(() -> {
                            newCa.creatorId = currentUserId;
                            dao.updateCorrectedAddress(newCa);
                        }).start();
                    }
                    @Override public void onFailure(String msg) {
                        if (msg != null && msg.startsWith("ALREADY_CORRECTED_BY_OTHER")) {
                            String[] parts = msg.split(":");
                            String creatorName = (parts.length > 1 && !parts[1].isEmpty()) ? parts[1] : "outro entregador";
                            Activity act = getActivity();
                            if (act != null) {
                                act.runOnUiThread(() -> promptSubstitutionRequestDialog(newCa, creatorName));
                            }
                        }
                    }
                });
            }
        };

        undoHandler.postDelayed(pendingUndoRunnable, 5000);
    }

    private void promptCorrectLocation(RouteStop stop) {
        if (cardFixMode != null) {
            TextView textFix = cardFixMode.findViewById(R.id.textFixTitle);
            if (textFix != null) textFix.setText("Toque no mapa para corrigir");
            cardFixMode.setVisibility(View.VISIBLE);
        }
        Toast.makeText(getContext(), "Toque no local correto", Toast.LENGTH_LONG).show();
        currentFixOverlay = new MapEventsOverlay(new MapEventsReceiver() {
            @Override public boolean singleTapConfirmedHelper(GeoPoint p) {
                new Thread(() -> {
                    AppDao dao = AppDatabase.getInstance(requireContext()).appDao();

                    double oldLat = stop.latitude;
                    double oldLon = stop.longitude;
                    CorrectedAddress oldCa = dao.getCorrectedAddress(stop.address);

                    stop.latitude = p.getLatitude(); 
                    stop.longitude = p.getLongitude();
                    dao.updateRouteStop(stop);
                    
                    CorrectedAddress ca = oldCa;

                    if (ca == null) {
                        ca = new CorrectedAddress(stop.address, stop.neighborhood, stop.latitude, stop.longitude);
                    } else {
                        ca.latitude = stop.latitude;
                        ca.longitude = stop.longitude;
                    }
                    ca.creatorId = null;
                    ca.updatedAt = System.currentTimeMillis();
                    long newId = dao.insertCorrectedAddress(ca);
                    ca.id = (int) newId;
                    final CorrectedAddress finalCa = ca;

                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.runOnUiThread(() -> {
                            refreshMarkers();
                            showUndoCorrectionSnackbar(stop, oldLat, oldLon, oldCa, finalCa);
                        });
                    }

                    exitFixMode();
                }).start();
                return true;
            }
            @Override public boolean longPressHelper(GeoPoint p) { return false; }
        });
        map.getOverlays().add(currentFixOverlay);
    }
    public void showRouteTimerOptionsPopup(View v) {
        if (getContext() == null || v == null) return;
        if (currentRouteHeader != null && currentRouteHeader.startTime == 0) {
            startRouteTimerManually();
            return;
        }

        PopupMenu p = new PopupMenu(requireContext(), v);

        boolean homeDefined = sharedPreferences != null && sharedPreferences.getFloat("home_lat", 0) != 0;
        if (!homeDefined) {
            p.getMenu().add("Definir Endereço de Casa");
        }

        if (currentRouteHeader != null && currentRouteHeader.startTime > 0 && currentRouteHeader.endTime == 0) {
            if (currentRouteHeader.lastPauseStartTime > 0) {
                p.getMenu().add("Retomar Cronômetro");
            } else {
                p.getMenu().add("Pausar Cronômetro");
            }
        }

        if (currentRouteHeader != null && currentRouteHeader.startTime > 0) {
            p.getMenu().add("Estatísticas da Rota");
        }
        p.getMenu().add("Reiniciar Rota");

        p.setOnMenuItemClickListener(item -> {
            if ("Definir Endereço de Casa".equals(item.getTitle())) {
                startHomeSelection();
            } else if ("Retomar Cronômetro".equals(item.getTitle())) {
                long now = System.currentTimeMillis();
                if (currentRouteHeader != null && currentRouteHeader.lastPauseStartTime > 0) {
                    long pauseDuration = now - currentRouteHeader.lastPauseStartTime;
                    currentRouteHeader.totalPausedMs += pauseDuration;
                    currentRouteHeader.lastPauseStartTime = 0;
                    final RouteHeader h = currentRouteHeader;
                    new Thread(() -> AppDatabase.getInstance(requireContext()).appDao().updateRouteHeader(h)).start();
                    Toast.makeText(getContext(), "▶️ Cronômetro retomado!", Toast.LENGTH_SHORT).show();
                }
            } else if ("Pausar Cronômetro".equals(item.getTitle())) {
                long now = System.currentTimeMillis();
                if (currentRouteHeader != null && currentRouteHeader.lastPauseStartTime == 0) {
                    currentRouteHeader.lastPauseStartTime = now;
                    final RouteHeader h = currentRouteHeader;
                    new Thread(() -> AppDatabase.getInstance(requireContext()).appDao().updateRouteHeader(h)).start();
                    Toast.makeText(getContext(), "⏸️ Cronômetro pausado!", Toast.LENGTH_SHORT).show();
                }
            } else if ("Estatísticas da Rota".equals(item.getTitle())) {
                showRouteStatsPopup();
            } else if ("Reiniciar Rota".equals(item.getTitle())) {
                promptResetRoute();
            }
            return true;
        });
        p.show();
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
        boolean isOptPending = sharedPreferences != null && sharedPreferences.getBoolean("route_optimization_pending_" + currentRouteId, false);
        if (isOptPending) return;

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

    private void updateRouteCorrectionsCount() {
        if (currentRouteId == -1 || currentStops == null || currentStops.isEmpty() || getContext() == null) {
            Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(() -> {
                    if (cardRouteCorrections != null) cardRouteCorrections.setVisibility(View.GONE);
                });
            }
            return;
        }

        final List<RouteStop> stopsCopy = new ArrayList<>(currentStops);
        final Context ctx = getContext();
        if (ctx == null) return;

        new Thread(() -> {
            try {
                AppDao dao = AppDatabase.getInstance(ctx.getApplicationContext()).appDao();
                List<CorrectedAddress> allCa = dao.getAllCorrectedAddresses();
                
                Map<String, CorrectedAddress> localCaMap = new HashMap<>();
                if (allCa != null) {
                    for (CorrectedAddress ca : allCa) {
                        if (ca.address != null && !ca.address.trim().isEmpty()) {
                            localCaMap.put(ca.address.trim().toUpperCase(), ca);
                        }
                    }
                }

                final List<RouteStop> availableCorrectionsStops = Collections.synchronizedList(new ArrayList<>());
                final Map<Integer, CorrectedAddress> stopToCaMap = new ConcurrentHashMap<>();

                List<RouteStop> stopsNeedingGlobalCheck = new ArrayList<>();

                for (RouteStop s : stopsCopy) {
                    if (s.address == null || s.address.trim().isEmpty()) continue;
                    String addrClean = s.address.trim().toUpperCase();
                    
                    CorrectedAddress ca = localCaMap.get(addrClean);
                    if (ca == null) {
                        ca = dao.getCorrectedAddress(s.address);
                    }

                    if (ca != null) {
                        // Se existe uma correção local para este endereço,
                        // verifica se a parada nesta rota JÁ ESTÁ usando estas coordenadas corrigidas
                        boolean alreadyApplied = Math.abs(s.latitude - ca.latitude) < 0.00001 
                                              && Math.abs(s.longitude - ca.longitude) < 0.00001;
                        if (!alreadyApplied) {
                            availableCorrectionsStops.add(s);
                            stopToCaMap.put(s.id, ca);
                        }
                    } else {
                        stopsNeedingGlobalCheck.add(s);
                    }
                }

                // Atualiza UI com as correções já encontradas no banco local
                updateCorrectionsBalloonUI(availableCorrectionsStops, stopToCaMap);

                if (stopsNeedingGlobalCheck.isEmpty()) {
                    return;
                }

                final String currentUserId = ctx.getSharedPreferences("AppConfig", Context.MODE_PRIVATE).getString("current_user_id", "anon");

                for (RouteStop s : stopsNeedingGlobalCheck) {
                    FirebaseHelper.searchGlobal(s.address, new FirebaseHelper.GlobalCorrectionCallback() {
                        @Override
                        public void onResult(double lat, double lon, int likes, int dislikes, String creatorId, String note, int comments, String creatorName, long date, boolean hasCoordinateFix) {
                            if (!hasCoordinateFix || lat == 0.0 || lon == 0.0 || Math.abs(lat) < 0.001) {
                                return; // Ignora se não houver uma correção de coordenadas real na nuvem!
                            }

                            boolean alreadyApplied = Math.abs(s.latitude - lat) < 0.00001 && Math.abs(s.longitude - lon) < 0.00001;

                            if (!alreadyApplied) {
                                CorrectedAddress globalCa = new CorrectedAddress(s.address, s.neighborhood, lat, lon);
                                globalCa.notes = note;
                                globalCa.creatorId = creatorId != null ? creatorId : "community_anon";

                                synchronized (availableCorrectionsStops) {
                                    if (!availableCorrectionsStops.contains(s)) {
                                        availableCorrectionsStops.add(s);
                                        stopToCaMap.put(s.id, globalCa);
                                    }
                                }
                                updateCorrectionsBalloonUI(availableCorrectionsStops, stopToCaMap);
                            }
                        }
                        @Override public void onError(String msg) {}
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateCorrectionsBalloonUI(List<RouteStop> availableStops, Map<Integer, CorrectedAddress> caMap) {
        Activity activity = getActivity();
        if (activity == null) return;

        activity.runOnUiThread(() -> {
            if (cardRouteCorrections != null && textRouteCorrections != null) {
                int count;
                List<RouteStop> copyStops;
                Map<Integer, CorrectedAddress> copyMap;
                synchronized (availableStops) {
                    count = availableStops.size();
                    copyStops = new ArrayList<>(availableStops);
                    copyMap = new HashMap<>(caMap);
                }

                if (count > 0) {
                    cardRouteCorrections.setVisibility(View.VISIBLE);
                    String label = count == 1 ? "1 correção disponível" : count + " correções disponíveis";
                    textRouteCorrections.setText(label);
                    
                    Object[] payload = new Object[] { copyStops, copyMap };
                    cardRouteCorrections.setTag(payload);
                } else {
                    cardRouteCorrections.setVisibility(View.GONE);
                }
            }
        });
    }

    private void showRouteCorrectionsDialog() {
        if (cardRouteCorrections == null || getContext() == null) return;
        Object tag = cardRouteCorrections.getTag();
        if (!(tag instanceof Object[])) return;

        Object[] payload = (Object[]) tag;
        if (payload.length < 2) return;

        @SuppressWarnings("unchecked")
        List<RouteStop> stopsWithFixes = (List<RouteStop>) payload[0];
        @SuppressWarnings("unchecked")
        Map<Integer, CorrectedAddress> caMap = (Map<Integer, CorrectedAddress>) payload[1];

        if (stopsWithFixes == null || stopsWithFixes.isEmpty()) return;

        Context ctx = requireContext();

        LinearLayout rootLayout = new LinearLayout(ctx);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        int p = (int) (16 * getResources().getDisplayMetrics().density);
        rootLayout.setPadding(p, p, p, p);

        // 🔥 Botão Destacado na Parte Superior para Baixar e Aplicar Todas as Correções da Comunidade
        MaterialButton btnApplyAll = new MaterialButton(ctx);
        btnApplyAll.setText("📥 Baixar e aplicar todas as correções da comunidade (" + stopsWithFixes.size() + ")");
        btnApplyAll.setTextSize(13f);
        btnApplyAll.setTypeface(Typeface.DEFAULT_BOLD);
        btnApplyAll.setCornerRadius((int) (12 * getResources().getDisplayMetrics().density));
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.bottomMargin = (int) (12 * getResources().getDisplayMetrics().density);
        btnApplyAll.setLayoutParams(btnParams);

        rootLayout.addView(btnApplyAll);

        // Subtítulo
        TextView textListTitle = new TextView(ctx);
        textListTitle.setText("Paradas com correção disponível:");
        textListTitle.setTextSize(12f);
        textListTitle.setTextColor(Color.parseColor("#666666"));
        textListTitle.setPadding(0, 0, 0, (int) (8 * getResources().getDisplayMetrics().density));
        rootLayout.addView(textListTitle);

        // Lista de paradas individuais
        String[] items = new String[stopsWithFixes.size() + 1];
        for (int i = 0; i < stopsWithFixes.size(); i++) {
            RouteStop s = stopsWithFixes.get(i);
            items[i] = "Parada " + s.stopNumber + ": " + s.address;
        }
        items[stopsWithFixes.size()] = "📍 Gerenciar Endereços Corrigidos";

        ListView listView = new ListView(ctx);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                ctx, android.R.layout.simple_list_item_1, items);
        listView.setAdapter(adapter);

        int listHeight = (int) (Math.min(stopsWithFixes.size() + 1, 5) * 48 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, listHeight);
        listView.setLayoutParams(listParams);
        rootLayout.addView(listView);

        AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setTitle("Correções na Rota (" + stopsWithFixes.size() + ")")
                .setView(rootLayout)
                .setNegativeButton("Fechar", null)
                .create();

        btnApplyAll.setOnClickListener(v -> {
            dialog.dismiss();
            applyAllRouteCorrections(stopsWithFixes, caMap);
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            dialog.dismiss();
            if (position == stopsWithFixes.size()) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openFragmentInSettings(new CorrectedAddressesParentFragment(), "Endereços Corrigidos");
                }
            } else {
                RouteStop selected = stopsWithFixes.get(position);
                if (mapController != null && selected.latitude != 0 && selected.longitude != 0) {
                    mapController.animateTo(new GeoPoint(selected.latitude, selected.longitude));
                }
                showStopDetails(selected);
            }
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog_rounded);
        }
        dialog.show();
    }

    private void applyAllRouteCorrections(List<RouteStop> stopsToFix, Map<Integer, CorrectedAddress> caMap) {
        if (stopsToFix == null || stopsToFix.isEmpty() || getContext() == null) return;

        final Context ctx = getContext();
        new Thread(() -> {
            try {
                AppDao dao = AppDatabase.getInstance(ctx.getApplicationContext()).appDao();
                int appliedCount = 0;

                for (RouteStop s : stopsToFix) {
                    CorrectedAddress ca = caMap.get(s.id);
                    if (ca != null && ca.latitude != 0.0 && ca.longitude != 0.0 && Math.abs(ca.latitude) > 0.001) {
                        CorrectedAddress localCa = dao.getCorrectedAddress(s.address);
                        if (localCa == null) {
                            dao.insertCorrectedAddress(ca);
                        }

                        s.latitude = ca.latitude;
                        s.longitude = ca.longitude;
                        dao.updateRouteStop(s);
                        appliedCount++;
                    }
                }

                final int finalApplied = appliedCount;
                Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        Toast.makeText(getContext(), finalApplied + " correções da comunidade baixadas e aplicadas!", Toast.LENGTH_SHORT).show();
                        updateRouteCorrectionsCount();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void cleanupInvalidLocalFixes(Context context) {
        if (context == null) return;
        new Thread(() -> {
            try {
                AppDao dao = AppDatabase.getInstance(context.getApplicationContext()).appDao();
                List<CorrectedAddress> all = dao.getAllCorrectedAddresses();
                for (CorrectedAddress ca : all) {
                    if (ca.latitude == 0.0 || ca.longitude == 0.0 || Math.abs(ca.latitude) < 0.001) {
                        dao.deleteCorrectedAddress(ca);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void flashStatsSummary(View targetCard) {
        if (layoutStatsGroup == null || targetCard == null || getContext() == null) return;
        boolean isOptPending = sharedPreferences != null && sharedPreferences.getBoolean("route_optimization_pending_" + currentRouteId, false);
        boolean isManualPending = sharedPreferences != null && sharedPreferences.getBoolean("route_manual_list_pending_" + currentRouteId, false);
        if (isOptPending || isManualPending) return;

        boolean isExpandedPermanently = sharedPreferences != null && sharedPreferences.getBoolean("stats_summary_expanded", true);

        // Se o painel já estiver visível permanentemente, não precisamos fazer o flash temporário
        if (isExpandedPermanently && layoutStatsGroup.getVisibility() == View.VISIBLE && autoHideRunnable == null) {
            return;
        }

        // Exibe temporariamente o card referente à ação
        int errStops = 0;
        if (currentStops != null) {
            for (RouteStop s : currentStops) if (s.deliveryStatus == 2) errStops++;
        }

        if (cardSuccessSummary != null) cardSuccessSummary.setVisibility((targetCard == cardSuccessSummary || isExpandedPermanently) ? View.VISIBLE : View.GONE);
        if (cardPendingSummary != null) cardPendingSummary.setVisibility((targetCard == cardSuccessSummary || targetCard == cardPendingSummary || targetCard == cardFailedSummary || isExpandedPermanently) ? View.VISIBLE : View.GONE);
        if (cardFailedSummary != null) cardFailedSummary.setVisibility((targetCard == cardFailedSummary || (isExpandedPermanently && errStops > 0)) ? View.VISIBLE : View.GONE);

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
                boolean currentExpandedSetting = sharedPreferences.getBoolean("stats_summary_expanded", true);
                if (!currentExpandedSetting) {
                    layoutStatsGroup.animate()
                            .alpha(0f)
                            .translationY(-20f)
                            .setDuration(250)
                            .withEndAction(() -> {
                                layoutStatsGroup.setVisibility(View.GONE);
                                autoHideRunnable = null;
                            })
                            .start();
                } else {
                    autoHideRunnable = null;
                    if (cardSuccessSummary != null) cardSuccessSummary.setVisibility(View.VISIBLE);
                    if (cardPendingSummary != null) cardPendingSummary.setVisibility(View.VISIBLE);
                    if (cardFailedSummary != null) {
                        int currentErrStops = 0;
                        if (currentStops != null) {
                            for (RouteStop s : currentStops) if (s.deliveryStatus == 2) currentErrStops++;
                        }
                        cardFailedSummary.setVisibility(currentErrStops > 0 ? View.VISIBLE : View.GONE);
                    }
                }
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
                ((TextView)h.itemView).setText(Html.fromHtml(statsList.get(pos), Html.FROM_HTML_MODE_COMPACT));
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
        CircularProgressIndicator progress = dv.findViewById(R.id.progressOptimization);
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
        CircularProgressIndicator progress = dv.findViewById(R.id.progressOptimization);
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
                    
                    String uniqueId = Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID);
                    String userAgent = "DriveLogApp_v1527_" + uniqueId;
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
                
                Overlay selectedMarker = null;
                List<Overlay> otherMarkers = new ArrayList<>();
                
                // Primeiro removemos todos os marcadores de paradas para reinserir na ordem correta
                List<Overlay> stopsToRemove = new ArrayList<>();
                for (Overlay o : map.getOverlays()) {
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

    public String getFormattedSeqInfo(RouteStop s) {
        if (s == null) return "";
        String seqStr = (s.allSequences != null && !s.allSequences.isEmpty()) ? s.allSequences : String.valueOf(s.sequence);
        if (s.vehicleLocation == null || s.vehicleLocation.trim().isEmpty()) {
            return "Seq: " + seqStr;
        }
        
        String loc = s.vehicleLocation.trim();
        if (!loc.contains(":")) {
            return "Seq: " + seqStr + " (" + loc + ")";
        }
        
        Map<String, String> map = new HashMap<>();
        String[] parts = loc.split(";");
        for (String p : parts) {
            String[] kv = p.split(":");
            if (kv.length == 2) {
                map.put(kv[0].trim(), kv[1].trim());
            }
        }
        
        String[] seqArray = seqStr.split(",\\s*");
        Set<String> uniqueLocs = new HashSet<>(map.values());
        
        // Se TODAS as sequências possuem EXATAMENTE a mesma localização
        if (map.size() == seqArray.length && uniqueLocs.size() == 1) {
            String singleLoc = uniqueLocs.iterator().next();
            return "Seq: " + seqStr + " (" + singleLoc + ")";
        }
        
        // Mapeia a localização especificamente ao lado de cada sequência definida!
        StringBuilder sb = new StringBuilder("Seq: ");
        for (int i = 0; i < seqArray.length; i++) {
            String seqNum = seqArray[i].trim();
            sb.append(seqNum);
            if (map.containsKey(seqNum)) {
                sb.append(" (").append(map.get(seqNum)).append(")");
            }
            if (i < seqArray.length - 1) sb.append(", ");
        }
        return sb.toString();
    }

    public void showVehicleLocationPicker(RouteStop s, View anchorView) {
        if (getContext() == null || s == null) return;

        String seqStr = (s.allSequences != null && !s.allSequences.isEmpty()) ? s.allSequences : String.valueOf(s.sequence);
        String[] seqArray = seqStr.split(",\\s*");

        if (seqArray.length <= 1) {
            PopupMenu popup = new PopupMenu(requireContext(), anchorView);
            popup.getMenu().add("Frente");
            popup.getMenu().add("Passageiro");
            popup.getMenu().add("Porta Mala");
            popup.getMenu().add("❌ Remover Localização");

            popup.setOnMenuItemClickListener(item -> {
                CharSequence title = item.getTitle();
                if ("❌ Remover Localização".equals(title)) {
                    s.vehicleLocation = null;
                } else {
                    s.vehicleLocation = title.toString();
                }
                updateStopInDb(s);
                if (stopsCardAdapter != null) stopsCardAdapter.notifyDataSetChanged();
                if (stopsListAdapter != null) stopsListAdapter.notifyDataSetChanged();
                return true;
            });
            popup.show();
        } else {
            showMultiPackageLocationDialog(s, seqArray);
        }
    }

    private void showMultiPackageLocationDialog(RouteStop s, String[] seqArray) {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_multi_package_location, null);
        builder.setView(v);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView textTitle = v.findViewById(R.id.textMultiLocTitle);
        LinearLayout container = v.findViewById(R.id.containerPackageLocs);
        MaterialButton btnSave = v.findViewById(R.id.btnSaveMultiLoc);
        MaterialButton btnClear = v.findViewById(R.id.btnClearMultiLoc);

        if (textTitle != null) textTitle.setText("🚗 Local dos Pacotes (#" + s.stopNumber + ")");

        Map<String, String> currentMap = new HashMap<>();
        if (s.vehicleLocation != null && !s.vehicleLocation.trim().isEmpty()) {
            String loc = s.vehicleLocation.trim();
            if (!loc.contains(":")) {
                for (String seq : seqArray) currentMap.put(seq.trim(), loc);
            } else {
                String[] parts = loc.split(";");
                for (String p : parts) {
                    String[] kv = p.split(":");
                    if (kv.length == 2) currentMap.put(kv[0].trim(), kv[1].trim());
                }
            }
        }

        String[] options = new String[]{"-- Selecionar --", "Frente", "Passageiro", "Porta Mala"};
        Map<String, Spinner> spinnerMap = new HashMap<>();

        if (container != null) {
            container.removeAllViews();
            for (String seq : seqArray) {
                String trimmedSeq = seq.trim();
                LinearLayout row = new LinearLayout(requireContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, 12, 0, 12);

                TextView label = new TextView(requireContext());
                label.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                label.setText("📦 Seq " + trimmedSeq + ":");
                label.setTextSize(13);
                label.setTypeface(null, Typeface.BOLD);
                label.setTextColor(Color.parseColor("#333333"));

                Spinner spinner = new Spinner(requireContext());
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, options);
                spinner.setAdapter(adapter);

                String existingLoc = currentMap.get(trimmedSeq);
                if ("Frente".equals(existingLoc)) spinner.setSelection(1);
                else if ("Passageiro".equals(existingLoc)) spinner.setSelection(2);
                else if ("Porta Mala".equals(existingLoc)) spinner.setSelection(3);
                else spinner.setSelection(0);

                spinnerMap.put(trimmedSeq, spinner);

                row.addView(label);
                row.addView(spinner);
                container.addView(row);
            }
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(vSave -> {
                StringBuilder sb = new StringBuilder();
                Set<String> uniqueLocs = new HashSet<>();
                int selectedCount = 0;

                for (String seq : seqArray) {
                    String trimmedSeq = seq.trim();
                    Spinner sp = spinnerMap.get(trimmedSeq);
                    if (sp != null) {
                        int pos = sp.getSelectedItemPosition();
                        if (pos > 0 && pos < options.length) {
                            String selectedLoc = options[pos];
                            uniqueLocs.add(selectedLoc);
                            selectedCount++;
                            if (sb.length() > 0) sb.append(";");
                            sb.append(trimmedSeq).append(":").append(selectedLoc);
                        }
                    }
                }

                if (selectedCount == 0) {
                    s.vehicleLocation = null;
                } else if (uniqueLocs.size() == 1 && selectedCount == seqArray.length) {
                    s.vehicleLocation = uniqueLocs.iterator().next();
                } else {
                    s.vehicleLocation = sb.toString();
                }

                updateStopInDb(s);
                if (stopsCardAdapter != null) stopsCardAdapter.notifyDataSetChanged();
                if (stopsListAdapter != null) stopsListAdapter.notifyDataSetChanged();
                dialog.dismiss();
                Toast.makeText(getContext(), "Localização salva!", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnClear != null) {
            btnClear.setOnClickListener(vClear -> {
                s.vehicleLocation = null;
                updateStopInDb(s);
                if (stopsCardAdapter != null) stopsCardAdapter.notifyDataSetChanged();
                if (stopsListAdapter != null) stopsListAdapter.notifyDataSetChanged();
                dialog.dismiss();
                Toast.makeText(getContext(), "Localização removida!", Toast.LENGTH_SHORT).show();
            });
        }

        dialog.show();
    }

    private void showPackageOrganizationDialog() {
        if (getContext() == null || currentStops == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_package_organization, null);
        builder.setView(v);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextInputEditText editFrente = v.findViewById(R.id.editOrgFrente);
        TextInputEditText editPassageiros = v.findViewById(R.id.editOrgPassageiros);
        TextInputEditText editPortaMalas = v.findViewById(R.id.editOrgPortaMalas);
        MaterialButton btnSave = v.findViewById(R.id.btnSaveOrg);
        MaterialButton btnCancel = v.findViewById(R.id.btnCancelOrg);

        // Preenche os campos com as sequências ou números de parada atuais
        Set<Integer> frenteSeqSet = new TreeSet<>();
        Set<Integer> passageirosSeqSet = new TreeSet<>();
        Set<Integer> portaMalasSeqSet = new TreeSet<>();

        for (RouteStop s : currentStops) {
            if (s.vehicleLocation == null || s.vehicleLocation.trim().isEmpty()) continue;
            String loc = s.vehicleLocation.trim();
            String seqStr = (s.allSequences != null && !s.allSequences.isEmpty()) ? s.allSequences : String.valueOf(s.sequence);
            String[] seqArray = seqStr.split(",\\s*");

            if (!loc.contains(":")) {
                // Localização simples para a parada/pacotes
                for (String seq : seqArray) {
                    int num = parseSeqOrStopNum(seq, s.stopNumber);
                    if (num > 0) {
                        if ("Frente".equalsIgnoreCase(loc)) frenteSeqSet.add(num);
                        else if ("Passageiro".equalsIgnoreCase(loc) || "Passageiros".equalsIgnoreCase(loc)) passageirosSeqSet.add(num);
                        else if ("Porta Mala".equalsIgnoreCase(loc) || "Porta Malas".equalsIgnoreCase(loc)) portaMalasSeqSet.add(num);
                    }
                }
            } else {
                // Localização por sequência específica, ex: "19:Frente;20:Porta Mala" ou "-:Porta Mala"
                String[] parts = loc.split(";");
                for (String p : parts) {
                    String[] kv = p.split(":");
                    if (kv.length == 2) {
                        String seqKey = kv[0].trim();
                        String locVal = kv[1].trim();
                        int num = parseSeqOrStopNum(seqKey, s.stopNumber);
                        if (num > 0) {
                            if ("Frente".equalsIgnoreCase(locVal)) frenteSeqSet.add(num);
                            else if ("Passageiro".equalsIgnoreCase(locVal) || "Passageiros".equalsIgnoreCase(locVal)) passageirosSeqSet.add(num);
                            else if ("Porta Mala".equalsIgnoreCase(locVal) || "Porta Malas".equalsIgnoreCase(locVal)) portaMalasSeqSet.add(num);
                        }
                    }
                }
            }
        }

        List<String> frenteList = new ArrayList<>();
        for (Integer num : frenteSeqSet) frenteList.add(String.valueOf(num));
        List<String> passageirosList = new ArrayList<>();
        for (Integer num : passageirosSeqSet) passageirosList.add(String.valueOf(num));
        List<String> portaMalasList = new ArrayList<>();
        for (Integer num : portaMalasSeqSet) portaMalasList.add(String.valueOf(num));

        if (editFrente != null) editFrente.setText(String.join(", ", frenteList));
        if (editPassageiros != null) editPassageiros.setText(String.join(", ", passageirosList));
        if (editPortaMalas != null) editPortaMalas.setText(String.join(", ", portaMalasList));

        setupUnsequencedTriggerWatcher(editFrente, "Frente");
        setupUnsequencedTriggerWatcher(editPassageiros, "Passageiros");
        setupUnsequencedTriggerWatcher(editPortaMalas, "Porta Malas");

        if (btnCancel != null) btnCancel.setOnClickListener(v1 -> dialog.dismiss());

        if (btnSave != null) {
            btnSave.setOnClickListener(v1 -> {
                Set<String> frenteInputSet = parseNumberSet(editFrente != null && editFrente.getText() != null ? editFrente.getText().toString() : "");
                Set<String> passageirosInputSet = parseNumberSet(editPassageiros != null && editPassageiros.getText() != null ? editPassageiros.getText().toString() : "");
                Set<String> portaMalasInputSet = parseNumberSet(editPortaMalas != null && editPortaMalas.getText() != null ? editPortaMalas.getText().toString() : "");

                new Thread(() -> {
                    AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
                    for (RouteStop s : currentStops) {
                        String stopNumStr = String.valueOf(s.stopNumber);
                        String seqStr = (s.allSequences != null && !s.allSequences.isEmpty()) ? s.allSequences : String.valueOf(s.sequence);
                        String[] seqArray = seqStr.split(",\\s*");

                        StringBuilder sb = new StringBuilder();
                        Set<String> uniqueLocs = new HashSet<>();
                        int matchedCount = 0;

                        for (String seq : seqArray) {
                            String trimmedSeq = seq.trim();
                            // Se a sequência for "-" ou vazia, aceita tanto o número da parada quanto a própria indicação
                            String seqKeyToMatch = ("-".equals(trimmedSeq) || trimmedSeq.isEmpty()) ? stopNumStr : trimmedSeq;
                            String locForSeq = null;

                            if (frenteInputSet.contains(seqKeyToMatch)) {
                                locForSeq = "Frente";
                            } else if (passageirosInputSet.contains(seqKeyToMatch)) {
                                locForSeq = "Passageiro";
                            } else if (portaMalasInputSet.contains(seqKeyToMatch)) {
                                locForSeq = "Porta Mala";
                            }

                            if (locForSeq != null) {
                                uniqueLocs.add(locForSeq);
                                matchedCount++;
                                if (sb.length() > 0) sb.append(";");
                                sb.append(trimmedSeq).append(":").append(locForSeq);
                            }
                        }

                        if (matchedCount == 0) {
                            s.vehicleLocation = null;
                        } else if (uniqueLocs.size() == 1 && matchedCount == seqArray.length) {
                            s.vehicleLocation = uniqueLocs.iterator().next();
                        } else {
                            s.vehicleLocation = sb.toString();
                        }

                        dao.updateRouteStop(s);
                    }

                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.runOnUiThread(() -> {
                            if (stopsCardAdapter != null) stopsCardAdapter.notifyDataSetChanged();
                            if (stopsListAdapter != null) stopsListAdapter.notifyDataSetChanged();
                            dialog.dismiss();
                            Toast.makeText(getContext(), "Organização dos pacotes salva!", Toast.LENGTH_SHORT).show();
                        });
                    }
                }).start();
            });
        }

        dialog.show();
    }

    private Set<String> parseNumberSet(String text) {
        Set<String> set = new HashSet<>();
        if (text == null || text.trim().isEmpty()) return set;
        String[] parts = text.split("[,;\\s]+");
        for (String p : parts) {
            String trimmed = p.trim().replaceAll("^[#pP]+", "");
            if (!trimmed.isEmpty()) {
                set.add(trimmed);
            }
        }
        return set;
    }

    private int parseSeqOrStopNum(String seqKey, int defaultStopNum) {
        if (seqKey == null || seqKey.trim().isEmpty() || "-".equals(seqKey.trim())) {
            return defaultStopNum;
        }
        try {
            return Integer.parseInt(seqKey.trim());
        } catch (NumberFormatException e) {
            return defaultStopNum;
        }
    }

    private void setupUnsequencedTriggerWatcher(TextInputEditText editText, String positionName) {
        if (editText == null) return;
        editText.addTextChangedListener(new TextWatcher() {
            private boolean isInternalChange = false;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isInternalChange) return;
                if (count == 1 && s != null && start < s.length() && s.charAt(start) == '-') {
                    isInternalChange = true;
                    showUnsequencedPackagesSelectorDialog(editText, positionName);
                    isInternalChange = false;
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void showUnsequencedPackagesSelectorDialog(TextInputEditText targetEdit, String positionName) {
        if (getContext() == null || currentStops == null) return;

        List<RouteStop> unsequencedStops = new ArrayList<>();
        for (RouteStop s : currentStops) {
            String seqStr = (s.allSequences != null && !s.allSequences.isEmpty()) ? s.allSequences : String.valueOf(s.sequence);
            if (seqStr.contains("-") || s.sequence == 0) {
                unsequencedStops.add(s);
            }
        }

        if (unsequencedStops.isEmpty()) {
            Toast.makeText(getContext(), "Nenhum pacote sem sequência (-) encontrado na rota.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_select_unsequenced_package, null);
        builder.setView(v);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView title = v.findViewById(R.id.textSelectUnseqTitle);
        RecyclerView recycler = v.findViewById(R.id.recyclerUnsequencedStops);
        MaterialButton btnClose = v.findViewById(R.id.btnCloseUnseqDialog);

        if (title != null) title.setText("📦 Pacotes Sem Sequência (-) em " + positionName);

        if (recycler != null) {
            recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
            recycler.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                @NonNull @Override
                public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                    View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dialog_stop_info, parent, false);
                    return new RecyclerView.ViewHolder(itemView) {};
                }

                @Override
                public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                    RouteStop s = unsequencedStops.get(position);
                    TextView tNum = holder.itemView.findViewById(R.id.textDialogStopNumber);
                    TextView tAddr = holder.itemView.findViewById(R.id.textDialogStopAddress);
                    TextView tSeq = holder.itemView.findViewById(R.id.textDialogStopSeq);
                    TextView btnLoc = holder.itemView.findViewById(R.id.btnDialogVehicleLocation);

                    if (tNum != null) tNum.setText(String.valueOf(s.stopNumber));
                    if (tAddr != null) tAddr.setText(s.address);
                    if (tSeq != null) {
                        String neighborhood = (s.neighborhood != null && !s.neighborhood.isEmpty()) ? " - " + s.neighborhood : "";
                        tSeq.setText("Endereço: " + s.address + neighborhood);
                    }

                    if (btnLoc != null) {
                        btnLoc.setText("Selecionar ➕");
                    }

                    holder.itemView.setOnClickListener(vClick -> {
                        if (targetEdit != null) {
                            String currentText = targetEdit.getText() != null ? targetEdit.getText().toString().trim() : "";
                            if (currentText.endsWith("-")) {
                                currentText = currentText.substring(0, currentText.length() - 1).trim();
                            }
                            if (currentText.endsWith(",")) {
                                currentText = currentText.substring(0, currentText.length() - 1).trim();
                            }

                            String stopNumStr = String.valueOf(s.stopNumber);
                            Set<String> parsedSet = parseNumberSet(currentText);
                            if (!parsedSet.contains(stopNumStr)) {
                                if (!currentText.isEmpty()) {
                                    currentText += ", " + stopNumStr;
                                } else {
                                    currentText = stopNumStr;
                                }
                                targetEdit.setText(currentText);
                                targetEdit.setSelection(targetEdit.getText().length());
                            }
                        }
                        dialog.dismiss();
                        Toast.makeText(getContext(), "Parada #" + s.stopNumber + " selecionada!", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public int getItemCount() {
                    return unsequencedStops.size();
                }
            });
        }

        if (btnClose != null) btnClose.setOnClickListener(vClose -> dialog.dismiss());
        dialog.show();
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
            if (routeHeader != null && routeHeader.startTime > 0 && fragment != null) {
                long now = System.currentTimeMillis();
                long currentPausedMs = routeHeader.totalPausedMs +
                    (routeHeader.lastPauseStartTime > 0 ? (now - routeHeader.lastPauseStartTime) : 0);
                long elapsed = (routeHeader.endTime > 0 ? routeHeader.endTime : now) - routeHeader.startTime - currentPausedMs;
                if (elapsed < 0) elapsed = 0;

                long s = elapsed / 1000;
                long m = s / 60;
                long h_val = m / 60;
                String time = String.format(Locale.getDefault(), "%02d:%02d:%02d", h_val, m % 60, s % 60);
                if (routeHeader.lastPauseStartTime > 0 && routeHeader.endTime == 0) {
                    time += " ⏸️";
                }

                if (h.textRouteTotalTime != null) {
                    h.textRouteTotalTime.setText(time);
                }

                boolean timerOnCardsOnly = fragment.sharedPreferences.getBoolean("timer_on_cards_only", false);
                boolean showCard = !timerOnCardsOnly || routeHeader.endTime > 0;

                if (h.cardRouteTotalTime != null) {
                    h.cardRouteTotalTime.setVisibility(showCard ? View.VISIBLE : View.GONE);
                }

                if (h.imageHomeWarning != null) {
                    boolean homeDefined = fragment.sharedPreferences != null && fragment.sharedPreferences.getFloat("home_lat", 0) != 0;
                    h.imageHomeWarning.setVisibility(homeDefined ? View.GONE : View.VISIBLE);
                }
            } else if (routeHeader != null && fragment != null) {
                // Rota ainda não iniciada (startTime == 0)
                boolean timerOnCardsOnly = fragment.sharedPreferences.getBoolean("timer_on_cards_only", false);
                boolean isOptPending = fragment.sharedPreferences.getBoolean("route_optimization_pending_" + routeHeader.id, false);

                if (!timerOnCardsOnly && !isOptPending) {
                    if (h.textRouteTotalTime != null) h.textRouteTotalTime.setText("Iniciar Rota");
                    if (h.imageHomeWarning != null) h.imageHomeWarning.setVisibility(View.GONE);
                    if (h.cardRouteTotalTime != null) h.cardRouteTotalTime.setVisibility(View.VISIBLE);
                } else {
                    if (h.cardRouteTotalTime != null) h.cardRouteTotalTime.setVisibility(View.GONE);
                }
            } else {
                if (h.cardRouteTotalTime != null) h.cardRouteTotalTime.setVisibility(View.GONE);
            }
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder h, int position) { 
            int pos = h.getBindingAdapterPosition();
            RouteStop s = list.get(pos); 
            h.textNumber.setText(String.valueOf(pos + 1)); 
            updateTimer(h);

            int totalStopsCount = (list != null) ? list.size() : 0;
            int deliveredStopsCount = 0;
            if (list != null) {
                for (RouteStop rs : list) {
                    if (rs != null && rs.deliveryStatus == 1) {
                        deliveredStopsCount++;
                    }
                }
            }

            if (h.textRouteStopsProgress != null) {
                h.textRouteStopsProgress.setText(deliveredStopsCount + "/" + totalStopsCount);
            }

            boolean isOptPending = (fragment != null && fragment.sharedPreferences != null)
                    && fragment.sharedPreferences.getBoolean("route_optimization_pending_" + fragment.currentRouteId, false);

            if (h.cardRouteStopsProgress != null) {
                h.cardRouteStopsProgress.setVisibility((totalStopsCount > 0 && !isOptPending) ? View.VISIBLE : View.GONE);
                h.cardRouteStopsProgress.setOnClickListener(v -> {
                    if (fragment != null) fragment.showStatsPopup(1);
                });
            }

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
            String seqInfo = fragment.getFormattedSeqInfo(s);
            h.textDownloadedStatus.setVisibility(View.VISIBLE); // Reutilizando campo ou garantindo visibilidade
            
            // Note: I will use a dedicated TextView if available, otherwise appending to secondary info
            h.textPackageCount.setText(h.textPackageCount.getText() + " | " + s.buyerCount + " Comp. | " + seqInfo);

            if (h.btnCarLocation != null) {
                h.btnCarLocation.setOnClickListener(v -> fragment.showVehicleLocationPicker(s, v));
            }

            // --- Verificação de Correção Local e Global ---
            final Context cardCtx = h.itemView.getContext();
            new Thread(() -> {
                AppDao dao = AppDatabase.getInstance(cardCtx).appDao();
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
                    public void onResult(double lat, double lon, int likes, int dislikes, String creatorId, String note, int comments, String creatorName, long date, boolean hasCoordinateFix) {
                        if (h.getBindingAdapterPosition() == pos) {
                            Activity activity = fragment.getActivity();
                            if (activity != null) activity.runOnUiThread(() -> {
                                boolean hasVotesOrNotes = (likes > 0 || dislikes > 0 || (note != null && !note.isEmpty()));
                                if (hasVotesOrNotes) {
                                    h.layoutGlobalFeedback.setVisibility(View.VISIBLE);
                                    h.textGlobalStats.setText(likes + " 👍 | " + dislikes + " 👎");

                                    if (note != null && !note.isEmpty()) {
                                        h.textGlobalNote.setVisibility(View.VISIBLE);
                                        h.textGlobalNote.setText("Obs: " + note);
                                    } else {
                                        h.textGlobalNote.setVisibility(View.GONE);
                                    }
                                } else {
                                    h.layoutGlobalFeedback.setVisibility(View.GONE);
                                }
                                
                                // Se não tem correção local e EXISTE CORREÇÃO DE COORDENADA VÁLIDA, mostra disponível
                                if (localFix == null && hasCoordinateFix && lat != 0.0 && lon != 0.0 && Math.abs(lat) > 0.001) {
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
            if (h.cardRouteTotalTime != null) {
                h.cardRouteTotalTime.setOnClickListener(v -> fragment.showRouteTimerOptionsPopup(v));
            }

            if (h.btnHideStopsCard != null) {
                h.btnHideStopsCard.setOnClickListener(v -> {
                    if (fragment != null && fragment.sharedPreferences != null) {
                        fragment.sharedPreferences.edit().putBoolean("show_bottom_sheet_stops", false).apply();
                        fragment.updateFloatingButtonsVisibility();
                        fragment.updateFabsPosition();
                    }
                });
            }

            h.itemView.setOnLongClickListener(v -> {
                listener.onAction(s, 4); // 4 é o código para excluir
                return true;
            });
        }
        @Override public int getItemCount() { return list.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder { TextView textNumber, textStopTimer, textAddress, textRawAddress, textNeighborhood, textStatus, textPackageCount, textGlobalStats, textGlobalNote, textDownloadedStatus, textRouteTotalTime, textRouteStopsProgress; View btnSuccess, btnFailed, btnNavigate, btnFixLocation, btnReset, btnCarLocation, layoutAddress, layoutGlobalFeedback, imageHomeWarning, btnHideStopsCard; MaterialCardView card, cardRouteTotalTime, cardRouteStopsProgress; ViewHolder(View v) { super(v); card = v.findViewById(R.id.cardStop); cardRouteTotalTime = v.findViewById(R.id.cardRouteTotalTime); textRouteTotalTime = v.findViewById(R.id.textRouteTotalTime); cardRouteStopsProgress = v.findViewById(R.id.cardRouteStopsProgress); textRouteStopsProgress = v.findViewById(R.id.textRouteStopsProgress); imageHomeWarning = v.findViewById(R.id.imageHomeWarning); btnHideStopsCard = v.findViewById(R.id.btnHideStopsCard); textNumber = v.findViewById(R.id.textStopNumber); textStopTimer = v.findViewById(R.id.textStopTimer); textAddress = v.findViewById(R.id.textStopAddress); textRawAddress = v.findViewById(R.id.textRawAddress); textNeighborhood = v.findViewById(R.id.textStopNeighborhood); textStatus = v.findViewById(R.id.textStopStatus); textPackageCount = v.findViewById(R.id.textPackageCount); btnCarLocation = v.findViewById(R.id.btnCarLocation); textGlobalStats = v.findViewById(R.id.textGlobalStats); textGlobalNote = v.findViewById(R.id.textGlobalNote); textDownloadedStatus = v.findViewById(R.id.textDownloadedStatus); btnSuccess = v.findViewById(R.id.btnSuccess); btnFailed = v.findViewById(R.id.btnFailed); btnNavigate = v.findViewById(R.id.btnNavigate); btnFixLocation = v.findViewById(R.id.btnFixLocation); btnReset = v.findViewById(R.id.btnReset); layoutAddress = v.findViewById(R.id.layoutStopText); layoutGlobalFeedback = v.findViewById(R.id.layoutGlobalFeedback); } }
    }
    private static class StopsListAdapter extends RecyclerView.Adapter<StopsListAdapter.ViewHolder> {
        private final RouteFragment fragment;
        private List<RouteStop> fullList = new ArrayList<>();
        private List<RouteStop> filteredList = new ArrayList<>();
        private final OnItemClickListener click; 
        private final OnItemLongClickListener longClick; 
        private final Map<Integer, String> groupColors = new HashMap<>();
        private boolean isUnifyMode = false;
        private final Set<RouteStop> selectedStops = new HashSet<>();

        interface OnItemClickListener { void onItemClick(RouteStop s); } 
        interface OnItemLongClickListener { void onItemLongClick(RouteStop s); }
        
        StopsListAdapter(RouteFragment fragment, List<RouteStop> l, OnItemClickListener c, OnItemLongClickListener lc) { 
            this.fragment = fragment;
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
        
        Set<RouteStop> getSelectedStops() { return selectedStops; }

        void setStops(List<RouteStop> s) { 
            this.fullList = s; 
            this.filteredList = new ArrayList<>(s); 
            notifyDataSetChanged(); 
        }

        void swap(int from, int to) {
            if (from < filteredList.size() && to < filteredList.size()) {
                Collections.swap(filteredList, from, to);
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

            // --- Status Colorido na Lista ---
            String statusStr = "Pendente";
            int statusColor = Color.parseColor("#FF9800"); // Laranja
            int iconRes = android.R.drawable.ic_menu_agenda;

            if (s.deliveryStatus == 1) { // Entregue
                statusStr = "Entregue";
                statusColor = Color.parseColor("#388E3C"); // Verde
                iconRes = android.R.drawable.checkbox_on_background;
            } else if (s.deliveryStatus == 2) { // Falha
                statusStr = "Falha";
                statusColor = Color.parseColor("#D32F2F"); // Vermelho
                iconRes = android.R.drawable.ic_delete;
            }

            if (h.textStatus != null) {
                h.textStatus.setText(statusStr);
                h.textStatus.setTextColor(statusColor);
            }
            if (h.imageStatus != null) {
                h.imageStatus.setImageResource(iconRes);
                h.imageStatus.setColorFilter(statusColor);
            } 

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
            final Context listCtx = h.itemView.getContext();
            new Thread(() -> {
                AppDao dao = AppDatabase.getInstance(listCtx).appDao();
                CorrectedAddress localFix = dao.getCorrectedAddress(boundAddress);
                
                h.itemView.post(() -> {
                    // Se o ViewHolder já foi reciclado para outro endereço, ignora
                    if (!boundAddress.equals(h.textDownloadedStatus.getTag())) return;

                    if (localFix != null) {
                        h.textDownloadedStatus.setVisibility(View.VISIBLE);
                        String currentUserId = h.itemView.getContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE).getString("current_user_id", "anon");
                        
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
                        @Override public void onResult(double lat, double lon, int likes, int dislikes, String creatorId, String note, int comments, String creatorName, long date, boolean hasCoordinateFix) {
                            h.itemView.post(() -> {
                                if (boundAddress.equals(h.textDownloadedStatus.getTag())) {
                                    if (localFix == null && hasCoordinateFix && lat != 0.0 && lon != 0.0 && Math.abs(lat) > 0.001) {
                                        h.textDownloadedStatus.setVisibility(View.VISIBLE);
                                        h.textDownloadedStatus.setText("Correção disponível");
                                        h.textDownloadedStatus.setTextColor(Color.parseColor("#F44336")); // Vermelho
                                    }
                                    
                                    boolean hasVotes = (likes > 0 || dislikes > 0);
                                    if (hasVotes) {
                                        h.layoutGlobalFeedback.setVisibility(View.VISIBLE);
                                        h.textGlobalStats.setText(likes + " 👍 | " + dislikes + " 👎");
                                    } else {
                                        h.layoutGlobalFeedback.setVisibility(View.GONE);
                                    }
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

            if (h.btnListVehicleLocation != null) {
                if (s.vehicleLocation != null && !s.vehicleLocation.trim().isEmpty()) {
                    h.btnListVehicleLocation.setText("📍 " + s.vehicleLocation + " ▾");
                } else {
                    h.btnListVehicleLocation.setText("📍 Local ▾");
                }
                h.btnListVehicleLocation.setOnClickListener(v -> {
                    if (fragment != null) fragment.showVehicleLocationPicker(s, v);
                });
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
            TextView textNumber, textAddress, textRawAddress, textNeighborhood, textStatus, textDownloadedStatus, textGlobalStats, btnListVehicleLocation; 
            ImageView imageStatus; 
            CheckBox checkBox;
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
                btnListVehicleLocation = v.findViewById(R.id.btnListVehicleLocation);
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
        ShapeDrawable dot = new ShapeDrawable(new OvalShape());
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
        updateNavigationButtonStyle();
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
        if (textTimelineTime != null) textTimelineTime.setText(timeFormat.format(new Date(p.timestamp)));
        mapController.animateTo(gp);
        map.invalidate();
    }
}
