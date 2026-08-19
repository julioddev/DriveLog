package com.example.drivelog;

import android.app.DatePickerDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class KmAdapter extends RecyclerView.Adapter<KmAdapter.KmViewHolder> {

    private List<DailyKm> kmList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
    private OnKmClickListener listener;
    private int editingKmId = -1;

    public interface OnKmClickListener {
        void onKmClick(DailyKm dailyKm);
        void onKmLongClick(DailyKm dailyKm, View anchor);
        void onSaveEdit(DailyKm dailyKm, int position);
    }

    public KmAdapter(List<DailyKm> kmList, OnKmClickListener listener) {
        this.kmList = kmList;
        this.listener = listener;
    }

    public void setKmList(List<DailyKm> kmList) {
        this.kmList = kmList;
        // Não resetamos mais o editingKmId aqui para evitar fechar a janela durante rastreamento
        notifyDataSetChanged();
    }

    public DailyKm getKmAt(int position) {
        return kmList.get(position);
    }

    public int getEditingKmId() {
        return editingKmId;
    }

    public DailyKm getEditingItem() {
        if (editingKmId != -1) {
            for (DailyKm k : kmList) {
                if (k.id == editingKmId) return k;
            }
        }
        return null;
    }

    public void setEditingKmId(int id) {
        this.editingKmId = id;
        notifyDataSetChanged();
    }

    public void setEditingPosition(int position) {
        if (position >= 0 && position < kmList.size()) {
            int newId = kmList.get(position).id;
            if (editingKmId == newId) {
                editingKmId = -1; // Toggle off
            } else {
                editingKmId = newId;
            }
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public KmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_km, parent, false);
        return new KmViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull KmViewHolder holder, int pos) {
        final int position = holder.getBindingAdapterPosition();
        DailyKm dailyKm = kmList.get(position);
        holder.textDate.setText(dateFormat.format(new Date(dailyKm.date)));
        
        if (dailyKm.isCompleted || dailyKm.isAutomatic) {
            StringBuilder valueBuilder = new StringBuilder();
            
            double displayKm = dailyKm.totalKm;
            if (dailyKm.isAutomatic && dailyKm.gpsDistance > 0) {
                displayKm = dailyKm.gpsDistance;
            }
            
            valueBuilder.append(String.format(Locale.getDefault(), "%.1f KM", displayKm));
            
            if (!dailyKm.isAutomatic && dailyKm.gpsDistance > 0) {
                valueBuilder.append(String.format(Locale.getDefault(), " | Maps: %.1f KM", dailyKm.gpsDistance));
            }

            double estimatedCost = dailyKm.estimatedFuelCost;
            if (estimatedCost <= 0 && displayKm > 0) {
                android.content.SharedPreferences prefs = holder.itemView.getContext().getSharedPreferences("AppConfig", android.content.Context.MODE_PRIVATE);
                float defConsumption = prefs.getFloat("default_consumption", 10.0f);
                float defPrice = prefs.getFloat("default_fuel_price", 5.50f);
                estimatedCost = (displayKm / defConsumption) * defPrice;
            }

            if (estimatedCost > 0) {
                valueBuilder.append(String.format(Locale.getDefault(), " (R$ %.2f est.)", estimatedCost));
            }
            holder.textValue.setText(valueBuilder.toString());

            double consumption = dailyKm.consumptionUsed;
            if (consumption <= 0) {
                android.content.SharedPreferences prefs = holder.itemView.getContext().getSharedPreferences("AppConfig", android.content.Context.MODE_PRIVATE);
                consumption = prefs.getFloat("default_consumption", 10.0f);
            }

            String details = "";
            if (dailyKm.isAutomatic) {
                details = "Rota Automática (GPS)";
            } else {
                details = String.format(Locale.getDefault(), "Início: %.1f | Fim: %.1f", dailyKm.kmStart, dailyKm.kmEnd);
            }

            if (consumption > 0) {
                details += String.format(Locale.getDefault(), " | Média: %.2f KM/L", consumption);
            }
            holder.textDetails.setText(details);
            holder.textDetails.setVisibility(View.VISIBLE);
            holder.textPending.setVisibility(View.GONE);
        } else {
            holder.textValue.setText("...");
            holder.textDetails.setVisibility(View.GONE);
            holder.textPending.setVisibility(View.VISIBLE);
        }

        // Inline Editing Logic
        if (dailyKm.id == editingKmId) {
            holder.layoutEdit.setVisibility(View.VISIBLE);
            holder.editStart.setText(String.format(Locale.getDefault(), "%.1f", dailyKm.kmStart));
            holder.editEnd.setText(String.format(Locale.getDefault(), "%.1f", dailyKm.kmEnd));
            
            holder.editEnd.setOnClickListener(v -> {
                String current = holder.editEnd.getText().toString();
                if (current.equals("0.0") || current.equals("0,0")) {
                    holder.editEnd.setText("");
                }
            });

            holder.editDate.setText(dateFormat.format(new Date(dailyKm.date)));

            // Watchers para atualizar o objeto em tempo real (evita perda de dados ao minimizar)
            TextWatcher draftWatcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    try {
                        dailyKm.kmStart = parseDouble(holder.editStart.getText().toString());
                        dailyKm.kmEnd = parseDouble(holder.editEnd.getText().toString());
                        dailyKm.totalKm = dailyKm.kmEnd - dailyKm.kmStart;
                        dailyKm.isCompleted = (dailyKm.kmEnd > 0);
                    } catch (Exception ignored) {}
                }
            };
            holder.editStart.addTextChangedListener(draftWatcher);
            holder.editEnd.addTextChangedListener(draftWatcher);

            holder.editDate.setOnClickListener(v -> {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(dailyKm.date);
                new DatePickerDialog(holder.itemView.getContext(), (view1, year, month, dayOfMonth) -> {
                    cal.set(Calendar.YEAR, year);
                    cal.set(Calendar.MONTH, month);
                    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    dailyKm.date = cal.getTimeInMillis();
                    holder.editDate.setText(dateFormat.format(new Date(dailyKm.date)));
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
            });

        } else {
            holder.layoutEdit.setVisibility(View.GONE);
        }

        holder.btnSave.setOnClickListener(v -> {
            try {
                double start = parseDouble(holder.editStart.getText().toString());
                double end = parseDouble(holder.editEnd.getText().toString());
                
                dailyKm.kmStart = start;
                dailyKm.kmEnd = end;
                dailyKm.totalKm = end - start;
                dailyKm.isCompleted = (end > 0);
                
                if (listener != null) {
                    listener.onSaveEdit(dailyKm, position);
                }
                editingKmId = -1;
                notifyItemChanged(position);
            } catch (Exception e) {
                // Handle error
            }
        });

        holder.btnCancel.setOnClickListener(v -> {
            editingKmId = -1;
            notifyItemChanged(position);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onKmClick(dailyKm);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onKmLongClick(dailyKm, v);
            return true;
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
        return kmList.size();
    }

    static class KmViewHolder extends RecyclerView.ViewHolder {
        TextView textDate, textValue, textDetails, textPending;
        View layoutEdit;
        EditText editStart, editEnd, editDate;
        MaterialButton btnSave, btnCancel;

        public KmViewHolder(@NonNull View itemView) {
            super(itemView);
            textDate = itemView.findViewById(R.id.textKmDate);
            textValue = itemView.findViewById(R.id.textKmValue);
            textDetails = itemView.findViewById(R.id.textKmDetails);
            textPending = itemView.findViewById(R.id.textKmPending);
            
            layoutEdit = itemView.findViewById(R.id.layoutEditKm);
            editStart = itemView.findViewById(R.id.editKmStart);
            editEnd = itemView.findViewById(R.id.editKmEnd);
            editDate = itemView.findViewById(R.id.editKmDateInline);
            btnSave = itemView.findViewById(R.id.btnSaveEditKm);
            btnCancel = itemView.findViewById(R.id.btnCancelEditKm);
        }
    }
}
