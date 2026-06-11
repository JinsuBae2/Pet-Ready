package com.example.pet.model;

public class CareStatus {
    public int batteryLevel;
    public int hungerLevel;
    public int affinityLevel;

    public CareStatus(int batteryLevel, int hungerLevel, int affinityLevel) {
        this.batteryLevel = batteryLevel;
        this.hungerLevel = hungerLevel;
        this.affinityLevel = affinityLevel;
    }
}
