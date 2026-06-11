package com.example.pet.model;

/*
 * 반려견 로봇 상태 전송 요청 데이터
 * 현재 백엔드에는 ESP32가 센서 값을 보내는 POST /pet/status API가 먼저 구현되어 있다.
 */
public class PetStatusRequest {
    public String deviceId;
    public Boolean headTouch;
    public Boolean backTouch1;
    public Boolean backTouch2;

    public PetStatusRequest(
            String deviceId,
            Boolean headTouch,
            Boolean backTouch1,
            Boolean backTouch2
    ) {
        this.deviceId = deviceId;
        this.headTouch = headTouch;
        this.backTouch1 = backTouch1;
        this.backTouch2 = backTouch2;
    }
}
