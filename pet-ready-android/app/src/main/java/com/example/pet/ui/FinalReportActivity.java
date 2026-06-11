package com.example.pet.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.pet.R;
import com.example.pet.model.RecommendedAnimal;
import com.example.pet.model.ReportAnalysis;
import com.example.pet.repository.ReportAnalysisRepository;

import java.util.List;
import java.util.Locale;

public class FinalReportActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_final_report);

        TextView tvFinalReportTitle = findViewById(R.id.tvFinalReportTitle);
        TextView tvFinalReportSubtitle = findViewById(R.id.tvFinalReportSubtitle);
        TextView tvFinalScore = findViewById(R.id.tvFinalReportScore);
        TextView tvFinalStats = findViewById(R.id.tvFinalReportStats);
        TextView tvUserType = findViewById(R.id.tvFinalUserType);
        TextView tvBreedRecommendation = findViewById(R.id.tvFinalBreedRecommendation);
        TextView tvContextMessage = findViewById(R.id.tvFinalContextMessage);
        LinearLayout layoutAnimals = findViewById(R.id.layoutFinalAnimals);
        Button btnClose = findViewById(R.id.btnFinalReportClose);

        tvFinalReportTitle.setText("최종 리포트");
        tvFinalReportSubtitle.setText("최종 리포트를 불러오는 중입니다.");
        new ReportAnalysisRepository(this).getFinalReport((report, fromServer, message) -> {
            if (report == null) {
                tvFinalReportSubtitle.setText(message);
                return;
            }
            tvFinalReportSubtitle.setText(
                    "돌봄 달성도와 훈련 점수, 건강 패널티 및 AI 분석을 반영한 결과예요."
            );
            renderReport(
                    report,
                    tvFinalScore,
                    tvFinalStats,
                    tvUserType,
                    tvBreedRecommendation,
                    tvContextMessage,
                    layoutAnimals
            );
        });
        btnClose.setOnClickListener(v -> finish());
    }

    private void renderReport(
            ReportAnalysis report,
            TextView tvFinalScore,
            TextView tvFinalStats,
            TextView tvUserType,
            TextView tvBreedRecommendation,
            TextView tvContextMessage,
            LinearLayout layoutAnimals
    ) {
        tvFinalScore.setText(String.format(Locale.KOREA, "%d점 · %s 등급", report.finalScore, report.grade));
        tvFinalStats.setText(String.format(
                Locale.KOREA,
                "산책 목표 달성률  %d점\n돌발 상황 대응  %d점\n건강 페널티  %d점\n누적 산책 거리  %.1fkm\n평균 반응 시간  %d초\n누적 병원비  %,d원",
                report.walkScore,
                report.responseScore,
                report.healthPenalty,
                report.totalWalkKm,
                report.avgResponseSec,
                report.totalMedicalFee
        ));
        tvUserType.setText(safe(report.userTypeLabel) + "\n" + safe(report.userType));
        if (report.breedRecommendation == null) {
            tvBreedRecommendation.setText("추천 품종 분석 결과가 없습니다.");
        } else {
            tvBreedRecommendation.setText(String.format(
                    Locale.KOREA,
                    "%s\n\n추천 예시  %s\n\n%s",
                    safe(report.breedRecommendation.type),
                    safe(report.breedRecommendation.examples),
                    safe(report.breedRecommendation.reason)
            ));
        }
        tvContextMessage.setText(safe(report.contextMessage));
        renderAnimals(layoutAnimals, report.recommendedAnimals);
    }

    private String safe(String value) {
        return value == null ? "" : value;
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
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.setBackgroundResource(R.drawable.bg_animal_card);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardParams);

        ImageView photo = new ImageView(this);
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        photo.setBackgroundResource(R.drawable.bg_animal_photo_placeholder);
        photo.setClipToOutline(true);
        LinearLayout.LayoutParams photoParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(154)
        );
        photo.setLayoutParams(photoParams);
        Glide.with(this)
                .load(animal.filename)
                .centerCrop()
                .placeholder(R.drawable.bg_animal_photo_placeholder)
                .error(R.drawable.bg_animal_photo_placeholder)
                .into(photo);

        TextView title = new TextView(this);
        title.setText(animal.breedType);
        title.setTextColor(getColor(R.color.pet_text));
        title.setTextSize(18f);
        title.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(0, dp(12), 0, 0);
        title.setLayoutParams(titleParams);

        TextView meta = new TextView(this);
        meta.setText(buildAnimalMeta(animal));
        meta.setTextColor(getColor(R.color.pet_text_muted));
        meta.setTextSize(13f);
        meta.setLineSpacing(0f, 1.15f);

        TextView detail = new TextView(this);
        detail.setText(String.format(
                Locale.KOREA,
                "%s\n\n%s",
                animal.careNm == null ? "보호소 정보 없음" : animal.careNm,
                safe(animal.matchReason)
        ));
        detail.setTextColor(getColor(R.color.pet_text_muted));
        detail.setTextSize(14f);
        detail.setGravity(Gravity.START);
        detail.setLineSpacing(0f, 1.15f);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        detailParams.setMargins(0, dp(8), 0, 0);
        detail.setLayoutParams(detailParams);

        card.addView(photo);
        card.addView(title);
        card.addView(meta);
        card.addView(detail);
        container.addView(card);
    }

    private String buildAnimalMeta(RecommendedAnimal animal) {
        if ((animal.age != null && !animal.age.isEmpty())
                || (animal.region != null && !animal.region.isEmpty())) {
            return safe(animal.age) + " · " + safe(animal.region);
        }

        return String.format(
                Locale.KOREA,
                "%s · %s · 중성화 %s · 접수 %s",
                safe(animal.colorCd),
                formatSex(animal.sexCd),
                formatNeutered(animal.neuteredYn),
                safe(animal.happenDt)
        );
    }

    private String formatSex(String sexCd) {
        if ("M".equalsIgnoreCase(sexCd)) {
            return "남아";
        }
        if ("F".equalsIgnoreCase(sexCd)) {
            return "여아";
        }
        return "성별 미상";
    }

    private String formatNeutered(String neuteredYn) {
        if ("Y".equalsIgnoreCase(neuteredYn)) {
            return "완료";
        }
        if ("N".equalsIgnoreCase(neuteredYn)) {
            return "미완료";
        }
        return "확인 필요";
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
