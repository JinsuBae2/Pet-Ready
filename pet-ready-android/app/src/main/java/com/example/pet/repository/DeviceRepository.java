package com.example.pet.repository;

import android.content.Context;
import android.content.SharedPreferences;

public class DeviceRepository {
    private static final String PREF_NAME = "pet_ready_device";
    private static final String KEY_DEVICE_ID = "device_id";

    private final SharedPreferences preferences;

    public DeviceRepository(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveDeviceId(String deviceId) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            return;
        }
        preferences.edit()
                .putString(KEY_DEVICE_ID, deviceId.trim())
                .apply();
    }

    public String getDeviceId() {
        return preferences.getString(KEY_DEVICE_ID, "");
    }

    public void reset() {
        preferences.edit().clear().apply();
    }
}
