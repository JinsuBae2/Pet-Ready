package com.example.pet.repository;

import android.content.Context;

import com.example.pet.api.ApiClient;
import com.example.pet.api.ApiErrorMessage;
import com.example.pet.api.ApiService;
import com.example.pet.model.ExpenseReportResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ServerExpenseRepository {
    private final ApiService apiService;
    private final AuthRepository authRepository;

    public ServerExpenseRepository(Context context) {
        apiService = ApiClient.getClient().create(ApiService.class);
        authRepository = new AuthRepository(context);
    }

    public void getExpenses(ExpenseCallback callback) {
        String token = authRepository.getAccessToken();
        if (token == null || token.isEmpty()) {
            callback.onResult(null, "로그인이 필요합니다.");
            return;
        }

        apiService.getExpenses("Bearer " + token).enqueue(new Callback<ExpenseReportResponse>() {
            @Override
            public void onResponse(
                    Call<ExpenseReportResponse> call,
                    Response<ExpenseReportResponse> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(response.body(), "");
                    return;
                }
                callback.onResult(null, ApiErrorMessage.from(response));
            }

            @Override
            public void onFailure(Call<ExpenseReportResponse> call, Throwable t) {
                callback.onResult(null, t.getMessage());
            }
        });
    }

    public interface ExpenseCallback {
        void onResult(ExpenseReportResponse response, String errorMessage);
    }
}
