package com.petready.backend.domain.command.repository;

import com.petready.backend.domain.command.entity.Command;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Command 엔티티에 대한 데이터베이스 접근을 담당하는 리포지토리입니다.
 */
@Repository
public interface CommandRepository extends JpaRepository<Command, Long> {
    
    /**
     * 특정 기기에 전달된 최근 명령 목록을 조회합니다 (오래된 순 - FIFO 큐 구조).
     * 
     * @param deviceId 기기 ID
     * @return 명령 이력 목록 (생성일 오름차순)
     */
    List<Command> findAllByDeviceDeviceIdOrderByCreatedAtAsc(String deviceId);
}
