package com.example.drivelog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.services.drive.DriveScopes;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.InputStream;
import java.util.Locale;

public class SettingsFragment extends Fragment {

    private TextInputEditText editProfileName, editProfileEmail;
    private TextView textTrialExpiration;
    private Button btnUpgradeSubscription;
    private Button btnDetectGoogle, btnLogoutGoogle, btnBackToLogin, btnCheckUpdates, btnAboutApp;
    private TextView textSyncLog, textOverlayWarning, textNotificationWarning;
    private TextInputEditText editWeeklyGoal, editConsumption, editFuelPrice, editRestStart, editRestEnd, editCpfInterval, editCpfInactivity;
    private MaterialSwitch switchSubtract, switchAutoBackup, switchRestEnabled, switchAutoCopyCpf, switchCpfInterval, switchAutoCheckUpdates;
    private RadioGroup rgKmSource, rgAppMode, rgSubscription, rgComboioVisibility;
    private View layoutDevTools, layoutCpfInterval, layoutAdvancedDevControls;
    private Spinner spinnerTab, spinnerTheme;
    private SharedPreferences sharedPreferences;
    private GoogleDriveHelper driveHelper;

    private final String[] tabOptions = {"Mapa", "Ganhos", "KM Diário", "Abastecimentos", "Manutenção", "Relatórios", "Ajustes"};
    private final String[] themeOptions = {"Padrão", "Azul Oceano", "Verde Floresta", "Roxo Moderno", "Laranja Energia", "Escuro Profundo"};

