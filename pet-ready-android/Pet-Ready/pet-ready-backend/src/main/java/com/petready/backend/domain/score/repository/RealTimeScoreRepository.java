package com.petready.backend.domain.score.repository;

import com.petready.backend.domain.score.entity.RealTimeScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 기기별 실시간 점수(RealTimeScore) 엔티티에 대한 데이터베이스 접근을 담당합니다.
 */
@Repository
public interface RealTimeScoreRepository extends JpaRepository<RealTimeScore, String> {
}
