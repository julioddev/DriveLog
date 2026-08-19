package com.example.drivelog;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TodayWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_today);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0);
        long start = cal.getTimeInMillis();
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59);
        long end = cal.getTimeInMillis();

        AppDao dao = AppDatabase.getInstance(context).appDao();
        List<Earnings> earnings = dao.getAllEarnings();
        double dailyEarnings = 0;
        for (Earnings e : earnings) {
            if (e.date >= start && e.date <= end) dailyEarnings += e.totalValue;
        }

        DailyKm lastKm = dao.getLastDailyKm();
        String kmStart = "KM Inicial: --";
        if (lastKm != null && lastKm.date >= start) {
            kmStart = String.format(Locale.getDefault(), "KM Inicial: %.1f", lastKm.kmStart);
        }

        views.setTextViewText(R.id.widget_earnings, String.format(Locale.getDefault(), "R$ %.2f", dailyEarnings));
        views.setTextViewText(R.id.widget_km_start, kmStart);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
