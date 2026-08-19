package com.example.drivelog;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "maintenance")
public class Maintenance implements java.io.Serializable {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String description;
    public double value;
    public int km;
    public long date;
    public String type; // "Emergencial" ou "Recorrente"
    public int intervalKm; // Intervalo para próxima manutenção (ex: 5000)
    public int alertKm; // KM em que deve ocorrer o alerta
}