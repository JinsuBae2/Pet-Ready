package com.petready.backend.domain.score.entity;

import com.petready.backend.domain.device.entity.Device;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 기기별 실시간 현재 점수를 관리하는 엔티티입니다.
 * 점수 변동 시 0~100 사이의 범위를 벗어나지 않도록 제한(Clamping)합니다.
 */
@Entity
@Table(name = "real_time_scores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RealTimeScore {

    // IoT 기기 고유 ID (PK 및 FK로 사용됨)
    @Id
    @Column(name = "device_id", length = 50)
    private String deviceId;

    // 점수를 소유한 기기와의 1:1 식별 관계 (DB상 device_id를 공유함)
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "device_id")
    private Device device;

    // 현재 실시간 점수 (기본값 100점)
    @Builder.Default
    @Column(name = "current_score", nullable = false)
    private Integer currentScore = 100;

    // 점수 최종 수정 시각
    @Column(name = "last_updated_at", nullable = false)
    private LocalDateTime lastUpdatedAt;

    /**
     * JPA가 삽입/수정하기 전 자동으로 최종 수정 시각을 갱신합니다.
     */
    @PrePersist
    @PreUpdate
    public void preUpdate() {
        this.lastUpdatedAt = LocalDateTime.now();
    }

    public void applyScoreDelta(int delta) {
        this.currentScore += delta;
        if (this.currentScore < 0) {
            this.currentScore = 0;
        } else if (this.currentScore > 100) {
            this.currentScore = 100;
        }
    }

    /**
     * 점수를 100점으로 초기화하고 수정 시각을 갱신합니다.
     */
    public void resetScoreToMax() {
        this.currentScore = 100;
        this.lastUpdatedAt = LocalDateTime.now();
    }
}
