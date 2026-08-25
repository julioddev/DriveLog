package com.example.drivelog;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FuelRegisterFragment extends Fragment {

    private TextInputEditText editValue, editPricePerLiter, editLiters, editFuelKm, editFuelKmFinal, editFuelKmDriven, editKmFinalInput;
    private TextInputLayout layoutPricePerLiter, layoutFuelLiters, layoutFuelKm, layoutEditKmDriven, layoutEditKmFinal;
    private LinearLayout layoutNewFuel, layoutFinishFuel;
    private TextView textLastFuelInfo;
    private Button btnSave, btnCancel, btnFinish;
    private RadioGroup rgFuelType, rgRegisterMode;
    private Spinner spinnerStation;
    private Fuel lastPendingFuel;
    private int editingFuelId = -1;
    private String lastSelectedStation = null;
    private List<GasStation> stationsList = new ArrayList<>();
    private boolean isRestoring = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fuel_register, container, false);

        editValue = view.findViewById(R.id.editFuelValue);
        editPricePerLiter = view.findViewById(R.id.editPricePerLiter);
        editLiters = view.findViewById(R.id.editFuelLiters);
        editFuelKm = view.findViewById(R.id.editFuelKm);
        editFuelKmFinal = view.findViewById(R.id.editFuelKmFinal);
        editFuelKmDriven = view.findViewById(R.id.editFuelKmDriven);
        layoutPricePerLiter = view.findViewById(R.id.layoutPricePerLiter);
        layoutFuelLiters = view.findViewById(R.id.layoutFuelLiters);
        layoutFuelKm = view.findViewById(R.id.layoutFuelKm);
        layoutEditKmFinal = view.findViewById(R.id.layoutEditKmFinal);
        layoutEditKmDriven = view.findViewById(R.id.layoutEditKmDriven);
        rgFuelType = view.findViewById(R.id.rgFuelType);
        rgRegisterMode = view.findViewById(R.id.rgRegisterMode);
        spinnerStation = view.findViewById(R.id.spinnerGasStation);
        btnSave = view.findViewById(R.id.btnSaveFuel);
        btnCancel = view.findViewById(R.id.btnCancelFuelEdit);

        layoutNewFuel = view.findViewById(R.id.layoutNewFuel);
        layoutFinishFuel = view.findViewById(R.id.layoutFinishFuel);
        textLastFuelInfo = view.findViewById(R.id.textLastFuelInfo);
        editKmFinalInput = view.findViewById(R.id.editKmFinalInput);
        btnFinish = view.findViewById(R.id.btnFinishFuel);

        rgRegisterMode.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isDetailed = (checkedId == R.id.rbModeDetailed);
            int visibility = isDetailed ? View.VISIBLE : View.GONE;
            layoutPricePerLiter.setVisibility(visibility);
            layoutFuelLiters.setVisibility(visibility);
            layoutFuelKm.setVisibility(visibility);
        });

        setupCalculationWatcher();
        setupKmCalculationWatcher();
        loadStations();
        checkPendingFuel();

        btnSave.setOnClickListener(v -> saveNewFuel());
        btnCancel.setOnClickListener(v -> cancelEdit());
        btnFinish.setOnClickListener(v -> finishPendingFuel());

        if (savedInstanceState != null) {
            isRestoring = true;
            editingFuelId = savedInstanceState.getInt("editing_id", -1);
            lastSelectedStation = savedInstanceState.getString("last_station", null);
            
            // Restaura textos
            editValue.setText(savedInstanceState.getString("draft_val", ""));
            editPricePerLiter.setText(savedInstanceState.getString("draft_price", ""));
            editLiters.setText(savedInstanceState.getString("draft_liters", ""));
            editFuelKm.setText(savedInstanceState.getString("draft_km", ""));
            editFuelKmFinal.setText(savedInstanceState.getString("draft_km_final", ""));
            editKmFinalInput.setText(savedInstanceState.getString("draft_km_pending", ""));

            if (editingFuelId != -1) {
                btnSave.setText("Atualizar Abastecimento");
                btnCancel.setVisibility(View.VISIBLE);
            }
            isRestoring = false;
        }

        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("editing_id", editingFuelId);
        outState.putString("last_station", lastSelectedStation);
        
        // Salva rascunhos de texto
        if (editValue != null) outState.putString("draft_val", editValue.getText().toString());
        if (editPricePerLiter != null) outState.putString("draft_price", editPricePerLiter.getText().toString());
        if (editLiters != null) outState.putString("draft_liters", editLiters.getText().toString());
        if (editFuelKm != null) outState.putString("draft_km", editFuelKm.getText().toString());
        if (editFuelKmFinal != null) outState.putString("draft_km_final", editFuelKmFinal.getText().toString());
        if (editKmFinalInput != null) outState.putString("draft_km_pending", editKmFinalInput.getText().toString());
    }

    private void loadStations() {
        AppDatabase.getInstance(getContext()).appDao().getAllGasStationsLive().observe(getViewLifecycleOwner(), stations -> {
            stationsList = stations;
            updateSpinner();
        });
    }

    private void updateSpinner() {
        List<String> names = new ArrayList<>();
        names.add("Selecionar Posto...");
        for (GasStation s : stationsList) names.add(s.name);
        names.add("+ Novo Posto");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStation.setAdapter(adapter);

        if (lastSelectedStation != null) {
            int pos = names.indexOf(lastSelectedStation);
            if (pos != -1) spinnerStation.setSelection(pos);
        }

        spinnerStation.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position == names.size() - 1) {
                    promptNewStation();
                } else if (position > 0) {
                    lastSelectedStation = names.get(position);
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void promptNewStation() {
        android.widget.EditText input = new android.widget.EditText(getContext());
        input.setHint("Nome do Posto");
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Novo Posto")
                .setView(input)
                .setPositiveButton("Adicionar", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        new Thread(() -> {
                            AppDatabase.getInstance(getContext()).appDao().insertGasStation(new GasStation(name, 0));
                            requireActivity().runOnUiThread(() -> lastSelectedStation = name);
                        }).start();
                    }
                })
                .setNegativeButton("Cancelar", (d, w) -> spinnerStation.setSelection(0))
                .show();
    }

    private void setupCalculationWatcher() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { calculateLiters(); }
        };
        editValue.addTextChangedListener(watcher);
        editPricePerLiter.addTextChangedListener(watcher);
    }

    private void calculateLiters() {
        double val = parseDouble(editValue.getText().toString());
        double price = parseDouble(editPricePerLiter.getText().toString());
        if (price > 0) {
            double lit = val / price;
            editLiters.setText(String.format(Locale.getDefault(), "%.2f", lit));
        } else {
            editLiters.setText("");
        }
    }

    private void setupKmCalculationWatcher() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { calculateKmDriven(); }
        };
        editFuelKm.addTextChangedListener(watcher);
        editFuelKmFinal.addTextChangedListener(watcher);
    }

    private void calculateKmDriven() {
        try {
            int start = Integer.parseInt(editFuelKm.getText().toString());
            int end = Integer.parseInt(editFuelKmFinal.getText().toString());
            if (end >= start) {
                editFuelKmDriven.setText(String.format(Locale.getDefault(), "%.1f", (double)(end - start)));
            } else {
                editFuelKmDriven.setText("");
            }
        } catch (Exception e) {
            editFuelKmDriven.setText("");
        }
    }

    private void checkPendingFuel() {
        new Thread(() -> {
            AppDao dao = AppDatabase.getInstance(getContext()).appDao();
            lastPendingFuel = dao.getLastFuel();
            
            if (lastPendingFuel != null && !lastPendingFuel.isCompleted) {
                // Cálculo de autonomia média
                List<Fuel> all = dao.getAllFuel();
                double totalKm = 0;
                double totalLiters = 0;
                int count = 0;
                for (Fuel f : all) {
                    if (f.isCompleted && f.kmDriven > 0 && f.liters > 0) {
                        totalKm += f.kmDriven;
                        totalLiters += f.liters;
                        count++;
                    }
                }
                
                final double avgAll = count > 0 ? totalKm / totalLiters : 0;
                
                // Média por posto
                List<Fuel> byStation = new ArrayList<>();
                for(Fuel f : all) if(f.isCompleted && f.gasStation.equals(lastPendingFuel.gasStation)) byStation.add(f);
                
                double sKm = 0, sLit = 0;
                for(Fuel f : byStation) { sKm += f.kmDriven; sLit += f.liters; }
                final double avgStation = !byStation.isEmpty() ? sKm / sLit : 0;
                
                requireActivity().runOnUiThread(() -> {
                    layoutNewFuel.setVisibility(View.GONE);
                    layoutFinishFuel.setVisibility(View.VISIBLE);
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format(Locale.getDefault(), "Você abasteceu %.2f litros no posto %s.", 
                        lastPendingFuel.liters, lastPendingFuel.gasStation));
                    
                    double finalAvg = avgStation > 0 ? avgStation : avgAll;
                    String finalSource = avgStation > 0 ? "• Baseado no histórico deste posto." : "• Baseado na média geral.";

                    if (finalAvg > 0) {
                        double autonomia = lastPendingFuel.liters * finalAvg;
                        double kmEsperado = lastPendingFuel.km + autonomia;
                        
                        sb.append("\n\nESTIMATIVA DE AUTONOMIA:");
                        sb.append(String.format(Locale.getDefault(), "\n• Deve rodar: %.1f KM", autonomia));
                        sb.append(String.format(Locale.getDefault(), "\n• KM Final esperado: %.0f KM", kmEsperado));
                        sb.append("\n").append(finalSource);
                        sb.append(String.format(Locale.getDefault(), "\n• Média de referência: %.2f KM/L", finalAvg));
                    }
                    
                    textLastFuelInfo.setText(sb.toString());
                });
            } else {
                if (getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        layoutNewFuel.setVisibility(View.VISIBLE);
                        layoutFinishFuel.setVisibility(View.GONE);
                    });
                }
            }
        }).start();
    }

    private void saveNewFuel() {
        String valStr = editValue.getText().toString();
        if (valStr.isEmpty()) {
            Toast.makeText(getContext(), "Informe o valor", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isDetailed = rgRegisterMode.getCheckedRadioButtonId() == R.id.rbModeDetailed;
        String priceStr = editPricePerLiter.getText().toString();
        String litStr = editLiters.getText().toString();
        String kmStr = editFuelKm.getText().toString();

        if (isDetailed && (litStr.isEmpty() || kmStr.isEmpty())) {
            Toast.makeText(getContext(), "Preencha todos os campos do modo Detalhado", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            AppDao dao = AppDatabase.getInstance(getContext()).appDao();
            Fuel fuel;
            if (editingFuelId != -1) {
                fuel = dao.getAllFuel().stream()
                        .filter(f -> f.id == editingFuelId).findFirst().orElse(new Fuel());
            } else {
                fuel = new Fuel();
                fuel.date = System.currentTimeMillis();
            }

            fuel.value = parseDouble(valStr);
            fuel.pricePerLiter = isDetailed ? parseDouble(priceStr) : 0.0;
            fuel.liters = isDetailed ? parseDouble(litStr) : 0.0;
            fuel.km = isDetailed ? (int) parseDouble(kmStr) : 0;
            fuel.fuelType = rgFuelType.getCheckedRadioButtonId() == R.id.rbGasComum ? "Comum" : "Aditivada";
            fuel.gasStation = spinnerStation.getSelectedItem() != null ? spinnerStation.getSelectedItem().toString() : "";
            
            // Se for SIMPLES, já marca como completado para não pedir KM final depois
            fuel.isCompleted = !isDetailed;

            if (isDetailed && layoutEditKmFinal.getVisibility() == View.VISIBLE) {
                String kmFinalStr = editFuelKmFinal.getText().toString();
                if (!kmFinalStr.isEmpty()) {
                    int kmFinal = Integer.parseInt(kmFinalStr);
                    fuel.kmDriven = kmFinal - fuel.km;
                    fuel.isCompleted = true;
                }
            }

            if (editingFuelId == -1) {
                dao.insertFuel(fuel);
            } else {
                dao.updateFuel(fuel);
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Abastecimento salvo!", Toast.LENGTH_SHORT).show();
                    cancelEdit();
                    checkPendingFuel();
                    CloudSyncHelper.syncNow(requireContext(), editingFuelId == -1 ? "Novo Abastecimento" : "Abastecimento Editado");
                });
            }
        }).start();
    }

    private void finishPendingFuel() {
        String kmFinalStr = editKmFinalInput.getText().toString();
        if (!kmFinalStr.isEmpty()) {
            int kmFinal = Integer.parseInt(kmFinalStr);
            if (kmFinal < lastPendingFuel.km) {
                Toast.makeText(getContext(), "KM Final não pode ser menor que o Inicial", Toast.LENGTH_SHORT).show();
                return;
            }
            
            lastPendingFuel.kmDriven = kmFinal - lastPendingFuel.km;
            lastPendingFuel.isCompleted = true;

            new Thread(() -> {
                AppDatabase.getInstance(getContext()).appDao().updateFuel(lastPendingFuel);
                requireActivity().runOnUiThread(() -> {
                    double consumption = lastPendingFuel.kmDriven / lastPendingFuel.liters;
                    Toast.makeText(getContext(), String.format(Locale.getDefault(), 
                        "Finalizado! Rodou: %.1f KM. Consumo: %.2f KM/L", lastPendingFuel.kmDriven, consumption), Toast.LENGTH_LONG).show();
                    editKmFinalInput.setText("");
                    checkPendingFuel();
                    CloudSyncHelper.syncNow(requireContext());
                });
            }).start();
        } else {
            Toast.makeText(getContext(), "Informe o KM Final", Toast.LENGTH_SHORT).show();
        }
    }

    private void cancelEdit() {
        editingFuelId = -1;
        lastSelectedStation = null;
        editValue.setText("");
        editPricePerLiter.setText("");
        editLiters.setText("");
        editFuelKm.setText("");
        editFuelKmFinal.setText("");
        editFuelKmDriven.setText("");
        rgFuelType.check(R.id.rbGasAditivada);
        if (rgRegisterMode != null) rgRegisterMode.check(R.id.rbModeDetailed);
        layoutEditKmFinal.setVisibility(View.GONE);
        layoutEditKmDriven.setVisibility(View.GONE);
        btnSave.setText("Salvar Abastecimento");
        btnCancel.setVisibility(View.GONE);
        checkPendingFuel();
        updateSpinner();
    }

    private double parseDouble(String value) {
        if (value == null || value.isEmpty()) return 0;
        try {
            return Double.parseDouble(value.replace(",", "."));
        } catch (Exception e) { return 0; }
    }

    public void startEdit(Fuel fuel) {
        editingFuelId = fuel.id;
        lastSelectedStation = fuel.gasStation;
        if (getView() != null) {
            layoutNewFuel.setVisibility(View.VISIBLE);
            layoutFinishFuel.setVisibility(View.GONE);

            editValue.setText(String.format(Locale.getDefault(), "%.2f", fuel.value));
            editPricePerLiter.setText(String.format(Locale.getDefault(), "%.2f", fuel.pricePerLiter));
            editLiters.setText(String.format(Locale.getDefault(), "%.2f", fuel.liters));
            editFuelKm.setText(String.valueOf(fuel.km));
            
            rgFuelType.check("Comum".equals(fuel.fuelType) ? R.id.rbGasComum : R.id.rbGasAditivada);
            
            // Define o modo baseado se temos dados detalhados
            if (fuel.liters > 0 || fuel.km > 0) {
                rgRegisterMode.check(R.id.rbModeDetailed);
            } else {
                rgRegisterMode.check(R.id.rbModeSimple);
            }

            updateSpinner();

            if (fuel.isCompleted) {
                layoutEditKmFinal.setVisibility(View.VISIBLE);
                layoutEditKmDriven.setVisibility(View.VISIBLE);
                editFuelKmFinal.setText(String.valueOf(fuel.km + (int)fuel.kmDriven));
                editFuelKmDriven.setText(String.format(Locale.getDefault(), "%.1f", fuel.kmDriven));
            } else {
                layoutEditKmFinal.setVisibility(View.GONE);
                layoutEditKmDriven.setVisibility(View.GONE);
            }

            btnSave.setText("Atualizar Abastecimento");
            btnCancel.setVisibility(View.VISIBLE);
        }
    }
}
