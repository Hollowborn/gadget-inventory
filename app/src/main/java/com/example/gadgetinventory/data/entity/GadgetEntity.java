package com.example.gadgetinventory.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import com.example.gadgetinventory.data.converter.DateConverter;
import java.util.Date;

@Entity(tableName = "gadgets")
public class GadgetEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;

    private String name;
    private String model;
    private String condition;
    
    @TypeConverters(DateConverter.class)
    private Date purchaseDate;
    
    private double estimatedValue;
    private String imageUri;

    // Constructor
    public GadgetEntity(String name, String model, String condition, Date purchaseDate, double estimatedValue, String imageUri) {
        this.name = name;
        this.model = model;
        this.condition = condition;
        this.purchaseDate = purchaseDate;
        this.estimatedValue = estimatedValue;
        this.imageUri = imageUri;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public Date getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(Date purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public double getEstimatedValue() {
        return estimatedValue;
    }

    public void setEstimatedValue(double estimatedValue) {
        this.estimatedValue = estimatedValue;
    }

    public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }
} 