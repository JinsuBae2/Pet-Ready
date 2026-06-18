package com.example.pet.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pet.R;
import com.example.pet.model.MissionItem;
import com.example.pet.notification.PetFirebaseMessagingService;
import com.example.pet.repository.MissionRepository;
import com.example.pet.repository.PetProfileRepository;

public class MissionDetailActivity extends AppCompatActivity {
    private static final long MISSION_POLL_INTERVAL_MS = 3000L;

    private MissionItem mission;
    private MissionRepository missionRepository;
    private Button btnMissionStart;
    private TextView tvMissionDetailStatus;
    private final Handler missionHandler = new Handler(Looper.getMainLooper());
    private boolean polling = false;
    private boolean completionHandled = false;
    private final Runnable missionPoller = this::pollMissionState;
    private final BroadcastReceiver missionCompletionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long completedMissionId = intent.getLongExtra(
                    PetFirebaseMessagingService.EXTRA_COMPLETED_MISSION_ID,
                    -1L
            );
            if (completedMissionId == mission.missionId) {
                handleMissionCompleted("미션에 성공했습니다.");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission_detail);

        mission = MissionIntentExtras.readMission(getIntent());
        missionRepository = new MissionRepository(this);

        TextView tvMissionDetailTitle = findViewById(R.id.tvMissionDetailTitle);
        tvMissionDetailStatus = findViewById(R.id.tvMissionDetailStatus);
        TextView tvMissionDetailDescription = findViewById(R.id.tvMissionDetailDescription);
        TextView tvMissionSteps = findViewById(R.id.tvMissionSteps);
        TextView tvMissionNotes = findViewById(R.id.tvMissionNotes);
        btnMissionStart = findViewById(R.id.btnMissionStart);

        findViewById(R.id.btnMissionDetailBack).setOnClickListener(v -> finish());

