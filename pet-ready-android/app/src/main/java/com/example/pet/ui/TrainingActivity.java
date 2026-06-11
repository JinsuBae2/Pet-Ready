package com.example.pet.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pet.R;
import com.example.pet.model.TrainingRewardResponse;
import com.example.pet.repository.TrainingRepository;

public class TrainingActivity extends AppCompatActivity {
    private TrainingRepository trainingRepository;
    private Button btnGiveReward;
    private TextView tvTrainingStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training);

        trainingRepository = new TrainingRepository(this);
        btnGiveReward = findViewById(R.id.btnGiveTrainingReward);
        tvTrainingStatus = findViewById(R.id.tvTrainingStatus);
        TextView tvTrainingDevice = findViewById(R.id.tvTrainingDevice);

        tvTrainingDevice.setText("연결 기기: " + trainingRepository.getDeviceId());
        findViewById(R.id.btnTrainingBack).setOnClickListener(v -> finish());
        btnGiveReward.setOnClickListener(v -> giveReward());
    }

    private void giveReward() {
        btnGiveReward.setEnabled(false);
        btnGiveReward.setText("판정 중...");
        tvTrainingStatus.setText("훈련 보상 결과를 확인하고 있어요.");

        trainingRepository.giveReward((response, errorMessage) -> {
            btnGiveReward.setEnabled(true);
            btnGiveReward.setText("간식 주기");

            if (response == null) {
                String message = isBlank(errorMessage)
                        ? "보상 요청에 실패했습니다."
                        : "보상 요청에 실패했습니다.\n" + errorMessage;
                tvTrainingStatus.setText(message);
                new AlertDialog.Builder(this)
                        .setTitle("연결 오류")
                        .setMessage(message)
                        .setPositiveButton("확인", null)
                        .show();
                return;
            }

            showRewardResult(response);
        });
    }

    private void showRewardResult(TrainingRewardResponse response) {
        boolean success = "SUCCESS".equalsIgnoreCase(response.status);
        String title = success ? "훈련 성공" : "훈련 흐름 확인";
        String summary = success
                ? "제스처가 확인되어 간식을 지급했어요."
                : "최근 60초 안에 감지된 제스처가 없어 CONFUSED로 기록됐어요.";
        String petMessage = joinLcdText(response.lcdTextLine1, response.lcdTextLine2);
        String dialogMessage = petMessage.isEmpty() ? summary : summary + "\n\n" + petMessage;

        tvTrainingStatus.setText(safe(response.status) + "\n" + dialogMessage);
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(dialogMessage)
                .setPositiveButton("확인", null)
                .show();
    }

    private String joinLcdText(String line1, String line2) {
        String first = isBlank(line1) ? "" : line1.trim();
        String second = isBlank(line2) ? "" : line2.trim();
        if (first.isEmpty()) {
            return second;
        }
        if (second.isEmpty()) {
            return first;
        }
        return first + "\n" + second;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
