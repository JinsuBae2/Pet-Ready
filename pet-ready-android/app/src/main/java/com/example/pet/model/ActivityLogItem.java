package com.example.pet.model;

public class ActivityLogItem {
    public String type;
    public String title;
    public String detail;
    public long timestampMillis;

    public ActivityLogItem() {
    }

    public ActivityLogItem(String type, String title, String detail, long timestampMillis) {
        this.type = type;
        this.title = title;
        this.detail = detail;
        this.timestampMillis = timestampMillis;
    }
}
