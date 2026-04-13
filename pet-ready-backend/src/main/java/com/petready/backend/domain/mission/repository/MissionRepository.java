package com.petready.backend.domain.mission.repository;

import com.petready.backend.domain.mission.entity.Mission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Mission 엔티티에 대한 데이터베이스 접근을 담당하는 리포지토리입니다.
 */
@Repository
public interface MissionRepository extends JpaRepository<Mission, Long> {
    
    /**
     * 특정 기기의 완료되지 않은 미션 목록을 조회합니다.
     * 
     * @param deviceId 기기 ID
     * @return 미완료 미션 목록
     */
    List<Mission> findAllByDeviceDeviceIdAndIsCompletedFalse(String deviceId);
}
