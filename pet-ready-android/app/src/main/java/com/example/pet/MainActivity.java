package com.example.pet;

import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.pet.api.ApiClient;
import com.example.pet.api.ApiErrorMessage;
import com.example.pet.api.ApiService;
import com.example.pet.model.LoginRequest;
import com.example.pet.model.LoginResponse;
import com.example.pet.notification.NotificationHelper;
import com.example.pet.repository.AuthRepository;
import com.example.pet.repository.FcmTokenRepository;
import com.example.pet.ui.RegisterActivity;
import com.example.pet.ui.RobotConnectActivity;
import com.google.firebase.messaging.FirebaseMessaging;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "PetFCM";
    private EditText etEmail;
    private EditText etPassword;
    private Button btnLogin;
    private Button btnGoRegister;
    private Button btnDemoDashboard;
    private TextView tvResult;

    private ApiService apiService;
    private AuthRepository authRepository;
    private FcmTokenRepository fcmTokenRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoRegister = findViewById(R.id.btnGoRegister);
        btnDemoDashboard = findViewById(R.id.btnDemoDashboard);
        tvResult = findViewById(R.id.tvResult);

        apiService = ApiClient.getClient().create(ApiService.class);
        authRepository = new AuthRepository(this);
        fcmTokenRepository = new FcmTokenRepository(this);
        NotificationHelper.createNotificationChannel(this);
        requestNotificationPermissionIfNeeded();
        loadFcmToken();

        btnLogin.setOnClickListener(v -> login());
        btnGoRegister.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, RegisterActivity.class))
        );
        btnDemoDashboard.setOnClickListener(v -> enterDemoMode());
    }

    private void login() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            tvResult.setText("이메일과 비밀번호를 입력해주세요.");
            return;
        }

        tvResult.setText("FCM 토큰 확인 중...");
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    fcmTokenRepository.saveToken(token);
                    requestLogin(email, password, fcmTokenRepository.getSavedToken());
                })
                .addOnFailureListener(error -> requestLogin(email, password, fcmTokenRepository.getSavedToken()));
    }

    private void requestLogin(String email, String password, String fcmToken) {
        if (fcmToken == null || fcmToken.isEmpty()) {
            Log.w(TAG, "Login request will be sent without FCM token.");
        } else {
            Log.d(TAG, "Login request includes FCM token. length=" + fcmToken.length());
        }
        LoginRequest request = new LoginRequest(email, password, fcmToken);
        apiService.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();

                    if (loginResponse.accessToken == null || loginResponse.accessToken.isEmpty()) {
                        tvResult.setText("로그인 응답에 accessToken이 없습니다.");
                        return;
                    }

                    authRepository.saveTokens(
                            loginResponse.accessToken,
                            loginResponse.refreshToken == null ? "" : loginResponse.refreshToken
                    );
                    moveToRobotConnect();
                } else {
                    tvResult.setText("로그인 실패: " + ApiErrorMessage.from(response));
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                tvResult.setText("서버 연결 오류: " + t.getMessage());
            }
        });
    }

    private void moveToRobotConnect() {
        Intent intent = new Intent(MainActivity.this, RobotConnectActivity.class);
        startActivity(intent);
        finish();
    }

    private void enterDemoMode() {
        authRepository.saveTokens("demo-access-token", "demo-refresh-token");
        moveToRobotConnect();
    }

    private void loadFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> fcmTokenRepository.saveToken(token));
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    100
            );
        }
    }
}
