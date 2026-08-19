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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DailyReportFragment extends Fragment {

    private TextView textEarnings, textFuel, textMaintenance, textBalance, textKmStatus, textDate, textKmRodados;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private Calendar currentDisplayDate = Calendar.getInstance();
    private List<Earnings> allEarnings = new java.util.ArrayList<>();
    private List<DailyKm> allKm = new java.util.ArrayList<>();
    private List<Maintenance> allMaintenance = new java.util.ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report_daily, container, false);

        textDate = view.findViewById(R.id.textReportDate);
        textEarnings = view.findViewById(R.id.textDailyEarnings);
        textFuel = view.findViewById(R.id.textDailyFuelEst);
        textMaintenance = view.findViewById(R.id.textDailyMaintenance);
        textBalance = view.findViewById(R.id.textDailyBalance);
        textKmStatus = view.findViewById(R.id.textDailyKmStatus);
        textKmRodados = view.findViewById(R.id.textDailyKmRodados);
        Button btnSelectDate = view.findViewById(R.id.btnSelectDate);

        btnSelectDate.setOnClickListener(v -> showDatePicker());

        AppDao dao = AppDatabase.getInstance(getContext()).appDao();
        
        dao.getAllEarningsLive().observe(getViewLifecycleOwner(), earnings -> {
            allEarnings = earnings;
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
        long startOfDay = cal.getTimeInMillis();
        
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        long endOfDay = cal.getTimeInMillis();

        String dateLabel = android.text.format.DateUtils.isToday(startOfDay) ? "Hoje: " : "Data: ";
        textDate.setText(String.format("%s%s", dateLabel, dateFormat.format(startOfDay)));

        double dailyEarnings = 0;
        for (Earnings e : allEarnings) {
            if (e.date >= startOfDay && e.date <= endOfDay) dailyEarnings += e.totalValue;
        }

        double dailyMaintenance = 0;
        for (Maintenance m : allMaintenance) {
            if (m.date >= startOfDay && m.date <= endOfDay) dailyMaintenance += m.value;
        }

        double dailyFuelEst = 0;
        double dailyKmRodados = 0;
        boolean dailyKmPending = true;
        
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        float defConsumption = prefs.getFloat("default_consumption", 10.0f);
        float defPrice = prefs.getFloat("default_fuel_price", 5.50f);
        int kmSource = prefs.getInt("report_km_source", 0); // 0: Manual, 1: Auto

        for (DailyKm k : allKm) {
            if (k.date >= startOfDay && k.date <= endOfDay) {
                // Filtra pelo tipo de KM selecionado nas configurações
                if (k.isAutomatic != (kmSource == 1)) continue;

                if (k.isCompleted) {
                    double kmForThisDay = (kmSource == 1) ? k.gpsDistance : k.totalKm;
                    dailyKmRodados += kmForThisDay;
                    dailyKmPending = false;
                    
                    if (kmSource == 1) {
                        // Se a fonte for GPS, tentamos usar o custo estimado já salvo (que é baseado no GPS)
                        // Se não houver custo salvo, calculamos com base no gpsDistance
                        if (k.estimatedFuelCost > 0) {
                            dailyFuelEst += k.estimatedFuelCost;
                        } else {
                            dailyFuelEst += (k.gpsDistance / defConsumption) * defPrice;
                        }
                    } else {
                        // Se a fonte for Manual, calculamos com base no totalKm
                        dailyFuelEst += (k.totalKm / defConsumption) * defPrice;
                    }
                }
            }
        }

        textEarnings.setText(String.format(Locale.getDefault(), "R$ %.2f", dailyEarnings));
        textFuel.setText(String.format(Locale.getDefault(), "R$ %.2f", dailyFuelEst));
        textMaintenance.setText(String.format(Locale.getDefault(), "R$ %.2f", dailyMaintenance));
        textBalance.setText(String.format(Locale.getDefault(), "R$ %.2f", dailyEarnings - dailyFuelEst));
        textKmRodados.setText(String.format(Locale.getDefault(), "%.1f KM", dailyKmRodados));
        
        if (dailyKmPending && android.text.format.DateUtils.isToday(startOfDay)) {
            textKmStatus.setVisibility(View.VISIBLE);
            textKmStatus.setText("KM Diário: PENDENTE (Informe KM Final)");
            textFuel.setTextColor(getResources().getColor(android.R.color.darker_gray));
        } else {
            textKmStatus.setVisibility(View.GONE);
            textFuel.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }
}
