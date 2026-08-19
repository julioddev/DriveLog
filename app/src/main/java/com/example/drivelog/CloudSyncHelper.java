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
    private static final AtomicBoolean isSyncing = new AtomicBoolean(false);

    public static final androidx.lifecycle.MutableLiveData<String> syncLog = new androidx.lifecycle.MutableLiveData<>("Aguardando atividade...");

    public static void syncNow(Context context) {
        if (pendingSync != null) {
            handler.removeCallbacks(pendingSync);
        }

        pendingSync = () -> executeSync(context.getApplicationContext());
        handler.postDelayed(pendingSync, 2000);
    }

    private static final java.util.concurrent.ExecutorService syncExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();

    private static void executeSync(Context context) {
        if (isSyncing.get()) {
            Log.d("CloudSync", "Sincronização em andamento, pulando.");
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSyncTime < SYNC_COOLDOWN_MS) {
            handler.postDelayed(() -> executeSync(context), SYNC_COOLDOWN_MS - (currentTime - lastSyncTime) + 500);
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
        syncLog.postValue("Preparando backup...");

        syncExecutor.execute(() -> {
            try {
                // 1. Salva SharedPreferences no banco (Síncrono)
                SettingsSyncHelper.savePrefsToDb(context);
                
                AppDatabase db = AppDatabase.getInstance(context);
                File tempFile = new File(context.getCacheDir(), "cloud_upload_tmp.db");
                
                // 2. Limpa arquivo temporário anterior se existir
                if (tempFile.exists()) tempFile.delete();

                // 3. Tenta usar VACUUM INTO, mas captura erro silenciosamente se a versão do SQLite não suportar
                try {
                    db.getOpenHelper().getWritableDatabase().execSQL("VACUUM INTO '" + tempFile.getPath() + "'");
                    Log.d("CloudSync", "Backup criado usando VACUUM INTO");
                } catch (Exception e) {
                    // Logamos apenas no Logcat, sem estourar exception para o usuário
                    Log.w("CloudSync", "VACUUM INTO não suportado ou falhou, usando cópia manual");

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

                syncLog.postValue("Enviando para o Drive...");

                // 4. TESTE DE INTEGRIDADE LOCAL ANTES DO ENVIO
                int testCount = 0;
                try (android.database.sqlite.SQLiteDatabase dbCheck = android.database.sqlite.SQLiteDatabase.openDatabase(tempFile.getPath(), null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY)) {
                    try (android.database.Cursor c = dbCheck.rawQuery("SELECT COUNT(*) FROM earnings", null)) {
                        if (c.moveToFirst()) testCount = c.getInt(0);
                    }
                } catch (Exception e) {
                    Log.e("CloudSync", "Erro ao validar arquivo temporário", e);
                }

                // 5. Upload via GoogleDriveHelper
                GoogleDriveHelper driveHelper = new GoogleDriveHelper(context, account);
                driveHelper.syncBackupSync(tempFile);
                
                String time = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
                syncLog.postValue("[" + time + "] Nuvem: Salvo (" + testCount + " registros)");
                Log.i("CloudSync", "Backup na nuvem concluído com sucesso: " + testCount + " registros.");

            } catch (Exception e) {
                Log.e("CloudSync", "Erro na sincronização", e);
                syncLog.postValue("Erro: " + (e.getMessage() != null ? e.getMessage() : "Erro desconhecido"));
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
        if (enabled) syncNow(context);
    }
}
