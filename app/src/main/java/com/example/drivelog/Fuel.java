package com.example.drivelog;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "fuel")
public class Fuel implements java.io.Serializable {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public double value;
    public double pricePerLiter;
    public double liters;
    public int km; // KM at start of fuel
    public double kmDriven; // KM driven with this fuel
    public long date;
    public boolean isCompleted;
    public String fuelType; // "Comum" ou "Aditivada"
    public String gasStation; // Nome do posto
}