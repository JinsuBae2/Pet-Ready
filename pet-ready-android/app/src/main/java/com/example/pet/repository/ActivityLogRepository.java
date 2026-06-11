package com.example.pet.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.pet.model.ActivityLogItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ActivityLogRepository {
    public static final String TYPE_MISSION = "MISSION";
    public static final String TYPE_WALK = "WALK";
    public static final String TYPE_URGENT = "URGENT";

    private static final String PREF_NAME = "pet_ready_activity_logs";
    private static final String KEY_LOGS = "logs";

    private final SharedPreferences preferences;
    private final Gson gson = new Gson();

    public ActivityLogRepository(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void addMissionCompleted(String title) {
        addLog(TYPE_MISSION, title, "완료");
    }

    public void addWalkStarted() {
        addLog(TYPE_WALK, "산책 시작", "산책 기록");
    }

    public void addWalkEnded(String distance, String duration) {
        addLog(TYPE_WALK, "산책 마무리", duration + " · " + distance);
    }

    public void addUrgentMissionAlert(String title) {
        addLog(TYPE_URGENT, title, "긴급 알림 도착");
    }

    public void addUrgentMissionCompleted(String title) {
        addLog(TYPE_URGENT, title, "대응 완료");
    }

    public List<ActivityLogItem> getLogs() {
        List<ActivityLogItem> logs = readLogs();
        Collections.sort(logs, (left, right) -> Long.compare(right.timestampMillis, left.timestampMillis));
        return logs;
    }

    public void reset() {
        preferences.edit().clear().apply();
    }

    private void addLog(String type, String title, String detail) {
        List<ActivityLogItem> logs = readLogs();
        logs.add(new ActivityLogItem(type, title, detail, System.currentTimeMillis()));
        preferences.edit().putString(KEY_LOGS, gson.toJson(logs)).apply();
    }

    private List<ActivityLogItem> readLogs() {
        String json = preferences.getString(KEY_LOGS, "");
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            Type type = new TypeToken<List<ActivityLogItem>>() {}.getType();
            List<ActivityLogItem> logs = gson.fromJson(json, type);
            return logs == null ? new ArrayList<>() : logs;
        } catch (RuntimeException e) {
            return new ArrayList<>();
        }
    }
}
