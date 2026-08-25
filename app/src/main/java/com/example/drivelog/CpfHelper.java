package com.example.drivelog;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;
import java.util.Random;

public class CpfHelper {
    /**
     * Gera um novo CPF fictício e copia para a área de transferência.
     */
    public static void generateAndCopyCpf(Context context) {
        String cpf = generateFakeCpf();
        android.util.Log.d("CpfHelper", "CPF Gerado: " + cpf);
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("CPF fictício", cpf);
            clipboard.setPrimaryClip(clip);
            // Cópia realizada de forma silenciosa para não interromper o motorista
        }
    }

    private static String generateFakeCpf() {
        Random random = new Random();
        int[] digits = new int[11];
        
        boolean allSame = true;
        for (int i = 0; i < 9; i++) {
            digits[i] = random.nextInt(10);
            if (i > 0 && digits[i] != digits[i-1]) allSame = false;
        }
        if (allSame) return generateFakeCpf();

        digits[9] = calculateCpfDigit(digits, 10);
        digits[10] = calculateCpfDigit(digits, 11);
        
        StringBuilder sb = new StringBuilder();
        for (int d : digits) sb.append(d);
        return sb.toString();
    }

    private static int calculateCpfDigit(int[] digits, int weight) {
        int sum = 0;
        for (int i = 0; i < weight - 1; i++) sum += digits[i] * (weight - i);
        int remainder = sum % 11;
        return (remainder < 2) ? 0 : 11 - remainder;
    }
}
