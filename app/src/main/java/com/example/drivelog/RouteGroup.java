package com.example.drivelog;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "route_groups")
public class RouteGroup {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public String color; // Hex color
    public int routeId;

    public RouteGroup() {}

    public RouteGroup(String name, String color, int routeId) {
        this.name = name;
        this.color = color;
        this.routeId = routeId;
    }
}
