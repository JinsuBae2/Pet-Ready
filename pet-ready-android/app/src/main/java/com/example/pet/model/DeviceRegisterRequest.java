package com.example.pet.model;

public class DeviceRegisterRequest {
    public String deviceId;
    public String petName;
    public Double walkGoalKm;

    public DeviceRegisterRequest(String deviceId, String petName, Double walkGoalKm) {
        this.deviceId = deviceId;
        this.petName = petName;
        this.walkGoalKm = walkGoalKm;
    }
}
