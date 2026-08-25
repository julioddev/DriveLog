package com.example.drivelog;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class EarningsRegisterFragment extends Fragment {

    private TextInputEditText editDate, editExtra, editTotal;
    private TextInputLayout layoutExtra;
    private RadioGroup rgPlatforms;
    private Button btnSave, btnCancel, btnFinalize;
    private Calendar selectedDate = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private int editingEarningsId = -1;
    private List<Platform> activePlatforms = new ArrayList<>();
    private String lastPlatformName = "";

    private boolean isRestoring = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_earnings_register, container, false);

        editDate = view.findViewById(R.id.editEarningsDate);
        rgPlatforms = view.findViewById(R.id.rgPlatforms);
        layoutExtra = view.findViewById(R.id.layoutExtra);
        editExtra = view.findViewById(R.id.editExtraValue);
        editTotal = view.findViewById(R.id.editEarningsTotal);
        btnSave = view.findViewById(R.id.btnSaveEarnings);
        btnFinalize = view.findViewById(R.id.btnFinalizeDay);
        btnCancel = view.findViewById(R.id.btnCancelEdit);

        AppDao dao = AppDatabase.getInstance(getContext()).appDao();
        
        // Observe hoje para mostrar o botão de finalizar
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0); today.set(Calendar.MINUTE, 0); today.set(Calendar.SECOND, 0); today.set(Calendar.MILLISECOND, 0);
        long start = today.getTimeInMillis();
        long end = start + (24 * 60 * 60 * 1000);
        
        dao.getTodayEarningsEntriesLive(start, end).observe(getViewLifecycleOwner(), entries -> {
            boolean hasPending = false;
            if (entries != null) {
                for (Earnings e : entries) if (!e.isCompleted) { hasPending = true; break; }
            }
            btnFinalize.setVisibility(hasPending ? View.VISIBLE : View.GONE);
        });

        dao.getAllPlatformsLive().observe(getViewLifecycleOwner(), platforms -> {
            activePlatforms.clear();
            for (Platform p : platforms) if (p.isEnabled) activePlatforms.add(p);
            updatePlatformRadioButtons();
            
            // 🔥 Após carregar plataformas, verifica se precisamos restaurar a seleção
            if (isRestoring && lastPlatformName != null && !lastPlatformName.isEmpty()) {
                for (int i = 0; i < rgPlatforms.getChildCount(); i++) {
                    RadioButton rb = (RadioButton) rgPlatforms.getChildAt(i);
                    if (rb.getText().toString().equals(lastPlatformName)) {
                        rb.setChecked(true);
                        break;
                    }
                }
            }
        });

        updateDateLabel();
        editDate.setOnClickListener(v -> showDatePicker());

        rgPlatforms.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton rb = group.findViewById(checkedId);
            if (rb == null) return;
            
            String text = rb.getText().toString();
            lastPlatformName = text; // Mantém sincronizado para o save state

            if (text.contains("Shopee")) {
                layoutExtra.setVisibility(View.VISIBLE);
            } else {
                layoutExtra.setVisibility(View.GONE);
                if (!isRestoring) editExtra.setText(""); 
            }
            
            if (!isRestoring) {
                if (text.equals("99")) {
                    editTotal.setText("");
                } else if (text.equals("Folga / Não trabalhei")) {
                    editTotal.setText("0,00");
                }
                updateTotalFromCurrentSelection();
            }
        });

        editExtra.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isRestoring) updateTotalFromCurrentSelection();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnSave.setOnClickListener(v -> saveEarnings());
        btnFinalize.setOnClickListener(v -> finalizeTodayEarnings());
        btnCancel.setOnClickListener(v -> cancelEdit());

        if (savedInstanceState != null) {
            isRestoring = true;
            editingEarningsId = savedInstanceState.getInt("editing_id", -1);
            lastPlatformName = savedInstanceState.getString("last_platform", "");
            selectedDate.setTimeInMillis(savedInstanceState.getLong("selected_date", System.currentTimeMillis()));
            updateDateLabel();
            
            // Restaura o texto digitado manualmente pelo usuário
            editExtra.setText(savedInstanceState.getString("draft_extra", ""));
            editTotal.setText(savedInstanceState.getString("draft_total", ""));

            if (editingEarningsId != -1) {
                btnSave.setText("Atualizar Ganho");
                btnCancel.setVisibility(View.VISIBLE);
            }
            isRestoring = false;
        }

        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("editing_id", editingEarningsId);
        outState.putLong("selected_date", selectedDate.getTimeInMillis());
        
        // Salva o texto atual dos campos para não perder ao minimizar
        if (editExtra != null) outState.putString("draft_extra", editExtra.getText().toString());
        if (editTotal != null) outState.putString("draft_total", editTotal.getText().toString());
        
        // Salva qual plataforma estava selecionada
        int checkedId = rgPlatforms.getCheckedRadioButtonId();
        if (checkedId != -1) {
            RadioButton rb = rgPlatforms.findViewById(checkedId);
            if (rb != null) outState.putString("last_platform", rb.getText().toString());
        } else {
            outState.putString("last_platform", lastPlatformName);
        }
    }

    private void updatePlatformRadioButtons() {
        rgPlatforms.removeAllViews();

        for (Platform p : activePlatforms) {
            RadioButton rb = new RadioButton(getContext());
            rb.setText(p.name);
            rb.setId(View.generateViewId());
            rb.setTag(p);
            rgPlatforms.addView(rb);
            if (editingEarningsId != -1 && p.name.equals(lastPlatformName)) {
                rb.setChecked(true);
            }
        }
    }

    private void showDatePicker() {
        new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
            selectedDate.set(Calendar.YEAR, year);
            selectedDate.set(Calendar.MONTH, month);
            selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateLabel();
        }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateLabel() {
        editDate.setText(dateFormat.format(selectedDate.getTime()));
    }

    private void updateTotalFromCurrentSelection() {
        int checkedId = rgPlatforms.getCheckedRadioButtonId();
        if (checkedId == -1) return;

        RadioButton rb = rgPlatforms.findViewById(checkedId);
        if (rb == null) return;

        String name = rb.getText().toString();
        if (name.equals("99") || name.equals("Folga / Não trabalhei")) return;

        Object tag = rb.getTag();
        if (tag instanceof Platform) {
            Platform p = (Platform) tag;
            double extra = parseDouble(editExtra.getText().toString());
            editTotal.setText(String.format(Locale.getDefault(), "%.2f", p.defaultValue + extra));
        }
    }

    private void saveEarnings() {
        int checkedId = rgPlatforms.getCheckedRadioButtonId();
        if (checkedId == -1) {
            Toast.makeText(getContext(), "Selecione uma plataforma", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton rb = rgPlatforms.findViewById(checkedId);
        String platformName = rb.getText().toString();
        double base = 0;

        Object tag = rb.getTag();
        if (tag instanceof Platform) {
            base = ((Platform) tag).defaultValue;
        }

        double extra = parseDouble(editExtra.getText().toString());
        double total = parseDouble(editTotal.getText().toString());

        AppDao dao = AppDatabase.getInstance(getContext()).appDao();
        Earnings earnings = (editingEarningsId != -1) ? dao.getAllEarnings().stream().filter(e -> e.id == editingEarningsId).findFirst().orElse(new Earnings()) : new Earnings();
        if (editingEarningsId != -1) earnings.id = editingEarningsId;
        
        earnings.baseValue = base;
        earnings.extraValue = extra;
        earnings.totalValue = total;
        earnings.platforms = platformName;
        earnings.date = selectedDate.getTimeInMillis();
        earnings.isCompleted = platformName.equals("Folga / Não trabalhei");

        new Thread(() -> {
            if (editingEarningsId == -1) {
                dao.insertEarnings(earnings);
                if (platformName.equals("Folga / Não trabalhei")) {
                    DailyKm dailyKm = new DailyKm();
                    dailyKm.date = earnings.date;
                    dailyKm.kmStart = 0; dailyKm.kmEnd = 0; dailyKm.totalKm = 0;
                    dailyKm.isCompleted = true; dailyKm.estimatedFuelCost = 0; dailyKm.consumptionUsed = 0;
                    dao.insertDailyKm(dailyKm);
                }
            } else {
                dao.updateEarnings(earnings);
            }
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "Ganho salvo!", Toast.LENGTH_SHORT).show();
                cancelEdit();
                
                // Trigger auto cloud sync if enabled
                CloudSyncHelper.syncNow(requireContext(), "Novo Ganho (Registro)");
            });
        }).start();
    }

    private void finalizeTodayEarnings() {
        new Thread(() -> {
            AppDao dao = AppDatabase.getInstance(getContext()).appDao();
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0); today.set(Calendar.MINUTE, 0); today.set(Calendar.SECOND, 0); today.set(Calendar.MILLISECOND, 0);
            long start = today.getTimeInMillis();
            long end = start + (24 * 60 * 60 * 1000);
            
            List<Earnings> todayEntries = dao.getAllEarnings().stream()
                    .filter(e -> e.date >= start && e.date <= end && !e.isCompleted)
                    .collect(java.util.stream.Collectors.toList());
            
            for (Earnings e : todayEntries) {
                e.isCompleted = true;
                dao.updateEarnings(e);
            }
            
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "Dia finalizado! Ganhos concluídos.", Toast.LENGTH_SHORT).show();
                CloudSyncHelper.syncNow(requireContext(), "Novo Ganho (Registro)");
            });
        }).start();
    }

    private void cancelEdit() {
        editingEarningsId = -1;
        lastPlatformName = "";
        rgPlatforms.clearCheck();
        editExtra.setText("");
        editTotal.setText("");
        selectedDate = Calendar.getInstance();
        updateDateLabel();
        btnSave.setText("Salvar Ganhos");
        btnCancel.setVisibility(View.GONE);
    }

    private double parseDouble(String value) {
        if (value == null || value.isEmpty()) return 0;
        try {
            return Double.parseDouble(value.replace(",", "."));
        } catch (Exception e) { return 0; }
    }

    public void startEdit(Earnings earnings) {
        editingEarningsId = earnings.id;
        lastPlatformName = earnings.platforms;
        if (getView() != null) {
            selectedDate.setTimeInMillis(earnings.date);
            updateDateLabel();
            updatePlatformRadioButtons();
            editExtra.setText(String.format(Locale.getDefault(), "%.2f", earnings.extraValue));
            editTotal.setText(String.format(Locale.getDefault(), "%.2f", earnings.totalValue));
            btnSave.setText("Atualizar Ganho");
            btnCancel.setVisibility(View.VISIBLE);
        }
    }
}
