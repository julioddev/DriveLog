package com.example.drivelog;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ScannerAlertManager {
    public static class AlertModel {
        public String title, message;
        public long timestamp;
        public AlertModel(String t, String m, long ts) {
            this.title = t; this.message = m; this.timestamp = ts;
        }
    }

    private static final String PREF_NAME = "ScannerAlerts";
    private static final String KEY_ALERTS = "alert_list";
    private static final int MAX_ALERTS = 50;

    public static synchronized void addAlert(Context context, String title, String message, long timestamp) {
        List<AlertModel> alerts = getAlerts(context);
        
        // Evita duplicados idênticos em sequência (2 segundos)
        if (!alerts.isEmpty()) {
            AlertModel last = alerts.get(0);
            if (last.title.equals(title) && last.message.equals(message) && (timestamp - last.timestamp < 2000)) {
                return;
            }
        }
        
        alerts.add(0, new AlertModel(title, message, timestamp));
        if (alerts.size() > MAX_ALERTS) {
            alerts = alerts.subList(0, MAX_ALERTS);
        }
        
        saveAlerts(context, alerts);
    }

    public static synchronized List<AlertModel> getAlerts(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_ALERTS, null);
        if (json == null) return new ArrayList<>();
        
        try {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<AlertModel>>() {}.getType();
            return gson.fromJson(json, type);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static void saveAlerts(Context context, List<AlertModel> alerts) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = gson.toJson(alerts);
        prefs.edit().putString(KEY_ALERTS, json).apply();
    }
}
