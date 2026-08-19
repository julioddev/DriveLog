package com.example.drivelog;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "route_stops",
        foreignKeys = @ForeignKey(entity = RouteHeader.class,
                parentColumns = "id",
                childColumns = "routeId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("routeId")})
public class RouteStop {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public int routeId; 
    
    public String atId;
    public int sequence;
    public String allSequences; 
    public String allAddresses; // Campo para armazenar os endereços originais detalhados
    public int stopNumber;
    public String spxTn;
    public String address;
    public String neighborhood;
    public String city;
    public String zipcode;
    
    public double latitude;
    public double longitude;
    public double originalLatitude; // NOVO: Armazena a lat original da planilha
    public double originalLongitude; // NOVO: Armazena a lon original da planilha
    
    public int deliveryStatus = 0; 
    public int packageCount = 1;
    public int buyerCount = 1; // Novo campo para número de compradores únicos
    
    public int sortOrder = 0;
    public Integer groupId = null;
    
    public long createdAt;
    public long deliveryTimestamp = 0; // Novo: Horário da entrega

    public RouteStop() {}

    public RouteStop(String address, double latitude, double longitude) {
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdAt = System.currentTimeMillis();
        this.packageCount = 1;
    }
}
