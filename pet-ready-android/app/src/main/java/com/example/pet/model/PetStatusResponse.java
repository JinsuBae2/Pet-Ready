package com.example.pet.model;

import com.google.gson.annotations.SerializedName;

/*
 * 반려견 상태 분석 응답 데이터
 * Jackson/Gson의 boolean 필드명 차이를 고려해 hungry, isHungry 둘 다 받을 수 있게 처리한다.
 */
public class PetStatusResponse {
    @SerializedName(value = "hungry", alternate = {"isHungry"})
    public boolean hungry;

    public String mood;
    public String healthStatus;
    public String analysisMessage;
    public String lcdCommand;
    public String lcdTextLine1;
    public String lcdTextLine2;
    public String ledColor;
}
