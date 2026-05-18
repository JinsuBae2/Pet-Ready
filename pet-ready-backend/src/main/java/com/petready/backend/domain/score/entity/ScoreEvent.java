package com.petready.backend.domain.score.entity;

import com.petready.backend.domain.device.entity.Device;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 실시간 점수 변동 이벤트 히스토리를 기록하는 엔티티입니다.
 * 미션, 산책 등 각 이벤트 발생 시 변동폭과 결과 점수를 저장합니다.
 */
@Entity
@Table(name = "score_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ScoreEvent {

    // 로그 고유 번호
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 이벤트를 발생시킨 기기 (외래 키 연관관계)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    // 이벤트 타입 코드 (예: MISSION_FAST_COMPLETE 등)
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    // 점수 변동값 (부호 포함, 예: +5, -10)
    @Column(name = "delta", nullable = false)
    private Integer delta;

    // 변동 적용 후 최종 점수
    @Column(name = "score_after", nullable = false)
    private Integer scoreAfter;

    // 이벤트 발생 시각
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    /**
     * JPA가 삽입하기 전 자동으로 발생 시각을 기록합니다.
     */
    @PrePersist
    public void prePersist() {
        if (this.occurredAt == null) {
            this.occurredAt = LocalDateTime.now();
        }
    }
}
