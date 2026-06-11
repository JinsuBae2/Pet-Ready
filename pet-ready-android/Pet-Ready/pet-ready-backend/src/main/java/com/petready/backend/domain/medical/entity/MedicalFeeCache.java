package com.petready.backend.domain.medical.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 전국 동물병원 진료비(초진료, 재진료 등) 데이터를 캐싱하는 엔티티입니다.
 * 외부 데이터 연동 실패를 대비해 안정적인 과금 시뮬레이션을 지원합니다.
 */
@Entity
@Table(name = "medical_fee_cache")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MedicalFeeCache {

    // 캐시 고유 식별자
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 진료 항목 명칭 (예: 초진료, 재진료, 백신접종 등)
    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    // 항목별 평균 진료 비용 (오차 방지를 위해 BigDecimal 사용)
    @Column(name = "fee_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal feeAmount;

    // 해당 데이터가 수집/기준이 되는 날짜
    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    // 데이터가 캐시 테이블에 삽입된 실제 시각
    @Column(name = "cached_at", nullable = false, updatable = false)
    private LocalDateTime cachedAt;

    /**
     * JPA가 데이터를 저장하기 전에 캐시 시간을 자동 설정합니다.
     */
    @PrePersist
    public void prePersist() {
        if (this.cachedAt == null) {
            this.cachedAt = LocalDateTime.now();
        }
    }
}
