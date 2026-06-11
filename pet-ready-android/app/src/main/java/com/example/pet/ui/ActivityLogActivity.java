package com.example.pet.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pet.R;
import com.example.pet.model.ActivityLogItem;
import com.example.pet.repository.ActivityLogRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ActivityLogActivity extends AppCompatActivity {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd", Locale.KOREA);
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.KOREA);

    private LinearLayout layoutActivityLogs;
    private ActivityLogRepository activityLogRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity_log);

        layoutActivityLogs = findViewById(R.id.layoutActivityLogs);
        activityLogRepository = new ActivityLogRepository(this);

        findViewById(R.id.btnNavHome).setOnClickListener(v -> startActivity(new Intent(this, DashboardActivity.class)));
        findViewById(R.id.btnNavMission).setOnClickListener(v -> startActivity(new Intent(this, MissionActivity.class)));
        findViewById(R.id.btnNavWalk).setOnClickListener(v -> startActivity(new Intent(this, WalkActivity.class)));
        findViewById(R.id.btnNavExpense).setOnClickListener(v -> startActivity(new Intent(this, ExpenseActivity.class)));
        findViewById(R.id.btnNavMore).setOnClickListener(v -> startActivity(new Intent(this, MoreActivity.class)));

        renderLogs();
    }

    private void renderLogs() {
        layoutActivityLogs.removeAllViews();
        List<ActivityLogItem> logs = activityLogRepository.getLogs();

        if (logs.isEmpty()) {
            addDateHeader(dateFormat.format(new Date()));
            addLogRow(new ActivityLogItem(
                    ActivityLogRepository.TYPE_MISSION,
                    "아직 기록이 없습니다",
                    "미션, 산책, 긴급 알림 기록이 여기에 표시돼요",
                    System.currentTimeMillis()
            ));
            return;
        }

        String currentDate = "";
        for (ActivityLogItem log : logs) {
            String date = dateFormat.format(new Date(log.timestampMillis));
            if (!date.equals(currentDate)) {
                currentDate = date;
                addDateHeader(currentDate);
            }
            addLogRow(log);
        }
    }

    private void addDateHeader(String date) {
        TextView dateView = new TextView(this);
        dateView.setText(date + " ⌄");
        dateView.setTextColor(getColor(R.color.pet_text_muted));
        dateView.setTextSize(14f);
        dateView.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(18), 0, dp(8));
        dateView.setLayoutParams(params);
        layoutActivityLogs.addView(dateView);
    }

    private void addLogRow(ActivityLogItem log) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(8), 0, dp(8));

        TextView icon = new TextView(this);
        icon.setText(getIcon(log.type));
        icon.setGravity(Gravity.CENTER);
        icon.setTextSize(20f);
        icon.setBackgroundResource(getIconBackground(log.type));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(42), dp(42));
        row.addView(icon, iconParams);

        LinearLayout textColumn = new LinearLayout(this);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        textColumn.setPadding(dp(12), 0, dp(8), 0);

        TextView title = new TextView(this);
        title.setText(log.title);
        title.setTextColor(getColor(R.color.pet_text));
        title.setTextSize(16f);
        title.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));

        TextView detail = new TextView(this);
        detail.setText(log.detail);
        detail.setTextColor(getColor(R.color.pet_text_muted));
        detail.setTextSize(13f);

        textColumn.addView(title);
        textColumn.addView(detail);
        row.addView(textColumn, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView time = new TextView(this);
        time.setText(timeFormat.format(new Date(log.timestampMillis)));
        time.setTextColor(getColor(R.color.pet_text_muted));
        time.setTextSize(13f);
        time.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));
        row.addView(time);

        layoutActivityLogs.addView(row);
    }

    private String getIcon(String type) {
        if (ActivityLogRepository.TYPE_WALK.equals(type)) {
            return "⌁";
        }
        if (ActivityLogRepository.TYPE_URGENT.equals(type)) {
            return "!";
        }
        return "⌂";
    }

    private int getIconBackground(String type) {
        if (ActivityLogRepository.TYPE_WALK.equals(type)) {
            return R.drawable.bg_activity_icon_blue;
        }
        if (ActivityLogRepository.TYPE_URGENT.equals(type)) {
            return R.drawable.bg_activity_icon_pink;
        }
        return R.drawable.bg_activity_icon_mint;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
