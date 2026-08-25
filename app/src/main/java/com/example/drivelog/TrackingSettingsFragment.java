package com.example.drivelog;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import com.google.android.material.materialswitch.MaterialSwitch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

public class TrackingSettingsFragment extends Fragment {

    private SharedPreferences sharedPreferences;
    
    // UI Elements
    private RadioGroup rgTrackingMode, rgComboioMode;
    private MaterialSwitch switchBackgroundTracking;
    private LinearLayout layoutAutoTime, layoutAutoDistance, layoutDevComboioSettings;
    private TextInputEditText editStart, editEnd, editHomeTriggerRadius, editHomeArrivalRadius, editHomeArrivalTime, editMinStopDuration;
    private TextInputEditText editShortTime, editShortRadius, editMediumTime, editMediumRadius, editLongRadius;
    private TextInputEditText editLoadingRadius, editLoadingTime;
    
    private View viewColorShort, viewColorRouteLine;
    private View viewColorMedium;
    private View viewColorLong;

    // Preference Keys
    public static final String PREF_TRACKING_MODE = "tracking_mode_v2"; // 0: Manual, 1: Tempo, 2: Distância
    public static final String PREF_HOME_TRIGGER_RADIUS = "home_trigger_radius";
    public static final String PREF_ROUTE_LINE_COLOR = "tracking_route_line_color";
    
    public static final String PREF_SHORT_TIME = "tracking_short_stop_time";
    public static final String PREF_SHORT_RADIUS = "tracking_short_stop_radius";
    public static final String PREF_MEDIUM_TIME = "tracking_medium_stop_time";
    public static final String PREF_MEDIUM_RADIUS = "tracking_medium_stop_radius";
    public static final String PREF_LONG_RADIUS = "tracking_long_stop_radius";
    
    public static final String PREF_COLOR_SHORT = "tracking_color_short";
    public static final String PREF_COLOR_MEDIUM = "tracking_color_medium";
    public static final String PREF_COLOR_LONG = "tracking_color_long";

