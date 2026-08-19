package com.example.drivelog;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SettingsSyncHelper {
    private static final String PREFS_NAME = "AppConfig";

    public static void savePrefsToDb(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        AppDao dao = AppDatabase.getInstance(context).appDao();
        
        Map<String, ?> allEntries = prefs.getAll();
        List<SettingEntry> entries = new ArrayList<>();
        
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String type = "string";
            
            if (value instanceof Integer) type = "int";
            else if (value instanceof Boolean) type = "boolean";
            else if (value instanceof Float) type = "float";
            else if (value instanceof Long) type = "long";
            
            entries.add(new SettingEntry(key, String.valueOf(value), type));
        }
        
        // Executa a limpeza e inserção de forma síncrona dentro da thread que chamou savePrefsToDb
        // Normalmente chamado por CloudSyncHelper que já está em uma thread separada
        dao.clearSettings();
        dao.insertSettings(entries);
    }

    public static void loadPrefsFromDb(Context context) {
        AppDao dao = AppDatabase.getInstance(context).appDao();
        List<SettingEntry> entries = dao.getAllSettings();
        
        if (entries == null || entries.isEmpty()) return;
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        for (SettingEntry entry : entries) {
            try {
                String val = entry.value;
                switch (entry.type) {
                    case "int": editor.putInt(entry.key, Integer.parseInt(val)); break;
                    case "boolean": editor.putBoolean(entry.key, Boolean.parseBoolean(val)); break;
                    case "float": editor.putFloat(entry.key, Float.parseFloat(val)); break;
                    case "long": editor.putLong(entry.key, Long.parseLong(val)); break;
                    default: editor.putString(entry.key, val); break;
                }
            } catch (Exception e) {
                android.util.Log.e("SettingsSyncHelper", "Error restoring key: " + entry.key, e);
            }
        }
        editor.commit(); // commit imediato para garantir que a UI veja as mudanças ao reiniciar
    }
}
