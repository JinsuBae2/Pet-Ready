package com.example.pet.model;

public class ScoreEventItem {
    public String eventType;
    public int delta;
    public int scoreAfter;
    public long occurredAtMillis;

    public ScoreEventItem() {
    }

    public ScoreEventItem(String eventType, int delta, int scoreAfter, long occurredAtMillis) {
        this.eventType = eventType;
        this.delta = delta;
        this.scoreAfter = scoreAfter;
        this.occurredAtMillis = occurredAtMillis;
    }
}
