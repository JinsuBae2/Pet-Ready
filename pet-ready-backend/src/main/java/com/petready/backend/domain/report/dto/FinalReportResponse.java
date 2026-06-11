package com.petready.backend.domain.report.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 최종 양육 리포트 API 응답 DTO 클래스입니다.
 */
@Getter
@Builder
public class FinalReportResponse {
    
    // 최종 누적 점수
    private Integer finalScore;
    
    // 양육 등급 (A+, A, B+, B, C, F)
    private String grade;
    
    // 산책 점수
    private Integer walkScore;
    
    // 알림 반응 점수
    private Integer responseScore;
    
    // 건강 관리 벌점
    private Integer healthPenalty;
    
    // 총 누적 산책 거리 (km)
    private Double totalWalkKm;
    
    // 평균 응답 시간 (초)
    private Integer avgResponseSec;
    
    // 총 의료비 패널티 금액 (원)
    private Integer totalMedicalFee;
    
    // AI 판별 사용자 유형 코드 (예: READY_ACTIVE)
    private String userType;
    
    // 사용자 유형 라벨 (예: 준비된 활동가형)
    private String userTypeLabel;
    
    // 추천 반려동물 정보 객체
    private BreedRecommendation breedRecommendation;
    
    // 교육용 사회적 맥락 문구
    private String contextMessage;
    
    // 실시간 매칭 추천 보호동물 리스트 (최대 5건)
    private List<RecommendedAnimalDto> recommendedAnimals;

    @Getter
    @Builder
    public static class BreedRecommendation {
        private String type;
        private String examples;
        private String reason;
    }

    @Getter
    @Builder
    public static class RecommendedAnimalDto {
        private String animalId;
        private String breed;
        private String age;
        private String shelterName;
        private String region;
        private String imageUrl;
        private Boolean isFallback;
        private String matchReason;
    }
}
