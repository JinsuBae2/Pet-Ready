package com.petready.backend.domain.device.repository;

import com.petready.backend.domain.device.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Device 엔티티에 대한 데이터베이스 접근을 담당하는 리포지토리입니다.
 */
@Repository
public interface DeviceRepository extends JpaRepository<Device, String> {
    
    /**
     * 특정 사용자가 소유한 기기 목록을 조회합니다.
     * 
     * @param userId 사용자의 ID
     * @return 기기 목록
     */
    List<Device> findAllByUserId(Long userId);

    /**
     * 사용자의 이메일을 기준으로 소유한 기기를 조회합니다.
     *
     * @param email 사용자의 이메일
     * @return 기기 엔티티 (Optional)
     */
    Optional<Device> findByUserEmail(String email);

    @Modifying
    @Query(value = "DELETE FROM score_events WHERE device_id = ?1", nativeQuery = true)
    void deleteScoreEventsByDeviceId(String deviceId);

    @Modifying
    @Query(value = "DELETE FROM commands WHERE device_id = ?1", nativeQuery = true)
    void deleteCommandsByDeviceId(String deviceId);

    @Modifying
    @Query(value = "DELETE FROM missions WHERE device_id = ?1", nativeQuery = true)
    void deleteMissionsByDeviceId(String deviceId);

    @Modifying
    @Query(value = "DELETE FROM walks WHERE device_id = ?1", nativeQuery = true)
    void deleteWalksByDeviceId(String deviceId);

    @Modifying
    @Query(value = "DELETE FROM pet_status_logs WHERE device_id = ?1", nativeQuery = true)
    void deletePetStatusLogsByDeviceId(String deviceId);

    @Modifying
    @Query(value = "DELETE FROM real_time_scores WHERE device_id = ?1", nativeQuery = true)
    void deleteRealTimeScoreByDeviceId(String deviceId);
}
