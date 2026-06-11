package com.petready.backend.domain.report.entity;

import com.petready.backend.domain.user.entity.User;
import com.petready.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * 사용자의 전체 시뮬레이션 통계 및 성적표 정보를 담는 엔티티입니다.
 * 가상 영수증 내역과 누적 점수를 관리합니다.
 */
@Entity
@Table(name = "pet_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PetReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * 누적 평균 양육 점수 (0.00 ~ 100.00)
     */
    @Column(name = "total_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal totalScore;

    /**
     * 양육 등급 (A, B, C, D, E, F)
     */
    @Column(name = "grade", nullable = false, length = 5)
    private String grade;

    /**
     * 총 누적 가상 영수증 금액 (단위: 원)
     */
    @Column(name = "total_receipt_amount", nullable = false)
    private Long totalReceiptAmount;

    /**
     * 상세 영수증 내역 (JSON 리스트 형식: [{"item": "...", "amount": 1000, "reason": "..."}])
     */
    @Lob
    @Column(name = "receipt_details_json", columnDefinition = "TEXT")
    private String receiptDetailsJson;

    /**
     * 총 누적 산책 횟수
     */
    @Column(name = "total_walk_count", nullable = false)
    private Integer totalWalkCount;

    /**
     * 총 누적 미션 횟수
     */
    @Column(name = "total_mission_count", nullable = false)
    private Integer totalMissionCount;

    /**
     * 생성형 AI가 분석한 피드백 평론 문구 (Lazy 캐싱 대상)
     */
    @Lob
    @Column(name = "ai_feedback", columnDefinition = "TEXT")
    private String aiFeedback;

    /**
     * 리포트 정보를 새로운 데이터로 갱신합니다.
     */
    public void updateReport(BigDecimal score, String grade, Long additionalAmount, String detailsJson) {
        this.totalScore = score;
        this.grade = grade;
        this.totalReceiptAmount += additionalAmount;
        this.receiptDetailsJson = detailsJson;
    }
    
    public void updateAiFeedback(String aiFeedback) {
        this.aiFeedback = aiFeedback;
    }
    
    public void updateScoreAndGrade(BigDecimal score, String grade) {
        this.totalScore = score;
        this.grade = grade;
    }
    
    public void incrementWalkCount() {
        this.totalWalkCount++;
    }

    
    public void incrementMissionCount() {
        this.totalMissionCount++;
    }
}
