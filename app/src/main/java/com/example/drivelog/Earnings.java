package com.example.drivelog;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "earnings")
public class Earnings implements java.io.Serializable {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public double baseValue;
    public double extraValue;
    public double totalValue;
    public String platforms;
    public long date;
    public boolean isCompleted;
}