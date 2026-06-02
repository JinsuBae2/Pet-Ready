package com.petready.backend.domain.medical.repository;

import com.petready.backend.domain.medical.entity.MedicalFeeCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

/**
 * 진료비 캐시 데이터에 대한 데이터베이스 접근을 담당합니다.
 */
@Repository
public interface MedicalFeeCacheRepository extends JpaRepository<MedicalFeeCache, Long> {

    /**
     * 특정 일자의 진료비 캐시 데이터가 존재하는지 확인합니다.
     * (중복 삽입 방지 목적)
     * 
     * @param targetDate 확인할 기준 일자
     * @return 데이터 존재 여부
     */
    boolean existsByTargetDate(LocalDate targetDate);
}
