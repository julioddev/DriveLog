package com.example.drivelog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class TrackingReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        boolean backgroundEnabled = prefs.getBoolean("background_tracking_enabled", true);
        
        String action = intent.getStringExtra("tracking_action");
        if (action != null) {
            // Se for para parar, paramos sempre
            if ("STOP".equals(action)) {
                startService(context, action);
            } else {
                // Se for para iniciar (START ou MONITOR), só iniciamos se o segundo plano estiver ativo
                if (backgroundEnabled) {
                    startService(context, action);
                } else {
                    android.util.Log.d("TrackingReceiver", "Início/monitoramento bloqueado porque o rastreamento em segundo plano está desativado.");
                }
            }
        }
        
        // Reschedule for tomorrow
        TrackingHelper.updateAutoTracking(context);
    }

    private void startService(Context context, String action) {
        Intent serviceIntent = new Intent(context, TrackingService.class);
        serviceIntent.setAction(action);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } catch (Exception e) {
            android.util.Log.e("TrackingReceiver", "Falha ao iniciar o serviço: " + e.getMessage());
        }
    }
}
