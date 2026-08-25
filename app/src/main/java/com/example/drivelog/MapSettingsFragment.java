package com.example.drivelog;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MapSettingsFragment extends Fragment {

    private SharedPreferences sharedPreferences;
    private View view1, view2, view3, view4;
    private ImageView imageSelectedApp;
    private TextView textSelectedAppName, textSelectedAppPackage, textSelectedVoiceName, textSelectedMapStyle;
    private com.google.android.material.materialswitch.MaterialSwitch switchAutoShare;
    private TextToSpeech tts;

    private static final String PREF_COLOR_G1 = "color_group_1";
    private static final String PREF_COLOR_G2 = "color_group_2";
    private static final String PREF_COLOR_G3 = "color_group_3";
    private static final String PREF_COLOR_G4 = "color_group_4";
    private static final String PREF_DELIVERY_APP = "delivery_app_package";
    private static final String PREF_AUTO_SHARE = "auto_share_corrections";
    private static final String PREF_ROUTE_OPACITY = "route_line_opacity";
    private static final String PREF_VOICE_COMMANDS = "voice_commands_enabled";
    private static final String PREF_VOICE_NAME = "voice_name";
    private static final String PREF_USER_ICON = "user_map_icon";
    private static final String PREF_MAP_STYLE = "map_tile_style";

    private static final String DEFAULT_G1 = "#2196F3";
    private static final String DEFAULT_G2 = "#9C27B0";
    private static final String DEFAULT_G3 = "#FBC02D";
    private static final String DEFAULT_G4 = "#795548";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map_settings, container, false);
        sharedPreferences = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);

        view1 = view.findViewById(R.id.viewColorGroup1);
        view2 = view.findViewById(R.id.viewColorGroup2);
        view3 = view.findViewById(R.id.viewColorGroup3);
        view4 = view.findViewById(R.id.viewColorGroup4);

        imageSelectedApp = view.findViewById(R.id.imageSelectedApp);
        textSelectedAppName = view.findViewById(R.id.textSelectedAppName);
        textSelectedAppPackage = view.findViewById(R.id.textSelectedAppPackage);
        textSelectedVoiceName = view.findViewById(R.id.textSelectedVoiceName);
        textSelectedMapStyle = view.findViewById(R.id.textSelectedMapStyle);
        switchAutoShare = view.findViewById(R.id.switchAutoShareCommunity);

        updatePreviewColors();
        updateSelectedAppInfo();
        updateVoiceInfo();
        updateMapStyleInfo();

        tts = new TextToSpeech(requireContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                // TTS inicializado
            }
        });

        if (switchAutoShare != null) {
            switchAutoShare.setChecked(sharedPreferences.getBoolean(PREF_AUTO_SHARE, true));
            switchAutoShare.setOnCheckedChangeListener((btn, isChecked) -> {
                sharedPreferences.edit().putBoolean(PREF_AUTO_SHARE, isChecked).apply();
            });
        }

        view.findViewById(R.id.layoutColorGroup1).setOnClickListener(v -> showColorPicker(PREF_COLOR_G1, DEFAULT_G1));
        view.findViewById(R.id.layoutColorGroup2).setOnClickListener(v -> showColorPicker(PREF_COLOR_G2, DEFAULT_G2));
        view.findViewById(R.id.layoutColorGroup3).setOnClickListener(v -> showColorPicker(PREF_COLOR_G3, DEFAULT_G3));
        view.findViewById(R.id.layoutColorGroup4).setOnClickListener(v -> showColorPicker(PREF_COLOR_G4, DEFAULT_G4));
        
        view.findViewById(R.id.layoutSelectApp).setOnClickListener(v -> showAppPicker());
        view.findViewById(R.id.layoutSelectVoice).setOnClickListener(v -> showVoicePicker());
        view.findViewById(R.id.layoutSelectMapStyle).setOnClickListener(v -> showMapStylePicker());

        com.google.android.material.materialswitch.MaterialSwitch switchVoice = view.findViewById(R.id.switchVoiceCommands);
        if (switchVoice != null) {
            switchVoice.setChecked(sharedPreferences.getBoolean(PREF_VOICE_COMMANDS, false));
            switchVoice.setOnCheckedChangeListener((btn, isChecked) -> {
                sharedPreferences.edit().putBoolean(PREF_VOICE_COMMANDS, isChecked).apply();
                CloudSyncHelper.syncNow(requireContext(), "Ajuste Mapa");
            });
        }

        SeekBar seekRouteOpacity = view.findViewById(R.id.seekRouteOpacity);
        TextView textRouteOpacityPercent = view.findViewById(R.id.textRouteOpacityPercent);

        if (seekRouteOpacity != null && textRouteOpacityPercent != null) {
            int currentOpacity = sharedPreferences.getInt(PREF_ROUTE_OPACITY, 95);
            seekRouteOpacity.setProgress(currentOpacity);
            textRouteOpacityPercent.setText(currentOpacity + "%");

            seekRouteOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    textRouteOpacityPercent.setText(progress + "%");
                    if (fromUser) {
                        sharedPreferences.edit().putInt(PREF_ROUTE_OPACITY, progress).apply();
                        CloudSyncHelper.syncNow(requireContext(), "Ajuste Mapa");
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        view.findViewById(R.id.cardIconArrow).setOnClickListener(v -> selectUserIcon("arrow"));
        view.findViewById(R.id.cardIconCar).setOnClickListener(v -> selectUserIcon("car"));
        view.findViewById(R.id.cardIconMoto).setOnClickListener(v -> selectUserIcon("moto"));
        view.findViewById(R.id.cardIconTruck).setOnClickListener(v -> selectUserIcon("truck"));
        updateUserIconSelection(view);

        return view;
    }

    private void selectUserIcon(String type) {
        sharedPreferences.edit().putString(PREF_USER_ICON, type).apply();
        updateUserIconSelection(getView());
        CloudSyncHelper.syncNow(requireContext(), "Ajuste Mapa");
    }

    private void updateUserIconSelection(View root) {
        if (root == null) return;
        String selected = sharedPreferences.getString(PREF_USER_ICON, "arrow");
        
        com.google.android.material.card.MaterialCardView cardArrow = root.findViewById(R.id.cardIconArrow);
        com.google.android.material.card.MaterialCardView cardCar = root.findViewById(R.id.cardIconCar);
        com.google.android.material.card.MaterialCardView cardMoto = root.findViewById(R.id.cardIconMoto);
        com.google.android.material.card.MaterialCardView cardTruck = root.findViewById(R.id.cardIconTruck);

        if (cardArrow != null) cardArrow.setStrokeWidth(selected.equals("arrow") ? (int)(2 * getResources().getDisplayMetrics().density) : 0);
        if (cardCar != null) cardCar.setStrokeWidth(selected.equals("car") ? (int)(2 * getResources().getDisplayMetrics().density) : 0);
        if (cardMoto != null) cardMoto.setStrokeWidth(selected.equals("moto") ? (int)(2 * getResources().getDisplayMetrics().density) : 0);
        if (cardTruck != null) cardTruck.setStrokeWidth(selected.equals("truck") ? (int)(2 * getResources().getDisplayMetrics().density) : 0);
    }

    private void updateVoiceInfo() {
        if (textSelectedVoiceName != null) {
            String name = sharedPreferences.getString(PREF_VOICE_NAME, "Padrão do Sistema");
            textSelectedVoiceName.setText(name);
        }
    }

    private void updateMapStyleInfo() {
        if (textSelectedMapStyle != null) {
            int style = sharedPreferences.getInt(PREF_MAP_STYLE, 0);
            String[] styles = {"OpenStreetMap (Padrão)", "Visão de Satélite", "Vias Urbanas (Claro)", "OpenTopoMap (Relevo)", "Satélite com Ruas"};
            if (style >= 0 && style < styles.length) {
                textSelectedMapStyle.setText(styles[style]);
            }
        }
    }

    private void showMapStylePicker() {
        String[] styles = {"OpenStreetMap (Padrão)", "Visão de Satélite", "Vias Urbanas (Claro)", "OpenTopoMap (Relevo)", "Satélite com Ruas"};
        int current = sharedPreferences.getInt(PREF_MAP_STYLE, 0);

        new AlertDialog.Builder(requireContext())
                .setTitle("Estilo do Mapa")
                .setSingleChoiceItems(styles, current, (dialog, which) -> {
                    sharedPreferences.edit().putInt(PREF_MAP_STYLE, which).apply();
                    updateMapStyleInfo();
                    dialog.dismiss();
                    Toast.makeText(getContext(), "Estilo alterado com sucesso!", Toast.LENGTH_SHORT).show();
                    CloudSyncHelper.syncNow(requireContext(), "Ajuste Mapa");
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showVoicePicker() {
        if (tts == null) return;
        
        List<Voice> ptVoices = new ArrayList<>();
        try {
            for (Voice v : tts.getVoices()) {
                if (v.getLocale().getLanguage().startsWith("pt")) {
                    ptVoices.add(v);
                }
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Erro ao carregar vozes", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ptVoices.isEmpty()) {
            Toast.makeText(getContext(), "Nenhuma voz alternativa encontrada", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[ptVoices.size()];
        for (int i = 0; i < ptVoices.size(); i++) {
            names[i] = ptVoices.get(i).getName();
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Escolha a Voz")
                .setItems(names, (dialog, which) -> {
                    String selected = names[which];
                    sharedPreferences.edit().putString(PREF_VOICE_NAME, selected).apply();
                    updateVoiceInfo();
                    
                    // Testar a voz
                    tts.setVoice(ptVoices.get(which));
                    tts.speak("Teste da nova voz do Drive Log", TextToSpeech.QUEUE_FLUSH, null, "test");
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    private void updateSelectedAppInfo() {
        String pkg = sharedPreferences.getString(PREF_DELIVERY_APP, "");
        if (pkg.isEmpty()) {
            imageSelectedApp.setImageResource(R.drawable.ic_map);
            textSelectedAppName.setText("Nenhum app selecionado");
            textSelectedAppPackage.setText("Clique para escolher");
        } else {
            try {
                PackageManager pm = requireContext().getPackageManager();
                ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                imageSelectedApp.setImageDrawable(info.loadIcon(pm));
                textSelectedAppName.setText(info.loadLabel(pm));
                textSelectedAppPackage.setText(pkg);
            } catch (Exception e) {
                imageSelectedApp.setImageResource(R.drawable.ic_map);
                textSelectedAppName.setText("App não encontrado");
                textSelectedAppPackage.setText(pkg);
            }
        }
    }

    private void showAppPicker() {
        PackageManager pm = requireContext().getPackageManager();
        // Usamos getInstalledPackages para maior compatibilidade e garantia de ver todos
        List<android.content.pm.PackageInfo> packages = pm.getInstalledPackages(0);
        List<ApplicationInfo> allApps = new ArrayList<>();
        
        for (android.content.pm.PackageInfo pkg : packages) {
            allApps.add(pkg.applicationInfo);
        }
        
        Collections.sort(allApps, (a, b) -> 
            pm.getApplicationLabel(a).toString().compareToIgnoreCase(pm.getApplicationLabel(b).toString()));

        AppAdapter adapter = new AppAdapter(requireContext(), allApps);
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Escolha o App de Entrega")
                .setAdapter(adapter, (dialog, which) -> {
                    String pkg = allApps.get(which).packageName;
                    sharedPreferences.edit().putString(PREF_DELIVERY_APP, pkg).commit();
                    updateSelectedAppInfo();
                    CloudSyncHelper.syncNow(requireContext(), "Ajuste Mapa");
                })
                .setNeutralButton("Digitar Pacote Manualmente", (dialog, which) -> showManualPackageEntry())
                .setNegativeButton("Remover Atalho", (dialog, which) -> {
                    sharedPreferences.edit().putString(PREF_DELIVERY_APP, "").commit();
                    updateSelectedAppInfo();
                    CloudSyncHelper.syncNow(requireContext(), "Ajuste Mapa");
                })
                .show();
    }

    private void showManualPackageEntry() {
        android.widget.EditText input = new android.widget.EditText(getContext());
        input.setHint("ex: com.shopee.spx.driver");
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Pacote do Aplicativo")
                .setMessage("Se o app não aparece na lista, digite o nome do pacote (Package Name) dele aqui:")
                .setView(input)
                .setPositiveButton("Salvar", (dialog, which) -> {
                    String pkg = input.getText().toString().trim();
                    if (!pkg.isEmpty()) {
                        sharedPreferences.edit().putString(PREF_DELIVERY_APP, pkg).apply();
                        updateSelectedAppInfo();
                        CloudSyncHelper.syncNow(requireContext(), "Ajuste Mapa");
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private static class AppAdapter extends BaseAdapter {
        private final Context context;
        private final List<ApplicationInfo> apps;
        private final PackageManager pm;

        AppAdapter(Context context, List<ApplicationInfo> apps) {
            this.context = context;
            this.apps = apps;
            this.pm = context.getPackageManager();
        }

        @Override public int getCount() { return apps.size(); }
        @Override public Object getItem(int position) { return apps.get(position); }
        @Override public long getItemId(int position) { return position; }
        @Override public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(android.R.layout.activity_list_item, parent, false);
            }
            ImageView icon = convertView.findViewById(android.R.id.icon);
            TextView text = convertView.findViewById(android.R.id.text1);
            
            ApplicationInfo info = apps.get(position);
            icon.setImageDrawable(info.loadIcon(pm));
            text.setText(info.loadLabel(pm));
            
            return convertView;
        }
    }

    private void updatePreviewColors() {
        setCircleColor(view1, sharedPreferences.getString(PREF_COLOR_G1, DEFAULT_G1));
        setCircleColor(view2, sharedPreferences.getString(PREF_COLOR_G2, DEFAULT_G2));
        setCircleColor(view3, sharedPreferences.getString(PREF_COLOR_G3, DEFAULT_G3));
        setCircleColor(view4, sharedPreferences.getString(PREF_COLOR_G4, DEFAULT_G4));
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
                    sharedPreferences.edit().putString(key, hex).commit();
                    updatePreviewColors();
                    CloudSyncHelper.syncNow(requireContext(), "Ajuste Mapa");
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
