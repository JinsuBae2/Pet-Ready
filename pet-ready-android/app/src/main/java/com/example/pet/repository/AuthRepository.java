package com.example.pet.repository;

import android.content.Context;
import android.content.SharedPreferences;

/*
 * 로그인 토큰을 저장하고 꺼내는 Repository
 * 실제 API 연동이 늘어나면 accessToken을 Authorization 헤더에 붙이는 데 사용한다.
 */
public class AuthRepository {
    private static final String PREF_NAME = "pet_ready_auth";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";

    private final SharedPreferences preferences;

    public AuthRepository(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveTokens(String accessToken, String refreshToken) {
        preferences.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .apply();
    }

    public String getAccessToken() {
        return preferences.getString(KEY_ACCESS_TOKEN, "");
    }

    public String getRefreshToken() {
        return preferences.getString(KEY_REFRESH_TOKEN, "");
    }

    public boolean hasAccessToken() {
        return !getAccessToken().isEmpty();
    }

    public void clearTokens() {
        preferences.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .apply();
    }
}
