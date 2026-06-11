package com.example.pet.repository;

import android.content.Context;

import com.example.pet.api.ApiClient;
import com.example.pet.api.ApiErrorMessage;
import com.example.pet.api.ApiService;
import com.example.pet.model.TrainingRewardRequest;
import com.example.pet.model.TrainingRewardResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TrainingRepository {
    private final ApiService apiService;
    private final DeviceRepository deviceRepository;

    public TrainingRepository(Context context) {
        apiService = ApiClient.getClient().create(ApiService.class);
        deviceRepository = new DeviceRepository(context);
    }

    public String getDeviceId() {
        return deviceRepository.getDeviceId();
    }

    public void giveReward(RewardCallback callback) {
        apiService.giveTrainingReward(new TrainingRewardRequest(getDeviceId()))
                .enqueue(new Callback<TrainingRewardResponse>() {
                    @Override
                    public void onResponse(
                            Call<TrainingRewardResponse> call,
                            Response<TrainingRewardResponse> response
                    ) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onResult(response.body(), "");
                            return;
                        }
                        callback.onResult(null, ApiErrorMessage.from(response));
                    }

                    @Override
                    public void onFailure(Call<TrainingRewardResponse> call, Throwable t) {
                        callback.onResult(null, t.getMessage());
                    }
                });
    }

    public interface RewardCallback {
        void onResult(TrainingRewardResponse response, String errorMessage);
    }
}
