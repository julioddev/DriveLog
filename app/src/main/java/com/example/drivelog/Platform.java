package com.example.drivelog;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "platforms", indices = {@Index(value = "name", unique = true)})
public class Platform {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public boolean isEnabled;
    public double defaultValue;
    public boolean isDefault = false;
    public int orderIndex;

    public Platform() {}

    @Ignore
    public Platform(String name, boolean isEnabled, double defaultValue, int orderIndex) {
        this.name = name;
        this.isEnabled = isEnabled;
        this.defaultValue = defaultValue;
        this.orderIndex = orderIndex;
    }
}
