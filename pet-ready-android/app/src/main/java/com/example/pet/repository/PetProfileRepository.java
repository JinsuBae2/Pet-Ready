package com.example.pet.repository;

import android.content.Context;
import android.content.SharedPreferences;

public class PetProfileRepository {
    private static final String PREF_NAME = "pet_ready_profile";
    private static final String KEY_PET_NAME = "pet_name";
    private static final String KEY_AVATAR_TYPE = "avatar_type";
    private static final String KEY_PHOTO_URI = "photo_uri";

    private final SharedPreferences preferences;

    public PetProfileRepository(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getPetName() {
        return preferences.getString(KEY_PET_NAME, "몽치");
    }

    public int getAvatarType() {
        return preferences.getInt(KEY_AVATAR_TYPE, 0);
    }

    public String getPhotoUri() {
        return preferences.getString(KEY_PHOTO_URI, "");
    }

    public void saveProfile(String petName, int avatarType, String photoUri) {
        preferences.edit()
                .putString(KEY_PET_NAME, petName)
                .putInt(KEY_AVATAR_TYPE, avatarType)
                .putString(KEY_PHOTO_URI, photoUri)
                .apply();
    }
}
