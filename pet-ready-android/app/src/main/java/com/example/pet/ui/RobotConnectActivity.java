package com.example.pet.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pet.R;
import com.example.pet.api.ApiClient;
import com.example.pet.api.ApiErrorMessage;
import com.example.pet.api.ApiService;
import com.example.pet.model.DeviceRegisterRequest;
import com.example.pet.repository.AuthRepository;
import com.example.pet.repository.DeviceRepository;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RobotConnectActivity extends AppCompatActivity {
    private TextView tvConnectStatus;
    private Button btnQrConnect;
    private Button btnCheckRobot;
    private ApiService apiService;
    private AuthRepository authRepository;
    private DeviceRepository deviceRepository;

    private final ActivityResultLauncher<ScanOptions> qrScanLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                if (result.getContents() == null) {
                    Toast.makeText(this, "QR 스캔이 취소되었습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
                registerScannedDevice(result.getContents());
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_robot_connect);

        tvConnectStatus = findViewById(R.id.tvConnectStatus);
        btnCheckRobot = findViewById(R.id.btnCheckRobot);
        btnQrConnect = findViewById(R.id.btnQrConnect);
        apiService = ApiClient.getClient().create(ApiService.class);
        authRepository = new AuthRepository(this);
        deviceRepository = new DeviceRepository(this);

        showFailedState();

        btnCheckRobot.setOnClickListener(v -> showFailedState());
        btnQrConnect.setOnClickListener(v -> startQrScan());
    }

    private void showFailedState() {
        tvConnectStatus.setText("로봇 연결 실패\n같은 Wi-Fi에 있는지 확인하거나 QR 코드를 다시 스캔해주세요.");
        btnQrConnect.setVisibility(Button.VISIBLE);
        btnCheckRobot.setEnabled(true);
        btnQrConnect.setEnabled(true);
    }

    private void startQrScan() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("기기에 부착된 QR 코드를 화면 안에 맞춰주세요.");
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        qrScanLauncher.launch(options);
    }

    private void registerScannedDevice(String rawText) {
        String deviceId = normalizeDeviceId(rawText);
        if (deviceId.isEmpty()) {
            tvConnectStatus.setText("QR 코드에서 deviceId를 읽지 못했습니다.\n기기 QR 코드를 다시 스캔해주세요.");
            return;
        }

        String accessToken = authRepository.getAccessToken();
        if (accessToken == null || accessToken.trim().isEmpty()) {
            tvConnectStatus.setText("로그인 토큰이 없습니다.\n다시 로그인한 뒤 QR 등록을 시도해주세요.");
            return;
        }

        setLoadingState("기기 등록 중...\n등록할 기기: " + deviceId);
        DeviceRegisterRequest request = new DeviceRegisterRequest(deviceId);
        apiService.registerDevice("Bearer " + accessToken, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    deviceRepository.saveDeviceId(deviceId);
                    tvConnectStatus.setText("기기 등록 완료\n등록된 기기: " + deviceId);
                    openDashboard();
                } else {
                    tvConnectStatus.setText("기기 등록 실패\n" + ApiErrorMessage.from(response));
                    setButtonsEnabled(true);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                tvConnectStatus.setText("기기 등록 서버 연결 오류\n" + t.getMessage());
                setButtonsEnabled(true);
            }
        });
    }

    private String normalizeDeviceId(String rawText) {
        if (rawText == null) {
            return "";
        }
        String value = rawText.trim();
        if (value.toUpperCase(Locale.US).startsWith("PETREADY-")) {
            value = value.substring("PETREADY-".length()).trim();
        }
        if (value.toUpperCase(Locale.US).startsWith("DOG-")) {
            value = "DOG_" + value.substring("DOG-".length()).trim();
        }
        return value;
    }

    private void setLoadingState(String message) {
        tvConnectStatus.setText(message);
        setButtonsEnabled(false);
    }

    private void setButtonsEnabled(boolean enabled) {
        btnCheckRobot.setEnabled(enabled);
        btnQrConnect.setEnabled(enabled);
    }

    private void openDashboard() {
        tvConnectStatus.postDelayed(() -> {
            Intent intent = new Intent(this, DashboardActivity.class);
            startActivity(intent);
            finish();
        }, 600);
    }
}
