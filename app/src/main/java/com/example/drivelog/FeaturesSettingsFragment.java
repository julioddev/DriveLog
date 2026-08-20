package com.example.drivelog;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.materialswitch.MaterialSwitch;

public class FeaturesSettingsFragment extends Fragment {

    private MaterialSwitch switchEarnings, switchKm, switchFuel, switchMaintenance, switchMaps, switchRouteDetection, switchFloatingIcon;
    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_features_settings, container, false);

        switchEarnings = view.findViewById(R.id.switchTabEarnings);
        switchKm = view.findViewById(R.id.switchTabKm);
        switchFuel = view.findViewById(R.id.switchTabFuel);
        switchMaintenance = view.findViewById(R.id.switchTabMaintenance);
        switchMaps = view.findViewById(R.id.switchTabMaps);
        switchRouteDetection = view.findViewById(R.id.switchTabRouteDetection);
        switchFloatingIcon = view.findViewById(R.id.switchFloatingIcon);
        
        Button btnSave = view.findViewById(R.id.btnSaveFeatures);

        sharedPreferences = requireActivity().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);

        checkDeveloperAccess();
        loadSettings();

        btnSave.setOnClickListener(v -> saveSettings());

        switchFloatingIcon.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(requireContext())) {
                    Toast.makeText(getContext(), "Permissão de Sobreposição necessária para o Ícone Flutuante", Toast.LENGTH_LONG).show();
                    android.content.Intent intent = new android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:" + requireContext().getPackageName()));
                    startActivity(intent);
                    // Opcional: desmarcar o switch se não permitir? Melhor deixar marcado e tentar de novo.
                }
            }
        });

        return view;
    }

    private void loadSettings() {
        switchEarnings.setChecked(getBoolSafe("tab_earnings_enabled", true));
        switchKm.setChecked(getBoolSafe("tab_km_enabled", true));
        switchFuel.setChecked(getBoolSafe("tab_fuel_enabled", true));
        switchMaintenance.setChecked(getBoolSafe("tab_maintenance_enabled", true));
        switchMaps.setChecked(getBoolSafe("maps_enabled", true));
        switchRouteDetection.setChecked(getBoolSafe("tab_route_detection_enabled", true));
        switchFloatingIcon.setChecked(getBoolSafe("floating_icon_enabled", true));
    }

    private void checkDeveloperAccess() {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            FirebaseHelper.checkDeveloperAccess(user.getEmail(), isDeveloper -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (switchRouteDetection != null) {
                            switchRouteDetection.setVisibility(isDeveloper ? View.VISIBLE : View.GONE);
                        }
                    });
                }
            });
        }
    }

    private boolean getBoolSafe(String key, boolean def) {
        try {
            return sharedPreferences.getBoolean(key, def);
        } catch (Exception e) {
            Object val = sharedPreferences.getAll().get(key);
            if (val != null) {
                String s = String.valueOf(val);
                boolean result = s.equalsIgnoreCase("true") || s.equals("1") || s.equalsIgnoreCase("on");
                sharedPreferences.edit().putBoolean(key, result).apply();
                return result;
            }
            return def;
        }
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("tab_earnings_enabled", switchEarnings.isChecked());
        editor.putBoolean("tab_km_enabled", switchKm.isChecked());
        editor.putBoolean("tab_fuel_enabled", switchFuel.isChecked());
        editor.putBoolean("tab_maintenance_enabled", switchMaintenance.isChecked());
        editor.putBoolean("maps_enabled", switchMaps.isChecked());
        editor.putBoolean("tab_route_detection_enabled", switchRouteDetection.isChecked());
        editor.putBoolean("floating_icon_enabled", switchFloatingIcon.isChecked());
        editor.commit(); // Usamos commit para garantir que o CloudSyncHelper veja os dados salvos

        // Sincronização instantânea na nuvem
        CloudSyncHelper.syncNow(requireContext());

        Toast.makeText(getContext(), "Recursos atualizados!", Toast.LENGTH_SHORT).show();
        
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).recreate(); // Recreate to update tabs immediately
        }
    }
}
