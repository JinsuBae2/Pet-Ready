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
    private final SharedPreferences preferences;

    public CareStatusRepository(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        ensureInitialized();
    }

    public CareStatus getStatus() {
        return new CareStatus(
                preferences.getInt(KEY_BATTERY, INITIAL_BATTERY),
                preferences.getInt(KEY_HUNGER, INITIAL_HUNGER),
                preferences.getInt(KEY_AFFINITY, INITIAL_AFFINITY)
        );
    }

    public void increaseAffinityForMission() {
        int current = preferences.getInt(KEY_AFFINITY, INITIAL_AFFINITY);
        preferences.edit()
                .putInt(KEY_AFFINITY, clamp(current + 3))
                .apply();
    }

    public void refillHungerForFeeding() {
        updateHunger(100);
    }

    public void updateHunger(int hungerLevel) {
        preferences.edit()
                .putInt(KEY_HUNGER, clamp(hungerLevel))
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

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
