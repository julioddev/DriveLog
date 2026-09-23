package com.example.drivelog;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import java.io.Serializable;

@Entity(tableName = "corrected_quadras")
public class CorrectedQuadra implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name; // Ex: "QD 104", "Quadra 504 Norte", "Conjunto B"
    public String neighborhood; // Bairro
    public String city; // Cidade
    public double latitude;
    public double longitude;
    public String notes; // Observação
    public long updatedAt;
    public String creatorId; // ID do usuário criador
    public String creatorName;
    public String type; // "QUADRA" ou "BLOCO"

    @Ignore
    public int likes;
    @Ignore
    public int dislikes;
    @Ignore
    public String docId; // ID no Firestore

    public CorrectedQuadra() {}

    @Ignore
    public CorrectedQuadra(String name, String neighborhood, String city, double latitude, double longitude) {
        this.name = name;
        this.neighborhood = neighborhood;
        this.city = city;
        this.latitude = latitude;
        this.longitude = longitude;
        this.updatedAt = System.currentTimeMillis();
    }
}
