package com.example.pet.ui;

import android.os.Bundle;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pet.R;
import com.example.pet.model.ReportSummary;
import com.example.pet.model.ScoreEventItem;
import com.example.pet.repository.ScoreRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportActivity extends AppCompatActivity {
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("MM.dd HH:mm", Locale.KOREA);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        TextView tvFinalScore = findViewById(R.id.tvFinalScore);
        TextView tvCareScore = findViewById(R.id.tvCareScore);
        TextView tvWalkScore = findViewById(R.id.tvWalkScore);
        TextView tvMissionScore = findViewById(R.id.tvMissionScore);
        TextView tvReportComment = findViewById(R.id.tvReportComment);
        LinearLayout layoutScoreEvents = findViewById(R.id.layoutScoreEvents);
        ProgressBar progressCareScore = findViewById(R.id.progressCareScore);
        ProgressBar progressWalkScore = findViewById(R.id.progressWalkScore);
        ProgressBar progressMissionScore = findViewById(R.id.progressMissionScore);

        ScoreRepository scoreRepository = new ScoreRepository(this);
        ReportSummary report = scoreRepository.getReportSummary();

        tvFinalScore.setText(String.format(Locale.KOREA, "현재 점수 %d점", report.getFinalScore()));
        tvCareScore.setText(String.format(Locale.KOREA, "건강 점수: %d점", report.careScore));
        tvWalkScore.setText(String.format(Locale.KOREA, "산책 점수: %d점", report.walkScore));
        tvMissionScore.setText(String.format(Locale.KOREA, "응답 점수: %d점", report.missionScore));
        tvReportComment.setText(String.format(
                Locale.KOREA,
                "최근 점수 이벤트\n%s (%+d점)\n\n종합 코멘트\n%s",
                report.lastScoreEvent == null || report.lastScoreEvent.isEmpty() ? "아직 없음" : report.lastScoreEvent,
                report.lastScoreDelta,
                report.comment
        ));
        progressCareScore.setProgress(report.careScore);
        progressWalkScore.setProgress(report.walkScore);
        progressMissionScore.setProgress(report.missionScore);

        renderScoreEvents(layoutScoreEvents, scoreRepository.getRecentScoreEvents());
    }

    private void renderScoreEvents(LinearLayout container, List<ScoreEventItem> events) {
        container.removeAllViews();

        if (events.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("아직 기록된 점수 이벤트가 없습니다.");
            emptyView.setTextColor(getColor(R.color.pet_text_muted));
            emptyView.setTextSize(15f);
            emptyView.setGravity(Gravity.CENTER);
            emptyView.setPadding(0, dp(20), 0, dp(16));
            container.addView(emptyView);
            return;
        }

        for (ScoreEventItem event : events) {
            addScoreEventRow(container, event);
        }
    }

    private void addScoreEventRow(LinearLayout container, ScoreEventItem event) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(8), 0, dp(8));

        LinearLayout textColumn = new LinearLayout(this);
        textColumn.setOrientation(LinearLayout.VERTICAL);

        TextView eventType = new TextView(this);
        eventType.setText(event.eventType);
        eventType.setTextColor(getColor(R.color.pet_text));
        eventType.setTextSize(15f);
        eventType.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));

        TextView eventMeta = new TextView(this);
        eventMeta.setText(String.format(Locale.KOREA, "%s · 반영 후 %d점", timeFormat.format(new Date(event.occurredAtMillis)), event.scoreAfter));
        eventMeta.setTextColor(getColor(R.color.pet_text_muted));
        eventMeta.setTextSize(13f);

        textColumn.addView(eventType);
        textColumn.addView(eventMeta);
        row.addView(textColumn, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView delta = new TextView(this);
        delta.setText(String.format(Locale.KOREA, "%+d", event.delta));
        delta.setTextColor(getColor(event.delta >= 0 ? R.color.pet_primary : R.color.pet_text_muted));
        delta.setTextSize(17f);
        delta.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));
        row.addView(delta);

        container.addView(row);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
