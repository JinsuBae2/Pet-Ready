package com.example.pet.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pet.R;
import com.example.pet.model.MissionItem;

public class MissionDetailActivity extends AppCompatActivity {
    private MissionItem mission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission_detail);

        mission = MissionIntentExtras.readMission(getIntent());

        TextView tvMissionDetailTitle = findViewById(R.id.tvMissionDetailTitle);
        TextView tvMissionDetailStatus = findViewById(R.id.tvMissionDetailStatus);
        TextView tvMissionDetailDescription = findViewById(R.id.tvMissionDetailDescription);
        TextView tvMissionSteps = findViewById(R.id.tvMissionSteps);
        TextView tvMissionNotes = findViewById(R.id.tvMissionNotes);
        Button btnMissionStart = findViewById(R.id.btnMissionStart);

        findViewById(R.id.btnMissionDetailBack).setOnClickListener(v -> finish());

        tvMissionDetailTitle.setText(mission.title);
        tvMissionDetailStatus.setText("진행 중");
        tvMissionDetailDescription.setText(getDetailDescription());
        tvMissionSteps.setText(getMissionSteps());
        tvMissionNotes.setText(getMissionNotes());
        btnMissionStart.setOnClickListener(v -> openProgress());
    }

    private void openProgress() {
        Intent intent = new Intent(this, MissionProgressActivity.class);
        MissionIntentExtras.putMission(intent, mission);
        startActivity(intent);
    }

    private String getDetailDescription() {
        if (mission.description != null && !mission.description.isEmpty()) {
            return mission.description;
        }
        return "로봇 강아지와 함께 미션을 수행해 주세요.";
    }

    private String getMissionSteps() {
        if ("ROBOT_PLAY".equalsIgnoreCase(mission.missionType)) {
            return "1  로봇 강아지를 가까이 두기\n\n2  터치하거나 말을 걸어 상호작용하기\n\n3  반응을 확인한 뒤 완료 누르기";
        }
        if ("DEVICE_STATUS".equalsIgnoreCase(mission.missionType)) {
            return "1  로봇 전원과 배터리 확인하기\n\n2  연결 상태 확인하기\n\n3  이상이 없으면 완료 누르기";
        }
        if ("FEEDING_TIME".equalsIgnoreCase(mission.missionType)) {
            return "1  지출 화면에서 사료가 있는지 확인하기\n\n2  밥그릇을 채워주기\n\n3  로봇이 밥그릇 상태를 확인하면 완료돼요";
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
            return "- 밥그릇 미션을 완료하면 사료가 1회분 줄어듭니다.\n- 사료가 부족하면 지출 화면에서 사료를 사서 채울 수 있어요.";
        }
        if ("VET_CHECK".equalsIgnoreCase(mission.missionType)
                || "VACCINATION".equalsIgnoreCase(mission.missionType)) {
            return "- 이 미션은 실제 반려동물 양육비를 보여주기 위한 기록 흐름입니다.\n- 완료 시 비용이 자동으로 기록됩니다.";
        }
        return "- 로봇과 가까운 위치에서 진행해 주세요.\n- ESP32 센서 확인 API가 준비되면 이 단계에서 자동 완료로 연결됩니다.";
    }
}
