package com.example.drivelog;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "corrected_addresses")
public class CorrectedAddress {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String address;
    public String neighborhood; // Campo para separar por pastas/bairros
    public String city; // Campo para separar por cidades
    public double latitude;
    public double longitude;
    public String notes; // Observação pessoal/comunidade
    public boolean isNotePublic; // Se a nota deve ser compartilhada
    public long updatedAt;

    @androidx.room.Ignore
    public int likes;
    @androidx.room.Ignore
    public int dislikes;
    @androidx.room.Ignore
    public int commentCount;
    public String creatorId; // ID do usuário que enviou para a comunidade

    public CorrectedAddress() {}

    public CorrectedAddress(String address, String neighborhood, double latitude, double longitude) {
        this.address = address;
        this.neighborhood = neighborhood;
        this.latitude = latitude;
        this.longitude = longitude;
        this.updatedAt = System.currentTimeMillis();
    }

    public CorrectedAddress(String address, String neighborhood, String city, double latitude, double longitude) {
        this.address = address;
        this.neighborhood = neighborhood;
        this.city = city;
        this.latitude = latitude;
        this.longitude = longitude;
        this.updatedAt = System.currentTimeMillis();
    }
}
