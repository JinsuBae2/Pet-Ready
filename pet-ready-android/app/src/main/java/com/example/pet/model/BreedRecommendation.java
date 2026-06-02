package com.example.pet.model;

public class BreedRecommendation {
    public String type;
    public String examples;
    public String reason;

    public BreedRecommendation(String type, String examples, String reason) {
        this.type = type;
        this.examples = examples;
        this.reason = reason;
    }
}
