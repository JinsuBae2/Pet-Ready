package com.example.pet.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.pet.model.MissionItem;
import com.example.pet.model.ReportSummary;
import com.example.pet.model.ScoreEventItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ScoreRepository {
    private static final String PREF_NAME = "pet_ready_score";
    private static final String KEY_CURRENT_SCORE = "current_score";
    private static final String KEY_LAST_SCORE_DELTA = "last_score_delta";
    private static final String KEY_LAST_SCORE_EVENT = "last_score_event";
    private static final String KEY_SCORE_EVENTS = "score_events";
    private static final String KEY_CARE_SCORE = "care_score";
    private static final String KEY_WALK_SCORE = "walk_score";
    private static final String KEY_MISSION_SCORE = "mission_score";
    private static final String KEY_MISSION_SCORE_PREFIX = "mission_score_";
    private static final int INITIAL_SCORE = 100;
    private static final double WALK_GOAL_KM = 2.0;

    private final SharedPreferences preferences;
    private final Gson gson = new Gson();

    public ScoreRepository(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void applyMissionCompleted(MissionItem mission) {
        if (mission == null) {
            return;
        }

        String key = KEY_MISSION_SCORE_PREFIX + getMissionKey(mission);
        if (preferences.getBoolean(key, false)) {
            return;
        }

        ScoreEvent event = getMissionEvent(mission);
        if (isCareMission(mission)) {
            addCareScore(event.delta);
        } else {
            addMissionScore(event.delta);
        }
        applyCurrentScore(event.type, event.delta);
        preferences.edit().putBoolean(key, true).apply();
    }

    public void applyWalkCompleted(long durationSeconds, float distanceMeters) {
        double achievementRate = (distanceMeters / 1000.0) / WALK_GOAL_KM;
        ScoreEvent event;
        if (achievementRate >= 1.0) {
            event = new ScoreEvent("WALK_FULL", 5);
        } else if (achievementRate >= 0.7) {
            event = new ScoreEvent("WALK_PARTIAL_HIGH", 2);
        } else if (achievementRate >= 0.3) {
            event = new ScoreEvent("WALK_PARTIAL_LOW", -3);
        } else {
            event = new ScoreEvent("WALK_NONE", -5);
        }

        addWalkScore(event.delta);
        applyCurrentScore(event.type, event.delta);
    }

    public ReportSummary getReportSummary() {
        int currentScore = getCurrentScore();
        int careScore = getCareScore();
        int walkScore = getWalkScore();
        int missionScore = getMissionScore();
        return new ReportSummary(
                currentScore,
                preferences.getInt(KEY_LAST_SCORE_DELTA, 0),
                preferences.getString(KEY_LAST_SCORE_EVENT, ""),
                careScore,
                walkScore,
                missionScore,
                buildComment(currentScore, careScore, walkScore, missionScore)
        );
    }

    public List<ScoreEventItem> getRecentScoreEvents() {
        List<ScoreEventItem> events = readScoreEvents();
        Collections.sort(events, (left, right) -> Long.compare(right.occurredAtMillis, left.occurredAtMillis));
        if (events.size() <= 10) {
            return events;
        }
        return new ArrayList<>(events.subList(0, 10));
    }

    public void reset() {
        preferences.edit().clear().apply();
    }

    private ScoreEvent getMissionEvent(MissionItem mission) {
        if (mission.responseTimeSec == null) {
            return new ScoreEvent(isFeedingMission(mission) ? "FEED_ON_TIME" : "MISSION_NORMAL_COMPLETE", isFeedingMission(mission) ? 3 : 2);
        }

        if (mission.responseTimeSec <= 300) {
            return new ScoreEvent("MISSION_FAST_COMPLETE", 5);
        }
        if (mission.responseTimeSec <= 900) {
            return new ScoreEvent("MISSION_NORMAL_COMPLETE", 2);
        }
        if (mission.responseTimeSec <= 1800) {
            return new ScoreEvent("MISSION_LATE_COMPLETE", -3);
        }
        return new ScoreEvent("MISSION_NO_RESPONSE", -10);
    }

    private boolean isCareMission(MissionItem mission) {
        String type = mission.missionType == null ? "" : mission.missionType.toUpperCase(Locale.ROOT);
        return type.contains("FEED")
                || type.contains("CARE")
                || type.contains("VET")
                || type.contains("VACC");
    }

    private boolean isFeedingMission(MissionItem mission) {
        String type = mission.missionType == null ? "" : mission.missionType.toUpperCase(Locale.ROOT);
        String title = mission.title == null ? "" : mission.title;
        return type.contains("FEED") || title.contains("밥") || title.contains("급식") || title.contains("사료");
    }

    private String getMissionKey(MissionItem mission) {
        if (mission.missionIdValue != null && !mission.missionIdValue.isEmpty()) {
            return mission.missionIdValue;
        }
        return String.valueOf(mission.missionId);
    }

    private int getCurrentScore() {
        return preferences.getInt(KEY_CURRENT_SCORE, INITIAL_SCORE);
    }

    private int getCareScore() {
        return preferences.getInt(KEY_CARE_SCORE, INITIAL_SCORE);
    }

    private int getWalkScore() {
        return preferences.getInt(KEY_WALK_SCORE, INITIAL_SCORE);
    }

    private int getMissionScore() {
        return preferences.getInt(KEY_MISSION_SCORE, INITIAL_SCORE);
    }

    private void addCareScore(int delta) {
        saveScore(KEY_CARE_SCORE, getCareScore() + delta);
    }

    private void addWalkScore(int delta) {
        saveScore(KEY_WALK_SCORE, getWalkScore() + delta);
    }

    private void addMissionScore(int delta) {
        saveScore(KEY_MISSION_SCORE, getMissionScore() + delta);
    }

    private void saveScore(String key, int score) {
        int boundedScore = Math.max(0, Math.min(INITIAL_SCORE, score));
        preferences.edit().putInt(key, boundedScore).apply();
    }

    private void applyCurrentScore(String eventType, int delta) {
        int scoreAfter = Math.max(0, Math.min(INITIAL_SCORE, getCurrentScore() + delta));
        List<ScoreEventItem> events = readScoreEvents();
        events.add(new ScoreEventItem(eventType, delta, scoreAfter, System.currentTimeMillis()));

        preferences.edit()
                .putInt(KEY_CURRENT_SCORE, scoreAfter)
                .putInt(KEY_LAST_SCORE_DELTA, delta)
                .putString(KEY_LAST_SCORE_EVENT, eventType)
                .putString(KEY_SCORE_EVENTS, gson.toJson(events))
                .apply();
    }

    private List<ScoreEventItem> readScoreEvents() {
        String json = preferences.getString(KEY_SCORE_EVENTS, "");
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            Type type = new TypeToken<List<ScoreEventItem>>() {}.getType();
            List<ScoreEventItem> events = gson.fromJson(json, type);
            return events == null ? new ArrayList<>() : events;
        } catch (RuntimeException e) {
            return new ArrayList<>();
        }
    }

    private String buildComment(int currentScore, int careScore, int walkScore, int missionScore) {
        if (currentScore >= 95) {
            return "명세서 기준 currentScore 100점에서 좋은 상태를 유지하고 있어요.";
        }
        if (walkScore < careScore && walkScore < missionScore) {
            return "산책 시간이 부족하면 점수가 내려가요. 다음 산책을 조금 더 길게 해보세요.";
        }
        if (careScore < walkScore && careScore < missionScore) {
            return "밥, 병원, 접종 같은 돌봄 미션을 놓치면 점수가 내려가요.";
        }
        return "미션 대응이 늦어지면 감점돼요. 알림이 오면 빠르게 확인해 주세요.";
    }

    private static class ScoreEvent {
        final String type;
        final int delta;

        ScoreEvent(String type, int delta) {
            this.type = type;
            this.delta = delta;
        }
    }
}
