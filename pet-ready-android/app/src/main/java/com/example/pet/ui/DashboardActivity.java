package com.example.pet.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pet.R;
import com.example.pet.model.CareStatus;
import com.example.pet.model.ReportSummary;
import com.example.pet.repository.CareStatusRepository;
import com.example.pet.repository.PetProfileRepository;
import com.example.pet.repository.PetRepository;
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
    private Button btnReport;
    private ImageButton btnPetSettings;
    private Button btnActivityLog;
    private TextView btnNavMission;
    private TextView btnNavWalk;
    private TextView btnNavExpense;
    private TextView btnNavMore;

    private PetRepository petRepository;
    private PetProfileRepository profileRepository;
    private ScoreRepository scoreRepository;
    private CareStatusRepository careStatusRepository;
    private SimulationRepository simulationRepository;

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
        btnReport = findViewById(R.id.btnReport);
        btnPetSettings = findViewById(R.id.btnPetSettings);
        btnActivityLog = findViewById(R.id.btnActivityLog);
        btnNavMission = findViewById(R.id.btnNavMission);
        btnNavWalk = findViewById(R.id.btnNavWalk);
        btnNavExpense = findViewById(R.id.btnNavExpense);
        btnNavMore = findViewById(R.id.btnNavMore);

        petRepository = PetRepository.getInstance();
        profileRepository = new PetProfileRepository(this);
        scoreRepository = new ScoreRepository(this);
        careStatusRepository = new CareStatusRepository(this);
        simulationRepository = new SimulationRepository(this);

        btnPetSettings.setOnClickListener(v -> startActivity(new Intent(this, PetSettingsActivity.class)));
        btnReport.setOnClickListener(v -> startActivity(new Intent(this, ReportActivity.class)));
        btnActivityLog.setOnClickListener(v -> startActivity(new Intent(this, ActivityLogActivity.class)));
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
            intent.putExtra(FinalReportActivity.EXTRA_PREVIEW_MODE, false);
            startActivity(intent);
            return;
        }
        showPetStatus();
        showScoreSummary();
        showSimulationDay();
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
