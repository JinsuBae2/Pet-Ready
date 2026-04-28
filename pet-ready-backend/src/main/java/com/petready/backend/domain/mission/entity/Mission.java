package com.petready.backend.domain.mission.entity;

import com.petready.backend.domain.device.entity.Device;
import com.petready.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 사용자에게 할당된 미션(예: 새벽 짖음 대처 등) 이력을 관리하는 엔티티입니다.
 * 미션 발생 시각, 사용자의 응답 시각 및 완료 여부를 기록합니다.
 */
@Entity
@Table(name = "missions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Mission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 미션이 발생한 대상 기기
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    /**
     * 미션의 종류 (예: BARKING_ALERT, FEEDING_TIME 등)
     */
    @Column(nullable = false, length = 50)
    private String type;

    /**
     * 미션이 발급된 시각
     */
    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    /**
     * 사용자가 미션에 응답한 시각
     */
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    /**
     * 미션 발급 후 응답까지 걸린 시간 (초 단위)
     */
    @Column(name = "response_time_sec")
    private Long responseTimeSec;

    /**
     * 미션 완료 여부
     */
    @Builder.Default
    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted = false;

    /**
     * 미션을 완료 처리하고 응답 시간을 기록합니다.
     * @param respondedAt 응답한 시각
     * @param responseTimeSec 발급부터 응답까지 걸린 시간(초)
     */
    public void complete(LocalDateTime respondedAt, Long responseTimeSec) {
        this.respondedAt = respondedAt;
        this.responseTimeSec = responseTimeSec;
        this.isCompleted = true;
    }
}