    private final ActivityResultLauncher<Intent> driveSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    com.google.android.gms.tasks.Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(com.google.android.gms.common.api.ApiException.class);
                        if (account != null) {
                            updateProfileFromAccount(account);
                            initializeDriveHelper(account);
                            updateGoogleButtonState();
                        }
                    } catch (com.google.android.gms.common.api.ApiException e) {
                        Toast.makeText(getContext(), "Erro Google: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> localDbPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        importBackupFromLocalFile(uri);
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        editProfileName = view.findViewById(R.id.editProfileName);
        editProfileEmail = view.findViewById(R.id.editProfileEmail);
        textTrialExpiration = view.findViewById(R.id.textTrialExpiration);
        btnUpgradeSubscription = view.findViewById(R.id.btnUpgradeSubscription);
        btnDetectGoogle = view.findViewById(R.id.btnDetectGoogle);
        btnLogoutGoogle = view.findViewById(R.id.btnLogoutGoogle);
        btnBackToLogin = view.findViewById(R.id.btnBackToLogin);
        textSyncLog = view.findViewById(R.id.textSyncLog);
        textOverlayWarning = view.findViewById(R.id.textOverlayWarningSettings);
        textNotificationWarning = view.findViewById(R.id.textNotificationWarningSettings);
        
        editWeeklyGoal = view.findViewById(R.id.editWeeklyGoal);
        editConsumption = view.findViewById(R.id.editDefaultConsumption);
        editFuelPrice = view.findViewById(R.id.editDefaultFuelPrice);
        switchSubtract = view.findViewById(R.id.switchSubtractFuel);
        switchAutoCopyCpf = view.findViewById(R.id.switchAutoCopyCpf);
        switchCpfInterval = view.findViewById(R.id.switchCpfInterval);
        layoutCpfInterval = view.findViewById(R.id.layoutCpfInterval);
        editCpfInterval = view.findViewById(R.id.editCpfInterval);
        editCpfInactivity = view.findViewById(R.id.editCpfInactivity);
        switchAutoBackup = view.findViewById(R.id.switchAutoBackup);
        switchRestEnabled = view.findViewById(R.id.switchRestEnabled);
        switchAutoCheckUpdates = view.findViewById(R.id.switchAutoCheckUpdates);
        editRestStart = view.findViewById(R.id.editRestStart);
        editRestEnd = view.findViewById(R.id.editRestEnd);
        rgKmSource = view.findViewById(R.id.rgKmSource);
        rgAppMode = view.findViewById(R.id.rgAppMode);
        rgComboioVisibility = view.findViewById(R.id.rgComboioVisibility);
        rgSubscription = view.findViewById(R.id.rgSubscriptionType);
        spinnerTab = view.findViewById(R.id.spinnerDefaultTab);
        spinnerTheme = view.findViewById(R.id.spinnerTheme);

        CloudSyncHelper.syncLog.observe(getViewLifecycleOwner(), log -> {
            if (textSyncLog != null) textSyncLog.setText(log);
        });

        View layoutRestTimes = view.findViewById(R.id.layoutRestTimes);
        Button btnSave = view.findViewById(R.id.btnSaveSettings);
        btnCheckUpdates = view.findViewById(R.id.btnCheckUpdates);
        btnAboutApp = view.findViewById(R.id.btnAboutApp);
        Button btnReset = view.findViewById(R.id.btnResetData);
        Button btnDriveSync = view.findViewById(R.id.btnDriveSync);
        Button btnDriveDownload = view.findViewById(R.id.btnDriveDownload);
        Button btnImportLocalDb = view.findViewById(R.id.btnImportLocalDb);
        Button btnExportLocalDb = view.findViewById(R.id.btnExportLocalDb);
        layoutDevTools = view.findViewById(R.id.layoutDevTools);
        layoutAdvancedDevControls = view.findViewById(R.id.layoutAdvancedDevControls);

        if (textOverlayWarning != null) {
            textOverlayWarning.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, 
                        android.net.Uri.parse("package:" + requireContext().getPackageName()));
                startActivity(intent);
            });
        }

        if (textNotificationWarning != null) {
            textNotificationWarning.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent();
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    intent.setAction(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                    intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());
                } else {
                    intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                    intent.putExtra("app_package", requireContext().getPackageName());
                    intent.putExtra("app_uid", requireContext().getApplicationInfo().uid);
                }
                startActivity(intent);
            });
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, tabOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTab.setAdapter(adapter);

        ArrayAdapter<String> themeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, themeOptions);
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTheme.setAdapter(themeAdapter);

        sharedPreferences = requireActivity().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);

        loadSettings();
        checkDeveloperAccess();

        switchRestEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (layoutRestTimes != null) layoutRestTimes.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        editRestStart.setOnClickListener(v -> showTimePicker(editRestStart));
        editRestEnd.setOnClickListener(v -> showTimePicker(editRestEnd));

        btnSave.setOnClickListener(v -> saveSettings());
        
        if (btnCheckUpdates != null) {
            btnCheckUpdates.setOnClickListener(v -> {
                UpdateHelper.handleUpdateProcess(requireActivity(), true, new UpdateHelper.UpdateCallback() {
                    @Override public void onNoUpdate() {}
                    @Override public void onError(String error) {
                        Toast.makeText(getContext(), "Erro ao verificar: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

        if (btnAboutApp != null) {
            btnAboutApp.setOnClickListener(v -> showAboutDialog());
        }
        
        btnReset.setOnClickListener(v -> showResetConfirmation());

        if (btnUpgradeSubscription != null) {
            btnUpgradeSubscription.setOnClickListener(v -> {
                // Scroll até a seção de assinaturas ou apenas informa
                View subSection = getView() != null ? getView().findViewById(R.id.rgSubscriptionType) : null;
                if (subSection != null) {
                    subSection.getParent().requestChildFocus(subSection, subSection);
                    Toast.makeText(getContext(), "Escolha seu plano abaixo", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        btnDriveSync.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Sincronização Manual")
                    .setMessage("Deseja enviar todos os seus dados e ajustes atuais para o Google Drive agora?")
                    .setPositiveButton("Sim", (dialog, which) -> requestDriveAction(true))
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
        
        btnDriveDownload.setOnClickListener(v -> requestDriveAction(false));
        
        btnImportLocalDb.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            localDbPickerLauncher.launch(intent);
        });

        btnExportLocalDb.setOnClickListener(v -> performLocalDbExport());

        btnDetectGoogle.setOnClickListener(v -> detectGoogleProfile());
        switchCpfInterval.setOnCheckedChangeListener((v, isChecked) -> {
            layoutCpfInterval.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            saveSettings();
        });

        btnLogoutGoogle.setOnClickListener(v -> logoutGoogle());
        btnBackToLogin.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Sair")
                    .setMessage("Deseja voltar para a tela de login?")
                    .setPositiveButton("Sair", (dialog, which) -> {
                        sharedPreferences.edit()
                                .putBoolean("is_local_user", false)
                                .putString("profile_name", "")
                                .putString("profile_email", "")
                                .apply();
                        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build();
                        GoogleSignIn.getClient(requireActivity(), gso).signOut();
                        Intent intent = new Intent(getActivity(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        updateGoogleButtonState();
        CloudSyncHelper.syncLog.observe(getViewLifecycleOwner(), log -> {
            if (textSyncLog != null) textSyncLog.setText(log);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkPermissions();
        refreshVisibility();
    }

    public void refreshVisibility() {
        if (btnCheckUpdates != null && getActivity() instanceof MainActivity) {
            boolean visible = ((MainActivity) getActivity()).isMenuVisible("btn_check_updates");
            btnCheckUpdates.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void checkPermissions() {
        if (getContext() == null) return;

        // Check Overlay
        if (textOverlayWarning != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                boolean hasOverlay = android.provider.Settings.canDrawOverlays(getContext());
                textOverlayWarning.setVisibility(hasOverlay ? View.GONE : View.VISIBLE);
            } else {
                textOverlayWarning.setVisibility(View.GONE);
            }
        }

        // Check Notifications
        if (textNotificationWarning != null) {
            boolean hasNotifications = androidx.core.app.NotificationManagerCompat.from(requireContext()).areNotificationsEnabled();
            textNotificationWarning.setVisibility(hasNotifications ? View.GONE : View.VISIBLE);
        }
    }

    private void updateGoogleButtonState() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
        if (account != null) {
            btnDetectGoogle.setVisibility(View.GONE);
            btnLogoutGoogle.setVisibility(View.VISIBLE);
        } else {
            btnDetectGoogle.setVisibility(View.VISIBLE);
            btnLogoutGoogle.setVisibility(View.GONE);
        }
    }

    private void showTimePicker(TextInputEditText editText) {
        String current = editText.getText().toString();
        int hour = 12, minute = 0;
        if (current.contains(":")) {
            String[] parts = current.split(":");
            try {
                hour = Integer.parseInt(parts[0]);
                minute = Integer.parseInt(parts[1]);
            } catch (Exception ignored) {}
        }
        new android.app.TimePickerDialog(getContext(), (view, h, m) -> {
            editText.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m));
        }, hour, minute, true).show();
    }

    private void logoutGoogle() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build();
        GoogleSignInClient client = GoogleSignIn.getClient(requireActivity(), gso);
        client.signOut().addOnCompleteListener(task -> {
            editProfileName.setText("");
            editProfileEmail.setText("");
            sharedPreferences.edit()
                    .putString("profile_name", "")
                    .putString("profile_email", "")
                    .apply();
            updateGoogleButtonState();
            new AlertDialog.Builder(getContext())
                    .setTitle("Desconectado")
                    .setMessage("Deseja voltar para a tela de login?")
                    .setPositiveButton("Sim", (dialog, which) -> {
                        Intent intent = new Intent(getActivity(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    })
                    .setNegativeButton("Ficar aqui", null)
                    .show();
        });
    }

    private void detectGoogleProfile() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build();
        GoogleSignInClient client = GoogleSignIn.getClient(requireActivity(), gso);
        client.signOut().addOnCompleteListener(task -> {
            driveSignInLauncher.launch(client.getSignInIntent());
        });
    }

    private void updateProfileFromAccount(GoogleSignInAccount account) {
        if (account != null) {
            if (account.getDisplayName() != null) editProfileName.setText(account.getDisplayName());
            if (account.getEmail() != null) editProfileEmail.setText(account.getEmail());
            sharedPreferences.edit()
                    .putString("profile_name", account.getDisplayName())
                    .putString("profile_email", account.getEmail())
                    .apply();
            Toast.makeText(getContext(), "Perfil detectado!", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestDriveAction(boolean isUpload) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
        if (account != null && GoogleSignIn.hasPermissions(account, new Scope(DriveScopes.DRIVE_FILE))) {
            if (driveHelper == null) initializeDriveHelper(account);
            if (isUpload) startDriveSync(); else startDriveDownload();
        } else {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                    .build();
            GoogleSignInClient client = GoogleSignIn.getClient(requireActivity(), gso);
            driveSignInLauncher.launch(client.getSignInIntent());
        }
    }

    private void initializeDriveHelper(GoogleSignInAccount account) {
        driveHelper = new GoogleDriveHelper(requireContext(), account);
    }

    private void startDriveSync() {
        Toast.makeText(getContext(), "Iniciando envio para nuvem...", Toast.LENGTH_SHORT).show();
        CloudSyncHelper.syncNow(requireContext(), "Backup Manual");
    }

    private void startDriveDownload() {
        File tempFile = new File(requireContext().getCacheDir(), "drive_import_tmp.db");
        driveHelper.downloadBackup(tempFile, new GoogleDriveHelper.SyncCallback() {
            @Override public void onSuccess(String message) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        importBackupFromLocalFile(Uri.fromFile(tempFile));
                    });
                }
            }
            @Override public void onError(String error) {
                if (isAdded()) requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Erro Download: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void importBackupFromLocalFile(Uri uri) {
        try {
            java.util.Map<String, Integer> contents = DbHelper.peekDbContents(requireContext(), uri);
            if (contents.isEmpty()) {
                Toast.makeText(getContext(), "Backup vazio.", Toast.LENGTH_SHORT).show();
                return;
            }

            String[] tables = contents.keySet().toArray(new String[0]);
            String[] displayNames = new String[tables.length];
            boolean[] checkedItems = new boolean[tables.length];
            java.util.Set<String> selectedTables = new java.util.HashSet<>();

            for (int i = 0; i < tables.length; i++) {
                String table = tables[i];
                int count = contents.get(table);
                String name;
                switch (table) {
                    case "EARNINGS": name = "Ganhos"; break;
                    case "FUEL": name = "Abastecimentos"; break;
                    case "DAILY_KM": name = "KM Diário"; break;
                    case "MAINTENANCE": name = "Manutenção"; break;
                    case "ROUTE_HEADERS": name = "Gravações (Rotas)"; break;
                    case "CORRECTED_ADDRESSES": name = "Endereços Corrigidos"; break;
                    case "SETTINGS": name = "Configurações"; break;
                    case "LOADING_POINTS": name = "Pontos de Carregamento"; break;
                    case "PLATFORMS": name = "Plataformas"; break;
                    case "GAS_STATIONS": name = "Postos"; break;
                    default: name = table; break;
                }
                displayNames[i] = name + " (" + count + ")";
                checkedItems[i] = true;
                selectedTables.add(table);
            }

            new AlertDialog.Builder(getContext())
                    .setTitle("Restaurar Dados")
                    .setMultiChoiceItems(displayNames, checkedItems, (dialog, which, isChecked) -> {
                        if (isChecked) selectedTables.add(tables[which]);
                        else selectedTables.remove(tables[which]);
                    })
                    .setPositiveButton("Restaurar", (dialog, which) -> {
                        if (selectedTables.isEmpty()) return;
                        new AlertDialog.Builder(getContext())
                                .setTitle("Como deseja importar?")
                                .setMessage("Deseja MESCLAR os dados (adicionar aos atuais) ou SUBSTITUIR tudo o que tem hoje?")
                                .setPositiveButton("Mesclar (Mixar)", (d, w) -> startImportProcess(uri, selectedTables, true))
                                .setNegativeButton("Substituir Tudo", (d, w) -> startImportProcess(uri, selectedTables, false))
                                .setNeutralButton("Cancelar", null)
                                .show();
                    })
                    .setNeutralButton("Substituição Radical", (dialog, which) -> performFullDbReplacement(uri))
                    .setNegativeButton("Cancelar", null)
                    .show();

        } catch (Exception e) {
            Toast.makeText(getContext(), "Erro ler backup: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startImportProcess(Uri uri, java.util.Set<String> selectedTables, boolean isMerge) {
        new Thread(() -> {
            try {
                DbHelper.importFromDb(requireContext(), uri, selectedTables, isMerge);
                if (isAdded()) requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), isMerge ? "Dados mesclados com sucesso!" : "Dados substituídos com sucesso!", Toast.LENGTH_SHORT).show();
                    
                    if (selectedTables.contains("SETTINGS")) {
                        // Se as configurações mudaram, precisamos de um pequeno atraso antes do recreate
                        // para garantir que os SharedPreferences foram persistidos e o usuário viu a mensagem
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            if (isAdded()) {
                                requireActivity().recreate();
                            }
                        }, 1500);
                    }
                });
            } catch (Exception e) {
                if (isAdded()) requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void performFullDbReplacement(Uri uri) {
        try {
            AppDatabase.forceCloseInstance();
            String dbName = "entregas_db_" + sharedPreferences.getString("profile_email", "local").replaceAll("[^a-zA-Z0-9]", "_");
            File dbFile = requireContext().getDatabasePath(dbName);
            try (InputStream in = requireContext().getContentResolver().openInputStream(uri);
                 java.io.FileOutputStream out = new java.io.FileOutputStream(dbFile)) {
                byte[] buf = new byte[4096];
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            }
            requireActivity().finishAffinity();
            System.exit(0);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void checkDeveloperAccess() {
        MainActivity main = (MainActivity) getActivity();
        if (main == null) return;

        boolean isAdvancedVisible = main.isMenuVisible("premium_features");
        if (layoutAdvancedDevControls != null) {
            layoutAdvancedDevControls.setVisibility(isAdvancedVisible ? View.VISIBLE : View.GONE);
        }

        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            FirebaseHelper.checkDeveloperAccess(user.getEmail(), isDeveloper -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (layoutDevTools != null) layoutDevTools.setVisibility(isDeveloper ? View.VISIBLE : View.GONE);
                    });
                }
            });
        } else {
            if (layoutDevTools != null) layoutDevTools.setVisibility(View.GONE);
        }
    }

    private void performLocalDbExport() {
        try {
            AppDatabase.forceCloseInstance();
            String dbName = "entregas_db_" + sharedPreferences.getString("profile_email", "local").replaceAll("[^a-zA-Z0-9]", "_");
            File dbFile = requireContext().getDatabasePath(dbName);
            
            if (!dbFile.exists()) {
                Toast.makeText(getContext(), "Arquivo de banco de dados não encontrado", Toast.LENGTH_SHORT).show();
                return;
            }

            // Cria uma cópia temporária para exportação segura
            File exportFile = new File(requireContext().getCacheDir(), "DriveLog_Backup_" + System.currentTimeMillis() + ".db");
            try (java.io.FileInputStream in = new java.io.FileInputStream(dbFile);
                 java.io.FileOutputStream out = new java.io.FileOutputStream(exportFile)) {
                byte[] buf = new byte[4096];
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            }

            Uri uri = androidx.core.content.FileProvider.getUriForFile(requireContext(), 
                    requireContext().getPackageName() + ".fileprovider", exportFile);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/octet-stream");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Exportar Banco de Dados"));

        } catch (Exception e) {
            Toast.makeText(getContext(), "Erro ao exportar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void loadSettings() {
        editProfileName.setText(sharedPreferences.getString("profile_name", ""));
        editProfileEmail.setText(sharedPreferences.getString("profile_email", ""));

        // Lógica de tempo restante (Modo Free)
        int subType = sharedPreferences.getInt("sub_type", 2);
        boolean trialExpired = false;
        if (subType == 0) { // Free
            long installDate = sharedPreferences.getLong("install_date", System.currentTimeMillis());
            long diff = System.currentTimeMillis() - installDate;
            long sevenDaysMs = 7L * 24 * 60 * 60 * 1000;
            long remainingMs = sevenDaysMs - diff;

            if (textTrialExpiration != null) {
                textTrialExpiration.setVisibility(View.VISIBLE);
                if (remainingMs > 0) {
                    long days = remainingMs / (24 * 60 * 60 * 1000);
                    long hours = (remainingMs % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
                    long minutes = (remainingMs % (60 * 60 * 1000)) / (60 * 1000);
                    textTrialExpiration.setText(String.format(Locale.getDefault(), 
                        "Seu período de teste completo expira em: %d dias, %d horas e %d min.", days, hours, minutes));
                    textTrialExpiration.setBackgroundColor(0xFFFFF3E0); // Laranja claro
                    textTrialExpiration.setTextColor(0xFFE65100); // Laranja escuro
                    if (btnUpgradeSubscription != null) btnUpgradeSubscription.setVisibility(View.VISIBLE);
                } else {
                    trialExpired = true;
                    textTrialExpiration.setText("Seu período de teste completo EXPIROU. Funções financeiras bloqueadas.");
                    textTrialExpiration.setBackgroundColor(0xFFFFEBEE); // Vermelho claro
                    textTrialExpiration.setTextColor(0xFFC62828); // Vermelho escuro
                    if (btnUpgradeSubscription != null) {
                        btnUpgradeSubscription.setVisibility(View.VISIBLE);
                        btnUpgradeSubscription.setText("Assinar Plano Premium agora");
                    }
                }
            }
        } else {
            if (textTrialExpiration != null) textTrialExpiration.setVisibility(View.GONE);
            if (btnUpgradeSubscription != null) btnUpgradeSubscription.setVisibility(View.GONE);
        }

        editWeeklyGoal.setText(formatDecimal(sharedPreferences.getFloat("weekly_goal", 1500.0f)));
        editConsumption.setText(formatDecimal(sharedPreferences.getFloat("default_consumption", 10.0f)));
        editFuelPrice.setText(formatDecimal(sharedPreferences.getFloat("default_fuel_price", 5.50f)));
        switchSubtract.setChecked(sharedPreferences.getBoolean("subtract_fuel", false));
        switchAutoCopyCpf.setChecked(sharedPreferences.getBoolean("auto_copy_fake_cpf", true));
        switchCpfInterval.setChecked(sharedPreferences.getBoolean("cpf_interval_enabled", false));
        layoutCpfInterval.setVisibility(switchCpfInterval.isChecked() ? View.VISIBLE : View.GONE);
        editCpfInterval.setText(String.valueOf(sharedPreferences.getInt("cpf_interval_minutes", 2)));
        editCpfInactivity.setText(String.valueOf(sharedPreferences.getInt("cpf_inactivity_minutes", 5)));
        switchAutoCheckUpdates.setChecked(sharedPreferences.getBoolean("auto_check_updates", true));
        
        boolean restEnabled = sharedPreferences.getBoolean("rest_interval_enabled", false);
        switchRestEnabled.setChecked(restEnabled);
        View lrt = getView() != null ? getView().findViewById(R.id.layoutRestTimes) : null;
        if (lrt != null) lrt.setVisibility(restEnabled ? View.VISIBLE : View.GONE);

        editRestStart.setText(sharedPreferences.getString("rest_start_time", "12:00"));
        editRestEnd.setText(sharedPreferences.getString("rest_end_time", "13:00"));
        if (sharedPreferences.getInt("report_km_source", 0) == 1) rgKmSource.check(R.id.rbKmAuto);
        else rgKmSource.check(R.id.rbKmManual);
        
        if (trialExpired) {
            rgAppMode.check(R.id.rbModeMapsOnly);
            for (int i = 0; i < rgAppMode.getChildCount(); i++) {
                rgAppMode.getChildAt(i).setEnabled(false);
            }
            rgAppMode.setAlpha(0.5f); // Visual cinza
        } else {
            if (sharedPreferences.getInt("app_mode", 0) == 1) rgAppMode.check(R.id.rbModeMapsOnly);
            else rgAppMode.check(R.id.rbModeFull);
            for (int i = 0; i < rgAppMode.getChildCount(); i++) {
                rgAppMode.getChildAt(i).setEnabled(true);
            }
            rgAppMode.setAlpha(1.0f);
        }

        int cVisibility = sharedPreferences.getInt("comboio_visibility_mode", 2);
        if (cVisibility == 0) rgComboioVisibility.check(R.id.rbComboioShowMap);
        else if (cVisibility == 1) rgComboioVisibility.check(R.id.rbComboioShowTracking);
        else rgComboioVisibility.check(R.id.rbComboioShowBoth);

        if (subType == 0) rgSubscription.check(R.id.rbSubFree);
        else if (subType == 1) rgSubscription.check(R.id.rbSubPremium);
        else rgSubscription.check(R.id.rbSubDev);

        spinnerTab.setSelection(sharedPreferences.getInt("default_tab", 0));
        // 🔥 PADRÃO: Azul Oceano (1)
        spinnerTheme.setSelection(sharedPreferences.getInt("app_theme", 1));
    }

    private String formatDecimal(float value) {
        return String.valueOf(value).replace(".", ",");
    }

    private void saveSettings() {
        try {
            int oldMode = sharedPreferences.getInt("app_mode", 0);
            int newMode = rgAppMode.getCheckedRadioButtonId() == R.id.rbModeMapsOnly ? 1 : 0;
            boolean modeChanged = oldMode != newMode;

            int selectedTheme = spinnerTheme.getSelectedItemPosition();
            
            int cVisibility = 2;
            int checkedVisibility = rgComboioVisibility.getCheckedRadioButtonId();
            if (checkedVisibility == R.id.rbComboioShowMap) cVisibility = 0;
            else if (checkedVisibility == R.id.rbComboioShowTracking) cVisibility = 1;

            int subType = rgSubscription.getCheckedRadioButtonId() == R.id.rbSubFree ? 0 : 
                           (rgSubscription.getCheckedRadioButtonId() == R.id.rbSubPremium ? 1 : 2);

            sharedPreferences.edit()
                    .putString("profile_name", editProfileName.getText().toString().trim())
                    .putString("profile_email", editProfileEmail.getText().toString().trim())
                    .putFloat("weekly_goal", parseDecimal(editWeeklyGoal))
                    .putFloat("default_consumption", parseDecimal(editConsumption))
                    .putFloat("default_fuel_price", parseDecimal(editFuelPrice))
                    .putBoolean("subtract_fuel", switchSubtract.isChecked())
                    .putBoolean("auto_copy_fake_cpf", switchAutoCopyCpf.isChecked())
                .putBoolean("cpf_interval_enabled", switchCpfInterval.isChecked())
                .putInt("cpf_interval_minutes", parseSafeInt(editCpfInterval, 2))
                .putInt("cpf_inactivity_minutes", parseSafeInt(editCpfInactivity, 5))
                    .putBoolean("auto_check_updates", switchAutoCheckUpdates.isChecked())
                    .putBoolean("rest_interval_enabled", switchRestEnabled.isChecked())
                    .putString("rest_start_time", editRestStart.getText().toString())
                    .putString("rest_end_time", editRestEnd.getText().toString())
                    .putInt("report_km_source", rgKmSource.getCheckedRadioButtonId() == R.id.rbKmAuto ? 1 : 0)
                    .putInt("app_mode", newMode)
                    .putInt("comboio_visibility_mode", cVisibility)
                    .putInt("sub_type", subType)
                    .putInt("default_tab", spinnerTab.getSelectedItemPosition())
                    .putInt("app_theme", selectedTheme)
                    .commit();

            TrackingHelper.updateAutoTracking(requireContext());

            // 🔥 Sincroniza a mudança de assinatura e data de instalação com o Firebase
            String userId = sharedPreferences.getString("current_user_id", null);
            if (userId != null) {
                long installDate = sharedPreferences.getLong("install_date", System.currentTimeMillis());
                FirebaseHelper.syncUserMetadata(userId, installDate, subType, null);
            }

            CloudSyncHelper.syncNow(requireContext(), "Ajustes Gerais");
            MainActivity main = (MainActivity) getActivity();
            if (main != null) {
                main.applyAppTheme(selectedTheme);
                if (modeChanged) {
                    requireActivity().getSupportFragmentManager().popBackStack();
                }
            }
            Toast.makeText(getContext(), modeChanged ? "Modo de App alterado!" : "Configurações salvas!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Erro ao salvar.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAboutDialog() {
        String versionName = BuildConfig.VERSION_NAME;
        int versionCode = BuildConfig.VERSION_CODE;
        String buildType = BuildConfig.BUILD_TYPE;

        new AlertDialog.Builder(getContext())
                .setTitle("Sobre o DriveLog")
                .setMessage("DriveLog - Gestão para Motoristas\n\n" +
                        "Versão: " + versionName + "\n" +
                        "Build: " + versionCode + "\n" +
                        "Ambiente: " + buildType + "\n\n" +
                        "Desenvolvido por Julio Dev")
                .setPositiveButton("FECHAR", null)
                .show();
    }

    private float parseDecimal(TextInputEditText editText) {
        if (editText == null || editText.getText() == null) return 0;
        String val = editText.getText().toString().replace(",", ".");
        if (val.isEmpty()) return 0;
        try { return Float.parseFloat(val); } catch (Exception e) { return 0; }
    }

    private int parseSafeInt(TextInputEditText et, int defaultValue) {
        if (et == null || et.getText() == null) return defaultValue;
        try {
            int val = Integer.parseInt(et.getText().toString());
            return Math.max(1, val); 
        } catch (Exception e) { return defaultValue; }
    }

    private void showResetConfirmation() {
        UiHelper.showBottomSheetConfirm(
                requireContext(),
                "Apagar Tudo?",
                "Esta ação excluirá PERMANENTEMENTE todos os seus dados locais e configurações. Não pode ser desfeita.",
                "APAGAR TUDO",
                this::resetAppData
        );
    }

    private void resetAppData() {
        try {
            AppDao dao = AppDatabase.getInstance(getContext()).appDao();
            dao.clearEarnings(); dao.clearFuel(); dao.clearDailyKm(); dao.clearMaintenance();
            dao.clearPlatforms(); dao.clearGasStations(); dao.clearRoutePoints();
            Toast.makeText(getContext(), "Dados apagados!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Erro ao resetar.", Toast.LENGTH_LONG).show();
        }
    }
}
