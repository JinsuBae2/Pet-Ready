package com.example.pet.repository;

import android.content.Context;
import android.content.SharedPreferences;

public class SimulationRepository {
    private static final String PREF_NAME = "pet_ready_simulation";
    private static final String KEY_SETUP_DONE = "setup_done";
    private static final String KEY_STARTED_AT = "started_at";
    private static final String KEY_DURATION_WEEKS = "duration_weeks";
    private static final String KEY_FINAL_REPORT_DISMISSED = "final_report_dismissed";
    private static final int DEFAULT_DURATION_WEEKS = 3;
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    private final SharedPreferences preferences;

    public SimulationRepository(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void startSimulation(int durationWeeks) {
        int boundedWeeks = Math.max(1, Math.min(3, durationWeeks));
        preferences.edit()
                .putBoolean(KEY_SETUP_DONE, true)
                .putLong(KEY_STARTED_AT, System.currentTimeMillis())
                .putInt(KEY_DURATION_WEEKS, boundedWeeks)
                .putBoolean(KEY_FINAL_REPORT_DISMISSED, false)
                .apply();
    }

    public boolean isSetupDone() {
        return preferences.getBoolean(KEY_SETUP_DONE, false);
    }

    public int getCurrentDay() {
        long startedAt = preferences.getLong(KEY_STARTED_AT, System.currentTimeMillis());
        long elapsedDays = Math.max(0L, (System.currentTimeMillis() - startedAt) / DAY_MS);
        return (int) elapsedDays + 1;
    }

    public int getTotalDays() {
        return getDurationWeeks() * 7;
    }

    public int getDurationWeeks() {
        return preferences.getInt(KEY_DURATION_WEEKS, DEFAULT_DURATION_WEEKS);
    }

    public boolean isFinished() {
        return getCurrentDay() > getTotalDays();
    }

    public boolean shouldShowFinalReport() {
        return isFinished() && !preferences.getBoolean(KEY_FINAL_REPORT_DISMISSED, false);
    }

    public void dismissFinalReport() {
        preferences.edit()
                .putBoolean(KEY_FINAL_REPORT_DISMISSED, true)
                .apply();
    }

    public void forceFinishSimulation() {
        long completedStartedAt = System.currentTimeMillis()
                - ((long) getTotalDays() + 1L) * DAY_MS;
        preferences.edit()
                .putBoolean(KEY_SETUP_DONE, true)
                .putLong(KEY_STARTED_AT, completedStartedAt)
                .putBoolean(KEY_FINAL_REPORT_DISMISSED, false)
                .apply();
    }

    public void reset() {
        preferences.edit().clear().apply();
    }
}
