package com.petready.backend.domain.log.entity;

import com.petready.backend.domain.device.entity.Device;
import com.petready.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

/**
 * 반려견의 상태 및 기기 센서 로그 데이터를 저장하는 엔티티입니다.
 * 배터리 수준, 충전 상태, 터치 여부, 압력 값 등을 기록합니다.
 * 대량의 데이터 조회를 위해 device_id와 recorded_at에 인덱스가 적용되어 있습니다.
 */
@Entity
@Table(name = "pet_status_logs", indexes = {
    @Index(name = "idx_device_recorded", columnList = "device_id, recorded_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PetStatusLog extends BaseEntity {

    /**
     * 로그의 고유 식별자 (정수형 PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 로그를 전송한 대상 기기 (연관 관계)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    /**
     * 기기의 배터리 잔량 (단위: %)
     * 서버 사이드 가상 배터리 감쇄 시뮬레이션용으로 유지합니다.
     */
    @Column(name = "battery_level")
    private Integer batteryLevel;

    /**
     * 머리 터치 센서 활성화 여부
     */
    @Column(name = "head_touch")
    private Boolean headTouch;

    /**
     * 등 터치 센서 1 활성화 여부
     */
    @Column(name = "back_touch1")
    private Boolean backTouch1;

    /**
     * 등 터치 센서 2 활성화 여부
     */
    @Column(name = "back_touch2")
    private Boolean backTouch2;

    /**
     * 데이터가 기록된 시각 (인덱스용 별도 필드)
     */
    @CreatedDate
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private LocalDateTime recordedAt;
}
