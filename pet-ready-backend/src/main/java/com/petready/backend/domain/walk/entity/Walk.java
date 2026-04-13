package com.petready.backend.domain.walk.entity;

import com.petready.backend.domain.device.entity.Device;
import com.petready.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 반려견과 함께한 산책 기록을 저장하는 엔티티입니다.
 * 산책 거리, 시간, 시작/종료 시각 및 GPS 이동 경로 데이터를 관리합니다.
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

    /**
     * 산책을 수행한 반려견의 기기
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    /**
     * 총 산책 거리 (km 단위)
     */
    @Column(name = "distance_km", nullable = false)
    private Double distanceKm;

    /**
     * 총 산책 소요 시간 (초 단위)
     */
    @Column(name = "duration_sec", nullable = false)
    private Long durationSec;

    /**
     * 산책 시작 시각
     */
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    /**
     * 산책 종료 시각
     */
    @Column(name = "ended_at", nullable = false)
    private LocalDateTime endedAt;

    /**
     * 상세 이동 경로 (JSON 형식의 좌표 데이터 배열 문자열)
     * 대용량 텍스트 데이터를 저장하기 위해 ColumnDefinition을 TEXT로 지정합니다.
     */
    @Column(name = "route_json", columnDefinition = "TEXT")
    private String routeJson;
}