    private static final String DEFAULT_SHORT = "#4CAF50";
    private static final String DEFAULT_MEDIUM = "#FBC02D";
    private static final String DEFAULT_LONG = "#F44336";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tracking_settings, container, false);
        sharedPreferences = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);

        // Mode & Auto Time & Auto Distance
        rgTrackingMode = view.findViewById(R.id.rgTrackingMode);
        rgComboioMode = view.findViewById(R.id.rgComboioMode);
        layoutDevComboioSettings = view.findViewById(R.id.layoutDevComboioSettings);
        switchBackgroundTracking = view.findViewById(R.id.switchBackgroundTracking);
        layoutAutoTime = view.findViewById(R.id.layoutAutoTrackingTime);
        layoutAutoDistance = view.findViewById(R.id.layoutAutoTrackingDistance);
        editStart = view.findViewById(R.id.editTrackingStart);
        editEnd = view.findViewById(R.id.editTrackingEnd);
        editHomeTriggerRadius = view.findViewById(R.id.editHomeTriggerRadius);
        editHomeArrivalRadius = view.findViewById(R.id.editHomeArrivalRadius);
        editHomeArrivalTime = view.findViewById(R.id.editHomeArrivalTime);
        editMinStopDuration = view.findViewById(R.id.editMinStopDuration);
        
        editShortTime = view.findViewById(R.id.editShortTime);
        editShortRadius = view.findViewById(R.id.editShortRadius);
        editMediumTime = view.findViewById(R.id.editMediumTime);
        editMediumRadius = view.findViewById(R.id.editMediumRadius);
        editLongRadius = view.findViewById(R.id.editLongRadius);

        editLoadingRadius = view.findViewById(R.id.editLoadingRadius);
        editLoadingTime = view.findViewById(R.id.editLoadingTime);

        // Colors
        viewColorShort = view.findViewById(R.id.viewColorShort);
        viewColorMedium = view.findViewById(R.id.viewColorMedium);
        viewColorLong = view.findViewById(R.id.viewColorLong);
        viewColorRouteLine = view.findViewById(R.id.viewColorRouteLine);

        checkDevAccess();
        setupListeners();
        loadSettings();

        view.findViewById(R.id.layoutColorShort).setOnClickListener(v -> showColorPicker(PREF_COLOR_SHORT, DEFAULT_SHORT));
        view.findViewById(R.id.layoutColorMedium).setOnClickListener(v -> showColorPicker(PREF_COLOR_MEDIUM, DEFAULT_MEDIUM));
        view.findViewById(R.id.layoutColorLong).setOnClickListener(v -> showColorPicker(PREF_COLOR_LONG, DEFAULT_LONG));
        view.findViewById(R.id.layoutColorRouteLine).setOnClickListener(v -> showColorPicker(PREF_ROUTE_LINE_COLOR, "#2196F3"));

        return view;
    }

    private void checkDevAccess() {
        if (layoutDevComboioSettings == null) return;
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            FirebaseHelper.checkDeveloperAccess(user.getEmail(), isDev -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        layoutDevComboioSettings.setVisibility(isDev ? View.VISIBLE : View.GONE);
                    });
                }
            });
        } else {
            layoutDevComboioSettings.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        // Mode
        rgTrackingMode.setOnCheckedChangeListener((group, checkedId) -> {
            int mode = 0;
            if (checkedId == R.id.rbAutoTracking) mode = 1;
            else if (checkedId == R.id.rbDistanceTracking) mode = 2;
            
            layoutAutoTime.setVisibility(mode == 1 ? View.VISIBLE : View.GONE);
            layoutAutoDistance.setVisibility(mode == 2 ? View.VISIBLE : View.GONE);
            
            sharedPreferences.edit()
                .putInt(PREF_TRACKING_MODE, mode)
                .putBoolean("tracking_auto", mode == 1) 
                .putBoolean("home_tracking_enabled", mode == 2)
                .apply();
                
            if (mode != 0) {
                sharedPreferences.edit().putInt("last_auto_mode_v2", mode).apply();
            }
                
            TrackingHelper.updateAutoTracking(requireContext());
        });

        switchBackgroundTracking.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("background_tracking_enabled", isChecked).apply();
            CloudSyncHelper.syncNow(requireContext(), "Ajuste Rastreamento");
        });

        rgComboioMode.setOnCheckedChangeListener((group, checkedId) -> {
            int mode = 0; // 0: Targeted, 1: All, 2: Disabled
            if (checkedId == R.id.rbComboioAll) mode = 1;
            else if (checkedId == R.id.rbComboioDisabled) mode = 2;
            
            sharedPreferences.edit().putInt("comboio_global_mode", mode).apply();
            
            // 🔥 Sincroniza com o Firebase para os amigos saberem se podem te ver
            com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getEmail() != null) {
                FirebaseHelper.updateComboioPreference(user.getEmail(), mode);
            }

            CloudSyncHelper.syncNow(requireContext(), "Ajuste Rastreamento");
            updateComboioHint(mode);
        });

        editStart.setOnClickListener(v -> showTimePicker(editStart, "tracking_start"));
        editEnd.setOnClickListener(v -> showTimePicker(editEnd, "tracking_end"));
        
        // Settings Watcher
        android.text.TextWatcher watcher = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                saveAllTrackingSettings();
            }
        };
        
        editHomeTriggerRadius.addTextChangedListener(watcher);
        editHomeArrivalRadius.addTextChangedListener(watcher);
        editHomeArrivalTime.addTextChangedListener(watcher);
        editMinStopDuration.addTextChangedListener(watcher);
        
        editShortTime.addTextChangedListener(watcher);
        editShortRadius.addTextChangedListener(watcher);
        editMediumTime.addTextChangedListener(watcher);
        editMediumRadius.addTextChangedListener(watcher);
        editLongRadius.addTextChangedListener(watcher);
        editLoadingRadius.addTextChangedListener(watcher);
        editLoadingTime.addTextChangedListener(watcher);
    }

    private void saveAllTrackingSettings() {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            
            editor.putInt(PREF_HOME_TRIGGER_RADIUS, Integer.parseInt(editHomeTriggerRadius.getText().toString()));
            editor.putInt("home_arrival_radius", Integer.parseInt(editHomeArrivalRadius.getText().toString()));
            editor.putInt("home_arrival_time", Integer.parseInt(editHomeArrivalTime.getText().toString()));
            editor.putInt("min_stop_duration_seconds", Integer.parseInt(editMinStopDuration.getText().toString()));
            
            editor.putInt(PREF_SHORT_TIME, Integer.parseInt(editShortTime.getText().toString()));
            editor.putInt(PREF_SHORT_RADIUS, Integer.parseInt(editShortRadius.getText().toString()));
            editor.putInt(PREF_MEDIUM_TIME, Integer.parseInt(editMediumTime.getText().toString()));
            editor.putInt(PREF_MEDIUM_RADIUS, Integer.parseInt(editMediumRadius.getText().toString()));
            editor.putInt(PREF_LONG_RADIUS, Integer.parseInt(editLongRadius.getText().toString()));
            
            editor.putInt("loading_base_radius", Integer.parseInt(editLoadingRadius.getText().toString()));
            editor.putInt("loading_base_time", Integer.parseInt(editLoadingTime.getText().toString()));

            editor.commit(); // Usamos commit para persistência imediata antes do sync
            
            CloudSyncHelper.syncNow(requireContext(), "Ajuste Rastreamento");
            TrackingHelper.updateAutoTracking(requireContext());
        } catch (Exception ignored) {}
    }

    private void showTimePicker(TextInputEditText target, String prefKey) {
        String currentTime = target.getText() != null ? target.getText().toString() : "";
        int hour = 8, minute = 0;
        if (!currentTime.isEmpty()) {
            try {
                String[] parts = currentTime.split(":");
                hour = Integer.parseInt(parts[0]);
                minute = Integer.parseInt(parts[1]);
            } catch (Exception ignored) {}
        }

        new TimePickerDialog(getContext(), (view, hourOfDay, min) -> {
            String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, min);
            target.setText(time);
            sharedPreferences.edit().putString(prefKey, time).apply();
            CloudSyncHelper.syncNow(requireContext(), "Ajuste Rastreamento");
            TrackingHelper.updateAutoTracking(requireContext());
        }, hour, minute, true).show();
    }

    private void updateComboioHint(int mode) {
        if (getView() == null) return;
        android.widget.TextView hint = getView().findViewById(R.id.textComboioHint);
        if (hint == null) return;

        if (mode == 0) hint.setText("Modo Direcionado: Você ativa o sinal individualmente no perfil de cada amigo.");
        else if (mode == 1) hint.setText("Modo Todos: Qualquer amigo seu pode te ver no mapa enquanto você trabalha.");
        else hint.setText("Modo Desativado: Você está invisível para todos os amigos.");
    }

    private void loadSettings() {
        // Mode
        int mode = 0;
        try {
            mode = sharedPreferences.getInt(PREF_TRACKING_MODE, 0);
        } catch (Exception e) {
            Object val = sharedPreferences.getAll().get(PREF_TRACKING_MODE);
            if (val != null) {
                try { mode = (int) Double.parseDouble(String.valueOf(val)); } catch (Exception ignored) {}
            }
            sharedPreferences.edit().putInt(PREF_TRACKING_MODE, mode).apply();
        }
        
        if (mode == 0) rgTrackingMode.check(R.id.rbManualTracking);
        else if (mode == 1) rgTrackingMode.check(R.id.rbAutoTracking);
        else rgTrackingMode.check(R.id.rbDistanceTracking);
        
        layoutAutoTime.setVisibility(mode == 1 ? View.VISIBLE : View.GONE);
        layoutAutoDistance.setVisibility(mode == 2 ? View.VISIBLE : View.GONE);
        
        switchBackgroundTracking.setChecked(sharedPreferences.getBoolean("background_tracking_enabled", true));
        
        // 🔥 PADRÃO: Modo Comboio inicia DESATIVADO (2) ao instalar
        int cMode = sharedPreferences.getInt("comboio_global_mode", 2);
        if (cMode == 0) rgComboioMode.check(R.id.rbComboioTargeted);
        else if (cMode == 1) rgComboioMode.check(R.id.rbComboioAll);
        else rgComboioMode.check(R.id.rbComboioDisabled);
        updateComboioHint(cMode);

        editStart.setText(sharedPreferences.getString("tracking_start", "08:00"));
        editEnd.setText(sharedPreferences.getString("tracking_end", "18:00"));

        editHomeTriggerRadius.setText(String.valueOf(getIntSafe(PREF_HOME_TRIGGER_RADIUS, 100)));
        editHomeArrivalRadius.setText(String.valueOf(getIntSafe("home_arrival_radius", 50)));
        editHomeArrivalTime.setText(String.valueOf(getIntSafe("home_arrival_time", 5)));
        editMinStopDuration.setText(String.valueOf(getIntSafe("min_stop_duration_seconds", 15)));

        // Params
        editShortTime.setText(String.valueOf(getIntSafe(PREF_SHORT_TIME, 60)));
        editShortRadius.setText(String.valueOf(getIntSafe(PREF_SHORT_RADIUS, 20)));
        
        editMediumTime.setText(String.valueOf(getIntSafe(PREF_MEDIUM_TIME, 240)));
        editMediumRadius.setText(String.valueOf(getIntSafe(PREF_MEDIUM_RADIUS, 40)));
        
        editLongRadius.setText(String.valueOf(getIntSafe(PREF_LONG_RADIUS, 80)));
        
        editLoadingRadius.setText(String.valueOf(getIntSafe("loading_base_radius", 100)));
        editLoadingTime.setText(String.valueOf(getIntSafe("loading_base_time", 5)));
        
        updatePreviewColors();
    }

    private int getIntSafe(String key, int def) {
        if (getContext() == null) return def;
        try {
            return sharedPreferences.getInt(key, def);
        } catch (Exception e) {
            Object val = sharedPreferences.getAll().get(key);
            if (val != null) {
                try {
                    String s = String.valueOf(val);
                    int result;
                    if (s.equalsIgnoreCase("true")) result = 1;
                    else if (s.equalsIgnoreCase("false")) result = 0;
                    else result = (int) Double.parseDouble(s);

                    sharedPreferences.edit().putInt(key, result).apply();
                    return result;
                } catch (Exception ignored) {}
            }
            return def;
        }
    }

    private String formatTime(int seconds) {
        if (seconds < 60) return seconds + "s";
        int mins = seconds / 60;
        int secs = seconds % 60;
        if (secs == 0) return mins + "min";
        return mins + "m " + secs + "s";
    }

    private void updatePreviewColors() {
        setCircleColor(viewColorShort, sharedPreferences.getString(PREF_COLOR_SHORT, DEFAULT_SHORT));
        setCircleColor(viewColorMedium, sharedPreferences.getString(PREF_COLOR_MEDIUM, DEFAULT_MEDIUM));
        setCircleColor(viewColorLong, sharedPreferences.getString(PREF_COLOR_LONG, DEFAULT_LONG));
        setCircleColor(viewColorRouteLine, sharedPreferences.getString(PREF_ROUTE_LINE_COLOR, "#2196F3"));
    }

    private void setCircleColor(View v, String hex) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(Color.parseColor(hex));
        v.setBackground(shape);
    }

    private void showColorPicker(String key, String def) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_color_picker, null);
        View preview = dialogView.findViewById(R.id.viewColorPreview);
        SeekBar seekHue = dialogView.findViewById(R.id.seekHue);
        SeekBar seekSat = dialogView.findViewById(R.id.seekSaturation);
        SeekBar seekVal = dialogView.findViewById(R.id.seekValue);
        android.widget.EditText editHex = dialogView.findViewById(R.id.editColorHex);

        String currentColor = sharedPreferences.getString(key, def);
        float[] hsv = new float[3];
        Color.colorToHSV(Color.parseColor(currentColor), hsv);
        final boolean[] isUpdating = {false};

        seekHue.setProgress((int) hsv[0]);
        seekSat.setProgress((int) (hsv[1] * 100));
        seekVal.setProgress((int) (hsv[2] * 100));

        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (isUpdating[0]) return;
                hsv[0] = seekHue.getProgress();
                hsv[1] = seekSat.getProgress() / 100f;
                hsv[2] = seekVal.getProgress() / 100f;
                int color = Color.HSVToColor(hsv);
                String hex = String.format("#%06X", (0xFFFFFF & color));
                setCircleColor(preview, hex);
                editHex.setText(hex);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        seekHue.setOnSeekBarChangeListener(listener);
        seekSat.setOnSeekBarChangeListener(listener);
        seekVal.setOnSeekBarChangeListener(listener);

        editHex.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                if (s.length() == 7 && s.toString().startsWith("#")) {
                    try {
                        int color = Color.parseColor(s.toString());
                        isUpdating[0] = true;
                        Color.colorToHSV(color, hsv);
                        seekHue.setProgress((int) hsv[0]);
                        seekSat.setProgress((int) (hsv[1] * 100));
                        seekVal.setProgress((int) (hsv[2] * 100));
                        setCircleColor(preview, s.toString());
                        isUpdating[0] = false;
                    } catch (Exception ignored) {}
                }
            }
        });
        
        // Init preview
        int initialColor = Color.HSVToColor(hsv);
        String initialHex = String.format("#%06X", (0xFFFFFF & initialColor));
        setCircleColor(preview, initialHex);
        editHex.setText(initialHex);

        new AlertDialog.Builder(requireContext())
                .setTitle("Personalizar Cor")
                .setView(dialogView)
                .setPositiveButton("Salvar", (dialog, which) -> {
                    int finalColor = Color.HSVToColor(hsv);
                    String hex = String.format("#%06X", (0xFFFFFF & finalColor));
                    sharedPreferences.edit().putString(key, hex).apply();
                    updatePreviewColors();
                    CloudSyncHelper.syncNow(requireContext(), "Ajuste Rastreamento");
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private abstract static class SimpleSeekBarListener implements SeekBar.OnSeekBarChangeListener {
        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override public void onStopTrackingTouch(SeekBar seekBar) {}
    }
}
