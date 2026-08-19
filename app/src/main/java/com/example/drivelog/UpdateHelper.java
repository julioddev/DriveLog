package com.example.drivelog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateHelper {

    // Substitua pelo link real do seu arquivo JSON de versão no GitHub ou Servidor
    private static final String VERSION_JSON_URL = "https://julioddev.github.io/DriveLog/update.json";

    public interface UpdateCallback {
        void onNoUpdate();
        void onError(String error);
    }

    public static void checkForUpdates(Activity activity, boolean showToastIfLatest, UpdateCallback callback) {
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(VERSION_JSON_URL).openConnection();
                conn.setConnectTimeout(5000);
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject json = new JSONObject(sb.toString());
                int remoteVersion = json.getInt("versionCode");
                String remoteName = json.getString("versionName");
                String apkUrl = json.getString("apkUrl");
                String notes = json.optString("releaseNotes", "");

                int localVersion = BuildConfig.VERSION_CODE;

                if (remoteVersion > localVersion) {
                    activity.runOnUiThread(() -> showUpdateDialog(activity, remoteName, apkUrl, notes));
                } else {
                    activity.runOnUiThread(() -> {
                        if (showToastIfLatest) Toast.makeText(activity, "Você já está na versão mais recente!", Toast.LENGTH_SHORT).show();
                        if (callback != null) callback.onNoUpdate();
                    });
                }

            } catch (Exception e) {
                activity.runOnUiThread(() -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
            }
        }).start();
    }

    private static void showUpdateDialog(Activity activity, String name, String url, String notes) {
        new AlertDialog.Builder(activity)
                .setTitle("Nova Versão Disponível (" + name + ")")
                .setMessage("Deseja baixar a atualização agora?\n\nO que há de novo:\n" + notes)
                .setPositiveButton("BAIXAR E INSTALAR", (d, w) -> startDownload(activity, url))
                .setNegativeButton("MAIS TARDE", null)
                .show();
    }

    private static void startDownload(Context context, String url) {
        Toast.makeText(context, "Iniciando download...", Toast.LENGTH_LONG).show();

        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk");
        if (file.exists()) file.delete();

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url))
                .setTitle("DriveLog Update")
                .setDescription("Baixando nova versão...")
                .setDestinationUri(Uri.fromFile(file))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm == null) return;
        
        long downloadId = dm.enqueue(request);

        // Receiver para instalar assim que o download terminar
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id == downloadId) {
                    installApk(c, file);
                    c.unregisterReceiver(this);
                }
            }
        };

        // Correção para Android 14+: Define se o receiver é exportado ou não
        ContextCompat.registerReceiver(context, receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED);
    }

    private static void installApk(Context context, File file) {
        try {
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.getPackageManager().canRequestPackageInstalls()) {
                context.startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.getPackageName())));
                Toast.makeText(context, "Ative a permissão para instalar a atualização.", Toast.LENGTH_LONG).show();
                return;
            }

            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Erro ao iniciar instalação: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
