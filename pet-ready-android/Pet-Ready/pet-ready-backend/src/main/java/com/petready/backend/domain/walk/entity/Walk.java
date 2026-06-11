package com.petready.backend.domain.walk.entity;

import com.petready.backend.domain.device.entity.Device;
import com.petready.backend.domain.user.entity.User;
import com.petready.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * 반려견과 함께한 산책 기록을 저장하는 엔티티입니다.
 * 정밀도 유지를 위해 거리 관련 필드에는 BigDecimal을 사용합니다.
 */
@Entity
@Table(name = "walks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Walk extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    /**
     * 실제 산책한 거리 (km 단위)
     */
    @Column(name = "distance_km", nullable = false, precision = 10, scale = 2)
    private BigDecimal distanceKm;

    /**
     * 당시 설정되어 있던 산책 목표 거리 (km 단위)
     */
    @Column(name = "walk_goal_km", nullable = false, precision = 10, scale = 2)
    private BigDecimal walkGoalKm;

    /**
     * 산책 지속 시간 (초 단위)
     */
    @Column(name = "duration_sec", nullable = false)
    private Long durationSec;

    /**
     * 실제 산책 시작 시각
     */
    @Column(name = "started_at", nullable = false)
    private java.time.LocalDateTime startedAt;

    /**
     * 실제 산책 종료 시각
     */
    @Column(name = "ended_at", nullable = false)
    private java.time.LocalDateTime endedAt;

    /**
     * 산책 경로 데이터 (JSON 형식의 문자열)
     * DTO에서는 객체 리스트로 받지만, DB에는 직렬화된 문자열로 저장합니다.
     */
    @Lob
    @Column(name = "route_json", columnDefinition = "TEXT")
    private String routeJson;
}
