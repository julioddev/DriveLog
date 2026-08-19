package com.example.drivelog;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class KmRegisterFragment extends Fragment implements KmAdapter.OnKmClickListener {

    private TextInputEditText editStartKm, editDate;
    private RecyclerView recyclerPending;
    private KmAdapter adapter;
    private Button btnSaveStart, btnCancelEdit;
    private TextView textInstruction;
    private int editingKmId = -1;
    private Calendar selectedDate = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public KmRegisterFragment() {
        // Inicializa com a data de hoje zerada
        selectedDate.set(Calendar.HOUR_OF_DAY, 0);
        selectedDate.set(Calendar.MINUTE, 0);
        selectedDate.set(Calendar.SECOND, 0);
        selectedDate.set(Calendar.MILLISECOND, 0);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_km_register, container, false);

        editDate = view.findViewById(R.id.editKmDate);
        editStartKm = view.findViewById(R.id.editStartKm);
        btnSaveStart = view.findViewById(R.id.btnSaveStartKm);
        btnCancelEdit = view.findViewById(R.id.btnCancelKmEdit);
        textInstruction = view.findViewById(R.id.textPendingInstruction);

        recyclerPending = view.findViewById(R.id.recyclerPendingKm);
        recyclerPending.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new KmAdapter(new ArrayList<>(), this);
        recyclerPending.setAdapter(adapter);

        updateDateLabel();
        editDate.setOnClickListener(v -> showDatePicker());

        btnSaveStart.setOnClickListener(v -> saveStartKm());
        btnCancelEdit.setOnClickListener(v -> cancelEdit());

        if (savedInstanceState != null) {
            editingKmId = savedInstanceState.getInt("editing_id", -1);
            selectedDate.setTimeInMillis(savedInstanceState.getLong("selected_date", System.currentTimeMillis()));
            updateDateLabel();
            
            // Restaura o texto digitado
            editStartKm.setText(savedInstanceState.getString("draft_km", ""));

            if (editingKmId != -1) {
                btnSaveStart.setText("Atualizar Registro");
                btnCancelEdit.setVisibility(View.VISIBLE);
            }
        }

        setupSwipeActions();
        loadPendingKm();

        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("selected_date", selectedDate.getTimeInMillis());
        outState.putInt("editing_id", editingKmId);
        if (editStartKm != null) outState.putString("draft_km", editStartKm.getText().toString());
    }

    private void updateDateLabel() {
        editDate.setText(dateFormat.format(selectedDate.getTime()));
    }

    private void showDatePicker() {
        new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
            selectedDate.set(Calendar.YEAR, year);
            selectedDate.set(Calendar.MONTH, month);
            selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            selectedDate.set(Calendar.HOUR_OF_DAY, 0);
            selectedDate.set(Calendar.MINUTE, 0);
            selectedDate.set(Calendar.SECOND, 0);
            selectedDate.set(Calendar.MILLISECOND, 0);
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

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    View itemView = viewHolder.itemView;
                    Paint paint = new Paint();
                    Drawable icon;
                    int iconMargin, iconTop, iconBottom, iconLeft, iconRight;

                    if (dX > 0) { // Right (Delete)
                        paint.setColor(Color.RED);
                        c.drawRect((float) itemView.getLeft(), (float) itemView.getTop(), dX, (float) itemView.getBottom(), paint);
                        icon = ContextCompat.getDrawable(getContext(), R.drawable.ic_delete);
                        if (icon != null) {
                            iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                            iconTop = itemView.getTop() + iconMargin;
                            iconBottom = iconTop + icon.getIntrinsicHeight();
                            iconLeft = itemView.getLeft() + iconMargin;
                            iconRight = iconLeft + icon.getIntrinsicWidth();
                            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                            icon.draw(c);
                        }
                    } else if (dX < 0) { // Left (Edit)
                        paint.setColor(Color.parseColor("#FFC107"));
                        c.drawRect((float) itemView.getRight() + dX, (float) itemView.getTop(), (float) itemView.getRight(), (float) itemView.getBottom(), paint);
                        icon = ContextCompat.getDrawable(getContext(), R.drawable.ic_edit);
                        if (icon != null) {
                            iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                            iconTop = itemView.getTop() + iconMargin;
                            iconBottom = iconTop + icon.getIntrinsicHeight();
                            iconRight = itemView.getRight() - iconMargin;
                            iconLeft = iconRight - icon.getIntrinsicWidth();
                            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                            icon.draw(c);
                        }
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerPending);
    }

    private void showDeleteConfirmation(DailyKm dailyKm, int position) {
        new AlertDialog.Builder(getContext())
                .setTitle("Confirmar Exclusão")
                .setMessage("Deseja realmente excluir este registro de KM?")
                .setPositiveButton("Excluir", (dialog, which) -> {
                    AppDatabase.getInstance(getContext()).appDao().deleteDailyKm(dailyKm);
                    Toast.makeText(getContext(), "Registro excluído", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", (dialog, which) -> adapter.notifyItemChanged(position))
                .setOnCancelListener(dialog -> adapter.notifyItemChanged(position))
                .show();
    }

    private void loadPendingKm() {
        AppDatabase.getInstance(getContext()).appDao().getAllPendingDailyKmLive().observe(getViewLifecycleOwner(), list -> {
            adapter.setKmList(list);
            textInstruction.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
        });
    }

    @Override
    public void onKmClick(DailyKm dailyKm) {
        // Encontra a posição do item na lista para ativar a edição
        for (int i = 0; i < adapter.getItemCount(); i++) {
            if (adapter.getKmAt(i).id == dailyKm.id) {
                adapter.setEditingPosition(i);
                break;
            }
        }
    }

    @Override
    public void onKmLongClick(DailyKm dailyKm, View anchor) {
        // Opções de desenvolvedor
    }

    @Override
    public void onSaveEdit(DailyKm dailyKm, int position) {
        if (dailyKm.isCompleted) {
            Fuel lastFuel = AppDatabase.getInstance(getContext()).appDao().getLastCompletedFuel();
            if (lastFuel != null && lastFuel.liters > 0 && lastFuel.kmDriven > 0) {
                double consumption = lastFuel.kmDriven / lastFuel.liters;
                dailyKm.consumptionUsed = consumption;
                dailyKm.estimatedFuelCost = (dailyKm.totalKm / consumption) * lastFuel.pricePerLiter;
            } else {
                // Fallback para valores padrão das configurações
                android.content.SharedPreferences prefs = requireActivity().getSharedPreferences("AppConfig", android.content.Context.MODE_PRIVATE);
                float defConsumption = prefs.getFloat("default_consumption", 10.0f);
                float defPrice = prefs.getFloat("default_fuel_price", 5.50f);
                
                dailyKm.consumptionUsed = defConsumption;
                dailyKm.estimatedFuelCost = (dailyKm.totalKm / defConsumption) * defPrice;
            }
        }
        AppDatabase.getInstance(getContext()).appDao().updateDailyKm(dailyKm);
        Toast.makeText(getContext(), "Registro atualizado!", Toast.LENGTH_SHORT).show();
        
        // Trigger auto cloud sync if enabled
        CloudSyncHelper.syncNow(requireContext());
    }

    private void cancelEdit() {
        editingKmId = -1;
        editStartKm.setText("");
        selectedDate = Calendar.getInstance();
        updateDateLabel();
        btnSaveStart.setText("Registrar KM Inicial");
        btnCancelEdit.setVisibility(View.GONE);
    }

    private void saveStartKm() {
        String kmStr = editStartKm.getText().toString();
        if (!kmStr.isEmpty()) {
            DailyKm dailyKm = new DailyKm();
            dailyKm.kmStart = parseDouble(kmStr);
            dailyKm.date = selectedDate.getTimeInMillis();
            dailyKm.isCompleted = false;
            dailyKm.isAutomatic = false;
            AppDatabase.getInstance(getContext()).appDao().insertDailyKm(dailyKm);
            Toast.makeText(getContext(), "KM Inicial salvo!", Toast.LENGTH_SHORT).show();
            cancelEdit();
            
            // Trigger auto cloud sync if enabled
            CloudSyncHelper.syncNow(requireContext());
        }
    }

    private double parseDouble(String value) {
        if (value == null || value.isEmpty()) return 0;
        try { return Double.parseDouble(value.replace(",", ".")); } catch (Exception e) { return 0; }
    }

    public void startEdit(DailyKm dailyKm) {
        editingKmId = dailyKm.id;
        if (getView() != null) {
            selectedDate.setTimeInMillis(dailyKm.date);
            updateDateLabel();
            editStartKm.setText(String.format(Locale.getDefault(), "%.1f", dailyKm.kmStart));
            btnSaveStart.setText("Atualizar Registro");
            btnCancelEdit.setVisibility(View.VISIBLE);
        }
    }
}
