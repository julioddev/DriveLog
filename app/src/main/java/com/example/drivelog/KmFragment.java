package com.example.drivelog;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class KmFragment extends Fragment implements KmAdapter.OnKmClickListener {

    private TextInputEditText editStartKm, editEndKm, editManualConsumption, editDate;
    private TextInputLayout layoutManualConsumption;
    private RadioGroup rgConsumptionType;
    private LinearLayout layoutStart, layoutFinish;
    private TextView textKmStartInfo, textPendingDate;
    private MaterialCardView cardPendingKm, cardRestWarning;
    private RecyclerView recyclerHistory;
    private KmAdapter adapter;
    private DailyKm lastPendingKm;
    private Button btnSaveStart, btnCancelEdit, btnSaveEnd;
    private View btnDisableRest;
    private int editingKmId = -1;
    private Calendar selectedDate = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_km, container, false);

        layoutStart = view.findViewById(R.id.layoutStartDay);
        editDate = view.findViewById(R.id.editKmDate);
        editStartKm = view.findViewById(R.id.editStartKm);
        btnSaveStart = view.findViewById(R.id.btnSaveStartKm);
        btnCancelEdit = view.findViewById(R.id.btnCancelKmEdit);
        cardPendingKm = view.findViewById(R.id.cardPendingKm);
        textPendingDate = view.findViewById(R.id.textPendingDate);
        cardRestWarning = view.findViewById(R.id.cardRestWarningKm);
        btnDisableRest = view.findViewById(R.id.btnDisableRestInKm);

        if (btnDisableRest != null) {
            btnDisableRest.setOnClickListener(v -> disableRestAndGoToSettings());
        }

        layoutFinish = view.findViewById(R.id.layoutFinishDay);
        textKmStartInfo = view.findViewById(R.id.textKmStartInfo);
        editEndKm = view.findViewById(R.id.editEndKm);
        editManualConsumption = view.findViewById(R.id.editManualConsumption);
        layoutManualConsumption = view.findViewById(R.id.layoutManualConsumption);
        rgConsumptionType = view.findViewById(R.id.rgConsumptionType);
        btnSaveEnd = view.findViewById(R.id.btnSaveEndKm);

        recyclerHistory = view.findViewById(R.id.recyclerKmHistory);
        recyclerHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new KmAdapter(new ArrayList<>(), this);
        recyclerHistory.setAdapter(adapter);

        // Fix conflict between Swipe and ViewPager2
        recyclerHistory.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull android.view.MotionEvent e) {
                if (e.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    rv.getParent().requestDisallowInterceptTouchEvent(true);
                }
                return false;
            }
            @Override public void onTouchEvent(@NonNull RecyclerView rv, @NonNull android.view.MotionEvent e) {}
            @Override public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
        });

        updateDateLabel();
        editDate.setOnClickListener(v -> showDatePicker());

        rgConsumptionType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbManualConsumption) {
                layoutManualConsumption.setVisibility(View.VISIBLE);
            } else {
                layoutManualConsumption.setVisibility(View.GONE);
                editManualConsumption.setText("");
            }
        });

        setupSwipeActions();
        btnSaveStart.setOnClickListener(v -> saveStartKm());
        btnCancelEdit.setOnClickListener(v -> cancelEdit());
        btnSaveEnd.setOnClickListener(v -> saveEndKm());
        
        cardPendingKm.setOnClickListener(v -> {
            if (lastPendingKm != null) {
                onKmClick(lastPendingKm);
            }
        });

        checkPendingKm();
        updateHistory();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkRestInterval();
    }

    private void updateDateLabel() {
        editDate.setText(dateFormat.format(selectedDate.getTime()));
    }

    private void showDatePicker() {
        new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
            selectedDate.set(Calendar.YEAR, year);
            selectedDate.set(Calendar.MONTH, month);
            selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateLabel();
        }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void setupSwipeActions() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                DailyKm dailyKm = adapter.getKmAt(position);

                if (direction == ItemTouchHelper.LEFT) {
                    adapter.setEditingPosition(position);
                } else if (direction == ItemTouchHelper.RIGHT) {
                    showDeleteConfirmation(dailyKm, position);
                }
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerHistory);
    }

    private void showDeleteConfirmation(DailyKm dailyKm, int position) {
        new AlertDialog.Builder(getContext())
                .setTitle("Confirmar Exclusão")
                .setMessage("Deseja realmente excluir este registro de KM?")
                .setPositiveButton("Excluir", (dialog, which) -> {
                    AppDatabase.getInstance(getContext()).appDao().deleteDailyKm(dailyKm);
                    updateHistory();
                    checkPendingKm();
                    Toast.makeText(getContext(), "Registro excluído", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", (dialog, which) -> adapter.notifyItemChanged(position))
                .setOnCancelListener(dialog -> adapter.notifyItemChanged(position))
                .show();
    }

    private void checkPendingKm() {
        checkRestInterval();
        lastPendingKm = AppDatabase.getInstance(getContext()).appDao().getLastPendingDailyKm();
        
        if (editingKmId != -1) {
            // Se ESTIVERMOS editando algo, ocultamos o card e mostramos o layout apropriado
            cardPendingKm.setVisibility(View.GONE);
            layoutStart.setVisibility(View.GONE);
            layoutFinish.setVisibility(View.GONE);

            DailyKm editing = AppDatabase.getInstance(getContext()).appDao().getAllDailyKm().stream()
                    .filter(k -> k.id == editingKmId).findFirst().orElse(null);
            
            if (editing != null) {
                if (!editing.isCompleted) {
                    layoutFinish.setVisibility(View.VISIBLE);
                    textKmStartInfo.setText(String.format(Locale.getDefault(), "KM Inicial registrado: %.1f", editing.kmStart));
                } else {
                    layoutStart.setVisibility(View.VISIBLE);
                }
            }
        } else {
            // Se NÃO estivermos editando nada
            layoutFinish.setVisibility(View.GONE);
            layoutStart.setVisibility(View.VISIBLE);
            
            if (lastPendingKm != null) {
                cardPendingKm.setVisibility(View.VISIBLE);
                textPendingDate.setText(dateFormat.format(new Date(lastPendingKm.date)));
            } else {
                cardPendingKm.setVisibility(View.GONE);
            }
        }
    }

    private void checkRestInterval() {
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        boolean restEnabled = prefs.getBoolean("rest_interval_enabled", false);
        if (restEnabled) {
            String start = prefs.getString("rest_start_time", "12:00");
            String end = prefs.getString("rest_end_time", "13:00");
            if (isCurrentTimeInInterval(start, end)) {
                cardRestWarning.setVisibility(View.VISIBLE);
                return;
            }
        }
        cardRestWarning.setVisibility(View.GONE);
    }

    private boolean isCurrentTimeInInterval(String start, String end) {
        try {
            String[] sParts = start.split(":");
            String[] eParts = end.split(":");
            int sHour = Integer.parseInt(sParts[0]), sMin = Integer.parseInt(sParts[1]);
            int eHour = Integer.parseInt(eParts[0]), eMin = Integer.parseInt(eParts[1]);
            Calendar now = Calendar.getInstance();
            int nowHour = now.get(Calendar.HOUR_OF_DAY);
            int nowMin = now.get(Calendar.MINUTE);
            int startTotal = sHour * 60 + sMin;
            int endTotal = eHour * 60 + eMin;
            int nowTotal = nowHour * 60 + nowMin;
            if (startTotal < endTotal) return nowTotal >= startTotal && nowTotal < endTotal;
            else return nowTotal >= startTotal || nowTotal < endTotal;
        } catch (Exception e) { return false; }
    }

    private void disableRestAndGoToSettings() {
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("rest_interval_enabled", false).apply();
        checkRestInterval();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openGeneralSettings();
        }
    }

    @Override
    public void onKmClick(DailyKm dailyKm) {
        editingKmId = dailyKm.id;
        
        if (!dailyKm.isCompleted) {
            // Se o que cliquei para editar está pendente, mostro o layout de finalizar
            layoutStart.setVisibility(View.GONE);
            layoutFinish.setVisibility(View.VISIBLE);
            textKmStartInfo.setText(String.format(Locale.getDefault(), "KM Inicial registrado: %.1f", dailyKm.kmStart));
            editEndKm.setText("");
            btnSaveEnd.setText("Atualizar KM Final");
        } else {
            // Se já está completo, mostro o layout de iniciar para editar o inicial/data
            layoutStart.setVisibility(View.VISIBLE);
            layoutFinish.setVisibility(View.GONE);
            editStartKm.setText(String.format(Locale.getDefault(), "%.1f", dailyKm.kmStart));
            btnSaveStart.setText("Atualizar Registro");
        }
        
        selectedDate.setTimeInMillis(dailyKm.date);
        updateDateLabel();
        
        btnCancelEdit.setVisibility(View.VISIBLE);
        cardPendingKm.setVisibility(View.GONE);
        Toast.makeText(getContext(), "Modo de edição ativado", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onKmLongClick(DailyKm dailyKm, View anchor) {
        // Opções de desenvolvedor podem ser adicionadas aqui se necessário
    }

    @Override
    public void onSaveEdit(DailyKm dailyKm, int position) {
        AppDatabase.getInstance(getContext()).appDao().updateDailyKm(dailyKm);
        updateHistory();
        checkPendingKm();
        Toast.makeText(getContext(), "Registro atualizado!", Toast.LENGTH_SHORT).show();
        
        // Trigger auto cloud sync if enabled
        CloudSyncHelper.syncNow(requireContext());
    }

    private void cancelEdit() {
        editingKmId = -1;
        editStartKm.setText("");
        editEndKm.setText("");
        selectedDate = Calendar.getInstance();
        updateDateLabel();
        btnSaveStart.setText("Registrar KM Inicial");
        btnSaveEnd.setText("Registrar KM Final e Calcular");
        btnCancelEdit.setVisibility(View.GONE);
        checkPendingKm();
    }

    private void saveStartKm() {
        String kmStr = editStartKm.getText().toString();
        if (!kmStr.isEmpty()) {
            if (editingKmId == -1) {
                DailyKm dailyKm = new DailyKm();
                dailyKm.kmStart = parseDouble(kmStr);
                dailyKm.date = selectedDate.getTimeInMillis();
                dailyKm.isCompleted = false;
                AppDatabase.getInstance(getContext()).appDao().insertDailyKm(dailyKm);
                Toast.makeText(getContext(), "KM Inicial salvo!", Toast.LENGTH_SHORT).show();
            } else {
                DailyKm dailyKm = AppDatabase.getInstance(getContext()).appDao().getAllDailyKm().stream().filter(k -> k.id == editingKmId).findFirst().orElse(null);
                if (dailyKm != null) {
                    dailyKm.kmStart = parseDouble(kmStr);
                    dailyKm.date = selectedDate.getTimeInMillis();
                    if (dailyKm.isCompleted) {
                        dailyKm.totalKm = dailyKm.kmEnd - dailyKm.kmStart;
                        // Recalculate fuel cost if it was completed
                        Fuel lastFuel = AppDatabase.getInstance(getContext()).appDao().getLastCompletedFuel();
                        if (lastFuel != null && lastFuel.liters > 0 && lastFuel.kmDriven > 0) {
                            double consumption = (dailyKm.consumptionUsed > 0) ? dailyKm.consumptionUsed : (lastFuel.kmDriven / lastFuel.liters);
                            double estimatedLiters = dailyKm.totalKm / consumption;
                            dailyKm.estimatedFuelCost = estimatedLiters * lastFuel.pricePerLiter;
                        }
                    }
                    AppDatabase.getInstance(getContext()).appDao().updateDailyKm(dailyKm);
                    Toast.makeText(getContext(), "Registro Atualizado!", Toast.LENGTH_SHORT).show();
                }
            }
            cancelEdit();
            updateHistory();
            
            // Trigger auto cloud sync if enabled
            CloudSyncHelper.syncNow(requireContext());
        } else {
            Toast.makeText(getContext(), "Informe o KM Inicial", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveEndKm() {
        String kmStr = editEndKm.getText().toString();
        if (kmStr.isEmpty()) {
            Toast.makeText(getContext(), "Informe o KM Final", Toast.LENGTH_SHORT).show();
            return;
        }

        DailyKm targetKm = (editingKmId != -1) ? 
            AppDatabase.getInstance(getContext()).appDao().getAllDailyKm().stream().filter(k -> k.id == editingKmId).findFirst().orElse(null) : 
            lastPendingKm;

        if (targetKm == null) return;

        double kmEnd = parseDouble(kmStr);
        if (kmEnd < targetKm.kmStart) {
            Toast.makeText(getContext(), "KM Final não pode ser menor que o Inicial", Toast.LENGTH_SHORT).show();
            return;
        }

        double consumption;
        if (rgConsumptionType.getCheckedRadioButtonId() == R.id.rbManualConsumption) {
            String manualStr = editManualConsumption.getText().toString();
            if (manualStr.isEmpty()) {
                Toast.makeText(getContext(), "Informe o consumo manual", Toast.LENGTH_SHORT).show();
                return;
            }
            consumption = parseDouble(manualStr);
            if (consumption <= 0) {
                Toast.makeText(getContext(), "Consumo deve ser maior que zero", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            Fuel lastFuel = AppDatabase.getInstance(getContext()).appDao().getLastCompletedFuel();
            if (lastFuel != null && lastFuel.liters > 0 && lastFuel.kmDriven > 0) {
                consumption = lastFuel.kmDriven / lastFuel.liters;
            } else {
                Toast.makeText(getContext(), "Nenhum abastecimento finalizado encontrado para cálculo automático. Use o manual.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        double totalKm = kmEnd - targetKm.kmStart;
        targetKm.kmEnd = kmEnd;
        targetKm.totalKm = totalKm;
        targetKm.isCompleted = true;
        targetKm.consumptionUsed = consumption;

        Fuel lastFuel = AppDatabase.getInstance(getContext()).appDao().getLastCompletedFuel();
        double price = (lastFuel != null) ? lastFuel.pricePerLiter : 0;
        targetKm.estimatedFuelCost = (totalKm / consumption) * price;

        AppDatabase.getInstance(getContext()).appDao().updateDailyKm(targetKm);
        Toast.makeText(getContext(), String.format(Locale.getDefault(), "Finalizado! Rodou %.1f KM (Média: %.2f)", totalKm, consumption), Toast.LENGTH_LONG).show();
        
        editEndKm.setText("");
        editManualConsumption.setText("");
        rgConsumptionType.check(R.id.rbAutoConsumption);
        cancelEdit();
        updateHistory();
        
        // Trigger auto cloud sync if enabled
        CloudSyncHelper.syncNow(requireContext());
    }

    private void updateHistory() {
        List<DailyKm> list = AppDatabase.getInstance(getContext()).appDao().getRecentDailyKm();
        adapter.setKmList(list);
    }

    private double parseDouble(String value) {
        if (value == null || value.isEmpty()) return 0;
        try {
            return Double.parseDouble(value.replace(",", "."));
        } catch (Exception e) { return 0; }
    }
}
