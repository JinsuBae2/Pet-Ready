package com.example.pet.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pet.R;
import com.example.pet.model.CareStatus;
import com.example.pet.model.DashboardResponse;
import com.example.pet.model.MissionItem;
import com.example.pet.model.ReportSummary;
import com.example.pet.repository.CareStatusRepository;
import com.example.pet.repository.DashboardRepository;
import com.example.pet.repository.MissionRepository;
import com.example.pet.repository.PetProfileRepository;
import com.example.pet.repository.ScoreRepository;
import com.example.pet.repository.SimulationRepository;

public class DashboardActivity extends AppCompatActivity {
    private ImageView ivPetAvatar;
    private TextView tvPetName;
    private TextView tvSimulationDay;
    private TextView tvHungerGraph;
    private TextView tvAffinityGraph;
    private android.widget.ProgressBar progressHunger;
    private android.widget.ProgressBar progressAffinity;
    private TextView tvHomeScore;
    private TextView tvHomeScoreEvent;
    private TextView tvTodayMissionSummary;
    private TextView tvTodayMissionStatus;
    private Button btnReport;
    private ImageButton btnPetSettings;
    private Button btnActivityLog;
    private TextView btnNavMission;
    private TextView btnNavWalk;
    private TextView btnNavExpense;
    private TextView btnNavMore;

