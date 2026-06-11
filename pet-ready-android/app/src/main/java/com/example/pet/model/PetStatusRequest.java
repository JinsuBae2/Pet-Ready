package com.example.pet.model;

/*
 * 반려견 로봇 상태 전송 요청 데이터
 * 현재 백엔드에는 ESP32가 센서 값을 보내는 POST /pet/status API가 먼저 구현되어 있다.
 */
public class PetStatusRequest {
    public String deviceId;
    public Integer batteryLevel;
    public Boolean isCharging;
    public Boolean touchActive;
    public Double pressureValue;

    public PetStatusRequest(String deviceId, Integer batteryLevel, Boolean isCharging,
                            Boolean touchActive, Double pressureValue) {
        this.deviceId = deviceId;
        this.batteryLevel = batteryLevel;
        this.isCharging = isCharging;
        this.touchActive = touchActive;
        this.pressureValue = pressureValue;
    }
}
