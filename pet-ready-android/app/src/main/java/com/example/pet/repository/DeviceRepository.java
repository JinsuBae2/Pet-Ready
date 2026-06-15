package com.example.pet.repository;

import android.content.Context;
import android.content.SharedPreferences;

public class DeviceRepository {
    private static final String PREF_NAME = "pet_ready_device";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String DEMO_DEVICE_ID = "DOG_01";

    private final SharedPreferences preferences;

    public DeviceRepository(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveDeviceId(String deviceId) {
        preferences.edit()
                .putString(KEY_DEVICE_ID, DEMO_DEVICE_ID)
                .apply();
    }

    public String getDeviceId() {
        String savedDeviceId = preferences.getString(KEY_DEVICE_ID, "");
        if (!DEMO_DEVICE_ID.equals(savedDeviceId)) {
            preferences.edit()
                    .putString(KEY_DEVICE_ID, DEMO_DEVICE_ID)
                    .apply();
        }
        return DEMO_DEVICE_ID;
    }

    public void reset() {
        preferences.edit().clear().apply();
    }
}
