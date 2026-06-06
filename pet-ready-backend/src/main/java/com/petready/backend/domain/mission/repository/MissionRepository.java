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

    /**
     * 특정 기기의 지정된 타입 중 완료되지 않은 가장 최근 미션을 조회합니다.
     *
     * @param deviceId 기기 ID
     * @param type 미션 타입
     * @return 미완료 미션 (Optional)
     */
    java.util.Optional<Mission> findFirstByDeviceDeviceIdAndTypeAndIsCompletedFalseOrderByIssuedAtDesc(String deviceId, String type);

    /**
     * 특정 타입의 미션 중 지정된 시간 이전에 발급되었으나 아직 완료되지 않은 미션을 조회합니다.
     *
     * @param type 미션 타입
     * @param time 지정 시간
     * @return 만료된 미션 목록
     */
    List<Mission> findAllByTypeAndIsCompletedFalseAndIssuedAtBefore(String type, java.time.LocalDateTime time);

    /**
     * 특정 기기의 지정된 시간 이후에 발급된 미션 목록을 조회합니다.
     *
     * @param deviceId 기기 ID
     * @param time 지정 시간
     * @return 미션 목록
     */
    List<Mission> findAllByDeviceDeviceIdAndIssuedAtAfter(String deviceId, java.time.LocalDateTime time);

    /**
     * 특정 기기에 특정 타입의 미션이 지정된 시간 이후에 발행되었는지 여부를 확인합니다.
     * (동시성 다중 호출 시 중복 생성 방지를 위해 사용됩니다.)
     *
     * @param deviceId 기기 ID
     * @param type 미션 타입
     * @param time 기준 시간 (오늘 자정 등)
     * @return 발행 여부
     */
    boolean existsByDeviceDeviceIdAndTypeAndIssuedAtAfter(String deviceId, String type, java.time.LocalDateTime time);

    /**
     * 특정 사용자의 모든 미션 목록을 조회합니다.
     * (누적 평균 점수 산출을 위해 사용됩니다.)
     * 
     * @param email 사용자 이메일
     * @return 전체 미션 목록
     */
    List<Mission> findAllByDeviceUserEmail(String email);

    /**
     * 특정 기기의 지정된 시간 이후에 발급된 모든 미션 목록을 조회합니다.
     * (오늘의 미션 목록 조회를 위해 사용됩니다.)
     * 
     * @param deviceId 기기 ID
     * @param time 기준 시간 (오늘 자정 등)
     * @return 지정 시간 이후 발급된 미션 목록
     */
    List<Mission> findAllByDeviceDeviceIdAndIssuedAtAfter(String deviceId, java.time.LocalDateTime time);
}
