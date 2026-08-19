package com.example.drivelog;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "gas_stations", indices = {@Index(value = "name", unique = true)})
public class GasStation {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public boolean isEnabled = true;
    public boolean isDefault = false;
    public int orderIndex;

    public GasStation() {}

    @Ignore
    public GasStation(String name, int orderIndex) {
        this.name = name;
        this.orderIndex = orderIndex;
    }
}
