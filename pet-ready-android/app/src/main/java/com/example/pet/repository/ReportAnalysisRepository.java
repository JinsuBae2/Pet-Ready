package com.example.pet.repository;

import android.content.Context;

import com.example.pet.model.BreedRecommendation;
import com.example.pet.model.RecommendedAnimal;
import com.example.pet.model.ReportAnalysis;
import com.example.pet.model.ReportSummary;

import java.util.ArrayList;
import java.util.List;

public class ReportAnalysisRepository {
    private final ScoreRepository scoreRepository;
    private final ExpenseRepository expenseRepository;

    public ReportAnalysisRepository(Context context) {
        scoreRepository = new ScoreRepository(context);
        expenseRepository = new ExpenseRepository(context);
    }

    public ReportAnalysis getReportAnalysis() {
        ReportSummary score = scoreRepository.getReportSummary();

        ReportAnalysis report = new ReportAnalysis();
        report.finalScore = score.getFinalScore();
        report.grade = calculateGrade(report.finalScore);
        report.walkScore = score.walkScore;
        report.responseScore = score.missionScore;
        report.healthPenalty = Math.max(0, 100 - score.careScore);
        report.totalWalkKm = estimateTotalWalkKm(report.walkScore);
        report.avgResponseSec = estimateAvgResponseSec(report.responseScore);
        report.totalMedicalFee = expenseRepository.getMedicalTotal();
        report.userType = calculateUserType(report.finalScore, report.walkScore, report.responseScore);
        report.userTypeLabel = getUserTypeLabel(report.userType);
        report.breedRecommendation = buildBreedRecommendation(report.userType);
        report.contextMessage = buildContextMessage(report);
        report.recommendedAnimals = buildRecommendedAnimals(report.breedRecommendation);
        return report;
    }

    private String calculateGrade(int score) {
        if (score >= 95) return "A+";
        if (score >= 85) return "A";
        if (score >= 75) return "B+";
        if (score >= 65) return "B";
        if (score >= 55) return "C";
        if (score >= 45) return "D";
        return "F";
    }

    private String calculateUserType(int finalScore, int walkScore, int responseScore) {
        if (finalScore >= 85 && walkScore >= 80 && responseScore >= 75) {
            return "READY_ACTIVE";
        }
        if (walkScore < 70) {
            return "NEED_WALK";
        }
        if (finalScore < 60) {
            return "NOT_READY";
        }
        return "READY_STEADY";
    }

    private String getUserTypeLabel(String userType) {
        if ("READY_ACTIVE".equals(userType)) {
            return "준비된 활동가형";
        }
        if ("NEED_WALK".equals(userType)) {
            return "산책 부족형";
        }
        if ("NOT_READY".equals(userType)) {
            return "양육 준비 미흡형";
        }
        return "차분한 준비형";
    }

    private BreedRecommendation buildBreedRecommendation(String userType) {
        if ("READY_ACTIVE".equals(userType)) {
            return new BreedRecommendation(
                    "활동형 예비견주 맞춤 품종",
                    "골든 리트리버, 보더콜리, 웰시코기",
                    "산책 목표 달성률과 돌발 상황 대응이 안정적이어서 활동량이 많은 품종도 꾸준히 돌볼 준비가 되어 있습니다."
            );
        }
        if ("NEED_WALK".equals(userType)) {
            return new BreedRecommendation(
                    "실내 생활형 예비견주 맞춤 품종",
                    "말티즈, 비숑프리제, 시츄",
                    "산책 지표가 낮게 나타나 활동량이 과도하지 않고 실내 돌봄에 적응하기 쉬운 품종부터 고려하는 편이 좋습니다."
            );
        }
        if ("NOT_READY".equals(userType)) {
            return new BreedRecommendation(
                    "입양 전 준비 우선 그룹",
                    "품종 추천 보류",
                    "현재 시뮬레이션 결과로는 산책, 반응, 건강 관리 중 보완할 부분이 있어 실제 입양 전 생활 리듬 점검이 필요합니다."
            );
        }
        return new BreedRecommendation(
                "균형형 예비견주 맞춤 품종",
                "푸들, 코카스파니엘, 믹스견",
                "전반적인 돌봄 흐름은 안정적이며, 활동성과 관리 난이도가 중간 정도인 품종과 잘 맞을 가능성이 있습니다."
        );
    }

    private String buildContextMessage(ReportAnalysis report) {
        if ("NOT_READY".equals(report.userType)) {
            return "매년 약 10만 마리의 유기동물이 새로운 가족을 기다립니다. 시뮬레이션에서 놓친 돌봄이 많을수록 실제 양육 부담은 더 크게 돌아오므로, 입양 전 충분한 준비가 필요합니다.";
        }
        if ("NEED_WALK".equals(report.userType)) {
            return "공공데이터는 산책과 돌봄 부족이 양육 포기 사유로 이어질 수 있음을 보여줍니다. 실제 반려견은 매일 반복되는 산책과 관심이 필요합니다.";
        }
        return "매년 약 10만 마리의 유기동물이 새로운 가족을 기다립니다. 시뮬레이션 결과는 성실한 편이지만, 실제 양육에는 더 많은 변수가 있으므로 신중한 입양을 권장합니다.";
    }

    private List<RecommendedAnimal> buildRecommendedAnimals(BreedRecommendation recommendation) {
        List<RecommendedAnimal> animals = new ArrayList<>();
        animals.add(new RecommendedAnimal(
                "443302",
                "https://images.unsplash.com/photo-1552053831-71594a27632d",
                "2026-05-25",
                "골든 리트리버",
                "황색",
                "M",
                "Y",
                "대구 유기동물 보호센터",
                "추천 품종 예시와 일치하여 최우선 추천된 아이입니다."
        ));
        animals.add(new RecommendedAnimal(
                "443305",
                "https://images.unsplash.com/photo-1583511655857-d19b40a7a54e",
                "2026-05-24",
                "웰시코기",
                "갈색/흰색",
                "F",
                "N",
                "한국동물보호협회",
                "활동성과 돌봄 패턴이 추천 품종군과 잘 맞아 매칭되었습니다."
        ));
        return animals;
    }

    private double estimateTotalWalkKm(int walkScore) {
        return Math.round((walkScore / 100.0 * 14.5) * 10.0) / 10.0;
    }

    private int estimateAvgResponseSec(int responseScore) {
        return Math.max(120, 900 - responseScore * 6);
    }
}
