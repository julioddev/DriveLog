package com.example.drivelog;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;
import java.util.Random;

public class CpfHelper {
    public static void generateAndCopyCpf(Context context) {
        String cpf = generateFakeCpf();
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("CPF", cpf);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            // Mensagem removida para tornar a cópia silenciosa
        }
    }

    private static String generateFakeCpf() {
        Random random = new Random();
        int[] digits = new int[11];
        // Gera os 9 primeiros dígitos
        for (int i = 0; i < 9; i++) digits[i] = random.nextInt(10);
        // Calcula os dígitos verificadores
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
