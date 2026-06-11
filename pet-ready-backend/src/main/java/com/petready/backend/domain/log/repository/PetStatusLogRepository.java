package com.petready.backend.domain.log.repository;

import com.petready.backend.domain.log.entity.PetStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    /**
     * 특정 기기의 가장 최근 로그 1건을 조회합니다.
     * 
     * @param deviceId 기기 ID
     * @return 가장 최근 로그 (Optional)
     */
    java.util.Optional<PetStatusLog> findFirstByDeviceDeviceIdOrderByRecordedAtDesc(String deviceId);

    /**
     * 특정 기기의 머리 터치 또는 등 터치가 발생한 가장 최근 로그 1건을 조회합니다.
     */
    @Query("SELECT l FROM PetStatusLog l WHERE l.device.deviceId = :deviceId AND (l.headTouch = true OR l.backTouch1 = true OR l.backTouch2 = true) ORDER BY l.recordedAt DESC")
    Optional<PetStatusLog> findLastTouchLog(@Param("deviceId") String deviceId);

    /**
     * 특정 시각 이전에 저장된 모든 상태 로그들을 삭제합니다.
     * 
     * @param dateTime 기준 시각
     * @return 삭제된 행 개수
     */
    long deleteByRecordedAtBefore(LocalDateTime dateTime);
}

