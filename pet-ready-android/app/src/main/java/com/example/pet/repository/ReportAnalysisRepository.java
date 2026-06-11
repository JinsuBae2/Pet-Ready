package com.example.pet.repository;

import android.content.Context;

import com.example.pet.api.ApiClient;
import com.example.pet.api.ApiErrorMessage;
import com.example.pet.api.ApiService;
import com.example.pet.model.ReportAnalysis;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportAnalysisRepository {
    private final ApiService apiService;
    private final AuthRepository authRepository;

    public ReportAnalysisRepository(Context context) {
        apiService = ApiClient.getClient().create(ApiService.class);
        authRepository = new AuthRepository(context);
    }

    public void getFinalReport(ReportCallback callback) {
        String accessToken = authRepository.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            callback.onResult(null, false, "로그인이 필요합니다.");
            return;
        }

        apiService.getFinalReport("Bearer " + accessToken).enqueue(new Callback<ReportAnalysis>() {
            @Override
            public void onResponse(Call<ReportAnalysis> call, Response<ReportAnalysis> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(response.body(), true, "");
                    return;
                }
                callback.onResult(
                        null,
                        false,
                        "서버 리포트 조회 실패: " + ApiErrorMessage.from(response)
                );
            }

            @Override
            public void onFailure(Call<ReportAnalysis> call, Throwable t) {
                callback.onResult(null, false, "서버 리포트 연결 실패: " + t.getMessage());
            }
        });
    }

    public interface ReportCallback {
        void onResult(ReportAnalysis report, boolean fromServer, String message);
    }
}
