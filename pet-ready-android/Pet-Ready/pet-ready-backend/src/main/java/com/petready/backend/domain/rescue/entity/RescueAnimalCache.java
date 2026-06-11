package com.petready.backend.domain.rescue.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 공공데이터 API에서 수집한 구조동물 현황 정보를 저장하는 캐시 엔티티입니다.
 * 실시간 API 호출의 부하를 줄이기 위해 주기적으로 갱신하여 사용합니다.
 */
@Entity
@Table(name = "rescue_animals_cache")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RescueAnimalCache {

    // 캐시 고유 번호
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 공공데이터 API 제공동물 고유 ID (중복 저장 방지를 위해 UNIQUE 제약조건)
    @Column(name = "animal_id", nullable = false, unique = true, length = 50)
    private String animalId;

    // 축종 (예: [개], [고양이] 등)
    @Column(name = "species", length = 50)
    private String species;

    // 품종 명칭 (예: 푸들, 코리안숏헤어 등)
    @Column(name = "breed", length = 100)
    private String breed;

    // 동물 나이
    @Column(name = "age", length = 50)
    private String age;

    // 관할 보호소명
    @Column(name = "shelter_name", length = 100)
    private String shelterName;

    // 구조 지역 (시도 단위 등)
    @Column(name = "region", length = 50)
    private String region;

    // 동물 사진 URL
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // 구조 일자
    @Column(name = "rescue_date")
    private LocalDate rescueDate;

    // 데이터 수집 및 적재 시각
    @Column(name = "cached_at", nullable = false)
    private LocalDateTime cachedAt;

    /**
     * JPA가 삽입하기 전 자동으로 적재 시각을 기록합니다.
     */
    @PrePersist
    public void prePersist() {
        if (this.cachedAt == null) {
            this.cachedAt = LocalDateTime.now();
        }
    }
}