        String petName = new PetProfileRepository(this).getPetName();
        tvMissionDetailTitle.setText(PetProfileRepository.replaceRobotDog(mission.title, petName));
        renderMissionState();
        tvMissionDetailDescription.setText(PetProfileRepository.replaceRobotDog(getDetailDescription(), petName));
        tvMissionSteps.setText(PetProfileRepository.replaceRobotDog(getMissionSteps(), petName));
        tvMissionNotes.setText(PetProfileRepository.replaceRobotDog(getMissionNotes(), petName));
        btnMissionStart.setOnClickListener(v -> handlePrimaryAction());
        if (isInProgress(mission)) {
            startPolling();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(PetFirebaseMessagingService.ACTION_MISSION_COMPLETED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(missionCompletionReceiver, filter, RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(missionCompletionReceiver, filter);
        }
        if (isInProgress(mission) && !completionHandled) {
            startPolling();
        }
    }

    @Override
    protected void onStop() {
        unregisterReceiver(missionCompletionReceiver);
        stopPolling();
        super.onStop();
    }

    private void startMission() {
        btnMissionStart.setEnabled(false);
        btnMissionStart.setText("시작 요청 중...");
        MissionRepository.MissionStateCallback callback = (startedMission, success, message) -> {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            if (!success) {
                mission = startedMission;
                btnMissionStart.setEnabled(true);
                btnMissionStart.setText(isFeedingMission() ? "밥 주기 다시 시도" : "미션 진행하기");
                return;
            }

            mission = startedMission;
            mission.status = "IN_PROGRESS";
            renderMissionState();
            if (isWalkMission()) {
                openWalkMission();
                return;
            }
            startPolling();
        };

        if (isFeedingMission()) {
            missionRepository.startFeedingMission(mission, callback);
        } else {
            missionRepository.startMission(mission, callback);
        }
    }

    private void handlePrimaryAction() {
        if (isWalkMission() && isInProgress(mission)) {
            openWalkMission();
            return;
        }
        if (isRobotPlayMission() || isImmediateUrgentMission()) {
            completeInteractionMission();
            return;
        }
        startMission();
    }

    private void completeInteractionMission() {
        btnMissionStart.setEnabled(false);
        btnMissionStart.setText("완료 요청 중...");
        missionRepository.completeMission(mission, (completedMission, fromServer, message) -> {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            if (!completedMission.completed) {
                btnMissionStart.setEnabled(true);
                btnMissionStart.setText("완료하기");
                return;
            }
            mission = completedMission;
            handleMissionCompleted("상호작용 미션에 성공했습니다.");
        });
    }

    private void openWalkMission() {
        stopPolling();
        Intent intent = new Intent(this, WalkActivity.class);
        MissionIntentExtras.putMission(intent, mission);
        startActivity(intent);
    }

    private void startPolling() {
        polling = true;
        missionHandler.removeCallbacks(missionPoller);
        missionHandler.post(missionPoller);
    }

    private void stopPolling() {
        polling = false;
        missionHandler.removeCallbacks(missionPoller);
    }

    private void pollMissionState() {
        if (!polling || completionHandled) {
            return;
        }

        missionRepository.getMissionState(mission, (updatedMission, success, message) -> {
            if (!polling || completionHandled) {
                return;
            }
            if (success) {
                mission = updatedMission;
                if (isCompleted(mission)) {
                    handleMissionCompleted("미션에 성공했습니다.");
                    return;
                }
                if ("FAILED".equalsIgnoreCase(mission.status)) {
                    stopPolling();
                    renderMissionState();
                    Toast.makeText(this, "미션에 실패했습니다.", Toast.LENGTH_LONG).show();
                    return;
                }
                renderMissionState();
            }
            missionHandler.postDelayed(missionPoller, MISSION_POLL_INTERVAL_MS);
        });
    }

    private void renderMissionState() {
        if (isCompleted(mission)) {
            tvMissionDetailStatus.setText("완료");
            btnMissionStart.setText("완료됨");
            btnMissionStart.setEnabled(false);
            return;
        }
        if ("FAILED".equalsIgnoreCase(mission.status)) {
            tvMissionDetailStatus.setText("실패");
            btnMissionStart.setText("미션 실패");
            btnMissionStart.setEnabled(false);
            return;
        }
        if (isInProgress(mission)) {
            tvMissionDetailStatus.setText("진행 중");
            if (isWalkMission()) {
                btnMissionStart.setText("산책 계속하기");
                btnMissionStart.setEnabled(true);
            } else if (isRobotPlayMission()) {
                btnMissionStart.setText("완료하기");
                btnMissionStart.setEnabled(true);
            } else if (isImmediateUrgentMission()) {
                btnMissionStart.setText("미션 해결");
                btnMissionStart.setEnabled(true);
            } else if (isFeedingMission()) {
                btnMissionStart.setText("밥그릇 인식 대기 중");
                btnMissionStart.setEnabled(false);
            } else {
                btnMissionStart.setText("진행 중...");
                btnMissionStart.setEnabled(false);
            }
            return;
        }
        tvMissionDetailStatus.setText("진행 전");
        btnMissionStart.setText(
                isRobotPlayMission()
                        ? "상호작용 완료"
                        : isImmediateUrgentMission() ? "미션 해결" : "미션 진행하기"
        );
        btnMissionStart.setEnabled(true);
    }

    private boolean isInProgress(MissionItem item) {
        return item != null && "IN_PROGRESS".equalsIgnoreCase(item.status);
    }

    private boolean isCompleted(MissionItem item) {
        return item != null
                && (item.completed || "COMPLETED".equalsIgnoreCase(item.status));
    }

    private boolean isWalkMission() {
        return mission != null && "WALK".equalsIgnoreCase(mission.missionType);
    }

    private boolean isRobotPlayMission() {
        return mission != null && "ROBOT_PLAY".equalsIgnoreCase(mission.missionType);
    }

    private boolean isFeedingMission() {
        return mission != null && "FEEDING_TIME".equalsIgnoreCase(mission.missionType);
    }

    private boolean isImmediateUrgentMission() {
        return MissionRepository.isUrgentMission(mission);
    }

    private void handleMissionCompleted(String message) {
        if (completionHandled) {
            return;
        }
        completionHandled = true;
        stopPolling();
        mission.completed = true;
        mission.status = "COMPLETED";

        Intent intent = new Intent(this, MissionCompleteActivity.class);
        MissionIntentExtras.putMission(intent, mission);
        intent.putExtra("complete_message", message);
        startActivity(intent);
        finish();
    }

    private String getDetailDescription() {
        if (mission.description != null && !mission.description.isEmpty()) {
            return mission.description;
        }
        return "로봇 강아지와 함께 미션을 수행해 주세요.";
    }

    private String getMissionSteps() {
        if ("WALK".equalsIgnoreCase(mission.missionType)) {
            return "1  산책 화면에서 산책 시작 누르기\n\n2  1분 이상 산책하기\n\n3  산책 종료 후 서버 완료 확인하기";
        }
        if ("ROBOT_PLAY".equalsIgnoreCase(mission.missionType)) {
            return "1  로봇 강아지를 가까이 두기\n\n2  터치하거나 말을 걸어 상호작용하기\n\n3  반응을 확인한 뒤 완료 누르기";
        }
        if ("DEVICE_STATUS".equalsIgnoreCase(mission.missionType)) {
            return "1  로봇 전원 상태 확인하기\n\n2  연결 상태 확인하기\n\n3  이상이 없으면 완료 요청하기";
        }
        if ("FEEDING_TIME".equalsIgnoreCase(mission.missionType)) {
            return "1  사료 상태 확인하기\n\n2  밥그릇을 채워주기\n\n3  카메라가 밥그릇을 인식할 때까지 기다리기";
        }
        if ("MEDICAL".equalsIgnoreCase(mission.missionType)) {
            return "1  로봇 강아지의 상태 확인하기\n\n2  필요한 진료를 진행하기\n\n3  완료 버튼을 눌러 진료비 반영하기";
        }
        if ("VET_CHECK".equalsIgnoreCase(mission.missionType)) {
            return "1  동물병원 진료 받기\n\n2  진료가 끝나면 완료 처리하기\n\n3  병원비가 양육비 기록에 자동 반영돼요";
        }
        if ("VACCINATION".equalsIgnoreCase(mission.missionType)) {
            return "1  예방접종 일정 확인하기\n\n2  접종을 마치면 완료 처리하기\n\n3  접종 비용이 양육비 기록에 자동 반영돼요";
        }
        return "1  로봇 강아지가 미션 위치를 확인하기\n\n2  필요한 행동을 수행하기\n\n3  완료되면 앱에서 확인 누르기";
    }

    private String getMissionNotes() {
        if ("FEEDING_TIME".equalsIgnoreCase(mission.missionType)) {
            return "- 완료 요청은 서버의 미션 완료 API로 처리됩니다.\n- 실제 기기 밥주기 명령은 별도 기능에서 처리됩니다.";
        }
        if ("MEDICAL".equalsIgnoreCase(mission.missionType)
                || "VET_CHECK".equalsIgnoreCase(mission.missionType)
                || "VACCINATION".equalsIgnoreCase(mission.missionType)) {
            return "- 이 미션은 실제 반려동물 양육비를 보여주기 위한 기록 흐름입니다.\n- 완료 시 비용이 자동으로 기록됩니다.";
        }
        return "- 로봇과 가까운 위치에서 진행해 주세요.\n- ESP32 센서 확인 API가 준비되면 이 단계에서 자동 완료로 연결됩니다.";
    }
}
