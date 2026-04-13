package com.petready.backend.domain.log.repository;

import com.petready.backend.domain.log.entity.PetStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PetStatusLog 엔티티에 대한 데이터베이스 접근을 담당하는 리포지토리입니다.
 */
@Repository
public interface PetStatusLogRepository extends JpaRepository<PetStatusLog, Long> {
    
    /**
     * 특정 기기의 특정 시간 범위 내 로그를 조회합니다.
     * 
     * @param deviceId 기기 ID
     * @param start 시작 시각
     * @param end 종료 시각
     * @return 로그 목록
     */
    List<PetStatusLog> findAllByDeviceDeviceIdAndRecordedAtBetween(String deviceId, LocalDateTime start, LocalDateTime end);
}
