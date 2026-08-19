package com.example.drivelog;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class WeeklyReportFragment extends Fragment {

    private TextView textEarnings, textFuel, textMaintenance, textRange, textPayment, textKmRodados;
    private TextView textNetBalance, textFuelEstInfo, textGoalStatus;
    private com.google.android.material.progressindicator.LinearProgressIndicator progressGoal;
    private RecyclerView recyclerPastWeeks;
    private WeeklyAdapter weeklyAdapter;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
    private Calendar currentDisplayDate = Calendar.getInstance();
    
    private List<Earnings> allEarnings = new java.util.ArrayList<>();
    private List<Fuel> allFuel = new java.util.ArrayList<>();
    private List<DailyKm> allKm = new java.util.ArrayList<>();
    private List<Maintenance> allMaintenance = new java.util.ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report_weekly, container, false);

        textEarnings = view.findViewById(R.id.textWeeklyEarnings);
        textFuel = view.findViewById(R.id.textWeeklyFuel);
        textMaintenance = view.findViewById(R.id.textWeeklyMaintenance);
        textRange = view.findViewById(R.id.textWeekRange);
        textPayment = view.findViewById(R.id.textPaymentDate);
        textNetBalance = view.findViewById(R.id.textWeeklyNetBalance);
        textFuelEstInfo = view.findViewById(R.id.textWeeklyFuelEstInfo);
        textKmRodados = view.findViewById(R.id.textWeeklyKmRodados);
        textGoalStatus = view.findViewById(R.id.textWeeklyGoalStatus);
        progressGoal = view.findViewById(R.id.progressWeeklyGoal);
        Button btnSelectWeek = view.findViewById(R.id.btnSelectWeek);
        
        recyclerPastWeeks = view.findViewById(R.id.recyclerPastWeeks);
        recyclerPastWeeks.setLayoutManager(new LinearLayoutManager(getContext()));
        weeklyAdapter = new WeeklyAdapter(new ArrayList<>());
        recyclerPastWeeks.setAdapter(weeklyAdapter);

        btnSelectWeek.setOnClickListener(v -> showDatePicker());

        AppDao dao = AppDatabase.getInstance(getContext()).appDao();
        
        dao.getAllEarningsLive().observe(getViewLifecycleOwner(), earnings -> {
            allEarnings = earnings;
            updateReports();
        });
        
        dao.getAllFuelLive().observe(getViewLifecycleOwner(), fuel -> {
            allFuel = fuel;
            updateReports();
        });
        
        dao.getAllKmAnyLive().observe(getViewLifecycleOwner(), km -> {
            allKm = km;
            updateReports();
        });
        
        dao.getAllMaintenanceLive().observe(getViewLifecycleOwner(), maintenance -> {
            allMaintenance = maintenance;
            updateReports();
        });

        updateReports();

        return view;
    }

    private void showDatePicker() {
        new android.app.DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
            currentDisplayDate.set(Calendar.YEAR, year);
            currentDisplayDate.set(Calendar.MONTH, month);
            currentDisplayDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateReports();
        }, currentDisplayDate.get(Calendar.YEAR), currentDisplayDate.get(Calendar.MONTH), currentDisplayDate.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateReports() {
        if (!isAdded()) return;

        Calendar cal = (Calendar) currentDisplayDate.clone();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int daysToSubtract = (dayOfWeek == Calendar.SUNDAY) ? 6 : (dayOfWeek - Calendar.MONDAY);
        cal.add(Calendar.DAY_OF_MONTH, -daysToSubtract);
        long startOfWeek = cal.getTimeInMillis();
        
        cal.add(Calendar.DAY_OF_MONTH, 6);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        long endOfWeek = cal.getTimeInMillis();

        Calendar payCal = Calendar.getInstance();
        payCal.setTimeInMillis(endOfWeek);
        payCal.add(Calendar.DAY_OF_MONTH, 4);
        
        textRange.setText(String.format("Período: %s até %s", dateFormat.format(startOfWeek), dateFormat.format(endOfWeek)));
        textPayment.setText(String.format("Recebimento: Quinta, %s", dateFormat.format(payCal.getTime())));

        // Fallbacks
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        float defConsumption = prefs.getFloat("default_consumption", 10.0f);
        float defPrice = prefs.getFloat("default_fuel_price", 5.50f);
        int kmSource = prefs.getInt("report_km_source", 0); // 0: Manual, 1: Auto

        // Cálculos da Semana Selecionada
        double earningsTotal = 0;
        for (Earnings e : allEarnings) {
            if (e.date >= startOfWeek && e.date <= endOfWeek) earningsTotal += e.totalValue;
        }

        double maintenanceTotal = 0;
        for (Maintenance m : allMaintenance) {
            if (m.date >= startOfWeek && m.date <= endOfWeek) maintenanceTotal += m.value;
        }

        double fuelActualTotal = 0;
        for (Fuel f : allFuel) {
            if (f.date >= startOfWeek && f.date <= endOfWeek) fuelActualTotal += f.value;
        }

        double fuelEstimatedTotal = 0;
        double kmRodadosTotal = 0;
        for (DailyKm k : allKm) {
            if (k.date >= startOfWeek && k.date <= endOfWeek) {
                if (k.isAutomatic != (kmSource == 1)) continue;
                if (k.isCompleted) {
                    double kmForThisDay = (kmSource == 1) ? k.gpsDistance : k.totalKm;
                    kmRodadosTotal += kmForThisDay;
                    
                    if (kmSource == 1) {
                        if (k.estimatedFuelCost > 0) fuelEstimatedTotal += k.estimatedFuelCost;
                        else fuelEstimatedTotal += (k.gpsDistance / defConsumption) * defPrice;
                    } else {
                        fuelEstimatedTotal += (k.totalKm / defConsumption) * defPrice;
                    }
                }
            }
        }

        textEarnings.setText(String.format(Locale.getDefault(), "R$ %.2f", earningsTotal));
        textFuel.setText(String.format(Locale.getDefault(), "R$ %.2f", fuelActualTotal));
        textMaintenance.setText(String.format(Locale.getDefault(), "R$ %.2f", maintenanceTotal));
        textKmRodados.setText(String.format(Locale.getDefault(), "%.1f KM", kmRodadosTotal));

        double netBalance = earningsTotal - fuelEstimatedTotal;
        textNetBalance.setText(String.format(Locale.getDefault(), "R$ %.2f", netBalance));
        textFuelEstInfo.setText(String.format(Locale.getDefault(), 
                "(Ganhos - R$ %.2f de combustível rodado)", fuelEstimatedTotal));

        updateGoalProgress(earningsTotal);

        // 10 Semanas anteriores
        List<WeeklyReport> pastWeeks = new ArrayList<>();
        Calendar weekCal = Calendar.getInstance();
        weekCal.setTimeInMillis(startOfWeek);
        for (int i = 0; i < 10; i++) {
            long s = weekCal.getTimeInMillis();
            weekCal.add(Calendar.DAY_OF_MONTH, 6);
            long e = weekCal.getTimeInMillis();
            
            double wEarnings = 0;
            for (Earnings earn : allEarnings) if (earn.date >= s && earn.date <= e) wEarnings += earn.totalValue;
            
            double wFuelEst = 0;
            for (DailyKm km : allKm) {
                if (km.isAutomatic != (kmSource == 1)) continue;
                if (km.date >= s && km.date <= e && km.isCompleted) {
                    if (kmSource == 1) {
                        if (km.estimatedFuelCost > 0) wFuelEst += km.estimatedFuelCost;
                        else wFuelEst += (km.gpsDistance / defConsumption) * defPrice;
                    } else {
                        wFuelEst += (km.totalKm / defConsumption) * defPrice;
                    }
                }
            }
            
            String period = dateFormat.format(s) + " - " + dateFormat.format(e);
            pastWeeks.add(new WeeklyReport(period, wEarnings, wFuelEst));
            
            weekCal.setTimeInMillis(s);
            weekCal.add(Calendar.DAY_OF_MONTH, -7);
        }
        weeklyAdapter.setReports(pastWeeks);
    }

    private void updateGoalProgress(double earningsTotal) {
        android.content.SharedPreferences prefs = requireActivity().getSharedPreferences("AppConfig", android.content.Context.MODE_PRIVATE);
        float goal = prefs.getFloat("weekly_goal", 1500.0f);
        
        int percent = (int) ((earningsTotal / goal) * 100);
        progressGoal.setProgress(Math.min(percent, 100));
        
        textGoalStatus.setText(String.format(Locale.getDefault(), 
                "R$ %.2f de R$ %.2f (%d%%)", earningsTotal, goal, percent));
    }
}
