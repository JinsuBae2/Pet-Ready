package com.petready.backend.domain.analysis.entity;

import com.petready.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 사용자의 시뮬레이션 활동 패턴을 분석한 결과 및 
 * 그에 따른 맞춤형 반려동물 추천 정보를 저장하는 엔티티입니다.
 */
@Entity
@Table(name = "user_analysis_results")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserAnalysisResult {

    // 결과 고유 번호
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 분석 대상 사용자 (외래 키 연관관계)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // AI 판별 사용자 유형 코드 (예: READY_ACTIVE, NEED_WALK 등)
    @Column(name = "user_type", nullable = false, length = 50)
    private String userType;

    // 화면 표시용 사용자 유형 설명 문구
    @Column(name = "user_type_label", nullable = false, length = 100)
    private String userTypeLabel;

    // 추천 반려동물 유형 (소형견, 중형견 등)
    @Column(name = "breed_type", length = 50)
    private String breedType;

    // 추천 대표 품종 예시 (예: "푸들, 말티즈")
    @Column(name = "breed_examples", length = 255)
    private String breedExamples;

    // 해당 품종을 추천하는 이유에 대한 상세 설명
    @Lob
    @Column(name = "breed_reason", columnDefinition = "TEXT")
    private String breedReason;

    // 반려견 입양에 대한 사회적 책임 및 맥락 교육용 메시지
    @Lob
    @Column(name = "context_message", columnDefinition = "TEXT")
    private String contextMessage;

    // AI 분석 수행 시각
    @Column(name = "analyzed_at", nullable = false, updatable = false)
    private LocalDateTime analyzedAt;

    /**
     * JPA가 삽입하기 전 자동으로 분석 시각을 기록합니다.
     */
    @PrePersist
    public void prePersist() {
        if (this.analyzedAt == null) {
            this.analyzedAt = LocalDateTime.now();
        }
    }
}
