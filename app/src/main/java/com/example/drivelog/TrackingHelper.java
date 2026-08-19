package com.example.drivelog;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import java.util.Calendar;

public class TrackingHelper {

    public static void updateAutoTracking(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        
        // Bloqueia rastreamento automático se estiver no Modo Mapa (isMapsOnly = 1)
        boolean isMapsOnly = prefs.getInt("app_mode", 0) == 1;
        if (isMapsOnly) {
            cancelAlarm(context, 101);
            cancelAlarm(context, 102);
            // Se não estiver rastreando manualmente, garante que o serviço pare
            if (!Boolean.TRUE.equals(TrackingService.isTracking.getValue())) {
                Intent intent = new Intent(context, TrackingService.class);
                context.stopService(intent);
            }
            return;
        }

        // Recupera o modo: 0: Manual, 1: Por Tempo, 2: Por Saída de Casa
        int mode = prefs.getInt("tracking_mode_v2", 0);
        boolean mapsEnabled = prefs.getBoolean("maps_enabled", true);

        if (mode == 1 && mapsEnabled) {
            // MODO 1: Rastreamento por Agendamento de Horário
            String startTimeStr = prefs.getString("tracking_start", "08:00");
            String endTimeStr = prefs.getString("tracking_end", "18:00");
            
            scheduleAlarm(context, "START", startTimeStr, 101);
            scheduleAlarm(context, "STOP", endTimeStr, 102);

            // Inicia imediatamente se estiver dentro do horário
            if (isTimeInRange(startTimeStr, endTimeStr) && !Boolean.TRUE.equals(TrackingService.isTracking.getValue())) {
                Intent intent = new Intent(context.getApplicationContext(), TrackingService.class);
                intent.setAction("START");
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.getApplicationContext().startForegroundService(intent);
                    } else {
                        context.getApplicationContext().startService(intent);
                    }
                } catch (Exception e) {
                    Log.e("TrackingHelper", "Erro ao iniciar serviço no alcance", e);
                }
            }
        } else if (mode == 2 && mapsEnabled) {
            // MODO 2: Rastreamento por Localização (Ao sair de casa)
            cancelAlarm(context, 101);
            cancelAlarm(context, 102);

            // Inicia monitoramento de casa se não estiver rastreando já
            if (!Boolean.TRUE.equals(TrackingService.isTracking.getValue())) {
                Intent intent = new Intent(context.getApplicationContext(), TrackingService.class);
                intent.setAction("MONITOR");
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.getApplicationContext().startForegroundService(intent);
                    } else {
                        context.getApplicationContext().startService(intent);
                    }
                } catch (Exception e) {
                    Log.e("TrackingHelper", "Erro ao iniciar serviço para monitoramento", e);
                }
            }
        } else {
            // MODO 0: Manual ou Mapas Desativados
            cancelAlarm(context, 101);
            cancelAlarm(context, 102);
            
            // Se mudou para manual, paramos o monitoramento de saída (MONITOR)
            // mas MANTEMOS o rastreio ativo se o usuário deu Play manualmente.
            if (!Boolean.TRUE.equals(TrackingService.isTracking.getValue())) {
                Intent intent = new Intent(context, TrackingService.class);
                context.stopService(intent);
            } else {
                // Se estiver rastreando manualmente, apenas garantimos que não haja MONITOR pendente
                // No próximo "Arrivo em Casa", o Auto-Stop cuidará de encerrar e limpar.
            }
        }
    }

    private static boolean isTimeInRange(String start, String end) {
        try {
            String[] sParts = start.split(":");
            String[] eParts = end.split(":");
            
            int sHour = Integer.parseInt(sParts[0]);
            int sMin = Integer.parseInt(sParts[1]);
            int eHour = Integer.parseInt(eParts[0]);
            int eMin = Integer.parseInt(eParts[1]);

            Calendar now = Calendar.getInstance();
            int nowHour = now.get(Calendar.HOUR_OF_DAY);
            int nowMin = now.get(Calendar.MINUTE);

            int startMinTotal = sHour * 60 + sMin;
            int endMinTotal = eHour * 60 + eMin;
            int nowMinTotal = nowHour * 60 + nowMin;

            if (startMinTotal < endMinTotal) {
                return nowMinTotal >= startMinTotal && nowMinTotal < endMinTotal;
            } else {
                // Caso o horário vire a meia-noite (ex: 22:00 às 06:00)
                return nowMinTotal >= startMinTotal || nowMinTotal < endMinTotal;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private static void scheduleAlarm(Context context, String action, String time, int requestCode) {
        try {
            String[] parts = time.split(":");
            if (parts.length != 2) return;
            
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            if (cal.before(Calendar.getInstance())) {
                cal.add(Calendar.DAY_OF_MONTH, 1);
            }

            Intent intent = new Intent(context, TrackingReceiver.class);
            intent.putExtra("tracking_action", action);
            
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (am.canScheduleExactAlarms()) {
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
                    } else {
                        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
                    }
                } else {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
                }
            }
        } catch (Exception e) {
            Log.e("TrackingHelper", "Erro ao agendar alarme", e);
        }
    }

    public static void cancelAlarm(Context context, int requestCode) {
        try {
            Intent intent = new Intent(context, TrackingReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                am.cancel(pendingIntent);
            }
        } catch (Exception e) {
            Log.e("TrackingHelper", "Erro ao cancelar alarme", e);
        }
    }
}
