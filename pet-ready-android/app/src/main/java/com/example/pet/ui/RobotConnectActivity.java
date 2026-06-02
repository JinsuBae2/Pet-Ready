package com.example.pet.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pet.R;

public class RobotConnectActivity extends AppCompatActivity {
    private TextView tvConnectStatus;
    private Button btnQrConnect;
    private Button btnDemoEnter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_robot_connect);

        tvConnectStatus = findViewById(R.id.tvConnectStatus);
        Button btnCheckRobot = findViewById(R.id.btnCheckRobot);
        btnQrConnect = findViewById(R.id.btnQrConnect);
        btnDemoEnter = findViewById(R.id.btnDemoEnter);

        showFailedState();

        btnCheckRobot.setOnClickListener(v -> showFailedState());
        btnQrConnect.setOnClickListener(v ->
                tvConnectStatus.setText("QR 연결은 준비 중입니다.\n나중에 로봇 등록 API가 준비되면 이 흐름으로 연결합니다.")
        );
        btnDemoEnter.setOnClickListener(v -> {
            Intent intent = new Intent(this, DashboardActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void showFailedState() {
        tvConnectStatus.setText("로봇 연결 실패\n같은 Wi-Fi에 있는지 확인하거나 데모 모드로 진입해주세요.");
        btnQrConnect.setVisibility(Button.VISIBLE);
        btnDemoEnter.setVisibility(Button.VISIBLE);
    }
}
