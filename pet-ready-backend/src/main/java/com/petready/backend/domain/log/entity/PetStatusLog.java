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
     */
    @Column(name = "battery_level")
    private Integer batteryLevel;

    /**
     * 현재 기기 충전 여부
     */
    @Column(name = "is_charging")
    private Boolean isCharging;

    /**
     * 사용자나 반려견의 물리적 확인(터치) 발생 여부
     */
    @Column(name = "touch_active")
    private Boolean touchActive;

    /**
     * 장착된 센서의 압력 측정값
     */
    @Column(name = "pressure_value")
    private Double pressureValue;

    /**
     * 데이터가 기록된 시각 (인덱스용 별도 필드)
     */
    @CreatedDate
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private LocalDateTime recordedAt;
}
