package com.example.pet.model;

import com.google.gson.annotations.SerializedName;

public class MissionItem {
    @SerializedName(value = "missionId", alternate = {"id"})
    public long missionId;

    public String missionIdValue;

    @SerializedName(value = "title", alternate = {"name"})
    public String title;

    @SerializedName(value = "completed", alternate = {"isCompleted", "success"})
    public boolean completed;

    public String description;

    @SerializedName(value = "missionType", alternate = {"type"})
    public String missionType;

    public String issuedAt;
    public String respondedAt;
    public Long responseTimeSec;

    public MissionItem() {
    }

    public MissionItem(long missionId, String title, boolean completed) {
        this.missionId = missionId;
        this.missionIdValue = String.valueOf(missionId);
        this.title = title;
        this.completed = completed;
    }
}
