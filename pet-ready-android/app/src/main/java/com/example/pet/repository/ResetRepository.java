package com.example.pet.repository;

import android.content.Context;

import com.example.pet.api.ApiClient;
import com.example.pet.api.ApiErrorMessage;
import com.example.pet.api.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetRepository {
    public interface ResultCallback {
        void onResult(boolean success, String message);
    }

    private final ApiService apiService;
    private final AuthRepository authRepository;

    public ResetRepository(Context context) {
        apiService = ApiClient.getClient().create(ApiService.class);
        authRepository = new AuthRepository(context);
    }

    public void resetSimulation(ResultCallback callback) {
        String authorization = getAuthorizationHeader();
        if (authorization.isEmpty()) {
            callback.onResult(false, "로그인이 필요합니다.");
            return;
        }

        apiService.resetSimulation(authorization).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                callback.onResult(
                        response.isSuccessful(),
                        response.isSuccessful()
                                ? "시뮬레이션 데이터를 초기화했습니다."
                                : "초기화 실패: " + ApiErrorMessage.from(response)
                );
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onResult(false, "초기화 연결 실패: " + t.getMessage());
            }
        });
    }

    public void withdrawUser(ResultCallback callback) {
        String authorization = getAuthorizationHeader();
        if (authorization.isEmpty()) {
            callback.onResult(false, "로그인이 필요합니다.");
            return;
        }

        apiService.withdrawUser(authorization).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                callback.onResult(
                        response.isSuccessful(),
                        response.isSuccessful()
                                ? "회원 탈퇴가 완료되었습니다."
                                : "회원 탈퇴 실패: " + ApiErrorMessage.from(response)
                );
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onResult(false, "회원 탈퇴 연결 실패: " + t.getMessage());
            }
        });
    }

    private String getAuthorizationHeader() {
        String accessToken = authRepository.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            return "";
        }
        return "Bearer " + accessToken;
    }
}
