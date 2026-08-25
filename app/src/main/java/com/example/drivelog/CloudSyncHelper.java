package com.example.drivelog;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public class CloudSyncHelper {

    private static final String PREF_AUTO_BACKUP = "auto_backup_cloud";
    private static long lastSyncTime = 0;
    private static final long SYNC_COOLDOWN_MS = 5000;
    private static final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private static Runnable pendingSync = null;
    private static String currentReason = "Atividade detectada";
    private static final AtomicBoolean isSyncing = new AtomicBoolean(false);

    public static final androidx.lifecycle.MutableLiveData<String> syncLog = new androidx.lifecycle.MutableLiveData<>("Aguardando atividade...");

    /**
     * Sincronização padrão sem motivo específico
     */
    public static void syncNow(Context context) {
        syncNow(context, "Atividade detectada");
    }

    /**
     * Aciona a sincronização com a nuvem informando o que mudou
     */
    public static void syncNow(Context context, String reason) {
        if (pendingSync != null) {
            handler.removeCallbacks(pendingSync);
        }

        currentReason = reason;
        syncLog.postValue("Pendente: " + reason + "...");

        pendingSync = () -> executeSync(context.getApplicationContext(), reason);
        // 🔥 Reduzido para 500ms para ser "imediato"
        handler.postDelayed(pendingSync, 500);
    }

    private static final java.util.concurrent.ExecutorService syncExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();

    private static void executeSync(Context context, String reason) {
        if (isSyncing.get()) {
            Log.d("CloudSync", "Sincronização em andamento, pulando.");
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSyncTime < SYNC_COOLDOWN_MS) {
            handler.postDelayed(() -> executeSync(context, reason), SYNC_COOLDOWN_MS - (currentTime - lastSyncTime) + 500);
            return;
        }

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account == null) {
            syncLog.postValue("Google não conectado.");
            return;
        }

        if (!GoogleSignIn.hasPermissions(account, new com.google.android.gms.common.api.Scope(com.google.api.services.drive.DriveScopes.DRIVE_FILE))) {
            syncLog.postValue("Sem permissão para o Drive.");
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        boolean autoBackup = prefs.getBoolean(PREF_AUTO_BACKUP, true);
        if (!autoBackup) {
            syncLog.postValue("Backup automático pausado.");
            return;
        }

        isSyncing.set(true);
        lastSyncTime = System.currentTimeMillis();
        syncLog.postValue("☁️ Enviando: " + reason + "...");

        syncExecutor.execute(() -> {
            try {
                // 1. Salva SharedPreferences no banco (Síncrono)
                SettingsSyncHelper.savePrefsToDb(context);
                
                AppDatabase db = AppDatabase.getInstance(context);
                File tempFile = new File(context.getCacheDir(), "cloud_upload_tmp.db");
                
                if (tempFile.exists()) tempFile.delete();

                try {
                    db.getOpenHelper().getWritableDatabase().execSQL("VACUUM INTO '" + tempFile.getPath() + "'");
                } catch (Exception e) {
                    String dbName = db.getOpenHelper().getDatabaseName();
                    File dbFile = context.getDatabasePath(dbName);
                    synchronized (AppDatabase.class) {
                        try (java.io.FileInputStream in = new java.io.FileInputStream(dbFile);
                             java.io.FileOutputStream out = new java.io.FileOutputStream(tempFile)) {
                            byte[] buf = new byte[4096];
                            int len;
                            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                            out.getFD().sync();
                        }
                    }
                }

                // Upload via GoogleDriveHelper
                GoogleDriveHelper driveHelper = new GoogleDriveHelper(context, account);
                driveHelper.syncBackupSync(tempFile);
                
                String time = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
                syncLog.postValue("✅ [" + time + "] Nuvem salva: " + reason);
                Log.i("CloudSync", "Backup concluído: " + reason);

            } catch (Exception e) {
                Log.e("CloudSync", "Erro na sincronização", e);
                syncLog.postValue("❌ Erro: " + (e.getMessage() != null ? e.getMessage() : "Erro desconhecido"));
            } finally {
                isSyncing.set(false);
            }
        });
    }

    public static boolean isAutoBackupEnabled(Context context) {
        return context.getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
                .getBoolean(PREF_AUTO_BACKUP, true);
    }

    public static void setAutoBackupEnabled(Context context, boolean enabled) {
        context.getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
                .edit()
                .putBoolean(PREF_AUTO_BACKUP, enabled)
                .apply();
        if (enabled) syncNow(context, "Backup Ativado");
    }
}
