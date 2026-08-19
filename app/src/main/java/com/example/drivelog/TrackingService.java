package com.example.drivelog;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.os.PowerManager;

public class TrackingService extends Service {

    private static final String CHANNEL_ID = "tracking_channel";
    private static final int NOTIFICATION_ID = 123;
    
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private PowerManager.WakeLock wakeLock;
    
    // Agora armazenamos RoutePoint para manter o timestamp original de captura
    public static final MutableLiveData<List<RoutePoint>> pathPoints = new MutableLiveData<>(new ArrayList<>());
    public static final MutableLiveData<Boolean> isTracking = new MutableLiveData<>(false);
    public static final MutableLiveData<Boolean> isPaused = new MutableLiveData<>(false);
    public static final MutableLiveData<Double> currentDistance = new MutableLiveData<>(0.0);
    public static final MutableLiveData<Double> estimatedCost = new MutableLiveData<>(0.0);

    private static int currentSessionId = -1;
    private double lastConsumption = 10.0;
    private double lastFuelPrice = 5.50;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private Location stationaryCenter = null;
    private long stationaryStartTime = 0;
    private boolean isAutoPaused = false;
    
    // Novas variáveis para Auto-Stop em casa
    private long homeArrivalStartTime = 0;
    private boolean isInsideHomeRadius = false;
    private boolean isResting = false;
    
    // Auto-Earnings at Loading Points
    private List<LoadingPoint> activeLoadingPoints = new ArrayList<>();
    private LoadingPoint currentInsideLoadingPoint = null;
    private long loadingPointEntryTime = 0;
    private boolean hasRegisteredEarningForCurrentStay = false;
    
