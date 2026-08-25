package com.example.drivelog;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MaintenanceRegisterFragment extends Fragment {

    private TextInputEditText editDescription, editValue, editDate, editKm, editInterval;
    private com.google.android.material.textfield.TextInputLayout layoutInterval, layoutKm;
    private android.widget.RadioGroup rgType;
    private Button btnSave, btnCancel;
    private int editingMaintId = -1;
    private Calendar selectedDate = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_maintenance_register, container, false);

        editDate = view.findViewById(R.id.editMaintDate);
        rgType = view.findViewById(R.id.rgMaintType);
        layoutInterval = view.findViewById(R.id.layoutMaintInterval);
        layoutKm = view.findViewById(R.id.layoutMaintKm);
        editInterval = view.findViewById(R.id.editMaintInterval);
        editKm = view.findViewById(R.id.editMaintKm);
        editDescription = view.findViewById(R.id.editMaintDescription);
        editValue = view.findViewById(R.id.editMaintValue);
        btnSave = view.findViewById(R.id.btnSaveMaintenance);
        btnCancel = view.findViewById(R.id.btnCancelMaintEdit);

        updateDateLabel();
        editDate.setOnClickListener(v -> showDatePicker());

        rgType.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isRecurring = (checkedId == R.id.rbRecorrente);
            layoutInterval.setVisibility(isRecurring ? View.VISIBLE : View.GONE);
            layoutKm.setVisibility(isRecurring ? View.VISIBLE : View.GONE);
        });

        btnSave.setOnClickListener(v -> saveMaintenance());
        btnCancel.setOnClickListener(v -> cancelEdit());

        if (savedInstanceState != null) {
            editingMaintId = savedInstanceState.getInt("editing_id", -1);
            selectedDate.setTimeInMillis(savedInstanceState.getLong("selected_date", System.currentTimeMillis()));
            updateDateLabel();
            
            // Restaura rascunhos
            editDescription.setText(savedInstanceState.getString("draft_desc", ""));
            editValue.setText(savedInstanceState.getString("draft_val", ""));
            editKm.setText(savedInstanceState.getString("draft_km", ""));
            editInterval.setText(savedInstanceState.getString("draft_interval", ""));

            if (editingMaintId != -1) {
                btnSave.setText("Atualizar Manutenção");
                btnCancel.setVisibility(View.VISIBLE);
            }
        }

        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("editing_id", editingMaintId);
        outState.putLong("selected_date", selectedDate.getTimeInMillis());
        
        // Salva rascunhos
        if (editDescription != null) outState.putString("draft_desc", editDescription.getText().toString());
        if (editValue != null) outState.putString("draft_val", editValue.getText().toString());
        if (editKm != null) outState.putString("draft_km", editKm.getText().toString());
        if (editInterval != null) outState.putString("draft_interval", editInterval.getText().toString());
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

    private void saveMaintenance() {
        String descStr = editDescription.getText().toString();
        String valStr = editValue.getText().toString();
        String kmStr = editKm.getText().toString();

        if (!descStr.isEmpty() && !valStr.isEmpty()) {
            Maintenance maint = new Maintenance();
            if (editingMaintId != -1) maint.id = editingMaintId;
            
            maint.description = descStr;
            maint.value = parseDouble(valStr);
            maint.date = selectedDate.getTimeInMillis();
            maint.type = rgType.getCheckedRadioButtonId() == R.id.rbRecorrente ? "Recorrente" : "Emergencial";
            
            if (maint.type.equals("Recorrente")) {
                if (kmStr.isEmpty()) {
                    Toast.makeText(getContext(), "Informe o KM para manutenção recorrente", Toast.LENGTH_SHORT).show();
                    return;
                }
                maint.km = Integer.parseInt(kmStr);
                String intStr = editInterval.getText().toString();
                if (!intStr.isEmpty()) {
                    maint.intervalKm = Integer.parseInt(intStr);
                    maint.alertKm = maint.km + maint.intervalKm;
                }
            } else {
                maint.km = 0;
            }

            if (editingMaintId == -1) {
                AppDatabase.getInstance(getContext()).appDao().insertMaintenance(maint);
                Toast.makeText(getContext(), "Manutenção salva!", Toast.LENGTH_SHORT).show();
            } else {
                AppDatabase.getInstance(getContext()).appDao().updateMaintenance(maint);
                Toast.makeText(getContext(), "Manutenção atualizada!", Toast.LENGTH_SHORT).show();
            }

            clearFields();
            if (editingMaintId != -1) cancelEdit();
            
            // Trigger auto cloud sync if enabled
            CloudSyncHelper.syncNow(requireContext(), editingMaintId == -1 ? "Nova Manutenção" : "Manutenção Editada");
        } else {
            Toast.makeText(getContext(), "Preencha todos os campos", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearFields() {
        editDescription.setText("");
        editValue.setText("");
        editKm.setText("");
        editInterval.setText("");
        rgType.check(R.id.rbEmergencial);
        selectedDate = Calendar.getInstance();
        updateDateLabel();
    }

    private void cancelEdit() {
        editingMaintId = -1;
        clearFields();
        btnSave.setText("Salvar Manutenção");
        btnCancel.setVisibility(View.GONE);
    }

    private double parseDouble(String value) {
        if (value == null || value.isEmpty()) return 0;
        try {
            return Double.parseDouble(value.replace(",", "."));
        } catch (Exception e) { return 0; }
    }

    public void startEdit(Maintenance maintenance) {
        editingMaintId = maintenance.id;
        if (getView() != null) {
            editDescription.setText(maintenance.description);
            editValue.setText(String.format(Locale.getDefault(), "%.2f", maintenance.value));
            selectedDate.setTimeInMillis(maintenance.date);
            updateDateLabel();
            
            if ("Recorrente".equals(maintenance.type)) {
                rgType.check(R.id.rbRecorrente);
                layoutInterval.setVisibility(View.VISIBLE);
                layoutKm.setVisibility(View.VISIBLE);
                editInterval.setText(String.valueOf(maintenance.intervalKm));
                editKm.setText(String.valueOf(maintenance.km));
            } else {
                rgType.check(R.id.rbEmergencial);
                layoutInterval.setVisibility(View.GONE);
                layoutKm.setVisibility(View.GONE);
                editKm.setText("");
            }

            btnSave.setText("Atualizar Manutenção");
            btnCancel.setVisibility(View.VISIBLE);
        }
    }
}