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

public class AnnualReportFragment extends Fragment {

    private TextView textEarnings, textFuel, textMaintenance, textBalance, textKm, textYear;
    private SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
    private Calendar currentDisplayDate = Calendar.getInstance();
    private List<Earnings> allEarnings = new java.util.ArrayList<>();
    private List<Fuel> allFuel = new java.util.ArrayList<>();
    private List<DailyKm> allKm = new java.util.ArrayList<>();
    private List<Maintenance> allMaintenance = new java.util.ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report_annual, container, false);

        textYear = view.findViewById(R.id.textYearName);
        textEarnings = view.findViewById(R.id.textAnnualEarnings);
        textFuel = view.findViewById(R.id.textAnnualFuel);
        textMaintenance = view.findViewById(R.id.textAnnualMaintenance);
        textBalance = view.findViewById(R.id.textAnnualBalance);
        textKm = view.findViewById(R.id.textAnnualKm);
        Button btnSelectYear = view.findViewById(R.id.btnSelectYear);

        btnSelectYear.setOnClickListener(v -> showYearPicker());

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

    private void showYearPicker() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        android.widget.NumberPicker yearPicker = new android.widget.NumberPicker(getContext());
        
        int year = Calendar.getInstance().get(Calendar.YEAR);
        yearPicker.setMinValue(year - 10);
        yearPicker.setMaxValue(year + 10);
        yearPicker.setValue(currentDisplayDate.get(Calendar.YEAR));
        
        builder.setView(yearPicker)
                .setTitle("Selecionar Ano")
                .setPositiveButton("OK", (dialog, which) -> {
                    currentDisplayDate.set(Calendar.YEAR, yearPicker.getValue());
                    updateReports();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void updateReports() {
        if (!isAdded()) return;

        textYear.setText("ANO: " + yearFormat.format(currentDisplayDate.getTime()));

        Calendar cal = (Calendar) currentDisplayDate.clone();
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfYear = cal.getTimeInMillis();

        cal.add(Calendar.YEAR, 1);
        long endOfYear = cal.getTimeInMillis();

        double totalEarnings = 0;
        for (Earnings e : allEarnings) {
            if (e.date >= startOfYear && e.date < endOfYear) totalEarnings += e.totalValue;
        }

        double totalMaintenance = 0;
        for (Maintenance m : allMaintenance) {
            if (m.date >= startOfYear && m.date < endOfYear) totalMaintenance += m.value;
        }

        double totalKmValue = 0;
        double totalFuelEst = 0;
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        float defConsumption = prefs.getFloat("default_consumption", 10.0f);
        float defPrice = prefs.getFloat("default_fuel_price", 5.50f);
        int kmSource = prefs.getInt("report_km_source", 0); // 0: Manual, 1: Auto

        for (DailyKm k : allKm) {
            if (k.isCompleted && k.date >= startOfYear && k.date < endOfYear) {
                // Filtra pelo tipo de KM selecionado nas configurações
                if (k.isAutomatic != (kmSource == 1)) continue;

                double kmForThisDay = (kmSource == 1) ? k.gpsDistance : k.totalKm;
                totalKmValue += kmForThisDay;
                
                if (kmSource == 1) {
                    if (k.estimatedFuelCost > 0) totalFuelEst += k.estimatedFuelCost;
                    else totalFuelEst += (k.gpsDistance / defConsumption) * defPrice;
                } else {
                    totalFuelEst += (k.totalKm / defConsumption) * defPrice;
                }
            }
        }

        textEarnings.setText(String.format(Locale.getDefault(), "R$ %.2f", totalEarnings));
        textFuel.setText(String.format(Locale.getDefault(), "R$ %.2f", totalFuelEst)); // Agora usa o estimado como os outros
        textMaintenance.setText(String.format(Locale.getDefault(), "R$ %.2f", totalMaintenance));
        textBalance.setText(String.format(Locale.getDefault(), "R$ %.2f", totalEarnings - totalFuelEst));
        textKm.setText(String.format(Locale.getDefault(), "%.1f KM", totalKmValue));
    }
}