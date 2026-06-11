package com.example.pet.model;

import com.google.gson.annotations.SerializedName;

public class RecommendedAnimal {
    @SerializedName(value = "desertionNo", alternate = {"animalId"})
    public String desertionNo;

    @SerializedName(value = "filename", alternate = {"imageUrl"})
    public String filename;

    public String happenDt;

    @SerializedName(value = "breedType", alternate = {"breed"})
    public String breedType;

    public String colorCd;
    public String sexCd;
    public String neuteredYn;

    @SerializedName(value = "careNm", alternate = {"shelterName"})
    public String careNm;

    public String age;
    public String region;
    public String matchReason;

    public RecommendedAnimal() {
    }

    public RecommendedAnimal(String desertionNo, String filename, String happenDt, String breedType,
                             String colorCd, String sexCd, String neuteredYn, String careNm,
                             String matchReason) {
        this.desertionNo = desertionNo;
        this.filename = filename;
        this.happenDt = happenDt;
        this.breedType = breedType;
        this.colorCd = colorCd;
        this.sexCd = sexCd;
        this.neuteredYn = neuteredYn;
        this.careNm = careNm;
        this.matchReason = matchReason;
    }
}
