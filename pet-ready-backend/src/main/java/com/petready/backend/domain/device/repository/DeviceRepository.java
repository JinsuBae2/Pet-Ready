package com.petready.backend.domain.device.repository;

import com.petready.backend.domain.device.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}
