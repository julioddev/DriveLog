package com.example.drivelog;

import android.content.Context;
import android.util.Log;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GoogleDriveHelper {
    private final Drive mDriveService;
    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private static final String BACKUP_FILE_NAME = "drivelog_backup.db";

    public GoogleDriveHelper(Context context, GoogleSignInAccount account) {
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                context, Collections.singleton(DriveScopes.DRIVE_FILE));
        credential.setSelectedAccount(account.getAccount());

        mDriveService = new Drive.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                request -> {
                    credential.initialize(request);
                    request.setConnectTimeout(60000);
                    request.setReadTimeout(60000);
                })
                .setApplicationName("DriveLog")
                .build();
    }

    public void syncBackup(java.io.File localFile, SyncCallback callback) {
        mExecutor.execute(() -> {
            try {
                syncBackupSync(localFile);
                callback.onSuccess("Sincronizado com sucesso!");
            } catch (IOException e) {
                Log.e("GoogleDriveHelper", "Erro no envio", e);
                callback.onError(e.getMessage());
            }
        });
    }

    public void syncBackupSync(java.io.File localFile) throws IOException {
        String fileId = null;
        
        for (int i = 0; i < 2; i++) {
            try {
                FileList result = mDriveService.files().list()
                        .setQ("name = '" + BACKUP_FILE_NAME + "' and trashed = false")
                        .setSpaces("drive")
                        .setFields("files(id)")
                        .execute();
                if (!result.getFiles().isEmpty()) {
                    fileId = result.getFiles().get(0).getId();
                }
                break;
            } catch (IOException e) {
                if (i == 1) throw e;
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
        }

        File metadata = new File().setName(BACKUP_FILE_NAME);
        metadata.setAppProperties(Collections.singletonMap("app_id", "com.example.entregas"));
        FileContent content = new FileContent("application/octet-stream", localFile);

        if (fileId == null) {
            Log.d("GoogleDriveHelper", "Criando novo arquivo de backup");
            mDriveService.files().create(metadata, content).execute();
        } else {
            Log.d("GoogleDriveHelper", "Atualizando arquivo de backup existente: " + fileId);
            // Ao enviar um objeto File vazio no update, forçamos o Drive a focar apenas na atualização do CONTEÚDO (binário)
            mDriveService.files().update(fileId, new File(), content).execute();
        }
    }

    public void downloadBackup(java.io.File localFile, SyncCallback callback) {
        mExecutor.execute(() -> {
            try {
                FileList result = mDriveService.files().list()
                        .setQ("name = '" + BACKUP_FILE_NAME + "' and trashed = false")
                        .setSpaces("drive")
                        .setOrderBy("modifiedTime desc") // Garante que a versão mais nova venha primeiro
                        .setFields("files(id, name, modifiedTime)")
                        .execute();

                if (result.getFiles().isEmpty()) {
                    callback.onError("Backup não encontrado no Drive.");
                    return;
                }

                String fileId = result.getFiles().get(0).getId();
                try (OutputStream outputStream = new FileOutputStream(localFile)) {
                    mDriveService.files().get(fileId)
                            .executeMediaAndDownloadTo(outputStream);
                }

                callback.onSuccess("Backup baixado!");
            } catch (IOException e) {
                Log.e("GoogleDriveHelper", "Erro no download", e);
                callback.onError(e.getMessage());
            }
        });
    }

    public void checkBackupExists(BackupCheckCallback callback) {
        mExecutor.execute(() -> {
            try {
                FileList result = mDriveService.files().list()
                        .setQ("name = '" + BACKUP_FILE_NAME + "' and trashed = false")
                        .setSpaces("drive")
                        .setFields("files(id)")
                        .execute();
                callback.onResult(!result.getFiles().isEmpty());
            } catch (IOException e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public interface SyncCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public interface ProgressCallback extends SyncCallback {
        void onProgress(int percent);
    }

    public interface BackupCheckCallback {
        void onResult(boolean exists);
        void onError(String error);
    }
}
