package com.example.drivelog;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.content.Intent;
import android.util.Log;

public class NotificationService extends NotificationListenerService {

    public static final String ACTION_NOTIFICATION_RECEIVED = "com.example.drivelog.NOTIFICATION_RECEIVED";
    private static final String ENVIOS_EXTRA_PACKAGE = "com.mercadolibre.android.extradriver"; // Pacote do Envios Extra

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        
        if (ENVIOS_EXTRA_PACKAGE.equals(packageName)) {
            String title = "";
            String text = "";
            
            if (sbn.getNotification().extras != null) {
                title = sbn.getNotification().extras.getString("android.title", "");
                text = sbn.getNotification().extras.getString("android.text", "");
            }

            Log.d("NotificationService", "Recebida do Envios Extra: " + title + " - " + text);

            Intent intent = new Intent(ACTION_NOTIFICATION_RECEIVED);
            intent.putExtra("title", title);
            intent.putExtra("text", text);
            intent.putExtra("time", System.currentTimeMillis());
            sendBroadcast(intent);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Opcional: tratar remoção
    }
}