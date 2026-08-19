package com.example.drivelog;

import android.app.DatePickerDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MaintenanceAdapter extends RecyclerView.Adapter<MaintenanceAdapter.MaintenanceViewHolder> {

    private List<Maintenance> maintenanceList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private OnMaintClickListener listener;
    private int editingMaintId = -1;

    public interface OnMaintClickListener {
        void onMaintClick(Maintenance maintenance);
        void onSaveEdit(Maintenance maintenance, int position);
    }

    public MaintenanceAdapter(List<Maintenance> maintenanceList, OnMaintClickListener listener) {
        this.maintenanceList = maintenanceList;
        this.listener = listener;
    }

    public void setMaintenanceList(List<Maintenance> maintenanceList) {
        this.maintenanceList = maintenanceList;
        notifyDataSetChanged();
    }

    public Maintenance getMaintAt(int position) {
        return maintenanceList.get(position);
    }

    public int getEditingId() {
        return editingMaintId;
    }

    public Maintenance getEditingItem() {
        if (editingMaintId != -1) {
            for (Maintenance m : maintenanceList) {
                if (m.id == editingMaintId) return m;
            }
        }
        return null;
    }

    public void setEditingId(int id) {
        this.editingMaintId = id;
        notifyDataSetChanged();
    }

    public void setEditingPosition(int position) {
        if (position >= 0 && position < maintenanceList.size()) {
            int newId = maintenanceList.get(position).id;
            if (editingMaintId == newId) {
                editingMaintId = -1;
            } else {
                editingMaintId = newId;
            }
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public MaintenanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_maintenance, parent, false);
        return new MaintenanceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MaintenanceViewHolder holder, int pos) {
        final int position = holder.getBindingAdapterPosition();
        Maintenance maint = maintenanceList.get(position);
        holder.textDescription.setText(maint.description);
        holder.textValue.setText(String.format(Locale.getDefault(), "R$ %.2f", maint.value));
        holder.textDate.setText(dateFormat.format(new Date(maint.date)));

        // Inline Editing Logic
        if (maint.id == editingMaintId) {
            holder.layoutEdit.setVisibility(View.VISIBLE);
            holder.editDescription.setText(maint.description);
            holder.editValue.setText(String.format(Locale.getDefault(), "%.2f", maint.value));
            holder.editDate.setText(dateFormat.format(new Date(maint.date)));
            
            if ("Recorrente".equals(maint.type)) {
                holder.rgType.check(R.id.rbEditMaintRecorrente);
            } else {
                holder.rgType.check(R.id.rbEditMaintEmergencial);
            }

            // Watcher para atualizar o objeto em tempo real (preservar dados ao minimizar)
            TextWatcher draftWatcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    try {
                        maint.description = holder.editDescription.getText().toString();
                        maint.value = parseDouble(holder.editValue.getText().toString());
                        maint.type = holder.rgType.getCheckedRadioButtonId() == R.id.rbEditMaintRecorrente ? "Recorrente" : "Emergencial";
                    } catch (Exception ignored) {}
                }
            };
            holder.editDescription.addTextChangedListener(draftWatcher);
            holder.editValue.addTextChangedListener(draftWatcher);

            holder.editDate.setOnClickListener(v -> {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(maint.date);
                new DatePickerDialog(holder.itemView.getContext(), (view1, year, month, dayOfMonth) -> {
                    cal.set(Calendar.YEAR, year);
                    cal.set(Calendar.MONTH, month);
                    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    maint.date = cal.getTimeInMillis();
                    holder.editDate.setText(dateFormat.format(new Date(maint.date)));
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
            });

        } else {
            holder.layoutEdit.setVisibility(View.GONE);
        }

        holder.btnSave.setOnClickListener(v -> {
            try {
                String description = holder.editDescription.getText().toString();
                double value = parseDouble(holder.editValue.getText().toString());
                String type = holder.rgType.getCheckedRadioButtonId() == R.id.rbEditMaintRecorrente ? "Recorrente" : "Emergencial";

                maint.description = description;
                maint.value = value;
                maint.type = type;

                if (listener != null) {
                    listener.onSaveEdit(maint, position);
                }
                editingMaintId = -1;
                notifyItemChanged(position);
            } catch (Exception e) {
                // Handle error
            }
        });

        holder.btnCancel.setOnClickListener(v -> {
            editingMaintId = -1;
            notifyItemChanged(position);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onMaintClick(maint);
        });
    }

    private double parseDouble(String value) {
        if (value == null || value.isEmpty()) return 0;
        try {
            return Double.parseDouble(value.replace(",", "."));
        } catch (Exception e) { return 0; }
    }

    @Override
    public int getItemCount() {
        return maintenanceList.size();
    }

    static class MaintenanceViewHolder extends RecyclerView.ViewHolder {
        TextView textDescription, textValue, textDate;
        View layoutEdit;
        EditText editDescription, editValue, editDate;
        RadioGroup rgType;
        MaterialButton btnSave, btnCancel;

        public MaintenanceViewHolder(@NonNull View itemView) {
            super(itemView);
            // Main display views
            textDescription = itemView.findViewById(R.id.textMaintDescription);
            textValue = itemView.findViewById(R.id.textMaintValue);
            textDate = itemView.findViewById(R.id.textMaintDate);
            
            // Edit layout views
            layoutEdit = itemView.findViewById(R.id.layoutEditMaint);
            editDescription = itemView.findViewById(R.id.editMaintDescription);
            editValue = itemView.findViewById(R.id.editMaintValue);
            editDate = itemView.findViewById(R.id.editMaintDate);
            rgType = itemView.findViewById(R.id.rgEditMaintType);
            btnSave = itemView.findViewById(R.id.btnSaveEditMaint);
            btnCancel = itemView.findViewById(R.id.btnCancelEditMaint);
        }
    }
}
