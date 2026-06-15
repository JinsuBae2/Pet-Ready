package com.example.pet.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pet.R;
import com.example.pet.model.ReportAnalysis;
import com.example.pet.repository.AccountDataManager;
import com.example.pet.repository.ReportAnalysisRepository;
import com.example.pet.repository.ResetRepository;

import java.util.Locale;

public class FinalReportActivity extends AppCompatActivity {
    public static final String EXTRA_DEMO_PREVIEW = "demo_preview";
    private Button btnClose;

    // Loading overlay UI views
    private LinearLayout layoutFinalReportLoading;
    private ScrollView scrollFinalReportContent;
    private ImageView ivLoadingGlow;
    private ImageView ivLoadingIcon;
    private ProgressBar progressLoadingCircle;
    private ProgressBar progressLoadingBar;
    private TextView tvLoadingTitle;
    private TextView tvLoadingStatus;
    private TextView tvLoadingPercent;

    private ReportAnalysis loadedReport;
    private String apiErrorMessage;
    private boolean isApiCallCompleted = false;
    private boolean isProgressAnimationCompleted = false;
    private RecommendedAnimalAdapter animalAdapter;

    private final String[] loadingSteps = {
            "반려견 활동 및 미션 기록 분석 중...",
            "AI 기반 양육 성향 유형 판단 중...",
            "맞춤 추천 견종 및 돌봄 피드백 생성 중...",
            "공공데이터 연계 추천 유기견 리스트 분석 중...",
            "최종 리포트 생성 완료!"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_final_report);

        // Bind loading views
        layoutFinalReportLoading = findViewById(R.id.layoutFinalReportLoading);
        scrollFinalReportContent = findViewById(R.id.scrollFinalReportContent);
        ivLoadingGlow = findViewById(R.id.ivLoadingGlow);
        ivLoadingIcon = findViewById(R.id.ivLoadingIcon);
        progressLoadingCircle = findViewById(R.id.progressLoadingCircle);
        progressLoadingBar = findViewById(R.id.progressLoadingBar);
        tvLoadingTitle = findViewById(R.id.tvLoadingTitle);
        tvLoadingStatus = findViewById(R.id.tvLoadingStatus);
        tvLoadingPercent = findViewById(R.id.tvLoadingPercent);

        TextView tvFinalReportTitle = findViewById(R.id.tvFinalReportTitle);
        TextView tvFinalReportSubtitle = findViewById(R.id.tvFinalReportSubtitle);
        TextView tvFinalScore = findViewById(R.id.tvFinalReportScore);
        TextView tvFinalStats = findViewById(R.id.tvFinalReportStats);
        TextView tvUserType = findViewById(R.id.tvFinalUserType);
        TextView tvBreedRecommendation = findViewById(R.id.tvFinalBreedRecommendation);
        TextView tvContextMessage = findViewById(R.id.tvFinalContextMessage);
        RecyclerView rvAnimals = findViewById(R.id.rvFinalAnimals);
        TextView tvAnimalsEmpty = findViewById(R.id.tvFinalAnimalsEmpty);
        btnClose = findViewById(R.id.btnFinalReportClose);

        animalAdapter = new RecommendedAnimalAdapter();
        rvAnimals.setLayoutManager(new LinearLayoutManager(this));
        rvAnimals.setAdapter(animalAdapter);

        tvFinalReportTitle.setText("최종 리포트");
        tvFinalReportSubtitle.setText("돌봄 달성도와 훈련 점수, 건강 패널티 및 AI 분석을 반영한 결과예요.");

        // Start loading animations and fetch report
        startLoadingAnimations(
                tvFinalScore,
                tvFinalStats,
                tvUserType,
                tvBreedRecommendation,
                tvContextMessage,
                rvAnimals,
                tvAnimalsEmpty,
                tvFinalReportSubtitle
        );

        new ReportAnalysisRepository(this).getFinalReport((report, fromServer, message) -> {
            loadedReport = report;
            apiErrorMessage = message;
            isApiCallCompleted = true;
            checkAndFinishLoading(
                    tvFinalScore,
                    tvFinalStats,
                    tvUserType,
                    tvBreedRecommendation,
                    tvContextMessage,
                    rvAnimals,
                    tvAnimalsEmpty,
                    tvFinalReportSubtitle
            );
        });

