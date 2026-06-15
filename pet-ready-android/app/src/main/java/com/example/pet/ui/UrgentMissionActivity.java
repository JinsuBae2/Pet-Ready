package com.example.pet.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pet.R;
import com.example.pet.model.MissionItem;
import com.example.pet.repository.MissionRepository;
import com.example.pet.repository.PetProfileRepository;

public class UrgentMissionActivity extends AppCompatActivity {
    public static final String EXTRA_MISSION_ID = "mission_id";
    public static final String EXTRA_MISSION_TYPE = "mission_type";
    public static final String EXTRA_MISSION_TITLE = "mission_title";
    public static final String EXTRA_MISSION_MESSAGE = "mission_message";

    private TextView tvUrgentMissionTitle;
    private TextView tvUrgentMissionMessage;
    private TextView tvUrgentMissionStatus;
    private TextView tvUrgentMissionSteps;
    private TextView tvUrgentMissionNotes;
    private Button btnUrgentRespond;
    private TextView btnUrgentClose;

    private MissionRepository missionRepository;
    private MissionItem mission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_urgent_mission);

        tvUrgentMissionTitle = findViewById(R.id.tvUrgentMissionTitle);
        tvUrgentMissionMessage = findViewById(R.id.tvUrgentMissionMessage);
        tvUrgentMissionStatus = findViewById(R.id.tvUrgentMissionStatus);
        tvUrgentMissionSteps = findViewById(R.id.tvUrgentMissionSteps);
        tvUrgentMissionNotes = findViewById(R.id.tvUrgentMissionNotes);
        btnUrgentRespond = findViewById(R.id.btnUrgentRespond);
        btnUrgentClose = findViewById(R.id.btnUrgentClose);

        missionRepository = new MissionRepository(this);
        mission = buildMissionFromIntent();

        String petName = new PetProfileRepository(this).getPetName();
        tvUrgentMissionTitle.setText(PetProfileRepository.replaceRobotDog(mission.title, petName));
        tvUrgentMissionMessage.setText(PetProfileRepository.replaceRobotDog(mission.description, petName));
        tvUrgentMissionStatus.setText("긴급");
        tvUrgentMissionSteps.setText(PetProfileRepository.replaceRobotDog(getMissionSteps(), petName));
        tvUrgentMissionNotes.setText(PetProfileRepository.replaceRobotDog(getMissionNotes(), petName));

        btnUrgentRespond.setOnClickListener(v -> respondToMission());
        btnUrgentClose.setOnClickListener(v -> finish());
    }

    private MissionItem buildMissionFromIntent() {
        long missionId = getIntent().getLongExtra(EXTRA_MISSION_ID, 101L);
        String missionType = getIntent().getStringExtra(EXTRA_MISSION_TYPE);
        String missionTitle = getIntent().getStringExtra(EXTRA_MISSION_TITLE);
        String missionMessage = getIntent().getStringExtra(EXTRA_MISSION_MESSAGE);

        if (missionType == null || missionType.isEmpty()) {
            missionType = "BARKING_ALERT";
        }
        if (missionTitle == null || missionTitle.isEmpty()) {
            missionTitle = "돌발 미션이 도착했어요";
        }
        if (missionMessage == null || missionMessage.isEmpty()) {
            missionMessage = "로봇 강아지의 상태를 확인하고 대응해 주세요.";
        }

        MissionItem item = new MissionItem(missionId, missionTitle, false);
        item.missionType = missionType;
        item.description = missionMessage;
        return item;
    }

    private void respondToMission() {
        btnUrgentRespond.setEnabled(false);
        tvUrgentMissionStatus.setText("전송 중");

        missionRepository.completeMission(mission, (completedMission, fromServer, message) -> {
            mission = completedMission;
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            if (mission.completed) {
                btnUrgentRespond.setText("대응 완료");
                tvUrgentMissionStatus.setText("완료");
                setResult(RESULT_OK);
                finish();
            } else if (fromServer && "FEEDING_TIME".equalsIgnoreCase(mission.missionType)) {
                btnUrgentRespond.setText("밥그릇 인식 대기 중");
                tvUrgentMissionStatus.setText("확인 대기");
            } else {
                btnUrgentRespond.setEnabled(true);
                tvUrgentMissionStatus.setText("전송 실패");
            }
        });
    }

    private String getMissionSteps() {
        if ("BARKING_ALERT".equalsIgnoreCase(mission.missionType)) {
            return "1  로봇 강아지의 현재 상태 확인하기\n\n2  주변 소음과 위험 요소 확인하기\n\n3  앱에서 대응 완료 처리하기";
        }
        if ("FEEDING_TIME".equalsIgnoreCase(mission.missionType)) {
            return "1  급식 상태 확인하기\n\n2  물그릇과 주변 상태 확인하기\n\n3  필요한 돌봄 후 대응 완료하기";
        }
        return "1  서버가 보낸 돌발 미션 내용 확인하기\n\n2  로봇 강아지 주변 상태 살펴보기\n\n3  필요한 조치 후 대응 완료하기";
    }

    private String getMissionNotes() {
        return "- 실제 상황을 먼저 확인한 뒤 대응해 주세요.\n- 대응 완료를 누르면 서버에 완료 상태가 반영됩니다.";
    }
}