    private static final int AUTO_PAUSE_MINUTES = 10;
    private static final int AUTO_PAUSE_RADIUS = 40;

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        TrackingService getService() {
            return TrackingService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 🔥 REGRA DE OURO ANDROID 12+: startForeground DEVE ser chamado no primeiro milissegundo.
        // Usamos sempre o mesmo ID (123) para evitar RemoteServiceException.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, getNotification("DriveLog", "Iniciando rastreamento..."), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(NOTIFICATION_ID, getNotification("DriveLog", "Iniciando rastreamento..."));
        }

        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            
            if ("START".equals(action)) {
                acquireWakeLock();
                updateNotification("Rastreamento Ativo", "Gravando seu trajeto...");
                if (isRestIntervalNow()) {
                    isResting = true;
                    updateNotification("Modo Descanso", "Economizando bateria (Localização bloqueada)");
                    return START_NOT_STICKY;
                }
                startTracking();
            } else if ("MONITOR".equals(action)) {
                acquireWakeLock();
                updateNotification("Monitorando Saída", "Aguardando você sair de casa...");
                if (isRestIntervalNow()) {
                    // Já chamamos startForeground, então podemos parar com segurança.
                    stopForeground(true);
                    stopSelf();
                    return START_NOT_STICKY;
                }
                startHomeMonitoring();
            } else if ("PAUSE".equals(action)) {
                pauseTracking();
            } else if ("STOP".equals(action)) {
                stopTracking();
            }
        }
        return START_STICKY;
    }

    private void acquireWakeLock() {
        if (wakeLock == null || !wakeLock.isHeld()) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DriveLog:TrackingWakeLock");
                wakeLock.acquire(10 * 60 * 60 * 1000L); // 10 horas de limite por segurança
            }
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    private void startHomeMonitoring() {
        if (Boolean.TRUE.equals(isTracking.getValue())) return;

        android.content.SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        int mode = prefs.getInt("tracking_mode_v2", 0);
        
        // Só monitora se o modo for 2 (Distância/Localização)
        if (mode != 2 || !prefs.getBoolean("home_tracking_enabled", false)) {
            android.util.Log.d("TrackingService", "Monitoramento cancelado: Modo não é Localização (" + mode + ")");
            stopForeground(true);
            stopSelf();
            return;
        }

        float homeLat = prefs.getFloat("home_lat", 0);
        float homeLon = prefs.getFloat("home_lon", 0);
        if (homeLat == 0 || homeLon == 0) {
            stopForeground(true);
            stopSelf();
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 30000)
                .setMinUpdateIntervalMillis(15000)
                .build();

        if (locationCallback != null) fusedLocationClient.removeLocationUpdates(locationCallback);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location lastLoc = locationResult.getLastLocation();
                if (lastLoc == null) return;

                float[] results = new float[1];
                Location.distanceBetween(lastLoc.getLatitude(), lastLoc.getLongitude(), homeLat, homeLon, results);
                float distance = results[0];

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

                if (distance > triggerRadius) {
                    // Remove o monitoramento antes de iniciar o rastreio real
                    fusedLocationClient.removeLocationUpdates(this);
                    startTracking();
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    private void startTracking() {
        if (Boolean.TRUE.equals(isTracking.getValue()) && Boolean.FALSE.equals(isPaused.getValue())) {
            if (currentSessionId == -1) {
                executor.execute(() -> {
                    DailyKm last = AppDatabase.getInstance(this).appDao().getLastPendingAutomaticKm();
                    if (last != null) currentSessionId = last.id;
                });
            }
            return;
        }

        // Garante que qualquer callback anterior (como o de monitoramento) seja removido
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }
        
        isTracking.postValue(true);
        isPaused.postValue(false);
        isAutoPaused = false;
        stationaryCenter = null;
        stationaryStartTime = 0;

        // Limpa KM e pontos da sessão anterior para iniciar zerado
        currentDistance.postValue(0.0);
        pathPoints.postValue(new ArrayList<>());
        estimatedCost.postValue(0.0);
        currentSessionId = -1;
        
        // Carrega pontos de carregamento ativos
        executor.execute(() -> {
            activeLoadingPoints = AppDatabase.getInstance(this).appDao().getAllLoadingPoints();
        });

        // Carrega último consumo e cria sessão no banco ANTES de começar a processar locais
        executor.execute(() -> {
            AppDao dao = AppDatabase.getInstance(this).appDao();
            Fuel lastFuel = dao.getLastCompletedFuel();
            if (lastFuel != null && lastFuel.liters > 0 && lastFuel.kmDriven > 0) {
                lastConsumption = lastFuel.kmDriven / lastFuel.liters;
                lastFuelPrice = lastFuel.pricePerLiter;
            } else {
                android.content.SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
                lastConsumption = prefs.getFloat("default_consumption", 10.0f);
                lastFuelPrice = prefs.getFloat("default_fuel_price", 5.50f);
            }

            // Cria sessão automática se for um novo início ou se não tiver ID
            if (currentSessionId == -1) {
                DailyKm newSession = new DailyKm();
                newSession.date = System.currentTimeMillis();
                newSession.isAutomatic = true;
                newSession.isCompleted = false;
                newSession.consumptionUsed = lastConsumption;
                currentSessionId = (int) dao.insertDailyKm(newSession);
            }
        });

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .setWaitForAccurateLocation(false)
                .build();

        if (locationCallback == null) {
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    if (checkRestInterval()) return;

                    Location lastLoc = locationResult.getLastLocation();
                    if (lastLoc == null) return;

                    // Lógica de Auto-Pause e Auto-Resume
                    if (stationaryCenter == null) {
                        stationaryCenter = lastLoc;
                        stationaryStartTime = System.currentTimeMillis();
                    }

                    float distFromCenter = lastLoc.distanceTo(stationaryCenter);

                    if (distFromCenter > AUTO_PAUSE_RADIUS) {
                        if (isAutoPaused) {
                            isPaused.postValue(false);
                            isAutoPaused = false;
                            updateNotification("Rastreamento Retomado", "Você voltou a se mover");
                        }
                        stationaryCenter = lastLoc;
                        stationaryStartTime = System.currentTimeMillis();
                    } else {
                        if (Boolean.FALSE.equals(isPaused.getValue())) {
                            long stationaryDuration = System.currentTimeMillis() - stationaryStartTime;
                            if (stationaryDuration > (AUTO_PAUSE_MINUTES * 60000)) { 
                                isPaused.postValue(true);
                                isAutoPaused = true;
                                updateNotification("Rastreamento Auto-Pausado", "Você está parado há mais de 10 min");
                            }
                        }
                    }

                    if (Boolean.TRUE.equals(isPaused.getValue())) return;
                    
                    List<RoutePoint> currentPoints = pathPoints.getValue();
                    if (currentPoints == null) currentPoints = new ArrayList<>();
                    
                    Double totalDist = currentDistance.getValue();
                    if (totalDist == null) totalDist = 0.0;

                    for (Location location : locationResult.getLocations()) {
                        long now = System.currentTimeMillis();
                        
                        // 🔥 MODO COMBOIO: Envia localização para amigos se estiver ativo
                        sendLiveLocationToFirebase(location);

                        final int sessionIdSnapshot = currentSessionId;
                        
                        if (sessionIdSnapshot == -1) continue; // Pula se a sessão ainda não foi criada no banco

                        RoutePoint newRp = new RoutePoint(sessionIdSnapshot, location.getLatitude(), location.getLongitude(), now);
                        
                        if (!currentPoints.isEmpty()) {
                            RoutePoint lastRp = currentPoints.get(currentPoints.size() - 1);
                            float[] results = new float[1];
                            Location.distanceBetween(lastRp.latitude, lastRp.longitude, newRp.latitude, newRp.longitude, results);
                            double distMeters = results[0];
                            
                            if (distMeters > 2) { 
                                totalDist += distMeters / 1000.0;
                            }
                        }
                        currentPoints.add(newRp);
                        
                        final double currentTotalDist = totalDist;
                        executor.execute(() -> {
                            AppDao dao = AppDatabase.getInstance(getApplicationContext()).appDao();
                            dao.insertRoutePoint(newRp);
                            
                            // Atualiza distância no banco periodicamente ou a cada ponto
                            DailyKm session = dao.getDailyKmById(sessionIdSnapshot);
                            if (session != null) {
                                session.gpsDistance = currentTotalDist;
                                session.estimatedFuelCost = (session.gpsDistance / lastConsumption) * lastFuelPrice;
                                dao.updateDailyKm(session);
                            }
                        });
                    }
                    pathPoints.postValue(new ArrayList<>(currentPoints));
                    currentDistance.postValue(totalDist);
                    estimatedCost.postValue((totalDist / lastConsumption) * lastFuelPrice);

                    // Atualiza a notificação em tempo real
                    String stats = String.format(java.util.Locale.getDefault(), 
                            "Distância: %.2f KM | Combustível: R$ %.2f", 
                            totalDist, (totalDist / lastConsumption) * lastFuelPrice);
                    updateNotification("Rastreamento Ativo", stats);

                    // Lógica de Auto-Stop ao chegar em casa
                    // 🔥 Só executa se NÃO estiver no modo Tempo (mode 1)
                    android.content.SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
                    int currentMode = prefs.getInt("tracking_mode_v2", 0);
                    if (currentMode != 1) {
                        checkAutoStopAtHome(lastLoc);
                    }
                    
                    // Lógica de Registro Automático de Ganhos
                    checkAutoEarningsAtLoadingPoint(lastLoc);
                }
            };
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    private boolean isRestIntervalNow() {
        android.content.SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        if (!prefs.getBoolean("rest_interval_enabled", false)) return false;

        String start = prefs.getString("rest_start_time", "12:00");
        String end = prefs.getString("rest_end_time", "13:00");
        return isCurrentTimeInInterval(start, end);
    }

    private boolean checkRestInterval() {
        if (isRestIntervalNow()) {
            if (!isResting) {
                isResting = true;
                updateNotification("Modo Descanso", "Economizando bateria (Localização bloqueada)");
                
                // 🔥 REMOÇÃO TOTAL DA LOCALIZAÇÃO
                if (locationCallback != null) {
                    fusedLocationClient.removeLocationUpdates(locationCallback);
                }
                
                // Se estiver apenas monitorando (sem estar no modo Tracking real), encerra o serviço
                if (!Boolean.TRUE.equals(isTracking.getValue())) {
                    stopForeground(true);
                    stopSelf();
                }
            }
            return true;
        } else {
            if (isResting) {
                isResting = false;
                updateNotification("Rastreamento Ativo", "Gravando seu trajeto...");
                // Reinicia as atualizações de localização ao sair do horário de descanso
                if (Boolean.TRUE.equals(isTracking.getValue()) && Boolean.FALSE.equals(isPaused.getValue())) {
                    // Limpa callback anterior para evitar duplicidade
                    if (locationCallback != null) {
                        fusedLocationClient.removeLocationUpdates(locationCallback);
                    }
                    startTracking();
                }
            }
            return false;
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

            if (startTotal < endTotal) {
                return nowTotal >= startTotal && nowTotal < endTotal;
            } else {
                // Intervalo cruza meia-noite
                return nowTotal >= startTotal || nowTotal < endTotal;
            }
        } catch (Exception e) { return false; }
    }

    private void checkAutoStopAtHome(Location currentLoc) {
        android.content.SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        float homeLat = prefs.getFloat("home_lat", 0);
        float homeLon = prefs.getFloat("home_lon", 0);
        
        if (homeLat == 0 || homeLon == 0) return;

        float[] results = new float[1];
        Location.distanceBetween(currentLoc.getLatitude(), currentLoc.getLongitude(), homeLat, homeLon, results);
        float distanceToHome = results[0];

        int arrivalRadius = 50;
        try {
            arrivalRadius = prefs.getInt("home_arrival_radius", 50);
        } catch (ClassCastException e) {
            Object val = prefs.getAll().get("home_arrival_radius");
            if (val != null) {
                try { arrivalRadius = Integer.parseInt(String.valueOf(val)); } catch (Exception ignored) {}
            }
            prefs.edit().putInt("home_arrival_radius", arrivalRadius).apply();
        }

        if (distanceToHome <= arrivalRadius) {
            // 🔥 Para e salva IMEDIATAMENTE ao entrar no raio de casa
            stopTracking();
            
            // Se estiver no modo de localização, volta a monitorar a saída após o stop
            android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
            handler.postDelayed(() -> {
                TrackingHelper.updateAutoTracking(getApplicationContext());
            }, 2000);
            
            isInsideHomeRadius = false;
            homeArrivalStartTime = 0;
        } else {
            isInsideHomeRadius = false;
            homeArrivalStartTime = 0;
        }
    }

    private void checkAutoEarningsAtLoadingPoint(Location currentLoc) {
        if (activeLoadingPoints == null || activeLoadingPoints.isEmpty()) return;

        android.content.SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);

        LoadingPoint nearest = null;
        float minDistance = Float.MAX_VALUE;

        for (LoadingPoint lp : activeLoadingPoints) {
            float[] results = new float[1];
            Location.distanceBetween(currentLoc.getLatitude(), currentLoc.getLongitude(), lp.latitude, lp.longitude, results);
            
            int loadingRadius = 100;
            try {
                loadingRadius = prefs.getInt("loading_base_radius", 100);
            } catch (Exception e) {
                Object val = prefs.getAll().get("loading_base_radius");
                if (val != null) {
                    try { loadingRadius = Integer.parseInt(String.valueOf(val)); } catch (Exception ignored) {}
                }
            }

            if (results[0] <= loadingRadius && results[0] < minDistance) {
                minDistance = results[0];
                nearest = lp;
            }
        }

        if (nearest != null) {
            if (currentInsideLoadingPoint == null || currentInsideLoadingPoint.id != nearest.id) {
                currentInsideLoadingPoint = nearest;
                loadingPointEntryTime = System.currentTimeMillis();
                hasRegisteredEarningForCurrentStay = false;
            } else if (!hasRegisteredEarningForCurrentStay) {
                long duration = System.currentTimeMillis() - loadingPointEntryTime;
                
                int loadingTimeMin = 5;
                try {
                    loadingTimeMin = prefs.getInt("loading_base_time", 5);
                } catch (Exception e) {
                    Object val = prefs.getAll().get("loading_base_time");
                    if (val != null) {
                        try { loadingTimeMin = Integer.parseInt(String.valueOf(val)); } catch (Exception ignored) {}
                    }
                }

                if (duration >= (long) loadingTimeMin * 60000) { // minutos configuráveis
                    registerAutoEarning(nearest);
                    hasRegisteredEarningForCurrentStay = true;
                }
            }
        } else {
            currentInsideLoadingPoint = null;
            loadingPointEntryTime = 0;
            hasRegisteredEarningForCurrentStay = false;
        }
    }

    private void registerAutoEarning(LoadingPoint lp) {
        executor.execute(() -> {
            AppDao dao = AppDatabase.getInstance(this).appDao();
            
            // Verifica se já existe um ganho para esta plataforma HOJE
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0); cal.set(java.util.Calendar.MINUTE, 0); cal.set(java.util.Calendar.SECOND, 0); cal.set(java.util.Calendar.MILLISECOND, 0);
            long startOfDay = cal.getTimeInMillis();
            cal.set(java.util.Calendar.HOUR_OF_DAY, 23); cal.set(java.util.Calendar.MINUTE, 59); cal.set(java.util.Calendar.SECOND, 59);
            long endOfDay = cal.getTimeInMillis();

            Earnings existing = dao.getEarningForPlatformToday(lp.platformName, startOfDay, endOfDay);
            if (existing != null) {
                android.util.Log.d("TrackingService", "Auto-ganho ignorado: Plataforma " + lp.platformName + " já registrada hoje.");
                return;
            }

            // Busca a plataforma para pegar o valor padrão
            List<Platform> platforms = dao.getAllPlatforms();
            Platform targetPlatform = null;
            for (Platform p : platforms) {
                if (p.name.equalsIgnoreCase(lp.platformName)) {
                    targetPlatform = p;
                    break;
                }
            }

            double baseValue = 0.0;
            if (targetPlatform != null) {
                baseValue = targetPlatform.defaultValue;
            }

            Earnings earnings = new Earnings();
            earnings.date = System.currentTimeMillis();
            earnings.platforms = lp.platformName;
            earnings.baseValue = baseValue;
            earnings.extraValue = 0.0;
            earnings.totalValue = baseValue;
            
            dao.insertEarnings(earnings);
            
            // Notifica o usuário
            String msg = String.format(java.util.Locale.getDefault(), 
                "Ganhos de R$ %.2f registrados para %s em %s", 
                baseValue, lp.platformName, lp.name);
            updateNotification("Ganhos Automáticos", msg);
        });
    }

    private void updateNotification(String title, String text) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, getNotification(title, text));
        }
    }

    private void performDevAutoShare(DailyKm km) {
        // Usa um novo Executor específico para não travar no shutdown do executor principal
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            AppDao dao = AppDatabase.getInstance(this).appDao();
            List<RoutePoint> points = dao.getRoutePointsForKm(km.id);
            com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            
            if (user != null && !points.isEmpty()) {
                FirebaseHelper.shareRecordingWithDevelopers(user.getEmail(), user.getDisplayName(), km, points, new FirebaseHelper.GlobalUploadCallback() {
                    @Override public void onSuccess() {
                        android.util.Log.d("TrackingService", "Dev Auto-Compartilhamento Sucesso");
                    }
                    @Override public void onFailure(String msg) {
                        android.util.Log.e("TrackingService", "Dev Auto-Compartilhamento Falhou: " + msg);
                    }
                });
            }
        });
    }

    private void pauseTracking() {
        isPaused.postValue(true);
        isAutoPaused = false; // Se pausou manualmente, desativa flag de auto-pause para não confundir
        updateNotification("Rastreamento Pausado", "Toque em Play para continuar");
    }

    private long lastLiveUpdate = 0;
    private void sendLiveLocationToFirebase(Location loc) {
        long now = System.currentTimeMillis();
        // Atualiza a cada 10 segundos para economizar dados e bateria
        if (now - lastLiveUpdate < 10000) return;
        lastLiveUpdate = now;

        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        // 🔥 BLOQUEIO TOTAL: Modo Comboio apenas para DEVs
        FirebaseHelper.checkDeveloperAccess(user.getEmail(), isDev -> {
            if (isDev) {
                FirebaseHelper.updateLiveLocation(user.getEmail(), loc.getLatitude(), loc.getLongitude());
            }
        });
    }

    private void stopTracking() {
        // 🔥 MODO COMBOIO: Avisa que ficou Offline
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            FirebaseHelper.checkDeveloperAccess(user.getEmail(), isDev -> {
                if (isDev) {
                    FirebaseHelper.goOffline(user.getEmail());
                }
            });
        }

        executor.execute(() -> {
            AppDao dao = AppDatabase.getInstance(this).appDao();
            
            // Tenta usar o ID atual ou recuperar o último pendente automático
            int sessionIdToStop = currentSessionId;
            if (sessionIdToStop == -1) {
                DailyKm last = dao.getLastPendingAutomaticKm();
                if (last != null) sessionIdToStop = last.id;
            }

            if (sessionIdToStop != -1) {
                DailyKm session = dao.getDailyKmById(sessionIdToStop);
                if (session != null) {
                    session.isCompleted = true;
                    // Garante que o custo final seja salvo baseado na distância rastreada
                    session.estimatedFuelCost = (session.gpsDistance / session.consumptionUsed) * lastFuelPrice;
                    dao.updateDailyKm(session);
                    
                    // 🔥 MODO DEV: Compartilhamento Automático
                    android.content.SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
                    if (prefs.getBoolean("dev_auto_share_recordings", false)) {
                        performDevAutoShare(session);
                    }

                    // Trigger auto cloud sync if enabled
                    CloudSyncHelper.syncNow(getApplicationContext());
                }
            }
            
            // Limpa estado estático após salvar no banco
            currentSessionId = -1;
        });
        
        isTracking.postValue(false);
        isPaused.postValue(false);

        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }
        releaseWakeLock();
        stopForeground(true);
        stopSelf();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Serviço de Rastreamento", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification getNotification(String title, String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_map)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        android.content.SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean backgroundEnabled = prefs.getBoolean("background_tracking_enabled", true);
        
        if (!backgroundEnabled) {
            android.util.Log.d("TrackingService", "Tarefa removida e rastreamento em segundo plano desativado. Parando serviço.");
            stopTracking();
        }
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }
        executor.shutdown();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