        btnClose.setOnClickListener(v -> closeReport());
    }

    private void startLoadingAnimations(
            TextView tvFinalScore,
            TextView tvFinalStats,
            TextView tvUserType,
            TextView tvBreedRecommendation,
            TextView tvContextMessage,
            RecyclerView rvAnimals,
            TextView tvAnimalsEmpty,
            TextView tvFinalReportSubtitle
    ) {
        // 1. Glow Pulse Animation (Radial Gradient background glow)
        ObjectAnimator scaleXGlow = ObjectAnimator.ofFloat(ivLoadingGlow, "scaleX", 0.9f, 1.15f);
        ObjectAnimator scaleYGlow = ObjectAnimator.ofFloat(ivLoadingGlow, "scaleY", 0.9f, 1.15f);
        ObjectAnimator alphaGlow = ObjectAnimator.ofFloat(ivLoadingGlow, "alpha", 0.6f, 1.0f);

        scaleXGlow.setDuration(1200);
        scaleXGlow.setRepeatCount(ValueAnimator.INFINITE);
        scaleXGlow.setRepeatMode(ValueAnimator.REVERSE);
        scaleYGlow.setDuration(1200);
        scaleYGlow.setRepeatCount(ValueAnimator.INFINITE);
        scaleYGlow.setRepeatMode(ValueAnimator.REVERSE);
        alphaGlow.setDuration(1200);
        alphaGlow.setRepeatCount(ValueAnimator.INFINITE);
        alphaGlow.setRepeatMode(ValueAnimator.REVERSE);

        AnimatorSet glowAnim = new AnimatorSet();
        glowAnim.playTogether(scaleXGlow, scaleYGlow, alphaGlow);
        glowAnim.start();

        // 2. Robot Pet Floating Animation
        ObjectAnimator floatIcon = ObjectAnimator.ofFloat(ivLoadingIcon, "translationY", -12f, 12f);
        floatIcon.setDuration(1500);
        floatIcon.setRepeatCount(ValueAnimator.INFINITE);
        floatIcon.setRepeatMode(ValueAnimator.REVERSE);
        floatIcon.start();

        // 3. Progress simulated increment 0% to 95% over 4000ms
        ValueAnimator progressAnimator = ValueAnimator.ofInt(0, 95);
        progressAnimator.setDuration(4000);
        progressAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int progress = (int) animation.getAnimatedValue();
                progressLoadingBar.setProgress(progress);
                tvLoadingPercent.setText(progress + "%");

                if (progress < 25) {
                    tvLoadingStatus.setText(loadingSteps[0]);
                } else if (progress < 50) {
                    tvLoadingStatus.setText(loadingSteps[1]);
                } else if (progress < 75) {
                    tvLoadingStatus.setText(loadingSteps[2]);
                } else {
                    tvLoadingStatus.setText(loadingSteps[3]);
                }
            }
        });
        progressAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isProgressAnimationCompleted = true;
                if (!isApiCallCompleted) {
                    tvLoadingStatus.setText("거의 완료되었습니다! 결과를 받아오고 있습니다...");
                } else {
                    checkAndFinishLoading(
                            tvFinalScore,
                            tvFinalStats,
                            tvUserType,
                            tvBreedRecommendation,
                            tvContextMessage,
                            rvAnimals,
                            tvAnimalsEmpty,
                            tvFinalReportSubtitle
                    );
                }
            }
        });
        progressAnimator.start();
    }

    private void checkAndFinishLoading(
            TextView tvFinalScore,
            TextView tvFinalStats,
            TextView tvUserType,
            TextView tvBreedRecommendation,
            TextView tvContextMessage,
            RecyclerView rvAnimals,
            TextView tvAnimalsEmpty,
            TextView tvFinalReportSubtitle
    ) {
        if (isApiCallCompleted && isProgressAnimationCompleted) {
            // Animate progress from current progress to 100%
            int currentProgress = progressLoadingBar.getProgress();
            ValueAnimator finalProgress = ValueAnimator.ofInt(currentProgress, 100);
            finalProgress.setDuration(500);
            finalProgress.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int p = (int) animation.getAnimatedValue();
                    progressLoadingBar.setProgress(p);
                    tvLoadingPercent.setText(p + "%");
                }
            });
            tvLoadingStatus.setText(loadingSteps[4]); // "최종 리포트 생성 완료!"
            finalProgress.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (loadedReport != null) {
                        renderReport(
                                loadedReport,
                                tvFinalScore,
                                tvFinalStats,
                                tvUserType,
                                tvBreedRecommendation,
                                tvContextMessage,
                                animalAdapter,
                                rvAnimals,
                                tvAnimalsEmpty
                        );
                    } else {
                        tvFinalReportSubtitle.setText(apiErrorMessage);
                    }
                    fadeToContent();
                }
            });
            finalProgress.start();
        }
    }

    private void fadeToContent() {
        layoutFinalReportLoading.animate()
                .alpha(0f)
                .setDuration(400)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        layoutFinalReportLoading.setVisibility(View.GONE);
                        scrollFinalReportContent.setVisibility(View.VISIBLE);
                        scrollFinalReportContent.setAlpha(0f);
                        scrollFinalReportContent.animate()
                                .alpha(1f)
                                .setDuration(400)
                                .start();
                    }
                })
                .start();
    }

    private void closeReport() {
        btnClose.setEnabled(false);
        btnClose.setText("초기화 중...");
        new ResetRepository(this).resetSimulation((success, message) -> {
            Toast.makeText(this, message, success ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
            if (!success) {
                btnClose.setEnabled(true);
                btnClose.setText("처음부터 다시 시작");
                return;
            }

            AccountDataManager.resetForNewSimulation(this);
            Intent intent = new Intent(this, SimulationSetupActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    @Override
    public void onBackPressed() {
        closeReport();
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
