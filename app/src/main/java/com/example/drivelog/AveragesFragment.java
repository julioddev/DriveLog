package com.example.drivelog;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AveragesFragment extends Fragment {

    private BarChart chartEarnings, chartKm, chartStation;
    private LineChart chartFuel;
    private PieChart chartPlatform;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_averages, container, false);

        chartEarnings = view.findViewById(R.id.chartWeeklyEarnings);
        chartFuel = view.findViewById(R.id.chartFuelConsumption);
        chartKm = view.findViewById(R.id.chartDailyKm);
        chartPlatform = view.findViewById(R.id.chartPlatformProfit);
        chartStation = view.findViewById(R.id.chartStationEfficiency);

        loadDataAndSetupCharts();

        return view;
    }

    private void loadDataAndSetupCharts() {
        Context context = getContext();
        if (context == null) return;

        AppDao dao = AppDatabase.getInstance(context).appDao();
        List<Earnings> earnings = dao.getAllEarnings();
        List<Fuel> fuels = dao.getAllFuel();
        List<DailyKm> kms = dao.getAllDailyKm();

        setupWeeklyEarningsChart(earnings);
        setupFuelConsumptionChart(fuels);
        setupDailyKmChart(kms);
        setupPlatformProfitChart(earnings);
        setupStationEfficiencyChart(fuels);
    }

    private void setupWeeklyEarningsChart(List<Earnings> allEarnings) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
        
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int diff = (dayOfWeek == Calendar.SUNDAY) ? 6 : (dayOfWeek - Calendar.MONDAY);
        cal.add(Calendar.DAY_OF_MONTH, -diff);

        for (int i = 7; i >= 0; i--) {
            Calendar weekStart = (Calendar) cal.clone();
            weekStart.add(Calendar.WEEK_OF_YEAR, -i);
            long start = weekStart.getTimeInMillis();
            
            Calendar weekEnd = (Calendar) weekStart.clone();
            weekEnd.add(Calendar.DAY_OF_YEAR, 6);
            weekEnd.set(Calendar.HOUR_OF_DAY, 23); weekEnd.set(Calendar.MINUTE, 59);
            long end = weekEnd.getTimeInMillis();

            float weekTotal = 0;
            for (Earnings e : allEarnings) {
                if (e.date >= start && e.date <= end) weekTotal += (float)e.totalValue;
            }
            
            entries.add(new BarEntry(7 - i, weekTotal));
            labels.add(dateFormat.format(start));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Ganhos Semanais");
        dataSet.setColor(Color.parseColor("#4CAF50"));
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(10f);
        
        BarData data = new BarData(dataSet);
        chartEarnings.setData(data);
        chartEarnings.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chartEarnings.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chartEarnings.getXAxis().setGranularity(1f);
        chartEarnings.setDrawValueAboveBar(false);
        chartEarnings.getDescription().setEnabled(false);
        chartEarnings.invalidate();
    }

    private void setupFuelConsumptionChart(List<Fuel> allFuel) {
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        List<Fuel> completed = new ArrayList<>();
        for (Fuel f : allFuel) if (f.isCompleted && f.kmDriven > 0) completed.add(f);
        
        Collections.reverse(completed);
        
        int startIdx = Math.max(0, completed.size() - 10);
        int x = 0;
        for (int i = startIdx; i < completed.size(); i++) {
            Fuel f = completed.get(i);
            float consumption = (float) (f.kmDriven / f.liters);
            entries.add(new Entry(x++, consumption));
            labels.add(dateFormat.format(f.date));
        }

        LineDataSet dataSet = new LineDataSet(entries, "KM/L");
        dataSet.setColor(Color.parseColor("#FF9800"));
        dataSet.setCircleColor(Color.parseColor("#FF9800"));
        dataSet.setLineWidth(2f);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(50);
        dataSet.setFillColor(Color.parseColor("#FF9800"));

        LineData data = new LineData(dataSet);
        chartFuel.setData(data);
        chartFuel.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chartFuel.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chartFuel.getDescription().setEnabled(false);
        chartFuel.invalidate();
    }

    private void setupDailyKmChart(List<DailyKm> allKm) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -14);
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0);

        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        int kmSource = prefs.getInt("report_km_source", 0); // 0: Manual, 1: Auto

        for (int i = 0; i < 15; i++) {
            long start = cal.getTimeInMillis();
            cal.add(Calendar.DAY_OF_YEAR, 1);
            long end = cal.getTimeInMillis();

            float dayKm = 0;
            for (DailyKm k : allKm) {
                if (k.date >= start && k.date < end && k.isCompleted) {
                    // Filtra pelo tipo de KM selecionado nas configurações
                    if (k.isAutomatic != (kmSource == 1)) continue;

                    dayKm += (float) ((kmSource == 1) ? k.gpsDistance : k.totalKm);
                }
            }
            
            entries.add(new BarEntry(i, dayKm));
            labels.add(dateFormat.format(start));
        }

        BarDataSet dataSet = new BarDataSet(entries, "KM Diário");
        dataSet.setColor(Color.parseColor("#2196F3"));
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(10f);

        BarData data = new BarData(dataSet);
        chartKm.setData(data);
        chartKm.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chartKm.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chartKm.getXAxis().setGranularity(1f);
        chartKm.setDrawValueAboveBar(false);
        chartKm.getDescription().setEnabled(false);
        chartKm.invalidate();
    }

    private void setupPlatformProfitChart(List<Earnings> allEarnings) {
        Map<String, Double> totals = new HashMap<>();
        
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        long startOfMonth = cal.getTimeInMillis();

        for (Earnings e : allEarnings) {
            if (e.date >= startOfMonth) {
                String p = (e.platforms != null && !e.platforms.isEmpty()) ? e.platforms : "Outros";
                totals.put(p, totals.getOrDefault(p, 0.0) + e.totalValue);
            }
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : totals.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);

        PieData data = new PieData(dataSet);
        chartPlatform.setData(data);
        chartPlatform.setCenterText("Ganhos/Plataforma");
        chartPlatform.getDescription().setEnabled(false);
        chartPlatform.invalidate();
    }

    private void setupStationEfficiencyChart(List<Fuel> allFuel) {
        Map<String, List<Double>> stationStats = new HashMap<>();
        
        for (Fuel f : allFuel) {
            if (f.isCompleted && f.kmDriven > 0 && f.gasStation != null && !f.gasStation.isEmpty()) {
                if (!stationStats.containsKey(f.gasStation)) {
                    stationStats.put(f.gasStation, new ArrayList<>());
                }
                stationStats.get(f.gasStation).add(f.kmDriven / f.liters);
            }
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int i = 0;

        for (Map.Entry<String, List<Double>> entry : stationStats.entrySet()) {
            double sum = 0;
            for (Double val : entry.getValue()) sum += val;
            double avg = sum / entry.getValue().size();
            
            entries.add(new BarEntry(i++, (float) avg));
            labels.add(entry.getKey());
        }

        BarDataSet dataSet = new BarDataSet(entries, "KM/L por Posto");
        dataSet.setColors(ColorTemplate.VORDIPLOM_COLORS);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(10f);

        BarData data = new BarData(dataSet);
        chartStation.setData(data);
        chartStation.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chartStation.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chartStation.getXAxis().setGranularity(1f);
        chartStation.setDrawValueAboveBar(false);
        chartStation.getDescription().setEnabled(false);
        chartStation.invalidate();
    }
}
