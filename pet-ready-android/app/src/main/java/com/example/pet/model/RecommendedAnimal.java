package com.example.pet.model;

public class RecommendedAnimal {
    public String desertionNo;
    public String filename;
    public String happenDt;
    public String breedType;
    public String colorCd;
    public String sexCd;
    public String neuteredYn;
    public String careNm;
    public String matchReason;

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
