package com.example.drivelog;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.app.NotificationCompat;

public class FloatingIconService extends Service {

    private WindowManager windowManager;
    private View floatingView;
    private View dismissView;
    private View btnPausePlay;
    private ImageView imgPausePlay;
    private WindowManager.LayoutParams params;
    private WindowManager.LayoutParams dismissParams;
    private PowerManager.WakeLock wakeLock;
    private static final String CHANNEL_ID = "FloatingIconChannel";
    private boolean isScannerPaused = false;
    private boolean isDeveloper = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 🔥 Chamada redundante apenas por segurança, mas o onCreate já deve ter resolvido
        ensureForeground();
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 🔥 CORREÇÃO DEFINITIVA: startForeground DEVE ser a primeira coisa no onCreate
        // para evitar que o sistema mate o processo por timeout
        ensureForeground();
        
        try {
            ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(this, R.style.Theme_Entregas);
            floatingView = LayoutInflater.from(contextThemeWrapper).inflate(R.layout.layout_floating_bubble, null);

            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? 
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : 
                        WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | 
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                    PixelFormat.TRANSLUCENT);

            params.gravity = Gravity.TOP | Gravity.START;
            
            // 🔥 Carrega a última posição salva
            android.content.SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
            params.x = prefs.getInt("floating_icon_x", 100);
            params.y = prefs.getInt("floating_icon_y", 200);

            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            windowManager.addView(floatingView, params);

            btnPausePlay = floatingView.findViewById(R.id.btn_pause_play);
            imgPausePlay = floatingView.findViewById(R.id.img_pause_play);
            
            checkDeveloperStatus();
            setupPausePlayLogic();

