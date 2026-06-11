package com.example.pet.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pet.R;
import com.example.pet.model.MissionItem;
import com.example.pet.repository.MissionRepository;

public class MissionProgressActivity extends AppCompatActivity {
    private static final long ROBOT_CHECK_INTERVAL_MS = 2000L;

    private final Handler robotCheckHandler = new Handler(Looper.getMainLooper());
    private MissionItem mission;
    private MissionRepository missionRepository;
    private TextView statusView;
    private boolean finished;

    private final Runnable robotCheckRunnable = this::checkRobotMissionState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission_progress);

        mission = MissionIntentExtras.readMission(getIntent());
        missionRepository = new MissionRepository(this);

        TextView titleView = findViewById(R.id.tvMissionProgressTitle);
        TextView descriptionView = findViewById(R.id.tvMissionProgressDescription);
        TextView checklistView = findViewById(R.id.tvMissionChecklist);
        statusView = findViewById(R.id.tvMissionProgressStatus);

        findViewById(R.id.btnMissionProgressBack).setOnClickListener(v -> finish());
        titleView.setText(mission.title);
        descriptionView.setText(mission.description == null ? "" : mission.description);
        checklistView.setText("");
        statusView.setText("로봇의 미션 완료 상태를 확인하고 있습니다.");

        robotCheckHandler.post(robotCheckRunnable);
    }

    @Override
    protected void onDestroy() {
        robotCheckHandler.removeCallbacks(robotCheckRunnable);
        super.onDestroy();
    }

    private void checkRobotMissionState() {
        if (finished) {
            return;
        }

        missionRepository.getMissionState(mission, (serverMission, success, message) -> {
            if (finished) {
                return;
            }
            if (!success) {
                statusView.setText(message);
                scheduleNextCheck();
                return;
            }

            mission = serverMission;
            if (serverMission.completed || "COMPLETED".equalsIgnoreCase(serverMission.status)) {
                openMissionComplete(serverMission, message);
                return;
            }

            statusView.setText("아직 로봇에서 미션 완료가 확인되지 않았습니다.");
            scheduleNextCheck();
        });
    }

    private void scheduleNextCheck() {
        robotCheckHandler.postDelayed(robotCheckRunnable, ROBOT_CHECK_INTERVAL_MS);
    }

    private void openMissionComplete(MissionItem completedMission, String message) {
        finished = true;
        robotCheckHandler.removeCallbacks(robotCheckRunnable);
        Intent intent = new Intent(this, MissionCompleteActivity.class);
        MissionIntentExtras.putMission(intent, completedMission);
        intent.putExtra("complete_message", message);
        startActivity(intent);
        finish();
    }
}
