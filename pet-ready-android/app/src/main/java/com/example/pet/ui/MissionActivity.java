package com.example.pet.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pet.R;
import com.example.pet.model.MissionItem;
import com.example.pet.repository.MissionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MissionActivity extends AppCompatActivity {
    private final List<MissionItem> missions = new ArrayList<>();
    private LinearLayout layoutMissionList;
    private TextView tvMissionProgress;
    private TextView tvMissionStatus;
    private ProgressBar progressMission;
    private MissionRepository missionRepository;
    private boolean bindingMissions = false;
    private boolean lastLoadFromServer = false;
    private String lastLoadMessage = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission);

        layoutMissionList = findViewById(R.id.layoutMissionList);
        tvMissionProgress = findViewById(R.id.tvMissionProgress);
        tvMissionStatus = findViewById(R.id.tvMissionStatus);
        progressMission = findViewById(R.id.progressMission);
        missionRepository = new MissionRepository(this);
        findViewById(R.id.btnMissionBack).setOnClickListener(v -> finish());

        loadMissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (missionRepository != null) {
            loadMissions();
        }
    }

    private void loadMissions() {
        tvMissionStatus.setText("미션을 불러오는 중...");
        layoutMissionList.removeAllViews();

        missionRepository.getTodayMissions((loadedMissions, fromServer, message) -> {
            missions.clear();
            missions.addAll(loadedMissions);
            lastLoadFromServer = fromServer;
            lastLoadMessage = message;
            tvMissionStatus.setText(message);
            renderMissions();
        });
    }

    private void renderMissions() {
        bindingMissions = true;
        layoutMissionList.removeAllViews();

        if (missions.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText(lastLoadFromServer
                    ? "오늘은 등록된 미션이 없습니다."
                    : lastLoadMessage);
            emptyView.setTextSize(16f);
            layoutMissionList.addView(emptyView);
        }

        for (MissionItem mission : missions) {
            LinearLayout missionRow = new LinearLayout(this);
            missionRow.setGravity(Gravity.CENTER_VERTICAL);
            missionRow.setOrientation(LinearLayout.HORIZONTAL);
            missionRow.setMinimumHeight(dp(102));
            missionRow.setPadding(dp(16), dp(14), dp(14), dp(14));
            missionRow.setElevation(dp(3));

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            rowParams.setMargins(0, 0, 0, dp(12));
            missionRow.setLayoutParams(rowParams);

            LinearLayout textColumn = new LinearLayout(this);
            textColumn.setOrientation(LinearLayout.VERTICAL);
            textColumn.setGravity(Gravity.CENTER_VERTICAL);

            TextView titleView = new TextView(this);
            titleView.setText(buildMissionTitle(mission));
            titleView.setTextSize(20f);
            titleView.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));
            titleView.setTextColor(getColor(R.color.pet_text));
            titleView.setMaxLines(3);

            TextView statusView = new TextView(this);
            statusView.setText(getMissionStatusText(mission));
            statusView.setTextSize(14f);
            statusView.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));
            statusView.setGravity(Gravity.CENTER);
            statusView.setPadding(dp(10), dp(4), dp(10), dp(4));

            LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            statusParams.setMargins(0, dp(8), 0, 0);
            statusView.setLayoutParams(statusParams);

            textColumn.addView(titleView);
            textColumn.addView(statusView);

            if (mission.description != null && !mission.description.isEmpty()) {
                TextView descriptionView = new TextView(this);
                descriptionView.setText(mission.description);
                descriptionView.setTextSize(13f);
                descriptionView.setTextColor(getColor(R.color.pet_text_muted));
                descriptionView.setMaxLines(2);
                LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                descriptionParams.setMargins(0, dp(8), 0, 0);
                descriptionView.setLayoutParams(descriptionParams);
                textColumn.addView(descriptionView);
            }

            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            textParams.weight = 1f;
            textParams.setMargins(0, 0, dp(12), 0);
            textColumn.setLayoutParams(textParams);

            Button completeButton = new Button(this);
            completeButton.setAllCaps(false);
            completeButton.setText(getMissionButtonText(mission));
            completeButton.setTextSize(15f);
            completeButton.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));
            completeButton.setEnabled(!mission.completed);
            completeButton.setBackgroundResource(getMissionButtonBackground(mission, mission.completed));

            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                    dp(148),
                    dp(58)
            );
            completeButton.setLayoutParams(buttonParams);

            applyMissionStyle(missionRow, titleView, statusView, completeButton, mission, mission.completed);

            completeButton.setOnClickListener(v -> {
                openMissionTarget(mission);
            });

            missionRow.addView(textColumn);
            missionRow.addView(completeButton);
            layoutMissionList.addView(missionRow);
        }

        bindingMissions = false;
        updateMissionProgress();
    }

    private String buildMissionTitle(MissionItem mission) {
        if (mission.title == null || mission.title.isEmpty()) {
            return "미션 #" + mission.missionId;
        }
        return mission.title;
    }

    private void applyMissionStyle(View missionRow, TextView titleView, TextView statusView, Button completeButton, MissionItem mission, boolean completed) {
        if (completed) {
            missionRow.setBackgroundResource(R.drawable.bg_mission_item_done);
            titleView.setTextColor(getColor(R.color.pet_text_muted));
            statusView.setText("완료됨");
            statusView.setTextColor(getColor(R.color.pet_success));
            statusView.setBackgroundResource(R.drawable.bg_status_chip_done);
            completeButton.setBackgroundTintList(null);
            completeButton.setBackgroundResource(R.drawable.bg_mission_done_button);
            completeButton.setTextColor(getColor(R.color.pet_primary));
        } else {
            missionRow.setBackgroundResource(R.drawable.bg_mission_item);
            titleView.setTextColor(getColor(R.color.pet_text));
            statusView.setText(getMissionStatusText(mission));
            statusView.setTextColor(getColor(R.color.pet_warning));
            statusView.setBackgroundResource(R.drawable.bg_status_chip_wait);
            completeButton.setBackgroundTintList(null);
            completeButton.setBackgroundResource(getMissionButtonBackground(mission, false));
            completeButton.setTextColor(getColor(R.color.white));
        }
    }

    private int getMissionButtonBackground(MissionItem mission, boolean completed) {
        if (completed) {
            return R.drawable.bg_mission_done_button;
        }
        return isWalkMission(mission) ? R.drawable.bg_mission_walk : R.drawable.bg_mission_action;
    }

    private String getActionButtonText(MissionItem mission) {
        if (isWalkMission(mission)) {
            return "산책하러 가기";
        }
        return "미션 하러가기";
    }

    private String getMissionStatusText(MissionItem mission) {
        if (mission.completed || "COMPLETED".equalsIgnoreCase(mission.status)) {
            return "완료됨";
        }
        if ("IN_PROGRESS".equalsIgnoreCase(mission.status)) {
            return "진행 중";
        }
        if ("FAILED".equalsIgnoreCase(mission.status)) {
            return "실패";
        }
        return "진행 전";
    }

    private String getMissionButtonText(MissionItem mission) {
        if (mission.completed || "COMPLETED".equalsIgnoreCase(mission.status)) {
            return "완료";
        }
        if ("IN_PROGRESS".equalsIgnoreCase(mission.status)) {
            return "진행 중";
        }
        return getActionButtonText(mission);
    }

    private void openMissionTarget(MissionItem mission) {
        Intent intent = new Intent(this, MissionDetailActivity.class);
        MissionIntentExtras.putMission(intent, mission);
        startActivity(intent);
    }

    private boolean isWalkMission(MissionItem mission) {
        return "WALK".equalsIgnoreCase(mission.missionType)
                || (mission.title != null && mission.title.toLowerCase().contains("walk"));
    }

    private void updateMissionProgress() {
        int completedCount = 0;
        for (MissionItem mission : missions) {
            if (mission.completed) {
                completedCount++;
            }
        }

        int progress = missions.isEmpty() ? 0 : completedCount * 100 / missions.size();
        progressMission.setProgress(progress);
        tvMissionProgress.setText(String.format(
                Locale.KOREA,
                "완료율 %d%% (%d/%d)",
                progress,
                completedCount,
                missions.size()
        ));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
