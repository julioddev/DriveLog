package com.example.drivelog;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Objects;

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
    public String vehicleLocation; // Localização do pacote no veículo (ex: "Frente", "Porta Malas")
    
    public long createdAt;
    public long deliveryTimestamp = 0; // Novo: Horário da entrega

    public RouteStop() {}

    @Ignore
    public RouteStop(String address, double latitude, double longitude) {
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdAt = System.currentTimeMillis();
        this.packageCount = 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouteStop routeStop = (RouteStop) o;
        if (id > 0 && routeStop.id > 0) {
            return id == routeStop.id;
        }
        return stopNumber == routeStop.stopNumber &&
               routeId == routeStop.routeId &&
               Double.compare(routeStop.latitude, latitude) == 0 &&
               Double.compare(routeStop.longitude, longitude) == 0 &&
               Objects.equals(address, routeStop.address);
    }

    @Override
    public int hashCode() {
        if (id > 0) {
            return Objects.hash(id);
        }
        return Objects.hash(stopNumber, routeId, address, latitude, longitude);
    }
}
