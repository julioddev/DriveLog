package com.example.drivelog;

import android.app.DatePickerDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FuelAdapter extends RecyclerView.Adapter<FuelAdapter.FuelViewHolder> {

    private List<Fuel> fuelList;
    private List<String> stations = new ArrayList<>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault());
    private OnFuelClickListener listener;
    private int editingFuelId = -1;
    private boolean isUpdatingInternal = false;

    public interface OnFuelClickListener {
        void onFuelClick(Fuel fuel);
        void onSaveEdit(Fuel fuel, int position);
    }

    public FuelAdapter(List<Fuel> fuelList, OnFuelClickListener listener) {
        this.fuelList = fuelList;
        this.listener = listener;
    }

    public void setFuelList(List<Fuel> fuelList) {
        this.fuelList = fuelList;
        notifyDataSetChanged();
    }

    public void setStations(List<String> stations) {
        this.stations = stations;
        notifyDataSetChanged();
    }

    public Fuel getFuelAt(int position) {
        return fuelList.get(position);
    }

    public int getEditingId() {
        return editingFuelId;
    }

    public Fuel getEditingItem() {
        if (editingFuelId != -1) {
            for (Fuel f : fuelList) {
                if (f.id == editingFuelId) return f;
            }
        }
        return null;
    }

    public void setEditingId(int id) {
        this.editingFuelId = id;
        notifyDataSetChanged();
    }

    public void setEditingPosition(int position) {
        if (position >= 0 && position < fuelList.size()) {
            int newId = fuelList.get(position).id;
            if (editingFuelId == newId) {
                editingFuelId = -1;
            } else {
                editingFuelId = newId;
            }
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public FuelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fuel, parent, false);
        return new FuelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FuelViewHolder holder, int pos) {
        final int position = holder.getBindingAdapterPosition();
        Fuel fuel = fuelList.get(position);
        holder.textDate.setText(dateFormat.format(new Date(fuel.date)));
        holder.textTotalValue.setText(String.format(Locale.getDefault(), "R$ %.2f", fuel.value));
        holder.textDetails.setText(String.format(Locale.getDefault(), "%.2f L @ R$ %.2f/L | KM: %d", 
                fuel.liters, fuel.pricePerLiter, fuel.km));

        if (fuel.isCompleted) {
            double consumption = fuel.kmDriven / fuel.liters;
            holder.textConsumption.setVisibility(View.VISIBLE);
            holder.textPending.setVisibility(View.GONE);
            holder.textConsumption.setText(String.format(Locale.getDefault(), 
                    "Consumo: %.2f KM/L (Rodou %.1f KM)", consumption, fuel.kmDriven));
        } else {
            holder.textConsumption.setVisibility(View.GONE);
            holder.textPending.setVisibility(View.VISIBLE);
        }

        // Inline Editing Logic
        if (fuel.id == editingFuelId) {
            holder.layoutEdit.setVisibility(View.VISIBLE);
            
            isUpdatingInternal = true;
            holder.editValue.setText(String.format(Locale.getDefault(), "%.2f", fuel.value));
            holder.editPricePerLiter.setText(String.format(Locale.getDefault(), "%.2f", fuel.pricePerLiter));
            holder.editLiters.setText(String.format(Locale.getDefault(), "%.2f", fuel.liters));
            holder.editKm.setText(String.valueOf(fuel.km));
            holder.editDate.setText(dateFormat.format(new Date(fuel.date)));
            isUpdatingInternal = false;

            // Watcher para atualizar o objeto em tempo real (preservar dados ao minimizar)
            TextWatcher draftWatcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    if (isUpdatingInternal) return;
                    try {
                        fuel.value = parseDouble(holder.editValue.getText().toString());
                        fuel.pricePerLiter = parseDouble(holder.editPricePerLiter.getText().toString());
                        fuel.liters = parseDouble(holder.editLiters.getText().toString());
                        fuel.km = Integer.parseInt(holder.editKm.getText().toString());
                        fuel.gasStation = holder.spinnerStation.getSelectedItem() != null ? holder.spinnerStation.getSelectedItem().toString() : "";
                        fuel.fuelType = holder.rgFuelType.getCheckedRadioButtonId() == R.id.rbEditGasComum ? "Comum" : "Aditivada";
                        
                        if (fuel.isCompleted) {
                            String kmFinalStr = holder.editKmFinal.getText().toString();
                            if (!kmFinalStr.isEmpty()) {
                                fuel.kmDriven = Integer.parseInt(kmFinalStr) - fuel.km;
                            }
                        }
                    } catch (Exception ignored) {}
                }
            };
            holder.editValue.addTextChangedListener(draftWatcher);
            holder.editPricePerLiter.addTextChangedListener(draftWatcher);
            holder.editLiters.addTextChangedListener(draftWatcher);
            holder.editKm.addTextChangedListener(draftWatcher);
            holder.editKmFinal.addTextChangedListener(draftWatcher);
            
            // Setup Station Spinner
            ArrayAdapter<String> stationAdapter = new ArrayAdapter<>(holder.itemView.getContext(), 
                    android.R.layout.simple_spinner_item, stations.isEmpty() ? new String[]{"Nenhum posto"} : stations.toArray(new String[0]));
            stationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            holder.spinnerStation.setAdapter(stationAdapter);
            
            int stationPos = stations.indexOf(fuel.gasStation);
            if (stationPos >= 0) holder.spinnerStation.setSelection(stationPos);

            // Setup Fuel Type RadioGroup
            if ("Comum".equals(fuel.fuelType)) holder.rgFuelType.check(R.id.rbEditGasComum);
            else holder.rgFuelType.check(R.id.rbEditGasAditivada);
            
            if (fuel.isCompleted) {
                holder.layoutKmFinal.setVisibility(View.VISIBLE);
                holder.editKmFinal.setText(String.valueOf(fuel.km + (int)fuel.kmDriven));
            } else {
                holder.layoutKmFinal.setVisibility(View.GONE);
                holder.editKmFinal.setText("");
            }

            holder.editDate.setOnClickListener(v -> {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(fuel.date);
                new DatePickerDialog(holder.itemView.getContext(), (view1, year, month, dayOfMonth) -> {
                    cal.set(Calendar.YEAR, year);
                    cal.set(Calendar.MONTH, month);
                    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    fuel.date = cal.getTimeInMillis();
                    holder.editDate.setText(dateFormat.format(new Date(fuel.date)));
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
            });
            
            // TextWatchers for auto-calculation
            TextWatcher calculationWatcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (!isUpdatingInternal) {
                        isUpdatingInternal = true;
                        try {
                            double totalValue = parseDouble(holder.editValue.getText().toString());
                            double pricePerLiter = parseDouble(holder.editPricePerLiter.getText().toString());
                            if (pricePerLiter > 0) {
                                double liters = totalValue / pricePerLiter;
                                holder.editLiters.setText(String.format(Locale.getDefault(), "%.2f", liters));
                            }
                        } catch (Exception e) {}
                        isUpdatingInternal = false;
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            };
            
            holder.editValue.addTextChangedListener(calculationWatcher);
            holder.editPricePerLiter.addTextChangedListener(calculationWatcher);

        } else {
            holder.layoutEdit.setVisibility(View.GONE);
        }

        holder.btnSave.setOnClickListener(v -> {
            try {
                double value = parseDouble(holder.editValue.getText().toString());
                double pricePerLiter = parseDouble(holder.editPricePerLiter.getText().toString());
                double liters = parseDouble(holder.editLiters.getText().toString());
                int km = Integer.parseInt(holder.editKm.getText().toString());
                String station = holder.spinnerStation.getSelectedItem() != null ? holder.spinnerStation.getSelectedItem().toString() : "";
                String fuelType = holder.rgFuelType.getCheckedRadioButtonId() == R.id.rbEditGasComum ? "Comum" : "Aditivada";

                fuel.value = value;
                fuel.pricePerLiter = pricePerLiter;
                fuel.liters = liters;
                fuel.km = km;
                fuel.gasStation = station;
                fuel.fuelType = fuelType;

                if (fuel.isCompleted) {
                    String kmFinalStr = holder.editKmFinal.getText().toString();
                    if (!kmFinalStr.isEmpty()) {
                        int kmFinal = Integer.parseInt(kmFinalStr);
                        fuel.kmDriven = kmFinal - km;
                    }
                }

                if (listener != null) {
                    listener.onSaveEdit(fuel, position);
                }
                editingFuelId = -1;
                notifyItemChanged(position);
            } catch (Exception e) {
                // Handle error
            }
        });

        holder.btnCancel.setOnClickListener(v -> {
            editingFuelId = -1;
            notifyItemChanged(position);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onFuelClick(fuel);
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
        return fuelList.size();
    }

    static class FuelViewHolder extends RecyclerView.ViewHolder {
        TextView textDate, textTotalValue, textDetails, textConsumption, textPending;
        View layoutEdit, layoutKmFinal;
        EditText editValue, editPricePerLiter, editLiters, editKm, editKmFinal, editDate;
        Spinner spinnerStation;
        RadioGroup rgFuelType;
        MaterialButton btnSave, btnCancel;

        public FuelViewHolder(@NonNull View itemView) {
            super(itemView);
            textDate = itemView.findViewById(R.id.textDate);
            textTotalValue = itemView.findViewById(R.id.textTotalValue);
            textDetails = itemView.findViewById(R.id.textDetails);
            textConsumption = itemView.findViewById(R.id.textConsumption);
            textPending = itemView.findViewById(R.id.textPending);
            
            layoutEdit = itemView.findViewById(R.id.layoutEditFuel);
            layoutKmFinal = itemView.findViewById(R.id.layoutEditFuelKmFinal);
            editValue = itemView.findViewById(R.id.editFuelValue);
            editPricePerLiter = itemView.findViewById(R.id.editFuelPricePerLiter);
            editLiters = itemView.findViewById(R.id.editFuelLiters);
            editKm = itemView.findViewById(R.id.editFuelKm);
            editKmFinal = itemView.findViewById(R.id.editFuelKmFinal);
            editDate = itemView.findViewById(R.id.editFuelDateInline);
            spinnerStation = itemView.findViewById(R.id.spinnerEditFuelStation);
            rgFuelType = itemView.findViewById(R.id.rgEditFuelType);
            btnSave = itemView.findViewById(R.id.btnSaveEditFuel);
            btnCancel = itemView.findViewById(R.id.btnCancelEditFuel);
        }
    }
}
