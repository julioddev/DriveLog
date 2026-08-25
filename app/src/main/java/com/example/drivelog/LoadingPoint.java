package com.example.drivelog;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "loading_points")
public class LoadingPoint {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public double latitude;
    public double longitude;
    public String platformName;

    public LoadingPoint() {}

    @androidx.room.Ignore
    public LoadingPoint(String name, double latitude, double longitude, String platformName) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.platformName = platformName;
    }
}
