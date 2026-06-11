package com.example.pet.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.pet.model.CareStatus;

public class CareStatusRepository {
    private static final String PREF_NAME = "pet_ready_care_status";
    private static final String KEY_BATTERY = "battery";
    private static final String KEY_HUNGER = "hunger";
    private static final String KEY_AFFINITY = "affinity";
    private static final String KEY_LAST_UPDATED = "last_updated";
    private static final int INITIAL_BATTERY = 100;
    private static final int INITIAL_HUNGER = 100;
    private static final int INITIAL_AFFINITY = 30;
    private static final long ONE_HOUR_MS = 60L * 60L * 1000L;
    private static final long FEED_INTERVAL_MS = 12L * ONE_HOUR_MS;

    private final SharedPreferences preferences;

    public CareStatusRepository(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        ensureInitialized();
    }

    public CareStatus getStatus() {
        applyTimeDecay();
        return new CareStatus(
                preferences.getInt(KEY_BATTERY, INITIAL_BATTERY),
                preferences.getInt(KEY_HUNGER, INITIAL_HUNGER),
                preferences.getInt(KEY_AFFINITY, INITIAL_AFFINITY)
        );
    }

    public void increaseAffinityForMission() {
        applyTimeDecay();
        int current = preferences.getInt(KEY_AFFINITY, INITIAL_AFFINITY);
        preferences.edit()
                .putInt(KEY_AFFINITY, clamp(current + 3))
                .apply();
    }

    public void refillHungerForFeeding() {
        applyTimeDecay();
        long now = System.currentTimeMillis();
        preferences.edit()
                .putInt(KEY_HUNGER, 100)
                .putLong(KEY_LAST_UPDATED, now)
                .apply();
    }

    public void reset() {
        preferences.edit()
                .putInt(KEY_BATTERY, INITIAL_BATTERY)
                .putInt(KEY_HUNGER, INITIAL_HUNGER)
                .putInt(KEY_AFFINITY, INITIAL_AFFINITY)
                .putLong(KEY_LAST_UPDATED, System.currentTimeMillis())
                .apply();
    }

    private void ensureInitialized() {
        if (preferences.contains(KEY_LAST_UPDATED)) {
            return;
        }
        reset();
    }

    private void applyTimeDecay() {
        long now = System.currentTimeMillis();
        long lastUpdated = preferences.getLong(KEY_LAST_UPDATED, now);
        long elapsedHours = Math.max(0L, (now - lastUpdated) / ONE_HOUR_MS);
        if (elapsedHours <= 0L) {
            return;
        }

        int batteryDrop = (int) elapsedHours;
        int hungerDrop = calculateHungerDrop(now, lastUpdated);
        int battery = clamp(preferences.getInt(KEY_BATTERY, INITIAL_BATTERY) - batteryDrop);
        int hunger = clamp(preferences.getInt(KEY_HUNGER, INITIAL_HUNGER) - hungerDrop);

        preferences.edit()
                .putInt(KEY_BATTERY, battery)
                .putInt(KEY_HUNGER, hunger)
                .putLong(KEY_LAST_UPDATED, lastUpdated + elapsedHours * ONE_HOUR_MS)
                .apply();
    }

    private int calculateHungerDrop(long now, long lastUpdated) {
        long elapsedMs = Math.max(0L, now - lastUpdated);
        double mealProgress = Math.min(1.0, elapsedMs / (double) FEED_INTERVAL_MS);
        return (int) Math.round(mealProgress * 50.0);
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
