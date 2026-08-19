package com.example.drivelog;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.util.List;
import java.util.Map;

public class DevDetectionSettingsFragment extends Fragment {

    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dev_detection_settings, container, false);
        sharedPreferences = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);

        com.google.android.material.checkbox.MaterialCheckBox cbLocalOffer = view.findViewById(R.id.cbLocalOffer);
        com.google.android.material.checkbox.MaterialCheckBox cbGlobalOffer = view.findViewById(R.id.cbGlobalOffer);
        com.google.android.material.checkbox.MaterialCheckBox cbLocalNoOffer = view.findViewById(R.id.cbLocalNoOffer);
        com.google.android.material.checkbox.MaterialCheckBox cbGlobalNoOffer = view.findViewById(R.id.cbGlobalNoOffer);

        RadioGroup rgFilterMode = view.findViewById(R.id.rgScannerFilterMode);
        RadioButton rbNone = view.findViewById(R.id.rbFilterNone);
        RadioButton rbBlacklist = view.findViewById(R.id.rbFilterBlacklist);
        RadioButton rbWhitelist = view.findViewById(R.id.rbFilterWhitelist);

        MaterialSwitch switchScanAll = view.findViewById(R.id.switchScanAllApps);
        MaterialSwitch switchAutoClick = view.findViewById(R.id.switchAutoClickNextDay);
        MaterialSwitch switchLoop = view.findViewById(R.id.switchScannerLoop);
        MaterialSwitch switchWakeLock = view.findViewById(R.id.switchWakeLock);
        SeekBar seekBarWait = view.findViewById(R.id.seekbarScannerWait);
        TextView textWaitValue = view.findViewById(R.id.textScannerWaitValue);
        TextView textBatteryWarning = view.findViewById(R.id.textBatteryWarning);

        if (textBatteryWarning != null) {
            checkBatteryOptimization(textBatteryWarning);
            textBatteryWarning.setOnClickListener(v -> {
                try {
                    android.content.Intent intent = new android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    startActivity(intent);
                } catch (Exception e) {
                    android.content.Intent intent = new android.content.Intent(android.provider.Settings.ACTION_SETTINGS);
                    startActivity(intent);
                }
            });
        }

        if (cbLocalOffer != null) {
            cbLocalOffer.setChecked(sharedPreferences.getBoolean("notif_offer_local", true));
            cbLocalOffer.setOnCheckedChangeListener((btn, isChecked) -> sharedPreferences.edit().putBoolean("notif_offer_local", isChecked).apply());
        }
        if (cbGlobalOffer != null) {
            cbGlobalOffer.setChecked(sharedPreferences.getBoolean("notif_offer_global", true));
            cbGlobalOffer.setOnCheckedChangeListener((btn, isChecked) -> sharedPreferences.edit().putBoolean("notif_offer_global", isChecked).apply());
        }
        if (cbLocalNoOffer != null) {
            cbLocalNoOffer.setChecked(sharedPreferences.getBoolean("notif_no_offer_local", false));
            cbLocalNoOffer.setOnCheckedChangeListener((btn, isChecked) -> sharedPreferences.edit().putBoolean("notif_no_offer_local", isChecked).apply());
        }
        if (cbGlobalNoOffer != null) {
            cbGlobalNoOffer.setChecked(sharedPreferences.getBoolean("notif_no_offer_global", false));
            cbGlobalNoOffer.setOnCheckedChangeListener((btn, isChecked) -> sharedPreferences.edit().putBoolean("notif_no_offer_global", isChecked).apply());
        }

        if (switchScanAll != null) {
            switchScanAll.setChecked(sharedPreferences.getBoolean("scanner_scan_all_apps", false));
            switchScanAll.setOnCheckedChangeListener((btn, isChecked) -> sharedPreferences.edit().putBoolean("scanner_scan_all_apps", isChecked).apply());
        }

        if (switchAutoClick != null) {
            switchAutoClick.setChecked(sharedPreferences.getBoolean("scanner_auto_click_next_day", false));
            switchAutoClick.setOnCheckedChangeListener((btn, isChecked) -> sharedPreferences.edit().putBoolean("scanner_auto_click_next_day", isChecked).apply());
        }

        if (switchLoop != null) {
            switchLoop.setChecked(sharedPreferences.getBoolean("scanner_loop_enabled", true));
            switchLoop.setOnCheckedChangeListener((btn, isChecked) -> {
                sharedPreferences.edit().putBoolean("scanner_loop_enabled", isChecked).apply();
            });
        }

        if (switchWakeLock != null) {
            switchWakeLock.setChecked(sharedPreferences.getBoolean("scanner_wakelock_enabled", true));
            switchWakeLock.setOnCheckedChangeListener((btn, isChecked) -> {
                sharedPreferences.edit().putBoolean("scanner_wakelock_enabled", isChecked).apply();
            });
        }

        if (seekBarWait != null && textWaitValue != null) {
            int currentWait = sharedPreferences.getInt("scanner_loop_wait_time", 30);
            seekBarWait.setProgress(currentWait);
            textWaitValue.setText(currentWait + "s");

            seekBarWait.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    textWaitValue.setText(progress + "s");
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {
                    sharedPreferences.edit().putInt("scanner_loop_wait_time", seekBar.getProgress()).apply();
                }
            });
        }

        SeekBar seekBarAction = view.findViewById(R.id.seekbarActionDelay);
        TextView textActionValue = view.findViewById(R.id.textActionDelayValue);
        if (seekBarAction != null && textActionValue != null) {
            int currentDelayMs = sharedPreferences.getInt("scanner_action_delay", 2000); 
            
            seekBarAction.setMax(10); // 10 passos de 0.5s = 5s
            seekBarAction.setProgress(currentDelayMs / 500);
            textActionValue.setText((currentDelayMs / 1000.0) + "s");

            seekBarAction.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    textActionValue.setText((progress * 0.5) + "s");
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {
                    int finalDelayMs = seekBar.getProgress() * 500;
                    sharedPreferences.edit().putInt("scanner_action_delay", finalDelayMs).apply();
                }
            });
        }

        SeekBar seekBarAntiTrava = view.findViewById(R.id.seekbarAntiTrava);
        TextView textAntiTravaValue = view.findViewById(R.id.textAntiTravaValue);
        if (seekBarAntiTrava != null && textAntiTravaValue != null) {
            int currentTimeout = sharedPreferences.getInt("scanner_antitrava_timeout", 30);
            
            // Max 60s, Min 10s. SeekBar vai de 0 a 50 (+10)
            seekBarAntiTrava.setMax(50);
            seekBarAntiTrava.setProgress(currentTimeout - 10);
            textAntiTravaValue.setText(currentTimeout + "s");

            seekBarAntiTrava.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    textAntiTravaValue.setText((progress + 10) + "s");
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {
                    int finalTimeout = seekBar.getProgress() + 10;
                    sharedPreferences.edit().putInt("scanner_antitrava_timeout", finalTimeout).apply();
                }
            });
        }

        // 0: None, 1: Blacklist, 2: Whitelist
        int filterMode = sharedPreferences.getInt("scanner_filter_mode", 0);
        if (filterMode == 1) rbBlacklist.setChecked(true);
        else if (filterMode == 2) rbWhitelist.setChecked(true);
        else rbNone.setChecked(true);

        rgFilterMode.setOnCheckedChangeListener((group, checkedId) -> {
            int mode = 0;
            if (checkedId == R.id.rbFilterBlacklist) mode = 1;
            else if (checkedId == R.id.rbFilterWhitelist) mode = 2;
            sharedPreferences.edit().putInt("scanner_filter_mode", mode).apply();
        });

        View btnManageApps = view.findViewById(R.id.btnManageScannerApps);
        if (btnManageApps != null) {
            btnManageApps.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), ScannerAppManagerActivity.class);
                startActivity(intent);
            });
        }

        setupDevList(view);

        return view;
    }

    private void setupDevList(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.recyclerDevNotifications);
        if (recyclerView == null) return;

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        FirebaseHelper.fetchDeveloperList(new FirebaseHelper.DeveloperListCallback() {
            @Override
            public void onResult(List<String> emails, Map<String, Boolean> adminStatus) {
                if (isAdded()) {
                    recyclerView.setAdapter(new DevControlAdapter(emails));
                }
            }

            @Override
            public void onError(String msg) {}
        });
    }

    private class DevControlAdapter extends RecyclerView.Adapter<DevControlAdapter.ViewHolder> {
        private final List<String> emails;

        DevControlAdapter(List<String> emails) { this.emails = emails; }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_dev_notification_control, p, false));
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
            String email = emails.get(h.getBindingAdapterPosition());
            h.textEmail.setText(email);
            
            // 🔥 Busca nome na coleção de usuários se possível (opcional, para visual melhor)
            FirebaseHelper.fetchUserProfile(email, new FirebaseHelper.FriendProfileCallback() {
                @Override public void onResult(String name, String e, String u, String a, int l, int f, int r, boolean isFixed) {
                    if (h.getBindingAdapterPosition() == pos) h.textName.setText(name);
                }
                @Override public void onError(String msg) {
                    if (h.getBindingAdapterPosition() == pos) h.textName.setText("Desenvolvedor");
                }
            });

            String prefKeyOffer = "notif_dev_offer_" + email.replace(".", "_");
            String prefKeyNoOffer = "notif_dev_nooffer_" + email.replace(".", "_");

            h.cbOffer.setChecked(sharedPreferences.getBoolean(prefKeyOffer, true));
            h.cbNoOffer.setChecked(sharedPreferences.getBoolean(prefKeyNoOffer, true));

            h.cbOffer.setOnCheckedChangeListener((b, isChecked) -> sharedPreferences.edit().putBoolean(prefKeyOffer, isChecked).apply());
            h.cbNoOffer.setOnCheckedChangeListener((b, isChecked) -> sharedPreferences.edit().putBoolean(prefKeyNoOffer, isChecked).apply());
        }

        @Override public int getItemCount() { return emails.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textName, textEmail;
            com.google.android.material.checkbox.MaterialCheckBox cbOffer, cbNoOffer;
            ViewHolder(View v) {
                super(v);
                textName = v.findViewById(R.id.textDevName);
                textEmail = v.findViewById(R.id.textDevEmail);
                cbOffer = v.findViewById(R.id.cbNotifyOffer);
                cbNoOffer = v.findViewById(R.id.cbNotifyNoOffer);
            }
        }
    }

    private void checkBatteryOptimization(TextView warningView) {
        android.os.PowerManager pm = (android.os.PowerManager) requireContext().getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            boolean isIgnoring = pm.isIgnoringBatteryOptimizations(requireContext().getPackageName());
            warningView.setVisibility(isIgnoring ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        TextView textBatteryWarning = getView() != null ? getView().findViewById(R.id.textBatteryWarning) : null;
        if (textBatteryWarning != null) {
            checkBatteryOptimization(textBatteryWarning);
        }
    }
}
