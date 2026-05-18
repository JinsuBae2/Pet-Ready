package com.petready.backend.domain.walk.repository;

import com.petready.backend.domain.walk.entity.Walk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalkRepository extends JpaRepository<Walk, Long> {
    /**
     * 특정 사용자의 모든 산책 기록을 조회합니다.
     */
    List<Walk> findAllByUserEmail(String email);
}
