package com.example.pet.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pet.R;
import com.example.pet.model.ReportAnalysis;
import com.example.pet.repository.ReportAnalysisRepository;

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
        RecyclerView rvAnimals = findViewById(R.id.rvFinalAnimals);
        TextView tvAnimalsEmpty = findViewById(R.id.tvFinalAnimalsEmpty);
        Button btnClose = findViewById(R.id.btnFinalReportClose);
        RecommendedAnimalAdapter animalAdapter = new RecommendedAnimalAdapter();
        rvAnimals.setLayoutManager(new LinearLayoutManager(this));
        rvAnimals.setAdapter(animalAdapter);

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
                    animalAdapter,
                    rvAnimals,
                    tvAnimalsEmpty
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
            RecommendedAnimalAdapter animalAdapter,
            RecyclerView rvAnimals,
            TextView tvAnimalsEmpty
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
        animalAdapter.submitList(report.recommendedAnimals);
        boolean empty = animalAdapter.isEmpty();
        tvAnimalsEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvAnimals.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

}
