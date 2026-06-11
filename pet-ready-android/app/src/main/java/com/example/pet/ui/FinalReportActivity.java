package com.example.pet.ui;

import android.os.Bundle;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pet.R;
import com.example.pet.model.RecommendedAnimal;
import com.example.pet.model.ReportAnalysis;
import com.example.pet.repository.ReportAnalysisRepository;

import java.util.List;
import java.util.Locale;

public class FinalReportActivity extends AppCompatActivity {
    public static final String EXTRA_PREVIEW_MODE = "preview_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_final_report);

        boolean previewMode = getIntent().getBooleanExtra(EXTRA_PREVIEW_MODE, false);
        ReportAnalysis report = new ReportAnalysisRepository(this).getReportAnalysis();

        TextView tvFinalReportTitle = findViewById(R.id.tvFinalReportTitle);
        TextView tvFinalScore = findViewById(R.id.tvFinalReportScore);
        TextView tvFinalStats = findViewById(R.id.tvFinalReportStats);
        TextView tvUserType = findViewById(R.id.tvFinalUserType);
        TextView tvBreedRecommendation = findViewById(R.id.tvFinalBreedRecommendation);
        TextView tvContextMessage = findViewById(R.id.tvFinalContextMessage);
        LinearLayout layoutAnimals = findViewById(R.id.layoutFinalAnimals);
        Button btnClose = findViewById(R.id.btnFinalReportClose);

        tvFinalReportTitle.setText(previewMode ? "최종 리포트 미리보기" : "최종 리포트");
        tvFinalScore.setText(String.format(Locale.KOREA, "%d점 · %s", report.finalScore, report.grade));
        tvFinalStats.setText(String.format(
                Locale.KOREA,
                "산책 점수 %d점\n응답 점수 %d점\n건강 페널티 %d점\n누적 산책 %.1fkm\n평균 반응 %d초\n누적 병원비 %,d원",
                report.walkScore,
                report.responseScore,
                report.healthPenalty,
                report.totalWalkKm,
                report.avgResponseSec,
                report.totalMedicalFee
        ));
        tvUserType.setText(report.userTypeLabel + "\n" + report.userType);
        tvBreedRecommendation.setText(String.format(
                Locale.KOREA,
                "%s\n추천 예시: %s\n\n%s",
                report.breedRecommendation.type,
                report.breedRecommendation.examples,
                report.breedRecommendation.reason
        ));
        tvContextMessage.setText(report.contextMessage);
        renderAnimals(layoutAnimals, report.recommendedAnimals);
        btnClose.setOnClickListener(v -> finish());
    }

    private void renderAnimals(LinearLayout container, List<RecommendedAnimal> animals) {
        container.removeAllViews();
        if (animals == null || animals.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("추천 가능한 유기견 정보가 아직 없습니다.");
            empty.setTextColor(getColor(R.color.pet_text_muted));
            empty.setTextSize(15f);
            container.addView(empty);
            return;
        }

        for (RecommendedAnimal animal : animals) {
            addAnimalRow(container, animal);
        }
    }

    private void addAnimalRow(LinearLayout container, RecommendedAnimal animal) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(10), 0, dp(12));

        TextView title = new TextView(this);
        title.setText(animal.breedType + " · " + animal.sexCd + " · " + animal.happenDt);
        title.setTextColor(getColor(R.color.pet_text));
        title.setTextSize(16f);
        title.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));

        TextView detail = new TextView(this);
        detail.setText(String.format(
                Locale.KOREA,
                "%s / 중성화 %s\n%s\n%s",
                animal.colorCd,
                animal.neuteredYn,
                animal.careNm,
                animal.matchReason
        ));
        detail.setTextColor(getColor(R.color.pet_text_muted));
        detail.setTextSize(13f);
        detail.setGravity(Gravity.START);
        detail.setLineSpacing(0f, 1.15f);

        row.addView(title);
        row.addView(detail);
        container.addView(row);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
