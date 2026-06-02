package com.example.pet.repository;

import android.content.Context;
import android.util.Log;

import com.example.pet.api.ApiClient;
import com.example.pet.api.ApiErrorMessage;
import com.example.pet.api.ApiService;
import com.example.pet.model.WalkEndRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WalkRepository {
    private static final String TAG = "PetWalk";

    private final ApiService apiService;
    private final AuthRepository authRepository;

    public WalkRepository(Context context) {
        apiService = ApiClient.getClient().create(ApiService.class);
        authRepository = new AuthRepository(context);
    }

    public void sendWalkEnd(WalkEndRequest request) {
        Log.d(TAG, "Sending walk/end: durationSec=" + request.durationSec
                + ", distanceKm=" + request.distanceKm);

        String accessToken = authRepository.getAccessToken();
        if (accessToken == null || accessToken.isEmpty() || accessToken.startsWith("demo-")) {
            Log.d(TAG, "Walk result saved locally only.");
            return;
        }

        apiService.endWalk("Bearer " + accessToken, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Walk result sent to server.");
                } else {
                    Log.w(TAG, "Walk result send failed: " + ApiErrorMessage.from(response));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.w(TAG, "Walk result send error: " + t.getMessage());
            }
        });
    }
}
