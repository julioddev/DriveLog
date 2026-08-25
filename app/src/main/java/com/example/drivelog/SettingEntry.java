package com.example.drivelog;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "settings")
public class SettingEntry {
    @PrimaryKey
    @NonNull
    public String key;
    public String value;
    public String type; // "string", "int", "float", "long", "boolean"

    public SettingEntry() {}

    @androidx.room.Ignore
    public SettingEntry(@NonNull String key, String value, String type) {
        this.key = key;
        this.value = value;
        this.type = type;
    }
}
