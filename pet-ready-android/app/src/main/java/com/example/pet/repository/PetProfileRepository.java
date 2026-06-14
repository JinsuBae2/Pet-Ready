package com.example.pet.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.pet.api.ApiClient;
import com.example.pet.api.ApiErrorMessage;
import com.example.pet.api.ApiService;
import com.example.pet.model.UpdatePetNameRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PetProfileRepository {
    private static final String PREF_NAME = "pet_ready_profile";
    private static final String KEY_PET_NAME = "pet_name";
    private static final String KEY_AVATAR_TYPE = "avatar_type";
    private static final String KEY_PHOTO_URI = "photo_uri";

    private final SharedPreferences preferences;
    private final ApiService apiService;
    private final AuthRepository authRepository;

    public PetProfileRepository(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        apiService = ApiClient.getClient().create(ApiService.class);
        authRepository = new AuthRepository(context);
    }

    public String getPetName() {
        return preferences.getString(KEY_PET_NAME, "몽치");
    }

    public int getAvatarType() {
        return preferences.getInt(KEY_AVATAR_TYPE, 0);
    }

    public boolean hasPetName() {
        String petName = preferences.getString(KEY_PET_NAME, "");
        return petName != null && !petName.trim().isEmpty();
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

    public void reset() {
        preferences.edit().clear().apply();
    }

    public void syncPetName(String petName, SyncCallback callback) {
        String accessToken = authRepository.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            callback.onResult(false, "로그인이 필요합니다.");
            return;
        }

        apiService.updatePetName(
                "Bearer " + accessToken,
                new UpdatePetNameRequest(petName)
        ).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onResult(true, "펫 이름을 서버에 저장했습니다.");
                } else {
                    callback.onResult(false, "펫 이름 저장 실패: " + ApiErrorMessage.from(response));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onResult(false, "펫 이름 서버 연결 실패: " + t.getMessage());
            }
        });
    }

    public interface SyncCallback {
        void onResult(boolean success, String message);
    }

    public static String replaceRobotDog(String text, String petName) {
        if (text == null || text.isEmpty() || petName == null || petName.isEmpty()) {
            return text;
        }

        char lastChar = petName.charAt(petName.length() - 1);
        boolean hasJongseong = false;
        if (lastChar >= 0xAC00 && lastChar <= 0xD7A3) {
            hasJongseong = (lastChar - 0xAC00) % 28 != 0;
        }

        String waGwa = hasJongseong ? "과" : "와";
        String eulLeul = hasJongseong ? "을" : "를";
        String iGa = hasJongseong ? "이" : "가";

        return text.replace("로봇 강아지와", petName + waGwa)
                   .replace("로봇강아지와", petName + waGwa)
                   .replace("로봇 강아지를", petName + eulLeul)
                   .replace("로봇강아지를", petName + eulLeul)
                   .replace("로봇 강아지가", petName + iGa)
                   .replace("로봇강아지가", petName + iGa)
                   .replace("로봇 강아지", petName)
                   .replace("로봇강아지", petName);
    }
}
