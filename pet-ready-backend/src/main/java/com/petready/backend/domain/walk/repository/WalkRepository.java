package com.petready.backend.domain.walk.repository;

import com.petready.backend.domain.walk.entity.Walk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Walk 엔티티에 대한 데이터베이스 접근을 담당하는 리포지토리입니다.
 */
@Repository
public interface WalkRepository extends JpaRepository<Walk, Long> {
    
    /**
     * 특정 기기의 최근 산책 기록 목록을 조회합니다.
     * 
     * @param deviceId 기기 ID
     * @return 산책 기록 목록
     */
    List<Walk> findAllByDeviceDeviceIdOrderByStartedAtDesc(String deviceId);
}
