package com.example.drivelog;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.textfield.TextInputEditText;
import java.util.Locale;

public class FuelCalculatorFragment extends Fragment {

    private TextInputEditText editKm, editConsumption, editPrice;
    private RadioGroup rgSource;
    private TextView textResult, textDetails;
    private SharedPreferences sharedPreferences;

    private double lastConsumption = 0;
    private double lastPrice = 0;
    private float defaultConsumption = 10.0f;
    private float defaultPrice = 5.50f;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fuel_calculator, container, false);

        editKm = view.findViewById(R.id.editCalcKm);
        editConsumption = view.findViewById(R.id.editCalcConsumption);
        editPrice = view.findViewById(R.id.editCalcPrice);
        rgSource = view.findViewById(R.id.rgCalcSource);
        textResult = view.findViewById(R.id.textCalcResult);
        textDetails = view.findViewById(R.id.textCalcDetails);

        sharedPreferences = requireActivity().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);

        loadData();
        setupListeners();

        return view;
    }

    private void loadData() {
        // Carrega valores padrão dos ajustes
        defaultConsumption = sharedPreferences.getFloat("default_consumption", 10.0f);
        defaultPrice = sharedPreferences.getFloat("default_fuel_price", 5.50f);

        // Carrega valores do último abastecimento via Thread
        new Thread(() -> {
            Fuel last = AppDatabase.getInstance(getContext()).appDao().getLastCompletedFuel();
            if (last != null && last.liters > 0) {
                lastConsumption = last.kmDriven / last.liters;
                lastPrice = last.pricePerLiter;
            } else {
                lastConsumption = defaultConsumption;
                lastPrice = defaultPrice;
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(this::applySourceSelection);
            }
        }).start();
    }

    private void setupListeners() {
        rgSource.setOnCheckedChangeListener((group, checkedId) -> applySourceSelection());

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { calculate(); }
            @Override public void afterTextChanged(Editable s) {}
        };

        editKm.addTextChangedListener(watcher);
        editConsumption.addTextChangedListener(watcher);
        editPrice.addTextChangedListener(watcher);

        // Implementação do "clicar para apagar tudo"
        View.OnClickListener clearListener = v -> {
            if (v instanceof TextInputEditText) {
                ((TextInputEditText) v).setText("");
            }
        };
        editConsumption.setOnClickListener(clearListener);
        editPrice.setOnClickListener(clearListener);
    }

    private void applySourceSelection() {
        if (rgSource.getCheckedRadioButtonId() == R.id.rbSourceLast) {
            editConsumption.setText(String.format(Locale.getDefault(), "%.2f", lastConsumption));
            editPrice.setText(String.format(Locale.getDefault(), "%.2f", lastPrice));
            editConsumption.setEnabled(false);
            editPrice.setEnabled(false);
            editConsumption.setAlpha(0.6f);
            editPrice.setAlpha(0.6f);
        } else {
            editConsumption.setText(String.format(Locale.getDefault(), "%.2f", (double)defaultConsumption));
            editPrice.setText(String.format(Locale.getDefault(), "%.2f", (double)defaultPrice));
            editConsumption.setEnabled(true);
            editPrice.setEnabled(true);
            editConsumption.setAlpha(1.0f);
            editPrice.setAlpha(1.0f);
        }
        calculate();
    }

    private void calculate() {
        try {
            double km = parseDouble(editKm.getText().toString());
            double cons = parseDouble(editConsumption.getText().toString());
            double price = parseDouble(editPrice.getText().toString());

            if (km > 0 && cons > 0 && price > 0) {
                double liters = km / cons;
                double total = liters * price;

                textResult.setText(String.format(Locale.getDefault(), "R$ %.2f", total));
                textDetails.setText(String.format(Locale.getDefault(), "Consumo: %.2f L necessários para a viagem", liters));
            } else {
                textResult.setText("R$ 0,00");
                textDetails.setText("Informe a distância acima");
            }
        } catch (Exception e) {
            textResult.setText("R$ 0,00");
        }
    }

    private double parseDouble(String val) {
        if (val == null || val.isEmpty()) return 0;
        try {
            return Double.parseDouble(val.replace(",", "."));
        } catch (Exception e) { return 0; }
    }
}
