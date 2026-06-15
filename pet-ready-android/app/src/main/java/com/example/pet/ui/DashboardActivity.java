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
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pet.R;
import com.example.pet.model.CareStatus;
import com.example.pet.model.DashboardResponse;
import com.example.pet.model.MissionItem;
import com.example.pet.model.PetStatusResponse;
import com.example.pet.model.ReportSummary;
import com.example.pet.repository.CareStatusRepository;
import com.example.pet.repository.DashboardRepository;
import com.example.pet.repository.MissionRepository;
import com.example.pet.repository.PetProfileRepository;
import com.example.pet.repository.PetStatusRepository;
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
    private TextView tvTodayMissionSummary;
    private TextView tvTodayMissionStatus;
    private TextView tvPetRoutineStatus;
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
    private PetStatusRepository petStatusRepository;
    private final Handler dashboardHandler = new Handler(Looper.getMainLooper());
    private final Runnable dashboardPoller = new Runnable() {
        @Override
        public void run() {
            loadServerDashboard();
            loadPetStatus();
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
        tvTodayMissionSummary = findViewById(R.id.tvTodayMissionSummary);
        tvTodayMissionStatus = findViewById(R.id.tvTodayMissionStatus);
        tvPetRoutineStatus = findViewById(R.id.tvPetRoutineStatus);
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
        petStatusRepository = new PetStatusRepository(this);

        btnPetSettings.setOnClickListener(v -> startActivity(new Intent(this, PetSettingsActivity.class)));
        btnReport.setOnClickListener(v -> startActivity(new Intent(this, ReportActivity.class)));
        btnActivityLog.setOnClickListener(v -> startActivity(new Intent(this, ActivityLogActivity.class)));
        findViewById(R.id.btnFeedPet).setOnClickListener(v -> feedPet());
        findViewById(R.id.btnTrainingReward)
                .setOnClickListener(v -> startActivity(new Intent(this, TrainingActivity.class)));
        findViewById(R.id.btnForceFinishSimulation).setOnClickListener(v -> forceFinishSimulation());
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
        if (simulationRepository.shouldShowFinalReport()) {
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
    }

    private void loadServerDashboard() {
        dashboardRepository.getDashboard((dashboard, fromServer, message) -> {
            if (!fromServer || dashboard == null) {
                return;
            }
            renderServerDashboard(dashboard);
        });
    }

    private void loadPetStatus() {
        petStatusRepository.getStatus((status, errorMessage) -> {
            if (status == null) {
                tvPetRoutineStatus.setText("상태 연결 실패");
                return;
            }
            renderPetStatus(status);
        });
    }

    private void renderPetStatus(PetStatusResponse status) {
        String mood = status.mood == null ? "HAPPY" : status.mood.toUpperCase(java.util.Locale.US);
        tvPetRoutineStatus.setText(getMoodLabel(mood));
        ivPetAvatar.setContentDescription("반려견 상태: " + mood);

        careStatusRepository.updateHunger(status.batteryLevel);
        showPetStatus();

        if ("SICK".equals(mood) || "HUNGRY".equals(mood)
                || "BARKING".equals(mood) || "CONFUSED".equals(mood)
                || "SAD".equals(mood)) {
            ivPetAvatar.setBackgroundResource(R.drawable.bg_pet_avatar_pink);
        } else if ("SLEEPING".equals(mood) || "BORED".equals(mood)) {
            ivPetAvatar.setBackgroundResource(R.drawable.bg_pet_avatar_blue);
        } else {
            ivPetAvatar.setBackgroundResource(R.drawable.bg_pet_avatar_green);
        }
    }

    private String getMoodLabel(String mood) {
        switch (mood) {
            case "SLEEPING":
                return "취침 중";
            case "HUNGRY":
                return "배고픔";
            case "BARKING":
                return "짖는 중";
            case "SICK":
                return "아픔";
            case "BORED":
                return "심심함";
            case "SUCCESS":
                return "훈련 성공";
            case "CONFUSED":
                return "혼란";
            case "SAD":
                return "속상함";
            default:
                return "행복";
        }
    }

    private void feedPet() {
        petStatusRepository.feed((success, errorMessage) -> {
            if (success) {
                Toast.makeText(this, "밥 주기 신호를 전송했습니다.", Toast.LENGTH_SHORT).show();
                loadPetStatus();
                return;
            }
            Toast.makeText(this, "밥 주기 실패: " + errorMessage, Toast.LENGTH_LONG).show();
        });
    }

    private void forceFinishSimulation() {
        new AlertDialog.Builder(this)
                .setTitle("시뮬레이션 강제 종료")
                .setMessage("데모용으로 체험 기간을 즉시 완료하고 최종 리포트로 이동할까요?")
                .setNegativeButton("취소", null)
                .setPositiveButton("종료", (dialog, which) -> {
                    Intent intent = new Intent(this, FinalReportActivity.class);
                    intent.putExtra(FinalReportActivity.EXTRA_DEMO_PREVIEW, true);
                    startActivity(intent);
                })
                .show();
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
