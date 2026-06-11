package com.petready.backend.domain.rescue.repository;

import com.petready.backend.domain.rescue.entity.RescueAnimalCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * rescue_animals_cache 테이블에 대한 데이터베이스 접근을 담당하는 리포지토리입니다.
 */
@Repository
public interface RescueAnimalCacheRepository extends JpaRepository<RescueAnimalCache, Long> {

    /**
     * 공공데이터 API 제공동물 고유 ID를 통해 캐시가 존재하는지 여부를 확인합니다.
     *
     * @param animalId 공공데이터 동물 고유 ID
     * @return 존재 여부
     */
    boolean existsByAnimalId(String animalId);

    /**
     * 공공데이터 API 제공동물 고유 ID를 통해 캐시 엔티티를 조회합니다.
     *
     * @param animalId 공공데이터 동물 고유 ID
     * @return 캐시 엔티티 (Optional)
     */
    Optional<RescueAnimalCache> findByAnimalId(String animalId);
}