    private PetProfileRepository profileRepository;
    private ScoreRepository scoreRepository;
    private CareStatusRepository careStatusRepository;
    private SimulationRepository simulationRepository;
    private DashboardRepository dashboardRepository;
    private MissionRepository missionRepository;
    private final Handler dashboardHandler = new Handler(Looper.getMainLooper());
    private final Runnable dashboardPoller = new Runnable() {
        @Override
        public void run() {
            loadServerDashboard();
            dashboardHandler.postDelayed(this, 30_000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        ivPetAvatar = findViewById(R.id.ivPetAvatar);
        tvPetName = findViewById(R.id.tvPetName);
        tvSimulationDay = findViewById(R.id.tvSimulationDay);
        tvHungerGraph = findViewById(R.id.tvHungryGraph);
        tvAffinityGraph = findViewById(R.id.tvAffinityGraph);
        progressHunger = findViewById(R.id.progressHungry);
        progressAffinity = findViewById(R.id.progressAffinity);
        tvHomeScore = findViewById(R.id.tvHomeScore);
        tvHomeScoreEvent = findViewById(R.id.tvHomeScoreEvent);
        tvTodayMissionSummary = findViewById(R.id.tvTodayMissionSummary);
        tvTodayMissionStatus = findViewById(R.id.tvTodayMissionStatus);
        btnReport = findViewById(R.id.btnReport);
        btnPetSettings = findViewById(R.id.btnPetSettings);
        btnActivityLog = findViewById(R.id.btnActivityLog);
        btnNavMission = findViewById(R.id.btnNavMission);
        btnNavWalk = findViewById(R.id.btnNavWalk);
        btnNavExpense = findViewById(R.id.btnNavExpense);
        btnNavMore = findViewById(R.id.btnNavMore);

        profileRepository = new PetProfileRepository(this);
        scoreRepository = new ScoreRepository(this);
        careStatusRepository = new CareStatusRepository(this);
        simulationRepository = new SimulationRepository(this);
        dashboardRepository = new DashboardRepository(this);
        missionRepository = new MissionRepository(this);

        btnPetSettings.setOnClickListener(v -> startActivity(new Intent(this, PetSettingsActivity.class)));
        btnReport.setOnClickListener(v -> startActivity(new Intent(this, ReportActivity.class)));
        btnActivityLog.setOnClickListener(v -> startActivity(new Intent(this, ActivityLogActivity.class)));
        findViewById(R.id.btnOpenTodayMissions)
                .setOnClickListener(v -> startActivity(new Intent(this, MissionActivity.class)));
        btnNavMission.setOnClickListener(v -> startActivity(new Intent(this, MissionActivity.class)));
        btnNavWalk.setOnClickListener(v -> startActivity(new Intent(this, WalkActivity.class)));
        btnNavExpense.setOnClickListener(v -> startActivity(new Intent(this, ExpenseActivity.class)));
        btnNavMore.setOnClickListener(v -> startActivity(new Intent(this, MoreActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!simulationRepository.isSetupDone()) {
            startActivity(new Intent(this, SimulationSetupActivity.class));
            return;
        }
        if (simulationRepository.isFinished()) {
            Intent intent = new Intent(this, FinalReportActivity.class);
            startActivity(intent);
            return;
        }
        showPetStatus();
        showScoreSummary();
        showSimulationDay();
        loadTodayMissionSummary();
        dashboardHandler.removeCallbacks(dashboardPoller);
        dashboardHandler.post(dashboardPoller);
    }

    @Override
    protected void onPause() {
        dashboardHandler.removeCallbacks(dashboardPoller);
        super.onPause();
    }

    private void showPetStatus() {
        CareStatus careStatus = careStatusRepository.getStatus();
        String photoUri = profileRepository.getPhotoUri();

        tvPetName.setText(profileRepository.getPetName());
        tvHungerGraph.setText(careStatus.hungerLevel + "%");
        tvAffinityGraph.setText(careStatus.affinityLevel + "%");
        progressHunger.setProgress(careStatus.hungerLevel);
        progressAffinity.setProgress(careStatus.affinityLevel);

        ivPetAvatar.setBackgroundResource(getAvatarBackground(profileRepository.getAvatarType()));
        if (photoUri == null || photoUri.isEmpty()) {
            ivPetAvatar.setImageResource(R.drawable.ic_robot_pet);
        } else {
            ivPetAvatar.setImageURI(Uri.parse(photoUri));
        }
    }

    private void showScoreSummary() {
        ReportSummary report = scoreRepository.getReportSummary();
        tvHomeScore.setText(report.getFinalScore() + "점");
        String event = report.lastScoreEvent == null || report.lastScoreEvent.isEmpty()
                ? "아직 점수 변동 없음"
                : report.lastScoreEvent + " " + String.format("%+d", report.lastScoreDelta);
        tvHomeScoreEvent.setText(event);
    }

    private void loadServerDashboard() {
        dashboardRepository.getDashboard((dashboard, fromServer, message) -> {
            if (!fromServer || dashboard == null) {
                return;
            }
            renderServerDashboard(dashboard);
        });
    }

    private void loadTodayMissionSummary() {
        tvTodayMissionStatus.setText("오늘의 미션을 불러오는 중...");
        missionRepository.getTodayMissions((missions, fromServer, message) -> {
            int completedCount = 0;
            for (MissionItem mission : missions) {
                if (mission.completed) {
                    completedCount++;
                }
            }

            tvTodayMissionSummary.setText(
                    "전체 " + missions.size() + "개 · 완료 " + completedCount + "개"
            );
            tvTodayMissionStatus.setText(message);
        });
    }

    private void renderServerDashboard(DashboardResponse dashboard) {
        tvHomeScore.setText(dashboard.currentScore + "점");
        String event = dashboard.lastScoreEvent == null
                || dashboard.lastScoreEvent.isEmpty()
                || "NONE".equalsIgnoreCase(dashboard.lastScoreEvent)
                ? "아직 점수 변동 없음"
                : dashboard.lastScoreEvent + " " + String.format("%+d", dashboard.lastScoreDelta);
        tvHomeScoreEvent.setText(event);

        if (dashboard.petName != null && !dashboard.petName.trim().isEmpty()) {
            tvPetName.setText(dashboard.petName);
            profileRepository.saveProfile(
                    dashboard.petName,
                    profileRepository.getAvatarType(),
                    profileRepository.getPhotoUri()
            );
        }
    }

    private void showSimulationDay() {
        tvSimulationDay.setText(simulationRepository.getCurrentDay() + "일차 / " + simulationRepository.getTotalDays() + "일");
    }

    private int getAvatarBackground(int avatarType) {
        if (avatarType == 1) {
            return R.drawable.bg_pet_avatar_blue;
        }
        if (avatarType == 2) {
            return R.drawable.bg_pet_avatar_pink;
        }
        return R.drawable.bg_pet_avatar_green;
    }
}