            createDismissView(contextThemeWrapper);
            setupTouchListener();
            acquireWakeLock();
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao criar ícone: " + e.getMessage(), Toast.LENGTH_LONG).show();
            stopSelf();
        }
    }

    private void createDismissView(Context themedContext) {
        dismissView = LayoutInflater.from(themedContext).inflate(R.layout.layout_floating_dismiss, null);
        dismissParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        dismissView.setVisibility(View.GONE);
        windowManager.addView(dismissView, dismissParams);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Ícone Flutuante",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Atalho de Retorno")
                .setContentText("Toque no ícone para voltar ao DriveLog")
                .setSmallIcon(R.drawable.ic_map)
                .build();
    }

    private void checkDeveloperStatus() {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            FirebaseHelper.checkDeveloperAccess(user.getEmail(), isDev -> {
                this.isDeveloper = isDev;
            });
        }
    }

    private void setupPausePlayLogic() {
        if (btnPausePlay == null) return;
        
        isScannerPaused = getSharedPreferences("AppConfig", MODE_PRIVATE)
                .getBoolean("scanner_paused_by_user", false);
        updatePausePlayUI();

        btnPausePlay.setOnClickListener(v -> {
            isScannerPaused = !isScannerPaused;
            getSharedPreferences("AppConfig", MODE_PRIVATE).edit()
                    .putBoolean("scanner_paused_by_user", isScannerPaused)
                    .apply();
            
            updatePausePlayUI();
            
            // 🔥 Faz o botão sumir após o clique
            btnPausePlay.setVisibility(View.GONE);
            
            String msg = isScannerPaused ? "Scanner PAUSADO" : "Scanner ATIVO";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            
            // Avisa o ScannerService sobre a mudança
            Intent intent = new Intent("com.example.drivelog.SCANNER_PAUSE_TOGGLE");
            intent.putExtra("paused", isScannerPaused);
            sendBroadcast(intent);
        });
    }

    private void updatePausePlayUI() {
        if (imgPausePlay != null) {
            // Usando recursos padrão garantidos do Android
            imgPausePlay.setImageResource(isScannerPaused ? 
                    android.R.drawable.ic_media_play : 
                    android.R.drawable.ic_media_pause);
        }
    }

    private void setupTouchListener() {
        floatingView.findViewById(R.id.root_container).setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private long touchStartTime;
            private boolean isLongPressTriggered = false;
            private final android.os.Handler handler = new android.os.Handler();
            private final Runnable longPressRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isDeveloper && btnPausePlay != null) {
                        isLongPressTriggered = true;
                        btnPausePlay.setVisibility(btnPausePlay.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                    }
                }
            };

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        touchStartTime = System.currentTimeMillis();
                        isLongPressTriggered = false;
                        handler.postDelayed(longPressRunnable, 600); // 600ms para long-press
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        dismissView.setVisibility(View.VISIBLE);
                        
                        // Se moveu mais que um pouco, cancela a possibilidade de ser long-press ou clique
                        if (Math.abs(event.getRawX() - initialTouchX) > 15 || Math.abs(event.getRawY() - initialTouchY) > 15) {
                            handler.removeCallbacks(longPressRunnable);
                            if (!isLongPressTriggered && btnPausePlay != null) {
                                btnPausePlay.setVisibility(View.GONE);
                            }
                        }

                        // 🔥 Correção: O cálculo agora usa a posição absoluta (Raw) sincronizada
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        
                        // 🔥 Previne que o ícone fique preso "fora" da tela
                        if (params.x < 0) params.x = 0;
                        if (params.y < 0) params.y = 0;
                        
                        windowManager.updateViewLayout(floatingView, params);
                        
                        // Feedback visual de escala enquanto arrasta
                        floatingView.setScaleX(1.1f);
                        floatingView.setScaleY(1.1f);
                        
                        updateDismissFeedback(event.getRawX(), event.getRawY());
                        return true;

                    case MotionEvent.ACTION_UP:
                        floatingView.setScaleX(1.0f);
                        floatingView.setScaleY(1.0f);
                        handler.removeCallbacks(longPressRunnable);
                        dismissView.setVisibility(View.GONE);
                        
                        if (isLongPressTriggered) return true;

                        long duration = System.currentTimeMillis() - touchStartTime;
                        float diffX = Math.abs(event.getRawX() - initialTouchX);
                        float diffY = Math.abs(event.getRawY() - initialTouchY);
                        
                        // Verifica se soltou sobre a área de fechar
                        if (isOverDismissArea(event.getRawX(), event.getRawY())) {
                            stopSelf();
                            return true;
                        }

                        // 🔥 Efeito de "Snap" (Ímã): Joga o ícone para a borda lateral mais próxima
                        snapToEdge();

                        // Se foi um clique rápido e sem muito movimento
                        if (duration < 200 && diffX < 10 && diffY < 10) {
                            Intent intent = new Intent(FloatingIconService.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(intent);
                            stopSelf(); // Remove o ícone ao voltar
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void snapToEdge() {
        if (floatingView == null || windowManager == null) return;
        
        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        int screenWidth = metrics.widthPixels;
        int viewWidth = floatingView.getWidth();
        
        int finalX;
        if (params.x + (viewWidth / 2) < screenWidth / 2) {
            finalX = 0; // Cola na esquerda
        } else {
            finalX = screenWidth - viewWidth; // Cola na direita
        }
        
        // Animação suave de "deslize" para a borda
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofInt(params.x, finalX);
        animator.setDuration(250);
        animator.addUpdateListener(animation -> {
            params.x = (int) animation.getAnimatedValue();
            if (floatingView != null && floatingView.getParent() != null) {
                windowManager.updateViewLayout(floatingView, params);
            }
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                getSharedPreferences("AppConfig", MODE_PRIVATE).edit()
                        .putInt("floating_icon_x", params.x)
                        .putInt("floating_icon_y", params.y)
                        .apply();
            }
        });
        animator.start();
    }

    private boolean isOverDismissArea(float x, float y) {
        if (dismissView == null) return false;
        View circle = dismissView.findViewById(R.id.dismiss_circle);
        int[] location = new int[2];
        circle.getLocationOnScreen(location);
        int centerX = location[0] + circle.getWidth() / 2;
        int centerY = location[1] + circle.getHeight() / 2;
        
        double distance = Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2));
        return distance < (circle.getWidth() * 1.2); // Raio de captura um pouco maior
    }

    private void updateDismissFeedback(float x, float y) {
        View circle = dismissView.findViewById(R.id.dismiss_circle);
        if (isOverDismissArea(x, y)) {
            circle.setScaleX(1.2f);
            circle.setScaleY(1.2f);
            circle.setAlpha(1.0f);
        } else {
            circle.setScaleX(1.0f);
            circle.setScaleY(1.0f);
            circle.setAlpha(0.6f);
        }
    }

    private void ensureForeground() {
        createNotificationChannel();
        Notification notification = createNotification();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(999, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(999, notification);
        }
    }

    private void acquireWakeLock() {
        android.content.SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean wakeLockEnabled = prefs.getBoolean("scanner_wakelock_enabled", true);

        if (wakeLockEnabled) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            if (powerManager != null && wakeLock == null) {
                // 🔥 Usando FULL_WAKE_LOCK para garantir tela e processador ativos
                wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | 
                        PowerManager.ACQUIRE_CAUSES_WAKEUP | 
                        PowerManager.ON_AFTER_RELEASE, "DriveLog:ScannerWakeLock");
                wakeLock.acquire();
            }
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseWakeLock();
        if (floatingView != null && windowManager != null) windowManager.removeView(floatingView);
        if (dismissView != null && windowManager != null) windowManager.removeView(dismissView);
    }
}
