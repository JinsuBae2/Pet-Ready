package com.example.pet;

import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
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
import com.example.pet.model.MyDeviceResponse;
import com.example.pet.notification.NotificationHelper;
import com.example.pet.repository.AuthRepository;
import com.example.pet.repository.AccountDataManager;
import com.example.pet.repository.DeviceRepository;
import com.example.pet.repository.FcmTokenRepository;
import com.example.pet.repository.PetProfileRepository;
import com.example.pet.repository.SimulationRepository;
import com.example.pet.ui.DashboardActivity;
import com.example.pet.ui.RegisterActivity;
import com.example.pet.ui.RobotConnectActivity;
import com.example.pet.ui.SimulationSetupActivity;
import com.google.firebase.messaging.FirebaseMessaging;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "PetFCM";
    private static final String LOGIN_PREFS = "pet_ready_login_preferences";
    private static final String KEY_SAVE_EMAIL = "save_email";
    private static final String KEY_SAVED_EMAIL = "saved_email";
    private EditText etEmail;
    private EditText etPassword;
    private Button btnLogin;
    private Button btnGoRegister;
    private CheckBox checkSaveEmail;
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
        checkSaveEmail = findViewById(R.id.checkSaveEmail);
        tvResult = findViewById(R.id.tvResult);
        loadSavedEmail();

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
    }

    private void login() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            tvResult.setText("이메일과 비밀번호를 입력해주세요.");
            return;
        }

        saveEmailPreference(email);
        tvResult.setText("FCM 토큰 확인 중...");
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    fcmTokenRepository.saveToken(token);
                    requestLogin(email, password, fcmTokenRepository.getSavedToken());
                })
                .addOnFailureListener(error -> requestLogin(email, password, fcmTokenRepository.getSavedToken()));
    }

    private void loadSavedEmail() {
        android.content.SharedPreferences preferences =
                getSharedPreferences(LOGIN_PREFS, MODE_PRIVATE);
        boolean shouldSaveEmail = preferences.getBoolean(KEY_SAVE_EMAIL, false);
        checkSaveEmail.setChecked(shouldSaveEmail);
        if (shouldSaveEmail) {
            etEmail.setText(preferences.getString(KEY_SAVED_EMAIL, ""));
        }
    }

    private void saveEmailPreference(String email) {
        android.content.SharedPreferences.Editor editor =
                getSharedPreferences(LOGIN_PREFS, MODE_PRIVATE).edit();
        editor.putBoolean(KEY_SAVE_EMAIL, checkSaveEmail.isChecked());
        if (checkSaveEmail.isChecked()) {
            editor.putString(KEY_SAVED_EMAIL, email);
        } else {
            editor.remove(KEY_SAVED_EMAIL);
        }
        editor.apply();
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

                    if (authRepository.isDifferentAccount(email)) {
                        AccountDataManager.resetForNewAccount(MainActivity.this);
                    }
                    authRepository.saveTokens(
                            loginResponse.accessToken,
                            loginResponse.refreshToken == null ? "" : loginResponse.refreshToken
                    );
                    authRepository.saveAccountEmail(email);
                    resolveLoginDestination(loginResponse.accessToken);
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

    private void resolveLoginDestination(String accessToken) {
        tvResult.setText("등록된 기기 정보를 확인하고 있습니다.");
        apiService.getMyDevice("Bearer " + accessToken)
                .enqueue(new Callback<MyDeviceResponse>() {
                    @Override
                    public void onResponse(
                            Call<MyDeviceResponse> call,
                            Response<MyDeviceResponse> response
                    ) {
                        MyDeviceResponse device = response.body();
                        if (response.isSuccessful()
                                && device != null
                                && device.deviceId != null
                                && !device.deviceId.trim().isEmpty()) {
                            cacheRegisteredDevice(device);
                            moveToRegisteredUserHome();
                            return;
                        }

                        if (response.code() == 404) {
                            moveToRobotConnect();
                            return;
                        }

                        tvResult.setText("기기 정보 확인 실패: " + ApiErrorMessage.from(response));
                    }

                    @Override
                    public void onFailure(Call<MyDeviceResponse> call, Throwable t) {
                        tvResult.setText("기기 정보 확인 중 서버 연결 오류: " + t.getMessage());
                    }
                });
    }

    private void cacheRegisteredDevice(MyDeviceResponse device) {
        new DeviceRepository(this).saveDeviceId(device.deviceId);

        if (device.petName == null || device.petName.trim().isEmpty()) {
            return;
        }

        PetProfileRepository profileRepository = new PetProfileRepository(this);
        profileRepository.saveProfile(
                device.petName.trim(),
                profileRepository.getAvatarType(),
                profileRepository.getPhotoUri()
        );
    }

    private void moveToRegisteredUserHome() {
        Class<?> destination = new SimulationRepository(this).isSetupDone()
                ? DashboardActivity.class
                : SimulationSetupActivity.class;
        Intent intent = new Intent(this, destination);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void moveToRobotConnect() {
        Intent intent = new Intent(MainActivity.this, RobotConnectActivity.class);
        startActivity(intent);
        finish();
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
