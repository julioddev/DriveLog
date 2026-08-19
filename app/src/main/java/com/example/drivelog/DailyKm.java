package com.example.drivelog;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "daily_km")
public class DailyKm implements java.io.Serializable {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public double kmStart;
    public double kmEnd;
    public double totalKm;
    public double gpsDistance; // Nova coluna para km detectado pelo Maps
    public double estimatedFuelCost;
    public double consumptionUsed;
    public long date;
    public boolean isCompleted;
    public boolean isAutomatic; // Flag para identificar se foi gerado pelo GPS
}
