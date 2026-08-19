package com.example.drivelog;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

    private GoogleSignInClient googleSignInClient;
    private View layoutLoading, btnLoginGoogle;
    private TextView textLoadingStatus, textLoadingPercent;
    private com.google.android.material.progressindicator.CircularProgressIndicator progressLoading;
    private android.content.SharedPreferences prefs;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> checkNextPermission()
    );

    private final ActivityResultLauncher<String> notificationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> checkNextPermission()
    );

    private final ActivityResultLauncher<Intent> overlayPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> checkNextPermission()
    );

    private final ActivityResultLauncher<Intent> loginLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getData() != null) {
                    com.google.android.gms.tasks.Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(com.google.android.gms.common.api.ApiException.class);
                        if (account != null) {
                            saveGoogleProfile(account);
                        }
                    } catch (com.google.android.gms.common.api.ApiException e) {
                        Toast.makeText(this, "Erro Google (" + e.getStatusCode() + ")", Toast.LENGTH_LONG).show();
                    }
                }
            }
    );

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent event) {
        if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof android.widget.EditText) {
                android.graphics.Rect outRect = new android.graphics.Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private void saveGoogleProfile(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnSuccessListener(authResult -> completeGoogleLogin(account))
                .addOnFailureListener(e -> {
                    Log.e("LoginActivity", "Autenticação Firebase falhou", e);
                    completeGoogleLogin(account); // Tenta seguir mesmo assim
                });
    }

    private void completeGoogleLogin(GoogleSignInAccount account) {
        FirebaseHelper.syncUserMetadata(account.getEmail(), System.currentTimeMillis(), 0, new FirebaseHelper.UserMetadataCallback() {
            @Override
            public void onSuccess(long cloudDate, int cloudSub) {
                prefs.edit()
                        .putBoolean("is_local_user", false)
                        .putString("current_user_id", account.getEmail())
                        .putString("profile_name", account.getDisplayName())
                        .putString("profile_email", account.getEmail())
                        .putBoolean("auto_backup_cloud", true)
                        .putBoolean("auto_copy_fake_cpf", true)
                        .putLong("install_date", cloudDate)
                        .putInt("sub_type", cloudSub)
                        .commit();
                
                checkNextPermission();
            }

            @Override
            public void onError(String msg) {
                prefs.edit()
                        .putBoolean("is_local_user", false)
                        .putString("current_user_id", account.getEmail())
                        .putString("profile_name", account.getDisplayName())
                        .putString("profile_email", account.getEmail())
                        .putBoolean("auto_backup_cloud", true)
                        .putBoolean("auto_copy_fake_cpf", true)
                        .commit();
                
                checkNextPermission();
            }
        });
    }

    private void checkCloudBackupAndRestore(GoogleSignInAccount account) {
        runOnUiThread(() -> { 
            if (btnLoginGoogle != null) btnLoginGoogle.setVisibility(View.GONE);
            if (layoutLoading != null) layoutLoading.setVisibility(View.VISIBLE);
            updateVisualProgress(10, "Buscando backup na nuvem...");
        });

        new Thread(() -> {
            try {
                GoogleDriveHelper driveHelper = new GoogleDriveHelper(this, account);
                File tempDb = new File(getCacheDir(), "recovery_check.db");

                driveHelper.downloadBackup(tempDb, new GoogleDriveHelper.SyncCallback() {
                    @Override
                    public void onSuccess(String message) {
                        analyzeAndRestore(tempDb);
                    }
                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            updateVisualProgress(100, "Iniciando DriveLog...");
                            prefs.edit().putBoolean("first_setup_splash_done", true).apply();
                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> navigateToMain(), 500);
                        });
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> navigateToMain());
            }
        }).start();
    }

    private void analyzeAndRestore(File tempDb) {
        new Thread(() -> {
            try {
                AppDatabase.forceCloseInstance();
                String userId = prefs.getString("current_user_id", "");
                String dbName = "entregas_db_" + userId.replaceAll("[^a-zA-Z0-9]", "_");
                File dbFile = getDatabasePath(dbName);

                try (FileInputStream in = new FileInputStream(tempDb);
                     FileOutputStream out = new FileOutputStream(dbFile)) {
                    byte[] buf = new byte[4096];
                    int len;
                    while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                    out.getFD().sync();
                }

                SettingsSyncHelper.loadPrefsFromDb(this);
                
                // Limpeza do arquivo temporário
                if (tempDb.exists()) tempDb.delete();

                runOnUiThread(() -> {
                    updateVisualProgress(100, "Tudo recuperado com sucesso!");
                    prefs.edit().putBoolean("first_setup_splash_done", true).apply();
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    }, 1200);
                });

            } catch (Exception e) {
                runOnUiThread(() -> navigateToMain());
            }
        }).start();
    }

    private void updateVisualProgress(int percent, String status) {
        if (textLoadingStatus != null) textLoadingStatus.setText(status);
        if (textLoadingPercent != null) textLoadingPercent.setText(percent + "%");
        if (progressLoading != null) progressLoading.setProgress(percent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);

        btnLoginGoogle = findViewById(R.id.btnLoginGoogle);
        layoutLoading = findViewById(R.id.layoutLoading);
        textLoadingStatus = findViewById(R.id.textLoadingStatus);
        textLoadingPercent = findViewById(R.id.textLoadingPercent);
        progressLoading = findViewById(R.id.progressLoading);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail().requestProfile()
                .requestScopes(new com.google.android.gms.common.api.Scope(com.google.api.services.drive.DriveScopes.DRIVE_FILE))
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        btnLoginGoogle.setOnClickListener(v -> loginLauncher.launch(googleSignInClient.getSignInIntent()));

        // Auto-login se já autenticado
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null && FirebaseAuth.getInstance().getCurrentUser() != null) {
            checkNextPermission();
        }
    }

    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void showModernPermissionDialog(String title, String message, Runnable onConfirm) {
        View v = getLayoutInflater().inflate(R.layout.dialog_modern_confirm, null);
        TextView txtTitle = v.findViewById(R.id.textModernTitle);
        TextView txtMessage = v.findViewById(R.id.textModernMessage);
        com.google.android.material.button.MaterialButton btnConfirm = v.findViewById(R.id.btnModernPositive);
        com.google.android.material.button.MaterialButton btnCancel = v.findViewById(R.id.btnModernNegative);

        txtTitle.setText(title);
        txtMessage.setText(message);
        btnConfirm.setText("CONTINUAR");
        btnCancel.setVisibility(View.GONE); // Permissões obrigatórias, não damos opção de cancelar

        AlertDialog dialog = new AlertDialog.Builder(this).setView(v).setCancelable(false).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        btnConfirm.setOnClickListener(v2 -> {
            dialog.dismiss();
            onConfirm.run();
        });
        dialog.show();
    }

    private void checkNextPermission() {
        // Se NÃO estiver logado, não pede nada. Mostra o botão de login.
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account == null || FirebaseAuth.getInstance().getCurrentUser() == null) {
            if (btnLoginGoogle != null) btnLoginGoogle.setVisibility(View.VISIBLE);
            if (layoutLoading != null) layoutLoading.setVisibility(View.GONE);
            return;
        }

        // 1. Localização
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            showModernPermissionDialog(
                "Localização Obrigatória",
                "O DriveLog utiliza o GPS para identificar seu trajeto e mostrar as paradas no mapa em tempo real.\n\nPor favor, selecione 'Enquanto usa o app' na próxima tela.",
                () -> locationPermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
            );
            return;
        }

        // 2. Notificação
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                showModernPermissionDialog(
                    "Alertas e Notificações",
                    "Precisamos de permissão para enviar alertas de perigo e manter o rastreamento ativo em segundo plano.",
                    () -> notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                );
                return;
            }
        }

        // 3. Sobreposição
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            showModernPermissionDialog(
                "Sobrepor Outros Apps",
                "O botão flutuante permite que você volte ao DriveLog rapidamente enquanto utiliza o Google Maps ou Waze.\n\nAtive a chave 'DriveLog' na lista de aplicativos que aparecerá.",
                () -> overlayPermissionLauncher.launch(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())))
            );
            return;
        }

        // 4. Background Location
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            showModernPermissionDialog(
                "Rastreamento de KM",
                "Para calcular sua distância percorrida mesmo com a tela bloqueada, é necessário autorizar o acesso 'Permitir o tempo todo'.\n\nIsso garante a precisão total dos seus ganhos por KM.",
                () -> locationPermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION})
            );
            return;
        }

        // TUDO OK -> Recuperação do Backup (Apenas se for o primeiro setup após login ou reinstalação)
        boolean firstSetupDone = prefs.getBoolean("first_setup_splash_done", false);

        if (!firstSetupDone) {
            checkCloudBackupAndRestore(account);
        } else {
            navigateToMain();
        }
    }
}
