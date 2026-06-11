package com.example.pet.repository;

import android.content.Context;

import com.example.pet.api.ApiClient;
import com.example.pet.api.ApiErrorMessage;
import com.example.pet.api.ApiService;
import com.example.pet.model.DashboardResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardRepository {
    private final ApiService apiService;
    private final AuthRepository authRepository;
    private final DeviceRepository deviceRepository;

    public DashboardRepository(Context context) {
        apiService = ApiClient.getClient().create(ApiService.class);
        authRepository = new AuthRepository(context);
        deviceRepository = new DeviceRepository(context);
    }

    public void getDashboard(DashboardCallback callback) {
        String accessToken = authRepository.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            callback.onResult(null, false, "로그인이 필요합니다.");
            return;
        }

        String deviceId = deviceRepository.getDeviceId();
        if (deviceId.isEmpty()) {
            callback.onResult(null, false, "등록된 기기가 없습니다.");
            return;
        }

        apiService.getDashboard(
                "Bearer " + accessToken,
                deviceId
        ).enqueue(new Callback<DashboardResponse>() {
            @Override
            public void onResponse(
                    Call<DashboardResponse> call,
                    Response<DashboardResponse> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(response.body(), true, "");
                } else {
                    callback.onResult(null, false, ApiErrorMessage.from(response));
                }
            }

            @Override
            public void onFailure(Call<DashboardResponse> call, Throwable t) {
                callback.onResult(null, false, t.getMessage());
            }
        });
    }

    public interface DashboardCallback {
        void onResult(DashboardResponse dashboard, boolean fromServer, String message);
    }
}
