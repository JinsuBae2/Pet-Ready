package com.example.pet.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class FcmTokenRepository {
    private static final String TAG = "PetFCM";
    private static final String PREF_NAME = "pet_ready_fcm";
    private static final String KEY_FCM_TOKEN = "fcm_token";

    private final SharedPreferences preferences;

    public FcmTokenRepository(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveToken(String token) {
        if (!isUsableToken(token)) {
            Log.w(TAG, "Ignored invalid FCM token.");
            return;
        }

        preferences.edit().putString(KEY_FCM_TOKEN, token).apply();
        Log.d(TAG, "FCM token saved locally. length=" + token.length());
    }

    public String getSavedToken() {
        String token = preferences.getString(KEY_FCM_TOKEN, "");
        return isUsableToken(token) ? token : null;
    }

    public boolean isUsableToken(String token) {
        return token != null && token.trim().length() >= 80;
    }
}
