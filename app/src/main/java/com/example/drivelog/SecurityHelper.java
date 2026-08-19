package com.example.drivelog;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import java.security.MessageDigest;

public class SecurityHelper {

    // NOME DE PACOTE OFICIAL
    private static final String OFFICIAL_PACKAGE = "com.example.drivelog";
    
    // HASH DA ASSINATURA (Você deve atualizar este valor com o seu SHA-256 real)
    // Para descobrir o seu, rode o app uma vez e veja o Logcat por "APP_SIGNATURE"
    private static final String OFFICIAL_SIGNATURE_HASH = "uGyVZNmqnVvHT2ukiQ+M0QZvoNAS2g3nIYAKGz/ncY0=";

    public static boolean isAppSafe(Context context) {
        // 1. Verificar Nome do Pacote (Protege contra clones simples)
        if (!context.getPackageName().equals(OFFICIAL_PACKAGE)) {
            Log.e("Seguranca", "Nome do pacote incorreto!");
            return false;
        }

        // 2. Verificar Assinatura (Protege contra edição e re-assinatura do APK)
        if (!isSignatureValid(context)) {
            Log.e("Seguranca", "Assinatura do app invalida!");
            return false;
        }

        // 3. Verificar se está rodando em um ambiente de clonagem (Dual Space, Parallel Space, etc)
        if (isCloned(context)) {
            Log.e("Seguranca", "Ambiente de clonagem detectado!");
            return false;
        }

        return true;
    }

    private static boolean isCloned(Context context) {
        // Caminhos comuns usados por aplicativos de clonagem
        String[] clonePaths = {
            "/data/data/com.ludashi.dualspace",
            "/data/data/com.parallel.space",
            "/data/data/com.lbe.parallel",
            "/data/data/com.excelliance.multiaccounts"
        };

        for (String path : clonePaths) {
            if (new java.io.File(path).exists()) return true;
        }

        // Verifica se o caminho de arquivos do app contém indicadores de clonagem
        String filesPath = context.getFilesDir().getPath();
        if (filesPath.contains("dualspace") || filesPath.contains("parallel") || filesPath.contains("virtual")) {
            return true;
        }

        // 🔥 PROTEÇÃO PARA XIAOMI DUAL APPS
        // A Xiaomi usa o ID de usuário 999 para apps duplicados. 
        // O caminho padrão é /data/user/999/...
        if (filesPath.contains("/999/")) {
            return true;
        }

        return false;
    }

    private static boolean isSignatureValid(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_SIGNATURES);
            
            for (Signature signature : packageInfo.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(signature.toByteArray());
                String currentSignature = Base64.encodeToString(md.digest(), Base64.DEFAULT).trim();
                
                // Log para você descobrir sua chave na primeira vez
                Log.d("APP_SIGNATURE", "Sua assinatura atual: " + currentSignature);

                // Se ainda não definiu a oficial, permitimos (apenas para o desenvolvedor configurar)
                if (OFFICIAL_SIGNATURE_HASH.isEmpty()) return true;
                
                if (OFFICIAL_SIGNATURE_HASH.equals(currentSignature)) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isRunningOnEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }
}
