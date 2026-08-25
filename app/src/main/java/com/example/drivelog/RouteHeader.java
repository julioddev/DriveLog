package com.example.drivelog;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "route_headers")
public class RouteHeader {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String name;
    public long date;
    public boolean isActive = true;
    public boolean isCompleted = false;
    public int failedCount = 0;
    public long startTime = 0;
    public long endTime = 0;
    public long totalPausedMs = 0;
    public long lastPauseStartTime = 0;

    public RouteHeader() {}

    @androidx.room.Ignore
    public RouteHeader(String name) {
        this.name = name;
        this.date = System.currentTimeMillis();
    }
}
