package com.example.drivelog;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MonthlyReportFragment extends Fragment {

    private TextView textEarnings, textFuel, textMaintenance, textBalance, textKm, textMonth, textFuelEst;
    private SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM / yyyy", Locale.getDefault());
    private Calendar currentDisplayDate = Calendar.getInstance();
    private List<Earnings> allEarnings = new java.util.ArrayList<>();
    private List<Fuel> allFuel = new java.util.ArrayList<>();
    private List<DailyKm> allKm = new java.util.ArrayList<>();
    private List<Maintenance> allMaintenance = new java.util.ArrayList<>();
    
    private double totalEarnings, totalFuel, totalMaintenance, totalKmValue, totalFuelEst;

    private final ActivityResultLauncher<Intent> createPdfLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    writePdfToUri(result.getData().getData());
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report_monthly, container, false);

        textMonth = view.findViewById(R.id.textMonthName);
        textEarnings = view.findViewById(R.id.textMonthlyEarnings);
        textFuel = view.findViewById(R.id.textMonthlyFuel);
        textFuelEst = view.findViewById(R.id.textMonthlyFuelEst);
        textMaintenance = view.findViewById(R.id.textMonthlyMaintenance);
        textBalance = view.findViewById(R.id.textMonthlyBalance);
        textKm = view.findViewById(R.id.textMonthlyKm);
        Button btnSelectMonth = view.findViewById(R.id.btnSelectMonth);
        Button btnExportPdf = view.findViewById(R.id.btnExportMonthlyPdf);

        btnSelectMonth.setOnClickListener(v -> showMonthPicker());
        btnExportPdf.setOnClickListener(v -> startPdfExport());

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

    private void showMonthPicker() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_month_year_picker, null);
        
        android.widget.NumberPicker monthPicker = dialogView.findViewById(R.id.monthPicker);
        android.widget.NumberPicker yearPicker = dialogView.findViewById(R.id.yearPicker);
        
        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(11);
        monthPicker.setDisplayedValues(new String[]{"Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez"});
        monthPicker.setValue(currentDisplayDate.get(Calendar.MONTH));
        
        int year = Calendar.getInstance().get(Calendar.YEAR);
        yearPicker.setMinValue(year - 5);
        yearPicker.setMaxValue(year + 5);
        yearPicker.setValue(currentDisplayDate.get(Calendar.YEAR));
        
        builder.setView(dialogView)
                .setTitle("Selecionar Mês e Ano")
                .setPositiveButton("OK", (dialog, which) -> {
                    currentDisplayDate.set(Calendar.MONTH, monthPicker.getValue());
                    currentDisplayDate.set(Calendar.YEAR, yearPicker.getValue());
                    updateReports();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void updateReports() {
        if (!isAdded()) return;

        textMonth.setText(monthFormat.format(currentDisplayDate.getTime()).toUpperCase());

        Calendar cal = (Calendar) currentDisplayDate.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfMonth = cal.getTimeInMillis();

        cal.add(Calendar.MONTH, 1);
        long endOfMonth = cal.getTimeInMillis();

        totalEarnings = 0;
        for (Earnings e : allEarnings) {
            if (e.date >= startOfMonth && e.date < endOfMonth) totalEarnings += e.totalValue;
        }

        totalFuel = 0;
        for (Fuel f : allFuel) {
            if (f.date >= startOfMonth && f.date < endOfMonth) totalFuel += f.value;
        }

        totalMaintenance = 0;
        for (Maintenance m : allMaintenance) {
            if (m.date >= startOfMonth && m.date < endOfMonth) totalMaintenance += m.value;
        }

        totalKmValue = 0;
        totalFuelEst = 0;

        SharedPreferences prefs = requireActivity().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        float defConsumption = prefs.getFloat("default_consumption", 10.0f);
        float defPrice = prefs.getFloat("default_fuel_price", 5.50f);
        int kmSource = prefs.getInt("report_km_source", 0); // 0: Manual, 1: Auto

        for (DailyKm k : allKm) {
            if (k.date >= startOfMonth && k.date < endOfMonth) {
                if (k.isAutomatic != (kmSource == 1)) continue;
                if (k.isCompleted) {
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
        }

        textEarnings.setText(String.format(Locale.getDefault(), "R$ %.2f", totalEarnings));
        textFuel.setText(String.format(Locale.getDefault(), "R$ %.2f", totalFuel));
        textFuelEst.setText(String.format(Locale.getDefault(), "R$ %.2f", totalFuelEst));
        textMaintenance.setText(String.format(Locale.getDefault(), "R$ %.2f", totalMaintenance));
        
        textBalance.setText(String.format(Locale.getDefault(), "R$ %.2f", totalEarnings - totalFuelEst));
        textKm.setText(String.format(Locale.getDefault(), "%.1f KM", totalKmValue));
    }

    private void startPdfExport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        String fileName = "Relatorio_" + monthFormat.format(currentDisplayDate.getTime()).replace(" ", "_") + ".pdf";
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        createPdfLauncher.launch(intent);
    }

    private void writePdfToUri(Uri uri) {
        if (uri == null) return;

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 size
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();

        int y = 50;

        // Cabeçalho
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(24);
        canvas.drawText("DriveLog - Relatório Mensal", 50, y, paint);
        y += 40;

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setTextSize(16);
        canvas.drawText("Mês: " + monthFormat.format(currentDisplayDate.getTime()), 50, y, paint);
        y += 60;

        // Resumo
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Resumo Financeiro", 50, y, paint);
        y += 30;

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Ganhos Totais: R$ " + String.format(Locale.getDefault(), "%.2f", totalEarnings), 70, y, paint);
        y += 25;
        canvas.drawText("Custo Est. Combustível (KM): R$ " + String.format(Locale.getDefault(), "%.2f", totalFuelEst), 70, y, paint);
        y += 25;
        canvas.drawText("Gastos Manutenção: R$ " + String.format(Locale.getDefault(), "%.2f", totalMaintenance), 70, y, paint);
        y += 35;
        
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Saldo Líquido (Estimado): R$ " + String.format(Locale.getDefault(), "%.2f", totalEarnings - totalFuelEst), 70, y, paint);
        y += 50;

        canvas.drawText("Atividade", 50, y, paint);
        y += 30;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Quilometragem Total: " + String.format(Locale.getDefault(), "%.1f KM", totalKmValue), 70, y, paint);

        // Rodapé
        paint.setTextSize(10);
        paint.setColor(android.graphics.Color.GRAY);
        canvas.drawText("Gerado pelo aplicativo DriveLog em " + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new java.util.Date()), 50, 800, paint);

        document.finishPage(page);

        try {
            document.writeTo(getContext().getContentResolver().openOutputStream(uri));
            Toast.makeText(getContext(), "Relatório PDF salvo com sucesso!", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(getContext(), "Erro ao salvar PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            document.close();
        }
    }
}
