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
import com.example.pet.repository.PetRepository;

public class MissionProgressActivity extends AppCompatActivity {
    private static final long ROBOT_CHECK_INTERVAL_MS = 2000L;

    private MissionItem mission;
    private MissionRepository missionRepository;
    private PetRepository petRepository;
    private TextView tvMissionProgressStatus;
    private final Handler robotCheckHandler = new Handler(Looper.getMainLooper());
    private boolean completingMission = false;

    private final Runnable robotCheckRunnable = new Runnable() {
        @Override
        public void run() {
            checkRobotMissionState();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission_progress);

        mission = MissionIntentExtras.readMission(getIntent());
        missionRepository = new MissionRepository(this);
        petRepository = PetRepository.getInstance();

        TextView tvMissionProgressTitle = findViewById(R.id.tvMissionProgressTitle);
        TextView tvMissionProgressDescription = findViewById(R.id.tvMissionProgressDescription);
        TextView tvMissionChecklist = findViewById(R.id.tvMissionChecklist);
        tvMissionProgressStatus = findViewById(R.id.tvMissionProgressStatus);

        findViewById(R.id.btnMissionProgressBack).setOnClickListener(v -> finish());

        tvMissionProgressTitle.setText(mission.title);
        tvMissionProgressDescription.setText(getProgressDescription());
        tvMissionChecklist.setText(getChecklist());
        tvMissionProgressStatus.setText("로봇/ESP32 확인을 기다리는 중이에요.");
        robotCheckHandler.postDelayed(robotCheckRunnable, ROBOT_CHECK_INTERVAL_MS);
    }

    @Override
    protected void onDestroy() {
        robotCheckHandler.removeCallbacks(robotCheckRunnable);
        super.onDestroy();
    }

    private void checkRobotMissionState() {
        if (completingMission) {
            return;
        }

        if (petRepository.isMissionVerifiedByRobot(mission)) {
            completeMission();
            return;
        }

        tvMissionProgressStatus.setText("아직 로봇에서 행동이 확인되지 않았어요.");
        robotCheckHandler.postDelayed(robotCheckRunnable, ROBOT_CHECK_INTERVAL_MS);
    }

    private void completeMission() {
        completingMission = true;
        robotCheckHandler.removeCallbacks(robotCheckRunnable);
        tvMissionProgressStatus.setText("로봇이 행동을 확인했어요. 완료를 반영하는 중...");

        missionRepository.completeMission(mission, (completedMission, fromServer, message) -> {
            Intent intent = new Intent(this, MissionCompleteActivity.class);
            MissionIntentExtras.putMission(intent, completedMission);
            intent.putExtra("complete_message", message);
            startActivity(intent);
            finish();
        });
    }

    private String getProgressDescription() {
        if ("ROBOT_PLAY".equalsIgnoreCase(mission.missionType)) {
            return "로봇 강아지와 상호작용을 진행해 주세요.";
        }
        if ("DEVICE_STATUS".equalsIgnoreCase(mission.missionType)) {
            return "로봇 상태를 확인하고 이상 여부를 살펴봐 주세요.";
        }
        if ("FEEDING_TIME".equalsIgnoreCase(mission.missionType)) {
            return "밥그릇을 채워 주세요. 완료되면 사료가 1회분 사용됩니다.";
        }
        if ("VET_CHECK".equalsIgnoreCase(mission.missionType)) {
            return "정기 진료 완료를 확인하고 병원비 기록을 반영합니다.";
        }
        if ("VACCINATION".equalsIgnoreCase(mission.missionType)) {
            return "예방접종 완료를 확인하고 접종 비용 기록을 반영합니다.";
        }
        return "미션을 수행하면 로봇이 상태를 확인하고 자동으로 완료됩니다.";
    }

    private String getChecklist() {
        if ("ROBOT_PLAY".equalsIgnoreCase(mission.missionType)) {
            return "1   가까이 다가가기      ○\n\n2   터치 반응 확인하기   ●\n\n3   상호작용 마무리      ○";
        }
        if ("DEVICE_STATUS".equalsIgnoreCase(mission.missionType)) {
            return "1   배터리 확인하기      ○\n\n2   연결 상태 확인하기   ●\n\n3   이상 여부 기록하기   ○";
        }
        if ("FEEDING_TIME".equalsIgnoreCase(mission.missionType)) {
            return "1   사료 보유량 확인하기  ○\n\n2   밥그릇 채워주기      ●\n\n3   사료 사용 반영하기    ○";
        }
        if ("VET_CHECK".equalsIgnoreCase(mission.missionType)) {
            return "1   병원 방문하기        ○\n\n2   진료 완료 확인하기    ●\n\n3   병원비 기록하기       ○";
        }
        if ("VACCINATION".equalsIgnoreCase(mission.missionType)) {
            return "1   접종 일정 확인하기    ○\n\n2   예방접종 완료하기     ●\n\n3   접종비 기록하기       ○";
        }
        return "1   미션 위치 확인하기   ○\n\n2   행동 수행하기        ●\n\n3   완료 확인하기        ○";
    }
}
