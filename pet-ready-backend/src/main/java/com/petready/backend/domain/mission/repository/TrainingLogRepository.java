package com.petready.backend.domain.mission.repository;

import com.petready.backend.domain.mission.entity.TrainingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 훈련 결과 로그 데이터에 접근하기 위한 JPA Repository 인터페이스입니다.
 */
@Repository
public interface TrainingLogRepository extends JpaRepository<TrainingLog, Long> {

    /**
     * 특정 기기에 등록된 전체 훈련 로그를 반환합니다.
     */
    List<TrainingLog> findAllByDeviceDeviceId(String deviceId);

    /**
     * 특정 기기의 총 훈련 시도 횟수를 조회합니다.
     */
    long countByDeviceDeviceId(String deviceId);

    /**
     * 특정 기기 및 특정 훈련 상태(SUCCESS, CONFUSED, SAD)의 로그 개수를 조회합니다.
     */
    long countByDeviceDeviceIdAndStatus(String deviceId, String status);

    /**
     * 특정 기기 및 특정 훈련 상태의 가장 최근 로그 1건을 조회합니다.
     */
    java.util.Optional<TrainingLog> findFirstByDeviceDeviceIdAndStatusOrderByCreatedAtDesc(String deviceId, String status);
}
