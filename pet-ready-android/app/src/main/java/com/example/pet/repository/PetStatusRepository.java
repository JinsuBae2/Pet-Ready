package com.example.pet.repository;

import android.content.Context;

import com.example.pet.api.ApiClient;
import com.example.pet.api.ApiErrorMessage;
import com.example.pet.api.ApiService;
import com.example.pet.model.PetFeedRequest;
import com.example.pet.model.PetStatusRequest;
import com.example.pet.model.PetStatusResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PetStatusRepository {
    private final ApiService apiService;
    private final AuthRepository authRepository;
    private final DeviceRepository deviceRepository;

    public PetStatusRepository(Context context) {
        apiService = ApiClient.getClient().create(ApiService.class);
        authRepository = new AuthRepository(context);
        deviceRepository = new DeviceRepository(context);
    }

    public void getStatus(StatusCallback callback) {
        PetStatusRequest request = new PetStatusRequest(
                deviceRepository.getDeviceId(),
                false,
                false,
                false
        );
        apiService.sendPetStatus(request).enqueue(new Callback<PetStatusResponse>() {
            @Override
            public void onResponse(Call<PetStatusResponse> call, Response<PetStatusResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(response.body(), "");
                    return;
                }
                callback.onResult(null, ApiErrorMessage.from(response));
            }

            @Override
            public void onFailure(Call<PetStatusResponse> call, Throwable t) {
                callback.onResult(null, t.getMessage());
            }
        });
    }

    public void feed(FeedCallback callback) {
        String token = authRepository.getAccessToken();
        apiService.feedPet(
                token.isEmpty() ? "" : "Bearer " + token,
                new PetFeedRequest(deviceRepository.getDeviceId())
        ).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                callback.onResult(response.isSuccessful(),
                        response.isSuccessful() ? "" : ApiErrorMessage.from(response));
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onResult(false, t.getMessage());
            }
        });
    }

    public interface StatusCallback {
        void onResult(PetStatusResponse status, String errorMessage);
    }

    public interface FeedCallback {
        void onResult(boolean success, String errorMessage);
    }
}
