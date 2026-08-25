package com.example.drivelog;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "route_points")
public class RoutePoint {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int dailyKmId;
    public double latitude;
    public double longitude;
    public long timestamp;

    public RoutePoint() {}

    @androidx.room.Ignore
    public RoutePoint(int dailyKmId, double latitude, double longitude, long timestamp) {
        this.dailyKmId = dailyKmId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }
}
